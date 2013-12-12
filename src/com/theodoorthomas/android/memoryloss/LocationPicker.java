package com.theodoorthomas.android.memoryloss;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;

import com.squareup.otto.Subscribe;
import com.theodoorthomas.android.memoryloss.fragments.DeviceListFragment;
import com.theodoorthomas.android.memoryloss.fragments.InformationFragment;
import com.theodoorthomas.android.memoryloss.fragments.DeviceListFragment.DeviceListInterface;
import com.theodoorthomas.android.memoryloss.services.LogMonitService;
import com.theodoorthomas.android.memoryloss.services.PkgInformation;
import com.theodoorthomas.android.memoryloss.services.ScheduleReciever;
import com.mobileapptracker.*;

import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.content.Intent;

public class LocationPicker extends Activity implements DeviceListInterface {
	private static final String LOG_TAG = "MemoryLoss";
	public static final boolean DEVELOPER_MODE = true;
	
	private MainThreadBus bus = new MainThreadBus();
	private ArrayList<PkgInformation> packageMetaData;
	private FrameLayout locationPickerContainerView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if ( DEVELOPER_MODE ) {
			Util.strictMode();
		}				
		BugSenseHandler.initAndStartSession(this, "ec55b9e6");
		super.onCreate(savedInstanceState);
        
		Intent intent = new Intent(this, 
				LogMonitService.class);
		PendingIntent sender = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
		AlarmManager am = (AlarmManager) getSystemService(Activity.ALARM_SERVICE);
		am.cancel(sender); // cancel any existing alarms
		// AlarmManager.INTERVAL_FIFTEEN_MINUTES
		am.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis(), 
				Math.round(AlarmManager.INTERVAL_FIFTEEN_MINUTES / 6), sender);
		
		setContentView(R.layout.activity_location_picker);
		locationPickerContainerView = (FrameLayout) findViewById(R.id.activity_location_container);
		bus.register(this);	
	}
	
	@Override
	protected void onPause() {
		new MainThreadBus().unregister(this);
		super.onPause();
	}
	
	@Override
	protected void onResume() {
		new MainThreadBus().register(this);
		super.onResume();
	}
	
	// Response to BUS messages
	@Subscribe public void answerAvailable(ArrayList<PkgInformation> event) {
		Log.w(LOG_TAG, "Recieved message on bus");		
		packageMetaData = event;
		
		DeviceListFragment dlf = new DeviceListFragment();
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		if (locationPickerContainerView != null) {
			ft.replace(locationPickerContainerView.getId(), dlf).commit();
		}
	}

	@Override
	public void onDeviceClicked(File data) {		
		Log.d(LOG_TAG, "Implementing Activity recieved "
				+ "click from deviceListFragment id: " + data);
		
		InformationFragment infoFragment = new InformationFragment();
		infoFragment.setData(data);
		
		getFragmentManager().beginTransaction().setCustomAnimations(
                R.animator.card_flip_right_in, R.animator.card_flip_right_out,
                R.animator.card_flip_left_in, R.animator.card_flip_left_out)
        	.replace(R.id.activity_location_container, infoFragment)
        	.addToBackStack(data.getName()).commit();
	}

	@Override
	public void onReadyForPopulation(DeviceListFragment deviceListFragment) {
		Log.d(LOG_TAG, "Recieved data from the bus with which we can fill the list view");
		deviceListFragment.setAndPopulateAdapter(packageMetaData);
	}
}
