package com.gappleclub.gappleutils.color;

import com.gappleclub.gappleutils.text.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColorUtilsTest {

    /** Colores efectivos, en orden, de cada caracter de un componente. */
    private static List<TextColor> coloresDe(Component component) {
        List<TextColor> out = new ArrayList<>();
        recoger(component, out);
        return out;
    }

    private static void recoger(Component component, List<TextColor> out) {
        if (component instanceof TextComponent text) {
            for (int i = 0; i < text.content().length(); i++) {
                out.add(component.color());
            }
        }
        component.children().forEach(child -> recoger(child, out));
    }

    @Nested
    class Paleta {

        @Test
        @DisplayName("los hex de marca coinciden con los de la web")
        void hexDeMarca() {
            // Si esto cambia, tiene que cambiar tambien en gappleweb2/src/Landing.css.
            assertEquals("#f3c14a", ColorUtils.GOLD.asHexString());
            assertEquals("#e9c877", ColorUtils.GOLD_SOFT.asHexString());
            assertEquals("#c9962f", ColorUtils.GOLD_DEEP.asHexString());
        }

        @Test
        void byNameIgnoraMayusculasYEspacios() {
            assertEquals(ColorUtils.GOLD, ColorUtils.byName("  GoLd ").orElseThrow());
            assertEquals(ColorUtils.GOLD_DEEP, ColorUtils.byName("gold_deep").orElseThrow());
        }

        @Test
        void byNameDesconocidoNoLanza() {
            assertFalse(ColorUtils.byName("morado_chillon").isPresent());
            assertFalse(ColorUtils.byName(null).isPresent());
        }

        @Test
        void namesListaTodaLaPaleta() {
            assertTrue(ColorUtils.names().containsAll(List.of("gold", "success", "error")));
            ColorUtils.names().forEach(n -> assertTrue(ColorUtils.byName(n).isPresent(), n));
        }
    }

    @Nested
    class Gradientes {

        @Test
        void conservaElTextoIntacto() {
            assertEquals("Gapple",
                    TextUtils.strip(ColorUtils.gradient("Gapple", ColorUtils.GOLD, ColorUtils.INFO)));
        }

        @Test
        @DisplayName("empieza y acaba exactamente en los colores dados")
        void respetaLosExtremos() {
            // El viaje de ida y vuelta por HSV no debe desviar los stops.
            List<TextColor> colores = coloresDe(
                    ColorUtils.gradient("abcdef", ColorUtils.GOLD, ColorUtils.INFO));
            assertEquals(ColorUtils.GOLD, colores.get(0));
            assertEquals(ColorUtils.INFO, colores.get(colores.size() - 1));
        }

        @Test
        void unSoloStopPintaTodoDeEseColor() {
            List<TextColor> colores = coloresDe(ColorUtils.gradient("abc", ColorUtils.GOLD));
            assertTrue(colores.stream().allMatch(ColorUtils.GOLD::equals));
        }

        @Test
        void textoVacioODeUnCaracterNoRompe() {
            assertEquals("", TextUtils.strip(ColorUtils.gradient("", ColorUtils.GOLD, ColorUtils.INFO)));
            assertEquals("x", TextUtils.strip(ColorUtils.gradient("x", ColorUtils.GOLD, ColorUtils.INFO)));
        }

        @Test
        @DisplayName("interpola en HSV, no en RGB")
        void interpolaEnHsv() {
            // En RGB el punto medio entre dos colores lejanos se va a un gris sucio.
            // En HSV conserva la saturacion, que es lo que uno espera ver.
            TextColor medioHsv = coloresDe(ColorUtils.gradient("abc",
                    TextColor.color(0xFF0000), TextColor.color(0x0000FF))).get(1);
            TextColor medioRgb = ColorUtils.blend(TextColor.color(0xFF0000), TextColor.color(0x0000FF), 0.5f);
            assertNotEquals(medioRgb, medioHsv);

            int max = Math.max(medioHsv.red(), Math.max(medioHsv.green(), medioHsv.blue()));
            int min = Math.min(medioHsv.red(), Math.min(medioHsv.green(), medioHsv.blue()));
            assertTrue(max - min > 200, "el punto medio deberia seguir saturado, no gris");
        }

        @Test
        @DisplayName("la fase desplaza el degradado sin alterar el texto")
        void faseDesplaza() {
            Component sinFase = ColorUtils.gradient("abcdef", 0f, ColorUtils.GOLD, ColorUtils.INFO);
            Component conFase = ColorUtils.gradient("abcdef", 0.5f, ColorUtils.GOLD, ColorUtils.INFO);
            assertEquals(TextUtils.strip(sinFase), TextUtils.strip(conFase));
            assertNotEquals(coloresDe(sinFase), coloresDe(conFase));
        }
    }

    @Nested
    @DisplayName("gradientLines (lo que MiniMessage no sabe hacer)")
    class GradientesMultilinea {

        @Test
        void repartenUnUnicoDegradadoEntreTodasLasLineas() {
            List<Component> lineas = ColorUtils.gradientLines(
                    List.of("uno", "dos"), ColorUtils.GOLD, ColorUtils.INFO);

            assertEquals(2, lineas.size());
            assertEquals("unodos", TextUtils.strip(lineas.get(0)) + TextUtils.strip(lineas.get(1)));

            // Continuo: el primer caracter de la 2a linea NO reinicia el degradado, que es
            // justo lo que pasa aplicando el tag <gradient> nativo linea por linea.
            List<TextColor> primera = coloresDe(lineas.get(0));
            List<TextColor> segunda = coloresDe(lineas.get(1));
            assertEquals(ColorUtils.GOLD, primera.get(0));
            assertNotEquals(ColorUtils.GOLD, segunda.get(0));
            assertEquals(ColorUtils.INFO, segunda.get(segunda.size() - 1));
        }

        @Test
        void lineasVaciasNoRompenLaCuenta() {
            List<Component> lineas = ColorUtils.gradientLines(
                    java.util.Arrays.asList("uno", "", null, "dos"), ColorUtils.GOLD, ColorUtils.INFO);
            assertEquals(4, lineas.size());
        }

        @Test
        void listaVaciaDevuelveListaVacia() {
            assertTrue(ColorUtils.gradientLines(List.of(), ColorUtils.GOLD).isEmpty());
            assertTrue(ColorUtils.gradientLines(null, ColorUtils.GOLD).isEmpty());
        }
    }

    @Nested
    class Derivacion {

        @Test
        void losExtremosSonBlancoYNegro() {
            assertEquals("#ffffff", ColorUtils.lighten(ColorUtils.GOLD, 1f).asHexString());
            assertEquals("#000000", ColorUtils.darken(ColorUtils.GOLD, 1f).asHexString());
        }

        @Test
        void cantidadCeroDevuelveElMismoColor() {
            assertEquals(ColorUtils.GOLD, ColorUtils.lighten(ColorUtils.GOLD, 0f));
            assertEquals(ColorUtils.GOLD, ColorUtils.darken(ColorUtils.GOLD, 0f));
        }

        @Test
        void seSaturaFueraDelRangoEnVezDeDesbordar() {
            assertEquals("#ffffff", ColorUtils.lighten(ColorUtils.GOLD, 5f).asHexString());
            assertEquals(ColorUtils.GOLD, ColorUtils.darken(ColorUtils.GOLD, -3f));
        }

        @Test
        void blendMedioEsElPuntoIntermedio() {
            assertEquals("#808080", ColorUtils.blend(
                    TextColor.color(0x000000), TextColor.color(0xFFFFFF), 0.5f).asHexString());
        }
    }
}
