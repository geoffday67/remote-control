package com.sullenart.remotecontrol;

import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.Activity;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;
import android.widget.TextView;

public class GetDevices extends AsyncTask<Void, Void, JSONArray>
{
	Activity activity;

	GetDevices(Activity activity)
	{
		this.activity = activity;
	}

	// ProgressDialog progress;

	@Override
	protected void onPreExecute()
	{
		// progress = ProgressDialog.show(MainActivity.this, "Please wait",
		// "Switching...", true);
	}

	@Override
	protected JSONArray doInBackground(Void... params)
	{
		try
		{
			URL url = new URL(MainActivity.INTERFACE);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();

			final String basicAuth = "Basic " + Base64.encodeToString(MainActivity.authentication.getBytes(), Base64.NO_WRAP);
			connection.setRequestProperty ("Authorization", basicAuth);

			// Set POST method and send body data
			connection.setDoOutput(true);
			OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
			String body = "operation=get_devices";
			writer.write(body, 0, body.length());
			writer.close();

			// Get response
			InputStream input = connection.getInputStream();
			byte[] buffer = new byte[1024];
			int count = input.read(buffer);
			input.close();

			connection.disconnect();

			// Check response for error
			String response = new String(buffer, 0, count);
			JSONObject json = new JSONObject(response);
			if (!json.getString("result").equals("ok"))
				throw new Exception(json.getString("message"));
			JSONArray devices = json.getJSONArray("devices");
			return devices;
		}
		catch (Exception e)
		{
			Log.e("REMOTE", e.toString());
			return null;
		}
	}

	@Override
	protected void onPostExecute(JSONArray devices)
	{
		if (devices == null)
			return;

		try
		{
			for (int n = 0; n < devices.length(); n++)
			{
				JSONObject device = devices.getJSONObject(n);

				int id = activity.getResources().getIdentifier(String.format("label_%d", device.getInt("id")), "id", activity.getPackageName());
				TextView text = (TextView) activity.findViewById(id);
				text.setText(device.getString("name"));
			}
			Log.d ("REMOTE", String.format ("%d names updated", devices.length ()));
		}
		catch (Exception e)
		{
		}

		/*
		 * progress.dismiss ();
		 * 
		 * if (!result) { Toast toast = Toast.makeText(MainActivity.this,
		 * "Error switching, try again!", Toast.LENGTH_SHORT); toast.show (); }
		 */
	}
}
