package rockstar.modules.autobuy;

import java.util.List;
import java.util.Locale;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

import pyrock.events.game.GameTickEvent;

import rockstar.client.autobuy.FunTimePriceParser;
import rockstar.client.autobuy.ItemManager;
import rockstar.client.event.EventListener;
import rockstar.client.module.Module;
import rockstar.client.module.ModuleCategory;
import rockstar.client.module.ModuleInfo;
import rockstar.client.setting.ButtonSetting;
import rockstar.client.setting.SliderSetting;

/**
 * CRESTClient AutoSetup — ported from Zenith (1.21.4) AutoSetup to 1.21.11.
 *
 * <p>Walks every {@link ItemManager} entry, searches the FunTime auction,
 * finds the cheapest current price per item over a time window, then writes
 * {@code bestPrice * (1 - discount)} into {@link AutoBuy}'s buy price.
 */
@ModuleInfo(name = "AutoSetup", category = ModuleCategory.AUTOBUY, internalMethod09633 = "modules.descriptions.autosetup")
public class AutoSetup extends Module {

    private static volatile AutoSetup instance;

    private final SliderSetting discountPercent;
    private final SliderSetting searchDuration;
    private final SliderSetting pageUpdateDelay;
    private final ButtonSetting startButton;

    private boolean isSettingUp = false;
    private boolean waitingForAuction = false;
    private boolean retrying = false;
    private int currentItemIndex = 0;
    private int refreshCount = 0;
    private long bestPricePerItem = Long.MAX_VALUE;
    private String currentItemId = "";

    private long pageTimerAt = 0L;
    private long refreshTimerAt = 0L;
    private long retryTimerAt = 0L;

    private HandledScreen<?> currentAuctionScreen = null;

    private final EventListener<GameTickEvent> tickListener = event -> onTick();

    public AutoSetup() {
        super();
        instance = this;

        this.discountPercent = new SliderSetting(this, "Discount Percent")
                .internalMethod05900(10f).internalMethod02732(90f).internalMethod08673(1f).internalMethod08074(50f);
        this.searchDuration = new SliderSetting(this, "Search Duration (s)")
                .internalMethod05900(5f).internalMethod02732(60f).internalMethod08673(1f).internalMethod08074(10f);
        this.pageUpdateDelay = new SliderSetting(this, "Page Update Delay (ms)")
                .internalMethod05900(1000f).internalMethod02732(5000f).internalMethod08673(100f).internalMethod08074(2000f);

        this.startButton = new ButtonSetting(this, "Запустить автонастройку").internalMethod07149(this::startSetup);
    }

    public static AutoSetup getInstance() {
        return instance;
    }

    // ---------------- lifecycle ----------------

    @Override
    public void onDisable() {
        isSettingUp = false;
        waitingForAuction = false;
        retrying = false;
        currentAuctionScreen = null;
        super.onDisable();
    }

    // ---------------- control ----------------

    public void startSetup() {
        AutoBuy autoBuy = AutoBuy.getInstance();
        if (autoBuy == null) {
            return;
        }
        isSettingUp = true;
        currentItemIndex = 0;
        retrying = false;
        waitingForAuction = false;
        searchNextItem();
    }

    public void stop() {
        isSettingUp = false;
        waitingForAuction = false;
        retrying = false;
    }

    // ---------------- tick ----------------

    private void onTick() {
        if (!isSettingUp) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        long now = System.currentTimeMillis();

        if (retrying && now - retryTimerAt > 2000L) {
            retrying = false;
            searchCurrentItem(mc);
        }

        if (mc.currentScreen instanceof HandledScreen<?> screen) {
            currentAuctionScreen = screen;

            if (waitingForAuction) {
                waitingForAuction = false;
                retrying = false;
                refreshCount = 0;
                bestPricePerItem = Long.MAX_VALUE;
                pageTimerAt = now;
                refreshTimerAt = now;
            }

            if (!waitingForAuction && !retrying) {
                long windowMs = (long) (searchDuration.internalMethod08576() * 1000L);
                if (now - pageTimerAt < windowMs) {
                    scanPrices(mc, screen);
                    if (now - refreshTimerAt > (long) pageUpdateDelay.internalMethod08576()) {
                        refreshPage(mc, screen);
                        refreshTimerAt = now;
                        refreshCount++;
                    }
                } else {
                    finishCurrentItem(mc, now);
                }
            }
        } else {
            if (!waitingForAuction && !retrying && isSettingUp
                    && currentItemIndex < ItemManager.getInstance().auctionList().size()) {
                retrying = true;
                retryTimerAt = now;
            }
            currentAuctionScreen = null;
        }
    }

