package com.douncoding.guaranteedanp_s;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = MainActivity.class.getSimpleName();

    DrawerLayout mDrawerLayout;
    ActionBarDrawerToggle mDrawerToggle;
    Toolbar mToolbar;
    AppBarLayout mAppBarLayout;
    CollapsingToolbarLayout mCollapsingLayout;
    FloatingActionButton mFab;

    LinearLayout mProfileView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mToolbar = (Toolbar)findViewById(R.id.toolbar);
        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        mAppBarLayout = (AppBarLayout)findViewById(R.id.appbar_layout);
        mCollapsingLayout = (CollapsingToolbarLayout)findViewById(R.id.collapsing_layout);
        mFab = (FloatingActionButton)findViewById(R.id.fab);
        mProfileView = (LinearLayout)findViewById(R.id.student_profile_container);

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
         * 수강신청 화면 출력
         */
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    @Override
    protected void onResume() {
        super.onResume();

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_container, new EnrollmentFragment())
                .commit();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}
