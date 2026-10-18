package it.fr4ncyilgrande.netherbreach;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoofPerforatorManagerTest {

    @Test
    void acceptsOnlyTheFiveLayersBelowTheNetherLogicalHeight() {
        assertFalse(RoofPerforatorManager.isWithinRoofLayer(122, 0, 256, 128));
        assertTrue(RoofPerforatorManager.isWithinRoofLayer(123, 0, 256, 128));
        assertTrue(RoofPerforatorManager.isWithinRoofLayer(127, 0, 256, 128));
        assertFalse(RoofPerforatorManager.isWithinRoofLayer(128, 0, 256, 128));
        assertFalse(RoofPerforatorManager.isWithinRoofLayer(255, 0, 256, 128));
    }

    @Test
    void adaptsToCustomDimensionHeightsWithoutLeavingWorldBounds() {
        assertFalse(RoofPerforatorManager.isWithinRoofLayer(250, -64, 320, 256));
        assertTrue(RoofPerforatorManager.isWithinRoofLayer(251, -64, 320, 256));
        assertTrue(RoofPerforatorManager.isWithinRoofLayer(255, -64, 320, 256));
        assertFalse(RoofPerforatorManager.isWithinRoofLayer(256, -64, 320, 256));
        assertFalse(RoofPerforatorManager.isWithinRoofLayer(10, 10, 10, 10));
    }

    @Test
    void neverUsesLogicalHeightAboveThePhysicalWorldLimit() {
        assertTrue(RoofPerforatorManager.isWithinRoofLayer(319, -64, 320, 512));
        assertFalse(RoofPerforatorManager.isWithinRoofLayer(320, -64, 320, 512));
    }
}
