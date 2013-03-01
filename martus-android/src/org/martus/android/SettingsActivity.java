package org.martus.android;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import android.app.ActionBar;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.view.MenuItem;
//import com.bugsense.trace.BugSenseHandler;

/**
 * @author roms
 */
public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String KEY_HAVE_UPLOAD_RIGHTS = "server_upload_rights";
    public static final String KEY_DEFAULT_LANGUAGE = "language_preference";
    public static final String KEY_TIMEOUT_MINUTES = "timeout_preference";
    public static final String KEY_SERVER_IP = "server_ip_preference";
    public static final String KEY_AUTHOR = "author_preference";
    public static final String KEY_DESKTOP_PUBLIC_KEY = "desktop_public_keystring";
    public static final String KEY_SERVER_PUBLIC_KEY = "server_public_keystring";
    public static final String KEY_KEY_PAIR = "key_pair";
    public static final String DEFAULT_TIMEOUT_MINUTES = "7";

    String[] languageNamesArray;
    String[] languageCodesArray;

    String[] timeoutNamesArray;
    String[] timeoutValuesArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Comment out for production build
        //BugSenseHandler.initAndStartSession(SettingsActivity.this, ExternalKeys.BUGSENSE_KEY);

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1) {
            ActionBar actionBar = getActionBar();
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        languageNamesArray = getResources().getStringArray(R.array.entries_language_preference);
        languageCodesArray = getResources().getStringArray(R.array.values_language_preference);

        timeoutNamesArray = getResources().getStringArray(R.array.entries_timeout_preference);
        timeoutValuesArray = getResources().getStringArray(R.array.values_timeout_preference);

        addPreferencesFromResource(R.xml.settings);
        SharedPreferences mySettings = getPreferenceScreen().getSharedPreferences();

        Map<String, ?> allPrefs = mySettings.getAll();


        //Initialize summaries of previously set settings
        Set<String> prefKeys = allPrefs.keySet();
        for (String key : prefKeys) {
            setPreferenceSummary(mySettings, key);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        setPreferenceSummary(sharedPreferences, key);
    }

    private void setPreferenceSummary(SharedPreferences sharedPreferences, String key) {
        Preference preference = findPreference(key);
        if (null != preference) {
            if (key.equals(KEY_DEFAULT_LANGUAGE)) {
                //need to show description of language as the summary, not the language code
                final String languageCode = sharedPreferences.getString(key, "?");
                final int index = Arrays.asList(languageCodesArray).indexOf(languageCode);
                preference.setSummary(languageNamesArray[index]);
            } else if (key.equals(KEY_TIMEOUT_MINUTES)) {
                final String timeoutValue = sharedPreferences.getString(key, DEFAULT_TIMEOUT_MINUTES);
                final int index = Arrays.asList(timeoutValuesArray).indexOf(timeoutValue);
                preference.setSummary(timeoutNamesArray[index]);
                BaseActivity.setTimeout(Integer.valueOf(timeoutValue));
            } else {
                // Set summary to be the selected value
                preference.setSummary(sharedPreferences.getString(key, ""));
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // app icon in action bar clicked; go home
                Intent intent = new Intent(this, MartusActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //Comment out for production build
        //BugSenseHandler.closeSession(SettingsActivity.this);
    }

}
