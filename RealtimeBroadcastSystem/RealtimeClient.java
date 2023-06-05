import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

public class RealtimeClient {
    boolean firstflag;
    SocketChannel clientChannel;
    String hostName = "127.0.0.1";
    int portNumber = 12001;
    File outFile;
    boolean finishflag;
    FileOutputStream ost;
    FileChannel fcout;
    ByteBuffer buf;

    class HeartbeatWorker implements Runnable {
        boolean ready;

        HeartbeatWorker() {
            this.ready = true;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    Thread.sleep(5000);
                    this.ready = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    RealtimeClient() {
        firstflag = true;
        finishflag = false;
        clientChannel = null;
        buf = ByteBuffer.allocate(4096);
    }

    public static void main(String[] args) throws IOException {
        RealtimeClient client = new RealtimeClient();
        client.launch();
    }

    void InfoReceiver() throws IOException {
        if (firstflag) {
            outFile = new File("realtime receive " + clientChannel.socket().getLocalPort() + ".txt");
            ost = new FileOutputStream(outFile);
            firstflag = false;
        } else {
            int temp = clientChannel.read(buf);
            if (temp > 0) {
                PrintHelp(buf);
            }
        }
    }

    void PrintHelp(ByteBuffer buf) throws IOException {
        buf.flip();
        while (buf.hasRemaining()) {
            byte temp = buf.get();
            ost.write(temp);
            System.out.print((char) temp);
        }
        buf.clear();
    }

    void launch() {
        try {
            clientChannel = SocketChannel.open();
            clientChannel.configureBlocking(false);
            boolean connected = clientChannel.connect(new InetSocketAddress(hostName, portNumber));
            Selector selector = Selector.open();
            if (connected) {
                clientChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            } else {
                clientChannel.register(selector, SelectionKey.OP_CONNECT);
            }

            HeartbeatWorker hbw = new HeartbeatWorker();
            Thread hbwt = (new Thread(hbw));
            hbwt.setDaemon(true);
            hbwt.start();
            while (true) {
                selector.select();
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> it = keys.iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove();

                    if (key.isConnectable()) {
                        if (clientChannel.finishConnect()) {
                            clientChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                        }
                    }
                    if (key.isReadable()) {
                        try {
                            InfoReceiver();
                        } catch (Exception e) {
                            finishflag = true;
                        }
                    }
                    if (key.isWritable() && hbw.ready) {
                        ByteBuffer heartbeattext = ByteBuffer.wrap(("I am here.\n").getBytes());
                        try {
                            clientChannel.write(heartbeattext);
                        } catch (Exception e) {
                            finishflag = true;
                        }
                        hbw.ready = false;
                    }

                }
                if (finishflag) {
                    System.out.println("this closed.");
                    this.clientChannel.close();
                    ost.close();
                    break;

                }
                Thread.sleep(30000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
