import java.awt.*;

/**
 * ─────────────────────────────────────────────────────────────
 * HitEffect.java — Abstract Base Class
 * ─────────────────────────────────────────────────────────────
 *
 * The parent class for all visual effects that play when a target is hit.
 * Cannot be instantiated directly — you must use a subclass.
 *
 * Current subclasses:
 *   - FloatingTextEffect  → "+pts" text that floats upward and fades out
 *   - ExplosionEffect     → expanding rings and flying particles
 *
 * To add a new effect, simply extend this class and implement draw().
 *
 * OOP concept: INHERITANCE + ABSTRACTION
 * Mirrors the same pattern as Target / CircleTarget / DiamondTarget.
 */
public abstract class HitEffect {

    protected int  x;          // X position where the effect plays (pixels)
    protected int  y;          // Y position where the effect plays (pixels)
    protected long createdAt;  // System.currentTimeMillis() when this effect was created
    protected long duration;   // how many milliseconds the effect lasts before removal

    /**
     * Constructor — called by every subclass via super(x, y, duration).
     *
     * @param x        X position on screen
     * @param y        Y position on screen
     * @param duration How long (ms) this effect should last
     */
    public HitEffect(int x, int y, long duration) {
        this.x         = x;
        this.y         = y;
        this.duration  = duration;
        this.createdAt = System.currentTimeMillis(); // record creation time
    }

    /**
     * Returns how far through its lifetime this effect is, from 0.0 to 1.0.
     *   0.0 = just created (beginning of animation)
     *   0.5 = halfway through
     *   1.0 = fully expired (should be removed)
     *
     * Math.min(1f, ...) prevents the value from going above 1 if a frame is late.
     * Subclasses use this to drive their fade-out and movement animations.
     */
    public float getProgress() {
        return Math.min(1f, (System.currentTimeMillis() - createdAt) / (float) duration);
    }

    /**
     * Returns true when the effect has finished animating and should be removed.
     * GamePanel calls effects.removeIf(HitEffect::isExpired) every frame.
     */
    public boolean isExpired() {
        return getProgress() >= 1f;
    }

    /**
     * Draws this effect onto the screen.
     * Called every frame by GamePanel while the effect is alive.
     * Each subclass defines its own unique visual animation.
     *
     * @param g2  Swing's 2D graphics context
     */
    public abstract void draw(Graphics2D g2);
}
