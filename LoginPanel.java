import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

/**
 * ─────────────────────────────────────────────────────────────
 * LoginPanel.java — Swing UI Panel
 * ─────────────────────────────────────────────────────────────
 *
 * The login / registration screen shown when the app starts.
 * Extends JPanel (a Swing container) and draws a centred card
 * with username, password fields, Login and Register buttons.
 *
 * When login succeeds, it fires the onLoginSuccess callback,
 * which tells AimTrainer to swap this panel out for the game.
 *
 * Responsibilities:
 *   - Render the login UI
 *   - Validate input (empty fields, short passwords, illegal chars)
 *   - Call UserManager.login() / UserManager.register() for file I/O
 *   - Show success or error messages inline
 *
 * OOP concept: ENCAPSULATION + EVENT-DRIVEN PROGRAMMING
 * All login/register logic is contained here. AimTrainer only
 * needs to pass a callback — it never touches username/password.
 */
public class LoginPanel extends JPanel {

    // ── Colours — dark theme matching the game ────────────────
    private static final Color BG          = new Color( 10,  12,  20); // near-black background
    private static final Color BG_CARD     = new Color( 18,  22,  36); // slightly lighter card background
    private static final Color ACCENT      = new Color( 80, 180, 255); // blue — Login button and title
    private static final Color ACCENT2     = new Color( 80, 200, 120); // green — Register button
    private static final Color FG          = new Color(200, 210, 255); // light lavender — input text
    private static final Color FG_DIM      = new Color(100, 120, 170); // dim blue — field labels
    private static final Color BORDER_COL  = new Color( 40,  55,  90); // dark border for inputs
    private static final Color ERR_COL     = new Color(255,  90,  90); // red — error messages
    private static final Color SUCCESS_COL = new Color( 80, 200, 120); // green — success messages

    // ── Shared fonts ──────────────────────────────────────────
    private static final Font MONO_BOLD  = new Font("Monospaced", Font.BOLD,  13);
    private static final Font MONO_PLAIN = new Font("Monospaced", Font.PLAIN, 12);

    // ── UI components ─────────────────────────────────────────
    private final JTextField     usernameField; // text input for the username
    private final JPasswordField passwordField; // text input that hides characters
    private final JLabel         messageLabel;  // shows error or success feedback
    private final JButton        loginBtn;      // triggers handleLogin()
    private final JButton        registerBtn;   // triggers handleRegister()

    /**
     * Callback fired when login succeeds.
     * Consumer<String> is a functional interface that accepts one String argument.
     * AimTrainer passes in: username -> showGameScreen(username)
     */
    private final java.util.function.Consumer<String> onLoginSuccess;

