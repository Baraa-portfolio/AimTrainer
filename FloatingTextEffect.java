import java.awt.*;

/**
 * ─────────────────────────────────────────────────────────────
 * FloatingTextEffect.java — Concrete Subclass of HitEffect
 * ─────────────────────────────────────────────────────────────
 *
 * Displays "+pts" text at the click location that:
 *   - Floats upward over 750 milliseconds
 *   - Fades out as it rises
 *   - Has a dark drop shadow for readability on any background
 *
 * Example: clicking a target worth 320 points shows "+320"
 *
 * OOP concept: INHERITANCE
 * Extends HitEffect and implements draw() with text-specific animation.
 */
public class FloatingTextEffect extends HitEffect {

    private final String text;  // the "+pts" string to display (e.g. "+320")
    private final Color  color; // accent colour matching the current difficulty

    /**
     * Constructor.
     *
     * @param x      X position where the text appears (mouse click X)
     * @param y      Y position where the text appears (mouse click Y)
     * @param points Point value to display (shown as "+points")
     * @param color  Text colour — passed in from difficulty.color
     */
    public FloatingTextEffect(int x, int y, int points, Color color) {
        super(x, y, 750); // lasts 750 milliseconds
        this.text  = "+" + points;
        this.color = color;
    }

    /**
     * Draws the floating score text.
     *
     * Animation breakdown:
     *   t (progress) goes from 0.0 → 1.0 over 750ms
     *   alpha = 1 - t          → starts opaque, fades to invisible
     *   offsetY = t * -50      → moves 50 pixels upward (negative Y = up in Swing)
     *
     * The drop shadow is drawn 2px down and right at 47% opacity,
     * then the coloured text is drawn on top at full opacity.
     *
     * @param g2  Swing's 2D graphics context
     */
    @Override
    public void draw(Graphics2D g2) {
        float t       = getProgress();           // 0.0 = just created, 1.0 = done
        float alpha   = Math.max(0f, 1f - t);   // fade out: 1.0 → 0.0 over lifetime
        int   offsetY = (int)(t * -50);          // float upward: 0 → -50 pixels

        // Set transparency for both shadow and text
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2.setFont(new Font("Monospaced", Font.BOLD, 22));

        // Measure text width so we can centre it horizontally on (x, y)
        FontMetrics fm = g2.getFontMetrics();
        int tx = x - fm.stringWidth(text) / 2; // left edge so text is centred on x
        int ty = y + offsetY;                   // current Y position (moves upward)

        // ── Drop shadow ───────────────────────────────────────
        // Drawn at ~47% of the current alpha, offset 2px down-right
        // Makes the text readable whether the background is light or dark
        g2.setColor(new Color(0, 0, 0, 120));
        g2.drawString(text, tx + 2, ty + 2);

        // ── Main coloured text ────────────────────────────────
        g2.setColor(color);
        g2.drawString(text, tx, ty);

        // Reset composite to fully opaque so other drawing is not affected
        g2.setComposite(AlphaComposite.SrcOver);
    }
}
