import javax.swing.*;
import java.awt.*;

/**
 * ─────────────────────────────────────────────────────────────
 * AimTrainer.java — Application Entry Point
 * ─────────────────────────────────────────────────────────────
 *
 * The main class and JFrame (window) for the entire application.
 * Contains only window setup and panel-switching logic.
 * All game logic lives in GamePanel and its dependencies.
 *
 * Flow:
 *   1. main() creates an AimTrainer window and makes it visible
 *   2. Constructor calls showLoginScreen() — LoginPanel is shown first
 *   3. When LoginPanel fires its callback, showGameScreen(username) runs
 *   4. showGameScreen() builds the toolbar and swaps in GamePanel
 *
 * OOP concept: SEPARATION OF CONCERNS
 * AimTrainer only manages the window and knows which panel to show.
 * It does not contain any game logic, rendering, or file I/O.
 */
public class AimTrainer extends JFrame {

    /**
     * Constructor — creates and configures the application window.
     * Does NOT call setVisible(true) — that is done in main() on the EDT.
     */
    public AimTrainer() {
        setTitle("Aim Trainer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // close app when window is closed
        setSize(900, 650);           // initial window size (pixels)
        setLocationRelativeTo(null); // centre window on the screen
        setResizable(true);          // allow the player to resize the window

        showLoginScreen(); // start with the login panel
    }

    // ── Panel switching ───────────────────────────────────────

    /**
     * Displays the login / register screen.
     *
     * Passes a method reference (this::showGameScreen) as the success callback.
     * When LoginPanel calls onLoginSuccess.accept(username), Java automatically
     * calls this.showGameScreen(username).
     *
     * setContentPane() replaces the entire content of the window.
     * revalidate() + repaint() force Swing to redraw with the new panel.
     */
    private void showLoginScreen() {
        setContentPane(new LoginPanel(this::showGameScreen));
        revalidate();
        repaint();
    }

    /**
     * Swaps out the login panel and shows the game.
     * Called automatically by LoginPanel after successful login or registration.
     *
     * Builds the layout:
     *   - Top: toolbar with difficulty buttons + "Playing as: X" label
     *   - Centre: GamePanel (the game itself)
     *
     * @param username  The username of the player who just logged in.
     *                  Passed to GamePanel so scores are saved under the right account.
     */
    private void showGameScreen(String username) {
        // Create the game panel and link it to the logged-in user
        GamePanel gamePanel = new GamePanel();
        gamePanel.setUsername(username); // loads this player's high score from file

        // ── Difficulty toolbar ────────────────────────────────
        // FlowLayout(LEFT, 8, 6) = left-aligned, 8px horizontal gap, 6px vertical gap
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        toolbar.setBackground(new Color(10, 12, 20));

        // "DIFFICULTY:" label at the start of the toolbar
        JLabel diffLabel = new JLabel("DIFFICULTY:");
        diffLabel.setForeground(new Color(120, 140, 190));
        diffLabel.setFont(new Font("Monospaced", Font.BOLD, 12));
        toolbar.add(diffLabel);

        // One toggle button per difficulty level
        // ButtonGroup ensures only one can be selected at a time (like radio buttons)
        ButtonGroup group = new ButtonGroup();
        for (Difficulty d : Difficulty.values()) {
            JToggleButton btn = new JToggleButton(d.label);
            btn.setFont(new Font("Monospaced", Font.BOLD, 12));
            btn.setForeground(d.color);               // text colour matches difficulty colour
            btn.setBackground(new Color(18, 22, 36));
            btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(d.color.darker(), 1), // coloured border
                BorderFactory.createEmptyBorder(3, 10, 3, 10)        // inner padding
            ));
            btn.setFocusPainted(false); // remove focus rectangle
            btn.setSelected(d == Difficulty.MEDIUM); // MEDIUM pre-selected on startup

            // When clicked, update GamePanel's difficulty setting
            btn.addActionListener(e -> gamePanel.setDifficulty(d));

            group.add(btn);   // add to radio group (mutual exclusion)
            toolbar.add(btn); // add to toolbar panel
        }

        // ── "Playing as:" label ───────────────────────────────
        // Shown on the right side of the toolbar so the player always sees their name
        JLabel playerLabel = new JLabel("Playing as: " + username + "   ");
        playerLabel.setForeground(new Color(80, 180, 255)); // blue accent
        playerLabel.setFont(new Font("Monospaced", Font.BOLD, 12));

        // ── Toolbar wrapper ───────────────────────────────────
        // BorderLayout lets us put difficulty buttons on the LEFT
        // and the player name on the RIGHT, with the HUD border underneath
        JPanel toolbarWrapper = new JPanel(new BorderLayout());
        toolbarWrapper.setBackground(new Color(10, 12, 20));
        toolbarWrapper.setBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(30, 40, 70)) // bottom border only
        );
        toolbarWrapper.add(toolbar,     BorderLayout.WEST);  // difficulty buttons
        toolbarWrapper.add(playerLabel, BorderLayout.EAST);  // player name

        // ── Game layout ───────────────────────────────────────
        // toolbar at the top (NORTH), game panel fills the rest (CENTER)
        JPanel layout = new JPanel(new BorderLayout());
        layout.add(toolbarWrapper, BorderLayout.NORTH);
        layout.add(gamePanel,      BorderLayout.CENTER);

        // Swap the content pane and refresh the window
        setContentPane(layout);
        revalidate();
        repaint();
    }

    // ── Entry point ───────────────────────────────────────────

    /**
     * Application entry point — called by the JVM to start the program.
     *
     * SwingUtilities.invokeLater() ensures all Swing UI creation happens
     * on the Event Dispatch Thread (EDT). This is required by Swing's
     * threading model to prevent race conditions and rendering glitches.
     *
     * @param args  Command-line arguments (not used)
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Use the operating system's native look and feel (Windows/Mac/Linux)
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // If the system L&F fails, Swing falls back to its default Metal theme
            }
            new AimTrainer().setVisible(true); // create window and show it
        });
    }
}
