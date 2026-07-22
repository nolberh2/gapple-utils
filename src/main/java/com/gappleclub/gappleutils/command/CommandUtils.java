package com.gappleclub.gappleutils.command;

import com.gappleclub.gappleutils.text.TextUtils;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parseo y validacion de argumentos de comandos, con mensajes de error uniformes.
 *
 * <p>Los {@code require*} lanzan {@link CommandException} en vez de devolver null, y
 * {@link #execute(CommandSender, Runnable)} la captura y responde al sender. El cuerpo del
 * comando queda en linea recta:</p>
 *
 * <pre>{@code
 * return CommandUtils.execute(sender, () -> {
 *     Player target = CommandUtils.requireOnlinePlayer(args, 0);
 *     int amount    = CommandUtils.requireInt(args, 1, 1, 64);
 *     // ... logica, sin un if de validacion por argumento
 * });
 * }</pre>
 *
 * <p>Se usa un wrapper con lambda y no una clase base de comando a proposito: asi sirve
 * igual desde {@code onCommand}, desde Brigadier o desde cualquier framework, sin atar la
 * libreria al sistema de comandos de turno.</p>
 */
public final class CommandUtils {

    private static final Pattern DURATION = Pattern.compile("(\\d+)\\s*(mo|ms|[wdhms])", Pattern.CASE_INSENSITIVE);

    private static volatile CommandMessages messages = new CommandMessages();

    private CommandUtils() {
    }

    /**
     * Sustituye los mensajes de error por los del plugin.
     *
     * <p>Llamalo en el {@code onEnable()} y tambien en tu {@code /reload}: si no, quedan
     * cacheados los de la carga anterior.</p>
     *
     * <p><b>Ojo:</b> es estado estatico. Si dos plugins incluyen GappleUtils sin
     * relocalizar el paquete al shadearla, comparten esta instancia y el segundo pisa los
     * mensajes del primero. El README explica como relocalizar.</p>
     */
    public static void setMessages(CommandMessages newMessages) {
        if (newMessages != null) {
            messages = newMessages;
        }
    }

    /** Mensajes en uso. */
    public static CommandMessages getMessages() {
        return messages;
    }

    // ------------------------------------------------------------------
    // Wrapper
    // ------------------------------------------------------------------

    /**
     * Ejecuta el cuerpo del comando capturando los errores de validacion.
     *
     * <p>Una {@link CommandException} se convierte en mensaje al sender. Cualquier otra
     * excepcion se loguea y se responde con un error generico, para que un fallo en un
     * comando no propague ni ensucie la consola con un stacktrace sin contexto.</p>
     *
     * @return siempre {@code true}, para devolverlo directamente desde {@code onCommand}
     */
    public static boolean execute(CommandSender sender, Runnable body) {
        try {
            body.run();
        } catch (CommandException e) {
            if (e.componentMessage() != null) {
                sender.sendMessage(e.componentMessage());
            }
        } catch (RuntimeException e) {
            // Con la traza: sin ella, un bug real queda invisible y solo se ve el mensaje
            // generico que recibe el jugador.
            Bukkit.getLogger().log(Level.SEVERE, "[GappleUtils] Error ejecutando un comando", e);
            TextUtils.send(sender, messages.getInternalError());
        }
        return true;
    }

    // ------------------------------------------------------------------
    // Requisitos del sender
    // ------------------------------------------------------------------

    /** Exige que el sender sea un jugador. */
    public static Player requireSenderPlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        throw fail(messages.getPlayerOnly());
    }

    /** Exige un permiso. */
    public static void requirePermission(CommandSender sender, String permission) {
        if (!sender.hasPermission(permission)) {
            throw fail(messages.getNoPermission());
        }
    }

    // ------------------------------------------------------------------
    // Argumentos
    // ------------------------------------------------------------------

    /** Exige que el argumento exista y no este vacio. */
    public static String requireArg(String[] args, int index) {
        if (args == null || index < 0 || index >= args.length || args[index].isEmpty()) {
            throw fail(messages.getMissingArgument());
        }
        return args[index];
    }

    /** Une los argumentos desde {@code index} hasta el final, para mensajes o razones. */
    public static String requireRest(String[] args, int index) {
        String first = requireArg(args, index);
        if (index == args.length - 1) {
            return first;
        }
        return String.join(" ", java.util.Arrays.copyOfRange(args, index, args.length));
    }

    /** Exige un jugador conectado. */
    public static Player requireOnlinePlayer(String[] args, int index) {
        String name = requireArg(args, index);
        Player player = Bukkit.getPlayerExact(name);
        if (player == null) {
            throw fail(messages.getPlayerNotFound(), Placeholder.unparsed("arg", name));
        }
        return player;
    }

    /**
     * Exige un jugador que haya entrado alguna vez al servidor.
     *
     * <p>No usa {@code getOfflinePlayer(String)}: esa llamada consulta a Mojang y bloquea
     * el hilo principal si el nombre no esta cacheado.</p>
     */
    public static OfflinePlayer requireOfflinePlayer(String[] args, int index) {
        String name = requireArg(args, index);
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online;
        }
        for (OfflinePlayer candidate : Bukkit.getOfflinePlayers()) {
            if (name.equalsIgnoreCase(candidate.getName())) {
                return candidate;
            }
        }
        throw fail(messages.getPlayerNeverPlayed(), Placeholder.unparsed("arg", name));
    }

    /** Exige un entero dentro del rango (ambos extremos incluidos). */
    public static int requireInt(String[] args, int index, int min, int max) {
        String raw = requireArg(args, index);
        int value;
        try {
            value = Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw fail(messages.getNotANumber(), Placeholder.unparsed("arg", raw));
        }
        if (value < min || value > max) {
            throw fail(messages.getOutOfRange(),
                    Placeholder.unparsed("arg", raw),
                    Placeholder.unparsed("min", String.valueOf(min)),
                    Placeholder.unparsed("max", String.valueOf(max)));
        }
        return value;
    }

    /** Exige un decimal dentro del rango (ambos extremos incluidos). */
    public static double requireDouble(String[] args, int index, double min, double max) {
        String raw = requireArg(args, index);
        double value;
        try {
            value = Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            throw fail(messages.getNotANumber(), Placeholder.unparsed("arg", raw));
        }
        if (value < min || value > max) {
            throw fail(messages.getOutOfRange(),
                    Placeholder.unparsed("arg", raw),
                    Placeholder.unparsed("min", String.valueOf(min)),
                    Placeholder.unparsed("max", String.valueOf(max)));
        }
        return value;
    }

    /** Exige una duracion valida, en milisegundos. Ver {@link #parseDuration(String)}. */
    public static long requireDuration(String[] args, int index) {
        String raw = requireArg(args, index);
        long millis = parseDuration(raw);
        if (millis < 0) {
            throw fail(messages.getInvalidDuration(), Placeholder.unparsed("arg", raw));
        }
        return millis;
    }

    /** Exige un valor de un enum, sin distinguir mayusculas. */
    public static <E extends Enum<E>> E requireEnum(String[] args, int index, Class<E> type) {
        String raw = requireArg(args, index);
        for (E constant : type.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(raw)) {
                return constant;
            }
        }
        StringBuilder options = new StringBuilder();
        for (E constant : type.getEnumConstants()) {
            if (options.length() > 0) {
                options.append(", ");
            }
            options.append(constant.name().toLowerCase(Locale.ROOT));
        }
        throw fail(messages.getInvalidOption(),
                Placeholder.unparsed("arg", raw),
                Placeholder.unparsed("options", options.toString()));
    }

    // ------------------------------------------------------------------
    // Duraciones
    // ------------------------------------------------------------------

    /**
     * Parsea una duracion tipo {@code 1d2h30m} a milisegundos.
     *
     * <p>Unidades: {@code mo} (mes, 30 dias), {@code w}, {@code d}, {@code h}, {@code m},
     * {@code s}, {@code ms}. Se pueden encadenar y se suman.</p>
     *
     * @return los milisegundos, o {@code -1} si la entrada no es valida
     */
    public static long parseDuration(String input) {
        if (input == null || input.isEmpty()) {
            return -1;
        }
        Matcher matcher = DURATION.matcher(input);
        long total = 0;
        int matchedChars = 0;

        while (matcher.find()) {
            matchedChars += matcher.group().length();
            long amount;
            try {
                amount = Long.parseLong(matcher.group(1));
            } catch (NumberFormatException e) {
                return -1;
            }
            total += switch (matcher.group(2).toLowerCase(Locale.ROOT)) {
                case "mo" -> TimeUnit.DAYS.toMillis(30 * amount);
                case "w" -> TimeUnit.DAYS.toMillis(7 * amount);
                case "d" -> TimeUnit.DAYS.toMillis(amount);
                case "h" -> TimeUnit.HOURS.toMillis(amount);
                case "m" -> TimeUnit.MINUTES.toMillis(amount);
                case "s" -> TimeUnit.SECONDS.toMillis(amount);
                default -> amount; // ms
            };
        }

        // Si sobra texto sin consumir, la entrada tenia basura: mejor rechazarla que
        // interpretar "10x" como 10 milisegundos.
        if (matchedChars != input.replace(" ", "").length() || total == 0 && matchedChars == 0) {
            return -1;
        }
        return total;
    }

    /** Igual que {@link #parseDuration(String)} pero en ticks. */
    public static long parseDurationTicks(String input) {
        long millis = parseDuration(input);
        return millis < 0 ? -1 : millis / 50L;
    }

    /** Formato compacto, hasta dos unidades: {@code "1d 2h"}. */
    public static String formatDuration(long millis) {
        return formatDuration(millis, false);
    }

    /** Formato con nombres completos y plurales: {@code "1 dia, 2 horas"}. */
    public static String formatDurationLong(long millis) {
        return formatDuration(millis, true);
    }

    private static String formatDuration(long millis, boolean verbose) {
        if (millis < 1000) {
            return verbose ? "menos de 1 segundo" : "0s";
        }

        long days = TimeUnit.MILLISECONDS.toDays(millis);
        long hours = TimeUnit.MILLISECONDS.toHours(millis) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;

        List<String> parts = new ArrayList<>(2);
        appendUnit(parts, days, "d", "dia", "dias", verbose);
        appendUnit(parts, hours, "h", "hora", "horas", verbose);
        appendUnit(parts, minutes, "m", "minuto", "minutos", verbose);
        appendUnit(parts, seconds, "s", "segundo", "segundos", verbose);

        return String.join(verbose ? ", " : " ", parts);
    }

    private static void appendUnit(List<String> parts, long value, String shortUnit,
                                   String singular, String plural, boolean verbose) {
        // Dos unidades bastan: "1d 2h" se lee mejor que "1d 2h 3m 4s".
        if (value <= 0 || parts.size() >= 2) {
            return;
        }
        parts.add(verbose ? value + " " + (value == 1 ? singular : plural) : value + shortUnit);
    }

    // ------------------------------------------------------------------
    // Tab-complete
    // ------------------------------------------------------------------

    /**
     * Nombres de jugadores conectados que el sender puede ver.
     *
     * <p>El filtro de visibilidad es el motivo de que este metodo exista: sugerir en el tab
     * a un jugador vanisheado delata su presencia.</p>
     */
    public static List<String> completePlayers(CommandSender sender, String arg) {
        List<String> names = new ArrayList<>();
        boolean isPlayer = sender instanceof Player;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!isPlayer || ((Player) sender).canSee(online)) {
                names.add(online.getName());
            }
        }
        return complete(arg, names);
    }

    /** Filtra las opciones por el prefijo escrito. */
    public static List<String> complete(String arg, Collection<String> options) {
        return StringUtil.copyPartialMatches(arg == null ? "" : arg, options, new ArrayList<>());
    }

    /** Filtra los valores de un enum por el prefijo escrito, en minusculas. */
    public static List<String> complete(String arg, Class<? extends Enum<?>> type) {
        List<String> options = new ArrayList<>();
        for (Enum<?> constant : type.getEnumConstants()) {
            options.add(constant.name().toLowerCase(Locale.ROOT));
        }
        return complete(arg, options);
    }

    // ------------------------------------------------------------------
    // Interno
    // ------------------------------------------------------------------

    private static CommandException fail(String message, TagResolver... resolvers) {
        return new CommandException(TextUtils.parse(message, null, resolvers));
    }
}
