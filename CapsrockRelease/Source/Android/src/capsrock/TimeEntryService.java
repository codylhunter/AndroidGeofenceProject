/*
 * TimeEntryService: Service which holds any data which may need to be accessed while the 
 * 	Android application is in the background. Includes sending and receiving data to/from Pebble,
 * 	holding timestamps and current location for Time Entry, and list of all Activities
 * 
 * Team Capsrock : Richard Bae, Chris Beichler, Cody Hunter, Taylor Woods
 */

package capsrock;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.google.android.gms.location.Geofence;

import capsrock.Geofencing.GeofenceRemover;
import capsrock.Geofencing.GeofenceRequester;
import capsrock.Geofencing.GeofenceUtils;
import capsrock.Geofencing.SimpleGeofence;
import capsrock.Geofencing.SimpleGeofenceStore;
import capsrock.Structures.Activities;
import capsrock.Structures.GLocation;
import capsrock.Structures.LocationTimeSheet;
import capsrock.Structures.TimeEntry;
import capsrock.Structures.TimeSheet;
import capsrock.Structures.WebTimeEntry;
import capsrock.Structures.dailyActivities;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.app.AlarmManager;

/*
 * Class: TimeEntryService
 * Purpose: Service containing all data which needs to be accessed in the background
 */
public class TimeEntryService extends IntentService {
	//Pebble ID for communication
	private final static UUID PEBBLE_APP_UUID = UUID.fromString("08b8ed0c-d3c1-49c6-88ba-ad34f0732e94");
	//Timestamp of when the current time entry started
	Calendar strDate;
	//Current state of logging time, constants defined in TimeEntryFragment
	int State;
	//Currently open TimeEntry
	TimeEntry currentEntry;
	//Holds all time entries for the current location on the current day
	LocationTimeSheet loca;
	//List of all registered activities
	Activities actList;
	//Allows communication with pebble
	PebbleDictionary PebbleData;
	//Holds the all time entries for the current day
	TimeSheet timeSheet;
	//Location string to be displayed, and sent to server
	String currentLocation = "Not on location";
	//Store of all currently active geo-fences
	private SimpleGeofenceStore geoStore;
	//Daily repeating alarm to unregister old geo-fences and register new geo-fences
	AlarmManager alarm;
	//Broadcast receivers and filters
	ServiceReceiver serviceReceiver;
    IntentFilter serviceFilter;
    GeofenceReceiver geoReceiver;
	IntentFilter geoFilter;
	
	public TimeEntryService() {
		super("TimeEntryService");
	}
	
