package com.gappleclub.gappleutils.text;

/**
 * Formato detectado en un string de entrada.
 *
 * <p>Es informativo: {@link TextUtils#parse(String)} maneja los cuatro casos igual
 * de bien. Sirve para diagnosticos o para migrar configs viejas.</p>
 */
public enum TextFormat {

    /** Sin color ni formato. */
    PLAIN,

    /** Solo codigos legacy ({@code &c}, {@code &#RRGGBB}). */
    LEGACY,

    /** Solo tags de MiniMessage. */
    MINIMESSAGE,

    /** Los dos mezclados en el mismo string. */
    MIXED
}
