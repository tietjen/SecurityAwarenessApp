package de.telekom.gis.saw;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import static android.app.PendingIntent.*;

// Source: http://informationideas.com/news/2012/03/06/how-to-keep-an-android-service-running/
// How to keep an Android service running? 
// This is kind of a trick question as the best practice is to not
// keep an Android service running. If you need to have something
// done continuously by your app via a service, the service should 
// be started by an AlarmManager at a regular interval and kills itself
// after the task is done. The reason for this is that a 
// long running service on Android is up to the OS to determine priority
// and in some cases would stop running. By using this methodology,
// you are treading lightly since the service does not stay in memory.

public class CheckerService extends Service {
	
	public static final String PREFS_NAME = "SawConfigAndStatus";
	
	public static final String TIMER_INTERVAL = "TimerInterval";
	public static final String IS_SERVICE_ACTIVE = "IsServiceActive";
	public static final String ALARM_MGR_INTERVAL = "AlarmMgrInterval";
	public static final String ALARM_TIMER_START = "AlarmTimerStart";

	public static final String NOTIFICATION_ID = "NotificationID";
	public static final Integer NOTIFICATION_NONE = 0;
	public static final Integer NOTIFICATION_UKS = 1;

	Integer notificationID = NOTIFICATION_NONE;
	Integer timerInterval = 15;
	boolean isServiceActive = false;
	Integer alarmMgrInterval = 5;
	long alarmTimerStart = 0;
	long currentTime = System.currentTimeMillis();
	
	@Override
	public IBinder onBind(Intent arg0) {
		// Auto-generated method stub
		return null;
	}
	@Override
	public void onCreate() {
		super.onCreate();
		Log.v("CheckerService","OnCreate()");
		this.check.run();
		this.stopSelf();
	}

