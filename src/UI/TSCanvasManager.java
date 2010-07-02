/**
 * @author lixinghu@usc.edu
 * @since 2010/6/25
 * */

package UI;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.awt.image.BufferedImage;
import java.io.*;

public class TSCanvasManager extends TSMsgComponent implements MouseListener, MouseMotionListener {
	private static final long serialVersionUID = 8870106869875440222L;
	private static final String m_addIconFile = "resource/add.png";	// this is the first icon
	private static final String m_delIconFile = "resource/delete.png";	// this is the second one
	private static final int m_iconSize = 32;
	private static final int m_iconGap = 10;
	private static final float m_leftSplitRatio = 0.8f;
	private static final float m_topSplitRatio = 0.1f;
	
	private static final float m_defaultCanvasWidthRatio = 0.8f;
	private static final int m_defaultCanvasHeight = 40;
	private static final int m_defaultCanvasGap = 10;
	
	private BufferedImage m_addIconImage = null;
	private BufferedImage m_delIconImage = null;
	
	public Vector<String> m_canvasNames = new Vector<String>();
	private Vector<Integer> m_iconTopLeftPositions = new Vector<Integer>();		// record the top left x of each icon
	private Vector<Integer> m_canvasTopLeftPositions = new Vector<Integer>();	// record the top left y of each canvas
	private int m_selectedCanvasIndex = -1;
	private int m_canvasIndex = 0;	// as a counter of canvas (including those deleted)

	// link pair of canvas so that they could be scrolled at the same time.
	private Vector<String> m_linkBegComp = new Vector<String>();	// these two vectors are used to record pairs
	private Vector<String> m_linkEndComp = new Vector<String>();
	private Vector<Vector<Integer>> m_linkPoint;	// canvases are linked with dash line, this vector is used to record the starting and end point of each dash line
	private boolean m_bLinkStart = false;
	private int m_linkLineBegX=0, m_linkLineBegY=0, m_linkLineEndX=0, m_linkLineEndY=0;
	private JPopupMenu m_popupMenu;	// used to delete dash line
	private PopupMenuActionListener m_popupMenuActionListener;
	
	public TSCanvasManager(int w, int h) {
		setPreferredSize(new Dimension(w,h));
		
		m_popupMenuActionListener = new PopupMenuActionListener(this);
		
		addMouseListener(this);
		addMouseMotionListener(this);
	}
	
	@SuppressWarnings("unused")
	private void _debugOutputPairs() {
		for( int i = 0; i < m_linkBegComp.size(); i++ ) {
			String _dbg = m_linkBegComp.get(i) + "<->" + m_linkEndComp.get(i);
			System.out.println(_dbg);
		}
	}
	
	private void paintButtons(Graphics2D g2) {
		m_iconTopLeftPositions.clear();
		try {
			if( m_addIconImage == null )
				m_addIconImage = ImageIO.read(new File(m_addIconFile));
			if( m_delIconImage == null )
				m_delIconImage = ImageIO.read(new File(m_delIconFile));
			
			Dimension sz = getSize();
			int toolboxH = (int)(sz.height*m_topSplitRatio);
			int toolboxW = (int)(sz.width*m_leftSplitRatio);
			
			int x = (toolboxW-(m_iconSize*2+m_iconGap))/2;
			int y = (toolboxH-m_iconSize)/2;
			
			Graphics2D newg = (Graphics2D)(g2.create());
			newg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			newg.drawImage(m_addIconImage, x, y, m_iconSize, m_iconSize, null);
			newg.drawImage(m_delIconImage, x+m_iconSize+m_iconGap, y, m_iconSize, m_iconSize, null);
			m_iconTopLeftPositions.add(new Integer(x));
			m_iconTopLeftPositions.add(new Integer(x+m_iconSize+m_iconGap));
			newg.dispose();
			
		} catch(Exception exp) {
			exp.printStackTrace();
		}
	}
	
