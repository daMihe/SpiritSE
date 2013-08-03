package org.michaels.s4a2.adapters;

import java.util.ArrayList;

import org.michaels.s4a2.Data;
import org.michaels.s4a2.FHSSchedule;
import org.michaels.s4a2.R;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class EventSpinAdapter extends S4A2BaseAdapter {
	
	public static final long NONE = -1;
	public static final long ADD = -2;
	
	ArrayList<String> m_eventStrings;
	ArrayList<Integer> m_eventIds;
	Context m_context;
	long m_lectureId;
	
	public EventSpinAdapter(Context context, long id){
		m_eventStrings = new ArrayList<String>();
		m_eventIds = new ArrayList<Integer>();
		m_context = context;
		m_lectureId = id;
		updateEvents();
	}
	
	public void updateEvents(){
		m_eventStrings.clear();
		m_eventIds.clear();
		
		Cursor c = Data.db.rawQuery("SELECT id, week, start FROM Event WHERE lecture = ?", 
				new String[]{ m_lectureId+"" });
		c.moveToFirst();

		String[] dayNames = m_context.getResources().getStringArray(R.array.days);
		while(!c.isAfterLast()){
			m_eventIds.add(c.getInt(c.getColumnIndex("id")));
			
			int secSinceSunday = c.getInt(c.getColumnIndex("start"));
			int day = secSinceSunday / 86400;
			int hour = (secSinceSunday % 86400) / 3600;
			int min = (secSinceSunday % 3600) / 60;
			int weekTmp = c.getInt(c.getColumnIndex("week"));
			String week = m_context.getString(weekTmp == FHSSchedule.WEEK_BOTH ? R.string.all : 
				(weekTmp == FHSSchedule.WEEK_EVEN ? R.string.even : R.string.odd));
			m_eventStrings.add(String.format("%s, %s, %02d:%02d", dayNames[day], week, hour, min));
			
			c.moveToNext();
		}
		c.close();
	}
	
	@Override
	public int getCount() {
		return 2+m_eventIds.size();
	}

	@Override
	public Object getItem(int position) {
		if(position == 0)
			return m_context.getString(R.string.ase_none);
		if(position == 1)
			return m_context.getString(R.string.ase_add_new_event);
		return m_eventStrings.get(position-2);
	}

	@Override
	public long getItemId(int position) {
		if(position == 0)
			return NONE;
		if(position == 1)
			return ADD;
		return m_eventIds.get(position-2);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		TextView rtn = (TextView) convertView;
		if(rtn == null){
			rtn = (TextView) LayoutInflater.from(parent.getContext()).
					inflate(android.R.layout.simple_spinner_item, parent, false);
		}
		rtn.setTag(getItemId(position));
		rtn.setText((String) getItem(position));
		return rtn;
	}
	
}
