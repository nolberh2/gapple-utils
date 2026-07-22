package com.gappleclub.gappleutils.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Aplana un texto a la secuencia de (caracter, estilo EFECTIVO heredado).
 *
 * <p>Comparar strings de codigos legacy no sirve para saber si dos formateos son
 * equivalentes: el serializador omite codigos redundantes y anida los componentes de otra
 * forma, asi que dos representaciones distintas pueden verse exactamente igual. Lo que
 * decide es el estilo efectivo de cada caracter, que es lo que el cliente renderiza.</p>
 */
final class Rendering {

    private static final LegacyComponentSerializer SECTION = LegacyComponentSerializer.builder()
            .character('§')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private Rendering() {
    }

    /** Representacion canonica de un string con codigos '§'. */
    static String ofLegacy(String legacySection) {
        return of(SECTION.deserialize(legacySection));
    }

    /** Representacion canonica de un componente. */
    static String of(Component component) {
        StringBuilder out = new StringBuilder();
        flatten(component, Style.empty(), out);
        return out.toString();
    }

    private static void flatten(Component component, Style inherited, StringBuilder out) {
        Style effective = inherited.merge(component.style(), Style.Merge.Strategy.IF_ABSENT_ON_TARGET);
        if (component instanceof TextComponent text && !text.content().isEmpty()) {
            String style = "[" + effective.color()
                    + "|b=" + effective.decoration(TextDecoration.BOLD)
                    + ",i=" + effective.decoration(TextDecoration.ITALIC)
                    + ",u=" + effective.decoration(TextDecoration.UNDERLINED)
                    + ",s=" + effective.decoration(TextDecoration.STRIKETHROUGH)
                    + ",o=" + effective.decoration(TextDecoration.OBFUSCATED) + "]";
            for (char c : text.content().toCharArray()) {
                out.append(style).append(c).append('\n');
            }
        }
        for (Component child : component.children()) {
            flatten(child, effective, out);
        }
    }
}
