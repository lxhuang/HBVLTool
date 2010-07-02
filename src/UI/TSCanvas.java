/**
 * @author lixinghu@usc.edu
 * @since 2010/6/8
 * to display time series data in this canvas
 * */
package UI;

import javax.imageio.ImageIO;
import java.io.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.lang.reflect.*;
import java.io.IOException;
import Data.DataManager;
import Data.DimensionData;
import Data.TimeSamplePipe;
import Data.Label;

enum TimeFormat {
	MILLISECOND,
	SECOND,
	MINUTE,
	HOUR,
	DAY
};

public class TSCanvas extends TSMsgComponent implements MouseListener, MouseMotionListener, MouseWheelListener{
	private static final long serialVersionUID = -2021133535744938668L;
	
	public Vector<String> m_signalSources = new Vector<String>();	// the sources of the signals displayed in this canvas
	public Vector<String> m_signalNames = new Vector<String>();	// the names of the signals displayed in this canvas
	public Vector<Color> m_signalColors = new Vector<Color>();	// the colors used to draw signals
	private Vector<Integer> m_eventDisplayOffset = new Vector<Integer>();	// since all events are binary value (0/1), the y values of all samples are the same. sometimes, in order to distinguish them, we may manually add offset.
	
	public long m_visibleDuration = 0;	// how long the width of this canvas represents of the signal
	public int m_samplerate = 0;	// the sample rate of this signal
	public long m_startOfSignals = 0;	// the start time of the signal
	public long m_visibleUnitDuration = 0;	// the duration of one single unit, where the number of samples = sample rate
	
	public int m_verticalGridNumber = 5;	// this is the default value for the number of grids vertically
	public TimeFormat m_timeFormat = TimeFormat.MILLISECOND;	// the default time stamp display format
	
	public DataManager m_dataManager;
	public String m_parentName = "";	// used to distinguish TSCanvas in different TSCanvasComponents
	
	private int m_canvasWidth, m_canvasHeight;
	private TimeSamplePipe m_timeConverter;	// the conversion used to convert between time and sample index
	private long m_startOfVisibleSignal = 0;	// the current start position (time stamp) of visible signal
//	private long m_oldStartOfVisibleSignal = 0;	// the previous start position of visible signal
//	private boolean m_bNeedRedrawAll = true;	// whether I need to redraw the whole area or just part of it
	private int m_selLeftX = 0, m_selRightX = 0;	// the boundary of selection region
	private int m_emphasisX = -1, m_emphasizedSampleIndex = 0;	// the mouse position, I should highlight the value of the sample at that position
//	private int m_oldEmphasisX = -1;
	private int m_longestLengthOfSignals = 0;
	
	private int m_mousePressedX = 0, m_mousePressedY = 0;	// the position where the mouse is pressed
	private int m_mouseMovedX = 0;	// the current position of mouse
	private TSLabel m_labelWidget;	// the widget to process labels
	private int m_selectedLabelIndex=-1;	// when user double click, he can select a label.
	private TSLabelTable m_labelTable=null;
	
	private static final int m_ovalWidth = 8;
	private long m_deltaPerScroll = 1000;
	
	public TSCanvas(DataManager dm, long visibleDuration, int samplerate, long unitDuration, long start, int w, int h) {	// link the UI with the underlying data
		super();
		m_dataManager = dm;
		m_visibleDuration = visibleDuration;
		m_samplerate = samplerate;
		m_visibleUnitDuration = unitDuration;
		m_startOfSignals = start;
		m_canvasWidth = w;
		m_canvasHeight = h;
		m_timeConverter = new TimeSamplePipe(samplerate, unitDuration, start);
		m_labelWidget = new TSLabel(m_dataManager, this);
		
		setPreferredSize(new Dimension(m_canvasWidth, m_canvasHeight));
		
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
//		setOpaque(true);	// copyArea only works if it is set to true
	}
	
	// sometimes, we need to use m_parentName to distinguish TSCanvas in differnt TSCanvasComponents
	public void setParentName(String name) { m_parentName = name; }
	
	// set the time and unit options just for visualization
	public TimeSamplePipe setSamplerate(int sr) {
		m_samplerate = sr;
		m_timeConverter.m_samplerate = sr;
		repaint();
		return m_timeConverter;
	}
	public TimeSamplePipe setUnitDuration(long dur) {
		m_visibleUnitDuration = dur;
		m_timeConverter.m_unit = dur;
		repaint();
		return m_timeConverter;
	}
	public TimeSamplePipe setStartOfSignal(long start) {
		m_startOfSignals = start;
		m_timeConverter.m_start = start;
		repaint();
		return m_timeConverter;
	}
	public int setVerticalGridNumber(int n) {	// set the number of grids vertically
		int old = m_verticalGridNumber;
		m_verticalGridNumber = n;
		return old;
	}
	public void setTimeStampDisplayFormat(TimeFormat f) { m_timeFormat = f; }
	public void setSelectionRegion(int leftBoundary, int rightBoundary) {
//		Dimension sz = getSize();
//		int oldLeft = m_selLeftX;
//		int oldRight = m_selRightX;
		m_selLeftX = leftBoundary;
		m_selRightX = rightBoundary;
		
		// compute the repainting area
//		int refreshBeg = (oldLeft<m_selLeftX) ? oldLeft : m_selLeftX;
//		int refreshEnd = (oldRight>m_selRightX) ? oldRight : m_selRightX;
//		repaint(refreshBeg, 0, refreshEnd-refreshBeg, sz.height);
		repaint();
	}
	public void setDurationOfVisibleSignal(long t) {
		long total = m_longestLengthOfSignals/m_samplerate*m_visibleUnitDuration;
		if( t > total )
			return;
		m_visibleDuration = t;
		repaint();
	}
	
