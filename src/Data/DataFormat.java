/**
 * @author lixinghu@usc.edu
 * @since 2010/6/4
 * how to load data file
 * */

package Data;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.util.*;
import java.io.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import java.lang.reflect.Type;
import Util.Conversion;

public class DataFormat {
	public DataFormat() {}
	
	/**
	 * the correct file format:
	 * type: Continuous
	 * source: \\sfs\data\public\1.avi
	 * length: 9000
	 * features: facex, facey, gaze, smile
	 * min: 0, 0, 0, 0
	 * max: 100, 100, 100, 100
	 * the first three are mandatory, if the min and max are missing, we are going to calculate that while loading data
	 * ##### is used to separate file header and real data (in CSV format)
	 * */
	public static void loadContinuousDimensionsFromCSVFile(String filename, Vector<DimensionData> dimensions) {
		final String[] keywords = {"source", "length", "features", "min", "max"};
		final String separator = "######";
		boolean dataStart = false;
		
		String source = "";
		int totalLen = 0;
		Vector<String> featureNames = new Vector<String>();
		Vector<String> maxValues = new Vector<String>();
		Vector<String> minValues = new Vector<String>();
		float[] _maxValues = null;
		float[] _minValues = null;
		boolean firstLineOfData = true;
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(filename)));
			String textOfLine;
			while( (textOfLine = br.readLine()) != null ) {
				if( textOfLine.length() == 0 )
					continue;
				if( textOfLine.contains(separator) ) {	// the end of dimension description
					dataStart = true;
					
					// initialize dimensions
					for( int i = 0; i < featureNames.size(); i++ ) {
						DimensionData d = new DimensionData(totalLen, 0.0f, 0.0f, featureNames.get(i), DimensionType.CONTINUOUS, source);
						if( maxValues.isEmpty() == false )
							d.m_dataMax = Float.parseFloat(maxValues.get(i));
						if( minValues.isEmpty() == false )
							d.m_dataMin = Float.parseFloat(minValues.get(i));
						dimensions.add(d);
					}
					
					_maxValues = new float[featureNames.size()];
					_minValues = new float[featureNames.size()];
					continue;
				}
				
				if( dataStart ) {
					StringTokenizer st = new StringTokenizer(textOfLine, ",");
					Vector<String> tokens = new Vector<String>();
					while( st.hasMoreTokens() )
						tokens.add( st.nextToken().trim() );
					if( tokens.size() != dimensions.size() )
						throw new IOException("the number of dimensions doesn't match the dimension description");
					
					for( int i = 0; i < tokens.size(); i++ ) {
						float v = Float.parseFloat(tokens.get(i));
						if( firstLineOfData ) {
							_maxValues[i] = v;
							_minValues[i] = v;
						} else {
							if( _maxValues[i] < v ) _maxValues[i] = v;
							if( _minValues[i] > v ) _minValues[i] = v;
						}
						dimensions.get(i).add(v);
					}
				} else {
					// get the dimension description
					int pos = textOfLine.indexOf(":", 0);
					String kw = textOfLine.substring(0, pos).trim();
					
					if( kw.equalsIgnoreCase(keywords[0]) ) { // source
						source = textOfLine.substring(pos+1).trim();
					}
					else if( kw.equalsIgnoreCase(keywords[1]) ) { // length of data
						totalLen = Integer.parseInt(textOfLine.substring(pos+1).trim());
					}
					else if( kw.equalsIgnoreCase(keywords[2]) ) { // feature names
						StringTokenizer st = new StringTokenizer( textOfLine.substring(pos+1).trim(), "," );
						while( st.hasMoreTokens() ) {
							featureNames.add(st.nextToken().trim());
						}
					}
					else if( kw.equalsIgnoreCase(keywords[3]) ) { // minimum values
						StringTokenizer st = new StringTokenizer( textOfLine.substring(pos+1).trim(), "," );
						while( st.hasMoreTokens() ) {
							minValues.add(st.nextToken().trim());
						}
					}
					else if( kw.equalsIgnoreCase(keywords[4]) ) { // max values
						StringTokenizer st = new StringTokenizer( textOfLine.substring(pos+1).trim(), "," );
						while( st.hasMoreTokens() ) {
							maxValues.add(st.nextToken().trim());
						}
					}
				}
			}
			
			if( maxValues.isEmpty() ) {	// user doesn't specify the range of data
				for( int i = 0; i < dimensions.size(); i++ ) {
					dimensions.get(i).m_dataMax = _maxValues[i];
					dimensions.get(i).m_dataMin = _minValues[i];
				}
			}
			
			br.close();
		} catch ( Exception exp ) {
			exp.printStackTrace();
		}
	}
	
	/**
	 * the correct file format
	 * type: Discrete
	 * source: //sfs/data/public/1.avi
	 * length: 9000 (the total number of samples)
	 * features: transcription, smile
	 * ###### is used to separate the description from the data
	 * hello,1,2 (can only in samples)
	 * world,3,4
	 * ====== is used to separate different dimensions
	 * big smile,2,3
	 * small smile,3,4
	 * */
	public static void loadDiscreteEventsFrom(String filename, Vector<DimensionData> dimensions) {
		final String[] keywords = {"source", "length", "features"};
		final String descriptionSeparator = "######";
		final String featureSeparator = "======";
		
		boolean dataStart = false;
		String source = "";
		int totalLen = 0, dimensionIndex = 0;
		Vector<String> featureNames = new Vector<String>();
		Vector<String> events = new Vector<String>();
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(filename)));
			String textOfLine;
			while( (textOfLine = br.readLine()) != null ) {
				if( textOfLine.length() == 0 )
					continue;
				
				if( textOfLine.contains(descriptionSeparator) ) { // the end of dimension description
					for( int i = 0; i < featureNames.size(); i++ ) {
						DimensionData d = new DimensionData(totalLen, 0.0f, 0.0f, featureNames.get(i), DimensionType.EVENT, source);
						dimensions.add(d);
					}
					dataStart = true;
					continue;
				}
				if( textOfLine.contains(featureSeparator) ) {
					dimensions.get(dimensionIndex).setEvent(events);
					events.clear();
					dimensionIndex++;
					continue;
				}
				
				if( !dataStart ) {	// description
					int pos = textOfLine.indexOf(":", 0);
					String kw = textOfLine.substring(0, pos).trim();
					if( kw.equalsIgnoreCase(keywords[0]) ) { // source
						source = textOfLine.substring(pos+1).trim();
					}
					else if( kw.equalsIgnoreCase(keywords[1]) ) { // total length of samples
						totalLen = Integer.parseInt(textOfLine.substring(pos+1).trim());
					}
					else if( kw.equalsIgnoreCase(keywords[2]) ) { // feature names
						StringTokenizer st = new StringTokenizer(textOfLine.substring(pos+1).trim(), ",");
						while( st.hasMoreTokens() ) {
							featureNames.add( st.nextToken().trim() );
						}
					}
				}
				else {
					DimensionData d = dimensions.get(dimensionIndex);
					StringTokenizer st = new StringTokenizer(textOfLine, ",");
					
					int eventIndex = 0;
					String event = st.nextToken().trim();
					for(; eventIndex < events.size(); eventIndex++ ) {
						if( events.get(eventIndex).equalsIgnoreCase(event) ) {
							break;
						}
					}
					if( eventIndex == events.size() )
						events.add(event);
					
					String beg = st.nextToken().trim();
					String end = st.nextToken().trim();

					int beg_sample = Integer.parseInt(beg);
					int end_sample = Integer.parseInt(end);
					for( int i = beg_sample; i < end_sample; i++ )
						d.setAt(i, eventIndex);
				}
			}
			dimensions.get(dimensionIndex).setEvent(events);
		} catch ( Exception exp ) {
			exp.printStackTrace();
		}
	}
	
	// load dimension description from JSON string
	// when might be useful? for example, if we want to load data from a database, I image we should first load the 
	// description of the dimensions; if we want to load data from web service, it is also better to first load the 
	// description first.
	public static void loadContinuousDimensionsDescFromJSONString(String descs, Vector<DimensionDescription> dims) {
		try {
			Type collectionType = new TypeToken<Collection<DimensionDescription>>(){}.getType();
			List<DimensionDescription> dimensions = new LinkedList<DimensionDescription>();
			Gson gson = new Gson();
			dimensions = gson.fromJson(descs, collectionType);
			dims.addAll(dimensions);
		} catch(Exception exp) {
			exp.printStackTrace();
		}
	}
	
	// load dimension description from JSON file
	public static void loadContinuousDimensionsDescFromJSONFile(String filename, Vector<DimensionDescription> dims) {
		try {
			Type collectionType = new TypeToken<Collection<DimensionDescription>>(){}.getType();
			List<DimensionDescription> dimensions = new LinkedList<DimensionDescription>();
			Gson gson = new Gson();
			dimensions = gson.fromJson(new FileReader(new File(filename)), collectionType);
			dims.addAll(dimensions);
		} catch( Exception exp ) {
			exp.printStackTrace();
		}
	}
	
	// load data from string
	// for each sample, different dimensions should be separated by ,
	// different samples should be separated by \n
	public static void loadContinuousDimensionsDataFromCSVString(String data, Vector<DimensionData> dimensions) {
		try {
			StringTokenizer st = new StringTokenizer(data, "\n");
			Vector<String> dims = new Vector<String>();
			while( st.hasMoreTokens() ) {
				String token = st.nextToken();
				StringTokenizer st1 = new StringTokenizer(token, ",");
				while( st1.hasMoreTokens() ) {
					dims.add(st1.nextToken().trim());
				}
				
				if( dims.size() != dimensions.size() )
					throw new IOException("the number of dimensions doesn't match the description");
				
				for( int i = 0; i < dimensions.size(); i++ )
					dimensions.get(i).add( Float.parseFloat(dims.get(i)) );
				
				dims.clear();
			}
		} catch(Exception exp) {
			exp.printStackTrace();
		}
	}
	
	// load data from CSV file
	// first you should load dimension description so that we can initialize dimensions correctly.
	public static void loadContinuousDimensionsDataFromCSVFile(String filename, Vector<DimensionData> dims) {
		try {
			Vector<String> tokens = new Vector<String>();
			BufferedReader br = new BufferedReader(new FileReader(new File(filename)));
			String textOfLine;
			while( (textOfLine = br.readLine()) != null ) {
				if( textOfLine.length() == 0 )
					continue;
				StringTokenizer st = new StringTokenizer(textOfLine, ",");
				String token;
				while( st.hasMoreTokens() ) {
					token = st.nextToken().trim();
					tokens.add(token);
				}
				if( dims.size() != tokens.size() )
					throw new IOException("the number of dimensions don't match the dimension description file");
				for( int i = 0; i < dims.size(); i++ ) {
					float f = Float.parseFloat(tokens.get(i));
					dims.get(i).add(f);
				}
				tokens.clear();
			}
			br.close();
		} catch ( Exception exp ) {
			exp.printStackTrace();
		}
	}
	
	// load data from wave file (windows PCM format)
	public static int loadContinuousDimensionDataFromWavFile(String filename, Vector<DimensionData> dims) {
		try {
			File file = new File(filename);
			DataInputStream f = new DataInputStream(new FileInputStream(file));
			
			byte[] buf4 = new byte[4];
			byte[] buf2 = new byte[2];
			
			f.read(buf4);	// RIFF
			f.read(buf4);	// the size of whole file - 8
			f.read(buf4);	// WAVE
			f.read(buf4);	// fmt
			f.read(buf4);	// 16 (PCM)
			
			f.read(buf2);	// 1 (PCM)
			f.read(buf2);	// number of channels
			int channelNum = Conversion.convertToShort(buf2);
			
			f.read(buf4);	// sample rate
			int samplerate = Conversion.convertToInt_little_endian(buf4);
			
			f.read(buf4);	// byte rate
			f.read(buf2);	// block align
			f.read(buf2);	// bits per sample
			
			f.read(buf4);	// subchunk2ID
			f.read(buf4);	// subchunk2Size
			int dataLen = Conversion.convertToInt_little_endian(buf4);
			
			byte[] dat = new byte[dataLen];
			f.read(dat);
			
			short[] wav = new short[dataLen/2];
			for( int i = 0; i < dataLen/2; i++ )
				wav[i] = Conversion.convertToShort(dat, i*2);
			
			for( int i = 0; i < channelNum; i++ ) {
				DimensionData d = new DimensionData(dataLen/channelNum, 20000.0f, -20000.0f, "WavChannel"+Integer.toString(i+1), DimensionType.CONTINUOUS, file.getAbsolutePath());
				dims.add(d);
				dims.get(i).m_data = new float[dataLen/2/channelNum];
				for( int j = 0; j < dataLen/2/channelNum; j++ ) {
					if( wav[channelNum*j+i] > dims.get(i).m_dataMax )
						dims.get(i).m_dataMax = wav[channelNum*j+i];
					if( wav[channelNum*j+i] < dims.get(i).m_dataMin )
						dims.get(i).m_dataMin = wav[channelNum*j+i];
					dims.get(i).add( (float)(wav[channelNum*j+i]) );
				}
			}
			return samplerate;
		} catch(Exception exp) {
			exp.printStackTrace();
			return -1;
		}
	}
	
	// write the dimension descriptions to external file
	public static void writeDimensionsDescToJSONFile(String filename, Vector<DimensionDescription> dd) {
		Gson gson = new Gson();
		String json = gson.toJson(dd);
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(filename)));
			bw.write(json);
			bw.close();
		} catch (Exception exp) {
			exp.printStackTrace();
		}
	}
	
	/**
	 * we allow user to label each dimension. it is necessary for them to save their labels.
	 * the format:
	 * type: label
	 * source: \\sfs\data\public\1.avi
	 * name: smile (dimension's name)
	 * length: 6133 (dimension's length)
	 * author: lixinghu (who labeled the dimension)
	 * ######
	 * smile,1,2 (meta,beg,end)
	 * smile,3,4,time
	 * ...
	 * */
	public static void writeLabelOfDimensionDataToFile(DimensionData d, String filename) {
		try {
			final String separator = "######";
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(filename)));
			String textOfLine;
			
			textOfLine = "type: label";
			bw.write(textOfLine); bw.newLine();
			textOfLine = "source: " + d.m_source;
			bw.write(textOfLine); bw.newLine();
			textOfLine = "name: " + d.m_name;
			bw.write(textOfLine); bw.newLine();
			textOfLine = "length: " + Integer.toString(d.getLen());
			bw.write(textOfLine); bw.newLine();
			textOfLine = "author: " + d.m_labelAuthor;
			bw.write(textOfLine); bw.newLine();
			bw.write(separator); bw.newLine();
			for( int i = 0; i < d.m_labels.size(); i++ ) {
				textOfLine = d.m_labels.get(i).m_meta+","+Integer.toString(d.m_labels.get(i).m_beg)+","+Integer.toString(d.m_labels.get(i).m_end)+",";
				
				int month = d.m_labels.get(i).m_date.get(Calendar.MONTH);
				int day = d.m_labels.get(i).m_date.get(Calendar.DAY_OF_MONTH);
				int hour = d.m_labels.get(i).m_date.get(Calendar.HOUR_OF_DAY);
				int minute = d.m_labels.get(i).m_date.get(Calendar.MINUTE);
				int second = d.m_labels.get(i).m_date.get(Calendar.SECOND);
				
				textOfLine = textOfLine + Integer.toString(month+1)+"/"+Integer.toString(day)+" "
					+Integer.toString(hour)+":"+Integer.toString(minute)+":"+Integer.toString(second);
				bw.write(textOfLine); bw.newLine();
			}
			bw.close();
		} catch(Exception exp) {
			exp.printStackTrace();
		}
	}
	// load labels for this dimension from file
	// the most important parts in the ELAN xml file are 1. TIME_SLOT, 2. ALIGNABLE_ANNOTATION
	public static void readLabelFromELAN(String filename, Vector<String> dimensionDesc, Vector<Label> labels) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(filename);
			
			NodeList list = document.getElementsByTagName("ANNOTATION_DOCUMENT");
			if( list.getLength() == 0 ) {
				throw new IOException("Invalid ELAN file, missing AUTHOR");
			}
			String author = ((Element)list.item(0)).getAttribute("AUTHOR");
			
			list = document.getElementsByTagName("MEDIA_DESCRIPTOR");
			if( list.getLength() == 0 ) {
				throw new IOException("Invalid ELAN file, missing MEDIA_DESCRIPTOR");
			}
			String source = ((Element)list.item(0)).getAttribute("MEDIA_URL");
			String name = "elan";
			dimensionDesc.add(source);
			dimensionDesc.add(name);
			dimensionDesc.add(author);
			
			Vector<String> time = new Vector<String>();
			Vector<String> timeId = new Vector<String>();
			list = document.getElementsByTagName("TIME_SLOT");
			for( int i = 0; i < list.getLength(); i++ ) {
				Element slot = (Element)list.item(i);
				time.add( slot.getAttribute("TIME_VALUE") );
				timeId.add( slot.getAttribute("TIME_SLOT_ID") );
			}
			
			list = document.getElementsByTagName("ANNOTATION");
			for( int i = 0; i < list.getLength(); i++ ) {
				NodeList alignable = ((Element)list.item(i)).getElementsByTagName("ALIGNABLE_ANNOTATION");
				String index1 = ((Element)alignable.item(0)).getAttribute("TIME_SLOT_REF1");
				String index2 = ((Element)alignable.item(0)).getAttribute("TIME_SLOT_REF2");
				int beg = Integer.parseInt( time.get(timeId.indexOf(index1)) );
				int end = Integer.parseInt( time.get(timeId.indexOf(index2)) );
				
				Label l = new Label();
				l.m_author = author;
				l.m_beg = beg;
				l.m_end = end;
				l.m_meta = "elan";
				l.m_date = Calendar.getInstance();
				
				labels.add(l);
			}
			
		} catch(Exception exp) {
			exp.printStackTrace();
		}
	}
	/**
	 * @param dimensionDesc[out], must be 3 elements, first is source, second is name, the last is authorName
	 * @param labels[out] the loaded labels
	 * */
	public static void readLabelFromFile(String filename, Vector<String> dimensionDesc, Vector<Label> labels) {
		try {
			final String separator = "######";
			int maxLength = 0;
			boolean startOfData = false;
			String author = "";
			
			BufferedReader br = new BufferedReader(new FileReader(new File(filename)));
			String textOfLine;
			while( (textOfLine = br.readLine()) != null ) {
				textOfLine = textOfLine.trim();
				if( textOfLine.length() == 0 )
					continue;
				
				if( textOfLine.contains(separator) ) {
					startOfData = true;
					continue;
				}
				
				if( !startOfData ) {
					String kw = textOfLine.substring(0, textOfLine.indexOf(":"));
					String val = textOfLine.substring(textOfLine.indexOf(":")+1).trim();
					if( kw.equalsIgnoreCase("type") ) {
						if( val.equalsIgnoreCase("label") == false )
							throw new IOException("the loaded file is not label file");
					}
					else if( kw.equalsIgnoreCase("source") ) {
						dimensionDesc.add(val);
					}
					else if( kw.equalsIgnoreCase("name") ) {
						dimensionDesc.add(val);
					}
					else if( kw.equalsIgnoreCase("length") ) {
						maxLength = Integer.parseInt(val);
					}
					else if( kw.equalsIgnoreCase("author") ) {
						dimensionDesc.add(val);
						author = val;
					}
				}
				else {
					StringTokenizer st = new StringTokenizer(textOfLine,",");
					String meta = st.nextToken().trim();
					int beg = Integer.parseInt(st.nextToken().trim());
					int end = Integer.parseInt(st.nextToken().trim());
					String date = st.nextToken();
					if( end >= maxLength || beg < 0 || beg >= end )
						throw new IOException("some label is out of bound");
					
					Label l = new Label();
					l.m_author = author;
					l.m_beg = beg;
					l.m_end = end;
					l.m_meta = meta;
					
					int month = Integer.parseInt( date.substring(0,date.indexOf("/")) );
					int day = Integer.parseInt( date.substring(date.indexOf("/")+1, date.indexOf(" ")) );
					int hour = Integer.parseInt( date.substring(date.indexOf(" ")+1, date.indexOf(":")) );
					int minute = Integer.parseInt( date.substring(date.indexOf(":")+1, date.lastIndexOf(":")) );
					int second = Integer.parseInt( date.substring(date.lastIndexOf(":")+1) );
					
					Calendar c = Calendar.getInstance();
					c.set(Calendar.MONTH, month);
					c.set(Calendar.DAY_OF_MONTH, day);
					c.set(Calendar.HOUR, hour);
					c.set(Calendar.MINUTE, minute);
					c.set(Calendar.SECOND, second);
					l.m_date = c;
					
					labels.add(l);
				}
			}
			br.close();
		} catch(Exception exp) {
			exp.printStackTrace();
		}
	}
	public static void readLabelOfDimensionDataFromFile(DimensionData d, String filename) {
		try {
			final String separator = "######";
			int maxLength = 0;
			boolean startOfData = false;
			
			BufferedReader br = new BufferedReader(new FileReader(new File(filename)));
			String textOfLine;
			while( (textOfLine = br.readLine()) != null ) {
				textOfLine = textOfLine.trim();
				if( textOfLine.length() == 0 )
					continue;
				
				if( textOfLine.contains(separator) ) {
					startOfData = true;
					continue;
				}
				
				if( !startOfData ) {
					String kw = textOfLine.substring(0, textOfLine.indexOf(":"));
					String val = textOfLine.substring(textOfLine.indexOf(":")+1).trim();
					if( kw.equalsIgnoreCase("type") ) {
						if( val.equalsIgnoreCase("label") == false )
							throw new IOException("the loaded file is not label file");
					}
					else if( kw.equalsIgnoreCase("source") ) {
						if( val.equalsIgnoreCase(d.m_source) == false )
							throw new IOException("the loaded label file is not for this dimension. should be "+d.m_source+" "+d.m_name);
					}
					else if( kw.equalsIgnoreCase("name") ) {
						if( val.equalsIgnoreCase(d.m_name) == false )
							throw new IOException("the loaded label file is not for this dimension. should be "+d.m_source+" "+d.m_name);
					}
					else if( kw.equalsIgnoreCase("length") ) {
						maxLength = Integer.parseInt(val);
					}
					else if( kw.equalsIgnoreCase("author") ) {
						d.m_labelAuthor = val;
					}
				}
				else {
					StringTokenizer st = new StringTokenizer(textOfLine,",");
					String meta = st.nextToken().trim();
					int beg = Integer.parseInt(st.nextToken().trim());
					int end = Integer.parseInt(st.nextToken().trim());
					String date = st.nextToken();
					if( end >= maxLength || beg < 0 || beg >= end )
						throw new IOException("some label is out of bound");
					
					d.addLabel(meta, beg, end, date);
				}
			}
			br.close();
		} catch(Exception exp) {
			exp.printStackTrace();
		}
	}
	
	// before start listening to the port, we need to know the configuration for each dimension
	// the file format should be (the sequence is really IMPORTANT)
	//	featureName,featureName,featureName,...
	//	maxValue,maxValue,maxValue...
	//	minValue,minValue,minValue...
	//	len,len,len...
	//	bufLen,bufLen,bufLen...
	//	bufFilename,bufFilename,bufFilename,...	// if the length of data is large than len, we store the extra one in buffer, and if the buffer is full, we save it to file
	//	
	public static void readRealtimeConfigFromFile(String filename, Vector<DimensionData> dims) {
		try {
			BufferedReader br = new BufferedReader( new FileReader(new File(filename)) );
			String textOfLine;
			
			// each element is for one dimension
			Vector<Vector<String>> config = new Vector<Vector<String>>();
			
			while( (textOfLine = br.readLine()) != null ) {
				StringTokenizer st = new StringTokenizer(textOfLine,",");
				Vector<String> c = new Vector<String>();
				while( st.hasMoreTokens() ) {
					c.add(st.nextToken().trim());
				}
				
				if( config.isEmpty() ) {
					for( int i = 0; i < c.size(); i++ ) {
						Vector<String> d = new Vector<String>();
						d.add(c.get(i));
						config.add(d);
					}
				}
				else {
					for( int i = 0; i < c.size(); i++ ) {
						config.get(i).add( c.get(i) );
					}
				}
			}
			br.close();
			
			for( int i = 0; i < config.size(); i++ ) {
				String name = config.get(i).get(0);
				float max = Float.parseFloat(config.get(i).get(1));
				float min = Float.parseFloat(config.get(i).get(2));
				int len = Integer.parseInt(config.get(i).get(3));
				int bufLen = Integer.parseInt(config.get(i).get(4));
				String bufFilename = config.get(i).get(5);
				
				DimensionData dd = new DimensionData(len, max, min, name, DimensionType.CONTINUOUS, bufLen, bufFilename, "");
				dims.add(dd);
			}
		}catch(Exception exp) {
			exp.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
//		Vector<Label> labels = new Vector<Label>();
//		Vector<String> dimensionDesc = new Vector<String>();
//		DataFormat.readLabelFromELAN("AGN02F0.xml", dimensionDesc, labels);
		
//		Vector<DimensionData> dims = new Vector<DimensionData>();
//		DataFormat.loadContinuousDimensionDataFromWavFile("test.wav", dims);
		
		Vector<DimensionDescription> dims = new Vector<DimensionDescription>();
//		DataFormat.loadContinuousDimensionsDescFromJSONFile("test.txt", dims);
		DataFormat.writeDimensionsDescToJSONFile("test.txt", dims);
	}
}
