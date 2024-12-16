import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.*;

public class TokTalk extends JFrame {
	private final static String localhost_txt = "localhost.txt"; 
    private static String Address = getServerIP(localhost_txt); //IP주소를 localhost.txt에서 받아옴
    private static int Port = 1025; // 기본값을 메인 서버의 포트 번호로 설정
    
    private Socket socket;
    private ObjectOutputStream out;
    private Thread mainReceiveThread = null;

    private String serverAddress;
    private int serverPort;
    protected Vector<String> roomList = new Vector<>(); // 방 목록

    private String uid;
    private JLabel Tok;
    private ImageIcon tok;

    private JButton b_login;
    private JTextField t_userID, t_hostAddr, t_portNum;

    public TokChat talkWindow = null;
    public MakeTalk makeWindow = null;
    public searchRoom searchWindow = null;
    
    public TokTalk(String serverAddress, int serverPort) {
        super("Tok Talk"); // 제목

        this.serverAddress = serverAddress;
        this.serverPort = serverPort;

        startGUI(); // 초기 화면 GUI 생성

        setSize(400, 500);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // 닫기 처리
        addWindowListener(new java.awt.event.WindowAdapter() {
        	@Override
            public void windowClosing(java.awt.event.WindowEvent e) {
        		disconnect();
                System.exit(0);
        	}
        });
        setVisible(true); // 창 보이기
    }
    private void startGUI() {
        add(startPanel(), BorderLayout.CENTER); // 상단 로고 영역

        JPanel p_input = new JPanel(new GridLayout(3, 0));
        p_input.setBackground(new Color(250,225,0));
        p_input.add(createInfoPanel()); // 사용자 정보 입력 영역
        p_input.add(loginPanel()); // 로그인 버튼 영역
        add(p_input, BorderLayout.SOUTH);
        
    }
    private JPanel startPanel() {
        JPanel p = new JPanel(new FlowLayout());
        p.setBackground(new Color(250,225,0));

        tok = new ImageIcon("images/kakao2.png");
        Tok = new JLabel(tok);

        p.add(Tok);

        return p;
    }
    private JPanel createInfoPanel() {
        JPanel p = new JPanel(new FlowLayout());
        p.setBackground(new Color(250,225,0));

        t_userID = new JTextField(7);
        t_hostAddr = new JTextField(12);
        t_portNum = new JTextField(5);

        // 기본 값 설정
        t_userID.setText("guest" + getLocalAddr().split("\\.")[3]);
        t_hostAddr.setText(this.serverAddress);
        t_portNum.setText(String.valueOf(this.serverPort));

        t_portNum.setHorizontalAlignment(JTextField.CENTER);

        p.add(new JLabel("아이디: "));
        p.add(t_userID);

        return p;
    }
    private JPanel loginPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.YELLOW);

        b_login = new JButton("로그인");
        b_login.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // 로그인 버튼 동작
                uid = t_userID.getText(); // 입력된 ID를 uid로 설정
                getContentPane().removeAll(); // 기존 화면 제거
                serverGUI(); // 채팅방 화면 GUI 생성
                revalidate(); // UI 업데이트
                repaint(); // 화면 새로고침
                connectToServer(uid);
            }
        });

        p.add(b_login);

        return p;
    }
    private static String getServerIP(String filePath) {
    	String ip = null;
    	try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            ip = reader.readLine(); // 첫 줄 읽기
        } catch (IOException e) {
            System.err.println("파일 읽기 오류: " + e.getMessage());
        }
        return (ip != null && !ip.isEmpty()) ? ip : "localhost"; // 읽은 값이 없으면 기본값으로 설정
    }

    private void connectToServer(String uid) {
        this.uid = uid;
        try {
            socket = new Socket(Address, Port);
            System.out.println("디버그: 서버와 연결 성공");

            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            System.out.println("디버그: 출력 스트림 초기화 완료");

            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            System.out.println("디버그: 입력 스트림 초기화 완료");

            send(new ChatMsg(uid, ChatMsg.MODE_LOGIN));

            // 메시지 수신 스레드 실행
            mainReceiveThread = new Thread(() -> {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        ChatMsg inMsg = (ChatMsg) in.readObject();
                        if (inMsg == null) {
                            System.out.println("디버그: null 메시지 수신");
                            break;
                        }

                        switch (inMsg.mode) {
                            case ChatMsg.MODE_ROOMLIST:
                                //System.out.println("디버그: 방 목록 업데이트 받음");
                                updateRoomList(inMsg.message);
                                break;
                            case ChatMsg.MODE_USERLIST:
                                //System.out.println("디버그: 유저 목록 업데이트 받음");
                                updateUserList(inMsg.message);
                                break;
                            default:
                                System.out.println("디버그: 알 수 없는 메시지 모드 수신 - " + inMsg.mode);
                        }
                    }
                } catch (IOException | ClassNotFoundException e) {
                    System.out.println("디버그: 메시지 수신 중 오류 발생 - " + e.getMessage());
                } finally {
                    disconnect(); // 연결 종료 처리
                }
            });
            mainReceiveThread.start();

        } catch (IOException e) {
            System.out.println("디버그: 서버 연결 실패 - " + e.getMessage());
            e.printStackTrace();
        }
    }
    private void disconnect() {
        if (socket != null && !socket.isClosed()) {
            try {
                send(new ChatMsg(uid, ChatMsg.MODE_LOGOUT)); // 로그아웃 메시지 전송
                socket.close();
                System.out.println("연결 해제 완료");
            } catch (IOException e) {
                System.err.println("클라이언트 닫기 오류: " + e.getMessage());
            } finally {
                mainReceiveThread = null;
                socket = null; // 소켓 객체 초기화
            }
        } else {
            System.out.println("소켓이 이미 닫혀 있거나 null 상태입니다.");
        }
    }
    private void send(ChatMsg msg) {
        try {
            if (out != null) {
                out.writeObject(msg);
                out.flush();
            } else {
                System.err.println("디버그: 출력 스트림이 null 상태입니다.");
            }
        } catch (IOException e) {
            System.err.println("디버그: 데이터 전송 중 오류 발생 - " + e.getMessage());
        }
    }

    private JSplitPane splitPane; // 디자인용 좌우 분할 패널
    private JPanel roomListPanel; // 방 목록 화면
    private JScrollPane scrollPane; // 방 목록
    private JPanel userListPanel; // 참가자 목록 화면
    private JScrollPane userScrollPane; // 참가자 목록
    
    private void serverGUI() {
        // 방 목록 패널 
        setupRoomListPanel(); 
        // 접속자 목록 패널 
        setupUserListPanel();

        // 좌우비율 설정
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPane, userScrollPane);
        splitPane.setResizeWeight(0.67);
        splitPane.setDividerSize(5);
        
        getContentPane().add(splitPane, BorderLayout.CENTER);
        getContentPane().add(makeRoomPanel(), BorderLayout.SOUTH); // 방 생성 버튼 추가
    }
    
    private JPanel makeRoomPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);

        // 방 생성 버튼
        JButton b_setPort = new JButton("방 만들기");
        b_setPort.setBackground(Color.ORANGE);
        b_setPort.addActionListener(e -> {
        	if (makeWindow != null) {
                makeWindow.dispose();
                makeWindow = null;
            } else {
                makeWindow = new MakeTalk(TokTalk.this);
            }
        	
        });
        
        //방 검색 버튼
        JButton b_searchRoom = new JButton("방 검색");
        b_searchRoom.setBackground(Color.WHITE);
        b_searchRoom.addActionListener(e -> {
        	if (searchWindow != null) {
        		searchWindow.dispose();
        		searchWindow = null;
            } else {
            	searchWindow = new searchRoom(TokTalk.this);
            }
        	
        });
        // 방 삭제 버튼
        JButton b_deleteRoom = new JButton("방 삭제");
        b_deleteRoom.setBackground(Color.RED);
        b_deleteRoom.addActionListener(e -> deleteRoomAction());

        JPanel buttonsPanel = new JPanel(new GridLayout(1,3));
        buttonsPanel.add(b_setPort);
        buttonsPanel.add(b_searchRoom);
        buttonsPanel.add(b_deleteRoom);

        p.add(buttonsPanel, BorderLayout.CENTER);
        return p;
    }
    private void deleteRoomAction() {
        if (roomList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "삭제 가능한 방이 없습니다.", "경고", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 다이얼로그를 위한 리스트 변환
        String[] displayList = new String[roomList.size()];
        for (int i = 0; i < roomList.size(); i++) {
            // 예: "방 이름: TestRoom | 참가자 수: 5명"
            String[] parts = roomList.get(i).split("\\|");
            String roomName = parts[0].split(":")[1].trim();
            String participantCount = parts[1].split(":")[1].trim(); // "5"
            displayList[i] = roomName + " (" + participantCount + "명 참가 중)";
        }

        // 방 삭제를 위한 선택 다이얼로그
        String selectedRoomDisplay = (String) JOptionPane.showInputDialog(
            this,
            "삭제할 방을 선택하세요:",
            "방 삭제",
            JOptionPane.QUESTION_MESSAGE,
            null,
            displayList,
            displayList[0]
        );

        if (selectedRoomDisplay != null) {
            // 선택한 방의 원본 데이터를 찾기
            int selectedIndex = -1;
            for (int i = 0; i < displayList.length; i++) {
                if (displayList[i].equals(selectedRoomDisplay)) {
                    selectedIndex = i;
                    break;
                }
            }

            if (selectedIndex == -1) {
                JOptionPane.showMessageDialog(this, "선택한 방을 찾을 수 없습니다.", "오류", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String selectedRoom = roomList.get(selectedIndex); // 원본 데이터
            Pattern pattern = Pattern.compile("방 이름: (.+) \\| 참가자 수: (\\d+) \\| 포트: (\\d+) \\| 비밀번호: (.*)");
            Matcher matcher = pattern.matcher(selectedRoom);

            if (matcher.find()) {
                String roomName = matcher.group(1);
                String port = matcher.group(3);
                String actualPassword = matcher.group(4);

                // 비밀번호 입력
                JPanel panel = new JPanel(new GridLayout(2, 1));
                JPasswordField passwordField = new JPasswordField(4);
                panel.add(new JLabel("비밀번호:"));
                panel.add(passwordField);

                int option = JOptionPane.showConfirmDialog(
                    this,
                    panel,
                    "비밀번호 입력",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
                );

                if (option == JOptionPane.OK_OPTION) {
                    String enteredPassword = new String(passwordField.getPassword());

                    // 비밀번호 검증
                    if (enteredPassword.equals(actualPassword)) {
                        // 방 삭제 메시지 생성
                        ChatMsg deleteRoomMsg = new ChatMsg(uid, ChatMsg.MODE_DELETE_ROOM, port);
                        send(deleteRoomMsg);

                        JOptionPane.showMessageDialog(this, "방 '" + roomName + "'이 삭제되었습니다.", "알림", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(this, "비밀번호가 일치하지 않습니다.", "경고", JOptionPane.WARNING_MESSAGE);
                    }
                }
            }
        }
    }

    
    private void setupUserListPanel() {
        userListPanel = new JPanel();
        userListPanel.setLayout(new BoxLayout(userListPanel, BoxLayout.Y_AXIS));
        userListPanel.setBackground(Color.WHITE);
        userScrollPane = new JScrollPane(userListPanel);
        userScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        userScrollPane.setPreferredSize(new Dimension(140, 300));
    }

    // 접속자 목록 업데이트
    private void updateUserList(String userData) {
        userListPanel.removeAll(); // 기존 목록 삭제

        String[] users = userData.split("\n");
        for (String user : users) {
            if (!user.trim().isEmpty()) {
                JLabel userLabel = new JLabel(user);
                userLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                userListPanel.add(userLabel);
            }
        }

        // UI 갱신
        userListPanel.revalidate();
        userListPanel.repaint();
    }
    
    private void updateRoomList(String roomData) {
    	roomList.clear();
        roomListPanel.removeAll(); // 기존 목록 삭제
 
        // 방 목록 문자열 파싱 및 버튼 추가
        String[] rooms = roomData.split("\n");
        Pattern pattern = Pattern.compile("방 이름: (.+?) \\| 참가자 수: (\\d+) \\| 포트: (\\d+) \\| 비밀번호: ?(.*)");

        for (String room : rooms) {
        	room = room.trim();
        	if (room.isEmpty()) continue;
            Matcher matcher = pattern.matcher(room);
            
            if (matcher.find()) {
                String roomName = matcher.group(1); // 방 이름
                int participantCount = Integer.parseInt(matcher.group(2)); // 참가자 수
                int port = Integer.parseInt(matcher.group(3)); // 포트 번호
                String password = matcher.group(4); // 비밀번호

                // 방 정보를 Vector에 추가
                String roomInfo = String.format("방 이름: %s | 참가자 수: %d | 포트: %d | 비밀번호: %s",
                        roomName, participantCount, port, password);
                roomList.add(roomInfo);
                
                // 방 이름 버튼
                JButton button = new JButton(roomName);
                Dimension buttonSize = new Dimension(130, 30);
                button.setPreferredSize(buttonSize);
                button.setMaximumSize(buttonSize);
                button.setMinimumSize(buttonSize);
                button.addActionListener(e -> openChatRoom(port)); // 방 입장

                // 참가자 수 레이블
                JLabel participantLabel = new JLabel(" : " + participantCount + "명 참가 중");
                participantLabel.setPreferredSize(new Dimension(100, 30));
                participantLabel.setHorizontalAlignment(SwingConstants.LEFT);

                // 방 목록 패널
                JPanel rowPanel = new JPanel();
                rowPanel.setLayout(new BoxLayout(rowPanel, BoxLayout.X_AXIS)); // 수평 정렬
                rowPanel.add(button);
                rowPanel.add(Box.createRigidArea(new Dimension(10, 0))); // 버튼과 텍스트 간격
                rowPanel.add(participantLabel);
                rowPanel.setAlignmentX(Component.LEFT_ALIGNMENT); // 왼쪽 정렬

                // 리스트 패널에 추가
                roomListPanel.add(rowPanel);
                roomListPanel.add(Box.createRigidArea(new Dimension(0, 5))); // 행 간격 추가
            }
        }
        // UI 갱신
        roomListPanel.revalidate();
        roomListPanel.repaint();
    }
    
    protected void openChatRoom(int port) {
        // 방 입장 시 서버 연결 확인 및 초기화
        if (talkWindow == null || !talkWindow.isConnected()) {
            talkWindow = new TokChat(Address, port, uid);
            talkWindow.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosed(java.awt.event.WindowEvent e) {
                    talkWindow = null; // 창 닫을 때 talkWindow를 null로 초기화
                }
            });
        }
    }

    private void setupRoomListPanel() {
        roomListPanel = new JPanel();
        roomListPanel.setLayout(new BoxLayout(roomListPanel, BoxLayout.Y_AXIS));
        scrollPane = new JScrollPane(roomListPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setPreferredSize(new Dimension(400, 300)); // 스크롤 영역의 크기 설정

        getContentPane().add(scrollPane, BorderLayout.CENTER);
    }
    
    public void makeRoom(RoomData room) {
        try {
        	if (out != null) {
                out.writeObject(room);
                out.flush();
            } else {
                System.err.println("디버그: 출력 스트림이 null 상태입니다.");
            }
        } catch (Exception e) {
            System.err.println("디버그: 방 생성 메시지 전송 중 오류 발생 - " + e.getMessage());
        }
    }
    
    private String getLocalAddr() {
        InetAddress local = null;
        String addr = "";
        try {
            local = InetAddress.getLocalHost();
            addr = local.getHostAddress();
            System.out.println(addr);
        } catch (java.net.UnknownHostException e) {
            e.printStackTrace();
        }
        return addr;
    }

    public static void main(String[] args) {
        String serverAddress = Address;
        int serverPort = 54321;

        new TokTalk(serverAddress, serverPort);
    }
}
