package com.gappleclub.gappleutils.command;

import com.gappleclub.gappleutils.text.TextUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Solo se prueba lo que no necesita un servidor arrancado. Los {@code require*} que
 * resuelven jugadores llaman a {@code Bukkit}, y montar un mock del servidor para eso
 * costaria mas de lo que aporta: su logica util (el mensaje de error) ya queda cubierta
 * por los demas.
 */
class CommandUtilsTest {

    @AfterEach
    void restaurarMensajes() {
        // Es estado estatico: sin esto, un test que cambie los mensajes contamina al resto.
        CommandUtils.setMessages(new CommandMessages());
    }

    @Nested
    @DisplayName("parseDuration")
    class Duraciones {

        @Test
        void sumaLasUnidadesEncadenadas() {
            assertEquals(TimeUnit.DAYS.toMillis(1) + TimeUnit.HOURS.toMillis(2) + TimeUnit.MINUTES.toMillis(30),
                    CommandUtils.parseDuration("1d2h30m"));
        }

        @ParameterizedTest
        @CsvSource({
                "30s, 30000",
                "1m, 60000",
                "1h, 3600000",
                "1d, 86400000",
                "1w, 604800000",
                "1mo, 2592000000",
                "500ms, 500"
        })
        void cadaUnidad(String entrada, long esperado) {
            assertEquals(esperado, CommandUtils.parseDuration(entrada));
        }

        @ParameterizedTest
        @DisplayName("rechaza la basura en vez de interpretarla a medias")
        @ValueSource(strings = {"10x", "hola", "", "1d basura", "d", "-5s"})
        void entradasInvalidas(String entrada) {
            // "10x" no debe colar como 10 milisegundos: es mejor un error claro.
            assertEquals(-1, CommandUtils.parseDuration(entrada));
        }

        @Test
        void nullEsInvalido() {
            assertEquals(-1, CommandUtils.parseDuration(null));
        }

        @Test
        void ticksSon50ms() {
            assertEquals(20, CommandUtils.parseDurationTicks("1s"));
            assertEquals(-1, CommandUtils.parseDurationTicks("basura"));
        }
    }

    @Nested
    @DisplayName("formatDuration")
    class Formateo {

        @Test
        void seQuedaEnDosUnidades() {
            // "1d 2h" se lee mejor que "1d 2h 3m 4s".
            assertEquals("1d 2h", CommandUtils.formatDuration(
                    TimeUnit.DAYS.toMillis(1) + TimeUnit.HOURS.toMillis(2) + TimeUnit.MINUTES.toMillis(3)));
        }

        @Test
        void saltaLasUnidadesVacias() {
            assertEquals("1d 30m", CommandUtils.formatDuration(
                    TimeUnit.DAYS.toMillis(1) + TimeUnit.MINUTES.toMillis(30)));
        }

        @Test
        void elFormatoLargoConcuerdaElPlural() {
            assertEquals("1 dia, 1 hora", CommandUtils.formatDurationLong(
                    TimeUnit.DAYS.toMillis(1) + TimeUnit.HOURS.toMillis(1)));
            assertEquals("2 dias, 3 horas", CommandUtils.formatDurationLong(
                    TimeUnit.DAYS.toMillis(2) + TimeUnit.HOURS.toMillis(3)));
        }

        @Test
        void pordebajoDeUnSegundo() {
            assertEquals("0s", CommandUtils.formatDuration(10));
            assertEquals("menos de 1 segundo", CommandUtils.formatDurationLong(10));
        }

        @Test
        void idaYVueltaConservaElValor() {
            String texto = CommandUtils.formatDuration(TimeUnit.HOURS.toMillis(5));
            assertEquals(TimeUnit.HOURS.toMillis(5), CommandUtils.parseDuration(texto));
        }
    }

    @Nested
    @DisplayName("Validacion de argumentos")
    class Validacion {

        private final String[] args = {"pepe", "42", "no-numero", "3.5"};

        @Test
        void devuelveElValorCuandoEsCorrecto() {
            assertEquals("pepe", CommandUtils.requireArg(args, 0));
            assertEquals(42, CommandUtils.requireInt(args, 1, 1, 64));
            assertEquals(3.5, CommandUtils.requireDouble(args, 3, 0, 10));
        }

        @Test
        void argumentoQueFaltaOEstaVacio() {
            assertThrows(CommandException.class, () -> CommandUtils.requireArg(args, 9));
            assertThrows(CommandException.class, () -> CommandUtils.requireArg(new String[]{""}, 0));
            assertThrows(CommandException.class, () -> CommandUtils.requireArg(null, 0));
        }

