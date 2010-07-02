/**
 * @author lixinghu@usc.edu
 * @since 2010/6/24
 * this table is used to display loaded signals, and assign signals to TSCanvas to visualize them
 * */

package UI;

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.table.*;
import java.util.*;
import java.awt.event.*;
import java.awt.*;

public class TSSignalViewTable extends TSMsgPanel{
	private static final long serialVersionUID = 8668365177003677090L;
	public Vector<String> m_signalNames = new Vector<String>();
	public Vector<String> m_signalSources = new Vector<String>();
	public Vector<String> m_signalContainerName = new Vector<String>();	// the name of the panel that visualizes the signal
	
	public JTable m_table;
	public TSSignalTableModel m_tableModel;
	
	class TSSignalTableModel extends AbstractTableModel {
		private static final long serialVersionUID = -2853887189261883630L;
		private final String[] m_columnNames = {"Source", "Feature Name", "Visualize in"};
		private Vector<Vector<Object>> m_tableData = new Vector<Vector<Object>>();
		
		public int getColumnCount() {
			return m_columnNames.length;
		} 
		public int getRowCount() {
			return m_tableData.size();
		}
		public String getColumnName(int col) {
			return m_columnNames[col];
		}
		public Object getValueAt(int row, int col) {
			try {
				return m_tableData.get(row).get(col);
			} catch(Exception exp) {
				exp.printStackTrace();
				return null;
			}
		}
		public void setValueAt(Object val, int row, int col) {
			if( col >= m_columnNames.length )
				return;
			if( row == getRowCount() ) {
				Vector<Object> newr = new Vector<Object>();
				for( int i = 0; i < m_columnNames.length; i++ )
					newr.add(new String(""));
				m_tableData.add(newr);
				m_tableData.get(row).set(col, val);
				fireTableRowsInserted(getRowCount()-1,getRowCount()-1);
			}
			else if( row < getRowCount() ) {
				m_tableData.get(row).set(col, val);
			}
			else {
				return;
			}
		}
		public void removeRow(int row) {
			if( row >= getRowCount() )
				return;
			m_tableData.remove(row);
			fireTableRowsDeleted(row, row);
		}
		public boolean isCellEditable(int row, int col) {
			if( col >= 2 )
				return true;
			else
				return false;
		}
	}
	
	public TSSignalViewTable(int w, int h) {
		setPreferredSize(new Dimension(w,h));
		
		m_tableModel = new TSSignalTableModel();
		m_table = new JTable( m_tableModel );
		m_table.setAutoCreateRowSorter(true);
		m_table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		m_table.getColumnModel().getColumn(0).setPreferredWidth(w/2);
		m_table.getColumnModel().getColumn(1).setPreferredWidth(w/4);
		m_table.getColumnModel().getColumn(2).setPreferredWidth(w/4);
		
		setDisplayColumn(m_table);
		setColumnTip(m_table);
		
		JScrollPane scrollPane = new JScrollPane( m_table );
		add(scrollPane);
	}
	
