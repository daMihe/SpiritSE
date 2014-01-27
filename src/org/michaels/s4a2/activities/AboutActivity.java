package org.michaels.s4a2.activities;

import org.michaels.s4a2.R;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
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
		Button but_donate = (Button) findViewById(R.id.a_donate);
		
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
		
		but_donate.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				startActivity(new Intent(Intent.ACTION_VIEW,
						Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=FJFBMJS9KPBVJ")));
			}
		});
	}

}
