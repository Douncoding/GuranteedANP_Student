package com.douncoding.guaranteedanp_s;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.douncoding.dao.DaoMaster;
import com.douncoding.dao.DaoSession;
import com.douncoding.dao.Student;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * 강사정보는 변경사항이 적음으로
 */
public class AppContext extends Application {
    public static final String TAG = AppContext.class.getSimpleName();

    // 데이터베이스 인터페이스
    DaoMaster.DevOpenHelper mHelper;

    // 통신 인터페이스
    WebService mWebService;

    MyAccount 내정보;

    @Override
    public void onCreate() {
        super.onCreate();

        // Setup Database Manager
        mHelper = new DaoMaster.DevOpenHelper(this, Constants.DATABASE_NAME, null);

        내정보 = new MyAccount();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Constants.HOST)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        mWebService = retrofit.create(WebService.class);
    }

    /*
    // 실제 안드로이드에서 발생하지 않는 콜백이며, 에뮬레이터에서만 발생 한다.
    // 즉, 로그아웃 버튼을 통해 다음과정을 초기화하는 절차를 옮긴다.
    @Override
    public void onTerminate() {
        super.onTerminate();

        // Return Database Resource
        mHelper = null;

        내정보.삭제();
    }
    */

    public DaoSession openDBReadable() {
        SQLiteDatabase database = mHelper.getReadableDatabase();
        DaoMaster daoMaster = new DaoMaster(database);
        return daoMaster.newSession();
    }

    public DaoSession openDBWritable() {
        SQLiteDatabase database = mHelper.getWritableDatabase();
        DaoMaster daoMaster = new DaoMaster(database);
        return daoMaster.newSession();
    }

    public WebService getWebServiceInstance() {
        return mWebService;
    }


    /**
     * 로그인 상태를 관리한다. 웹 서버와 세션처리가 없는 구조임에 따라
     * 내부적인 로그인 상태와 로그인한 사용자의 정보를 관리하기 위한 목적만
     * 가진다. (로그인 시점에 생성되며, 어플 종료시점에 반환된다.)
     */
    class MyAccount {
        SharedPreferences preferences;

        public MyAccount() {
            preferences = getSharedPreferences(
                    Constants.PREFERENCE_NAME, Context.MODE_PRIVATE);
        }

        public Student 얻기() {
            Student student = new Student();
            student.setId(preferences.getLong("id", 0));
            student.setName(preferences.getString("name", null));
            student.setEmail(preferences.getString("email", null));
            student.setPhone(preferences.getString("phone", null));
            return student;
        }

        public boolean 로그인() {
            Student student = 얻기();

            if (student.getId() != 0) {
                return true;
            } else {
                return false;
            }
        }

        public void 저장(Student student) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putLong("id", student.getId());
            editor.putString("name", student.getName());
            editor.putString("email", student.getEmail());
            editor.putString("phone", student.getPhone());
            editor.apply();
            Log.i(TAG, "내정보 저장:");
        }

        public void 삭제() {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putLong("id", 0);
            editor.putString("name", null);
            editor.putString("email", null);
            editor.putString("phone", null);
            editor.apply();
            Log.i(TAG, "내정보 삭제:");
        }
    }
}
