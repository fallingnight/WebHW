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
import java.util.Random;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Vector;
import java.net.SocketAddress;
import java.util.concurrent.locks.ReentrantLock;

public class RealtimeServer {
    static final String FILE_NAME = new String("realtime info.txt");
    static final int DELAY_TIME = 250;
    static final int BUFFER_INT_SIZE = 5;
    private final ReentrantLock lock;
    private File file;
    private PrintWriter outFile;
    private int curSerialNum;
    private ServerSocketChannel serverChannel;
    int portNumber;
    int buffer[];
    int count;

    RealtimeServer() {
        portNumber = 12001;
        serverChannel = null;
        lock = new ReentrantLock();
    }

    class BroadCastWorker {
        private SocketChannel Channel;
        private int serialNumMark;
        private FileInputStream ist;
        private int port;
        private boolean isAlive;
        private boolean hasHistory;
        private boolean isCurFinished;
        private FileChannel fcin;
        private boolean firstflag;
        private ByteBuffer buf;
        ByteBuffer tempBuf1;
        ByteBuffer tempBuf2;
        int bufferedCurInt[];
        int bufferindex;
        private int readlinecount;
        private Date lastHeartbeatTime = new Date();

        BroadCastWorker(SocketChannel clientChannel, int serialNum) {
            try {
                this.Channel = clientChannel;
                this.port = clientChannel.socket().getPort();
                this.serialNumMark = serialNum;
                isAlive = true;
                hasHistory = true;
                readlinecount = 0;
                firstflag = true;
                isCurFinished = true;
                this.bufferedCurInt = new int[BUFFER_INT_SIZE];
                bufferindex = 0;
                String test = 100 + "";
                for (int i = 0; i < BUFFER_INT_SIZE - 1; i++) {
                    test = test + " " + 100;
                }
                buf = ByteBuffer.wrap((test + "\n").getBytes());
                String test1 = 100 + " ";
                tempBuf1 = ByteBuffer.wrap(test1.getBytes());
                tempBuf1.rewind();
                String test2 = 100 + "\n";
                tempBuf2 = ByteBuffer.wrap(test2.getBytes());
                tempBuf2.rewind();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public Date getHB() {
            return lastHeartbeatTime;
        }

        public void setHB(Date lastHeartbeatTime) {
            this.lastHeartbeatTime = lastHeartbeatTime;
        }

        @Override
        public String toString() {
            return new String("port:" + port);
        }

        public void writeHelp() throws Exception {
            if (firstflag) {
                file = new File(FILE_NAME);
                ist = new FileInputStream(file);
                fcin = ist.getChannel();
                firstflag = false;
            } else {
                if (hasHistory && isCurFinished) {
                    fcin.read(buf);
                    if (readlinecount == serialNumMark) {
                        WriteSocketChannel(Channel, buf);
                        serialNumMark++;
                        hasHistory = false;
                        return;
                    }
                    WriteSocketChannel(Channel, buf);
                    readlinecount++;
                } else if (!isCurFinished) {
                    // System.out.println(bufferedCurInt[bufferindex]);
                    if (bufferindex != BUFFER_INT_SIZE - 1) {
                        tempBuf1.put((bufferedCurInt[bufferindex] + " ").getBytes());
                        WriteSocketChannel(Channel, tempBuf1);
                        bufferindex = (bufferindex + 1) % BUFFER_INT_SIZE;
                    } else {
                        tempBuf2.put((bufferedCurInt[bufferindex] + "\n").getBytes());
                        WriteSocketChannel(Channel, tempBuf2);
                        bufferindex = (bufferindex + 1) % BUFFER_INT_SIZE;
                        serialNumMark++;
                        readlinecount++;
                        this.isCurFinished = true;
                    }
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

        public void close() {
            try {
                if (isAlive) {
                    System.out.println(this + " close...");
                    this.Channel.close();
                    isAlive = false;
                } else {
                    this.Channel.close();
                    System.out.println(this + " has been already closed.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    class HBMonitor implements Runnable {
        private Vector<BroadCastWorker> workers = null;

        HBMonitor(Vector<BroadCastWorker> workers) {
            this.workers = workers;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    lock.lock();
                    try {
                        Iterator<BroadCastWorker> iterWorker = workers.iterator();
                        while (iterWorker.hasNext()) {
                            BroadCastWorker worker = iterWorker.next();
                            long diffTime = (new Date()).getTime() - worker.getHB().getTime();
                            if (!worker.isAlive) {
                                iterWorker.remove();
                            } else if (diffTime >= 20000) {
                                System.out.println(worker + " heartbeat timeout...");
                                worker.isAlive = false;
                            }

                        }
                    } finally {
                        lock.unlock();
                    }
                    Thread.sleep(2000);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    class RealtimeWorker implements Runnable {
        private Random randominfo;
        private int delay;
        private boolean firstflag;
        private Vector<BroadCastWorker> workers;
        boolean ready;

        RealtimeWorker(int time, Vector<BroadCastWorker> workers) {
            randominfo = new Random();
            firstflag = true;
            delay = time;
            this.workers = workers;
            buffer = new int[BUFFER_INT_SIZE];
            count = 0;
            ready = true;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    int temp = randominfo.nextInt(101);
                    buffer[count] = temp;
                    ready = true;
                    if (count == BUFFER_INT_SIZE - 1) {
                        WriteInfo(buffer);
                        curSerialNum += 1;
                    }
                    SaveBuffer();
                    count = (count + 1) % BUFFER_INT_SIZE;
                    Thread.sleep(delay);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        void WriteInfo(int[] buffer) throws Exception {
            if (firstflag) {
                outFile = new PrintWriter(new FileWriter(file));
                firstflag = false;
            } else {
                outFile = new PrintWriter(new FileWriter(file, true));
            }
            for (int i = 0; i < buffer.length; i++) {
                outFile.print(buffer[i] + " ");
            }
            outFile.println();
            outFile.close();
        }

        void SaveBuffer() {
            lock.lock();
            Iterator<BroadCastWorker> itf = workers.iterator();
            try {
                while (itf.hasNext()) {
                    BroadCastWorker temp = itf.next();
                    if (temp.readlinecount == (temp.serialNumMark - 1) && temp.serialNumMark == curSerialNum) {
                        temp.bufferedCurInt = buffer;
                        if (!temp.hasHistory && temp.bufferindex != BUFFER_INT_SIZE - 1) {
                            temp.isCurFinished = false;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }

    }

    public static void main(String[] args) throws IOException {
        RealtimeServer server = new RealtimeServer();
        server.launch();
    }

    void launch() throws IOException {
        curSerialNum = 0;
        try {
            file = new File("realtime info.txt");
            serverChannel = ServerSocketChannel.open();
            serverChannel.socket().bind(new InetSocketAddress(portNumber));
            serverChannel.configureBlocking(false);

            Selector selector = Selector.open();
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            Vector<BroadCastWorker> workers = new Vector<BroadCastWorker>();
            RealtimeWorker Infoworker = new RealtimeWorker(DELAY_TIME, workers);
            (new Thread(Infoworker)).start();
            (new Thread(new HBMonitor(workers))).start();
            while (true) {
                selector.select();
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> it = keys.iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove();
                    if (key.isValid() && key.isAcceptable()) {
                        SocketChannel clientChannel = serverChannel.accept();
                        clientChannel.configureBlocking(false);
                        clientChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                        BroadCastWorker bcw = new BroadCastWorker(clientChannel, curSerialNum);
                        workers.add(bcw);
                        System.out.println(bcw + " " + new Date() + " connected");

                    }
                    if (key.isValid() && key.isWritable()) {
                        SocketChannel clientChannel = (SocketChannel) key.channel();
                        writeThisWorker(workers, clientChannel);

                    }
                    if (key.isValid() && key.isReadable()) {
                        SocketChannel clientChannel = (SocketChannel) key.channel();
                        readThisWorker(workers, clientChannel);

                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            serverChannel.close();
        }
    }

    void readThisWorker(Vector<BroadCastWorker> workers, SocketChannel clientChannel) {
        lock.lock();
        Iterator<BroadCastWorker> itf = workers.iterator();
        BroadCastWorker temp = new BroadCastWorker(clientChannel, curSerialNum);
        try {
            while (itf.hasNext()) {
                temp = itf.next();
                if (temp.Channel.equals(clientChannel)) {

                    ByteBuffer hbbuf = ByteBuffer.allocate(1024);
                    String hb = null;
                    try {
                        hb = readLine(temp.Channel, hbbuf);
                    } catch (IOException e) {
                        temp.close();
                    }
                    if (hb != null) {
                        System.out.println(temp + " " + hb);
                        temp.setHB(new Date());
                    }
                }
                if (!temp.isAlive) {
                    temp.close();
                    itf.remove();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    void writeThisWorker(Vector<BroadCastWorker> workers, SocketChannel clientChannel) {
        lock.lock();
        Iterator<BroadCastWorker> itf = workers.iterator();
        try {
            while (itf.hasNext()) {
                BroadCastWorker temp = itf.next();
                if (temp.Channel.equals(clientChannel)) {
                    if (curSerialNum - temp.serialNumMark >= 1) {
                        temp.serialNumMark = curSerialNum;
                        temp.hasHistory = true;
                        System.out.println("test.");
                    }

                    try {
                        temp.writeHelp();
                    } catch (Exception e) {
                        temp.close();
                    }
                }
                if (!temp.isAlive) {
                    temp.close();
                    itf.remove();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    String readLine(SocketChannel channel, ByteBuffer buf) throws IOException {
        StringBuilder sb = new StringBuilder();
        long startTime = (new Date()).getTime();
        while (channel.read(buf) > 0) {
            long diffTime = (new Date()).getTime() - startTime;
            if (diffTime > 1000) {
                return null;
            }
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
