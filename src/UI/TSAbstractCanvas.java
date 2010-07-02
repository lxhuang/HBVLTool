/**
 * @author lixinghu@usc.edu
 * @since 2010/6/8
 * this is the canvas to display the overview of the time series data also we include a sliding button here.
 * the button is used to change the visible duration of time series in TSCanvas
 * the idea is from google visualization tool annotated time line
 * */

package UI;

import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.lang.reflect.*;
import Data.DataManager;
import Data.DimensionData;

public class TSAbstractCanvas extends TSMsgComponent implements MouseListener, MouseMotionListener{
	private static final long serialVersionUID = 6312687662466993845L;
	public Vector<String> m_signalSources = new Vector<String>();
	public Vector<String> m_signalNames = new Vector<String>();
	public Vector<Color> m_signalColors = new Vector<Color>();
	public Vector<Integer> m_eventDisplayOffset = new Vector<Integer>();
	
	public DataManager m_dataManager;
	public String m_parentName = "";	// used to distinguish TSAbstractCanvas in different TSCanvasComponents
	
	// the duration of visible signals (decide the length of the sliding button and how many samples will be drawn in TSCanvas)
	private long m_visibleDuration;
	private int m_samplerate;	// how many samples are there in a single unit
	private long m_visibleUnitDuration;	// the duration of one single unit, where the number of samples = sample rate
	private long m_slidingButtonBeg;	// the start position from which the signals are visible
	private int m_longestLengthOfSignals=0;	// the longest length of all signals visible inside this component
	private int m_mousePosX = 0;	// the current position of mouse
	// when scroll the mouse in the canvas area, the sliding button in abstract canvas should also move accordingly
	// when calculating the movement of the sliding button, we should be careful about the lost of precision when converting double to integer
	private double m_residue = 0.0;
	
	private static final Color m_backgroundColor = Color.GRAY;
	private static final Color m_buttonBoundaryColor = Color.BLACK;
	private static final int m_nearEnough = 2;
	private static final int m_buttonBoundaryWidth = 2;
	
	/**
	 * @param DataManager
	 * @param visibleDuration: the duration of the visible signals (decide the length of the sliding button)
	 * @param samplerate
	 * @param unitDuration: the duration of one single unit, where the number of samples = sample rate
	 * @param w: the width of the component
	 * @param h: the height of the component
	 * */
	public TSAbstractCanvas(DataManager dm, long visibleDuration, int samplerate, long unitDuration, int w, int h) {
		m_dataManager = dm;
		m_visibleDuration = visibleDuration;
		m_samplerate = samplerate;
		m_visibleUnitDuration = unitDuration;
		m_slidingButtonBeg = 0;
		setPreferredSize(new Dimension(w,h));
		
		addMouseMotionListener(this);
		addMouseListener(this);
	}
	
	// sometimes, we need to use m_parentName to distinguish TSAbstractCanvas in differnt TSCanvasComponents
	public void setParentName(String name) { m_parentName = name; }
	
	// add new signal into the abstract canvas and repaint the whole component
	// if the signal exists, return false; else return true
	public boolean addNewSignal(String source, String name, Color c) {
		if( m_signalSources.contains(source) && m_signalNames.contains(name) )
			return false;
		m_signalSources.add(source);
		m_signalNames.add(name);
		m_signalColors.add(c);
		m_eventDisplayOffset.add(new Integer(0));
		
		// refresh the longest length of signals
		if( m_signalNames.size() == 1 )
			m_longestLengthOfSignals = m_dataManager.getDimensionBySourceAndName(m_signalSources.get(0), m_signalNames.get(0)).getLen();
		else {
			int justAddedLen = m_dataManager.getDimensionBySourceAndName(m_signalSources.lastElement(), m_signalNames.lastElement()).getLen();
			if( justAddedLen > m_longestLengthOfSignals )
				m_longestLengthOfSignals = justAddedLen;
		}
		
		repaint();
		return true;
	}
	
