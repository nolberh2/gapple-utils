package com.gappleclub.gappleutils.command;

import com.gappleclub.gappleutils.text.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

/**
 * Lo minimo que hace falta para escribir comandos sin repetirse.
 *
 * <p>Esta clase llego a tener diecinueve metodos, con una familia de {@code require*} y su
 * propio sistema de mensajes de error. No se uso ninguno: los plugins ya tienen sus textos
 * en el YAML, y unos mensajes fijos en la libreria compiten con eso en vez de ayudar. Se
 * quedo lo que de verdad borraba codigo en el plugin que la usa.</p>
 *
 * <p>Para validar argumentos, hazlo en el propio comando y corta con
 * {@link CommandException}, que es lo que permite escribir el cuerpo en linea recta:</p>
 *
 * <pre>{@code
 * return CommandUtils.execute(sender, () -> {
 *     Player target = miGuard(sender);   // envia tu mensaje y lanza CommandException.silent()
 *     // ... logica, sin un if de validacion por argumento
 * });
 * }</pre>
 */
public final class CommandUtils {

    /**
     * Respuesta ante un fallo inesperado. Es deliberadamente fija: el detalle va al log,
     * y un plugin que quiera su propio texto puede envolver {@link #execute} el mismo.
     */
    private static final String ERROR_INTERNO = "<#E5534B>Ha ocurrido un error al ejecutar el comando.";

    private CommandUtils() {
    }

    /**
     * Ejecuta el cuerpo del comando capturando los errores de validacion.
     *
     * <p>Una {@link CommandException} con mensaje se le envia al sender; una silenciosa se
     * ignora, porque quien la lanzo ya respondio. Cualquier otra excepcion se registra con
     * su traza y el sender recibe un aviso generico, para que un fallo en un comando no
     * propague ni deje al jugador sin respuesta.</p>
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
            TextUtils.send(sender, ERROR_INTERNO);
        }
        return true;
    }

    /**
     * Filtra las opciones por lo que el jugador lleva escrito.
     *
     * <p>Es un envoltorio de una linea sobre {@link StringUtil#copyPartialMatches}, y por
     * eso mismo casi no entra en la libreria. Se queda porque en un tab-complete se llama
     * una vez por rama —catorce en el unico plugin que la usa— y evita repetir la lista
     * destino y el null-check en cada sitio.</p>
     */
    public static List<String> complete(String arg, Collection<String> options) {
        return StringUtil.copyPartialMatches(arg == null ? "" : arg, options, new ArrayList<>());
    }
}
