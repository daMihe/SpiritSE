package org.michaels.s4a2.activities;

import java.util.Calendar;
import java.util.Random;

import org.michaels.s4a2.Data;
import org.michaels.s4a2.R;
import org.michaels.s4a2.SomeFunctions;
import org.michaels.s4a2.parsers.NewsLoadParser;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class LaunchActivity extends Activity {
	
	private boolean launch;
	
	@Override
	protected void onStop(){
		launch = false;
		super.onStop();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_launch);
		launch = true;
		
		String randomsplashes[] = getResources().getStringArray(R.array.l_splashes);
		TextView splashView = ((TextView) findViewById(R.id.l_splash));
		Calendar currentmoment = Calendar.getInstance();
		int dom = currentmoment.get(Calendar.DATE), month = currentmoment.get(Calendar.MONTH);
		if(dom == 3 && month == Calendar.APRIL)
			splashView.setText(R.string.l_special_birthday_michi);
		else if(month == Calendar.DECEMBER && dom >= 24 && dom < 27)
			splashView.setText(R.string.l_special_christmas);
		else if(dom == 1 && month == Calendar.JANUARY)
			splashView.setText(R.string.l_special_newyear);
		else
			splashView.setText(randomsplashes[new Random(System.currentTimeMillis()).nextInt(randomsplashes.length)]);
		((TextView) findViewById(R.id.l_version)).setText(SomeFunctions.generateVersion(this));
		
		final Thread waiter = new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					long waitTime = 5000;
					Calendar currentmoment = Calendar.getInstance();
					int dom = currentmoment.get(Calendar.DATE), month = currentmoment.get(Calendar.MONTH);
					if((dom == 3 && month == Calendar.APRIL) || (month == Calendar.DECEMBER && dom >= 24 && dom < 27) ||
							(dom == 1 && month == Calendar.JANUARY))
						waitTime = 9001;
					Thread.sleep(waitTime);
				} catch (InterruptedException e1) {}
			}
		});
		
		Thread initializer = new Thread(new Runnable(){
		
			public void run(){
				Data.init(LaunchActivity.this);
				
				if(!Data.preferences.getBoolean("acceptedUsingConditions", false)){
					startActivity(new Intent(LaunchActivity.this, ConditionsOfUseActivity.class));
					LaunchActivity.this.finish();
					return;
				}
				
				if(Data.preferences.getString("deviceId", "").isEmpty()){
					String devId = "";
					byte[] numbers = new byte[32];
					new Random(System.nanoTime()).nextBytes(numbers);
					
					for(byte b:numbers)
						devId += String.format("%02x", b);
					
					Editor e = Data.preferences.edit();
					e.putString("deviceId", devId);
					e.apply();
				}
				
				Log.i("Spirit SE Information", "Device ID: "+
						Data.preferences.getString("deviceId", "0"));
				if(System.currentTimeMillis()- (Data.preferences.
						getLong(Data.PREF_UPDATE_INTERVAL, 86400L)*1000) >
						Data.preferences.getLong(Data.PREF_LAST_UPDATE, 0L)){
					NewsLoadParser newsUpdater = new NewsLoadParser();
					newsUpdater.loadAndParse(LaunchActivity.this);
				}
				
				try {
					waiter.join();
				} catch (InterruptedException e) {}
				
				if(launch)
					startActivity(new Intent(LaunchActivity.this, HomeActivity.class));
				LaunchActivity.this.finish();
			}
		});
		waiter.start();
		initializer.start();
	}

}
