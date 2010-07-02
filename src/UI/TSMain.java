/**
 * @author lixinghu@usc.edu
 * @since 2010/6/25
 * this is the entry point of the application
 * */

package UI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import UI.Widget.*;
import Data.DataManager;
import Util.RealtimeDataFeeder;
import Util.MessageRecvCallback;
import Util.MessageParser;

public class TSMain extends TSMsgPanel{
	private static final long serialVersionUID = 6271002025094739458L;
	
	private NotificationCenter m_notification = NotificationCenter.getInstance();	// message exchange center for the whole application
	private DataManager m_dataManager = DataManager.getInstance();
	
	public JMenuBar m_menuBar = new JMenuBar();
	private TSSignalViewController m_signalViewController;
	private Vector<JFrame> m_canvasFrame = new Vector<JFrame>();
	private Vector<TSCanvasComponent> m_canvas = new Vector<TSCanvasComponent>();
	
	// UI widget
	public FrameViewer m_frameViewer = new FrameViewer();
	
	// Realtime Observer
	public RealtimeDataFeeder m_feeder = null;
	
	/**
	 * menu action listener
	 * */
	private static final String m_menuOpenCSVFile = "Open CSV File";
	private static final String m_menuOpenCSVHead = "Open CSV Header";
	private static final String m_menuOpenCSVData = "Open CSV Data";
	private static final String m_menuNetworkSet  = "FrameViewer Network Setting";
	private static final String m_menuRealtimeNetworkSet = "Realtime Observer Setting";
	
	private MenuActionListener m_menuActionListener = new MenuActionListener(this);
	
	private String m_openedFilePath = "";
	
	private class FrameViewerNetworkSettingDialog extends JPanel {
		private static final long serialVersionUID = -9130562445504920717L;
		private JTextField m_portField = new JTextField(5);
		private JTextField m_ipAddrField = new JTextField(20);
		private short m_port;
		private String m_ip;
		private TSMain m_parent = null;
		private JFrame m_containerFrame = null;
		
		private class ButtonActionListener implements ActionListener {
			public void actionPerformed(ActionEvent evt) {
				try {
					m_ip = m_ipAddrField.getText();
					m_port = Short.parseShort(m_portField.getText().trim());
					m_parent.m_frameViewer.setRemoteAddress(m_ip, m_port);
					if( m_containerFrame != null ) {
						m_containerFrame.dispose();
					}
				} catch(Exception exp) {
					exp.printStackTrace();
				}
			}
		}
		
		public void setFrame(JFrame f) { m_containerFrame = f; }
		
