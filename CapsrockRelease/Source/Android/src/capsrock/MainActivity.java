/*
 * MainActivity: This activity starts and runs the app. It is the interface between our background
 * services and our fragments (tabs).
 * 
 * Team Capsrock : Richard Bae, Chris Beichler, Cody Hunter, Taylor Woods
 */

package capsrock;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.UUID;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.google.android.gms.location.Geofence;

import capsrock.R;
import capsrock.ActFragment.ActivityInterface;
import capsrock.Geofencing.GeofenceRequester;
import capsrock.Geofencing.GeofenceUtils;
import capsrock.Geofencing.SimpleGeofence;
import capsrock.Geofencing.SimpleGeofenceStore;
import capsrock.Structures.Activities;
import capsrock.Structures.GLocation;
import capsrock.Structures.LocationTimeSheet;
import capsrock.Structures.TimeEntry;
import capsrock.TimeEntryFragment.TimeEntryInterface;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends FragmentActivity implements ActivityInterface, TimeEntryInterface, ActionBar.TabListener {

    AppSectionsPagerAdapter mAppSectionsPagerAdapter;
    ViewPager mViewPager;
	private final static UUID PEBBLE_APP_UUID = UUID.fromString("08b8ed0c-d3c1-49c6-88ba-ad34f0732e94");

	public String startDate;
	String currentLocation = "Not on location";
	public AlertDialog singleAlert;
	Calendar strDate;
	int State;
	public Thread thr;
	LocationTimeSheet loca;
	TimeEntry newTE, oldTE;
	private Button startBtn;
	PebbleDictionary PebbleData;
	Activities actList;
	int geoID;
	private SimpleGeofenceStore geoStore;
	private IntentFilter geoFilter;
	GeofenceReceiver geoReceiver;
	TimeEntryReceiver timeReceiver;
    IntentFilter timeFilter;
	GeofenceRequester geoRequester;
	Calendar actDate;
	
	Handler mHandler = new Handler(Looper.getMainLooper()) {
		@Override
		public void handleMessage(Message m) {
			if (m.obj != null)
				if (findViewById(R.id.time) != null)
					((TextView) findViewById(R.id.time)).setText((String)m.obj);
		}
	};
	/*
	 * Method: onCreate
	 * Purpose: Initiates all of the instance variables to their default values.
	 * Sets filters for the broadcast receivers so we can communicate with background services
	 */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println("On Create Called");
        State = TimeEntryFragment.NONE;
        PebbleData = new PebbleDictionary();
        actList = new Activities();
        geoStore = new SimpleGeofenceStore(this);
        geoReceiver = new GeofenceReceiver();
        geoFilter = new IntentFilter();
        singleAlert = null;
        geoFilter.addAction(GeofenceUtils.ACTION_GEOFENCES_ADDED);
        geoFilter.addAction(GeofenceUtils.ACTION_GEOFENCES_REMOVED);
        geoFilter.addAction(GeofenceUtils.ACTION_GEOFENCE_ERROR);
        geoFilter.addAction("geofenceTransitionName");
        geoFilter.addCategory(GeofenceUtils.CATEGORY_LOCATION_SERVICES);
        timeReceiver = new TimeEntryReceiver();
		timeFilter = new IntentFilter();
	    timeFilter.addAction("start");
	    timeFilter.addAction("stop");
	    timeFilter.addAction("state");
	    timeFilter.addAction("location");
	    timeFilter.addAction("dismissDialog");
        geoID = 0;
        geoRequester = new GeofenceRequester(this);
        actDate = Calendar.getInstance();
        
        PebbleKit.startAppOnPebble(this, PEBBLE_APP_UUID);
        startService(new Intent(this, TimeEntryService.class));
        
        //Create the text file to store server connection string
        String externalStorageDir = Environment.getExternalStorageDirectory().toString();
        File dir = new File(externalStorageDir, "Capsrock");
        dir.mkdirs();
        File f = new File(externalStorageDir, "Capsrock/serverInfo.txt");
        
        //Create a file on phone to hold serverInfo
        try{
        	if(!f.isFile())
            	f.createNewFile();
        	BufferedReader read = new BufferedReader(new FileReader(f));
        	//If the file is empty give it a generic IP
        	if(read.readLine() == null)
        	{
        		PrintWriter p = new PrintWriter (new FileWriter(f));
        		p.println("129.65.148.170");
        		p.close();
        	}
        	read.close();
        	
        } catch (FileNotFoundException e)
        {
        	
        	e.printStackTrace();
        } catch (IOException e)
        {
        	e.printStackTrace();
        }
        
    	/*
    	 * Object: Thread
    	 * Purpose: This thread updates the timer on the TimeEntry Tab. This is purely
    	 * a UI element for the user and only runs when the tab is in the forefront.
    	 * No data is recorded from this thread.
    	 */
		thr = new Thread(new Runnable() {
	        @Override
	        public void run() {
	            while (true) {
	                try {
	                    mHandler.post(new Runnable() {
	                        @Override
	                        public void run() {
	                        	Message mes = new Message();
	                        	if (strDate != null) {
									long seconds = Calendar.getInstance().getTimeInMillis() - strDate.getTimeInMillis();
		                        	long minutes = seconds / 1000 / 60;
		                        	minutes %= 60;
		                        	
		                        	long hours = seconds / 1000 / 60 / 60;
		                        	hours %= 24;
		                        	
		                        	seconds /= 1000;
		                        	seconds %= 60;
		                        	
		                        	String sec = String.format("%02d", hours) + ":" + String.format("%02d", minutes) + ":" + String.format("%02d", seconds);
		                        	mes.obj = sec;
		                        	mHandler.sendMessage(mes);
	                        	}
	                        	else {
	                        		mes.obj = "00:00:00";
	                        		mHandler.sendMessage(mes);
	                        	}
	                        }
	                    });
	                    Thread.sleep(1000);
	                } catch (Exception e) {
	                   
	                }
	            }
	        }
	    });
		
		//Start Login Screen
		Intent intent = new Intent();
		intent.setClassName("capsrock.beta", "capsrock.beta.LoginActivity");
		//startActivity(intent);
		setContentView(R.layout.activity_main);

        // Create the adapter that will return a fragment for each of the three primary sections
        // of the app.
        mAppSectionsPagerAdapter = new AppSectionsPagerAdapter(getSupportFragmentManager());

        // Set up the action bar.
        final ActionBar actionBar = getActionBar();

        // Specify that the Home/Up button should not be enabled, since there is no hierarchical
        // parent.

        // Specify that we will be displaying tabs in the action bar.
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Set up the ViewPager, attaching the adapter and setting up a listener for when the
        // user swipes between sections.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mAppSectionsPagerAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                // When swiping between different app sections, select the corresponding tab.
                // We can also use ActionBar.Tab#select() to do this if we have a reference to the
                // Tab.
                actionBar.setSelectedNavigationItem(position);
            }
        });

        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < mAppSectionsPagerAdapter.getCount(); i++) {
            actionBar.addTab(actionBar.newTab().setText(mAppSectionsPagerAdapter.getPageTitle(i)).setTabListener(this));
        }
    }
	/*
	 * Method: onResume
	 * Purpose: Starts the app on the pebble when the user opens this app again. Registers the
	 * broadcase receivers again
	 */
    @Override
    public void onResume()
    {
    	super.onResume();
        PebbleKit.startAppOnPebble(getApplicationContext(), PEBBLE_APP_UUID);
    	LocalBroadcastManager.getInstance(this).registerReceiver(geoReceiver, geoFilter);
    	LocalBroadcastManager.getInstance(this).registerReceiver(timeReceiver, timeFilter);
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

    }
	/*
	 * Method: onTabSelected
	 * Purpose: Stops the UI Timer Thread, and then switches to a new tab
	 */
    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in the ViewPager.
    	if (thr.isAlive()) {
      	   thr.interrupt();
         }
    	if(tab.getPosition() != 0)
    		mViewPager.getAdapter().notifyDataSetChanged();
    	mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

    }

	/*
	 * Class: AppSectionsPagerAdapter
	 * Purpose: A {@link FragmentPagerAdapter} that returns a fragment corresponding to one of the primary
     * sections of the app.
	 */
    public static class AppSectionsPagerAdapter extends FragmentPagerAdapter {

        public AppSectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }
        
        @Override
        public int getItemPosition(Object object)
        {
        	return POSITION_NONE;
        }

        @Override
        public Fragment getItem(int i) {
            switch (i) {
                case 0:
                    return new TimeEntryFragment();
                case 1:
                	return new TimeSheetFragment();
                case 2:
                	return new ActFragment();
                //Default case is never reached
                default:
                	throw new RuntimeException("Reached a tab that doesn't exist");
            }
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
        	switch(position) {
	        	case 0: return "Time Entry";
	        	case 1: return "Time Sheet";
	        	case 2: return "Activities";
	        	default: return "BAD TAB";
        	}
        }
    }
    
    /* --------------------------------
     * Implement Interface Methods Here
     * -------------------------------*/
    
	/*
	 * Method: startTimer
	 * Purpose: Send a broadcast to TimeEntryService to start the timer
	 * Returns: void
	 */
	public void startTimer() {
		LocalBroadcastManager.getInstance(this).sendBroadcast(
				new Intent(this, TimeEntryService.class).setAction("getStart"));
	}
	/*
	 * Method: startTimer
	 * Purpose: Send a broadcast to TimeEntryService to stop the timer
	 * Returns: void
	 */
	public void stopTimer(boolean fromAndroid)
	{
		thr.interrupt();
		strDate = null;
		setState(TimeEntryFragment.NONE);
		handleStop(fromAndroid);
	}
	
	/*
	 * Method: startTimer
	 * Purpose: Start the Timer UI thread
	 * Returns: void
	 */
	public void startThread() {
		if (!thr.isAlive())
			thr.start();
	}
	
	/*
	 * Method: handleWBChange
	 * Purpose: Updates the UI of the TimeEntry Tab based on a change in logging work/break time
	 * Returns: void
	 */
    public void handleWBChange(boolean fromAndroid, boolean fromDialogue) {
        //Update the UI on both the pebble and android depending on what state you are in
        if(!fromDialogue && fromAndroid)
        	startBtn = (Button)findViewById(R.id.StartButton);
        
        if (State != TimeEntryFragment.WORK) {
            if (!fromDialogue && fromAndroid) {
                startBtn.setText("Log Break Time");
                ((TextView)findViewById(R.id.status)).setText("Logging Work Time");
            }       
        }
        else {
            if (!fromDialogue && fromAndroid) {
                startBtn.setText("Log Work Time");
                ((TextView)findViewById(R.id.status)).setText("Logging Break Time");
            }
        }
    }
    /*
     * Method: dismissDialog()
     * Purpose: Dismisses any active alert, used when starting/stoping timer
     */
    public void dismissDialog()
    {
    	if (singleAlert != null)
    	{
    		System.out.println("Dismissed Dialog");
    		singleAlert.dismiss();
    		singleAlert = null;
    	}
    }
    
	/*
	 * Method: handleStop
	 * Purpose: handles the UI of TimeEntry tab based on stopping the timer. Sends a broadcast
	 * 	to the TimeEntryService to stop logging time
	 * Returns: void
	 */
	public void handleStop(boolean fromAndroid) {
		if(mViewPager.getCurrentItem() == 0)
		{
			((Button)findViewById(R.id.StartButton)).setText("Log Work Time");
			((TextView)findViewById(R.id.time)).setText("00:00:00");
			((TextView)findViewById(R.id.status)).setText("Not Logging Time");
		}
		LocalBroadcastManager.getInstance(this).sendBroadcast(
				new Intent(this, TimeEntryService.class).setAction("handleStop").putExtra("fromAndroid", fromAndroid));
	}
	/*
	 * Method: setState
	 * Purpose: sets the current state of the timer
	 * Returns: void
	 */
	public void setState(int state) {
		LocalBroadcastManager.getInstance(this).sendBroadcast(
				new Intent(this, TimeEntryService.class).setAction("setState").putExtra("state", state));
	}
	/*
	 * Method: getState
	 * Purpose: gets the current state of the timer
	 * Returns: void
	 */
	public void getState(){
		LocalBroadcastManager.getInstance(this).sendBroadcast(
				new Intent(this, TimeEntryService.class).setAction("getState"));
	}
	/*
	 * Method: getGeofenceRequester
	 * Purpose: return the geoRequester
	 * Returns: GeofenceRequester
	 */
	public GeofenceRequester getGeofenceRequester()
	{
		return geoRequester;
	}
	/*
	 * Class: TimeEntryReceiver
	 * Purpose: Receives various Broadcasts from TimeEntryService
	 * Receives: "start" "stop" "state" "location"
	 * From: TimeEntryService
	 */
	public class TimeEntryReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			System.out.println("received message: " + action);
			if(action == "start")
			{
				strDate = (Calendar) intent.getSerializableExtra("start");
				boolean fromAndroid = intent.getBooleanExtra("fromAndroid", false);
				dismissDialog();
				startThread();
				handleWBChange(fromAndroid, false);
				getState();
			}
			else if(action == "stop")
			{
				dismissDialog();
				stopTimer(false);
			}
			else if(action == "state")
				State = intent.getIntExtra("state", 0);
			else if(action == "location")
			{
				currentLocation = intent.getStringExtra("location");
			}
			else if(action == "dismissDialog")
			{
				System.out.println("Dismiss requested");
				dismissDialog();
			}
		}
	}
	/*
	 * Class: GeofenceSampleReceiver
	 * Purpose: Receives Broadcasts from the GeofenceServices regarding Entering/Exiting 
	 * 	Geofences
	 * Receives: "ACTION_GEOFENCES_ADDED" "ACTION_GEOFENCES_REMOVED" "ACTION_GEOFENCE_ERROR" "geofenceTransisitionName"
	 * From: ReceiveTranistionIntentService
	 */
	public class GeofenceReceiver extends BroadcastReceiver
	{
		/*
		 * Method: OnReceive
		 * Purpose: Receives the Actions and calls the proper method
		 */
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if(TextUtils.equals(action, GeofenceUtils.ACTION_GEOFENCE_ERROR))
				handleGeofenceError(context, intent);
			else if(TextUtils.equals(action, GeofenceUtils.ACTION_GEOFENCES_ADDED))
				Toast.makeText(context, "Geofence added", Toast.LENGTH_LONG).show();
			else if(TextUtils.equals(action, GeofenceUtils.ACTION_GEOFENCES_REMOVED))
				Toast.makeText(context, "Geofence removed", Toast.LENGTH_LONG).show();
			else if(TextUtils.equals(action, "geofenceTransitionName"))
				handleGeofenceTransition(context, intent);
		}

		/*
		 * Method: handleGeofenceTransition
		 * Purpose: Creates an alert for the user to start logging time at the current
		 * location (if they enter) or stop logging time (if they exit)
		 */
		private void handleGeofenceTransition(Context context, Intent intent)
		{	
			int transType = intent.getIntExtra(GeofenceUtils.EXTRA_GEOFENCE_STATUS, -1);
			String name = intent.getStringExtra("locationName");
			if (singleAlert != null) {
				singleAlert.dismiss();
				singleAlert = null;
			}
			if(transType == Geofence.GEOFENCE_TRANSITION_ENTER)
			{
				singleAlert = new AlertDialog.Builder(getMainActivityContext())
	            .setTitle("Arrived at " + name)
	            .setMessage("Would you like to clock in?")
	            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int whichButton) {
	                	startTimer();
	    				startThread();
	                	if(mViewPager.getCurrentItem() == 0)
	                		handleWBChange(true, false);
	                	else
	                		handleWBChange(true, true);
	                	dialog.dismiss();
	                	singleAlert = null;
	                	mViewPager.setCurrentItem(0);
	                }
	            }).setNegativeButton("No", new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int whichButton) {
	                	singleAlert = null;
	                    dialog.dismiss();
	                    LocalBroadcastManager.getInstance(getMainActivityContext()).sendBroadcast(
	            				new Intent(getMainActivityContext(), TimeEntryService.class).setAction("dismiss"));
	                }
	            }).show();
			}
			else if(transType == Geofence.GEOFENCE_TRANSITION_EXIT)
			{
				new AlertDialog.Builder(getMainActivityContext())
	            .setTitle("Left " + name)
	            .setMessage( "Would you like to clock out?")
	            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int whichButton) {
	                	stopTimer(true);
	                }
	            }).setNegativeButton("No", new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int whichButton) {
	                    dialog.dismiss();
	                    LocalBroadcastManager.getInstance(getMainActivityContext()).sendBroadcast(
	            				new Intent(getMainActivityContext(), TimeEntryService.class).setAction("dismiss"));
	                }
	            }).show();
				currentLocation = "Not on location";
			}
		}
		/*
		 * Method: handleGeofenceError
		 * Purpose: handles errors in the geofence
		 */
		private void handleGeofenceError(Context context, Intent intent)
		{
			String msg = intent.getStringExtra(GeofenceUtils.EXTRA_GEOFENCE_STATUS);
			Log.e(GeofenceUtils.APPTAG, msg);
			Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
		}
	}
	/*
	 * Method: addActivity
	 * Purpose: Sends a broadcast to TimeEntryService with the activity that needs to be
	 * added.
	 */
	@Override
	public void addActivity(GLocation act, Calendar date) {
		LocalBroadcastManager.getInstance(this).sendBroadcast(
				new Intent(this, TimeEntryService.class).setAction("addActivity").putExtra("act", act)
																				 .putExtra("date", date));
	}
	/*
	 * Method: deleteActivity
	 * Purpose: Sends a broadcast to TimeEntryService with the activity that needs to be 
	 * deleted
	 */
	@Override
	public void deleteActivity(GLocation act, Calendar date) {
		LocalBroadcastManager.getInstance(this).sendBroadcast(
				new Intent(this, TimeEntryService.class).setAction("deleteActivity").putExtra("act", act)
				.putExtra("date", date));
	}
	/*
	 * Method: getMainActivityContext
	 * Purpose: returns this context for whoever might need it
	 */
	public Context getMainActivityContext() {
		return this;
	}
	/*
	 * Method: getDaily
	 * Purpose: sends a broadcast to TimeEntryServices with a request for the daily
	 * 	activity list for the current date
	 */
	@Override
	public void getDaily() {
		LocalBroadcastManager.getInstance(this).sendBroadcast(
				new Intent(this, TimeEntryService.class).setAction("getDaily").putExtra("date", actDate));
	}
	/*
	 * Method: getLocation
	 * Purpose: returns the current Location String
	 */
	@Override
	public String getLocation() {
		return currentLocation;
	}
	/*
	 * Method: dialogDismissed
	 * Purpose: updates the viewpager that the dialog has been dismissed
	 */
	@Override
	public void dialogDismissed()
	{
		mViewPager.getAdapter().notifyDataSetChanged();
	}

	/*
	 * Method: addPersistantGeofence
	 * Purpose: Adds a geofence to the store of geofences in this activity
	 */
	@Override
	public void addPersistantGeofence(SimpleGeofence g) {
		geoStore.setGeofence(g.getId(), g);
		geoID++;
	}
	/*
	 * Method: getNextId
	 * Purpose: returns the next ID value
	 */
	@Override
	public Integer getNextID() 
	{
		return geoID;
	}
	/*
	 * Method: addDay
	 * Purpose: Adds the specified number of days to the date displayed in the activity fragment
	 */
	@Override
	public void addDay(int add) {
		actDate.add(Calendar.DATE, add);
	}
	/*
	 * Method: getCa;
	 * Purpose: returns the date to be displayed in the activity fragment
	 */
	@Override
	public Calendar getCal() {
		return actDate;
	}
	
	
}
