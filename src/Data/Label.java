/**
 * @author lixinghu@usc.edu
 * @since 2010/6/17
 * we want to allow user to label each dimension.
 * */

package Data;

import java.util.Calendar;

public class Label {
	public String m_meta;
	public int m_beg, m_end;	// m_beg,m_end should within the signal's range
	public String m_author;
	public Calendar m_date;
}
