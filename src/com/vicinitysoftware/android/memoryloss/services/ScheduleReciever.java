package com.vicinitysoftware.android.memoryloss.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ScheduleReciever extends BroadcastReceiver {

	private static final String TAG = "ScheduleReciever";

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.w(TAG, "Recieved a broadcast to the memoryloss service");
		// use this to start and trigger a service
		Intent i= new Intent(context, LogMonitService.class);
		context.startService(i);	
	}

}