	// remove the signal from the canvas and repaint the whole component
	// if succeeds, return true; else return false
	public boolean removeSignal(String source, String name) {
		int signalIndex = 0;
		for( ; signalIndex < m_signalSources.size(); signalIndex++ ) {
			if( m_signalSources.get(signalIndex).equalsIgnoreCase(source) && m_signalNames.get(signalIndex).equalsIgnoreCase(name) )
				break;
		}
		if( signalIndex == m_signalSources.size() )
			return false;
		m_signalSources.remove(signalIndex);
		m_signalNames.remove(signalIndex);
		m_signalColors.remove(signalIndex);
		m_eventDisplayOffset.remove(signalIndex);
		
		// refresh the longest length of signals
		m_longestLengthOfSignals = Integer.MIN_VALUE;
		for( int i = 0; i < m_signalNames.size(); i++ ) {
			DimensionData d = m_dataManager.getDimensionBySourceAndName(m_signalSources.get(i), m_signalNames.get(i));
			if( d == null )
				continue;
			if( d.getLen() > m_longestLengthOfSignals )
				m_longestLengthOfSignals = d.getLen();
		}
		
		repaint();
		return true;
	}
	
	// change a specific signal's color to a new one
	public void changeSignalColorTo(String source, String name, Color newColor) throws IOException{
		int signalIndex = 0;
		for(; signalIndex < m_signalColors.size(); signalIndex++) {
			if( m_signalSources.get(signalIndex).equalsIgnoreCase(source) && m_signalNames.get(signalIndex).equalsIgnoreCase(name) )
				break;
		}
		if( signalIndex == m_signalColors.size() )
			throw new IOException("cannot find the corresponding signal ["+source+" ,"+name+"]");
		m_signalColors.set(signalIndex, newColor);
		repaint();
	}
	
	// change the boundary and position of the sliding button and repaint
	private void changeLeftBoundaryOfButtonBy(int x) {
		Dimension sz = getSize();
		long totalDuration = (long)((double)m_longestLengthOfSignals/m_samplerate*m_visibleUnitDuration);
		long delta = (long)((double)x/sz.width*totalDuration);
		long old = 0;
		
		if( m_slidingButtonBeg+delta < 0 )
			return;
		else if( m_slidingButtonBeg+delta >= m_slidingButtonBeg+m_visibleDuration )
			return;
		else {
			old = m_slidingButtonBeg;
			m_slidingButtonBeg += delta;
			m_visibleDuration -= delta;
		}
		
		// compute the area to be repainted
		long refreshBeg = (old<m_slidingButtonBeg) ? old : m_slidingButtonBeg;
		long refreshLen = Math.abs(old-m_slidingButtonBeg);
		int refreshAreaBeg = (int)((double)refreshBeg/totalDuration*sz.width);
		int refreshAreaLen = (int)((double)refreshLen/totalDuration*sz.width);
		
		repaint(refreshAreaBeg-m_buttonBoundaryWidth, 0, refreshAreaLen+m_buttonBoundaryWidth*2, sz.height);
	}
	private void changeRightBoundaryOfButtonBy(int x) {
		Dimension sz = getSize();
		long totalDuration = (long)((double)m_longestLengthOfSignals/m_samplerate*m_visibleUnitDuration);
		long delta = (long)((double)x/sz.width*totalDuration);
		long slidingButtonEnd = m_slidingButtonBeg+m_visibleDuration;
		long old = 0;
		
		if( slidingButtonEnd+delta > totalDuration )
			return;
		else if( slidingButtonEnd+delta <= m_slidingButtonBeg )
			return;
		else {
			old = m_visibleDuration;
			m_visibleDuration += delta;
		}

		// compute the area to be repainted
		long refreshBeg = m_slidingButtonBeg + ((old<m_visibleDuration) ? old : m_visibleDuration);
		long refreshLen = Math.abs(m_visibleDuration-old);
		int refreshAreaBeg = (int)((double)refreshBeg/totalDuration*sz.width);
		int refreshAreaLen = (int)((double)refreshLen/totalDuration*sz.width);
		repaint(refreshAreaBeg-m_buttonBoundaryWidth, 0, refreshAreaLen+m_buttonBoundaryWidth*2, sz.height);
	}
	private void changePositionOfButtonBy(int x) {
		Dimension sz = getSize();
		long totalDuration = (long)((double)m_longestLengthOfSignals/m_samplerate*m_visibleUnitDuration);
		long delta = (long)((double)x/sz.width*totalDuration);
		long old = 0;
		
		if( m_slidingButtonBeg+delta < 0 )
			return;
		else if( m_slidingButtonBeg+m_visibleDuration+delta > totalDuration )
			return;
		else {
			old = m_slidingButtonBeg;
			m_slidingButtonBeg += delta;
		}
		
		// compute the area to be repainted
		long refreshBeg = (m_slidingButtonBeg<old) ? m_slidingButtonBeg : old;
		long refreshLen = refreshBeg+Math.abs(m_slidingButtonBeg-old)+m_visibleDuration;
		int refreshAreaBeg = (int)((double)refreshBeg/totalDuration*sz.width);
		int refreshAreaLen = (int)((double)refreshLen/totalDuration*sz.width);
		repaint(refreshAreaBeg-m_buttonBoundaryWidth, 0, refreshAreaLen+m_buttonBoundaryWidth, sz.height);
	}
	
