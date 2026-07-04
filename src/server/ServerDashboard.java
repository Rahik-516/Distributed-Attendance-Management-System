package server;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Font;

/**
 * Minimal Swing status console for the server.
 * <p>
 * Shows every remote request live — including the RMI worker-thread name —
 * which visually demonstrates that multiple clients are being served
 * concurrently by different threads.
 * <p>
 * All UI mutations are marshalled onto the Event Dispatch Thread via
 * {@link SwingUtilities#invokeLater(Runnable)}, because log lines arrive
 * from arbitrary RMI worker threads.
 *
 * @author  Your Name (ID: YOUR-ID)
 * @version 1.0 — CSE 434 Lab Assignment, Spring 2026
 */
public final class ServerDashboard {

    /** Main window of the dashboard. */
    private final JFrame frame = new JFrame("Attendance Server — Live Console");

    /** Scrolling log of every remote request. */
    private final JTextArea logArea = new JTextArea();

    /** Header label showing the total request count. */
    private final JLabel statusLabel =
            new JLabel("Server running — 0 requests served");

    /** Builds and shows the dashboard window. */
    public ServerDashboard() {
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logArea.setLineWrap(false);

        statusLabel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD));

        JPanel root = new JPanel(new BorderLayout());
        root.add(statusLabel, BorderLayout.NORTH);
        root.add(new JScrollPane(logArea), BorderLayout.CENTER);

        frame.setContentPane(root);
        frame.setSize(820, 420);
        frame.setLocationByPlatform(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    /**
     * Appends one log line and refreshes the request counter.
     * Safe to call from any thread.
     *
     * @param line         formatted log line to append
     * @param requestCount total number of requests served so far
     */
    public void appendLog(String line, long requestCount) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(line + System.lineSeparator());
            logArea.setCaretPosition(logArea.getDocument().getLength());
            statusLabel.setText(
                    "Server running — " + requestCount + " requests served");
        });
    }
}
