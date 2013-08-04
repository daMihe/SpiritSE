package org.michaels.s4a2.activities;

import java.util.Arrays;
import java.util.Locale;

import net.yougli.shakethemall.ColorPickerDialog;

import org.michaels.s4a2.Data;
import org.michaels.s4a2.R;
import org.michaels.s4a2.parsers.ScheduleLoadParser;

import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

public class SettingsActivity extends FragmentActivity {

	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide
	 * fragments for each of the sections. We use a
	 * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which
	 * will keep every loaded fragment in memory. If this becomes too memory
	 * intensive, it may be best to switch to a
	 * {@link android.support.v4.app.FragmentStatePagerAdapter}.
	 */
	SectionsPagerAdapter m_SettingsSectionsAdapter;

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	ViewPager m_ViewPager;
 
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		m_SettingsSectionsAdapter = new SectionsPagerAdapter(
				getSupportFragmentManager());
		m_ViewPager = (ViewPager) findViewById(R.id.pager);
		m_ViewPager.setAdapter(m_SettingsSectionsAdapter);

	}

	/**
	 * This FragmentPagerAdapter divides Settings into a basic- and schedule-Fragment.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			Fragment fragment = null;
			switch(position){
				case 0: fragment = new BasicSettingsFragment(); break;
				case 1: fragment = new ScheduleSettingsFragment(); break;
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
				return getString(R.string.s_title_basic).toUpperCase(l);
			case 1:
				return getString(R.string.s_title_schedule).toUpperCase(l);
			}
			return null;
		}
	}
	
	public static class ScheduleSettingsFragment extends Fragment {
		private ScheduleLoadParser m_backgroundParser;
		
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.settings_schedule, container, false);
			
			prepareDownloadButton(rootView);
			fillCourseSpinner(rootView);
			prepareGroupsButton(rootView);
			prepareEditorButton(rootView);
			prepareBgColorButton(rootView);
			
			return rootView;
		}
		
		private void prepareBgColorButton(final View rootView){
			rootView.findViewById(R.id.ss_color).setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					new ColorPickerDialog(rootView.getContext(), 
							new ColorPickerDialog.OnColorChangedListener() {
								@Override
								public void colorChanged(String key, int color) {
									Editor e = Data.preferences.edit();
									e.putInt(Data.PREF_SCHEDULE_COLOR, color);
									e.apply();
								}
					}, Data.PREF_SCHEDULE_COLOR, 
					Data.preferences.getInt(Data.PREF_SCHEDULE_COLOR, 0xff007fff), 0xff007fff)
					.show();
				}
			});
		}
		
		private void prepareGroupsButton(View rootView) {
			rootView.findViewById(R.id.ss_groups).setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					getActivity().startActivity(new Intent(getActivity(),GroupSetupActivity.class));
				}
			});
		}
		
		private void prepareEditorButton(View rootView) {
			rootView.findViewById(R.id.ss_editor).setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					getActivity().startActivity(new Intent(getActivity(),
							ScheduleEditorActivity.class));
				}
			});
		}

		private void fillCourseSpinner(View rootView) {
			Spinner scheduleCourseSpinner = (Spinner) rootView.findViewById(R.id.ss_course_spin);
			scheduleCourseSpinner.setAdapter(new ArrayAdapter<String>(getActivity(), 
					android.R.layout.simple_spinner_item,
					getResources().getStringArray(R.array.courses)));
			int selection = Math.max(Arrays.asList(getResources().getStringArray(R.array.courses)).
					indexOf(Data.preferences.getString(Data.PREF_SCHEDULE_COURSE, "BaI1")),0);
			scheduleCourseSpinner.setSelection(selection);
			scheduleCourseSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

				@Override
				public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
					Editor e = Data.preferences.edit();
					e.putString(Data.PREF_SCHEDULE_COURSE, 
							getResources().getStringArray(R.array.courses)[pos]);
					e.apply();
				}

				@Override
				public void onNothingSelected(AdapterView<?> arg0) {}
			});
		}

		private void prepareDownloadButton(View rootView) {
			Button downloadButton = (Button) rootView.findViewById(R.id.ss_download);
			
			downloadButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {		
					m_backgroundParser = new ScheduleLoadParser(getActivity());
					m_backgroundParser.execute(Data.preferences.
							getString(Data.PREF_SCHEDULE_COURSE, "BaI1"));
				}
			});
		}
	}

	/**
	 * This Fragment shows very basic settings like Update interval, news filtering
	 */
	public static class BasicSettingsFragment extends Fragment {

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.settings_basic, container, false);
			
			fillNewsFilterSpinner(rootView);
			fillUpdateIntervalSpinner(rootView);
			
			return rootView;
		}
		
		private void fillUpdateIntervalSpinner(View rootView){
			Spinner updateInterval = (Spinner) rootView.findViewById(R.id.sb_newsinterval_spin);
			updateInterval.setAdapter(new ArrayAdapter<String>(getActivity(), 
					android.R.layout.simple_spinner_item, 
					getResources().getStringArray(R.array.sb_newsintervals)));
			int currentSelection = intIndexOf(getResources().getIntArray(R.array.sb_newsintervals_values),
					(int) Data.preferences.getLong(Data.PREF_UPDATE_INTERVAL, 86400));
			if(currentSelection >= 0)
				updateInterval.setSelection(currentSelection);
			updateInterval.setOnItemSelectedListener(new OnItemSelectedListener() {

				@Override
				public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3) {
					Editor e = Data.preferences.edit();
					e.putLong(Data.PREF_UPDATE_INTERVAL, 
							getResources().getIntArray(R.array.sb_newsintervals_values)[position]);
					e.apply();
				}

				@Override
				public void onNothingSelected(AdapterView<?> arg0) {
					Editor e = Data.preferences.edit();
					e.remove(Data.PREF_UPDATE_INTERVAL);
					e.apply();
				}
			});
		}
		
		private int intIndexOf(int[] array, int value){
			for(int i = 0; i < array.length; i++){
				if(array[i] == value)
					return i;
			}
			return -1;
		}

		private void fillNewsFilterSpinner(View rootView) {
			Spinner newsFilter = (Spinner) rootView.findViewById(R.id.sb_newsfilter_spin);
			String[] courses = getResources().getStringArray(R.array.courses);
			String[] newsFilterPossibilities = new String[courses.length+1];
			newsFilterPossibilities[0] = getString(R.string.none);
			System.arraycopy(courses, 0, newsFilterPossibilities, 1, courses.length);
			newsFilter.setAdapter(new ArrayAdapter<String>(getActivity(), 
					android.R.layout.simple_spinner_item, newsFilterPossibilities));
			
			int currentSelection = Arrays.asList(newsFilterPossibilities).
					indexOf(Data.preferences.getString(Data.PREF_NEWS_FILTER, ""));
			if(currentSelection >= 0)
				newsFilter.setSelection(currentSelection);
				
			newsFilter.setOnItemSelectedListener(new OnItemSelectedListener() {

				@Override
				public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
					Editor e = Data.preferences.edit();
					if(position == 0)
						e.remove(Data.PREF_NEWS_FILTER);
					else
						e.putString(Data.PREF_NEWS_FILTER, ((TextView) view).getText().toString());
					e.apply();
				}

				@Override
				public void onNothingSelected(AdapterView<?> arg0) {
					Editor e = Data.preferences.edit();
					e.remove(Data.PREF_NEWS_FILTER);
					e.apply();
				}
			});
		}
	}

}
