package common;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Serializable data model representing one student's attendance record.
 * <p>
 * Instances of this class travel over the network between server and
 * clients, therefore the class implements {@link Serializable}.
 * <p>
 * <b>Thread safety:</b> all methods that touch the internal entry list are
 * {@code synchronized} on the record instance itself. Combined with the
 * atomic per-key operations of the server's {@code ConcurrentHashMap},
 * this guarantees race-free access even when many clients update the same
 * student concurrently.
 *
 * @author  Your Name (ID: YOUR-ID)
 * @version 1.0 — CSE 434 Lab Assignment, Spring 2026
 */
public final class AttendanceRecord implements Serializable {

    /** Serialization contract version (keeps client/server compatible). */
    private static final long serialVersionUID = 1L;

    /** Timestamp pattern used for every attendance entry. */
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Unique student identifier (immutable). */
    private final String studentId;

    /**
     * Chronological attendance entries, each formatted as
     * {@code "yyyy-MM-dd HH:mm:ss | STATUS"}.
     */
    private final ArrayList<String> entries = new ArrayList<>();

    /** Number of entries whose status is "Present". */
    private int presentCount;

    /**
     * Creates an empty attendance record for one student.
     *
     * @param studentId unique student identifier, must not be blank
     */
    public AttendanceRecord(String studentId) {
        this.studentId = studentId;
    }

    /**
     * Appends one attendance entry with the current server timestamp.
     *
     * @param status normalized status, either "Present" or "Absent"
     */
    public synchronized void addEntry(String status) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        entries.add(timestamp + " | " + status);
        if ("Present".equals(status)) {
            presentCount++;
        }
    }

    /** @return the unique student identifier */
    public String getStudentId() {
        return studentId;
    }

    /** @return an unmodifiable snapshot of all attendance entries */
    public synchronized List<String> getEntries() {
        return Collections.unmodifiableList(new ArrayList<>(entries));
    }

    /** @return total number of classes recorded for this student */
    public synchronized int getTotalClasses() {
        return entries.size();
    }

    /** @return number of classes in which the student was present */
    public synchronized int getPresentCount() {
        return presentCount;
    }

    /**
     * Computes the attendance percentage of this student.
     *
     * @return {@code presentCount / totalClasses * 100}, or 0.0 when the
     *         record contains no entries yet
     */
    public synchronized double getPercentage() {
        return entries.isEmpty()
                ? 0.0
                : (presentCount * 100.0) / entries.size();
    }

    /**
     * Produces a deep copy of this record. The server hands out copies so
     * that client-side objects can never alias the server's shared state.
     *
     * @return an independent copy of this record
     */
    public synchronized AttendanceRecord copy() {
        AttendanceRecord clone = new AttendanceRecord(studentId);
        clone.entries.addAll(entries);
        clone.presentCount = presentCount;
        return clone;
    }

    /** @return concise one-line summary used in logs and tables */
    @Override
    public synchronized String toString() {
        return String.format("%s | Present: %d/%d | %.2f%%",
                studentId, presentCount, entries.size(), getPercentage());
    }
}
