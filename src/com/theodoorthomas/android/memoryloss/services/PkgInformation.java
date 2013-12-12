package com.theodoorthomas.android.memoryloss.services;

import java.io.Serializable;
import java.util.Date;

import android.util.Log;

public class PkgInformation implements Serializable, Comparable<PkgInformation> {
	private static final long serialVersionUID = -8770364457873710287L;
	
	private Date lastActive;
	private long size;
	private String displayName;
	private String packageNamespace;

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
	public Date getLastActive() {
		return lastActive;
	}
	public void setLastActive(Date lastActive) {
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

	@Override
	public int compareTo(PkgInformation another) {
		return Long.valueOf(another.getSize()).compareTo(this.getSize());
	}
}
