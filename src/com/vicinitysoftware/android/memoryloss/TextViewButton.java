package com.vicinitysoftware.android.memoryloss;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.TextView;

public class TextViewButton extends TextView {
	private final Context context;
	
	// TODO refactor this back to xml 
	private static final int configuredBGColor = android.R.color.holo_blue_dark;
	private static final int configuredBGHighlightColor = android.R.color.holo_blue_bright;
	
	public TextViewButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN: {
			setBackgroundResource(configuredBGHighlightColor);
			break;
		}
		case MotionEvent.ACTION_UP: {
			setBackgroundResource(configuredBGColor);
			performClick();
			break;
		}
		}
		return true;
	}
}
