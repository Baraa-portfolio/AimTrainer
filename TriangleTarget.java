import java.awt.*;

/**
 * ─────────────────────────────────────────────────────────────
 * TriangleTarget.java — Concrete Subclass of Target
 * ─────────────────────────────────────────────────────────────
 *
 * The rarest target type (15% spawn chance) and highest point value.
 * Drawn as an equilateral triangle using a 3-point Polygon.
 * Visual feature: colour continuously pulses between purple and gold
 * using a sine wave, making it visually distinct from other targets.
 *
 * Visual layers:
 *   - Semi-transparent glow fill
 *   - Pulsing colour fill
 *   - White border outline
 *   - White centre dot
 *
 * OOP concept: INHERITANCE
 * Extends Target and provides concrete implementations for
 * contains(), draw(), and getPoints().
 */
public class TriangleTarget extends Target {

    // ── Colours ───────────────────────────────────────────────
    // The triangle pulses between these two colours using Math.sin()
    private static final Color COLOR_A = new Color(180,  80, 255); // purple
    private static final Color COLOR_B = new Color(255, 180,  50); // gold / orange

    /**
     * Constructor — passes all arguments up to Target via super().
     *
     * @param x        Centre X on screen
     * @param y        Centre Y on screen
     * @param size     Bounding width of the triangle in pixels
     * @param lifetime Milliseconds before auto-expiry
     */
    public TriangleTarget(int x, int y, int size, long lifetime) {
        super(x, y, size, lifetime);
    }

    /**
     * Builds a 3-point Polygon for an equilateral triangle, centred on (x, y).
     *
     * Height of an equilateral triangle = size * (√3 / 2) ≈ size * 0.87
     * We halve that to get the distance from centre to top and centre to bottom.
     *
     *         (x, y-h)          ← top vertex
     *        /         \
     *  (x-w, y+h)   (x+w, y+h) ← bottom-left, bottom-right
     *
     * The small -2 offset on Y pushes the triangle up slightly so it
     * looks visually centred within its bounding area.
     */
    private Polygon getTriangle() {
        int h = (int)(size * 0.87f / 2); // half-height of the equilateral triangle
        return new Polygon(
            new int[]{ x,         x + size / 2, x - size / 2 }, // X: top, bottom-right, bottom-left
            new int[]{ y - h - 2, y + h - 2,    y + h - 2    }, // Y: top, bottom-right, bottom-left
            3                                                     // number of points
        );
    }

    /**
     * Hit detection — delegates to Java's built-in Polygon.contains().
     *
     * @param mx  Mouse X at click time
     * @param my  Mouse Y at click time
     * @return    true if the click is inside the triangle polygon
     */
    @Override
    public boolean contains(int mx, int my) {
        return getTriangle().contains(mx, my);
    }

    /**
     * Draws the triangle target onto the screen.
     *
     * The colour continuously oscillates between COLOR_A (purple) and COLOR_B (gold)
     * using Math.sin(pulsePhase). sin() returns values between -1 and +1,
     * so we rescale it to 0..1 with: (sin + 1) / 2 = sin * 0.5 + 0.5
     *
     * Drawing order:
     *   1. Dim glow fill (low opacity)
     *   2. Main colour fill (90% opacity)
     *   3. White border outline
     *   4. White centre dot
     *
     * @param g2  Swing's 2D graphics context
     */
    @Override
    public void draw(Graphics2D g2) {
        float life  = getLifeFraction();  // 1.0 = fresh, 0.0 = expired
        float alpha = Math.max(0f, life); // fade out as time runs out
        pulsePhase += 0.15f;              // faster pulse than other targets — makes it feel urgent

        // Compute current pulsing colour: oscillates between COLOR_A and COLOR_B
        // Math.sin(pulsePhase) ranges -1..1 → rescale to 0..1 for lerpColor
        Color c     = lerpColor(COLOR_A, COLOR_B, (float)(Math.sin(pulsePhase) * 0.5 + 0.5));
        Polygon tri = getTriangle(); // build the triangle polygon at current position

        // ── 1. Glow fill ──────────────────────────────────────
        // Very low opacity (20%) — gives a subtle halo behind the main shape
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * 0.2f));
        g2.setColor(c);
        g2.fillPolygon(tri);

        // ── 2. Main fill ──────────────────────────────────────
        // The pulsing colour drawn at 90% opacity on top of the glow
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * 0.9f));
        g2.setColor(c);
        g2.fillPolygon(tri);

        // ── 3. Border outline ─────────────────────────────────
        // White outline makes the edges crisp and readable on dark background
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2.setColor(new Color(255, 255, 255, 200)); // near-white, slightly transparent
        g2.setStroke(new BasicStroke(2f));
        g2.drawPolygon(tri);

        // ── 4. Centre dot ─────────────────────────────────────
        g2.setColor(Color.WHITE);
        g2.fillOval(x - 3, y - 3, 6, 6); // 6x6 dot at exact centre

        // Reset composite to fully opaque
        g2.setComposite(AlphaComposite.SrcOver);
    }

    /**
     * Calculates point value for hitting this triangle target.
     *
     * Formula: 500 - size + (150 * lifeFraction)
     *   - Base value of 500 — highest of all three target types
     *   - Smaller size = more points
     *   - Early hit adds a time bonus up to +150
     *   - Minimum of 25 points guaranteed
     */
    @Override
    public int getPoints() {
        return Math.max(25, 500 - size + (int)(150 * getLifeFraction()));
    }
}