		public FrameViewerNetworkSettingDialog(TSMain parent, String ip, short port) {
			m_port = port;
			m_ip   = ip;
			m_parent = parent;
			
			JLabel portLabel = new JLabel("Port");
			m_portField.setText(Short.toString(m_port));
			JLabel ipAddrLabel = new JLabel("IP Addr");
			m_ipAddrField.setText(m_ip);
			JButton confirmBtn = new JButton("OK");
			confirmBtn.addActionListener( new ButtonActionListener() );
			
			GroupLayout layout = new GroupLayout(this);
			setLayout(layout);
			
			layout.setAutoCreateContainerGaps(true);
			layout.setAutoCreateGaps(true);
			layout.setHorizontalGroup(
				layout.createSequentialGroup()
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
						.addComponent(portLabel, 0, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
						.addComponent(ipAddrLabel, 0, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
						.addComponent(confirmBtn, 0, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
						.addComponent(m_portField, 0, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
						.addComponent(m_ipAddrField, 0, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
			);
			layout.setVerticalGroup(
				layout.createSequentialGroup()
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(portLabel, 0, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
						.addComponent(m_portField, 0, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(ipAddrLabel, 0, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
						.addComponent(m_ipAddrField, 0, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
					.addComponent(confirmBtn, 0, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
			);
		}
	}
	
	private String m_openedConfigFilePath = "";
	private int m_defaultPort = 17744;
	private class RealtimeObserverSettingDialog extends JPanel {
		private static final long serialVersionUID = 6070577580289459822L;
		private JTextField m_portField = new JTextField(10);
		private JTextField m_configFileField = new JTextField(10);
		
		class ButtonActionListener implements ActionListener {
			public void actionPerformed(ActionEvent evt) {
				String cmd = ((JButton)evt.getSource()).getText();
				if( cmd.equalsIgnoreCase("Open Config") ) {
					JFileChooser fc = new JFileChooser();
					if( m_openedConfigFilePath.isEmpty() == false )
						fc.setCurrentDirectory(new File(m_openedConfigFilePath));
					int returnVal = fc.showOpenDialog(null);
					if( returnVal == JFileChooser.APPROVE_OPTION ) {
						m_configFileField.setText( fc.getSelectedFile().getAbsolutePath() );
					}
				}
				else if( cmd.equalsIgnoreCase("OK") ) {
					m_openedConfigFilePath = m_configFileField.getText().trim();
					m_defaultPort = Integer.parseInt( m_portField.getText().trim() );
					m_dataManager.loadRealtimeConfigFromFile(m_openedConfigFilePath);
					m_feeder = new RealtimeDataFeeder();
					m_feeder.setPort((short)m_defaultPort);
					
					// initialize the parser and callback
					MessageParser p = new MessageParser(m_dataManager);
					MessageRecvCallback cb = new MessageRecvCallback(m_canvas);
					p.setCallback(cb);
					m_feeder.setParser(p);
				}
				else if( cmd.equalsIgnoreCase("Start") ) {
					m_feeder.start();
				}
				else if( cmd.equalsIgnoreCase("Stop") ) {
					m_feeder.stop();
				}
			}
		}
		
		public RealtimeObserverSettingDialog() {
			JLabel portLabel = new JLabel("Listening on Port");
			JLabel configLabel = new JLabel("Config file");
			JButton openConfigBtn = new JButton("Open Config");
			openConfigBtn.addActionListener( new ButtonActionListener() );
			JButton confirmBtn = new JButton("OK");
			confirmBtn.addActionListener( new ButtonActionListener() );
			JButton startBtn = new JButton("Start");
			startBtn.addActionListener( new ButtonActionListener() );
			JButton stopBtn = new JButton("Stop");
			stopBtn.addActionListener( new ButtonActionListener() );
			
			m_portField.setText(Integer.toString(m_defaultPort));
			
			GroupLayout layout = new GroupLayout(this);
			setLayout(layout);
			layout.setAutoCreateContainerGaps(true);
			layout.setAutoCreateGaps(true);
			
			layout.setHorizontalGroup(
				layout.createSequentialGroup()
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
						.addComponent(portLabel)
						.addComponent(configLabel)
						.addComponent(confirmBtn))
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
						.addComponent(m_portField)
						.addComponent(m_configFileField)
						.addComponent(startBtn))
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
						.addComponent(openConfigBtn)
						.addComponent(stopBtn))
					
			);
			layout.setVerticalGroup(
				layout.createSequentialGroup()
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(portLabel)
						.addComponent(m_portField))
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(configLabel)
						.addComponent(m_configFileField)
						.addComponent(openConfigBtn))
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(confirmBtn)
						.addComponent(startBtn)
						.addComponent(stopBtn))
			);
		}
	}
	
	private class MenuActionListener implements ActionListener {
		private TSMain m_parent = null;
		public MenuActionListener(TSMain p) {
			m_parent = p;
		}
		public void actionPerformed(ActionEvent evt) {
			try {
				JMenuItem item = (JMenuItem)(evt.getSource());
				String name = item.getText();
				if( name.equalsIgnoreCase(m_menuOpenCSVFile) ) {	// open csv file
					JFileChooser fc = new JFileChooser();
					if( m_openedFilePath.isEmpty() == false )
						fc.setCurrentDirectory(new File(m_openedFilePath));
					
					int returnVal = fc.showOpenDialog(null);
					if( returnVal == JFileChooser.APPROVE_OPTION ) {
						m_openedFilePath = fc.getSelectedFile().getAbsolutePath();
					} else {
						return;
					}
					
					m_dataManager.loadDimensionsFromFile(m_openedFilePath);
					
					Vector<String> lastInsertedSources = new Vector<String>();
					Vector<String> lastInsertedNames = new Vector<String>();
					m_dataManager.getLastInsertDimensions(lastInsertedSources, lastInsertedNames);
					m_signalViewController.addNewSignals(lastInsertedSources, lastInsertedNames);
				}
				else if( name.equalsIgnoreCase(m_menuOpenCSVHead) ) {
					
				}
				else if( name.equalsIgnoreCase(m_menuOpenCSVData) ) {
					
				}
				else if( name.equalsIgnoreCase(m_menuNetworkSet) ) {
					short port = m_frameViewer.getPort();
					String ip  = m_frameViewer.getIP();
					FrameViewerNetworkSettingDialog net = new FrameViewerNetworkSettingDialog(m_parent, ip, port);
					
					JFrame f = new JFrame("Network Setting");
					net.setFrame(f);
					f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
					f.setSize(400, 200);
					f.add(net);
					f.pack();
					f.setVisible(true);
				}
				else if( name.equalsIgnoreCase(m_menuRealtimeNetworkSet) ) {
					JFrame f = new JFrame("Realtime Observer Setting");
					RealtimeObserverSettingDialog dlg = new RealtimeObserverSettingDialog();
					f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
					f.setSize(400, 200);
					f.add(dlg);
					f.pack();
					f.setVisible(true);
				}
			} catch(Exception exp) {
				exp.printStackTrace();
			}
		}
	}
	
	public TSMain(int w, int h) {
		// construct the menu bar
		JMenu openMenu = new JMenu("Open");
		JMenu csvMenu = new JMenu("CSV");
		JMenuItem openCSVFile = new JMenuItem(m_menuOpenCSVFile);
		openCSVFile.addActionListener(m_menuActionListener);
		JMenuItem openCSVHeader = new JMenuItem(m_menuOpenCSVHead);
		openCSVHeader.addActionListener(m_menuActionListener);
		JMenuItem openCSVData = new JMenuItem(m_menuOpenCSVData);
		openCSVData.addActionListener(m_menuActionListener);
		
		JMenu settingMenu = new JMenu("Setting");
		JMenuItem networkSettingMenu = new JMenuItem("FrameViewer Network Setting");
		JMenuItem realtimeFeedSetting = new JMenuItem("Realtime Observer Setting");
		networkSettingMenu.addActionListener(m_menuActionListener);
		realtimeFeedSetting.addActionListener(m_menuActionListener);
		settingMenu.add(networkSettingMenu);
		settingMenu.add(realtimeFeedSetting);
		
		openMenu.add(csvMenu);
		csvMenu.add(openCSVFile);
		csvMenu.add(openCSVHeader);
		csvMenu.add(openCSVData);
		m_menuBar.add(openMenu);
		m_menuBar.add(settingMenu);
		
		// initialize components
		m_signalViewController = new TSSignalViewController(w, h);
		
		// register
		try {
			m_notification.registerComponent("TSSignalViewController", m_signalViewController);
			m_notification.registerComponent("TSMain", this);
			m_notification.registerComponent("FrameViewer", m_frameViewer);
		} catch(Exception exp) {
			exp.printStackTrace();
		}
		
		add(m_signalViewController);
	}
	
	// for closing the canvas frame
	private class WindowListener extends WindowAdapter {
		public String m_title;	// title is the name of the panel being closed
		public WindowListener(String title) {
			m_title = title;
		}
		public void windowClosing(WindowEvent evt) {
			int frameIndex = 0;
			for(; frameIndex < m_canvasFrame.size(); frameIndex++) {
				if( m_canvasFrame.get(frameIndex).getTitle().equalsIgnoreCase(m_title) ) {
					break;
				}
			}
			if( frameIndex < m_canvasFrame.size() ) {
				m_canvasFrame.remove(frameIndex);
				m_canvas.remove(frameIndex);
				if( m_notification != null ) {
					m_notification.removeComponent(m_title);	// remove the current canvas component out of the notification center
				}
			
				String content = "removePanel:" + m_title;
				m_notification.sendMessage("TSMain", "TSCanvasManager", content);
			}
		}
	}
	
	/**
	 * @param from: the message is sent from which module
	 * @param content: the content of the message
	 * 	(1)addPanel:PanelName,
	 * */
	public void receiveMessage(String from, String content) {
		String cmd = content.substring(0, content.indexOf(":")).trim();
		String arg = content.substring(content.indexOf(":")+1).trim();
		if( cmd.equalsIgnoreCase("addPanel") ) {
			try {
				JFrame f = new JFrame(arg);
				f.addWindowListener( new WindowListener(arg) );
				
				TSCanvasComponent canvas = new TSCanvasComponent(m_dataManager, 800, 200, 1000, 0, 10000, 10, arg);
				f.setPreferredSize(new Dimension(800, 200));
				f.add(canvas);
				f.pack();
				f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				f.setVisible(true);
				
				m_notification.registerComponent(arg, canvas);	// add the current canvas component into the notification center
				
				m_canvasFrame.add(f);
				m_canvas.add(canvas);
			} catch(Exception exp) {
				exp.printStackTrace();
			}
		}
		else if( cmd.equalsIgnoreCase("removePanel") ) {
			int frameIndex = 0;
			for(; frameIndex < m_canvasFrame.size(); frameIndex++) {
				String frameTitle = m_canvasFrame.get(frameIndex).getTitle();
				if( frameTitle.equalsIgnoreCase(arg) ) {
					break;
				}
			}
			if( frameIndex < m_canvasFrame.size() ) {
				m_canvasFrame.get(frameIndex).dispose();
			}
		}
	}
	
	public static void main(String[] args) {
		JFrame f = new JFrame("HBVLTool");
		f.setPreferredSize(new Dimension(600, 400));
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		TSMain m = new TSMain(600, 400);
		f.add(m);
		f.setJMenuBar(m.m_menuBar);
		f.pack();
		f.setVisible(true);
	}
}
