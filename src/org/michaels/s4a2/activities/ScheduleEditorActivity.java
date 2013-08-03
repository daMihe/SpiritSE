package org.michaels.s4a2.activities;

import org.michaels.s4a2.Data;
import org.michaels.s4a2.FHSSchedule;
import org.michaels.s4a2.R;
import org.michaels.s4a2.adapters.EventSpinAdapter;
import org.michaels.s4a2.adapters.LectureSpinAdapter;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.CheckBox;

public class ScheduleEditorActivity extends Activity {
	
	private LectureSpinAdapter m_listAdapter;
	private long m_lectureId;
	
	@Override
	protected void onCreate(Bundle b){
		super.onCreate(b);
		setContentView(R.layout.activity_ase);
		prepareLectureSpinner();
		prepareEventDaySpinner();
	}
	
	private void prepareEventSpinner(long id){
		final EventSpinAdapter esa = new EventSpinAdapter(this, id);
		final Spinner eventSpinner = (Spinner) findViewById(R.id.ase_lecture_eventspinner);
		eventSpinner.setAdapter(esa);
		eventSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int pos, final long id) {
				if(id == EventSpinAdapter.NONE){
					enableEventSection(false);
					return;
				}
				if(id == EventSpinAdapter.ADD){
					ContentValues emptyEvent = new ContentValues();
					emptyEvent.put("week", FHSSchedule.WEEK_BOTH);
					emptyEvent.put("start", 0);
					emptyEvent.put("len", 0);
					emptyEvent.put("room", "");
					emptyEvent.put("egroup", 0);
					emptyEvent.put("lecture", m_lectureId);
					long newId = Data.db.insert("Event", null, emptyEvent);
					esa.updateEvents();
					for(int i = 2; i < esa.getCount(); i++){
						if(esa.getItemId(i) == newId){
							eventSpinner.setSelection(i);
							return;
						}
					}
				}
				
				Cursor c = Data.db.rawQuery("SELECT week, start, len, room, egroup FROM Event WHERE"
						+ " id = ?",  new String[]{ id+"" });
				c.moveToFirst();
				if(c.isAfterLast())
					return;
				int weekValue = c.getInt(c.getColumnIndex("week"));
				((CheckBox) findViewById(R.id.ase_event_week_even)).setChecked(
						(weekValue & FHSSchedule.WEEK_EVEN) != 0);
				((CheckBox) findViewById(R.id.ase_event_week_odd)).setChecked(
						(weekValue & FHSSchedule.WEEK_ODD) != 0);
				
				int start = c.getInt(c.getColumnIndex("start"));
				int day = start / 86400;
				int hour = (start % 86400) / 3600;
				int min = (start % 3600) / 60;
				((Spinner) findViewById(R.id.ase_event_start_day)).setSelection(day);
				((TextView) findViewById(R.id.ase_event_start_time)).
					setText(String.format("%02d:%02d", hour, min));
				
				int length = c.getInt(c.getColumnIndex("len"))/60;
				((EditText) findViewById(R.id.ase_event_length)).setText(length+"");
				
				((EditText) findViewById(R.id.ase_event_room)).
					setText(c.getString(c.getColumnIndex("room")));
				
				((EditText) findViewById(R.id.ase_event_group)).setText(
						c.getInt(c.getColumnIndex("egroup")) + "");
				
				enableEventSection(true);
				c.close();
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				enableEventSection(false);
			}
		});
	}
	
	private void prepareEventDaySpinner(){
		Spinner days = (Spinner) findViewById(R.id.ase_event_start_day);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, 
				android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.days));
		days.setAdapter(adapter);
	}

	private void prepareLectureSpinner() {
		final Spinner lv = (Spinner) findViewById(R.id.ase_lecturelist);
		m_listAdapter = new LectureSpinAdapter(this);
		lv.setAdapter(m_listAdapter);
		lv.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int pos, final long id) {
				m_lectureId = id;
				
				if(id == LectureSpinAdapter.NONE){
					enableLectureSection(false);
					return;
				}
				if(id == LectureSpinAdapter.ADD){
					ContentValues emptyLecture = new ContentValues();
					emptyLecture.put("title", "");
					emptyLecture.put("ltype", FHSSchedule.LTYPE_LECTURE);
					emptyLecture.put("lecturer", "");
					long newId = Data.db.insert("Lecture", null, emptyLecture);
					m_listAdapter.updateLectures();
					for(int i = 2; i < m_listAdapter.getCount(); i++){
						if(m_listAdapter.getItemId(i) == newId){
							lv.setSelection(i);
							return;
						}
					}
				}
				Cursor c = Data.db.rawQuery("SELECT title, ltype, lecturer FROM Lecture WHERE "
						+ "id = ?",	new String[]{id+""} );
				c.moveToFirst();
				
				prepareLectureTitleInput(id, c);
				prepareLecturerInput(id, c);
				prepareTypeInput(id, c);
				prepareEventSpinner(id);
				
				enableLectureSection(true);
				
				c.close();
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				m_lectureId = -1;
				enableLectureSection(false);				
			}
		});
	}
	
	private void enableLectureSection(boolean enable){
		findViewById(R.id.ase_lecture_title).setEnabled(enable);
		findViewById(R.id.ase_lecture_type_lecture).setEnabled(enable);
		findViewById(R.id.ase_lecture_type_exercise).setEnabled(enable);
		findViewById(R.id.ase_lecture_lecturer).setEnabled(enable);
		findViewById(R.id.ase_lecture_eventspinner).setEnabled(enable);
		findViewById(R.id.ase_lecture_delete).setEnabled(enable);
		if(!enable)
			enableEventSection(false);
	}
	
	private void enableEventSection(boolean enable){
		findViewById(R.id.ase_event_week_even).setEnabled(enable);
		findViewById(R.id.ase_event_week_odd).setEnabled(enable);
		findViewById(R.id.ase_event_start_day).setEnabled(enable);
		findViewById(R.id.ase_event_start_time).setEnabled(enable);
		findViewById(R.id.ase_event_length).setEnabled(enable);
		findViewById(R.id.ase_event_room).setEnabled(enable);
		findViewById(R.id.ase_event_group).setEnabled(enable);
		findViewById(R.id.ase_event_delete).setEnabled(enable);
	}

	private void prepareTypeInput(final long id, Cursor c) {
		RadioButton lectureInput = (RadioButton) findViewById(R.id.ase_lecture_type_lecture);
		RadioButton exerciseInput = (RadioButton) findViewById(R.id.ase_lecture_type_exercise);
		
		if(c.getInt(c.getColumnIndex("ltype")) == FHSSchedule.LTYPE_LECTURE)			
			lectureInput.setChecked(true);
		else
			exerciseInput.setChecked(true);
		
		OnClickListener radioListener = new OnClickListener() {
			@Override
			public void onClick(View view) {
				updateLecture(id);
			}
		};
		lectureInput.setOnClickListener(radioListener);
		exerciseInput.setOnClickListener(radioListener);
	}

	private void prepareLecturerInput(final long id, Cursor c) {
		EditText lecturerEditor = (EditText) findViewById(R.id.ase_lecture_lecturer);
		lecturerEditor.setText(c.getString(c.getColumnIndex("lecturer")));
		lecturerEditor.setOnFocusChangeListener(new OnFocusChangeListener() {
			
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if(hasFocus)
					return;
				updateLecture(id);
			}
		});
	}

	private void prepareLectureTitleInput(final long id, Cursor c) {
		EditText titleEditor = (EditText) findViewById(R.id.ase_lecture_title);
		titleEditor.setText(c.getString(c.getColumnIndex("title")));
		titleEditor.setOnFocusChangeListener(new OnFocusChangeListener() {
			
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if(hasFocus)
					return;
				updateLecture(id);
			}
		});
	}
	
	private void updateLecture(long id){
		Data.db.execSQL("UPDATE Lecture SET title = ?, lecturer = ?, ltype = ? WHERE id = ?", 
				new String[]{
					((EditText)findViewById(R.id.ase_lecture_title)).getText().toString(), 
					((EditText)findViewById(R.id.ase_lecture_lecturer)).getText().toString(),
					(((RadioButton)findViewById(R.id.ase_lecture_type_lecture)).isSelected() ?
							FHSSchedule.LTYPE_LECTURE : FHSSchedule.LTYPE_EXERCISE)+"",
					id+""
				});
		m_listAdapter.updateLectures();
	}
}
