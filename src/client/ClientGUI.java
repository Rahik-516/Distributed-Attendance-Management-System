package client;

import common.AttendanceRecord;
import common.AttendanceService;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Java Swing client GUI of the Distributed Attendance Management System.
 * <p>
 * The window is a {@link JTabbedPane} with four task-oriented tabs that map
 * one-to-one onto the four remote services:
 * <ol>
 *   <li><b>Mark Attendance</b> — ID field + Present/Absent radio buttons;</li>
 *   <li><b>History</b> — full attendance log of one student;</li>
 *   <li><b>Percentage</b> — computed percentage with a progress bar;</li>
 *   <li><b>All Records</b> — {@link JTable} snapshot of every record.</li>
 * </ol>
 * <b>Responsiveness:</b> every remote call runs inside a
 * {@link SwingWorker} background thread so the Event Dispatch Thread is
 * never blocked by network latency; success and failure are reported with
 * clear dialogs.
 *
 * @author  Your Name (ID: YOUR-ID)
 * @version 1.0 — CSE 434 Lab Assignment, Spring 2026
 */
public final class ClientGUI {

    /** Remote proxy (stub) obtained from the RMI registry. */
    private final AttendanceService service;

    /** Main application window. */
    private final JFrame frame;

    /** Table model backing the "All Records" tab. */
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[] {"Student ID", "Present", "Total Classes",
                          "Percentage (%)"}, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false; // read-only snapshot table
        }
    };

    /**
     * Builds the complete client window.
     *
     * @param service     remote service proxy looked up from the registry
     * @param clientLabel window-title suffix, e.g. "Faculty Client 1"
     */
    public ClientGUI(AttendanceService service, String clientLabel) {
        this.service = service;

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Mark Attendance", buildMarkPanel());
        tabs.addTab("History", buildHistoryPanel());
        tabs.addTab("Percentage", buildPercentagePanel());
        tabs.addTab("All Records", buildAllRecordsPanel());

        frame = new JFrame("Attendance Management — " + clientLabel);
        frame.setContentPane(tabs);
        frame.setSize(700, 480);
        frame.setMinimumSize(frame.getSize());
        frame.setLocationByPlatform(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    /* --------------------------------------------------------------- */
    /* Tab 1: Mark Attendance                                          */
    /* --------------------------------------------------------------- */

    /**
     * Builds the attendance-marking form (GridBagLayout).
     *
     * @return fully wired panel for the first tab
     */
    private JPanel buildMarkPanel() {
        JTextField idField = new JTextField(16);
        JRadioButton presentButton = new JRadioButton("Present", true);
        JRadioButton absentButton = new JRadioButton("Absent");
        ButtonGroup statusGroup = new ButtonGroup();
        statusGroup.add(presentButton);
        statusGroup.add(absentButton);

        JButton submitButton = new JButton("Submit Attendance");
        JLabel resultLabel = new JLabel(" ");

        submitButton.addActionListener(event -> {
            String id = idField.getText().trim();
            if (id.isEmpty()) {
                showError("Please enter a Student ID.");
                return;
            }
            String status = presentButton.isSelected() ? "Present" : "Absent";
            runRemote(submitButton,
                    () -> service.markAttendance(id, status),
                    message -> {
                        resultLabel.setText(message);
                        JOptionPane.showMessageDialog(frame, message,
                                "Success", JOptionPane.INFORMATION_MESSAGE);
                    });
        });

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(new JLabel("Student ID:"), gbc);
        gbc.gridx = 1;
        form.add(idField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        form.add(new JLabel("Status:"), gbc);
        gbc.gridx = 1;
        JPanel radios = new JPanel();
        radios.add(presentButton);
        radios.add(absentButton);
        form.add(radios, gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;
        form.add(submitButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        form.add(resultLabel, gbc);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(form, BorderLayout.NORTH);
        return panel;
    }

    /* --------------------------------------------------------------- */
    /* Tab 2: History                                                  */
    /* --------------------------------------------------------------- */

    /**
     * Builds the per-student history viewer.
     *
     * @return fully wired panel for the second tab
     */
    private JPanel buildHistoryPanel() {
        JTextField idField = new JTextField(16);
        JButton fetchButton = new JButton("Fetch History");
        JTextArea historyArea = new JTextArea();
        historyArea.setEditable(false);
        historyArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

        fetchButton.addActionListener(event -> {
            String id = idField.getText().trim();
            if (id.isEmpty()) {
                showError("Please enter a Student ID.");
                return;
            }
            runRemote(fetchButton,
                    () -> service.getAttendanceHistory(id),
                    record -> {
                        StringBuilder text = new StringBuilder();
                        text.append("Attendance history for ")
                            .append(record.getStudentId())
                            .append(System.lineSeparator());
                        text.append("Summary: ").append(record)
                            .append(System.lineSeparator())
                            .append(System.lineSeparator());
                        for (String entry : record.getEntries()) {
                            text.append(entry).append(System.lineSeparator());
                        }
                        historyArea.setText(text.toString());
                        historyArea.setCaretPosition(0);
                    });
        });

        JPanel top = new JPanel();
        top.add(new JLabel("Student ID:"));
        top.add(idField);
        top.add(fetchButton);

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(historyArea), BorderLayout.CENTER);
        return panel;
    }

    /* --------------------------------------------------------------- */
    /* Tab 3: Percentage                                               */
    /* --------------------------------------------------------------- */

    /**
     * Builds the percentage calculator with a visual progress bar.
     *
     * @return fully wired panel for the third tab
     */
    private JPanel buildPercentagePanel() {
        JTextField idField = new JTextField(16);
        JButton calcButton = new JButton("Calculate Percentage");
        JLabel resultLabel = new JLabel("Enter a Student ID and press Calculate.");
        resultLabel.setFont(resultLabel.getFont().deriveFont(Font.BOLD, 16f));
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setStringPainted(true);

        calcButton.addActionListener(event -> {
            String id = idField.getText().trim();
            if (id.isEmpty()) {
                showError("Please enter a Student ID.");
                return;
            }
            runRemote(calcButton,
                    () -> service.getAttendancePercentage(id),
                    percentage -> {
                        resultLabel.setText(String.format(
                                "Attendance of %s: %.2f%%", id, percentage));
                        bar.setValue((int) Math.round(percentage));
                        bar.setString(String.format("%.2f%%", percentage));
                    });
        });

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(new JLabel("Student ID:"), gbc);
        gbc.gridx = 1;
        form.add(idField, gbc);
        gbc.gridx = 2;
        form.add(calcButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        form.add(bar, gbc);

        gbc.gridy = 2;
        form.add(resultLabel, gbc);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(form, BorderLayout.NORTH);
        return panel;
    }

    /* --------------------------------------------------------------- */
    /* Tab 4: All Records                                              */
    /* --------------------------------------------------------------- */

    /**
     * Builds the table of every stored record with a refresh button.
     *
     * @return fully wired panel for the fourth tab
     */
    private JPanel buildAllRecordsPanel() {
        JTable table = new JTable(tableModel);
        table.setFillsViewportHeight(true);
        JButton refreshButton = new JButton("Refresh All Records");

        refreshButton.addActionListener(event ->
                runRemote(refreshButton,
                        service::getAllStudentRecords,
                        this::populateTable));

        JPanel top = new JPanel();
        top.add(refreshButton);

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    /**
     * Replaces the table contents with a fresh snapshot from the server.
     *
     * @param recordList snapshot returned by the server
     */
    private void populateTable(List<AttendanceRecord> recordList) {
        tableModel.setRowCount(0);
        for (AttendanceRecord record : recordList) {
            tableModel.addRow(new Object[] {
                    record.getStudentId(),
                    record.getPresentCount(),
                    record.getTotalClasses(),
                    String.format("%.2f", record.getPercentage())
            });
        }
    }

    /* --------------------------------------------------------------- */
    /* Remote-call plumbing                                            */
    /* --------------------------------------------------------------- */

    /**
     * Small functional interface for remote calls that may throw
     * {@link RemoteException}.
     *
     * @param <T> result type of the remote call
     */
    @FunctionalInterface
    private interface RemoteCall<T> {
        /**
         * Performs the remote call.
         *
         * @return the server's response
         * @throws RemoteException if a communication error occurs
         */
        T call() throws RemoteException;
    }

    /**
     * Callback that consumes a successful remote result on the EDT.
     *
     * @param <T> result type of the remote call
     */
    @FunctionalInterface
    private interface SuccessHandler<T> {
        /**
         * Handles the successful result.
         *
         * @param result value returned by the server
         */
        void accept(T result);
    }

    /**
     * Executes one remote call on a {@link SwingWorker} background thread,
     * disabling the trigger button while the call is in flight (acts as a
     * loading indicator) and reporting any failure in an error dialog.
     *
     * @param trigger   button that started the call (disabled while busy)
     * @param call      the remote operation to perform
     * @param onSuccess EDT callback receiving the server's response
     * @param <T>       result type of the remote call
     */
    private <T> void runRemote(JButton trigger, RemoteCall<T> call,
                               SuccessHandler<T> onSuccess) {
        trigger.setEnabled(false);
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return call.call();
            }

            @Override
            protected void done() {
                trigger.setEnabled(true);
                try {
                    onSuccess.accept(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    showError("The operation was interrupted.");
                } catch (ExecutionException e) {
                    Throwable cause =
                            (e.getCause() != null) ? e.getCause() : e;
                    showError(rootMessage(cause));
                }
            }
        }.execute();
    }

    /**
     * Extracts the most user-relevant message from a (possibly nested)
     * RMI exception chain.
     *
     * @param throwable failure raised by the remote call
     * @return concise message suitable for an error dialog
     */
    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null
                && !(current instanceof IllegalArgumentException)) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return (message != null && !message.isEmpty())
                ? message
                : current.getClass().getSimpleName();
    }

    /**
     * Shows a modal error dialog.
     *
     * @param message text to display
     */
    private void showError(String message) {
        JOptionPane.showMessageDialog(frame, message,
                "Error", JOptionPane.ERROR_MESSAGE);
    }
}
