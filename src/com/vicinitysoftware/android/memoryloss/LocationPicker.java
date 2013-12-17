package com.vicinitysoftware.android.memoryloss;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.bugsense.trace.BugSenseHandler;
import com.squareup.otto.Subscribe;
import com.vicinitysoftware.android.memoryloss.R.id;
import com.vicinitysoftware.android.memoryloss.fragments.DeviceListFragment;
import com.vicinitysoftware.android.memoryloss.fragments.DeviceListFragment.DeviceListInterface;
import com.vicinitysoftware.android.memoryloss.services.LogMonitService;
import com.vicinitysoftware.android.memoryloss.services.PkgInformation;

public class LocationPicker extends Activity implements DeviceListInterface {
	private static final String LOG_TAG = "MemoryLoss";
	public static final boolean DEVELOPER_MODE = true;
	
	private PackageArrayList<PkgInformation> packageMetaData;
	private FrameLayout locationPickerContainerView;
	private boolean isListItemExpanded = false;
	private View previousExpandedView;
	
	private Handler handler = new Handler();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if ( DEVELOPER_MODE ) {
			Util.strictMode();
		}				
		BugSenseHandler.initAndStartSession(this, "ec55b9e6");
		super.onCreate(savedInstanceState);
		
		Intent intent = new Intent(this, LogMonitService.class);
		PendingIntent sender = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		AlarmManager am = (AlarmManager) getSystemService(Activity.ALARM_SERVICE);
		am.cancel(sender); // cancel any existing alarms
        
		FragmentManager fm = getFragmentManager();
		FirstStartDialog firstStartDialog = new FirstStartDialog();
		firstStartDialog.show(fm, "first_start_dialog");
		
		setContentView(R.layout.activity_location_picker);
		locationPickerContainerView = (FrameLayout) findViewById(R.id.activity_location_container);		
	}
	
	@Override
	protected void onPause() {
		new MainThreadBus().unregister(this);
		BugSenseHandler.closeSession(this);
		super.onPause();
	}
	
	@Override
	protected void onResume() {
		new MainThreadBus().register(this);
		super.onResume();
	}
	
	// Response to BUS messages
	@Subscribe public void answerAvailable(PackageArrayList<PkgInformation> event) {
		if ( !isListItemExpanded ) {			
			packageMetaData = event;
			handler.post(new Runnable() {				
				@Override
				public void run() {
					DeviceListFragment dlf = new DeviceListFragment();
					FragmentTransaction ft = getFragmentManager().beginTransaction();
					if (locationPickerContainerView != null) {
						ft.replace(locationPickerContainerView.getId(), dlf).commit();
					}
				}
			});
		} else {
			Log.i(LOG_TAG, "Item expanded skipping refresh");
		}
	}

	@Override
	public void onDeviceClicked(View v, int postion) {		
		LinearLayout buttonBar = (LinearLayout)v.findViewById(id.button_bar);
		
		if ( isListItemExpanded ) {
			buttonBar.setVisibility(View.GONE);
			// Check if we were already expanded and if so restore view state
			if ( previousExpandedView != null ) {
				LinearLayout prevButtonBar = (LinearLayout)
						previousExpandedView.findViewById(id.button_bar);
				prevButtonBar.setVisibility(View.GONE);
				prevButtonBar.forceLayout();
				previousExpandedView = null;
				// Recurse back into the function which will take care 
				// of sliding open the listview item that was clicked
				onDeviceClicked(v, postion);
			} 
			isListItemExpanded = false;
		} else {
			String pkg = packageMetaData.get(postion).getPackageNamespace();
			buttonBar.findViewById(id.button_uninstall).setOnClickListener(
					getOnClickUninstall(pkg, v, postion));
			buttonBar.findViewById(id.button_launch).setOnClickListener(getOnClickLaunch(pkg));
			buttonBar.findViewById(id.button_playstore).setOnClickListener(getOnClickPlaystore(pkg));
			buttonBar.setVisibility(View.VISIBLE);
			isListItemExpanded = true;
			previousExpandedView = v;
		}
		v.forceLayout();
	}
	
	private OnClickListener getOnClickPlaystore(final String pkg) {
		return new OnClickListener() {
			@Override
			public void onClick(View v) {
				String url = "https://play.google.com/store/apps/details?id=" + pkg;
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setData(Uri.parse(url));
				startActivity(i);
			}
		};
	}
	
	private OnClickListener getOnClickLaunch(final String pkg) {
		return new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent LaunchIntent = getPackageManager()
						.getLaunchIntentForPackage(pkg);
				startActivity(LaunchIntent);
			}
		};
	}
	
	private OnClickListener getOnClickUninstall(final String pkg, final View row, final int position) {
		return new OnClickListener() {
			@Override
			public void onClick(View v) {
				Uri packageURI = Uri.parse("package:"+pkg);
				Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
				startActivity(uninstallIntent);
				packageMetaData.remove(position);
				DeviceListFragment dlf = new DeviceListFragment();
				FragmentTransaction ft = getFragmentManager().beginTransaction();
				if (locationPickerContainerView != null) {
					ft.replace(locationPickerContainerView.getId(), dlf).commit();
				}
			}
		};
	}

	@Override
	public void onReadyForPopulation(DeviceListFragment deviceListFragment) {
		deviceListFragment.setAndPopulateAdapter(packageMetaData);
	}

	@Override
	public void onInitialStartCompleted(ArrayList<String> pkgs) {
		new MainThreadBus().register(this);	
		
		String[] result = new String[20];
		pkgs.toArray(result);
		Intent intent = new Intent(this, LogMonitService.class);
		intent.putExtra("selected_packages", result);
		PendingIntent sender = PendingIntent.getService(this, 0, 
				intent, PendingIntent.FLAG_UPDATE_CURRENT);
		AlarmManager am = (AlarmManager) getSystemService(Activity.ALARM_SERVICE);
		am.cancel(sender); // cancel any existing alarms
		// Math.round(AlarmManager.INTERVAL_FIFTEEN_MINUTES / 6)
		am.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis(), 
				10000, sender);		
	}
}
