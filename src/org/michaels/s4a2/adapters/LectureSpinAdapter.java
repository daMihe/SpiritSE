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

public class LectureSpinAdapter extends S4A2BaseAdapter {
	
	public static final long NONE = -1;
	public static final long ADD = -2;
	
	ArrayList<String> m_lectureStrings;
	ArrayList<Integer> m_lectureIds;
	Context m_context;
	
	public LectureSpinAdapter(Context context){
		m_lectureStrings = new ArrayList<String>();
		m_lectureIds = new ArrayList<Integer>();
		m_context = context;
		updateLectures();
	}
	
	public void updateLectures(){
		m_lectureStrings.clear();
		m_lectureIds.clear();
		
		Cursor c = Data.db.rawQuery("SELECT id, ltype, lecturer || ': ' || title AS lecture " +
				"FROM Lecture", null);
		c.moveToFirst();
		while(!c.isAfterLast()){
			m_lectureIds.add(c.getInt(c.getColumnIndex("id")));
			m_lectureStrings.add(c.getString(c.getColumnIndex("lecture"))+" "+
					m_context.getString(c.getInt(
							c.getColumnIndex("ltype")) == FHSSchedule.LTYPE_LECTURE ? 
									R.string.lecture : R.string.exercise));
			c.moveToNext();
		}
		c.close();
		
		notifyObservers();
	}
	
	@Override
	public int getCount() {
		return m_lectureStrings.size()+2;
	}

	@Override
	public Object getItem(int pos) {
		if(pos == 0)
			return m_context.getString(R.string.ase_none);
		if(pos == 1)
			return m_context.getString(R.string.ase_add_new_lecture);
		return m_lectureStrings.get(pos-2);
	}

	@Override
	public long getItemId(int pos) {
		if(pos == 0)
			return NONE;
		if(pos == 1)
			return ADD;
		return m_lectureIds.get(pos-2);
	}

	@Override
	public View getView(int pos, View reuseView, ViewGroup rootView) {
		TextView rtn = (TextView) reuseView;
		if(rtn == null){
			rtn = (TextView) LayoutInflater.from(rootView.getContext()).
					inflate(android.R.layout.simple_spinner_item, rootView, false);
		}
		rtn.setTag(getItemId(pos));
		rtn.setText((String) getItem(pos));
		return rtn;
	}
	
}