package com.douncoding.guaranteedanp_s;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.douncoding.dao.Lesson;
import com.douncoding.dao.LessonTime;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * 출석 처리
 * {@link BeaconService} 에 의해 발샌한 이벤트를 수렴하며, 내부 테이블 정보를 통해
 * 현재 지역의 강의실 또는 강의정보를 추출하여 사용자에게 출석인증을 요구한다.
 */
public class RegionEnterActivity extends Activity {
    public static final String TAG = RegionEnterActivity.class.getSimpleName();

    public static final String ACTION_LESSON_FINISHED = "com.douncoding.guaranteed.ACTION_LESSON_FINISHED";
    public static final String ACTION_BEACON_EVENT = "com.douncoding.guaranteed.ACTION_BEACON_EVENT";
    public static final String ACTION_BEACON_EVENT_TEST = "com.douncoding.guaranteed.ACTION_BEACON_EVENT_TEST";

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
    TrackInteractor mTrackInteractor;
    BroadcastSender mBroadcastSender;

    /**
     * 공통변수
     */
    LessonTime mLessonTime;
    Lesson mLesson;

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

        // 기본자원 설정
        mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        mApp = (AppContext)getApplication();
        mBroadcastSender = new BroadcastSender(this);

        // 소리
        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        RingtoneManager.getRingtone(this, sound).play();

        // 주요 로직 분기점
        handleIntentAction(getIntent().getAction());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mNotificationManager = null;
    }

    private void handleIntentAction(String action) {
        if (action == null) {
            Log.w(TAG, "인텐트 액션 미설정 상태:");
            return ;
        }

        int timeId = getIntent().getIntExtra(EXTRA_LESSONTIME_ID, -1);
        int state = getIntent().getIntExtra(EXTRA_ENTER_STATE, 2);

        mTrackInteractor = new TrackInteractor(mApp, timeId);
        mLessonTime = mTrackInteractor.getLessonTime();
        mLesson = mTrackInteractor.getLesson();

        Log.e(TAG, "CHECK: " + action);
        if (action.equals(ACTION_LESSON_FINISHED)) {
            Log.i(TAG, "수업종료 인텐트 수신");
            handleLessonFinished();
        } else if (action.equals(ACTION_BEACON_EVENT)){
            Log.w(TAG, "비콘 상태변화 이벤트 수신");
            handleReceivedBeaconEvent(timeId, state);
        } else if (action.equals(ACTION_BEACON_EVENT_TEST)) {
            Log.w(TAG, "테스트 코드: 강제퇴장 실행");
            handleForceLessonFinished();
        }
    }

    /**
     * 강의종료 이벤트 수신 (테스트코드에 의해 호출)
     */
    private void handleForceLessonFinished() {
        mBroadcastSender.sendBeaconBroadcast();

        unregisterAlarm(mLessonTime.getId().intValue());

        switch (mTrackInteractor.sendResult()) {
            case ATTEND: case LATE:
                showSuccessDialog();
                break;
            case ABSENT: case UNKNOWN:
                showFailureDialog();
                break;
        }
    }

    /**
     * 강의종료 이벤트 수신 (타이머에 의해 호출)
     */
    private void handleLessonFinished() {
        mBroadcastSender.sendBeaconBroadcast();

        switch (mTrackInteractor.sendResult()) {
            case ATTEND: case LATE:
                showSuccessDialog();
                break;
            case ABSENT: case UNKNOWN:
                showFailureDialog();
                break;
        }
    }

    /**
     * 비콘 상태변화 이벤트 수신
     */
    private void handleReceivedBeaconEvent(int timeId, int state) {
        mTrackInteractor.tracking(state);

        switch (state) {
            case STATE_DATA_ENTER:
                showEnterLessonRoom();
                registerAlarm();
                break;
            case STATE_DATA_EXIT:
                showLeaveLessonRoom();
                break;
        }
    }
    /**
     * 강의실 입장
     */
    private void showEnterLessonRoom() {
        mTitleView.setText("강의실 입장!");
        mContentView.setText(String.format(Locale.getDefault()
                ,"%s 강의실에 입장하였습니다. 강의 종료시간까지 집중하세요.",mLesson.getName()));
        mStartTimeView.setText(mLessonTime.getStartTime());
        mEndTimeView.setText(mLessonTime.getEndTime());
        mAuthTimeView.setText(new Date().toString());
        mStateImageView.setImageResource(R.drawable.ic_enter_arrow_black_24dp);
        mStateImageView.setBackgroundResource(R.color.colorGreen);
    }

    /**
     * 강의실 퇴장
     */
    private void showLeaveLessonRoom() {
        mTitleView.setText("강의실 퇴장!");
        mContentView.setText(String.format(Locale.getDefault()
                ,"%s 강의실에서 퇴장하였습니다. 주의하세요.", mLesson.getName()));

        mStartTimeView.setText(mLessonTime.getStartTime());
        mEndTimeView.setText(mLessonTime.getEndTime());
        mAuthTimeView.setText(new Date().toString());
        mStateImageView.setImageResource(R.drawable.ic_exit_arrow_black_24dp);
        mStateImageView.setBackgroundResource(R.color.colorYellow);
    }

    /**
     * 출석인증
     */
    private void showSuccessDialog() {
        // UI 처리
        mTitleView.setText("출석!");
        mContentView.setText(String.format(Locale.getDefault()
                ,"%s 출석이 정상 처리 되었습니다.", mLesson.getName()));

        mStartTimeView.setText(mLessonTime.getStartTime());
        mEndTimeView.setText(mLessonTime.getEndTime());
        mAuthTimeView.setText(new Date().toString());
        mStateImageView.setImageResource(R.drawable.ic_circle_check_black_24dp);
        mStateImageView.setBackgroundResource(R.color.colorGreen);
    }

    /**
     * 출석인증 실패
     */
    private void showFailureDialog() {
        mTitleView.setText("결석!");
        mContentView.setText(String.format(Locale.getDefault()
                ,"%s 출석 인증에 실패 하였습니다.", mLesson.getName()));

        mStartTimeView.setText(mLessonTime.getStartTime());
        mEndTimeView.setText(mLessonTime.getEndTime());
        mAuthTimeView.setText(new Date().toString());
        mStateImageView.setImageResource(R.drawable.ic_circle_cancel_black_24dp);
        mStateImageView.setBackgroundResource(R.color.colorRed);
    }

    private void registerAlarm() {
        String[] endHourAndMin = mLessonTime.getEndTime().split(":");

        Calendar c = Calendar.getInstance();

        Intent intent = new Intent(this, RegionEnterActivity.class);
        intent.setAction(ACTION_LESSON_FINISHED);
        intent.putExtra(EXTRA_LESSONTIME_ID, mLessonTime.getId().intValue());
        intent.putExtra(EXTRA_ENTER_TIME, c.getTime().toString());

        PendingIntent pending = PendingIntent.getActivity(this, mLessonTime.getId().intValue(), intent, 0);
        if (pending != null) {
            unregisterAlarm(mLessonTime.getId().intValue());
            Log.i(TAG, "수업종료 알람 재설정:");
        }

        c.set(Calendar.HOUR_OF_DAY, Integer.valueOf(endHourAndMin[0]));
        c.set(Calendar.MINUTE, Integer.valueOf(endHourAndMin[1]));

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
