package crestclient.autobuy;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Parser for FunTime auction price lines.
 *
 * <p>FunTime renders prices as {@code $Цeна $xxx.xxx.xxx}. Section-sign
 * formatting codes ({@code §}) are stripped, matching is case-insensitive and
 * tolerant of Cyrillic/Latin homoglyphs for the {@code Ц/C} and {@code е/e}
 * letters in the keyword. Thousand separators ({@code .}, {@code ,}, spaces)
 * inside the number are removed before parsing to {@code long}.
 */
public final class FunTimePriceParser {

    // keyword: optional leading $, then ц/c + e/е + н/n + а/a, optional ending,
    // optional colon/dash, then a $ before the number.
    private static final Pattern PRICE_PATTERN = Pattern.compile(
            "(?:\\$\\s*)?[cсц][eе][нn][аa][ыи]?\\s*[:\\-]?\\s*\\$\\s*(\\d[\\d.,\\s]*\\d|\\d)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS);

    private FunTimePriceParser() {
    }

    /** Strip section-sign formatting; null-safe. */
    public static String clean(String raw) {
        if (raw == null) {
            return "";
        }
        String stripped = Formatting.strip(raw);
        return stripped == null ? "" : stripped;
    }

    /** Strip formatting + normalize case + unify Cyrillic/Latin homoglyphs. */
    public static String normalize(String raw) {
        String s = clean(raw).toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case 'ё' -> sb.append('е');
                case 'й' -> sb.append('и');
                // Latin/Cyrillic lookalikes collapsed to Latin
                case 'а' -> sb.append('a');
                case 'е' -> sb.append('e');
                case 'о' -> sb.append('o');
                case 'с' -> sb.append('c');
                case 'р' -> sb.append('p');
                case 'х' -> sb.append('x');
                case 'у' -> sb.append('y');
                case 'к' -> sb.append('k');
                case 'н' -> sb.append('n');
                // Cyrillic ц -> latin c (keyword "цена" -> "cena")
                case 'ц' -> sb.append('c');
                default -> sb.append(ch);
            }
        }
        return sb.toString();
    }

    /**
     * Parse a single price string to {@code long}. Returns 0 when no price is
     * found or the number cannot be parsed.
     */
    public static long parsePrice(String raw) {
        String clean = clean(raw);
        if (clean.isEmpty()) {
            return 0L;
        }
        Matcher m = PRICE_PATTERN.matcher(clean);
        if (m.find()) {
            return toLong(m.group(1));
        }
        return 0L;
    }

    /** Scan lore lines for the first FunTime price. */
    public static long parsePriceFromLore(List<String> lore) {
        if (lore == null || lore.isEmpty()) {
            return 0L;
        }
        for (String line : lore) {
            long price = parsePrice(line);
            if (price > 0L) {
                return price;
            }
        }
        return 0L;
    }

    /** Scan lore {@link Text} components for the first FunTime price. */
    public static long parsePriceFromLoreTexts(List<Text> lore) {
        if (lore == null || lore.isEmpty()) {
            return 0L;
        }
        for (Text line : lore) {
            long price = parsePrice(line == null ? null : line.getString());
            if (price > 0L) {
                return price;
            }
        }
        return 0L;
    }

    /** Remove everything except digits, then parse as long. */
    public static long toLong(String number) {
        if (number == null) {
            return 0L;
        }
        StringBuilder digits = new StringBuilder(number.length());
        for (int i = 0; i < number.length(); i++) {
            char c = number.charAt(i);
            if (c >= '0' && c <= '9') {
                digits.append(c);
            }
        }
        if (digits.length() == 0) {
            return 0L;
        }
        try {
            return Long.parseLong(digits.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    public static String formatPrice(long p) {
        if (p >= 1_000_000L) {
            return String.format(Locale.ROOT, "%.1fM", p / 1_000_000.0);
        }
        if (p >= 1_000L) {
            return String.format(Locale.ROOT, "%.1fK", p / 1_000.0);
        }
        return Long.toString(p);
    }
}
