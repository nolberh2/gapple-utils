package com.gappleclub.gappleutils.text;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Formateo de texto centralizado: MiniMessage como estandar, con codigos legacy
 * ({@code &c}) aceptados en el mismo string y PlaceholderAPI resuelto de forma opcional.
 *
 * <p>Todo es estatico y sin estado mutable. Las instancias de los serializadores son
 * inmutables y thread-safe, por eso se crean una sola vez.</p>
 */
public final class TextUtils {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    /** Serializador legacy con '&', hex moderno y el formato hex de BungeeCord. */
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    /** Igual pero con '§', que es lo que espera la API String de Bukkit. */
    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.builder()
            .character('§')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    /** Mitad del ancho util del chat, en pixeles. Es el estandar de facto para centrar. */
    private static final int CHAT_WIDTH_PX = 154;

    /** Ancho del espacio, ya con su pixel de separacion. */
    private static final int SPACE_PX = 4;

    /** Codigos legacy de color y formato, en el orden de los digitos hex. */
    private static final String[] LEGACY_TAGS = {
            "<black>", "<dark_blue>", "<dark_green>", "<dark_aqua>",
            "<dark_red>", "<dark_purple>", "<gold>", "<gray>",
            "<dark_gray>", "<blue>", "<green>", "<aqua>",
            "<red>", "<light_purple>", "<yellow>", "<white>"
    };

    private static final String RESET = "<reset>";

    /** Racha de codigos legacy al final del string, sin texto detras. */
    private static final Pattern TRAILING_CODES =
            Pattern.compile("((?:[&§](?:[0-9a-fk-orA-FK-OR]|#[0-9a-fA-F]{6}|[xX](?:[&§][0-9a-fA-F]){6}))+)$");

    private static final Pattern PLACEHOLDER = Pattern.compile("%([^%\\s]+)%");
    private static final Pattern MINI_TAG = Pattern.compile("<[#/a-zA-Z][^<>]*>");
    private static final Pattern LEGACY_CODE = Pattern.compile("[&§]([0-9a-fk-orA-FK-OR]|#[0-9a-fA-F]{6})");

    /**
     * Estado de PlaceholderAPI. Se resuelve una sola vez, en el primer uso: comprobarlo
     * en cada llamada seria un lookup por mensaje, y exigir un init() explicito es algo
     * que se olvida. {@code null} = todavia sin comprobar.
     */
    private static volatile Boolean papiPresent;

    private TextUtils() {
    }

    // ------------------------------------------------------------------
    // Parseo
    // ------------------------------------------------------------------

    /**
     * Parsea un string aceptando MiniMessage y legacy mezclados.
     *
     * @param input texto crudo; {@code null} devuelve un componente vacio
     */
    public static Component parse(String input) {
        return parse(input, (Player) null);
    }

    /** Igual que {@link #parse(String)}, con placeholders propios de MiniMessage. */
    public static Component parse(String input, TagResolver... resolvers) {
        return parse(input, null, resolvers);
    }

    /**
     * Parsea resolviendo antes los placeholders de PlaceholderAPI.
     *
     * <p>Si PlaceholderAPI no esta instalado, los {@code %...%} se dejan tal cual en vez
     * de fallar.</p>
     *
     * @param player contexto para los placeholders; {@code null} los omite
     */
    public static Component parse(String input, Player player) {
        return parse(input, player, new TagResolver[0]);
    }

