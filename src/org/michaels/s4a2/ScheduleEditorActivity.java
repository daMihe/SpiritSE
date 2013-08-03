package org.michaels.s4a2;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

public class ScheduleEditorActivity extends Activity implements OnItemClickListener {
	
	private LectureListAdapter m_listAdapter;
	
	@Override
	protected void onCreate(Bundle b){
		super.onCreate(b);
		setContentView(R.layout.activity_ase);
		ListView lv = (ListView) findViewById(R.id.ase_lecturelist);
		m_listAdapter = new LectureListAdapter(this);
		lv.setAdapter(m_listAdapter);
		lv.setOnItemClickListener(this);
		
		enableLectureSection(false);
	}
	
	private void enableLectureSection(boolean enable){
		findViewById(R.id.ase_lecture_title).setEnabled(enable);
		findViewById(R.id.ase_lecture_type_lecture).setEnabled(enable);
		findViewById(R.id.ase_lecture_type_exercise).setEnabled(enable);
		findViewById(R.id.ase_lecture_lecturer).setEnabled(enable);
		findViewById(R.id.ase_lecture_eventspinner).setEnabled(enable);
		findViewById(R.id.ase_lecture_eventbutton).setEnabled(enable);
	}
	
	private static class LectureListAdapter extends BaseAdapter {
		
		ArrayList<String> m_lectureStrings;
		ArrayList<Integer> m_lectureIds;
		ArrayList<DataSetObserver> m_observers;
		Context m_context;
		
		public LectureListAdapter(Context context){
			m_lectureStrings = new ArrayList<String>();
			m_lectureIds = new ArrayList<Integer>();
			m_observers = new ArrayList<DataSetObserver>();
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
			
			for(DataSetObserver o:m_observers)
				o.onChanged();
		}
		
		@Override
		public void registerDataSetObserver (DataSetObserver observer){
			if(!m_observers.contains(observer))
				m_observers.add(observer);
		}
		
		@Override
		public void unregisterDataSetObserver (DataSetObserver observer){
			if(m_observers.contains(observer))
				m_observers.remove(observer);
		}

		@Override
		public int getCount() {
			return m_lectureStrings.size();
		}

		@Override
		public Object getItem(int pos) {
			return m_lectureStrings.get(pos);
		}

		@Override
		public long getItemId(int pos) {
			return m_lectureIds.get(pos);
		}

		@Override
		public View getView(int pos, View reuseView, ViewGroup rootView) {
			TextView rtn = (TextView) reuseView;
			if(rtn == null)
				rtn = new TextView(rootView.getContext());
			rtn.setTextAppearance(rootView.getContext(), android.R.attr.textAppearanceMedium);
			rtn.setTag(m_lectureIds.get(pos));
			rtn.setText(m_lectureStrings.get(pos));
			return rtn;
		}
		
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int pos, final long id) {
		Cursor c = Data.db.rawQuery("SELECT title, ltype, lecturer FROM Lecture WHERE id = ?", 
				new String[]{id+""} );
		c.moveToFirst();
		
		((EditText) findViewById(R.id.ase_lecture_title)).setText(c.getString(c.getColumnIndex("title")));
		((EditText) findViewById(R.id.ase_lecture_title)).setOnFocusChangeListener(new OnFocusChangeListener() {
			
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if(hasFocus)
					return;
				Data.db.execSQL("UPDATE Lecture SET title = ? WHERE id = ?", new String[]{
						((EditText)v).getText().toString(), id+""
					});
				m_listAdapter.updateLectures();
			}
		});
		((EditText) findViewById(R.id.ase_lecture_lecturer)).setText(c.getString(c.getColumnIndex("lecturer")));
		if(c.getInt(c.getColumnIndex("ltype")) == FHSSchedule.LTYPE_LECTURE)
			((RadioButton) findViewById(R.id.ase_lecture_type_lecture)).setChecked(true);
		else
			((RadioButton) findViewById(R.id.ase_lecture_type_exercise)).setChecked(true);
		
		enableLectureSection(true);
		
		c.close();
	}
}
