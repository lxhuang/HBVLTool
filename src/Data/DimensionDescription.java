package Data;

/**
 * @author lixinghu@usc.edu
 * @since 2010/6/4
 * the dimension description
 * users can save their description of dimensions into a JSON file, in order to 
 * parse the JSON file using gson, we need to define a class
 * */

public class DimensionDescription {
	public String m_name;
	public int m_length;
	public int m_max, m_min;
	public String m_source;
}
