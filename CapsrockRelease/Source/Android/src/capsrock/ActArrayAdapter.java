/*
 * ActArrayAdapter: An array adapter which holds the format of activities to be displayed within ActFragment
 * 
 * Team Capsrock : Richard Bae, Chris Beichler, Cody Hunter, Taylor Woods
 */

package capsrock;

import java.util.ArrayList;
import java.util.Calendar;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import capsrock.R;
import capsrock.Structures.GLocation;

/*
 * Class ActArrayAdapter
 * Purpose: ArrayAdapter for displaying a list view of activities in ActFragment
 */
public class ActArrayAdapter extends ArrayAdapter<GLocation> {
	private final Context context;
	//Arraylist of activities to display
	private final ArrayList<GLocation> acts;
	public ActArrayAdapter(Context context, ArrayList<GLocation> objects) {
		super(context,R.layout.actrowlayout, objects);
		this.context = context;
		this.acts = objects;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		//Get the actrowlayout view so that we can set text fields within
		View rowView = inflater.inflate(R.layout.actrowlayout, parent, false);
		//Get handles to UI elements
		TextView tTime = (TextView) rowView.findViewById(R.id.actTime);
		TextView tName = (TextView) rowView.findViewById(R.id.actName);
		TextView tNote = (TextView) rowView.findViewById(R.id.actNote);
		TextView tAddress = (TextView) rowView.findViewById(R.id.actAddress);
		TextView tRadius = (TextView) rowView.findViewById(R.id.actShowRadius);

		//Set UI elements 
		tName.setText(acts.get(position).name);
		tTime.setText(String.format("%02d", acts.get(position).expectedStart.get(Calendar.HOUR)) + ":" + 
				String.format("%02d", acts.get(position).expectedStart.get(Calendar.MINUTE)) + ":00 - " +
				String.format("%02d",acts.get(position).expectedStop.get(Calendar.HOUR)) + ":" +
				String.format("%02d",acts.get(position).expectedStop.get(Calendar.MINUTE)) + ":00"
				);
		tNote.setText(acts.get(position).note);
		tAddress.setText(acts.get(position).address);
		Integer radius = acts.get(position).Radius; 
		tRadius.setText(radius.toString());

		return rowView;
	}
}