package com.gappleclub.gappleutils.command;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code execute} no se prueba aqui: necesita un {@code CommandSender} y, en su rama de
 * error, un servidor arrancado para el logger. Montar un mock del servidor costaria mas de
 * lo que aporta, y su comportamiento se verifico en el servertest.
 */
class CommandUtilsTest {

    @Nested
    @DisplayName("complete")
    class Complete {

        @Test
        void filtraPorPrefijo() {
            List<String> opciones = List.of("gapple", "gold", "hola");
            assertEquals(List.of("gapple", "gold"), CommandUtils.complete("g", opciones));
        }

        @Test
        void sinPrefijoDevuelveTodo() {
            List<String> opciones = List.of("gapple", "gold", "hola");
            assertEquals(opciones, CommandUtils.complete("", opciones));
        }

        @Test
        @DisplayName("un argumento nulo no revienta")
        void argumentoNulo() {
            // Pasa al empezar a escribir: Bukkit puede dar null en el ultimo argumento.
            assertEquals(List.of("gapple"), CommandUtils.complete(null, List.of("gapple")));
        }

        @Test
        void ignoraMayusculas() {
            assertEquals(List.of("gapple"), CommandUtils.complete("GA", List.of("gapple", "hola")));
        }

        @Test
        void sinCoincidenciasDevuelveListaVacia() {
            assertTrue(CommandUtils.complete("zzz", List.of("gapple", "gold")).isEmpty());
        }
    }

    @Nested
    @DisplayName("CommandException")
    class Excepcion {

        @Test
        void elAbortoSilenciosoNoLlevaMensaje() {
            // execute() no debe intentar enviar nada cuando el mensaje es null.
            assertNull(CommandException.silent().componentMessage());
        }

        @Test
        void elAbortoSilenciosoReutilizaLaMismaInstancia() {
            assertSame(CommandException.silent(), CommandException.silent());
        }

        @Test
        void noArrastraStacktrace() {
            // No es un fallo del servidor, es un usuario escribiendo mal: construir la
            // traza seria caro y no aporta nada.
            assertEquals(0, CommandException.silent().getStackTrace().length);
        }
    }
}
