package de.parasec.gis.saw;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class SawBroadcastReceiver extends BroadcastReceiver {
	@Override
    public void onReceive(Context context, Intent intent) {
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
        /*
         * start CheckerService once after system boot
         * everything else is done inside CheckerService	
         */
        	Log.v("SawBroadcastReceiver", "BOOT_COMPLETED received...");
        	context.startService(new Intent(context, CheckerService.class));
        }
    }
}
