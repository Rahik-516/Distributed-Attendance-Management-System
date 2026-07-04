#!/bin/bash
# Start one faculty client. Usage: ./run_client.sh [host] ["Window Label"]
java -cp out client.AttendanceClient "${1:-localhost}" "${2:-Faculty Client}"
