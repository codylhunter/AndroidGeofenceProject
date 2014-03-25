/*
 * SheetArrayAdapter: This ArrayAdapter is used to create a display of all
 * 	LocationTimeSheet Logs for a particular Day. It takes a list, and creates a UI
 * 	element for all of them.
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
import capsrock.Structures.DisplayTimeSheet;

public class SheetArrayAdapter extends ArrayAdapter<DisplayTimeSheet> {
	private final Context context;
	private final ArrayList<DisplayTimeSheet> sheets;
	public SheetArrayAdapter(Context context, ArrayList<DisplayTimeSheet> objects) {
		super(context,R.layout.entryrowlayout, objects);
		this.context = context;
		this.sheets = objects;
	}
	/*
	 * method: getView
	 * Purpose: Takes in an index in the ArrayList and makes a View for it.
	 * 	Some formatting is done for the times.
	 * returns: View - One Entry in the ArrayAdapter View
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		long seconds, minutes, hours;
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View rowView = inflater.inflate(R.layout.entryrowlayout, parent, false);
		TextView tLocation = (TextView) rowView.findViewById(R.id.sheetName);
		TextView tArrive = (TextView) rowView.findViewById(R.id.sheetArrived);
		TextView tDepart = (TextView) rowView.findViewById(R.id.sheetDepart);
		TextView tWork = (TextView) rowView.findViewById(R.id.sheetWork);
		TextView tEnd = (TextView) rowView.findViewById(R.id.sheetBreak);

		tLocation.setText(sheets.get(position).location);

		String sec = sheets.get(position).arrivalTime.get(Calendar.HOUR) + ":" + sheets.get(position).arrivalTime.get(Calendar.MINUTE) + ":" + sheets.get(position).arrivalTime.get(Calendar.SECOND);

		tArrive.setText(sec);

		sec = sheets.get(position).departureTime.get(Calendar.HOUR) + ":" + sheets.get(position).departureTime.get(Calendar.MINUTE) + ":" + sheets.get(position).departureTime.get(Calendar.SECOND);
		tDepart.setText(sec);
		seconds = sheets.get(position).workTime;
		minutes = seconds / 1000 / 60;
		minutes %= 60;

		hours = seconds / 1000 / 60 / 60;
		hours %= 24;

		seconds /= 1000;
		seconds %= 60;
		sec = String.format("%02d", hours) + ":" + String.format("%02d", minutes) + ":" + String.format("%02d", seconds);
		tWork.setText(sec);
		seconds = sheets.get(position).breakTime;
		minutes = seconds / 1000 / 60;
		minutes %= 60;

		hours = seconds / 1000 / 60 / 60;
		hours %= 24;

		seconds /= 1000;
		seconds %= 60;
		sec = String.format("%02d", hours) + ":" + String.format("%02d", minutes) + ":" + String.format("%02d", seconds);

		tEnd.setText(sec);

		return rowView;
	}
}