	private void paintCanvas(Graphics2D g2) {
		m_canvasTopLeftPositions.clear();
		Dimension sz = getSize();
		int canvasContainerWidth = (int)(sz.width*m_leftSplitRatio);
		int canvasContainerHeight = (int)(sz.height*(1-m_topSplitRatio));
		int numOfCanvas = m_canvasNames.size();
		int canvasHeight = m_defaultCanvasHeight;
		int canvasWidth = (int)(canvasContainerWidth*m_defaultCanvasWidthRatio);
		int canvasGap = m_defaultCanvasGap;
		if( numOfCanvas*(m_defaultCanvasHeight+m_defaultCanvasGap) > canvasContainerHeight ) {
			canvasHeight = (canvasContainerHeight/numOfCanvas)*4/5;
			canvasGap = (canvasContainerHeight/numOfCanvas)/5;
		}
		
		Graphics2D newg = (Graphics2D)(g2.create());
		for( int i = 0; i < m_canvasNames.size(); i++ ) {
			int x = (canvasContainerWidth-canvasWidth)/2;
			int y = (i+1)*canvasGap+i*canvasHeight;
			y = y + (int)(sz.height*m_topSplitRatio);
			if( i == m_selectedCanvasIndex )
				newg.setColor(Color.RED);
			else
				newg.setColor(Color.BLUE);
			newg.fillRect(x, y, canvasWidth, canvasHeight);
			
			newg.setFont(new Font(null, Font.BOLD, 12));
			newg.setColor(Color.BLACK);
			newg.drawString(m_canvasNames.get(i), x+canvasWidth/5, y+canvasHeight/2);
			
			m_canvasTopLeftPositions.add(new Integer(y));
		}
		newg.dispose();
	}
	
	private void paintFramewire(Graphics2D g2) {
		Dimension sz = getSize();
		g2.setColor(Color.WHITE);
		g2.fillRect(0, 0, sz.width, sz.height);
		
		Graphics2D newg = (Graphics2D)(g2.create());
		
		int horizontalY = (int)(sz.height*m_topSplitRatio);
		newg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
		newg.setColor(Color.GRAY);
		newg.fillRect(0, 0, sz.width, horizontalY);
		
		newg.dispose();
	}
	
	private void paintLinkLine(Graphics2D g2) {
		final int nearEnough = 3;
		Dimension sz = getSize();
		int offsetMax = (int)(sz.width*(1-m_leftSplitRatio)*0.5);
		
		Graphics2D newg = (Graphics2D)(g2.create());
		newg.setStroke(new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{3,1}, 0));
		newg.setColor(Color.GRAY);
		
		// paint the linked pairs
		int canvasContainerWidth = (int)(sz.width*m_leftSplitRatio);
		int canvasContainerHeight = (int)(sz.height*(1-m_topSplitRatio));
		int canvasX = (int)((canvasContainerWidth*(1-m_defaultCanvasWidthRatio))/2);
		int numOfCanvas = m_canvasNames.size();
		int canvasHeight = m_defaultCanvasHeight;
		if( numOfCanvas*(m_defaultCanvasHeight+m_defaultCanvasGap) > canvasContainerHeight ) {
			canvasHeight = (canvasContainerHeight/numOfCanvas)*4/5;
		}
		
		try {
			m_linkPoint = new Vector<Vector<Integer>>();
			for( int i = 0; i < m_linkBegComp.size(); i++ ) {
				String begComp = m_linkBegComp.get(i);
				String endComp = m_linkEndComp.get(i);
				int begIndex = m_canvasNames.indexOf(begComp);
				int endIndex = m_canvasNames.indexOf(endComp);
				int x1 = (int)(canvasX+canvasContainerWidth*m_defaultCanvasWidthRatio);
				int y1 = m_canvasTopLeftPositions.get(begIndex).intValue()+canvasHeight/2;
				int x2 = (int)(canvasX+canvasContainerWidth*m_defaultCanvasWidthRatio);
				int y2 = m_canvasTopLeftPositions.get(endIndex).intValue()+canvasHeight/2;
				
				Vector<Integer> p1p2 = new Vector<Integer>();
				p1p2.add(new Integer(x1));
				p1p2.add(new Integer(y1));
				p1p2.add(new Integer(x2));
				p1p2.add(new Integer(y2));
				m_linkPoint.add(p1p2);
				
				newg.drawLine(x1, y1, x1+offsetMax, y1);
				newg.drawLine(x1+offsetMax, y1, x1+offsetMax, y2);
				newg.drawLine(x1+offsetMax, y2, x2, y2);
			}
		} catch (Exception exp) {}
		
		
		if( m_bLinkStart == false ) {
			newg.dispose();
			return;
		}
		
