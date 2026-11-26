package rockstar.client.autobuy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

/**
 * Built-in FunTime item database. Each {@link ItemEntry} knows which Minecraft
 * item it is, its Russian display name, a keyword list, a category, and an
 * optional auction search term used by AutoSetup.
 */
public final class ItemManager {

    public enum Category {
        TALISMAN, SPHERE, BOOK, CONSUMABLE, KEY, CRUSHER
    }

    public record ItemEntry(String id, String name, Item item, Category category,
                            List<String> keywords, String searchTerm) {
        public boolean matchesItem(Item candidate) {
            return this.item == null || this.item == candidate;
        }
    }

    private static final List<ItemEntry> ENTRIES = new ArrayList<>();
    private static volatile ItemManager instance;

    private ItemManager() {
        registerDefaults();
    }

    public static ItemManager getInstance() {
        ItemManager local = instance;
        if (local == null) {
            synchronized (ItemManager.class) {
                local = instance;
                if (local == null) {
                    local = new ItemManager();
                    instance = local;
                }
            }
        }
        return local;
    }

    public List<ItemEntry> entries() {
        return Collections.unmodifiableList(ENTRIES);
    }

    public List<ItemEntry> byCategory(Category category) {
        List<ItemEntry> out = new ArrayList<>();
        for (ItemEntry e : ENTRIES) {
            if (e.category() == category) {
                out.add(e);
            }
        }
        return out;
    }

    /** Full FunTime list, used by AutoSetup to iterate every known item. */
    public List<ItemEntry> auctionList() {
        return entries();
    }

    private static void registerDefaults() {
        // ---- Talismans (totem_of_undying) ----
        talisman("talisman_krushitelya", "Крушителя", "талисман крушителя");
        talisman("talisman_karatel", "Карателя", "талисман карателя");
        talisman("talisman_razdora", "Раздора", "талисман раздора");
        talisman("talisman_tirana", "Тирана", "талисман тирана");
        talisman("talisman_yarosti", "Ярости", "талисман ярости");
        talisman("talisman_vihrya", "Вихря", "талисман вихря");
        talisman("talisman_mraka", "Мрака", "талисман мрака");
        talisman("talisman_demona", "Демона", "талисман демона");
        talisman("talisman_dyavola", "Дьявола", "талисман дьявола");
        talisman("talisman_grincha", "Гринча", "талисман гринча");
        talisman("talisman_nishevy", "Нишевый", "нишевый талисман");
        talisman("talisman_muravya", "Муравья", "талисман муравья");

        // ---- Spheres (player_head) ----
        sphere("sphere_afiny", "Афины", "сфера афины");
        sphere("sphere_titana", "Титана", "сфера титана");
        sphere("sphere_haosa", "Хаоса", "сфера хаоса");
        sphere("sphere_satira", "Сатира", "сфера сатира");
        sphere("sphere_bestii", "Бестии", "сфера бестии");
        sphere("sphere_aresa", "Ареса", "сфера ареса");
        sphere("sphere_gidry", "Гидры", "сфера гидры");
        sphere("sphere_ikary", "Икара", "сфера икара");
        sphere("sphere_eridy", "Эрида", "сфера эрида");
        sphere("sphere_tykvennaya", "Тыквенная", "сфера тыквенная");
        sphere("sphere_moroza", "Мороза", "сфера мороза");

        // ---- Books / enchantments ----
        book("book_bozhestvennoe_kasanie", "Божественное касание", "божественное касание");
        book("book_avtoplavka", "Автоплавка", "автоплавка");
        book("book_opytny", "Опытный", "опытный");
        book("book_buldozer", "Бульдозер", "бульдозер");
        book("book_pinger", "Пингер", "пингер");
        book("book_magnet", "Магнит", "магнит");
        book("book_vampirism", "Вампиризм", "вампиризм");
        book("book_prygun", "Прыгун", "прыгун");
        book("book_smertelny_udar", "Смертельный удар", "смертельный удар");
        book("book_okislenie", "Окисление", "окисление");
        book("book_detekciya", "Детекция", "детекция");

        // ---- Consumables ----
        consumable("consumable_trapped", "Трапка", Items.NETHERITE_SCRAP, "трапка");
        consumable("consumable_plast", "Пласт", Items.SLIME_BLOCK, "пласт");
        consumable("consumable_desorientaciya", "Дезориентация", Items.ENDER_PEARL, "дезориентация");
        consumable("consumable_yavnaya_pyl", "Явная пыль", Items.GLOWSTONE_DUST, "явная пыль");
        consumable("consumable_bozhya_aura", "Божья аура", Items.TOTEM_OF_UNDYING, "божья аура");
        consumable("consumable_ognenny_smerch", "Огненный смерч", Items.FIRE_CHARGE, "огненный смерч");
        consumable("consumable_snezhok_zamorozki", "Снежок заморозки", Items.SNOWBALL, "снежок заморозки");
        consumable("consumable_proklyataya_dusha", "Проклятая душа", Items.SOUL_SAND, "проклятая душа");
        consumable("consumable_beacon", "Маяк", Items.BEACON, "маяк");
        consumable("consumable_elytra", "Элитры", Items.ELYTRA, "элитры");
        consumable("consumable_netherite_ingot", "Незеритовый слиток", Items.NETHERITE_INGOT, "незеритовый слиток");
        consumable("consumable_golden_apple", "Золотое яблоко", Items.GOLDEN_APPLE, "золотое яблоко");
        consumable("consumable_xp_bottle", "Пузырёк опыта", Items.EXPERIENCE_BOTTLE, "пузырек опыта");

        // ---- Keys ----
        key("key_zloveshchy", "Зловещий ключ", "зловещий ключ");
        key("key_otmychka", "Отмычка", "отмычка");
        key("key_key_ot_kasea", "Ключ от кейса", "ключ от кейса");
        key("key_zvezda_nezera", "Звезда Незера", "звезда незера");

        // ---- Set Krushitelya ----
        crusher("crusher_helmet", "Шлем Крушителя", Items.NETHERITE_HELMET, "шлем крушителя");
        crusher("crusher_chestplate", "Нагрудник Крушителя", Items.NETHERITE_CHESTPLATE, "нагрудник крушителя");
        crusher("crusher_leggings", "Поножи Крушителя", Items.NETHERITE_LEGGINGS, "поножи крушителя");
        crusher("crusher_boots", "Ботинки Крушителя", Items.NETHERITE_BOOTS, "ботинки крушителя");
        crusher("crusher_sword", "Меч Крушителя", Items.NETHERITE_SWORD, "меч крушителя");
        crusher("crusher_pickaxe", "Кирка Крушителя", Items.NETHERITE_PICKAXE, "кирка крушителя");
        crusher("crusher_crossbow", "Арбалет Крушителя", Items.CROSSBOW, "арбалет крушителя");
        crusher("crusher_trident", "Трезубец Крушителя", Items.TRIDENT, "трезубец крушителя");
    }

