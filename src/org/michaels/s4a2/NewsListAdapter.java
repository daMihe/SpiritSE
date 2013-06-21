package org.michaels.s4a2;

import java.util.ArrayList;

import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.TextView;

public class NewsListAdapter implements ListAdapter {

	ArrayList<DataSetObserver> m_observers;
	ArrayList<String> m_titles;
	ArrayList<Integer> m_ids;
	ArrayList<Boolean> m_readstates;
	
	public NewsListAdapter(){
		m_observers = new ArrayList<DataSetObserver>();
		m_titles = new ArrayList<String>();
		m_ids = new ArrayList<Integer>();
		m_readstates = new ArrayList<Boolean>();
		updatedData();
	}
	
	public void updatedData(){
		m_titles.clear();
		m_ids.clear();
		m_readstates.clear();
		Cursor c = Data.db.rawQuery(
				"SELECT id, title, readstate FROM News ORDER BY dateutc DESC",
				null);
		c.moveToFirst();
		while(!c.isAfterLast()){
			m_titles.add(c.getString(c.getColumnIndex("title")));
			m_ids.add(c.getInt(c.getColumnIndex("id")));
			m_readstates.add(c.getInt(c.getColumnIndex("readstate")) != 0);
			c.moveToNext();
		}
		c.close();
		for(DataSetObserver i:m_observers)
			i.onChanged();
	}
	
	@Override
	public int getCount() {
		return m_titles.size();
	}

	@Override
	public Object getItem(int position) {
		return m_titles.get(position);
	}

	@Override
	public long getItemId(int position) {
		return m_ids.get(position);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		TextView rtn;
		if(convertView instanceof TextView)
			rtn = (TextView) convertView;
		else {
			rtn = new TextView(parent.getContext());
		}
		
		rtn.setTextAppearance(parent.getContext(), android.R.attr.textAppearanceMedium);
		
		if(!m_readstates.get(position))
			rtn.setTypeface(null, Typeface.BOLD);
		else
			rtn.setTypeface(null, Typeface.NORMAL);
		
		rtn.setText(m_titles.get(position));
		return rtn;
	}

	@Override
	public int getItemViewType(int position) {
		return 0;
	}

	@Override
	public int getViewTypeCount() {
		return 1;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public boolean isEmpty() {
		return m_titles.isEmpty();
	}

	@Override
	public void registerDataSetObserver(DataSetObserver newObserver) {
		m_observers.add(newObserver);
	}

	@Override
	public void unregisterDataSetObserver(DataSetObserver oldObserver) {
		m_observers.remove(oldObserver);
	}

	@Override
	public boolean areAllItemsEnabled() {
		return true;
	}

	@Override
	public boolean isEnabled(int position) {
		return true;
	}
}
