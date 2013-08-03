package org.michaels.s4a2;

import java.text.DateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.webkit.WebView;

public class NewsViewActivity extends Activity {
	
	public static int id;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_news_view);
		
		Cursor c = Data.db.rawQuery("SELECT title, content, dateutc, author FROM News WHERE id = ?",
				new String[]{ id+"" });
		if(c.getCount() == 1){
			c.moveToFirst();
			String author = c.getString(c.getColumnIndex("author"));
			String date = DateFormat.getDateTimeInstance().format(
					new Date(c.getLong(c.getColumnIndex("dateutc"))));
			String title = c.getString(c.getColumnIndex("title"));
			getActionBar().setTitle(title);
			getActionBar().setSubtitle(
					String.format(getString(R.string.n_writtenby), author, date));
			
			String html = formatMessage(c.getString(c.getColumnIndex("content")));
			
			((WebView) findViewById(R.id.n_message)).loadData(html, "text/html; charset=UTF-8", 
					null);
		}
		c.close();
	}

	/**
	 * This function transforms Spirit-formatted Text into more or less conform HTML. This method 
	 * was copied from Spirit 0.8 Landry.
	 * @param c The Spirit-formatted source
	 * @return The transformed HTML
	 */
	private String formatMessage(String c) {
		String html = "";
		String[] preHTML = c.split("%\\{");
		for(String s:preHTML){
			html += (s.indexOf("%")!=-1 ? "<span style=\""+s.replaceAll("\\}", "\">").
					replaceAll("%", "</span>") : s).replaceAll("\\r\\n","<br />");
		}
		while(html.matches("(.*\".*(\":http)s?(://).*)")){
			Pattern p = Pattern.compile("(\"[^<>]*(\":http)s?(://)[^\\s]+)");
			Matcher m = p.matcher(html);
			m.find();
			int sPosition = m.start();
			int ePosition = m.end();
			String s = html.substring(sPosition, ePosition);
			String newS = "<a href=\""+s.substring(s.indexOf(":")+1)+"\">"+s.substring(1, 
					s.indexOf("\"",2))+"</a>";
			html = html.replace(s, newS);	
		}
		while(html.matches(".*[\\*]{2}.*[\\*]{2}.*")){
			html = html.replaceFirst("[\\*]{2}", "<strong>");
			html = html.replaceFirst("[\\*]{2}", "</strong>");
		}
		html = html.replace("\\r\\n", "<br />");
		html = "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; " +
				"charset=UTF-8\" /></head><body>"+html+"</body></html>";
		return html;
	}

}
