package src;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * The login window for the application.
 * Users enter their credentials here, and based on their role, the corresponding dashboard is opened.
 */
public class LoginFrame extends JFrame {

    private JTextField usernameField;
    private JPasswordField passwordField;

    public LoginFrame() {
        setTitle("Smart Waste Management - Login");
        setSize(400, 250);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Center the window
        setLayout(new BorderLayout(10, 10));

        // Panel for the input fields
        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        panel.add(new JLabel("Username:"));
        usernameField = new JTextField();
        panel.add(usernameField);

        panel.add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        panel.add(passwordField);

        // Login button
        JButton loginButton = new JButton("Login");
        loginButton.addActionListener(this::performLogin);

        // Add components to the frame
        add(panel, BorderLayout.CENTER);
        add(loginButton, BorderLayout.SOUTH);
    }

    /**
     * Handles the login action when the login button is clicked.
     * @param e The ActionEvent triggered by the button click.
     */
    private void performLogin(ActionEvent e) {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username and password cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT id, role FROM users WHERE username = ? AND password = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setString(2, password);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int userId = rs.getInt("id");
                String role = rs.getString("role");

                // Open the appropriate dashboard based on the user's role
                openDashboard(userId, role);
                this.dispose(); // Close the login window
            } else {
                JOptionPane.showMessageDialog(this, "Invalid username or password.", "Login Failed", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Database error during login.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Opens the correct dashboard based on the user's role.
     * @param userId The ID of the logged-in user.
     * @param role The role of the logged-in user.
     */
    private void openDashboard(int userId, String role) {
        switch (role) {
            case "Admin":
                SwingUtilities.invokeLater(() -> new AdminDashboard().setVisible(true));
                break;
            case "Worker":
                SwingUtilities.invokeLater(() -> new WorkerDashboard(userId).setVisible(true));
                break;
            case "Citizen":
                SwingUtilities.invokeLater(() -> new CitizenDashboard(userId).setVisible(true));
                break;
            default:
                JOptionPane.showMessageDialog(this, "Unknown user role.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
