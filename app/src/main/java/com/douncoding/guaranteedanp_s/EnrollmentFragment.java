package com.douncoding.guaranteedanp_s;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.douncoding.dao.Instructor;
import com.douncoding.dao.InstructorDao;
import com.douncoding.dao.Lesson;
import com.douncoding.dao.LessonDao;
import com.douncoding.dao.LessonTime;
import com.douncoding.dao.LessonTimeDao;
import com.douncoding.dao.PlaceDao;

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * 수강신청이 가능한 강의목록을 나열하고 사용자가 수강신청
 * 동작을 수행할 수 있도록 하는 인터페이스 제공
 *
 */
public class EnrollmentFragment extends Fragment {
    public static final String TAG = EnrollmentFragment.class.getSimpleName();

    /**
     * UI
     */
    RecyclerView mRecyclerView;
    ClassListAdapter mAdapter;

    /**
     * 강의목록의 데이터
     */
    InstructorDao mInstructorsDao;
    PlaceDao mPlaceDao;

    /**
     * 내부자원
     */
    AppContext mApp;
    WebService mWebService;

    public static EnrollmentFragment newInstance() {
        EnrollmentFragment fragment = new EnrollmentFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public interface OnListener {
        void onEnrollment(int lessonId);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mApp = (AppContext)activity.getApplicationContext();
        if (mApp == null) {
            throw new RuntimeException("AppContext is not reference");
        }

        /**
         * 로컬 데이터베이스 정보 로딩
         */
        mInstructorsDao = mApp.openDBReadable().getInstructorDao();
        mPlaceDao = mApp.openDBReadable().getPlaceDao();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_enrollment, container, false);

        mAdapter = new ClassListAdapter();
        mRecyclerView = (RecyclerView)view.findViewById(R.id.lesson_list_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.setAdapter(mAdapter);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mRecyclerView = null;
        mAdapter = null;
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
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    class ClassListAdapter extends RecyclerView.Adapter<ClassListAdapter.ViewHolder> {
        List<Lesson> mLessons = mApp.openDBReadable().getLessonDao().loadAll();
        private int expandedPosition = -1;

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.cardviewe_enrollment, parent, false);

            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Lesson item = mLessons.get(position);

            holder.mNameText.setText(item.getName());
            holder.mDescText.setText(item.getDesc());
            holder.mPersonnelText.setText(String.valueOf(item.getPersonnel()));

            if (mInstructorsDao != null) {
                Instructor instructor = mInstructorsDao.load(item.getIid());

                if (instructor != null) {
                    holder.mLNameText.setText(instructor.getName());
                    holder.mLJobsText.setText(instructor.getJobs());
                } else {
                    Log.w(TAG, "강사번호를 찾을수 없음: 강사번호:" + item.getIid());
                }
            }

            if (position == expandedPosition) {
                holder.mExpandedView.setVisibility(View.VISIBLE);
                holder.mPlaceText.setText(mPlaceDao.load(item.getPid()).getName());
                holder.mPersonnelText.setText(String.valueOf(item.getPersonnel()));

                if (item.getLessonTimeList() != null && item.getLessonTimeList().size() > 0) {
                    SimpleDateFormat format = new SimpleDateFormat("yy년 MM월 dd일 EEE", Locale.KOREA);
                    holder.mStartDateText.setText(format.format(item.getLessonTimeList().get(0).getStartDate()));
                    holder.mEndDateText.setText(format.format(item.getLessonTimeList().get(0).getEndDate()));

                    holder.mTimeList.setLayoutManager(new LinearLayoutManager(getContext()));
                    LessonTimeAdapter timeAdapter = new LessonTimeAdapter();
                    timeAdapter.addItem(item.getLessonTimeList());
                    holder.mTimeList.setAdapter(timeAdapter);
                }
            } else {
                holder.mExpandedView.setVisibility(View.GONE);
            }

            if (item.getEnrollment() == null || item.getEnrollment() != 1) {
                holder.mCancelAction.setVisibility(View.GONE);
            } else {
                holder.mCancelAction.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public int getItemCount() {
            return mLessons.size();
        }

        @Override
        public int getItemViewType(int position) {
            return super.getItemViewType(position);
        }

        public void reload() {
            mLessons = mApp.openDBReadable().getLessonDao().loadAll();
            notifyDataSetChanged();
        }

        public class ViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

            TextView mNameText;
            TextView mDescText;

            TextView mLNameText;
            TextView mLJobsText;

            TextView mExpandedAction;
            TextView mEnrollmentAction;
            TextView mCancelAction;

            LinearLayout mExpandedView;
            EditText mPlaceText;
            EditText mPersonnelText;
            EditText mStartDateText;
            EditText mEndDateText;
            RecyclerView mTimeList;

            public ViewHolder(View itemView) {
                super(itemView);

                mNameText = (TextView)itemView.findViewById(R.id.enrollment_name);
                mDescText = (TextView)itemView.findViewById(R.id.enrollment_desc);

                mLNameText = (TextView)itemView.findViewById(R.id.enrollment_instructor_name);
                mLJobsText = (TextView)itemView.findViewById(R.id.enrollment_instructor_jobs);

                mExpandedView = (LinearLayout)itemView.findViewById(R.id.container_expanded);
                mExpandedAction = (TextView)itemView.findViewById(R.id.enrollment_expand_action);
                mEnrollmentAction = (TextView)itemView.findViewById(R.id.enrollment_post_action);
                mCancelAction = (TextView)itemView.findViewById(R.id.enrollment_cancel_action);

                mExpandedAction.setOnClickListener(this);
                mEnrollmentAction.setOnClickListener(this);
                mCancelAction.setOnClickListener(this);

                mPlaceText = (EditText)itemView.findViewById(R.id.place_text);
                mPersonnelText = (EditText)itemView.findViewById(R.id.personnel_text);
                mStartDateText = (EditText)itemView.findViewById(R.id.start_date_text);
                mEndDateText = (EditText)itemView.findViewById(R.id.end_date_text);
                mTimeList = (RecyclerView)itemView.findViewById(R.id.time_list);
            }

            @Override
            public void onClick(View v) {
                final int lid = mLessons.get(getPosition()).getId().intValue();
                final int sid = mApp.내정보.얻기().getId().intValue();

                switch (v.getId()) {
                    case R.id.enrollment_expand_action:

                        if (expandedPosition >= 0) {
                            notifyItemChanged(expandedPosition);
                        }

                        expandedPosition = getPosition();
                        notifyItemChanged(expandedPosition);
                        break;
                    case R.id.enrollment_post_action:
                        mWebService.enrollment(lid, sid).enqueue(new Callback<ResponseBody>() {
                            @Override
                            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                if (response.code() == 200) {
                                    LessonDao dao = mApp.openDBReadable().getLessonDao();
                                    Lesson lesson = dao.load((long)lid);
                                    lesson.setEnrollment(1);
                                    dao.update(lesson);

                                    mAdapter.reload();

                                    updateLessonTimes(lid);
                                    // 수강신청 성공
                                    Toast.makeText(getContext(),
                                            mLessons.get(getPosition()).getName() + " 수강신청 완료",
                                            Toast.LENGTH_SHORT).show();
                                } else {
                                    // 수강신청 실패
                                    Toast.makeText(getContext(),
                                            mLessons.get(getPosition()).getName() + " 수강신청 실패",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onFailure(Call<ResponseBody> call, Throwable t) {

                            }
                        });
                        break;
                    case R.id.enrollment_cancel_action:
                        mWebService.dropCourse(lid, sid).enqueue(new Callback<ResponseBody>() {
                            @Override
                            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                LessonDao dao = mApp.openDBReadable().getLessonDao();
                                Lesson lesson = dao.load((long)lid);
                                lesson.setEnrollment(0);
                                dao.update(lesson);

                                mAdapter.reload();

                                Toast.makeText(getContext(),
                                        mLessons.get(getPosition()).getName() + " 수강취소 완료",
                                        Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onFailure(Call<ResponseBody> call, Throwable t) {
                                Toast.makeText(getContext(),
                                        mLessons.get(getPosition()).getName() + " 수강취소 실패",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                        break;
                }
            }
        }
    }

    class LessonTimeAdapter extends RecyclerView.Adapter<LessonTimeAdapter.ViewHolder> {

        ArrayList<LessonTime> dataset = new ArrayList<>();

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_list_lesson_time, parent, false);

            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            LessonTime item = dataset.get(position);

            holder.mWeekText.setText(CommonUtils.dayOfString(item.getDay()));
            holder.mStartTimeText.setText(item.getStartTime());
            holder.mEndTimeText.setText(item.getEndTime());
        }

        @Override
        public int getItemCount() {
            return dataset.size();
        }

        public boolean addItem(List<LessonTime> items) {
            if (items == null) {
                Log.w(TAG, "갱신하고자 하는 항목이 NULL 상태: ");
                return false;
            }

            dataset.addAll(items);
            notifyDataSetChanged();
            return true;
        }

        public List<LessonTime> get() {
            return dataset;
        }

        public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            TextView mWeekText;
            TextView mStartTimeText;
            TextView mEndTimeText;

            public ViewHolder(View itemView) {
                super(itemView);

                mStartTimeText = (TextView)itemView.findViewById(R.id.item_start_time);
                mEndTimeText = (TextView)itemView.findViewById(R.id.item_end_time);
                mWeekText = (TextView)itemView.findViewById(R.id.item_week);

                itemView.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {

            }
        }
    }

    /**
     * 수강신청한 과목의 시간정보를 내려받음
     * @param aLessonId
     */
    private void updateLessonTimes(int aLessonId) {
        LessonDao dao = mApp.openDBReadable().getLessonDao();

        mWebService.getLessonTimes(dao.load((long)aLessonId).getName()).enqueue(new Callback<List<LessonTime>>() {
            @Override
            public void onResponse(Call<List<LessonTime>> call, Response<List<LessonTime>> response) {
                List<LessonTime> times = response.body();

                if (times != null && times.size() != 0) {
                    mApp.openDBWritable().getLessonTimeDao().insertOrReplaceInTx(times);
                    Log.i(TAG, String.format(Locale.getDefault(),"강의시간 동기화 완료 (강의번호:%d 개수:%d)",
                            times.get(0).getLid(), times.size()));
                }
            }

            @Override
            public void onFailure(Call<List<LessonTime>> call, Throwable t) {

            }
        });
    }
}
