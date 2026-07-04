package server;

import common.AttendanceRecord;
import common.AttendanceService;

import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Server-side implementation of {@link AttendanceService}.
 * <p>
 * <b>Multithreading model:</b> the RMI runtime dispatches every incoming
 * remote call on its own worker thread, so many faculty clients may execute
 * these methods at the same time. Shared state is therefore protected by:
 * <ul>
 *   <li>a {@link ConcurrentHashMap} as the single shared data structure
 *       (requirement: in-memory HashMap-style storage, thread-safe);</li>
 *   <li>atomic per-key updates via
 *       {@code ConcurrentHashMap.compute()},
 *       which locks only the affected map bin — no global lock, so
 *       different students can be updated fully in parallel;</li>
 *   <li>{@code synchronized} accessors inside {@link AttendanceRecord}
 *       for consistent reads of a record's entry list;</li>
 *   <li>an {@link AtomicLong} request counter for lock-free statistics.</li>
 * </ul>
 * The server never returns references to its internal records; every
 * response is a deep {@linkplain AttendanceRecord#copy() copy}.
 *
 * @author  Your Name (ID: YOUR-ID)
 * @version 1.0 — CSE 434 Lab Assignment, Spring 2026
 */
public final class AttendanceServiceImpl extends UnicastRemoteObject
        implements AttendanceService {

    /** Serialization contract version required by UnicastRemoteObject. */
    private static final long serialVersionUID = 1L;

    /**
     * Shared, thread-safe, in-memory data structure holding every
     * attendance record, keyed by unique Student ID.
     */
    private final ConcurrentHashMap<String, AttendanceRecord> records =
            new ConcurrentHashMap<>();

    /** Total number of remote requests served (lock-free counter). */
    private final AtomicLong requestCount = new AtomicLong();

    /** Sink that receives log lines (console and/or server dashboard). */
    private final transient Consumer<String> logSink;

    /**
     * Exports this remote object on an anonymous port using a dynamically
     * generated proxy stub (modern RMI — no {@code rmic} needed).
     *
     * @param logSink consumer that receives human-readable log lines
     * @throws RemoteException if the object cannot be exported
     */
    public AttendanceServiceImpl(Consumer<String> logSink) throws RemoteException {
        super();
        this.logSink = logSink;
    }

    /** {@inheritDoc} */
    @Override
    public String markAttendance(String studentId, String status)
            throws RemoteException {
        String id = validateId(studentId);
        String normalized = normalizeStatus(status);

        // Atomic create-or-update: compute() locks only this key's bin,
        // so concurrent updates to the SAME student are serialized while
        // updates to DIFFERENT students proceed in parallel.
        AttendanceRecord updated = records.compute(id, (key, existing) -> {
            AttendanceRecord record =
                    (existing != null) ? existing : new AttendanceRecord(key);
            record.addEntry(normalized);
            return record;
        });

        String message = String.format(
                "Attendance marked: %s -> %s (total classes: %d, %.2f%%)",
                id, normalized, updated.getTotalClasses(), updated.getPercentage());
        log("markAttendance", message);
        return message;
    }

    /** {@inheritDoc} */
    @Override
    public AttendanceRecord getAttendanceHistory(String studentId)
            throws RemoteException {
        String id = validateId(studentId);
        AttendanceRecord record = requireRecord(id);
        log("getAttendanceHistory", "History requested for " + id);
        return record.copy();
    }

    /** {@inheritDoc} */
    @Override
    public double getAttendancePercentage(String studentId)
            throws RemoteException {
        String id = validateId(studentId);
        double percentage = requireRecord(id).getPercentage();
        log("getAttendancePercentage",
                String.format("%s -> %.2f%%", id, percentage));
        return percentage;
    }

    /** {@inheritDoc} */
    @Override
    public List<AttendanceRecord> getAllStudentRecords() throws RemoteException {
        // Snapshot: deep-copy every record so clients never alias shared state.
        List<AttendanceRecord> snapshot = new ArrayList<>();
        records.values().forEach(record -> snapshot.add(record.copy()));
        snapshot.sort(Comparator.comparing(AttendanceRecord::getStudentId));
        log("getAllStudentRecords",
                "Full snapshot requested (" + snapshot.size() + " records)");
        return snapshot;
    }

    /** @return total number of remote requests served so far */
    public long getRequestCount() {
        return requestCount.get();
    }

    /**
     * Validates and trims a student ID.
     *
     * @param studentId raw ID received from the client
     * @return trimmed, validated ID
     * @throws IllegalArgumentException if the ID is null or blank
     */
    private static String validateId(String studentId) {
        if (studentId == null || studentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Student ID must not be empty.");
        }
        return studentId.trim();
    }

    /**
     * Normalizes an attendance status string.
     *
     * @param status raw status received from the client
     * @return exactly "Present" or "Absent"
     * @throws IllegalArgumentException if the status is anything else
     */
    private static String normalizeStatus(String status) {
        if (status != null) {
            String s = status.trim();
            if (s.equalsIgnoreCase("Present")) {
                return "Present";
            }
            if (s.equalsIgnoreCase("Absent")) {
                return "Absent";
            }
        }
        throw new IllegalArgumentException(
                "Status must be either \"Present\" or \"Absent\".");
    }

    /**
     * Fetches an existing record or fails with a clear message.
     *
     * @param id validated student ID
     * @return the live server-side record
     * @throws IllegalArgumentException if the student is unknown
     */
    private AttendanceRecord requireRecord(String id) {
        AttendanceRecord record = records.get(id);
        if (record == null) {
            throw new IllegalArgumentException(
                    "No attendance record found for Student ID: " + id);
        }
        return record;
    }

    /**
     * Emits one log line containing the calling thread, the client host and
     * the running request counter — this makes the multithreaded dispatch
     * visible in the server dashboard.
     *
     * @param operation remote method name
     * @param details   human-readable description of what happened
     */
    private void log(String operation, String details) {
        long count = requestCount.incrementAndGet();
        String clientHost;
        try {
            clientHost = RemoteServer.getClientHost();
        } catch (ServerNotActiveException e) {
            clientHost = "local";
        }
        String line = String.format("[req #%d] [%s] [client %s] %s: %s",
                count, Thread.currentThread().getName(), clientHost,
                operation, details);
        if (logSink != null) {
            logSink.accept(line);
        }
    }
}
