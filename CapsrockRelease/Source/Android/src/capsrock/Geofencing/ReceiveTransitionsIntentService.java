/*
 * ReceiveTransitionIntentService: This service runs in the background and handles intents
 * sent by Google Play Services. It is also responsible for sending a phone notification to
 * the user when they have entered/exited a geofence. 
 * 
 * Team Capsrock: Richard Bae, Chris Beichler, Cody Hunter, Taylor Woods
 * 
 * Contains code provided by Android developers under the Android Open Source Project
 */

package capsrock.Geofencing;

import capsrock.AlarmReceiver;
import capsrock.MainActivity;
import capsrock.R;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.List;

/**
 * This class receives geofence transition events from Location Services, in the
 * form of an Intent containing the transition type and geofence id(s) that triggered
 * the event.
 */
public class ReceiveTransitionsIntentService extends IntentService {
	IntentFilter geoServiceFilter;
	GeoServiceReceiver geoServiceReceiver;
	String transitionType;
	/**
     * Sets an identifier for this class' background thread
     */
    public ReceiveTransitionsIntentService() {
        super("ReceiveTransitionsIntentService");
    }
    
    @Override
    public void onCreate()
    {
    	super.onCreate();
    	geoServiceReceiver = new GeoServiceReceiver();
		geoServiceFilter = new IntentFilter();
	    geoServiceFilter.addAction("locationForNotification");
	    LocalBroadcastManager.getInstance(this).registerReceiver(geoServiceReceiver, geoServiceFilter);
    }

    /**
     * Handles incoming intents
     * @param intent The Intent sent by Location Services. This Intent is provided
     * to Location Services (inside a PendingIntent) when you call addGeofences()
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        // Create a local broadcast Intent
        Intent broadcastIntent = new Intent();
        Log.d("Intent", "Intent recieved: Handling");
        // Give it the category for all intents sent by the Intent Service
        //broadcastIntent.addCategory(GeofenceUtils.CATEGORY_LOCATION_SERVICES);

        // First check for errors
        if (LocationClient.hasError(intent)) {
            // Get the error code
            int errorCode = LocationClient.getErrorCode(intent);
            // Get the error message
            String errorMessage = LocationServiceErrorMessages.getErrorString(this, errorCode);
            // Log the error
            Log.e(GeofenceUtils.APPTAG,
                    getString(R.string.geofence_transition_error_detail, errorMessage)
            );

            // Set the action and error message for the broadcast intent
            broadcastIntent.setAction("geofenceTransitionName")
            			   .addCategory(GeofenceUtils.CATEGORY_LOCATION_SERVICES)
                           .putExtra(GeofenceUtils.EXTRA_GEOFENCE_STATUS, errorMessage);

            // Broadcast the error *locally* to other components in this app
            LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
        // If there's no error, get the transition type and create a notification
        } else {
            // Get the type of transition (entry or exit)
            int transition = LocationClient.getGeofenceTransition(intent);
            
            // Test that a valid transition was reported
            if ((transition == Geofence.GEOFENCE_TRANSITION_ENTER) ||
                (transition == Geofence.GEOFENCE_TRANSITION_EXIT)) 
            {
            	List<Geofence> geofences = LocationClient.getTriggeringGeofences(intent);
                String[] geofenceIds = new String[geofences.size()];
                for (int index = 0; index < geofences.size() ; index++) {
                    geofenceIds[index] = geofences.get(index).getRequestId();
                }
                String ids = TextUtils.join(GeofenceUtils.GEOFENCE_ID_DELIMITER, geofenceIds);
                transitionType = getTransitionString(transition);
            	
                broadcastIntent.setAction(GeofenceUtils.ACTION_GEOFENCE_TRANSITION)
				   .addCategory(GeofenceUtils.CATEGORY_LOCATION_SERVICES)
				   .putExtra(GeofenceUtils.EXTRA_GEOFENCE_STATUS, transition)
				   .putExtra(GeofenceUtils.GEOFENCE_ID, ids);
				LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);
                
            // An invalid transition was reported
            } else {
                // Always log as an error
                Log.e(GeofenceUtils.APPTAG,
                        getString(R.string.geofence_transition_invalid_type, transition));
            }
        }
    }

    /**
     * Posts a notification in the notification bar when a transition is detected.
     * If the user clicks the notification, control goes to the main Activity.
     * @param transitionType The type of transition that occurred.
     *
     */
    private void sendNotification(String transitionType, String name) {
    	String transition;
        // Create an explicit content Intent that starts the main Activity
        Intent startAppIntent =  new Intent(getApplicationContext(), MainActivity.class);
        startAppIntent.setAction("android.intent.action.MAIN");
        startAppIntent.addCategory("android.intent.category.LAUNCHER");

        Intent startTimerIntent =  new Intent(getApplicationContext(), AlarmReceiver.class);
        startTimerIntent.setAction("getStart");
        
        // Get a PendingIntent containing the entire back stack
        PendingIntent startAppPendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, startAppIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        if(transitionType == getString(R.string.geofence_transition_entered))
        	transition = "Arrived at ";
        else if(transitionType == getString(R.string.geofence_transition_exited))
        	transition = "Left ";
        else
        	transition = "Error ";
        
        // Get a notification builder that's compatible with platform versions >= 4
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        // Set the notification contents
        long[] array = {0, 500};
        RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.notification_layout);
        contentView.setImageViewResource(R.id.image, R.drawable.ic_launcher);
        contentView.setTextViewText(R.id.title, "Custom notification");
        contentView.setTextViewText(R.id.text, "This is a custom layout");
        
