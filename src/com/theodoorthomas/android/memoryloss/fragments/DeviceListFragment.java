package com.theodoorthomas.android.memoryloss.fragments;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.theodoorthomas.android.memoryloss.BaseApplication;
import com.theodoorthomas.android.memoryloss.DevicesListAdapter;
import com.theodoorthomas.android.memoryloss.MainThreadBus;
import com.theodoorthomas.android.memoryloss.R;
import com.theodoorthomas.android.memoryloss.Util;
import com.theodoorthomas.android.memoryloss.services.PkgInformation;
import com.theodoorthomas.android.memoryloss.services.ScheduleReciever;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.ListFragment;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

public class DeviceListFragment extends ListFragment {
	private static final String LOG_TAG = "MemoryLoss";
	private DeviceListInterface listener;
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);		
	}

	public void setAndPopulateAdapter(ArrayList<PkgInformation> packageSizeMeta) {
		if ( packageSizeMeta != null ) { 
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
//		listener.onDeviceClicked(data[position]);
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
		public void onDeviceClicked(File data);
		public void onReadyForPopulation(DeviceListFragment deviceListFragment);
	}
}
