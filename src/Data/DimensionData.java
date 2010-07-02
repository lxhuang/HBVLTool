/**
 * @author lixinghu@usc.edu
 * @since 2010/6/4
 * 
 * to hold data for a single dimension
 * */

package Data;

import java.util.Calendar;
import java.util.Vector;
import java.io.*;

enum DimensionType {
	INVALID,
	CONTINUOUS,
	EVENT
};

public class DimensionData {
	public static final DimensionType CONTINUOUS = DimensionType.CONTINUOUS;
	public static final DimensionType EVENT = DimensionType.EVENT;
	public static final int NOEVENT = -1;
	
	public float[] m_data;	// implemented as a circular queue
	public float m_dataMax, m_dataMin;	// for visualization purpose
	public int m_dataHead, m_dataTail;	// for the circular queue
	public String m_name;
	public DimensionType m_type;	// continuous, or discrete event
	public String m_source;	// where does the data come from or what does the data stand for
	
	// we should allow user to add labels for each dimension
	public Vector<Label> m_labels = new Vector<Label>();
	public String m_labelAuthor = "";
	
	// if the type is discrete event, we store the events here (only store the name, for example the transcripts)
	// and m_data use the index to refer to the events. for example, 
	// m_events: ['a', 'b', 'c']
	// m_data: [0,0,0,1,1,1,2,2,2] this means the first three samples refer to 'a' and so on
	private Vector<String> m_events;
	
	// if the user wants to save real time data stream, we need to have some extra buffer in case the m_data array
	// is not enough. since m_data is a circular queue and its size is defined ahead, when new data point comes and
	// replaces the old value, that old value should be saved in the buf1, and once the buf1 is full, those data should
	// be saved to disk, and at the same time, buf2 should take the responsibility to hold the old value from m_data
	// (simple two-buffer mechanism)
	private int m_bufFlag=0;	// used to indicate which buf is now active to receive old values
	private int m_buf1Index=0, m_buf2Index=0;	// used to iterate through buf
	private String m_filename;	// to store the data in buf
	private float[] m_buf1;
	private float[] m_buf2;
	
	/**
	 * @param dataLen the predefined length of the m_data array
	 * @param dataMax the maximum value of the data
	 * @param dataMin the minimum value of the data
	 * @param name the name of this dimension
	 * @param type the type of this dimension
	 * @param bufLen the length of the buf1 and buf2
	 * */
	public DimensionData(int dataLen, float dataMax, float dataMin, String name, DimensionType type, int bufLen, String filename, String source) {
		m_data = new float[dataLen+1];
		m_dataMax = dataMax;
		m_dataMin = dataMin;
		m_name = name;
		m_type = type;
		m_buf1 = new float[bufLen];
		m_buf2 = new float[bufLen];
		m_dataHead = m_dataTail = 0;
		m_filename = filename;
		m_source = source;
		if( m_type == DimensionType.EVENT ) {
			for( int i = 0; i < dataLen; i++ )
				m_data[i] = NOEVENT;
		}
	}
	
	public DimensionData(int dataLen, float dataMax, float dataMin, String name, DimensionType type, String source) {
		m_data = new float[dataLen+1];
		m_dataMax = dataMax;
		m_dataMin = dataMin;
		m_name = name;
		m_type = type;
		m_buf1 = m_buf2 = null;
		m_dataHead = m_dataTail = 0;
		m_source = source;
		if( m_type == DimensionType.EVENT ) {
			for( int i = 0; i < dataLen; i++ )
				m_data[i] = NOEVENT;
			m_dataTail = dataLen;
		}
	}
	
	public void setEvent(Vector<String> evts) {
		m_events = new Vector<String>();
		for( int i = 0; i < evts.size(); i++ )
			m_events.add(evts.get(i));
	}
	
	public void setEvents(String[] evts) {
		m_events = new Vector<String>();
		for( String s : evts )
			m_events.add(s);
	}
	
	private class SaveBufToFile implements Runnable {
		private String _fn;
		private float[] _buf;
		public SaveBufToFile(String filename, float[] buf) {
			_fn = filename;
			_buf = new float[buf.length];
			System.arraycopy(buf, 0, _buf, 0, buf.length);
		}
		public void run() {
			try {
				PrintWriter pw = new PrintWriter(new FileWriter(new File(_fn), true));
				for( int i = 0; i < _buf.length; i++ )
					pw.println(_buf[i]);
				pw.close();
			}
			catch( Exception exp ) {
				exp.printStackTrace();
			}
		}
	}
	
