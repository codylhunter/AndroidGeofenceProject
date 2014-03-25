/*
 * pullFromServer: This AsyncTask is used to pull TimeSheet Information from the server
 * 
 * Team Capsrock : Richard Bae, Chris Beichler, Cody Hunter, Taylor Woods
 */
package capsrock;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.StringTokenizer;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import capsrock.Structures.DisplayTimeSheet;
import capsrock.Structures.TimeEntry;
import capsrock.Structures.WebTimeEntry;
import capsrock.Structures.locationCompare;
import android.os.AsyncTask;
import android.os.Environment;

public class pullFromServer extends AsyncTask<String, Void, ArrayList<DisplayTimeSheet>>
{
	/*
	 * Interface: OnTaskCompleted
	 * Purpose: Interface between this AsynchTask and the Activity
	 * 	that called it
	 */
	public interface OnTaskCompleted {
		void onTaskCompleted(ArrayList<DisplayTimeSheet> list);
	}
	
	private OnTaskCompleted listener;
	/*
	 * Method: pullFromServer (Constructor)
	 * Purpose: Set the listener in OnTaskCompleted to listen for this
	 * 	task to be completed
	 */
	public pullFromServer(OnTaskCompleted listener) {
		this.listener = listener;
	}
	/*
	 * Method: onPostExecute
	 * Purpose: call the OnTaskCompleted function of the listener
	 * Return: void
	 */
	@Override
	protected void onPostExecute(ArrayList<DisplayTimeSheet> list) {
		if (listener != null) {
			listener.onTaskCompleted(list);
		}
		else
			;
	}
	/*
	 * Method: doInBackground
	 * Purpose: Creates a connection to the server. Formats the requested data, and
	 * 	returns an ArrayList of all the relevant data
	 * Return: ArrayList<DisplayTimeSheet> of all DisplayTimeSheet objects
	 * 	for the requested day
	 */
	@Override
    protected ArrayList<DisplayTimeSheet> doInBackground(String... date)
    {
		 String externalStorageDir = Environment.getExternalStorageDirectory().toString();
	        File f = new File(externalStorageDir, "Capsrock/serverInfo.txt");
	        String IP = null;
	        try{
	        	BufferedReader read = new BufferedReader(new FileReader(f));
	        	//If the file is empty return false
	        	IP = read.readLine();
	        	if(IP == null)
	        	{
	        		System.out.println("Pull Null IP");
	        		read.close();
	        		return null;
	        	}
	        	read.close();
	        } catch (FileNotFoundException e)
	        {
	        	e.printStackTrace();
	        } catch (IOException e)
	        {
	        	e.printStackTrace();
	        }
	        
	        if (IP == null)
	        	return null;
		
        String connectionStr = "http://" + IP + ":8000/api/activity/date/" + date[0];
        ArrayList<WebTimeEntry> wteList = new ArrayList<WebTimeEntry>();
        ArrayList<DisplayTimeSheet> dtsList = new ArrayList<DisplayTimeSheet>();
        DisplayTimeSheet dts = null;
        WebTimeEntry wte = null;
        TimeEntry te = null;
        String location = null;
        Calendar start = Calendar.getInstance();
        Calendar stop = Calendar.getInstance();
        boolean work = false;  
               
        try
        {
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(connectionStr);
            HttpResponse httpResponse = httpClient.execute(httpGet);
            HttpEntity httpEntity = httpResponse.getEntity();
            String output = EntityUtils.toString(httpEntity);
            
            //Read what was sent to us
            //Separate the responses
            StringTokenizer strTok = new StringTokenizer(output, "[]{}");
            while (strTok.hasMoreTokens()) {
                String singleEntry = strTok.nextToken();
                if(!singleEntry.equals(", "))
                {
                    StringTokenizer builder = new StringTokenizer(singleEntry, "\n\":,");
                    while(builder.hasMoreTokens())
                    {
                        String tag = builder.nextToken();
                        if(tag.equals("location") && location == null){
                            builder.nextToken(); //Empty space
                            location = builder.nextToken();
                        }
                        else if(tag.equals("startTime"))
                        {
                            builder.nextToken(); //Empty space
                            String hour = builder.nextToken();
                            String min = builder.nextToken();
                            String sec = builder.nextToken();
                            start.set(0, 0, 0, Integer.parseInt(hour), 
                                    Integer.parseInt(min), Integer.parseInt(sec));
                        }
                        else if(tag.equals("stopTime"))
                        {
                            builder.nextToken(); //Empty space
                            String hour = builder.nextToken();
                            String min = builder.nextToken();
                            String sec = builder.nextToken();
                            stop.set(0, 0, 0, Integer.parseInt(hour), 
                                    Integer.parseInt(min), Integer.parseInt(sec));
                        }
                        else if(tag.equals("workTime"))
                        {
                            //Yes there is a space in front of true
                            if(builder.nextToken().equals(" true"))
                                work = true;
                            else
                                work = false;
                        }
                    }
                    te = new TimeEntry(start, stop, work);
                    wte = new WebTimeEntry(te, location);
                    wteList.add(wte);
                    location = null;
                    start = Calendar.getInstance();
                    stop = Calendar.getInstance();
                }
            }
            
            //Sort list by location alphabetically
            Collections.sort(wteList, new locationCompare());
            
            if(wteList.size() >= 1)
            {
                    dts = new DisplayTimeSheet(wteList.get(0).te.startTime, 
                            wteList.get(0).te.endTime, wteList.get(0).location);
            }
            
            //Go through sorted list
            for (int i = 0; i < wteList.size(); i++)
            {
                wte = wteList.get(i);
                //Ensure we're still at the same location
                if(!wte.location.equals(dts.location))
                {
                    //Add to the list then make a new DisplayTimeSheet
                    dtsList.add(dts);
                    dts = new DisplayTimeSheet(wte.te.startTime, wte.te.endTime, wte.location);
                }
                
                //Update total work or break time at this location
                long diff = wte.te.endTime.getTimeInMillis() - wte.te.startTime.getTimeInMillis();
                if(wte.te.workTime)    
                    dts.workTime += diff;
                else
                    dts.breakTime += diff;

                //Update time arrival and/or departure if necessary
                if(wte.te.startTime.before(dts.arrivalTime))
                    dts.arrivalTime = wte.te.startTime;
                if(wte.te.endTime.after(dts.departureTime))
                    dts.departureTime = wte.te.endTime;
            }
            if(dts != null)
                dtsList.add(dts);
        }
        //Error catching
        catch (MalformedURLException ex)
        {
            //Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        } catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
        return dtsList;
    }

} 
