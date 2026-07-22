package com.gappleclub.gappleutils.text;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextUtilsTest {

    @Nested
    @DisplayName("legacyToMini")
    class LegacyToMini {

        @Test
        void convierteColoresYFormato() {
            assertEquals("<reset><red>Hola", TextUtils.legacyToMini("&cHola"));
            assertEquals("<b><u>X", TextUtils.legacyToMini("&l&nX"));
            assertEquals("<reset><green>Verde", TextUtils.legacyToMini("§aVerde"));
        }

        @Test
        void convierteLosTresFormatosHex() {
            assertEquals("<reset><#F3C14A>Oro", TextUtils.legacyToMini("&#F3C14AOro"));
            assertEquals("<reset><#f3c14a>Oro", TextUtils.legacyToMini("&x&f&3&c&1&4&aOro"));
        }

        @Test
        @DisplayName("un codigo de COLOR resetea el formato; uno de formato no")
        void colorReseteaFormato() {
            // Es la semantica de legacy y no la de Adventure. Sin el <reset>, en
            // "&4&lNombre &4texto" el negrita se arrastraria hasta el final.
            assertTrue(TextUtils.legacyToMini("&c").startsWith("<reset>"));
            assertFalse(TextUtils.legacyToMini("&l").startsWith("<reset>"));
        }

        @Test
        void dejaIntactoLoQueNoEsUnCodigo() {
            assertEquals("Tom & Jerry", TextUtils.legacyToMini("Tom & Jerry"));
            assertEquals("<red>X", TextUtils.legacyToMini("<red>X"));
            assertEquals("", TextUtils.legacyToMini(""));
        }
    }

    @Nested
    @DisplayName("Semantica de legacy preservada al renderizar")
    class SemanticaLegacy {

        /** El formateo resultante debe ser indistinguible del que daria Bukkit con '§'. */
        private void mismoRender(String entrada, String legacyEsperado) {
            assertEquals(Rendering.ofLegacy(legacyEsperado),
                    Rendering.of(TextUtils.parse(entrada)),
                    "El render de [" + entrada + "] no coincide con el legacy equivalente");
        }

        @Test
        @DisplayName("el color corta el negrita anterior")
        void colorCortaNegrita() {
            mismoRender("&4&lVictima &4murio a manos de &4&lAsesino",
                    "§4§lVictima §4murio a manos de §4§lAsesino");
        }

        @Test
        void formatoSeAcumulaSobreElColor() {
            mismoRender("&a&l&nTexto", "§a§l§nTexto");
        }

        @Test
        void hexSeComportaComoColor() {
            mismoRender("&#F3C14A&lOro &#F3C14Anormal", "§x§f§3§c§1§4§a§lOro §x§f§3§c§1§4§anormal");
        }
    }

    @Nested
    @DisplayName("colorize (puente a String)")
    class Colorize {

        @Test
        @DisplayName("conserva los codigos finales sin texto detras")
        void conservaCodigosFinales() {
            // Caso de un "prefix" que acaba en &7 para tenir lo que se le concatene
            // despues. Sin texto al que aplicarse, el serializador los descartaria.
            assertEquals("§8[§cCoreFFA§8] §7", TextUtils.colorize("&8[&cCoreFFA&8] &7"));
            assertEquals("§7", TextUtils.colorize("&7"));
        }

        @Test
        void expandeElHexFinalAlFormatoDeBukkit() {
            assertEquals("§x§f§3§c§1§4§a", TextUtils.colorize("&#F3C14A"));
        }

        @Test
        @DisplayName("un prefijo concatenado tiñe lo que viene detras")
        void prefijoConcatenadoTine() {
            String prefijo = TextUtils.colorize("&8[&cGapple&8] &7");
            assertEquals(Rendering.ofLegacy("§8[§cGapple§8] §7mensaje"),
                    Rendering.ofLegacy(prefijo + "mensaje"));
        }

        @Test
        void aceptaMiniMessageYLegacyMezclados() {
            assertEquals("Hola mundo", TextUtils.strip(TextUtils.colorize("&cHola <gold>mundo")));
        }

        @Test
        void nullYVacioSonSeguros() {
            assertEquals("", TextUtils.colorize(null));
            assertEquals("", TextUtils.colorize(""));
        }
    }

    @Nested
    @DisplayName("Texto que NO debe interpretarse como tags")
    class TagsLiterales {

        /**
         * Las ayudas de los comandos usan {@code <flag>} o {@code <set/add/clear>} como
         * sintaxis de uso. MiniMessage deja los tags desconocidos literales, y de eso
         * depende que los /help no salgan mutilados.
         */
        @ParameterizedTest
        @ValueSource(strings = {
                "/coreffa hide <flag>",
                "/coreffa lore <set/add/clear>",
                "/coreffa enchant <enchantment> [lvl]",
                "/coreffa attribute add <attribute> <value> <slot>",
                "Use: /coreffa <help|rename|lore|reload>",
                "/coreffa unbreakable [true|false]"
        })
        void sintaxisDeUsoSobrevive(String linea) {
            assertEquals(linea, TextUtils.strip(TextUtils.parse(linea)));
        }
    }

    @Nested
    class Deteccion {

        @Test
        void distingueLosCuatroCasos() {
            assertEquals(TextFormat.PLAIN, TextUtils.detect("hola"));
            assertEquals(TextFormat.LEGACY, TextUtils.detect("&cx"));
            assertEquals(TextFormat.MINIMESSAGE, TextUtils.detect("<red>x"));
            assertEquals(TextFormat.MIXED, TextUtils.detect("&c<gold>x"));
        }
    }

    @Nested
    class Strip {

        @Test
        void quitaAmbosFormatos() {
            assertEquals("Hola mundo", TextUtils.strip("&cHola <gold>mundo"));
            assertEquals("", TextUtils.strip((String) null));
        }
    }

    @Nested
    class Parse {

        @Test
        void nullDevuelveComponenteVacio() {
            assertEquals("", TextUtils.strip(TextUtils.parse((String) null)));
        }

        @Test
        @DisplayName("MiniMessage roto degrada a texto plano en vez de propagar")
        void miniMessageRotoDegrada() {
            // Un mensaje mal escrito en un YAML no debe tumbar el comando que lo muestra.
            String roto = "<gradient:#zzz>texto</gradient>";
            assertTrue(TextUtils.strip(TextUtils.parse(roto)).contains("texto"));
        }

        @Test
        void parseListMantieneElOrden() {
            List<String> entrada = List.of("&auno", "&cdos");
            assertEquals(List.of("uno", "dos"),
                    TextUtils.parseList(entrada, null).stream().map(TextUtils::strip).toList());
        }
    }

    @Nested
    @DisplayName("FontWidth (centrado)")
    class Anchos {

        @Test
        void midePorAnchoRealNoPorNumeroDeCaracteres() {
            assertEquals(2, FontWidth.of('i', false));
            assertEquals(6, FontWidth.of('W', false));
            assertEquals(4, FontWidth.of(' ', false));
        }

        @Test
        @DisplayName("la tabla llega hasta el final del rango imprimible")
        void laTablaNoSeQuedaCorta() {
            // Si a la tabla le faltaran entradas, estos caerian al ancho por defecto (7).
            // Es el fallo tipico al teclearla y descentra todo el texto que lleve simbolos.
            assertEquals(2, FontWidth.of('|', false));
            assertEquals(4, FontWidth.of('}', false));
            assertEquals(7, FontWidth.of('~', false));
        }

        @Test
        void laNegritaSumaUnPixelPorCaracter() {
            assertEquals(FontWidth.of('a', false) + 1, FontWidth.of('a', true));
        }

        @Test
        @DisplayName("el color no cambia el centrado, pero la negrita si")
        void centrarMideElTextoNoLosTags() {
            // Si se midiera el string crudo, los tags contarian como caracteres.
            String sinColor = TextUtils.strip(TextUtils.center("Gapple"));
            String conColor = TextUtils.strip(TextUtils.center("<red>Gapple"));
            String enNegrita = TextUtils.strip(TextUtils.center("<b>Gapple"));

            assertEquals(sinColor, conColor);
            assertTrue(enNegrita.length() < sinColor.length(),
                    "el texto en negrita es mas ancho, asi que necesita menos relleno");
        }
    }
}
