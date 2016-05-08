package com.douncoding.guaranteedanp_s;

import android.util.Log;

import com.douncoding.dao.Lesson;
import com.douncoding.dao.LessonTime;
import com.douncoding.dao.Track;
import com.douncoding.dao.TrackDao;
import com.google.gson.Gson;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 출석인증과 관련된 로직담당 클래스 (뷰와 로직을 구분하기 위함)
 */
public class TrackInteractor {
    public static final String TAG = TrackInteractor.class.getSimpleName();

    AppContext mApp;
    WebService mWebService;

    LessonTime mLessonTime;

    public TrackInteractor(AppContext aApp, long timeId) {
        this.mApp = aApp;
        this.mWebService = mApp.getWebServiceInstance();

        mLessonTime = mApp.openDBReadable().getLessonTimeDao().load(timeId);
    }

    public LessonTime getLessonTime() {
        return mLessonTime;
    }

    public Lesson getLesson() {
        return mLessonTime.getLesson();
    }

    /**
     * 트랙정보 얻기
     */
    private Track getTrackOfLessonTime(int timeId) {
        TrackDao dao = mApp.openDBReadable().getTrackDao();
        for (Track track : dao.loadAll()) {
            if (track.getLessonTimeId() == timeId) {
                return track;
            }
        }
        return null;
    }

    /**
     * 출석상태를 서버로 전송한다.
     */
    public AttendState sendResult() {
        AttendState state;
        Track track = getTrackOfLessonTime(mLessonTime.getId().intValue());

        if (track == null) {
            Log.w(TAG, "추적할 수 없는 강의시간 정보: 강의시간번호:" + mLessonTime.getId());
            return AttendState.UNKNOWN;
        }

        // 강의실 입장 시간
        Calendar ec = Calendar.getInstance();
        ec.setTime(track.getEnterTime());

        // 강의 시작시간
        String[] startHourAndMin = mLessonTime.getStartTime().split(":");
        Calendar sc = Calendar.getInstance();
        sc.setTime(mLessonTime.getStartDate());
        sc.set(Calendar.HOUR_OF_DAY, Integer.valueOf(startHourAndMin[0]));
        sc.set(Calendar.MINUTE, Integer.valueOf(startHourAndMin[1]));

        // 출석상태 분별
        if (track.getState() == RegionEnterActivity.STATE_DATA_ENTER) {
            if (ec.getTimeInMillis() > sc.getTimeInMillis()) {
                state = AttendState.LATE;
            } else {
                state = AttendState.ATTEND;
            }
        } else {
            state = AttendState.ABSENT;
        }

        // 예외 처리가 없기 때문에 다음동작을 위해 삭제한다.
        track.delete();

        // 전송용 데이터 포멧구성
        Attendance attendance = new Attendance();
        attendance.setEnterTime(ec.getTime());
        attendance.setExitTime(new Date());
        attendance.setState(state.getValue());
        attendance.setLtid(mLessonTime.getId().intValue());

        Log.i(TAG, "출석인증 최종결과: "+ new Gson().toJson(attendance));

        int lessonId = (int)mLessonTime.getLid();
        int studentId = mApp.내정보.얻기().getId().intValue();
        mWebService.attend(attendance, lessonId, studentId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Log.d(TAG, "출석처리 결과:" + response.code());
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {

            }
        });

        return state;
    }

    public void tracking(int state) {
        TrackDao trackDao = mApp.openDBWritable().getTrackDao();
        Track track = getTrackOfLessonTime(mLessonTime.getId().intValue());

        if (track == null) {
            if (state == RegionEnterActivity.STATE_DATA_ENTER) {
                Log.i(TAG, "최초입장: 현재시간을 출입시간기록:");

                Track t = new Track();
                t.setLessonTimeId(mLessonTime.getId());
                t.setState(state);
                t.setEnterTime(new Date());

                trackDao.insertOrReplace(t);
            } else {
                Log.w(TAG, "퇴장상태 중 퇴장 이벤트 수신");
            }
        } else {
            Log.i(TAG, String.format(Locale.getDefault(), "강의명:%s 강의시간:%d 상태변경->%d",
                    mLessonTime.getLesson().getName(), mLessonTime.getId(), state));
            track.setState(state);
            track.update();
        }
    }

    public enum AttendState {
        ATTEND(1), LATE(2), ABSENT(3), UNKNOWN(4);
        int value;
        AttendState(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
}
