/**
 * @author lixinghu@usc.edu
 * @since 2010/6/13
 * this is the place to put TSAbstractCanvas, TSCanvas and TSCanvasMenu together
 * */

package UI;

import java.awt.*;
import javax.swing.*;
import Data.DataManager;

public class TSCanvasComponent extends TSMsgPanel{
	private static final long serialVersionUID = -7842897987765719279L;

	private DataManager m_dataManager;
	
	private NotificationCenter m_notification;
	
	private long m_unitDuration = 1000;	// the duration of a single unit (milliseconds)
	private long m_startOfSignals = 0;	// the start time of signals
	private long m_visibleDuration = 10000;	// duration of the visible signals (how many samples the width of the canvas can hold)
	private int  m_samplerate = 16000;	// sample rate of signals
	
	// in the whole application, there is only one notification center, and we use name to distinguish different components.
	// now, the problem is that we allow multiple TSCanvasComponents to exist in the application. Previously, we use TSCanvas, TSCanvasMenu,
	// and TSAbstractCanvas to distinguish the three components within TSCanvasComponent, but since there are multiple TSCanvasComponents exist, 
	// this way doesn't work... because there are multiple TSCanvas, TSCanvasMenu, and TSAbstractCanvas
	// the simple solution would be add a prefix in front of the three names.
	private String m_parentFrameName = "";
	
	private TSCanvasMenu m_canvasMenu;
	private TSAbstractCanvas m_abstractCanvas;
	private TSCanvas m_canvas;
	
	private static final float m_leftSplitRatio = 0.04f;
	private static final float m_downSplitRatio = 0.3f;
	
	public TSCanvasComponent(DataManager dm, int w, int h) {
		m_notification = NotificationCenter.getInstance();
		
		m_dataManager = dm;
		setPreferredSize(new Dimension(w,h));
		
		initializeComponents(w,h);
		initializeLayout();
	}
	
	public TSCanvasComponent(DataManager dm, int w, int h, String parentName) {
		m_notification = NotificationCenter.getInstance();
		
		m_parentFrameName = parentName;
		m_dataManager = dm;
		setPreferredSize(new Dimension(w,h));
		
		initializeComponents(w,h);
		initializeLayout();
	}
	
	// parentName is the title of the frame which holds the TSCanvasComponent
	public TSCanvasComponent(DataManager dm, int w, int h, long unitDur, long startOfSignal, long visibleDur, int samplerate, String parentName) {
		m_notification = NotificationCenter.getInstance();
		
		m_parentFrameName = parentName;
		m_unitDuration = unitDur;
		m_startOfSignals = startOfSignal;
		m_visibleDuration = visibleDur;
		m_samplerate = samplerate;
		
		m_dataManager = dm;
		setPreferredSize(new Dimension(w,h));
		
		initializeComponents(w,h);
		initializeLayout();
	}
	
	private void initializeComponents(int w, int h) {
		// TSCanvasMenu
		m_canvasMenu = new TSCanvasMenu(m_dataManager, (int)(w*m_leftSplitRatio), h);
		m_canvasMenu.setSamplerate(m_samplerate).setStartTime(m_startOfSignals).setUnitDuration(m_unitDuration).setVisibleDuration(m_visibleDuration).setUnitForStartTime("ms").setUnitForUnitDuration("ms").setUnitForVisibleDuration("ms");
		m_canvasMenu.setParentName(m_parentFrameName);
		// TSAbstractCanvas
		m_abstractCanvas = new TSAbstractCanvas(m_dataManager, m_visibleDuration, m_samplerate, m_unitDuration, (int)(w*(1-m_leftSplitRatio)), (int)(h*m_downSplitRatio));
		m_abstractCanvas.setParentName(m_parentFrameName);
		// TSCanvas
		m_canvas = new TSCanvas(m_dataManager, m_visibleDuration, m_samplerate, m_unitDuration, m_startOfSignals, (int)(w*(1-m_leftSplitRatio)), (int)(h*(1-m_downSplitRatio)));
		m_canvas.setTimeStampDisplayFormat(TimeFormat.MILLISECOND);
		m_canvas.setParentName(m_parentFrameName);
		
		try {
			m_notification.registerComponent(m_parentFrameName+"TSCanvasMenu", m_canvasMenu);
			m_notification.registerComponent(m_parentFrameName+"TSAbstractCanvas", m_abstractCanvas);
			m_notification.registerComponent(m_parentFrameName+"TSCanvas", m_canvas);
		} catch(Exception exp) {
			exp.printStackTrace();
		}
	}
	
	private void initializeLayout() {
		GroupLayout layout = new GroupLayout(this);
		setLayout(layout);
		layout.setAutoCreateContainerGaps(true);
		layout.setAutoCreateGaps(true);
		
		layout.setHorizontalGroup(
			layout.createSequentialGroup()
				.addComponent(m_canvasMenu, 0, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
					.addComponent(m_canvas)
					.addComponent(m_abstractCanvas))
		);
		
		layout.setVerticalGroup(
			layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
				.addComponent(m_canvasMenu, 0, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
				.addGroup(layout.createSequentialGroup()
					.addComponent(m_canvas)
					.addComponent(m_abstractCanvas))
		);
	}
	
	public void addNewSignal(String source, String name) {
		Color c = m_canvasMenu.addNewSignal(source, name);
		m_abstractCanvas.addNewSignal(source, name, c);
		m_canvas.addNewSignal(source, name, c);
		revalidate();
	}
	
	public void removeSignal(String source, String name) {
		m_canvasMenu.removeSignal(source, name);
		m_abstractCanvas.removeSignal(source, name);
		m_canvas.removeSignal(source, name);
		revalidate();
	}
	
	/**
	 * addNewSignal: source_name feature_name,
	 * removeSignal: source_name feature_name,
	 * */
	public void receiveMessage(String from, String content) {
		String cmd = content.substring(0, content.indexOf(":")).trim();
		String arg = content.substring(content.indexOf(":")+1).trim();
		if( cmd.equalsIgnoreCase("addNewSignal") ) {
			String source = arg.substring(0, arg.indexOf(" ")).trim();
			String name = arg.substring(arg.indexOf(" ")+1).trim();
			addNewSignal(source, name);
		}
		else if( cmd.equalsIgnoreCase("removeSignal") ) {
			String source = arg.substring(0, arg.indexOf(" ")).trim();
			String name = arg.substring(arg.indexOf(" ")+1).trim();
			removeSignal(source, name);
		}
	}
	
	// Unit Testing
	private static void doCreateAndShowUI() {
		JFrame f = new JFrame("TSCanvas");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setSize(800, 200);
		
		DataManager dm = DataManager.getInstance();
		dm.loadDimensionsFromFile("test.oka");
//		dm.loadWaveFromFile("test.wav");
		
//		TSCanvasComponent c = new TSCanvasComponent(dm, 800, 200);
		TSCanvasComponent c = new TSCanvasComponent(dm, 800, 200, 1000, 0, 10000, 10, "Panel0");
		f.add(c);
		f.setVisible(true);
		
		c.addNewSignal("\\\\sfs\\data\\public\\1.avi", "smile");
		c.addNewSignal("\\\\sfs\\data\\public\\1.avi", "facex");
//		c.addNewSignal("/Users/lixinghu/Documents/workspace/HBVLTool/test.wav", "WavChannel1");
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