	// I want to highlight the sample at the mouse position and display its value
	public void setEmphasisPosition(int x) {
		Dimension sz = getSize();
		
//		m_oldEmphasisX = m_emphasisX;
		m_emphasisX = x;
			
//		int totalVisibleSampleNumber = (int)((double)m_visibleDuration/m_visibleUnitDuration*m_samplerate);
//		int begSampleIndex = m_timeConverter.getSampleIndexFrom((double)m_startOfVisibleSignal);
		// compute the repainting area 
		// it is the area between the previous emphasis point and the current emphasis point.
//		double old_t = ((double)m_oldEmphasisX/sz.width)*m_visibleDuration+m_startOfVisibleSignal;
		double new_t = ((double)m_emphasisX/sz.width)*m_visibleDuration+m_startOfVisibleSignal;
//		int old_index = m_timeConverter.getSampleIndexFrom(old_t);
		int new_index = m_timeConverter.getSampleIndexFrom(new_t);
//		int old_x = (int)((double)(old_index-begSampleIndex)/totalVisibleSampleNumber*sz.width);
//		int new_x = (int)((double)(new_index-begSampleIndex)/totalVisibleSampleNumber*sz.width);
		
//		int refreshBeg = ((old_x<new_x) ? old_x : new_x);
//		int refreshLen = Math.abs(new_x-old_x);
		m_emphasizedSampleIndex = new_index;
		
		repaint();
//		repaint(refreshBeg-m_ovalWidth, 0, refreshLen+2*m_ovalWidth+20, sz.height);	// add 20 in order to show the string
	}
	
	// change visible portion of signals
	public void reduceLeftBoundaryBy(long offset) {
//		m_oldStartOfVisibleSignal = m_startOfVisibleSignal;
		if( m_startOfVisibleSignal-offset < 0 )
			m_startOfVisibleSignal = 0;
		else
			m_startOfVisibleSignal-=offset;
//		m_bNeedRedrawAll = false;
		repaint();
	}
	public void increaseLeftBoundaryBy(long offset) {
//		m_oldStartOfVisibleSignal = m_startOfVisibleSignal;
		long t = m_longestLengthOfSignals/m_samplerate*m_visibleUnitDuration;
		if( m_startOfVisibleSignal+offset+m_visibleDuration>t )
			m_startOfVisibleSignal=t-m_visibleDuration;
		else
			m_startOfVisibleSignal += offset;
//		m_bNeedRedrawAll = false;
		repaint();
	}
	
