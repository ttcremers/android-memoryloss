package com.vicinitysoftware.android.memoryloss;

import com.squareup.otto.Subscribe;
import com.vicinitysoftware.android.memoryloss.services.LogMonitService;
import com.vicinitysoftware.android.memoryloss.services.PkgInformation;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;

public class FirstStartDialog extends DialogFragment {
	
	public FirstStartDialog() {
		super();
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		Intent intent = new Intent(activity, 
				LogMonitService.class);
		activity.startService(intent);
		getDialog().setTitle("Hello");
	}
	
	@Subscribe public void recieveFirstData(PackageArrayList<PkgInformation> list) {
		
	}
}
