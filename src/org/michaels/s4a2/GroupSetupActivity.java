package org.michaels.s4a2;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.NumberPicker;
import android.widget.NumberPicker.OnValueChangeListener;
import android.widget.TextView;

public class GroupSetupActivity extends ListActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_list_layout);
		GroupListAdapter adapter = new GroupListAdapter();
		setListAdapter(adapter);
		getListView().setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int pos, final long id) {
				AlertDialog.Builder groupDialer = new AlertDialog.Builder(GroupSetupActivity.this);
				groupDialer.setTitle(String.format(getString(R.string.g_setgroupfor), 
						(String) getListAdapter().getItem(pos)));
				NumberPicker groupPicker = new NumberPicker(GroupSetupActivity.this);
				Cursor c = Data.db.rawQuery("SELECT max(egroup) as maxgroup, ugroup FROM " +
						"Event e LEFT OUTER JOIN Usergroup u ON e.lecture = u.lecture WHERE " +
						"e.lecture = ?", new String[]{ id+"" });
				c.moveToFirst();
				groupPicker.setMaxValue(c.getInt(c.getColumnIndex("maxgroup")));
				groupPicker.setMinValue(0);
				groupPicker.setValue(c.isNull(c.getColumnIndex("ugroup")) ? 0 : 
					c.getInt(c.getColumnIndex("ugroup")));
				c.close();
				groupPicker.setOnValueChangedListener(new OnValueChangeListener() {
					
					@Override
					public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
						Data.db.execSQL("INSERT OR REPLACE INTO Usergroup (lecture,ugroup) VALUES "+
								"(?,?)",new String[]{ id+"", newVal+"" });
					}
				});
				groupDialer.setView(groupPicker);
				groupDialer.create().show();
			}
		});
		
	}
	
	private static class GroupListAdapter extends BaseAdapter {
		
		ArrayList<String> m_strings;
		ArrayList<Integer> m_lectureIds;
		
		public GroupListAdapter(){
			m_strings = new ArrayList<String>();
			m_lectureIds = new ArrayList<Integer>();
			Cursor c = Data.db.rawQuery("SELECT DISTINCT l.id as id, " +
					"l.lecturer || ': ' || l.title as desc FROM Lecture l, Event e WHERE " +
					"l.id = e.lecture AND e.egroup IS NOT NULL AND e.egroup > 0", null);
			c.moveToFirst();
			while(!c.isAfterLast()){
				m_strings.add(c.getString(c.getColumnIndex("desc")));
				m_lectureIds.add(Integer.valueOf(c.getInt(c.getColumnIndex("id"))));
				c.moveToNext();
			}
			c.close();
		}

		@Override
		public int getCount() {
			return m_strings.size();
		}

		@Override
		public Object getItem(int index) {
			if(index >= m_lectureIds.size())
				return -1;
			return m_strings.get(index);
		}

		@Override
		public long getItemId(int index) {
			if(index >= m_lectureIds.size())
				return -1;
			return m_lectureIds.get(index);
		}

		@Override
		public View getView(int index, View reUseView, ViewGroup vGroup) {
			if(index >= m_lectureIds.size())
				return null;
			TextView rtn = (TextView) reUseView;
			if(rtn == null)
				rtn = new TextView(vGroup.getContext());
			
			rtn.setTextAppearance(vGroup.getContext(), android.R.attr.textAppearanceMedium);
			
			rtn.setText(m_strings.get(index));
			rtn.setTag(m_lectureIds.get(index));
			
			return rtn;
		}
		
	}

}
