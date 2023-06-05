package UserRegistrationSystem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

public class Client {
	static String currentCommand;
	static String currentUsername = null;

	static class HeartbeatWorker implements Runnable {
		Socket clientSocket;
		static final String HEARTBEAT_STR = new String("i am ok.");

		HeartbeatWorker(Socket clientSocket) {
			this.clientSocket = clientSocket;
		}

		@Override
		public void run() {
			try {
				OutputStream ost = clientSocket.getOutputStream();
				while (true) {
					Thread.sleep(5000);
					ost.write(MsgStruct.formatMsgHead(HEARTBEAT_STR.getBytes(), MsgStruct.HEARTBEAT_MSG));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) throws IOException {
		String hostName = "localhost";
		int portNumber = 12001;
		Socket clientSocket = null;
		String tempName;
		try {
			clientSocket = new Socket(hostName, portNumber);
			BufferedReader stdIn = new BufferedReader(new InputStreamReader(
					System.in));
			Thread hbw = (new Thread(new HeartbeatWorker(clientSocket)));
			hbw.setDaemon(true);
			hbw.start();
			byte[] connectInfo = new byte[1024];
			InputStream ist = clientSocket.getInputStream();
			ist.read(connectInfo);
			System.out.println(new String(connectInfo));
			System.out.println("连接成功！");
			for (;;) {
				printUserName();
				System.out.println("请输入操作：（1.登录，2.注册，3.退出）");
				currentCommand = stdIn.readLine();
				tempName = ClientCommand(stdIn, clientSocket);
				MsgStruct msg_S = new MsgStruct(ist);
				if (msg_S.getCommandID() == MsgStruct.LOGIN_RESPONSE && msg_S.getStatus().equals("1")) {
					currentUsername = tempName;
					if (currentUsername != null) {
						System.out.println("欢迎！用户：" + currentUsername);
					}
				}
				System.out.print(msg_S);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static String ClientCommand(BufferedReader stdIn, Socket socket) throws Exception {
		String password;
		String username = null;
		byte[] formatStr;
		switch (currentCommand) {
			case "1":
				System.out.print("用户名:");
				username = stdIn.readLine();
				System.out.print("密码:");
				password = stdIn.readLine();
				formatStr = MsgStruct.formatUserString(username, password, MsgStruct.LOGIN_REQUEST);
				socket.getOutputStream().write(formatStr);
				break;
			case "2":
				System.out.print("用户名:");
				username = stdIn.readLine();
				System.out.print("密码:");
				password = stdIn.readLine();
				formatStr = MsgStruct.formatUserString(username, password, MsgStruct.REG_REQUEST);
				socket.getOutputStream().write(formatStr);
				break;
			case "3":
				socket.close();
				break;
			default:
				System.out.println("输入有误，请重新输入！");
				printUserName();
				System.out.println("请输入操作：（1.登录，2.注册，3.退出）");
				currentCommand = stdIn.readLine();
				ClientCommand(stdIn, socket);
				break;

		}
		return username;
	}

	static void printUserName() {
		if (currentUsername != null) {
			System.out.print("用户：" + currentUsername + " ");
		}
	}

}