/*
Copyright (c) 2008 Kevin Lundberg <kevinrlundberg@gmail.com>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
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

	private static final long TIME_DIVIDEND = 1000; // number to divide by for converting from milliseconds.

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
		switch (methodId) {
		case DATE: return date(cf, nargs);
		case DATEDIFF: return datediff(cf, nargs);
		case TIME: return time(cf, nargs);
		default: throw new RuntimeException("Undefined method called on os.");
		}
	}

	private int time(LuaCallFrame cf, int nargs) {
		if (nargs == 0) {
			Date d = new Date();
			cf.push(LuaState.toDouble(((d.getTime() / TIME_DIVIDEND))));
		} else if (nargs == 1) {
			Object o = cf.get(0);
			if (!(o instanceof LuaTable)) {
				throw new RuntimeException("Argument given to time() is not a table");
			}
			Date d = getDateFromTable((LuaTable)o);
			cf.push(LuaState.toDouble(d.getTime() / TIME_DIVIDEND));
		} else {
			throw new RuntimeException("time() accepts no more than argument.");
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
			cf.push(getdate("%c"));
		} else if (nargs == 1) {
			cf.push(getdate(BaseLib.rawTostring(cf.get(0))));
		} else if (nargs == 2) {
			cf.push(getdate(BaseLib.rawTostring(cf.get(0)), BaseLib.rawTonumber(cf.get(1)).longValue() * TIME_DIVIDEND));
		} else {
			// invalid argument list
			return 0;
		}
		return 1;
	}

	public static Object getdate(String format) {
		return getdate(format, new Date().getTime());
	}

	public static Object getdate(String format, long time) {
		//boolean universalTime = format.startsWith("!");
		Date d = new Date(time);
		if (format.indexOf("*t") > -1) {
			LuaTable timeTable = getTimeTable(d);
			return timeTable;
		} else {
			// return a formatted string
			// TODO: handle date formatting instead of just returning the date.toString()
			return d.toString();
		}
	}

	public static LuaTable getTimeTable(Date d) {
		LuaTable time = new LuaTable();
		Calendar c = Calendar.getInstance();
		c.setTime(d);
		time.rawset(YEAR, LuaState.toDouble(c.get(Calendar.YEAR)));
		time.rawset(MONTH, LuaState.toDouble(c.get(Calendar.MONTH)+1));
		time.rawset(DAY, LuaState.toDouble(c.get(Calendar.DAY_OF_MONTH)));
		time.rawset(HOUR, LuaState.toDouble(c.get(Calendar.HOUR_OF_DAY)));
		time.rawset(MIN, LuaState.toDouble(c.get(Calendar.MINUTE)));
		time.rawset(SEC, LuaState.toDouble(c.get(Calendar.SECOND)));
		time.rawset(WDAY, LuaState.toDouble(c.get(Calendar.DAY_OF_WEEK)));
		time.rawset(YDAY, LuaState.toDouble(getDayOfYear(c)));
		return time;
	}

	private static int getDayOfYear(Calendar c) {
		int daysPerMonth[] = new int[12];
		daysPerMonth[Calendar.JANUARY] = 31;
		daysPerMonth[Calendar.FEBRUARY] = (isLeapYear(c.get(Calendar.YEAR)) ? 29 : 28);
		daysPerMonth[Calendar.MARCH] = 31;
		daysPerMonth[Calendar.APRIL] = 30;
		daysPerMonth[Calendar.MAY] = 31;
		daysPerMonth[Calendar.JUNE] = 30;
		daysPerMonth[Calendar.JULY] = 31;
		daysPerMonth[Calendar.AUGUST] = 31;
		daysPerMonth[Calendar.SEPTEMBER] = 30;
		daysPerMonth[Calendar.OCTOBER] = 31;
		daysPerMonth[Calendar.NOVEMBER] = 30;
		daysPerMonth[Calendar.DECEMBER] = 31;

		int days = 0;
		int numberOfMonths = c.get(Calendar.MONTH);
		for (int i = Calendar.JANUARY; i < numberOfMonths; i++) {
			days += daysPerMonth[i];
		}
		days += c.get(Calendar.DAY_OF_MONTH);
		return days;
	}

	private static boolean isLeapYear(int year) {
		return year % 4 == 0 && year % 100 != 0 && year % 400 == 0;
	}

	/**
	 * converts the relevant fields in the given luatable to a Date object.
	 * @param time LuaTable with entries for year month and day, and optionally hour/min/sec
	 * @return a date object representing the date frim the luatable.
	 */
	public static Date getDateFromTable(LuaTable time) {
		Calendar c = Calendar.getInstance();
		c.set(Calendar.YEAR, (int)LuaState.fromDouble(time.rawget(YEAR)));
		c.set(Calendar.MONTH, (int)LuaState.fromDouble(time.rawget(MONTH))-1);
		c.set(Calendar.DAY_OF_MONTH, (int)LuaState.fromDouble(time.rawget(DAY)));
		Object hour = time.rawget(HOUR);
		Object minute = time.rawget(MIN);
		Object seconds = time.rawget(SEC);
		//Object isDst = time.rawget(ISDST);
		if (hour != null) {
			c.set(Calendar.HOUR_OF_DAY, (int)LuaState.fromDouble(hour));
		} else {
			c.set(Calendar.HOUR_OF_DAY, 0);
		}
		if (minute != null) {
			c.set(Calendar.MINUTE, (int)LuaState.fromDouble(minute));
		} else {
			c.set(Calendar.MINUTE, 0);
		}
		if (seconds != null) {
			c.set(Calendar.SECOND, (int)LuaState.fromDouble(seconds));
		} else {
			c.set(Calendar.SECOND, 0);
		}
		// TODO: daylight savings support (is it possible?)
		return c.getTime();
	}
}
