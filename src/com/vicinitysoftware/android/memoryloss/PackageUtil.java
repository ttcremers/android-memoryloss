package com.vicinitysoftware.android.memoryloss;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

public class PackageUtil {
	private static final ArrayList<ApplicationMeta> APPLICATION_META = 
			new ArrayList<ApplicationMeta>();
	
	public PackageUtil(Context context) {
		// We mem cache our app data if we can
		if ( APPLICATION_META.size() < 0 ) { 
			final PackageManager pm = context.getPackageManager();
			List<PackageInfo> packs = pm.getInstalledPackages(0);
			for ( int i=0; i<packs.size(); i++ ) {
				ApplicationMeta am = new ApplicationMeta();
				PackageInfo p = packs.get(i);
				am.dataDir = new File(p.applicationInfo.dataDir);
				
				am.installDate = p.firstInstallTime;
				am.icon =  p.applicationInfo.loadIcon(pm);
				am.appName = p.applicationInfo.loadLabel(pm).toString();
				APPLICATION_META.add(am);
			}
			
			// Here we sort out list to show the biggest and least used package first
			Collections.sort(APPLICATION_META, new Comparator<ApplicationMeta>() {
				@Override
				public int compare(ApplicationMeta lhs, ApplicationMeta rhs) {
					return 0;
				}});
		}
	}
	
	public ArrayList<ApplicationMeta> getApplications() {
		return APPLICATION_META;
	}

	private class ApplicationMeta {
		File dataDir;		
		long installDate;
		Drawable icon;
		String appName;
		
		@Override
		public String toString() {
			StringBuffer rv = new StringBuffer();
			rv.append("meta:[");
			rv.append(dataDir.getAbsolutePath());
			rv.append(",");
			rv.append(String.valueOf(installDate));
			rv.append(",");
			rv.append(appName);
			rv.append("]");
			return rv.toString();
		}
	}
}
