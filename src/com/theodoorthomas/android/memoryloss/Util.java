package com.theodoorthomas.android.memoryloss;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.StrictMode;
import android.util.Log;

public class Util {
	private static final String TAG = "UTIL";
	private static final HashMap<File, Long> DIRECTORY_SIZE_MAP = new HashMap<File, Long>();
	// Since we don't really want to rescan for devices every time lets mem cache
	private static File[] STORAGE_POINTS;
	private static HashMap<String,String> FILE_SYSTEM_TYPES = 
			new HashMap<String,String>();

	public static String getFSTypeForMountPoint(String mp) {
		if ( STORAGE_POINTS == null ) {
			STORAGE_POINTS = parseStorageDirectories();
		}
		return FILE_SYSTEM_TYPES.get(mp);
	}

	public static File[] getStoragePoints() {
		if ( STORAGE_POINTS == null ) {
			STORAGE_POINTS = parseStorageDirectories();
		}
		return STORAGE_POINTS;
	}

	/**
	 * Similar to android.os.Environment.getExternalStorageDirectory(), except that
	 * here, we return all possible storage directories. The Environment class only
	 * returns one storage directory. If you have an extended SD card, it does not
	 * return the directory path. Here we are trying to return all of them.
	 *
	 * @return
	 */
	private static File[] parseStorageDirectories() { 
		File[] dirs = null;
		BufferedReader bufReader = null;
		try {
			bufReader = new BufferedReader(new FileReader("/proc/mounts"));
			ArrayList<File> list = new ArrayList<File>();
			list.add(Environment.getExternalStorageDirectory());
			// For now just hard code the root end point, mainly for testing purposes
			list.add(new File("/"));
//			list.add(new File(Environment.DIRECTORY_DOWNLOADS));
//			list.add(new File(Environment.DIRECTORY_MOVIES));
//			list.add(new File(Environment.DIRECTORY_MUSIC));
//			list.add(new File(Environment.DIRECTORY_PICTURES));	
//			list.add(new File(Environment.DIRECTORY_PODCASTS));
//			list.add(new File(Environment.DIRECTORY_RINGTONES));
			
			String line;
			while ((line = bufReader.readLine()) != null) {
				if (line.contains("vfat") || line.contains("/mnt")) {
					StringTokenizer tokens = new StringTokenizer(line, " ");
					String s = tokens.nextToken();
					s = tokens.nextToken(); // Take the second token, i.e. mount point
					String fsType = tokens.nextToken();
					Log.d(TAG, "Found mountpoint: " + s + " with fs type: " + fsType);

					if (s.equals(Environment.getExternalStorageDirectory().getPath())) {
						FILE_SYSTEM_TYPES.put(s, fsType);
						continue; // We already added the directory that Android returns 
					} else if (line.contains("/dev/block/vold")) {
						if (!line.contains("/mnt/secure") && 
								!line.contains("/mnt/asec") && 
								!line.contains("/mnt/obb") && 
								!line.contains("/dev/mapper") &&
								!line.contains("tmpfs")) {
							list.add(new File(s));							
							FILE_SYSTEM_TYPES.put(s, fsType);
						}
					}
				}
			}
			dirs = new File[list.size()];
			for (int i = 0; i < list.size(); i++) {
				dirs[i] = list.get(i);
			}
		} catch (FileNotFoundException e) {
			Log.w("MemoryLossUtil", e);
		} catch (IOException e) {
			Log.w("MemoryLossUtil", e);
		} finally {
			if (bufReader != null) {
				try { bufReader.close(); }
				catch (IOException e) {
					Log.w("MemoryLossUtil", e);
				}
			}
		}
		return dirs;
	}
	
	@SuppressLint("NewApi") 
	public static long getDirectorySize(File directory) {
	    StatFs statFs = new StatFs(directory.getAbsolutePath());
	    long blockSize;
	    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
	        blockSize = statFs.getBlockSizeLong();
	    } else {
	        blockSize = statFs.getBlockSize();
	    }
	    Log.d(TAG, "Detected block-size: " + blockSize);
	    return getDirectorySize(directory, blockSize);
	}
	
	public static long getDirectorySize(File directory, long blockSize) {
		long size = 0;
		if ( DIRECTORY_SIZE_MAP.get(directory) != null ) {
			size = DIRECTORY_SIZE_MAP.get(directory);
		} else {
			File[] files = directory.listFiles();
			// space used by directory itself
			Log.d(TAG, "Scanning directory: " + directory.getAbsolutePath());
			size = directory.length();
			if ( files != null ) {
				for (File file : files) {
					Log.d(TAG, "	Entry: " + file.getAbsolutePath());
					if (file.isDirectory() && 
							!(file.getAbsolutePath().contains("sys") ||
							  file.getAbsolutePath().contains("proc") ||
							  file.getAbsolutePath().contains("dev") ||
							  (file.getAbsolutePath().contains("storage") && !directory.getAbsolutePath().contains("storage")))) {
						// space used by subdirectory
						size += getDirectorySize(file, blockSize);
					} else {
						// file size need to rounded up to full block sizes
						// (not a perfect function, it adds additional block to 0 sized files
						// and file who perfectly fill their blocks) 
						size += (file.length() / blockSize + 1) * blockSize;
					}
				}
				
			}
			DIRECTORY_SIZE_MAP.put(directory, size);			
		}
		
	    //Log.d(TAG, "TOTAL SIZE: " + size);
	    return size;
	}

	public static void strictMode() {
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
		.detectAll()
		.penaltyLog()
		.build());
		StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
		.detectLeakedSqlLiteObjects()
		.detectLeakedClosableObjects()
		.penaltyLog()
		.penaltyDeath()
		.build());
	}
}
