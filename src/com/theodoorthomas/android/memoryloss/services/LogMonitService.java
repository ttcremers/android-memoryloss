package com.theodoorthomas.android.memoryloss.services;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageStats;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.util.Log;

import com.squareup.otto.Produce;
import com.theodoorthomas.android.memoryloss.MainThreadBus;
import com.theodoorthomas.android.memoryloss.PackageArrayList;

public class LogMonitService extends Service {	
	private static final String TAG = "MemoryLossService";
	private static PackageArrayList<PkgInformation> packageLaunchInformation = 
			PackageArrayList.getInstance();

	private MainThreadBus bus = new MainThreadBus();
	private PackageManager pm;	
	
	private WakeLock mWakeLock;
	
	private void updateAppMetaData() {
		Log.d(TAG, "Updating package stats!....");
		getInstalledPackages();
		getRunningPackages();
		smartSortPackageInformation();
		bus.post(packageLaunchInformation);
		Log.d(TAG, "Updating package stats! DONE");
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		BugSenseHandler.initAndStartSession(this, "ec55b9e6");
	}

	@Override
	public void onDestroy() {
		mWakeLock.release();
		super.onDestroy();
	}
	
	private void smartSortPackageInformation() {
		Collections.sort(packageLaunchInformation, new Comparator<PkgInformation>() {
			public int compare(PkgInformation pkgInfo1, PkgInformation pkgInfo2) {
				long t1 = pkgInfo1.getLastActive().getTime();
			    long t2 = pkgInfo2.getLastActive().getTime();
			    if (t2 > t1)
			    	return -1;
			    else if (t1 > t2)
			    	return 1;
			    else
			    	return 0;
			}
		});
		Collections.sort(packageLaunchInformation, new Comparator<PkgInformation>() {
			public int compare(PkgInformation pkgInfo1, PkgInformation pkgInfo2) {
				long size1 = pkgInfo1.getSize();
				long size2 = pkgInfo2.getSize();
				if (size1 == size2)
					return 0;
				else if (size1 > size2)
					return -1;
				else
					return 1;
			}
		});
	}

	// As soon as a client connects to the bus send data
	@Produce public PackageArrayList<PkgInformation> produceAnswer() {
	    // Assuming 'packageLaunchInformation' is filled.
		Log.d(TAG, "Client connected to bus, lets send it our latest cached data");
	    return packageLaunchInformation;
	}

	private void getInstalledPackages() {		
		List<PackageInfo> list = pm.getInstalledPackages(0);		
		for (PackageInfo pi : list) {
			try {
				ApplicationInfo ai = pm.getApplicationInfo(pi.packageName,0);
				if (pm.getLaunchIntentForPackage(pi.packageName) != null) {
					if (packageLaunchInformation.contains(pi.packageName)) {
						PkgInformation pkgInfo = new PkgInformation();
						pkgInfo.setPackageNamespace(pi.packageName);
						getInstallAppSizeHack(pkgInfo);
						updatePkgLaunchInfoForApp(pi.packageName, pkgInfo.getSize(), null);
					} else {
						PkgInformation pkgInfo = new PkgInformation();
						pkgInfo.setLastActive(new Date(pi.firstInstallTime));
						pkgInfo.setPackageNamespace(ai.packageName);
						pkgInfo.setDisplayName((String)
								getPackageManager().getApplicationLabel(ai));
						getInstallAppSizeHack(pkgInfo);					
						packageLaunchInformation.add(pkgInfo);
					}
				}
			} catch (NameNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	@SuppressLint("NewApi") private void getInstallAppSizeHack(final PkgInformation pkgInfo) {
		Method getPackageSizeInfo;
		try {
			getPackageSizeInfo = getPackageManager().getClass().getMethod(
					"getPackageSizeInfo", String.class, IPackageStatsObserver.class);
			getPackageSizeInfo.invoke(getPackageManager(), pkgInfo.getPackageNamespace(),
					new IPackageStatsObserver.Stub() {

				public void onGetStatsCompleted(PackageStats pStats, boolean succeeded)
						throws RemoteException {

					long externalCodeSize = 0;
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
						externalCodeSize = pStats.externalCodeSize;
					}
					
					pkgInfo.setSize(pStats.codeSize + 
							pStats.dataSize + 
							pStats.cacheSize + 
							pStats.externalDataSize +
							pStats.externalCacheSize +
							externalCodeSize +
							pStats.externalMediaSize +
							pStats.externalObbSize);
				}
			});
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}
	
	private void getRunningPackages() {		
		ActivityManager am = (ActivityManager)this.getSystemService(ACTIVITY_SERVICE);
		List<RunningAppProcessInfo> runningProcesses = am.getRunningAppProcesses();
		for (RunningAppProcessInfo rpi : runningProcesses) {
			if (packageLaunchInformation.contains(rpi.processName)) {
				updatePkgLaunchInfoForApp(rpi.processName, 0, new Date());				
			} 
		} 
	}

	private void updatePkgLaunchInfoForApp(String processName, long size, Date date) {
		for ( int i = 0; i < packageLaunchInformation.size(); i++ ) {
			PkgInformation pkgInfo = packageLaunchInformation.get(i);
			if ( pkgInfo.getPackageNamespace().equals(processName) ) {	
				Log.w(TAG, "Updating data for: " + processName+ " to: " + date + " / " + size);
				if ( date != null) {
					Log.w(TAG, "Updating date for: " + processName+ " to: " + date);
				} 
				Date newDate = date == null ? packageLaunchInformation.get(i).getLastActive() : date;
				pkgInfo.setLastActive(newDate);
				long newSize = size == 0 ? packageLaunchInformation.get(i).getSize() : size;
				pkgInfo.setSize(newSize);
				packageLaunchInformation.remove(i);
				packageLaunchInformation.set(i, pkgInfo);
			} 
		}
	}
	
    private void handleIntent() {
        // obtain the wake lock
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        mWakeLock.acquire();
        
        // check the global background data setting
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (!cm.getBackgroundDataSetting()) {
            stopSelf();
            return;
        }
        
        // do the actual work, in a separate thread
        new PollTask().execute();
    }
	
	private class PollTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			pm = getPackageManager();
			updateAppMetaData();
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			stopSelf();
			super.onPostExecute(result);
		}
		
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		handleIntent();
		return Service.START_NOT_STICKY;
	}
}
