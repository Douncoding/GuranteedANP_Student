package com.douncoding.guaranteedanp_s;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.alamkanak.weekview.MonthLoader;
import com.alamkanak.weekview.WeekView;
import com.alamkanak.weekview.WeekViewEvent;
import com.douncoding.dao.Lesson;
import com.douncoding.dao.LessonDao;
import com.douncoding.dao.LessonTime;
import com.douncoding.dao.LessonTimeDao;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class CoursesFragment extends Fragment implements
        MonthLoader.MonthChangeListener {
    public static final String TAG = CoursesFragment.class.getSimpleName();

    /**
     * 내부 자원 및 통신 자원 할당
     */
    AppContext mApp;
    WebService mWebService;

    /**
     * UI
     */
    WeekView mWeekView;

    public static CoursesFragment newInstance() {
        CoursesFragment fragment = new CoursesFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mApp = (AppContext)context.getApplicationContext();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_day_courses, container, false);

        mWeekView = (WeekView)view.findViewById(R.id.weekView);
        mWeekView.setMonthChangeListener(this);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mWeekView = null;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Constants.HOST)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        mWebService = retrofit.create(WebService.class);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mWebService = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        mWeekView.notifyDatasetChanged();
    }

    /**
     * 내부 테이블에 저장된 강의시간을 통해 오늘의 강의 목록을 추출하여 현시한다.
     * 본래 다음의 리스너는 월이 변경되는 시점에서 출력됨에 따라 초기 설정이 월단위로
     * 설정해야 올바르게 동작하지만, 현 시점에서는 당일 기준의 처리만 진행한다.
     * @param newYear 선택된 년
     * @param newMonth 선택된 월
     * @return 시간목록
     */
    @Override
    public List<? extends WeekViewEvent> onMonthChange(int newYear, int newMonth) {
        List<WeekViewEvent> events = new ArrayList<>();

        List<Lesson> lessons = mApp.openDBReadable().getLessonDao().loadAll();

        // 내 수강목록 읽기
        for (Lesson lesson : lessons) {
            if (lesson.getEnrollment() == null || lesson.getEnrollment() != 1)
                continue;

            List<LessonTime> times = lesson.getLessonTimeList();
            for (LessonTime time : times) {
                // 오늘일자 포함되는 과목만 불러오기
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
                    continue;
                }

                // 요일 확인
                if (currentDate.get(Calendar.DAY_OF_WEEK) != time.getDay()) {
                    Log.v(TAG, "현재 요일:" + currentDate.get(Calendar.DAY_OF_WEEK) +
                        " 과목 요일:" + time.getDay());
                    continue;
                }

                // 시간표작성
                String[] startHourAndMin = time.getStartTime().split(":");
                String[] endHourAndMin = time.getEndTime().split(":");

                Calendar startTime = Calendar.getInstance();
                startTime.set(Calendar.HOUR_OF_DAY, Integer.valueOf(startHourAndMin[0]));
                startTime.set(Calendar.MINUTE, Integer.valueOf(startHourAndMin[0]));
                startTime.set(Calendar.MONTH, newMonth-1);
                startTime.set(Calendar.YEAR, newYear);

                Calendar endTime = Calendar.getInstance();
                endTime.set(Calendar.HOUR_OF_DAY, Integer.valueOf(endHourAndMin[0]));
                endTime.set(Calendar.MINUTE, Integer.valueOf(endHourAndMin[0]));
                endTime.set(Calendar.MONTH, newMonth-1);
                endTime.set(Calendar.YEAR, newYear);

                WeekViewEvent ev = new WeekViewEvent(1, lesson.getName(), startTime, endTime);
                events.add(ev);
            }
        }

        return events;
    }
}
