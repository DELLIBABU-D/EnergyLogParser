# Energy Usage Log Parser (Java)

A Java program that reads daily smart-meter usage logs, detects faulty readings with custom exceptions, stores valid readings as a compressed binary log, and serializes the smart meter's configuration profile.

## Objectives
1. Detect faulty meter readings with user-defined checked exceptions (`PowerSurgeException`, `MeterOfflineException`) and handle them without stopping the program.
2. Use Java NIO for file reading, `GZIPOutputStream` for compressed binary logging, and object serialization to store and reload SmartMeter configuration profiles.

## Features
- **Custom exceptions**
  - `PowerSurgeException`: voltage or kWh is above the meter's limit
  - `MeterOfflineException`: the meter reports `OFFLINE`
- **Java NIO**: `Paths`, `Files.write`, `Files.newBufferedReader`
- **Compressed binary log**: `DataOutputStream` → `GZIPOutputStream` → `usage_log.bin.gz`
- **Serialization**: the `SmartMeter` profile is saved to `meter_profile.ser` and loaded back. The `transient` field is not saved.

## Requirements
- JDK 17 or higher (`java -version`)

## How to Run
```bash
javac EnergyLogParser.java
java EnergyLogParser
```

## Input Format
Each line of `usage_2026-10-11.txt`:
```
time,meterId,kWh,voltage,status
08:00,MTR-101,1.20,230.5,ONLINE
```

## Output
```
Profile loaded : MTR-101 [maxV=250.0, maxKwh=3.0, token=null]
PowerSurgeException  : Surge on MTR-101 at 10:00 (265.8V, 1.1kWh)
MeterOfflineException: Meter MTR-101 offline at 11:00
Valid readings : 3, Total = 4.25 kWh
Text size = 159 bytes, Compressed binary size = 49 bytes
Binary log contents:
  08:00 -> 1.2 kWh
  09:00 -> 1.45 kWh
  12:00 -> 1.6 kWh
```

## Files Generated
| File | Description |
|------|-------------|
| `usage_2026-10-11.txt` | Daily usage text log (input) |
| `meter_profile.ser` | Serialized SmartMeter profile |
| `usage_log.bin.gz` | Compressed binary log of valid readings |

## Project Structure
```
EnergyLogParser.java
 ├── PowerSurgeException    (custom exception)
 ├── MeterOfflineException  (custom exception)
 ├── SmartMeter             (Serializable profile)
 └── EnergyLogParser        (main: NIO parse, GZIP write/read, serialization)
```

## Conclusion
Faulty readings were handled with custom exceptions, the log file was parsed with NIO, valid data was stored as a GZIP-compressed binary file, and the meter profile was serialized and deserialized.

## References
- Herbert Schildt, *Java: The Complete Reference*, 12th Ed., McGraw Hill
- [Java NIO Files API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/file/Files.html)
- [GZIPOutputStream](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/zip/GZIPOutputStream.html)
- [Oracle Java Tutorials](https://docs.oracle.com/javase/tutorial/essential/)