	// add new signal to this canvas and repaint the canvas
	// if the signal exists, return false; else return true
	public boolean addNewSignal(String source, String name, Color c) {
		if( m_signalSources.contains(source) && m_signalNames.contains(name) )	// if existed
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
	
	// remove signal from this canvas and repaint the canvas
	public boolean removeSignal(String source, String name) {
		int signalIndex = 0;
		for( signalIndex = 0; signalIndex < m_signalNames.size(); signalIndex++ )
			if( m_signalSources.get(signalIndex).equalsIgnoreCase(source) && m_signalNames.get(signalIndex).equalsIgnoreCase(name) )
				break;
		if( signalIndex == m_signalNames.size() )
			return false;
		else {
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
	}
	
	// change a specific signal's color to a new one
	public void changeSignalColorTo(String source, String name, Color newColor) throws IOException {
		int signalIndex = 0;
		for(; signalIndex < m_signalNames.size(); signalIndex++) {
			if( m_signalNames.get(signalIndex).equalsIgnoreCase(name) && m_signalSources.get(signalIndex).equalsIgnoreCase(source) )
				break;
		}
		if( signalIndex == m_signalNames.size() )
			throw new IOException("cannot find the corresponding signal ["+source+" ,"+name+"]");
		m_signalColors.set(signalIndex, newColor);
		repaint();
	}
	
	// convert the time stamp to proper format to display
	private String convertTimestampToString(long t) {
		double t1;
		String disp;
		switch(m_timeFormat) {
		case MILLISECOND:
			return Long.toString(t).concat("ms");
		case SECOND:
			t1 = (double)t/1000;
			disp = Double.toString(t1);
			disp = disp.substring(0, disp.indexOf(".")).concat("s");
			return disp;
		case MINUTE:
			t1 = (double)t/1000/60;
			disp = Double.toString(t1);
			disp = disp.substring(0, disp.indexOf(".")).concat("m");
			return disp;
		case HOUR:
			t1 = (double)t/1000/60/60;
			disp = Double.toString(t1);
			disp = disp.substring(0, disp.indexOf(".")).concat("h");
			return disp;
		case DAY:
			t1 = (double)t/1000/60/60/24;
			disp = Double.toString(t1);
			disp = disp.substring(0, disp.indexOf(".")).concat("d");
			return disp;
		default:
			return "null";	
		}
	}
	
	// painting code
	private void paintGrid(Graphics2D g2) {
		Dimension sz = getSize();
		Graphics2D newg = (Graphics2D)g2.create();
		
		// cannot draw too many horizontal grids
		if( m_visibleDuration/m_visibleUnitDuration > 20 )
			return;
		
		// draw horizontal grids
		Font f = new Font(null, Font.PLAIN, 8);
		newg.setFont(f);
		
		int horizontalIntervalNumber = (int)(m_visibleDuration/m_visibleUnitDuration);
		int horizontalInterval = sz.width/horizontalIntervalNumber;
		for( int i = 0; i < horizontalIntervalNumber; i++ ) {
			newg.setColor(new Color(225, 225, 225));
			newg.drawLine((i+1)*horizontalInterval, 0, (i+1)*horizontalInterval, sz.height);
			newg.setColor(Color.BLACK);
			
			// why add m_startOfSignals?
			// m_startOfSignals is the actual start time of the signals, it can be any time.
			// however, m_startOfVisibleSignal is the offset to the start of the signal.
			// we separate the two concepts because visualization code doesn't need to know the actual time.
			long t = ((int)( (m_startOfVisibleSignal+m_startOfSignals)/m_visibleUnitDuration)+1+i)*m_visibleUnitDuration;
			String str_t = convertTimestampToString(t);
			newg.drawString(str_t, (i+1)*horizontalInterval, sz.height);
		}
		
		// draw vertical grids
		int verticalInterval = sz.height/m_verticalGridNumber;
		float maxValue = 0.0f, minValue = 0.0f;
		for( int i = 0; i < m_signalNames.size(); i++ ) {
			DimensionData d = m_dataManager.getDimensionBySourceAndName(m_signalSources.get(i), m_signalNames.get(i));
			if( maxValue == minValue ) {
				maxValue = d.m_dataMax;
				minValue = d.m_dataMin;
			}
			else {
				if( maxValue != d.m_dataMax || minValue != d.m_dataMin ) {	// if two dimensions' range are different, it doesn't make sense to display the absolute value
					maxValue = minValue = 0.0f;
					break;
				}
			}
		}

		if( maxValue != minValue ) {	// draw the calibration
			newg.setColor(Color.BLACK);
			float delta = (maxValue-minValue)/m_verticalGridNumber;
			for( int i = 0; i < m_verticalGridNumber-1; i++ ) {
				float v = minValue+(i+1)*delta;
				newg.drawString(Float.toString(v), 1, sz.height/m_verticalGridNumber*(m_verticalGridNumber-1-i));
			}
		}
		newg.setStroke(new BasicStroke(1));
		newg.setColor(new Color(225, 225, 225));	// set the color of the grid line
		for( int i = 0; i < m_verticalGridNumber-1; i++ ) {
			newg.drawLine(0, (i+1)*verticalInterval, sz.width, (i+1)*verticalInterval);
		}
		
		newg.dispose();
	}
	private void paintSignal(Graphics2D g2) {
		Dimension sz = getSize();
		int totalVisibleSampleNumber = (int)((double)m_visibleDuration/m_visibleUnitDuration*m_samplerate);
		int begSampleIndex = m_timeConverter.getSampleIndexFrom((double)m_startOfVisibleSignal);
		int endSampleIndex = m_timeConverter.getSampleIndexFrom((double)m_startOfVisibleSignal+m_visibleDuration);
		
		// if the start of visible signal changes and it is not caused by the change of resolution (i.e. visible duration)
		// then we could improve the painting performance by just repainting the changed area
		/*
		if( m_bNeedRedrawAll == false && m_oldStartOfVisibleSignal!=m_startOfVisibleSignal ) {
			long span = m_startOfVisibleSignal-m_oldStartOfVisibleSignal;
			int newContentWidth = (int)((double)span/m_visibleDuration*sz.width);
			
			if( m_startOfVisibleSignal > m_oldStartOfVisibleSignal ) {	// copy to the left (newContentWidth>0)
				g2.copyArea(newContentWidth, 0, sz.width-newContentWidth, sz.height, -newContentWidth, 0);
				g2.setClip(sz.width-newContentWidth, 0, newContentWidth, sz.height);
			}
			else {
				g2.copyArea(0, 0, sz.width-newContentWidth, sz.height, -newContentWidth, 0);
				g2.setClip(0, 0, -newContentWidth, sz.height);
			}
			m_bNeedRedrawAll = true;
		}
		*/
		
		// if endSampleIndex-begSampleIndex is larger than sz.width, then it is not necessary to draw all points
		int skipSampleNumber = (endSampleIndex-begSampleIndex)/sz.width;
		if( skipSampleNumber < 2 )
			skipSampleNumber = 1;
		
		// draw background
		Rectangle clipRect = g2.getClip().getBounds();
		int clipL = (int)(clipRect.getX());
		int clipT = (int)(clipRect.getY());
		int clipR = (int)(clipRect.getMaxX());
		g2.setColor(Color.WHITE);
		g2.fillRect(clipL, clipT, clipRect.width, clipRect.height);
		g2.setStroke(new BasicStroke(2));
		for( int i = 0; i < m_signalNames.size(); i++ ) {
			DimensionData d = m_dataManager.getDimensionBySourceAndName(m_signalSources.get(i), m_signalNames.get(i));
			if( d == null )
				continue;
			
			g2.setColor(m_signalColors.get(i));
			int yOffset = m_eventDisplayOffset.get(i).intValue();
			float maxValue = d.m_dataMax;	// dimension's value range
			float minValue = d.m_dataMin;
			int dataLen = d.getLen();
			for( int j = begSampleIndex; j < endSampleIndex; j+=skipSampleNumber ) {
				if( j+1 >= dataLen )
					break;
				
				int xx1=0, yy1=0, xx2=0, yy2=0;
				xx1 = (int)((double)(j-begSampleIndex)/totalVisibleSampleNumber*sz.width);
				xx2 = (int)((double)(j+skipSampleNumber-begSampleIndex)/totalVisibleSampleNumber*sz.width);
				if( d.m_type == DimensionData.CONTINUOUS ) {
					yy1 = (int)(sz.height-((double)d.m_data[(d.m_dataHead+1+j)%dataLen]-minValue)/(maxValue-minValue)*sz.height);
					yy2 = (int)(sz.height-((double)d.m_data[(d.m_dataHead+1+j+skipSampleNumber)%dataLen]-minValue)/(maxValue-minValue)*sz.height);
				}
				else if( d.m_type == DimensionData.EVENT ) {
					if( d.m_data[(d.m_dataHead+1+j)%dataLen]!=DimensionData.NOEVENT )
						yy1 = sz.height/2+yOffset;
					else
						yy1 = sz.height;
					if( d.m_data[(d.m_dataHead+1+j+skipSampleNumber)%dataLen]!=DimensionData.NOEVENT )
						yy2 = sz.height/2+yOffset;
					else
						yy2 = sz.height;
				}
				
				if( (xx1 >= clipL && xx1 <= clipR) || (xx2 >= clipL && xx2 <= clipR) ) {
					g2.drawLine(xx1, yy1, xx2, yy2);
					if( j == m_emphasizedSampleIndex ) {
						g2.fillOval(xx1-m_ovalWidth/2, yy1-m_ovalWidth/2, m_ovalWidth, m_ovalWidth);
						
						// draw the indicated value at the top of the canvas
						// draw the value for different dimensions separately, right now, the interval is 30 pixels
						if( d.m_type == DimensionData.CONTINUOUS ) {
							String str_val = Float.toString(d.m_data[(d.m_dataHead+1+j)%dataLen]);
							str_val = str_val.substring(0, str_val.indexOf(".")+2);
							g2.setFont(new Font(null, Font.PLAIN, 8));
							g2.drawString(str_val, (int)(sz.width*0.9-30*i), (int)(sz.height*0.05));
						}
						else if( d.m_type == DimensionData.EVENT ) {
							try {
								String event = d.getEvent((int)d.m_data[(d.m_dataHead+1+j)%dataLen]);
								g2.setFont(new Font(null, Font.PLAIN, 8));
								g2.drawString(event, (int)(sz.width*0.1+30*i), (int)(sz.height*0.05));
							} catch ( ArrayIndexOutOfBoundsException exp ) {
								continue;
							}
						}
					}
				}
			}
		}
	}
	
	private int m_toolboxX = 0, m_toolboxY = 0;	// used to draw the translucent toolbox and process the mouse events
	private BufferedImage m_labelIcon = null, m_deleteIcon = null, m_playIcon = null;
	private static final int m_toolboxSize = 32;
	private void paintSelection(Graphics2D g2) {
		Dimension sz = getSize();
		Graphics2D newg = (Graphics2D)(g2.create());
		newg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
		newg.setColor(Color.GRAY);
		newg.fillRect(m_selLeftX, 0, m_selRightX-m_selLeftX, sz.height);
		
		if( m_selRightX != m_selLeftX ) {	// draw the tool box
			if( m_labelIcon == null ) {
				try {
					m_labelIcon = ImageIO.read( new File("resource/label.png") );
					m_deleteIcon = ImageIO.read( new File("resource/delete.png") );
					m_playIcon = ImageIO.read( new File("resource/play.jpg") );
				} catch(Exception exp) {
					exp.printStackTrace();
				}
			}
			
			m_toolboxX = m_selRightX+5;
			m_toolboxY = sz.height/2;
			newg.drawImage(m_playIcon, m_toolboxX, m_toolboxY-m_toolboxSize, m_toolboxSize, m_toolboxSize, null);
			newg.drawImage(m_labelIcon, m_toolboxX+m_toolboxSize, m_toolboxY-m_toolboxSize, m_toolboxSize, m_toolboxSize, null);
			newg.drawImage(m_deleteIcon, m_toolboxX+2*m_toolboxSize, m_toolboxY-m_toolboxSize, m_toolboxSize, m_toolboxSize, null);
		}
		newg.dispose();
	}
	private void paintLabels(Graphics2D g2) {
		Dimension sz = getSize();
		Graphics2D newg = (Graphics2D)g2.create();
		int totalVisibleSampleNumber = (int)((double)m_visibleDuration/m_visibleUnitDuration*m_samplerate);
		int begSampleIndex = m_timeConverter.getSampleIndexFrom((double)m_startOfVisibleSignal);
		int endSampleIndex = m_timeConverter.getSampleIndexFrom((double)m_startOfVisibleSignal+m_visibleDuration);
		
		for( int i = 0; i < m_signalNames.size(); i++ ) {
			String name = m_signalNames.get(i);
			String source = m_signalSources.get(i);
			DimensionData d = m_dataManager.getDimensionBySourceAndName(source, name);
			Vector<Label> l = d.m_labels;
			if( l.size() == 0 )
				continue;
			
			newg.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			newg.setColor(m_signalColors.get(i));
			newg.setFont(new Font(null, Font.ITALIC, 9));
			for( int j = 0; j < l.size(); j++ ) {
				if( l.get(j).m_end>begSampleIndex && l.get(j).m_beg<endSampleIndex ) {
					int x1 = (l.get(j).m_beg<begSampleIndex) ? begSampleIndex : l.get(j).m_beg;
					int x2 = (l.get(j).m_end>endSampleIndex) ? endSampleIndex : l.get(j).m_end;
					x1 = (int)((double)(x1-begSampleIndex)/totalVisibleSampleNumber*sz.width);
					x2 = (int)((double)(x2-begSampleIndex)/totalVisibleSampleNumber*sz.width);
					newg.drawLine(x1, sz.height/2, x2, sz.height/2);
					newg.drawString(l.get(j).m_meta, (x1+x2)/2, sz.height/2-5);
				}
				else if( l.get(j).m_beg >= endSampleIndex )
					break;
			}
		}
		newg.dispose();
	}
	
	public void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D)g;
		
		// paint signal
		paintSignal(g2);
		// paint the grids
		paintGrid(g2);
		// paint the selection
		paintSelection(g2);
		
		// paint the label. and yes, we only allow single kind of label every time
		paintLabels(g2);
	}
	
