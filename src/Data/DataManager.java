/**
 * @author lixinghu@usc.edu
 * @since 2010/6/4
 * @modified 2010/6/26 change it to singleton
 * work as a bridge between UI and underlying data
 * */

package Data;

import java.util.*;
import java.io.*;

public class DataManager {
	protected Vector<DimensionData> m_data;
	protected Vector<DimensionDescription> m_dimDescription;	// this is just a temporary variable, cannot rely on this
	protected int m_lastInsertDimBegIndex=-1, m_lastInsertDimEndIndex=-1;
	
	private static DataManager instance = null; 
	
	private DataManager() {
		m_data = new Vector<DimensionData>();
		m_dimDescription = new Vector<DimensionDescription>();
	}
	
	public static synchronized DataManager getInstance() {
		if( instance == null )
			instance = new DataManager();
		return instance;
	}
	
	private synchronized void loadContinuousDimensionsFromFile(String filename) {
		try {
			Vector<DimensionData> dims = new Vector<DimensionData>();
			DataFormat.loadContinuousDimensionsFromCSVFile(filename, dims);
			for( int i = 0; i < dims.size(); i++ )
				m_data.add(dims.get(i));
			m_lastInsertDimBegIndex = m_data.size()-dims.size();
			m_lastInsertDimEndIndex = m_data.size()-1;
		} catch ( Exception exp ) {
			exp.printStackTrace();
		}
	}
	
	private synchronized void loadDiscreteDimensionsFromFile(String filename) {
		try {
			Vector<DimensionData> dims = new Vector<DimensionData>();
			DataFormat.loadDiscreteEventsFrom(filename, dims);
			for( int i = 0; i < dims.size(); i++ )
				m_data.add(dims.get(i));
			m_lastInsertDimBegIndex = m_data.size()-dims.size();
			m_lastInsertDimEndIndex = m_data.size()-1;
		} catch ( Exception exp ) {
			exp.printStackTrace();
		}
	}
	
	public synchronized void loadWaveFromFile(String filename) {
		try {
			if( !(filename.contains(".wav") || filename.contains(".WAV")) ) {
				System.out.println("only can load wav file");
				return;
			}
			Vector<DimensionData> dims = new Vector<DimensionData>();
			DataFormat.loadContinuousDimensionDataFromWavFile(filename, dims);
			for( int i = 0; i < dims.size(); i++ )
				m_data.add(dims.get(i));
			m_lastInsertDimBegIndex = m_data.size()-dims.size();
			m_lastInsertDimEndIndex = m_data.size()-1;
		} catch( Exception exp ) {
			exp.printStackTrace();
		}
	}
	
