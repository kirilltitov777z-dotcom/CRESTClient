package crestclient.autobuy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

import crestclient.module.Module;

/** CRESTClient AutoBuy (own implementation, FunTime). */
public class AutoBuy extends Module {

    private final Map<String, Long> buyPrices = new HashMap<>();
    private final List<Integer> allAnarchies = new ArrayList<>();

    public long clickDelayMs = 200L;
    public long updateDelayMs = 500L;
    public long anarchyChangeDelayMs = 5L * 60_000L;

    private boolean isBuying = false;
    private boolean waitingForPurchaseConfirm = false;
    private long purchaseStartedAt = 0L;
    private long lastClickAt = 0L;
    private long lastUpdateAt = 0L;
    private long lastAnarchyAt = 0L;
    private final Random random = new Random();

    public AutoBuy() {
        super("AutoBuy", "FunTime auction AutoBuy + anti-cheat");
        initAllAnarchies();

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!isEnabled() || message == null) return;
            String s = message.getString();
            MinecraftClient mc = MinecraftClient.getInstance();
            if (s.contains("Вы успешно купили") || s.contains("куплен")) {
                if (waitingForPurchaseConfirm) {
                    waitingForPurchaseConfirm = false;
                    isBuying = false;
                    mc.execute(() -> {
                        if (mc.currentScreen != null) mc.currentScreen.close();
                        if (mc.player != null && isEnabled()) mc.player.networkHandler.sendChatCommand("ah");
                    });
                }
            }
            if (s.contains("не хватает") || s.contains("монет") || s.contains("Monet")) {
                if (waitingForPurchaseConfirm) {
                    waitingForPurchaseConfirm = false;
                    isBuying = false;
                    mc.execute(() -> { if (mc.player != null && isEnabled()) mc.player.networkHandler.sendChatCommand("ah"); });
                }
            }
        });
    }

    private void initAllAnarchies() {
        for (int i = 103; i <= 112; i++) allAnarchies.add(i);
        for (int i = 208; i <= 231; i++) allAnarchies.add(i);
        for (int i = 305; i <= 319; i++) allAnarchies.add(i);
        for (int i = 504; i <= 512; i++) allAnarchies.add(i);
        for (int i = 901; i <= 904; i++) allAnarchies.add(i);
    }

    public void setPriceForItem(String itemId, long price) {
        if (itemId == null) return;
        if (price <= 0L) buyPrices.remove(itemId); else buyPrices.put(itemId, price);
    }

    public long getPriceForItem(String itemId) { Long v = buyPrices.get(itemId); return v == null ? 0L : v; }

    @Override
    protected void onEnable() { start(); }

    @Override
    protected void onDisable() { stop(); }

    public void start() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) mc.player.networkHandler.sendChatCommand("ah");
        isBuying = false;
        waitingForPurchaseConfirm = false;
        lastAnarchyAt = System.currentTimeMillis();
    }

    public void stop() { isBuying = false; waitingForPurchaseConfirm = false; }

    @Override
    public void tick(MinecraftClient mc) {
        if (mc.player == null || mc.interactionManager == null) return;
        long now = System.currentTimeMillis();

        if (waitingForPurchaseConfirm && now - purchaseStartedAt > 7000L) { waitingForPurchaseConfirm = false; isBuying = false; }

        if (!isBuying && !waitingForPurchaseConfirm && !allAnarchies.isEmpty() && now - lastAnarchyAt > anarchyChangeDelayMs) {
            int n = allAnarchies.get(random.nextInt(allAnarchies.size()));
            mc.player.networkHandler.sendChatCommand("an" + n);
            lastAnarchyAt = now;
        }

        if (isBuying || waitingForPurchaseConfirm) return;
        if (!(mc.currentScreen instanceof HandledScreen<?> screen)) return;
        if (!isAuctionScreen(screen)) return;

        if (now - lastUpdateAt > updateDelayMs) { refreshPage(mc, screen); lastUpdateAt = now; }
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
            if (ItemManager.isSuspicious(stack)) continue;

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
        if (now - lastClickAt < clickDelayMs) return;
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
            if (isRefreshButton(stack)) { mc.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.PICKUP, mc.player); break; }
        }
    }

    private boolean isRefreshButton(ItemStack stack) {
        String name = FunTimePriceParser.clean(stack.getName().getString()).toLowerCase(Locale.ROOT);
        return name.contains("обновить") || name.contains("refresh");
    }
}
