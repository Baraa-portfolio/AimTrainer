import java.awt.*;

/**
 * ─────────────────────────────────────────────────────────────
 * CircleTarget.java — Concrete Subclass of Target
 * ─────────────────────────────────────────────────────────────
 *
 * The most common target type (60% spawn chance).
 * Drawn as a filled circle with:
 *   - A pulsing outer glow
 *   - A gradient fill that shifts from red → yellow as it nears expiry
 *   - An inner ring for visual depth
 *   - A white centre dot as an aiming reference
 *   - A white arc around the border that shrinks as lifetime runs out
 *
 * OOP concept: INHERITANCE
 * Extends Target and provides concrete implementations for
 * contains(), draw(), and getPoints().
 */
public class CircleTarget extends Target {

    // ── Colours ───────────────────────────────────────────────
    // static final = constant, shared across all CircleTarget instances
    private static final Color COLOR_FRESH  = new Color(255, 60,  60);   // bright red when freshly spawned
    private static final Color COLOR_EXPIRY = new Color(255, 200,  0);   // yellow when about to expire
    private static final Color COLOR_CENTER = new Color(255, 255, 255, 220); // white centre dot (slightly transparent)
    private static final Color COLOR_RING   = new Color(255, 255, 255, 100); // white inner ring (semi-transparent)

    /**
     * Constructor — passes all arguments up to Target via super().
     *
     * @param x        Centre X on screen
     * @param y        Centre Y on screen
     * @param size     Diameter in pixels
     * @param lifetime Milliseconds before auto-expiry
     */
    public CircleTarget(int x, int y, int size, long lifetime) {
        super(x, y, size, lifetime);
    }

    /**
     * Hit detection for a circle.
     * Uses the standard distance formula: dx² + dy² <= r²
     * If the squared distance from the click to the centre is within
     * the squared radius, the click is inside the circle.
     *
     * Squared values are used to avoid an expensive Math.sqrt() call.
     *
     * @param mx  Mouse X at click time
     * @param my  Mouse Y at click time
     * @return    true if the click is inside the circle
     */
    @Override
    public boolean contains(int mx, int my) {
        int dx     = mx - x;          // horizontal distance from click to centre
        int dy     = my - y;          // vertical distance from click to centre
        int radius = size / 2;        // radius = half the diameter
        return dx * dx + dy * dy <= radius * radius; // Pythagoras, no sqrt needed
    }

    /**
     * Draws the circle target onto the screen.
     * Called every frame by GamePanel — must be fast.
     *
     * Drawing order (painter's algorithm — back to front):
     *   1. Outer glow (large, transparent oval)
     *   2. Main gradient fill
     *   3. Inner ring
     *   4. Centre dot
     *   5. Lifetime arc (white arc that shrinks as time runs out)
     *
     * @param g2  Swing's 2D graphics context
     */
    @Override
    public void draw(Graphics2D g2) {
        float life  = getLifeFraction();   // 1.0 = fresh, 0.0 = expired
        float alpha = Math.max(0f, life);  // alpha matches life — fades out naturally
        pulsePhase += 0.12f;               // advance the animation timer each frame

        // Colour shifts from red (fresh) to yellow (about to expire)
        Color outerColor = lerpColor(COLOR_FRESH, COLOR_EXPIRY, 1f - life);

        // glowSize oscillates slightly using sin() to create a pulsing effect
        float glowSize = size + 6 + (float)(Math.sin(pulsePhase) * 3);

        // ── 1. Outer glow ─────────────────────────────────────
        // Drawn at 30% opacity — creates a soft halo around the target
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * 0.3f));
        g2.setColor(outerColor);
        g2.fillOval(
            (int)(x - glowSize / 2), (int)(y - glowSize / 2), // top-left corner of bounding box
            (int) glowSize, (int) glowSize                     // width and height
        );

        // ── 2. Main gradient fill ─────────────────────────────
        // GradientPaint creates a diagonal colour fade from top-left to bottom-right
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        GradientPaint grad = new GradientPaint(
            x - size / 2, y - size / 2, outerColor,                  // start: bright
            x + size / 2, y + size / 2, outerColor.darker().darker() // end: darker shade
        );
        g2.setPaint(grad);
        g2.fillOval(x - size / 2, y - size / 2, size, size);

        // ── 3. Inner ring ─────────────────────────────────────
        // A smaller circle drawn inside — half the radius — for visual depth
        g2.setColor(COLOR_RING);
        g2.setStroke(new BasicStroke(1.5f));
        int r = size / 4; // inner ring radius = quarter of the diameter
        g2.drawOval(x - r, y - r, r * 2, r * 2);

        // ── 4. Centre dot ─────────────────────────────────────
        // Small white dot at the exact centre — gives the player an aiming point
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2.setColor(COLOR_CENTER);
        g2.fillOval(x - 4, y - 4, 8, 8); // 8x8 pixel dot centred on (x, y)

        // ── 5. Lifetime arc ───────────────────────────────────
        // A white arc drawn around the border, starting at the top (90°) and
        // sweeping clockwise. Its angle shrinks proportionally with remaining life:
        //   Full life  → 360° arc (complete circle)
        //   Half life  → 180° arc
        //   No life    → 0° arc (invisible)
        g2.setColor(new Color(255, 255, 255, 180));
        g2.setStroke(new BasicStroke(2f));
        int arcAngle = (int)(360 * life); // how many degrees to draw
        g2.drawArc(x - size / 2, y - size / 2, size, size, 90, -arcAngle);

        // Reset composite to fully opaque so other drawing is not affected
        g2.setComposite(AlphaComposite.SrcOver);
    }

    /**
     * Calculates point value for hitting this target.
     *
     * Formula: 200 - size + (100 * lifeFraction)
     *   - Smaller targets are worth more (lower size = higher points)
     *   - Hitting early (high lifeFraction) adds a time bonus up to +100
     *   - Math.max(10, ...) ensures a minimum of 10 points
     *
     * Example: size=30, hit at 80% life → 200 - 30 + 80 = 250 pts
     */
    @Override
    public int getPoints() {
        return Math.max(10, 200 - size + (int)(100 * getLifeFraction()));
    }
}
