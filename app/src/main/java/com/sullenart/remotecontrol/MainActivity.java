package com.sullenart.remotecontrol;

import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import org.json.JSONObject;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

public class MainActivity extends Activity implements View.OnLongClickListener
{
	public final static String INTERFACE = "http://192.168.1.5:1968/interface";

	@SuppressWarnings("rawtypes")
	private void getViewsByClass (ViewGroup root, Class target, ArrayList<View> result)
	{
		for (int n = 0; n < root.getChildCount(); n++)
		{
			View view = root.getChildAt (n);
			if (view instanceof ViewGroup)
			{
				getViewsByClass ((ViewGroup) view, target, result);
			}
			else
			{
				if (target.isInstance (view))
				{
					result.add (view);
				}
			}
		}
	}
	
	public void onTest (View view)
	{
		new GetDevices(this).execute();
	}
	
	@SuppressWarnings("rawtypes")
	private ArrayList<View> getViewsByClass (ViewGroup root, Class target)
	{
		ArrayList<View> result = new ArrayList<View> ();
		getViewsByClass (root, target, result);
		return result;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		ViewGroup root = (ViewGroup) findViewById (R.id.root_layout);
		ArrayList<View> images = getViewsByClass (root, ImageButton.class);
		for (View view: images)
		{
			view.setOnLongClickListener(this);
		}
	}

	@Override
	public void onResume ()
	{
		super.onResume ();
		new GetDevices(this).execute();
	}
	
	@Override
	public void onConfigurationChanged(Configuration config)
	{
		super.onConfigurationChanged(config);
	}

	@Override
	public boolean onLongClick (View view)
	{
		// Show further options for 'on' only
		if (!view.getTag().toString().endsWith("_on"))
				return true;
		
		String[] parts = ((String) view.getTag()).split("_");
		final String channel = parts[1];
		final String command = parts[2];
		
		AlertDialog.Builder builder = new AlertDialog.Builder (this);
		LayoutInflater inflater = this.getLayoutInflater ();
		final View custom_view = inflater.inflate (R.layout.switch_on_options, null);
		builder.setView (custom_view);
		builder.setPositiveButton ("OK", new DialogInterface.OnClickListener ()
		{
			@Override
			public void onClick (DialogInterface dialog, int which)
			{
				EditText auto_off_view = (EditText) custom_view.findViewById (R.id.auto_off);
				String duration = auto_off_view.getText().toString();
                new async433Operation().execute (channel, command, duration);

			}
		});
		builder.setNegativeButton ("Cancel", new DialogInterface.OnClickListener ()
		{
			@Override
			public void onClick (DialogInterface dialog, int which)
			{
				dialog.cancel ();
			}
		});
		builder.setTitle ("Options");
		builder.create ().show ();

		return true;
	}
	
	public void onSwitchClick (View view)
	{
		String[] parts = ((String) view.getTag()).split("_");
		new async433Operation().execute (parts[1], parts[2]);
	}
				
	private class async433Operation extends AsyncTask<String, Void, Boolean>
	{
		ProgressDialog progress;
		
		@Override
		protected void onPreExecute ()
		{
			progress = ProgressDialog.show(MainActivity.this, "Please wait", "Switching...", true);
		}
		
		@Override
		protected Boolean doInBackground(String... params)
		{
			int channel = Integer.valueOf(params[0]);
			String command = params[1];
			
			int duration = 0;
			if (params.length >= 3)
				duration = Integer.valueOf (params[2]);
			
			try
			{
				sync433Operation (channel, command, duration);
				return true;
			}
			catch (Exception e)
			{
				Log.e ("REMOTE", e.toString ());
				return false;
			}
		}
		
		@Override
		protected void onPostExecute (Boolean result)
		{
			progress.dismiss ();
			
			if (!result)
			{
				Toast toast = Toast.makeText(MainActivity.this, "Error switching, try again!", Toast.LENGTH_SHORT);
				toast.show ();
			}
		}
	}
	
	private void sync433Operation (int channel, String command, int duration) throws Exception
	{
        URL url = new URL (INTERFACE);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection ();

        // Set POST method and send body data
        connection.setDoOutput (true);
        OutputStreamWriter writer = new OutputStreamWriter (connection.getOutputStream ());
        String body = String.format ("operation=433&channel=%d&command=%s", channel, command);
        
        if (duration > 0)
        	body += String.format ("&duration=%d", duration * 60);	// Convert minutes to seconds
        	
        writer.write (body, 0, body.length ());
        writer.close ();
        
        // Get response
        InputStream input = connection.getInputStream ();
        byte[] buffer = new byte[512];
        int count = input.read (buffer);
        input.close ();

        connection.disconnect ();

        // Check response for error
        String response = new String (buffer, 0, count);
        JSONObject json = new JSONObject (response);
        if (!json.getString ("result").equals ("ok"))
                throw new Exception ("Error returned by server");
	}
}
