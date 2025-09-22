package src;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

/**
 * The dashboard for Citizen users.
 * Allows citizens to submit new waste reports and view the status of their previous reports.
 */
public class CitizenDashboard extends JFrame {

    private final int citizenId;
    private JTable reportTable;
    private DefaultTableModel tableModel;
    private JTextField descriptionField;
    private JTextField locationField;

    public CitizenDashboard(int citizenId) {
        this.citizenId = citizenId;

        setTitle("Citizen Dashboard");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Panel for creating a new report
        JPanel formPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createTitledBorder("Submit a New Report"));
        formPanel.add(new JLabel("Description:"));
        descriptionField = new JTextField();
        formPanel.add(descriptionField);
        formPanel.add(new JLabel("Location:"));
        locationField = new JTextField();
        formPanel.add(locationField);
        JButton submitButton = new JButton("Submit Report");
        submitButton.addActionListener(this::submitReport);
        formPanel.add(new JLabel()); // Placeholder
        formPanel.add(submitButton);

        // Panel for displaying existing reports
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder("Your Reports"));
        tableModel = new DefaultTableModel(new String[]{"ID", "Description", "Location", "Status", "Date"}, 0);
        reportTable = new JTable(tableModel);
        tablePanel.add(new JScrollPane(reportTable), BorderLayout.CENTER);

        // Add panels to the frame
        add(formPanel, BorderLayout.NORTH);
        add(tablePanel, BorderLayout.CENTER);

        // Load initial data
        loadReports();
    }

    /**
     * Loads the reports submitted by the current citizen from the database and populates the table.
     */
    private void loadReports() {
        tableModel.setRowCount(0); // Clear existing data
        String sql = "SELECT id, description, location, status, submission_date FROM reports WHERE citizen_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, citizenId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("id"));
                row.add(rs.getString("description"));
                row.add(rs.getString("location"));
                row.add(rs.getString("status"));
                row.add(rs.getTimestamp("submission_date").toString());
                tableModel.addRow(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to load reports.", "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Handles the submission of a new waste report.
     * @param e The ActionEvent triggered by the submit button.
     */
    private void submitReport(ActionEvent e) {
        String description = descriptionField.getText();
        String location = locationField.getText();

        if (description.isEmpty() || location.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Description and location are required.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String sql = "INSERT INTO reports (citizen_id, description, location) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, citizenId);
            stmt.setString(2, description);
            stmt.setString(3, location);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(this, "Report submitted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                descriptionField.setText("");
                locationField.setText("");
                loadReports(); // Refresh the table
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to submit report.", "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