    public Runnable check = new Runnable() {
    
    		@SuppressWarnings("deprecation")
			@SuppressLint("NewApi")
		public void run() {
        	
        	/*
        	 * 01 process shared preferences
        	 * 02 if IS_SERVICE_ACTIVE == true
        	 * 03		if Unknown Sources == true
        	 * 04		if AlarmTimerStart == 0 // Timer noch nicht gesetzt
        	 * 05			set shared preferences: AlarmTimerStart = CurrentTime
        	 * 06		else // AlarmTimerStart enth�lt Anfangszeit
        	 * 07			 // es war ein AlarmTime gesetzt, notification
        	 * 08			if CurrentTime - AlarmTimerStart >= IntervalTimer // 15 Min abgelaufen
        	 * 09				if notification in status bar not active
        	 * 10					raise notification
        	 * 11	else // Unknown Sources == false
        	 * 12		if (notification in status bar is active)
        	 * 13			kill notification in status bar 
        	 * 14	set AlarmManager CurrentTime + alarmMgrInterval
        	 * 15 else
        	 * 16 		// do nothing => keinen AlarmManager programmieren
        	 * 
        	 */
    		Log.v("CheckerService", "started...");

    		/*
    		 * 01 process shared preferences
    		 */
    		SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_MULTI_PROCESS);
    		boolean edited = false;

    		// isServiceActive
    		if (settings.contains(IS_SERVICE_ACTIVE)) {
    			isServiceActive = settings.getBoolean(IS_SERVICE_ACTIVE,
    					isServiceActive);
    			String temp;
    			if (isServiceActive) temp="true";
    			else temp="false";
    			Log.v("CheckerService", temp);
    		} 
    		
    		// timerInterval
    		if (settings.contains(TIMER_INTERVAL)) {
    			timerInterval = settings.getInt(TIMER_INTERVAL, timerInterval);
    		}
    		
    		// alarmMgrInterval
    		if (!settings.contains(ALARM_MGR_INTERVAL)) {
    			settings.edit().putInt(ALARM_MGR_INTERVAL, alarmMgrInterval);
    			edited = true;
    		}
    	
    		// alarmTimerStart
    		if (settings.contains(ALARM_TIMER_START)) {
    			alarmTimerStart = settings.getLong(ALARM_TIMER_START, alarmTimerStart);
    			// do nothing
    		}
    		else {
    			settings.edit().putLong(ALARM_TIMER_START, alarmTimerStart);
    			edited = true;
    		}

    		// isNotificationSet
    		if (settings.contains(NOTIFICATION_ID)) {
    			notificationID = settings.getInt(NOTIFICATION_ID, notificationID);
    		} else {
    			settings.edit().putInt(NOTIFICATION_ID, notificationID);
    			edited = true;
    		}

    		// commit all changes to shared preferences
    		if (edited) {
    			settings.edit().commit();
    		}
        	
        	
    		/*
    		 * 02 if IS_SERVICE_ACTIVE == true
    		 */

    		if (isServiceActive) {
    		
    			/*
    			 * 03		if Unknown Sources == true
    			 */
    			boolean unknownSource;
    			if (Build.VERSION.SDK_INT < 3) {
    				unknownSource = Settings.System.getInt(getContentResolver(),
    						Settings.System.INSTALL_NON_MARKET_APPS, 0) == 1;
    			} else if (Build.VERSION.SDK_INT < 17) {
    				unknownSource = Settings.Secure.getInt(getContentResolver(),
    						Settings.Secure.INSTALL_NON_MARKET_APPS, 0) == 1;
    			} else {
    				unknownSource = Settings.Global.getInt(getContentResolver(),
    						Settings.Global.INSTALL_NON_MARKET_APPS, 0) == 1;
    			}
    			Log.v("CheckerService - Status UKS: ", String.valueOf(unknownSource));
    			
    			if (unknownSource) {
    				/*
    				 * 04 if AlarmTimerStart == 0 // Timer noch nicht gesetzt
    				 * 05	set shared preferences: AlarmTimerStart = CurrentTime
    				 */
    				Log.v("CS Alarm Timer Start: ", String.valueOf(alarmTimerStart));
    				Log.v("CurrentTime",String.valueOf(currentTime));
    				
    				if (alarmTimerStart == 0) {
    					settings.edit().putLong(ALARM_TIMER_START, currentTime).commit();
    				}
    				/*
    				 * 06 else // AlarmTimerStart enth�lt Anfangszeit
    				 * 07      // es war ein AlarmTime gesetzt, notification
    				 * 08	 if CurrentTime - AlarmTimerStart >= IntervalTimer // 15 Min abgelaufen
    				 * 09		if notification in status bar not active
    				 * 10			raise notification
    				 */
    				else {
    					// TODO remove quark
//    				if (currentTime - alarmTimerStart >= timerInterval * 60 * 1000) {
   					if (currentTime - alarmTimerStart >= 15 * 1000)
                        if (notificationID.equals(NOTIFICATION_NONE)) {
                            // raise notification
                            Log.v("CheckerService", "Raise Notification");

                            String title = "SAW: Installation from unknown sources is enabled!";
                            String text = "This puts your device at risk. Click here to change this security setting.";

                            Intent resetIntent = new Intent(getApplicationContext(), NotificationResetService.class);
                            PendingIntent rpIntent = getService(getApplicationContext(), 0, resetIntent, 0);

                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                            PendingIntent pIntent = getActivity(getApplicationContext(), 0, intent, 0);

                            Notification notification = new Notification.Builder(getApplicationContext())
                                    .setContentTitle(title)
                                    .setContentText(text)
                                    .setSmallIcon(android.R.drawable.ic_dialog_alert)
                                    .setContentIntent(pIntent)
                                    .setWhen(System.currentTimeMillis())
                                    .setDeleteIntent(rpIntent)
                                    .build();

                            settings.edit().putInt(NOTIFICATION_ID, NOTIFICATION_UKS).commit();

                            NotificationManager notificationMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                            notification.flags |= Notification.FLAG_AUTO_CANCEL;
                            notificationMgr.notify(NOTIFICATION_UKS, notification);
                        }
    				}
    			}
    			/*
    			 * 11 else // Unknown Sources == false
    			 * 12	if (notification in status bar is active)
    			 * 13		kill notification in status bar and update shared prefs accordingly
    			 */
    			else {
    				if (!notificationID.equals(NOTIFICATION_NONE)) {
    					// kill notification  					
    					settings.edit().putInt(NOTIFICATION_ID, 0).commit();
    					Log.v("CheckerService","Kill Notification");
    					// reset Alarm Timer Start (to Zero)
    					settings.edit().putLong(ALARM_TIMER_START, 0).commit();
    					NotificationManager notificationMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
				    notificationMgr.cancel(NOTIFICATION_UKS);
    					
    				}
    			}
    			/*
    			 * 14	set AlarmManager CurrentTime + alarmMgrInterval
    			 */
    			Intent iRestartService = new Intent(getApplicationContext(), CheckerService.class);
    			PendingIntent piRestartService = getService(getApplicationContext(), 0, iRestartService, FLAG_UPDATE_CURRENT);
    			AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
    			alarmManager.cancel(piRestartService);
    			alarmManager.set(AlarmManager.RTC, System.currentTimeMillis()+alarmMgrInterval*1000, piRestartService);
    			Log.v("CheckerService","AlarmManager set");
    		}
		/*
		 * 15 else
		 * 16 		// do nothing => keinen AlarmManager programmieren
		 */

        }
    };


}
/*
 * 01 process shared preferences
 * 02 if IS_SERVICE_ACTIVE == true
 * 03		if Unknown Sources == true
 * 04			if AlarmTimerStart == 0 // Timer noch nicht gesetzt
 * 05				set shared preferences: AlarmTimerStart = CurrentTime
 * 06			else // AlarmTimerStart enth�lt Anfangszeit
 * 07			 // es war ein AlarmTime gesetzt, notification
 * 08				if CurrentTime - AlarmTimerStart >= IntervalTimer // 15 Min abgelaufen
 * 09					if notification in status bar not active
 * 10						raise notification
 * 11		else // Unknown Sources == false
 * 12			if (notification in status bar is active)
 * 13				kill notification in status bar 
 * 14		set AlarmManager CurrentTime + alarmMgrInterval
 * 15 else
 * 16 		// do nothing => keinen AlarmManager programmieren
 * 
 */
