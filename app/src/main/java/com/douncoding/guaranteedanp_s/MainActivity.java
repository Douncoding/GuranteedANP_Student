package com.douncoding.guaranteedanp_s;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.douncoding.dao.Student;

public class MainActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener {
    public static final String TAG = MainActivity.class.getSimpleName();

    DrawerLayout mDrawerLayout;
    ActionBarDrawerToggle mDrawerToggle;
    Toolbar mToolbar;
    AppBarLayout mAppBarLayout;
    CollapsingToolbarLayout mCollapsingLayout;
    FloatingActionButton mFab;

    RelativeLayout mContainerView;
    LinearLayout mProfileView;
    TextView mProfileName;
    TextView mProfileJobs;

    AppContext mApp;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mToolbar = (Toolbar)findViewById(R.id.toolbar);
        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        mAppBarLayout = (AppBarLayout)findViewById(R.id.appbar_layout);
        mCollapsingLayout = (CollapsingToolbarLayout)findViewById(R.id.collapsing_layout);
        mFab = (FloatingActionButton)findViewById(R.id.fab);
        mProfileView = (LinearLayout)findViewById(R.id.profile_container);
        mContainerView = (RelativeLayout)findViewById(R.id.profile_container_parent);

        mProfileName = (TextView)findViewById(R.id.profile_name);
        mProfileJobs = (TextView)findViewById(R.id.profile_jobs);

        mApp = (AppContext) getApplicationContext();

        /**
         * 툴바 생성
         */
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() == null) {
            throw new NullPointerException("toolbar bind error");
        }

        /**
         * 네비케이션 생성
         */
        mDrawerToggle = new ActionBarDrawerToggle(
                this,
                mDrawerLayout,
                mToolbar,
                R.string.drawer_open,
                R.string.drawer_close) {

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
            }

            @Override
            public void onDrawerStateChanged(int newState) {
                super.onDrawerStateChanged(newState);
            }
        };

        mDrawerLayout.addDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        /**
         * Collasping 생성
         */
        mAppBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            int scrollRange = -1;

            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (scrollRange == -1) {
                    scrollRange = appBarLayout.getTotalScrollRange();
                }
                // 확장된 경우
                if (scrollRange + verticalOffset == 0) {
                    mProfileView.setVisibility(View.GONE);
                    //mCollapsingLayout.setTitle(getResources().
                    //        getString(R.string.toolbar_name_lesson_list));
                } else {
                    mProfileView.setVisibility(View.VISIBLE);
                    mCollapsingLayout.setTitle("");
                }
            }
        });

        /**
         * 내정보 화면
         */
        setupProfileView();

        showCoursesFragment();
    }

    private void setupProfileView() {
        Student student = mApp.내정보.얻기();

        mProfileName.setText(student.getName());
        mProfileJobs.setText(student.getEmail());
    }

    private void showEnrollmentFragment() {
        // 제목표시줄 변경
        mToolbar.setTitle("수강 편집");

        // FAB 버튼 기능 제정의 (수강목록 화면 보이기)
        mFab.setImageResource(R.drawable.ic_arrow_back_black_24dp);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCoursesFragment();
            }
        });

        // 화면 표시
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_container,
                        EnrollmentFragment.newInstance(),
                        EnrollmentFragment.TAG)
                .commit();
    }

    private void showCoursesFragment() {
        // 제목 표시줄 변경
        mToolbar.setTitle("수강 목록");

        // FAB 기능 재정의 (수강신청 화면 보이기)
        mFab.setImageResource(R.drawable.ic_add_black_24dp);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEnrollmentFragment();
            }
        });

        // 화면 표시
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_container,
                        CoursesFragment.newInstance(),
                        CoursesFragment.TAG)
                .commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_logout) {
            mApp.내정보.삭제();
            Toast.makeText(MainActivity.this
                    , "로그아웃 : 구현중"
                    , Toast.LENGTH_SHORT).show();
        }

        return true;
    }
}
