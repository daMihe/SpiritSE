package org.michaels.s4a2;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

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
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;


@SuppressLint("DefaultLocale")
public class ScheduleView extends SurfaceView implements SurfaceHolder.Callback, Runnable, 
		OnTouchListener {

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EE dd.MM.yy",
			Locale.getDefault());
	private static final int SEC_IN_MSEC = 1000, MIN_IN_MSEC = 60000, HOUR_IN_MSEC = 3600000;
	private static final float CIRCLEFACTOR = 0.95f;
	private static final long MAX_FPS_SLEEP = 50;
	private long m_lastDraw;
	private Thread m_drawThread;
	private boolean m_run;
	private boolean m_doReMeasure;
	private SurfaceHolder m_holder;
	private Paint m_circleBgPaints[], m_circleTextPaint, m_dateTextPaint, m_normalTextPaint;
	private FHSSchedule.Event m_nextEvent;
	private static FHSSchedule.Event m_shownEvents[];
	private static Calendar m_dayOfShownEvents;
	private static boolean m_listingCurrentDay;
	private RectF m_hourRect, m_minRect, m_secRect;
	private PointF m_centerTimeCircle, m_fingerDownCoords;
	private float m_circleTextHeight, m_dateTextHeight, m_eventTextHeight, m_radius;
	
	/**
	 * Constructor. Needed for using in layouts.
	 * @see init()
	 */
	public ScheduleView(Context context) {
		super(context);
		init();
	}

	/**
	 * Constructor. Needed for using in layouts.
	 * @see init()
	 */
	public ScheduleView(Context context, AttributeSet aset){
		super(context, aset);
		init();
	}
	
	/**
	 * Constructor. Needed for using in layouts.
	 * @see init()
	 */
	public ScheduleView(Context context, AttributeSet aset, int defStyle){
		super(context, aset, defStyle);
		init();
	}
	
	/**
	 * The real construction work. This function is called from a constructor. It sets colors, 
	 * initializes the schedule-graphic and adds the view to a holder and registers a touch-listener
	 * for keeping up to date.
	 */
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
		
		m_dateTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		m_dateTextPaint.setTextAlign(Align.CENTER);
		m_dateTextPaint.setColor(Color.rgb(255-r, 255-g, 255-b));
		
		m_normalTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		m_normalTextPaint.setColor(Color.parseColor("#ff0c0c0c"));
		
		m_nextEvent = FHSSchedule.getNextEvent();
		if(m_nextEvent != null){
			m_shownEvents = FHSSchedule.getEventsOfTheDay(m_nextEvent.nextOccurence);
			m_dayOfShownEvents = m_nextEvent.nextOccurence;
		} else
			m_shownEvents = new FHSSchedule.Event[0];
		m_listingCurrentDay = true;
		
		m_doReMeasure = true;
		
		m_holder = getHolder();
        m_holder.addCallback(this);
        
        setOnTouchListener(this);
	}
	

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		m_doReMeasure = true;
	}

	/**
	 * (Re-)Sizes the drawing-parameters for content (graphical clock and texts) to fit into the 
	 * view.
	 * @param width The new width of the view
	 * @param height The new height of the view
	 */
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
		
		float countdownTextWidth = forMeasure.width();
		m_dateTextPaint.getTextBounds(getDateToDraw(), 0, getDateToDraw().length(), forMeasure);
		m_dateTextPaint.setTextSize(m_dateTextPaint.getTextSize()*0.8f*
				(countdownTextWidth/forMeasure.width()));
		m_dateTextPaint.getTextBounds(getDateToDraw(), 0, getDateToDraw().length(), forMeasure);
		m_dateTextHeight = forMeasure.height();
		
		m_doReMeasure = false;
	}
	
	private String getDateToDraw(){
		if(m_dayOfShownEvents == null)
			return getContext().getString(R.string.hs_noeventsinschedule);
		return DATE_FORMAT.format(m_dayOfShownEvents.getTime());
	}

	/**
	 * Calculates the height of the schedule-list at bottom of the view.
	 * @param width The width of the view
	 * @return The calculated height.
	 */
	private float getListHeight(int width) {
		Rect forMeasure = new Rect();
		if(m_shownEvents.length > 0){
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
	
	/**
	 * Just an abbreviation for (float) Math.pow(base,exp). 
	 * @param base base-value of operation
	 * @param exp exponent of operation
	 * @return The result of base^exp as float.
	 */
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

	/**
	 * Stops the drawing thread.
	 */
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

	/**
	 * Starts the drawing thread.
	 */
	public void resume() {
		if(!m_run){
			m_run = true;
			m_drawThread = new Thread(this);
			m_drawThread.start();
		}
	}
	
	@Override
	public void onDraw(Canvas c){
		if(m_doReMeasure)
			reMeasure(getWidth(), getHeight());
		
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
		
		if(timeToDraw != 0){
			c.drawText(String.format("%02d:%02d:%02d", (int) hoursToDraw, (int) minutesToDraw, 
					(int) secondsToDraw), m_centerTimeCircle.x, m_centerTimeCircle.y + 
					m_circleTextHeight/2, m_circleTextPaint);
		} else {
			c.drawText("--:--:--", m_centerTimeCircle.x, m_centerTimeCircle.y + 
					m_circleTextHeight/2, m_circleTextPaint);
		}
		
		c.drawText(getDateToDraw(), m_centerTimeCircle.x, m_centerTimeCircle.y +
				m_circleTextHeight/2 + 5 + m_dateTextHeight, 
				m_dateTextPaint);
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

	/**
	 * The run-method of the drawing thread.
	 */
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
		if(motion.getAction() == MotionEvent.ACTION_DOWN)
			m_fingerDownCoords = new PointF(motion.getX(),motion.getY());
		if(motion.getAction() == MotionEvent.ACTION_UP){
			if(motion.getY()-m_fingerDownCoords.y > getHeight()/10){
				m_listingCurrentDay = false;
				m_dayOfShownEvents.add(Calendar.DATE, -1);
				m_shownEvents = FHSSchedule.getEventsOfTheDay(m_dayOfShownEvents);
				m_doReMeasure = true;
			} else if(m_fingerDownCoords.y-motion.getY() > getHeight()/10) {
				m_listingCurrentDay = false;
				m_dayOfShownEvents.add(Calendar.DATE, 1);
				m_shownEvents = FHSSchedule.getEventsOfTheDay(m_dayOfShownEvents);
				m_doReMeasure = true;
			} else if(Math.abs(m_fingerDownCoords.y-motion.getY()) < getHeight()/10 && 
					Math.abs(m_fingerDownCoords.x-motion.getX()) < getWidth()/10){
				m_listingCurrentDay = true;
				m_dayOfShownEvents = FHSSchedule.getNextEvent().nextOccurence;
				m_shownEvents = FHSSchedule.getEventsOfTheDay(m_dayOfShownEvents);
				m_doReMeasure = true;
			}
		}
		return true;
	}
}
