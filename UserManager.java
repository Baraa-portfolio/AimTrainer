import java.io.*;
import java.util.*;

/**
 * ─────────────────────────────────────────────────────────────
 * UserManager.java — File I/O Utility Class
 * ─────────────────────────────────────────────────────────────
 *
 * Handles reading and writing all persistent data to text files.
 * Two files are managed:
 *
 *   users.txt  — stores login credentials, one per line
 *                Format: username:password
 *                Example: alice:pass1234
 *
 *   scores.txt — stores each user's personal best score, one per line
 *                Format: username:score
 *                Example: alice:4820
 *
 * All methods are static — you call UserManager.login(...) directly
 * without creating a UserManager object. This makes sense because
 * there is only one set of files to manage.
 *
 * OOP concept: ENCAPSULATION
 * All file I/O is isolated here. No other class reads or writes files.
 * If you ever want to switch to a database, you only change this file.
 */
public class UserManager {

    // ── File path constants ───────────────────────────────────
    // Files are created in whatever directory you run javac/java from
    private static final String USERS_FILE  = "users.txt";
    private static final String SCORES_FILE = "scores.txt";

    // ── Authentication ────────────────────────────────────────

    /**
     * Checks if the given username and password match a line in users.txt.
     *
     * How it works:
     *   1. Opens users.txt line by line
     *   2. Splits each line on ":" to get [username, password]
     *   3. Compares case-insensitively for username, case-sensitively for password
     *   4. Returns true on first match; false if no match or file missing
     *
     * @param username  The typed username
     * @param password  The typed password
     * @return          true if credentials are valid
     */
    public static boolean login(String username, String password) {
        if (username.isBlank() || password.isBlank()) return false;
        try (BufferedReader br = new BufferedReader(new FileReader(USERS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                // split(":", 2) limits to 2 parts so passwords containing ":" still work
                String[] parts = line.split(":", 2);
                if (parts.length == 2
                        && parts[0].equalsIgnoreCase(username.trim()) // username: case-insensitive
                        && parts[1].equals(password)) {               // password: case-sensitive
                    return true;
                }
            }
        } catch (IOException e) {
            // FileNotFoundException is a subtype of IOException
            // If users.txt doesn't exist yet, no one can log in — return false
        }
        return false;
    }

    /**
     * Registers a new user by appending their credentials to users.txt.
     *
     * Checks:
     *   - Both fields must be non-empty
     *   - Username must not already exist (checked via userExists())
     *
     * FileWriter(USERS_FILE, true) — the "true" means APPEND mode,
     * so existing users are never overwritten.
     *
     * @param username  Desired username
     * @param password  Desired password
     * @return          true if registration succeeded; false if username taken or error
     */
    public static boolean register(String username, String password) {
        if (username.isBlank() || password.isBlank()) return false;
        if (userExists(username)) return false; // reject duplicate usernames

        try (PrintWriter pw = new PrintWriter(new FileWriter(USERS_FILE, true))) {
            pw.println(username.trim() + ":" + password); // write one line
            return true;
        } catch (IOException e) {
            return false; // could not write (e.g. permission denied)
        }
    }

    /**
     * Returns true if a username already exists in users.txt.
     * Used by register() to prevent duplicates.
     * Comparison is case-insensitive so "Alice" and "alice" are the same user.
     *
     * @param username  The username to look up
     * @return          true if found; false if not found or file missing
     */
    public static boolean userExists(String username) {
        try (BufferedReader br = new BufferedReader(new FileReader(USERS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(":", 2);
                if (parts.length >= 1 && parts[0].equalsIgnoreCase(username.trim()))
                    return true;
            }
        } catch (IOException e) {
            // File doesn't exist — no users registered yet
        }
        return false;
    }

    // ── High scores ───────────────────────────────────────────

    /**
     * Returns the stored high score for the given user, or 0 if none found.
     * Reads from scores.txt.
     *
     * @param username  The logged-in player's username
     * @return          Their personal best score, or 0 if not yet recorded
     */
    public static int getHighScore(String username) {
        try (BufferedReader br = new BufferedReader(new FileReader(SCORES_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(":", 2);
                if (parts.length == 2 && parts[0].equalsIgnoreCase(username.trim())) {
                    return Integer.parseInt(parts[1].trim()); // parse the stored number
                }
            }
        } catch (IOException | NumberFormatException e) {
            // IOException  = file missing (no scores yet)
            // NumberFormatException = corrupted line — skip it
        }
        return 0; // default: no high score recorded
    }

    /**
     * Saves the player's score ONLY if it beats their current personal best.
     * Rewrites the entire scores.txt with the updated value.
     *
     * Why rewrite the whole file?
     *   Text files don't support in-place editing of a single line.
     *   The safest approach is: read all scores → update one → write all back.
     *
     * How it works:
     *   1. Check if score > current best; return false immediately if not
     *   2. Load all existing scores from scores.txt into a LinkedHashMap
     *      (LinkedHashMap preserves insertion order — keeps file tidy)
     *   3. Update / insert this player's entry in the map
     *   4. Rewrite scores.txt with all entries from the map
     *
     * @param username  The player's username
     * @param score     The score they just achieved
     * @return          true if this is a new personal best (file was updated)
     */
    public static boolean saveScoreIfBest(String username, int score) {
        int current = getHighScore(username);
        if (score <= current) return false; // not a new record — do nothing

        // Step 2: read all existing scores into memory
        Map<String, Integer> scores = new LinkedHashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(SCORES_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(":", 2);
                if (parts.length == 2) {
                    try {
                        scores.put(parts[0].trim(), Integer.parseInt(parts[1].trim()));
                    } catch (NumberFormatException ignored) {
                        // skip any corrupted lines
                    }
                }
            }
        } catch (IOException e) {
            // scores.txt doesn't exist yet — that's fine, the map stays empty
        }

        // Step 3: update (or add) this player's entry
        scores.put(username.trim(), score);

        // Step 4: rewrite the file — FileWriter(path, false) = overwrite mode
        try (PrintWriter pw = new PrintWriter(new FileWriter(SCORES_FILE, false))) {
            for (Map.Entry<String, Integer> entry : scores.entrySet()) {
                pw.println(entry.getKey() + ":" + entry.getValue());
            }
            return true; // new high score saved successfully
        } catch (IOException e) {
            return false; // could not write
        }
    }
}
