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
import android.support.v4.content.WakefulBroadcastReceiver;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.douncoding.dao.Lesson;
import com.douncoding.dao.LessonTime;
import com.douncoding.dao.Place;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

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

    AppContext mApp;

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

        mApp = (AppContext)getApplication();

        // Foreground 서비스 등록
        Notification noti = new NotificationCompat.Builder(this)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setContentText("실행중")
                .setSmallIcon(R.drawable.ic_star_name)
                .build();
        startForeground(2, noti);

        // 브로드케스트 리시버 등록
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter filter = new IntentFilter(Constants.BROADCAST_ACTION);
        filter.addCategory(Intent.CATEGORY_DEFAULT);

        broadcastManager.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                /**
                 * 방법없다.. 설계를 잘못했다.. 모든 목록 클리어 간다..
                 */
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
        }, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");

        enterBeacons = null;
        mBeaconManager.unbind(this);

        // 종료시 재시작
        registerRestartAlarm();
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
        if (addNotifiedBeacons.remove(beacon))
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

                if (deboundCount > -deboundThreshold) {
                    enterBeacons.put(beacon, --deboundCount);
                    Log.d(TAG, "감소: 디바운싱 카운트: " + deboundCount);
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
        int pid = getPlaceIdentify(uuid);

        if (pid < 0) {
            return false;
        }

        for (Lesson lesson : getOwnLessonList()) {

            // 강의실이 다른 강의는 제외
            if (lesson.getPid() != pid)
                continue;

            // 해당 강의의 강의시간 목록
            for (LessonTime time : lesson.getLessonTimeList()) {
                // 이벤트 발생!
                if (isValidateLessonTime(time)) {
                    Log.i(TAG, String.format(Locale.getDefault(), "%s 시작시간:%s 종료시간:%s",
                            lesson.getName(), time.getStartTime(), time.getEndTime()));

                    Log.w(TAG, "RegionEnterActivity Call!");
                    Intent intent = new Intent(BeaconService.this, RegionEnterActivity.class);
                    intent.setAction(RegionEnterActivity.ACTION_BEACON_EVENT);
                    intent.putExtra(RegionEnterActivity.EXTRA_ENTER_STATE, state);
                    intent.putExtra(RegionEnterActivity.EXTRA_LESSONTIME_ID, time.getId().intValue());
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP );
                    startActivity(intent);
                    return true;
                }
            }
        }

        Log.d(TAG, "이벤트는 발생했지만, 수업시간이 아닌 시간에 입장");
        return false;
    }


    /**
     * 비콘 UUID 값을 이용해 강의실번호(식별자)를 구한다.
     * @param uuid 비콘 UUID
     * @return 강의실 번호
     */
    private int getPlaceIdentify(String uuid) {
        List<Place> placeList = mApp.openDBReadable().getPlaceDao().loadAll();

        for (Place place : placeList) {
            if (place.getUuid().equals(uuid)) {
                Log.d(TAG, String.format(Locale.getDefault(), "발생위치 강의실:%s 번호:%d",
                        place.getName(), place.getId().intValue()));
                return place.getId().intValue();
            }
        }

        Log.w(TAG, "알수 없는 UUID:" + uuid);
        return -1;
    }

    /**
     * @return 내 수강 목록
     */
    private List<Lesson> getOwnLessonList() {
        List<Lesson> lessons = new ArrayList<>();

        for (Lesson item : mApp.openDBReadable().getLessonDao().loadAll()) {
            if (item.getEnrollment() != null && item.getEnrollment() == 1) {
                lessons.add(item);
                //Log.i(TAG, "내 수강신청 목록:" + item.getName());
            }
        }

        if (lessons.size() == 0) {
            Log.w(TAG, "내 수강신청 목록 없음:");
        }

        return lessons;
    }

    /**
     * 입력된 강의시간이 오늘날짜에 포함되는지 확인한다.
     * 강의시작 시간 10분전부터 관리대상
     * @param time 확인을 원하는 강의시간
     * @return 포함여부
     */
    private boolean isValidateLessonTime(LessonTime time) {
        Calendar currentDate = Calendar.getInstance();
        Calendar startDate = Calendar.getInstance();
        Calendar endDate = Calendar.getInstance();

        String[] startHourAndMin = time.getStartTime().split(":");
        String[] endHourAndMin = time.getEndTime().split(":");

        startDate.setTime(time.getStartDate());
        endDate.setTime(time.getEndDate());

        // 사이 날짜인지 확인
        if (currentDate.getTimeInMillis() < startDate.getTimeInMillis() ||
                currentDate.getTimeInMillis() > endDate.getTimeInMillis()) {
            Log.v(TAG, "포함되지 않는 날짜: " +
                    String.format(Locale.getDefault(), "시작:%d 현재:%d 종료:%d",
                            startDate.getTimeInMillis(),
                            currentDate.getTimeInMillis(),
                            endDate.getTimeInMillis()));
            return false;
        }

        // 요일이 같은지 확인
        if (currentDate.get(Calendar.DAY_OF_WEEK) != time.getDay()) {
            Log.v(TAG, "현재 요일:" + currentDate.get(Calendar.DAY_OF_WEEK) +
                    " 과목 요일:" + time.getDay());
            return false;
        }

        // 사이 시간인지 확인
        Calendar startTime = Calendar.getInstance();
        startTime.set(Calendar.HOUR_OF_DAY, Integer.valueOf(startHourAndMin[0]));
        startTime.set(Calendar.MINUTE, Integer.valueOf(startHourAndMin[1]));
        Calendar endTime = Calendar.getInstance();
        endTime.set(Calendar.HOUR_OF_DAY, Integer.valueOf(endHourAndMin[0]));
        endTime.set(Calendar.MINUTE, Integer.valueOf(endHourAndMin[1]));

        // 사이 날짜인지 확인
        if (currentDate.getTimeInMillis() < startTime.getTimeInMillis() ||
                currentDate.getTimeInMillis() > endTime.getTimeInMillis()) {
            Log.v(TAG, "포함되지 않는 시간: " +
                    String.format(Locale.getDefault(), "시작:%s 현재:%s 종료:%s",
                            startDate.getTime(),
                            currentDate.getTime(),
                            endDate.getTime()));
            return false;
        }

        // 시간확인
        Log.i(TAG, "포함시간: " +
                String.format(Locale.getDefault(), "시작:%d 현재:%d 종료:%d",
                        startDate.getTimeInMillis(),
                        currentDate.getTimeInMillis(),
                        endDate.getTimeInMillis()));
        return true;
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

