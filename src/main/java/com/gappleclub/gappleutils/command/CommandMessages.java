package com.gappleclub.gappleutils.command;

/**
 * Mensajes de error de {@link CommandUtils}, con defaults en castellano.
 *
 * <p>La libreria no lee YAML, pero el estandar pide mensajes editables desde config. La
 * solucion es esta: cada plugin lee su propio YAML y llama a
 * {@link CommandUtils#setMessages(CommandMessages)} en el {@code onEnable()} <b>y en su
 * comando de reload</b>; si no, quedan cacheados los valores viejos.</p>
 *
 * <p>Los valores admiten MiniMessage y legacy, mas los placeholders indicados en cada
 * campo.</p>
 */
public class CommandMessages {

    private String noPermission = "<#E5534B>No tienes permiso para hacer eso.";
    private String playerOnly = "<#E5534B>Este comando solo puede usarlo un jugador.";
    /** Placeholder: {@code <arg>} */
    private String missingArgument = "<#E5534B>Falta un argumento.";
    /** Placeholder: {@code <arg>} */
    private String playerNotFound = "<#E5534B>El jugador <#E8A33D><arg><#E5534B> no esta conectado.";
    /** Placeholder: {@code <arg>} */
    private String playerNeverPlayed = "<#E5534B>El jugador <#E8A33D><arg><#E5534B> nunca ha entrado al servidor.";
    /** Placeholder: {@code <arg>} */
    private String notANumber = "<#E5534B><#E8A33D><arg><#E5534B> no es un numero valido.";
    /** Placeholders: {@code <arg>}, {@code <min>}, {@code <max>} */
    private String outOfRange = "<#E5534B>El valor debe estar entre <#E8A33D><min><#E5534B> y <#E8A33D><max><#E5534B>.";
    /** Placeholder: {@code <arg>} */
    private String invalidDuration = "<#E5534B><#E8A33D><arg><#E5534B> no es una duracion valida. Ejemplo: <#E8A33D>1d2h30m";
    /** Placeholders: {@code <arg>}, {@code <options>} */
    private String invalidOption = "<#E5534B>Opcion invalida: <#E8A33D><arg><#E5534B>. Validas: <#9AA3B2><options>";

    public String getNoPermission() {
        return noPermission;
    }

    public CommandMessages setNoPermission(String value) {
        this.noPermission = value;
        return this;
    }

    public String getPlayerOnly() {
        return playerOnly;
    }

    public CommandMessages setPlayerOnly(String value) {
        this.playerOnly = value;
        return this;
    }

    public String getMissingArgument() {
        return missingArgument;
    }

    public CommandMessages setMissingArgument(String value) {
        this.missingArgument = value;
        return this;
    }

    public String getPlayerNotFound() {
        return playerNotFound;
    }

    public CommandMessages setPlayerNotFound(String value) {
        this.playerNotFound = value;
        return this;
    }

    public String getPlayerNeverPlayed() {
        return playerNeverPlayed;
    }

    public CommandMessages setPlayerNeverPlayed(String value) {
        this.playerNeverPlayed = value;
        return this;
    }

    public String getNotANumber() {
        return notANumber;
    }

    public CommandMessages setNotANumber(String value) {
        this.notANumber = value;
        return this;
    }

    public String getOutOfRange() {
        return outOfRange;
    }

    public CommandMessages setOutOfRange(String value) {
        this.outOfRange = value;
        return this;
    }

    public String getInvalidDuration() {
        return invalidDuration;
    }

    public CommandMessages setInvalidDuration(String value) {
        this.invalidDuration = value;
        return this;
    }

    public String getInvalidOption() {
        return invalidOption;
    }

    public CommandMessages setInvalidOption(String value) {
        this.invalidOption = value;
        return this;
    }
}
