package org.michaels.s4a2.parsers;

import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.michaels.s4a2.Data;
import org.michaels.s4a2.FHSSchedule;
import org.michaels.s4a2.R;
import org.michaels.s4a2.SomeFunctions;

import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;

public class ScheduleLoadParser extends AsyncTask<String, Void, Void> {
	
	private Context m_context; 
	private ProgressDialog m_waitDialog;
	private final static String[] WEEKDAYS = new String[]{
			"Sonntag","Montag","Dienstag","Mittwoch","Donnerstag","Freitag","Samstag" };
	
	/**
	 * Constructor. The Context is used for displaying a progress dialog and generating the User-
	 * Agent.
	 * @param c A valid Context.
	 */
	public ScheduleLoadParser(Context c){
		m_context = c;
	}
	
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
		
		if(!appointment.optString("day").
				matches("((Mon|Diens|Donners|Frei|Sams|Sonn)tag)|Mittwoch"))
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
		
		if(!appointment.optString("time").
				matches("([0-1][0-9]|2[0-3])\\.([0-5][0-9])-([0-1][0-9]|2[0-3])\\.([0-5][0-9])"))
			return false;
		
		if(!appointment.optString("week").matches("w|g|u"))
			return false;
		
		if(!schedule_event.optString("group").matches("^.?([0-9]?)"))
			return false;
		
		return true;
	}
	
	/**
	 * Parses the downloaded JSON and saves it into the database.
	 * @param raw_json A String containing the JSON schedule
	 * @throws IllegalArgumentException
	 * @throws JSONException
	 */
	public static void parseJSON(String raw_json) throws IllegalArgumentException, JSONException{
		JSONArray scheduleJson = new JSONArray(raw_json);
		if(!validateJSONSchedule(scheduleJson))
			throw new IllegalArgumentException("JSON is no valid schedule.");
		
		Data.db.execSQL("DELETE FROM Event");
		Data.db.execSQL("DELETE FROM Usergroup");
		Data.db.execSQL("DELETE FROM Lecture");
		
		for(int i = 0; i<scheduleJson.length(); i++){
			JSONObject event = scheduleJson.optJSONObject(i);
			Cursor lecture = getLectureId(event);
			if(!lecture.moveToFirst()){
				String title = event.optString("titleShort");
				if(title.matches("^.*\\s[VÜ]$")){//endsWith(" Ü") || title.endsWith(" V")){
					title = title.substring(0, title.length()-2);
				}
				Data.db.execSQL("INSERT INTO Lecture (title,ltype,lecturer) VALUES (?,?,?)",
						new String[]{
							title,
							(event.optString("eventType").equals("Vorlesung") ? 
									FHSSchedule.LTYPE_LECTURE : FHSSchedule.LTYPE_EXERCISE)+"",
							event.optJSONArray("member").optJSONObject(0).optString("name")
						});
				lecture.close();
				lecture = getLectureId(event);
				lecture.moveToFirst();
			}
			
			insertEventToDB(event, lecture);
			lecture.close();
		}
	}

	private static void insertEventToDB(JSONObject event, Cursor lecture)
			throws JSONException {
		JSONObject appointment = event.optJSONObject("appointment");
		String weekString = appointment.optString("week");
		int week = (weekString.equals("w") ? FHSSchedule.WEEK_BOTH : 
			(weekString.equals("g") ? FHSSchedule.WEEK_EVEN : FHSSchedule.WEEK_ODD));
		
		Matcher daytimeMatcher = Pattern.compile(
				"([0-1][0-9]|2[0-3])\\.([0-5][0-9])-([0-1][0-9]|2[0-3])\\.([0-5][0-9])").
				matcher(appointment.optString("time"));
		daytimeMatcher.matches();
		int start = Arrays.asList(WEEKDAYS).indexOf(appointment.optString("day")) * 86400;
		int end = start;
		start += Integer.parseInt(daytimeMatcher.group(1)) * 3600 + 
				Integer.parseInt(daytimeMatcher.group(2)) * 60;
		end += Integer.parseInt(daytimeMatcher.group(3)) * 3600 + 
				Integer.parseInt(daytimeMatcher.group(4)) * 60;
		int len = end - start;
		JSONObject place = appointment.optJSONObject("location").optJSONObject("place");
		String room = place.getString("building")+place.getString("room");
		Matcher groupMatcher = Pattern.compile("^.([0-9]?)").matcher(event.getString("group"));
		int group = 0;
		if(groupMatcher.matches()){
				group = (groupMatcher.group(1).isEmpty() ? 0 : 
					Integer.parseInt(groupMatcher.group(1)));
		}
		
		Data.db.execSQL("INSERT INTO Event (week,start,len,room,lecture,egroup) VALUES " +
				"(?,?,?,?,?,?)", new String[]{
					week+"",start+"",len+"",room,
					lecture.getInt(lecture.getColumnIndex("id"))+"", group+""
				});
	}

	private static Cursor getLectureId(JSONObject event) {
		String title = event.optString("titleShort");
		if(title.matches("^.*\\s[VÜ]$")){
			title = title.substring(0, title.length()-2);
		}
		return Data.db.rawQuery(
				"SELECT id FROM Lecture WHERE title=? AND ltype=? AND lecturer=?",
				new String[]{
					title,
					(event.optString("eventType").equals("Vorlesung") ? 
							FHSSchedule.LTYPE_LECTURE : FHSSchedule.LTYPE_EXERCISE)+"",
					event.optJSONArray("member").optJSONObject(0).optString("name")
				});
	}
	
	public String load(final String classname){
		
		String rtn = "";
		HttpGet request = new HttpGet("http://spirit.fh-schmalkalden.de/rest/1.0/schedule?" +
				"classname="+classname.toLowerCase(Locale.ENGLISH));
		request.setHeader("User-Agent", SomeFunctions.generateUserAgent(m_context));
		DefaultHttpClient client = new DefaultHttpClient();
		HttpConnectionParams.setSoTimeout(client.getParams(), 30000);
		try {
			HttpResponse response = client.execute(request);
			rtn = EntityUtils.toString(response.getEntity());
		} catch (Exception e) {
			Log.e("Spirit SE Schedule Loader", e.getClass().getCanonicalName()+": "+e.getLocalizedMessage());
		}
		
		return rtn;	
	}
	
	@Override
	protected void onPreExecute(){
		m_waitDialog = new ProgressDialog(m_context);
		m_waitDialog.setMessage(m_context.getString(R.string.loading));
		m_waitDialog.setIndeterminate(true);
		m_waitDialog.setCancelable(false);
		m_waitDialog.show();
	}

	@Override
	protected Void doInBackground(String... params) {
		String raw_schedule = load(params[0]);
		try {
			parseJSON(raw_schedule);
		} catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	protected void onPostExecute(Void notUsed){
		m_waitDialog.dismiss();
	}
}
