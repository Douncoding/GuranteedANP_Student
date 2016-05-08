package com.douncoding.guaranteedanp_s;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.douncoding.dao.Lesson;
import com.douncoding.dao.Place;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;

public class BeaconService extends Service implements BeaconConsumer {
    public static final String TAG = BeaconService.class.getSimpleName();
    public static final String ACTION_SERVICE_STOP = "com.douncoding.SERVICE_STOP";

    BeaconManager mBeaconManager;
    BeaconBroadcastReceiver mReceiver;
    PrincipalInteractor mPrincipalInteractor;

    HashMap<Beacon, Integer> enterBeacons = new HashMap();
    HashMap<Beacon, Boolean> addNotifiedBeacons = new HashMap<>();

    /**
     * 비콘의 인식이 안정하지 못한 문제가 있으며, 이를 보완하기 위해 디바운싱의
     * 개념을 사용한다. 일반적인 경우 감지되고 있는 상태중 신호를 잃어 버리는 경우에
     * 대한 예외 처리임에 따라 최소 입려되는 enterBeacons 의 Integer 값은
     * -1 값이다. 이는 1회 감지되자 마자 바로 잃어버리는 경우
     * 와 같은 비정상 경우에 대비할 수 있음을 말한다.
     */
    private static final int DEBOUNCE_SIZE = 10;

    AppContext mApp;

    public BeaconService() { }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();        

        mApp = (AppContext)getApplication();
        mPrincipalInteractor = new PrincipalInteractor(mApp);

        setupBeaconDetector();

        setupBroadcastReceiver();

        setupNotification();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();        
        registerRestartAlarm();
    }

    private void setupBeaconDetector() {
        //new BackgroundPowerSaver(this);
        
        mBeaconManager = BeaconManager.getInstanceForApplication(this);
        mBeaconManager.getBeaconParsers()
                .add(new BeaconParser()
                        .setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
        mBeaconManager.bind(this);
    }
    
    // Foreground 서비스 등록
    private void setupNotification() {
        Notification noti = new NotificationCompat.Builder(this)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setContentText("실행중")
                .setSmallIcon(R.drawable.ic_star_name)
                .build();
        
        startForeground(Constants.SERVICE_FROGROUND_NOTI_ID, noti);
    }
    
    // 브로드케스트 리시버 등록
    private void setupBroadcastReceiver() {
        mReceiver = new BeaconBroadcastReceiver();
    
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter filter = new IntentFilter(Constants.BEACON_BROADCAST_ACTION);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastManager.registerReceiver(mReceiver, filter);
    }
    
    class BeaconBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //최소 1분이 경과한 뒤에 관리대상에서 삭제할 수 있도록함
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    synchronized (this) {
                        addNotifiedBeacons.clear();
                        Log.i(TAG, "초기화 브로드캐스트 수신: 모든 이벤트 활성화");
                    }
                }
            }, 60 * 1000);
        }
    }

    private void moveToEnterActivity(int state, int lessonTimeId) {
        Log.i(TAG, "출석처리 요청: 상태:" + state + " 강의시간번호:" + lessonTimeId);
        Intent intent = new Intent(BeaconService.this, RegionEnterActivity.class);

        intent.setAction(RegionEnterActivity.ACTION_BEACON_EVENT);
        intent.putExtra(RegionEnterActivity.EXTRA_ENTER_STATE, state);
        intent.putExtra(RegionEnterActivity.EXTRA_LESSONTIME_ID, lessonTimeId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP );
        startActivity(intent);
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
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 지역에 진입하는 경우 발생
     * @param beacon 지역 비콘 정보
     */
    private void notifyInsertBeacon(Beacon beacon) {
        if (!addNotifiedBeacons.containsKey(beacon)) {
            // 이벤트가 발생한 경우만 관리
            if (processEvent(beacon.getId1().toString(), RegionEnterActivity.STATE_DATA_ENTER)) {
                Log.i(TAG, "이벤트 발생목록: 추가");
                addNotifiedBeacons.put(beacon, true);
            }
        } else  {
            Log.d(TAG, "이벤트 발생목록: 이미 전송된 정보");
        }
    }

    /**
     * 지역을 이탈하는 경우 발생
     * @param beacon 지역 비콘 정보
     */
    private void notifyDeleteBeacon(Beacon beacon) {
        processEvent(beacon.getId1().toString(), RegionEnterActivity.STATE_DATA_EXIT);

        // 이벤트가 발생하지 않은 경우도 처리
        if (addNotifiedBeacons.remove(beacon) != null)
            Log.i(TAG, "이벤트 발생목록: 삭제");
        else
            Log.w(TAG, "입장 이벤트가 발생했던 이력이 없는 퇴장 이벤트는 무시한다.");
    }

    /**
     * 비콘 인식 안정화
     * @param beacons 지역에서 감지되는 모든 비콘 목록
     */
    private void validateBeaconStatus(Collection<Beacon> beacons) {
        Log.d(TAG, "이전:" + enterBeacons.size() + "//현재:" + beacons.size());

        // 추가된 비콘 처리
        for (Beacon beacon : beacons) {
            if (!enterBeacons.containsKey(beacon)) {
                enterBeacons.put(beacon, DEBOUNCE_SIZE);
            } else {
                Log.d(TAG, "증가: 디바운싱 카운트: " + enterBeacons.get(beacon));
                enterBeacons.put(beacon, DEBOUNCE_SIZE);
                notifyInsertBeacon(beacon);
            }
        }

        // 제거된 비콘 처리
        for (HashMap.Entry<Beacon, Integer> entry : enterBeacons.entrySet()) {
            Beacon beacon = entry.getKey();

            if (!beacons.contains(beacon)) {
                int debounceCount = entry.getValue();

                if (debounceCount > -DEBOUNCE_SIZE) {
                    enterBeacons.put(beacon, --debounceCount);
                    Log.d(TAG, "감소: 디바운싱 카운트: " + debounceCount);
                } else {
                    enterBeacons.remove(beacon);
                    notifyDeleteBeacon(beacon);
                }
            }
        }
    }

    /**
     * 유효한 이벤트인 경우 팝업창 출력을 한다.
     * 강의시간 중 강의실의 출입내역을 팝업창 생성하는 역할을 한다.
     * @param uuid 비콘식별자
     * @return true/false
     */
    private boolean processEvent(String uuid, int state) {
        Place place = mPrincipalInteractor.getPlace(uuid);

        // 내 수강목록
        for (Lesson lesson : mPrincipalInteractor.getOwnLessonList()) {
            // 강의실이 다른 강의는 제외
            if (lesson.getPid() != place.getId())
                continue;

            int timeId = mPrincipalInteractor.validateIncludeInLessonTime(lesson.getId());
            if (timeId != -1) {
                moveToEnterActivity(state, timeId);
                Log.d(TAG, String.format(Locale.getDefault(), "발생위치 강의실:%s 번호:%d",
                        place.getName(), place.getId().intValue()));
                return true;
            }
        }
        return false;
    }

    /**
     * 서비스가 강제종료된 경우 10초 뒤 재시작
     */
    void registerRestartAlarm() {
        Log.w(TAG, "10초 뒤 서비스를 재시작");
        Intent intent = new Intent(this, BeaconService.class);

        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, 0);
        long firstTime = SystemClock.elapsedRealtime();
        firstTime += 10*1000; // 10초 후에 알람이벤트 발생
        AlarmManager am = (AlarmManager)getSystemService(ALARM_SERVICE);
        am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, firstTime, 10*1000, pendingIntent);
    }


}

