package org.michaels.s4a2.activities;

import org.michaels.s4a2.Data;
import org.michaels.s4a2.FHSSchedule;
import org.michaels.s4a2.R;
import org.michaels.s4a2.adapters.EventSpinAdapter;
import org.michaels.s4a2.adapters.LectureSpinAdapter;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;

public class ScheduleEditorActivity extends Activity {
	
	private LectureSpinAdapter m_listAdapter;
	private EventSpinAdapter m_eventAdapter;
	private long m_lectureId;
	private long m_eventId;
	
	@Override
	protected void onCreate(Bundle b){
		super.onCreate(b);
		setContentView(R.layout.activity_ase);
		
		m_lectureId = LectureSpinAdapter.NONE;
		m_eventId = EventSpinAdapter.NONE;
		
		prepareLectureSpinner();
		prepareEventDaySpinner();
		prepareEventChangeListeners();
		prepareLectureChangeListeners();		
		prepareDeleteButtons();
		
	}

	private void prepareDeleteButtons() {
		((Button) findViewById(R.id.ase_lecture_delete)).setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				Data.db.delete("Event", "lecture = ?", new String[]{ m_lectureId+""});
				Data.db.delete("Lecture", "id = ?", new String[]{ m_lectureId+""});
				((Spinner) findViewById(R.id.ase_lecturelist)).setSelection(0);
				m_listAdapter.updateLectures();
				return true;
			}
		});
		((Button) findViewById(R.id.ase_event_delete)).setOnLongClickListener(new OnLongClickListener() {	
			@Override
			public boolean onLongClick(View v) {
				Data.db.delete("Event", "id = ?", new String[]{ m_eventId+""});
				((Spinner) findViewById(R.id.ase_lecture_eventspinner)).setSelection(0);
				m_eventAdapter.updateEvents();
				return true;
			}
		});
		
		
		OnClickListener shortTapListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				Toast.makeText(ScheduleEditorActivity.this, R.string.ase_long_tap_to_delete, Toast.LENGTH_SHORT).show();
			}
		};
		((Button) findViewById(R.id.ase_lecture_delete)).setOnClickListener(shortTapListener);
		((Button) findViewById(R.id.ase_event_delete)).setOnClickListener(shortTapListener);
	}

	private void prepareEventChangeListeners() {
		OnCheckedChangeListener onCheck = new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				updateEvent();
			}
		};
		((CheckBox) findViewById(R.id.ase_event_week_even)).setOnCheckedChangeListener(onCheck);
		((CheckBox) findViewById(R.id.ase_event_week_odd)).setOnCheckedChangeListener(onCheck);
		
		((Spinner) findViewById(R.id.ase_event_start_day)).setOnItemSelectedListener(
				new OnItemSelectedListener() {
	
				@Override
				public void onItemSelected(AdapterView<?> arg0, View arg1,
						int arg2, long arg3) {
					updateEvent();
				}
	
				@Override
				public void onNothingSelected(AdapterView<?> arg0) {}
			});
		
		OnClickListener onDate = new OnClickListener() {
			
			@Override
			public void onClick(final View v) {
				new TimePickerDialog(ScheduleEditorActivity.this, 
						new OnTimeSetListener(){

							@Override
							public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
								((Button)v).setText(String.format("%02d:%02d", hourOfDay, minute));
								updateEvent();
							}
							
						}, 
						Integer.parseInt(((Button)v).getText().toString().substring(0,2)), 
						Integer.parseInt(((Button)v).getText().toString().substring(3)), 
						true
					).show();				
			}
		};
		((Button) findViewById(R.id.ase_event_start_time)).setOnClickListener(onDate);
		
		OnFocusChangeListener onFocus = new OnFocusChangeListener() {
			
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if(hasFocus)
					return;
				updateEvent();
			}
		};
		((EditText) findViewById(R.id.ase_event_length)).setOnFocusChangeListener(onFocus);
		((EditText) findViewById(R.id.ase_event_room)).setOnFocusChangeListener(onFocus);
		((EditText) findViewById(R.id.ase_event_group)).setOnFocusChangeListener(onFocus);
	}
	
	private void prepareLectureChangeListeners() {
		OnClickListener radioListener = new OnClickListener() {
			@Override
			public void onClick(View view) {
				updateLecture();
			}
		};
		((RadioButton) findViewById(R.id.ase_lecture_type_lecture)).
			setOnClickListener(radioListener);
		((RadioButton) findViewById(R.id.ase_lecture_type_exercise)).
			setOnClickListener(radioListener);
		
		OnFocusChangeListener onFocus = new OnFocusChangeListener() {
			
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if(hasFocus)
					return;
				updateLecture();
			}
		};
		((EditText) findViewById(R.id.ase_lecture_lecturer)).setOnFocusChangeListener(onFocus);
		((EditText) findViewById(R.id.ase_lecture_title)).setOnFocusChangeListener(onFocus);
	}
	
	private void prepareEventSpinner(long id){
		m_eventAdapter = new EventSpinAdapter(this, id);
		final Spinner eventSpinner = (Spinner) findViewById(R.id.ase_lecture_eventspinner);
		eventSpinner.setAdapter(m_eventAdapter);
		eventSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int pos, final long id) {
				m_eventId = id;
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
					m_eventAdapter.updateEvents();
					for(int i = 2; i < m_eventAdapter.getCount(); i++){
						if(m_eventAdapter.getItemId(i) == newId){
							eventSpinner.setSelection(i);
							return;
						}
					}
				}
				
				setEventInputsFromDB(id);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				enableEventSection(false);
			}
		});
	}
	
	private void setEventInputsFromDB(final long id) {
		Cursor c = Data.db.rawQuery("SELECT week, start, len, room, egroup FROM Event WHERE"
				+ " id = ?",  new String[]{ id+"" });
		c.moveToFirst();
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
		((Button) findViewById(R.id.ase_event_start_time)).
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
				setLectureInputsFromDB(id);
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
	
	private void updateLecture(){
		if(m_lectureId == LectureSpinAdapter.NONE || m_listAdapter == null)
			return;
		
		Data.db.execSQL("UPDATE Lecture SET title = ?, lecturer = ?, ltype = ? WHERE id = ?", 
				new String[]{
					((EditText)findViewById(R.id.ase_lecture_title)).getText().toString(), 
					((EditText)findViewById(R.id.ase_lecture_lecturer)).getText().toString(),
					(((RadioButton)findViewById(R.id.ase_lecture_type_lecture)).isChecked() ?
							FHSSchedule.LTYPE_LECTURE : FHSSchedule.LTYPE_EXERCISE)+"",
					m_lectureId+""
				});
		m_listAdapter.updateLectures();
	}

	private void updateEvent(){
		if(m_eventId == EventSpinAdapter.NONE || m_eventAdapter == null)
			return;
		int week = 0;
		if(((CheckBox) findViewById(R.id.ase_event_week_even)).isChecked())
			week += FHSSchedule.WEEK_EVEN;
		if(((CheckBox) findViewById(R.id.ase_event_week_odd)).isChecked())
			week += FHSSchedule.WEEK_ODD;
		
		int start = ((Spinner) findViewById(R.id.ase_event_start_day)).getSelectedItemPosition() *
				86400;
		String startTimeStr = ((Button) findViewById(R.id.ase_event_start_time)).getText().
				toString();
		start += Integer.parseInt(startTimeStr.substring(0,2))*3600;
		start += Integer.parseInt(startTimeStr.substring(3))*60;
		
		int length = 0;
		try {
			length = 60*Integer.parseInt(((EditText) findViewById(R.id.ase_event_length)).
					getText().toString());
		} catch(NumberFormatException e){}
		
		String room = ((EditText) findViewById(R.id.ase_event_room)).getText().toString();
		
		int group = 0;
		try {
			group = Integer.parseInt(((EditText) findViewById(R.id.ase_event_group)).
					getText().toString());
		} catch(NumberFormatException e){}
		
		Data.db.execSQL("UPDATE Event SET week = ?, start = ?, len = ?, room = ?, egroup = ? "
				+ "WHERE id = ?", new String[]{
						week+"", start+"", length+"", room, group+"", m_eventId+""
				});
		
		m_eventAdapter.updateEvents();
	}
	
	private void setLectureInputsFromDB(final long id) {
		Cursor c = Data.db.rawQuery("SELECT title, ltype, lecturer FROM Lecture WHERE "
				+ "id = ?",	new String[]{id+""} );
		c.moveToFirst();
		
		((EditText) findViewById(R.id.ase_lecture_title)).
				setText(c.getString(c.getColumnIndex("title")));

		((EditText) findViewById(R.id.ase_lecture_lecturer)).
				setText(c.getString(c.getColumnIndex("lecturer")));
		
		if(c.getInt(c.getColumnIndex("ltype")) == FHSSchedule.LTYPE_LECTURE)			
			((RadioButton) findViewById(R.id.ase_lecture_type_lecture)).setChecked(true);
		else
			((RadioButton) findViewById(R.id.ase_lecture_type_exercise)).setChecked(true);
		
		prepareEventSpinner(id);
		
		enableLectureSection(true);
		
		c.close();
	}
}
