package com.github.arhke.siegeengines.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.List;

public class MessageUtil {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    /**
     * Parses a string using legacy {@code &} color codes into a component.
     */
    public static Component legacy(String text) {
        return LEGACY.deserialize(text == null ? "" : text);
    }

    /**
     * Turns a component back into a string using legacy {@code &} color codes, so components can be compared by
     * their text and colors only.
     */
    public static String toLegacy(Component component) {
        return LEGACY.serialize(component);
    }

    /**
     * Parses a list of strings using legacy {@code &} color codes into components.
     */
    public static List<Component> legacy(List<String> lines) {
        return lines.stream().map(MessageUtil::legacy).toList();
    }
}
