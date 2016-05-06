package com.douncoding.guaranteedanp_s;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.douncoding.dao.Lesson;
import com.douncoding.dao.LessonTime;
import com.douncoding.dao.Track;
import com.douncoding.dao.TrackDao;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 출석 처리
 * {@link BeaconService} 에 의해 발샌한 이벤트를 수렴하며, 내부 테이블 정보를 통해
 * 현재 지역의 강의실 또는 강의정보를 추출하여 사용자에게 출석인증을 요구한다.
 */
public class RegionEnterActivity extends Activity {
    public static final String TAG = RegionEnterActivity.class.getSimpleName();

    public static final String ACTION_LESSON_FINISHED = "com.douncoding.guaranteed.ACTION_LESSON_FINISHED";
    public static final String ACTION_BEACON_EVENT = "com.douncoding.guaranteed.ACTION_BEACON_EVENT";
    public static final String ACTION_LESSON_TRACK = "ACTION_LESSON_TRACK";

    public static final String EXTRA_ENTER_TIME = "EXTRA_ENTER_TIME";
    public static final String EXTRA_LESSONTIME_ID = "LESSONTIME_ID";
    public static final String EXTRA_ENTER_STATE = "ENTER_STATE";
    public static final int STATE_DATA_ENTER = 1;
    public static final int STATE_DATA_EXIT = 0;
    public static final int STATE_DATE_UNKNOWN = 2;

    private static final int NOTIFICATION_ID = 1;

    /**
     * 내부자원
     */
    NotificationManager mNotificationManager;
    AppContext mApp;
    WebService mWebService;

    /**
     * UI
     */
    LinearLayout mMainView;
    ImageView mStateImageView;

