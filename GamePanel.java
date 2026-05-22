import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * ─────────────────────────────────────────────────────────────
 * GamePanel.java — Main Game Panel
 * ─────────────────────────────────────────────────────────────
 *
 * The core Swing panel that runs the game.
 * Extends JPanel (so Swing can render it) and implements MouseListener
 * (so it can receive click events).
 *
 * Responsibilities:
 *   1. GAME LOOP — a javax.swing.Timer fires tick() ~60 times per second
 *   2. SPAWNING  — adds new Target objects at random positions
 *   3. RENDERING — paintComponent() draws everything each frame
 *   4. INPUT     — mouseClicked() handles hit detection and miss counting
 *   5. SCORES    — saves high scores via UserManager at round end
 *
 * OOP concept: COMPOSITION + POLYMORPHISM
 * GamePanel holds a List<Target> — it doesn't know or care whether each
 * entry is a Circle, Diamond, or Triangle. It just calls t.draw() and
 * t.contains() — polymorphism handles the rest.
 * Same pattern applies to List<HitEffect>.
 */
public class GamePanel extends JPanel implements MouseListener {

    // ── Core state objects ────────────────────────────────────

    // GameState holds score, hits, misses, and the timer — no rendering
    private final GameState state = new GameState();

    // All currently active targets on screen
    // List<Target> uses the abstract type — can hold any subclass
    private final List<Target> targets = new ArrayList<>();

    // All currently playing hit effects (explosions, floating text)
    // Same polymorphism pattern as targets
    private final List<HitEffect> effects = new ArrayList<>();

    // Shared random number generator used for spawn position, size, lifetime, and shape roll
    private final Random rng = new Random();

    // ── Settings ──────────────────────────────────────────────

    private Difficulty difficulty = Difficulty.MEDIUM; // active difficulty level
    private long       lastSpawn  = 0;                 // timestamp of last spawn (ms)

    // ── Player info ───────────────────────────────────────────

    private String  username  = "Player"; // set by AimTrainer after login
    private int     highScore = 0;        // loaded from scores.txt via UserManager
    private boolean newRecord = false;    // true if the most recent round beat the high score

    // ── Mouse position ────────────────────────────────────────
    // Updated by MouseMotionAdapter every time the cursor moves
    // protected so a subclass could use it (e.g. to draw a custom crosshair)
    protected int mouseX = 0;
    protected int mouseY = 0;

    // ── Colour constants ──────────────────────────────────────
    private static final Color BG_DARK = new Color( 10,  12,  20); // main background
    private static final Color BG_GRID = new Color( 20,  25,  40); // dot-grid colour
    private static final Color HUD_FG  = new Color(200, 210, 255); // HUD text colour
    private static final Color ACCENT  = new Color( 80, 180, 255); // blue — timer bar, title
    private static final Color GOLD    = new Color(255, 210,  60); // gold — high score display
    private static final Color GREEN   = new Color( 80, 200, 120); // green (reserved for future use)
    private static final Color RED     = new Color(255,  80,  80); // red — low-time warning

    // ── Constructor ───────────────────────────────────────────

