# Distributed Attendance Management System (Java RMI + Swing)

**Course:** CSE 434 — Parallel Processing and Distributed System Lab (Spring 2026)
**Student:** Your Name — ID: YOUR-ID

A multithreaded client–server attendance system. Multiple faculty clients
connect to one RMI server that stores all records in a shared, thread-safe
`ConcurrentHashMap`.

## Project structure

```
AttendanceRMI/
├── src/
│   ├── common/
│   │   ├── AttendanceService.java      # Remote interface (RMI contract)
│   │   └── AttendanceRecord.java       # Serializable data model
│   ├── server/
│   │   ├── AttendanceServiceImpl.java  # Remote object (thread-safe logic)
│   │   ├── AttendanceServer.java       # Bootstrap: registry + binding
│   │   └── ServerDashboard.java        # Swing live-request console
│   ├── client/
│   │   ├── AttendanceClient.java       # Bootstrap: lookup + GUI launch
│   │   └── ClientGUI.java              # Swing GUI (4 tabs)
│   └── test/
│       └── ConcurrencyTest.java        # Headless thread-safety stress test
├── compile.sh / compile.bat
├── run_server.sh / run_server.bat
├── run_client.sh / run_client.bat
└── README.md
```

## Requirements

- JDK 11 or newer (`java -version` to check). No external libraries.

## How to run

### 1. Compile

```bash
# Linux / macOS
./compile.sh
# Windows
compile.bat
# Or manually:
javac -d out src/common/*.java src/server/*.java src/client/*.java src/test/*.java
```

### 2. Start the RMI registry

Not needed as a separate step — `AttendanceServer` calls
`LocateRegistry.createRegistry(1099)` and hosts the registry itself
(modern practice). If you prefer the classic external registry:

```bash
cd out
rmiregistry 1099 &        # Windows: start rmiregistry 1099
cd ..
```

The server detects the existing registry and attaches to it.

### 3. Launch the server

```bash
./run_server.sh           # Windows: run_server.bat
# manually: java -cp out server.AttendanceServer
```

A "Live Console" window opens and logs every remote request with the RMI
worker-thread name.

### 4. Launch multiple clients (in separate terminals)

```bash
./run_client.sh localhost "Faculty Client 1"
./run_client.sh localhost "Faculty Client 2"
./run_client.sh localhost "Faculty Client 3"
# manually: java -cp out client.AttendanceClient localhost "Faculty Client 1"
```

To connect from another machine, pass the server's IP as the first
argument and start the server with
`java -Djava.rmi.server.hostname=<server-ip> -cp out server.AttendanceServer`.

### 5. (Optional) Prove thread safety

With the server running:

```bash
java -cp out test.ConcurrencyTest
```

10 simulated clients fire 1000 concurrent `markAttendance` calls at 5
shared students. The test PASSES only if no update is lost — verified
output: every student ends with exactly 200 entries, 100 Present, 50.00%.

## Thread-safety design (summary)

| Mechanism | Where | Purpose |
|---|---|---|
| RMI worker threads | RMI runtime | Each client call runs on its own thread |
| `ConcurrentHashMap` | `AttendanceServiceImpl` | Shared store; lock striping per bin |
| `map.compute()` | `markAttendance` | Atomic create-or-update per Student ID |
| `synchronized` methods | `AttendanceRecord` | Consistent reads/writes of entry list |
| `AtomicLong` | request counter | Lock-free statistics |
| Deep copies | all getters | Clients never alias server state |
| `SwingWorker` / `invokeLater` | both GUIs | All UI updates on the EDT |
