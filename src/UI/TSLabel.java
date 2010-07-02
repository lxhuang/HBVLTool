/**
 * @author lixinghu@usc.edu
 * @since 2010/6/19
 * to add label for dimension data
 * */

package UI;

import Data.DataFormat;
import Data.DataManager;
import Data.Label;
import Data.DimensionData;
import javax.swing.*;
import java.awt.event.*;
import java.util.Vector;
import java.io.*;

public class TSLabel {
	private DataManager m_dataManager;
	private String m_toLabelSignalSource = "";
	private String m_toLabelSignalName = "";
	public String m_meta = "";
	public String m_author = "labeler";
	private boolean m_constantMeta = false;
	private TSCanvas m_parent = null;
	
	public TSLabel(DataManager dm, TSCanvas p) {
		m_dataManager = dm;
		m_parent = p;
	}
	
	/**
	 * add label to dimension
	 * @param beg: beginning sample index
	 * @param end: end sample index
	 * @param x, y: the mouse position. at the first time, a frame will pop up. user is going to set which signal he wants to label (in case
	 * there are more than one signals in the canvas right now), he also wants to set whether the meta will be constant or not
	 * */
	public boolean addLabel(int beg, int end, int x, int y) {
		if( m_toLabelSignalSource == "" || m_toLabelSignalName == "" || m_constantMeta == false ) {
			showSettingFrame(x, y, beg, end);
			return false;
		}
		DimensionData d = m_dataManager.getDimensionBySourceAndName(m_toLabelSignalSource, m_toLabelSignalName);
		if( d != null )
			return d.addLabel(m_meta, beg, end);
		else
			return false;
	}
	
	// delete label by its boundaries
	public boolean deleteLabel(int beg, int end) {
		DimensionData d = m_dataManager.getDimensionBySourceAndName(m_toLabelSignalSource, m_toLabelSignalName);
		if( d != null )
			return d.deleteLabel(beg, end);
		else
			return false;
	}
	
	// delete label by its index
	public boolean deleteLabelByIndex(int index) {
		DimensionData d = m_dataManager.getDimensionBySourceAndName(m_toLabelSignalSource, m_toLabelSignalName);
		if( d != null )
			return d.deleteLabelByIndex(index);
		else
			return false;
	}
	
	// find label by sample index
	public Label findLabel(int sampleIndex) {
		DimensionData d = m_dataManager.getDimensionBySourceAndName(m_toLabelSignalSource, m_toLabelSignalName);
		if( d != null )
			return d.findLabel(sampleIndex);
		else
			return null;
	}
	
	// find the label index
	public int findLabelIndex(Label l) {
		DimensionData d = m_dataManager.getDimensionBySourceAndName(m_toLabelSignalSource, m_toLabelSignalName);
		if( d != null )
			return d.findLabelIndex(l);
		else
			return -1;
	}
	
	// close label
	public void closeLabels() {
		DimensionData d = m_dataManager.getDimensionBySourceAndName(m_toLabelSignalSource, m_toLabelSignalName);
		if( d != null ) {
			d.clearLabels();
			m_toLabelSignalSource = m_toLabelSignalName = "";
			m_meta = "";
			m_constantMeta = false;
		}
	}
	
	// save label
	public void saveLabels(String filename) {
		DimensionData d = m_dataManager.getDimensionBySourceAndName(m_toLabelSignalSource, m_toLabelSignalName);
		if( d != null )
			DataFormat.writeLabelOfDimensionDataToFile(d, filename);
	}
	
	// load label (actually, it appends label to m_label)
	public void loadLabel(String filename) throws IOException{
		Vector<Label> labels = new Vector<Label>();
		Vector<String> desc = new Vector<String>();
		DataFormat.readLabelFromFile(filename, desc, labels);
		
		if( desc.size() != 3 ) {
			throw new IOException("the label file must contain source, name, and author");
		}
		
		if( m_toLabelSignalSource=="" && m_toLabelSignalName=="" ) {
			m_toLabelSignalSource=desc.get(0);
			m_toLabelSignalName=desc.get(1);
			m_author=desc.get(2);
		} else {
			if( m_toLabelSignalSource.equalsIgnoreCase(desc.get(0))==false || m_toLabelSignalName.equalsIgnoreCase(desc.get(1))==false ) {
				JOptionPane.showMessageDialog(m_parent, "Cannot load this label. The signal it labels is different from the signal the current label is for");
				return;
			}
		}
		
		DimensionData d = m_dataManager.getDimensionBySourceAndName(desc.get(0), desc.get(1));
		if( d != null ) {
			d.m_labelAuthor = desc.get(2);
			for( int i = 0; i < labels.size(); i++ )
				d.m_labels.add(labels.get(i));
		}
		
//		DimensionData d = m_dataManager.getDimensionBySourceAndName(m_toLabelSignalSource, m_toLabelSignalName);
//		if( d != null )
//			DataFormat.readLabelOfDimensionDataFromFile(d, filename);
	}
	
