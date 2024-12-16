import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.InetAddress;
import java.util.Vector;
import java.util.Random;
import javax.swing.*;

public class TokTalkServer extends JFrame {
	private final static String localhost_txt = "localhost.txt";
    private int mainServerPort = 1025; // ���� ������ ��Ʈ ��ȣ
    private int[] ports; // ����� ��Ʈ ��ȣ �迭
    private Vector<ChatRoom> chatRooms = new Vector<>(); // ä�ù� ���
    private JTextArea t_display; // ���� �α� ���â
    private JButton b_exit; // ���� ���� ��ư
    private boolean[] port_isOpen = new boolean[65536]; // ��Ʈ ���� ���� ǥ�� �迭
    
    private MainRoom mainRoom ;  // ���� ���� ��
    private RoomData mainRoomData = new RoomData("MAIN ROBY",null); //���� ���� �� ������

    public TokTalkServer() {
        super("TokTalk Server");

        buildGUI();

        setSize(400, 300);
        setLocation(750, 200);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);

        // ���� ���� ����
        startMainServer();
    }

    private void buildGUI() {
        add(createDisplayPanel(), BorderLayout.CENTER); // �α� ���â
        add(createControlPanel(), BorderLayout.SOUTH); // ���� ��ư
    }

    private JPanel createDisplayPanel() {
        JPanel p = new JPanel(new BorderLayout());

        t_display = new JTextArea();
        t_display.setEditable(false); // �α� ���â ���� �Ұ�

        p.add(new JScrollPane(t_display), BorderLayout.CENTER);

        return p;
    }

    private JPanel createControlPanel() {
        JPanel p = new JPanel(new GridLayout(1, 0));

        b_exit = new JButton("����");
        b_exit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopChatRooms(mainServerPort);
                System.exit(0); // ���α׷� ����
            }
        });

        p.add(b_exit);

        return p;
    }

    private void startMainServer() {
    	mainRoom = new MainRoom(mainServerPort, mainRoomData);
        port_isOpen[mainServerPort] = true;
        new Thread(mainRoom).start(); // ���� ���� ����
    }
     
    private void startChatRooms(RoomData roomdata) {
    	Random random = new Random();
    	int rand_port;
    	do {
            rand_port = 1025 + random.nextInt(49151 - 1025 + 1);
        } while (port_isOpen[rand_port]); 
    	
        ChatRoom chatRoom = new ChatRoom(rand_port, roomdata);
        chatRooms.add(chatRoom);
        port_isOpen[rand_port] = true;
        new Thread(chatRoom).start(); // ���ο� ä�ù� ����
        mainRoom.broadcastRoomList(); 
    }

    private void stopChatRooms(int port) {
        // ���� ������ ��Ʈ��� ��� ä�ù� ����
        if (port == mainServerPort) {
            for (ChatRoom chatRoom : chatRooms) {
                chatRoom.stopRoom();
            }
            chatRooms.clear(); // ��� ä�ù� ����
            port_isOpen[mainServerPort] = false;
            printDisplay("��� ä�ù��� ����Ǿ����ϴ�.");
        } else {
            // ��� ��Ʈ�� ����ϴ� �� ã��
            ChatRoom roomToStop = null;
            for (ChatRoom chatRoom : chatRooms) {
                if (chatRoom.port == port) {
                    roomToStop = chatRoom;
                    break;
                }
            }
            // ��� ��Ʈ�� ����ϴ� �� ����
            if (roomToStop != null) {
                roomToStop.stopRoom();
                chatRooms.remove(roomToStop);
                port_isOpen[port] = false; // �ش� ��Ʈ�� ����
                printDisplay("��Ʈ " + port + "�� ä�ù��� ����Ǿ����ϴ�.");
                mainRoom.broadcastRoomList(); // �� ��� ���� ��ε�ĳ��Ʈ
            } else {
                printDisplay("��Ʈ " + port + "�� ä�ù��� ã�� �� �����ϴ�.");
            }
        }
    }
    private void printDisplay(String msg) {
        t_display.append(msg + "\n");
        t_display.setCaretPosition(t_display.getDocument().getLength());
    }

    private String getServerIP(String filePath) {
    	String ip = null;
    	try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            ip = reader.readLine(); // ù �� �б�
        } catch (IOException e) {
            System.err.println("���� �б� ����: " + e.getMessage());
        }
        return (ip != null && !ip.isEmpty()) ? ip : "localhost"; // ���� ���� ������ �⺻������ ����
    }
    
    private class ChatRoom implements Runnable {
        private int port;
        private RoomData roomData;
        private ServerSocket serverSocket = null;
        protected Vector<ClientHandler> users = new Vector<>(); // ���� ä�ù� ����� ���
        private Thread acceptThread = null;

        public ChatRoom(int port, RoomData roomData) {
        	this.port = port;
        	this.roomData= roomData;
        }
        public RoomData getRoomData() {
            return roomData;
        }

        @Override
        public void run() {
            try {
            	String serverIP = getServerIP(localhost_txt);
            	
            	serverSocket = new ServerSocket(port, 0, InetAddress.getByName(serverIP));
            	
                String serverAddress = serverSocket.getInetAddress().getHostAddress();

                if (port == mainServerPort)
                	printDisplay("������ ���Ƚ��ϴ�. �ּ�: " + serverAddress + ", ��Ʈ: " + port);
                else
                	printDisplay("ä�ù��� ���Ƚ��ϴ�. �ּ�: " + serverAddress + ", ��Ʈ: " + port);

                acceptThread = Thread.currentThread();

                while (acceptThread == Thread.currentThread()) {
                    Socket clientSocket = serverSocket.accept();
                    String cAddr = clientSocket.getInetAddress().getHostAddress();
                    printDisplay("��Ʈ " + port + " - Ŭ���̾�Ʈ ����: " + cAddr);

                    ClientHandler clientHandler = new ClientHandler(clientSocket);
                    users.add(clientHandler); // �� ����� �߰�
                    clientHandler.start();
                }
            } catch (SocketException e) {
                printDisplay("��Ʈ " + port + " - ä�ù� ����");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (serverSocket != null) serverSocket.close();
                } catch (IOException e) {
                    System.err.println("��Ʈ " + port + " - ���� ���� �ݱ� ����: " + e.getMessage());
                }
            }
        }

        public void stopRoom() {
            try {
                acceptThread = null;
                if (serverSocket != null) serverSocket.close();
            } catch (IOException e) {
                System.err.println("��Ʈ " + port + " - ���� ���� �ݱ� ����: " + e.getMessage());
            }
        }
        
        protected class ClientHandler extends Thread {
            private Socket clientSocket;
            private ObjectOutputStream out;
            private ObjectInputStream in;
            private String uid;

            public ClientHandler(Socket clientSocket) {
                this.clientSocket = clientSocket;
                try {
                    this.out = new ObjectOutputStream(new BufferedOutputStream(clientSocket.getOutputStream()));
                    this.out.flush();
                    System.out.println("�����: Ŭ���̾�Ʈ ��� ��Ʈ�� �ʱ�ȭ �Ϸ�");

                    this.in = new ObjectInputStream(new BufferedInputStream(clientSocket.getInputStream()));
                    System.out.println("�����: Ŭ���̾�Ʈ �Է� ��Ʈ�� �ʱ�ȭ �Ϸ�");
                } catch (IOException e) {
                    System.err.println("�����: ��Ʈ�� �ʱ�ȭ ���� - " + e.getMessage());
                }
            }

            private void receiveMessages() {
                try {
                    System.out.println("�����: Ŭ���̾�Ʈ �޽��� ���� ��� ��");

                    Object obj;
                    while ((obj = in.readObject()) != null) {
                        System.out.println("�����: �޽��� ���� �Ϸ� - Ŭ����: " + obj.getClass().getSimpleName());

                        if (obj instanceof ChatMsg) {
                            ChatMsg msg = (ChatMsg) obj;
                            handleChatMsg(msg); // ChatMsg ó�� �������� �и�
                        } else if (obj instanceof RoomData) {
                            RoomData room = (RoomData) obj;
                            handleRoomData(room); // RoomData ó�� �������� �и�
                        } else {
                            System.err.println("�����: �� �� ���� ��ü ���� - " + obj.getClass().getName());
                        }
                    }
                } catch (IOException | ClassNotFoundException e) {
                    printDisplay("��Ʈ " + port + " - " + uid + " ���� ����. ���� ����� ��: " + users.size());
                    System.err.println("�����: �޽��� ���� �� ���� �߻� - " + e.getMessage());
                } finally {
                    try {
                        clientSocket.close();
                        System.out.println("�����: Ŭ���̾�Ʈ ���� �ݱ� �Ϸ�");
                    } catch (IOException e) {
                        System.err.println("�����: Ŭ���̾�Ʈ ���� �ݱ� ���� - " + e.getMessage());
                    }
                    users.removeElement(this);
                    mainRoom.broadcastRoomList();
                }
            }
            
            private void handleChatMsg(ChatMsg msg) {
                switch (msg.mode) {
                    case ChatMsg.MODE_LOGIN:
                        uid = msg.userID;
                        printDisplay("��Ʈ " + port + " - �� �����: " + uid);
                        printDisplay("��Ʈ " + port + " - ���� ����� ��: " + users.size());
                        ChatMsg roomListMsg = new ChatMsg("SERVER", ChatMsg.MODE_ROOMLIST, mainRoom.currentBuffer);
                        // �α��� �� �� ���� �� �� ��� ����
                        mainRoom.broadcastUserList();
                        send(roomListMsg); 
                        
                        break;

                    case ChatMsg.MODE_LOGOUT:
                    	users.remove(this);
                    	mainRoom.broadcastUserList();
                        return; // ���� ����
                        
                    case ChatMsg.MODE_TX_STRING:
                        String message = uid + ": " + msg.message;
                        printDisplay("��Ʈ " + port + " - " + message);
                        broadcasting(msg);
                        break;

                    case ChatMsg.MODE_TX_IMAGE:
                        printDisplay("��Ʈ " + port + " - " + uid + ": [�̹���]");
                        broadcasting(msg);
                        break;
                        
                    case ChatMsg.MODE_DELETE_ROOM:
                    	int port = Integer.parseInt(msg.message); 
                    	stopChatRooms(port);
                    	mainRoom.broadcastRoomList();
                    	break;

                    default:
                        System.err.println("�����: �� �� ���� �޽��� ��� ���� - " + msg.mode);
                        break;
                }
            }
            private void handleRoomData(RoomData roomdata) {
                System.out.println("�����: RoomData ���� - �� �̸�: " + roomdata.getName());
                startChatRooms(roomdata);
            }

            private void send(ChatMsg msg) {
                try {
                    if (out != null) {
                        out.writeObject(msg);
                        out.flush();
                    } else {
                        System.err.println("��� ��Ʈ���� null�Դϴ�.");
                    }
                } catch (IOException e) {
                    System.err.println("��Ʈ " + port + " - Ŭ���̾�Ʈ ������ ���� ����: " + e.getMessage());
                }
            }

            private void broadcasting(ChatMsg msg) {
                for (ClientHandler c : users) {
                    c.send(msg); // ��� ����ڿ��� �޽��� ����
                }
            }

            @Override
            public void run() {
                receiveMessages();
            }
        }

    }

    public class MainRoom extends ChatRoom {
        private String currentBuffer = ""; // ���� ���۵� �� ���
        private String pendingBuffer = ""; // ���� ��� ���� �� ���

        public MainRoom(int port, RoomData mainRoomData) {
            super(port, mainRoomData);
            startBufferedUpdate();
        }

        private String getRoomList() {
            StringBuilder roomList = new StringBuilder("���� �� ���:\n");
            for (ChatRoom room : chatRooms) {
                RoomData roomData = room.getRoomData();
                int participantCount = room.users.size(); // ������ ��
                roomList.append("�� �̸�: ").append(roomData.getName())
                        .append(" | ������ ��: ").append(participantCount)
                        .append(" | ��Ʈ: ").append(room.port)
                        .append(" | ��й�ȣ: ").append(roomData.getPW())
                        .append("\n");
            }
            return roomList.toString();
        }
        // �� ����� ���ϰ� ���� �� Ŭ���̾�Ʈ�� ����
        
        private String getUserList() {
        	StringBuilder userList = new StringBuilder("\n");
        	for (ClientHandler user : this.users) {
                userList.append(user.uid).append("\n");
            }
        	return userList.toString();
        }
        private void startBufferedUpdate() {
            Thread updateThread = new Thread(() -> {
                while (true) {
                    try {
                        pendingBuffer = getRoomList(); // �� ��� ����
                        if (!pendingBuffer.equals(currentBuffer)) { // ����� ��쿡�� ����
                            currentBuffer = pendingBuffer;
                            broadcastRoomList(); // Ŭ���̾�Ʈ�� ����
                        }
                        Thread.sleep(100); // 0.1�� ���
                    } catch (InterruptedException e) {
                        System.out.println("�� ��� ���� ������ �ߴ�");
                        break;
                    }
                }
            });
            updateThread.setDaemon(true); // ���α׷� ���� �� �����嵵 ����
            updateThread.start();
        }

        public void broadcastRoomList() {
            ChatMsg roomListMsg = new ChatMsg("SERVER", ChatMsg.MODE_ROOMLIST, currentBuffer);
            for (ClientHandler user : this.users) { // ���� �� �����鿡�� ����
                user.send(roomListMsg);
                printDisplay("�� ��� ���� ��� - " + user.uid);
            }
        }
        public void broadcastUserList() {
        	String userList = getUserList();
            ChatMsg userListMsg = new ChatMsg("SERVER", ChatMsg.MODE_USERLIST, userList);
            for (ClientHandler user : this.users) {
                user.send(userListMsg);
            }
        }
    }

    
    public static void main(String[] args) {
        new TokTalkServer(); // ���� ���� ����
    }
}
