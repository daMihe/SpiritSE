package org.michaels.s4a2;

import java.util.Calendar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;


public class ScheduleView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

	private static final int SEC_IN_MSEC = 1000;
	private static final int MIN_IN_MSEC = 60000;
	private static final int HOUR_IN_MSEC = 3600000;
	private static final float CIRCLEFACTOR = 0.95f;
	private static final long MAX_FPS_SLEEP = 50;
	private long m_lastDraw;
	private Thread m_drawThread;
	private boolean m_run;
	private SurfaceHolder m_holder;
	private Paint m_paints[];
	private Paint m_textPaint;
	
	public ScheduleView(Context context) {
		super(context);
		init();
	}

	private void init() {
		m_run = false;
		setBackgroundColor(Color.parseColor("#fff3f3f3"));
		m_paints = new Paint[4];
		int r=0,g=127,b=255;
		for(int i = 0; i < m_paints.length; i++){
			m_paints[i] = new Paint(Paint.ANTI_ALIAS_FLAG);
			m_paints[i].setColor(Color.rgb(r, g, b));
			r=Math.max(0, (int) (r*0.7));
			g=Math.max(0, (int) (g*0.7));
			b=Math.max(0, (int) (b*0.7));
		}
		m_textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		m_textPaint.setTextAlign(Align.CENTER);
		m_textPaint.setColor(Color.rgb(255-r, 255-g, 255-b));
				
		m_holder = getHolder();
        m_holder.addCallback(this);
	}
	
	public ScheduleView(Context context, AttributeSet aset){
		super(context, aset);
		init();
	}
	
	public ScheduleView(Context context, AttributeSet aset, int defStyle){
		super(context, aset, defStyle);
		init();
	}
	
	@Override
	protected void onMeasure(int widthSpec, int heightSpec){
		int rootWidth = getRootView().getWidth();
		int rootHeight = getRootView().getHeight();
		setMeasuredDimension(rootWidth, rootHeight/2);
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
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
		// TODO change timeToDraw to a countdown to an event.
		float timeToDraw = FHSSchedule.getNextEvent().nextOccurence.getTimeInMillis() - System.currentTimeMillis();
		float hoursToDraw = timeToDraw/HOUR_IN_MSEC;
		float minutesToDraw = (float) ((timeToDraw-Math.floor(hoursToDraw)*HOUR_IN_MSEC)/MIN_IN_MSEC);
		float secondsToDraw = (float) ((timeToDraw-(Math.floor(hoursToDraw)*HOUR_IN_MSEC+
				Math.floor(minutesToDraw)*MIN_IN_MSEC)))/SEC_IN_MSEC;
		float centerX = getWidth()/2;
		float centerY = getHeight()/2;
		float radius = Math.min(centerX, centerY);
		RectF hourRect = new RectF(centerX-radius,centerY-radius,centerX+radius,centerY+radius);
		RectF minRect = new RectF(centerX-radius*CIRCLEFACTOR,centerY-radius*CIRCLEFACTOR,
				centerX+radius*CIRCLEFACTOR,centerY+radius*CIRCLEFACTOR);
		RectF secRect = new RectF(centerX-radius*CIRCLEFACTOR*CIRCLEFACTOR,
				centerY-radius*CIRCLEFACTOR*CIRCLEFACTOR,
				centerX+radius*CIRCLEFACTOR*CIRCLEFACTOR,
				centerY+radius*CIRCLEFACTOR*CIRCLEFACTOR);
		c.drawArc(hourRect, -90, (hoursToDraw/24)*360, true, m_paints[0]);
		c.drawArc(minRect, -90, (minutesToDraw/60)*360, true, m_paints[1]);
		c.drawArc(secRect, -90, (secondsToDraw/60)*360, true, m_paints[2]);
		c.drawCircle(centerX, centerY, radius*CIRCLEFACTOR*CIRCLEFACTOR*CIRCLEFACTOR, m_paints[3]);
		String timeString = String.format("%02d:%02d:%02d", (int) hoursToDraw, (int) minutesToDraw, 
				(int) secondsToDraw);
		while(m_textPaint.measureText(timeString) < radius*CIRCLEFACTOR*CIRCLEFACTOR*CIRCLEFACTOR)
			m_textPaint.setTextSize(m_textPaint.getTextSize()+1);
		c.drawText(timeString, centerX, centerY, m_textPaint);
		m_lastDraw = System.currentTimeMillis();
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
}
