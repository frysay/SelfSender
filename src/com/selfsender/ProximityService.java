package com.selfsender;

import java.util.List;

import com.selfsender.structures.Contact;
import com.selfsender.structures.ContactsDataSource;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Criteria;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

public class ProximityService extends Service implements android.location.GpsStatus.Listener{

	private Messenger msg = new Messenger(new MyHandler());

	private LocationManager locationManager;
	private GpsStatus gpsStatus;

	private Boolean isGPSFix; // I use Boolean instead of boolean cause I need the null check for the gps

	static final long POINT_RADIUS = 250;
	static final long PROX_ALERT_EXPIRATION = -1;

	static final int ADD_PROXIMITY_ALERT = 2;
	static final int ADD_PROXIMITY_ALERT_RESPONSE = 3;
	static final int REMOVE_PROXIMITY_ALERT = 4;
	static final int REMOVE_PROXIMITY_ALERT_RESPONSE = 5;

	int minDistance = 50;
	int minTime = 1000*5*1;

	private MyLocationListener myLocationListener;

	private Location finalLocation;
	private Location lastLocation;

	private static boolean isRunning;

	ContactsDataSource datasource;

	List<Contact> contacts;

	@Override
	public void onCreate() {
		datasource = new ContactsDataSource(getApplicationContext());
		datasource.open();
		contacts = datasource.findAll();
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);	
		locationManager.addGpsStatusListener(this);
		myLocationListener = new MyLocationListener();
		lastLocation = locationManager.getLastKnownLocation(getProvider());
		// the next try-catch is used to track down a possible Android bug that sometimes occurs.
		try {
			lastLocation.getLatitude();
			lastLocation.getLongitude();
		} catch (Exception e) {
			sendBroadcast(new Intent("error.bug.android"));
		}
		isRunning = true;
	}

	@Override
	public void onGpsStatusChanged(int event) {
		gpsStatus = locationManager.getGpsStatus(gpsStatus);
		Criteria criteria = new Criteria();
		switch (event) {
		case GpsStatus.GPS_EVENT_STARTED:			
			isGPSFix = false;
			break;

		case GpsStatus.GPS_EVENT_STOPPED:			
			isGPSFix = false;
			finalLocation = closestLocation();
			locationManager.removeUpdates(myLocationListener);
			estimateTimeDistance();
			criteria.setAccuracy(Criteria.ACCURACY_COARSE);
			locationManager.requestLocationUpdates(locationManager.getBestProvider(criteria, true), minTime, minDistance, myLocationListener);
			break;

		case GpsStatus.GPS_EVENT_FIRST_FIX:			
			if (!isGPSFix) {
				isGPSFix = true;
				if(getBatteryLevel() > 15 * 1.0f) {
					finalLocation = closestLocation();
					locationManager.removeUpdates(myLocationListener);
					estimateTimeDistance();
					criteria.setAccuracy(Criteria.ACCURACY_FINE);
					// I've used getBestProvider instead of LocationManager.GPS_PROVIDER because that, given it's an async task, by the time it's done the gps could be off again 
					locationManager.requestLocationUpdates(locationManager.getBestProvider(criteria, true), minTime, minDistance, myLocationListener);
				}
			}
			break;

		case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
			// If I check for = too I could run into the problem that I do this before getting the new location
			if(System.currentTimeMillis() - lastLocation.getTime() > minTime*2 && isGPSFix) {
				isGPSFix = false;
				finalLocation = closestLocation();
				locationManager.removeUpdates(myLocationListener);
				estimateTimeDistance();
				criteria.setAccuracy(Criteria.ACCURACY_COARSE);
				locationManager.requestLocationUpdates(locationManager.getBestProvider(criteria, true), minTime, minDistance, myLocationListener);
			} 
			break;

		default:
			break;
		}
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return msg.getBinder();
	}

	@SuppressLint("HandlerLeak")
	class MyHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {

			int msgType = msg.what;

			switch(msgType) {
			case ADD_PROXIMITY_ALERT: {
				try {
					final Bundle bundle = msg.getData();
					bundle.setClassLoader(getClassLoader());
					Contact contact = bundle.getParcelable("data");

					if(isGPSFix == null) {
						isGPSFix = false;
					}

					datasource.open();
					contacts = datasource.findAll();

					addProximityAlert(contact);

					Message resp = Message.obtain(null, ADD_PROXIMITY_ALERT_RESPONSE);

					Bundle bResp = new Bundle();
					bResp.putBoolean("respData", true);
					resp.setData(bResp);

					msg.replyTo.send(resp);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
				break;
			}

			case REMOVE_PROXIMITY_ALERT: {
				try {
					final Bundle bundle = msg.getData();
					bundle.setClassLoader(getClassLoader());
					Contact contact = bundle.getParcelable("data");

					removeProximityAlert(contact);

					Message resp = Message.obtain(null, REMOVE_PROXIMITY_ALERT_RESPONSE);
					Bundle bResp = new Bundle();
					bResp.putBoolean("respData", true);
					resp.setData(bResp);

					msg.replyTo.send(resp);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
				break;
			}

			default:
				super.handleMessage(msg);
			}
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		try {
			Bundle bundle = intent.getExtras();
			bundle.setClassLoader(getClassLoader());
			if(bundle.getBoolean("com.selfsender.proximityservice.serviceCall")) {
				Contact contact = bundle.getParcelable("com.selfsender.proximityservice.addProximityAlert");
				addProximityAlert(contact);
			}else {
				Contact contact = bundle.getParcelable("com.selfsender.proximityservice.removeProximityAlert");
				removeProximityAlert(contact);
			}
		} catch (Exception e) {
			datasource.open();
			getLastBestLocation();
		}
		return(START_REDELIVER_INTENT);
	}

	public static boolean isRunning()
	{
		return isRunning;
	}

	private void addProximityAlert(Contact contact) {
		finalLocation = closestLocation();
		locationManager.removeUpdates(myLocationListener);
		locationManager.requestLocationUpdates(getProvider(), minTime, minDistance, myLocationListener);

		Intent intent = new Intent(this, ShowStoredMessages.MyBroadcastReceiver.class);
		intent.putExtra("com.selfsender.MyBroadcastReceiver.contactID", contact.getId());
		PendingIntent proximityIntent = PendingIntent.getBroadcast(getApplicationContext(), (int) contact.getId(), intent, PendingIntent.FLAG_ONE_SHOT);
		locationManager.addProximityAlert(contact.getLocation().getLatitude(), contact.getLocation().getLongitude(), POINT_RADIUS, PROX_ALERT_EXPIRATION, proximityIntent);
	}

	private void removeProximityAlert(Contact contact) {
		Intent intent = new Intent(this, ShowStoredMessages.MyBroadcastReceiver.class);
		PendingIntent proximityIntent = PendingIntent.getBroadcast(getApplicationContext(), (int) contact.getId(), intent, PendingIntent.FLAG_ONE_SHOT); //NOTA: mettere un controllo cosa da avere davvero un int e non un long

		locationManager.removeProximityAlert(proximityIntent);
		locationManager.removeUpdates(myLocationListener);

		datasource.removeContact(contact);
		contacts = datasource.findAll();
		if (contacts.size() == 0) {
			locationManager.removeGpsStatusListener(this);
			locationManager.removeUpdates(myLocationListener);
			myLocationListener = null;
			locationManager = null;
			stopSelf();
		} else {
			finalLocation = closestLocation();
			locationManager.requestLocationUpdates(getProvider(), minTime, minDistance, myLocationListener);
		}
	}

	private Location closestLocation () {
		getLastBestLocation();
		double distance = Double.MAX_VALUE;
		int index = -1;
		for (int i=0; i < contacts.size(); i++) {
			// I've set it to = too cause it could mean that I'm moving toward the new location instead of the old one
			//(this cause if the distance got closer it means that I'm getting away from the old location)
			if (lastLocation.distanceTo(contacts.get(i).getLocation()) <= distance) {
				distance = lastLocation.distanceTo(contacts.get(i).getLocation());	
				index = i;
			}
		}
		return contacts.get(index).getLocation();
	}

	private void removeAndScheduleNewUpdate() {
		locationManager.removeUpdates(myLocationListener);
		estimateTimeDistance();
		locationManager.requestLocationUpdates(getProvider(), minTime, minDistance, myLocationListener);
	}

	private String getProvider() {
		Criteria criteria = new Criteria();
		if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && (isGPSFix) && getBatteryLevel() > 15 * 1.0f){
			criteria.setAccuracy(Criteria.ACCURACY_FINE);
		}else {
			criteria.setAccuracy(Criteria.ACCURACY_COARSE);
		}	
		return locationManager.getBestProvider(criteria, true);
	}

	private void estimateTimeDistance() {
		getLastBestLocation();
		float mLeft3;
		long tLeft3;

		mLeft3 = lastLocation.distanceTo(finalLocation);    	
		// Estimation of the time (yet to be implemented)
		tLeft3 = timeEstimation();

		minDistance = (int) mLeft3/2;
		minTime = (int) tLeft3/2;
	}

	// Note: yet to be implemented
	private long timeEstimation() {
		return 1000*10;
	}

	private float getBatteryLevel() {
		Intent batteryIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

		return ((float)level / (float)scale) * 100.0f; 
	}

	private Location getLastBestLocation() {
		Location bestResult = null;
		float bestAccuracy = Float.MAX_VALUE;
		long bestTime = Long.MIN_VALUE;

		List<String> matchingProviders = locationManager.getAllProviders();
		for (String provider: matchingProviders) {
			Location location = locationManager.getLastKnownLocation(provider);
			if (location != null) {
				float accuracy = location.getAccuracy();
				long time = location.getTime();
				if ((time > minTime && accuracy < bestAccuracy)) {
					bestResult = location;
					bestAccuracy = accuracy;
					bestTime = time;
				}
				else if (time < minTime && bestAccuracy == Float.MAX_VALUE && time > bestTime) {
					bestResult = location;
					bestTime = time;
				}
			}
		}

		// If the best result is beyond the allowed time limit, or the accuracy of the
		// best result is wider than the acceptable maximum distance, request a single update.
		// This check simply implements the same conditions we set when requesting regular
		// location updates every [minTime] and [minDistance].
		if (myLocationListener != null && (bestTime < minTime || bestAccuracy > minDistance)) {
			locationManager.requestSingleUpdate(getProvider(), myLocationListener, getMainLooper());
			bestResult = locationManager.getLastKnownLocation(getProvider());
		}

		return bestResult;
	}

	public class MyLocationListener implements LocationListener {

		@Override
		public void onLocationChanged(Location location) { 
			if((locationManager != null) && (myLocationListener != null)) {
				if (lastLocation.distanceTo(closestLocation()) > POINT_RADIUS*2) {
					finalLocation = closestLocation();
					removeAndScheduleNewUpdate();
				}else if (lastLocation.distanceTo(finalLocation) <= POINT_RADIUS) {
					// It was needed for test purposes
				}else {
					finalLocation = closestLocation();
				}
			}
		}

		@Override
		public void onProviderDisabled(String provider) {
			// It does work (Known Android issue)
		}

		@Override
		public void onProviderEnabled(String provider) {
			// It does work (Known Android issue)
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// It does work (Known Android issue)
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		datasource.close();
		isRunning = false;
	}

}

