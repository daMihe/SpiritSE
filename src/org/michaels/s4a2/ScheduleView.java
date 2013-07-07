package org.michaels.s4a2;

import java.util.Calendar;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;


@SuppressLint("DefaultLocale")
public class ScheduleView extends SurfaceView implements SurfaceHolder.Callback, Runnable, 
		OnTouchListener {

	private static final int SEC_IN_MSEC = 1000, MIN_IN_MSEC = 60000, HOUR_IN_MSEC = 3600000;
	private static final float CIRCLEFACTOR = 0.95f;
	private static final long MAX_FPS_SLEEP = 50;
	private long m_lastDraw;
	private Thread m_drawThread;
	private boolean m_run;
	private SurfaceHolder m_holder;
	private Paint m_circleBgPaints[], m_circleTextPaint, m_normalTextPaint;
	private FHSSchedule.Event m_nextEvent;
	private static FHSSchedule.Event m_shownEvents[];
	private static Calendar m_dayOfShownEvents;
	private static boolean m_listingCurrentDay;
	private RectF m_hourRect, m_minRect, m_secRect;
	private PointF m_centerTimeCircle, m_fingerDownCoords;
	private float m_circleTextHeight, m_eventTextHeight, m_radius;
	
	public ScheduleView(Context context) {
		super(context);
		init();
	}

	public ScheduleView(Context context, AttributeSet aset){
		super(context, aset);
		init();
	}
	
	public ScheduleView(Context context, AttributeSet aset, int defStyle){
		super(context, aset, defStyle);
		init();
	}
	
	private void init() {
		m_run = false;
		setBackgroundColor(Color.parseColor("#fff3f3f3"));
		m_circleBgPaints = new Paint[4];
		int r=0,g=127,b=255;
		int rStep = (int) (r*0.2), gStep = (int) (g*0.2), bStep = (int) (b*0.2);
		for(int i = 0; i < m_circleBgPaints.length; i++){
			m_circleBgPaints[i] = new Paint(Paint.ANTI_ALIAS_FLAG);
			m_circleBgPaints[i].setColor(Color.rgb(r, g, b));
			r=Math.max(0, r - rStep);
			g=Math.max(0, g - gStep);
			b=Math.max(0, b - bStep);
		}
		m_circleTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		m_circleTextPaint.setTextAlign(Align.CENTER);
		m_circleTextPaint.setColor(Color.rgb(255-r, 255-g, 255-b));
		m_normalTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		m_normalTextPaint.setColor(Color.parseColor("#ff0c0c0c"));
		m_nextEvent = FHSSchedule.getNextEvent();
		if(m_nextEvent != null){
			m_shownEvents = FHSSchedule.getEventsOfTheDay(m_nextEvent.nextOccurence);
			m_dayOfShownEvents = m_nextEvent.nextOccurence;
		} else
			m_shownEvents = new FHSSchedule.Event[0];
		m_listingCurrentDay = true;
		
		m_holder = getHolder();
        m_holder.addCallback(this);
        
        setOnTouchListener(this);
	}
	

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		reMeasure(width, height);
	}

	private void reMeasure(int width, int height) {
		Rect forMeasure = new Rect();
		float neededSpace = getListHeight(width);
		
		m_centerTimeCircle = new PointF(width/2, (height-neededSpace)/2);
		m_radius = Math.min(m_centerTimeCircle.x, m_centerTimeCircle.y);
		
		m_hourRect = new RectF(m_centerTimeCircle.x-m_radius, m_centerTimeCircle.y-m_radius,
				m_centerTimeCircle.x+m_radius, m_centerTimeCircle.y+m_radius);
		m_minRect = new RectF(m_centerTimeCircle.x-m_radius*CIRCLEFACTOR,
				m_centerTimeCircle.y-m_radius*CIRCLEFACTOR, m_centerTimeCircle.x+m_radius*CIRCLEFACTOR,
				m_centerTimeCircle.y+m_radius*CIRCLEFACTOR);
		m_secRect = new RectF(m_centerTimeCircle.x-m_radius*pow(CIRCLEFACTOR,2), 
				m_centerTimeCircle.y-m_radius*pow(CIRCLEFACTOR,2), 
				m_centerTimeCircle.x+m_radius*pow(CIRCLEFACTOR,2),
				m_centerTimeCircle.y+m_radius*pow(CIRCLEFACTOR,2));
		
		m_circleTextPaint.getTextBounds("00:00:00", 0, "00:00:00".length(), forMeasure);
		m_circleTextPaint.setTextSize(m_circleTextPaint.getTextSize()*
				( ((m_radius*2) * pow(CIRCLEFACTOR,5))/forMeasure.width() ) );
		m_circleTextPaint.getTextBounds("00:00:00", 0, "00:00:00".length(), forMeasure);
		m_circleTextHeight = forMeasure.height();
	}

	private float getListHeight(int width) {
		Rect forMeasure = new Rect();
		if(m_shownEvents.length > 0){
			m_shownEvents = FHSSchedule.getEventsOfTheDay(FHSSchedule.getNextEvent().nextOccurence);
			m_normalTextPaint.setTextSize(new Button(getContext()).getTextSize());
			float widestText = 0f;
			m_eventTextHeight = 0f;
			for(int i=0; i<m_shownEvents.length; i++){
				String current = String.format("%02d.%02d/%s: %s", 
						m_shownEvents[i].nextOccurence.get(Calendar.HOUR_OF_DAY),
						m_shownEvents[i].nextOccurence.get(Calendar.MINUTE),
						m_shownEvents[i].room, m_shownEvents[i].name);
				m_normalTextPaint.getTextBounds(current,0,current.length(),forMeasure);
				if(forMeasure.width() > widestText)
					widestText = forMeasure.width();
				if(forMeasure.height() > m_eventTextHeight)
					m_eventTextHeight = forMeasure.height();
			}
			if(widestText > width){
				m_normalTextPaint.setTextSize(m_normalTextPaint.getTextSize()*(width/widestText));
				m_eventTextHeight *= (width/widestText);
			}
			return( (m_eventTextHeight+3)*m_shownEvents.length +5 );
		} else 
			return(0f);
	}
	
	private static float pow(float base,float exp){
		return (float) Math.pow(base, exp);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		resume();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		pause();
	}

	public void pause() {
		if(m_run){
			m_run = false;
			while(m_drawThread.isAlive()){
				try {
					m_drawThread.join();
				} catch (InterruptedException e) {}
			}
		}
	}

	public void resume() {
		if(!m_run){
			m_run = true;
			m_drawThread = new Thread(this);
			m_drawThread.start();
		}
	}
	
	@Override
	public void onDraw(Canvas c){
		drawCountdown(c);
		drawEventlist(c);
		
		m_lastDraw = System.currentTimeMillis();
	}

	/**
	 * Draws the countdown to the next event or shows how to get a schedule.
	 * @param c A Canvas to draw on.
	 */
	private void drawCountdown(Canvas c) {
		float timeToDraw = 0;
		for(FHSSchedule.Event e:m_shownEvents){
			if(e.nextOccurence.after(Calendar.getInstance())){
				timeToDraw = e.nextOccurence.getTimeInMillis() - System.currentTimeMillis();
				break;
			}
		}
		float hoursToDraw = timeToDraw/HOUR_IN_MSEC;
		float minutesToDraw = (float) ((timeToDraw-Math.floor(hoursToDraw)*HOUR_IN_MSEC)/
				MIN_IN_MSEC);
		float secondsToDraw = (float) ((timeToDraw-(Math.floor(hoursToDraw)*HOUR_IN_MSEC+
				Math.floor(minutesToDraw)*MIN_IN_MSEC)))/SEC_IN_MSEC;
		c.drawArc(m_hourRect, -90, (hoursToDraw/24)*360, true, m_circleBgPaints[0]);
		c.drawArc(m_minRect, -90, (minutesToDraw/60)*360, true, m_circleBgPaints[1]);
		c.drawArc(m_secRect, -90, (secondsToDraw/60)*360, true, m_circleBgPaints[2]);
		c.drawCircle(m_centerTimeCircle.x, m_centerTimeCircle.y, m_radius*pow(CIRCLEFACTOR,3), 
				m_circleBgPaints[3]);
		String timeString = String.format("%02d:%02d:%02d", (int) hoursToDraw, (int) minutesToDraw, 
				(int) secondsToDraw);
		
		c.drawText(timeString, m_centerTimeCircle.x, m_centerTimeCircle.y+m_circleTextHeight/2, 
				m_circleTextPaint);
	}
	
	/**
	 * Draws a list of events on the selected day.
	 * @param c A Canvas to draw on.
	 */
	private void drawEventlist(Canvas c) {
		if(m_shownEvents.length > 0 && m_shownEvents[m_shownEvents.length-1].nextOccurence.
				before(Calendar.getInstance()) && m_listingCurrentDay){
			Calendar nextOccurence = FHSSchedule.getNextEvent().nextOccurence;
			m_shownEvents = FHSSchedule.getEventsOfTheDay(nextOccurence);
			m_dayOfShownEvents = nextOccurence;
		}
		for(int i=0; i<m_shownEvents.length; i++){
			String current = String.format("%02d.%02d/%s: %s", 
					m_shownEvents[i].nextOccurence.get(Calendar.HOUR_OF_DAY),
					m_shownEvents[i].nextOccurence.get(Calendar.MINUTE),
					m_shownEvents[i].room, m_shownEvents[i].name);
			if(m_shownEvents[i].nextOccurence.before(Calendar.getInstance()))
				m_normalTextPaint.setAlpha(127);
			else
				m_normalTextPaint.setAlpha(255);
			c.drawLine(0, getHeight()-((m_shownEvents.length-i)*(m_eventTextHeight+3))-1, 
					getWidth(), getHeight()-((m_shownEvents.length-i)*(m_eventTextHeight+3))-4, 
					m_normalTextPaint);
			c.drawText(current, 0, getHeight()-((m_shownEvents.length-1-i)*(m_eventTextHeight+3))-5, m_normalTextPaint);
		}
	}

	@Override
	public void run() {
		long timeToWait;
		while(m_run){
			postInvalidate();
			try {
				timeToWait = m_lastDraw+MAX_FPS_SLEEP-System.currentTimeMillis();
				if(timeToWait > 0)
					Thread.sleep(timeToWait);
			} catch (InterruptedException e) {}
		}
	}

	@Override
	public boolean onTouch(View arg0, MotionEvent motion) {
		Log.i("Motion",motion.toString());
		// TODO reMeasure causes bugs
		if(motion.getAction() == MotionEvent.ACTION_DOWN)
			m_fingerDownCoords = new PointF(motion.getX(),motion.getY());
		if(motion.getAction() == MotionEvent.ACTION_UP){
			if(motion.getY()-m_fingerDownCoords.y > getHeight()/10){
				m_listingCurrentDay = false;
				m_dayOfShownEvents.add(Calendar.DATE, -1);
				m_shownEvents = FHSSchedule.getEventsOfTheDay(m_dayOfShownEvents);
				//reMeasure(getWidth(), getHeight());
			} else if(m_fingerDownCoords.y-motion.getY() > getHeight()/10) {
				m_listingCurrentDay = false;
				m_dayOfShownEvents.add(Calendar.DATE, 1);
				m_shownEvents = FHSSchedule.getEventsOfTheDay(m_dayOfShownEvents);
				//reMeasure(getWidth(), getHeight());
			}
		}
		return true;
	}
}
