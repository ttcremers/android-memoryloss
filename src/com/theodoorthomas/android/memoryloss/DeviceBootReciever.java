package com.theodoorthomas.android.memoryloss;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class DeviceBootReciever extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		Log.d("STORAGEDEVICERECIEVER", "I recieved a message! " + intent.getAction());
		if (intent.getAction().equalsIgnoreCase(
		        "android.intent.action.UMS_CONNECTED")) {
			Toast.makeText(context, "Device Connect recieved", Toast.LENGTH_LONG).show();
		}
	}

}
