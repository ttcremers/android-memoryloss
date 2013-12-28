package com.vicinitysoftware.android.memoryloss.services;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.joda.time.DateTime;

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
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.bugsense.trace.BugSenseHandler;
import com.vicinitysoftware.android.memoryloss.MainThreadBus;
import com.vicinitysoftware.android.memoryloss.PackageArrayList;

public class LogMonitService extends Service {	
	private static final String TAG = "MemoryLossService";
	public static final String OBJECT_CACHE_FILE = "object.cache";
	
	private PackageArrayList<PkgInformation> packageLaunchInformation;

	private MainThreadBus bus = new MainThreadBus();
	private PackageManager pm;
	private String[] frequentlyUsedPkgs;	
	
	private void updateAppMetaData() {
		Log.i(TAG, "Updating package stats");
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
			Log.e(TAG, "Error comminicating with object cache ", e);
		} finally {
			try {
				if ( fileOutputStream != null )
					fileOutputStream.close();
			} catch (IOException e) {
				Log.e(TAG, "Error closing object cache", e);
			}
		}
	}
	
	@Override
	public void onCreate() {
		super.onCreate();	
		BugSenseHandler.initAndStartSession(this, "ec55b9e6");
	}
	
	private int calculateEntryWeight(PkgInformation pkgInfo) {
		boolean isStale = pkgInfo.getLastActive()
				.isBefore(new DateTime().minusDays(10));
		boolean isOld = pkgInfo.getLastActive()
				.isBefore(new DateTime().minusMonths(1));
		boolean isBig = pkgInfo.getSize() > 104857600;
		boolean isHuge = pkgInfo.getSize() > 1073741824;
		 
		if ( isOld && isBig ) 
			return 2; // Red
		else if ( isStale && isHuge ) 
			return 2; // Red
		else if ( isOld ) 
			return 1; // Orange
			
		return 0; // Green
	}

	private void getInstalledPackages() {		
		List<PackageInfo> list = pm.getInstalledPackages(0);		
		for (PackageInfo pi : list) {
			try {
				ApplicationInfo ai = pm.getApplicationInfo(pi.packageName,0);
				if (pm.getLaunchIntentForPackage(pi.packageName) != null &&
						!ai.sourceDir.contains("/system")) {
					
					PkgInformation pkgInfo = new PkgInformation();
					pkgInfo.setPackageNamespace(pi.packageName);											
					pkgInfo.setSize(getInstallAppSizeHack(pkgInfo));
					
					if ( packageLaunchInformation.contains(pi.packageName) ) {
						updatePkgLaunchInfoForApp(pi.packageName, pkgInfo.getSize(), null);
					} else {
						if (Arrays.asList(frequentlyUsedPkgs).contains(pi.packageName)) {
							Log.w(TAG, "First run notification, updating run time for package: " + pi.packageName);
							pkgInfo.setLastActive(new DateTime());
						} else {
							pkgInfo.setLastActive(new DateTime(pi.lastUpdateTime));
						}
						pkgInfo.setDisplayName((String)
								getPackageManager().getApplicationLabel(ai));
						getInstallAppSizeHack(pkgInfo);
						pkgInfo.setWeight(calculateEntryWeight(pkgInfo));
						packageLaunchInformation.add(pkgInfo);
					}
				}
			} catch (NameNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	@SuppressLint("NewApi") private synchronized long getInstallAppSizeHack(final PkgInformation pkgInfo) {
		final long[] rvContainer = new long[1];
		
		// Start a Semaphore so we can have a method that actually return a long eg: wait until we have a value
		final Semaphore semaphore = new Semaphore(0);
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Method getPackageSizeInfo;
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
							
							rvContainer[0] = pkgInfo.getSize();
							semaphore.release();
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
		}).start();
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return rvContainer[0];
	}

	private void getRunningPackages() {		
		ActivityManager am = (ActivityManager)this.getSystemService(ACTIVITY_SERVICE);
		List<RunningAppProcessInfo> runningProcesses = am.getRunningAppProcesses();
		for (RunningAppProcessInfo rpi : runningProcesses) {
			if (packageLaunchInformation.contains(rpi.processName)) {
				updatePkgLaunchInfoForApp(rpi.processName, 0, new DateTime());
			} 
		} 
	}

	private void updatePkgLaunchInfoForApp(String processName, long size, DateTime date) {
		synchronized (packageLaunchInformation) {
			for ( int i = 0; i < packageLaunchInformation.size(); i++ ) {
				PkgInformation pkgInfo = packageLaunchInformation.get(i);
				if ( pkgInfo.getPackageNamespace().equals(processName) ) {	
					Log.i(TAG, "Updating data for: " + pkgInfo.getPackageNamespace());
					DateTime newDate = date == null ? pkgInfo.getLastActive() : date;
					pkgInfo.setLastActive(newDate);
					Long newSize = size == 0 ? pkgInfo.getSize() : size;
					pkgInfo.setSize(newSize);
					pkgInfo.setWeight(calculateEntryWeight(pkgInfo));
					packageLaunchInformation.remove(i);		
					packageLaunchInformation.trimToSize();
					packageLaunchInformation.add(pkgInfo);					
				} 
			}
		}
	}
	
    private synchronized void handleIntent(Intent intent) {
    	Bundle extras = intent.getExtras();
    	if (extras.containsKey("selected_packages")) {
    		frequentlyUsedPkgs = extras.getStringArray("selected_packages");
    	}
        // do the actual work, in a separate thread
        new PollTask().execute();
    }
	
	private class PollTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {			
			//Open file and read the saved object back.
			FileInputStream fileInputStream = null;
			try {
				if ( getFileStreamPath(LogMonitService.OBJECT_CACHE_FILE).exists() ) {
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
		handleIntent(intent);
		return Service.START_NOT_STICKY;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		BugSenseHandler.closeSession(this);
	}
}
