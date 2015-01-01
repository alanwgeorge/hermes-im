package com.alangeorge.android.hermes;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;
import java.security.KeyPair;

public class MainActivity extends ActionBarActivity {
    private static final String TAG = "Hermes.MainActivity";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    private GoogleCloudMessaging gcm;
    private SignOnFragment signOnFragment = new SignOnFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, signOnFragment)
                    .commit();
        }

        // Check device for Play Services APK. If check succeeds, proceed with GCM registration.
        if (checkPlayServices()) {
            gcm = GoogleCloudMessaging.getInstance(this);
            String gcmRegistrationId = App.getGcmRegistrationId();

            if (gcmRegistrationId == null || gcmRegistrationId.isEmpty()) {
                registerInBackground();
            }
        } else {
            Log.i(TAG, "No valid Google Play Services APK found.");
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        KeyPair keyPair;

        switch (id) {
            case R.id.action_sign_off:
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, signOnFragment)
                        .commit();
                return true;
            case R.id.action_get_keypair:
                keyPair = App.getMyKeyPair();
                byte[] publicKey = keyPair.getPublic().getEncoded();
                Toast.makeText(App.context, "Public Key: " + Base64.encodeToString(publicKey, Base64.NO_WRAP), Toast.LENGTH_LONG).show();
                return true;
            case R.id.action_delete_keypair:
                App.deleteMyKeyPair();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void signOn(View view) {
        if (signOnFragment.signOn(view)) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, new ContactListFragment())
                    .commit();
        } else {
            Toast.makeText(App.context, "Sign On Failed", Toast.LENGTH_LONG).show();
        }
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device does not support Google Play Services.");
                finish();
            }
            return false;
        }
        return true;
    }

    /**
     * Registers the application with GCM servers asynchronously.
     * <p>
     * Stores the registration ID and app versionCode in the application's
     * shared preferences.
     */
    private void registerInBackground() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg;
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(getApplicationContext());
                    }
                    String gcmRegistrationId = gcm.register(getResources().getString(R.string.gcm_sender_id));
                    msg = "Device registered, registration ID=" + gcmRegistrationId;

                    sendGcmRegistrationIdToBackend();

                    // Persist the regID - no need to register again.
                    App.storeRegistrationId(gcmRegistrationId);
                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                }
                return msg;
            }

            @Override
            protected void onPostExecute(String msg) {
                super.onPostExecute(msg);
                Log.d(TAG, msg);
            }
        }.execute(null, null, null);
    }

    private void sendGcmRegistrationIdToBackend() {
        // No Op for now
    }
}
