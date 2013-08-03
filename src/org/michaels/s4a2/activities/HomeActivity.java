package org.michaels.s4a2.activities;

import java.util.Locale;

import org.michaels.s4a2.Data;
import org.michaels.s4a2.R;
import org.michaels.s4a2.ScheduleView;
import org.michaels.s4a2.R.id;
import org.michaels.s4a2.R.layout;
import org.michaels.s4a2.R.menu;
import org.michaels.s4a2.R.string;
import org.michaels.s4a2.adapters.NewsListAdapter;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class HomeActivity extends FragmentActivity {

	private HomeSectionsAdapter m_sectionsAdapter;
	private ViewPager m_pager;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);
		m_sectionsAdapter = new HomeSectionsAdapter(getSupportFragmentManager());
		m_pager = (ViewPager) findViewById(R.id.pager);
		m_pager.setAdapter(m_sectionsAdapter);
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

	private class HomeSectionsAdapter extends FragmentPagerAdapter {
		
		public HomeSectionsAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int pos){
			Fragment fragment = null;
			switch(pos){
				case 0: fragment = new ScheduleFragment(); break;
				case 1: fragment = new NewsFragment(); break;
			}
			return fragment;
		}

		@Override
		public int getCount() {
			return 2;
		}
		
		@Override
		public CharSequence getPageTitle(int position) {
			Locale l = Locale.getDefault();
			switch (position) {
			case 0:
				return getString(R.string.h_title_schedule).toUpperCase(l);
			case 1:
				return getString(R.string.h_title_news).toUpperCase(l);
			}
			return null;
		}
	}
	
	public static class NewsFragment extends Fragment {
		private ListView m_newsList;
		
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, 
				Bundle savedInstanceState){
			m_newsList = (ListView) inflater.inflate(R.layout.home_news, container, false);
			
			m_newsList.setAdapter(new NewsListAdapter());
			m_newsList.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					Data.db.execSQL("UPDATE News SET readstate = 1 WHERE id = ?",new String[]{id+""});
					NewsViewActivity.id = (int) id;
					startActivity(new Intent(getActivity(), NewsViewActivity.class));
				}
			});
			
			return m_newsList;
		}
		
		public void onResume(){
			super.onResume();
			((NewsListAdapter) m_newsList.getAdapter()).updatedData();
		}
	}
	
	public static class ScheduleFragment extends Fragment {
		private ScheduleView m_schedule;
		
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, 
				Bundle savedInstanceState){
			m_schedule = (ScheduleView) inflater.inflate(R.layout.home_schedule, container,
					false);
			return m_schedule;
		}
		
		@Override
		public void onPause(){
			super.onPause();
			m_schedule.pause();
		}
		
		@Override
		public void onResume(){
			m_schedule.resume();
			super.onResume();
		}
	}
}
