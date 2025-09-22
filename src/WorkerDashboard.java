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
 * The dashboard for Worker users.
 * Allows workers to view their assigned tasks and mark them as completed.
 */
public class WorkerDashboard extends JFrame {

    private final int workerId;
    private JTable taskTable;
    private DefaultTableModel tableModel;

    public WorkerDashboard(int workerId) {
        this.workerId = workerId;

        setTitle("Worker Dashboard");
        setSize(800, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Panel for displaying assigned tasks
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder("Your Assigned Tasks"));
        tableModel = new DefaultTableModel(new String[]{"Task ID", "Report ID", "Description", "Location", "Status", "Assigned Date"}, 0);
        taskTable = new JTable(tableModel);
        tablePanel.add(new JScrollPane(taskTable), BorderLayout.CENTER);

        // Panel for the action button
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton completeButton = new JButton("Mark Task as Completed");
        completeButton.addActionListener(this::completeTask);
        actionPanel.add(completeButton);

        add(tablePanel, BorderLayout.CENTER);
        add(actionPanel, BorderLayout.SOUTH);

        // Load initial data
        loadTasks();
    }

    /**
     * Loads the tasks assigned to the current worker from the database.
     */
    private void loadTasks() {
        tableModel.setRowCount(0); // Clear existing data
        String sql = "SELECT t.id, t.report_id, r.description, r.location, t.status, t.assigned_date " +
                     "FROM tasks t JOIN reports r ON t.report_id = r.id " +
                     "WHERE t.worker_id = ? ORDER BY t.assigned_date DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, workerId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("id"));
                row.add(rs.getInt("report_id"));
                row.add(rs.getString("description"));
                row.add(rs.getString("location"));
                row.add(rs.getString("status"));
                row.add(rs.getTimestamp("assigned_date").toString());
                tableModel.addRow(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to load tasks.", "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Marks a selected task as completed.
     * @param e The ActionEvent triggered by the complete button.
     */
    private void completeTask(ActionEvent e) {
        int selectedRow = taskTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a task to mark as complete.", "Selection Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Check if the task is already completed
        String currentStatus = (String) tableModel.getValueAt(selectedRow, 4);
        if ("Completed".equalsIgnoreCase(currentStatus)) {
            JOptionPane.showMessageDialog(this, "This task is already marked as completed.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int taskId = (int) tableModel.getValueAt(selectedRow, 0);
        int reportId = (int) tableModel.getValueAt(selectedRow, 1);

        // Use a transaction to ensure both updates succeed or fail together
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Start transaction

            // 1. Update the task status to 'Completed'
            String updateTaskSql = "UPDATE tasks SET status = 'Completed', completed_date = CURRENT_TIMESTAMP WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(updateTaskSql)) {
                stmt.setInt(1, taskId);
                stmt.executeUpdate();
            }

            // 2. Update the corresponding report status to 'Completed'
            String updateReportSql = "UPDATE reports SET status = 'Completed' WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(updateReportSql)) {
                stmt.setInt(1, reportId);
                stmt.executeUpdate();
            }

            conn.commit(); // Commit the transaction
            JOptionPane.showMessageDialog(this, "Task marked as completed successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            loadTasks(); // Refresh the table

        } catch (SQLException ex) {
            if (conn != null) {
                try {
                    conn.rollback(); // Rollback on error
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to update task status.", "Database Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); // Restore default behavior
                } catch (SQLException finalEx) {
                    finalEx.printStackTrace();
                }
            }
        }
    }
}
