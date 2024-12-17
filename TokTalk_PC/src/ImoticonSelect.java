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
        super("Select Imoticon"); // ���� ����
        this.client = client;

        add(createButtonPanel());
        setSize(300, 300);
        setLocationRelativeToParent(client);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                client.emoticonWindow = null; // â�� ���� �� �׻� null ����
                dispose(); // â �ݱ�
            }

            @Override
            public void windowClosed(WindowEvent e) {
                client.emoticonWindow = null; // â�� ���� ���Ŀ��� null ���� ����
            }
        });
        setVisible(true); // ȭ�� ǥ��
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 3));

        panel.add(addButton(EMOTICON_0)); // 0�� �̸�Ƽ��
        panel.add(addButton(EMOTICON_1)); // 1�� �̸�Ƽ��
        panel.add(addButton(EMOTICON_2)); // 2�� �̸�Ƽ��
        panel.add(addButton(EMOTICON_3)); // 3�� �̸�Ƽ��
        panel.add(addButton(EMOTICON_4)); // 4�� �̸�Ƽ��
        panel.add(addButton(EMOTICON_5)); // 5�� �̸�Ƽ��
        panel.add(addButton(EMOTICON_6)); // 6�� �̸�Ƽ��
        panel.add(addButton(EMOTICON_7)); // 7�� �̸�Ƽ��
        panel.add(addButton(EMOTICON_8)); // 8�� �̸�Ƽ��

        return panel;
    }

    private JButton addButton(String resourcePath) {
        JButton button = new JButton();
        URL imageUrl = getClass().getResource(resourcePath);

        if (imageUrl != null) {
            button.setIcon(new ImageIcon(imageUrl)); // URL ��ü�� ���� ���
            System.out.println("�̹��� ����: " + imageUrl);
        } else {
            System.out.println("�̹��� ����: " + resourcePath); 
            button.setText("No Image"); // �̹����� ���� ��� �⺻ �ؽ�Ʈ ����
        }

        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setVerticalAlignment(SwingConstants.CENTER);
        button.setText(null);
        button.addActionListener(e -> {
            client.sendImoticon(resourcePath); // ��ư Ŭ�� �� Ŭ���̾�Ʈ�� sendImoticon ȣ��
            client.emoticonWindow = null;
            dispose(); // â �ݱ�
        });
        return button;
    }


    private void setLocationRelativeToParent(JFrame parent) {
        if (parent != null) {
            Point parentLocation = parent.getLocation(); // �θ� â�� ��ġ ��������
            int parentWidth = parent.getWidth();
            int parentHeight = parent.getHeight();

            int x = parentLocation.x + parentWidth; // �θ� â �����ʿ� ���� (1px ����)
            int y = 100 + parentLocation.y + (parentHeight - getHeight()) / 2; // �θ� â�� ���� �߽� ���߱�

            setLocation(x, y); // ��ġ ����
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
