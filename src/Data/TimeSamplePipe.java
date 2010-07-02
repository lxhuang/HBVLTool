/**
 * @author lixinghu@usc.edu
 * @since 2010/6/4
 * deal with the relationship between samples and time
 * e.g. get samples based on time, calculate time based on sample index, etc.
 * */

package Data;

import java.util.*;

public class TimeSamplePipe {
	public int m_samplerate;
	public long m_unit;	// milliseconds, the basic unit. samplerate is the number of samples in this unit
	public long m_start;	// milliseconds, the start time
	
	public TimeSamplePipe(int sr, Calendar unit, Calendar start) {
		m_samplerate = sr;
		m_unit = unit.getTimeInMillis();
		m_start = start.getTimeInMillis();
	}
	
	public TimeSamplePipe(int sr, long unit, long start) {
		m_samplerate = sr;
		m_unit = unit;
		m_start = start;
	}
	
	// why change?
	// I am imaging the m_start is a kind of meta info which the visualization code doesn't need to know.
	// when the visualization code is doing work, it takes any signals as if they were starting at 0 no matter whether it is or not.
	public int getSampleIndexFrom(Calendar c) {
		long t = c.getTimeInMillis();
//		return (int)( (double)(t - m_start) / m_unit * m_samplerate );
		return (int)( (double)t / m_unit * m_samplerate );
	}
	
	public int getSampleIndexFrom(double t) {
//		return (int)( (double)(t - m_start) / m_unit * m_samplerate );
		return (int)( (double)t / m_unit * m_samplerate );
	}
	
	// this is where m_start counts.
	// notes: getTimeFromSampleIndex is not reverse of getSampleIndexFrom
	public long getTimeFromSampleIndex(int n) {
		return (long)((double)n/m_samplerate*m_unit) + m_start;
	}
	
	public Calendar getCalendarTimeFromSampleIndex(int n) {
		long t = (long)((double)n/m_samplerate*m_unit) + m_start;
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(t);
		return c;
	}
	
	public static void main(String[] args) {
		TimeSamplePipe p = new TimeSamplePipe(30, 1000, 0);
		Calendar c = p.getCalendarTimeFromSampleIndex(0);
		System.out.println(c.getTime().toString());
	}
}
