package org.michaels.s4a2;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.List;
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

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.util.Log;

public class NewsLoadParser {
	/**
	 * loadAndParse tries to download up-to-date news from the spirit-server. If successful, the
	 * news can read afterwards through the database and 
	 * {@link Data.PREF_LAST_UPDATE} will be refreshed.
	 * @param context A context for generating the User-Agent.
	 */
	public void loadAndParse(final Context context){
		JSONArray toParse = validate(load(context));
		if(toParse == null)
			return;
		parseAndSave(toParse);
		Log.i("","Updated News!");
		Editor e = Data.preferences.edit();
		e.putLong(Data.PREF_LAST_UPDATE, System.currentTimeMillis());
		e.apply();
	}
	
	/**
	 * Parses the given JSONArray as array of news and saves them to the database. 
	 * @param allNews A validated JSONArray of news
	 */
	private void parseAndSave(JSONArray allNews){
		for(int i = 0; i < allNews.length(); i++){
			JSONObject currentObject = allNews.optJSONObject(i);
			List<String> targetSemesters = Arrays.asList(currentObject.optString("semester").split(" "));
			if(!targetSemesters.contains("semester") && 
					Data.preferences.getString(Data.PREF_NEWS_FILTER, null) != null){
				String usersSemester = Data.preferences.getString(Data.PREF_NEWS_FILTER, null).replace("Ba", "");
				if(!targetSemesters.contains(usersSemester))
					continue;
			}
			SimpleDateFormat writeDateParser = new SimpleDateFormat("EEE, dd MMM yyyy H:m:s Z",Locale.ENGLISH);
			Matcher eolDateParser = Pattern.compile("([0-9]+)\\.([0-9]+)\\.([0-9]+)").
					matcher(currentObject.optString("lifecycle"));
			try {
				GregorianCalendar writeDate = new GregorianCalendar(), eolDate;
				writeDate.setTime(writeDateParser.parse(currentObject.optString("date")));
				eolDate = (GregorianCalendar) writeDate.clone();
				eolDateParser.matches();
				eolDate.set(GregorianCalendar.DATE, Integer.parseInt(eolDateParser.group(1)));
				eolDate.set(GregorianCalendar.MONTH, Integer.parseInt(eolDateParser.group(2))-1);
				eolDate.set(GregorianCalendar.YEAR, Integer.parseInt(eolDateParser.group(3)));
				
				saveNewsEntry(currentObject, writeDate, eolDate);
			} catch (ParseException e) {}
		}
	}
	
	/**
	 * Saves a single news-entry into the database.
	 * @param currentObject The JSONObject containing a news-message.
	 * @param writeDate The parsed creation-date.
	 * @param eolDate The parsed End-of-life date.
	 */
	private void saveNewsEntry(JSONObject currentObject,
			GregorianCalendar writeDate, GregorianCalendar eolDate) {
		Cursor previousEntry = Data.db.rawQuery("SELECT content FROM News WHERE id = "+
				Integer.parseInt(currentObject.optString("nr")), null);
		if(!previousEntry.moveToFirst()){
			Data.db.execSQL("INSERT INTO News (id,title,content,readstate,dateutc,invalidatutc," +
					"author) VALUES (?,?,?,0,?,?,?)",
					new String[]{ currentObject.optString("nr"), 
						currentObject.optString("subject"), currentObject.optString("news"),
						""+writeDate.getTimeInMillis(),	""+eolDate.getTimeInMillis(),
						 currentObject.optString("writer")});
		} else {
			if(!previousEntry.getString(previousEntry.getColumnIndex("content")).
					equals(currentObject.optString("news"))){
				Data.db.execSQL("UPDATE News SET title = ?, content = ?, readstate = 0, " +
						"dateutc = ?, invalidatutc = ?, author = ? WHERE id = ?",
						new String[]{ currentObject.optString("subject"), 
							currentObject.optString("news"), ""+writeDate.getTimeInMillis(),
							""+eolDate.getTimeInMillis(), currentObject.optString("writer"),
							currentObject.optString("nr")
						});
			}
		}
		previousEntry.close();
	}
	
	/**
	 * Validates a read String containing the news-array.
	 * @param input The String to validate.
	 * @return If successful, a JSONArray parsed from the String. If not, null will be returned.
	 */
	private JSONArray validate(String input){
		if(input == null)
			return null;
		try {
			JSONArray rtn = new JSONArray(input);
			for(int i = 0; i < rtn.length(); i++){
				JSONObject objectToCheck = rtn.getJSONObject(i);
				if(objectToCheck.optString("semester") == null || 
						objectToCheck.optString("subject") == null ||
						objectToCheck.optString("writer") == null ||
						objectToCheck.optString("lifecycle") == null ||
						objectToCheck.optString("nr") == null ||
						objectToCheck.optString("date") == null ||
						objectToCheck.optString("news") == null)
					return null;
			}
			return rtn;
		} catch (JSONException e) {
			return null;
		}
	}
	
	/**
	 * Loads the news from the Spirit-REST-server.
	 * @param context A context to generate the User-Agent.
	 * @return The news as string or null, if not succeeded.
	 */
	private String load(final Context context) {
		HttpGet request = new HttpGet("http://spirit.fh-schmalkalden.de/rest/1.0/news");
		request.setHeader("User-Agent", SomeFunctions.generateUserAgent(context));
		DefaultHttpClient client = new DefaultHttpClient();
		HttpConnectionParams.setSoTimeout(client.getParams(), 30000);
		try {
			HttpResponse response = client.execute(request);
			return EntityUtils.toString(response.getEntity());
		} catch (Exception e) {
			return null;
		}
	}
}
