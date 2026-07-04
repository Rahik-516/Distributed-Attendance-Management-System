package test;

import common.AttendanceRecord;
import common.AttendanceService;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Headless concurrency stress test (no GUI) that PROVES the server's
 * thread safety over a real RMI connection.
 * <p>
 * It simulates 10 faculty clients, each marking attendance 100 times for
 * the same 5 students (1000 concurrent remote calls in total). If the
 * shared data structure were an unsynchronized {@code HashMap}, lost
 * updates or corruption would make the final counts wrong; with the
 * server's {@code ConcurrentHashMap.compute()} design, every count must be
 * exact.
 * <p>
 * <b>Usage:</b> start {@code server.AttendanceServer} first, then run
 * {@code java -cp out test.ConcurrencyTest}.
 *
 * @author  Your Name (ID: YOUR-ID)
 * @version 1.0 — CSE 434 Lab Assignment, Spring 2026
 */
public final class ConcurrencyTest {

    /** Number of simulated concurrent faculty clients. */
    private static final int CLIENTS = 10;

    /** Number of markAttendance calls issued by each client. */
    private static final int CALLS_PER_CLIENT = 100;

    /** Student IDs shared (and contended) by all simulated clients. */
    private static final String[] STUDENT_IDS =
            {"S-1001", "S-1002", "S-1003", "S-1004", "S-1005"};

    /** Utility class — not instantiable. */
    private ConcurrencyTest() {
    }

    /**
     * Runs the stress test and prints a PASS/FAIL verdict.
     *
     * @param args optional: {@code args[0]} = server host (default localhost)
     * @throws Exception if the registry lookup or thread handling fails
     */
    public static void main(String[] args) throws Exception {
        String host = (args.length > 0) ? args[0] : "localhost";
        Registry registry = LocateRegistry
                .getRegistry(host, AttendanceService.REGISTRY_PORT);
        AttendanceService service = (AttendanceService)
                registry.lookup(AttendanceService.SERVICE_NAME);

        ExecutorService pool = Executors.newFixedThreadPool(CLIENTS);
        CountDownLatch startGun = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(CLIENTS);

        for (int c = 0; c < CLIENTS; c++) {
            final int clientNo = c;
            pool.execute(() -> {
                try {
                    startGun.await(); // all clients fire simultaneously
                    for (int i = 0; i < CALLS_PER_CLIENT; i++) {
                        String id = STUDENT_IDS[i % STUDENT_IDS.length];
                        String status = (i % 2 == 0) ? "Present" : "Absent";
                        service.markAttendance(id, status);
                    }
                } catch (Exception e) {
                    System.err.println("Client " + clientNo
                            + " failed: " + e.getMessage());
                } finally {
                    finished.countDown();
                }
            });
        }

        long begin = System.nanoTime();
        startGun.countDown();
        boolean done = finished.await(60, TimeUnit.SECONDS);
        long elapsedMs = (System.nanoTime() - begin) / 1_000_000;
        pool.shutdown();

        if (!done) {
            System.err.println("FAIL: test timed out.");
            return;
        }

        // Each student receives (CLIENTS * CALLS_PER_CLIENT / #students)
        // entries, half Present and half Absent, so the expected
        // percentage is exactly 50%.
        int expectedTotal = CLIENTS * CALLS_PER_CLIENT / STUDENT_IDS.length;
        boolean pass = true;
        List<AttendanceRecord> all = service.getAllStudentRecords();
        for (AttendanceRecord record : all) {
            boolean ok = record.getTotalClasses() == expectedTotal
                    && record.getPresentCount() == expectedTotal / 2;
            System.out.printf("%s -> total=%d (expected %d), present=%d"
                            + " (expected %d), %.2f%% : %s%n",
                    record.getStudentId(), record.getTotalClasses(),
                    expectedTotal, record.getPresentCount(),
                    expectedTotal / 2, record.getPercentage(),
                    ok ? "OK" : "MISMATCH");
            pass &= ok;
        }

        System.out.printf("%n%d concurrent remote calls completed in %d ms%n",
                CLIENTS * CALLS_PER_CLIENT, elapsedMs);
        System.out.println(pass
                ? "RESULT: PASS — no lost updates, thread safety verified."
                : "RESULT: FAIL — race condition detected!");
    }
}