	// when dimension description and the dimension data are in the same file
	// we should call this function
	public synchronized void loadDimensionsFromFile(String filename) {
		final String separator = "######";
		final String keyword = "type";
		DimensionType dimensionType = DimensionType.INVALID;
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(filename)));
			String textOfLine;
			while( (textOfLine = br.readLine()) != null ) {
				if( textOfLine.equalsIgnoreCase(separator) )
					break;
				int pos = textOfLine.indexOf(":");
				String kw = textOfLine.substring(0, pos).trim();
				if( kw.equalsIgnoreCase(keyword) ) {	// type of dimensions
					if( textOfLine.substring(pos+1).trim().equalsIgnoreCase("Continuous") )
						dimensionType = DimensionType.CONTINUOUS;
					else if( textOfLine.substring(pos+1).trim().equalsIgnoreCase("Discrete") )
						dimensionType = DimensionType.EVENT;
					break;
				}
			}
			br.close();
			
			if( dimensionType == DimensionType.INVALID )
				throw new IOException("missing dimension type or having wrong type");
			else if( dimensionType == DimensionType.EVENT )
				loadDiscreteDimensionsFromFile(filename);
			else if( dimensionType == DimensionType.CONTINUOUS )
				loadContinuousDimensionsFromFile(filename);
		} catch( Exception exp ) {
			exp.printStackTrace();
		}
	}
	
	// load dimension description from file
	// can only load continuous data
	public synchronized void loadDimensionsDescriptionFromFile(String filename, String type) {
		m_dimDescription.clear();
		try {
			if( type.equalsIgnoreCase("json") ) {
				Vector<DimensionDescription> descs = new Vector<DimensionDescription>();
				DataFormat.loadContinuousDimensionsDescFromJSONFile(filename, descs);
				for( int i = 0; i < descs.size(); i++ )
					m_dimDescription.add(descs.get(i));
			}
		} catch(Exception exp) {
			exp.printStackTrace();
		}
	}
	
	// load the dimension data from file
	// MUST first load dimension description!
	public synchronized void loadDimensionsDataFromFile(String filename, String type) {
		try {
			if( m_dimDescription.isEmpty() )
				throw new IOException("please first load dimension description");
			
			Vector<DimensionData> dims = new Vector<DimensionData>();
			
			// initialize the dims by the dimension descriptions just loaded
			for( int i = 0; i < m_dimDescription.size(); i++ ) {
				int totalLen  = m_dimDescription.get(i).m_length;
				float dataMax = m_dimDescription.get(i).m_max;
				float dataMin = m_dimDescription.get(i).m_min;
				String name   = m_dimDescription.get(i).m_name;
				String source = m_dimDescription.get(i).m_source;
				
				DimensionData d = new DimensionData(totalLen, dataMax, dataMin, name, DimensionType.CONTINUOUS, source);
				dims.add(d);
			}
			
			// clear the just loaded dimension description
			m_dimDescription.clear();
			
			if( type.equalsIgnoreCase("csv") ) {
				DataFormat.loadContinuousDimensionsDataFromCSVFile(filename, dims);
				for( int i = 0; i < dims.size(); i++ )
					m_data.add(dims.get(i));
			}
			else if( type.equalsIgnoreCase("wav") ) {
				DataFormat.loadContinuousDimensionDataFromWavFile(filename, dims);
				for( int i = 0; i < dims.size(); i++ )
					m_data.add(dims.get(i));
			}
			m_lastInsertDimBegIndex = m_data.size()-dims.size();
			m_lastInsertDimEndIndex = m_data.size()-1;
		} catch(Exception exp) {
			exp.printStackTrace();
		}
	}
	
	// load dimension description from string
	public synchronized void loadDimensionsDescriptionFromString(String desc, String type) {
		m_dimDescription.clear();
		if( type.equalsIgnoreCase("json") ) {
			Vector<DimensionDescription> dims = new Vector<DimensionDescription>();
			DataFormat.loadContinuousDimensionsDescFromJSONString(desc, dims);
			for( int i = 0; i < dims.size(); i++ ) {
				m_dimDescription.add(dims.get(i));
			}
		}
	}
	
	// load dimension data from string
	// MUST first load dimension description first
	public synchronized void loadDimensionsDataFromString(String data, String type) throws IOException {
		if( type.equalsIgnoreCase("csv") ) {
			if( m_dimDescription.isEmpty() )
				throw new IOException("please first load dimension description from string");
			
			Vector<DimensionData> dims = new Vector<DimensionData>();
			
			// initialize the dims by the dimension description just loaded
			for( int i = 0; i < m_dimDescription.size(); i++ ) {
				int totalLen  = m_dimDescription.get(i).m_length;
				float dataMax = m_dimDescription.get(i).m_max;
				float dataMin = m_dimDescription.get(i).m_min;
				String name   = m_dimDescription.get(i).m_name;
				String source = m_dimDescription.get(i).m_source;
				
				DimensionData d = new DimensionData(totalLen, dataMax, dataMin, name, DimensionType.CONTINUOUS, source);
				dims.add(d);
			}
			
			// clear the just loaded dimension description
			m_dimDescription.clear();
			
			DataFormat.loadContinuousDimensionsDataFromCSVString(data, dims);
			for( int i = 0; i < dims.size(); i++ )
				m_data.add(dims.get(i));
			m_lastInsertDimBegIndex = m_data.size()-dims.size();
			m_lastInsertDimEndIndex = m_data.size()-1;
		}
	}
	
	// if you want to keep the loaded dimension description, and keep adding data into dimensions
	// MUST first load dimension description, and the dimension description will not be cleared
	public synchronized void appendDimensionsDataFromFile(String filename, String type) {
		if( type.equalsIgnoreCase("csv") ) {
			try {
				if( m_dimDescription.isEmpty() )
					throw new IOException("please first load dimension description");
				
				Vector<DimensionData> dims = new Vector<DimensionData>();
				
				// initialize the dims by the dimension description just loaded
				for( int i = 0; i < m_dimDescription.size(); i++ ) {
					int totalLen  = m_dimDescription.get(i).m_length;
					float dataMax = m_dimDescription.get(i).m_max;
					float dataMin = m_dimDescription.get(i).m_min;
					String name   = m_dimDescription.get(i).m_name;
					String source = m_dimDescription.get(i).m_source;
					
					DimensionData d = new DimensionData(totalLen, dataMax, dataMin, name, DimensionType.CONTINUOUS, source);
					dims.add(d);
				}
				
				DataFormat.loadContinuousDimensionsDataFromCSVFile(filename, dims);
				for( int i = 0; i < dims.size(); i++ ) {
					int _n = dims.get(i).getLen();
					float[] _val = new float[_n];
					dims.get(i).get(0, _n, _val);
					for(int j = 0; j < _n; j++)
						m_data.get(i).add(_val[j]);
				}
			} catch ( Exception exp ) {
				exp.printStackTrace();
			}
		}
	}
	
	// if you want to keep the loaded dimension description, and keep adding data into dimensions
	// MUST first load dimension description, and the dimension description will not be cleared
	public synchronized void appendDimensionsDataFromString(String data, String type) throws IOException{
		if( type.equalsIgnoreCase("csv") ) {
			if( m_dimDescription.isEmpty() )
				throw new IOException("please first load dimension description");
			
			Vector<DimensionData> dims = new Vector<DimensionData>();
			
			// initialize the dims by the dimension description just loaded
			for( int i = 0; i < m_dimDescription.size(); i++ ) {
				int totalLen  = m_dimDescription.get(i).m_length;
				float dataMax = m_dimDescription.get(i).m_max;
				float dataMin = m_dimDescription.get(i).m_min;
				String name   = m_dimDescription.get(i).m_name;
				String source = m_dimDescription.get(i).m_source;
				
				DimensionData d = new DimensionData(totalLen, dataMax, dataMin, name, DimensionType.CONTINUOUS, source);
				dims.add(d);
			}
			
			DataFormat.loadContinuousDimensionsDataFromCSVString(data, dims);
			for( int i = 0; i < dims.size(); i++ ) {
				int _n = dims.get(i).getLen();
				float[] _val = new float[_n];
				dims.get(i).get(0, _n, _val);
				for( int j = 0; j < _n; j++ )
					m_data.get(i).add(_val[j]);
			}
		}
	}
	
	// get the dimension by its name and source
	// if you cannot find the correct dimension just return null
	public synchronized DimensionData getDimensionBySourceAndName(String source, String name) {
		for( int i = 0; i < m_data.size(); i++ ) {
			if( m_data.get(i).m_name.equalsIgnoreCase(name) && m_data.get(i).m_source.equalsIgnoreCase(source) )
				return m_data.get(i);
		}
		return null;
	}
	
	// get the last inserted dimensions' name and source
	public synchronized void getLastInsertDimensions(Vector<String> sources, Vector<String> names) {
		if( m_lastInsertDimBegIndex==-1 || m_lastInsertDimEndIndex==-1 )
			return;
		for( int i = m_lastInsertDimBegIndex; i <= m_lastInsertDimEndIndex; i++ ) {
			sources.add( m_data.get(i).m_source );
			names.add( m_data.get(i).m_name );
		}
	}
	
	public synchronized void loadRealtimeConfigFromFile(String filename) {
		DataFormat.readRealtimeConfigFromFile(filename, m_data);
	}
}
