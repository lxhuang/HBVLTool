/**
 * @author lixinghu@usc.edu
 * @since 2010/6/13
 * this is the extended JComponent which implements message passing interface
 * */

package UI;

import javax.swing.*;

public class TSMsgComponent extends JComponent{
	private static final long serialVersionUID = 6897706149988662225L;
	
	protected NotificationCenter m_privateNotification = null;
	
	public TSMsgComponent() {
		
	}
	
	public void receiveMessage(String from, String content) {
		
	}
	
	public void setNotificationCenter(NotificationCenter c) {
		m_privateNotification = c;
	}
}
