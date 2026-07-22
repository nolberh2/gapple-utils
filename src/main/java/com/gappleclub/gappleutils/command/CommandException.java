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

    private final transient Component message;

    public CommandException(Component message) {
        // Sin stacktrace: no es un fallo del servidor, es un usuario escribiendo mal.
        super(null, null, false, false);
        this.message = message;
    }

    /** Mensaje listo para enviar al sender. */
    public Component componentMessage() {
        return message;
    }
}
