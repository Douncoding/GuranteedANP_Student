package com.douncoding.guaranteedanp_s;

import android.support.annotation.NonNull;
import android.util.Log;

import com.douncoding.dao.Instructor;
import com.douncoding.dao.Lesson;
import com.douncoding.dao.LessonDao;
import com.douncoding.dao.LessonTime;
import com.douncoding.dao.Place;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 수업을 관리하는 클래스 (교장)
 * 1. 데이터베이스 동기화 로직 관리
 */
public class PrincipalInteractor {
    public static final String TAG = PrincipalInteractor.class.getSimpleName();

    AppContext mApp;
    WebService mWebService;
    OnListener onListener;

    enum Unit {INSTRUCTOR, PLACE, LESSON, LESSONTIME, ENROLLMENT}
    HashMap<Unit, Boolean> mLoadStatus;

    public PrincipalInteractor(@NonNull AppContext aApp) {
        this.mApp = aApp;
        this.mWebService = mApp.getWebServiceInstance();

        this.mLoadStatus = new HashMap<>();
    }

    public interface OnListener {
        void onLoad(); //
    }

    public void setOnListener(OnListener listener) {
        this.onListener = listener;
    }

    /**
     * 데이터베이스 내려받기
     * 출석정보는 데이터베이스로 관리하지 않으며, 상황에 따라 요청받아 사용한다.
     */
    public void load() {
        mLoadStatus.clear();

        loadLessonRoom();

        loadInsructor();

        loadLesson();

        loadLessonTimes();

        loadEnrollment();
    }

    /**
     * 어플 동작 중에 데이터베이스를 초기화 하는것은 시스템의 치명적인
     * 오류를 발생시킬수 있는 소지가 있기 때문에 다음을 호출하는 것은
     * 매우 주의깊게 사용되야 한다.
     */
    public void sync() {
        Log.w(TAG, "데이터베이스 초기화");
        mApp.openDBWritable().getLessonDao().deleteAll();
        mApp.openDBWritable().getPlaceDao().deleteAll();
        mApp.openDBWritable().getStudentDao().deleteAll();
        mApp.openDBWritable().getInstructorDao().deleteAll();
        mApp.openDBWritable().getLessonTimeDao().deleteAll();
        load();
    }

    public String getNameLessonRoom(long placeId) {
        return mApp.openDBReadable().getPlaceDao().load(placeId).getName();
    }

    /**
     * 비콘 UUID 값을 이용해 강의실번호(식별자)를 구한다.
     */
    public Place getPlace(String uuid) {
        List<Place> placeList = mApp.openDBReadable().getPlaceDao().loadAll();

        for (Place place : placeList) {
            if (place.getUuid().equals(uuid)) {
                return place;
            }
        }
        Log.w(TAG, "알수 없는 UUID:" + uuid);
        return null;
    }


    /**
     * 오늘 강의시간이 할당된 강의목록을 구한다.
     */
    public List<Lesson> getTodayLessonList(List<Lesson> aLessons) {
        List<Lesson> todayLessonList = new ArrayList<>();

        if (aLessons == null) {
            Log.w(TAG, "오늘의 강의목록 추출실패: 매개변수 Null");
            return null;
        }

        for (Lesson lesson : aLessons) {
            for (LessonTime time: lesson.getLessonTimeList()) {
                if (validateDayOfWeekLessonTime(time)) {
                    todayLessonList.add(lesson);
                }
            }
        }
        return todayLessonList;
    }

    /**
     * 자신의 강의목록을 구함
     */
    public List<Lesson> getOwnLessonList() {
        List<Lesson> ownLessons = new ArrayList<>();
        List<Lesson> allLessons = mApp.openDBReadable().getLessonDao().loadAll();

        for (Lesson lesson : allLessons) {
            if (lesson.getEnrollment() != null && lesson.getEnrollment() == 1) {
                ownLessons.add(lesson);
            }
        }
        return ownLessons;
    }

    /**
     * 현재시간이 포함되는 강의시간 번호를 반환한다. 만약 포함되는 강의시간이
     * 없다면, -1을 반환한다.
     */
    public int validateIncludeInLessonTime(long lessonId) {
        Lesson lesson = mApp.openDBReadable().getLessonDao().load(lessonId);

        Calendar cc = Calendar.getInstance();

        for (LessonTime time : lesson.getLessonTimeList()) {
            if (!validateDayOfWeekLessonTime(time)) {
                continue;
            }

            String[] startHourAndMin = time.getStartTime().split(":");
            String[] endHourAndMin = time.getEndTime().split(":");

            // 사이 시간인지 확인
            Calendar startTime = Calendar.getInstance();
            startTime.set(Calendar.HOUR_OF_DAY, Integer.valueOf(startHourAndMin[0]));
            startTime.set(Calendar.MINUTE, Integer.valueOf(startHourAndMin[1]));
            Calendar endTime = Calendar.getInstance();
            endTime.set(Calendar.HOUR_OF_DAY, Integer.valueOf(endHourAndMin[0]));
            endTime.set(Calendar.MINUTE, Integer.valueOf(endHourAndMin[1]));

            // 사이 날짜인지 확인
            if (cc.getTimeInMillis() >= startTime.getTimeInMillis() &&
                    cc.getTimeInMillis() <= endTime.getTimeInMillis()) {
                return time.getId().intValue();
            }
        }
        return -1;
    }

