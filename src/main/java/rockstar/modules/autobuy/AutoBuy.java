package rockstar.modules.autobuy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

import pyrock.events.game.GameTickEvent;
import pyrock.events.network.ReceivePacketEvent;

import rockstar.client.autobuy.FunTimePriceParser;
import rockstar.client.autobuy.ItemManager;
import rockstar.client.event.EventListener;
import rockstar.client.module.Module;
import rockstar.client.module.ModuleCategory;
import rockstar.client.module.ModuleInfo;
import rockstar.client.setting.ButtonSetting;
import rockstar.client.setting.SliderSetting;

/**
 * CRESTClient AutoBuy — ported from Zenith (1.21.4) AutoBuy to 1.21.11.
 *
 * <p>Scans FunTime auction screens, parses {@code $Цeна $xxx.xxx.xxx} lore,
 * filters fake lots (custom attribute modifiers / enchantments), and buys
 * items below a per-item configured maximum price.
 */
@ModuleInfo(name = "AutoBuy", category = ModuleCategory.AUTOBUY, internalMethod09633 = "modules.descriptions.autobuy")
public class AutoBuy extends Module {

    private static volatile AutoBuy instance;

    /** per-target-id maximum buy price (per item). */
    private final Map<String, Long> buyPrices = new HashMap<>();
    private final List<Integer> allAnarchies = new ArrayList<>();

    private final SliderSetting clickDelay;
    private final SliderSetting updateDelay;
    private final SliderSetting anarchyChangeDelay;
    private final ButtonSetting startButton;
    private final ButtonSetting stopButton;

    private boolean isBuying = false;
    private boolean waitingForPurchaseConfirm = false;
    private long purchaseStartedAt = 0L;
    private long lastClickAt = 0L;
    private long lastUpdateAt = 0L;
    private long lastAnarchyAt = 0L;
    private final Random random = new Random();

    private final EventListener<GameTickEvent> tickListener = event -> onTick();
    private final EventListener<ReceivePacketEvent> packetListener = this::onPacket;

    public AutoBuy() {
        super();
        instance = this;

        this.clickDelay = new SliderSetting(this, "Click Delay")
                .internalMethod05900(50f).internalMethod02732(500f).internalMethod08673(10f).internalMethod08074(200f);
        this.updateDelay = new SliderSetting(this, "Update Delay (ms)")
                .internalMethod05900(100f).internalMethod02732(2000f).internalMethod08673(50f).internalMethod08074(500f);
        this.anarchyChangeDelay = new SliderSetting(this, "Anarchy Change Delay (min)")
                .internalMethod05900(3f).internalMethod02732(10f).internalMethod08673(1f).internalMethod08074(5f);

        this.startButton = new ButtonSetting(this, "Начать AutoBuy").internalMethod07149(this::start);
        this.stopButton = new ButtonSetting(this, "Остановить").internalMethod07149(this::stop);

        initAllAnarchies();
    }

    public static AutoBuy getInstance() {
        return instance;
    }

    private void initAllAnarchies() {
        for (int i = 103; i <= 112; i++) allAnarchies.add(i);
        for (int i = 208; i <= 231; i++) allAnarchies.add(i);
        for (int i = 305; i <= 319; i++) allAnarchies.add(i);
        for (int i = 504; i <= 512; i++) allAnarchies.add(i);
        for (int i = 901; i <= 904; i++) allAnarchies.add(i);
    }

    // ---------------- public API (used by AutoSetup) ----------------

    public void setPriceForItem(String itemId, long price) {
        if (itemId == null) return;
        if (price <= 0L) buyPrices.remove(itemId); else buyPrices.put(itemId, price);
    }

    public long getPriceForItem(String itemId) {
        Long v = buyPrices.get(itemId);
        return v == null ? 0L : v;
    }

