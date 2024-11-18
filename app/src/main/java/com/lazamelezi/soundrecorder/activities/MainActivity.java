package com.lazamelezi.soundrecorder.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.lazamelezi.soundrecorder.R;
import com.lazamelezi.soundrecorder.adapters.MyViewPagerAdapter;
import com.lazamelezi.soundrecorder.fragments.FileViewerFragment;
import com.lazamelezi.soundrecorder.fragments.RecordFragment;

public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = "MainActivityLog";
    private static final int action_settings = R.id.action_settings;
    FileViewerFragment fileViewerFragment;
    RecordFragment mRecordFragment;
    private String[] titles;

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        if (mRecordFragment.isRecordStarted())
            mRecordFragment.stopService();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {
            case action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LifeOfApp();

        Toolbar toolbar = findViewById(R.id.mToolbar);
        setSupportActionBar(toolbar);

        ViewPager2 mViewPager2 = findViewById(R.id.mViewPager2);
        TabLayout tabLayout = findViewById(R.id.tabLayout);

        titles = new String[]{getString(R.string.tab_title_record), getString(R.string.tab_title_saved_recordings)};

        MyViewPagerAdapter viewPagerAdapter = new MyViewPagerAdapter(this);

        viewPagerAdapter.addFragment(new RecordFragment(MainActivity.this));
        viewPagerAdapter.addFragment(new FileViewerFragment(MainActivity.this));

        mViewPager2.setAdapter(viewPagerAdapter);

        mRecordFragment = (RecordFragment) viewPagerAdapter.getFragment(0);
        fileViewerFragment = (FileViewerFragment) viewPagerAdapter.getFragment(1);

        new TabLayoutMediator(tabLayout, mViewPager2, new TabLayoutMediator.TabConfigurationStrategy() {
            @Override
            public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                tab.setText(titles[position]);
            }
        }).attach();
    }

    private void LifeOfApp() {

        ProcessLifecycleOwner.get().getLifecycle().addObserver(new LifecycleEventObserver() {
            @Override
            public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {

                Log.d(LOG_TAG, "Event - " + event.name());

                switch (event) {

                    case ON_CREATE:

                        break;
                    case ON_START:
                        break;
                    case ON_RESUME:

                        if (fileViewerFragment != null)
                            fileViewerFragment.trackWhenFileDelete();

                        break;
                    case ON_PAUSE:
                        break;
                    case ON_STOP:
                        break;


                }

            }
        });

    }
}