import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class Client {
    public static void main(String[] args) throws IOException {
        String hostName = "127.0.0.1";
        int portNumber = 12001;
        Socket clientSocket = null;

        try {
            clientSocket = new Socket(hostName, portNumber);
            InputStream ist = clientSocket.getInputStream();
            byte[] data = new byte[1024];
            ist.read(data, 0, 1024);
            String filename = new String(data).trim();
            System.out.println(filename);
            File outFile = new File("G:\\" + filename);
            OutputStream ost = new FileOutputStream(outFile);
            int temp = 0;
            while ((temp = ist.read(data)) != -1) {
                ost.write(data, 0, temp);
                //Thread.sleep(5);
            }
            ist.close();
            ost.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
