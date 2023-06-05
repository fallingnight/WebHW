package UserRegistrationSystem;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

class MsgParse {
    static final int HEAD_SIZE_HALF = 4;
    static final int HEAD_SIZE = 8;

    public int[] getMsgHead(InputStream inputStream) throws IOException {
        int[] result = new int[2];
        byte[] bytes = new byte[HEAD_SIZE];
        bytes = readFullBytes(HEAD_SIZE, inputStream);
        byte[] lengthBytes = Arrays.copyOfRange(bytes, 0, HEAD_SIZE_HALF);
        byte[] commandBytes = Arrays.copyOfRange(bytes, HEAD_SIZE_HALF, HEAD_SIZE);
        result[0] = MsgStruct.byteConvert(lengthBytes);
        result[1] = MsgStruct.byteConvert(commandBytes);
        return result;
    }

    public byte[] getMsgBody(int length, InputStream inputStream) throws IOException {
        byte[] result = new byte[length];
        result = readFullBytes(length, inputStream);
        return result;

    }

    public byte[] readFullBytes(int length, InputStream inputStream) throws IOException {
        int rest = length;
        byte[] result = new byte[length];
        while (rest != 0) {
            rest = length - inputStream.read(result, length - rest, rest);
        }
        return result;
    }

}

public class MsgStruct {
    static final int STATUS_SIZE = 1;
    static final int DESCRIPTION_SIZE = 64;
    static final int USERNAME_SIZE = 20;
    static final int PASSWORD_SIZE = 30;
    static final int REG_REQUEST = 1;
    static final int REG_RESPONSE = 2;
    static final int LOGIN_REQUEST = 3;
    static final int LOGIN_RESPONSE = 4;
    static final int HEARTBEAT_MSG = 5;
    private byte[] msgBody;
    private byte[] msgByte;
    private int length;
    private int commandID;
    private String username = null;
    private String password = null;
    private String status = null;
    private String description = null;

    MsgStruct(InputStream inputStream) throws Exception {
        MsgParse msgparse = new MsgParse();
        int[] head = msgparse.getMsgHead(inputStream); // 先读包头获取commandID和消息长度
        length = head[0];
        commandID = head[1];
        if (commandID != HEARTBEAT_MSG) {
            msgBody = msgparse.getMsgBody(length - MsgParse.HEAD_SIZE, inputStream); // 此程序设计为发送heartbeat时只发送带commandID的包头，因此不是heartbeat时读包体
            splitMsgBody(this.msgBody); // 获取包体各字段信息
        }
    }

    public void splitMsgBody(byte[] msgBD) {
        switch (commandID) {
            case REG_REQUEST:
            case LOGIN_REQUEST:
                byte[] un = Arrays.copyOfRange(msgBD, 0, USERNAME_SIZE);
                byte[] pw = Arrays.copyOfRange(msgBD, USERNAME_SIZE, PASSWORD_SIZE + USERNAME_SIZE);
                username = (new String(un)).trim();
                password = (new String(pw)).trim();
                break;
            case REG_RESPONSE:
            case LOGIN_RESPONSE:
                byte[] st = Arrays.copyOfRange(msgBD, 0, STATUS_SIZE);
                byte[] dc = Arrays.copyOfRange(msgBD, STATUS_SIZE, STATUS_SIZE + DESCRIPTION_SIZE);
                status = (new String(st)).trim();
                description = (new String(dc)).trim();
                break;
        }
    }

    static byte[] formatMsgHead(byte[] bodyformat, int command) {
        byte[] lengthformat = intConvert(MsgParse.HEAD_SIZE + bodyformat.length);
        byte[] commandformat = intConvert(command);
        byte[] msghead = Arrays.copyOf(lengthformat, MsgParse.HEAD_SIZE);
        System.arraycopy(commandformat, 0, msghead, MsgParse.HEAD_SIZE_HALF, MsgParse.HEAD_SIZE_HALF);
        return msghead;

    }

    public static byte[] formatResponseString(String status, String description, int command) {
        byte[] stabytes = status.getBytes();
        byte[] desbytes = description.getBytes();
        byte[] staformat = Arrays.copyOfRange(stabytes, 0, STATUS_SIZE);
        byte[] desformat = Arrays.copyOfRange(desbytes, 0, DESCRIPTION_SIZE);
        byte[] resformat = Arrays.copyOf(staformat, staformat.length + desformat.length);
        System.arraycopy(desformat, 0, resformat, staformat.length, desformat.length);
        byte[] result = Arrays.copyOf(formatMsgHead(resformat, command), resformat.length + MsgParse.HEAD_SIZE);
        System.arraycopy(resformat, 0, result, MsgParse.HEAD_SIZE, resformat.length);
        return result;
    }

    public static byte[] formatUserString(String username, String password, int command) {
        byte[] userbytes = username.getBytes();
        byte[] pswdbytes = password.getBytes();
        byte[] userformat = Arrays.copyOfRange(userbytes, 0, USERNAME_SIZE);
        byte[] pswdformat = Arrays.copyOfRange(pswdbytes, 0, PASSWORD_SIZE);
        byte[] unpformat = Arrays.copyOf(userformat, userformat.length + pswdformat.length);
        System.arraycopy(pswdformat, 0, unpformat, userformat.length, pswdformat.length);
        byte[] result = Arrays.copyOf(formatMsgHead(unpformat, command), unpformat.length + MsgParse.HEAD_SIZE);
        System.arraycopy(unpformat, 0, result, MsgParse.HEAD_SIZE, unpformat.length);
        return result;
    }

    public static byte[] intConvert(int value) {
        byte[] result = new byte[4];
        result[0] = (byte) ((value >> 24) & 0xFF);
        result[1] = (byte) ((value >> 16) & 0xFF);
        result[2] = (byte) ((value >> 8) & 0xFF);
        result[3] = (byte) ((value) & 0xFF);
        return result;
    }

    public static int byteConvert(byte[] bytes) {
        int result = 0;
        for (int i = 0; i < 4; i++) {
            result <<= 8;
            int temp = bytes[i] & 0xFF;
            result |= temp;
        }
        return result;
    }

    public int getLength() {
        return length;
    }

    public int getCommandID() {
        return commandID;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public byte[] getMsg() {
        return msgByte;
    }

    public String toString() {
        return new String(msgBody) + "\n";
    }

}