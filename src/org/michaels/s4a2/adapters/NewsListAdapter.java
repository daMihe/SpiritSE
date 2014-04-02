package org.michaels.s4a2.adapters;

import java.util.ArrayList;

import org.michaels.s4a2.Data;

import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.TextView;

public class NewsListAdapter implements ListAdapter {

	ArrayList<DataSetObserver> m_observers;
	ArrayList<String> m_titles;
	ArrayList<Integer> m_ids;
	ArrayList<Boolean> m_readstates;
	ArrayList<String> m_newsgroups;
	
	public NewsListAdapter(){
		m_observers = new ArrayList<DataSetObserver>();
		m_titles = new ArrayList<String>();
		m_ids = new ArrayList<Integer>();
		m_readstates = new ArrayList<Boolean>();
		m_newsgroups = new ArrayList<String>();
		updatedData();
	}
	
	public void updatedData(){
		m_titles.clear();
		m_ids.clear();
		m_readstates.clear();
		Cursor c;
		if(Data.preferences.contains(Data.PREF_NEWS_FILTER))
			c = Data.db.rawQuery("SELECT id, title, readstate FROM News, Newsgroup " +
					"WHERE id = News_id AND ngroup = ? ORDER BY dateutc DESC",
					new String[]{ Data.preferences.getString(Data.PREF_NEWS_FILTER,"") });
		else
			c = Data.db.rawQuery("SELECT id, title, readstate FROM News ORDER BY dateutc DESC",
					null);
		c.moveToFirst();
		while(!c.isAfterLast()){
			m_titles.add(c.getString(c.getColumnIndex("title")));
			m_ids.add(c.getInt(c.getColumnIndex("id")));
			m_readstates.add(c.getInt(c.getColumnIndex("readstate")) != 0);
			
			Cursor c2 = Data.db.rawQuery("SELECT ngroup FROM Newsgroup WHERE News_id = ?", 
					new String[]{c.getInt(c.getColumnIndex("id"))+""});
			c2.moveToFirst();
			String newsgroup = "";
			while(!c2.isAfterLast()){
				newsgroup += (newsgroup.isEmpty() ? "" : ", ")+c2.getString(c2.getColumnIndex("ngroup"));
				c2.moveToNext();
			}
			c2.close();
			m_newsgroups.add(newsgroup);
			
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
		View rtn;
		if(convertView != null)
			rtn = convertView;
		else {
			rtn = LayoutInflater.from(parent.getContext()).
					inflate(android.R.layout.two_line_list_item, parent, false);
		}
		
		TextView mainline = (TextView) rtn.findViewById(android.R.id.text1);
		if(!m_readstates.get(position))
			mainline.setTypeface(null, Typeface.BOLD);
		else
			mainline.setTypeface(null, Typeface.NORMAL);		
		mainline.setText(m_titles.get(position));
		
		TextView subline = (TextView) rtn.findViewById(android.R.id.text2);
		subline.setAlpha(0.6f);
		subline.setText(m_newsgroups.get(position));
		
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
