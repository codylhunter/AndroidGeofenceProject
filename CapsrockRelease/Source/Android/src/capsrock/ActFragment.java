/*
 * ActFragment: Fragment used to display activities, contains a list of 
 * 	all Activites for a current day, as well as 2 buttons which allow switching
 * 	between days. Finally there is an "Add Activity" button which allows users to 
 * 	create a new Activity
 * 
 * Team Capsrock : Richard Bae, Chris Beichler, Cody Hunter, Taylor Woods
 */

package capsrock;

import java.util.ArrayList;

import android.app.AlertDialog;

import java.util.Calendar;
import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ListFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import capsrock.R;
import capsrock.Geofencing.GeofenceRemover;
import capsrock.Geofencing.GeofenceRequester;
import capsrock.Geofencing.SimpleGeofence;
import capsrock.Structures.GLocation;
import capsrock.Structures.dailyActivities;

public class ActFragment extends ListFragment implements OnClickListener {
	//Calendar for the current day being viewed
	Calendar cal;
	//Callback to main
	ActivityInterface mCallback;
	//Broadcast receiver and filter for messages from TimeEntryService
	ActReceiver actReceiver;
    IntentFilter actFilter;
    //ArrayAdapter for displaying activites in a list format
    ActArrayAdapter adapter;
    //Add Activity button
    Button activityAdder;
    //Allows removal of previously created activities
    GeofenceRemover geoRemover;
    GLocation toRemove;
    
	/*
	 * Interface: ActivityInterface
	 * Purpose: Callback to main to retrieve various data
	 */
	public interface ActivityInterface {
        public void addActivity(GLocation act, Calendar date);
        public void addPersistantGeofence(SimpleGeofence g);
        public void addDay(int add);
        public Calendar getCal();
        public Integer getNextID();
        public void getDaily();
        public GeofenceRequester getGeofenceRequester();
        public String getLocation();
        public void dialogDismissed();
        public void deleteActivity(GLocation act, Calendar date);
    }
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		actReceiver = new ActReceiver();
		actFilter = new IntentFilter();
	    actFilter.addAction("dailyActs"); 
	    geoRemover = new GeofenceRemover(getActivity());
	  
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(actReceiver, actFilter);
	}
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_act_display, container, false);
        ((ImageButton) rootView.findViewById(R.id.actNextDay)).setOnClickListener(this);
        ((ImageButton) rootView.findViewById(R.id.actPrevDay)).setOnClickListener(this);
        ((Button) rootView.findViewById(R.id.addActivity)).setOnClickListener(this);
        activityAdder = (Button) rootView.findViewById(R.id.addActivity);
        
        ((TextView)rootView.findViewById(R.id.actDate)).setText(TimeSheetFragment.conString(mCallback.getCal()));
        mCallback.getDaily();
        return rootView;
    }
	
	/*
	 * Method: onAttach
	 * Purpose: Attach the callback option
	 */
	public void onAttach(Activity activity) {
        super.onAttach(activity);
        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (ActivityInterface) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement ActivityPasser");
        }
    }
	
	/*
	 * Method: onListItemClick
	 * Purpose: Allows clicking on the Activites to bring up a dialog which
	 * 	will let you delete the Activity
	 */
	@Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        String title, message;
        toRemove = (GLocation) getListAdapter().getItem(position);
        title = "Delete?";
        message = "Would you like to delete " + toRemove.name + "?";
        
        new AlertDialog.Builder(getActivity())
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                List<String> removeList = new ArrayList<String>();
                removeList.add(toRemove.gFence.getId());
                geoRemover.removeGeofencesById(removeList);
                mCallback.deleteActivity(toRemove, mCallback.getCal());
                mCallback.getDaily();
                dialog.dismiss();
            }
        }).setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        }).show();
	}
	
	/*
	 * Method: onClick
	 * Purpose: Handles clicks on the left and right buttons which changes the displayed date
	 */
	@Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.actPrevDay:
            mCallback.getCal().add(Calendar.DATE, -1);
            if (mCallback.getCal().getTimeInMillis() < Calendar.getInstance().getTimeInMillis() &&
            		(mCallback.getCal().get(Calendar.DATE) < Calendar.getInstance().get(Calendar.DATE) ||
            				mCallback.getCal().get(Calendar.MONTH) < Calendar.getInstance().get(Calendar.MONTH))){
            	activityAdder.setEnabled(false);
            	activityAdder.setBackgroundColor(Color.GRAY);
            }
            break;
        case R.id.actNextDay:
            mCallback.getCal().add(Calendar.DATE, 1);
            if (mCallback.getCal().getTimeInMillis() > Calendar.getInstance().getTimeInMillis() || 
            		(mCallback.getCal().get(Calendar.DATE) == Calendar.getInstance().get(Calendar.DATE)
            				&& mCallback.getCal().get(Calendar.MONTH) == Calendar.getInstance().get(Calendar.MONTH)
            				&& mCallback.getCal().get(Calendar.YEAR) == Calendar.getInstance().get(Calendar.YEAR))) {
            	activityAdder.setEnabled(true);
            	activityAdder.setBackgroundColor(0xff99cc00);
            }
            break;
        case R.id.addActivity:
            showDialog();
            break;
        }
        ((TextView)getActivity().findViewById(R.id.actDate)).setText(TimeSheetFragment.conString(mCallback.getCal()));
        mCallback.getDaily();
    }
	/*
	 * Method: showDialog
	 * Purpose: Displays ActAddDialogue when the user clicks the Add Activity button
	 */
	void showDialog() {
	    FragmentManager fm = getFragmentManager();
	    ActAddDialogue newFragment = new ActAddDialogue();
	    newFragment.show(fm, "addActivity");
	}
	
	/*
	 * Class: ActReceiver
	 * Purpose: BroadcastReceiver for getting daily activities to be displayed
	 * Receives: "dailyActs"
	 * From: TimeEntryService
	 */
	public class ActReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if(getActivity() != null)
			{
				if(action == "dailyActs")
				{
					dailyActivities dActs = (dailyActivities) intent.getSerializableExtra("acts");
					if(dActs != null)
					{
						adapter = new ActArrayAdapter(getActivity(), dActs.acts);
						setListAdapter(adapter);
					}
					else
						setListAdapter(null);
				}
			}
		}
	}
}
