import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

public class TokChat extends JFrame {
	
	private String EMO_SEND = "/images/emoticon.jpg";
	private String FILE_SEND = "/images/filesend.jpg";
	private String MSG_SEND = "/images/send.jpg";


    private String serverAddress;
    private int serverPort;

    private String uid;
    
    private DefaultStyledDocument document;

    private JTextPane t_display;
    private JTextField t_input;
    private JButton b_emoticon, b_send, b_select;

    public ImoticonSelect emoticonWindow = null;

    private Socket socket;
    private ObjectOutputStream out;
    private Thread receiveThread = null;
    
    public boolean isConnected() {
        return socket != null && !socket.isClosed(); // 소켓이 null이 아니고 닫혀있지 않으면 연결 상태로 간주
    }

    public TokChat(String serverAddress, int serverPort, String uid) {
        super("나 :  " + uid); // 채팅방 제목

        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.uid = uid;

        buildGUI(); // 화면 구성

        setSize(400, 600); // 창 크기
        setLocation(350, 50); // 창 위치
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // 창 닫기 시 종료 처리
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                disconnect(); // 창 닫힐 때 자동으로 disconnect 호출
            }
        });

        setVisible(true); // 창 보이기

        try {
            connectToServer();
            sendUserID();
        } catch (UnknownHostException e1) {
            printDisplay("서버 주소와 포트 번호를 확인하세요: " + e1.getMessage());
            return;
        } catch (IOException e1) {
            printDisplay("서버와의 연결 오류: " + e1.getMessage());
            return;
        }
    }

    private void buildGUI() {
        add(createDisplayPanel(), BorderLayout.CENTER); // 중앙 영역

        JPanel p_input = new JPanel(new GridLayout(1, 0));
        p_input.add(createInputPanel());
        add(p_input, BorderLayout.SOUTH);
    }

    private JButton createSquareButton(String resourcePath) {
        URL resourceUrl = getClass().getResource(resourcePath);
        if (resourceUrl == null) {
            throw new IllegalArgumentException("리소스를 찾을 수 없습니다: " + resourcePath);
        }
        JButton button = new JButton(new ImageIcon(resourceUrl));
        int size = 40; // 버튼 크기 (정사각형)
        Dimension dimension = new Dimension(size, size);
        button.setPreferredSize(dimension);
        button.setMinimumSize(dimension);
        button.setMaximumSize(dimension);
        return button;
    }


    private JPanel createDisplayPanel() {
        JPanel p = new JPanel(new BorderLayout());

        document = new DefaultStyledDocument();
        t_display = new JTextPane(document);

        t_display.setFont(new Font("Malgun Gothic", Font.PLAIN, 14)); // 맑은 고딕, 보통, 14pt
        t_display.setEditable(false); // 텍스트 창 수정 불가
        t_display.setBackground(new Color(186,206,224));

        p.add(new JScrollPane(t_display), BorderLayout.CENTER);

        return p;
    }

    private JPanel createInputPanel() {
        JPanel p = new JPanel(new BorderLayout());

        t_input = new JTextField(40);
        t_input.setFont(new Font("Malgun Gothic", Font.PLAIN, 14));
        
        t_input.setPreferredSize(new Dimension(t_input.getWidth(), 40));
        t_input.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        // 텍스트 입력 필드 패널
        JPanel textPanel = new JPanel(new BorderLayout());
        textPanel.add(t_input, BorderLayout.CENTER);

        // 버튼 패널
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
        JPanel mainButtonPanel = new JPanel(new BorderLayout());

        b_emoticon = createSquareButton(EMO_SEND);
        b_emoticon.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (emoticonWindow != null) {
                    emoticonWindow.dispose();
                    emoticonWindow = null;
                } else {
                    emoticonWindow = new ImoticonSelect(TokChat.this); // 이모티콘 선택창 열기
                }
            }
        });

        b_send = createSquareButton(MSG_SEND);
        b_send.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        b_select = createSquareButton(FILE_SEND);
        b_select.addActionListener(new ActionListener() {

            JFileChooser chooser = new JFileChooser();

            @Override
            public void actionPerformed(ActionEvent e) {
                FileNameExtensionFilter filter = new FileNameExtensionFilter(
                        "JPG & GIF & PNG Images", // 파일 이름 표시
                        "jpg", "gif", "png");     // 확장자 필터 설정

                chooser.setFileFilter(filter);

                int ret = chooser.showOpenDialog(TokChat.this);
                if (ret != JFileChooser.APPROVE_OPTION) {
                    JOptionPane.showMessageDialog(TokChat.this, "파일을 선택하지 않았습니다.", "경고", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                t_input.setText(chooser.getSelectedFile().getAbsolutePath());
                sendImage();
            }
        });

        mainButtonPanel.add(b_select, BorderLayout.WEST); // 파일 버튼 왼쪽에 배치

        buttonPanel.add(b_emoticon);
        buttonPanel.add(b_send);
        mainButtonPanel.add(buttonPanel, BorderLayout.EAST); // 다른 버튼 오른쪽에 배치

        p.add(mainButtonPanel, BorderLayout.NORTH);
        p.add(textPanel, BorderLayout.SOUTH);
        p.setBackground(new Color(250,225,0));
        
        t_input.setEnabled(true);
        b_emoticon.setEnabled(true);
        b_select.setEnabled(true);
        b_send.setEnabled(true); // 모든 버튼 활성화

        return p;
    }
    
    private void printDisplay(String msg) {
        int len = t_display.getDocument().getLength();
        SimpleAttributeSet attrSet = new SimpleAttributeSet();
        StyleConstants.setAlignment(attrSet, StyleConstants.ALIGN_CENTER);
        try {
            document.insertString(len, msg + "\n", null);
            document.setParagraphAttributes(len, msg.length(), attrSet, false);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        t_display.setCaretPosition(len);
    }
    private void printDisplay(String msg, String userID) {
        int len = t_display.getDocument().getLength();

        boolean isOwnMessage = userID.equals(uid);
        SimpleAttributeSet attrSet = new SimpleAttributeSet();
        StyleConstants.setAlignment(attrSet, isOwnMessage ? StyleConstants.ALIGN_RIGHT : StyleConstants.ALIGN_LEFT);

        try {
            document.insertString(len, msg + "\n", null);
            document.setParagraphAttributes(len, msg.length(), attrSet, false);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        t_display.setCaretPosition(len);
    }
    private void printDisplay(ImageIcon icon, String userID) {
        int len = t_display.getDocument().getLength();

        boolean isOwnMessage = userID.equals(uid);
        SimpleAttributeSet attrSet = new SimpleAttributeSet();
        StyleConstants.setAlignment(attrSet, isOwnMessage ? StyleConstants.ALIGN_RIGHT : StyleConstants.ALIGN_LEFT);
        
        try {
            document.insertString(len, "[" + userID + "] :\n", null);
            document.setParagraphAttributes(len, userID.length() + 1, attrSet, false);

            // 이미지 삽입
            t_display.setCaretPosition(t_display.getDocument().getLength());
            if (icon.getIconWidth() > 400) {
                Image img = icon.getImage();
                Image changeImg = img.getScaledInstance(400, -1, Image.SCALE_SMOOTH);
                icon = new ImageIcon(changeImg);
            }
            t_display.insertIcon(icon);

            // 빈 줄 삽입 및 정렬
            len = t_display.getDocument().getLength();
            document.insertString(len, "\n", null);
            document.setParagraphAttributes(len, 1, attrSet, false);

        } catch (BadLocationException e) {
            e.printStackTrace();
        }

        t_display.setCaretPosition(t_display.getDocument().getLength()); // 스크롤을 최신 위치로 이동
    }


    private void connectToServer() throws UnknownHostException, IOException {
        try {
            socket = new Socket();
            SocketAddress sa = new InetSocketAddress(serverAddress, serverPort);
            socket.connect(sa, 3000);

            out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));

            receiveThread = new Thread(new Runnable() {
                private ObjectInputStream in;

                private void receiveMessage() {
                    try {
                        ChatMsg inMsg = (ChatMsg) in.readObject();
                        if (inMsg == null) {
                            disconnect();
                            printDisplay("서버 연결이 끊겼습니다.");
                            return;
                        }

                        switch (inMsg.mode) {
                            case ChatMsg.MODE_TX_STRING:
                                printDisplay("[" + inMsg.userID + "] : " + inMsg.message , inMsg.userID);
                                break;

                            case ChatMsg.MODE_TX_IMAGE:
                                printDisplay(inMsg.image,inMsg.userID);
                                break;
                        }
                    } catch (IOException e) {
                        printDisplay("연결이 종료되었습니다.");
                    } catch (ClassNotFoundException e) {
                        printDisplay("알 수 없는 객체가 전송되었습니다.");
                    }
                }

                @Override
                public void run() {
                    try {
                        in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
                    } catch (IOException e) {
                        printDisplay("입력 스트림이 열리지 않음");
                    }
                    while (receiveThread == Thread.currentThread()) {
                        receiveMessage();
                    }
                }
            });
            receiveThread.start();
        } catch (IOException e) {
            if (socket != null) socket.close(); // 소켓 객체 정리
            throw e; // 예외 재발생
        }
    }

    private void disconnect() {
        if (socket != null && !socket.isClosed()) {
            try {
                send(new ChatMsg(uid, ChatMsg.MODE_LOGOUT)); // 로그아웃 메시지 전송
                socket.close();
            } catch (IOException e) {
                System.err.println("클라이언트 닫기 오류: " + e.getMessage());
            } finally {
                receiveThread = null;
                socket = null; // 소켓 객체 초기화
                System.out.println("연결 해제 완료");
            }
        } else {
            System.out.println("소켓이 이미 닫혀 있거나 null 상태입니다.");
        }
    }

    private void send(ChatMsg msg) {
        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            System.err.println("클라이언트 데이터 전송 오류: " + e.getMessage());
        }
    }

    private void sendMessage() {
        String message = t_input.getText();
        if (message.isEmpty()) return;

        send(new ChatMsg(uid, ChatMsg.MODE_TX_STRING, message + "\n"));

        t_input.setText("");
    }

    private void sendUserID() {
        send(new ChatMsg(uid, ChatMsg.MODE_LOGIN));
    }

    public void sendImage() {
        String filename = t_input.getText().strip();
        if (filename.isEmpty()) return;

        File file = new File(filename);
        if (!file.exists()) {
            printDisplay(">> 파일이 존재하지 않습니다: " + filename + "\n");
            return;
        }

        ImageIcon icon = new ImageIcon(filename);
        send(new ChatMsg(uid, ChatMsg.MODE_TX_IMAGE, file.getName(), icon));

        t_input.setText("");
    }

    public void sendImoticon(String resourcePath) {
        // JAR 파일 및 디버그 환경에서 이미지 로드
        URL resourceUrl = getClass().getResource(resourcePath);
        if (resourceUrl == null) {
            throw new IllegalArgumentException("리소스를 찾을 수 없습니다: " + resourcePath);
        }

        // ImageIcon 생성
        ImageIcon icon = new ImageIcon(resourceUrl);

        // ChatMsg 객체 생성 및 전송
        send(new ChatMsg(uid, ChatMsg.MODE_TX_IMAGE, "", icon));
    }


    public static void main(String[] args) {
        String serverAddress = "localhost";
        int serverPort = 54321;
        String userID = "default";

        new TokChat(serverAddress, serverPort, userID);
    }
}
