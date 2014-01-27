package org.michaels.s4a2.activities;
 
import org.michaels.s4a2.Data;
import org.michaels.s4a2.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class ConditionsOfUseActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_conditions_of_use);
		
		Button but_accept = (Button) findViewById(R.id.cou_accept);
		Button but_decline = (Button) findViewById(R.id.cou_decline);
		Button but_dwtr = (Button) findViewById(R.id.cou_dont_want_to_read);
		if(Data.preferences.getBoolean("acceptedUsingConditions", false)){
			but_accept.setEnabled(false);
			but_decline.setEnabled(false);
			but_dwtr.setEnabled(false);
		} else {
			but_accept.setOnClickListener(new OnAcceptListener());
			but_dwtr.setOnClickListener(new OnDWTRListener());
			but_decline.setOnClickListener(new OnDeclineListener());
		}
	}
	
	private class OnAcceptListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			Editor e = Data.preferences.edit();
			e.putBoolean("acceptedUsingConditions", true);
			e.apply();
			
			startActivity(new Intent(ConditionsOfUseActivity.this,LaunchActivity.class));
			ConditionsOfUseActivity.this.finish();
		}
		
	}
	
	private class OnDWTRListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			AlertDialog.Builder b = new AlertDialog.Builder(ConditionsOfUseActivity.this);
			b.setMessage(R.string.cou_come_on);
			
			DialogInterface.OnClickListener but_handler = new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					arg0.dismiss();
				}
			};
			
			b.setPositiveButton(R.string.yes, but_handler);
			b.setNegativeButton(R.string.sure, but_handler);
			
			b.create().show();
		}	
	}
	
	private class OnDeclineListener implements OnClickListener {
		@Override
		public void onClick(View arg0) {
			ConditionsOfUseActivity.this.finish();
		}
	}
}
