import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.*;

public class MakeTalk extends JFrame{
	private TokTalk tok;
	private JTextField t_name;
	private JPasswordField f_password; 
	private JButton b_make;
	
	private RoomData roomdata;
	
	public MakeTalk(TokTalk tok) {
		super("�� �����"); // ����
		this.tok = tok;
		setSize(250, 150);
		setLocationRelativeToParent(tok);
		add(createInfoPanel()); 
		addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                tok.makeWindow = null; // â�� ���� �� �׻� null ����
                dispose(); // â �ݱ�
            }

            @Override
            public void windowClosed(WindowEvent e) {
                tok.makeWindow = null; // â�� ���� ���Ŀ��� null ���� ����
            }
        });
		setVisible(true); // â ���̱�
    }
	
	public JPanel createInfoPanel() {
		JPanel p = new JPanel(new GridLayout(3, 0));
		
		t_name = new JTextField(8);
		f_password = new JPasswordField(4);
		b_make = new JButton("�����");
		
		b_make.addActionListener(e -> {
			 String roomName = t_name.getText().trim(); // �� �̸��� ���� ����
			 if (roomName.isEmpty()) {
	                JOptionPane.showMessageDialog(this, "�� �̸��� �ʼ� �׸��Դϴ�.", "���", JOptionPane.WARNING_MESSAGE);
	                return;
	            }
			roomdata = new RoomData(t_name.getText(),new String(f_password.getPassword()));
            tok.makeRoom(roomdata); // ��ư Ŭ�� �� Ŭ���̾�Ʈ�� makeRoom ȣ��
            tok.makeWindow = null;
            dispose(); // â �ݱ�
        });
		
		JPanel p_1 = new JPanel();
		JPanel p_2 = new JPanel();
		JPanel p_3 = new JPanel();
		
		p_1.add(new JLabel("�� ���� : "));
		p_1.add(t_name);
		p_2.add(new JLabel("���� ��й�ȣ : "));
		p_2.add(f_password);
		p_3.add(b_make);
		p.add(p_1);p.add(p_2);p.add(p_3);
		 
		return p;
		
	}
	
    private void setLocationRelativeToParent(JFrame parent) {
        if (parent != null) {
            Point parentLocation = parent.getLocation(); // �θ� â�� ��ġ ��������
            int parentWidth = parent.getWidth(); 
            int parentHeight = parent.getHeight(); 

            int x = parentLocation.x + (parentWidth - getWidth()) / 2;; 
            int y = parentLocation.y + (parentHeight - getHeight()) / 2; // �θ� â ����� ���߱�

            setLocation(x, y); // ��ġ ����
        }
    }
}
