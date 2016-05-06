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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.douncoding.dao.Instructor;
import com.douncoding.dao.InstructorDao;
import com.douncoding.dao.Lesson;
import com.douncoding.dao.LessonDao;
import com.douncoding.dao.PlaceDao;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

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
    WebService mWebService;

    /**
     * UI
     */
    RecyclerView mRecyclerView;
    ClassListAdapter mAdapter;

    /**
     * 강의목록의 데이터
     */
    ArrayList<Lesson> mLessons = new ArrayList<>();
    InstructorDao mInstructorsDao;
    PlaceDao mPlaceDao;
    LessonDao mLessonDao;

    AppContext mApp;

    public static EnrollmentFragment newInstance() {
        EnrollmentFragment fragment = new EnrollmentFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
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
        mLessonDao = mApp.openDBReadable().getLessonDao();
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

        mLessons.addAll(mLessonDao.loadAll());
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onPause() {
        super.onPause();
        mLessons.clear();
    }

    class ClassListAdapter extends RecyclerView.Adapter<ClassListAdapter.ViewHolder> {

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
            } else {
                holder.mExpandedView.setVisibility(View.GONE);
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

        public class ViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

            TextView mNameText;
            TextView mDescText;
            TextView mPersonnelText;

            TextView mLNameText;
            TextView mLJobsText;

            TextView mExpandedAction;
            TextView mEnrollmentAction;

            LinearLayout mExpandedView;

            public ViewHolder(View itemView) {
                super(itemView);

                mNameText = (TextView)itemView.findViewById(R.id.enrollment_name);
                mDescText = (TextView)itemView.findViewById(R.id.enrollment_desc);
                mPersonnelText = (TextView)itemView.findViewById(R.id.enrollment_personnel);

                mLNameText = (TextView)itemView.findViewById(R.id.enrollment_instructor_name);
                mLJobsText = (TextView)itemView.findViewById(R.id.enrollment_instructor_jobs);

                mExpandedView = (LinearLayout)itemView.findViewById(R.id.container_expanded);
                mExpandedAction = (TextView)itemView.findViewById(R.id.enrollment_expand_action);
                mEnrollmentAction = (TextView)itemView.findViewById(R.id.enrollment_post_action);

                mExpandedAction.setOnClickListener(this);
                mEnrollmentAction.setOnClickListener(this);
                //itemView.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.enrollment_expand_action:

                        if (expandedPosition >= 0) {
                            notifyItemChanged(expandedPosition);
                        }

                        expandedPosition = getPosition();
                        notifyItemChanged(expandedPosition);
                        break;
                    // 수강신청 버튼 클릭
                    case R.id.enrollment_post_action:
                        int lid = mLessons.get(getPosition()).getId().intValue();
                        int sid = mApp.내정보.얻기().getId().intValue();

                        // 서버 등록 요청
                        mWebService.enrollment(lid, sid).enqueue(new Callback<ResponseBody>() {
                            @Override
                            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                if (response.code() == 200) {
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
                }
            }
        }
    }
}
