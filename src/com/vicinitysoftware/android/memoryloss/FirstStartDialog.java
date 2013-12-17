package com.vicinitysoftware.android.memoryloss;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;

import com.vicinitysoftware.android.memoryloss.fragments.DeviceListFragment.DeviceListInterface;

public class FirstStartDialog extends DialogFragment {
	private View rootView;
	private ArrayList<String> appList;
	private ArrayList<String> selectedApps = 
			new ArrayList<String>();
	private DeviceListInterface listener;
	private static final String TAG = "MemoryLossFirstStart";
	
	public FirstStartDialog() {
		super();		
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		if (activity instanceof DeviceListInterface) {
			listener = (DeviceListInterface) activity;			
		} else {
			throw new ClassCastException(activity.toString()
					+ " must implemenet DeviceListInterface");
		}
	}
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        getDialog().setTitle("Select your favorite apps");
		
        rootView = inflater.inflate(R.layout.first_start_dialog, container);
        appList = getInstalledPackages();
        
        Button doneButton = (Button) rootView.findViewById(R.id.app_grid_submit);
		doneButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				listener.onInitialStartCompleted(selectedApps);
				FirstStartDialog.this.dismissAllowingStateLoss();
			}
		});
		
		GridView appGrid = (GridView)rootView.findViewById(R.id.app_grid);
		appGrid.setOnItemClickListener(new OnItemClickListener() {
	        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
	        	v.setBackgroundColor(toggleEdition(appList.get(position)));
	        }
	    });
		appGrid.setAdapter(new AppGridAdapter(getActivity()));
		appGrid.forceLayout();
		
        return rootView;
    }
    
    private int toggleEdition(String pkg) {
    	if (selectedApps.contains(pkg)) {
    		selectedApps.remove(pkg);
    		return Color.WHITE;
    	} else {
    		selectedApps.add(pkg);
    		return getResources()
    				.getColor(android.R.color.holo_blue_bright);
    	}
    }
	
    private ArrayList<String> getInstalledPackages() {
    	ArrayList<String> rv = new ArrayList<String>();
    	PackageManager pm = getActivity().getPackageManager();    	
		List<PackageInfo> list = pm.getInstalledPackages(0);		
		for (PackageInfo pi : list) {
			try {
				ApplicationInfo ai = pm.getApplicationInfo(pi.packageName,0);
				if (pm.getLaunchIntentForPackage(pi.packageName) != null &&
						!ai.sourceDir.contains("/system")) {
					rv.add(pi.packageName);
				}
			} catch (NameNotFoundException e) {
				e.printStackTrace();
			}
		}
		return rv;
    }

	private class AppGridAdapter extends BaseAdapter {
		public AppGridAdapter(Context context) {
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			String pkg = appList.get(position);
			
			ImageView imageView = new ImageView(getActivity());
			imageView.setLayoutParams(new GridView.LayoutParams(-1, 70));
            imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            imageView.setBackgroundColor(Color.WHITE);
			try {
				imageView.setImageDrawable(
						getActivity().getPackageManager().getApplicationIcon(pkg));
			} catch (NameNotFoundException e) {
				e.printStackTrace();
			}
			return imageView;
		}

		@Override
		public int getCount() {
			return appList.size();
		}

		@Override
		public String getItem(int position) {
			return appList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}
	}
}
