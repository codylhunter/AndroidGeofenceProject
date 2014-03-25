/*
 * Structures: This interface is used for holding data structures that track logged data in the 
 * app before sending it to the server. This includes activities and timeSheet and all lists
 * containing these objects
 * 
 * Team Capsrock : Richard Bae, Chris Beichler, Cody Hunter, Taylor Woods
 */

package capsrock;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import capsrock.Geofencing.SimpleGeofence;

public interface Structures {
	
	/*
	 * Class: WebTimeEntry
	 * Purpose: Class for sending Time Entry from Android to Web
	 */
	public class WebTimeEntry {
		public TimeEntry te;
		public String location;
		public Calendar date;
		
		public WebTimeEntry(TimeEntry te, String location) {
			this.te = te;
			this.location = location;
			date = Calendar.getInstance();
		}
	}
	/*
	 * Class: DisplayTimeSheet
	 * Purpose: Class for displaying a TimeSheet on the TimeSheet Tab
	 */
	public class DisplayTimeSheet
	{
	    public Calendar arrivalTime;
	    public Calendar departureTime;
	    public long workTime;
	    public long breakTime;
	    public String location;
		/*
		 * Method: DisplayTimeSheet (Constructor)
		 * Purpose: Contruct a simple DisplayTimeSheet
		 * Returns: N/A
		 */
	    public DisplayTimeSheet(Calendar arrive, Calendar depart, long work, long end, String loc)
	    {
	        arrivalTime = arrive;
	        departureTime = depart;
	        workTime = work;
	        breakTime = end;
	        location = loc;           
	    }
		/*
		 * Method: DisplayTimeSheet (Constructor)
		 * Purpose: Contruct a simpler DisplayTimeSheet
		 * Returns: N/A
		 */
	    public DisplayTimeSheet(Calendar arrive, Calendar depart, String loc)
	    {
	        arrivalTime = arrive;
	        departureTime = depart;
	        workTime = 0;
	        breakTime = 0;
	        location = loc;
	    }
		/*
		 * Method: print
		 * Purpose: Print out a display time sheet's information
		 * Returns: void
		 */
	    @SuppressWarnings("deprecation")
		public void print()
	    {
	        System.out.println(location);
	        System.out.println("Arrived at: " + arrivalTime.getTime().getHours() + ":" + 
	                    arrivalTime.getTime().getMinutes() + ":" + arrivalTime.getTime().getSeconds());
	        System.out.println("Departed at: " + departureTime.getTime().getHours() + ":" + 
	                    departureTime.getTime().getMinutes() + ":" + departureTime.getTime().getSeconds());
	        System.out.println("Total work time: " + workTime);
	        System.out.println("Total break time: " + breakTime);
	        System.out.println("");
	    }
	    
	}

