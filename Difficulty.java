import java.awt.Color;
import java.util.Random;

/**
 * ─────────────────────────────────────────────────────────────
 * Difficulty.java — Enum
 * ─────────────────────────────────────────────────────────────
 *
 * Defines the four difficulty levels for the game.
 * Each level controls:
 *   - How big targets are (minSize / maxSize)
 *   - How long targets stay on screen before expiring (lifetime range)
 *   - How frequently new targets spawn (spawnInterval)
 *   - What colour the difficulty button and HUD label appear in
 *
 * Using an enum here instead of separate constants or a class means:
 *   - All difficulty data lives in one place (easy to change)
 *   - You can loop over Difficulty.values() to build the toolbar buttons
 *   - Adding a new level (e.g. NIGHTMARE) only requires one new enum entry
 *
 * OOP concept: ENCAPSULATION — all data for a difficulty is bundled together
 */
public enum Difficulty {

    //          label     minSz maxSz  maxLife minLife spawnMs  colour
    EASY   ("EASY",   60,  90,  2200, 1400, 1000, new Color( 80, 200, 120)), // green
    MEDIUM ("MEDIUM", 40,  65,  1500,  900,  750, new Color(255, 200,  50)), // yellow
    HARD   ("HARD",   25,  45,   900,  550,  500, new Color(255, 100,  50)), // orange
    INSANE ("INSANE", 16,  30,   550,  350,  300, new Color(220,  60, 255)); // purple

    // ── Fields ────────────────────────────────────────────────
    // public final = readable from anywhere, but never changeable after construction

    public final String label;          // displayed name (e.g. "HARD")
    public final int    minSize;        // smallest possible target diameter (pixels)
    public final int    maxSize;        // largest possible target diameter (pixels)
    public final long   maxLifetime;    // longest a target can live (milliseconds)
    public final long   minLifetime;    // shortest a target can live (milliseconds)
    public final int    spawnInterval;  // minimum ms between target spawns
    public final Color  color;          // HUD / button accent colour for this difficulty

    /**
     * Enum constructor — called once per enum constant at program start.
     * Parameters map directly to the values listed above each constant.
     */
    Difficulty(String label, int minSize, int maxSize,
               long maxLifetime, long minLifetime,
               int spawnInterval, Color color) {
        this.label         = label;
        this.minSize       = minSize;
        this.maxSize       = maxSize;
        this.maxLifetime   = maxLifetime;
        this.minLifetime   = minLifetime;
        this.spawnInterval = spawnInterval;
        this.color         = color;
    }

    /**
     * Returns a random lifetime (in ms) within this difficulty's range.
     * Called by GamePanel.spawnTargets() each time a new target is created.
     *
     * Formula: minLifetime + random portion of the range
     * Example (HARD): 550 + rng.nextDouble() * (900 - 550) → 550..900 ms
     *
     * @param rng  The shared Random instance from GamePanel
     */
    public long randomLifetime(Random rng) {
        return minLifetime + (long)(rng.nextDouble() * (maxLifetime - minLifetime));
    }

    /**
     * Returns a random size (in pixels) within this difficulty's range.
     * Called by GamePanel.spawnTargets() each time a new target is created.
     *
     * rng.nextInt(n) returns 0..n-1, so +1 makes the range inclusive on both ends.
     * Example (MEDIUM): 40 + rng.nextInt(26) → 40..65 pixels
     *
     * @param rng  The shared Random instance from GamePanel
     */
    public int randomSize(Random rng) {
        return minSize + rng.nextInt(maxSize - minSize + 1);
    }
}
