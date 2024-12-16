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
    private int mainServerPort = 1025; // 메인 서버의 포트 번호
    private int[] ports; // 사용할 포트 번호 배열
    private Vector<ChatRoom> chatRooms = new Vector<>(); // 채팅방 목록
    private JTextArea t_display; // 서버 로그 출력창
    private JButton b_exit; // 서버 종료 버튼
    private boolean[] port_isOpen = new boolean[65536]; // 포트 열림 상태 표시 배열
    
    private MainRoom mainRoom ;  // 서버 메인 룸
    private RoomData mainRoomData = new RoomData("MAIN ROBY",null); //서버 메인 룸 데이터

    public TokTalkServer() {
        super("TokTalk Server");

        buildGUI();

        setSize(400, 300);
        setLocation(750, 200);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);

        // 메인 서버 시작
        startMainServer();
    }

    private void buildGUI() {
        add(createDisplayPanel(), BorderLayout.CENTER); // 로그 출력창
        add(createControlPanel(), BorderLayout.SOUTH); // 종료 버튼
    }

    private JPanel createDisplayPanel() {
        JPanel p = new JPanel(new BorderLayout());

        t_display = new JTextArea();
        t_display.setEditable(false); // 로그 출력창 수정 불가

        p.add(new JScrollPane(t_display), BorderLayout.CENTER);

        return p;
    }

    private JPanel createControlPanel() {
        JPanel p = new JPanel(new GridLayout(1, 0));

        b_exit = new JButton("종료");
        b_exit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopChatRooms(mainServerPort);
                System.exit(0); // 프로그램 종료
            }
        });

        p.add(b_exit);

        return p;
    }

    private void startMainServer() {
    	mainRoom = new MainRoom(mainServerPort, mainRoomData);
        port_isOpen[mainServerPort] = true;
        new Thread(mainRoom).start(); // 메인 서버 실행
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
        new Thread(chatRoom).start(); // 새로운 채팅방 실행
        mainRoom.broadcastRoomList(); 
    }

    private void stopChatRooms(int port) {
        // 메인 서버의 포트라면 모든 채팅방 종료
        if (port == mainServerPort) {
            for (ChatRoom chatRoom : chatRooms) {
                chatRoom.stopRoom();
            }
            chatRooms.clear(); // 모든 채팅방 제거
            port_isOpen[mainServerPort] = false;
            printDisplay("모든 채팅방이 종료되었습니다.");
        } else {
            // 대상 포트를 사용하는 방 찾기
            ChatRoom roomToStop = null;
            for (ChatRoom chatRoom : chatRooms) {
                if (chatRoom.port == port) {
                    roomToStop = chatRoom;
                    break;
                }
            }
            // 대상 포트를 사용하는 방 제거
            if (roomToStop != null) {
                roomToStop.stopRoom();
                chatRooms.remove(roomToStop);
                port_isOpen[port] = false; // 해당 포트를 닫음
                printDisplay("포트 " + port + "의 채팅방이 종료되었습니다.");
                mainRoom.broadcastRoomList(); // 방 목록 갱신 브로드캐스트
            } else {
                printDisplay("포트 " + port + "의 채팅방을 찾을 수 없습니다.");
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
            ip = reader.readLine(); // 첫 줄 읽기
        } catch (IOException e) {
            System.err.println("파일 읽기 오류: " + e.getMessage());
        }
        return (ip != null && !ip.isEmpty()) ? ip : "localhost"; // 읽은 값이 없으면 기본값으로 설정
    }
    
    private class ChatRoom implements Runnable {
        private int port;
        private RoomData roomData;
        private ServerSocket serverSocket = null;
        protected Vector<ClientHandler> users = new Vector<>(); // 현재 채팅방 사용자 목록
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
                	printDisplay("서버가 열렸습니다. 주소: " + serverAddress + ", 포트: " + port);
                else
                	printDisplay("채팅방이 열렸습니다. 주소: " + serverAddress + ", 포트: " + port);

                acceptThread = Thread.currentThread();

                while (acceptThread == Thread.currentThread()) {
                    Socket clientSocket = serverSocket.accept();
                    String cAddr = clientSocket.getInetAddress().getHostAddress();
                    printDisplay("포트 " + port + " - 클라이언트 연결: " + cAddr);

                    ClientHandler clientHandler = new ClientHandler(clientSocket);
                    users.add(clientHandler); // 새 사용자 추가
                    clientHandler.start();
                }
            } catch (SocketException e) {
                printDisplay("포트 " + port + " - 채팅방 종료");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (serverSocket != null) serverSocket.close();
                } catch (IOException e) {
                    System.err.println("포트 " + port + " - 서버 소켓 닫기 오류: " + e.getMessage());
                }
            }
        }

        public void stopRoom() {
            try {
                acceptThread = null;
                if (serverSocket != null) serverSocket.close();
            } catch (IOException e) {
                System.err.println("포트 " + port + " - 서버 소켓 닫기 오류: " + e.getMessage());
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
                    System.out.println("디버그: 클라이언트 출력 스트림 초기화 완료");

                    this.in = new ObjectInputStream(new BufferedInputStream(clientSocket.getInputStream()));
                    System.out.println("디버그: 클라이언트 입력 스트림 초기화 완료");
                } catch (IOException e) {
                    System.err.println("디버그: 스트림 초기화 오류 - " + e.getMessage());
                }
            }

            private void receiveMessages() {
                try {
                    System.out.println("디버그: 클라이언트 메시지 수신 대기 중");

                    Object obj;
                    while ((obj = in.readObject()) != null) {
                        System.out.println("디버그: 메시지 수신 완료 - 클래스: " + obj.getClass().getSimpleName());

                        if (obj instanceof ChatMsg) {
                            ChatMsg msg = (ChatMsg) obj;
                            handleChatMsg(msg); // ChatMsg 처리 로직으로 분리
                        } else if (obj instanceof RoomData) {
                            RoomData room = (RoomData) obj;
                            handleRoomData(room); // RoomData 처리 로직으로 분리
                        } else {
                            System.err.println("디버그: 알 수 없는 객체 유형 - " + obj.getClass().getName());
                        }
                    }
                } catch (IOException | ClassNotFoundException e) {
                    printDisplay("포트 " + port + " - " + uid + " 연결 종료. 현재 사용자 수: " + users.size());
                    System.err.println("디버그: 메시지 수신 중 오류 발생 - " + e.getMessage());
                } finally {
                    try {
                        clientSocket.close();
                        System.out.println("디버그: 클라이언트 소켓 닫기 완료");
                    } catch (IOException e) {
                        System.err.println("디버그: 클라이언트 소켓 닫기 오류 - " + e.getMessage());
                    }
                    users.removeElement(this);
                    mainRoom.broadcastRoomList();
                }
            }
            
            private void handleChatMsg(ChatMsg msg) {
                switch (msg.mode) {
                    case ChatMsg.MODE_LOGIN:
                        uid = msg.userID;
                        printDisplay("포트 " + port + " - 새 사용자: " + uid);
                        printDisplay("포트 " + port + " - 현재 사용자 수: " + users.size());
                        ChatMsg roomListMsg = new ChatMsg("SERVER", ChatMsg.MODE_ROOMLIST, mainRoom.currentBuffer);
                        // 로그인 할 때 유저 및 방 목록 갱신
                        mainRoom.broadcastUserList();
                        send(roomListMsg); 
                        
                        break;

                    case ChatMsg.MODE_LOGOUT:
                    	users.remove(this);
                    	mainRoom.broadcastUserList();
                        return; // 연결 종료
                        
                    case ChatMsg.MODE_TX_STRING:
                        String message = uid + ": " + msg.message;
                        printDisplay("포트 " + port + " - " + message);
                        broadcasting(msg);
                        break;

                    case ChatMsg.MODE_TX_IMAGE:
                        printDisplay("포트 " + port + " - " + uid + ": [이미지]");
                        broadcasting(msg);
                        break;
                        
                    case ChatMsg.MODE_DELETE_ROOM:
                    	int port = Integer.parseInt(msg.message); 
                    	stopChatRooms(port);
                    	mainRoom.broadcastRoomList();
                    	break;

                    default:
                        System.err.println("디버그: 알 수 없는 메시지 모드 수신 - " + msg.mode);
                        break;
                }
            }
            private void handleRoomData(RoomData roomdata) {
                System.out.println("디버그: RoomData 수신 - 방 이름: " + roomdata.getName());
                startChatRooms(roomdata);
            }

            private void send(ChatMsg msg) {
                try {
                    if (out != null) {
                        out.writeObject(msg);
                        out.flush();
                    } else {
                        System.err.println("출력 스트림이 null입니다.");
                    }
                } catch (IOException e) {
                    System.err.println("포트 " + port + " - 클라이언트 데이터 전송 오류: " + e.getMessage());
                }
            }

            private void broadcasting(ChatMsg msg) {
                for (ClientHandler c : users) {
                    c.send(msg); // 모든 사용자에게 메시지 전송
                }
            }

            @Override
            public void run() {
                receiveMessages();
            }
        }

    }

    public class MainRoom extends ChatRoom {
        private String currentBuffer = ""; // 현재 전송된 방 목록
        private String pendingBuffer = ""; // 변경 대기 중인 방 목록

        public MainRoom(int port, RoomData mainRoomData) {
            super(port, mainRoomData);
            startBufferedUpdate();
        }

        private String getRoomList() {
            StringBuilder roomList = new StringBuilder("현재 방 목록:\n");
            for (ChatRoom room : chatRooms) {
                RoomData roomData = room.getRoomData();
                int participantCount = room.users.size(); // 참가자 수
                roomList.append("방 이름: ").append(roomData.getName())
                        .append(" | 참가자 수: ").append(participantCount)
                        .append(" | 포트: ").append(room.port)
                        .append(" | 비밀번호: ").append(roomData.getPW())
                        .append("\n");
            }
            return roomList.toString();
        }
        // 방 목록을 비교하고 변경 시 클라이언트에 전송
        
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
                        pendingBuffer = getRoomList(); // 방 목록 갱신
                        if (!pendingBuffer.equals(currentBuffer)) { // 변경된 경우에만 전송
                            currentBuffer = pendingBuffer;
                            broadcastRoomList(); // 클라이언트에 전송
                        }
                        Thread.sleep(100); // 0.1초 대기
                    } catch (InterruptedException e) {
                        System.out.println("방 목록 갱신 스레드 중단");
                        break;
                    }
                }
            });
            updateThread.setDaemon(true); // 프로그램 종료 시 스레드도 종료
            updateThread.start();
        }

        public void broadcastRoomList() {
            ChatMsg roomListMsg = new ChatMsg("SERVER", ChatMsg.MODE_ROOMLIST, currentBuffer);
            for (ClientHandler user : this.users) { // 메인 룸 유저들에게 전송
                user.send(roomListMsg);
                printDisplay("방 목록 전송 대상 - " + user.uid);
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
        new TokTalkServer(); // 메인 서버 시작
    }
}
