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
    private static String Address = getServerIP(localhost_txt); //IP�ּҸ� localhost.txt���� �޾ƿ�
    private static int Port = 1025; // �⺻���� ���� ������ ��Ʈ ��ȣ�� ����
    
    private Socket socket;
    private ObjectOutputStream out;
    private Thread mainReceiveThread = null;

    private String serverAddress;
    private int serverPort;
    protected Vector<String> roomList = new Vector<>(); // �� ���

    private String uid;
    private JLabel Tok;
    private ImageIcon tok;

    private JButton b_login;
    private JTextField t_userID, t_hostAddr, t_portNum;

    public TokChat talkWindow = null;
    public MakeTalk makeWindow = null;
    public searchRoom searchWindow = null;
    
    public TokTalk(String serverAddress, int serverPort) {
        super("Tok Talk"); // ����

        this.serverAddress = serverAddress;
        this.serverPort = serverPort;

        startGUI(); // �ʱ� ȭ�� GUI ����

        setSize(400, 500);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // �ݱ� ó��
        addWindowListener(new java.awt.event.WindowAdapter() {
        	@Override
            public void windowClosing(java.awt.event.WindowEvent e) {
        		disconnect();
                System.exit(0);
        	}
        });
        setVisible(true); // â ���̱�
    }
    private void startGUI() {
        add(startPanel(), BorderLayout.CENTER); // ��� �ΰ� ����

        JPanel p_input = new JPanel(new GridLayout(3, 0));
        p_input.setBackground(new Color(250,225,0));
        p_input.add(createInfoPanel()); // ����� ���� �Է� ����
        p_input.add(loginPanel()); // �α��� ��ư ����
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

        // �⺻ �� ����
        t_userID.setText("guest" + getLocalAddr().split("\\.")[3]);
        t_hostAddr.setText(this.serverAddress);
        t_portNum.setText(String.valueOf(this.serverPort));

        t_portNum.setHorizontalAlignment(JTextField.CENTER);

        p.add(new JLabel("���̵�: "));
        p.add(t_userID);

        return p;
    }
    private JPanel loginPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.YELLOW);

        b_login = new JButton("�α���");
        b_login.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // �α��� ��ư ����
                uid = t_userID.getText(); // �Էµ� ID�� uid�� ����
                getContentPane().removeAll(); // ���� ȭ�� ����
                serverGUI(); // ä�ù� ȭ�� GUI ����
                revalidate(); // UI ������Ʈ
                repaint(); // ȭ�� ���ΰ�ħ
                connectToServer(uid);
            }
        });

        p.add(b_login);

        return p;
    }
    private static String getServerIP(String filePath) {
    	String ip = null;
    	try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            ip = reader.readLine(); // ù �� �б�
        } catch (IOException e) {
            System.err.println("���� �б� ����: " + e.getMessage());
        }
        return (ip != null && !ip.isEmpty()) ? ip : "localhost"; // ���� ���� ������ �⺻������ ����
    }

    private void connectToServer(String uid) {
        this.uid = uid;
        try {
            socket = new Socket(Address, Port);
            System.out.println("�����: ������ ���� ����");

            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            System.out.println("�����: ��� ��Ʈ�� �ʱ�ȭ �Ϸ�");

            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            System.out.println("�����: �Է� ��Ʈ�� �ʱ�ȭ �Ϸ�");

            send(new ChatMsg(uid, ChatMsg.MODE_LOGIN));

            // �޽��� ���� ������ ����
            mainReceiveThread = new Thread(() -> {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        ChatMsg inMsg = (ChatMsg) in.readObject();
                        if (inMsg == null) {
                            System.out.println("�����: null �޽��� ����");
                            break;
                        }

                        switch (inMsg.mode) {
                            case ChatMsg.MODE_ROOMLIST:
                                //System.out.println("�����: �� ��� ������Ʈ ����");
                                updateRoomList(inMsg.message);
                                break;
                            case ChatMsg.MODE_USERLIST:
                                //System.out.println("�����: ���� ��� ������Ʈ ����");
                                updateUserList(inMsg.message);
                                break;
                            default:
                                System.out.println("�����: �� �� ���� �޽��� ��� ���� - " + inMsg.mode);
                        }
                    }
                } catch (IOException | ClassNotFoundException e) {
                    System.out.println("�����: �޽��� ���� �� ���� �߻� - " + e.getMessage());
                } finally {
                    disconnect(); // ���� ���� ó��
                }
            });
            mainReceiveThread.start();

        } catch (IOException e) {
            System.out.println("�����: ���� ���� ���� - " + e.getMessage());
            e.printStackTrace();
        }
    }
    private void disconnect() {
        if (socket != null && !socket.isClosed()) {
            try {
                send(new ChatMsg(uid, ChatMsg.MODE_LOGOUT)); // �α׾ƿ� �޽��� ����
                socket.close();
                System.out.println("���� ���� �Ϸ�");
            } catch (IOException e) {
                System.err.println("Ŭ���̾�Ʈ �ݱ� ����: " + e.getMessage());
            } finally {
                mainReceiveThread = null;
                socket = null; // ���� ��ü �ʱ�ȭ
            }
        } else {
            System.out.println("������ �̹� ���� �ְų� null �����Դϴ�.");
        }
    }
    private void send(ChatMsg msg) {
        try {
            if (out != null) {
                out.writeObject(msg);
                out.flush();
            } else {
                System.err.println("�����: ��� ��Ʈ���� null �����Դϴ�.");
            }
        } catch (IOException e) {
            System.err.println("�����: ������ ���� �� ���� �߻� - " + e.getMessage());
        }
    }

    private JSplitPane splitPane; // �����ο� �¿� ���� �г�
    private JPanel roomListPanel; // �� ��� ȭ��
    private JScrollPane scrollPane; // �� ���
    private JPanel userListPanel; // ������ ��� ȭ��
    private JScrollPane userScrollPane; // ������ ���
    
    private void serverGUI() {
        // �� ��� �г� 
        setupRoomListPanel(); 
        // ������ ��� �г� 
        setupUserListPanel();

        // �¿���� ����
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPane, userScrollPane);
        splitPane.setResizeWeight(0.67);
        splitPane.setDividerSize(5);
        
        getContentPane().add(splitPane, BorderLayout.CENTER);
        getContentPane().add(makeRoomPanel(), BorderLayout.SOUTH); // �� ���� ��ư �߰�
    }
    
    private JPanel makeRoomPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);

        // �� ���� ��ư
        JButton b_setPort = new JButton("�� �����");
        b_setPort.setBackground(Color.ORANGE);
        b_setPort.addActionListener(e -> {
        	if (makeWindow != null) {
                makeWindow.dispose();
                makeWindow = null;
            } else {
                makeWindow = new MakeTalk(TokTalk.this);
            }
        	
        });
        
        //�� �˻� ��ư
        JButton b_searchRoom = new JButton("�� �˻�");
        b_searchRoom.setBackground(Color.WHITE);
        b_searchRoom.addActionListener(e -> {
        	if (searchWindow != null) {
        		searchWindow.dispose();
        		searchWindow = null;
            } else {
            	searchWindow = new searchRoom(TokTalk.this);
            }
        	
        });
        // �� ���� ��ư
        JButton b_deleteRoom = new JButton("�� ����");
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
            JOptionPane.showMessageDialog(this, "���� ������ ���� �����ϴ�.", "���", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // ���̾�α׸� ���� ����Ʈ ��ȯ
        String[] displayList = new String[roomList.size()];
        for (int i = 0; i < roomList.size(); i++) {
            // ��: "�� �̸�: TestRoom | ������ ��: 5��"
            String[] parts = roomList.get(i).split("\\|");
            String roomName = parts[0].split(":")[1].trim();
            String participantCount = parts[1].split(":")[1].trim(); // "5"
            displayList[i] = roomName + " (" + participantCount + "�� ���� ��)";
        }

        // �� ������ ���� ���� ���̾�α�
        String selectedRoomDisplay = (String) JOptionPane.showInputDialog(
            this,
            "������ ���� �����ϼ���:",
            "�� ����",
            JOptionPane.QUESTION_MESSAGE,
            null,
            displayList,
            displayList[0]
        );

        if (selectedRoomDisplay != null) {
            // ������ ���� ���� �����͸� ã��
            int selectedIndex = -1;
            for (int i = 0; i < displayList.length; i++) {
                if (displayList[i].equals(selectedRoomDisplay)) {
                    selectedIndex = i;
                    break;
                }
            }

            if (selectedIndex == -1) {
                JOptionPane.showMessageDialog(this, "������ ���� ã�� �� �����ϴ�.", "����", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String selectedRoom = roomList.get(selectedIndex); // ���� ������
            Pattern pattern = Pattern.compile("�� �̸�: (.+) \\| ������ ��: (\\d+) \\| ��Ʈ: (\\d+) \\| ��й�ȣ: (.*)");
            Matcher matcher = pattern.matcher(selectedRoom);

            if (matcher.find()) {
                String roomName = matcher.group(1);
                String port = matcher.group(3);
                String actualPassword = matcher.group(4);

                // ��й�ȣ �Է�
                JPanel panel = new JPanel(new GridLayout(2, 1));
                JPasswordField passwordField = new JPasswordField(4);
                panel.add(new JLabel("��й�ȣ:"));
                panel.add(passwordField);

                int option = JOptionPane.showConfirmDialog(
                    this,
                    panel,
                    "��й�ȣ �Է�",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
                );

                if (option == JOptionPane.OK_OPTION) {
                    String enteredPassword = new String(passwordField.getPassword());

                    // ��й�ȣ ����
                    if (enteredPassword.equals(actualPassword)) {
                        // �� ���� �޽��� ����
                        ChatMsg deleteRoomMsg = new ChatMsg(uid, ChatMsg.MODE_DELETE_ROOM, port);
                        send(deleteRoomMsg);

                        JOptionPane.showMessageDialog(this, "�� '" + roomName + "'�� �����Ǿ����ϴ�.", "�˸�", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(this, "��й�ȣ�� ��ġ���� �ʽ��ϴ�.", "���", JOptionPane.WARNING_MESSAGE);
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

    // ������ ��� ������Ʈ
    private void updateUserList(String userData) {
        userListPanel.removeAll(); // ���� ��� ����

        String[] users = userData.split("\n");
        for (String user : users) {
            if (!user.trim().isEmpty()) {
                JLabel userLabel = new JLabel(user);
                userLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                userListPanel.add(userLabel);
            }
        }

        // UI ����
        userListPanel.revalidate();
        userListPanel.repaint();
    }
    
    private void updateRoomList(String roomData) {
    	roomList.clear();
        roomListPanel.removeAll(); // ���� ��� ����
 
        // �� ��� ���ڿ� �Ľ� �� ��ư �߰�
        String[] rooms = roomData.split("\n");
        Pattern pattern = Pattern.compile("�� �̸�: (.+?) \\| ������ ��: (\\d+) \\| ��Ʈ: (\\d+) \\| ��й�ȣ: ?(.*)");

        for (String room : rooms) {
        	room = room.trim();
        	if (room.isEmpty()) continue;
            Matcher matcher = pattern.matcher(room);
            
            if (matcher.find()) {
                String roomName = matcher.group(1); // �� �̸�
                int participantCount = Integer.parseInt(matcher.group(2)); // ������ ��
                int port = Integer.parseInt(matcher.group(3)); // ��Ʈ ��ȣ
                String password = matcher.group(4); // ��й�ȣ

                // �� ������ Vector�� �߰�
                String roomInfo = String.format("�� �̸�: %s | ������ ��: %d | ��Ʈ: %d | ��й�ȣ: %s",
                        roomName, participantCount, port, password);
                roomList.add(roomInfo);
                
                // �� �̸� ��ư
                JButton button = new JButton(roomName);
                Dimension buttonSize = new Dimension(130, 30);
                button.setPreferredSize(buttonSize);
                button.setMaximumSize(buttonSize);
                button.setMinimumSize(buttonSize);
                button.addActionListener(e -> openChatRoom(port)); // �� ����

                // ������ �� ���̺�
                JLabel participantLabel = new JLabel(" : " + participantCount + "�� ���� ��");
                participantLabel.setPreferredSize(new Dimension(100, 30));
                participantLabel.setHorizontalAlignment(SwingConstants.LEFT);

                // �� ��� �г�
                JPanel rowPanel = new JPanel();
                rowPanel.setLayout(new BoxLayout(rowPanel, BoxLayout.X_AXIS)); // ���� ����
                rowPanel.add(button);
                rowPanel.add(Box.createRigidArea(new Dimension(10, 0))); // ��ư�� �ؽ�Ʈ ����
                rowPanel.add(participantLabel);
                rowPanel.setAlignmentX(Component.LEFT_ALIGNMENT); // ���� ����

                // ����Ʈ �гο� �߰�
                roomListPanel.add(rowPanel);
                roomListPanel.add(Box.createRigidArea(new Dimension(0, 5))); // �� ���� �߰�
            }
        }
        // UI ����
        roomListPanel.revalidate();
        roomListPanel.repaint();
    }
    
    protected void openChatRoom(int port) {
        // �� ���� �� ���� ���� Ȯ�� �� �ʱ�ȭ
        if (talkWindow == null || !talkWindow.isConnected()) {
            talkWindow = new TokChat(Address, port, uid);
            talkWindow.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosed(java.awt.event.WindowEvent e) {
                    talkWindow = null; // â ���� �� talkWindow�� null�� �ʱ�ȭ
                }
            });
        }
    }

    private void setupRoomListPanel() {
        roomListPanel = new JPanel();
        roomListPanel.setLayout(new BoxLayout(roomListPanel, BoxLayout.Y_AXIS));
        scrollPane = new JScrollPane(roomListPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setPreferredSize(new Dimension(400, 300)); // ��ũ�� ������ ũ�� ����

        getContentPane().add(scrollPane, BorderLayout.CENTER);
    }
    
    public void makeRoom(RoomData room) {
        try {
        	if (out != null) {
                out.writeObject(room);
                out.flush();
            } else {
                System.err.println("�����: ��� ��Ʈ���� null �����Դϴ�.");
            }
        } catch (Exception e) {
            System.err.println("�����: �� ���� �޽��� ���� �� ���� �߻� - " + e.getMessage());
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
