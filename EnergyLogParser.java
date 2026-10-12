import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

class PowerSurgeException extends Exception {
    PowerSurgeException(String m) { super(m); }
}
class MeterOfflineException extends Exception {
    MeterOfflineException(String m) { super(m); }
}
class SmartMeter implements Serializable {
    int maxVolt = 250;
}

public class Energy {
    public static void main(String[] args) throws Exception {
        Files.write(Paths.get("usage.txt"), List.of("230", "265", "OFF", "228"));

        new ObjectOutputStream(new FileOutputStream("meter.ser")).writeObject(new SmartMeter());

        DataOutputStream out = new DataOutputStream(new GZIPOutputStream(new FileOutputStream("log.gz")));
        for (String s : Files.readAllLines(Paths.get("usage.txt"))) {
            try {
                if (s.equals("OFF")) throw new MeterOfflineException("Meter offline");
                int v = Integer.parseInt(s);
                if (v > 250) throw new PowerSurgeException("Surge " + v + "V");
                out.writeInt(v);
                System.out.println("Saved " + v + "V");
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
        out.close();
    }
}