	private void saveToBuf(float d) {
		if( m_bufFlag == 0 && m_buf1Index < m_buf1.length ) {
			m_buf1[m_buf1Index++] = d;
		}
		else if( m_bufFlag == 0 && m_buf1Index >= m_buf1.length ) {
			m_bufFlag = 1 - m_bufFlag;
			m_buf2[m_buf2Index++] = d;
			
			// save buf1 to file
			SaveBufToFile saveThread = new SaveBufToFile(m_filename, m_buf1);
			new Thread(saveThread).run();
			
			m_buf1Index = 0;
		}
		else if( m_bufFlag == 1 && m_buf2Index < m_buf2.length ) {
			m_buf2[m_buf2Index++] = d;
		}
		else if( m_bufFlag == 1 && m_buf2Index >= m_buf2.length ) {
			m_bufFlag = 1 - m_bufFlag;
			m_buf1[m_buf1Index++] = d;
			
			// save buf2 to file
			SaveBufToFile saveThread = new SaveBufToFile(m_filename, m_buf2);
			new Thread(saveThread).run();
			
			m_buf2Index = 0;
		}
	}
	
	public void saveAllDataTo(String filename) {
		try {
			PrintWriter pw = new PrintWriter(new FileWriter(new File(filename)));
			// first copy the buf file if it exists
			if( m_filename != null && (new File(m_filename)).exists() ) {
				BufferedReader br = new BufferedReader(new FileReader(new File(m_filename)));
				String textOfLine;
				while( (textOfLine = br.readLine()) != null ) {
					pw.println(textOfLine);
				}
				br.close();
			}
			// copy the buf
			if( m_bufFlag == 0 ) {
				for( int i = 0; i < m_buf1Index; i++ )
					pw.println(m_buf1[i]);
			}
			else {
				for( int i = 0; i < m_buf2Index; i++ )
					pw.println(m_buf2[i]);
			}
			// copy the main array
			int n = getLen();
			for( int i = 0; i < n; i++ )
				pw.println(m_data[(m_dataHead+1+i)%m_data.length]);
			pw.close();
		} catch( Exception exp ) {
			exp.printStackTrace();
		}
	}
	
	// get the actual length of m_data
	public int getLen() {
		return (m_dataTail+m_data.length-m_dataHead)%m_data.length;
	}
	
	// add new data point
	public void add(float d) {
		m_dataTail = (m_dataTail+1) % m_data.length;
		m_data[m_dataTail] = d;
		if( m_dataTail == m_dataHead ) {	// full
			m_dataHead = (m_dataHead+1) % m_data.length;
			if( m_buf1 != null ) { // save to buf
				saveToBuf(m_data[m_dataHead]);
			}
		}
	}
	
	// set the data at position p to value d
	public void setAt(int p, float d) {
		if( p < 0 || p >= m_data.length )
			throw new ArrayIndexOutOfBoundsException("out or range");
		m_data[p] = d;
	}
	
	/**
	 * @param index 
	 * @throws ArrayIndexOutOfBoundsException
	 * */
	public float getAt(int index) {
		if( index < 0 || index >= getLen() ) {
			throw new ArrayIndexOutOfBoundsException("out of range");
		}
		return m_data[(m_dataHead+1+index)%m_data.length];
	}
	
	/** get data between [from, from+n-1]
	 * 
	 * @param from
	 * @param n
	 * @param val
	 * @throws ArrayIndexOutOfBoundsException
	 */
	public void get(int from, int n, float[] val) {
		if( from < 0 || from >= getLen() || n < 1 || n+from-1 >= getLen() )
			throw new ArrayIndexOutOfBoundsException("out of range");
		for( int i = 0; i < n; i++ ) {
			val[i] = m_data[(m_dataHead+1+from+i)%m_data.length];
		}
	}
	
	/***
	 * 
	 * @param evtIndex
	 * @return
	 * @throws ArrayIndexOutOfBoundsException
	 */
	public String getEvent(int evtIndex) {
		try {
			return m_events.get(evtIndex);
		} catch (Exception exp) {
			throw new ArrayIndexOutOfBoundsException();
		}
	}
	
	public void setLabelAuthor(String author) { m_labelAuthor = author; }
	
