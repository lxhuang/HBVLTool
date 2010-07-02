package Util;

import UI.TSCanvasComponent;
import java.util.*;

public class MessageRecvCallback implements MessageRecvCallbackInterface{
	public Vector<TSCanvasComponent> m_canvas = null;
	public MessageRecvCallback( Vector<TSCanvasComponent> c ) {
		m_canvas = c;
	}
	public void onReceive() {
		for( int i = 0; i < m_canvas.size(); i++ ) {
			m_canvas.get(i).revalidate();
		}
	}
}
