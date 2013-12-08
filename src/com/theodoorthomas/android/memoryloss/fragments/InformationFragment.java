package com.theodoorthomas.android.memoryloss.fragments;

import java.io.File;
import java.text.DecimalFormat;

import com.theodoorthomas.android.memoryloss.R;
import com.theodoorthomas.android.memoryloss.Util;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class InformationFragment extends Fragment {
	private static final String TAG = "InformationFragment";
	private static float GB = 1073741824;
	private File data;
	private final DecimalFormat format = new DecimalFormat("#.##");
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.information_hover,
		        container, false);
		
		TextView descr = (TextView)view.findViewById(R.id.HoverDescription);
		if ( descr != null )
			descr.setText("The Location " + data + " is used to do completely great and interesting stuff just not yet.");
		
		TextView entryDiskSize = 
				(TextView) view.findViewById(R.id.table_value_size);
		Log.d(TAG, "Location size in bytes: " + data.getFreeSpace());
		float diskSizeMBs = data.getTotalSpace() / GB;
		entryDiskSize.setText(format.format(diskSizeMBs) + "GB");	
		
		TextView entryFilesystemType = 
				(TextView) view.findViewById(R.id.entry_filesystem_value);
		entryFilesystemType.setText(Util.getFSTypeForMountPoint(data.getAbsolutePath()));
			
		return view;
	}

	public void setData(File data) {
		this.data = data;
	}
}
