package org.michaels.s4a2;

import java.util.Random;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class LaunchActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_launch);
		
		String randomsplashes[] = getResources().getStringArray(R.array.l_splashes);
		((TextView) findViewById(R.id.l_splash)).setText(randomsplashes[new Random(System.nanoTime()).nextInt(randomsplashes.length)]);
		((TextView) findViewById(R.id.l_version)).setText(SomeFunctions.generateVersion(this));
		
		final Thread waiter = new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e1) {}
			}
		});
		
		Thread initializer = new Thread(new Runnable(){
		
			public void run(){
				Data.init(LaunchActivity.this);
				
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
				
				Log.i("Spirit SE Information", "Device ID: "+Data.preferences.getString("deviceId", "00000000000000000000000000000000"));
				
				try {
					waiter.join();
				} catch (InterruptedException e) {}
				if(Data.preferences.getBoolean("acceptedUsingConditions", false))
					startActivity(new Intent(LaunchActivity.this, HomeActivity.class));
				else
					startActivity(new Intent(LaunchActivity.this, ConditionsOfUseActivity.class));
				
				LaunchActivity.this.finish();
			}
		});
		waiter.start();
		initializer.start();
	}

}