	@Override
	public void onCreate()
	{
		super.onCreate();
		serviceReceiver = new ServiceReceiver();
		serviceFilter = new IntentFilter();
	    serviceFilter.addAction("handleStop");
	    serviceFilter.addAction("setState");
	    serviceFilter.addAction("getState");
	    serviceFilter.addAction("getStart");
	    serviceFilter.addAction("addActivity");
	    serviceFilter.addAction("deleteActivity");
	    serviceFilter.addAction("getDaily");
	    serviceFilter.addAction("cleanUp");
	    serviceFilter.addAction("dismiss");
		LocalBroadcastManager.getInstance(this).registerReceiver(serviceReceiver, serviceFilter);
		
		geoStore = new SimpleGeofenceStore(this);
	    geoReceiver = new GeofenceReceiver();
	    geoFilter = new IntentFilter();
	    geoFilter.addAction(GeofenceUtils.ACTION_GEOFENCE_TRANSITION);
	    geoFilter.addCategory(GeofenceUtils.CATEGORY_LOCATION_SERVICES);
	    LocalBroadcastManager.getInstance(this).registerReceiver(geoReceiver, geoFilter);
		
        timeSheet = new TimeSheet();
        State = TimeEntryFragment.NONE;
        actList = new Activities();
        //Build an alarm that reoccurs everyday at midnight, when triggered launches the "cleanUp"
        //action in AlarmReceiver, which forwards it to ServiceReceiver
        Calendar alarmTime = Calendar.getInstance();
        alarmTime.set(Calendar.HOUR_OF_DAY, 0);
		alarmTime.set(Calendar.MINUTE, 0);
		alarmTime.set(Calendar.SECOND, 0);
		Intent alarmIntent = new Intent(this, AlarmReceiver.class);
		alarmIntent.setAction("cleanUp");
		PendingIntent alarmPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		
        alarm = (AlarmManager)getSystemService(ALARM_SERVICE);
        alarm.setRepeating(AlarmManager.RTC_WAKEUP, alarmTime.getTimeInMillis(), AlarmManager.INTERVAL_DAY, alarmPendingIntent);
		
		PebbleData = new PebbleDictionary();
		PebbleKit.startAppOnPebble(getApplicationContext(), PEBBLE_APP_UUID);
		//Set up to Receive messages from the pebble and handle them correctly
		PebbleKit.registerReceivedDataHandler(getApplicationContext(), new PebbleKit.PebbleDataReceiver(PEBBLE_APP_UUID) {
			@Override
			public void receiveData(final Context context, final int transactionId, final PebbleDictionary data) {
				String mode = data.getString(1);
				System.out.println("Pebble Sent: " + mode);
				//Work and break time messages
				if(mode.length() >= 9)
				{
					if (mode.substring(9).equals("Break") || mode.substring(9).equals("Work")) {
						strDate = Calendar.getInstance();
						handleWBChange(false);
						Intent broadcastIntent = new Intent();
						broadcastIntent.setAction("start")
						.putExtra("start", strDate)
						.putExtra("fromAndroid", false);
						LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(broadcastIntent);
					}
					//Request time message, occurs when app is started on pebble
					//ensures that pebble displays proper time
					else if (mode.equals("request_time"))
					{
						String toSend = "time";
						if(State == TimeEntryFragment.WORK)
							toSend += "work ";
						else if (State == TimeEntryFragment.BREAK)
							toSend += "break ";
						else
							toSend += "none ";

						if (strDate != null)
						{
							long seconds = strDate.getTimeInMillis();
							int hour = strDate.get(Calendar.HOUR_OF_DAY);
							int minute = strDate.get(Calendar.MINUTE);
							int second = strDate.get(Calendar.SECOND);
							toSend += (hour*3600 + minute*60 + second);
							System.out.println("Android time: " + toSend);
							//seconds -= 1394600000000l;
							//Calendar startofDay = Calendar.getInstance();
							//startofDay.set(Calendar.HOUR_OF_DAY, 0);
							//startofDay.set(Calendar.MINUTE, 0);
							//startofDay.set(Calendar.SECOND, 0);
							//long secondsPastToday = seconds - startofDay.getTimeInMillis();

							//toSend += seconds;
						}

						PebbleData.addString(0, toSend);
						System.out.println("Sending " + toSend + " to Pebble");
						PebbleKit.sendDataToPebble(getActivity(), PEBBLE_APP_UUID, PebbleData);
					}
					//Stop message
					else {
						strDate = null;
						State = TimeEntryFragment.NONE;
						handleStop(false);
						Intent broadcastIntent = new Intent();
						broadcastIntent.setAction("stop");
						LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(broadcastIntent);
					}
				}
				//User ignored the dialog on Pebble, dismiss our dialog
				else if(mode.equals("dismiss"))
				{
					System.out.println("Getting ready to dismiss");
					Intent broadcastIntent = new Intent();
					broadcastIntent.setAction("dismissDialog");
					LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(broadcastIntent);
				}
				PebbleKit.sendAckToPebble(getApplicationContext(), transactionId);
			}
		});
	}

	@Override
	protected void onHandleIntent(Intent intent) {
	}
	
