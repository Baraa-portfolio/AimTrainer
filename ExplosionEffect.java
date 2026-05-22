import java.awt.*;

/**
 * ─────────────────────────────────────────────────────────────
 * ExplosionEffect.java — Concrete Subclass of HitEffect
 * ─────────────────────────────────────────────────────────────
 *
 * Plays a burst animation at the target's centre when it is hit:
 *   - Two expanding rings (outer full-size, inner 60% size)
 *   - 8 particle dots that fly outward along the expanding rings
 *   - All elements fade out as they expand, over 450 milliseconds
 *
 * The explosion is centred on the TARGET (not the mouse click),
 * so it always appears to come from the shape itself.
 *
 * OOP concept: INHERITANCE
 * Extends HitEffect and implements draw() with ring/particle animation.
 */
public class ExplosionEffect extends HitEffect {

    private final Color color;          // accent colour matching the current difficulty
    private final int   maxRadius;      // maximum radius the rings expand to (= target size)
    private static final int NUM_PARTICLES = 8; // number of dots flying outward

    /**
     * Constructor.
     *
     * @param x          Centre X of the target that was hit
     * @param y          Centre Y of the target that was hit
     * @param color      Colour for rings and particles (from difficulty.color)
     * @param targetSize The hit target's size — rings expand to this radius
     */
    public ExplosionEffect(int x, int y, Color color, int targetSize) {
        super(x, y, 450); // lasts 450 milliseconds — snappy burst
        this.color     = color;
        this.maxRadius = targetSize;
    }

    /**
     * Draws the explosion animation.
     *
     * Animation breakdown (t = progress, 0.0 → 1.0):
     *   r       = t * maxRadius      → rings grow from 0 to maxRadius
     *   r2      = t * maxRadius*0.6  → inner ring grows slower (60% size)
     *   alpha   = 1 - t              → everything fades out as it expands
     *   dotSize = 7 * (1 - t) + 2   → particles shrink as they fly outward
     *
     * Particles are positioned by distributing NUM_PARTICLES evenly around
     * a circle using trigonometry:
     *   angle = (2π / NUM_PARTICLES) * i   → evenly spaced angles
     *   px    = x + cos(angle) * r         → X on the ring
     *   py    = y + sin(angle) * r         → Y on the ring
     *
     * @param g2  Swing's 2D graphics context
     */
    @Override
    public void draw(Graphics2D g2) {
        float t     = getProgress();           // 0.0 = just created, 1.0 = done
        float alpha = Math.max(0f, 1f - t);   // fade out over lifetime
        int   r     = (int)(t * maxRadius);   // current radius of the outer ring

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

        // ── Outer expanding ring ──────────────────────────────
        // Grows from 0 to maxRadius pixels in diameter
        g2.setColor(color);
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawOval(x - r, y - r, r * 2, r * 2); // drawOval takes top-left x/y, width, height

        // ── Inner expanding ring ──────────────────────────────
        // Grows to 60% of the outer ring — creates a layered depth effect
        // Slightly brighter colour to distinguish it from the outer ring
        int r2 = (int)(t * maxRadius * 0.6f);
        g2.setColor(new Color(
            Math.min(255, color.getRed()   + 60), // brighten each channel by 60
            Math.min(255, color.getGreen() + 60), // Math.min(255, ...) prevents overflow
            Math.min(255, color.getBlue()  + 60)
        ));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawOval(x - r2, y - r2, r2 * 2, r2 * 2);

        // ── Particles ─────────────────────────────────────────
        // NUM_PARTICLES dots placed evenly around the outer ring
        // They follow the ring as it expands, and shrink over time
        int dotSize = Math.max(2, (int)(7 * (1f - t))); // 7px → 2px as t approaches 1

        for (int i = 0; i < NUM_PARTICLES; i++) {
            // Divide the full circle (2π radians) evenly among particles
            double angle = (2 * Math.PI / NUM_PARTICLES) * i;

            // Position on the ring using trigonometry
            int px = x + (int)(Math.cos(angle) * r); // cos gives X component
            int py = y + (int)(Math.sin(angle) * r); // sin gives Y component

            g2.setColor(color);
            // Centre the dot on (px, py) by subtracting half the dot size
            g2.fillOval(px - dotSize / 2, py - dotSize / 2, dotSize, dotSize);
        }

        // Reset stroke and composite so other drawing is not affected
        g2.setComposite(AlphaComposite.SrcOver);
        g2.setStroke(new BasicStroke(1f));
    }
}