		builder.setSmallIcon(R.drawable.ic_notification)
			   .setContentTitle(transition + name)
               .setContentText("Click to return to application")
               .setContentIntent(startAppPendingIntent)
               .setVibrate(array)
               .setAutoCancel(true);
		
        // Get an instance of the Notification manager
        NotificationManager mNotificationManager =
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // Issue the notification
        mNotificationManager.notify(0, builder.build());
    }

    /**
     * Maps geofence transition types to their human-readable equivalents.
     * @param transitionType A transition type constant defined in Geofence
     * @return A String indicating the type of transition
     */
    private String getTransitionString(int transitionType) {
        switch (transitionType) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                return getString(R.string.geofence_transition_entered);
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                return getString(R.string.geofence_transition_exited);
            default:
                return getString(R.string.geofence_transition_unknown);
        }
    }
    
    
    /*
     * Class: GeoServiceReceiver
     * Purpose: Receives the name of the location the user is currently at and sends a notification
     * with that location's name.
     * Receives: "location"
     * From: TimeEntryService
     * 
     */
    public class GeoServiceReceiver extends BroadcastReceiver
	{
    	@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			int transition = -1;
			if (action == "locationForNotification")
			{
				String loc = intent.getStringExtra("location");
				
				// Changes the int transition based on whether the geofence was entered or exited
				if(transitionType == getString(R.string.geofence_transition_entered))
		        	transition = Geofence.GEOFENCE_TRANSITION_ENTER;
		        else if(transitionType == getString(R.string.geofence_transition_exited))
		        	transition = Geofence.GEOFENCE_TRANSITION_EXIT;
				
				// Sends a broadcast containing the transtion type and thhe location name to MainActivity
				// in order to create a popup notifcation. 
				Intent broadcastIntent = new Intent();
				broadcastIntent.setAction("geofenceTransitionName")
 				   .addCategory(GeofenceUtils.CATEGORY_LOCATION_SERVICES)
 				   .putExtra(GeofenceUtils.EXTRA_GEOFENCE_STATUS, transition)
 				   .putExtra("locationName", loc);
				LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);
				
				 // Post a notification
                sendNotification(transitionType, loc);                
                
                // Log the transition type and a message
                Log.d(GeofenceUtils.APPTAG,
                        getString(
                                R.string.geofence_transition_notification_title,
                                transitionType,
                                loc));
                Log.d(GeofenceUtils.APPTAG,
                        getString(R.string.geofence_transition_notification_text));
			}
		}
	}
}
