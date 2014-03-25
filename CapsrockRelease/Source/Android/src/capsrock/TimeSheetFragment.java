/*
 * TimeSheetFragment: This Fragment is used to display the Time Sheet tab. This
 * 	includes Displaying all the logged time entries by date.
 *  
 * Team Capsrock : Richard Bae, Chris Beichler, Cody Hunter, Taylor Woods
 */
package capsrock;

import java.util.ArrayList;
import java.util.Calendar;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import capsrock.R;
import capsrock.Structures.DisplayTimeSheet;
import capsrock.pullFromServer.OnTaskCompleted;

public class TimeSheetFragment extends ListFragment implements OnClickListener {
	SheetArrayAdapter adapter;
	ListView mListView;
	Calendar cal;

	private OnTaskCompleted listener = new OnTaskCompleted() {
		/*
		 * method: onTaskCompleted
		 * Purpose: Updates the ArrayAdapter once the server pulls the information
		 * returns: void
		 */
		@Override
		public void onTaskCompleted(ArrayList<DisplayTimeSheet> sheet) {
			if(getActivity() != null)
			{
				if(sheet == null)
				{
					Toast.makeText(getActivity(), "Unable to connect to server", Toast.LENGTH_LONG).show();
					adapter = new SheetArrayAdapter(getActivity(), new ArrayList<DisplayTimeSheet>());	
				}
				else
					adapter = new SheetArrayAdapter(getActivity(), sheet);
				setListAdapter(adapter);
			}
		}
	};
	/*
	 * method: onCreateView
	 * Purpose: Sets up the initial UI for this Tab. Registers all the buttons
	 * 	and pulls the data for the server for today
	 * returns: void
	 */
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_timesheet_display, container, false);
        ((ImageButton) rootView.findViewById(R.id.prevDay)).setOnClickListener(this);
		((ImageButton) rootView.findViewById(R.id.nextDay)).setOnClickListener(this);
        mListView = (ListView) rootView.findViewById(android.R.id.list);
        cal = Calendar.getInstance();
        ((TextView)rootView.findViewById(R.id.datePicker)).setText(conString(cal));
		new pullFromServer(listener).execute((String)((TextView)rootView.findViewById(R.id.datePicker)).getText());
        return rootView;
    }
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	/*
	 * method: onClick
	 * Purpose: Changes the day based on button press (prev/next) and pulls the 
	 * 	date from the server for that day for display purposes
	 * returns: void
	 */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.prevDay:
            cal.add(Calendar.DATE, -1);
            break;
        case R.id.nextDay:
            Calendar now = Calendar.getInstance();
            if (cal.get(Calendar.DATE) != now.get(Calendar.DATE) ||
                    cal.get(Calendar.MONTH) != now.get(Calendar.MONTH) ||
                    cal.get(Calendar.YEAR) != now.get(Calendar.YEAR))
                cal.add(Calendar.DATE, 1);
            break;
        }
        ((TextView)getActivity().findViewById(R.id.datePicker)).setText(conString(cal));
        new pullFromServer(listener).execute((String)((TextView)getActivity().findViewById(R.id.datePicker)).getText());
    }
	
	/*
	 * method: conString
	 * Purpose: Formats the date to be readable
	 * returns: String - formatted date string
	 */
	public static String conString(Calendar cal) {
		String ret = String.format("%04d", cal.get(Calendar.YEAR)) + "-";
		ret += String.format("%02d", cal.get(Calendar.MONTH)+1) + "-";
		ret += String.format("%02d", cal.get(Calendar.DATE));
		return ret;
	}

}

