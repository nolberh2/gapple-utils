package com.gappleclub.gappleutils.color;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Paleta de marca de GappleClub y las operaciones de color que Adventure no trae.
 *
 * <p>Deliberadamente corta. Para hex a color, color a hex o el color vanilla mas cercano,
 * usa Adventure directamente ({@code TextColor.fromHexString}, {@code color.asHexString()},
 * {@code NamedTextColor.nearestTo}); envolverlo aqui solo añadiria una capa que mantener.</p>
 *
 * <p>Lo que si vive aqui es la paleta —una unica fuente de verdad, para no buscar
 * {@code #F3C14A} a mano por ocho plugins el dia que cambie— y los gradientes que
 * MiniMessage no sabe hacer.</p>
 */
public final class ColorUtils {

    // --- Marca. Identicos a los de la web (gappleweb2/src/Landing.css). ---

    public static final TextColor GOLD = TextColor.color(0xF3C14A);
    public static final TextColor GOLD_SOFT = TextColor.color(0xE9C877);
    public static final TextColor GOLD_DEEP = TextColor.color(0xC9962F);
    public static final TextColor TEXT = TextColor.color(0xF4F5F7);
    public static final TextColor MUTED = TextColor.color(0x9AA3B2);
    public static final TextColor MUTED_2 = TextColor.color(0x6B7482);

    // --- Semanticos. No existen en la web; armonizados con el dorado. ---

    public static final TextColor SUCCESS = TextColor.color(0x5FD98A);
    public static final TextColor ERROR = TextColor.color(0xE5534B);
    public static final TextColor WARNING = TextColor.color(0xE8A33D);
    public static final TextColor INFO = TextColor.color(0x6BA8E5);

    private static final Map<String, TextColor> BY_NAME = new LinkedHashMap<>();

    static {
        BY_NAME.put("gold", GOLD);
        BY_NAME.put("gold_soft", GOLD_SOFT);
        BY_NAME.put("gold_deep", GOLD_DEEP);
        BY_NAME.put("text", TEXT);
        BY_NAME.put("muted", MUTED);
        BY_NAME.put("muted_2", MUTED_2);
        BY_NAME.put("success", SUCCESS);
        BY_NAME.put("error", ERROR);
        BY_NAME.put("warning", WARNING);
        BY_NAME.put("info", INFO);
    }

    private ColorUtils() {
    }

    /**
     * Busca un color de la paleta por nombre, para que en un YAML se pueda escribir
     * {@code color: gold} en vez del hex.
     */
    public static Optional<TextColor> byName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_NAME.get(name.toLowerCase(Locale.ROOT).trim()));
    }

    /** Nombres disponibles en {@link #byName(String)}. */
    public static List<String> names() {
        return new ArrayList<>(BY_NAME.keySet());
    }

    // ------------------------------------------------------------------
    // Gradientes
    // ------------------------------------------------------------------

    /**
     * Gradiente caracter a caracter sobre texto plano.
     *
     * <p>Para un degradado simple en una linea, el tag nativo {@code <gradient:#a:#b>}
     * de MiniMessage alcanza. Esto existe por la interpolacion en HSV, que conserva la
     * saturacion: en RGB, el punto medio entre dos colores lejanos se va a un gris sucio.</p>
     *
     * @param text  texto plano (sin tags: cada caracter recibe su propio color)
     * @param stops dos o mas colores por los que pasa el degradado
     */
    public static Component gradient(String text, TextColor... stops) {
        return gradient(text, 0f, stops);
    }

    /**
     * Gradiente con desfase, para animarlo.
     *
     * <p>La libreria solo produce el componente para una fase dada; programar el redibujado
     * es cosa del plugin. Meter aqui una tarea repetitiva romperia la regla de una unica
     * timing wheel global.</p>
     *
     * @param phase desplazamiento del degradado, de 0 a 1 (se envuelve)
     */
    public static Component gradient(String text, float phase, TextColor... stops) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        if (stops == null || stops.length == 0) {
            return Component.text(text);
        }
        if (stops.length == 1) {
            return Component.text(text, stops[0]);
        }

        TextComponent.Builder builder = Component.text();
        int len = text.length();
        for (int i = 0; i < len; i++) {
            float t = (len == 1) ? 0f : (float) i / (len - 1);
            // Sin fase no se envuelve: t=1 debe quedarse en 1 y dar el ultimo stop exacto,
            // no dar la vuelta al degradado y repetir el primero en el ultimo caracter.
            builder.append(Component.text(text.charAt(i), colorAt(phase == 0f ? t : shift(t, phase), stops)));
        }
        return builder.build();
    }

    /**
     * Reparte un unico degradado continuo a lo largo de varias lineas.
     *
     * <p>Esto es lo que el tag nativo no puede hacer: aplicado por linea, cada una reinicia
     * el degradado y un lore de varias lineas queda a rayas.</p>
     */
    public static List<Component> gradientLines(List<String> lines, TextColor... stops) {
        List<Component> out = new ArrayList<>();
        if (lines == null || lines.isEmpty()) {
            return out;
        }

        int total = 0;
        for (String line : lines) {
            total += (line == null ? 0 : line.length());
        }
        if (total == 0 || stops == null || stops.length == 0) {
            for (String line : lines) {
                out.add(Component.text(line == null ? "" : line));
            }
            return out;
        }

        int index = 0;
        for (String line : lines) {
            if (line == null || line.isEmpty()) {
                out.add(Component.empty());
                continue;
            }
            TextComponent.Builder builder = Component.text();
            for (int i = 0; i < line.length(); i++) {
                float t = (total == 1) ? 0f : (float) index / (total - 1);
                builder.append(Component.text(line.charAt(i), colorAt(t, stops)));
                index++;
            }
            out.add(builder.build());
        }
        return out;
    }

    // ------------------------------------------------------------------
    // Derivacion de colores
    // ------------------------------------------------------------------

    /** Aclara el color hacia el blanco. {@code amount} de 0 a 1. */
    public static TextColor lighten(TextColor color, float amount) {
        return blend(color, TextColor.color(0xFFFFFF), clamp01(amount));
    }

    /** Oscurece el color hacia el negro. {@code amount} de 0 a 1. */
    public static TextColor darken(TextColor color, float amount) {
        return blend(color, TextColor.color(0x000000), clamp01(amount));
    }

    /**
     * Mezcla dos colores en RGB.
     *
     * @param ratio 0 devuelve {@code a}, 1 devuelve {@code b}
     */
    public static TextColor blend(TextColor a, TextColor b, float ratio) {
        float t = clamp01(ratio);
        return TextColor.color(
                Math.round(a.red() + (b.red() - a.red()) * t),
                Math.round(a.green() + (b.green() - a.green()) * t),
                Math.round(a.blue() + (b.blue() - a.blue()) * t)
        );
    }

    // ------------------------------------------------------------------
    // Interno
    // ------------------------------------------------------------------

    /** Color en la posicion {@code t} (0 a 1) del degradado definido por los stops. */
    private static TextColor colorAt(float t, TextColor[] stops) {
        float scaled = t * (stops.length - 1);
        int index = Math.min((int) scaled, stops.length - 2);
        return lerpHsv(stops[index], stops[index + 1], scaled - index);
    }

    private static float shift(float t, float phase) {
        float shifted = (t + phase) % 1f;
        return shifted < 0 ? shifted + 1f : shifted;
    }

    /**
     * Interpola en HSV en vez de RGB. El tono toma el camino corto de la rueda, que es
     * lo que uno espera ver: dorado a rojo pasa por naranja, no por gris.
     */
    private static TextColor lerpHsv(TextColor from, TextColor to, float t) {
        float[] a = toHsv(from);
        float[] b = toHsv(to);

        float dh = b[0] - a[0];
        if (dh > 0.5f) dh -= 1f;
        if (dh < -0.5f) dh += 1f;

        float h = a[0] + dh * t;
        if (h < 0) h += 1f;
        if (h > 1) h -= 1f;

        float s = a[1] + (b[1] - a[1]) * t;
        float v = a[2] + (b[2] - a[2]) * t;
        return fromHsv(h, s, v);
    }

    /** RGB a HSV, con los tres componentes normalizados de 0 a 1. Sin java.awt. */
    private static float[] toHsv(TextColor color) {
        float r = color.red() / 255f;
        float g = color.green() / 255f;
        float b = color.blue() / 255f;

        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;

        float h = 0f;
        if (delta > 0f) {
            if (max == r) {
                h = ((g - b) / delta) % 6f;
            } else if (max == g) {
                h = ((b - r) / delta) + 2f;
            } else {
                h = ((r - g) / delta) + 4f;
            }
            h /= 6f;
            if (h < 0f) h += 1f;
        }
        return new float[]{h, max == 0f ? 0f : delta / max, max};
    }

    /** HSV a RGB, inversa de {@link #toHsv(TextColor)}. */
    private static TextColor fromHsv(float h, float s, float v) {
        float c = v * s;
        float x = c * (1f - Math.abs(((h * 6f) % 2f) - 1f));
        float m = v - c;

        float r;
        float g;
        float b;
        int sector = (int) (h * 6f) % 6;
        switch (sector) {
            case 0 -> { r = c; g = x; b = 0f; }
            case 1 -> { r = x; g = c; b = 0f; }
            case 2 -> { r = 0f; g = c; b = x; }
            case 3 -> { r = 0f; g = x; b = c; }
            case 4 -> { r = x; g = 0f; b = c; }
            default -> { r = c; g = 0f; b = x; }
        }
        return TextColor.color(
                Math.round((r + m) * 255f),
                Math.round((g + m) * 255f),
                Math.round((b + m) * 255f)
        );
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
