/*
 * sendToServer: This AsyncTask is used to send TimeEntry Objects to the server
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
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import capsrock.Structures.WebTimeEntry;
import android.os.AsyncTask;
import android.os.Environment;

public class sendToServer extends AsyncTask<WebTimeEntry, Void, Boolean>
{
	/*
	 * Method: doInBackground
	 * Purpose: Creates a connection to the server. Formats the Time Entry 
	 * 	and sends it off
	 * Return: Boolean - true/false depending on errors
	 */
    @SuppressWarnings("deprecation")
	@Override
    protected Boolean doInBackground(WebTimeEntry... entry)
    {
        URL url;
        String externalStorageDir = Environment.getExternalStorageDirectory().toString();
        File f = new File(externalStorageDir, "Capsrock/serverInfo.txt");
        String IP = null;
        try{
        	BufferedReader read = new BufferedReader(new FileReader(f));
        	//If the file is empty return false
        	IP = read.readLine();
        	if(IP == null)
        	{
        		System.out.println("Null IP");
        		read.close();
        		return false;
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
        	return false;
        String connectionStr = "http://" + IP + ":8000/api/activity/"; 
        try
        {
            //Set up our connection
            url = new URL(connectionStr);
            Integer month = entry[0].date.getTime().getMonth() + 1;
            String monthString;
            if(month < 10)
                monthString = "0" + month;
            else
                monthString = month.toString();
            int year = entry[0].date.getTime().getYear() + 1900;

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            //connection.setRequestProperty("Content-Type", "application/json");

            //Construct string to be sent
            String input = "{\"location\": \"" + entry[0].location + "\", "
                             + "\"date\": \"" + year + "-" + monthString + "-" + entry[0].date.getTime().getDate() + "\", "
                             + "\"startTime\": \"" + entry[0].te.startTime.getTime().getHours() + ":" + entry[0].te.startTime.getTime().getMinutes() + ":" + entry[0].te.startTime.getTime().getSeconds() + "\", "
                             + "\"stopTime\": \"" + entry[0].te.endTime.getTime().getHours() + ":" + entry[0].te.endTime.getTime().getMinutes() + ":" + entry[0].te.endTime.getTime().getSeconds() + "\", "
                             + "\"workTime\": \"" + entry[0].te.workTime + "\""
                             + "}";
            //Send string to server
            OutputStream out = connection.getOutputStream();
            out.write(input.getBytes());
            out.flush();

            //Verify that there was no error
            if(connection.getResponseCode() != HttpURLConnection.HTTP_CREATED)
                throw new RuntimeException("Failed : HTTP error code : " + connection.getResponseCode());              

            //Exit the connection
            connection.disconnect();
        } 
        //Error catching
        catch (MalformedURLException ex)
        {
            return false;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
        return true;
    }
} 