    /** Parseo completo: PlaceholderAPI + legacy + MiniMessage + resolvers propios. */
    public static Component parse(String input, Player player, TagResolver... resolvers) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        String text = input;
        if (player != null) {
            text = applyPlaceholders(text, player);
        }
        text = legacyToMini(text);
        return parseMini(text, resolvers);
    }

    /**
     * Parsea como MiniMessage estricto, sin convertir codigos legacy.
     *
     * <p>Ante un error de sintaxis degrada al texto plano en vez de propagar: un mensaje
     * mal escrito en un YAML no debe tumbar el comando que lo muestra.</p>
     */
    public static Component parseMini(String input, TagResolver... resolvers) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        try {
            return resolvers.length == 0
                    ? MINI.deserialize(input)
                    : MINI.deserialize(input, resolvers);
        } catch (RuntimeException e) {
            Bukkit.getLogger().warning("[GappleUtils] MiniMessage invalido: " + input + " (" + e.getMessage() + ")");
            return Component.text(strip(input));
        }
    }

    /** Parsea como legacy estricto, sin interpretar tags de MiniMessage. */
    public static Component parseLegacy(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        return parseMini(legacyToMini(MINI.escapeTags(input)));
    }

    /** Parsea una lista completa, util para lores de items. */
    public static List<Component> parseList(List<String> input, Player player, TagResolver... resolvers) {
        if (input == null || input.isEmpty()) {
            return new ArrayList<>();
        }
        List<Component> out = new ArrayList<>(input.size());
        for (String line : input) {
            out.add(parse(line, player, resolvers));
        }
        return out;
    }

    // ------------------------------------------------------------------
    // Conversion
    // ------------------------------------------------------------------

    /**
     * Convierte codigos legacy a tags de MiniMessage, dejando intactos los tags que ya
     * hubiera en el string.
     *
     * <p>Soporta {@code &c}, {@code §c}, {@code &#RRGGBB} y el formato de BungeeCord
     * {@code &x&f&f&0&0&0&0}.</p>
     */
    public static String legacyToMini(String legacy) {
        if (legacy == null || legacy.isEmpty()) {
            return legacy;
        }
        // Guard barato: la inmensa mayoria de los strings no llevan codigos legacy.
        if (legacy.indexOf('&') < 0 && legacy.indexOf('§') < 0) {
            return legacy;
        }

        StringBuilder out = new StringBuilder(legacy.length() + 16);
        int i = 0;
        int len = legacy.length();

        while (i < len) {
            char c = legacy.charAt(i);
            if ((c != '&' && c != '§') || i + 1 >= len) {
                out.append(c);
                i++;
                continue;
            }

            char code = legacy.charAt(i + 1);

            // Formato BungeeCord: &x&f&f&0&0&0&0
            if ((code == 'x' || code == 'X') && i + 13 < len) {
                String hex = readXHex(legacy, i);
                if (hex != null) {
                    out.append(RESET).append('<').append('#').append(hex).append('>');
                    i += 14;
                    continue;
                }
            }

            // Hex directo: &#RRGGBB
            if (code == '#' && i + 7 < len && isHex(legacy, i + 2, 6)) {
                out.append(RESET).append('<').append(legacy, i + 1, i + 8).append('>');
                i += 8;
                continue;
            }

            String tag = tagFor(code);
            if (tag != null) {
                // Semantica legacy: un codigo de COLOR resetea tambien el formato activo.
                // Sin esto, "&4&lNombre &4texto" dejaria "texto" en negrita, porque en
                // Adventure el color no arrastra el formato. Los codigos de formato
                // (k-o) no resetean nada.
                if (Character.digit(code, 16) >= 0) {
                    out.append(RESET);
                }
                out.append(tag);
                i += 2;
                continue;
            }

            out.append(c);
            i++;
        }
        return out.toString();
    }

    /**
     * Convierte tags de MiniMessage a codigos legacy.
     *
     * <p>Con perdida: los gradientes se aproximan al color mas cercano y los eventos de
     * click/hover se pierden, porque legacy no sabe representarlos.</p>
     */
    public static String miniToLegacy(String mini) {
        return toLegacy(parse(mini));
    }

    /** Serializa un componente a codigos legacy con {@code &}. */
    public static String toLegacy(Component component) {
        return component == null ? "" : LEGACY.serialize(component);
    }

    /**
     * Puente para codigo que todavia trabaja con {@code String} en vez de {@link Component}.
     *
     * <p>Acepta MiniMessage y legacy mezclados y devuelve un string con codigos {@code §},
     * listo para {@code player.sendMessage(String)} o para un lore de item a la vieja usanza.
     * Permite migrar un plugin gradualmente: se cambia el traductor de color sin tocar las
     * firmas de los metodos que lo usan.</p>
     *
     * <p>Con perdida frente a {@link #parse(String)}: los gradientes se aproximan al color
     * mas cercano y los eventos de click/hover se pierden, porque legacy no los representa.
     * Para texto nuevo, usa {@link #parse(String)} y trabaja con componentes.</p>
     */
    public static String colorize(String input) {
        return colorize(input, null);
    }

    /** Igual que {@link #colorize(String)} pero resolviendo antes PlaceholderAPI. */
    public static String colorize(String input, Player player) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        // Los codigos sueltos al final del string no sobreviven al viaje por Component:
        // sin texto detras no hay nada que colorear y el serializador los descarta. Son
        // justo el caso de un "prefix" que termina en "&7" para tenir lo que se concatene
        // despues, asi que se apartan y se vuelven a pegar al final.
        Matcher trailing = TRAILING_CODES.matcher(input);
        String body = input;
        String suffix = "";
        if (trailing.find()) {
            suffix = codesToSection(trailing.group(1));
            body = input.substring(0, trailing.start());
        }
        if (body.isEmpty()) {
            return suffix;
        }
        return toLegacySection(parse(body, player)) + suffix;
    }

    /**
     * Sustituye marcadores en una plantilla, de forma literal.
     *
     * <p>Existe por un fallo concreto y repetido: {@code String.replaceAll} interpreta el
     * <b>reemplazo</b> como expresion regular, asi que la barra invertida y el dolar son
     * especiales. Si el valor viene de lo que escribe un jugador, un nombre con {@code $1}
     * corrompe el mensaje y uno acabado en {@code \} lanza
     * {@code IllegalArgumentException}. Encadenar {@code replace} es seguro pero recorre
     * el texto una vez por marcador y es facil equivocarse de metodo.</p>
     *
     * <p>Los pares son clave y valor, con la clave tal cual aparece en la plantilla, para
     * no imponer una convencion de delimitadores:</p>
     *
     * <pre>{@code
     * TextUtils.fill("%prefix% %player% gano %amount%", "%prefix%", prefix,
     *                "%player%", nombre, "%amount%", String.valueOf(cantidad));
     * }</pre>
     *
     * <p>Un valor {@code null} se sustituye por texto vacio. Para texto que ya trabaja con
     * {@link Component}, es preferible {@link #parse(String, TagResolver...)} con
     * {@code Placeholder.unparsed}, que ademas impide inyectar formato.</p>
     *
     * @throws IllegalArgumentException si el numero de argumentos es impar
     */
    public static String fill(String template, String... pairs) {
        if (template == null || template.isEmpty()) {
            return "";
        }
        if (pairs == null || pairs.length == 0) {
            return template;
        }
        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException("fill() espera pares clave/valor, recibio " + pairs.length + " argumentos");
        }

        StringBuilder out = new StringBuilder(template);
        for (int i = 0; i < pairs.length; i += 2) {
            String key = pairs[i];
            if (key == null || key.isEmpty()) {
                continue;
            }
            String value = pairs[i + 1] == null ? "" : pairs[i + 1];

            // Se avanza por el indice del reemplazo para no volver a mirar lo ya sustituido:
            // si no, un valor que contenga su propia clave entraria en bucle.
            int from = 0;
            int at;
            while ((at = out.indexOf(key, from)) >= 0) {
                out.replace(at, at + key.length(), value);
                from = at + value.length();
            }
        }
        return out.toString();
    }

    /** Serializa un componente a codigos legacy con {@code §}, el formato que espera Bukkit. */
    public static String toLegacySection(Component component) {
        return component == null ? "" : LEGACY_SECTION.serialize(component);
    }

    /** Serializa un componente a MiniMessage. */
    public static String toMini(Component component) {
        return component == null ? "" : MINI.serialize(component);
    }

    // ------------------------------------------------------------------
    // Deteccion
    // ------------------------------------------------------------------

    /** Detecta que formatos usa el string. Informativo: {@link #parse} maneja todos. */
    public static TextFormat detect(String input) {
        boolean legacy = hasLegacy(input);
        boolean mini = hasMiniTags(input);
        if (legacy && mini) return TextFormat.MIXED;
        if (legacy) return TextFormat.LEGACY;
        if (mini) return TextFormat.MINIMESSAGE;
        return TextFormat.PLAIN;
    }

    /** {@code true} si hay al menos un codigo legacy de color o formato. */
    public static boolean hasLegacy(String input) {
        return input != null && LEGACY_CODE.matcher(input).find();
    }

    /** {@code true} si hay al menos algo con forma de tag de MiniMessage. */
    public static boolean hasMiniTags(String input) {
        return input != null && MINI_TAG.matcher(input).find();
    }

    // ------------------------------------------------------------------
    // Strip
    // ------------------------------------------------------------------

    /** Quita color y formato de ambos formatos y devuelve texto plano. */
    public static String strip(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        return PLAIN.serialize(parseMini(legacyToMini(input)));
    }

    /** Texto plano de un componente ya construido. */
    public static String strip(Component component) {
        return component == null ? "" : PLAIN.serialize(component);
    }

    // ------------------------------------------------------------------
    // Helpers de chat
    // ------------------------------------------------------------------

    /** Centra el texto en el chat usando los anchos reales de la fuente. */
    public static Component center(String input) {
        return center(input, CHAT_WIDTH_PX);
    }

    /**
     * Centra el texto respecto al ancho indicado.
     *
     * <p>El ancho se mide sobre el texto ya parseado y sin color: medir el string crudo
     * contaria los tags como caracteres y el resultado saldria descentrado.</p>
     */
    public static Component center(String input, int widthPx) {
        Component parsed = parse(input);
        int textPx = widthOf(strip(parsed), containsBold(input));
        int padding = widthPx - (textPx / 2);
        if (padding <= 0) {
            return parsed;
        }
        return Component.text(" ".repeat(padding / SPACE_PX)).append(parsed);
    }

    /** Separador por defecto: linea tachada gris a lo ancho del chat. */
    public static Component separator() {
        return separator("<dark_gray>", '-');
    }

    /**
     * Separador que rellena el ancho del chat con el caracter dado.
     *
     * @param miniColor prefijo MiniMessage aplicado a la linea, p.ej. {@code "<gold>"}
     */
    public static Component separator(String miniColor, char character) {
        boolean bold = miniColor != null && (miniColor.contains("<b>") || miniColor.contains("<bold>"));
        int charPx = FontWidth.of(character, bold);
        int count = (CHAT_WIDTH_PX * 2) / Math.max(1, charPx);
        return parse((miniColor == null ? "" : miniColor) + String.valueOf(character).repeat(count));
    }

    /** Separador + titulo centrado + separador, como cabecera de un menu o un /help. */
    public static Component header(String title, String miniColor) {
        return separator()
                .append(Component.newline())
                .append(center((miniColor == null ? "" : miniColor) + title))
                .append(Component.newline())
                .append(separator());
    }

    // ------------------------------------------------------------------
    // Envio
    // ------------------------------------------------------------------

    /** Envia el mensaje ya parseado. {@link Audience} cubre jugador, consola y broadcast. */
    public static void send(Audience target, String input) {
        if (target != null) {
            target.sendMessage(parse(input));
        }
    }

    /** Envia resolviendo los placeholders de PlaceholderAPI con el jugador dado. */
    public static void send(Audience target, String input, Player papiContext) {
        if (target != null) {
            target.sendMessage(parse(input, papiContext));
        }
    }

    /** Envia a todo el servidor, consola incluida. */
    public static void broadcast(String input) {
        Bukkit.getServer().sendMessage(parse(input));
    }

    /** Envia a la action bar. */
    public static void actionBar(Audience target, String input) {
        if (target != null) {
            target.sendActionBar(parse(input));
        }
    }

    // ------------------------------------------------------------------
    // Interno
    // ------------------------------------------------------------------

    /**
     * Resuelve los placeholders de uno en uno y escapa la salida de cada uno.
     *
     * <p>Escapar importa: si un placeholder devuelve un nombre de clan como
     * {@code <Rey>}, MiniMessage intentaria leerlo como un tag y se comeria el texto.
     * Resolverlos por separado permite escapar solo lo que viene de fuera, dejando
     * intactos los tags que el admin escribio a proposito.</p>
     */
    private static String applyPlaceholders(String input, Player player) {
        if (input.indexOf('%') < 0 || !isPapiPresent()) {
            return input;
        }
        Matcher matcher = PLACEHOLDER.matcher(input);
        StringBuilder out = new StringBuilder(input.length());
        while (matcher.find()) {
            String resolved = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, matcher.group());
            matcher.appendReplacement(out, Matcher.quoteReplacement(MINI.escapeTags(resolved)));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static boolean isPapiPresent() {
        Boolean cached = papiPresent;
        if (cached == null) {
            cached = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
            papiPresent = cached;
        }
        return cached;
    }

    /** Ancho en pixeles del texto plano. */
    private static int widthOf(String plain, boolean bold) {
        int width = 0;
        for (int i = 0; i < plain.length(); i++) {
            width += FontWidth.of(plain.charAt(i), bold);
        }
        return width;
    }

    private static boolean containsBold(String input) {
        return input != null && (input.contains("<b>") || input.contains("<bold>")
                || input.contains("&l") || input.contains("§l"));
    }

    private static String tagFor(char code) {
        int index = Character.digit(code, 16);
        if (index >= 0) {
            return LEGACY_TAGS[index];
        }
        return switch (Character.toLowerCase(code)) {
            case 'k' -> "<obf>";
            case 'l' -> "<b>";
            case 'm' -> "<st>";
            case 'n' -> "<u>";
            case 'o' -> "<i>";
            case 'r' -> "<reset>";
            default -> null;
        };
    }

    /** Lee el formato {@code &x&f&f&0&0&0&0} y devuelve los 6 digitos, o null si no encaja. */
    private static String readXHex(String s, int start) {
        StringBuilder hex = new StringBuilder(6);
        for (int j = 0; j < 6; j++) {
            int pos = start + 2 + (j * 2);
            char sep = s.charAt(pos);
            char digit = s.charAt(pos + 1);
            if ((sep != '&' && sep != '§') || Character.digit(digit, 16) < 0) {
                return null;
            }
            hex.append(digit);
        }
        return hex.toString();
    }

    /**
     * Pasa una racha de codigos legacy a formato '§'. El hex {@code &#RRGGBB} no tiene
     * equivalente directo y se expande al formato {@code §x§R§R§G§G§B§B} que entiende Bukkit.
     */
    private static String codesToSection(String codes) {
        StringBuilder out = new StringBuilder(codes.length());
        int i = 0;
        while (i < codes.length()) {
            char c = codes.charAt(i);
            if ((c == '&' || c == '§') && i + 7 < codes.length() && codes.charAt(i + 1) == '#') {
                out.append('§').append('x');
                for (int j = i + 2; j < i + 8; j++) {
                    out.append('§').append(Character.toLowerCase(codes.charAt(j)));
                }
                i += 8;
                continue;
            }
            out.append(c == '&' ? '§' : c);
            i++;
        }
        return out.toString();
    }

    private static boolean isHex(String s, int start, int count) {
        for (int i = start; i < start + count; i++) {
            if (Character.digit(s.charAt(i), 16) < 0) {
                return false;
            }
        }
        return true;
    }
}
