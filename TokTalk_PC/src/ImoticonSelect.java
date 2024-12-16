import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class ImoticonSelect extends JFrame {
    private String EMOTICON_0 = "images/Mokoko.jpg";
    private String EMOTICON_1 = "images/smile.jpg";
    private String EMOTICON_2 = "images/jotsonyang.jpg";
    private String EMOTICON_3 = "images/OYO.jpg";
    private String EMOTICON_4 = "images/bugijammin.jpg";
    private String EMOTICON_5 = "images/nyang.jpg";
    private String EMOTICON_6 = "images/bugiok.jpg";
    private String EMOTICON_7 = "images/bugino.jpg";
    private String EMOTICON_8 = "images/bugicry.jpg";

    private TokChat client;

    public ImoticonSelect(TokChat client) {
        super("Select Imoticon"); // 제목 설정
        this.client = client;

        add(createButtonPanel());
        setSize(300, 300);
        setLocationRelativeToParent(client);
        
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                client.emoticonWindow = null; // 창이 닫힐 때 항상 null 설정
                dispose(); // 창 닫기
            }

            @Override
            public void windowClosed(WindowEvent e) {
                client.emoticonWindow = null; // 창이 닫힌 이후에도 null 설정 보장
            }
        });
        setVisible(true); // 화면 표시
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 3));

        JButton button0 = addButton(EMOTICON_0);
        panel.add(button0); // 0번 이모티콘
        JButton button1 = addButton(EMOTICON_1);
        panel.add(button1); // 1번 이모티콘
        JButton button2 = addButton(EMOTICON_2);
        panel.add(button2); // 2번 이모티콘
        JButton button3 = addButton(EMOTICON_3);
        panel.add(button3); // 3번 이모티콘
        JButton button4 = addButton(EMOTICON_4);
        panel.add(button4); // 4번 이모티콘
        JButton button5 = addButton(EMOTICON_5);
        panel.add(button5); // 5번 이모티콘
        JButton button6 = addButton(EMOTICON_6);
        panel.add(button6); // 6번 이모티콘
        JButton button7 = addButton(EMOTICON_7);
        panel.add(button7); // 7번 이모티콘
        JButton button8 = addButton(EMOTICON_8);
        panel.add(button8); // 8번 이모티콘

        return panel;
    }

    private JButton addButton(String FilePath) {
        JButton button = new JButton(new ImageIcon(FilePath));
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setVerticalAlignment(SwingConstants.CENTER);
        button.setText(null);
        button.addActionListener(e -> {
            client.sendImoticon(FilePath); // 버튼 클릭 시 클라이언트의 sendImoticon 호출
            client.emoticonWindow = null;
            dispose(); // 창 닫기
        });
        return button;
    }
    
    private void setLocationRelativeToParent(JFrame parent) {
        if (parent != null) {
            Point parentLocation = parent.getLocation(); // 부모 창의 위치 가져오기
            int parentWidth = parent.getWidth(); 
            int parentHeight = parent.getHeight();


            int x = parentLocation.x + parentWidth; // 부모 창 오른쪽에 띄우기 (1px 간격)
            int y = 100 + parentLocation.y + (parentHeight - getHeight()) / 2; // 부모 창과 세로 중심 맞추기

            setLocation(x, y); // 위치 설정
        }
    }
}