    /**
     * Sets up the panel, registers input listeners, and starts the game loop timer.
     */
    public GamePanel() {
        setBackground(BG_DARK);

        // Use the system crosshair cursor instead of the default arrow
        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

        // Register click listener (this class implements MouseListener)
        addMouseListener(this);

        // Track mouse position every time the cursor moves or is dragged
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e)   { mouseX = e.getX(); mouseY = e.getY(); }
            @Override public void mouseDragged(MouseEvent e) { mouseX = e.getX(); mouseY = e.getY(); }
        });

        // javax.swing.Timer fires on the Swing Event Dispatch Thread (safe for UI updates)
        // 16ms ≈ 62.5 fps — smooth enough for this game
        new javax.swing.Timer(16, e -> tick()).start();
    }

    // ── Public API ────────────────────────────────────────────

    /** Changes the active difficulty. Called by AimTrainer when a toolbar button is clicked. */
    public void setDifficulty(Difficulty d) { this.difficulty = d; }

    /** Returns the current GameState — used if AimTrainer ever needs to read score data. */
    public GameState getGameState() { return state; }

    /**
     * Sets the logged-in player's username and immediately loads their high score.
     * Called by AimTrainer.showGameScreen() right after the panel is created.
     *
     * @param username  The username returned by the login screen
     */
    public void setUsername(String username) {
        this.username  = username;
        this.highScore = UserManager.getHighScore(username); // pre-load best score from file
    }

    // ── Game loop ─────────────────────────────────────────────

    /**
     * Called ~60 times per second by the javax.swing.Timer.
     * Checks if the round is still going, then either ends it or advances the game.
     * Always ends with repaint() to schedule a fresh frame.
     */
    private void tick() {
        if (state.isActive()) {
            if (state.isTimeUp()) {
                endRound(); // 30 seconds elapsed — finish the round
            } else {
                spawnTargets();    // maybe spawn a new target this frame
                removeDeadTargets(); // clean up expired and hit targets
            }
        }
        repaint(); // tell Swing to call paintComponent() again
    }

    /**
     * Ends the current round:
     *   1. Stops the game loop logic (state.endGame sets active = false)
     *   2. Clears any remaining targets from the screen
     *   3. Saves the score if it's a new personal best
     *   4. Reloads the high score from file so the result screen is current
     */
    private void endRound() {
        state.endGame();   // mark round as inactive
        targets.clear();   // remove all remaining targets

        // saveScoreIfBest returns true only if this round beat the stored record
        newRecord = UserManager.saveScoreIfBest(username, state.getScore());

        // Reload from file in case saveScoreIfBest updated it
        highScore = UserManager.getHighScore(username);
    }

    /**
     * Spawns a new target if enough time has passed since the last spawn.
     * The gap is controlled by difficulty.spawnInterval.
     *
     * Spawn position is random within a margin from the edges so targets
     * never appear behind the HUD bar or partially off-screen.
     *
     * Shape probabilities:
     *   0–59  (60%) → CircleTarget   — most common
     *   60–84 (25%) → DiamondTarget  — uncommon
     *   85–99 (15%) → TriangleTarget — rare
     */
    private void spawnTargets() {
        long now = System.currentTimeMillis();
        if (now - lastSpawn < difficulty.spawnInterval) return; // too soon — wait
        lastSpawn = now;

        int margin = 80; // minimum distance from window edges (pixels)
        int px = margin + rng.nextInt(Math.max(1, getWidth()  - 2 * margin));
        int py = margin + rng.nextInt(Math.max(1, getHeight() - 2 * margin - 80));
        int sz = difficulty.randomSize(rng);       // random size within difficulty range
        long lt = difficulty.randomLifetime(rng);  // random lifetime within difficulty range

        // Roll a number 0–99 to decide which shape to spawn
        int roll = rng.nextInt(100);
        Target t;
        if      (roll < 60) t = new CircleTarget  (px, py, sz, lt); // 60%
        else if (roll < 85) t = new DiamondTarget (px, py, sz, lt); // 25%
        else                t = new TriangleTarget(px, py, sz, lt); // 15%

        targets.add(t); // add to the active targets list
    }

    /**
     * Removes targets that are no longer needed:
     *   - isHit()     → player clicked it; remove immediately after markHit()
     *   - isExpired() → lifetime ran out; increment expired counter then remove
     *
     * Uses an Iterator (not a for-each loop) because we modify the list while iterating.
     * Removing from a list inside a for-each loop throws ConcurrentModificationException.
     */
    private void removeDeadTargets() {
        Iterator<Target> it = targets.iterator();
        while (it.hasNext()) {
            Target t = it.next();
            if (t.isHit()) {
                it.remove(); // already handled — just remove
            } else if (t.isExpired()) {
                state.addExpired(); // count as a missed opportunity
                it.remove();
            }
        }
    }

    // ── Rendering ─────────────────────────────────────────────

    /**
     * The main drawing method — called by Swing every time repaint() is requested.
     *
     * Always:
     *   1. Call super.paintComponent(g) — clears the previous frame
     *   2. Create a Graphics2D copy (g.create()) — prevents state from leaking between calls
     *   3. Enable anti-aliasing for smooth shapes and text
     *   4. Draw background
     *   5. Draw the correct screen based on game state
     *   6. Dispose of the Graphics2D copy to free resources
     *
     * Three possible screens:
     *   - Active game  → targets + effects + HUD
     *   - Start screen → shown before the first round (score and hits both 0)
     *   - Result screen → shown after a round ends
     *
     * @param g  Swing's graphics context, cast to Graphics2D for advanced features
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // clear the panel with its background colour
        Graphics2D g2 = (Graphics2D) g.create(); // copy so we don't modify the original

        // Smooth out jagged edges on shapes and text
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawBackground(g2); // always drawn regardless of state

        if (state.isActive()) {
            // ── Active round ──────────────────────────────────
            for (Target t : targets) t.draw(g2);      // draw every active target
            for (HitEffect ef : effects) ef.draw(g2); // draw every active hit effect
            effects.removeIf(HitEffect::isExpired);   // clean up finished effects
            drawHUD(g2);                               // draw score bar on top

        } else if (state.getScore() == 0 && state.getHits() == 0) {
            // ── Start screen ──────────────────────────────────
            // Both score and hits are 0 → game has never been played this session
            drawStartScreen(g2);

        } else {
            // ── Result screen ─────────────────────────────────
            // Round ended — show stats and high score
            drawResultScreen(g2);
        }

        g2.dispose(); // release the Graphics2D copy
    }

    /**
     * Draws the dark background and the subtle dot grid.
     * Called every frame before anything else so it's always behind all other elements.
     */
    private void drawBackground(Graphics2D g2) {
        g2.setColor(BG_DARK);
        g2.fillRect(0, 0, getWidth(), getHeight()); // solid dark fill

        // Dot grid — small 2x2 dots every 40 pixels in both directions
        g2.setColor(BG_GRID);
        for (int x = 20; x < getWidth();  x += 40)
            for (int y = 20; y < getHeight(); y += 40)
                g2.fillOval(x - 1, y - 1, 2, 2);
    }

    /**
     * Draws the heads-up display (HUD) bar at the top of the screen during a round.
     *
     * Contains:
     *   - Dark rounded background bar
     *   - Difficulty label (left, in difficulty colour)
     *   - Best score label (left of centre, in gold)
     *   - Current score (centre)
     *   - Time remaining (right, colour shifts blue → yellow → red)
     *   - Timer bar (thin progress bar at the bottom of the HUD)
     */
    private void drawHUD(Graphics2D g2) {
        int   W         = getWidth();
        float timeRatio = state.getTimeFraction(); // 1.0 = full time, 0.0 = none left

        // ── HUD background bar ────────────────────────────────
        g2.setColor(new Color(15, 18, 30, 220)); // near-black, slightly transparent
        g2.fillRoundRect(10, 10, W - 20, 54, 12, 12);
        g2.setColor(new Color(50, 60, 100));     // subtle blue border
        g2.setStroke(new BasicStroke(1));
        g2.drawRoundRect(10, 10, W - 20, 54, 12, 12);

        // ── Timer bar colour ──────────────────────────────────
        // Shifts from blue (lots of time) → yellow (low) → red (critical)
        Color timerColor = timeRatio > 0.4f ? ACCENT
                         : timeRatio > 0.2f ? new Color(255, 200, 50)
                                            : RED;

        // ── Timer bar track (dark background) ─────────────────
        g2.setColor(new Color(30, 35, 55));
        g2.fillRoundRect(20, 50, W - 40, 8, 4, 4);

        // ── Timer bar fill (shrinks left-to-right as time runs out) ──
        g2.setColor(timerColor);
        g2.fillRoundRect(20, 50, (int)((W - 40) * timeRatio), 8, 4, 4);

        // ── Text elements ─────────────────────────────────────
        g2.setFont(new Font("Monospaced", Font.BOLD, 15));
        FontMetrics fm = g2.getFontMetrics();

        String diffStr  = difficulty.label;
        String scoreStr = "SCORE  " + state.getScore();
        String timeStr  = String.format("%.1f s", state.getRemainingMs() / 1000.0);
        String bestStr  = "BEST  " + highScore;

        // Accuracy — real-time percentage: Hits / (Hits + Misses) * 100
        // Shows "ACC  --%" before any shots are taken (avoids divide-by-zero)
        int    acc    = state.getAccuracy();
        String accStr = "ACC  " + (acc < 0 ? "--%" : acc + "%");

        // Difficulty label — left side, in difficulty's accent colour
        g2.setColor(difficulty.color);
        g2.drawString(diffStr, 22, 41);

        // Score — centred horizontally
        g2.setColor(HUD_FG);
        g2.drawString(scoreStr, W / 2 - fm.stringWidth(scoreStr) / 2, 41);

        // Time remaining — far right, in timer colour
        g2.setColor(timerColor);
        g2.drawString(timeStr, W - fm.stringWidth(timeStr) - 22, 41);

        // Accuracy — just left of the time display, in a soft green/white colour
        // Colour shifts: green (high acc) → yellow (mid) → red (low)
        Color accColor;
        if      (acc < 0)   accColor = new Color(140, 160, 210); // no shots yet — dim blue
        else if (acc >= 70) accColor = new Color( 80, 200, 120); // 70%+ → green
        else if (acc >= 40) accColor = new Color(255, 200,  50); // 40–69% → yellow
        else                accColor = new Color(255,  80,  80); // below 40% → red

        // Switch to size-15 font briefly to measure how wide the time string is,
        // then switch to size-12 for the accuracy label itself
        g2.setFont(new Font("Monospaced", Font.BOLD, 15));
        int timeStrWidth = g2.getFontMetrics().stringWidth(timeStr); // time label width at size 15
        g2.setFont(new Font("Monospaced", Font.BOLD, 12));
        fm = g2.getFontMetrics();
        g2.setColor(accColor);
        // Place accuracy label to the left of the time label, with a 14px gap
        g2.drawString(accStr, W - timeStrWidth - fm.stringWidth(accStr) - 36, 41);

        // Best score — positioned just right of the difficulty label
        // We measure the difficulty label width first (at size 15), then switch to size 12
        g2.setFont(new Font("Monospaced", Font.BOLD, 15));
        int diffStrWidth = g2.getFontMetrics().stringWidth(diffStr); // width of difficulty text
        g2.setFont(new Font("Monospaced", Font.BOLD, 12));           // smaller font for best score
        fm = g2.getFontMetrics();
        g2.setColor(GOLD);
        g2.drawString(bestStr, 22 + diffStrWidth + 16, 41); // 16px gap after difficulty label
    }

    /**
     * Draws the start screen — shown when score and hits are both zero.
     * Displays the game title, a welcome message, and the player's existing best score.
     */
    private void drawStartScreen(Graphics2D g2) {
        int W = getWidth(), H = getHeight();

        centeredString(g2, "AIM TRAINER",
            new Font("Monospaced", Font.BOLD, 46), ACCENT, W, H / 2 - 60);

        centeredString(g2, "Welcome, " + username + "!",
            new Font("Monospaced", Font.PLAIN, 18), new Color(160, 180, 220), W, H / 2 - 10);

        // Only show "Your Best" if they have a recorded score
        if (highScore > 0) {
            centeredString(g2, "Your Best: " + highScore,
                new Font("Monospaced", Font.BOLD, 16), GOLD, W, H / 2 + 25);
        }

        centeredString(g2, "Click anywhere to start",
            new Font("Monospaced", Font.PLAIN, 15), new Color(100, 130, 180), W, H / 2 + 70);
    }

    /**
     * Draws the result screen after a round ends.
     * Shows a stats card with score, hits, misses, accuracy, and expired count.
     * Displays either "★ NEW HIGH SCORE ★" in gold, or the existing best score.
     */
    private void drawResultScreen(Graphics2D g2) {
        int W = getWidth(), H = getHeight();
        int score = state.getScore();
        int acc   = state.getAccuracy();
        // Format accuracy as "72%" or "—" if no shots were taken
        String accStr = acc < 0 ? "—" : acc + "%";

        // ── Round over title ──────────────────────────────────
        centeredString(g2, "ROUND OVER",
            new Font("Monospaced", Font.BOLD, 40), new Color(255, 210, 60), W, H / 2 - 130);

        // ── Stats card ────────────────────────────────────────
        int boxW = 340, boxH = 200;
        int boxX = W / 2 - boxW / 2;  // centred horizontally
        int boxY = H / 2 - 100;

        // Dark rounded card background
        g2.setColor(new Color(15, 18, 30, 210));
        g2.fillRoundRect(boxX, boxY, boxW, boxH, 16, 16);
        g2.setColor(new Color(50, 60, 100)); // subtle blue border
        g2.setStroke(new BasicStroke(1));
        g2.drawRoundRect(boxX, boxY, boxW, boxH, 16, 16);

        // Draw each stat row (label left, value right) inside the card
        Font statFont = new Font("Monospaced", Font.BOLD, 17);
        int  lineH    = 34;  // vertical spacing between rows
        int  startY   = boxY + 38; // first row Y position

        drawStat(g2, statFont, "Score",    String.valueOf(score),                    W, startY);
        drawStat(g2, statFont, "Hits",     String.valueOf(state.getHits()),          W, startY + lineH);
        drawStat(g2, statFont, "Misses",   String.valueOf(state.getMisses()),        W, startY + lineH * 2);
        drawStat(g2, statFont, "Accuracy", accStr,                                  W, startY + lineH * 3);
        drawStat(g2, statFont, "Expired",  String.valueOf(state.getTargetsExpired()),W, startY + lineH * 4);

        // ── High score line ───────────────────────────────────
        if (newRecord) {
            // Player beat their personal best — celebrate with a gold star message
            centeredString(g2, "★ NEW HIGH SCORE: " + score + " ★",
                new Font("Monospaced", Font.BOLD, 17), GOLD, W, boxY + boxH + 36);
        } else {
            // Show their existing best score for reference
            centeredString(g2, "Best: " + highScore,
                new Font("Monospaced", Font.BOLD, 15), GOLD, W, boxY + boxH + 36);
        }

        // Prompt to restart
        centeredString(g2, "Click to play again",
            new Font("Monospaced", Font.PLAIN, 14), new Color(100, 130, 180), W, boxY + boxH + 70);
    }

    /**
     * Draws one row of the result screen stats card.
     * Label is left-aligned; value is right-aligned within a fixed 280px wide column.
     *
     * @param g2    Graphics context
     * @param font  Font for this row
     * @param label The stat name (e.g. "Score")
     * @param value The stat value (e.g. "4820")
     * @param W     Panel width — used to calculate the centred column position
     * @param y     Y position for this row
     */
    private void drawStat(Graphics2D g2, Font font, String label, String value, int W, int y) {
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        int totalW = 280;                                  // total width of the stat column
        int lx     = W / 2 - totalW / 2;                  // label X: left edge of column
        int rx     = W / 2 + totalW / 2 - fm.stringWidth(value); // value X: right-aligned

        g2.setColor(new Color(140, 160, 210)); // dim blue for label
        g2.drawString(label, lx, y);

        g2.setColor(HUD_FG); // bright for value
        g2.drawString(value, rx, y);
    }

    /**
     * Helper that draws a string centred horizontally at the given Y position.
     * Used by drawStartScreen() and drawResultScreen() for titles and messages.
     *
     * @param g2    Graphics context
     * @param text  String to draw
     * @param font  Font to use
     * @param color Text colour
     * @param W     Panel width (used to calculate centre X)
     * @param y     Y position (baseline of text)
     */
    private void centeredString(Graphics2D g2, String text, Font font, Color color, int W, int y) {
        g2.setFont(font);
        g2.setColor(color);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, W / 2 - fm.stringWidth(text) / 2, y);
    }

    // ── Mouse input ───────────────────────────────────────────

    /**
     * Called by Swing when the mouse button is clicked (pressed and released).
     *
     * Two behaviours depending on game state:
     *
     * 1. Game NOT active (start or result screen):
     *    → Resets state and starts a new round
     *
     * 2. Game active:
     *    → Check targets in reverse order (last added = visually on top)
     *    → If click lands inside a target: mark it hit, add score, spawn effects
     *    → If no target was hit: count as a miss
     *
     * @param e  MouseEvent containing the click coordinates
     */
    @Override
    public void mouseClicked(MouseEvent e) {
        if (!state.isActive()) {
            // Start a new round — clear leftover targets and reset the newRecord flag
            targets.clear();
            newRecord = false;
            state.startGame();
            lastSpawn = 0; // allow immediate first spawn
            return;
        }

        int mx = e.getX(); // click X coordinate
        int my = e.getY(); // click Y coordinate
        boolean hitSomething = false;

        // Iterate in reverse so the topmost-drawn (last in list) target is checked first
        for (int i = targets.size() - 1; i >= 0; i--) {
            Target t = targets.get(i);
            // Only check targets that are alive and not already clicked
            if (!t.isHit() && !t.isExpired() && t.contains(mx, my)) {
                t.markHit();                        // flag the target as clicked
                int pts = t.getPoints();            // calculate point value
                state.addScore(pts);                // add to running score
                state.addHit();                     // increment hit counter

                // Spawn visual effects at the click/target position
                effects.add(new FloatingTextEffect(mx, my, pts, difficulty.color));
                effects.add(new ExplosionEffect(t.getX(), t.getY(), difficulty.color, t.getSize()));

                hitSomething = true;
                break; // only one target can be hit per click
            }
        }

        // If no target was hit, count the click as a miss
        if (!hitSomething) state.addMiss();
    }

    // ── Unused MouseListener methods (must be implemented by the interface) ──
    @Override public void mousePressed(MouseEvent e)  {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e)  {}
    @Override public void mouseExited(MouseEvent e)   {}
}
