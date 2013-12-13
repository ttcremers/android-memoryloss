package com.vicinitysoftware.android.memoryloss;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.graphics.drawable.shapes.RoundRectShape;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class UsageIndicatorView extends View {
	private static final String TAG="UsageIndicatorView";
	
	private ShapeDrawable mDrawable;
	private long totalValue = 0;
	private long partValue = 0;
	
	public UsageIndicatorView(Context context) {
		super(context);
		setup();
	}
	
	public UsageIndicatorView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setup();
	}
	
	public UsageIndicatorView(Context context, AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		setup();
	}
	
	private void setup() {
		mDrawable = new ShapeDrawable(new RoundRectShape(
				new float [] { 4f, 4f, 4f, 4f, 4f, 4f, 4f, 4f },
				null, 
				null));
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		// Draw Total in a green bar
		mDrawable.getPaint().setColor(Color.GREEN);
		mDrawable.setBounds(0, 0, getWidth(), getHeight());
		mDrawable.draw(canvas);
		
		// Draw percentage of Total in Red
		mDrawable.getPaint().setColor(Color.RED);
		mDrawable.setBounds(0, 0, calculateRedWidth(getWidth()), getHeight());
		mDrawable.draw(canvas);
	}
	
	private int calculateRedWidth(int width) {
		float percent = (partValue * 100.0f) / totalValue;
		//Log.d(TAG, "usage "+ String.valueOf(percent) + " percent");
		float percentOfWidth = (percent / 100.0f) * width;
		return (int)percentOfWidth;
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		//Log.d(TAG, "onMeasure triggered: " + widthMeasureSpec + "x" + heightMeasureSpec);
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
	}

	public long getTotalValue() {
		return totalValue;
	}

	public void setTotalValue(long totalValue) {
		this.totalValue = totalValue;
	}

	public long getPartValue() {
		return partValue;
	}

	public void setPartValue(long partValue) {
		this.partValue = partValue;
	}
}
