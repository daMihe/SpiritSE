package org.michaels.s4a2;

import java.util.Arrays;
import java.util.Locale;

import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
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
	SectionsPagerAdapter mSectionsPagerAdapter;

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	ViewPager mViewPager;
 
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);

		// Show the Up button in the action bar.
		getActionBar().setDisplayHomeAsUpEnabled(true);

		// Create the adapter that will return a fragment for each of the three
		// primary sections of the app.
		mSectionsPagerAdapter = new SectionsPagerAdapter(
				getSupportFragmentManager());

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			// getItem is called to instantiate the fragment for the given page.
			// Return a DummySectionFragment (defined as a static inner class
			// below) with the page number as its lone argument.
			Fragment fragment = new BasicSettingsFragment();
			/*Bundle args = new Bundle();
			args.putInt(DummySectionFragment.ARG_SECTION_NUMBER, position + 1);
			fragment.setArguments(args);*/
			return fragment;
		}

		@Override
		public int getCount() {
			// Show 3 total pages.
			return 1;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			Locale l = Locale.getDefault();
			switch (position) {
			case 0:
				return getString(R.string.s_title_basic).toUpperCase(l);
			case 1:
				return getString(R.string.s_title_schedule).toUpperCase(l);
			case 2:
				return getString(R.string.s_title_section3).toUpperCase(l);
			}
			return null;
		}
	}

	/**
	 * This Fragment shows very basic settings like Update interval, news filtering
	 * @author Michael Wodniok
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
			int currentSelection = Arrays.asList(getResources().
					getIntArray(R.array.sb_newsintervals_values)).indexOf(
							Data.preferences.getInt(Data.PREF_UPDATE_INTERVAL, 86400));
			if(currentSelection >= 0)
				updateInterval.setSelection(currentSelection);
			updateInterval.setOnItemSelectedListener(new OnItemSelectedListener() {

				@Override
				public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3) {
					Editor e = Data.preferences.edit();
					e.putInt(Data.PREF_UPDATE_INTERVAL, 
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
