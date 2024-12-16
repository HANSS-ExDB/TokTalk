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

        JButton button0 = addButton(EMOTICON_0);
        panel.add(button0); // 0�� �̸�Ƽ��
        JButton button1 = addButton(EMOTICON_1);
        panel.add(button1); // 1�� �̸�Ƽ��
        JButton button2 = addButton(EMOTICON_2);
        panel.add(button2); // 2�� �̸�Ƽ��
        JButton button3 = addButton(EMOTICON_3);
        panel.add(button3); // 3�� �̸�Ƽ��
        JButton button4 = addButton(EMOTICON_4);
        panel.add(button4); // 4�� �̸�Ƽ��
        JButton button5 = addButton(EMOTICON_5);
        panel.add(button5); // 5�� �̸�Ƽ��
        JButton button6 = addButton(EMOTICON_6);
        panel.add(button6); // 6�� �̸�Ƽ��
        JButton button7 = addButton(EMOTICON_7);
        panel.add(button7); // 7�� �̸�Ƽ��
        JButton button8 = addButton(EMOTICON_8);
        panel.add(button8); // 8�� �̸�Ƽ��

        return panel;
    }

    private JButton addButton(String FilePath) {
        JButton button = new JButton(new ImageIcon(FilePath));
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setVerticalAlignment(SwingConstants.CENTER);
        button.setText(null);
        button.addActionListener(e -> {
            client.sendImoticon(FilePath); // ��ư Ŭ�� �� Ŭ���̾�Ʈ�� sendImoticon ȣ��
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
}
