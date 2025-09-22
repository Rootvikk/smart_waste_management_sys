package src;

import javax.swing.SwingUtilities;

/**
 * Main class to start the Smart Waste Management System application.
 */
public class Main {
    public static void main(String[] args) {
        // Use SwingUtilities.invokeLater to ensure that the GUI is created and updated on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            // Create and show the login frame
            new LoginFrame().setVisible(true);
        });
    }
}
