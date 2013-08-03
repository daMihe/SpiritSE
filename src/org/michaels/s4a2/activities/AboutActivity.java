package org.michaels.s4a2.activities;

import org.michaels.s4a2.R;
import org.michaels.s4a2.R.id;
import org.michaels.s4a2.R.layout;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class AboutActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);
		
		TextView tex_name = (TextView) findViewById(R.id.a_name);
		Button but_vcou = (Button) findViewById(R.id.a_conditions_of_use);
		
		try {
			PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
			tex_name.setText("Spirit Second Edition\nv"+pi.versionName+"."+pi.versionCode);
		} catch (NameNotFoundException e) {
			tex_name.setText("Spirit Second Edition");
		}
		
		but_vcou.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(AboutActivity.this,ConditionsOfUseActivity.class));
			}
		});
	}

}
