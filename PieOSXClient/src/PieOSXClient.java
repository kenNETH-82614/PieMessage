import java.io.File;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by eric on 11/17/15.
 */
public class PieOSXClient {

    private static String address;
    private static int port;

    private PieOSXClient() {
        doesMessageScriptExist();   // Ensure we have a ~/messages.applescript;

        // Start outgoing thread
        OSXOutgoingMessageThread outgoingThread = new OSXOutgoingMessageThread();
        Thread outThread = new Thread(outgoingThread);
        outgoingThread.setThread(outThread);
        outThread.start();

        // Start incoming thread
        OSXIncomingMessageThread incomingThread = new OSXIncomingMessageThread();
        Thread inThread = new Thread(incomingThread);
        incomingThread.setThread(inThread);
        inThread.start();
    }

    public static void main(String[] args) {
        if (args.length > 0) {
            address = args[0];
            port = 5000;
            if (address.contains(":")) {
                port = Integer.parseInt(address.split(":")[1]);
                address = address.split(":")[0];
            }
            new PieOSXClient();
        } else System.out.println("Please provide your.public.ip.address:port as an argument (:port is optional)");
    }

    public static String getDateString() {
        Date d = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);

        int month = cal.get(Calendar.MONTH) + 1;
        int dateNum = cal.get(Calendar.DAY_OF_MONTH);
        int year = cal.get(Calendar.YEAR);

        String dateString = year + "-" + month + "-" + dateNum;

        return dateString;
    }

    public static String getHomeDirectory() {
        return System.getProperty("user.home");
    }

    public static String getAddress() {
        return address;
    }

    public static int getPort() {
        return port;
    }

    private boolean doesMessageScriptExist() {
        String filePath = getHomeDirectory() + "/messages.applescript";
        File messagesFile = new File(filePath);

        if (messagesFile.exists()) {
            System.out.println("File \"/messages.applescript\" exists");
        } else {
            System.out.println("WARNING - \"/messages.applescript\" does NOT exist");
        }

        return messagesFile.exists();
    }
}
