package server;

import common.AttendanceService;

import javax.swing.SwingUtilities;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Entry point of the RMI server.
 * <p>
 * Startup sequence (standard RMI architecture):
 * <ol>
 *   <li>create (or attach to) the RMI registry on port 1099;</li>
 *   <li>instantiate {@link AttendanceServiceImpl}, which exports itself
 *       as a remote object with a dynamically generated stub;</li>
 *   <li>bind the object in the registry under
 *       {@link AttendanceService#SERVICE_NAME};</li>
 *   <li>open the Swing {@link ServerDashboard} so every concurrent
 *       request can be observed live.</li>
 * </ol>
 * From this point on the RMI runtime accepts connections and dispatches
 * each incoming call on its own worker thread (built-in multithreading).
 *
 * @author  Your Name (ID: YOUR-ID)
 * @version 1.0 — CSE 434 Lab Assignment, Spring 2026
 */
public final class AttendanceServer {

    /** JUL logger for server lifecycle events. */
    private static final Logger LOGGER =
            Logger.getLogger(AttendanceServer.class.getName());

    /** Utility class — not instantiable. */
    private AttendanceServer() {
    }

    /**
     * Starts the registry, exports and binds the service, opens the
     * dashboard, then keeps the JVM alive.
     *
     * @param args unused command-line arguments
     */
    public static void main(String[] args) {
        try {
            // 1. Create the dashboard on the Event Dispatch Thread.
            AtomicReference<ServerDashboard> dashboardRef =
                    new AtomicReference<>();
            CountDownLatch uiReady = new CountDownLatch(1);
            SwingUtilities.invokeLater(() -> {
                dashboardRef.set(new ServerDashboard());
                uiReady.countDown();
            });
            uiReady.await();
            ServerDashboard dashboard = dashboardRef.get();

            // 2. Create the remote object; wire its log output to both the
            //    dashboard and the terminal.
            AtomicReference<AttendanceServiceImpl> serviceRef =
                    new AtomicReference<>();
            AttendanceServiceImpl service = new AttendanceServiceImpl(line -> {
                System.out.println(line);
                AttendanceServiceImpl s = serviceRef.get();
                long count = (s != null) ? s.getRequestCount() : 0L;
                dashboard.appendLog(line, count);
            });
            serviceRef.set(service);

            // 3. Create the registry inside this JVM (preferred), or attach
            //    to an already-running external `rmiregistry`.
            Registry registry;
            try {
                registry = LocateRegistry
                        .createRegistry(AttendanceService.REGISTRY_PORT);
                LOGGER.info("RMI registry created on port "
                        + AttendanceService.REGISTRY_PORT);
            } catch (ExportException alreadyRunning) {
                registry = LocateRegistry
                        .getRegistry(AttendanceService.REGISTRY_PORT);
                LOGGER.info("Attached to existing RMI registry on port "
                        + AttendanceService.REGISTRY_PORT);
            }

            // 4. Bind (rebind is idempotent across server restarts).
            registry.rebind(AttendanceService.SERVICE_NAME, service);

            String banner = "AttendanceService bound as \""
                    + AttendanceService.SERVICE_NAME + "\" on port "
                    + AttendanceService.REGISTRY_PORT
                    + " — waiting for clients...";
            LOGGER.info(banner);
            dashboard.appendLog(banner, service.getRequestCount());

            // The exported remote object keeps a non-daemon RMI thread
            // alive, and the Swing window keeps the EDT alive, so main()
            // may simply return here.
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Server failed to start", e);
            System.exit(1);
        }
    }
}
