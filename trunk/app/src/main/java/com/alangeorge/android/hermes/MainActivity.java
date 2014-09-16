package com.alangeorge.android.hermes;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.alangeorge.android.hermes.model.Message;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;
import java.security.KeyPair;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;

import static com.alangeorge.android.hermes.App.DEFAULT_AES_CIPHER;
import static com.alangeorge.android.hermes.App.DEFAULT_AES_SECURITY_PROVIDER;
import static com.alangeorge.android.hermes.App.DEFAULT_CHARACTER_SET;
import static com.alangeorge.android.hermes.App.DEFAULT_RSA_CIPHER;
import static com.alangeorge.android.hermes.App.DEFAULT_RSA_SECURITY_PROVIDER;

public class MainActivity extends FragmentActivity {
    private static final String TAG = "MainActivity";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String SENDER_ID = "323620638301";

    private GoogleCloudMessaging gcm;
    private SignOnFragment signOnFragment = new SignOnFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
                keyPair = App.getKeyPair();
                byte[] publicKey = keyPair.getPublic().getEncoded();
                Toast.makeText(App.context, "Public Key: " + Base64.encodeToString(publicKey, Base64.NO_WRAP), Toast.LENGTH_LONG).show();
                return true;
            case R.id.action_delete_keypair:
                App.deleteKayPair();
                return true;
            case R.id.action_test:
                keyPair = App.getKeyPair();

                Message message = new Message();
                Message.Body body = new Message.Body();

                body.setGcmRegistrationId(App.getGcmRegistrationId());
                body.setPublicKey(Base64.encodeToString(keyPair.getPublic().getEncoded(), Base64.NO_WRAP));

                SecretKeySpec symmetricKey = generateSymmetricKey();

                String theTestTextIn = "RSA stands for Ron Rivest, Adi Shamir and Leonard Adleman. They developed the algorithm by using the large integer factorization technique in 1977. It has since become so popular that we almost depend on similar technologies used in everyday life, such as banking, messaging, etc. As we briefly mentioned before, this type of algorithm uses a pair of keys used for encryption and decryption respectively.";

                // encode the message with symmetric key
                byte[] theTestTextInEncodedBytes;
                try {
                    Cipher cipher = Cipher.getInstance(DEFAULT_AES_CIPHER, DEFAULT_AES_SECURITY_PROVIDER);
                    cipher.init(Cipher.ENCRYPT_MODE, symmetricKey);
                    theTestTextInEncodedBytes = cipher.doFinal(theTestTextIn.getBytes(DEFAULT_CHARACTER_SET));
                } catch (Exception e) {
                    Log.e(TAG, "AES encryption error", e);
                    return true;
                }

                body.setMessage(Base64.encodeToString(theTestTextInEncodedBytes, Base64.NO_WRAP));

                // encode symmetricKey with receivers public key
                byte[] symmetricKeyEncodedBytes;
                try {
                    Cipher cipher = Cipher.getInstance(DEFAULT_RSA_CIPHER, DEFAULT_RSA_SECURITY_PROVIDER);
                    cipher.init(Cipher.ENCRYPT_MODE, keyPair.getPublic());
                    symmetricKeyEncodedBytes = cipher.doFinal(symmetricKey.getEncoded());
                } catch (Exception e) {
                    Log.e(TAG, "RSA encryption error", e);
                    return true;
                }

                body.setMessageKey(Base64.encodeToString(symmetricKeyEncodedBytes, Base64.NO_PADDING));
                message.setBody(body);
                message.sign(keyPair.getPrivate());

                String messageJson = message.toJson();
                Log.d(TAG, "message json: " + messageJson);
                Log.d(TAG, "message json length: " + messageJson.length());

                Message receivedMessage = new Message(messageJson);

                Log.d(TAG, "verify = " + receivedMessage.verify(keyPair.getPublic()));

                // decode the symmetricKey using receivers private key
                byte[] symmetricKeyDecodedBytes;
                try {
                    Cipher cipher = Cipher.getInstance(DEFAULT_RSA_CIPHER, DEFAULT_RSA_SECURITY_PROVIDER);
                    cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
                    symmetricKeyDecodedBytes = cipher.doFinal(Base64.decode(receivedMessage.getBody().getMessageKey(), Base64.NO_WRAP));
                } catch (Exception e) {
                    Log.e(TAG, "RSA decryption error", e);
                    return true;
                }

                // turn our decrypted bytes into a key
                SecretKeySpec symmetricKeyFromMessage = new SecretKeySpec(symmetricKeyDecodedBytes, "AES");

                // decrypt our message with our decrypted symmetric key
                byte[] theTestTextInDecodedBytes;
                try {
                    Cipher cipher = Cipher.getInstance(DEFAULT_AES_CIPHER, DEFAULT_AES_SECURITY_PROVIDER);
                    cipher.init(Cipher.DECRYPT_MODE, symmetricKeyFromMessage);
                    theTestTextInDecodedBytes = cipher.doFinal(Base64.decode(receivedMessage.getBody().getMessage(), Base64.NO_WRAP));
                } catch (Exception e) {
                    Log.e(TAG, "AES decryption error", e);
                    return true;
                }

                // display the message
                String receivedMessageText = new String(theTestTextInDecodedBytes, DEFAULT_CHARACTER_SET);
                Log.d(TAG, "Message:" + receivedMessageText);
                Toast.makeText(App.context, "Message:" + receivedMessageText, Toast.LENGTH_LONG).show();

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
                    String gcmRegistrationId = gcm.register(SENDER_ID);
                    msg = "Device registered, registration ID=" + gcmRegistrationId;

                    // You should send the registration ID to your server over HTTP,
                    // so it can use GCM/HTTP or CCS to send messages to your app.
                    // The request to your server should be authenticated if your app
                    // is using accounts.
                    sendGcmRegistrationIdToBackend();

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

    private void sendGcmRegistrationIdToBackend() {
        // No Op for now
    }

    private SecretKeySpec generateSymmetricKey() {
        SecretKeySpec secretKeySpec = null;
        try {
            KeyGenerator kg = KeyGenerator.getInstance("AES");
            kg.init(256, App.secureRandom);
            secretKeySpec = new SecretKeySpec((kg.generateKey()).getEncoded(), "AES");
        } catch (Exception e) {
            Log.e(TAG, "AES secret key spec error", e);
        }

        return secretKeySpec;
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            @SuppressWarnings("UnnecessaryLocalVariable") View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }
    }
}
