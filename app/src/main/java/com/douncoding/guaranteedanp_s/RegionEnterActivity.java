package com.douncoding.guaranteedanp_s;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Window;
import android.view.WindowManager;

public class RegionEnterActivity extends Activity {
    public static final String TAG = RegionEnterActivity.class.getSimpleName();

    public static final String ACTION_GONE_ENTER = "com.douncoding.GONE_ENTER";
    public static final String EXTRA_ENTER_STATE = "ENTER_STATE";
    public static final String EXTRA_BEACON_UUID = "BEACON_UUID";


    private static final int NOTIFICATION_ID = 1;

    NotificationManager mNotificationManager;
    EnterReceiver mReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
                WindowManager.LayoutParams.FLAG_BLUR_BEHIND);

        setContentView(R.layout.activity_enter_region);

        mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        mReceiver = new EnterReceiver();

        IntentFilter filter = new IntentFilter(ACTION_GONE_ENTER);
        filter.addCategory(Intent.CATEGORY_DEFAULT);

        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mNotificationManager = null;

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
    }


    @Override
    protected void onResume() {
        super.onResume();

        String uuid = getIntent().getStringExtra(EXTRA_BEACON_UUID);
        int state = getIntent().getIntExtra(EXTRA_ENTER_STATE, 2);

        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        RingtoneManager.getRingtone(this, sound).play();

    }

    public void sendNotification(String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_region_enter)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setContentText(message);

        //builder.setSound();
        builder.setAutoCancel(true);
        mNotificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private class EnterReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    }


    /**
     *
     * @return 과목 정보
     */
    public void 수업시간() {

    }
}