        @Test
        void numeroInvalidoOFueraDeRango() {
            assertThrows(CommandException.class, () -> CommandUtils.requireInt(args, 2, 0, 100));
            assertThrows(CommandException.class, () -> CommandUtils.requireInt(args, 1, 1, 10));
        }

        @Test
        void losLimitesDelRangoEntran() {
            String[] limites = {"1", "64"};
            assertEquals(1, CommandUtils.requireInt(limites, 0, 1, 64));
            assertEquals(64, CommandUtils.requireInt(limites, 1, 1, 64));
        }

        @Test
        void enumSinDistinguirMayusculas() {
            assertEquals(TimeUnit.SECONDS,
                    CommandUtils.requireEnum(new String[]{"seconds"}, 0, TimeUnit.class));
            assertThrows(CommandException.class,
                    () -> CommandUtils.requireEnum(new String[]{"parsec"}, 0, TimeUnit.class));
        }

        @Test
        @DisplayName("el error del enum lista las opciones validas")
        void elErrorDelEnumAyuda() {
            CommandException e = assertThrows(CommandException.class,
                    () -> CommandUtils.requireEnum(new String[]{"parsec"}, 0, TimeUnit.class));
            String mensaje = TextUtils.strip(e.componentMessage());
            assertTrue(mensaje.contains("parsec"), mensaje);
            assertTrue(mensaje.contains("seconds"), mensaje);
        }

        @Test
        void requireRestUneElRestoDeArgumentos() {
            assertEquals("42 no-numero 3.5", CommandUtils.requireRest(args, 1));
            assertEquals("3.5", CommandUtils.requireRest(args, 3));
        }

        @Test
        void duracionInvalida() {
            assertThrows(CommandException.class,
                    () -> CommandUtils.requireDuration(new String[]{"10x"}, 0));
            assertEquals(60000, CommandUtils.requireDuration(new String[]{"1m"}, 0));
        }
    }

    @Nested
    @DisplayName("Aborto silencioso")
    class Silencioso {

        @Test
        void noLlevaMensaje() {
            // execute() no debe intentar enviar nada cuando el mensaje es null.
            assertNull(CommandException.silent().componentMessage());
        }

        @Test
        void reutilizaLaMismaInstancia() {
            assertSame(CommandException.silent(), CommandException.silent());
        }
    }

    @Nested
    class Mensajes {

        @Test
        @DisplayName("los mensajes del plugin sustituyen a los por defecto")
        void sePuedenSustituir() {
            CommandUtils.setMessages(new CommandMessages().setNotANumber("<red>Eso no vale: <arg>"));
            CommandException e = assertThrows(CommandException.class,
                    () -> CommandUtils.requireInt(new String[]{"xyz"}, 0, 0, 10));
            assertEquals("Eso no vale: xyz", TextUtils.strip(e.componentMessage()));
        }

        @Test
        void setMessagesConNullNoBorraLosActuales() {
            CommandUtils.setMessages(null);
            assertTrue(CommandUtils.getMessages().getNoPermission().contains("permiso"));
        }

        @Test
        @DisplayName("el valor de un argumento no se interpreta como MiniMessage")
        void elArgumentoNoSeParsea() {
            // Un jugador escribiendo "<red>" en un argumento no debe poder inyectar
            // formato en el mensaje de error.
            CommandException e = assertThrows(CommandException.class,
                    () -> CommandUtils.requireInt(new String[]{"<red>x"}, 0, 0, 10));
            assertTrue(TextUtils.strip(e.componentMessage()).contains("<red>x"));
        }
    }

    @Nested
    @DisplayName("Tab-complete")
    class TabComplete {

        @Test
        void filtraPorPrefijo() {
            List<String> opciones = List.of("gapple", "gold", "hola");
            assertEquals(List.of("gapple", "gold"), CommandUtils.complete("g", opciones));
            assertEquals(opciones, CommandUtils.complete("", opciones));
            assertEquals(opciones, CommandUtils.complete(null, opciones));
        }

        @Test
        void ignoraMayusculas() {
            assertEquals(List.of("gapple"), CommandUtils.complete("GA", List.of("gapple", "hola")));
        }

        @Test
        void deUnEnumDevuelveMinusculas() {
            assertTrue(CommandUtils.complete("sec", TimeUnit.class).contains("seconds"));
        }
    }
}
