/*
 * TimeEntryFragment: This Fragment is used to display the Time Entry tab. This includes buttons for
 * Time start/stop and current Location/Time. Most of the work here has been outsourced to the
 * TimeEntryService
 *  
 * Team Capsrock : Richard Bae, Chris Beichler, Cody Hunter, Taylor Woods
 */
package capsrock;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import capsrock.R;


public class TimeEntryFragment extends Fragment implements OnClickListener {
	
	TimeEntryInterface mCallback;
    public static final int NONE = 0;
    public static final int WORK = 1;
    public static final int BREAK = 2;
    TimeEntryReceiver timeReceiver;
    IntentFilter timeFilter;
    

	/*
	 * Interface: TimeEntryInterface
	 * Purpose: Interface between this fragment and the main activity to handl
	 * button presses and update this UI
	 */
    public interface TimeEntryInterface {
        public void startTimer();
        public void handleStop(boolean fromAndroid);
        public void setState(int state);
        public void getState();
        public String getLocation();
        public void stopTimer(boolean fromAndroid);
    }
	/*
	 * method: onCreate
	 * Purpose: sets up the Time Entry broadcast Receiver
	 * returns: void
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);	
		timeReceiver = new TimeEntryReceiver();
		timeFilter = new IntentFilter();
	    timeFilter.addAction("state");
	}
	/*
	 * method: onCreateView
	 * Purpose: sets up the UI for this tab
	 * returns: void
	 */
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		  View rootView = inflater.inflate(R.layout.fragment_timeentry_display, container, false);
			((Button) rootView.findViewById(R.id.StartButton)).setOnClickListener(this);
			((Button) rootView.findViewById(R.id.StopButton)).setOnClickListener(this);
			((TextView) rootView.findViewById(R.id.actName)).setText(mCallback.getLocation());
			mCallback.getState();
			
			return rootView;
    }
	/*
	 * method: onClick
	 * Purpose: handles button press
	 * returns: void
	 */
	@Override
	public void onClick(View v) {
		onTimeEntry(v, true);
	}
	/*
	 * method: onResume
	 * Purpose: re-registers the broadcast manager
	 * returns: void
	 */
	@Override 
	public void onResume()
	{
		super.onResume();
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(timeReceiver, timeFilter);
	}
	/*
	 * method: onAttach
	 * Purpose: creates the interface between this fragment and the main activity
	 * 	that calls it
	 * returns: void
	 */
	public void onAttach(Activity activity) {
        super.onAttach(activity);
        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (TimeEntryInterface) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement ActivityPasser");
        }
    }
	/*
	 * method: onTimeEntry
	 * Purpose: calls the correct method based on button press
	 * returns: void
	 */
	public void onTimeEntry(View v, boolean fromAndroid) {
		switch (v.getId()) {
			case R.id.StartButton:
				mCallback.startTimer();
				break;
			case R.id.StopButton:
				mCallback.stopTimer(fromAndroid);
				break;
		}
	}
	/*
	 * Class: TimeEntryReceiver
	 * Purpose: Receives Broadcasts related to current state of logging time.
	 * 	This is so the tab can update it's UI to reflect the current location,
	 * 	mode, and time
	 */
	public class TimeEntryReceiver extends BroadcastReceiver
	{
		/*
		 * method: onReceive
		 * Purpose: updates the UI based on the current state
		 * returns: void
		 */
		@Override
		public void onReceive(Context context, Intent intent) {
			if(getActivity() == null)
				return;
			if(getActivity().findViewById(R.id.status) == null)
				return;
			switch (intent.getIntExtra("state", 0)) {
			case NONE:
				((TextView) getActivity().findViewById(R.id.status)).setText("Not Logging Time");
				((TextView) getActivity().findViewById(R.id.StartButton)).setText("Log Work Time");
				break;
			case WORK:
				((TextView) getActivity().findViewById(R.id.status)).setText("Logging Work Time");
				((TextView) getActivity().findViewById(R.id.StartButton)).setText("Log Break Time");
				break;
			case BREAK:
				((TextView) getActivity().findViewById(R.id.status)).setText("Logging Break Time");
				((TextView) getActivity().findViewById(R.id.StartButton)).setText("Log Work Time");
				break;
			default:
				break;
		}
		}
	}
}
