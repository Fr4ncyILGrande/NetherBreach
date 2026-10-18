package it.fr4ncyilgrande.netherbreach;

import java.util.Locale;

/** Supported plugin languages and Minecraft locale resolution. */
enum PluginLanguage {
    IT("it"),
    EN("en");

    private final String configKey;

    PluginLanguage(String configKey) {
        this.configKey = configKey;
    }

    String configKey() {
        return configKey;
    }

    static PluginLanguage fromConfiguredValue(String value, PluginLanguage fallback) {
        if (value == null) {
            return fallback;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "it", "italian", "italiano" -> IT;
            case "en", "english", "inglese" -> EN;
            default -> fallback;
        };
    }

    static PluginLanguage fromClientLocale(String locale, PluginLanguage fallback) {
        if (locale == null || locale.isBlank()) {
            return fallback;
        }
        String normalized = locale.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("it") && isLocaleBoundary(normalized, 2)) {
            return IT;
        }
        if (normalized.startsWith("en") && isLocaleBoundary(normalized, 2)) {
            return EN;
        }
        return fallback;
    }

    private static boolean isLocaleBoundary(String locale, int languageLength) {
        return locale.length() == languageLength
                || locale.charAt(languageLength) == '_'
                || locale.charAt(languageLength) == '-';
    }
}