    private static void talisman(String id, String name, String search) {
        ENTRIES.add(new ItemEntry(id, name, Items.TOTEM_OF_UNDYING, Category.TALISMAN, keywords(name), search));
    }

    private static void sphere(String id, String name, String search) {
        ENTRIES.add(new ItemEntry(id, name, Items.PLAYER_HEAD, Category.SPHERE, keywords(name), search));
    }

    private static void book(String id, String name, String search) {
        ENTRIES.add(new ItemEntry(id, name, Items.ENCHANTED_BOOK, Category.BOOK, keywords(name), search));
    }

    private static void consumable(String id, String name, Item item, String search) {
        ENTRIES.add(new ItemEntry(id, name, item, Category.CONSUMABLE, keywords(name), search));
    }

    private static void key(String id, String name, String search) {
        ENTRIES.add(new ItemEntry(id, name, Items.TRIAL_KEY, Category.KEY, keywords(name), search));
    }

    private static void crusher(String id, String name, Item item, String search) {
        ENTRIES.add(new ItemEntry(id, name, item, Category.CRUSHER, keywords(name), search));
    }

    private static List<String> keywords(String name, String... extra) {
        List<String> kw = new ArrayList<>();
        kw.add(name.toLowerCase(Locale.ROOT));
        kw.addAll(Arrays.asList(extra));
        return kw;
    }

    // ---------------- matching ----------------

    /** All display text for a stack (custom name + lore), formatting stripped. */
    public static String stackText(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        Text customName = stack.get(DataComponentTypes.CUSTOM_NAME);
        if (customName != null) {
            sb.append(FunTimePriceParser.clean(customName.getString())).append(' ');
        }
        sb.append(FunTimePriceParser.clean(stack.getName().getString())).append(' ');
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore != null) {
            for (Text line : lore.lines()) {
                if (line != null) {
                    sb.append(FunTimePriceParser.clean(line.getString())).append(' ');
                }
            }
        }
        return sb.toString();
    }

    /** Lore lines as plain strings (formatting stripped); never null. */
    public static List<String> loreStrings(ItemStack stack) {
        List<String> out = new ArrayList<>();
        if (stack == null) {
            return out;
        }
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore == null) {
            return out;
        }
        for (Text line : lore.lines()) {
            out.add(line == null ? "" : FunTimePriceParser.clean(line.getString()));
        }
        return out;
    }

    public boolean matches(ItemStack stack) {
        return findMatch(stack) != null;
    }

    public ItemEntry findMatch(ItemStack stack) {
        if (stack == null || stack.isEmpty() || isSuspicious(stack)) {
            return null;
        }
        String haystack = FunTimePriceParser.normalize(stackText(stack));
        Item item = stack.getItem();
        for (ItemEntry e : ENTRIES) {
            if (!e.matchesItem(item)) {
                continue;
            }
            for (String kw : e.keywords()) {
                if (!kw.isEmpty() && haystack.contains(FunTimePriceParser.normalize(kw))) {
                    return e;
                }
            }
            // fall back to normalized display name containment
            String normName = FunTimePriceParser.normalize(e.name());
            if (!normName.isEmpty() && haystack.contains(normName)) {
                return e;
            }
        }
        return null;
    }

    /**
     * Anti-cheat: reject fake lots. A real FunTime item carries no custom
     * attribute modifiers and no enchantments added by a fake-lot hider.
     */
    public static boolean isSuspicious(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return true;
        }
        net.minecraft.component.type.AttributeModifiersComponent mods =
                stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if (mods != null && !mods.modifiers().isEmpty()) {
            return true;
        }
        net.minecraft.component.type.ItemEnchantmentsComponent ench =
                stack.get(DataComponentTypes.ENCHANTMENTS);
        if (ench != null && !ench.isEmpty()) {
            return true;
        }
        net.minecraft.component.type.ItemEnchantmentsComponent stored =
                stack.get(DataComponentTypes.STORED_ENCHANTMENTS);
        if (stored != null && !stored.isEmpty()) {
            return true;
        }
        return false;
    }

    /** Custom name or null. */
    public static String customName(ItemStack stack) {
        if (stack == null) {
            return null;
        }
        Text name = stack.get(DataComponentTypes.CUSTOM_NAME);
        return name == null ? null : FunTimePriceParser.clean(name.getString());
    }
}
