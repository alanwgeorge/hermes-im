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
import java.nio.charset.Charset;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.util.Calendar;
import java.util.Date;

import javax.security.auth.x500.X500Principal;

public class App extends Application {
    private static final String TAG = "App";
    private static final String PROPERTY_GCM_REG_ID = "gcm_registration_id";
    private static final String PROPERTY_APP_VERSION = "app_version";
    private static final String PROPERTY_PASSWORD_HASH = "password_hash";
    public static final String KEYPAIR_ALIAS = "hermes_keypair_alias";
    public static final String DEFAULT_AES_SECURITY_PROVIDER = "BC";
    public static final String DEFAULT_AES_CIPHER = "AES";
    public static final String DEFAULT_RSA_SECURITY_PROVIDER = "AndroidOpenSSL";
    public static final String DEFAULT_RSA_CIPHER = "RSA/ECB/PKCS1Padding";
    public static final String DEFAULT_KEYPAIR_ALGORITHM = "RSA";
    public static final String DEFAULT_KEYPAIR_PROVIDER = "AndroidKeyStore";
    public static final String DEFAULT_SIGNATURE_ALGORITHM = "SHA512withRSA";
    public static final Charset DEFAULT_CHARACTER_SET = Charset.forName("UTF-8");

    public static final SecureRandom secureRandom;

    static {
        SecureRandom temp = null;
        try {
            temp = SecureRandom.getInstance("SHA1PRNG", DEFAULT_RSA_SECURITY_PROVIDER);
        } catch (Exception e) {
            Log.e(TAG, "Unable to initialize SecureRandom", e);
            throw new IllegalArgumentException("Unable to initialize SecureRandom", e);
        } finally {
            secureRandom = temp;
        }
    }

    public static Context context;

    /**
     * Here we make a statically scoped public ApplicationContext available.
     */
    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate()");
        super.onCreate();
        context = getApplicationContext();

//        if (BuildConfig.DEBUG) {
//            Provider[] providers = Security.getProviders();
//            for (Provider provider : providers) {
//                Log.i("CRYPTO", "provider:" + provider.getName());
//                Set<Provider.Service> services = provider.getServices();
//                for (Provider.Service service : services) {
//                    Log.i("CRYPTO", "  algorithm: " + service.getAlgorithm());
//                }
//            }
//        }
    }

    public static String getPasswordHash() {
        SharedPreferences prefs = getPreferences();

        return prefs.getString(PROPERTY_PASSWORD_HASH, null);
    }

    public static void storePasswordHash(String passwordHash) {
        SharedPreferences prefs = getPreferences();
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_PASSWORD_HASH, passwordHash);
        editor.apply();
    }


    public static String getGcmRegistrationId() {
        SharedPreferences prefs = getPreferences();
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
        SharedPreferences prefs = getPreferences();
        int appVersion = getAppVersion();
        Log.i(TAG, "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_GCM_REG_ID, gcmRegistrationId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.apply();
    }

    public static KeyPair getMyKeyPair() {
        KeyStore keyStore;
        try {
            keyStore = KeyStore.getInstance(DEFAULT_KEYPAIR_PROVIDER);
            keyStore.load(null);

//            Enumeration<String> keys = keyStore.aliases();
//            while (keys.hasMoreElements()) {
//                Log.d(TAG, "keystore alias: " + keys.nextElement());
//            }

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

        return makeKeyPair(KEYPAIR_ALIAS);
    }

    public static void deleteMyKeyPair() {
        deleteKeyPair(KEYPAIR_ALIAS);
    }

    public static KeyPair makeKeyPair(String alias) {
        Calendar cal = Calendar.getInstance();
        Date now = cal.getTime();
        cal.add(Calendar.YEAR, 1);
        Date end = cal.getTime();

        KeyPair keyPair = null;

        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(DEFAULT_KEYPAIR_ALGORITHM, DEFAULT_KEYPAIR_PROVIDER);

            keyPairGenerator.initialize(new KeyPairGeneratorSpec.Builder(context)
                    .setAlias(alias)
                    .setStartDate(now)
                    .setEndDate(end)
                    .setSerialNumber(BigInteger.valueOf(1))
                    .setSubject(new X500Principal("CN=test1"))
//                    .setEncryptionRequired()
//                    .setKeySize(2048)
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

    public static void deleteKeyPair(String alias) {
        KeyStore keyStore;
        try {
            keyStore = KeyStore.getInstance(DEFAULT_KEYPAIR_PROVIDER);
            keyStore.load(null);
            keyStore.deleteEntry(alias);
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