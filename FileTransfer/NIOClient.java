import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;

public class NIOClient {
    boolean firstflag;
    SocketChannel clientChannel;
    String hostName = "127.0.0.1";
    int portNumber = 12001;
    File outFile;
    boolean finishflag;
    FileOutputStream ost;
    FileChannel fcout;
    ByteBuffer buf;
    static char disc;

    NIOClient() {
        firstflag = true;
        finishflag = false;
        clientChannel = null;
        buf = ByteBuffer.allocate(1024);
    }

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        disc = scanner.next().charAt(0);
        NIOClient client = new NIOClient();
        client.launch();
    }

    void launch() {
        try {
            clientChannel = SocketChannel.open();
            clientChannel.configureBlocking(false);
            boolean connected = clientChannel.connect(new InetSocketAddress(hostName, portNumber));
            Selector selector = Selector.open();
            if (connected) {
                clientChannel.register(selector, SelectionKey.OP_READ);
            } else {
                clientChannel.register(selector, SelectionKey.OP_CONNECT);
            }

            while (true) {
                selector.select();
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> it = keys.iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove();
                    if (key.isReadable()) {
                        fileReceive();
                    } else if (key.isConnectable()) {
                        if (clientChannel.finishConnect()) {
                            clientChannel.register(selector, SelectionKey.OP_READ);
                        }
                    }

                }
                if (finishflag) {
                    System.out.println("this closed.");
                    this.clientChannel.close();
                    ost.close();
                    break;

                }
                // Thread.sleep(10);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void fileReceive() {
        try {
            if (firstflag) {
                String filename = readLine(clientChannel);
                System.out.println(filename);
                outFile = new File(disc + ":\\" + filename);
                ost = new FileOutputStream(outFile);
                fcout = ost.getChannel();
                firstflag = false;
                buf.reset();
                buf.compact();
                buf.flip();
                fcout.write(buf);
                buf.clear();

            } else {
                int temp = clientChannel.read(buf);
                if (temp == -1) {
                    finishflag = true;
                    return;
                }
                if (temp > 0) {
                    WriteHelp(buf, fcout);
                }
            }

        }

        catch (Exception e) {
            e.printStackTrace();
        }
    }

    void WriteHelp(ByteBuffer buf, FileChannel fcout) throws IOException {
        buf.flip();
        while (buf.hasRemaining()) {
            fcout.write(buf);
        }
        buf.clear();
    }

    String readLine(SocketChannel channel) throws IOException {
        StringBuilder sb = new StringBuilder();
        while (channel.read(buf) > 0) {
            buf.flip();
            while (buf.hasRemaining()) {
                char c = (char) buf.get();
                if (c == '\n') {
                    buf.mark();
                    return sb.toString();
                }
                sb.append(c);
            }
            buf.clear();
        }
        return null;
    }

}
