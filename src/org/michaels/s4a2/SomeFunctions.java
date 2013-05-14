package org.michaels.s4a2;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.DisplayMetrics;
import android.view.WindowManager;

public class SomeFunctions {
	public static String generateUserAgent(Context c){
		String rtn = "Spirit SE";
		
		String version = generateVersion(c);
		if(!version.isEmpty())
			rtn += " v"+version;
		
		DisplayMetrics metrics = new DisplayMetrics();
		WindowManager wm = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
		wm.getDefaultDisplay().getMetrics(metrics);
		rtn += " ("+metrics.widthPixels+"*"+metrics.heightPixels+"px; "+metrics.densityDpi+
				"dpi; #"+Data.preferences.getString("deviceId", "00000000000000000000000000000000")+")";		
		
		return rtn;
	}
	
	public static String generateVersion(Context c){
		try {
			PackageInfo pi = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
			return pi.versionName+"."+pi.versionCode;
		} catch (NameNotFoundException e) {
			return "";
		}
	}
}