	/*
	 * Class: ServiceReceiver
	 * Purpose: Receives broadcasts from throughout the app for anything not related to geo-fences
	 * Receives: "handleStop", "setState", "getState", "getStart", "addActivity", "deleteActivity", "getDaily", and "cleanUp"
	 * From: MainActivity, TimeEntryFragment, ActAddDialog, ActFragment, AlarmReceiver
	 */
	public class ServiceReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if(action == "handleStop")
				handleStop(intent.getBooleanExtra("fromAndroid", true));
			else if(action == "setState")
				State = intent.getIntExtra("state", 0);
			else if(action == "getState")
			{
				//Sends the state back to MainActivity
				Intent broadcastIntent = new Intent();
				broadcastIntent.setAction("state")
					   .putExtra("state", State);
				LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(broadcastIntent);
			}
			else if(action == "getStart")
			{
				strDate = Calendar.getInstance();
				
				Intent broadcastIntent = new Intent();
				//Sends the start date back to MainActivity
				broadcastIntent.setAction("start")
				   .putExtra("start", strDate)
				   .putExtra("fromAndroid", true);
				LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(broadcastIntent);
				handleWBChange(true);
			}
			else if(action == "addActivity")
			{
				addActivity((GLocation) intent.getSerializableExtra("act"), (Calendar) intent.getSerializableExtra("date"));
			}
			else if(action == "deleteActivity")
            {
                deleteActivity((GLocation) intent.getSerializableExtra("act"), (Calendar) intent.getSerializableExtra("date"));
            }
			else if(action == "getDaily")
			{
				Calendar date = (Calendar) intent.getSerializableExtra("date");
				//Sends the dailyActivities for the requested day back to ActFragment
				Intent broadcastIntent = new Intent();
				broadcastIntent.setAction("dailyActs")
				   .putExtra("acts", actList.getDaily(date));
				LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(broadcastIntent);
			}
			else if(action == "cleanUp")
			{
				Calendar yesterday = Calendar.getInstance();
				//Remove Yesterdays geo-fences
				yesterday.add(Calendar.DATE, -1);
				dailyActivities yesterdaysActs = actList.getDaily(yesterday);
				if(yesterdaysActs != null)
				{
					List<String> yesterdayActIds = yesterdaysActs.getGeofenceIds();
					GeofenceRemover geoRemover = new GeofenceRemover(getActivity());
					//Unregister all geofences from yesterday
					geoRemover.removeGeofencesById(yesterdayActIds);
					for(int i = 0; i < yesterdayActIds.size(); i++)
					{
						//Remove all geo-fences from yesterday from the store
						geoStore.clearGeofence(yesterdayActIds.get(i));
					}
				}
				
				//Register todays geo-fences
				Calendar today = Calendar.getInstance();
				dailyActivities todaysActs = actList.getDaily(today);
				if(todaysActs != null)
				{
					List<SimpleGeofence> todaysFences = todaysActs.getGeofences();
					//geoRequester requires a list of Geofence, not SimpleGeofence, so we have to convert
					List<Geofence> fencesToAdd = new ArrayList<Geofence>();
					for(int i = 0; i < todaysFences.size(); i++)
					{
						//Add todays geo-fences to the store
						geoStore.setGeofence(todaysFences.get(i).getId(), todaysFences.get(i));
						//Convert each SimpleGeofence to a Geofence and add it to the list
						fencesToAdd.add(todaysFences.get(i).toGeofence());
					}
					GeofenceRequester geoRequester = new GeofenceRequester(getActivity());
					//Register the geo-fences
					geoRequester.addGeofences(fencesToAdd);
				}
			}
			else if (action == "dismiss")
			{
				//Let the Pebble know that the user dismissed the dialog on Android,
				//dismiss on Pebble as well
				System.out.println("Sending Dismiss");
				PebbleData.addString(0, "dismiss");
	            PebbleKit.sendDataToPebble(context, PEBBLE_APP_UUID, PebbleData);
			}
		}
	}

	/*
	 * Class: GeofenceReceiver
	 * Purpose: Receives when geo-fence transitions are dectected so that they can be
	 * 	properly handled
	 * Receives: ACTION_GEOFENCE_TRANSITION
	 * From: ReceiveTransitionsIntentService
	 */
	public class GeofenceReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if(TextUtils.equals(action, GeofenceUtils.ACTION_GEOFENCE_TRANSITION))
			{
				handleGeofenceTransition(context, intent);
			}
		}
		
		/*
		 * Method: handleGeofenceTransition
		 * Purpose: When a geo-fence tranisition is detected, if it is an enter
		 * 	and a time log is underway, we must end the current timelog
		 */
		private void handleGeofenceTransition(Context context, Intent intent)
		{
			String ids = intent.getStringExtra(GeofenceUtils.GEOFENCE_ID);
			int transType = intent.getIntExtra(GeofenceUtils.EXTRA_GEOFENCE_STATUS, -1);
			String transition;
			TimeEntry oldTE = null;
			//We entered a geofence
			if(transType == Geofence.GEOFENCE_TRANSITION_ENTER)
			{
				transition = "enter ";
				if(loca != null)
					oldTE = loca.GetOpenEntry();
				//If a time entry is underway, stop it in MainActivity, this triggers a "handleStop" intent sent to ServiceReceiver
				if (oldTE != null) {
					Intent broadcastIntent = new Intent();
	            	broadcastIntent.setAction("stop");
					LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(broadcastIntent);
				}
				
				//Update currentLocation based on the geo-fence we entered
				SimpleGeofence currentFence = geoStore.getGeofence(ids);
				dailyActivities todaysActs = actList.getDaily(Calendar.getInstance());
				if(todaysActs == null)
					return;
				for(int i = 0; i < todaysActs.acts.size(); i++)
				{
					if(currentFence.getId().equals(todaysActs.acts.get(i).gFence.getId()))
					{
						currentLocation = todaysActs.acts.get(i).name;
						break;
					}
				}
			}
			else if(transType == Geofence.GEOFENCE_TRANSITION_EXIT)
				transition = "exit ";
			else
				transition = "";
			
			//Send the new location to Pebble so that it can update the display
			PebbleData.addString(0, transition + currentLocation);
            PebbleKit.sendDataToPebble(context, PEBBLE_APP_UUID, PebbleData);
            System.out.println("Sending " + transition + currentLocation + " to Pebble");
            
          //Send the new location to ReceiveTransitionsIntentService so that it can make the notification
			Intent broadcastIntent = new Intent();
			broadcastIntent.setAction("locationForNotification")
				   .putExtra("location", currentLocation);
			LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(broadcastIntent);
			
            if (transType == Geofence.GEOFENCE_TRANSITION_EXIT)
            	currentLocation = "Not on location";
            
            //Send the new location to Main so that it can update the display
			Intent broadcastIntentMain = new Intent();
			broadcastIntentMain.setAction("location")
				   .putExtra("location", currentLocation);
			LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(broadcastIntentMain);
			
		}
	}

	/*
	 * Method: handleWBChange
	 * Purpose: Logic for keeping track of time entry as user switches between Work and Break time
	 * Parameters: boolean whether this change was start by Android or not, used to 
	 * 	avoid an infinite loop when Pebble sends message 
	 */
    public void handleWBChange(boolean fromAndroid) {
        
    	//If there is no location time sheet, make a new one. Else use found Sheet
        if(timeSheet.findLocaSheet(currentLocation) == null) {
        	//Address of GLocation is unused for time sheet
            loca = new LocationTimeSheet(new GLocation("123", currentLocation));
            timeSheet.AddLocaSheet(loca);
        }
        else {
            loca = timeSheet.findLocaSheet(currentLocation);
        }
        //Set Time Entry to the open entry. If there is an open entry, complete it with an
        //end time and send the data to the server.
        currentEntry = loca.GetOpenEntry();
        if (currentEntry != null) {
            currentEntry.AddEndTime();
            new sendToServer().execute(new WebTimeEntry(currentEntry, loca.location.name));
        }
        //Update state and prepare pebble message
        if (State != TimeEntryFragment.WORK) {
            State = TimeEntryFragment.WORK;
            PebbleData.addString(0, "work");
            currentEntry = new TimeEntry(true);        
        }
        else {
            State = TimeEntryFragment.BREAK;
            PebbleData.addString(0, "break");
            currentEntry = new TimeEntry(false);
        }
        loca.AddTimeEntry(currentEntry);
        if (fromAndroid)
        {
        	String toSend = "stime";
			if(State == TimeEntryFragment.WORK)
				toSend += "work ";
			else if (State == TimeEntryFragment.BREAK)
				toSend += "break ";
			else
				toSend += "none ";

			if (strDate != null)
			{
				long seconds = strDate.getTimeInMillis();
				int hour = strDate.get(Calendar.HOUR_OF_DAY);
				int minute = strDate.get(Calendar.MINUTE);
				int second = strDate.get(Calendar.SECOND);
				toSend += (hour*3600 + minute*60 + second);
				System.out.println("Android time: " + toSend);
				//seconds -= 1394600000000l;
				//Calendar startofDay = Calendar.getInstance();
				//startofDay.set(Calendar.HOUR_OF_DAY, 0);
				//startofDay.set(Calendar.MINUTE, 0);
				//startofDay.set(Calendar.SECOND, 0);
				//long secondsPastToday = seconds - startofDay.getTimeInMillis();

				//toSend += seconds;
			}

			PebbleData.addString(0, toSend);
			System.out.println("Sending " + toSend + " to Pebble");
			//PebbleKit.sendDataToPebble(getActivity(), PEBBLE_APP_UUID, PebbleData);
            PebbleKit.sendDataToPebble(this, PEBBLE_APP_UUID, PebbleData);
            //System.out.println("Sending time to Pebble");
        }
    }
    
    /*
     * Method: getActivity
     * Purpose: Used from the BroadcastReceivers to get the TimeEntryService's context
     * Returns: TimeEntryService's context
     */
	public Context getActivity() {
		return this;
	}
	
	/*
	 * Method: addActivity
	 * Purpose: Adds a given activity to the proper dailyActivities list
	 * Parameters: Activity to be added and the date to add that activity
	 */
	public void addActivity(GLocation act, Calendar date) {
		if (actList.getDaily(date) == null) {
			actList.daily.add(new dailyActivities(date));
		}
		actList.getDaily(date).acts.add(act);
	}
	
	/*
	 * Method: deleteActivity
	 * Purpose: Deletes a given activity from the proper dailyActivities list
	 * Parameters: Activity to be deleted and the date to add that activity
	 */
	public void deleteActivity(GLocation act, Calendar date) {
        actList.getDaily(date).acts.remove(act);
    }
	
	/*
	 * Method: dailyActivities
	 * Parameters: Date of dailyActivities to retrieve
	 * Returns: A dailyActivities list for the requested day
	 */
	public dailyActivities getDaily(Calendar date) {
		return actList.getDaily(date);
	}

	/*
	 * Method: handleStop
	 * Purpose: Handle the time entry data when the stop button is pressed or stop message
	 * 	is received from the watch.
	 * Parameters: boolean whether stop was pressed on the Android or not
	 */
	public void handleStop(boolean fromAndroid) {
		if (loca != null) {
			currentEntry = loca.GetOpenEntry();
			if (currentEntry != null) {
				currentEntry.AddEndTime();
				new sendToServer().execute(new WebTimeEntry(currentEntry, loca.location.name));
				PebbleData.addString(0, "stop");
				if (fromAndroid)
				{
					PebbleKit.sendDataToPebble(this, PEBBLE_APP_UUID, PebbleData);
					System.out.println("Sending stop to Pebble");
				}
				currentEntry = null;
			}
		}
	}
}
