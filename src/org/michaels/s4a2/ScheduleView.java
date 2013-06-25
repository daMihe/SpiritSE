package org.michaels.s4a2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;


public class ScheduleView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

	Thread m_drawThread;
	boolean m_run;
	float m_move = 0.0f;
	SurfaceHolder m_holder;
	
	public ScheduleView(Context context) {
		super(context);
		init();
	}

	private void init() {
		m_run = false;
		setBackgroundColor(Color.parseColor("#fff3f3f3"));
		
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
		Paint p = new Paint();
		p.setColor(Color.GREEN);
		c.drawCircle(m_move, 0, 100, p);
		m_move++;
		if(m_move > getWidth())
			m_move = 0.0f;
	}

	@Override
	public void run() {
		while(m_run){
			postInvalidate();
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {}
		}
	}
}
