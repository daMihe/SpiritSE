package org.michaels.s4a2;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;

public class Data {
	public static final String PREF_NEWS_FILTER = "newsFilter";
	public static final String PREF_UPDATE_INTERVAL = "updateInterval";
	public static final String PREF_LAST_UPDATE = "lastSuccessfulUpdate";
	public static final String PREF_SCHEDULE_COURSE = "course";
	public static SharedPreferences preferences = null;
	public static SQLiteDatabase db = null;
	
	public static void init(Context c){
		if(preferences != null)
			preferences = null;
		if(db != null)
			db.close();
		
		preferences = PreferenceManager.getDefaultSharedPreferences(c);
		
		DBOpenHelper dboh = new DBOpenHelper(c);
		db = dboh.getWritableDatabase();
	}
	
	static class DBOpenHelper extends SQLiteOpenHelper {

		private final static int DB_VERSION = 6;
		
		public DBOpenHelper(Context context) {
			super(context,"db",null,DB_VERSION);
		}
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("PRAGMA foreign_keys = ON;");
			
			db.execSQL("CREATE TABLE Lecture ("+
					"id INTEGER PRIMARY KEY AUTOINCREMENT, "+
					"title TEXT NOT NULL, "+
					"ltype INTEGER NOT NULL, "+
					"lecturer TEXT NOT NULL);");
			
			db.execSQL("CREATE TABLE Event ("+
					"id INTEGER PRIMARY KEY AUTOINCREMENT, "+
					"week INTEGER NOT NULL, "+
					"start INTEGER NOT NULL, "+
					"len INTEGER NOT NULL, "+
					"room TEXT NOT NULL, "+
					"lecture INTEGER NOT NULL," +
					"egroup INTEGER," +
					"FOREIGN KEY(lecture) REFERENCES Lecture(id));");
			
			db.execSQL("CREATE TABLE Sparetime ("+
					"id INTEGER PRIMARY KEY AUTOINCREMENT, "+
					"reason TEXT UNIQUE NOT NULL, "+
					"startutc INTEGER NOT NULL, "+
					"len INTEGER NOT NULL);");
			
			db.execSQL("CREATE TABLE News ("+
					"id INTEGER PRIMARY KEY AUTOINCREMENT, "+
					"title TEXT NOT NULL, "+
					"content TEXT NOT NULL, "+
					"readstate INTEGER NOT NULL, "+
					"dateutc INTEGER NOT NULL, "+
					"invalidatutc INTEGER NOT NULL, " +
					"author TEXT NOT NULL);");
			
			db.execSQL("CREATE TABLE Usergroup ("+
					"lecture INTEGER PRIMARY KEY, "+
					"ugroup INTEGER," +
					"FOREIGN KEY(lecture) REFERENCES Lecture(id));");
			
			db.execSQL("CREATE TABLE Sparetime_blocks_Lecture ("+
					"lecture INTEGER, "+
					"sparetime INTEGER, "+
					"PRIMARY KEY(lecture, sparetime), "+
					"FOREIGN KEY(lecture) REFERENCES Lecture(id), "+
					"FOREIGN KEY(sparetime) REFERENCES Sparetime(id))");
			
			db.execSQL("CREATE TABLE Newsgroup (" +
					"ngroup TEXT, " +
					"News_id INTEGER, " +
					"PRIMARY KEY(ngroup, News_id), " +
					"FOREIGN KEY(News_id) REFERENCES News(id))");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			if(oldVersion < 2){
				db.execSQL("CREATE TABLE News ("+
						"id INTEGER PRIMARY KEY AUTOINCREMENT, "+
						"title TEXT NOT NULL, "+
						"content TEXT NOT NULL, "+
						"readstate INTEGER NOT NULL, "+
						"dateutc INTEGER NOT NULL, "+
						"invalidatutc INTEGER NOT NULL);");
				db.execSQL("CREATE TABLE Usergroup ("+
						"lecture INTEGER PRIMARY KEY, "+
						"ugroup INTEGER," +
						"FOREIGN KEY(lecture) REFERENCES Lecture(id));");
			}
			if(oldVersion < 3){
				db.execSQL("CREATE TABLE Sparetime_blocks_Lecture ("+
						"lecture INTEGER, "+
						"sparetime INTEGER, "+
						"PRIMARY KEY(lecture, sparetime), "+
						"FOREIGN KEY(lecture) REFERENCES Lecture(id), "+
						"FOREIGN KEY(sparetime) REFERENCES Sparetime(id))");
			}
			if(oldVersion < 4){
				db.execSQL("ALTER TABLE News ADD COLUMN author TEXT NOT NULL DEFAULT ''");
			}
			if(oldVersion < 5)
				db.execSQL("ALTER TABLE Event ADD COLUMN egroup INTEGER");
			if(oldVersion < 6){
				db.execSQL("CREATE TABLE Newsgroup (" +
						"ngroup TEXT, " +
						"News_id INTEGER, " +
						"PRIMARY KEY(ngroup, News_id), " +
						"FOREIGN KEY(News_id) REFERENCES News(id))");
				db.execSQL("DELETE FROM News");
			}
		}
		
	}
}
