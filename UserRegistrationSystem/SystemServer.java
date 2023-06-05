package UserRegistrationSystem;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;

public class SystemServer {
	// 响应消息文本
	static final String REG_FAIL_DUPLICATE = new String("用户名重复，注册失败！");
	static final String REG_SUCCESS = new String("注册成功！");
	static final String LOGIN_FAIL_NOUSER = new String("用户不存在，登录失败！");
	static final String LOGIN_FAIL_WRONGPSWD = new String("密码错误，登录失败！");
	static final String LOGIN_SUCCESS = new String("登录成功！");

	// 查找数据文件中的用户名行
	static final String SEARCH_USERNAME = new String("Username:");

	// 用于提醒客户端已连上服务器
	static final String WELCOME_TEXT = new String("Welcome!");

	// 客户端服务线程
	class RLClientWorker implements Runnable {
		private Socket socket;
		private FileServer fileserver;
		private String usernm = null; // 用于打印heartbeat消息时显示客户端用户名

		// 用于heartbeat服务
		// 客户端断联有两种情况，一是服务器接收到了reset给它close，二是服务器没有收到消息，heartbeat超时后close
		// 第一种情况socket虽然已经close不过还在检测heartbeat队列里面，为了避免重复，设置一个标志
		private boolean isAlive;
		private Date lastHeartbeatTime = new Date();

		RLClientWorker(Socket socket, FileServer fileserver) {
			this.socket = socket;
			this.fileserver = fileserver;
			isAlive = true;
		}

		public synchronized Date getHB() {
			return lastHeartbeatTime;
		}

		public synchronized void setHB(Date lastHeartbeatTime) {
			this.lastHeartbeatTime = lastHeartbeatTime;
		}

		@Override
		public String toString() {
			return usernm;
		}

		public void close() {
			try {
				if (isAlive) {
					System.out.println(this + " close...");
					this.socket.close();
					isAlive = false;
				} else {
					System.out.println(this + " has been already closed.");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			try {
				OutputStream ost = this.socket.getOutputStream();
				InputStream ist = this.socket.getInputStream();
				ost.write(WELCOME_TEXT.getBytes());
				while (true) {
					// 从输入流读消息
					MsgStruct msg_S = new MsgStruct(ist);
					if (msg_S.getCommandID() == MsgStruct.HEARTBEAT_MSG) {
						this.setHB(new Date());
						System.out.println(usernm + " - " + "i am ready." + " [" + (new Date()) + "]");
					} else {
						this.setHB(new Date());
						CommandServe(msg_S, ost);
						System.out.print(msg_S);
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					this.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		// 响应不同请求
		void CommandServe(MsgStruct msg, OutputStream ost) {
			try {
				// 格式化响应消息包
				byte[] formatStr;
				switch (msg.getCommandID()) {
					// 响应注册请求
					case MsgStruct.REG_REQUEST:
						if (fileserver.FindString(msg.getUsername(), msg.getPassword(), msg.getCommandID())) {
							// 注册时用户名重复
							formatStr = MsgStruct.formatResponseString("0", REG_FAIL_DUPLICATE, MsgStruct.REG_RESPONSE);
						} else {
							// 用户名未发生重复，可注册
							formatStr = MsgStruct.formatResponseString("1", REG_SUCCESS, MsgStruct.REG_RESPONSE);
						}
						ost.write(formatStr);
						break;
					// 响应登录请求
					case MsgStruct.LOGIN_REQUEST:
						if (!fileserver.FindString(msg.getUsername(), msg.getPassword(), msg.getCommandID())) {
							// 用户不存在
							formatStr = MsgStruct.formatResponseString("0", LOGIN_FAIL_NOUSER,
									MsgStruct.LOGIN_RESPONSE);
						} else if ((fileserver.FindUserPswd(msg.getUsername()))
								.equals(fileserver.PswdHash(msg.getPassword()))) {
							// 密码正确（hash值相同）
							formatStr = MsgStruct.formatResponseString("1", LOGIN_SUCCESS, MsgStruct.LOGIN_RESPONSE);
							usernm = msg.getUsername();
						} else {
							/// 密码错误
							formatStr = MsgStruct.formatResponseString("0", LOGIN_FAIL_WRONGPSWD,
									MsgStruct.LOGIN_RESPONSE);
						}
						ost.write(formatStr);
						break;

					default:
						break;

				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}

	class HBMonitor implements Runnable {
		private Vector<RLClientWorker> workers = null;

		HBMonitor(Vector<RLClientWorker> workers) {
			this.workers = workers;
		}

		@Override
		public void run() {
			try {
				while (true) {
					try {
						Iterator<RLClientWorker> iterWorker = workers.iterator();
						while (iterWorker.hasNext()) {
							RLClientWorker worker = iterWorker.next();
							long diffTime = (new Date()).getTime() - worker.getHB().getTime();
							if (!worker.isAlive) {
								iterWorker.remove();
							} else if (diffTime >= 20000) {
								System.out.println(worker + " heartbeat timeout...");
								worker.close();
								iterWorker.remove();
							}
							Thread.sleep(2000);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	class FileServer {
		File file;
		BufferedReader inFile;
		PrintWriter outFile;

		FileServer() throws Exception {
			file = new File("passwd.txt");
		}

		public synchronized boolean FindString(String curname, String curpswd, int commandID) throws Exception {
			inFile = new BufferedReader(new FileReader(file));
			String tempS;
			boolean found = false;
			while ((tempS = inFile.readLine()) != null) {
				if (tempS.indexOf(SEARCH_USERNAME) != -1) {
					if ((tempS.substring(tempS.indexOf(SEARCH_USERNAME) + SEARCH_USERNAME.length()))
							.equals(curname)) {
						found = true;
						break;
					}
				}
			}
			inFile.close();
			if (!found && commandID == MsgStruct.REG_REQUEST) {
				WriteInfo(curname, curpswd);
			}
			return found;
		}

		void WriteInfo(String curname, String curpswd) throws Exception {
			outFile = new PrintWriter(new FileWriter(file, true));
			outFile.println(SEARCH_USERNAME + curname);
			outFile.println(PswdHash(curpswd));
			outFile.close();
		}

		public synchronized String FindUserPswd(String curname) throws Exception {
			inFile = new BufferedReader(new FileReader(file));
			String tempS;
			while ((tempS = inFile.readLine()) != null) {
				if (tempS.indexOf(SEARCH_USERNAME) != -1) {
					if ((tempS.substring(tempS.indexOf(SEARCH_USERNAME) + SEARCH_USERNAME.length()))
							.equals(curname)) {
						tempS = inFile.readLine();
						return tempS;
					}
				}
			}
			return null;
		}

		String PswdHash(String curpswd) throws Exception {
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			md5.update(curpswd.getBytes());
			byte[] result = md5.digest();
			StringBuilder hashstr = new StringBuilder();
			for (byte temp : result) {
				hashstr.append(String.format("%02x", temp));
			}
			return hashstr.toString();
		}
	}

	public static void main(String[] args) throws IOException {
		SystemServer server = new SystemServer();
		server.launch();
	}

	void launch() throws IOException {
		int portNumber = 12001;
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(portNumber);
			FileServer fileserver = new FileServer();
			Vector<RLClientWorker> workers = new Vector<RLClientWorker>();
			(new Thread(new HBMonitor(workers))).start();
			while (true) {
				Socket clientSocket = serverSocket.accept();
				RLClientWorker newWorker = new RLClientWorker(clientSocket, fileserver);
				workers.add(newWorker);
				(new Thread(newWorker)).start();
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		} finally {
			serverSocket.close();
		}
	}

}