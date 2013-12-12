package com.theodoorthomas.android.memoryloss.fragments;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;

import org.joda.time.JodaTimePermission;

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import com.google.common.collect.ComparisonChain;
import com.theodoorthomas.android.memoryloss.DevicesListAdapter;
import com.theodoorthomas.android.memoryloss.PackageArrayList;
import com.theodoorthomas.android.memoryloss.R;
import com.theodoorthomas.android.memoryloss.services.PkgInformation;

public class DeviceListFragment extends ListFragment {
	private static final String LOG_TAG = "MemoryLoss";
	private DeviceListInterface listener;
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);		
	}

	public void setAndPopulateAdapter(PackageArrayList<PkgInformation> packageSizeMeta) {
		if ( packageSizeMeta != null) { 			
			
			Collections.sort(packageSizeMeta, new Comparator<PkgInformation>() {
				@Override
				public int compare(PkgInformation lhs, PkgInformation rhs) {
					int sizeComp = Long.valueOf(rhs.getSize()).compareTo(
							Long.valueOf(lhs.getSize()));
					int dateComp = lhs.getLastActive().withTimeAtStartOfDay()
							.compareTo(rhs.getLastActive().withTimeAtStartOfDay()); 

					if ( sizeComp == 1 && dateComp == 1) {
						return 1;
					}
					return sizeComp;	
				}
			});
			
			DevicesListAdapter<File> adapter = 
				new DevicesListAdapter<File>(getActivity(),
						R.layout.location_picker_list_row, packageSizeMeta);
			setListAdapter(adapter);
		} else {
			Log.w("TAG", "An attempt was made to populate the list adapter with null data");
		}		
	}

	@Override
	public void onAttach(Activity activity) {		
		super.onAttach(activity);
//		getListView().addHeaderView(
//				activity.getLayoutInflater().inflate(R.layout.location_picker_list_header, null));
		if (activity instanceof DeviceListInterface) {
			listener = (DeviceListInterface) activity;
			listener.onReadyForPopulation(this);
		} else {
			throw new ClassCastException(activity.toString()
					+ " must implemenet DeviceListInterface");
		}
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Log.d(LOG_TAG, "List view click: " + id);
		listener.onDeviceClicked(v, position);
	}
	
	@Override
	public void onPause() {
		super.onPause();
	}
	
	/**
	 * Interface for communicating between fragment and calling activity
	 * 
	 * @author thomas
	 *
	 */
	public interface DeviceListInterface {
		public void onDeviceClicked(View v, int position);
		public void onReadyForPopulation(DeviceListFragment deviceListFragment);
	}
}
