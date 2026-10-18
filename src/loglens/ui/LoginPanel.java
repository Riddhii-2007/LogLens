package loglens.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import loglens.dao.UserDAO;
import loglens.model.User;

// Centered login screen that authenticates users against the database.
public class LoginPanel extends JPanel {

    // Callback interface notified when authentication succeeds
    public interface LoginListener {
        void onLoginSuccess(User user);
    }

    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JButton loginButton;
    private final JLabel errorLabel;
    private final UserDAO userDAO;
    private LoginListener listener;

    public LoginPanel() {
        this(null);
    }

    public LoginPanel(LoginListener listener) {
        super(new GridBagLayout());
        this.listener = listener;
        this.userDAO = new UserDAO();

        // Inner form card with clean borders
        JPanel formCard = new JPanel(new GridBagLayout());
        formCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(24, 32, 24, 32)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // --- Title Label ---
        JLabel titleLabel = new JLabel("LogLens Login", JLabel.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(0, 0, 16, 0);
        formCard.add(titleLabel, gbc);

        // Reset insets and gridwidth
        gbc.gridwidth = 1;
        gbc.insets = new Insets(6, 6, 6, 6);

        // --- Username ---
        gbc.gridx = 0;
        gbc.gridy = 1;
        formCard.add(new JLabel("Username:"), gbc);

        usernameField = new JTextField(15);
        gbc.gridx = 1;
        gbc.gridy = 1;
        formCard.add(usernameField, gbc);

        // --- Password ---
        gbc.gridx = 0;
        gbc.gridy = 2;
        formCard.add(new JLabel("Password:"), gbc);

        passwordField = new JPasswordField(15);
        gbc.gridx = 1;
        gbc.gridy = 2;
        formCard.add(passwordField, gbc);

        // --- Error Label ---
        errorLabel = new JLabel(" ", JLabel.CENTER);
        errorLabel.setForeground(Color.RED);
        errorLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        formCard.add(errorLabel, gbc);

        // --- Login Button ---
        loginButton = new JButton("Login");
        loginButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(10, 6, 6, 6);
        formCard.add(loginButton, gbc);

        // Center form card in the main panel
        add(formCard, new GridBagConstraints());

        // Action listener for both button click and Enter key in password field
        ActionListener loginAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performLogin();
            }
        };

        loginButton.addActionListener(loginAction);
        passwordField.addActionListener(loginAction);
    }

    public void setLoginListener(LoginListener listener) {
        this.listener = listener;
    }

    // Handles user authentication attempt
    private void performLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Invalid username or password");
            passwordField.setText("");
            return;
        }

        try {
            User user = userDAO.authenticate(username, password);
            if (user != null) {
                errorLabel.setText(" ");
                if (listener != null) {
                    listener.onLoginSuccess(user);
                }
            } else {
                errorLabel.setText("Invalid username or password");
                passwordField.setText("");
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Database error during authentication: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Clears all form fields when returning from a session
    public void clearFields() {
        usernameField.setText("");
        passwordField.setText("");
        errorLabel.setText(" ");
        usernameField.requestFocusInWindow();
    }
}