    /**
     * Constructor — builds the entire login UI.
     *
     * @param onLoginSuccess  Lambda called with the username after successful login.
     *                        AimTrainer uses this to switch panels.
     */
    public LoginPanel(java.util.function.Consumer<String> onLoginSuccess) {
        this.onLoginSuccess = onLoginSuccess;

        setBackground(BG);
        setLayout(new GridBagLayout()); // GridBagLayout with no constraints = centres its child

        // ── Card ──────────────────────────────────────────────
        // A sub-panel that holds all the form elements, centred on screen
        JPanel card = new JPanel();
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COL, 1),        // outer border line
            BorderFactory.createEmptyBorder(40, 50, 40, 50)       // inner padding
        ));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS)); // stack children vertically
        card.setMaximumSize(new Dimension(380, 600));          // cap width so it doesn't stretch

        // ── Title label ───────────────────────────────────────
        JLabel title = new JLabel("AIM TRAINER");
        title.setFont(new Font("Monospaced", Font.BOLD, 28));
        title.setForeground(ACCENT);
        title.setAlignmentX(Component.CENTER_ALIGNMENT); // centre horizontally in BoxLayout

        // ── Subtitle ──────────────────────────────────────────
        JLabel subtitle = new JLabel("Login or Register to Play");
        subtitle.setFont(MONO_PLAIN);
        subtitle.setForeground(FG_DIM);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ── Username field ────────────────────────────────────
        JLabel userLabel = makeLabel("USERNAME"); // small caps label above the field
        usernameField    = makeTextField();        // styled text input

        // ── Password field ────────────────────────────────────
        JLabel passLabel = makeLabel("PASSWORD");
        passwordField    = new JPasswordField();   // hides characters with bullets
        styleTextField(passwordField);             // apply same dark styling

        // ── Message label ─────────────────────────────────────
        // Starts blank — updated by showMessage() on login/register attempts
        messageLabel = new JLabel(" "); // space so it takes up height before any message
        messageLabel.setFont(MONO_PLAIN);
        messageLabel.setForeground(ERR_COL);
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ── Buttons ───────────────────────────────────────────
        loginBtn    = makeButton("LOGIN",    ACCENT);  // blue
        registerBtn = makeButton("REGISTER", ACCENT2); // green

        // Wire button clicks to handler methods
        loginBtn.addActionListener(e -> handleLogin());
        registerBtn.addActionListener(e -> handleRegister());

        // Pressing Enter in either field also triggers login
        ActionListener enterLogin = e -> handleLogin();
        usernameField.addActionListener(enterLogin);
        passwordField.addActionListener(enterLogin);

        // ── Assemble the card ─────────────────────────────────
        // Box.createVerticalStrut(n) adds n pixels of vertical spacing
        card.add(title);
        card.add(Box.createVerticalStrut(4));
        card.add(subtitle);
        card.add(Box.createVerticalStrut(32));
        card.add(userLabel);
        card.add(Box.createVerticalStrut(6));
        card.add(usernameField);
        card.add(Box.createVerticalStrut(18));
        card.add(passLabel);
        card.add(Box.createVerticalStrut(6));
        card.add(passwordField);
        card.add(Box.createVerticalStrut(10));
        card.add(messageLabel);
        card.add(Box.createVerticalStrut(20));
        card.add(loginBtn);
        card.add(Box.createVerticalStrut(10));
        card.add(registerBtn);
        card.add(Box.createVerticalStrut(20));

        // Small hint at the bottom so users know where data is saved
        JLabel hint = new JLabel("Credentials saved to users.txt");
        hint.setFont(new Font("Monospaced", Font.PLAIN, 10));
        hint.setForeground(new Color(60, 80, 120));
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(hint);

        add(card); // add card to this panel (GridBagLayout centres it)
        setOpaque(true); // ensure background colour is painted
    }

    // ── Action handlers ───────────────────────────────────────

    /**
     * Called when the Login button is clicked or Enter is pressed.
     * Validates input, then calls UserManager.login().
     * On success, waits 600ms so the player sees "Welcome back!" then fires the callback.
     */
    private void handleLogin() {
        String user = usernameField.getText().trim();
        String pass = new String(passwordField.getPassword()).trim(); // getPassword() returns char[]

        if (user.isEmpty() || pass.isEmpty()) {
            showMessage("Please fill in both fields.", false);
            return;
        }

        if (UserManager.login(user, pass)) {
            showMessage("Welcome back, " + user + "!", true);
            // Brief delay so the success message is visible before switching panels
            javax.swing.Timer t = new javax.swing.Timer(600, e -> onLoginSuccess.accept(user));
            t.setRepeats(false); // fire only once, not repeatedly
            t.start();
        } else {
            showMessage("Incorrect username or password.", false);
            passwordField.setText(""); // clear password field for re-entry
        }
    }

    /**
     * Called when the Register button is clicked.
     * Validates input, then calls UserManager.register().
     * Shows feedback inline — does NOT auto-log-in after registration.
     */
    private void handleRegister() {
        String user = usernameField.getText().trim();
        String pass = new String(passwordField.getPassword()).trim();

        // ── Input validation ──────────────────────────────────
        if (user.isEmpty() || pass.isEmpty()) {
            showMessage("Please fill in both fields.", false);
            return;
        }
        if (user.contains(":")) {
            // Colon is the separator in users.txt — cannot allow it in usernames
            showMessage("Username cannot contain ':'.", false);
            return;
        }
        if (pass.length() < 4) {
            showMessage("Password must be at least 4 characters.", false);
            return;
        }

        if (UserManager.register(user, pass)) {
            showMessage("Account created! You can now log in.", true);
            passwordField.setText(""); // clear so user must re-type to log in
        } else {
            showMessage("Username '" + user + "' is already taken.", false);
        }
    }

    /**
     * Updates the message label with feedback text.
     *
     * @param text     The message to display
     * @param success  true = green (success), false = red (error)
     */
    private void showMessage(String text, boolean success) {
        messageLabel.setText(text);
        messageLabel.setForeground(success ? SUCCESS_COL : ERR_COL);
    }

    // ── Custom painting ───────────────────────────────────────

    /**
     * Overrides JPanel's paintComponent to draw the dot-grid background.
     * super.paintComponent(g) must be called first — it fills the background colour.
     * The dots match the GamePanel aesthetic for visual consistency.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(new Color(20, 25, 40)); // slightly lighter than the background
        for (int x = 20; x < getWidth();  x += 40) // every 40px horizontally
            for (int y = 20; y < getHeight(); y += 40) // every 40px vertically
                g.fillOval(x - 1, y - 1, 2, 2); // 2x2 dot
    }

    // ── UI builder helpers ────────────────────────────────────

    /**
     * Creates a small all-caps label used above each input field.
     *
     * @param text  Label text (e.g. "USERNAME")
     */
    private JLabel makeLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Monospaced", Font.BOLD, 11));
        lbl.setForeground(FG_DIM);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT); // align to left edge of card
        return lbl;
    }

    /**
     * Creates a styled plain text input field.
     * Calls styleTextField() to apply the dark theme.
     */
    private JTextField makeTextField() {
        JTextField field = new JTextField();
        styleTextField(field);
        return field;
    }

    /**
     * Applies the dark theme styling to any JTextField or JPasswordField.
     * Also adds a focus listener that highlights the border when the field is active.
     *
     * @param field  The text field to style (works for both JTextField and JPasswordField)
     */
    private void styleTextField(JTextField field) {
        field.setFont(MONO_BOLD);
        field.setForeground(FG);                          // text colour
        field.setBackground(new Color(12, 15, 26));       // very dark input background
        field.setCaretColor(ACCENT);                      // cursor colour = blue accent
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COL, 1),        // outer border
            BorderFactory.createEmptyBorder(8, 12, 8, 12)         // inner padding
        ));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40)); // full width, fixed height
        field.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Highlight border blue when the field is focused; restore when focus is lost
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ACCENT, 1),     // blue border on focus
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
                ));
            }
            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER_COL, 1), // restore dim border
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
                ));
            }
        });
    }

    /**
     * Creates a styled button with a coloured border and hover effect.
     *
     * @param text    Button label text
     * @param accent  Border and text colour (ACCENT for Login, ACCENT2 for Register)
     */
    private JButton makeButton(String text, Color accent) {
        JButton btn = new JButton(text);
        btn.setFont(MONO_BOLD);
        btn.setForeground(accent);
        btn.setBackground(new Color(18, 22, 36));     // same as card background
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(accent.darker(), 1), // slightly darker border
            BorderFactory.createEmptyBorder(10, 20, 10, 20)     // padding inside button
        ));
        btn.setFocusPainted(false);                              // remove default focus rectangle
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); // pointer on hover
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42)); // full width, fixed height

        // Hover: lighten background slightly when mouse enters; restore on exit
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setBackground(new Color(25, 32, 55)); }
            @Override public void mouseExited(MouseEvent e)  { btn.setBackground(new Color(18, 22, 36)); }
        });
        return btn;
    }
}
