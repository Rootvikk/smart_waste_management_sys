package src;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * The dashboard for Admin users.
 * Allows admins to view all submitted reports, assign them to workers, and update report statuses.
 */
public class AdminDashboard extends JFrame {

    private JTable reportTable;
    private DefaultTableModel tableModel;
    private JComboBox<String> workerComboBox;
    private JComboBox<String> statusComboBox;
    private Map<String, Integer> workerMap; // Maps worker username to their ID

    public AdminDashboard() {
        setTitle("Admin Dashboard");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Panel for displaying reports
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder("All Waste Reports"));
        tableModel = new DefaultTableModel(new String[]{"Report ID", "Citizen ID", "Description", "Location", "Status", "Date"}, 0);
        reportTable = new JTable(tableModel);
        tablePanel.add(new JScrollPane(reportTable), BorderLayout.CENTER);

        // Panel for actions (assigning tasks, updating status)
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        actionPanel.setBorder(BorderFactory.createTitledBorder("Actions"));
        
        actionPanel.add(new JLabel("Assign to Worker:"));
        workerComboBox = new JComboBox<>();
        actionPanel.add(workerComboBox);
        JButton assignButton = new JButton("Assign Task");
        assignButton.addActionListener(this::assignTask);
        actionPanel.add(assignButton);

        actionPanel.add(Box.createHorizontalStrut(20)); // Spacer

        actionPanel.add(new JLabel("Update Status:"));
        statusComboBox = new JComboBox<>(new String[]{"Pending", "In Progress", "Completed"});
        actionPanel.add(statusComboBox);
        JButton updateStatusButton = new JButton("Update Report Status");
        updateStatusButton.addActionListener(this::updateReportStatus);
        actionPanel.add(updateStatusButton);

        add(tablePanel, BorderLayout.CENTER);
        add(actionPanel, BorderLayout.SOUTH);

        // Load initial data
        loadWorkers();
        loadReports();
    }

    /**
     * Loads all worker users from the database to populate the worker selection dropdown.
     */
    private void loadWorkers() {
        workerMap = new HashMap<>();
        workerComboBox.removeAllItems();
        String sql = "SELECT id, username FROM users WHERE role = 'Worker'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int workerId = rs.getInt("id");
                String username = rs.getString("username");
                workerMap.put(username, workerId);
                workerComboBox.addItem(username);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to load workers.", "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Loads all reports from the database and populates the main table.
     */
    private void loadReports() {
        tableModel.setRowCount(0); // Clear existing data
        String sql = "SELECT id, citizen_id, description, location, status, submission_date FROM reports ORDER BY submission_date DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("id"));
                row.add(rs.getInt("citizen_id"));
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
     * Assigns a selected report as a task to a selected worker.
     * @param e The ActionEvent triggered by the assign button.
     */
    private void assignTask(ActionEvent e) {
        int selectedRow = reportTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a report to assign.", "Selection Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (workerComboBox.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "No workers available to assign.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int reportId = (int) tableModel.getValueAt(selectedRow, 0);
        String selectedWorkerName = (String) workerComboBox.getSelectedItem();
        int workerId = workerMap.get(selectedWorkerName);

        String sql = "INSERT INTO tasks (report_id, worker_id, status) VALUES (?, ?, 'Assigned')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, reportId);
            stmt.setInt(2, workerId);
            stmt.executeUpdate();

            // Also update the report status to "In Progress"
            updateReportStatusInDB(reportId, "In Progress");

            JOptionPane.showMessageDialog(this, "Task assigned successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            loadReports(); // Refresh table to show updated status
        } catch (SQLException ex) {
            // Handle unique constraint violation (task already assigned for this report)
            if (ex.getSQLState().startsWith("23")) {
                 JOptionPane.showMessageDialog(this, "A task for this report has already been assigned.", "Error", JOptionPane.ERROR_MESSAGE);
            } else {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Failed to assign task.", "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Updates the status of a selected report.
     * @param e The ActionEvent triggered by the update status button.
     */
    private void updateReportStatus(ActionEvent e) {
        int selectedRow = reportTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a report to update.", "Selection Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int reportId = (int) tableModel.getValueAt(selectedRow, 0);
        String newStatus = (String) statusComboBox.getSelectedItem();
        
        updateReportStatusInDB(reportId, newStatus);
        loadReports(); // Refresh the table
    }

    /**
     * Helper method to update a report's status in the database.
     * @param reportId The ID of the report to update.
     * @param status The new status for the report.
     */
    private void updateReportStatusInDB(int reportId, String status) {
        String sql = "UPDATE reports SET status = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status);
            stmt.setInt(2, reportId);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                 System.out.println("Report " + reportId + " status updated to " + status);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to update report status.", "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
