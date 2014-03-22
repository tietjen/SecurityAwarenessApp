package de.telekom.gis.saw;

import android.os.IBinder;
import android.util.Log;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class NotificationResetService extends Service {
	
	public static final String PREFS_NAME = "SawConfigAndStatus";
	// preference strings
	public static final String ALARM_TIMER_START = "AlarmTimerStart";
	public static final String NOTIFICATION_ID = "NotificationID";

	// constants
	public static final Integer NOTIFICATION_NONE = 0;
	public static final Integer NOTIFICATION_UKS = 1;

	// shared preference default/initial values 
	Integer notificationID = NOTIFICATION_NONE;

	@Override
	public IBinder onBind(Intent arg0) {
		// Auto-generated method stub
		//setContentView(R.layout.activity_notification);
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_MULTI_PROCESS);
		
		// reset NOTIFICATION_ID to Zero
		settings.edit().putInt(NOTIFICATION_ID, 0).commit();
		Log.v("NotificationResetService - onBind(): ","Kill Notification");
		// reset ALARM_TIMER_START to Zero
		settings.edit().putLong(ALARM_TIMER_START, 0).commit();
		// cancel active notification
		NotificationManager notificationMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notificationMgr.cancel(NOTIFICATION_UKS);

		return null;
	}

	public void onCreate() {
		super.onCreate();

		//setContentView(R.layout.activity_notification);
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_MULTI_PROCESS);
		
		// reset NOTIFICATION_ID to Zero
		settings.edit().putInt(NOTIFICATION_ID, 0).commit();
		Log.v("NotificationResetService - onCreate(): ","Kill Notification");
		// reset ALARM_TIMER_START to Zero
		settings.edit().putLong(ALARM_TIMER_START, 0).commit();
		// cancel active notification
		NotificationManager notificationMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notificationMgr.cancel(NOTIFICATION_UKS);
		
		this.stopSelf();
	}

}
