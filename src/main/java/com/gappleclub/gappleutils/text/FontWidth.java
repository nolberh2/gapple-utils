package com.gappleclub.gappleutils.text;

/**
 * Anchos en pixeles de la fuente por defecto del chat de Minecraft.
 *
 * <p>La fuente es de ancho variable ({@code i} mide 1px, {@code W} mide 5px), asi que
 * centrar contando caracteres da resultados torcidos. Esta tabla es lo que permite
 * que {@link TextUtils#center(String)} quede realmente centrado.</p>
 *
 * <p>Los valores excluyen el pixel de separacion entre caracteres, que se suma aparte
 * (y otro mas si el texto esta en negrita).</p>
 */
final class FontWidth {

    /** Ancho por defecto para lo que no esta en la tabla (acentos, simbolos, unicode). */
    private static final int DEFAULT = 6;

    /** Indexada desde el espacio (32) hasta '~' (126). */
    private static final int[] WIDTHS = {
            3, // ' '
            1, // !
            3, // "
            5, // #
            5, // $
            5, // %
            5, // &
            1, // '
            3, // (
            3, // )
            3, // *
            5, // +
            1, // ,
            5, // -
            1, // .
            5, // /
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, // 0-9
            1, // :
            1, // ;
            4, // <
            5, // =
            4, // >
            5, // ?
            6, // @
            5, 5, 5, 5, 5, 5, 5, 5, // A-H
            3, // I
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, // J-Z
            3, // [
            5, // \
            3, // ]
            5, // ^
            5, // _
            2, // `
            5, 5, 5, 5, 5, // a-e
            4, // f
            5, 5, // g-h
            1, // i
            5, // j
            4, // k
            2, // l
            5, 5, 5, 5, 5, 5, 5, // m-s
            3, // t
            5, 5, 5, 5, 5, 5, // u-z
            3, // {
            1, // |
            3, // }
            6  // ~
    };

    private FontWidth() {
    }

    /**
     * Ancho del caracter en pixeles, incluyendo el pixel de separacion.
     *
     * @param c      caracter a medir
     * @param bold   si esta en negrita (cada caracter gana 1px)
     */
    static int of(char c, boolean bold) {
        int index = c - 32;
        int width = (index >= 0 && index < WIDTHS.length) ? WIDTHS[index] : DEFAULT;
        return width + 1 + (bold ? 1 : 0);
    }
}