    /**
     * 요청된 강의시간이 오늘에 해당하는지 확인
     */
    private boolean validateDayOfWeekLessonTime(LessonTime time) {
        Calendar currentDate = Calendar.getInstance();
        Calendar startDate = Calendar.getInstance();
        Calendar endDate = Calendar.getInstance();

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
        return true;
    }

    /**
     * 로딩상태를 관리며, 모든 목록(Unit.Size)이 로딩완료된 경우
     * 등록된 콜백함수를 호출한다.
     */
    private synchronized void setLoadStatus(Unit key, boolean state) {
        mLoadStatus.put(key, state);

        for (Map.Entry<Unit, Boolean> entry : mLoadStatus.entrySet()) {
            if (entry.getValue() == null || !entry.getValue()) {
                Log.w(TAG, entry.getKey().name() + " 로딩 실패");
                return;
            }
        }

        if (mLoadStatus.size() == Unit.values().length) {
            Log.i(TAG, "트랜잭션 로딩완료");
            if (onListener != null)
                onListener.onLoad();
        }
    }

    private void loadInsructor() {
        mWebService.loadAllInstructors().enqueue(new Callback<List<Instructor>>() {
            @Override
            public void onResponse(Call<List<Instructor>> call, Response<List<Instructor>> response) {
                List<Instructor> body = response.body();
                if (body != null) {
                    mApp.openDBWritable().getInstructorDao().insertOrReplaceInTx(body);
                    Log.i(TAG, "강사목록 동기화 성공: 항목수:" + body.size());
                }
                setLoadStatus(Unit.INSTRUCTOR, true);
            }

            @Override
            public void onFailure(Call<List<Instructor>> call, Throwable t) {

            }
        });
    }

    public void loadLessonRoom() {
        mWebService.loadAllPlaces().enqueue(new Callback<List<Place>>() {
            @Override
            public void onResponse(Call<List<Place>> call, Response<List<Place>> response) {
                List<Place> body = response.body();
                if (body != null) {
                    mApp.openDBWritable().getPlaceDao().insertOrReplaceInTx(body);
                    Log.i(TAG, "강의실 목록 동기화 성공: 항목수:" + body.size());
                }
                setLoadStatus(Unit.PLACE, true);
            }

            @Override
            public void onFailure(Call<List<Place>> call, Throwable t) {

            }
        });
    }

    private void loadLesson() {
        mWebService.getLessons().enqueue(new Callback<List<Lesson>>() {
            @Override
            public void onResponse(Call<List<Lesson>> call, Response<List<Lesson>> response) {
                List<Lesson> body = response.body();
                if (body != null) {
                    mApp.openDBWritable().getLessonDao().insertOrReplaceInTx(body);
                    Log.i(TAG, "강의 목록 동기화 성공: 항목수:" + body.size());
                }
                setLoadStatus(Unit.LESSON, true);
            }

            @Override
            public void onFailure(Call<List<Lesson>> call, Throwable t) {
                Log.e(TAG, "강사 목록 동기화 실패:" + t.toString());
            }
        });
    }

    private void loadLessonTimes() {
        mWebService.getLessonTimes("all").enqueue(new Callback<List<LessonTime>>() {
            @Override
            public void onResponse(Call<List<LessonTime>> call, Response<List<LessonTime>> response) {
                List<LessonTime> body = response.body();
                if (body != null) {
                    mApp.openDBWritable().getLessonTimeDao().insertOrReplaceInTx(body);
                    Log.i(TAG, "강의시간 목록 동기화 성공: 항목수:" + body.size());
                }
                setLoadStatus(Unit.LESSONTIME, true);
            }

            @Override
            public void onFailure(Call<List<LessonTime>> call, Throwable t) {

            }
        });
    }

    // 수강신청 강의 동기화
    private void loadEnrollment() {
        long ownId = mApp.내정보.얻기().getId();

        mWebService.getEnrollmentLesson((int)ownId).enqueue(new Callback<List<Lesson>>() {
            @Override
            public void onResponse(Call<List<Lesson>> call, Response<List<Lesson>> response) {
                LessonDao dao = mApp.openDBWritable().getLessonDao();

                List<Lesson> body = response.body();
                if (body != null) {
                    for (Lesson lesson : body) {
                        lesson.setEnrollment(Integer.valueOf(1));
                        dao.update(lesson);
                    }
                }
                setLoadStatus(Unit.ENROLLMENT, true);
                Log.i(TAG, "내 강의 목록 동기화 성공: 항목수:" + body.size());
            }

            @Override
            public void onFailure(Call<List<Lesson>> call, Throwable t) {

            }
        });
    }

}
