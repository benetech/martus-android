package org.martus.android;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;

/**
 * @author roms
 */
public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String KEY_HAVE_UPLOAD_RIGHTS = "server_upload_rights";
    public static final String KEY_DEFAULT_LANGUAGE = "language_preference";
    public static final String KEY_SERVER_IP = "server_ip_preference";
    public static final String KEY_AUTHOR = "author_preference";
    public static final String KEY_DESKTOP_PUBLIC_KEY = "desktop_public_keystring";
    public static final String KEY_SERVER_PUBLIC_KEY = "server_public_keystring";
    public static final String KEY_KEY_PAIR = "key_pair";

    String[] languageNamesArray;
    String[] languageCodesArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        languageNamesArray = getResources().getStringArray(R.array.entries_language_preference);
        languageCodesArray = getResources().getStringArray(R.array.values_language_preference);

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

}
