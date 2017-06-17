package ru.orgcom.picky;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;


public class pref extends PreferenceActivity {
    static String TAG = "djd";
    public SharedPreferences prefs_default;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref);

        prefs_default = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        getActionBar().setTitle(getResources().getString(R.string.app_name)+" - Настройки");
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.boxgreen));

        String versionName = "";
        try {
            versionName = "(version "+getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0).versionName+")";
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Exception Version Name: " + e.getLocalizedMessage());
        }
        ((PreferenceCategory) findPreference("about")).setTitle("О программе "+versionName);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                finish();//Toast.makeText(getApplicationContext(), "Home", Toast.LENGTH_SHORT).show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

}