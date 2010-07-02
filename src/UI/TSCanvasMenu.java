/**
 * @author lixinghu@usc.edu
 * @since 2010/6/12
 * this is the controller bar for TSCanvas.
 * display the meta info of signals which are visible in the canvas;
 * let user change the colors for signals
 * let user change the sample rate etc.
 * */

package UI;

import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import Data.DataManager;
import Data.DimensionData;

public class TSCanvasMenu extends TSMsgComponent implements MouseListener, MouseMotionListener{
	private static final long serialVersionUID = -7404376970851819989L;
	private static final String m_menuItemSaveLabel = "Save Label";
	private static final String m_menuItemSaveAsLabel = "Save Label As";
	private static final String m_menuItemLoadLabel = "Load Label";
	private static final String m_menuItemCloseLabel = "Close Label";
	private static final String m_menuItemDisplaySetting = "Display Setting";
	
	public Vector<String> m_signalSources = new Vector<String>();
	public Vector<String> m_signalNames = new Vector<String>();
	public Vector<Color> m_signalColors = new Vector<Color>();
	
	public DataManager m_dataManager;
	public String m_parentName = "";
	
	private static final Color[] m_palette = {Color.BLACK, Color.BLUE, Color.CYAN, Color.DARK_GRAY, Color.GRAY, Color.GREEN, 
			Color.LIGHT_GRAY, Color.MAGENTA, Color.ORANGE, Color.PINK, Color.RED, Color.YELLOW};
	private Vector<Color> m_usedColors = new Vector<Color>();
	private static final int m_fixedRowNumber = 6;

	private String m_unitForStartTime = "ms", m_unitForUnitDuration = "ms", m_unitForVisibleDuration = "ms";
	private long m_startTime = 0, m_unitDuration = 1000, m_visibleDuration = 1000;
	private int m_samplerate = 10;
	
	private JPopupMenu m_popupMenu;
	
	public TSCanvasMenu setSamplerate(int sr) { m_samplerate = sr; return this; }
	public TSCanvasMenu setStartTime(long t) { m_startTime = t; return this; }
	public TSCanvasMenu setUnitDuration(long d) { m_unitDuration = d; return this; }
	public TSCanvasMenu setVisibleDuration(long d) { m_visibleDuration = d; return this; }
	public TSCanvasMenu setUnitForStartTime(String u) { m_unitForStartTime = u; return this; }
	public TSCanvasMenu setUnitForUnitDuration(String u) { m_unitForUnitDuration = u; return this; }
	public TSCanvasMenu setUnitForVisibleDuration(String u) { m_unitForVisibleDuration = u; return this; }
	
	public class SignalColorPalette extends JPanel {
		private static final long serialVersionUID = -6800746635772546033L;
		private final Color[] m_palette = {Color.BLACK, Color.BLUE, Color.CYAN, Color.DARK_GRAY, Color.GRAY, Color.GREEN, 
			Color.LIGHT_GRAY, Color.MAGENTA, Color.ORANGE, Color.PINK, Color.RED, Color.YELLOW};
		
		public TSCanvasMenu m_initializer;
		public int m_signalIndex;
		
