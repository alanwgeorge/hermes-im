package com.alangeorge.android.hermes;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;
import java.security.KeyPair;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;

public class MainActivity extends FragmentActivity {
    private static final String TAG = "MainActivity";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String SENDER_ID = "323620638301";

    private GoogleCloudMessaging gcm;
//    private String gcmRegistrationId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new SignOnFragment())
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
            case R.id.action_get_keypair:
                keyPair = App.getKeyPair();
                byte[] publicKey = keyPair.getPublic().getEncoded();
                Toast.makeText(App.context, "Public Key: " + Base64.encodeToString(publicKey, Base64.NO_WRAP), Toast.LENGTH_LONG).show();
                return true;
            case R.id.action_delete_keypair:
                App.deleteKayPair();
                return true;
            case R.id.action_test:
                keyPair = App.getKeyPair();

                SecretKeySpec secretKeySpec = generateSymmetricKey();

                String encodedSecretKeySpec = Base64.encodeToString(secretKeySpec.getEncoded(), Base64.NO_PADDING);

//                String theTestText = "RSA stands for Ron Rivest, Adi Shamir and Leonard Adleman. They developed the algorithm by using the large integer factorization technique in 1977. It has since become so popular that we almost depend on similar technologies used in everyday life, such as banking, messaging, etc. As we briefly mentioned before, this type of algorithm uses a pair of keys used for encryption and decryption respectively.";
//                String theTestText = "RSA stands for Ron Rivest, Adi Shamir and Leonard Adleman.";

                byte[] encodedBytes;
                try {
                    Cipher c = Cipher.getInstance("RSA/ECB/PKCS1Padding", "AndroidOpenSSL");
                    c.init(Cipher.ENCRYPT_MODE, keyPair.getPrivate());
                    encodedBytes = c.doFinal(encodedSecretKeySpec.getBytes());
                } catch (Exception e) {
                    Log.e(TAG, "RSA encryption error", e);
                    return true;
                }

                byte[] decodedBytes;
                try {
                    Cipher c = Cipher.getInstance("RSA/ECB/PKCS1Padding", "AndroidOpenSSL");
                    c.init(Cipher.DECRYPT_MODE, keyPair.getPublic());
                    decodedBytes = c.doFinal(encodedBytes);
                } catch (Exception e) {
                    Log.e(TAG, "RSA decryption error", e);
                    return true;
                }

                assert decodedBytes != null;
                String result = new String(decodedBytes);

                assert result.endsWith(encodedSecretKeySpec);

                Toast.makeText(App.context, result, Toast.LENGTH_LONG).show();

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
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
                    String gcmRegistrationId = gcm.register(SENDER_ID);
                    msg = "Device registered, registration ID=" + gcmRegistrationId;

                    // You should send the registration ID to your server over HTTP,
                    // so it can use GCM/HTTP or CCS to send messages to your app.
                    // The request to your server should be authenticated if your app
                    // is using accounts.
                    sendRegistrationIdToBackend();

                    // For this demo: we don't need to send it because the device
                    // will send upstream messages to a server that echo back the
                    // message using the 'from' address in the message.

                    // Persist the regID - no need to register again.
                    App.storeRegistrationId(gcmRegistrationId);
                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
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

    /**
     * Sends the registration ID to your server over HTTP, so it can use GCM/HTTP or CCS to send
     * messages to your app. Not needed for this demo since the device sends upstream messages
     * to a server that echoes back the message using the 'from' address in the message.
     */
    private void sendRegistrationIdToBackend() {
        // Your implementation here.
    }

    private SecretKeySpec generateSymmetricKey() {
        SecretKeySpec secretKeySpec = null;
        try {
            SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
            secureRandom.setSeed("foobar".getBytes());
            KeyGenerator kg = KeyGenerator.getInstance("AES");
            kg.init(128, secureRandom);
            secretKeySpec = new SecretKeySpec((kg.generateKey()).getEncoded(), "AES");
        } catch (Exception e) {
            Log.e(TAG, "AES secret key spec error", e);
        }

        return secretKeySpec;
    }
}