	// paint the full length of the signals in an abstract way (meaning, not every sample point is drawn)
	private void paintSignals(Graphics2D g2) {
		Dimension sz = getSize();
		// paint the background
		Rectangle clipRect = g2.getClip().getBounds();
		int clipL = (int)(clipRect.getX());
		int clipT = (int)(clipRect.getY());
		int clipR = (int)(clipRect.getMaxX());
		g2.setColor(Color.WHITE);
		g2.fillRect(clipL, clipT, clipRect.width, clipRect.height);
		
		// paint the signals
		for( int i = 0; i < m_signalNames.size(); i++ ) {
			DimensionData d = m_dataManager.getDimensionBySourceAndName(m_signalSources.get(i), m_signalNames.get(i));
			if( d == null )
				continue;
			g2.setColor(m_signalColors.get(i));
			float maxValue = d.m_dataMax;
			float minValue = d.m_dataMin;
			int yOffset = m_eventDisplayOffset.get(i).intValue();
			// since the total width is sz.width, if the number of samples is larger than this number, it doesn't
			// make sense to draw these samples one by one, because the resolution only allows us to draw at most sz.width samples
			int skipSampleNumber = m_longestLengthOfSignals/sz.width;
			int n = m_longestLengthOfSignals/skipSampleNumber;	// the actual number of samples to be drawn
			int dataLen = d.getLen();
			for( int j = 0; j < n-1; j++ ) {
				if( (j+1)*skipSampleNumber >= dataLen )	// out of range
					continue;
				
				int x1=0,x2=0,y1=0,y2=0;
				
				x1 = (int)((double)j/n*sz.width);
				x2 = (int)((double)(j+1)/n*sz.width);
				if( d.m_type == DimensionData.CONTINUOUS ) {
					y1 = (int)(sz.height-(d.m_data[(d.m_dataHead+1+j*skipSampleNumber)%dataLen]-minValue)/(maxValue-minValue)*sz.height);
					y2 = (int)(sz.height-(d.m_data[(d.m_dataHead+1+(j+1)*skipSampleNumber)%dataLen]-minValue)/(maxValue-minValue)*sz.height);
				}
				else if( d.m_type == DimensionData.EVENT ) {
					if( d.m_data[(d.m_dataHead+1+j*skipSampleNumber)%dataLen]!=DimensionData.NOEVENT )
						y1 = sz.height/2+yOffset;
					else
						y1 = sz.height;
					if( d.m_data[(d.m_dataHead+1+(j+1)*skipSampleNumber)%dataLen]!=DimensionData.NOEVENT )
						y2 = sz.height/2+yOffset;
					else
						y2 = sz.height;
				}
				if( (x1 >= clipL && x1 <= clipR) || (x2 >= clipL && x2 <= clipR) )
					g2.drawLine(x1, y1, x2, y2);
			}
		}
	}
	
