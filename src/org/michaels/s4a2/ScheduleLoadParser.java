package org.michaels.s4a2;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.Toast;

public class ScheduleLoadParser {
	
	/**
	 * Checks if schedule has correct format (spirit-rest 1.0).
	 * @param schedule JSONArray containing the complete schedule
	 * @return Returns whether correct or not.
	 */
	public static boolean validateJSONSchedule(JSONArray schedule){
		for(int i=0; i<schedule.length(); ++i){
			JSONObject event = schedule.optJSONObject(i);
			if(event == null)
				return false;
			if(!validateJSONScheduleEvent(event))
				return false;
		}
		return true;
	}
	
	/**
	 * Checks if schedule_event is a valid event read from spirit-rest 1.0
	 * @param schedule_event JSONObject containing event to check
	 * @return Returns whether valid or not.
	 */
	public static boolean validateJSONScheduleEvent(JSONObject schedule_event){
		String event_type = schedule_event.optString("eventType");
		if(!event_type.equals("Vorlesung") && !event_type.equals("Uebung"))
			return false;
		
		if(schedule_event.optString("titleShort").isEmpty())
			return false;
		
		JSONArray member = schedule_event.optJSONArray("member");
		if(member == null || member.length() == 0)
			return false;
		
		if(member.optJSONObject(0).optString("name").isEmpty())
			return false;
		
		JSONObject appointment = schedule_event.optJSONObject("appointment");
		if(appointment == null)
			return false;
		
		if(!appointment.optString("day").matches("((Mon|Diens|Donners|Frei|Sams|Sonn)tag)|Mittwoch"))
			return false;
		
		JSONObject appointment_location = appointment.optJSONObject("location");
		if(appointment_location == null)
			return false;
		
		JSONObject appointment_location_place = appointment_location.optJSONObject("place");
		if(appointment_location_place == null)
			return false;
		
		if(appointment_location_place.optString("building").isEmpty())
			return false;
		
		if(appointment_location_place.optString("room").isEmpty())
			return false;
		
		if(!appointment.optString("time").matches("([0-1][0-9]|2[0-3])\\.[0-5][0-9]-([0-1][0-9]|2[0-3])\\.[0-5][0-9]"))
			return false;
		
		if(!appointment.optString("week").matches("w|g|u"))
			return false;
		
		if(!schedule_event.optString("group").matches("^$|.[0-9]?"))
			return false;
		
		return true;
	}
	
	public static void parseJSON(String raw_json, SQLiteDatabase db) throws IllegalArgumentException, JSONException{
		JSONArray schedule_json = new JSONArray(raw_json);
		if(!validateJSONSchedule(schedule_json))
			throw new IllegalArgumentException("JSON is no valid schedule.");
		
	}
	
	public static String load(final String classname, final Context a){
		AlertDialog.Builder ad = new AlertDialog.Builder(a);
		ad.setMessage(R.string.loading);
		ad.setCancelable(false);
		AlertDialog dialog = ad.create();
		dialog.show();
		
		final String rtn = "";
		
		Thread t = new Thread(new Runnable(){
			public void run(){
				HttpGet request = new HttpGet("http://spirit.fh-schmalkalden.de/rest/1.0/schedule?classname="+classname.toLowerCase());
				request.setHeader("User-Agent", SomeFunctions.generateUserAgent(a));
				DefaultHttpClient client = new DefaultHttpClient();
				HttpConnectionParams.setSoTimeout(client.getParams(), 30000);
				try {
					HttpResponse response = client.execute(request);
					rtn.concat(EntityUtils.toString(response.getEntity()));
				} catch (Exception e) {
					Log.e("Spirit SE Schedule Loader", e.getClass().getCanonicalName()+": "+e.getLocalizedMessage());
				}
			}
		});
		t.start();
		
		while(t.isAlive()){
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {}
		}
		
		Toast.makeText(a, rtn.length()+" Chars", Toast.LENGTH_LONG).show();
		
		return rtn;
		
		
	}
}
