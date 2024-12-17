import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;

public class ImoticonSelect extends JFrame {
    private final String EMOTICON_0 = "/images/Mokoko.jpg";
    private final String EMOTICON_1 = "/images/smile.jpg";
    private final String EMOTICON_2 = "/images/jotsonyang.jpg";
    private final String EMOTICON_3 = "/images/OYO.jpg";
    private final String EMOTICON_4 = "/images/bugijammin.jpg";
    private final String EMOTICON_5 = "/images/nyang.jpg";
    private final String EMOTICON_6 = "/images/bugiok.jpg";
    private final String EMOTICON_7 = "/images/bugino.jpg";
    private final String EMOTICON_8 = "/images/bugicry.jpg";

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

        panel.add(addButton(EMOTICON_0)); // 0번 이모티콘
        panel.add(addButton(EMOTICON_1)); // 1번 이모티콘
        panel.add(addButton(EMOTICON_2)); // 2번 이모티콘
        panel.add(addButton(EMOTICON_3)); // 3번 이모티콘
        panel.add(addButton(EMOTICON_4)); // 4번 이모티콘
        panel.add(addButton(EMOTICON_5)); // 5번 이모티콘
        panel.add(addButton(EMOTICON_6)); // 6번 이모티콘
        panel.add(addButton(EMOTICON_7)); // 7번 이모티콘
        panel.add(addButton(EMOTICON_8)); // 8번 이모티콘

        return panel;
    }

    private JButton addButton(String resourcePath) {
        JButton button = new JButton();
        URL imageUrl = getClass().getResource(resourcePath);

        if (imageUrl != null) {
            button.setIcon(new ImageIcon(imageUrl)); // URL 객체를 직접 사용
            System.out.println("이미지 생성: " + imageUrl);
        } else {
            System.out.println("이미지 없음: " + resourcePath); 
            button.setText("No Image"); // 이미지가 없을 경우 기본 텍스트 설정
        }

        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setVerticalAlignment(SwingConstants.CENTER);
        button.setText(null);
        button.addActionListener(e -> {
            client.sendImoticon(resourcePath); // 버튼 클릭 시 클라이언트의 sendImoticon 호출
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
    
    private String getImagePath(String resourcePath) {
        URL url = getClass().getResource(resourcePath);
        if (url != null) {
            return url.toString(); 
        }
        System.err.println("Resource not found: " + resourcePath);
        return null;
    }

}