	// paint the sliding button
	private void paintSlidingButton(Graphics2D g2) {
		if( m_signalSources.isEmpty() )
			return;
		
		Dimension sz = getSize();
		
		Graphics2D newg = (Graphics2D)g2.create();
		
		long totalDuration = (long)((double)m_longestLengthOfSignals/m_samplerate*m_visibleUnitDuration);
		
		// calculate the start position of the sliding button given the start of the visible signal
		// the signals within the button will be visible in the TSCanvas
		// and calculate the length of the sliding button given the length of visible signals
		int startPositionOfButton = (int)((double)m_slidingButtonBeg/totalDuration*sz.width);
		int lengthOfButton = (int)((double)m_visibleDuration/totalDuration*sz.width);
		
		if( startPositionOfButton+lengthOfButton > sz.width ) {
			lengthOfButton = sz.width-startPositionOfButton;
			m_visibleDuration =(int)((double)lengthOfButton/sz.width*totalDuration);
		}
		
		// print the boundary of button
		g2.setColor(m_buttonBoundaryColor);
		g2.setStroke(new BasicStroke(m_buttonBoundaryWidth));
		g2.drawLine(startPositionOfButton, 0, startPositionOfButton, sz.height);
		g2.drawLine(startPositionOfButton+lengthOfButton, 0, startPositionOfButton+lengthOfButton, sz.height);
		
		// draw the translucent mask over the parts where the signals are not visible
		g2.setColor(m_backgroundColor);
		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
		g2.fillRect(0, 0, startPositionOfButton, sz.height);
		g2.fillRect(startPositionOfButton+lengthOfButton, 0, sz.width-startPositionOfButton-lengthOfButton, sz.height);
		
		newg.dispose();
	}
	
