package org.michaels.s4a2;

import java.util.Calendar;

import android.database.Cursor;

public class FHSSchedule {
	public final static int LTYPE_LECTURE = 0;
	public final static int LTYPE_EXERCISE = 1;
	public final static int WEEK_EVEN = 2;
	public final static int WEEK_ODD = 1;
	public final static int WEEK_BOTH = 3;
	
	public static Event getNextEvent(){
		Calendar current = Calendar.getInstance();
		Calendar weekStart = (Calendar) current.clone();
		weekStart.add(Calendar.DATE, -(weekStart.get(Calendar.DAY_OF_WEEK)-1));
		weekStart.set(Calendar.HOUR_OF_DAY, 0);
		weekStart.set(Calendar.MINUTE, 0);
		weekStart.set(Calendar.SECOND, 0);
		weekStart.set(Calendar.MILLISECOND, 0);
		Cursor c = Data.db.rawQuery("SELECT id, start FROM Event e WHERE start > ? AND " +
				"(week = 3 OR week = ?) ORDER BY start",new String[]{
				(current.getTimeInMillis()-weekStart.getTimeInMillis())/1000+"",
				(current.get(Calendar.WEEK_OF_YEAR)%2 == 1 ? WEEK_ODD : WEEK_EVEN)+""
			});
		c.moveToFirst();
		Event rtn = new Event();
		rtn.nextOccurence = (Calendar) weekStart.clone();
		rtn.nextOccurence.add(Calendar.SECOND, c.getInt(c.getColumnIndex("start")));
		return rtn;
	}
	
	public static class Event {
		public Calendar nextOccurence;
	}
}
