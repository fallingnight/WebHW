import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.Set;
import java.util.Iterator;
import java.util.LinkedList;

public class NIOFileServer {

    class FileWorker {

        SocketChannel Channel;
        String filename;
        boolean firstflag;
        boolean finishflag;
        File infile;
        FileInputStream ist;
        FileChannel fcin;
        ByteBuffer buf;
        ByteBuffer test;

        FileWorker(SocketChannel clientChannel) {
            this.Channel = clientChannel;
            firstflag = true;
            finishflag = false;
            filename = "Sc1.mp4";
            buf = ByteBuffer.allocate(1024);
            test = ByteBuffer.wrap((filename + "\n").getBytes());
        }

        public void filetrans() {
            try {
                if (firstflag) {
                    Channel.write(test);
                    infile = new File(filename);
                    ist = new FileInputStream(infile);
                    fcin = ist.getChannel();
                    firstflag = false;
                } else {
                    int temp = fcin.read(buf);
                    if (temp == -1) {
                        if (buf.hasRemaining()) {
                            WriteSocketChannel(Channel, buf);
                        }
                        finishflag = true;
                        System.out.println(filename + " finished " + finishflag + " " + new Date());
                        return;
                    }
                    if (temp > 0) {
                        WriteSocketChannel(Channel, buf);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    void WriteSocketChannel(SocketChannel channel, ByteBuffer buf) throws IOException {
        buf.flip();
        while (buf.hasRemaining()) {
            channel.write(buf);
        }
        buf.clear();

    }

    public static void main(String[] args) throws IOException {
        NIOFileServer server = new NIOFileServer();
        server.launch();
    }

    void launch() throws IOException {
        int portNumber = 12001;
        ServerSocketChannel serverChannel = null;
        try {
            serverChannel = ServerSocketChannel.open();
            serverChannel.socket().bind(new InetSocketAddress(portNumber));
            serverChannel.configureBlocking(false);

            Selector selector = Selector.open();
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            LinkedList<FileWorker> fileworkerlist = new LinkedList<FileWorker>();

            while (true) {
                selector.select();
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> it = keys.iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove();
                    if (key.isAcceptable()) {
                        SocketChannel clientChannel = serverChannel.accept();
                        clientChannel.configureBlocking(false);
                        clientChannel.register(selector, SelectionKey.OP_WRITE);
                        fileworkerlist.add(new FileWorker(clientChannel));
                        System.out.println(new Date() + " connected");

                    } else if (key.isWritable()) {
                        SocketChannel clientChannel = (SocketChannel) key.channel();
                        Iterator<FileWorker> itf = fileworkerlist.iterator();
                        while (itf.hasNext()) {
                            FileWorker temp = itf.next();
                            if (temp.Channel.equals(clientChannel)) {
                                temp.filetrans();
                            }
                            if (temp.finishflag) {
                                temp.ist.close();
                                temp.Channel.close();
                                itf.remove();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            serverChannel.close();
        }
    }

}
