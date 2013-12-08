package com.theodoorthomas.android.memoryloss;

import java.util.ArrayList;
import java.util.Iterator;

import com.theodoorthomas.android.memoryloss.services.PkgInformation;

public class PackageArrayList<T> extends ArrayList<PkgInformation> {
	private static final long serialVersionUID = 3232141627726656599L;
	private static PackageArrayList<PkgInformation> self;

	public static PackageArrayList<PkgInformation> getInstance() {
		if ( self != null ) {
			return self;
		} else {
			return new PackageArrayList<PkgInformation>();
		}
	}
	
	private PackageArrayList() {		
		super();
	}

	public PackageArrayList(int capacity) {
		super(capacity);
	}

	@Override
	public boolean contains(Object object) {
		String packageName = (String)object;
		for (Iterator<PkgInformation> iterator = iterator(); iterator.hasNext();) {
			String entry = iterator.next().getPackageNamespace();			
			if (entry.equalsIgnoreCase(packageName)) {
				return true;
			}
		}
		return false;
	}
}
