package com.theodoorthomas.android.memoryloss.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageStats;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.bugsense.trace.BugSenseHandler;
import com.theodoorthomas.android.memoryloss.MainThreadBus;
import com.theodoorthomas.android.memoryloss.PackageArrayList;

public class LogMonitService extends Service {	
	private static final String TAG = "MemoryLossService";
	private static final String OBJECT_CACHE_FILE = "object.cache";
	
	private PackageArrayList<PkgInformation> packageLaunchInformation;

	private MainThreadBus bus = new MainThreadBus();
	private PackageManager pm;	
	
	private void updateAppMetaData() {
		Log.d(TAG, "Updating package stats!...");
		getInstalledPackages();
		getRunningPackages();
		bus.post(packageLaunchInformation);
		
		FileOutputStream fileOutputStream = null;
		try {
			fileOutputStream = openFileOutput(OBJECT_CACHE_FILE, Context.MODE_PRIVATE);
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
			objectOutputStream.writeObject(packageLaunchInformation);
		} catch (FileNotFoundException e) {
			Log.e(TAG, "Error in accessing object cache", e);
		} catch (IOException e) {
			Log.e(TAG, "Error comminicating with object cache", e);
		} finally {
			try {
				if ( fileOutputStream != null )
					fileOutputStream.close();
			} catch (IOException e) {
				Log.e(TAG, "Error closing object cache", e);
			}
		}
		Log.d(TAG, "Updating package stats! DONE");
	}
	
	@Override
	public void onCreate() {
		super.onCreate();	
		BugSenseHandler.initAndStartSession(this, "ec55b9e6");
	}

//	// As soon as a client connects to the bus send data
//	@Produce public PackageArrayList<PkgInformation> produceAnswer() {
//	    // Assuming 'packageLaunchInformation' is filled.
//		Log.d(TAG, "Client connected to bus, lets send it our latest cached data");
//	    return packageLaunchInformation;
//	}

	private void getInstalledPackages() {		
		List<PackageInfo> list = pm.getInstalledPackages(0);		
		for (PackageInfo pi : list) {
			try {
				ApplicationInfo ai = pm.getApplicationInfo(pi.packageName,0);
				if (pm.getLaunchIntentForPackage(pi.packageName) != null &&
						!ai.sourceDir.contains("/system")) {
					if (packageLaunchInformation.contains(pi.packageName)) {
						PkgInformation pkgInfo = new PkgInformation();
						pkgInfo.setPackageNamespace(pi.packageName);
						getInstallAppSizeHack(pkgInfo);
						updatePkgLaunchInfoForApp(pi.packageName, pkgInfo.getSize(), null);
					} else {
						PkgInformation pkgInfo = new PkgInformation();
						pkgInfo.setLastActive(new Date(pi.lastUpdateTime));
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

					Long externalCodeSize = 0l;
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
						externalCodeSize = pStats.externalCodeSize;
					}
					
					pkgInfo.setSize(Long.valueOf(pStats.codeSize + 
							pStats.dataSize + 
							pStats.cacheSize + 
							pStats.externalDataSize +
							pStats.externalCacheSize +
							externalCodeSize +
							pStats.externalMediaSize +
							pStats.externalObbSize));
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
				Date newDate = date == null ? pkgInfo.getLastActive() : date;
				pkgInfo.setLastActive(newDate);
				Long newSize = size == 0 ? pkgInfo.getSize() : size;
				pkgInfo.setSize(newSize);
				packageLaunchInformation.remove(i);
				packageLaunchInformation.set(i, pkgInfo);
			} 
		}
	}
	
    private void handleIntent() {
        // do the actual work, in a separate thread
        new PollTask().execute();
    }
	
	private class PollTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {			
			//Open file and read the saved object back.
			FileInputStream fileInputStream = null;
			try {
				if (new File(OBJECT_CACHE_FILE).exists()) {
					fileInputStream = openFileInput(OBJECT_CACHE_FILE);
					//Open File Stream and cast it into array of ItemAttributes
					ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
					packageLaunchInformation = (PackageArrayList<PkgInformation>)objectInputStream.readObject();
				} else {
					packageLaunchInformation = new PackageArrayList<PkgInformation>();
				}
			} catch (FileNotFoundException e) {
				Log.e(TAG, "Error accessing cache file", e);
			} catch (StreamCorruptedException e) {
				Log.e(TAG, "Object cache curropted!!", e);
			} catch (IOException e) {
				Log.e(TAG, "Error reading object cache", e);
			} catch (ClassNotFoundException e) {
				Log.e(TAG, "Error in object cache class (read error)", e);
			} finally {
				if ( fileInputStream != null )
					try {
						fileInputStream.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
			}
			
			pm = getPackageManager();
			updateAppMetaData();
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			// Clean up
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
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		BugSenseHandler.closeSession(this);
	}
}
