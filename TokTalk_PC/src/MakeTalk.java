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
		super("방 만들기"); // 제목
		this.tok = tok;
		setSize(250, 150);
		setLocationRelativeToParent(tok);
		add(createInfoPanel()); 
		addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                tok.makeWindow = null; // 창이 닫힐 때 항상 null 설정
                dispose(); // 창 닫기
            }

            @Override
            public void windowClosed(WindowEvent e) {
                tok.makeWindow = null; // 창이 닫힌 이후에도 null 설정 보장
            }
        });
		setVisible(true); // 창 보이기
    }
	
	public JPanel createInfoPanel() {
		JPanel p = new JPanel(new GridLayout(3, 0));
		
		t_name = new JTextField(8);
		f_password = new JPasswordField(4);
		b_make = new JButton("만들기");
		
		b_make.addActionListener(e -> {
			 String roomName = t_name.getText().trim(); // 방 이름의 공백 제거
			 if (roomName.isEmpty()) {
	                JOptionPane.showMessageDialog(this, "방 이름은 필수 항목입니다.", "경고", JOptionPane.WARNING_MESSAGE);
	                return;
	            }
			roomdata = new RoomData(t_name.getText(),new String(f_password.getPassword()));
            tok.makeRoom(roomdata); // 버튼 클릭 시 클라이언트의 makeRoom 호출
            tok.makeWindow = null;
            dispose(); // 창 닫기
        });
		
		JPanel p_1 = new JPanel();
		JPanel p_2 = new JPanel();
		JPanel p_3 = new JPanel();
		
		p_1.add(new JLabel("방 제목 : "));
		p_1.add(t_name);
		p_2.add(new JLabel("삭제 비밀번호 : "));
		p_2.add(f_password);
		p_3.add(b_make);
		p.add(p_1);p.add(p_2);p.add(p_3);
		 
		return p;
		
	}
	
    private void setLocationRelativeToParent(JFrame parent) {
        if (parent != null) {
            Point parentLocation = parent.getLocation(); // 부모 창의 위치 가져오기
            int parentWidth = parent.getWidth(); 
            int parentHeight = parent.getHeight(); 

            int x = parentLocation.x + (parentWidth - getWidth()) / 2;; 
            int y = parentLocation.y + (parentHeight - getHeight()) / 2; // 부모 창 가운데에 맞추기

            setLocation(x, y); // 위치 설정
        }
    }
}