	public void showSettingFrame(int x, int y, int beg, int end) {
		JFrame f = new JFrame("Label Setting");
		f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		f.setSize(400, 200);
		
		LabelSettingUI ui = new LabelSettingUI(m_parent, this, beg, end);	// beg, end are the bounds for label. when user presses ok, we should add the label into dimension data
		ui.m_container = f;
		
		f.add(ui);
		f.setVisible(true);
		f.setLocation(x, y);
	}
	
	private class LabelSettingUI extends JPanel {
		private static final long serialVersionUID = 3199828437634269799L;
		public TSCanvas m_canvas = null;
		public TSLabel m_parent = null;
		public JFrame m_container = null;
		public JComboBox m_signalSourceComboBox = null, m_signalNameComboBox = null;
		public JTextField m_metaTextField = null, m_authorTextField = null;
		public JCheckBox m_metaConstantCheckBox = null;
		public int m_currentLabelBeg=0, m_currentLabelEnd=0;
		
		public LabelSettingUI(TSCanvas canvas, TSLabel parent, int beg, int end) {
			m_canvas = canvas;
			m_parent = parent;
			m_currentLabelBeg=beg;
			m_currentLabelEnd=end;
			
			JLabel signalSourceLabel = new JLabel("Signal Source");
			Vector<String> noduplicate = new Vector<String>();
			for( int i = 0; i < m_canvas.m_signalSources.size(); i++ ) {
				if( noduplicate.contains(m_canvas.m_signalSources.get(i)) == false )
					noduplicate.add(m_canvas.m_signalSources.get(i));
			}
			m_signalSourceComboBox = new JComboBox( noduplicate );
			
			JLabel signalNameLabel = new JLabel("Signal Name");
			Vector<String> noduplicate1 = new Vector<String>();
			for( int i = 0; i < m_canvas.m_signalNames.size(); i++ ) {
				if( noduplicate1.contains(m_canvas.m_signalNames.get(i)) == false )
					noduplicate1.add(m_canvas.m_signalNames.get(i));
			}
			m_signalNameComboBox = new JComboBox( noduplicate1 );
			
			JLabel metaLabel = new JLabel("Label Meta");
			m_metaTextField = new JTextField(5);
			m_metaConstantCheckBox = new JCheckBox("Constant?");
			m_metaConstantCheckBox.setSelected(m_parent.m_constantMeta);
			
			JLabel authorLabel = new JLabel("Author");
			m_authorTextField = new JTextField(m_parent.m_author);
			
			JButton confirmBtn = new JButton("OK");
			confirmBtn.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					m_parent.m_author = m_authorTextField.getText();
					m_parent.m_constantMeta = m_metaConstantCheckBox.isSelected();
					m_parent.m_meta = m_metaTextField.getText();
					m_parent.m_toLabelSignalName = (String)m_signalNameComboBox.getSelectedItem();
					m_parent.m_toLabelSignalSource = (String)m_signalSourceComboBox.getSelectedItem();
					m_parent.addLabel(m_currentLabelBeg, m_currentLabelEnd, 0, 0);
					m_container.dispose();
				}
			});
			
			GroupLayout layout = new GroupLayout(this);
			setLayout(layout);
			layout.setAutoCreateContainerGaps(true);
			layout.setAutoCreateGaps(true);
			
			layout.setHorizontalGroup(
				layout.createSequentialGroup()
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
						.addComponent(signalSourceLabel)
						.addComponent(signalNameLabel)
						.addComponent(metaLabel)
						.addComponent(authorLabel)
						.addComponent(confirmBtn))
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
						.addComponent(m_signalSourceComboBox)
						.addComponent(m_signalNameComboBox)
						.addComponent(m_metaTextField)
						.addComponent(m_authorTextField))
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
						.addComponent(m_metaConstantCheckBox))
			);
			
			layout.setVerticalGroup(
				layout.createSequentialGroup()
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(signalSourceLabel)
						.addComponent(m_signalSourceComboBox))
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(signalNameLabel)
						.addComponent(m_signalNameComboBox))
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(metaLabel)
						.addComponent(m_metaTextField)
						.addComponent(m_metaConstantCheckBox))
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(authorLabel)
						.addComponent(m_authorTextField))
					.addComponent(confirmBtn)
			);
		}
	}
}