    private List<ItemManager.ItemEntry> list() {
        return ItemManager.getInstance().auctionList();
    }

    private void searchNextItem() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (currentItemIndex >= list().size()) {
            finishSetup(mc);
            return;
        }
        searchCurrentItem(mc);
    }

    private void searchCurrentItem(MinecraftClient mc) {
        List<ItemManager.ItemEntry> list = list();
        if (currentItemIndex >= list.size()) {
            finishSetup(mc);
            return;
        }
        ItemManager.ItemEntry data = list.get(currentItemIndex);
        currentItemId = data.id();
        String searchTerm = data.searchTerm() == null ? data.name() : data.searchTerm();

        if (mc.player != null) {
            mc.player.networkHandler.sendChatCommand("ah search " + searchTerm);
        }
        waitingForAuction = true;
        bestPricePerItem = Long.MAX_VALUE;
    }

    private void scanPrices(MinecraftClient mc, HandledScreen<?> screen) {
        var handler = screen.getScreenHandler();
        for (int i = 0; i < handler.slots.size(); i++) {
            Slot slot = handler.getSlot(i);
            if (slot.inventory == mc.player.getInventory()) continue;
            ItemStack stack = slot.getStack();
            if (stack == null || stack.isEmpty()) continue;
            if (isRefreshButton(stack)) continue;
            if (ItemManager.isSuspicious(stack)) continue; // anti-cheat

            long totalPrice = FunTimePriceParser.parsePriceFromLore(ItemManager.loreStrings(stack));
            if (totalPrice <= 0L) continue;

            int count = Math.max(1, stack.getCount());
            long pricePerItem = totalPrice / count;
            if (pricePerItem > 0L && pricePerItem < bestPricePerItem) {
                bestPricePerItem = pricePerItem;
            }
        }
    }

    private void refreshPage(MinecraftClient mc, HandledScreen<?> screen) {
        var handler = screen.getScreenHandler();
        for (Slot slot : handler.slots) {
            ItemStack stack = slot.getStack();
            if (stack == null || stack.isEmpty()) continue;
            if (isRefreshButton(stack)) {
                mc.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.PICKUP, mc.player);
                break;
            }
        }
    }

    private boolean isRefreshButton(ItemStack stack) {
        String name = FunTimePriceParser.clean(stack.getName().getString()).toLowerCase(Locale.ROOT);
        return name.contains("обновить") || name.contains("refresh");
    }

    private void finishCurrentItem(MinecraftClient mc, long now) {
        AutoBuy autoBuy = AutoBuy.getInstance();
        if (bestPricePerItem != Long.MAX_VALUE && bestPricePerItem > 0L && autoBuy != null) {
            long discounted = (long) (bestPricePerItem * (1.0 - discountPercent.internalMethod08576() / 100.0));
            if (discounted < 0L) discounted = 0L;
            autoBuy.setPriceForItem(currentItemId, discounted);
        }

        if (mc.currentScreen != null) mc.currentScreen.close();
        currentItemIndex++;

        if (currentItemIndex < list().size()) {
            searchNextItem();
        } else {
            finishSetup(mc);
        }
    }

    private void finishSetup(MinecraftClient mc) {
        isSettingUp = false;
        waitingForAuction = false;
        retrying = false;
        if (mc.currentScreen != null) mc.currentScreen.close();
    }
}