    TextView mTitleView;
    TextView mContentView;
    TextView mAuthTimeView;
    TextView mStartTimeView;
    TextView mEndTimeView;
    TextView mConfirmView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /**
         * 액티비티 다이얼로그 기본설정
         */
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND, WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH);
        setContentView(R.layout.activity_enter_region);

        mMainView = (LinearLayout)findViewById(R.id.enter_container);
        mStateImageView = (ImageView)findViewById(R.id.icon_state);
        mTitleView = (TextView)findViewById(R.id.title);
        mContentView = (TextView)findViewById(R.id.content);
        mAuthTimeView = (TextView)findViewById(R.id.enter_time);
        mStartTimeView = (TextView)findViewById(R.id.start_time);
        mEndTimeView = (TextView)findViewById(R.id.end_time);

        mConfirmView = (TextView)findViewById(R.id.confirm);
        mConfirmView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        mApp = (AppContext)getApplication();

        mWebService = mApp.getWebServiceInstance();


        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        RingtoneManager.getRingtone(this, sound).play();

        String action = getIntent().getAction();
        if (action.equals(ACTION_LESSON_FINISHED)) {
            Log.i(TAG, "수업종료 인텐트 수신");

            int timeId = getIntent().getIntExtra(EXTRA_LESSONTIME_ID, -1);

            // 비콘 관리목록 초기화
            sendLocalBroadcast();

            Track findTrack = getTrackOfLessonTime(timeId);
            if (findTrack != null) {
                findTrack.setExitTime(new Date());

                // 출석처리
                authAttendance(findTrack, timeId);

                switch (findTrack.getState()) {
                    case STATE_DATA_ENTER: // 출석 또는 지각
                        attendAuthSuccess(timeId);
                        break;
                    case STATE_DATA_EXIT: // 결석
                        attendAuthFailure(timeId);
                        break;
                }
                findTrack.delete();
            } else {
                Log.w(TAG, "트랙번호를 찾을수 없음:");
            }

        } else if (action.equals(ACTION_BEACON_EVENT)){
            int state = getIntent().getIntExtra(EXTRA_ENTER_STATE, 2);
            int timeId = getIntent().getIntExtra(EXTRA_LESSONTIME_ID, -1);

            Log.w(TAG, "ACTION_BEACON_EVENT 수신");

            manageTrack(timeId, state);

            switch (state) {
                case STATE_DATA_ENTER:
                    enterLessonPlace(timeId);
                    break;
                case STATE_DATA_EXIT:
                    exitLessonPlace(timeId);
                    break;
                default:
                    Log.w(TAG, "비콘이벤트 수신: 알 수 없는 상태");
                    break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mNotificationManager = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
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


    /**
     *
     */
    private void authAttendance(Track track, int timeId) {
        int attendanceState;

        LessonTime item = mApp.openDBReadable().getLessonTimeDao().load((long)timeId);
        String[] startHourAndMin = item.getStartTime().split(":");

        Date enterTime = track.getEnterTime();

        // 강의실 입장 시간
        Calendar ec = Calendar.getInstance();
        ec.setTime(enterTime);

        // 강의 시작시간
        Calendar sc = Calendar.getInstance();
        sc.setTime(enterTime);
        sc.set(Calendar.HOUR_OF_DAY, Integer.valueOf(startHourAndMin[0]));
        sc.set(Calendar.MINUTE, Integer.valueOf(startHourAndMin[1]));

        // 출석:1 / 지각:2
        if (ec.getTimeInMillis() > sc.getTimeInMillis()){
            attendanceState = 2;
        } else {
            attendanceState = 1;
        }

        Attendance attendance = new Attendance();
        attendance.setState(attendanceState);
        attendance.setEnterTime(ec.getTime());
        attendance.setExitTime(track.getExitTime());

        mWebService.attend(attendance, (int)item.getLid(), mApp.내정보.얻기().getId().intValue()).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Log.d(TAG, "출석처리 결과:" + response.code());
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {

            }
        });
    }

    /**
     * 관리비콘을 초기화 하라는 메시지를 서비스로 알림
     * 한 강의실에서 반복적인 수업이 있는 경우를 대비함
     */
    private void sendLocalBroadcast() {
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        Intent intent = new Intent();
        intent.setAction(Constants.BROADCAST_ACTION);
        localBroadcastManager.sendBroadcast(intent);
    }


    private Track getTrackOfLessonTime(int lessonTimeId) {
        TrackDao dao = mApp.openDBReadable().getTrackDao();
        for (Track track : dao.loadAll()) {
            if (track.getLessonTimeId() == lessonTimeId) {
                return track;
            }
        }
        return null;
    }

    /**
     * 강의시간 중 학생의 출입상태를 관리하여 출입인증을 위한 처리
     * 인증을 위한 추가 알고리즘이 필요한 경우 여기에 정의
     * - 최초입장
     * - 재입장
     * - 퇴장
     * @param lessonTimeId 강의시간번호
     * @param state 상태
     */
    private void manageTrack(int lessonTimeId, int state) {
        TrackDao dao = mApp.openDBReadable().getTrackDao();

        Track findTrack = getTrackOfLessonTime(lessonTimeId);

        if (findTrack == null) {
            if (state == STATE_DATA_ENTER) {
                Log.i(TAG, "최초입장: 현재시간을 출입시간으로 기록:");
                Track track = new Track();
                track.setLessonTimeId(lessonTimeId);
                track.setState(state);
                track.setEnterTime(new Date());
                dao.insertOrReplace(track);
                registerAlarm((int)track.getLessonTimeId());
            } else {
                Log.w(TAG, "퇴장상태 중 퇴장 이벤트 수신");
            }
        } else {
            findTrack.setState(state);
            switch (state) {
                case STATE_DATA_ENTER:
                    Log.i(TAG, "재입장");
                    registerAlarm((int)findTrack.getLessonTimeId());
                    break;
                case STATE_DATA_EXIT:
                    Log.i(TAG, "퇴장");
                    //unregisterAlarm((int)findTrack.getLessonTimeId());
                    break;
            }
        }
    }

    /**
     * 강의실 입장
     */
    private void enterLessonPlace(int timeId) {
        LessonTime time = mApp.openDBReadable()
                .getLessonTimeDao()
                .load(Long.valueOf(timeId));
        Lesson lesson = time.getLesson();

        // UI 처리
        mTitleView.setText("강의실 입장!");
        mContentView.setText(String.format(Locale.getDefault()
                ,"%s 강의실에 입장하였습니다. 강의종료 시간까지 집중하세요."
                , lesson.getName()));
        mStartTimeView.setText(time.getStartTime());
        mEndTimeView.setText(time.getEndTime());
        mAuthTimeView.setText(new Date().toString());
        mStateImageView.setImageResource(R.drawable.ic_enter_arrow_black_24dp);
        mStateImageView.setBackgroundResource(R.color.colorGreen);
    }

    /**
     * 강의실 퇴장
     */
    private void exitLessonPlace(int timeId) {
        LessonTime time = mApp.openDBReadable()
                .getLessonTimeDao()
                .load(Long.valueOf(timeId));
        Lesson lesson = time.getLesson();

        // UI 처리
        mTitleView.setText("강의실 퇴장!");
        mContentView.setText(String.format(Locale.getDefault()
                ,"%s 강의실에서 퇴장하였습니다. 주의하세요."
                , lesson.getName()));

        mStartTimeView.setText(time.getStartTime());
        mEndTimeView.setText(time.getEndTime());
        mAuthTimeView.setText(new Date().toString());
        mStateImageView.setImageResource(R.drawable.ic_exit_arrow_black_24dp);
        mStateImageView.setBackgroundResource(R.color.colorYellow);
    }

    /**
     * 출석인증
     */
    private void attendAuthSuccess(int timeId) {
        LessonTime time = mApp.openDBReadable()
                .getLessonTimeDao()
                .load(Long.valueOf(timeId));
        Lesson lesson = time.getLesson();

        // UI 처리
        mTitleView.setText("출석!");
        mContentView.setText(String.format(Locale.getDefault()
                ,"%s 출석이 정상 처리 되었습니다."
                , lesson.getName()));

        mStartTimeView.setText(time.getStartTime());
        mEndTimeView.setText(time.getEndTime());
        mAuthTimeView.setText(new Date().toString());
        mStateImageView.setImageResource(R.drawable.ic_circle_check_black_24dp);
        mStateImageView.setBackgroundResource(R.color.colorGreen);
    }

    /**
     * 출석인증 실패
     */
    private void attendAuthFailure(int timeId) {
        LessonTime time = mApp.openDBReadable()
                .getLessonTimeDao()
                .load(Long.valueOf(timeId));
        Lesson lesson = time.getLesson();

        // UI 처리
        mTitleView.setText("결석!");
        mContentView.setText(String.format(Locale.getDefault()
                ,"%s 출석 인증에 실패 하였습니다."
                , lesson.getName()));

        mStartTimeView.setText(time.getStartTime());
        mEndTimeView.setText(time.getEndTime());
        mAuthTimeView.setText(new Date().toString());
        mStateImageView.setImageResource(R.drawable.ic_circle_cancel_black_24dp);
        mStateImageView.setBackgroundResource(R.color.colorRed);
    }

    private void registerAlarm(int lessonTimeId) {
        LessonTime time = mApp.openDBReadable()
                .getLessonTimeDao()
                .load(Long.valueOf(lessonTimeId));

        String[] endHourAndMin = time.getEndTime().split(":");

        Calendar c = Calendar.getInstance();

        Intent intent = new Intent(this, RegionEnterActivity.class);
        intent.setAction(ACTION_LESSON_FINISHED);
        intent.putExtra(EXTRA_LESSONTIME_ID, lessonTimeId);
        intent.putExtra(EXTRA_ENTER_TIME, c.getTime().toString());

        PendingIntent pending = PendingIntent.getActivity(this, lessonTimeId, intent, 0);
        if (pending != null) {
            unregisterAlarm(lessonTimeId);
            Log.i(TAG, "수업종료 알람 재설정:");
        }

        c.set(Calendar.HOUR_OF_DAY, Integer.valueOf(endHourAndMin[0]));
        c.set(Calendar.MINUTE, Integer.valueOf(endHourAndMin[1]));

        /*
        if ((c.getTimeInMillis() < Calendar.getInstance().getTimeInMillis()) {
            Log.e(TAG, String.format(Locale.getDefault(),
                    "알람설정 시간이 현재시간 보다 낮음: 현재:%s 예약:%s",
                    c.getTime(), Calendar.getInstance().getTime()));
            return;
        }
        */

        AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pending);
        Log.i(TAG, "수업종료 알람설정: 예약시간:" + c.getTime());
    }

    private void unregisterAlarm(int lessonTimeId) {
        Intent intent = new Intent();

        PendingIntent pending = PendingIntent.getActivity(this, lessonTimeId, intent, 0);
        if (pending == null) {
            Log.d(TAG, "등록된 알람없음");
        } else {
            Log.d(TAG, "등록된 알람존재");
        }

        AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
        alarmManager.cancel(pending);
        Log.i(TAG, "수업종료 알람해제: 식별자:" + lessonTimeId);
    }
}
