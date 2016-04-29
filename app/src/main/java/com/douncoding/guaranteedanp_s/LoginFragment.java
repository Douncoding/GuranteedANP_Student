package com.douncoding.guaranteedanp_s;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.douncoding.dao.Student;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginFragment extends Fragment {
    public static final String TAG = LoginFragment.class.getSimpleName();

    EditText mEmailEdit;
    EditText mPasswordEdit;
    Button mLoginView;
    Button mSignUpView;

    WebService mWebService;
    AppContext mApp;

    OnListener onListener;

    public static LoginFragment newInstance() {
        LoginFragment fragment = new LoginFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public interface OnListener {
        void onLogin(boolean state);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            onListener = (OnListener)activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(TAG + "의 리스너를 꼭 구현해야 합니다.");
        }

        mApp = (AppContext)activity.getApplicationContext();
        mWebService = mApp.getWebServiceInstance();

        if (mWebService == null) {
            throw new RuntimeException("WebService is null. please network state check");
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        mEmailEdit = (EditText)view.findViewById(R.id.email_edit);
        mPasswordEdit = (EditText)view.findViewById(R.id.password_edit);

        mLoginView = (Button)view.findViewById(R.id.login_send_btn);
        mSignUpView = (Button)view.findViewById(R.id.signup_send_btn);
        mLoginView.setOnClickListener(onLoginListener);
        mSignUpView.setOnClickListener(onSignUpListener);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * 로그인 동작
     */
    View.OnClickListener onLoginListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String email = CommonUtils.getStringAndEmptyErrorHandle(mEmailEdit);
            String password = CommonUtils.getStringAndEmptyErrorHandle(mPasswordEdit);

            mWebService.login(email, password).enqueue(new Callback<Student>() {
                @Override
                public void onResponse(Call<Student> call, Response<Student> response) {
                    if (response.body() != null) {
                        mApp.내정보.저장(response.body());
                        onListener.onLogin(true);
                    } else {
                        Log.w(TAG, "로그인 결과는 정상이지만, 학생정보를 받지 못함");
                    }
                }

                @Override
                public void onFailure(Call<Student> call, Throwable t) {
                    onListener.onLogin(false);
                }
            });
        }
    };

    View.OnClickListener onSignUpListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Toast.makeText(getContext()
                    , "공사중"
                    , Toast.LENGTH_SHORT).show();
        }
    };
}
