package com.douncoding.guaranteedanp_s;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.douncoding.dao.Instructor;
import com.douncoding.dao.Lesson;
import com.douncoding.dao.LessonDao;
import com.douncoding.dao.LessonTime;
import com.douncoding.dao.Place;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * 로그인과 서버 데이터베이스 동기화 과정을 처리 하는 액티비티
 */
public class SplashActivity extends AppCompatActivity implements
        LoginFragment.OnListener {
    public static final String TAG = SplashActivity.class.getSimpleName();

    /**
     * 로딩순서 정의
     */
    enum LoadingStep {
        INIT, INSTRUCTOR, PLACE, LESSON, LESSONTIMES, BEACON, COMPLETE
    } LoadingStep mCurrentStep;

    ProgressDialog mProgDialog;

    /**
     * 웹 서비스와 내부자원를 관리하기 위한 리소스
     */
    WebService mWebService;
    AppContext mApp;

    TextView mHideOption;
    public int optionCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        mHideOption = (TextView)findViewById(R.id.hide_option);
        mHideOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (optionCount++ > 5) {
                    showHideOptionDialog();
                }
            }
        });

        mApp = (AppContext)getApplicationContext();
    }

    private void showHideOptionDialog() {
        final EditText edit = new EditText(this);
        edit.setText(Constants.HOST);

        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("네트워크 설정");
        dialog.setView(edit);
        dialog.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Constants.HOST = edit.getText().toString();
                onResume();
            }
        });
        dialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Constants.HOST)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        mWebService = retrofit.create(WebService.class);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mApp.내정보.로그인()) {
                    loading(LoadingStep.INIT);
                } else {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.splash_container, LoginFragment.newInstance())
                            .commit();
                }
            }
        }, 2000);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    /**
     * {@link LoginFragment} 의 사용자 로그인 수행 결과를 전달받음
     * @param state 로그인 결과
     */
    @Override
    public void onLogin(boolean state) {
        if (state) {
            /**
             * 항상 처음부터 로딩시작
             * 부분 적인 로딩이 필요한 경우 {@link mCurrentStep} 의 위치부터
             * 로딩을 수행할 수 있다.
             */
            loading(LoadingStep.INIT);
        } else {
            Toast.makeText(this
                    , "로그인 실패"
                    , Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 로딩 처리로직의 최상위 부모
     * 서버 데이터베이스와 강사, 강의, 장소정보를 동기화한다. 순서는 {@link LoadingStep} 에 정의된
     * 순서에 따라 처리된다.
     * @param step 현재 로딩 위치
     */
    private void loading(final LoadingStep step) {
        if (LoadingStep.INIT.equals(step)) {
            mProgDialog = new ProgressDialog(this);
            mProgDialog.setTitle("로딩중 ...");
            mProgDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgDialog.show();

            // 내부 모든 테이블 초기화 - 재시작 시점에 재구성
            mApp.openDBWritable().getLessonDao().deleteAll();
            mApp.openDBWritable().getPlaceDao().deleteAll();
            mApp.openDBWritable().getStudentDao().deleteAll();
            mApp.openDBWritable().getInstructorDao().deleteAll();
            mApp.openDBWritable().getLessonTimeDao().deleteAll();

            loadingNextStep(step);
        } else if (LoadingStep.INSTRUCTOR.equals(step)) {
            mProgDialog.setMessage("강사목록 동기화 중");

            mWebService.loadAllInstructors().enqueue(new Callback<List<Instructor>>() {
                @Override
                public void onResponse(Call<List<Instructor>> call, Response<List<Instructor>> response) {
                    List<Instructor> body = response.body();

                    if (body != null) {
                        mApp.openDBWritable().getInstructorDao().insertOrReplaceInTx(body);
                        Log.i(TAG, "강사목록 동기화 성공: 항목수:" + body.size());
                    } else {
                        Log.w(TAG, "강사목록 동기화 오류: 서버로 부터 전송받은 강사정보 없음");
                    }

                    loadingNextStep(step);
                }

                @Override
                public void onFailure(Call<List<Instructor>> call, Throwable t) {
                    Log.e(TAG, "강사목록 동기화 실패:" + t.toString());
                }
            });
        } else if (LoadingStep.PLACE.equals(step)) {
            mProgDialog.setMessage("강의실 목록 동기화 중");

            mWebService.loadAllPlaces().enqueue(new Callback<List<Place>>() {
                @Override
                public void onResponse(Call<List<Place>> call, Response<List<Place>> response) {
                    List<Place> body = response.body();

                    if (body != null) {
                        mApp.openDBWritable().getPlaceDao().insertOrReplaceInTx(body);
                        Log.i(TAG, "강의실 목록 동기화 성공: 항목수:" + body.size());
                    } else {
                        Log.w(TAG, "강사실 목록 동기화 오류: 서버로 부터 전송받은 강사정보 없음");
                    }

                    loadingNextStep(step);
                }

                @Override
                public void onFailure(Call<List<Place>> call, Throwable t) {
                    Log.e(TAG, "강사실 목록 동기화 실패:" + t.toString());
                }
            });
        } else if (LoadingStep.LESSON.equals(step)) {
            mProgDialog.setMessage("강의 목록 동기화 중");

            mWebService.loadAllLessons().enqueue(new Callback<List<Lesson>>() {
                @Override
                public void onResponse(Call<List<Lesson>> call, Response<List<Lesson>> response) {
                    List<Lesson> body = response.body();

                    if (body != null) {
                        mApp.openDBWritable().getLessonDao().insertOrReplaceInTx(body);
                        Log.i(TAG, "강의 목록 동기화 성공: 항목수:" + body.size());
                    } else {
                        Log.w(TAG, "강사 목록 동기화 오류: 서버로 부터 전송받은 강사정보 없음");
                    }

                    loadingNextStep(step);
                }

                @Override
                public void onFailure(Call<List<Lesson>> call, Throwable t) {
                    Log.e(TAG, "강사 목록 동기화 실패:" + t.toString());
                }
            });
        } else if (LoadingStep.LESSONTIMES.equals(step)) {
            mProgDialog.setMessage("내 강의목록 동기화 중");
            loadOwnerLessonAndTimes(step);
        } else if (LoadingStep.BEACON.equals(step)) {
            mProgDialog.setMessage("비콘 모니터링 서비스 활성화 중");
            startService(new Intent(this, BeaconService.class));
            loadingNextStep(step);
        } else if (LoadingStep.COMPLETE.equals(step)) {
            mProgDialog.setMessage("로딩완료 시작합니다.");
            mProgDialog.dismiss();

            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else {
            Log.e(TAG, "정의되지 않은 로딩절차 발생: ");
            loadingNextStep(step);
        }
    }

    /**
     * 자신의 수강신청내역과 강의 시간 동기화
     */
    private void loadOwnerLessonAndTimes(final LoadingStep step) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                int ownId = mApp.내정보.얻기().getId().intValue();

                try {
                    /**
                     * 내 수강목록 요청
                     */
                    List<Lesson> lessons = mWebService.getEnrollmentLesson(ownId).execute().body();

                    LessonDao dao = mApp.openDBReadable().getLessonDao();
                    for (Lesson lesson : lessons) {
                        lesson.setEnrollment(Integer.valueOf(1));
                        dao.update(lesson);
                        Log.e(TAG, "내수강목록: 강의번호:" + lesson.getId());
                    }

                    /**
                     * 수강목록의 강의시간 요청
                     */
                    for (Lesson lesson : lessons) {
                        List<LessonTime> times = mWebService
                                .getLessonTimes(lesson.getName())
                                .execute().body();

                        mApp.openDBWritable()
                                .getLessonTimeDao()
                                .insertOrReplaceInTx(times);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                loadingNextStep(step);
            }
        }.execute();
    }

    private void loadingNextStep(LoadingStep step) {
        mCurrentStep = step;

        LoadingStep[] nextSteps = LoadingStep.values();
        loading(nextSteps[step.ordinal()+1]);
    }
}
