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
		super("방 검색"); // 제목
		this.tok = tok;
		setSize(300, 150);
		setLocationRelativeToParent(tok);
		add(createInfoPanel()); 
		addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                tok.searchWindow = null; // 창이 닫힐 때 항상 null 설정
                dispose(); // 창 닫기
            }

            @Override
            public void windowClosed(WindowEvent e) {
                tok.searchWindow = null; // 창이 닫힌 이후에도 null 설정 보장
            }
        });
		setVisible(true); // 창 보이기
    }
	
	private JPanel createInfoPanel() {
		
		JPanel inputPanel = new JPanel(new FlowLayout());
		JTextField searchField = new JTextField(15);
		JButton searchButton = new JButton("검색");
		
		inputPanel.add(new JLabel("검색어:"));
	    inputPanel.add(searchField);
	    inputPanel.add(searchButton);
		
	    //검색 버튼
	    searchButton.addActionListener(e -> {
	        String query = searchField.getText().trim();
	        if (query.isEmpty()) {
	            JOptionPane.showMessageDialog(this, "검색어를 입력하세요.", "경고", JOptionPane.WARNING_MESSAGE);
	            return;
	        }
	        searchRooms(query);
	    });
		return inputPanel;
	}
	
	private void searchRooms(String query) {
	    // 검색어를 포함한 방 목록 필터링
	    Vector<String> matchingRooms = new Vector<>();
	    for (String room : tok.roomList) {
	        if (room.toLowerCase().contains(query.toLowerCase())) {
	            matchingRooms.add(room);
	        }
	    }

	    if (matchingRooms.isEmpty()) {
	        JOptionPane.showMessageDialog(this, "검색어를 포함한 방이 없습니다.", "검색 결과", JOptionPane.INFORMATION_MESSAGE);
	        return;
	    }

	    // 검색 결과 다이얼로그
	    String[] displayRooms = new String[matchingRooms.size()];
	    for (int i = 0; i < matchingRooms.size(); i++) {
	        String[] parts = matchingRooms.get(i).split("\\|");
	        String roomName = parts[0].split(":")[1].trim();
	        String participantCount = parts[1].split(":")[1].trim();
	        displayRooms[i] = roomName + " (" + participantCount + "명 참가 중)";
	    }

	    String selectedRoom = (String) JOptionPane.showInputDialog(
	        this,
	        "접속할 방을 선택하세요:",
	        "검색 결과",
	        JOptionPane.QUESTION_MESSAGE,
	        null,
	        displayRooms,
	        displayRooms[0]
	    );

	    if (selectedRoom != null) {
	        // 선택한 방 접속
	        int selectedIndex = -1;
	        for (int i = 0; i < displayRooms.length; i++) {
	            if (displayRooms[i].equals(selectedRoom)) {
	                selectedIndex = i;
	                break;
	            }
	        }

	        if (selectedIndex != -1) {
	            String roomData = matchingRooms.get(selectedIndex);
	            Pattern pattern = Pattern.compile("방 이름: (.+) \\| 참가자 수: (\\d+) \\| 포트: (\\d+) \\| 비밀번호: (.*)");
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
            Point parentLocation = parent.getLocation(); // 부모 창의 위치 가져오기
            int parentWidth = parent.getWidth(); 
            int parentHeight = parent.getHeight(); 

            int x = parentLocation.x + (parentWidth - getWidth()) / 2;; 
            int y = parentLocation.y + (parentHeight - getHeight()) / 2; // 부모 창 가운데에 맞추기

            setLocation(x, y); // 위치 설정
        }
    }
    
    
}