	public void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D)g;
		// paint signals
		paintSignals(g2);
		// paint sliding button
		paintSlidingButton(g2);
	}
	
	/**
	 * @param from: the message is sent from which module
	 * @param content: the content of the message, the format of the message must be:
	 * changeVisibleDuration:10;changeSignalColor:source name Color;changePositionBy:10;changeSamplerate:20;
	 * the keywords TSAbstractCanvas can deal with contain: 
	 * 	changeVisibleDurationTo,
	 * 	changeSignalColor,
	 * 	changePositionBy,
	 * 	changeSamplerate,
	 * 	changeUnitDuration,
	 * 	changeEventYOffset,
	 * 	changeSlidingButtonLeftBoundBy,
	 * 	changeSlidingButtonRightBoundBy,
	 * 	changeSlidingButtonPositionBy
	 * */
	public void receiveMessage(String from, String content) {
		try {
			StringTokenizer st = new StringTokenizer(content, ";");
			while(st.hasMoreTokens()) {
				String msg = st.nextToken().trim();
				String cmd = msg.substring(0, msg.indexOf(":")).trim();
				String arg = msg.substring(msg.indexOf(":")+1).trim();
				if( cmd.equalsIgnoreCase("changeVisibleDurationTo") ) {
					long t = Long.parseLong(arg);
					m_visibleDuration = t;
					repaint();
				}
				else if( cmd.equalsIgnoreCase("changeSignalColor") ) {
					String source = arg.substring( 0,arg.indexOf(" ",0) );
					String name = arg.substring( arg.indexOf(" ",0)+1, arg.lastIndexOf(" ") );
					String color = arg.substring(arg.lastIndexOf(" ")+1);
					Field field = Class.forName("java.awt.Color").getField(color);
					Color newColor = (Color)field.get(null);
					
					int index = 0;
					for( index = 0; index < m_signalSources.size(); index++ ) {
						if( m_signalSources.get(index).equalsIgnoreCase(source) && m_signalNames.get(index).equalsIgnoreCase(name) )
							break;
					}
					if( index == m_signalSources.size() )	// didn't find
						return;
					Color oldColor = m_signalColors.get(index);
					if( oldColor.equals(newColor) )	// if color doesn't change
						return;
					
					changeSignalColorTo(source, name, newColor);
				}
				else if( cmd.equalsIgnoreCase("changePositionBy") ) {
					int delta = Integer.parseInt(arg);
					long t = (long)( (double)m_longestLengthOfSignals/m_samplerate*m_visibleUnitDuration );
					double d = (double)delta*getSize().width/t;	// actual value
					delta = (int)Math.floor(d+m_residue);
					
					m_residue = (d+m_residue)-Math.floor(d+m_residue);
					
					changePositionOfButtonBy(delta);
				}
				else if( cmd.equalsIgnoreCase("changeSamplerate") ) {
					int newSamplerate = Integer.parseInt(arg);
					// added on 2010/6/27
					m_slidingButtonBeg = (int)((double)m_slidingButtonBeg*m_samplerate/newSamplerate);
					m_samplerate = newSamplerate;
					repaint();
				}
				else if( cmd.equalsIgnoreCase("changeUnitDuration") ) {
					int newDur = Integer.parseInt(arg);
					m_visibleUnitDuration = newDur;
					repaint();
				}
				else if( cmd.equalsIgnoreCase("changeEventYOffset") ) {
					String source = arg.substring( 0,arg.indexOf(" ",0) );
					String name = arg.substring( arg.indexOf(" ",0)+1, arg.lastIndexOf(" ") );
					String offset = arg.substring(arg.lastIndexOf(" ")+1);
					
					int index = 0;
					for( index = 0; index < m_signalSources.size(); index++ ) {
						if( m_signalSources.get(index).equalsIgnoreCase(source) && m_signalNames.get(index).equalsIgnoreCase(name) )
							break;
					}
					if( index == m_signalSources.size() )	// didn't find
						return;
					
					int offsetVal = Integer.parseInt(offset);
					int y = m_eventDisplayOffset.get(index).intValue();
					if( offsetVal < 0 )
						m_eventDisplayOffset.set(index, new Integer(y-5));
					else
						m_eventDisplayOffset.set(index, new Integer(y+5));
					
					repaint();
				}
				else if( cmd.equalsIgnoreCase("changeSlidingButtonLeftBoundBy") ) {
					int offset = Integer.parseInt(arg);
					changeLeftBoundaryOfButtonBy(offset);
				}
				else if( cmd.equalsIgnoreCase("changeSlidingButtonRightBoundBy") ) {
					int offset = Integer.parseInt(arg);
					changeRightBoundaryOfButtonBy(offset);
				}
				else if( cmd.equalsIgnoreCase("changeSlidingButtonPositionBy") ) {
					int offset = Integer.parseInt(arg);
					changePositionOfButtonBy(offset);
				}
			}
		} catch(Exception exp) {
			exp.printStackTrace();
		}
	}
	
	// mouse related events' delegate
	public void mousePressed(MouseEvent e) {	// this is the start action of any dragging
		m_mousePosX = e.getX();
	}
	public void mouseReleased(MouseEvent e) {}
	public void mouseMoved(MouseEvent e) {
		int mousePosX = e.getX();
		Dimension sz = getSize();
		// change the cursor based on the position of mouse,
		// if the mouse is near the boundary of sliding button, it will turn to W_RESIZE_CURSOR or E_RESIZE_CURSOR;
		// if the mouse is inside the sliding button, it will turn to MOVE_CURSOR
		
		long totalDuration = (long)((double)m_longestLengthOfSignals/m_samplerate*m_visibleUnitDuration);
		
		// calculate the start position of the sliding button given the start of the visible signal
		// the signals within the button will be visible in the TSCanvas
		// and calculate the length of the sliding button given the length of visible signals
		int startPositionOfButton = (int)((double)m_slidingButtonBeg/totalDuration*sz.width);
		int lengthOfButton = (int)((double)m_visibleDuration/totalDuration*sz.width);
		
		if( Math.abs(mousePosX-startPositionOfButton)<=m_nearEnough )
			setCursor( new Cursor(Cursor.W_RESIZE_CURSOR) );
		else if( Math.abs(mousePosX-startPositionOfButton-lengthOfButton)<m_nearEnough )
			setCursor( new Cursor(Cursor.E_RESIZE_CURSOR) );
		else if( mousePosX>startPositionOfButton+m_nearEnough && mousePosX<startPositionOfButton+lengthOfButton-m_nearEnough )
			setCursor( new Cursor(Cursor.HAND_CURSOR) );
		else
			setCursor( new Cursor(Cursor.DEFAULT_CURSOR) );
	}
	public void mouseDragged(MouseEvent e) {
		if(m_signalNames.isEmpty())
			return;
		
		if( e.getX() > getSize().width || e.getX() < 0 )
			return;
			
		int cursorType = getCursor().getType();
		switch( cursorType ) {
		case Cursor.E_RESIZE_CURSOR:
			changeRightBoundaryOfButtonBy(e.getX()-m_mousePosX);
			if( m_privateNotification != null ) {
				int delta = e.getX()-m_mousePosX;
				String content = "changeSlidingButtonRightBoundBy:"+Integer.toString(delta)+";";
				m_privateNotification.sendMulticastMessage(m_parentName+"TSAbstractCanvas", "TSAbstractCanvas", content);
				
				int w = getSize().width;
				long t = (long)( (double)m_longestLengthOfSignals/m_samplerate*m_visibleUnitDuration );
				delta = (int)((double)delta/w*t);
				
				content = "changeVisibleDurationBy:"+Integer.toString(delta)+";";
				m_privateNotification.sendMessage(m_parentName+"TSAbstractCanvas", m_parentName+"TSCanvas", content);
				m_privateNotification.sendMulticastMessage(m_parentName+"TSAbstractCanvas", "TSCanvas", content);
			}
			break;
		case Cursor.W_RESIZE_CURSOR:
			changeLeftBoundaryOfButtonBy(e.getX()-m_mousePosX);
			if( m_privateNotification != null ) {
				int delta = e.getX()-m_mousePosX;
				String content = "changeSlidingButtonLeftBoundBy:"+Integer.toString(delta)+";";
				m_privateNotification.sendMulticastMessage(m_parentName+"TSAbstractCanvas", "TSAbstractCanvas", content);
				
				int w = getSize().width;
				long t = (long)( (double)m_longestLengthOfSignals/m_samplerate*m_visibleUnitDuration );
				delta = (int)((double)delta/w*t);
				
				content = "changeVisibleDurationBy:"+Integer.toString(-delta)+";"+
					"changeStartOfVisibleSignalBy:"+Integer.toString(delta)+";";
				m_privateNotification.sendMessage(m_parentName+"TSAbstractCanvas", m_parentName+"TSCanvas", content);
				m_privateNotification.sendMulticastMessage(m_parentName+"TSAbstractCanvas", "TSCanvas", content);
			}
			break;
		case Cursor.HAND_CURSOR:
			changePositionOfButtonBy(e.getX()-m_mousePosX);
			if( m_privateNotification != null ) {
				int delta = e.getX()-m_mousePosX;
				String content = "changeSlidingButtonPositionBy:"+Integer.toString(delta)+";";
				m_privateNotification.sendMulticastMessage(m_parentName+"TSAbstractCanvas", "TSAbstractCanvas", content);
				
				int w = getSize().width;
				long t = (long)( (double)m_longestLengthOfSignals/m_samplerate*m_visibleUnitDuration );
				delta = (int)((double)delta/w*t);	// convert to time
				
				content = "changeStartOfVisibleSignalBy:"+Integer.toString(delta)+";";
				m_privateNotification.sendMessage(m_parentName+"TSAbstractCanvas", m_parentName+"TSCanvas", content);
				m_privateNotification.sendMulticastMessage(m_parentName+"TSAbstractCanvas", "TSCanvas", content);
			}
			break;
		default:
			break;
		}
		m_mousePosX = e.getX();
	}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mouseClicked(MouseEvent e) {}
	
	// Unit testing
	private static void doCreateAndShowUI() {
		JFrame f = new JFrame("Test Abstract Canvas");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setSize(800, 100);
		
		DataManager dm = DataManager.getInstance();
		dm.loadDimensionsFromFile("test.oka");
		
		TSAbstractCanvas tc = new TSAbstractCanvas(dm, 10000, 10, 1000, 800, 100);
		tc.addNewSignal("\\\\sfs\\data\\public\\1.avi", "smile", Color.RED);
		
		f.add(tc);
		f.setVisible(true);
	}
	public static void main(String[] args) {
		Runnable createAndShowUI = new Runnable() {
			public void run() {
				doCreateAndShowUI();
			}
		};
		SwingUtilities.invokeLater(createAndShowUI);
	}
}
