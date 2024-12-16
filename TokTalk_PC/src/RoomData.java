import java.io.Serializable;

public class RoomData implements Serializable  {
    
	
	private String ROOM_NAME;
	private String ROOM_PASSWORD;
	
	public RoomData(String ROOM_NAME, String ROOM_PASSWORD) {
		this.ROOM_NAME = ROOM_NAME;
		this.ROOM_PASSWORD = ROOM_PASSWORD;
	}
	
	public String getName() {
		return ROOM_NAME;
	}
	public String getPW() {
		return ROOM_PASSWORD;
	}

	@Override
    public String toString() {
        return "Room: " + ROOM_NAME + " (Password: " + ROOM_PASSWORD + ")";
    }
	
}
