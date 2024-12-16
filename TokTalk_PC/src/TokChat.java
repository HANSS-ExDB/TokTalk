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
        return socket != null && !socket.isClosed(); // ������ null�� �ƴϰ� �������� ������ ���� ���·� ����
    }

    public TokChat(String serverAddress, int serverPort, String uid) {
        super("�� :  " + uid); // ä�ù� ����

        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.uid = uid;

        buildGUI(); // ȭ�� ����

        setSize(400, 600); // â ũ��
        setLocation(350, 50); // â ��ġ
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // â �ݱ� �� ���� ó��
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                disconnect(); // â ���� �� �ڵ����� disconnect ȣ��
            }
        });

        setVisible(true); // â ���̱�

        try {
            connectToServer();
            sendUserID();
        } catch (UnknownHostException e1) {
            printDisplay("���� �ּҿ� ��Ʈ ��ȣ�� Ȯ���ϼ���: " + e1.getMessage());
            return;
        } catch (IOException e1) {
            printDisplay("�������� ���� ����: " + e1.getMessage());
            return;
        }
    }

    private void buildGUI() {
        add(createDisplayPanel(), BorderLayout.CENTER); // �߾� ����

        JPanel p_input = new JPanel(new GridLayout(1, 0));
        p_input.add(createInputPanel());
        add(p_input, BorderLayout.SOUTH);
    }

    private JButton createSquareButton(String resourcePath) {
        URL resourceUrl = getClass().getResource(resourcePath);
        if (resourceUrl == null) {
            throw new IllegalArgumentException("���ҽ��� ã�� �� �����ϴ�: " + resourcePath);
        }
        JButton button = new JButton(new ImageIcon(resourceUrl));
        int size = 40; // ��ư ũ�� (���簢��)
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

        t_display.setFont(new Font("Malgun Gothic", Font.PLAIN, 14)); // ���� ���, ����, 14pt
        t_display.setEditable(false); // �ؽ�Ʈ â ���� �Ұ�
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

        // �ؽ�Ʈ �Է� �ʵ� �г�
        JPanel textPanel = new JPanel(new BorderLayout());
        textPanel.add(t_input, BorderLayout.CENTER);

        // ��ư �г�
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
                    emoticonWindow = new ImoticonSelect(TokChat.this); // �̸�Ƽ�� ����â ����
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
                        "JPG & GIF & PNG Images", // ���� �̸� ǥ��
                        "jpg", "gif", "png");     // Ȯ���� ���� ����

                chooser.setFileFilter(filter);

                int ret = chooser.showOpenDialog(TokChat.this);
                if (ret != JFileChooser.APPROVE_OPTION) {
                    JOptionPane.showMessageDialog(TokChat.this, "������ �������� �ʾҽ��ϴ�.", "���", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                t_input.setText(chooser.getSelectedFile().getAbsolutePath());
                sendImage();
            }
        });

        mainButtonPanel.add(b_select, BorderLayout.WEST); // ���� ��ư ���ʿ� ��ġ

        buttonPanel.add(b_emoticon);
        buttonPanel.add(b_send);
        mainButtonPanel.add(buttonPanel, BorderLayout.EAST); // �ٸ� ��ư �����ʿ� ��ġ

        p.add(mainButtonPanel, BorderLayout.NORTH);
        p.add(textPanel, BorderLayout.SOUTH);
        p.setBackground(new Color(250,225,0));
        
        t_input.setEnabled(true);
        b_emoticon.setEnabled(true);
        b_select.setEnabled(true);
        b_send.setEnabled(true); // ��� ��ư Ȱ��ȭ

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

            // �̹��� ����
            t_display.setCaretPosition(t_display.getDocument().getLength());
            if (icon.getIconWidth() > 400) {
                Image img = icon.getImage();
                Image changeImg = img.getScaledInstance(400, -1, Image.SCALE_SMOOTH);
                icon = new ImageIcon(changeImg);
            }
            t_display.insertIcon(icon);

            // �� �� ���� �� ����
            len = t_display.getDocument().getLength();
            document.insertString(len, "\n", null);
            document.setParagraphAttributes(len, 1, attrSet, false);

        } catch (BadLocationException e) {
            e.printStackTrace();
        }

        t_display.setCaretPosition(t_display.getDocument().getLength()); // ��ũ���� �ֽ� ��ġ�� �̵�
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
                            printDisplay("���� ������ ������ϴ�.");
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
                        printDisplay("������ ����Ǿ����ϴ�.");
                    } catch (ClassNotFoundException e) {
                        printDisplay("�� �� ���� ��ü�� ���۵Ǿ����ϴ�.");
                    }
                }

                @Override
                public void run() {
                    try {
                        in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
                    } catch (IOException e) {
                        printDisplay("�Է� ��Ʈ���� ������ ����");
                    }
                    while (receiveThread == Thread.currentThread()) {
                        receiveMessage();
                    }
                }
            });
            receiveThread.start();
        } catch (IOException e) {
            if (socket != null) socket.close(); // ���� ��ü ����
            throw e; // ���� ��߻�
        }
    }

    private void disconnect() {
        if (socket != null && !socket.isClosed()) {
            try {
                send(new ChatMsg(uid, ChatMsg.MODE_LOGOUT)); // �α׾ƿ� �޽��� ����
                socket.close();
            } catch (IOException e) {
                System.err.println("Ŭ���̾�Ʈ �ݱ� ����: " + e.getMessage());
            } finally {
                receiveThread = null;
                socket = null; // ���� ��ü �ʱ�ȭ
                System.out.println("���� ���� �Ϸ�");
            }
        } else {
            System.out.println("������ �̹� ���� �ְų� null �����Դϴ�.");
        }
    }

    private void send(ChatMsg msg) {
        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            System.err.println("Ŭ���̾�Ʈ ������ ���� ����: " + e.getMessage());
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
            printDisplay(">> ������ �������� �ʽ��ϴ�: " + filename + "\n");
            return;
        }

        ImageIcon icon = new ImageIcon(filename);
        send(new ChatMsg(uid, ChatMsg.MODE_TX_IMAGE, file.getName(), icon));

        t_input.setText("");
    }

    public void sendImoticon(String resourcePath) {
        // JAR ���� �� ����� ȯ�濡�� �̹��� �ε�
        URL resourceUrl = getClass().getResource(resourcePath);
        if (resourceUrl == null) {
            throw new IllegalArgumentException("���ҽ��� ã�� �� �����ϴ�: " + resourcePath);
        }

        // ImageIcon ����
        ImageIcon icon = new ImageIcon(resourceUrl);

        // ChatMsg ��ü ���� �� ����
        send(new ChatMsg(uid, ChatMsg.MODE_TX_IMAGE, "", icon));
    }


    public static void main(String[] args) {
        String serverAddress = "localhost";
        int serverPort = 54321;
        String userID = "default";

        new TokChat(serverAddress, serverPort, userID);
    }
}
