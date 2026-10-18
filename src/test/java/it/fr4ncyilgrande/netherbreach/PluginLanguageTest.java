package it.fr4ncyilgrande.netherbreach;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PluginLanguageTest {

    @Test
    void recognizesSupportedMinecraftLocales() {
        assertEquals(PluginLanguage.IT,
                PluginLanguage.fromClientLocale("it_it", PluginLanguage.EN));
        assertEquals(PluginLanguage.IT,
                PluginLanguage.fromClientLocale("IT-CH", PluginLanguage.EN));
        assertEquals(PluginLanguage.EN,
                PluginLanguage.fromClientLocale("en_us", PluginLanguage.IT));
        assertEquals(PluginLanguage.EN,
                PluginLanguage.fromClientLocale("en-GB", PluginLanguage.IT));
    }

    @Test
    void usesFallbackForMissingUnsupportedOrMisleadingLocales() {
        assertEquals(PluginLanguage.IT,
                PluginLanguage.fromClientLocale(null, PluginLanguage.IT));
        assertEquals(PluginLanguage.EN,
                PluginLanguage.fromClientLocale("fr_fr", PluginLanguage.EN));
        assertEquals(PluginLanguage.EN,
                PluginLanguage.fromClientLocale("item", PluginLanguage.EN));
        assertEquals(PluginLanguage.IT,
                PluginLanguage.fromClientLocale("english", PluginLanguage.IT));
    }

    @Test
    void recognizesConfiguredAliasesAndRejectsUnknownValues() {
        assertEquals(PluginLanguage.IT,
                PluginLanguage.fromConfiguredValue("Italiano", PluginLanguage.EN));
        assertEquals(PluginLanguage.EN,
                PluginLanguage.fromConfiguredValue("English", PluginLanguage.IT));
        assertEquals(PluginLanguage.IT,
                PluginLanguage.fromConfiguredValue("unknown", PluginLanguage.IT));
    }
}