		if( Math.abs(m_linkLineEndX-m_linkLineBegX)<nearEnough ) {
			newg.drawLine(m_linkLineBegX, m_linkLineBegY, m_linkLineBegX+offsetMax, m_linkLineBegY);
			newg.drawLine(m_linkLineBegX+offsetMax, m_linkLineBegY, m_linkLineBegX+offsetMax, m_linkLineEndY);
			newg.drawLine(m_linkLineBegX+offsetMax, m_linkLineEndY, m_linkLineEndX, m_linkLineEndY);
		} else {
			newg.drawLine(m_linkLineBegX, m_linkLineBegY, m_linkLineEndX, m_linkLineBegY);
			newg.drawLine(m_linkLineEndX, m_linkLineBegY, m_linkLineEndX, m_linkLineEndY);
		}
		
		newg.dispose();
	}
	
	public void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D)g;
		
		paintFramewire(g2);
		paintButtons(g2);
		paintCanvas(g2);
		paintLinkLine(g2);
	}
	
	public String addNewCanvas() {
		String canvasName = "Panel" + Integer.toString(m_canvasIndex++);
		m_canvasNames.add(canvasName);
		
		if( m_privateNotification != null ) {
			String content = "addPanel:" + canvasName;
			m_privateNotification.sendMessage("TSCanvasManager", "TSSignalViewTable", content);
			m_privateNotification.sendMessage("TSCanvasManager", "TSMain", content);
		}
		
		repaint();
		return canvasName;
	}
	
	// usually this is initialized by clicking the delete icon on TSCanvasManager
	public String removeCanvas() {
		if( m_selectedCanvasIndex == -1 )	return "";
		
		String canvasName = m_canvasNames.get(m_selectedCanvasIndex);
		try {
			// change the pair
			m_canvasNames.remove(m_selectedCanvasIndex);
			int index = m_linkBegComp.indexOf(canvasName);
			if( index != -1 ) {
				String frame1 = m_linkBegComp.remove(index);
				String frame2 = m_linkEndComp.remove(index);
				if( m_privateNotification != null ) {
					m_privateNotification.removeLinkedFrame(frame1, frame2);
				}
			}
			index = m_linkEndComp.indexOf(canvasName);
			if( index != -1 ) {
				String frame1 = m_linkBegComp.remove(index);
				String frame2 = m_linkEndComp.remove(index);
				if( m_privateNotification != null ) {
					m_privateNotification.removeLinkedFrame(frame1, frame2);
				}
			}
		} catch( Exception exp ) {}
		
		if( m_privateNotification != null ) {
			String content = "removePanel:" + canvasName;
			m_privateNotification.sendMessage("TSCanvasManager", "TSSignalViewTable", content);
			m_privateNotification.sendMessage("TSCanvasManager", "TSMain", content);
		}
		m_selectedCanvasIndex = -1;
		repaint();
		
//		_debugOutputPairs();
		
		return canvasName;
	}
	
	// usually this message is initialized from TSMain by closing the window directly
	public String removeCanvas(String canvasName) {
		if( m_canvasNames.remove(canvasName) ) {
			int index = m_linkBegComp.indexOf(canvasName);
			if( index != -1 ) {
				String frame1 = m_linkBegComp.remove(index);
				String frame2 = m_linkEndComp.remove(index);
				if( m_privateNotification != null ) {
					m_privateNotification.removeLinkedFrame(frame1, frame2);
				}
			}
			index = m_linkEndComp.indexOf(canvasName);
			if( index != -1 ) {
				String frame1 = m_linkBegComp.remove(index);
				String frame2 = m_linkEndComp.remove(index);
				if( m_privateNotification != null ) {
					m_privateNotification.removeLinkedFrame(frame1, frame2);
				}
			}
			if( m_privateNotification != null ) {
				String content = "removePanel:" + canvasName;
				m_privateNotification.sendMessage("TSCanvasManager", "TSSignalViewTable", content);
			}
			repaint();
		}
		return canvasName;
	}
	
	/**
	 * @param from: the message is sent from which module
	 * @param content: the content of the message
	 * 	(1) removePanel:panelName;
	 * */
	public void receiveMessage(String from, String content) {
		String cmd = content.substring(0, content.indexOf(":")).trim();
		String arg = content.substring(content.indexOf(":")+1).trim();
		if( cmd.equalsIgnoreCase("removePanel") ) {
			removeCanvas(arg);
		}
	}
	
	
	// pop up menu action listener, to delete the linked canvases
	private class PopupMenuActionListener implements ActionListener {
		public TSCanvasManager m_parent;
		public PopupMenuActionListener(TSCanvasManager m) {
			m_parent = m;
		}
		public void actionPerformed(ActionEvent evt) {
			JMenuItem item = (JMenuItem)(evt.getSource());
			String name = item.getText();
			try {
				String pair = name.substring(name.indexOf(":")+1).trim();
				String begComp = pair.substring(0, pair.indexOf("-"));
				String endComp = pair.substring(pair.indexOf("-")+1);
				if( m_linkBegComp.indexOf(begComp)!=m_linkEndComp.indexOf(endComp) ) {
					throw new IOException("the pair of begin component and end component doesn't match!");
				}
				m_linkBegComp.remove(begComp);
				m_linkEndComp.remove(endComp);
				if( m_privateNotification != null ) {
					m_privateNotification.removeLinkedFrame(begComp, endComp);
				}
				m_parent.repaint();
			}catch(Exception exp) {
				exp.printStackTrace();
			}
		}
	}
	
	public void mouseEntered(MouseEvent evt) {}
	public void mouseExited(MouseEvent evt) {}
	public void mousePressed(MouseEvent evt) {
		Dimension sz = getSize();
		int canvasContainerWidth = (int)(sz.width*m_leftSplitRatio);
		int canvasX = (int)((canvasContainerWidth*(1-m_defaultCanvasWidthRatio))/2);
		int canvasContainerHeight = (int)(sz.height*(1-m_topSplitRatio));
		int numOfCanvas = m_canvasNames.size();
		int canvasHeight = m_defaultCanvasHeight;
		if( numOfCanvas*(m_defaultCanvasHeight+m_defaultCanvasGap) > canvasContainerHeight ) {
			canvasHeight = (canvasContainerHeight/numOfCanvas)*4/5;
		}
		
		int x = evt.getX();
		int y = evt.getY();
		
		int canvasIndex = 0;
		for(; canvasIndex < m_canvasNames.size(); canvasIndex++) {
			if( x>canvasX+canvasContainerWidth*m_defaultCanvasWidthRatio-5 && 
				x<canvasX+canvasContainerWidth*m_defaultCanvasWidthRatio+5 && 
				y>m_canvasTopLeftPositions.get(canvasIndex).intValue() && 
				y<m_canvasTopLeftPositions.get(canvasIndex).intValue()+canvasHeight) {
				m_bLinkStart = true;
				m_linkBegComp.add(m_canvasNames.get(canvasIndex));
				m_linkLineBegX = x;
				m_linkLineBegY = y;
				return;
			}
		}
	}
	public void mouseClicked(MouseEvent evt) {
		Dimension sz = getSize();
		int canvasContainerWidth = (int)(sz.width*m_leftSplitRatio);
		int toolboxHeight = (int)(sz.height*m_topSplitRatio);
		int toolboxY = (toolboxHeight-m_iconSize)/2;
		int canvasX = (int)(canvasContainerWidth*(1-m_defaultCanvasWidthRatio)/2);
		
		int x = evt.getX();
		int y = evt.getY();
		
		if( evt.getButton() == MouseEvent.BUTTON1 ) {
			// toolbox
			int iconIndex = 0;
			for(; iconIndex < m_iconTopLeftPositions.size(); iconIndex++ ) {
				if( x>m_iconTopLeftPositions.get(iconIndex).intValue() && x<m_iconTopLeftPositions.get(iconIndex).intValue()+m_iconSize &&
					y>toolboxY && y<toolboxY+m_iconSize) {
					break;
				}
			}
			if( iconIndex < m_iconTopLeftPositions.size() ) {
				switch(iconIndex) {
				case 0:	// add
					addNewCanvas();
					break;
				case 1:	// delete
					removeCanvas();
					break;
				default:
					break;
				}
				return;
			}
			
			// canvas
			int canvasContainerHeight = (int)(sz.height*(1-m_topSplitRatio));
			int numOfCanvas = m_canvasNames.size();
			int canvasHeight = m_defaultCanvasHeight;
			if( numOfCanvas*(m_defaultCanvasHeight+m_defaultCanvasGap) > canvasContainerHeight ) {
				canvasHeight = (canvasContainerHeight/numOfCanvas)*4/5;
			}
			int canvasIndex = 0;
			for(; canvasIndex < m_canvasNames.size(); canvasIndex++ ) {
				if( x>canvasX && x<canvasX+canvasContainerWidth*m_defaultCanvasWidthRatio && 
					y>m_canvasTopLeftPositions.get(canvasIndex) && y<m_canvasTopLeftPositions.get(canvasIndex)+canvasHeight ) {
					break;
				}
			}
			if( canvasIndex < m_canvasNames.size() )
				m_selectedCanvasIndex = canvasIndex;
			else
				m_selectedCanvasIndex = -1;
			repaint();
		}
		else if( evt.getButton() == MouseEvent.BUTTON3 ) {
			final int threshold = 5;
			Vector<Integer> selectedDashLineIndex = new Vector<Integer>();
			for( int i = 0; i < m_linkPoint.size(); i++ ) {	// iterate through each dash line
				int x1 = m_linkPoint.get(i).get(0).intValue();
				int y1 = m_linkPoint.get(i).get(1).intValue();
				int x2 = m_linkPoint.get(i).get(2).intValue();
				int y2 = m_linkPoint.get(i).get(3).intValue();
				if( (Math.abs(x-x1)<threshold && Math.abs(y-y1)<threshold) || Math.abs(x-x2)<threshold && Math.abs(y-y2)<threshold ) {
					selectedDashLineIndex.add(new Integer(i));
				}
			}
			
			// show up the popup menu
			if( selectedDashLineIndex.isEmpty() )
				return;
		
			m_popupMenu = new JPopupMenu();
			for( int i = 0; i < selectedDashLineIndex.size(); i++ ) {
				String begComp = m_linkBegComp.get(i);
				String endComp = m_linkEndComp.get(i);
				/**
				 * format: Delete: begCompName-endCompName
				 * */
				JMenuItem pair = new JMenuItem("Delete: "+begComp+"-"+endComp);
				pair.addActionListener(m_popupMenuActionListener);
				m_popupMenu.add(pair);
			}
			m_popupMenu.show(this, x, y);
		}
	}
	public void mouseReleased(MouseEvent evt) {
		if( m_bLinkStart == false )
			return;
		Dimension sz = getSize();
		int canvasContainerWidth = (int)(sz.width*m_leftSplitRatio);
		int canvasX = (int)((canvasContainerWidth*(1-m_defaultCanvasWidthRatio))/2);
		int canvasContainerHeight = (int)(sz.height*(1-m_topSplitRatio));
		int numOfCanvas = m_canvasNames.size();
		int canvasHeight = m_defaultCanvasHeight;
		if( numOfCanvas*(m_defaultCanvasHeight+m_defaultCanvasGap) > canvasContainerHeight ) {
			canvasHeight = (canvasContainerHeight/numOfCanvas)*4/5;
		}
		
		int x = evt.getX();
		int y = evt.getY();
		
		int canvasIndex = 0;
		for(; canvasIndex < m_canvasNames.size(); canvasIndex++) {
			if( x>canvasX+canvasContainerWidth*m_defaultCanvasWidthRatio-5 && 
				x<canvasX+canvasContainerWidth*m_defaultCanvasWidthRatio+5 && 
				y>m_canvasTopLeftPositions.get(canvasIndex).intValue() && 
				y<m_canvasTopLeftPositions.get(canvasIndex).intValue()+canvasHeight) {
				
				if( m_linkBegComp.lastElement().equalsIgnoreCase(m_canvasNames.get(canvasIndex)) )
					break;
				m_linkEndComp.add(m_canvasNames.get(canvasIndex));
				if( m_privateNotification != null ) {
					String frame1 = m_linkBegComp.lastElement();
					String frame2 = m_linkEndComp.lastElement();
					m_privateNotification.addLinkedFrame(frame1, frame2);
				}
				
//				_debugOutputPairs();
				
				m_bLinkStart = false;
				m_linkLineBegX = m_linkLineBegY = m_linkLineEndX = m_linkLineEndY = 0;	// reset the dynamic variables
				repaint();
				return;
			}
		}
		
		m_linkBegComp.remove(m_linkBegComp.size()-1);
		m_bLinkStart = false;
		m_linkLineBegX = m_linkLineBegY = m_linkLineEndX = m_linkLineEndY = 0;	// reset the dynamic variables
		repaint();
		
	}
	public void mouseMoved(MouseEvent evt) {
		Dimension sz = getSize();
		int canvasContainerWidth = (int)(sz.width*m_leftSplitRatio);
		int toolboxHeight = (int)(sz.height*m_topSplitRatio);
		int toolboxY = (toolboxHeight-m_iconSize)/2;
		int canvasX = (int)(canvasContainerWidth*(1-m_defaultCanvasWidthRatio)/2);
		
		int x = evt.getX();
		int y = evt.getY();
		
		// toolbox
		for( int i = 0; i < m_iconTopLeftPositions.size(); i++ ) {
			if( x>m_iconTopLeftPositions.get(i).intValue() && x<m_iconTopLeftPositions.get(i).intValue()+m_iconSize &&
				y>toolboxY && y<toolboxY+m_iconSize) {
				setCursor( new Cursor(Cursor.HAND_CURSOR) );
				return;
			}
		}
		// canvas
		int canvasContainerHeight = (int)(sz.height*(1-m_topSplitRatio));
		int numOfCanvas = m_canvasNames.size();
		int canvasHeight = m_defaultCanvasHeight;
		if( numOfCanvas*(m_defaultCanvasHeight+m_defaultCanvasGap) > canvasContainerHeight ) {
			canvasHeight = (canvasContainerHeight/numOfCanvas)*4/5;
		}
		for( int i = 0; i < m_canvasNames.size(); i++ ) {
			if( x<canvasX+canvasContainerWidth*m_defaultCanvasWidthRatio+5 && 
					x>canvasX+canvasContainerWidth*m_defaultCanvasWidthRatio-5 &&
					y>m_canvasTopLeftPositions.get(i) && y<m_canvasTopLeftPositions.get(i)+canvasHeight) {
				setCursor( new Cursor(Cursor.CROSSHAIR_CURSOR) );
				return;
			}
			else if( x>canvasX && x<canvasX+canvasContainerWidth*m_defaultCanvasWidthRatio && y>m_canvasTopLeftPositions.get(i) && 
				y<m_canvasTopLeftPositions.get(i)+canvasHeight ) {
				setCursor( new Cursor(Cursor.HAND_CURSOR) );
				return;
			}
		}
		setCursor( new Cursor(Cursor.DEFAULT_CURSOR) );
	}
	public void mouseDragged(MouseEvent evt) {
		if( m_bLinkStart ) {
			m_linkLineEndX = evt.getX();
			m_linkLineEndY = evt.getY();
			repaint();
		}
	}
	
	// Unit testing
	private static void createAndShowUI() {
		TSCanvasManager m = new TSCanvasManager(100, 400);
		m.addNewCanvas();
		
		JFrame f = new JFrame("Canvas Container");
		f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		f.setSize(400, 400);
		f.add(m);
		f.setVisible(true);
	}
	public static void main(String[] args) {
		Runnable doCreateAndShowUI = new Runnable() {
			public void run() {
				createAndShowUI();
			}
		};
		SwingUtilities.invokeLater(doCreateAndShowUI);
	}
}
