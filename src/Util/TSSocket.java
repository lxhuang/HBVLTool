package Util;

import java.net.*;
import java.io.*;

public class TSSocket {
	protected String m_remoteAddr;
	protected int m_port;
	protected Socket client;
	protected DataOutputStream os = null;
	protected DataInputStream is = null;
	protected boolean bConnected = false;
	
	public TSSocket( String addr, int port ) {
		this.m_port = port;
		this.m_remoteAddr = addr;
	}
	public boolean isConnected() { return bConnected; }
	public void connect() {
		try {
			client = new Socket(m_remoteAddr, m_port);
			os = new DataOutputStream(client.getOutputStream());
			is = new DataInputStream(client.getInputStream());
			bConnected = true;
		} catch (UnknownHostException uhe) {
			
		} catch (IOException ie) {
			
		}
	}
	public void disconnect() {
		try {
			bConnected = false;
			is.close();
			os.close();
			client.close();
		} catch (IOException exp){
			
		}
	}
	public void send(String request)
	{
		if( os == null )
			return;
		try {
			byte[] requestBytes = Conversion.convertToByteArray(request);
			os.write(requestBytes);
			os.flush();
		} catch (IOException ioe) {
		}
	}
	public void recv()
	{
		int recvLength = 0;
		String message;
		if( is == null ) return;
		try {
			byte[] buffer = new byte[1024];
			recvLength = is.read(buffer);
			if( recvLength == 0 )
				return;
			message = Conversion.convertToString( buffer );
			
			if (onMessageReceived(message))
				return;
		} catch (IOException exp) {
			System.out.println("Error: client cannot receive messages");
		}
	}
	public boolean onMessageReceived( String message ) {
		return true;
	}
}
