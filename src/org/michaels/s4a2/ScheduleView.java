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
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.v4.view.GestureDetectorCompat;
import android.text.Layout.Alignment;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.Toast;


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
	private boolean m_run, m_doReMeasure, m_showhelp;
	private SurfaceHolder m_holder;
	private Paint m_circleBgPaints[], m_circleTextPaint, m_dateTextPaint, m_normalTextPaint;
	private FHSSchedule.Event m_nextEvent;
	private static FHSSchedule.Event m_shownEvents[];
	private static Calendar m_dayOfShownEvents;
	private static boolean m_listingCurrentDay;
	private RectF m_hourRect, m_minRect, m_secRect;
	private PointF m_centerTimeCircle;
	private float m_circleTextHeight, m_dateTextHeight, m_eventTextHeight, m_radius;
	private GestureDetectorCompat m_gestureDetector;
	
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
		colorize();		
		m_nextEvent = FHSSchedule.getNextEvent();
		if(m_nextEvent != null){
			m_shownEvents = FHSSchedule.getEventsOfTheDay(m_nextEvent.nextOccurence);
			m_dayOfShownEvents = m_nextEvent.nextOccurence;
		} else
			m_shownEvents = new FHSSchedule.Event[0];
		m_listingCurrentDay = true;
		m_doReMeasure = true;
		m_showhelp = false;
		m_holder = getHolder();
        m_holder.addCallback(this);
        setOnTouchListener(this);
        m_gestureDetector = new GestureDetectorCompat(getContext(), new OnGestureListener());
	}

	/**
	 * Sets/Calculates all needed colors.
	 */
	private void colorize() {
		setBackgroundColor(Color.parseColor("#fff3f3f3"));
		m_circleBgPaints = new Paint[4];
		int scheduleColor = Data.preferences.getInt(Data.PREF_SCHEDULE_COLOR, 0xff007fff);
		int r=(scheduleColor & 0xff0000)>>16, g=(scheduleColor & 0xff00) >> 8, 
				b=scheduleColor & 0xff;
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
		float neededSpace = getListHeight(width);
		
		m_centerTimeCircle = new PointF(width/2, (height-neededSpace)/2);
		m_radius = Math.min(m_centerTimeCircle.x, m_centerTimeCircle.y);
		
		reMeasureCircles();
		reMeasureCountdown();
		
		m_doReMeasure = false;
	}

	/**
	 * (Re-)Sizes the drawing-parameters for time and day of countdown.
	 */
	private void reMeasureCountdown() {
		Rect forMeasure = new Rect();
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
	}

	/**
	 * (Re-)Sizes the circles (graphical clock).
	 */
	private void reMeasureCircles() {
		m_hourRect = new RectF(m_centerTimeCircle.x-m_radius, m_centerTimeCircle.y-m_radius,
				m_centerTimeCircle.x+m_radius, m_centerTimeCircle.y+m_radius);
		m_minRect = new RectF(m_centerTimeCircle.x-m_radius*CIRCLEFACTOR,
				m_centerTimeCircle.y-m_radius*CIRCLEFACTOR, m_centerTimeCircle.x+m_radius*CIRCLEFACTOR,
				m_centerTimeCircle.y+m_radius*CIRCLEFACTOR);
		m_secRect = new RectF(m_centerTimeCircle.x-m_radius*pow(CIRCLEFACTOR,2), 
				m_centerTimeCircle.y-m_radius*pow(CIRCLEFACTOR,2), 
				m_centerTimeCircle.x+m_radius*pow(CIRCLEFACTOR,2),
				m_centerTimeCircle.y+m_radius*pow(CIRCLEFACTOR,2));
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
				String current = String.format("%02d.%02d/%s: %s %s", 
						m_shownEvents[i].nextOccurence.get(Calendar.HOUR_OF_DAY),
						m_shownEvents[i].nextOccurence.get(Calendar.MINUTE),
						m_shownEvents[i].room, m_shownEvents[i].name, getContext().
						getString(m_shownEvents[i].type == FHSSchedule.LTYPE_EXERCISE ? 
								R.string.exercise : R.string.lecture));
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
	 * Starts the drawing thread. Restarts the activity if requested by other activities.
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
		drawHelp(c);
		
		m_lastDraw = System.currentTimeMillis();
	}

	/**
	 * Draws the countdown to the next event or shows how to get a schedule.
	 * @param c A Canvas to draw on.
	 */
	private void drawCountdown(Canvas c) {		
		float timeToDraw = 0;
		for(FHSSchedule.Event e:m_shownEvents){
			if(e.nextOccurence.getTimeInMillis() + FHSSchedule.getDSTRepairOffset(e.nextOccurence) >
					System.currentTimeMillis()){
				timeToDraw = e.nextOccurence.getTimeInMillis() + 
						FHSSchedule.getDSTRepairOffset(e.nextOccurence) - System.currentTimeMillis();
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
			String current = String.format("%02d.%02d/%s: %s %s", 
					m_shownEvents[i].nextOccurence.get(Calendar.HOUR_OF_DAY)+ 
					FHSSchedule.getDSTRepairOffset(m_shownEvents[i].nextOccurence)/3600000,
					m_shownEvents[i].nextOccurence.get(Calendar.MINUTE),
					m_shownEvents[i].room, m_shownEvents[i].name, getContext().
					getString(m_shownEvents[i].type == FHSSchedule.LTYPE_EXERCISE ? 
							R.string.exercise : R.string.lecture));
			if(m_shownEvents[i].nextOccurence.before(Calendar.getInstance()))
				m_normalTextPaint.setAlpha(127);
			else
				m_normalTextPaint.setAlpha(255);
			c.drawLine(0, getHeight()-((m_shownEvents.length-i)*(m_eventTextHeight+3))-1, 
					getWidth(), getHeight()-((m_shownEvents.length-i)*(m_eventTextHeight+3))-4, 
					m_normalTextPaint);
			c.drawText(current, 0, getHeight()-((m_shownEvents.length-1-i)*(m_eventTextHeight+3))-5,
					m_normalTextPaint);
		}
	}
	
	/**
	 * drawHelp - draws a help for the users. Only draws something if m_showhelp is set to true.
	 * @param c A canvas to draw on.
	 */
	private void drawHelp(Canvas c) {
		if(m_showhelp){			
			PointF center = new PointF(getWidth()/2, getHeight()/2);
			Paint overlayBackcolor = new Paint();
			overlayBackcolor.setARGB(180, 255, 255, 255);
			c.drawRect(0, 0, getWidth(), getHeight(), overlayBackcolor);
			
			Paint overlayFrontcolor = new Paint();
			overlayFrontcolor.setColor(Color.BLACK);
			overlayFrontcolor.setStrokeWidth(10);
			overlayFrontcolor.setAntiAlias(true);
			overlayFrontcolor.setStrokeCap(Paint.Cap.ROUND);
			overlayFrontcolor.setStyle(Paint.Style.STROKE);
			
			c.save();
			c.translate(center.x, center.y);
			float circleRadius = Math.min(getWidth(), getHeight())/10;			
			Path topPath = new Path();
			topPath.moveTo(-circleRadius, -4f*circleRadius);
			topPath.lineTo(0, -5f*circleRadius);
			topPath.lineTo(circleRadius, -4f*circleRadius);
			c.drawPath(topPath, overlayFrontcolor);
			
			
			c.drawCircle(0, -2.5f*circleRadius, circleRadius, overlayFrontcolor);
			c.drawCircle(0, -2.5f*circleRadius, circleRadius/2, overlayFrontcolor);
			
			Path bottomPath = new Path();
			bottomPath.moveTo(-circleRadius, -1f*circleRadius);
			bottomPath.lineTo(0, 0);
			bottomPath.lineTo(circleRadius, -1f*circleRadius);
			c.drawPath(bottomPath, overlayFrontcolor);
						
			overlayFrontcolor.setStrokeWidth(0);
			overlayFrontcolor.setTextSize(new Button(getContext()).getTextSize());
			c.drawText("+ 24h", circleRadius+10, -4.5f*circleRadius-overlayFrontcolor.ascent()/2, overlayFrontcolor);
			c.drawText("\u2192 0", circleRadius+10, -2.5f*circleRadius-overlayFrontcolor.ascent()/2, overlayFrontcolor);
			c.drawText("- 24h", circleRadius+10, -0.5f*circleRadius-overlayFrontcolor.ascent()/2, overlayFrontcolor);

			c.translate(-(getWidth()/2)+10, 10);
			StaticLayout localTextLayout = new StaticLayout(
					getContext().getString(R.string.hs_helptext), 
					new TextPaint(overlayFrontcolor), getWidth()-20, Alignment.ALIGN_NORMAL, 1.0f, 
					0.0f, false);
			localTextLayout.draw(c);
			c.restore();
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
		m_gestureDetector.onTouchEvent(motion);
		return true;
	}
	
	class OnGestureListener extends GestureDetector.SimpleOnGestureListener {
		private static final float VELOCITY_THRESHOLD = 200f;
		
		public boolean onFling (MotionEvent e1, MotionEvent e2, float velocityX, float velocityY){
			if(m_showhelp)
				return false;
			if(velocityY < -VELOCITY_THRESHOLD && m_dayOfShownEvents != null){
				m_listingCurrentDay = false;
				m_dayOfShownEvents.add(Calendar.DATE, 1);
			} else if(velocityY > VELOCITY_THRESHOLD && m_dayOfShownEvents != null){
				m_listingCurrentDay = false;
				m_dayOfShownEvents.add(Calendar.DATE, -1);
			} else if(m_dayOfShownEvents == null)
				Toast.makeText(getContext(), R.string.hs_holdforhelp, Toast.LENGTH_LONG).show();
			m_shownEvents = FHSSchedule.getEventsOfTheDay(m_dayOfShownEvents);
			m_doReMeasure = true;
			return true;
		}
		
		public void onLongPress (MotionEvent e){
			m_showhelp = !m_showhelp;
		}
		public boolean onDoubleTap (MotionEvent e){
			m_listingCurrentDay = true;
			m_dayOfShownEvents = FHSSchedule.getNextEvent().nextOccurence;
			m_shownEvents = FHSSchedule.getEventsOfTheDay(m_dayOfShownEvents);
			m_doReMeasure = true;
			return false;
		}
	}
	
}
