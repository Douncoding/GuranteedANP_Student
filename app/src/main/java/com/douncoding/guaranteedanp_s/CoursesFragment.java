package com.douncoding.guaranteedanp_s;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.alamkanak.weekview.MonthLoader;
import com.alamkanak.weekview.WeekView;
import com.alamkanak.weekview.WeekViewEvent;
import com.douncoding.dao.Lesson;
import com.douncoding.dao.LessonTime;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CoursesFragment extends Fragment implements
        MonthLoader.MonthChangeListener {
    public static final String TAG = CoursesFragment.class.getSimpleName();

    /**
     * 내부 자원 및 통신 자원 할당
     */
    AppContext mApp;
    PrincipalInteractor mPrincipalInteractor;

    /**
     * UI
     */
    WeekView mWeekView;

    /**
     * Hide Options
     */
    int hideCount = 0;
    BroadcastSender mBroadcastSender;

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
        mPrincipalInteractor = new PrincipalInteractor(mApp);
        mBroadcastSender = new BroadcastSender(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mApp = null;
        mPrincipalInteractor = null;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_day_courses, container, false);

        mWeekView = (WeekView)view.findViewById(R.id.weekView);
        mWeekView.setMonthChangeListener(this);

        mWeekView.setShowNowLine(true);
        mWeekView.setNowLineColor(Color.BLUE);

        mWeekView.setOnEventClickListener(new WeekView.EventClickListener() {
            @Override
            public void onEventClick(WeekViewEvent event, RectF eventRect) {
                if (hideCount++ > 5) {
                    Toast.makeText(getContext()
                            , "테스트코드 실행" + event.getEndTime().getTime()
                            , Toast.LENGTH_SHORT).show();
                    moveToEnterActivity(RegionEnterActivity.STATE_DATA_EXIT, (int)event.getId());
                    hideCount = 0;
                }

                Log.i(TAG, "테스트코드: " + hideCount);
            }
        });

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mWeekView = null;
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

        for (Lesson lesson : mPrincipalInteractor.getOwnLessonList()) {
            List<LessonTime> times = lesson.getLessonTimeList();

            // 등록된 강의시간이 없는 경우 제외
            if (times == null || times.size() == 0)
                continue;

            Calendar sc = Calendar.getInstance();
            Calendar ec = Calendar.getInstance();
            sc.setTime(times.get(0).getStartDate());
            ec.setTime(times.get(0).getEndDate());

            // 월이 다른 경우 제외
            if (sc.get(Calendar.MONTH) != newMonth-1)
                continue;

            // 시작일 부터 종료일 까지
            while (sc.getTimeInMillis() < ec.getTimeInMillis()) {
                sc.add(Calendar.DATE, 1);

                for (LessonTime day : times) {
                    if (sc.get(Calendar.DAY_OF_WEEK) == day.getDay()) {
                        String[] startHourAndMin = day.getStartTime().split(":");
                        String[] endHourAndMin = day.getEndTime().split(":");

                        Calendar startTime = Calendar.getInstance();
                        startTime.set(Calendar.HOUR_OF_DAY, Integer.valueOf(startHourAndMin[0]));
                        startTime.set(Calendar.MINUTE, Integer.valueOf(startHourAndMin[1]));
                        startTime.set(Calendar.DATE, sc.get(Calendar.DATE));
                        startTime.set(Calendar.MONTH, newMonth-1);
                        startTime.set(Calendar.YEAR, newYear);

                        Calendar endTime = Calendar.getInstance();
                        endTime.set(Calendar.HOUR_OF_DAY, Integer.valueOf(endHourAndMin[0]));
                        endTime.set(Calendar.MINUTE, Integer.valueOf(endHourAndMin[1]));
                        endTime.set(Calendar.DATE, sc.get(Calendar.DATE));
                        endTime.set(Calendar.MONTH, newMonth-1);
                        endTime.set(Calendar.YEAR, newYear);
                        WeekViewEvent ev = new WeekViewEvent(day.getId(), lesson.getName(), startTime, endTime);
                        ev.setColor(getResources().getColor(R.color.colorPrimaryLight));
                        events.add(ev);
                    }
                }
            }
        }

        return events;
    }

    private void moveToEnterActivity(int state, int lessonTimeId) {
        Log.i(TAG, "출석처리 요청: 상태:" + state + " 강의시간번호:" + lessonTimeId);
        Intent intent = new Intent(getContext(), RegionEnterActivity.class);
        intent.setAction(RegionEnterActivity.ACTION_BEACON_EVENT_TEST);
        intent.putExtra(RegionEnterActivity.EXTRA_ENTER_STATE, state);
        intent.putExtra(RegionEnterActivity.EXTRA_LESSONTIME_ID, lessonTimeId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP );
        startActivity(intent);
    }
}
