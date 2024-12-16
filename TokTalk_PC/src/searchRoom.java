import java.awt.*;
import java.awt.List;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.*;

public class searchRoom extends JFrame {
	private TokTalk tok;
	
	public searchRoom(TokTalk tok) {
		super("�� �˻�"); // ����
		this.tok = tok;
		setSize(300, 150);
		setLocationRelativeToParent(tok);
		add(createInfoPanel()); 
		addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                tok.searchWindow = null; // â�� ���� �� �׻� null ����
                dispose(); // â �ݱ�
            }

            @Override
            public void windowClosed(WindowEvent e) {
                tok.searchWindow = null; // â�� ���� ���Ŀ��� null ���� ����
            }
        });
		setVisible(true); // â ���̱�
    }
	
	private JPanel createInfoPanel() {
		
		JPanel inputPanel = new JPanel(new FlowLayout());
		JTextField searchField = new JTextField(15);
		JButton searchButton = new JButton("�˻�");
		
		inputPanel.add(new JLabel("�˻���:"));
	    inputPanel.add(searchField);
	    inputPanel.add(searchButton);
		
	    //�˻� ��ư
	    searchButton.addActionListener(e -> {
	        String query = searchField.getText().trim();
	        if (query.isEmpty()) {
	            JOptionPane.showMessageDialog(this, "�˻�� �Է��ϼ���.", "���", JOptionPane.WARNING_MESSAGE);
	            return;
	        }
	        searchRooms(query);
	    });
		return inputPanel;
	}
	
	private void searchRooms(String query) {
	    // �˻�� ������ �� ��� ���͸�
	    Vector<String> matchingRooms = new Vector<>();
	    for (String room : tok.roomList) {
	        if (room.toLowerCase().contains(query.toLowerCase())) {
	            matchingRooms.add(room);
	        }
	    }

	    if (matchingRooms.isEmpty()) {
	        JOptionPane.showMessageDialog(this, "�˻�� ������ ���� �����ϴ�.", "�˻� ���", JOptionPane.INFORMATION_MESSAGE);
	        return;
	    }

	    // �˻� ��� ���̾�α�
	    String[] displayRooms = new String[matchingRooms.size()];
	    for (int i = 0; i < matchingRooms.size(); i++) {
	        String[] parts = matchingRooms.get(i).split("\\|");
	        String roomName = parts[0].split(":")[1].trim();
	        String participantCount = parts[1].split(":")[1].trim();
	        displayRooms[i] = roomName + " (" + participantCount + "�� ���� ��)";
	    }

	    String selectedRoom = (String) JOptionPane.showInputDialog(
	        this,
	        "������ ���� �����ϼ���:",
	        "�˻� ���",
	        JOptionPane.QUESTION_MESSAGE,
	        null,
	        displayRooms,
	        displayRooms[0]
	    );

	    if (selectedRoom != null) {
	        // ������ �� ����
	        int selectedIndex = -1;
	        for (int i = 0; i < displayRooms.length; i++) {
	            if (displayRooms[i].equals(selectedRoom)) {
	                selectedIndex = i;
	                break;
	            }
	        }

	        if (selectedIndex != -1) {
	            String roomData = matchingRooms.get(selectedIndex);
	            Pattern pattern = Pattern.compile("�� �̸�: (.+) \\| ������ ��: (\\d+) \\| ��Ʈ: (\\d+) \\| ��й�ȣ: (.*)");
	            Matcher matcher = pattern.matcher(roomData);

	            if (matcher.find()) {
	                int port = Integer.parseInt(matcher.group(3));
	                tok.openChatRoom(port);
	                dispose();
	            }
	        }
	    }
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