	/**
	 * @param from: the message is sent from which module
	 * @param content: the content of the message, the format of the message must be:
	 * changeSamplerate:1000;changeUnitDuration:10;changeVisibleDurationBy:10;changeStartOfVisibleSignalBy:10;changeSignalColor:source name Color
	 * the keywords TSCanvas can deal with contain: 
	 * 	changeSamplerate, 
	 * 	changeUnitDuration, 
	 * 	changeVisibleDurationBy,
	 * 	changeVisibleDurationTo, 
	 * 	changeStartOfVisibleSignalBy,
	 * 	changeStartOfVisibleSignalToSampleIndex,
	 * 	changeSignalColor,
	 * 	changeStartOfSignals,
	 * 	changeTimestampFormat,
	 * 	saveLabelToFile,
	 * 	loadLabelFromFile,
	 * 	closeLabel
	 * */
	public void receiveMessage(String from, String content) {
		// analyze the content
		try {
			StringTokenizer st = new StringTokenizer(content, ";");
			while(st.hasMoreTokens()) {
				String msg = st.nextToken().trim();
				String cmd = msg.substring(0,msg.indexOf(":")).trim();
				String arg = msg.substring(msg.indexOf(":")+1).trim();
				if( cmd.equalsIgnoreCase("changeSamplerate") ) {
					int sr = Integer.parseInt(arg);
					if( sr == m_samplerate )
						return;
					// added on 2010/6/27
					m_startOfVisibleSignal = (int)((double)m_startOfVisibleSignal*m_samplerate/sr);
					setSamplerate(sr);
				}
				else if( cmd.equalsIgnoreCase("changeUnitDuration") ) {
					long dur = Long.parseLong(arg);
					if( dur == m_visibleUnitDuration )
						return;
					m_deltaPerScroll = dur;
					setUnitDuration(dur);
				}
				else if( cmd.equalsIgnoreCase("changeVisibleDurationBy") ) {
					long dur = Long.parseLong(arg);
					if( dur == 0 )
						return;
					setDurationOfVisibleSignal(m_visibleDuration+dur);
				}
				else if( cmd.equalsIgnoreCase("changeVisibleDurationTo") ) {
					long t = Long.parseLong(arg);
					setDurationOfVisibleSignal(t);
					repaint();
				}
				else if( cmd.equalsIgnoreCase("changeStartOfVisibleSignalBy") ) {
					int offset = Integer.parseInt(arg);
					if( offset < 0 )
						reduceLeftBoundaryBy(-offset);
					else
						increaseLeftBoundaryBy(offset);
				}
				else if( cmd.equalsIgnoreCase("changeStartOfVisibleSignalToSampleIndex") ) {
					int sampleIndex = Integer.parseInt(arg);
					long t = m_timeConverter.getTimeFromSampleIndex(sampleIndex);
					long offset = (t-m_startOfSignals) - m_startOfVisibleSignal;
					if( offset < 0 )
						reduceLeftBoundaryBy(-offset);
					else
						increaseLeftBoundaryBy(offset);
					
					// to move the sliding button
					if( m_privateNotification != null ) {
						String notify = "changePositionBy:"+Long.toString(offset)+";";
						m_privateNotification.sendMessage(m_parentName+"TSCanvas", m_parentName+"TSAbstractCanvas", notify);
					}
				}
				else if( cmd.equalsIgnoreCase("changeSignalColor") ) {
					String source = arg.substring( 0, arg.indexOf(" ", 0) );
					String name = arg.substring( arg.indexOf(" ", 0)+1, arg.lastIndexOf(" ") );
					String color = arg.substring(arg.lastIndexOf(" ")+1);
					Color newColor = null;
					try {
						Field field = Class.forName("java.awt.Color").getField(color);
						newColor = (Color)field.get(null);
					} catch(Exception exp) {
						exp.printStackTrace();
					}
					
					// checking
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
				else if( cmd.equalsIgnoreCase("changeStartOfSignals") ) {
					long s = Long.parseLong(arg);
					setStartOfSignal(s);
				}
				else if( cmd.equalsIgnoreCase("changeTimestampFormat") ) {
					if( arg.equalsIgnoreCase("ms") )
						setTimeStampDisplayFormat(TimeFormat.MILLISECOND);
					else if( arg.equalsIgnoreCase("s") )
						setTimeStampDisplayFormat(TimeFormat.SECOND);
					else if( arg.equalsIgnoreCase("m") )
						setTimeStampDisplayFormat(TimeFormat.MINUTE);
					else if( arg.equalsIgnoreCase("h") )
						setTimeStampDisplayFormat(TimeFormat.HOUR);
					else if( arg.equalsIgnoreCase("d") )
						setTimeStampDisplayFormat(TimeFormat.DAY);
				}
				else if( cmd.equalsIgnoreCase("saveLabelToFile") ) {
					m_labelWidget.saveLabels(arg);
				}
				else if( cmd.equalsIgnoreCase("loadLabelFromFile") ) {
					m_labelWidget.loadLabel(arg);
					repaint();
				}
				else if( cmd.equalsIgnoreCase("closeLabel") ) {
					if( arg.isEmpty() )
						m_labelWidget.closeLabels();
					else {
						m_labelWidget.saveLabels(arg);
						m_labelWidget.closeLabels();
					}
					repaint();
				}
			}
		} catch(Exception exp) {
			exp.printStackTrace();
		}
	}
	
	// mouse related events' delegates
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {
		m_mousePressedX = e.getX();
		m_mousePressedY = e.getY();
		
		// operations on the tool box
		if( m_selLeftX != m_selRightX ) {
			Dimension sz = getSize();
			int clickX=m_mousePressedX, clickY=m_mousePressedY;
			if( clickX>m_toolboxX && clickX<m_toolboxX+m_toolboxSize && clickY<m_toolboxY && clickY>m_toolboxY-m_toolboxSize ) {
				System.out.println("Play");
				
				if( m_signalSources.isEmpty() == false ) {
					double t1 = (double)m_selLeftX/sz.width*m_visibleDuration+m_startOfVisibleSignal;
					double t2 = (double)m_selRightX/sz.width*m_visibleDuration+m_startOfVisibleSignal;
					String content = m_signalSources.get(0)+" "+Integer.toString((int)t1)+" "+Integer.toString((int)t2);
					m_privateNotification.sendMessage(m_parentName+"TSCanvas", "QuickTimePlayer", content);
				}
			}
			else if( clickX>m_toolboxX+m_toolboxSize && clickX<m_toolboxX+2*m_toolboxSize && clickY<m_toolboxY && clickY>m_toolboxY-m_toolboxSize ) {
				System.out.println("Label");
				double t1 = (double)m_selLeftX/sz.width*m_visibleDuration+m_startOfVisibleSignal;
				double t2 = (double)m_selRightX/sz.width*m_visibleDuration+m_startOfVisibleSignal;
				int labelBegIndex = m_timeConverter.getSampleIndexFrom(t1);
				int labelEndIndex = m_timeConverter.getSampleIndexFrom(t2);
				if( m_labelWidget.addLabel(labelBegIndex, labelEndIndex, clickX, clickY) )
					repaint();
				
				// notify the Label Table
				// if the table doesn't exist yet, create one, and add it into a frame
				if( m_labelTable == null ) {
					m_labelTable = new TSLabelTable(500,200);
					m_labelTable.setParentFrameName(m_parentName);
					JFrame f = new JFrame(m_parentName+" Label Table");
					f.setSize(500,200);
					f.add(m_labelTable);
					f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
					f.pack();
					f.setVisible(true);
					try {
						if( m_privateNotification != null )
							m_privateNotification.registerComponent(m_parentName+"TSLabelTable", m_labelTable);
					} catch(Exception exp) { exp.printStackTrace(); }
				}
				if( m_privateNotification != null ) {
					String content = "addLabel: default"+" "+Integer.toString(labelBegIndex)+" "+Integer.toString(labelEndIndex);
					m_privateNotification.sendMessage(m_parentName+"TSCanvas", m_parentName+"TSLabelTable", content);
				}
			}
			else if( clickX>m_toolboxX+2*m_toolboxSize && clickX<m_toolboxX+3*m_toolboxSize && clickY<m_toolboxY && clickY>m_toolboxY-m_toolboxSize ) {
				System.out.println("Delete");
				if( m_selectedLabelIndex != -1 ) {	// make sure there is some label being selected
					if( m_labelWidget.deleteLabelByIndex(m_selectedLabelIndex) ) {
						m_selectedLabelIndex = -1;	// reset it
						repaint();
					}
				}
//				double t1 = (double)m_selLeftX/sz.width*m_visibleDuration+m_startOfVisibleSignal;
//				double t2 = (double)m_selRightX/sz.width*m_visibleDuration+m_startOfVisibleSignal;
//				int labelBegIndex = m_timeConverter.getSampleIndexFrom(t1);
//				int labelEndIndex = m_timeConverter.getSampleIndexFrom(t2);
//				if( m_labelWidget.deleteLabel(labelBegIndex, labelEndIndex) ) {
//					System.out.println("Delete success");
//					repaint();
//				}
			}
			m_selectedLabelIndex = -1;	// reset it
		}
		
		setSelectionRegion(m_mousePressedX, m_mousePressedX);	// erase the selection region
	}
	public void mouseReleased(MouseEvent e) {}
	public void mouseMoved(MouseEvent e) {
		setEmphasisPosition(e.getX());
	}
	public void mouseDragged(MouseEvent e) {
		final int yOffset = 10;
		int signalIndex = 0;
		for(; signalIndex < m_signalNames.size(); signalIndex++) {
			DimensionData d = m_dataManager.getDimensionBySourceAndName(m_signalSources.get(signalIndex), m_signalNames.get(signalIndex));
			if( d.m_type == DimensionData.EVENT && d.m_data[(d.m_dataHead+1+m_emphasizedSampleIndex)%d.getLen()] != DimensionData.NOEVENT )
				break;
		}
		if(signalIndex < m_signalNames.size()) {
			int deltaY = e.getY()-m_mousePressedY;
			if( deltaY > 15 ) {	// if the movement is large enough, then we know it is what the user really want to do
				m_eventDisplayOffset.set( signalIndex, new Integer(m_eventDisplayOffset.get(signalIndex).intValue()+yOffset) );
				m_mousePressedY = e.getY();
				if( m_privateNotification != null ) {
					String content = "changeEventYOffset:"+m_signalSources.get(signalIndex)+" "+m_signalNames.get(signalIndex)+" "+Integer.toString(deltaY);
					m_privateNotification.sendMessage(m_parentName+"TSCanvas", m_parentName+"TSAbstractCanvas", content);
				}
				return;
			}
			else if( deltaY < -15 ) {
				m_eventDisplayOffset.set( signalIndex, new Integer(m_eventDisplayOffset.get(signalIndex).intValue()-yOffset) );
				m_mousePressedY = e.getY();
				if( m_privateNotification != null ) {
					String content = "changeEventYOffset:"+m_signalSources.get(signalIndex)+" "+m_signalNames.get(signalIndex)+" "+Integer.toString(deltaY);
					m_privateNotification.sendMessage(m_parentName+"TSCanvas", m_parentName+"TSAbstractCanvas", content);
				}
				return;
			}
		}
		
		m_mouseMovedX = e.getX();
		int left = (m_mouseMovedX<m_mousePressedX) ? m_mouseMovedX : m_mousePressedX;
		int right = (m_mouseMovedX>m_mousePressedX) ? m_mouseMovedX : m_mousePressedX;
		setSelectionRegion(left, right);
	}
	
	// click could have several meanings: (1) click on the emphasized sample (2) double click to select the label
	public void mouseClicked(MouseEvent e) {
		final int threshold = 5;
		int clickX = e.getX();
		int clickY = e.getY();
		Dimension sz = getSize();
		if( e.getClickCount() == 1 ) {
			int offset = sz.width;
			int matchSignalIndex = 0;
			for( int i = 0; i < m_signalNames.size(); i++ ) {
				DimensionData d = m_dataManager.getDimensionBySourceAndName(m_signalSources.get(i), m_signalNames.get(i));
				float yVal = d.m_data[(d.m_dataHead+1+m_emphasizedSampleIndex)%d.getLen()];
				int y = (int)(sz.height-((double)yVal-d.m_dataMin)/(d.m_dataMax-d.m_dataMin)*sz.height);
				if( Math.abs(y-clickY) < offset ) {
					offset = Math.abs(y-clickY);
					matchSignalIndex = i;
				}
			}
			
			if( offset < threshold ) {	// close enough to the sample
				System.out.println(m_signalNames.get(matchSignalIndex)+" "+Integer.toString(m_emphasizedSampleIndex));
				if( m_privateNotification != null ) {
					String content = m_signalSources.get(matchSignalIndex)+" "+Integer.toString(m_emphasizedSampleIndex);
					m_privateNotification.sendMessage(m_parentName+"TSCanvas", "FrameViewer", content);
				}
			}
		} else {
			double t = (double)clickX/sz.width*m_visibleDuration+m_startOfVisibleSignal;
			int sampleIndex = m_timeConverter.getSampleIndexFrom(t);
			Label l = m_labelWidget.findLabel(sampleIndex);
			if( l != null ) {
				int left=0, right=0;
				m_selectedLabelIndex = m_labelWidget.findLabelIndex(l);
				double left_t = m_timeConverter.getTimeFromSampleIndex(l.m_beg);
				double right_t = m_timeConverter.getTimeFromSampleIndex(l.m_end);
				left = (int)( (double)((left_t-m_startOfSignals)-m_startOfVisibleSignal)/m_visibleDuration*sz.width );
				right = (int)( (double)((right_t-m_startOfSignals)-m_startOfVisibleSignal)/m_visibleDuration*sz.width );
				setSelectionRegion(left, right);
			}
		}
	}
	
	public void mouseWheelMoved(MouseWheelEvent e) {
		if( m_selLeftX != m_selRightX ) {
			setSelectionRegion(m_selLeftX, m_selLeftX);	// erase the selection region
		}
		if( m_signalNames.isEmpty() )
			return;
		if( e.getWheelRotation() > 0 ) {
			increaseLeftBoundaryBy(m_deltaPerScroll);
			if( m_privateNotification != null ) {
				String content = "changePositionBy:"+Long.toString(m_deltaPerScroll)+";";
				m_privateNotification.sendMessage(m_parentName+"TSCanvas", m_parentName+"TSAbstractCanvas", content);
				m_privateNotification.sendMulticastMessage(m_parentName+"TSCanvas", "TSAbstractCanvas", content);
				
				content = "changeStartOfVisibleSignalBy:"+Long.toString(m_deltaPerScroll)+";";
				m_privateNotification.sendMulticastMessage(m_parentName+"TSCanvas", "TSCanvas", content);
			}
		}
		else {
			reduceLeftBoundaryBy(m_deltaPerScroll);
			if( m_privateNotification != null ) {
				String content = "changePositionBy:"+Long.toString(-m_deltaPerScroll)+";";
				m_privateNotification.sendMessage(m_parentName+"TSCanvas", m_parentName+"TSAbstractCanvas", content);
				m_privateNotification.sendMulticastMessage(m_parentName+"TSCanvas", "TSAbstractCanvas", content);
				
				content = "changeStartOfVisibleSignalBy:"+Long.toString(-m_deltaPerScroll)+";";
				m_privateNotification.sendMulticastMessage(m_parentName+"TSCanvas", "TSCanvas", content);
			}
		}
	}
	
	// Unit testing
	private static void createAndShowUI() {
		JFrame f = new JFrame("Test TSCanvas");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setSize(800, 200);
		
		DataManager dm = DataManager.getInstance();
		dm.loadDimensionsFromFile("test.oka");
		
		TSCanvas tc = new TSCanvas(dm, 10000, 10, 1000, 0, 800, 200);
		tc.addNewSignal("\\\\sfs\\data\\public\\1.avi", "smile", Color.GREEN);
		
		f.add(tc);
		f.setVisible(true);
	}
	public static void main( String[] args ) {
		Runnable doCreateAndShowUI = new Runnable() {
			public void run() {
				createAndShowUI();
			}
		};
		SwingUtilities.invokeLater(doCreateAndShowUI);
	}
}
