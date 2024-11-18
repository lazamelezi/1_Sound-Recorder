package com.lazamelezi.soundrecorder.fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.lazamelezi.soundrecorder.BuildConfig;
import com.lazamelezi.soundrecorder.Helper.MySharedPreferences;
import com.lazamelezi.soundrecorder.R;
import com.lazamelezi.soundrecorder.about.About;

public class SettingsFragment extends PreferenceFragmentCompat {

    public static final String TAG = "SettingsFragmentLog";
    private AppCompatActivity mAppCompatActivity;


    public void setActivity(AppCompatActivity activity) {
        this.mAppCompatActivity = activity;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        setPreferencesFromResource(R.xml.preferences, rootKey);

        CheckBoxPreference highQualityPref = findPreference(getString(R.string.pref_high_quality_key));
        highQualityPref.setChecked(MySharedPreferences.getPrefHighQuality(mAppCompatActivity));
        highQualityPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                MySharedPreferences.setPrefHighQuality(mAppCompatActivity, (boolean) newValue);
                return true;
            }
        });

        Preference aboutPref = findPreference(getString(R.string.pref_about_key));
        aboutPref.setSummary(getString(R.string.pref_about_desc, BuildConfig.VERSION_NAME));
        aboutPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(mAppCompatActivity, About.class);
                startActivity(intent);
                return true;
            }
        });
    }
}