    public void start() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.networkHandler.sendChatCommand("ah");
        }
        isBuying = false;
        waitingForPurchaseConfirm = false;
        lastAnarchyAt = System.currentTimeMillis();
    }

    public void stop() {
        isBuying = false;
        waitingForPurchaseConfirm = false;
    }

    // ---------------- lifecycle ----------------

    @Override
    public void onEnable() {
        super.onEnable();
        start();
    }

    @Override
    public void onDisable() {
        stop();
        super.onDisable();
    }

    // ---------------- tick ----------------

    private void onTick() {
        if (!isEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null) return;

        long now = System.currentTimeMillis();

        // purchase timeout
        if (waitingForPurchaseConfirm && now - purchaseStartedAt > 7000L) {
            waitingForPurchaseConfirm = false;
            isBuying = false;
        }

        // periodic random anarchy change
        long anarchyMs = (long) (anarchyChangeDelay.internalMethod08576() * 60_000L);
        if (!isBuying && !waitingForPurchaseConfirm && !allAnarchies.isEmpty() && now - lastAnarchyAt > anarchyMs) {
            int n = allAnarchies.get(random.nextInt(allAnarchies.size()));
            mc.player.networkHandler.sendChatCommand("an" + n);
            lastAnarchyAt = now;
        }

        if (isBuying || waitingForPurchaseConfirm) return;
        if (!(mc.currentScreen instanceof HandledScreen<?> screen)) return;
        if (!isAuctionScreen(screen)) return;

        // periodic refresh
        if (now - lastUpdateAt > (long) updateDelay.internalMethod08576()) {
            refreshPage(mc, screen);
            lastUpdateAt = now;
        }

        scanSlots(mc, screen, now);
    }

    private boolean isAuctionScreen(HandledScreen<?> screen) {
        String title = screen.getTitle() == null ? "" : screen.getTitle().getString().toLowerCase(Locale.ROOT);
        return title.contains("аукцион") || title.contains("auction");
    }

    private void scanSlots(MinecraftClient mc, HandledScreen<?> screen, long now) {
        var handler = screen.getScreenHandler();
        for (int i = 0; i < handler.slots.size(); i++) {
            Slot slot = handler.getSlot(i);
            if (slot.inventory == mc.player.getInventory()) continue;
            ItemStack stack = slot.getStack();
            if (stack == null || stack.isEmpty() || isRefreshButton(stack)) continue;
            if (ItemManager.isSuspicious(stack)) continue; // anti-cheat fake-lot filter

            long totalPrice = FunTimePriceParser.parsePriceFromLore(ItemManager.loreStrings(stack));
            if (totalPrice <= 0L) continue;

            ItemManager.ItemEntry target = ItemManager.getInstance().findMatch(stack);
            if (target == null) continue;

            long maxPrice = buyPrices.getOrDefault(target.id(), 0L);
            if (maxPrice <= 0L) continue;

            int count = Math.max(1, stack.getCount());
            long pricePerItem = totalPrice / count;
            if (pricePerItem > maxPrice) continue;

            performPurchase(mc, screen, slot, now);
            break;
        }
    }

    private void performPurchase(MinecraftClient mc, HandledScreen<?> screen, Slot slot, long now) {
        if (isBuying || waitingForPurchaseConfirm) return;
        if (now - lastClickAt < (long) clickDelay.internalMethod08576()) return;

        isBuying = true;
        lastClickAt = now;
        mc.interactionManager.clickSlot(screen.getScreenHandler().syncId, slot.id, 0, SlotActionType.QUICK_MOVE, mc.player);
        waitingForPurchaseConfirm = true;
        purchaseStartedAt = now;
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

    // ---------------- packets ----------------

    private void onPacket(ReceivePacketEvent event) {
        if (!isEnabled()) return;
        if (!(event.getPacket() instanceof GameMessageS2CPacket msg)) return;
        String message = msg.content() == null ? "" : msg.content().getString();
        MinecraftClient mc = MinecraftClient.getInstance();

        if (message.contains("Вы успешно купили") || message.contains("куплен")) {
            if (waitingForPurchaseConfirm) {
                waitingForPurchaseConfirm = false;
                isBuying = false;
                mc.execute(() -> {
                    if (mc.currentScreen != null) mc.currentScreen.close();
                    if (mc.player != null && isEnabled()) mc.player.networkHandler.sendChatCommand("ah");
                });
            }
        }

        if (message.contains("не хватает") || message.contains("монет") || message.contains("Monet")) {
            if (waitingForPurchaseConfirm) {
                waitingForPurchaseConfirm = false;
                isBuying = false;
                mc.execute(() -> {
                    if (mc.player != null && isEnabled()) mc.player.networkHandler.sendChatCommand("ah");
                });
            }
        }
    }
}
