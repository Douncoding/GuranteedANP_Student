package com.douncoding.guaranteedanp_s;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.douncoding.dao.Lesson;
import com.douncoding.dao.LessonDao;
import com.douncoding.dao.LessonTime;

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
     * 웹 서비스와 내부자원를 관리하기 위한 리소스
     */
    AppContext mApp;
    PrincipalInteractor mPrincipal;

    TextView mHideOption;

    ProgressDialog progressDialog;
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
        mPrincipal = new PrincipalInteractor(mApp);
        mPrincipal.setOnListener(new PrincipalInteractor.OnListener() {
            @Override
            public void onLoad() {
                progressDialog.dismiss();
                startService(new Intent(SplashActivity.this, BeaconService.class));
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                finish();
            }
        });

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("로딩중 ...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mApp = null;
        mPrincipal = null;
    }

    /**
     * 네트워크 설정창 켜기
     */
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

        if (mApp.내정보.로그인()) {
            progressDialog.show();
            mPrincipal.sync();
        } else {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.splash_container, LoginFragment.newInstance())
                    .commit();
        }
    }

    @Override
    public void onLogin(boolean state) {
        if (state) {
            onResume();
        } else {
            Toast.makeText(this
                    , "로그인 실패"
                    , Toast.LENGTH_SHORT).show();
        }
    }
}
