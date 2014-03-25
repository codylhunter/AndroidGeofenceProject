/*
 * ActAddDialog: This dialogue is shown when users click to add an activity from ActFragment
 * It has fields for entering Activity data, and registers a geo-fence when the add button is clicked
 * 
 * Team Capsrock : Richard Bae, Chris Beichler, Cody Hunter, Taylor Woods
 */

package capsrock;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.Geofence;

import capsrock.R;
import capsrock.ActFragment.ActivityInterface;
import capsrock.Geofencing.GeofenceUtils;
import capsrock.Geofencing.SimpleGeofence;
import capsrock.Structures.GLocation;
import capsrock.Structures.dailyActivities;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

/*
 * Class: ActAddDialogue
 * Purpose: Popup dialogue to allow users to create geo-fences
 */
public class ActAddDialogue extends DialogFragment {
    // Store a list of geofences to add
    List<Geofence> mCurrentGeofences;

    //Holds the radius of the geofence, taken from the UI
    private int radius;
    
    //UI Elements
    TextView name, note, address, lblStart, lblEnd, lblRepeatEnd;
	TimePicker str, stp;
	Button toggleButton;
	DatePicker repeatEnd;
	Calendar start, stop;
	CheckBox check;
	SeekBar radiusBar;
	TextView seekBarValue;
	
