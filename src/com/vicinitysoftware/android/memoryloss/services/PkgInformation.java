package com.vicinitysoftware.android.memoryloss.services;

import java.io.Serializable;
import java.util.Date;

import org.joda.time.DateTime;

import android.util.Log;

public class PkgInformation implements Serializable {
	private static final long serialVersionUID = -8770364457873710287L;
	
	private DateTime lastActive;
	private long size;
	private String displayName;
	private String packageNamespace;
	private int weight;

	@Override
	public int hashCode() {
		return packageNamespace.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		Log.w("pkgInformation", "Comparing: " + packageNamespace + " with " + obj);
		String other = (String) obj;
		if (packageNamespace == null) {
			if (other != null)
				return false;
		} else if (!packageNamespace.equals(other))
			return false;
		return true;
	}

	public String getPackageNamespace() {
		return packageNamespace;
	}
	public void setPackageNamespace(String packageNamespace) {
		this.packageNamespace = packageNamespace;
	}
	public DateTime getLastActive() {
		return lastActive;
	}
	public void setLastActive(DateTime lastActive) {
		this.lastActive = lastActive;
	}
	public long getSize() {
		return size;
	}
	public void setSize(long l) {
		this.size = l;
	}
	public String getDisplayName() {
		return displayName;
	}
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public int getWeight() {
		return weight;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}
}
