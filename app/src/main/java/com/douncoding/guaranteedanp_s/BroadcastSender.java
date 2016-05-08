package com.douncoding.guaranteedanp_s;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

/**
 * 브로드캐스트 발생 담당 클래스
 */
public class BroadcastSender {
    LocalBroadcastManager mBraoManager;

    public BroadcastSender(Context context) {
        mBraoManager = LocalBroadcastManager.getInstance(context);
    }

    /**
     * 관리 비콘 상태 초기화 유도
     */
    public void sendBeaconBroadcast() {
        Intent intent = new Intent();
        intent.setAction(Constants.BEACON_BROADCAST_ACTION);
        mBraoManager.sendBroadcast(intent);
    }

    /*
    public void sendTestBroadcast(long lessonTimeId) {
        Intent intent = new Intent();
        intent.setAction(Constants.BEACON_BROADCAST_ACTION);
        intent.putExtra(Constants.EXTRA_BEACON_OPTIONS, Constants.OPTIONS_TEST);
        mBraoManager.sendBroadcast(intent);
    }
    */
}
