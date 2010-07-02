/**
 * @author lixinghu@usc.edu
 * @since 2010/6/28
 * this table is used to hold labels for one dimension so that user could see the details of labels more easily and navigate to certain
 * labels more quickly. this component is associated with each TSCanvasComponent
 * */

package UI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.util.*;

public class TSLabelTable extends TSMsgPanel{
	private static final long serialVersionUID = 6247412074492949600L;
	public JTable m_table;
	public String m_parentFrameName = "";
	
	private class ButtonEditor extends AbstractCellEditor implements TableCellEditor, ActionListener {
		private static final String m_goHereButton = "->";
		private static final String m_linkButton = "~>";
		private static final long serialVersionUID = 1251931660253283176L;
		private JTable m_parent;
		private JButton m_button = new JButton();
		
		public ButtonEditor(JTable table) {
			this.m_parent = table;
			m_button.addActionListener(this);
		}
		
		public void actionPerformed(ActionEvent evt) {
			try {
				String button = ((JButton)evt.getSource()).getText();
				int r = m_parent.getEditingRow();
				String meta = (String)m_parent.getValueAt(r, 0);
				String beg  = (String)m_parent.getValueAt(r, 1);
				String end  = (String)m_parent.getValueAt(r, 2);
				
				if( button.equalsIgnoreCase(m_goHereButton) ) {
					if( m_privateNotification != null ) {
						String content = "changeStartOfVisibleSignalToSampleIndex:"+beg+";";
						m_privateNotification.sendMessage(m_parentFrameName+"TSLabelTable", m_parentFrameName+"TSCanvas", content);
					}
				}
				else if( button.equalsIgnoreCase(m_linkButton) ) {
					//TODO: want to link the label to some external resource
				}
				
				System.out.println(button+": "+meta+" "+beg+" "+end);
			} catch(Exception exp) {
				exp.printStackTrace();
			}
		}
		
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
			m_button.setText(value.toString());
			return m_button;
		}
		
		public Object getCellEditorValue() {
			return m_button.getText();
		}
		
		public boolean isCellEditable(EventObject anEvent) { 
			return true; 
		}
		public boolean shouldSelectCell(EventObject anEvent) {
			return true;
		}
		public boolean stopCellEditing() { return super.stopCellEditing(); }
		public void cancelCellEditing() { super.cancelCellEditing(); }
	}
	
	private class ButtonRenderer implements TableCellRenderer {
		JButton m_button = new JButton();
		
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			m_button.setText(value.toString());
			return m_button;
		}
	}
	
	@SuppressWarnings("unused")
	private class RowSelectionHandler implements ListSelectionListener {
		public void valueChanged(ListSelectionEvent e) {
			ListSelectionModel lsm = (ListSelectionModel)e.getSource();
			int firstIndex = e.getFirstIndex();
			int lastIndex = e.getLastIndex();
			System.out.println(firstIndex+" "+lastIndex);
			
			int minIndex = lsm.getMinSelectionIndex();
			int maxIndex = lsm.getMaxSelectionIndex();
			System.out.println(minIndex+" "+maxIndex);
		}
	}
	
	public TSLabelTable(int w, int h) {
		DefaultTableModel tableModel = new DefaultTableModel(new String[]{"Meta", "BegSampleIdx", "EndSampleIdx", "", ""}, 0) {
			private static final long serialVersionUID = 1L;
			public boolean isCellEditable(int rowIndex, int columnIndex) {
				if( columnIndex >= 3 )
					return true;
				else
					return false;
			}
		};
		
		m_table = new JTable(tableModel);
		m_table.setCellSelectionEnabled(true);
//		ListSelectionModel listSelectionModel = m_table.getSelectionModel();
//		listSelectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
//		listSelectionModel.addListSelectionListener(new RowSelectionHandler());
//		m_table.setSelectionModel(listSelectionModel);
		m_table.getColumnModel().getColumn(3).setCellRenderer(new ButtonRenderer());
		m_table.getColumnModel().getColumn(4).setCellRenderer(new ButtonRenderer());
		m_table.getColumnModel().getColumn(3).setCellEditor(new ButtonEditor(m_table));
		m_table.getColumnModel().getColumn(4).setCellEditor(new ButtonEditor(m_table));
		
		m_table.setPreferredSize(new Dimension(w,h));
		
		JScrollPane scrollPane = new JScrollPane(m_table);
		add(scrollPane);
	}
	
	public void setParentFrameName(String n) {
		m_parentFrameName = n;
	}
	
	public void addLabel(String meta, String beg, String end) {
		DefaultTableModel model = (DefaultTableModel)m_table.getModel();
		model.addRow(new Object[] {meta, beg, end, "->", "~>"});
		model.fireTableRowsInserted(model.getRowCount()-1, model.getRowCount()-1);
	}
	
	public void removeLabel(String meta, String beg, String end) {
		DefaultTableModel model = (DefaultTableModel)m_table.getModel();
		int rowCnt = model.getRowCount();
		for( int i = 0; i < rowCnt; i++ ) {
			if( ((String)model.getValueAt(i, 0)).equalsIgnoreCase(meta) &&
				((String)model.getValueAt(i, 1)).equalsIgnoreCase(beg) &&
				((String)model.getValueAt(i, 2)).equalsIgnoreCase(end) ) {
				model.removeRow(i);
				model.fireTableRowsDeleted(i, i);
				return;
			}
		}
	}
	
	/**
	 * message:
	 * 	addLabel: meta beg end;
	 * 	removeLabel: meta beg end;
	 * */
	public void receiveMessage(String from, String content) {
		try {
			String cmd = content.substring(0, content.indexOf(":")).trim();
			String arg = content.substring(content.indexOf(":")+1).trim();
			String meta = arg.substring(0, arg.indexOf(" ")).trim();
			String beg = arg.substring(arg.indexOf(" ")+1, arg.lastIndexOf(" ")).trim();
			String end = arg.substring(arg.lastIndexOf(" ")+1).trim();
			if( cmd.equalsIgnoreCase("addLabel") )
				addLabel(meta, beg, end);
			else if( cmd.equalsIgnoreCase("removeLabel") )
				removeLabel(meta, beg, end);
		} catch(Exception exp) {
			exp.printStackTrace();
		}
	}
	
	// Unit testing
	private static void doCreateAndShowUI() {
		JFrame f = new JFrame("TSLabel Table");
		f.setSize(500, 200);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		TSLabelTable l = new TSLabelTable(500, 200);
		l.addLabel("test", "100", "200");
		l.addLabel("test", "300", "400");
		l.addLabel("test", "500", "600");
		
		l.removeLabel("test", "500", "600");
		
		f.add(l);
		
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
