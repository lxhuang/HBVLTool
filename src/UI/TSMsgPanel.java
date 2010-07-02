package UI;

import javax.swing.*;

public class TSMsgPanel extends JPanel{
	private static final long serialVersionUID = -8873610883232423678L;
	protected NotificationCenter m_privateNotification = null;
	
	public TSMsgPanel() {
		
	}
	
	public void receiveMessage(String from, String content) {
		
	}
	
	public void setNotificationCenter(NotificationCenter c) {
		m_privateNotification = c;
	}
	
}
