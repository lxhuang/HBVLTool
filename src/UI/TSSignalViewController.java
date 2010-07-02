/**
 * @author lixinghu@usc.edu
 * @since 2010/6/26
 * integrate TSSignalViewTable and TSCanvasManager together. you could create a new canvas, and decide which signal should be 
 * displayed in which canvas
 * */

package UI;

import javax.swing.*;
import java.util.*;
import java.awt.*;

public class TSSignalViewController extends TSMsgPanel{
	private static final long serialVersionUID = -5051637919756792100L;
	private static final float m_leftSplitRatio = 0.8f;
	
	private TSSignalViewTable m_signalViewTable;
	private TSCanvasManager m_canvasManager;
	
	private NotificationCenter m_notification;
	
	public Vector<String> m_signalNames = new Vector<String>();
	public Vector<String> m_signalSources = new Vector<String>();
	
	public TSSignalViewController(int w, int h) {
		
		// initialize components
		m_signalViewTable = new TSSignalViewTable((int)(w*m_leftSplitRatio), h);
		m_canvasManager = new TSCanvasManager((int)(w*(1-m_leftSplitRatio)), h);
		
		m_notification = NotificationCenter.getInstance();
		
		try {
			m_notification.registerComponent("TSSignalViewTable", m_signalViewTable);
			m_notification.registerComponent("TSCanvasManager", m_canvasManager);
		} catch(Exception exp) {
			exp.printStackTrace();
		}
		
		setLayout(new BorderLayout());
		add(m_signalViewTable, BorderLayout.CENTER);
		add(m_canvasManager, BorderLayout.EAST);
		
		// initialize layout
//		GroupLayout layout = new GroupLayout(this);
//		setLayout(layout);		
//		layout.setHorizontalGroup(
//			layout.createSequentialGroup()
//				.addComponent(m_signalViewTable, 0, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
//				.addComponent(m_canvasManager, 0, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
//		);
//		layout.setVerticalGroup(
//			layout.createParallelGroup()
//				.addComponent(m_signalViewTable, 0, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
//				.addComponent(m_canvasManager, 0, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
//		);
	}
	
	public void addNewSignals(Vector<String> sources, Vector<String> names) {
		for( int i = 0; i < sources.size(); i++ ) {
			m_signalViewTable.addNewSignal(sources.get(i), names.get(i));
		}
	}
	
	// Unit testing
	private static void doCreateAndShowUI() {
		JFrame f = new JFrame("Signal View Manager");
		f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		f.setSize(600, 400);
		
		Vector<String> sources = new Vector<String>();
		sources.add("\\\\sfs\\data\\public\\1.avi");
		sources.add("\\\\sfs\\data\\public\\1.avi");
		Vector<String> names = new Vector<String>();
		names.add("smile");
		names.add("facex");
		
		TSSignalViewController ctrl = new TSSignalViewController(600, 400);
		ctrl.addNewSignals(sources, names);
		f.add(ctrl);
		f.pack();
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
