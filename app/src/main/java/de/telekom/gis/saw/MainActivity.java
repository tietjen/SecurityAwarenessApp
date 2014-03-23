package de.telekom.gis.saw;

import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.Editable;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends Activity {
	
	// shared preference file name
	public static final String PREFS_NAME = "SawConfigAndStatus";

	// shared preference values
	public static final String TIMER_INTERVAL = "TimerInterval";
	public static final String IS_SERVICE_ACTIVE = "IsServiceActive";
	public static final String ALARM_MGR_INTERVAL = "AlarmMgrInterval";
	public static final String ALARM_TIMER_START = "AlarmTimerStart";
	public static final String NOTIFICATION_ID = "NotificationID";

	// constants
	public static final Integer NOTIFICATION_NONE = 0;
	public static final Integer NOTIFICATION_UKS = 1;

	// shared preference default/initial values 
	Integer notificationID;
	Integer timerInterval;
	boolean isServiceActive;
	Integer alarmMgrInterval;
	long alarmTimerStart;
    /*
	 * if alarmTimerStart value != 0
	 * 	then alarmTimerStart contains UNIX time stamp in milseconds (long)
	 *   of first time "Unknown Source" activated was detected by CheckerService
	 */
    public MainActivity() {
        isServiceActive = false;
        alarmMgrInterval = 5;
        timerInterval = 15;
        notificationID = NOTIFICATION_NONE;
        alarmTimerStart = 0;// value 0 denotes "alarmTimerStart not set"
    }


	protected void onPause() {
		super.onPause();
		// rufe CheckerService auf, wenn IS_SERVICE_ACTIVE == true
		Log.v("MainActivity","onPause()");
		if (isServiceActive)
		{
			//Intent intent = new Intent("de.telekom.gis.saw.CheckerService");
//			Intent intent = new Intent(this, CheckerService.class);
//			Log.v("MainActivity","onPause()+isServiceActive==true");
//			this.startService(intent);
			startService(new Intent(CheckerService.class.getName()));
		}

	}
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		/*
		 * implementation of configureButton
		 */
		Button configureButton = (Button) findViewById(R.id.configureButton);
		configureButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				Intent intent = new Intent(
						android.provider.Settings.ACTION_SECURITY_SETTINGS);
				startActivity(intent);

			}
		});

		/*
		 * IntervalTimer value changed handling
		 */
		EditText editTimerInt = (EditText) findViewById(R.id.timerIntervalValueEditText);
		editTimerInt.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				// Method left intentionally blank

			}

			@Override
			public void afterTextChanged(Editable s) {
				// Check if Editable is not empty before processing
				if (s.length()>0){
					timerInterval = Integer.parseInt(s.toString());
				
					SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_MULTI_PROCESS);
					final Editor editor = settings.edit();
					// Log.v("EditText", s.toString());
					editor.putInt(TIMER_INTERVAL, Integer.parseInt(s.toString()));
					editor.commit();
				}

			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// Method left intentionally blank

			}
		});

		/*
		 * service enable/disable checkbox handling
		 */
		CheckBox checkBox = (CheckBox) findViewById(R.id.isServiceActiveCheckBox);
		checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				isServiceActive = isChecked;
				SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_MULTI_PROCESS);
				final Editor editor = settings.edit();
				Log.v("onCheckChanged", String.valueOf(isChecked));
				editor.putBoolean(IS_SERVICE_ACTIVE, isChecked);
				editor.commit();
			}
			
		});


	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@SuppressWarnings({ "deprecation" })
	@SuppressLint("NewApi")
	@Override
	protected void onResume() {
		super.onResume();
		/*
		 * Get and set settings from preferences or initialize preferences if
		 * not initialized on startup
		 */
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_MULTI_PROCESS);
		final Editor editor = settings.edit();
		boolean edited = false;
		
		if (settings.contains(TIMER_INTERVAL)) {
			timerInterval = settings.getInt(TIMER_INTERVAL, timerInterval);
		} else {
			editor.putInt(TIMER_INTERVAL, timerInterval);
			edited = true;
		}
		if (settings.contains(IS_SERVICE_ACTIVE)) {
			isServiceActive = settings.getBoolean(IS_SERVICE_ACTIVE,
					isServiceActive);
		} else {
			editor.putBoolean(IS_SERVICE_ACTIVE, isServiceActive);
			edited = true;
		}
		if (!settings.contains(ALARM_MGR_INTERVAL)) {
			// in MainActivity we don't access alarmMgrInterval, therefore we don't read its value from preferences
			editor.putInt(ALARM_MGR_INTERVAL, alarmMgrInterval);
			edited = true;
		}
		
		if (!settings.contains(ALARM_TIMER_START)) {
            editor.putLong(ALARM_TIMER_START, alarmTimerStart);
            edited = true;
		}
		
		// isNotificationSet
		if (settings.contains(NOTIFICATION_ID)) {
			notificationID = settings.getInt(NOTIFICATION_ID, notificationID);
		} else {
			settings.edit().putInt(NOTIFICATION_ID, notificationID);
			edited = true;
		}

		if (edited) editor.commit();

		/*
		 * check and reset notification status
		 */
		if (!notificationID.equals(NOTIFICATION_NONE)) {
			// kill notification  					
			settings.edit().putInt(NOTIFICATION_ID, 0).commit();
			Log.v("MainActivity","Kill Notification");
			// reset Alarm Timer Start (to Zero)
			settings.edit().putLong(ALARM_TIMER_START, 0).commit();
			NotificationManager notificationMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			notificationMgr.cancel(NOTIFICATION_UKS);
		}

		/*
		 * Set values from preferences file
		 */
		// Timer interval value
		EditText editTimerInt = (EditText) findViewById(R.id.timerIntervalValueEditText);
		editTimerInt.setText(timerInterval.toString());
		// Service enabled checkbox
		Log.v("Set Preference Checkbox to ", String.valueOf(isServiceActive));

		CheckBox checkBox = (CheckBox) findViewById(R.id.isServiceActiveCheckBox);
		checkBox.setChecked(isServiceActive);

		// check Install from unknown sources flag
		// Judith: better enumerate settings in order to find
		// correct value
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
        TextView t;
        if (unknownSource) {

            t = (TextView) findViewById(R.id.uks_value);
            t.setText(R.string.on_string);

        } else {
            t = (TextView) findViewById(R.id.uks_value);
            t.setText(R.string.off_string);
        }

    }
}
