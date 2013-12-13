package com.vicinitysoftware.android.memoryloss;

import com.vicinitysoftware.android.memoryloss.services.LogMonitService;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.Toast;

public class DeviceBootReciever extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i("MemoryLoss", "Starting MemoryLoss app monitor service");
		Intent monitService = new Intent(context, 
				LogMonitService.class);
		PendingIntent sender = PendingIntent.getService(context, 0, monitService, PendingIntent.FLAG_UPDATE_CURRENT);
		AlarmManager am = (AlarmManager) context.getSystemService(Activity.ALARM_SERVICE);
		am.cancel(sender); // cancel any existing alarms
		// Math.round(AlarmManager.INTERVAL_FIFTEEN_MINUTES / 6)
		am.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis(), 
				10000, sender);
	}

}
