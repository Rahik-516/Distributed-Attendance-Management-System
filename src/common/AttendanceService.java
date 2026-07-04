package common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Remote interface of the Distributed Attendance Management System.
 * <p>
 * Every method declared here can be invoked remotely by any connected
 * client through Java RMI. Each method therefore declares
 * {@link RemoteException}, as required by the RMI specification.
 * <p>
 * The RMI runtime automatically generates a dynamic proxy (stub) for this
 * interface at export time, so the legacy {@code rmic} compiler is NOT
 * required (modern RMI practice, Java 5+).
 *
 * @author  Your Name (ID: YOUR-ID)
 * @version 1.0 — CSE 434 Lab Assignment, Spring 2026
 */
public interface AttendanceService extends Remote {

    /** Well-known name under which the service is bound in the RMI registry. */
    String SERVICE_NAME = "AttendanceService";

    /** Default TCP port of the RMI registry. */
    int REGISTRY_PORT = 1099;

    /**
     * Marks attendance (Present/Absent) for the student with the given ID.
     * If the student does not exist yet, a new record is created atomically.
     *
     * @param studentId unique, non-blank student identifier (e.g. "20251001")
     * @param status    attendance status; must be "Present" or "Absent"
     *                  (case-insensitive)
     * @return a human-readable confirmation message produced by the server
     * @throws IllegalArgumentException if the ID is blank or the status is
     *                                  neither "Present" nor "Absent"
     * @throws RemoteException          if a communication error occurs
     */
    String markAttendance(String studentId, String status) throws RemoteException;

    /**
     * Retrieves the complete attendance history of one student.
     *
     * @param studentId unique student identifier
     * @return a defensive copy of the student's {@link AttendanceRecord}
     * @throws IllegalArgumentException if no record exists for the ID
     * @throws RemoteException          if a communication error occurs
     */
    AttendanceRecord getAttendanceHistory(String studentId) throws RemoteException;

    /**
     * Calculates the attendance percentage of one student.
     *
     * @param studentId unique student identifier
     * @return percentage of "Present" entries, in the range [0.0, 100.0]
     * @throws IllegalArgumentException if no record exists for the ID
     * @throws RemoteException          if a communication error occurs
     */
    double getAttendancePercentage(String studentId) throws RemoteException;

    /**
     * Returns a snapshot of every attendance record stored on the server.
     *
     * @return list of defensive copies of all records, sorted by student ID
     * @throws RemoteException if a communication error occurs
     */
    List<AttendanceRecord> getAllStudentRecords() throws RemoteException;
}
