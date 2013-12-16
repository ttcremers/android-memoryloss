package com.vicinitysoftware.android.memoryloss;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Iterator;
import java.util.concurrent.Semaphore;

import org.joda.time.DateTime;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
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

import com.squareup.otto.Subscribe;
import com.vicinitysoftware.android.memoryloss.fragments.DeviceListFragment.DeviceListInterface;
import com.vicinitysoftware.android.memoryloss.services.LogMonitService;
import com.vicinitysoftware.android.memoryloss.services.PkgInformation;

public class FirstStartDialog extends DialogFragment {
	private View rootView;
	private PackageArrayList<PkgInformation> appList;
	private PackageArrayList<PkgInformation> selectedApps = 
			new PackageArrayList<PkgInformation>();
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
		Intent intent = new Intent(getActivity(), 
				LogMonitService.class);
		getActivity().startService(intent);
        new MainThreadBus().register(this);
	}
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        getDialog().setTitle("Select your favorite apps");
        rootView = inflater.inflate(R.layout.first_start_dialog, container);
        
        return rootView;
    }
    
    private int toggleEdition(PkgInformation pkg) {
    	if (selectedApps.contains(pkg.getPackageNamespace())) {
    		selectedApps.remove(pkg.getPackageNamespace());
    		return Color.WHITE;
    	} else {
    		selectedApps.add(pkg);
    		return Color.GREEN;
    	}
    }
	
	@Subscribe public void recieveFirstData(PackageArrayList<PkgInformation> list) {
		// We're only interested in the first message 
		new MainThreadBus().unregister(this);		
		appList = list;
		
		Button doneButton = (Button) rootView.findViewById(R.id.app_grid_submit);
		doneButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				saveData();
				listener.onInitialStartCompleted();
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
	}
	
	private synchronized void saveData() {
		final Semaphore semaphore = new Semaphore(0);
		new Thread(new Runnable() {			
			@Override
			public void run() {
				FileOutputStream fileOutputStream = null;
				FileInputStream fileInputStream = null;
				try {
					PackageArrayList<PkgInformation> packageLaunchInformation;
					Log.w(TAG, "MEEEEEEE11");
					if ( getActivity().getFileStreamPath(LogMonitService.OBJECT_CACHE_FILE).exists() ) {
						Log.w(TAG, "MEEEEEEE");
						fileInputStream = getActivity().openFileInput(LogMonitService.OBJECT_CACHE_FILE);
						//Open File Stream and cast it into array of ItemAttributes
						ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
						packageLaunchInformation = (PackageArrayList<PkgInformation>)objectInputStream.readObject();
						fileInputStream.close();
					} else {
						packageLaunchInformation = new PackageArrayList<PkgInformation>();
					}
	
					for ( PkgInformation pkgInfo : selectedApps ) {
						int index = packageLaunchInformation.indexOf(pkgInfo.getPackageNamespace());
						pkgInfo.setLastActive(new DateTime());
						Log.w(TAG, "Updating key with index: " + index);
						if ( index > -1 ) {							
							packageLaunchInformation.remove(index);
							packageLaunchInformation.add(pkgInfo);							
						} else {
							packageLaunchInformation.add(pkgInfo);
							Log.w(TAG, "Entry not found for: " + pkgInfo.getPackageNamespace());
						}
					}
					
					fileOutputStream = getActivity().openFileOutput(
							LogMonitService.OBJECT_CACHE_FILE, Context.MODE_PRIVATE);
					ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
					objectOutputStream.writeObject(packageLaunchInformation);
					
				} catch (FileNotFoundException e) {
					Log.e(TAG, "Error in accessing object cache", e);
				} catch (IOException e) {
					Log.e(TAG, "Error comminicating with object cache", e);
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
					try {
						if ( fileOutputStream != null )
							fileOutputStream.close();
					} catch (IOException e) {
						Log.e(TAG, "Error closing object cache", e);
					}
					semaphore.release();
				}
			}
		}).start();
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
	}

	private class AppGridAdapter extends BaseAdapter {
		public AppGridAdapter(Context context) {
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			PkgInformation pkgInformation = appList.get(position);
			
			ImageView imageView = new ImageView(getActivity());
			imageView.setLayoutParams(new GridView.LayoutParams(-1, -1));
            imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            imageView.setBackgroundColor(Color.WHITE);
            imageView.setPadding(18, 18, 18, 18);
			try {
				imageView.setImageDrawable(
						getActivity().getPackageManager().getApplicationIcon(
								pkgInformation.getPackageNamespace()));
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
		public PkgInformation getItem(int position) {
			return appList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}
	}
}
