import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

class PowerSurgeException extends Exception {
    PowerSurgeException(String msg) { super(msg); }
}

class MeterOfflineException extends Exception {
    MeterOfflineException(String msg) { super(msg); }
}

class SmartMeter implements Serializable {
    private static final long serialVersionUID = 1L;
    String meterId;
    double maxVoltage;
    double maxKwh;
    transient String sessionToken;

    SmartMeter(String id, double maxV, double maxK) {
        meterId = id; maxVoltage = maxV; maxKwh = maxK; sessionToken = "TOKEN-" + id;
    }
    public String toString() {
        return meterId + " [maxV=" + maxVoltage + ", maxKwh=" + maxKwh + ", token=" + sessionToken + "]";
    }
}

public class EnergyLogParser {

    static void validate(String[] p, SmartMeter m) throws PowerSurgeException, MeterOfflineException {
        if (p[4].equals("OFFLINE"))
            throw new MeterOfflineException("Meter " + p[1] + " offline at " + p[0]);
        double kwh = Double.parseDouble(p[2]);
        double volt = Double.parseDouble(p[3]);
        if (volt > m.maxVoltage || kwh > m.maxKwh)
            throw new PowerSurgeException("Surge on " + p[1] + " at " + p[0] + " (" + volt + "V, " + kwh + "kWh)");
    }

    public static void main(String[] args) throws Exception {
        Path txt = Paths.get("usage_2026-10-11.txt");
        Path bin = Paths.get("usage_log.bin.gz");
        Path ser = Paths.get("meter_profile.ser");

        // 1. Create a daily usage text file using NIO
        List<String> lines = List.of(
            "08:00,MTR-101,1.20,230.5,ONLINE",
            "09:00,MTR-101,1.45,231.0,ONLINE",
            "10:00,MTR-101,1.10,265.8,ONLINE",
            "11:00,MTR-101,0.00,0.0,OFFLINE",
            "12:00,MTR-101,1.60,229.4,ONLINE");
        Files.write(txt, lines);

        // 2. Serialize the SmartMeter configuration profile
        SmartMeter meter = new SmartMeter("MTR-101", 250.0, 3.0);
        try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(ser))) {
            oos.writeObject(meter);
        }
        SmartMeter loaded;
        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(ser))) {
            loaded = (SmartMeter) ois.readObject();
        }
        System.out.println("Profile loaded : " + loaded);

        // 3. Parse the text file with NIO and write valid readings as compressed binary
        int valid = 0;
        double total = 0;
        try (BufferedReader br = Files.newBufferedReader(txt);
             DataOutputStream dos = new DataOutputStream(
                     new GZIPOutputStream(Files.newOutputStream(bin)))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",");
                try {
                    validate(p, loaded);
                    dos.writeUTF(p[0]);
                    dos.writeDouble(Double.parseDouble(p[2]));
                    valid++;
                    total += Double.parseDouble(p[2]);
                } catch (PowerSurgeException e) {
                    System.out.println("PowerSurgeException  : " + e.getMessage());
                } catch (MeterOfflineException e) {
                    System.out.println("MeterOfflineException: " + e.getMessage());
                }
            }
        }

        // 4. Read the compressed binary log back
        System.out.println("Valid readings : " + valid + ", Total = " + String.format("%.2f", total) + " kWh");
        System.out.println("Text size = " + Files.size(txt) + " bytes, Compressed binary size = " + Files.size(bin) + " bytes");
        System.out.println("Binary log contents:");
        try (DataInputStream dis = new DataInputStream(
                new GZIPInputStream(Files.newInputStream(bin)))) {
            for (int i = 0; i < valid; i++)
                System.out.println("  " + dis.readUTF() + " -> " + dis.readDouble() + " kWh");
        }
    }
}