	/*
	 * Class: TimeEntry
	 * Purpose: Hold the start and end time for a TimeEntry, as well as a boolean for work/break
	 */
	public class TimeEntry {
		Calendar startTime;
		Calendar endTime;
		boolean workTime;
		/*
		 * Method: TimeEntry (Constructor)
		 * Purpose: Contruct a simple TimeEntry
		 * Returns: Nothing
		 */
		public TimeEntry() {
			startTime = Calendar.getInstance();
			endTime = null;
			workTime = true;
		}
		/*
		 * Method: TimeEntry (Constructor)
		 * Purpose: Contruct a simple TimeEntry
		 * Returns: Nothing
		 */
		public TimeEntry(boolean work) {
			startTime = Calendar.getInstance();
			endTime = null;
			workTime = work;
		}   
		/*
		 * Method: TimeEntry (Constructor)
		 * Purpose: Contruct a simple TimeEntry
		 * Returns: Nothing
		 */
        public TimeEntry(Calendar start, Calendar stop, boolean work)
        {
            startTime = start;
            endTime = stop;
            workTime = work;
        }
        /*
		 * Method: AddEndTime
		 * Purpose: Add the end Time to a Time Entry when logged time is stopped
		 * Returns: void
		 */
		public void AddEndTime() {
			endTime = Calendar.getInstance();
		}
	}
	/*
	 * Class: locationCompare
	 * Purpose: Compares two locations
	 */
    public class locationCompare implements Comparator<WebTimeEntry>
    {
    	public locationCompare() {
    		super();
    	}
        @Override
        public int compare(WebTimeEntry o1, WebTimeEntry o2)
        {
            return o1.location.compareTo(o2.location);
        }
    }
	/*
	 * Class: GLocation
	 * Purpose: Holds all the details about an activity. The address, name, note, start/stop times,
	 * the Geofence associated with it and the radius
	 */
	public class GLocation implements Serializable {
		private static final long serialVersionUID = -6753357416573204293L;
		String address;
		String name;
		String note;
		Calendar expectedStart;
		Calendar expectedStop;
		int Radius;
		SimpleGeofence gFence;
		/*
		 * Method: GLocation (Constructor)
		 * Purpose: Construct a Default GLocation
		 * Returns: N/A
		 */
		public GLocation() {
			address = "123 Main Street";
			name = "Default Location";
			note = "This is the Default Location";
			expectedStart = Calendar.getInstance();
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.HOUR, 1);
			expectedStop = cal;
		}
		/*
		 * Method: GLocation (Constructor)
		 * Purpose: Construct a simple GLocation
		 * Returns: N/A
		 */
		public GLocation(String add, String nm) {
			address = new String(add);
			name = new String(nm);
			note = "This is a Location";
			expectedStart = Calendar.getInstance();
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.HOUR, 1);
			expectedStop = cal;
		}
		/*
		 * Method: GLocation (Constructor)
		 * Purpose: Construct a Full GLocation
		 * Returns: N/A
		 */
		public GLocation(String name, String note, String add, Calendar start, Calendar stop, int radius, SimpleGeofence g) {
			this.name = new String(name);
			this.address = new String(add);
			this.note = new String(note);
			expectedStart = start;
			expectedStop = stop;
			this.Radius = radius;
			this.gFence = g;
		}
	}
	/*
	 * Class: Activities
	 * Purpose: Holds a list of all the daily activity lists (i.e. holds a list of ALL activities lists
	 * for March 7, March 8, etc...)
	 */
	public class Activities {
		ArrayList<dailyActivities> daily;
		/*
		 * Method: Activities (Constructor)
		 * Purpose: Construct a simple Activities Object
		 * Returns: N/A
		 */
		public Activities() {
			daily = new ArrayList<dailyActivities>();
		}
		/*
		 * Method: getDaily
		 * Purpose: Receives a date and returns the dailyActivities list for that day
		 * Returns: dailyActivities for "today"
		 */
		public dailyActivities getDaily(Calendar today) {
			Iterator<dailyActivities> it = daily.iterator();
			dailyActivities current;
			while(it.hasNext()) {
				current = it.next();
				if (current.day.get(Calendar.DATE) == today.get(Calendar.DATE)) {
					return current;
				}
			}
			return null;
		}
	}
	/*
	 * Class: dailyActivities
	 * Purpose: Holds a list of all activities in one day (i.e. holds a list of ALL activities
	 * for March 7th ONLY)
	 */
    public class dailyActivities implements Serializable {

        private static final long serialVersionUID = -5764804173377338875L;
        Calendar day;
        ArrayList<GLocation> acts;
		/*
		 * Method: dailyActivities (Constructor)
		 * Purpose: Construct a default dailyActivities Object
		 * Returns: N/A
		 */
        public dailyActivities() {
            day = Calendar.getInstance();
            acts = new ArrayList<GLocation>();
        }
		/*
		 * Method: dailyActivities (Constructor)
		 * Purpose: Construct a simple dailyActivities
		 * Returns: N/A
		 */
        public dailyActivities(Calendar day) {
            this.day = (Calendar)day.clone();
            acts = new ArrayList<GLocation>();
        }
		/*
		 * Method: getGeofenceIds
		 * Purpose: return the geofence ID's for every GLocation in this instance
		 * Returns: List<String> of all geofence ID's
		 */
        public List<String> getGeofenceIds() {
            List<String> gIDs = new ArrayList<String>();
            for (int i = 0; i < acts.size(); i++) {
                gIDs.add(acts.get(i).gFence.getId());
            }
            return gIDs;
        }
		/*
		 * Method: getGeofences
		 * Purpose: return the geofences for every GLocation in this instance
		 * Returns: List<SimpleGeofence> of all fences for this day.
		 */
        public List<SimpleGeofence> getGeofences()
        {
        	List<SimpleGeofence> fences = new ArrayList<SimpleGeofence>();
        	for (int i = 0; i < acts.size(); i++)
        	{
        		fences.add(acts.get(i).gFence);
        	}
        	return fences;
        }
        
    }
	
	/*
	 * Class: LocationTimeSheet
	 * Purpose: Holds the timesheet entries (actual logged time) for a certain activity.
	 */
	public class LocationTimeSheet {
		public GLocation location;
		ArrayList<TimeEntry> timeEntries;
		public int index;
		/*
		 * Method: LocationTimeSheet (Constructor)
		 * Purpose: Construct a default LocationTimeSheet
		 * Returns: N/A
		 */
		/*
		public LocationTimeSheet() {
			location = new GLocation("hry","grr");
			timeEntries = new ArrayList<TimeEntry>();
			index = -1;
		}
		*/
		/*
		 * Method: LocationTimeSheet (Constructor)
		 * Purpose: Construct a simple LocationTimeSheet
		 * Returns: N/A
		 */
		public LocationTimeSheet(GLocation loc) {
			location = loc;
			timeEntries = new ArrayList<TimeEntry>();
			index = -1;
		}
		/*
		 * Method: AddTimeEntry
		 * Purpose: Add a Time Entry to this TimeSheet
		 * Returns: void
		 */
		public void AddTimeEntry(TimeEntry entry) {
			index = timeEntries.size();
			timeEntries.add(entry);
		}		
		/*
		 * Method: GetOpenEntry
		 * Purpose: Return the very last entry in the TimeEntry List, if there is one
		 * Returns: TimeEntry of the open entry
		 */
		public TimeEntry GetOpenEntry() {
			if (index > -1)
				if (timeEntries.get(index).endTime == null)
					return timeEntries.get(index);
			return null;
		}
	}
	
	/*
	 * Class: TimeSheet
	 * Purpose: Holds a list of locationTimeSheets for a specific day.
	 */
	public class TimeSheet {
		Calendar date;
		ArrayList<LocationTimeSheet> locaSheets;
		/*
		 * Method: TimeSheet (Constructor)
		 * Purpose: Construct a simple TimeSheet
		 * Returns: N/A
		 */
		public TimeSheet() {
			date = Calendar.getInstance();
			locaSheets = new ArrayList<LocationTimeSheet>();
		}
		/*
		 * Method: AddLocaSheet 
		 * Purpose: Add the locationtimesheet to this Instance
		 * Returns: N/A
		 */
		public void AddLocaSheet(LocationTimeSheet loca) {
			locaSheets.add(loca);
		}
		/*
		 * Method: findLocaSheet
		 * Purpose: Search through all the locationTimeSheets and find the match
		 * Returns: LocationTimeSheet matching the locationString provided
		 */
		public LocationTimeSheet findLocaSheet(String locationString) {
			for (int i = 0; i < locaSheets.size(); i++) {
				if (locaSheets.get(i).location.name.equals(locationString)){
					return locaSheets.get(i);
				}
			}
			return null;
		}
	}
	
}