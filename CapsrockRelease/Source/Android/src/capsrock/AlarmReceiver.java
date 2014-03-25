/*
 * AlarmReceiver: Receives intents sent through the AlarmManager's pendingintents and broadcasts them through
 * a LocalBroadcastManager.
 * 
 * Team Capsrock : Richard Bae, Chris Beichler, Cody Hunter, Taylor Woods
 */

package capsrock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

/*
 * Class: AlarmReceiver
 * Purpose: Receives the intent sent by the AlarmManager in TimeEntryService and sends it back through
 * a LocalBroadcastManager.
 * Receives: "cleanup"
 * From: TimeEntryService
 */
public class AlarmReceiver extends BroadcastReceiver {

	/*
	 * Method: onReceive
	 * Purpose: Receives the "cleanup" intent sent by AlarmManager and sends it to TimeEntryService
	 * Returns: Null
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		LocalBroadcastManager.getInstance(context).sendBroadcast(
			new Intent(context, TimeEntryService.class).setAction(intent.getAction()));
	
	}
}