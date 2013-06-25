package org.michaels.s4a2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class HomeActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);
		
		final ListView newsList = (ListView) findViewById(R.id.h_newslist);
		newsList.setAdapter(new NewsListAdapter());
		newsList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Data.db.execSQL("UPDATE News SET readstate = 1 WHERE id = ?",new String[]{id+""});
				((NewsListAdapter) newsList.getAdapter()).updatedData();
				NewsViewActivity.id = (int) id;
				startActivity(new Intent(HomeActivity.this, NewsViewActivity.class));
			}
		});
	}
	
	@Override
	public void onResume(){
		((ScheduleView) findViewById(R.id.h_schedule)).resume();
		super.onResume();
	}
	
	@Override
	public void onPause(){
		((ScheduleView) findViewById(R.id.h_schedule)).pause();
		super.onPause();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.home, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
		case R.id.m_h_settings:
			startActivity(new Intent(this,SettingsActivity.class));
			return true;
		case R.id.m_h_about:
			startActivity(new Intent(this,AboutActivity.class));
			return true;
		}
		return false;
	}

}
