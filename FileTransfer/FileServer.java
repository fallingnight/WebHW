import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class FileServer {

    class Worker implements Runnable {

        Socket socket;

        Worker(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                String filename = "j4.mp3";
                byte[] data = new byte[1024];
                byte[] test = filename.getBytes();
                System.out.println("connected");
                OutputStream ost = socket.getOutputStream();
                ost.write(test, 0, test.length);
                File infile = new File(filename);
                InputStream ist = new FileInputStream(infile);
                int temp = 0;
                while ((temp = ist.read(data)) != -1) {
                    ost.write(data, 0, temp);
                }
                System.out.println(socket.getPort() + " " + filename + " finished.");
                ist.close();
                ost.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        FileServer server = new FileServer();
        server.launch();
    }

    void launch() throws IOException {
        int portNumber = 12001;
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(portNumber);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                (new Thread(new Worker(clientSocket))).start();
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            serverSocket.close();
        }
    }

}
