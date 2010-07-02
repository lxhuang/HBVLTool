/**
 * @author lixinghu@usc.edu
 * @since 2010/6/30
 * the feeder listens to the port and update DataManager whenever there is new data coming
 * */

package Util;

import java.net.*;

public class RealtimeDataFeeder {
	private static final int MAX_BUF_SIZE = 4096;
	private boolean m_bListening = false;
	private short m_port = 17744;
	
	private MessageParserInterface m_parser = null;
	
	class DataFeederListener implements Runnable {
		private DatagramSocket m_socket = null;
		
		public DataFeederListener() {
			try {
				m_socket = new DatagramSocket(m_port);
			}catch( Exception exp ) {
				exp.printStackTrace();
			}
		}
		
		public void run() {
			byte[] buffer = new byte[MAX_BUF_SIZE];
			try {
				while( m_bListening ) {
					DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
					m_socket.receive(packet);
					byte[] message = packet.getData();
					int messageLen = packet.getLength();
					if( messageLen > 0 && m_parser != null ) {
						m_parser.receiveMessage(message, messageLen);
					}
				}
				System.out.println("stopped");
				m_socket.close();
			} catch(Exception exp) {
				exp.printStackTrace();
			}
		}
	}
	
	public RealtimeDataFeeder() {}
	
	public void setPort( short p ) { m_port = p; }
	
	public void setParser(MessageParserInterface parser) {
		m_parser = parser;
	}
	
	public void start() {
		m_bListening = true;
		DataFeederListener l = new DataFeederListener();
		l.run();
	}
	
	public void stop() {
		m_bListening = false;
	}
}
