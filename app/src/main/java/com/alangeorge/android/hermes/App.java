package com.alangeorge.android.hermes;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.security.KeyPairGeneratorSpec;
import android.util.Log;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;

import javax.security.auth.x500.X500Principal;

public class App extends Application {
    private static final String TAG = "App";
    private static final String PROPERTY_GCM_REG_ID = "gcm_registration_id";
    private static final String PROPERTY_APP_VERSION = "app_version";
    public static final String KEYPAIR_ALIAS = "keypair_alias";

    public static Context context;

    /**
     * Here we make a statically scoped public ApplicationContext available.
     */
    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate()");
        super.onCreate();
        context = getApplicationContext();
    }

    public static String getGcmRegistrationId() {
        final SharedPreferences prefs = getPreferences();
        String registrationId = prefs.getString(PROPERTY_GCM_REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.i(TAG, "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion();
        if (registeredVersion != currentVersion) {
            Log.i(TAG, "App version changed.");
            return "";
        }
        return registrationId;
    }

    public static int getAppVersion() {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    public static void storeRegistrationId(String gcmRegistrationId) {
        final SharedPreferences prefs = getPreferences();
        int appVersion = getAppVersion();
        Log.i(TAG, "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_GCM_REG_ID, gcmRegistrationId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.apply();
    }

    public static KeyPair getKeyPair() {
        KeyStore keyStore;
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);

            Enumeration<String> keys = keyStore.aliases();
            while (keys.hasMoreElements()) {
                Log.d(TAG, "keystore alias: " + keys.nextElement());
            }

            KeyStore.Entry entry = keyStore.getEntry(KEYPAIR_ALIAS, null);

            if (entry != null) {

                if (!(entry instanceof KeyStore.PrivateKeyEntry)) {
                    Log.e(TAG, "Not an instance of a PrivateKeyEntry");
                    return null;
                }

                Certificate cert = keyStore.getCertificate(KEYPAIR_ALIAS);

                PublicKey publicKey = cert.getPublicKey();

                return new KeyPair(publicKey, ((KeyStore.PrivateKeyEntry) entry).getPrivateKey());
            }

//        } catch (KeyStoreException e) {
//        } catch (CertificateException e) {
//        } catch (NoSuchAlgorithmException e) {
//        } catch (IOException e) {
//        } catch (UnrecoverableEntryException e) {
        } catch (Throwable throwable) {
            Log.e(TAG, "unable to get KeyPair from KeyStore", throwable);
        }

        Calendar cal = Calendar.getInstance();
        Date now = cal.getTime();
        cal.add(Calendar.YEAR, 1);
        Date end = cal.getTime();

        KeyPair keyPair = null;

        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore");

            keyPairGenerator.initialize(new KeyPairGeneratorSpec.Builder(context)
                    .setAlias(KEYPAIR_ALIAS)
                    .setStartDate(now)
                    .setEndDate(end)
                    .setSerialNumber(BigInteger.valueOf(1))
                    .setSubject(new X500Principal("CN=test1"))
                    .setEncryptionRequired()
                    .setKeySize(2048)
                    .build());

            keyPair = keyPairGenerator.generateKeyPair();
//        } catch (InvalidAlgorithmParameterException e) {
//        } catch (NoSuchAlgorithmException e) {
//        } catch (NoSuchProviderException e) {
        } catch (Throwable throwable) {
            Log.e(TAG, "unable to create KeyPair", throwable);
        }

        return keyPair;
    }

    public static void deleteKayPair() {
        KeyStore keyStore;
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            keyStore.deleteEntry(KEYPAIR_ALIAS);
//        } catch (KeyStoreException e) {
//        } catch (CertificateException e) {
//        } catch (NoSuchAlgorithmException e) {
//        } catch (IOException e) {
        } catch (Throwable throwable) {
            Log.e(TAG, "unable to delete KeyPair", throwable);
        }
    }


    private static SharedPreferences getPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
}