	/**
	 * @param meta: the meta info attached to this label
	 * @param beg, end: the boundary of this label, should not out of the signal's range
	 * the labels are ordered by the beginning of each label in ascending order 
	 * */
	public boolean addLabel(String meta, int beg, int end) {
		if( beg < 0 || end >= getLen() || beg >= end )
			return false;
		
		Label newLabel = new Label();
		newLabel.m_author = m_labelAuthor;
		newLabel.m_beg = beg;
		newLabel.m_end = end;
		newLabel.m_meta = meta;
		newLabel.m_date = Calendar.getInstance();
		
		int sz = m_labels.size();
		int i = sz-1;
		for(; i >= 0; i-- ) {
			if( m_labels.get(i).m_beg < beg ) {
				m_labels.add(i+1, newLabel);
				break;
			}
		}
		if( i < 0 )
			m_labels.add(0, newLabel);
		return true;
	}
	public boolean addLabel(String meta, int beg, int end, String date) {
		if( beg < 0 || end >= getLen() || beg >= end )
			return false;
		
		Label newLabel = new Label();
		newLabel.m_author = m_labelAuthor;
		newLabel.m_beg = beg;
		newLabel.m_end = end;
		newLabel.m_meta = meta;
		
		//date format: 9/20 1:30:9
		int month = Integer.parseInt( date.substring(0,date.indexOf("/")) );
		int day = Integer.parseInt( date.substring(date.indexOf("/")+1, date.indexOf(" ")) );
		int hour = Integer.parseInt( date.substring(date.indexOf(" ")+1, date.indexOf(":")) );
		int minute = Integer.parseInt( date.substring(date.indexOf(":")+1, date.lastIndexOf(":")) );
		int second = Integer.parseInt( date.substring(date.lastIndexOf(":")+1) );
		
		Calendar c = Calendar.getInstance();
		c.set(Calendar.MONTH, month);
		c.set(Calendar.DAY_OF_MONTH, day);
		c.set(Calendar.HOUR, hour);
		c.set(Calendar.MINUTE, minute);
		c.set(Calendar.SECOND, second);
		
		newLabel.m_date = c;
		
		int sz = m_labels.size();
		int i = sz-1;
		for(; i >= 0; i-- ) {
			if( m_labels.get(i).m_beg < beg ) {
				m_labels.add(i+1, newLabel);
				break;
			}
		}
		if( i < 0 )
			m_labels.add(0, newLabel);
		return true;
	}
	
	/**
	 * delete the label
	 * */
	public boolean deleteLabelByIndex(int index) {
		if( index >= 0 && index < m_labels.size() ) {
			m_labels.remove(index);
			return true;
		} else
			return false;
	}
	public boolean deleteLabel(int beg, int end) {
		int sz = m_labels.size();
		int lo = 0, hi = sz-1;
		while( lo <= hi ) {
			int mid = (lo+hi)/2;
			if( m_labels.get(mid).m_beg == beg ) {
				int fw=1, bw=1;
				if( m_labels.get(mid).m_end == end ) {
					m_labels.remove(mid);
					return true;
				}
				while( mid+fw<sz && m_labels.get(mid+fw).m_beg==beg ) {
					if( m_labels.get(mid+fw).m_end==end ) {
						m_labels.remove(mid+fw);
						return true;
					}
					fw++;
				}
				while( mid-bw>=0 && m_labels.get(mid-bw).m_beg==beg ) {
					if( m_labels.get(mid-bw).m_end==end ) {
						m_labels.remove(mid-bw);
						return true;
					}
					bw++;
				}
				break;
			}
			else if( m_labels.get(mid).m_beg < beg ) {
				lo = mid+1;
			}
			else {
				hi = mid-1;
			}
		}
		return false;
	}
	
	public Label findLabel(int sampleIndex) {
		int sz = m_labels.size();
		int lo = 0, hi = sz-1;
		while( lo <= hi ) {
			int mid = (lo+hi)/2;
			if( m_labels.get(mid).m_beg<sampleIndex && m_labels.get(mid).m_end>sampleIndex )
				return m_labels.get(mid);
			else if( m_labels.get(mid).m_beg>sampleIndex )
				hi = mid-1;
			else if( m_labels.get(mid).m_end<sampleIndex )
				lo = mid+1;
		}
		return null;
	}
	
	public int findLabelIndex(Label l) {
		return m_labels.indexOf(l);
	}
	
	public void clearLabels() {
		m_labels.clear();
	}
}
