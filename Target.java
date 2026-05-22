import java.awt.*;

/**
 * ─────────────────────────────────────────────────────────────
 * Target.java — Abstract Base Class
 * ─────────────────────────────────────────────────────────────
 *
 * This is the parent class for every target shape in the game.
 * It cannot be instantiated directly — you must create a subclass
 * (e.g. CircleTarget, DiamondTarget, TriangleTarget).
 *
 * It stores the data every target needs (position, size, lifetime)
 * and defines three abstract methods that every subclass MUST implement:
 *   - contains()  → was this target clicked?
 *   - draw()      → how does it look?
 *   - getPoints() → how many points is it worth?
 *
 * OOP concept: INHERITANCE + ABSTRACTION
 * All three concrete target classes extend this class.
 */
public abstract class Target {

    // ── Position and size ─────────────────────────────────────
    // protected = visible to this class AND all subclasses
    protected int x;          // centre X position on screen (pixels)
    protected int y;          // centre Y position on screen (pixels)
    protected int size;       // diameter / bounding size in pixels

    // ── Timing ───────────────────────────────────────────────
    protected long spawnTime; // System.currentTimeMillis() when this target was created
    protected long lifetime;  // how many milliseconds this target lives before expiring

    // ── State ────────────────────────────────────────────────
    protected boolean hit = false;  // true once the player has successfully clicked this target

    // ── Animation ────────────────────────────────────────────
    // pulsePhase advances each frame and is passed into Math.sin()
    // to create a smooth pulsing glow effect in subclass draw() methods
    protected float pulsePhase = 0f;

    /**
     * Constructor — called by every subclass via super(x, y, size, lifetime).
     *
     * @param x        Centre X position on screen
     * @param y        Centre Y position on screen
     * @param size     Diameter of the target in pixels
     * @param lifetime How long (ms) before this target disappears on its own
     */
    public Target(int x, int y, int size, long lifetime) {
        this.x         = x;
        this.y         = y;
        this.size      = size;
        this.lifetime  = lifetime;
        this.spawnTime = System.currentTimeMillis(); // record exactly when it was created
    }

    // ── Getters ───────────────────────────────────────────────
    // Simple accessors — used by GamePanel and ExplosionEffect

    public int getX()    { return x;    }
    public int getY()    { return y;    }
    public int getSize() { return size; }

    /**
     * Returns how much life this target has remaining as a fraction from 0.0 to 1.0.
     *   1.0 = just spawned (full life)
     *   0.5 = halfway through its lifetime
     *   0.0 = expired (should be removed)
     *
     * Used by subclasses to fade out visually and by GamePanel to check expiry.
     * Math.max(0f, ...) prevents it from going negative if a frame is late.
     */
    public float getLifeFraction() {
        long elapsed = System.currentTimeMillis() - spawnTime; // ms since spawn
        return Math.max(0f, 1f - (float) elapsed / lifetime);
    }

    /**
     * Returns true when getLifeFraction() reaches zero.
     * GamePanel calls this every tick to decide whether to remove the target.
     */
    public boolean isExpired() {
        return getLifeFraction() <= 0f;
    }

    /**
     * Returns true if the player has already clicked this target.
     * GamePanel uses this to skip already-hit targets during rendering.
     */
    public boolean isHit() {
        return hit;
    }

    /**
     * Marks this target as hit.
     * Called by GamePanel.mouseClicked() when the player clicks inside it.
     */
    public void markHit() {
        this.hit = true;
    }

    // ── Shared helper ─────────────────────────────────────────

    /**
     * Linearly interpolates (blends) between two Colors.
     * Used by subclasses to smoothly shift color as the target nears expiry.
     *
     * Example: lerpColor(RED, YELLOW, 0.5f) → orange
     *
     * @param a  Starting color (t = 0.0)
     * @param b  Ending color   (t = 1.0)
     * @param t  Blend factor, clamped to [0.0, 1.0]
     * @return   Blended Color
     */
    protected Color lerpColor(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t)); // clamp so we never go out of range
        int r  = (int)(a.getRed()   + t * (b.getRed()   - a.getRed()));
        int g  = (int)(a.getGreen() + t * (b.getGreen() - a.getGreen()));
        int bv = (int)(a.getBlue()  + t * (b.getBlue()  - a.getBlue()));
        return new Color(r, g, bv);
    }

    // ── Abstract methods — must be implemented by every subclass ──

    /**
     * Returns true if the screen coordinate (mx, my) is inside this target.
     * Each shape calculates this differently:
     *   - Circle uses distance formula
     *   - Diamond and Triangle use Polygon.contains()
     *
     * @param mx  Mouse X coordinate when clicked
     * @param my  Mouse Y coordinate when clicked
     */
    public abstract boolean contains(int mx, int my);

    /**
     * Draws this target onto the game panel.
     * Called every frame (~60 times per second) by GamePanel.paintComponent().
     * Each subclass defines its own shape, colours, and animations.
     *
     * @param g2  The Graphics2D context provided by Swing
     */
    public abstract void draw(Graphics2D g2);

    /**
     * Returns the point value for hitting this target.
     * Typically higher when the target is small or nearly expired.
     */
    public abstract int getPoints();
}
