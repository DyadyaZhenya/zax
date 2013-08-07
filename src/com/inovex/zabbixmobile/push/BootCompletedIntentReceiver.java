package com.inovex.zabbixmobile.push;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * receiver to start the push service directly after the system boot
 */
public class BootCompletedIntentReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
			PushService.startOrStopPushService(context);
		}
	}

}
