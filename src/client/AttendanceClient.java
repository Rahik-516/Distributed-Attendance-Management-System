package client;

import common.AttendanceService;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Entry point of the faculty client application.
 * <p>
 * Standard RMI client sequence:
 * <ol>
 *   <li>locate the registry (host from {@code args[0]}, default
 *       {@code localhost});</li>
 *   <li>look up the remote proxy bound under
 *       {@link AttendanceService#SERVICE_NAME};</li>
 *   <li>hand the proxy to the Swing {@link ClientGUI}.</li>
 * </ol>
 * Run several instances of this class simultaneously to demonstrate that
 * the server shares one data structure among all clients and serves them
 * concurrently.
 *
 * @author  Your Name (ID: YOUR-ID)
 * @version 1.0 — CSE 434 Lab Assignment, Spring 2026
 */
public final class AttendanceClient {

    /** JUL logger for client lifecycle events. */
    private static final Logger LOGGER =
            Logger.getLogger(AttendanceClient.class.getName());

    /** Utility class — not instantiable. */
    private AttendanceClient() {
    }

    /**
     * Connects to the server and opens the GUI.
     *
     * @param args optional: {@code args[0]} = server host name/IP
     *             (default "localhost"), {@code args[1]} = window label
     *             (default "Faculty Client")
     */
    public static void main(String[] args) {
        String host = (args.length > 0) ? args[0] : "localhost";
        String label = (args.length > 1) ? args[1] : "Faculty Client";

        try {
            Registry registry = LocateRegistry
                    .getRegistry(host, AttendanceService.REGISTRY_PORT);
            AttendanceService service = (AttendanceService)
                    registry.lookup(AttendanceService.SERVICE_NAME);
            LOGGER.info("Connected to AttendanceService at " + host + ":"
                    + AttendanceService.REGISTRY_PORT);

            SwingUtilities.invokeLater(() -> new ClientGUI(service, label));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Could not connect to server", e);
            JOptionPane.showMessageDialog(null,
                    "Could not connect to the Attendance Server at " + host
                            + ":" + AttendanceService.REGISTRY_PORT
                            + "\nPlease start the server first.\n\nDetails: "
                            + e.getMessage(),
                    "Connection Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
}
