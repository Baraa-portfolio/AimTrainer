import java.awt.*;

/**
 * ─────────────────────────────────────────────────────────────
 * DiamondTarget.java — Concrete Subclass of Target
 * ─────────────────────────────────────────────────────────────
 *
 * An uncommon target type (25% spawn chance).
 * Drawn as a rotated square (diamond / rhombus) using a 4-point Polygon.
 * Visual layers:
 *   - A semi-transparent glow fill
 *   - A blue gradient fill that shifts toward red as it nears expiry
 *   - A light-blue border outline
 *   - A white centre dot
 *
 * OOP concept: INHERITANCE
 * Extends Target and provides concrete implementations for
 * contains(), draw(), and getPoints().
 */
public class DiamondTarget extends Target {

    // ── Colours ───────────────────────────────────────────────
    private static final Color COLOR_A = new Color(80,  200, 255); // bright blue — fresh
    private static final Color COLOR_B = new Color(20,  100, 220); // darker blue — used for gradient bottom

    /**
     * Constructor — passes all arguments up to Target via super().
     *
     * @param x        Centre X on screen
     * @param y        Centre Y on screen
     * @param size     Bounding size (tip-to-tip distance) in pixels
     * @param lifetime Milliseconds before auto-expiry
     */
    public DiamondTarget(int x, int y, int size, long lifetime) {
        super(x, y, size, lifetime);
    }

    /**
     * Builds a 4-point Polygon in the shape of a diamond (rotated square).
     * The four points are the top, right, bottom, and left tips.
     *
     * Polygon layout (h = size / 2):
     *
     *        (x, y-h)       ← top tip
     *       /        \
     *  (x-h, y)    (x+h, y) ← left and right tips
     *       \        /
     *        (x, y+h)       ← bottom tip
     *
     * A new Polygon is built each call so position stays accurate
     * if x/y ever change.
     */
    private Polygon getDiamond() {
        int h = size / 2; // half-size = distance from centre to each tip
        return new Polygon(
            new int[]{ x,     x + h, x,     x - h }, // X coords: top, right, bottom, left
            new int[]{ y - h, y,     y + h, y     }, // Y coords: top, right, bottom, left
            4                                         // number of points
        );
    }

    /**
     * Hit detection — delegates to Java's built-in Polygon.contains().
     * Polygon.contains() uses a ray-casting algorithm internally.
     *
     * @param mx  Mouse X at click time
     * @param my  Mouse Y at click time
     * @return    true if the click is inside the diamond polygon
     */
    @Override
    public boolean contains(int mx, int my) {
        return getDiamond().contains(mx, my);
    }

    /**
     * Draws the diamond target onto the screen.
     *
     * Drawing order:
     *   1. Semi-transparent glow fill (same polygon, low opacity)
     *   2. Gradient fill (blue top to darker blue bottom)
     *   3. Light-blue border outline
     *   4. White centre dot
     *
     * @param g2  Swing's 2D graphics context
     */
    @Override
    public void draw(Graphics2D g2) {
        float life   = getLifeFraction();  // 1.0 = fresh, 0.0 = expired
        float alpha  = Math.max(0f, life); // fade out as time runs out
        pulsePhase  += 0.1f;               // advance animation timer

        // Colour shifts from blue (fresh) to red (about to expire)
        Color c      = lerpColor(COLOR_A, new Color(255, 80, 80), 1f - life);
        Polygon poly = getDiamond(); // build the diamond polygon at current position

        // ── 1. Glow fill ──────────────────────────────────────
        // Drawn at 25% opacity — soft background halo effect
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * 0.25f));
        g2.setColor(c);
        g2.fillPolygon(poly);

        // ── 2. Gradient fill ──────────────────────────────────
        // Vertical gradient from bright blue (top) to darker blue (bottom)
        // 85% opacity so slight transparency makes it feel less flat
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * 0.85f));
        g2.setPaint(new GradientPaint(
            x, y - size / 2, COLOR_A, // gradient starts at the top tip
            x, y + size / 2, COLOR_B  // gradient ends at the bottom tip
        ));
        g2.fillPolygon(poly);

        // ── 3. Border outline ─────────────────────────────────
        // Light-blue border drawn at full opacity on top of the fill
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2.setColor(new Color(180, 240, 255, 200)); // light cyan, slightly transparent
        g2.setStroke(new BasicStroke(2f));
        g2.drawPolygon(poly);

        // ── 4. Centre dot ─────────────────────────────────────
        // Small white dot at the exact centre as an aiming reference
        g2.setColor(Color.WHITE);
        g2.fillOval(x - 3, y - 3, 6, 6); // 6x6 pixel dot centred on (x, y)

        // Reset composite to fully opaque
        g2.setComposite(AlphaComposite.SrcOver);
    }

    /**
     * Calculates point value for hitting this diamond target.
     *
     * Formula: 300 - size + (80 * lifeFraction)
     *   - Base value of 300 is higher than CircleTarget (reward for harder hit)
     *   - Smaller size = more points
     *   - Early hit adds a time bonus up to +80
     *   - Minimum of 15 points guaranteed
     */
    @Override
    public int getPoints() {
        return Math.max(15, 300 - size + (int)(80 * getLifeFraction()));
    }
}
