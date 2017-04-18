package com.ericchee.bboyairwreck.piemessage;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class PiePreferenceActivity extends android.preference.PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new PiePreferenceFragment()).commit();
    }

    public static class PiePreferenceFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        private static final String TAG = PiePreferenceFragment.class.getSimpleName();

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // Load user settings xml resource
            addPreferencesFromResource(R.xml.user_settings);
            updateSocketAddressSummary(findPreference(getString(R.string.pref_socket_address_key)));
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Preference pref = findPreference(key);

            boolean restartBridge = false;

            Log.i(TAG, "Preference change detected");
            if (pref instanceof EditTextPreference && key.equals(getString(R.string.pref_socket_address_key))) {
                Log.i(TAG, "Setting IP address");

                String socketAddress = sharedPreferences.getString(this.getString(R.string.pref_socket_address_key), "127.0.0.1");

                if (MainActivity.validIP(socketAddress)) {
                    restartBridge = true;
                } else {
                    CharSequence text = "Invalid IP address";
                    Toast.makeText(PieMessageApplication.getInstance(), text, Toast.LENGTH_SHORT).show();
                }

                updateSocketAddressSummary(pref);
            } else if (pref instanceof EditTextPreference && key.equals(getString(R.string.pref_password))) {
                Log.i(TAG, "Setting password");
                restartBridge = true;
            }

            if (restartBridge) {
                PieMessageApplication.getInstance().startServerBridge();
            }
        }

        private void updateSocketAddressSummary(Preference pref) {
            EditTextPreference editTextPreference = (EditTextPreference) pref;
            pref.setSummary(editTextPreference.getText());
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            PreferenceManager.getDefaultSharedPreferences(activity).registerOnSharedPreferenceChangeListener(this);
        }
    }


}
