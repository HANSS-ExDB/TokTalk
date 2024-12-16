import java.io.Serializable;
import javax.swing.ImageIcon;

public class ChatMsg implements Serializable {
    public final static int MODE_LOGIN = 0x1;
    public final static int MODE_LOGOUT = 0x2;
    public final static int MODE_ROOMLIST = 0x8;
    public final static int MODE_USERLIST = 0x80;
    
    public final static int MODE_TX_STRING = 0x10;
    public final static int MODE_TX_FILE = 0x20;
    public final static int MODE_TX_IMAGE = 0x40;

    public final static int MODE_MAKEROOM = 0x100;
    public final static int MODE_DELETE_ROOM = 0x80;

    String userID;
    int mode;
    String message;
    ImageIcon image;
    long size;
    
    public ChatMsg(String userID, int mode, String message, ImageIcon image, long size)  {
        this.userID = userID;
        this.mode = mode;
        this.message = message;
        this.image = image;
        this.size = size;
    }

    public ChatMsg(String userID, int mode, String message, ImageIcon image) {
        this(userID, mode, message, image, 0);
    }

    public ChatMsg(String userID, int mode) {
        this(userID, mode, null, null, 0);
    }

    public ChatMsg(String userID, int mode, String message) {
        this(userID, mode, message, null, 0);
    }

    public ChatMsg(String userID, int mode, ImageIcon image) {
        this(userID, mode, null, image, 0);
    }

    public ChatMsg(String userID, int mode, String filename, long size) {
    	this(userID, mode, filename, null, size);
    }
    
}
