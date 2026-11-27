package crestclient.autobuy;

import java.util.List;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

import crestclient.module.Module;
import crestclient.module.ModuleManager;

/** CRESTClient AutoSetup (own implementation): finds cheapest price and sets AutoBuy target. */
public class AutoSetup extends Module {

    public long discountPercent = 50L;
    public long searchDurationMs = 10_000L;
    public long pageUpdateDelayMs = 2000L;

    private boolean isSettingUp = false;
    private boolean waitingForAuction = false;
    private boolean retrying = false;
    private int currentItemIndex = 0;
    private long bestPricePerItem = Long.MAX_VALUE;
    private String currentItemId = "";

    private long pageTimerAt = 0L;
    private long refreshTimerAt = 0L;
    private long retryTimerAt = 0L;

    public AutoSetup() {
        super("AutoSetup", "Scan auction and auto-set AutoBuy prices");
    }

    @Override
    protected void onDisable() { stop(); }

    public void startSetup() {
        if (ModuleManager.getInstance().get(AutoBuy.class) == null) return;
        isSettingUp = true;
        currentItemIndex = 0;
        retrying = false;
        waitingForAuction = false;
        searchNextItem();
    }

    public void stop() { isSettingUp = false; waitingForAuction = false; retrying = false; }

    private List<ItemManager.ItemEntry> list() { return ItemManager.getInstance().auctionList(); }

    @Override
    public void tick(MinecraftClient mc) {
        if (!isSettingUp) return;
        if (mc.player == null) return;
        long now = System.currentTimeMillis();

        if (retrying && now - retryTimerAt > 2000L) { retrying = false; searchCurrentItem(mc); }

        if (mc.currentScreen instanceof HandledScreen<?> screen) {
            if (waitingForAuction) {
                waitingForAuction = false;
                retrying = false;
                bestPricePerItem = Long.MAX_VALUE;
                pageTimerAt = now;
                refreshTimerAt = now;
            }
            if (!waitingForAuction && !retrying) {
                if (now - pageTimerAt < searchDurationMs) {
                    scanPrices(mc, screen);
                    if (now - refreshTimerAt > pageUpdateDelayMs) { refreshPage(mc, screen); refreshTimerAt = now; }
                } else {
                    finishCurrentItem(mc);
                }
            }
        } else {
            if (!waitingForAuction && !retrying && currentItemIndex < list().size()) { retrying = true; retryTimerAt = now; }
        }
    }

    private void searchNextItem() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (currentItemIndex >= list().size()) { finishSetup(mc); return; }
        searchCurrentItem(mc);
    }

    private void searchCurrentItem(MinecraftClient mc) {
        List<ItemManager.ItemEntry> list = list();
        if (currentItemIndex >= list.size()) { finishSetup(mc); return; }
        ItemManager.ItemEntry data = list.get(currentItemIndex);
        currentItemId = data.id();
        String term = data.searchTerm() == null ? data.name() : data.searchTerm();
        if (mc.player != null) mc.player.networkHandler.sendChatCommand("ah search " + term);
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
            if (ItemManager.isSuspicious(stack)) continue;
            long totalPrice = FunTimePriceParser.parsePriceFromLore(ItemManager.loreStrings(stack));
            if (totalPrice <= 0L) continue;
            int count = Math.max(1, stack.getCount());
            long pricePerItem = totalPrice / count;
            if (pricePerItem > 0L && pricePerItem < bestPricePerItem) bestPricePerItem = pricePerItem;
        }
    }

    private void refreshPage(MinecraftClient mc, HandledScreen<?> screen) {
        var handler = screen.getScreenHandler();
        for (Slot slot : handler.slots) {
            ItemStack stack = slot.getStack();
            if (stack == null || stack.isEmpty()) continue;
            String name = FunTimePriceParser.clean(stack.getName().getString()).toLowerCase();
            if (name.contains("обновить") || name.contains("refresh")) { mc.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.PICKUP, mc.player); break; }
        }
    }

    private void finishCurrentItem(MinecraftClient mc) {
        AutoBuy autoBuy = ModuleManager.getInstance().get(AutoBuy.class);
        if (bestPricePerItem != Long.MAX_VALUE && bestPricePerItem > 0L && autoBuy != null) {
            long discounted = (long) (bestPricePerItem * (1.0 - discountPercent / 100.0));
            if (discounted < 0L) discounted = 0L;
            autoBuy.setPriceForItem(currentItemId, discounted);
        }
        if (mc.currentScreen != null) mc.currentScreen.close();
        currentItemIndex++;
        if (currentItemIndex < list().size()) searchNextItem(); else finishSetup(mc);
    }

    private void finishSetup(MinecraftClient mc) {
        isSettingUp = false;
        waitingForAuction = false;
        retrying = false;
        if (mc.currentScreen != null) mc.currentScreen.close();
    }
}
