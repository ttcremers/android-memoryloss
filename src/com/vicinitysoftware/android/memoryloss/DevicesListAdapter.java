package com.vicinitysoftware.android.memoryloss;

import java.text.DecimalFormat;
import java.util.ArrayList;

import org.joda.time.DateTime;

import android.R.color;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.vicinitysoftware.android.memoryloss.R;
import com.vicinitysoftware.android.memoryloss.services.PkgInformation;

public class DevicesListAdapter<T> extends ArrayAdapter<PkgInformation> {
	private static final String TAG = "DeviceListAdapter";
	private final Context context;
	private final ArrayList<PkgInformation> values;
	private static float MB = 1048576;
	private final DecimalFormat format = new DecimalFormat("#.##");

	public DevicesListAdapter(Context context,
			int textViewResourceId, ArrayList<PkgInformation> packageSizeMeta) {
		super(context, textViewResourceId, packageSizeMeta);
		this.context = context;	
		this.values = packageSizeMeta;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if ( values == null ) {
			return new TextView(getContext());			
		}
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		View rowView = inflater.inflate(R.layout.location_picker_list_row, parent, false);
		
		TextView textView = (TextView) rowView.findViewById(R.id.devicePath);
		textView.setText(values.get(position).getDisplayName());
		
		TextView lastUsed = (TextView)
				rowView.findViewById(R.id.entry_filesystem_value);
		lastUsed.setText(values.get(position).getLastActive().toLocalDate().toString());
		
		TextView entryDiskSize = 
				(TextView) rowView.findViewById(R.id.table_entry_size);
		float diskSizeMBs = values.get(position).getSize() / MB;
		entryDiskSize.setText(format.format(diskSizeMBs) + "MB");	
		
		ImageView imageView = (ImageView) rowView.findViewById(R.id.deviceIcon);		
		try {
			imageView.setImageDrawable(
					context.getPackageManager().getApplicationIcon(
							values.get(position).getPackageNamespace()));
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		
		View colorIndicator = rowView.findViewById(R.id.color_indicator);
		// 1 = orange drop 
		// 2 = red drop
		// 3 = green drop (default)
		if ( values.get(position).getWeight() == 1 ) {
			colorIndicator.setBackgroundColor(
					context.getResources().getColor(color.holo_orange_light));
		} else if ( values.get(position).getWeight() == 2 ) {
			colorIndicator.setBackgroundColor(
					context.getResources().getColor(color.holo_red_light));
		}
		
		return rowView;
	} 
}