	//The geo-fence that is being added
    private SimpleGeofence geofence;
    //Callback function to ActivityInterface
    ActivityInterface mCallback;
    //Flag for if a geo-fence registration is in progress
    boolean inProgress;
    //Holds todays activities, used for overlapping geo-fence detection
    dailyActivities dailyActs;
    //Receiver and filter for getting dailyActivities from the TimeEntryService
    BroadcastReceiver dialogueReceiver;
    IntentFilter dialogueFilter;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCurrentGeofences = new ArrayList<Geofence>();
        inProgress = false;
        mCallback.getDaily();
        dialogueReceiver = new ActAddDialogueReceiver();
        dialogueFilter = new IntentFilter();
        dialogueFilter.addAction("dailyActs");
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(dialogueReceiver, dialogueFilter);
        mCallback.getDaily();
    }
	
	/*
	 * Class: ActAddDialogueReceiver
	 * Purpose: Broadcast receiver for getting daily activites, required for overlapping geo-fence detection
	 * Receives: "dailyActs"
	 * From: TimeEntryService
	 */
	public class ActAddDialogueReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			dailyActs = (dailyActivities) intent.getSerializableExtra("acts");
		}
	}
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.add_activitiy_display, container, false);
		
		Button addButton = (Button)rootView.findViewById(R.id.butAddActivity);
		Button cancelButton = (Button)rootView.findViewById(R.id.butCancelActivity);
		toggleButton = (Button)rootView.findViewById(R.id.toggleDTbutton);
		//Invisible checkbox for changing between displaying times and dates
		check = (CheckBox)rootView.findViewById(R.id.chkRepeat);
		check.setVisibility(View.GONE);
		check.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					str.setVisibility(View.INVISIBLE);
					stp.setVisibility(View.INVISIBLE);
					repeatEnd.setVisibility(View.VISIBLE);
					lblRepeatEnd.setVisibility(View.VISIBLE);
					lblStart.setVisibility(View.INVISIBLE);
					lblEnd.setVisibility(View.INVISIBLE);
				}
				else {
					str.setVisibility(View.VISIBLE);
					stp.setVisibility(View.VISIBLE);
					repeatEnd.setVisibility(View.GONE);
					lblRepeatEnd.setVisibility(View.GONE);
					lblStart.setVisibility(View.VISIBLE);
					lblEnd.setVisibility(View.VISIBLE);
				}
			}
		});
		toggleButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if(toggleButton.getText().equals("Choose End Date")) {
					toggleButton.setText("Choose Start/Stop Time");
				}
				else {
					toggleButton.setText("Choose End Date");
				}
            	check.setChecked(!check.isChecked());
            }
		});
        addButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	//Starts the process of adding a geofence to local storage
            	if(onAddClicked(v) && !inProgress)
            	{
            		inProgress = true;
            		//Get start and stop times, and create the GLocation to be stored
            		start.set(Calendar.HOUR, str.getCurrentHour());
            		start.set(Calendar.MINUTE, str.getCurrentMinute());
            		stop.set(Calendar.HOUR, stp.getCurrentHour());
            		stop.set(Calendar.MINUTE, stp.getCurrentMinute());
            		GLocation g = new GLocation(name.getText().toString(), note.getText().toString(), address.getText().toString(), start, stop, radius,(SimpleGeofence) geofence);
            		Calendar index = (Calendar) mCallback.getCal().clone();
            		Calendar endDate = Calendar.getInstance();
            		endDate.set(Calendar.DATE, repeatEnd.getDayOfMonth());
            		endDate.set(Calendar.MONTH, repeatEnd.getMonth());
            		endDate.set(Calendar.YEAR, repeatEnd.getYear());
            		//Loop through all days the geo-fence will be active and add it through a callback
            		do {
            			mCallback.addActivity(g, (Calendar) index.clone());
            			index.add(Calendar.DATE, 1);
            		} while (index.get(Calendar.DATE) <= endDate.get(Calendar.DATE));
            		dismiss();
            		inProgress = false;
            	}
            }
        });
        cancelButton.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) {
    			dismiss();
            }
        });
        //assignment of UI handles
        name = (EditText) rootView.findViewById(R.id.newActName);
        note = (EditText) rootView.findViewById(R.id.newActNote);
        address = (EditText) rootView.findViewById(R.id.newActAddress);
        start = Calendar.getInstance();
        stop = Calendar.getInstance();
        lblStart = (TextView) rootView.findViewById(R.id.textView4);
        lblEnd = (TextView) rootView.findViewById(R.id.textView5);
        lblRepeatEnd = (TextView) rootView.findViewById(R.id.textView7);
        repeatEnd = (DatePicker) rootView.findViewById(R.id.repeatDatePicker);
        repeatEnd.setMinDate(mCallback.getCal().getTimeInMillis());
        repeatEnd.setVisibility(View.GONE);
        lblRepeatEnd.setVisibility(View.GONE);
        str = (TimePicker) rootView.findViewById(R.id.newActStart);
        stp = (TimePicker) rootView.findViewById(R.id.newActEnd);
        radiusBar = (SeekBar) rootView.findViewById(R.id.actRadius);
        seekBarValue = (TextView) rootView.findViewById(R.id.actRadiusDisplay);
        radiusBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){ 
        	   @Override 
        	   public void onProgressChanged(SeekBar seekBar, int progress, 
        	     boolean fromUser) { 
        		   if (progress < 5)
        			   seekBarValue.setText("5");
        		   else
        			   seekBarValue.setText(String.valueOf(progress * 30)); 
        	   } 

        	   @Override 
        	   public void onStartTrackingTouch(SeekBar seekBar) { 

        	   } 

        	   @Override 
        	   public void onStopTrackingTouch(SeekBar seekBar) { 

        	   } 
        }); 
        getDialog().setTitle("Add Activity");

        return rootView;
    }
	
	/*
	 * Method: onAttach
	 * Purpose: Attaches the callback to this dialogue
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
	 * Method: onActivityResult
	 * Purpose: Handles any errors which might occur from Google Play services
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {
            // If the request code matches the code sent in onConnectionFailed
            case GeofenceUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST :
                switch (resultCode) {
                    // If Google Play services resolved the problem
                    case Activity.RESULT_OK:
                        // Toggle the request flag and send a new request
                        mCallback.getGeofenceRequester().setInProgressFlag(false);
                        // Restart the process of adding the current geofences
                        mCallback.getGeofenceRequester().addGeofences(mCurrentGeofences);
                    break;
                    // If any other result was returned by Google Play services
                    default:
                        // Report that Google Play services was unable to resolve the problem.
                        Log.d(GeofenceUtils.APPTAG, getString(R.string.no_resolution));
                }
            // If any other request code was received
            default:
               // Report that this Activity received an unknown requestCode
               Log.d(GeofenceUtils.APPTAG,
                       getString(R.string.unknown_activity_request_code, requestCode));
               break;
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog)
    {
    	mCallback.dialogDismissed();
    }
    
    /*
     * Method: servicesConnected
     * Purpose: Verify that Google Play services is available before making a request.
     * Returns: true if Google Play services is available, otherwise false
     */
    private boolean servicesConnected() {
        // Check that Google Play services is available
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getActivity());
        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            Log.d(GeofenceUtils.APPTAG, getString(R.string.play_services_available));
            return true;
        // Google Play services was not available for some reason
        } else {
            // Display an error dialog
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode, getActivity(), 0);
            if (dialog != null) {
                ErrorDialogFragment errorFragment = new ErrorDialogFragment();
                errorFragment.setDialog(dialog);
                errorFragment.show(getFragmentManager(), GeofenceUtils.APPTAG);
            }
            return false;
        }
    }

    /*
     * Method: checkCollision
     * Parameters: 2 SimpleGeofences to compare
     * Purpose: Given 2 SimpleGeofences, determines if there is any overlap
     * Returns: true if there is an overlap, false otherwise
     */
    private boolean checkCollision(SimpleGeofence g1, SimpleGeofence g2)
    {
    	//Uses implicit equation of a circle to determine collision
    	//Must convert radius in meters to lat/long degrees
    	double radius = (g1.getRadius() * (1.0f / 111000.0f)) + (g2.getRadius() * (1.0f / 111000.0f));
    	double deltaX = g1.getLatitude() - g2.getLatitude();
    	double deltaY = g1.getLongitude() - g2.getLongitude();
    	return (deltaX * deltaX) + (deltaY * deltaY) <= (radius * radius);
    }
    
    /*
     * Method: onAddClicked
     * Purpose: Called when the user clicks the "Add" button.
     * 	Get the geofence parameters for each geofence and add them to
     * 	a List. Create the PendingIntent containing an Intent that
     * 	Location Services sends to this app's broadcast receiver when
     * 	Location Services detects a geofence transition. Send the List
     * 	and the PendingIntent to Location Services.
     * Returns: true if the geo-fence was successfully added, false otherwise
     */
    public boolean onAddClicked(View view) {
        radius = Integer.parseInt(seekBarValue.getText().toString());
        
        if (!servicesConnected())
            return false;

        if (!checkInputFields())
            return false;
       
        //Change the address string into an actual Address
        Address addr = getAddress((address.getText()).toString());
        if(addr == null)
        {
        	Toast.makeText(getActivity(), "Invalid address", Toast.LENGTH_LONG).show();
        	return false;
        }
        
        geofence = new SimpleGeofence(
            mCallback.getNextID().toString(),
            addr.getLatitude(),
            addr.getLongitude(),
            radius,
            com.google.android.gms.location.Geofence.NEVER_EXPIRE, //Geofences last forever, we manually delete
            Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT); //Listen for entrance and exit
        
      //Overlapping geo-fence detection
        if(dailyActs != null)
        {
        	for(int i = 0; i < dailyActs.acts.size(); i++)
        	{
        		if(checkCollision(geofence, dailyActs.acts.get(i).gFence))
        		{
        			Toast.makeText(getActivity(), "Geofence Overlap", Toast.LENGTH_LONG).show();
        			return false;
        		}
        	}
        }
        
        //Add the geo-fence to our list to be sent to google play services
        mCurrentGeofences.add(geofence.toGeofence());
        //Callback to main to store the geo-fence locally
        mCallback.addPersistantGeofence(geofence);

        try {
            // Try to add geofences
        	//Reduce the length of currentGeofences to 1 as we only register 1 geo-fence per dialogue
        	if(mCurrentGeofences.size() != 1)
        	{
        		for(int i = 1; i < mCurrentGeofences.size(); i++)
        			mCurrentGeofences.remove(i);
        	}
        	Calendar today = Calendar.getInstance();
        	//Only register the geo-fence if it's built for today
        	if((mCallback.getCal().get(Calendar.DATE) == today.get(Calendar.DATE)) &&
        			(mCallback.getCal().get(Calendar.MONTH) == today.get(Calendar.MONTH)) &&
        			(mCallback.getCal().get(Calendar.YEAR) == today.get(Calendar.YEAR)))
        	{
        		//Callback to get the geo-fence requester from MainActivity and then use it to add geo-fence
        		mCallback.getGeofenceRequester().addGeofences(mCurrentGeofences);
        	}
        } catch (UnsupportedOperationException e) {
            // Notify user that previous request hasn't finished.
            Toast.makeText(getActivity(), R.string.add_geofences_already_requested_error,Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }
    
    /*
     * Method: getAddress
     * Purpose: Converts an address string into an Address object by geo-coding
     * Parameter: Address in string format entered by user
     * Returns: An Address object 
     */
    private Address getAddress(String strAddr){
    	Geocoder coder = new Geocoder(getActivity());
    	List<Address> address;
    	try{
    		address = coder.getFromLocationName(strAddr,5);
    		if (address.size() == 0) {
    			return null;
    		}
    		Address location = address.get(0);
    		return location;
    	}
    	catch(IOException i){
    		return null;
    	}
    }
    
    /*
     * Method: checkInputFields
     * Purpose: Check the necessary input values and flag those that are incorrect
     * Returns: true if all the values are correct; false otherwise
     */
    private boolean checkInputFields() {
        boolean inputOK = true;
        
        if (TextUtils.isEmpty(address.getText())) {
            Toast.makeText(getActivity(), "Please enter an address", Toast.LENGTH_LONG).show();
            inputOK = false;
        }
        
        if (TextUtils.isEmpty(name.getText())) {
            Toast.makeText(getActivity(), "Please enter a name", Toast.LENGTH_LONG).show();
            inputOK = false;
        }

        // If everything passes, the validity flag will still be true, otherwise it will be false.
        return inputOK;
    }

    /*
     * Class: ErrorDialogFragment
     * Purpose: Define a DialogFragment to display the error dialog generated in
     * showErrorDialog.
     */
    public static class ErrorDialogFragment extends DialogFragment {
        // Global field to contain the error dialog
        private Dialog mDialog;

        public ErrorDialogFragment() {
            super();
            mDialog = null;
        }
        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }
        
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }
}


