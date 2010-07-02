/**
 * @author lixinghu@usc.edu
 * @since 2010/6/14
 * @modified 2010/6/26 make it a singleton
 * this module takes responsibility of exchanging messages between different modules/components
 * you could check all allowed components' names here
 * */

package UI;

import java.util.*;

public class NotificationCenter {
	private static NotificationCenter m_instance = null;
	
	private Vector<String> m_componentNames = new Vector<String>();
	private Vector<Object> m_components = new Vector<Object>();
	
	// this is a quick fix of link components. the problem here is:
	// I allow the whole application to have several TSCanvasComponents exising in their own JFrames,
	// and I allow different TSCanvasComponents are synced even if they are in different JFrames.
	private Vector<Vector<String>> m_linkedFrameNames = new Vector<Vector<String>>();
	
	
	private NotificationCenter() {}	// avoid this method being called. make it a singleton
	
	public static synchronized NotificationCenter getInstance() {
		if( m_instance == null )
			m_instance = new NotificationCenter();
		return m_instance;
	}
	
	private void _debugOutputPairFrames() {
		for( int i = 0; i < m_linkedFrameNames.size(); i++ ) {
			String l = m_linkedFrameNames.get(i).get(0)+"<->"+m_linkedFrameNames.get(i).get(1);
			System.out.print(l);
			System.out.print(" ");
		}
		System.out.println();
	}
	
	public synchronized void addLinkedFrame(String frame1, String frame2) {
		for( int i = 0; i < m_linkedFrameNames.size(); i++ ) {
			if( m_linkedFrameNames.get(i).contains(frame1.toLowerCase()) &&
				m_linkedFrameNames.get(i).contains(frame2.toLowerCase()) ) {
				return;
			}
		}
		Vector<String> newLink = new Vector<String>();
		newLink.add(frame1.toLowerCase());
		newLink.add(frame2.toLowerCase());
		m_linkedFrameNames.add(newLink);
		_debugOutputPairFrames();
	}
	
	public synchronized void removeLinkedFrame(String frame1, String frame2) {
		for( int i = 0; i < m_linkedFrameNames.size(); i++ ) {
			if( m_linkedFrameNames.get(i).contains(frame1.toLowerCase()) &&
				m_linkedFrameNames.get(i).contains(frame2.toLowerCase()) ) {
				m_linkedFrameNames.remove(i);
				_debugOutputPairFrames();
				return;
			}
		}
	}
	
	public synchronized void registerComponent(String name, Object c) throws Exception {
		m_componentNames.add(name);
		m_components.add(c);
		if( name.contains("TSCanvas") || // the reason to use contains: we may add prefix in front of TSCanvas to distinguish TSCanvas in different TSCanvasComponents
			name.contains("TSCanvasMenu") || 
			name.contains("TSAbstractCanvas") ||
			name.equalsIgnoreCase("TSCanvasManager") ||
			name.equalsIgnoreCase("FrameViewer") ) {
			TSMsgComponent comp = (TSMsgComponent)(m_components.lastElement());
			comp.setNotificationCenter(this);
		}
		else if( name.equalsIgnoreCase("TSSignalViewTable") ||
				 name.equalsIgnoreCase("TSSignalViewController") || 
				 name.equalsIgnoreCase("TSMain") ||
				 name.contains("TSLabelTable") ) {
			TSMsgPanel panel = (TSMsgPanel)(m_components.lastElement());
			panel.setNotificationCenter(this);
		}
		else if( name.contains("Panel") ) {	// the name should be Panel1, Panel2, etc.
			TSMsgPanel panel = (TSMsgPanel)(m_components.lastElement());
			panel.setNotificationCenter(this);
		}
		else {
			throw new Exception("Unknown component");
		}
	}
	
	public synchronized void removeComponent(String name) {
		int componentIndex = 0;
		for(; componentIndex < m_components.size(); componentIndex++) {
			if( m_componentNames.get(componentIndex).equalsIgnoreCase(name) )
				break;
		}
		if( componentIndex == m_components.size() )
			return;
		m_componentNames.remove(componentIndex);
		m_components.remove(componentIndex);
	}
	
	public synchronized boolean sendMessage(String from, String to, String content) {
		int index = 0;
		for(; index < m_componentNames.size(); index++) {
			if( m_componentNames.get(index).equalsIgnoreCase(to) )
				break;
		}
		if( index == m_componentNames.size() )
			return false;
		
		if( to.contains("TSCanvas") ||
			to.contains("TSCanvasMenu") ||
			to.contains("TSAbstractCanvas") ||
			to.equalsIgnoreCase("TSCanvasManager") ||
			to.equalsIgnoreCase("FrameViewer") ) {
			TSMsgComponent comp = (TSMsgComponent)(m_components.get(index));
			comp.receiveMessage(from, content);
		}
		else if( to.equalsIgnoreCase("TSSignalViewTable") ||
				 to.equalsIgnoreCase("TSSignalViewController") ||
				 to.equalsIgnoreCase("TSMain") ||
				 to.contains("TSLabelTable") ) {
			TSMsgPanel panel = (TSMsgPanel)(m_components.get(index));
			panel.receiveMessage(from, content);
		}
		else if( to.contains("Panel") ) {
			TSMsgPanel panel = (TSMsgPanel)(m_components.get(index));
			panel.receiveMessage(from, content);
		}
		
		return true;
	}
	
	// from contains the JFrame's name, while to doesn't.
	// this only works for TSCanvas, TSAbstractCanvas, since they are the only components that could be linked.
	public synchronized boolean sendMulticastMessage(String from, String to, String content) {
		for( int i = 0; i < m_linkedFrameNames.size(); i++ ) {
			String frame1 = m_linkedFrameNames.get(i).get(0);
			String frame2 = m_linkedFrameNames.get(i).get(1);
			if( from.toLowerCase().contains(frame1) ) {
				String compName = frame2+to;
				for( int j = 0; j < m_componentNames.size(); j++ ) {
					if( m_componentNames.get(j).toLowerCase().equalsIgnoreCase(compName) ) {
						((TSMsgComponent)m_components.get(j)).receiveMessage(from, content);
						break;
					}
				}
			}
			else if( from.toLowerCase().contains(frame2) ) {
				String compName = frame1+to;
				for( int j = 0; j < m_componentNames.size(); j++ ) {
					if( m_componentNames.get(j).toLowerCase().equalsIgnoreCase(compName) ) {
						((TSMsgComponent)m_components.get(j)).receiveMessage(from, content);
						break;
					}
				}
			}
		}
		return true;
	}
}