		public class ColorButton extends JButton {
			private static final long serialVersionUID = -6198621573934204455L;
			public Color m_color;
			public ColorButton(String str, Color c, int w, int h) {
				super(str);
				setOpaque(false);
				setPreferredSize(new Dimension(w,h));
				m_color = c;
			}
			public void paintComponent(Graphics g) {
				Dimension sz = getSize();
				g.setColor(m_color);
				g.fillRect(0, 0, sz.width, sz.height);
			}
		}
		public SignalColorPalette(TSCanvasMenu m, int index, int w, int h, int rows, int cols) {
			m_initializer = m;
			m_signalIndex = index;
			setPreferredSize(new Dimension(w,h));
			for( int i = 0; i < rows; i++ ) {
				for( int j = 0; j < cols; j++ ) {
					ColorButton b = new ColorButton("", m_palette[i*cols+j], w/cols, h/rows);
					b.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							ColorButton bb = (ColorButton)e.getSource();
							m_initializer.m_signalColors.set(m_signalIndex, bb.m_color);
							m_initializer.changeSignalColor(
								m_initializer.m_signalNames.get(m_signalIndex), 
								m_initializer.m_signalSources.get(m_signalIndex), 
								bb.m_color
							);
							m_initializer.repaint();
						}
					});
					add(b);
				}
			}
		}
	}
	
	public class SignalDisplaySetting extends JPanel {
		private static final long serialVersionUID = 7048932934653690379L;
		private TSCanvasMenu m_controller;
		private JTextField m_startTimeTextField, m_unitDurationTextField, m_samplerateTextField, m_visibleDurationTextField;
		public SignalDisplaySetting(TSCanvasMenu m, int w, int h) {
			m_controller = m;
			setPreferredSize(new Dimension(w,h));
			
			JLabel startOfSignalLabel = new JLabel("Start Time of Signals:");
			m_startTimeTextField = new JTextField(5);
			m_startTimeTextField.setText(Long.toString(convertMillisecondsTo(m_startTime, m_unitForStartTime)));
			String[] args = {"ms", "s", "m", "h", "d"};
			JComboBox unitList1 = new JComboBox(args);
			int selectedIndex = 0;
			for(; selectedIndex < args.length; selectedIndex++) {
				if( args[selectedIndex].equalsIgnoreCase(m_unitForStartTime) )
					break;
			}
			unitList1.setSelectedIndex(selectedIndex);
			unitList1.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					JComboBox b = (JComboBox)(e.getSource());
					m_unitForStartTime = (String)b.getSelectedItem();
				}
			});
			
			JLabel samplerateLabel = new JLabel("Sample rate:");
			m_samplerateTextField = new JTextField(10);
			m_samplerateTextField.setText(Integer.toString(m_samplerate));
			
			JLabel unitLabel = new JLabel("Unit duration:");
			m_unitDurationTextField = new JTextField(5);
			m_unitDurationTextField.setText(Long.toString(convertMillisecondsTo(m_unitDuration, m_unitForUnitDuration)));
			JComboBox unitList2 = new JComboBox(args);
			selectedIndex = 0;
			for(; selectedIndex < args.length; selectedIndex++) {
				if( args[selectedIndex].equalsIgnoreCase(m_unitForUnitDuration) )
					break;
			}
			unitList2.setSelectedIndex(selectedIndex);
			unitList2.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					JComboBox b = (JComboBox)(e.getSource());
					m_unitForUnitDuration = (String)b.getSelectedItem();
				}
			});
			
			JLabel visibleDurationLabel = new JLabel("Visible Duration");
			m_visibleDurationTextField = new JTextField(10);
			m_visibleDurationTextField.setText(Long.toString(convertMillisecondsTo(m_visibleDuration, m_unitForVisibleDuration)));
			JComboBox unitList3 = new JComboBox(args);
			selectedIndex = 0;
			for( ; selectedIndex < args.length; selectedIndex++ ) {
				if( args[selectedIndex].equalsIgnoreCase(m_unitForVisibleDuration) )
					break;
			}
			unitList3.setSelectedIndex(selectedIndex);
			unitList3.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					JComboBox b = (JComboBox)(e.getSource());
					m_unitForVisibleDuration = (String)b.getSelectedItem();
				}
			});
			
			JButton confirmButton = new JButton("OK");
			confirmButton.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					try {
						int startTime = Integer.parseInt(m_startTimeTextField.getText().trim());
						int unitDuration = Integer.parseInt(m_unitDurationTextField.getText().trim());
						int samplerate = Integer.parseInt(m_samplerateTextField.getText().trim());
						int visibleDuration = Integer.parseInt(m_visibleDurationTextField.getText().trim());
						m_controller.changeUnitForUnitDurationTo(m_unitForUnitDuration);
						m_controller.changeUnitForStartTimeTo(m_unitForStartTime);
						m_controller.changeUnitForVisibleDurationTo(m_unitForVisibleDuration);
						m_controller.changeSamplerateTo(samplerate);
						m_controller.changeStartTimeTo(convertToMilliseconds(startTime, m_unitForStartTime));
						m_controller.changeUnitDurationTo(convertToMilliseconds(unitDuration, m_unitForUnitDuration));
						m_controller.changeVisibleDurationTo(convertToMilliseconds(visibleDuration, m_unitForVisibleDuration));
					} catch(Exception exp) {
						exp.printStackTrace();
					}
				}
			});
			
			GroupLayout layout = new GroupLayout(this);
			setLayout(layout);
			layout.setAutoCreateContainerGaps(true);
			layout.setAutoCreateGaps(true);
			
			layout.setHorizontalGroup(
				layout.createSequentialGroup()
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
						.addComponent(startOfSignalLabel)
						.addComponent(samplerateLabel)
						.addComponent(visibleDurationLabel)
						.addComponent(unitLabel)
						.addComponent(confirmButton))
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
						.addComponent(m_startTimeTextField)
						.addComponent(m_samplerateTextField)
						.addComponent(m_visibleDurationTextField)
						.addComponent(m_unitDurationTextField))
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
						.addComponent(unitList1)
						.addComponent(unitList3)
						.addComponent(unitList2))
			);
			
			layout.setVerticalGroup(
				layout.createSequentialGroup()
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(startOfSignalLabel)
						.addComponent(m_startTimeTextField)
						.addComponent(unitList1))
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(samplerateLabel)
						.addComponent(m_samplerateTextField))
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(visibleDurationLabel)
						.addComponent(m_visibleDurationTextField)
						.addComponent(unitList3))
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(unitLabel)
						.addComponent(m_unitDurationTextField)
						.addComponent(unitList2))
					.addComponent(confirmButton)
			);
		}
		private long convertToMilliseconds(int t, String unit) {
			if( unit.equalsIgnoreCase("ms") )
				return t;
			else if( unit.equalsIgnoreCase("s") )
				return (long)t*1000;
			else if( unit.equalsIgnoreCase("m") )
				return (long)t*1000*60;
			else if( unit.equalsIgnoreCase("h") )
				return (long)t*1000*60*60;
			else
				return (long)t*1000*60*60*24;
		}
		private long convertMillisecondsTo(long t, String unit) {
			if( unit.equalsIgnoreCase("ms") )
				return t;
			else if( unit.equalsIgnoreCase("s") )
				return (long)((double)t/1000);
			else if( unit.equalsIgnoreCase("m") )
				return (long)((double)t/1000/60);
			else if( unit.equalsIgnoreCase("h") )
				return (long)((double)t/1000/60/60);
			else
				return (long)((double)t/1000/60/60/24);
		}
	}
	
	public void paintComponent(Graphics g) {
		Dimension sz = getSize();
		int totalSignalNum = m_signalColors.size();
		int colNumber = (int)Math.ceil((double)totalSignalNum / m_fixedRowNumber);
		
		Graphics2D g2 = (Graphics2D)(g.create());
		
		// draw signal signs
		for( int i = 0; i < totalSignalNum; i++ ) {
			int col = (int)(Math.floor((double)i/m_fixedRowNumber));
			int row = i%m_fixedRowNumber;
			int x = (int)((double)sz.width/colNumber*col);
			int y = (int)((double)sz.height/m_fixedRowNumber*row);
			g2.setColor(m_signalColors.get(i));
			
			int l = (sz.width/colNumber<sz.height/m_fixedRowNumber) ? sz.width/colNumber : sz.height/m_fixedRowNumber;
			//g2.fillRoundRect(x, y, l, l, l/4, l/4);
			g2.fillRect(x, y, l, l);
		}
	}
	
	private String showFileChooserDialog(boolean bSaveDialog, String defaultOpenLocation, JComponent parent) {
		String selectedFilename = "";
		
		JFileChooser fc = new JFileChooser();
		if( defaultOpenLocation.isEmpty() == false )
			fc.setCurrentDirectory(new File(defaultOpenLocation));
		
		if( bSaveDialog ) {
			int returnVal = fc.showSaveDialog(parent);
			switch(returnVal) {
			case JFileChooser.APPROVE_OPTION:
				File f = fc.getSelectedFile();
				if( f.exists() ) {
					int res = JOptionPane.showConfirmDialog(fc, "Would you like to replace "+f.getAbsolutePath()+"?", "Confirm", JOptionPane.YES_NO_OPTION);
					if( res == JOptionPane.YES_OPTION )
						selectedFilename = f.getAbsolutePath();
				}
				else {
					selectedFilename = f.getAbsolutePath();
				}
				break;
			case JFileChooser.CANCEL_OPTION:
				break;
			default:
				break;
			}
		}
		else {
			int returnVal = fc.showOpenDialog(parent);
			if( returnVal == JFileChooser.APPROVE_OPTION ) {
				selectedFilename = fc.getSelectedFile().getAbsolutePath();
			}
		}
		return selectedFilename;
	}
	
	private class PopupMenuActionListener implements ActionListener {
		private TSCanvasMenu m_canvasMenu;
		private String m_saveFilename = "";
		public PopupMenuActionListener(TSCanvasMenu m) {
			m_canvasMenu = m;
		}
		public void actionPerformed(ActionEvent evt) {
			JMenuItem item = (JMenuItem)evt.getSource();
			String name = item.getText();
			if( name.equalsIgnoreCase(m_menuItemDisplaySetting) ) {
				SignalDisplaySetting s = new SignalDisplaySetting(m_canvasMenu, 400, 200);
				JFrame f = new JFrame("Setting");
				f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				f.setSize(400, 200);
				f.add(s);
				f.setVisible(true);
			}
			else if( name.equalsIgnoreCase(m_menuItemSaveLabel) ) {	// saveLabelToFile:filename
				if( m_saveFilename.isEmpty() ) {
					try {
						String selectedFilename = showFileChooserDialog(true, m_saveFilename, m_canvasMenu);
						if( selectedFilename.isEmpty() )
							return;
						m_saveFilename = selectedFilename;
					
						String content = "saveLabelToFile:"+m_saveFilename;
						m_privateNotification.sendMessage(m_parentName+"TSCanvasMenu", m_parentName+"TSCanvas", content);
					} catch(Exception exp) {
						exp.printStackTrace();
					}
				}
				else {
					String content = "saveLabelToFile:"+m_saveFilename;
					m_privateNotification.sendMessage(m_parentName+"TSCanvasMenu", m_parentName+"TSCanvas", content);
				}
			}
			else if( name.equalsIgnoreCase(m_menuItemSaveAsLabel) ) {	// saveLabelToFile:filename
				try {
					String selectedFilename = showFileChooserDialog(true, m_saveFilename, m_canvasMenu);
					if( selectedFilename.isEmpty() )
						return;
					m_saveFilename = selectedFilename;
				
					String content = "saveLabelToFile:"+m_saveFilename;
					m_privateNotification.sendMessage(m_parentName+"TSCanvasMenu", m_parentName+"TSCanvas", content);
				} catch(Exception exp) {
					exp.printStackTrace();
				}
			}
			else if( name.equalsIgnoreCase(m_menuItemLoadLabel) ) {	// loadLabelFromFile:filename
				int i = 0;
				for( ; i < m_signalNames.size(); i++ ) {
					DimensionData d = m_dataManager.getDimensionBySourceAndName(m_signalSources.get(i), m_signalNames.get(i));
					if( d.m_labels.isEmpty() == false )
						break;
				}
				if( i < m_signalNames.size() ) {
					String[] options = {"Close", "Save and Close", "Keep"};
					int resultVal = JOptionPane.showOptionDialog(m_canvasMenu, "What would you like to do with the current labels?",
							"Confirm", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
					if( resultVal == JOptionPane.YES_OPTION ) {	// Close
						//System.out.println("Yes");
						String content = "closeLabel:";
						m_privateNotification.sendMessage(m_parentName+"TSCanvasMenu", m_parentName+"TSCanvas", content);
					}
					else if( resultVal == JOptionPane.NO_OPTION ) {	// Save and close
						//System.out.println("No");
						String selectedFilename = showFileChooserDialog(true, m_saveFilename, m_canvasMenu);
						String content = "closeLabel:"+selectedFilename;
						m_saveFilename = "";
						m_privateNotification.sendMessage(m_parentName+"TSCanvasMenu", m_parentName+"TSCanvas", content);
					}
					else if( resultVal == JOptionPane.CANCEL_OPTION ) {	// Keep
						//System.out.println("Cancel");
					}
				}
				
				String selectedFilename = showFileChooserDialog(false, m_saveFilename, m_canvasMenu);
				if( selectedFilename.isEmpty() )
					return;
				
				m_saveFilename = selectedFilename;
				String content = "loadLabelFromFile:"+m_saveFilename;
				m_privateNotification.sendMessage(m_parentName+"TSCanvasMenu", m_parentName+"TSCanvas", content);
			}
			else if( name.equalsIgnoreCase(m_menuItemCloseLabel) ) {	// closeLabel: toSaveFilename(optional)
				int res = JOptionPane.showConfirmDialog(m_canvasMenu, "Would you like to save your current labels?", "Confirm", JOptionPane.YES_NO_OPTION);
				if( res == JOptionPane.YES_OPTION ) {
					if( m_saveFilename.isEmpty() ) {
						String selectedFilename = showFileChooserDialog(true, m_saveFilename, m_canvasMenu);
						if( selectedFilename.isEmpty() )
							return;
						m_saveFilename = selectedFilename;
					}
					String content = "closeLabel:"+m_saveFilename;
					m_privateNotification.sendMessage(m_parentName+"TSCanvasMenu", m_parentName+"TSCanvas", content);
				}
				else {
					String content = "closeLabel:";
					m_privateNotification.sendMessage(m_parentName+"TSCanvasMenu", m_parentName+"TSCanvas", content);
				}
			}
		}
	}
	
	public TSCanvasMenu(DataManager dm, int w, int h) {
		m_dataManager = dm;
		setPreferredSize(new Dimension(w,h));
		
		PopupMenuActionListener popupListener = new PopupMenuActionListener(this);
		
		m_popupMenu = new JPopupMenu();
		JMenuItem menuItemSave = new JMenuItem(m_menuItemSaveLabel);
		menuItemSave.addActionListener(popupListener);
		m_popupMenu.add(menuItemSave);
		JMenuItem menuItemSaveAs = new JMenuItem(m_menuItemSaveAsLabel);
		menuItemSaveAs.addActionListener(popupListener);
		m_popupMenu.add(menuItemSaveAs);
		JMenuItem menuItemLoad = new JMenuItem(m_menuItemLoadLabel);
		menuItemLoad.addActionListener(popupListener);
		m_popupMenu.add(menuItemLoad);
		JMenuItem menuItemClose = new JMenuItem(m_menuItemCloseLabel);
		menuItemClose.addActionListener(popupListener);
		m_popupMenu.add(menuItemClose);
		JMenuItem menuItemSet = new JMenuItem(m_menuItemDisplaySetting);
		menuItemSet.addActionListener(popupListener);
		m_popupMenu.add(menuItemSet);
		
		addMouseListener(this);
		addMouseMotionListener(this);
	}
	
	// sometimes, we need to use m_parentName to distinguish TSCanvasMenu in differnt TSCanvasComponents
	public void setParentName(String name) { m_parentName = name; }
	
	private Color pickupUnusedColor() {
		for( int i = 0; i < m_palette.length; i++ ) {
			if( m_usedColors.isEmpty() || !m_usedColors.contains(m_palette[i]) ) {
				m_signalColors.add(m_palette[i]);
				m_usedColors.add(m_palette[i]);
				return m_palette[i];
			}
		}
		return null;
	}
	
	public Color addNewSignal(String source, String name) {
		if( m_signalSources.contains(source) && m_signalNames.contains(name) )
			return null;
		m_signalSources.add(source);
		m_signalNames.add(name);
		Color c = pickupUnusedColor();
		
		repaint();
		
		return c;
	}
	
	public boolean removeSignal(String source, String name) {
		int signalIndex = 0;
		for(; signalIndex < m_signalNames.size(); signalIndex++) {
			if( m_signalSources.get(signalIndex).equalsIgnoreCase(source) && m_signalNames.get(signalIndex).equalsIgnoreCase(name) )
				break;
		}
		if( signalIndex == m_signalNames.size() )
			return false;
		
		m_signalSources.remove(signalIndex);
		m_signalNames.remove(signalIndex);
		m_signalColors.remove(signalIndex);
		m_usedColors.remove(signalIndex);
		
		repaint();
		
		return true;
	}
	
	/**
	 * notification functions: notify other components what happened inside the CanvasMenu
	 * */
	// change some signal's color to a new one
	public void changeSignalColor(String name, String source, Color newColor) {
		final String[] colorNames = {"BLACK", "BLUE", "CYAN", "DARK_GRAY", "GRAY", "GREEN", "LIGHT_GRAY", "MAGENTA", "ORANGE", "PINK", "RED", "YELLOW"}; 
		
		int colorIndex = 0;
		for(; colorIndex < m_signalSources.size(); colorIndex++) {
			if( m_signalSources.get(colorIndex).equalsIgnoreCase(source) && m_signalNames.get(colorIndex).equalsIgnoreCase(name) )
				break;
		}
		m_signalColors.set(colorIndex, newColor);
		
		if( m_privateNotification != null ) {
			int i = 0;
			for( ; i < m_palette.length; i++ ) {
				if( m_palette[i].equals(newColor) )
					break;
			}
			String content = "changeSignalColor:"+source+" "+name+" "+colorNames[i]+";";
			m_privateNotification.sendMessage(m_parentName+"TSCanvasMenu", m_parentName+"TSCanvas", content);
			m_privateNotification.sendMessage(m_parentName+"TSCanvasMenu", m_parentName+"TSAbstractCanvas", content);
		}
	}
	public void mouseOnSignal(String name, String source) {
		
	}
	// change the sample rate of the signals inside the canvas to a new value
	public void changeSamplerateTo(int samplerate) {
		m_samplerate = samplerate;
		if( m_privateNotification != null ) {
			String content = "changeSamplerate:"+Integer.toString(samplerate)+";";
			m_privateNotification.sendMessage(m_parentName+"TSCanvasMenu", m_parentName+"TSCanvas", content);
			m_privateNotification.sendMessage(m_parentName+"TSCanvasMenu", m_parentName+"TSAbstractCanvas", content);
		}
	}
	// change the duration of a single unit to a new value
	public void changeUnitDurationTo(long t) {
		m_unitDuration = t;
		if( m_privateNotification != null ) {
			String content = "changeUnitDuration:"+Long.toString(t)+";";
			m_privateNotification.sendMessage(m_parentName+"TSCanvasMenu", m_parentName+"TSCanvas", content);
			m_privateNotification.sendMessage(m_parentName+"TSCanvasMenu", m_parentName+"TSAbstractCanvas", content);
		}
	}
	// change the start time of the signal to a new value
	public void changeStartTimeTo(long t) {
		m_startTime = t;
		if( m_privateNotification != null ) {
			String content = "changeStartOfSignals:"+Long.toString(t)+";";
			m_privateNotification.sendMessage(m_parentName+"TSCanvasMenu", m_parentName+"TSCanvas", content);
		}
	}
	public void changeVisibleDurationTo(long t) {
		m_visibleDuration = t;
		if( m_privateNotification != null ) {
			String content = "changeVisibleDurationTo:"+Long.toString(t)+";";
			m_privateNotification.sendMessage(m_parentName+"TSCanvasMenu", m_parentName+"TSCanvas", content);
			m_privateNotification.sendMessage(m_parentName+"TSCanvasMenu", m_parentName+"TSAbstractCanvas", content);
		}
	}
	// change the unit representation to a new one. (i.e. ms, s, m, h, d)
	public void changeUnitForUnitDurationTo(String u) {
		m_unitForUnitDuration = u;
	}
	public void changeUnitForStartTimeTo(String u) {
		m_unitForStartTime = u;
		if( m_privateNotification != null ) {
			String content = "changeTimestampFormat:"+u+";";
			m_privateNotification.sendMessage(m_parentName+"TSCanvasMenu", m_parentName+"TSCanvas", content);
		}
	}
	public void changeUnitForVisibleDurationTo(String u) {}
	
	// motion events' delegates
	public void mousePressed(MouseEvent e) {
		if( e.isPopupTrigger() )
			m_popupMenu.show(e.getComponent(), e.getX(), e.getY());
	}
	public void mouseReleased(MouseEvent e) {
		if( e.isPopupTrigger() )
			m_popupMenu.show(e.getComponent(), e.getX(), e.getY());
	}
	
	public void mouseClicked(MouseEvent e) {
		if( e.getButton() == MouseEvent.BUTTON3 ) {
//			TSCanvasMenu m = (TSCanvasMenu)e.getSource();
//			SignalDisplaySetting s = new SignalDisplaySetting(m, 400, 200);
//			JFrame f = new JFrame("Setting");
//			f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
//			f.setSize(400, 200);
//			f.add(s);
//			f.setVisible(true);
		}
		else if( e.getButton() == MouseEvent.BUTTON1 ) {
			TSCanvasMenu m = (TSCanvasMenu)e.getSource();
			Dimension sz = getSize();
			int x = e.getX();
			int y = e.getY();
			int totalSignalNum = m_signalColors.size();
			if( totalSignalNum == 0 )
				return;
			int colNumber = (int)Math.ceil((double)totalSignalNum / m_fixedRowNumber);
			int ceilX = sz.width/colNumber;
			int ceilY = sz.height/m_fixedRowNumber;
			
			int row = y/ceilY;
			int col = x/ceilX;
			int index = col*m_fixedRowNumber+row;
			
			if( index >= 0 && index < m_signalNames.size() ) {
				SignalColorPalette s = new SignalColorPalette(m, index, 60, 45, 3, 4);
				JFrame f = new JFrame("Palette");
				f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				f.setSize(100, 100);
				f.add(s);
				f.setVisible(true);
			}
		}
	}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mouseMoved(MouseEvent e) {
		Dimension sz = getSize();
		int x = e.getX();
		int y = e.getY();
		int totalSignalNum = m_signalColors.size();
		if( totalSignalNum == 0 )
			return;
		int colNumber = (int)Math.ceil((double)totalSignalNum / m_fixedRowNumber);
		int ceilX = sz.width/colNumber;
		int ceilY = sz.height/m_fixedRowNumber;
		
		int row = y/ceilY;
		int col = x/ceilX;
		int index = col*m_fixedRowNumber+row;
		
		if( index >= 0 && index < m_signalNames.size() ) {
			mouseOnSignal(m_signalNames.get(index), m_signalSources.get(index));
		}
	}
	public void mouseDragged(MouseEvent e) {}
	
	// Unit testing
	public static void doCreateAndShowUI() {
		JFrame f = new JFrame("Test Menu");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setPreferredSize(new Dimension(40,200));
		
		DataManager dm = DataManager.getInstance();
		dm.loadDimensionsFromFile("test.oka");
		
		TSCanvasMenu m = new TSCanvasMenu(dm, 40, 200);

		f.add( m );
		f.pack();
		f.setVisible(true);
		
		m.addNewSignal("\\\\sfs\\data\\public\\1.avi", "smile");
		m.addNewSignal("\\\\sfs\\data\\public\\1.avi", "facex");
		
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
