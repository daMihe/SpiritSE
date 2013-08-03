package org.michaels.s4a2.adapters;

import java.util.ArrayList;

import android.database.DataSetObserver;
import android.widget.BaseAdapter;

public abstract class S4A2BaseAdapter extends BaseAdapter {

	protected ArrayList<DataSetObserver> m_observers;

	public S4A2BaseAdapter() {
		super();
		m_observers = new ArrayList<DataSetObserver>();
	}

	@Override
	public void registerDataSetObserver(DataSetObserver observer) {
		if(!m_observers.contains(observer))
			m_observers.add(observer);
	}

	@Override
	public void unregisterDataSetObserver(DataSetObserver observer) {
		if(m_observers.contains(observer))
			m_observers.remove(observer);
	}
	
	protected void notifyObservers(){
		for(DataSetObserver o:m_observers)
			o.onChanged();
	}

}