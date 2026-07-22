package com.gappleclub.gappleutils.command;

import net.kyori.adventure.text.Component;

/**
 * Argumento invalido o requisito no cumplido, con el mensaje ya formateado.
 *
 * <p>La lanzan los {@code require*} de {@link CommandUtils} y la captura
 * {@link CommandUtils#execute}, que se lo envia al sender. Eso permite escribir el cuerpo
 * del comando en linea recta, sin un {@code if} de validacion por argumento.</p>
 */
public class CommandException extends RuntimeException {

    /**
     * Instancia unica para los abortos sin mensaje. No lleva estado ni stacktrace, asi
     * que compartirla evita crear un objeto por cada validacion fallida.
     */
    private static final CommandException SILENT = new CommandException(null);

    private final transient Component message;

    public CommandException(Component message) {
        // Sin stacktrace: no es un fallo del servidor, es un usuario escribiendo mal.
        super(null, null, false, false);
        this.message = message;
    }

    /**
     * Aborta el comando sin enviar nada.
     *
     * <p>Para codigo que ya tiene su propio sistema de mensajes y solo necesita cortar la
     * ejecucion. Permite adoptar {@link CommandUtils#execute} en un plugin que ya existe
     * sin tener que reescribir como emite sus mensajes.</p>
     */
    public static CommandException silent() {
        return SILENT;
    }

    /** Mensaje listo para enviar al sender, o {@code null} si el aborto es silencioso. */
    public Component componentMessage() {
        return message;
    }
}