	class ComboBoxEditor implements TableCellEditor, ItemListener {
		public JComboBox m_combo;
		public DefaultCellEditor m_cellEditor;
		public String m_src = "", m_name = "";	// the corresponding source name and signal name
		public ComboBoxEditor() {
			m_combo = new JComboBox();
			m_combo.addItem("");
			for( int i = 0; i < m_signalContainerName.size(); i++ )
				m_combo.addItem(m_signalContainerName.get(i));
			m_cellEditor = new DefaultCellEditor(m_combo);
			m_combo.addItemListener(this);
		}
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
			if( column == 2 ) {
				int rowIndex = m_table.convertRowIndexToModel(row);	// we allow sortable column
				m_src = (String)m_tableModel.getValueAt(rowIndex, 0);
				m_name = (String)m_tableModel.getValueAt(rowIndex, 1);
				String str = (value==null) ? "" : value.toString();
				return m_cellEditor.getTableCellEditorComponent(table, str, isSelected, row, column);
			}
			return null;
		}
		public Object getCellEditorValue() {
			return (String)m_combo.getSelectedItem();
		}
		public boolean stopCellEditing() {
			return m_cellEditor.stopCellEditing();
		}
		public void addCellEditorListener( CellEditorListener l ) {
			m_cellEditor.addCellEditorListener(l);
		}
		public void cancelCellEditing() {
			m_cellEditor.cancelCellEditing();
		}
		public boolean isCellEditable(EventObject arg0) {
			return m_cellEditor.isCellEditable(arg0);
		}
		public void removeCellEditorListener(CellEditorListener l) {
			m_cellEditor.removeCellEditorListener(l);
		}
		public boolean shouldSelectCell(EventObject anEvent) {
			return m_cellEditor.shouldSelectCell(anEvent);
		}
		public void itemStateChanged(ItemEvent e) {
			if( e.getID() == ItemEvent.ITEM_STATE_CHANGED ) {
				if( e.getStateChange() == ItemEvent.DESELECTED ) {
//					System.out.println(m_src + " " + m_name + " is deleted from " + e.getItem());
					if( m_privateNotification != null ) {
						String to = (String)e.getItem();
						String content = "removeSignal:" + m_src + " " + m_name;
						m_privateNotification.sendMessage("TSSignalViewTable", to, content);
					}
				}
				else if( e.getStateChange() == ItemEvent.SELECTED ) {
//					System.out.println(m_src + " " + m_name + " is added to " + e.getItem());
					if( m_privateNotification != null ) {
						String to = (String)e.getItem();
						String content = "addNewSignal:" + m_src + " " + m_name;
						m_privateNotification.sendMessage("TSSignalViewTable", to, content);
					}
				}
			}
		}
	}
	
	// set the column that used to choose which canvas to display in
	private void setDisplayColumn(JTable table) {
		TableColumn displayColumn = table.getColumnModel().getColumn(2);
		displayColumn.setCellEditor( new ComboBoxEditor() );
	}
	
	public void setColumnTip(JTable table) {
		DefaultTableCellRenderer rendererCol0 = new DefaultTableCellRenderer();
		rendererCol0.setToolTipText("the source of the signal");
		table.getColumnModel().getColumn(0).setCellRenderer(rendererCol0);
		
		DefaultTableCellRenderer rendererCol1 = new DefaultTableCellRenderer();
		rendererCol1.setToolTipText("the name of the signal");
		table.getColumnModel().getColumn(1).setCellRenderer(rendererCol1);
		
		DefaultTableCellRenderer rendererCol2 = new DefaultTableCellRenderer();
		rendererCol2.setToolTipText("select which canvas this signal will be drawn in");
		table.getColumnModel().getColumn(2).setCellRenderer(rendererCol2);
	}
	
	public void addNewSignal(String source, String name) {
		if( m_signalSources.contains(source) == true && m_signalNames.contains(name) == true )
			return;
		
		m_signalSources.add(source);
		m_signalNames.add(name);
		
		m_tableModel.setValueAt(source, m_signalSources.size()-1, 0);
		m_tableModel.setValueAt(name, m_signalNames.size()-1, 1);
		m_tableModel.setValueAt("", m_signalNames.size()-1, 2);
	}
	
	public void removeSignal(String source, String name) {
		int signalIndex = 0;
		for(; signalIndex<m_signalNames.size(); signalIndex++) {
			if( m_signalNames.get(signalIndex).equalsIgnoreCase(name) && m_signalSources.get(signalIndex).equalsIgnoreCase(source) )
				break;
		}
		if( signalIndex == m_signalNames.size() )	// doesn't exist
			return;
		m_signalNames.remove(signalIndex);
		m_signalSources.remove(signalIndex);
		
		m_tableModel.removeRow(signalIndex);
	}
	
	public void addNewVisualizationPanel(String name) {
		if( m_signalContainerName.contains(name) == false )
			m_signalContainerName.add(name);
		setDisplayColumn(m_table);
	}
	
	public void removeVisualizationPanel(String name) {
		m_signalContainerName.remove(name);
		setDisplayColumn(m_table);
	}
	
	/**
	 * @param from: the message is sent from which module
	 * @param content: the content of the message, the format of the message must be:
	 * 	addPanel:panelName,
	 * 	removePanel:panelName
	 * */
	public void receiveMessage(String from, String content) {
		try {
			String cmd = content.substring(0, content.indexOf(":")).trim();
			String arg = content.substring(content.indexOf(":")+1).trim();
			if( cmd.equalsIgnoreCase("addPanel") ) {
				addNewVisualizationPanel(arg);
			}
			else if( cmd.equalsIgnoreCase("removePanel") ) {
				removeVisualizationPanel(arg);
			}
		} catch(Exception exp) {
			exp.printStackTrace();
		}
	}
	
	// Unit Testing
	public static void doCreateAndShowUI() {
		JFrame f = new JFrame("Test Table");
		f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		f.setSize(500, 400);
		
		TSSignalViewTable table = new TSSignalViewTable(500, 400);
		table.addNewVisualizationPanel("Panel1");
		table.addNewVisualizationPanel("Panel2");
		
		table.addNewSignal("\\\\sfs\\data\\public\\1.avi", "smile");
		table.addNewSignal("\\\\sfs\\data\\public\\1.avi", "facex");
		
		f.add(table);
		f.pack();
		f.setVisible(true);
	}
	public static void main(String[] args) {
		Runnable createAndShowUI = new Runnable(){
			public void run() {
				doCreateAndShowUI();
			}
		};
		SwingUtilities.invokeLater(createAndShowUI);
	}
}
