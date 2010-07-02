package Util;

import java.util.*;
import Data.DataManager;

public class MessageParser implements MessageParserInterface {
	public DataManager m_dataManager = null;
	public MessageRecvCallbackInterface m_callback = null;
	
	public MessageParser(DataManager dm) {
		m_dataManager = dm;
	}
	
	public void setCallback(MessageRecvCallbackInterface i) {
		m_callback = i;
	}
	
	/**
	 * the message format:
	 * 	num_of_dimensions dimension_name dimension_name ... value value value ...
	 * */
	public void receiveMessage(byte[] msg, int len) {
		try {
			Vector<String> dimNames = new Vector<String>();
			String message = Conversion.convertToString(msg);
			StringTokenizer st = new StringTokenizer(message, " ");
			int numOfDimension = Integer.parseInt( st.nextToken() );
			for( int i = 0; i < numOfDimension; i++ )
				dimNames.add( st.nextToken().trim() );
			
			int dimIndex = 0;
			while( st.hasMoreTokens() ) {
				float v = Float.parseFloat(st.nextToken());
				m_dataManager.getDimensionBySourceAndName("", dimNames.get(dimIndex)).add(v);	// real time data doesn't have source
				dimIndex = (dimIndex+1) % numOfDimension;
			}
			// need to refresh canvas now
			if( m_callback != null )
				m_callback.onReceive();
		} catch(Exception exp) {
			exp.printStackTrace();
		}
	}
}