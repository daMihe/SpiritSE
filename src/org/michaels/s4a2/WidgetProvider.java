package org.michaels.s4a2;

import java.text.DateFormat;

import org.michaels.s4a2.activities.LaunchActivity;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class WidgetProvider extends AppWidgetProvider {
	private static long m_lastrefresh = 0;
	private static final long LAUNCHCLICK_THRESHOLD = 500;
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appwidgetmanager, int[] appwidgetids){
		RemoteViews widgetviews = new RemoteViews(context.getPackageName(),R.layout.widget);
		
		if(detectLaunchClick(context))
			return;
		
		setLabel(context, widgetviews);
		
		Intent refreshintent = new Intent(context,WidgetProvider.class);
		refreshintent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
		refreshintent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appwidgetids);
		widgetviews.setOnClickPendingIntent(R.id.w_label, PendingIntent.getBroadcast(context, 0, refreshintent, PendingIntent.FLAG_UPDATE_CURRENT));
		
		for(int widgetid:appwidgetids)
			appwidgetmanager.updateAppWidget(widgetid, widgetviews);
	}

	/**
	 * Generates text for label displayed in widget and sets it. Also sets colors.
	 * @param context Context for getting localized strings.
	 * @param views RemoteViews to change.
	 */
	private void setLabel(Context context, RemoteViews views) {
		if(Data.db == null)
			Data.init(context);
		FHSSchedule.Event nextevent = FHSSchedule.getNextEvent();
		String rtn = "";
		if(nextevent == null)
			rtn = (String) context.getText(R.string.hs_noeventsinschedule);
		else {
			DateFormat shortTimeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
			rtn = DateFormat.getDateInstance(DateFormat.SHORT).format(nextevent.nextOccurence.getTime())+"\n";
			for(FHSSchedule.Event e:FHSSchedule.getEventsOfTheDay(nextevent.nextOccurence)){
				rtn += shortTimeFormat.format(e.nextOccurence.getTime()) + "/"
					+ e.room + ": " + e.name + " " + 
					context.getText(nextevent.type == FHSSchedule.LTYPE_EXERCISE ? 
							R.string.exercise : R.string.lecture) + "\n";
			}
		}
		views.setTextViewText(R.id.w_label, rtn);
		
		int prefcolor = getPreferredColor();
		views.setInt(R.id.w_layout, "setBackgroundColor", prefcolor);
		int prefforeground = (0xffffffff - (prefcolor & 0xffffff));
		views.setInt(R.id.w_label, "setTextColor", prefforeground);
	}

	/**
	 * Calculates preferred background color.
	 * @return The preferred color.
	 */
	private int getPreferredColor() {
		int prefcolor = (Data.preferences.getInt(Data.PREF_SCHEDULE_COLOR, 0x007fff) & 0xffffff);
		prefcolor = (((int) (((prefcolor & 0xff0000)>>16)*0.4))<<16) | (((int) (((prefcolor & 0x00ff00)>>8)*0.4))<<8) |
				((int) ((prefcolor & 0x0000ff)*0.4)) | 0xa8000000;
		return prefcolor;
	}

	/**
	 * @param context
	 */
	private boolean detectLaunchClick(Context context) {
		if(System.currentTimeMillis() - m_lastrefresh < LAUNCHCLICK_THRESHOLD){
			AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			am.set(AlarmManager.RTC, System.currentTimeMillis()+100, 
					PendingIntent.getActivity(context, 0, new Intent(context,LaunchActivity.class), PendingIntent.FLAG_UPDATE_CURRENT));
			return true;
		}
		m_lastrefresh = System.currentTimeMillis();
		return false;
	}
}
