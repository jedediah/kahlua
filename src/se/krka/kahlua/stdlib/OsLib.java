/*
Copyright(c) 2008 Kevin Lundberg <kevinrlundberg@gmail.com>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files(the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/

package se.krka.kahlua.stdlib;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import se.krka.kahlua.vm.JavaFunction;
import se.krka.kahlua.vm.LuaCallFrame;
import se.krka.kahlua.vm.LuaState;
import se.krka.kahlua.vm.LuaTable;

public class OsLib implements JavaFunction {
	private static final int DATE = 0;
	private static final int DATEDIFF = 1;
	private static final int TIME = 2;

	private static final int NUM_FUNCS = 3;

	private static String[] funcnames;
	private static OsLib[] funcs;

	static {
		funcnames = new String[NUM_FUNCS];
		funcnames[DATE] = "date";
		funcnames[DATEDIFF] = "datediff";
		funcnames[TIME] = "time";

		funcs = new OsLib[NUM_FUNCS];
		for (int i = 0; i < NUM_FUNCS; i++) {
			funcs[i] = new OsLib(i);
		}
	}

	public static void register(LuaState state) {
		LuaTable os = new LuaTable();
		state.environment.rawset("os", os);

		for (int i = 0; i < NUM_FUNCS; i++) {
			os.rawset(funcnames[i], funcs[i]);
		}
	}

	public static final long TIME_DIVIDEND = 1000; // number to divide by for converting from milliseconds.

	private static final String YEAR = "year";
	private static final String MONTH = "month";
	private static final String DAY = "day";
	private static final String HOUR = "hour";
	private static final String MIN = "min";
	private static final String SEC = "sec";
	private static final String WDAY = "wday";
	private static final String YDAY = "yday";
	//private static final String ISDST = "isdst";

	private int methodId;
	private OsLib(int methodId) {
		this.methodId = methodId;
	}

	public int call(LuaCallFrame cf, int nargs) {
		switch(methodId) {
		case DATE: return date(cf, nargs);
		case DATEDIFF: return datediff(cf, nargs);
		case TIME: return time(cf, nargs);
		default: throw new RuntimeException("Undefined method called on os.");
		}
	}

	private int time(LuaCallFrame cf, int nargs) {
		if (nargs == 0) {
			cf.push(LuaState.toDouble(((Calendar.getInstance().getTime().getTime() / TIME_DIVIDEND))));
		} else {
			Object o = cf.get(0);
			if (!(o instanceof LuaTable)) {
				throw new RuntimeException("Argument given to time() is not a table");
			}
			Date d = getDateFromTable((LuaTable)o);
			cf.push(LuaState.toDouble(d.getTime() / TIME_DIVIDEND));
		}
		return 1;
	}

	private int datediff(LuaCallFrame cf, int nargs) {
		long t1 = BaseLib.rawTonumber(cf.get(0)).longValue();
		long t2 = BaseLib.rawTonumber(cf.get(1)).longValue();
		cf.push(LuaState.toDouble(t2-t1));
		return 1;
	}

	private int date(LuaCallFrame cf, int nargs) {
		if (nargs == 0) {
			return cf.push(getdate("%c"));
		} else if (nargs == 1) {
			return cf.push(getdate(BaseLib.rawTostring(cf.get(0))));
		} else {
			return cf.push(getdate(BaseLib.rawTostring(cf.get(0)), BaseLib.rawTonumber(cf.get(1)).longValue() * TIME_DIVIDEND));
		}
	}

	public static Object getdate(String format) {
		return getdate(format, Calendar.getInstance().getTime().getTime());
	}

	public static Object getdate(String format, long time) {
		//boolean universalTime = format.startsWith("!");
		Calendar calendar = null;
		int si = 0;
        if (format.charAt(si) == '!') { // UTC?
            calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            si++;  // skip `!'
        } else {
            calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        }
        calendar.setTime(new Date(time));

        if (calendar == null) { // invalid calendar?
            return null;
        } else if (format.substring(si, 2 + si).equals("*t")) {
        	return getTimeTable(calendar);
        } else {
            StringBuffer buffer = new StringBuffer();
            for (int stringIndex = 0; stringIndex < format.length(); stringIndex ++) {
                if (format.charAt(stringIndex) != '%' || 
               		format.charAt(stringIndex + 1) == '\0') { // no conversion specifier?
                    buffer.append(format.charAt(stringIndex));
                }
                else {
                	++stringIndex;
                    buffer.append(strftime(format.charAt(stringIndex), calendar));
                }
            }
            return buffer.toString();
        }
	}
	
	public static String strftime(char format, Calendar cal) {
		int dayOfMonth, month;
        switch(format) {
            case 'a':
            	dayOfMonth = cal.get(Calendar.DAY_OF_WEEK);
            	switch(dayOfMonth) {
            	case Calendar.MONDAY: return "Mon";
            	case Calendar.TUESDAY: return "Tue";
            	case Calendar.WEDNESDAY: return "Wed";
            	case Calendar.THURSDAY: return "Thu";
            	case Calendar.FRIDAY: return "Fri";
            	case Calendar.SATURDAY: return "Sat";
            	case Calendar.SUNDAY: return "Sun";	
            	}
            case 'A':
                dayOfMonth = cal.get(Calendar.DAY_OF_WEEK);
                switch(dayOfMonth) {
                case Calendar.MONDAY: return "Monday";
                case Calendar.TUESDAY: return "Tuesday";
                case Calendar.WEDNESDAY: return "Wednesday";
                case Calendar.THURSDAY: return "Thursday";
                case Calendar.FRIDAY: return "Friday";
                case Calendar.SATURDAY: return "Saturday";
                case Calendar.SUNDAY: return "Sunday";	
                }
            case 'b':
                month = cal.get(Calendar.MONTH);
                switch(month) {
                case Calendar.JANUARY: return "Jan";
                case Calendar.FEBRUARY: return "Feb";
                case Calendar.MARCH: return "Mar";
                case Calendar.APRIL: return "Apr";
                case Calendar.MAY: return "May";
                case Calendar.JUNE: return "Jun";
                case Calendar.JULY: return "Jul";
                case Calendar.AUGUST: return "Aug";
                case Calendar.SEPTEMBER: return "Sep";
                case Calendar.OCTOBER: return "Oct";
                case Calendar.NOVEMBER: return "Nov";
                case Calendar.DECEMBER: return "Dec";	
                }
            case 'B':
            	month = cal.get(Calendar.MONTH);
            	switch(month) {
            	case Calendar.JANUARY: return "January";
            	case Calendar.FEBRUARY: return "February";
            	case Calendar.MARCH: return "March";
            	case Calendar.APRIL: return "April";            	
            	case Calendar.MAY: return "May";
            	case Calendar.JUNE: return "June";
            	case Calendar.JULY: return "July";
            	case Calendar.AUGUST: return "August";
            	case Calendar.SEPTEMBER: return "September";
            	case Calendar.OCTOBER: return "October";
            	case Calendar.NOVEMBER: return "November";
            	case Calendar.DECEMBER: return "December";	
                }
            case 'c': return cal.getTime().toString();
            case 'd': return Integer.toString(cal.get(Calendar.DAY_OF_MONTH));
            case 'H': return Integer.toString(cal.get(Calendar.HOUR_OF_DAY));
            case 'I': return Integer.toString(cal.get(Calendar.HOUR));
            case 'j': return Integer.toString(getDayOfYear(cal));
            case 'm': return Integer.toString(cal.get(Calendar.MONTH) + 1);
            case 'M': return Integer.toString(cal.get(Calendar.MINUTE));
            case 'p': return cal.get(Calendar.AM_PM) == Calendar.AM ? "AM" : "PM";
            case 'S': return Integer.toString(cal.get(Calendar.SECOND));
            case 'U': return Integer.toString(getWeekOfYear(cal, true));
            case 'w': return Integer.toString(cal.get(Calendar.DAY_OF_WEEK) - 1);
            case 'W': return Integer.toString(getWeekOfYear(cal, false));
            case 'x':
                String str = Integer.toString(cal.get(Calendar.YEAR));
                return Integer.toString(cal.get(Calendar.MONTH)) + "/" + Integer.toString(cal.get(Calendar.DAY_OF_MONTH)) +
            			"/" + str.substring(2, str.length());
            case 'X': return Integer.toString(cal.get(Calendar.HOUR_OF_DAY)) + ":" + Integer.toString(cal.get(Calendar.MINUTE)) +
                		":" + Integer.toString(cal.get(Calendar.SECOND));
            case 'y': return Integer.toString(cal.get(Calendar.YEAR)).substring(2);
            case 'Y': return Integer.toString(cal.get(Calendar.YEAR));
            case 'Z': return cal.getTimeZone().getID();                    
        }
        return null; // bad input format.
    }

	public static LuaTable getTimeTable(Calendar c) {
		LuaTable time = new LuaTable();
		time.rawset(YEAR, LuaState.toDouble(c.get(Calendar.YEAR)));
		time.rawset(MONTH, LuaState.toDouble(c.get(Calendar.MONTH)+1));
		time.rawset(DAY, LuaState.toDouble(c.get(Calendar.DAY_OF_MONTH)));
		time.rawset(HOUR, LuaState.toDouble(c.get(Calendar.HOUR_OF_DAY)));
		time.rawset(MIN, LuaState.toDouble(c.get(Calendar.MINUTE)));
		time.rawset(SEC, LuaState.toDouble(c.get(Calendar.SECOND)));
		time.rawset(WDAY, LuaState.toDouble(c.get(Calendar.DAY_OF_WEEK)));
		time.rawset(YDAY, LuaState.toDouble(getDayOfYear(c)));
		//time.rawset(ISDST, null);
		return time;
	}

	public static int getDayOfYear(Calendar c) {		
		Calendar c2 = Calendar.getInstance();
        c2.setTime(c.getTime());
        c2.set(Calendar.MONTH, Calendar.JANUARY);
        c2.set(Calendar.DAY_OF_MONTH, 1);
        long diff =(c.getTime().getTime() - c2.getTime().getTime());

        Double d = new Double((double) diff /(double)(1000 * 24 * 60 * 60));
        if (d.doubleValue() - d.intValue() != 0) {
            return d.intValue() + 1;
        } else {
            return d.intValue();
        }
	}
	
	public static int getWeekOfYear(Calendar c, boolean weekStartsSunday) {
        Calendar c2 = Calendar.getInstance();
        c2.setTime(c.getTime());
        c2.set(Calendar.MONTH, 0);
        c2.set(Calendar.DAY_OF_MONTH, 1);
        int dayOfWeek = c2.get(Calendar.DAY_OF_WEEK);
        if (weekStartsSunday) {
            if (dayOfWeek != Calendar.SUNDAY) {
                c2.set(Calendar.DAY_OF_MONTH,(7 - dayOfWeek) + 1);
            }
        }
        else {
            if (dayOfWeek != Calendar.MONDAY) {
                c2.set(Calendar.DAY_OF_MONTH,(7 - dayOfWeek + 1) + 1);
            }
        }
        long diff =(Calendar.getInstance().getTime().getTime() - c2.getTime().getTime());
        
        double g =(double) diff /(double)(1000 * 24 * 60 * 60 * 7);

        Double d = new Double(g);
        if (d.doubleValue() - d.intValue() != 0) {
            return d.intValue() + 1;
        }
        else {
            return d.intValue();
        }
    }

	/**
	 * converts the relevant fields in the given luatable to a Date object.
	 * @param time LuaTable with entries for year month and day, and optionally hour/min/sec
	 * @return a date object representing the date frim the luatable.
	 */
	public static Date getDateFromTable(LuaTable time) {
		Calendar c = Calendar.getInstance();
		c.set(Calendar.YEAR,(int)LuaState.fromDouble(time.rawget(YEAR)));
		c.set(Calendar.MONTH,(int)LuaState.fromDouble(time.rawget(MONTH))-1);
		c.set(Calendar.DAY_OF_MONTH,(int)LuaState.fromDouble(time.rawget(DAY)));
		Object hour = time.rawget(HOUR);
		Object minute = time.rawget(MIN);
		Object seconds = time.rawget(SEC);
		//Object isDst = time.rawget(ISDST);
		if (hour != null) {
			c.set(Calendar.HOUR_OF_DAY,(int)LuaState.fromDouble(hour));
		} else {
			c.set(Calendar.HOUR_OF_DAY, 0);
		}
		if (minute != null) {
			c.set(Calendar.MINUTE,(int)LuaState.fromDouble(minute));
		} else {
			c.set(Calendar.MINUTE, 0);
		}
		if (seconds != null) {
			c.set(Calendar.SECOND,(int)LuaState.fromDouble(seconds));
		} else {
			c.set(Calendar.SECOND, 0);
		}
		// TODO: daylight savings support(is it possible?)
		return c.getTime();
	}
}
