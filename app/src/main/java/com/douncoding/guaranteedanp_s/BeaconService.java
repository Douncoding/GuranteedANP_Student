package com.douncoding.guaranteedanp_s;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;

import java.util.Collection;
import java.util.HashMap;

public class BeaconService extends Service implements BeaconConsumer {
    public static final String TAG = BeaconService.class.getSimpleName();
    public static final String ACTION_SERVICE_STOP = "com.douncoding.SERVICE_STOP";

    BeaconManager mBeaconManager;

    HashMap<Beacon, Integer> enterBeacons = new HashMap();
    HashMap<Beacon, Boolean> addNotifiedBeacons = new HashMap<>();

    /**
     * 비콘의 인식이 안정하지 못한 문제가 있으며, 이를 보완하기 위해 디바운싱의
     * 개념을 사용한다. 일반적인 경우 감지되고 있는 상태중 신호를 잃어 버리는 경우에
     * 대한 예외 처리임에 따라 최소 입려되는 enterBeacons 의 Integer 값은
     * deboundThreshold 의 -1 값이다. 이는 1회 감지되자 마자 바로 잃어버리는 경우
     * 와 같은 비정상 경우에 대비할 수 있음을 말한다.
     */
    private static final int deboundThreshold = 4;


    public BeaconService() { }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");

        mBeaconManager = BeaconManager.getInstanceForApplication(this);
        mBeaconManager.getBeaconParsers()
                .add(new BeaconParser()
                        .setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));

        new BackgroundPowerSaver(this);

        mBeaconManager.bind(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");

        enterBeacons = null;
        mBeaconManager.unbind(this);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onBeaconServiceConnect() {
        mBeaconManager.setRangeNotifier(new RangeNotifier() {

            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                Log.d(TAG, "Region: " + region.getUniqueId());
                validateBeaconStatus(beacons);
            }
        });

        try {
            mBeaconManager.startRangingBeaconsInRegion(new Region("myMonitoringUniqueId", null, null, null));
        } catch (RemoteException e) {    }
    }

    private void notifyInsertBeacon(Beacon beacon) {
        if (!addNotifiedBeacons.containsKey(beacon)) {
            Log.i(TAG, "추가");
            addNotifiedBeacons.put(beacon, true);

            Intent intent = new Intent(BeaconService.this, RegionEnterActivity.class);
            intent.putExtra(RegionEnterActivity.EXTRA_ENTER_STATE, 1);
            intent.putExtra(RegionEnterActivity.EXTRA_BEACON_UUID, beacon.getId1().toString());
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else  {
            Log.d(TAG, "추가: 이미 전송된 정보");
        }
    }

    private void notifyDeleteBeacon(Beacon beacon) {
        addNotifiedBeacons.remove(beacon);
        Log.i(TAG, "삭제");

        Intent intent = new Intent(BeaconService.this, RegionEnterActivity.class);
        intent.putExtra(RegionEnterActivity.EXTRA_ENTER_STATE, 0);
        intent.putExtra(RegionEnterActivity.EXTRA_BEACON_UUID, beacon.getId1().toString());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void validateBeaconStatus(Collection<Beacon> beacons) {
        Log.d(TAG, "이전:" + enterBeacons.size() + "//현재:" + beacons.size());

        // 추가된 비콘 처리
        for (Beacon beacon : beacons) {
            if (!enterBeacons.containsKey(beacon)) {
                enterBeacons.put(beacon, deboundThreshold-1);
            } else {
                int deboundCount = enterBeacons.get(beacon);

                if (deboundCount < deboundThreshold) {
                    enterBeacons.put(beacon, ++deboundCount);
                    Log.d(TAG, "증가: 디바운싱 카운트: " + deboundCount);
                } else if (deboundCount == deboundThreshold) {
                    notifyInsertBeacon(beacon);
                }
            }
        }

        // 제거된 비콘 처리
        for (HashMap.Entry<Beacon, Integer> entry : enterBeacons.entrySet()) {
            Beacon beacon = entry.getKey();

            if (!beacons.contains(beacon)) {
                int deboundCount = entry.getValue();

                if (deboundCount > 0) {
                    enterBeacons.put(beacon, --deboundCount);
                    Log.d(TAG, "감소: 디바운싱 카운트: " + deboundCount);
                } else {
                    enterBeacons.remove(beacon);
                    notifyDeleteBeacon(beacon);
                }
            }
        }
    }
}

