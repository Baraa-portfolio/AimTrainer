/**
 * ─────────────────────────────────────────────────────────────
 * GameState.java — Data / Model Class
 * ─────────────────────────────────────────────────────────────
 *
 * Stores all the live data for a single game session:
 *   score, hits, misses, expired targets, and the game timer.
 *
 * This class has NO rendering logic — it is pure data.
 * GamePanel reads and writes it; it never touches Swing.
 *
 * OOP concept: SEPARATION OF CONCERNS
 * Keeping data separate from rendering makes both easier to change.
 * For example, you could add online multiplayer by sending a GameState
 * object over the network without changing any drawing code.
 */
public class GameState {

    /** Total round length in milliseconds (30 seconds). */
    public static final long GAME_DURATION_MS = 30_000;

    // ── Session fields ────────────────────────────────────────
    // All start at 0 / false and are reset each time startGame() is called

    private int     score          = 0;     // total points earned this round
    private int     hits           = 0;     // number of targets successfully clicked
    private int     misses         = 0;     // number of clicks that didn't hit a target
    private int     targetsExpired = 0;     // targets that timed out without being clicked
    private long    startTime      = 0;     // System.currentTimeMillis() when the round began
    private boolean active         = false; // true only while a round is in progress

    // ── Session control ───────────────────────────────────────

    /**
     * Resets all stats and starts the 30-second countdown.
     * Called by GamePanel.mouseClicked() when the player clicks the start/result screen.
     */
    public void startGame() {
        score          = 0;
        hits           = 0;
        misses         = 0;
        targetsExpired = 0;
        startTime      = System.currentTimeMillis(); // capture the exact start time
        active         = true;
    }

    /**
     * Marks the round as finished.
     * Called by GamePanel.endRound() when the timer reaches zero.
     * Does NOT reset stats — they remain readable for the result screen.
     */
    public void endGame() {
        active = false;
    }

    // ── Mutators (called by GamePanel during gameplay) ────────

    /** Adds points to the running score. Called when a target is hit. */
    public void addScore(int points)  { score += points; }

    /** Increments hit counter. Called when a target is successfully clicked. */
    public void addHit()              { hits++;           }

    /** Increments miss counter. Called when a click lands on empty space. */
    public void addMiss()             { misses++;         }

    /** Increments expired counter. Called when a target times out. */
    public void addExpired()          { targetsExpired++; }

    // ── Getters (read-only access to state) ───────────────────

    public boolean isActive()        { return active;         }
    public int     getScore()        { return score;          }
    public int     getHits()         { return hits;           }
    public int     getMisses()       { return misses;         }
    public int     getTargetsExpired(){ return targetsExpired; }

    /**
     * Returns milliseconds remaining in the current round.
     * If the game is not active, returns 0.
     * Math.max(0, ...) prevents returning a negative number if a frame is slightly late.
     */
    public long getRemainingMs() {
        if (!active) return 0;
        long elapsed = System.currentTimeMillis() - startTime;
        return Math.max(0, GAME_DURATION_MS - elapsed);
    }

    /**
     * Returns the remaining time as a fraction from 0.0 to 1.0.
     *   1.0 = full time remaining
     *   0.0 = time is up
     * Used by GamePanel to draw the shrinking timer bar in the HUD.
     */
    public float getTimeFraction() {
        return getRemainingMs() / (float) GAME_DURATION_MS;
    }

    /**
     * Returns accuracy as an integer percentage (0–100).
     * Returns -1 if no shots have been taken yet (avoids divide-by-zero).
     *
     * Formula: hits / (hits + misses) * 100
     */
    public int getAccuracy() {
        int total = hits + misses;
        if (total == 0) return -1; // no shots fired yet
        return (int)(100.0 * hits / total);
    }

    /**
     * Returns true when the round timer has reached zero.
     * GamePanel.tick() calls this every frame to detect when to end the round.
     */
    public boolean isTimeUp() {
        return active && getRemainingMs() <= 0;
    }
}
