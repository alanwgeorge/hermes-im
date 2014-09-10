package com.alangeorge.android.hermes;



import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SignOnFragment extends Fragment {
    private static final String TAG = "SignOnFragment";

    private TextView passwordView;

    public SignOnFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        @SuppressWarnings("UnnecessaryLocalVariable")
        View view = inflater.inflate(R.layout.fragment_sign_on, container, false);

        passwordView = (TextView) view.findViewById(R.id.editText_password);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public boolean signOn(View view) {
        Log.d(TAG, "signOn(" + view + ")");

        // hide keyboard
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(passwordView.getWindowToken(), 0);

        String incomingPassword = passwordView.getText().toString();
        Log.d(TAG, "password entered: " + incomingPassword);

        passwordView.setText("");

        String incomingPasswordHash = hashPassword(incomingPassword);
        String storedPasswordHash = App.getPasswordHash();

        if (TextUtils.isEmpty(storedPasswordHash)) {
            App.deleteKayPair();
            App.getKeyPair();
            App.storePasswordHash(incomingPasswordHash);
            Toast.makeText(App.context, "New account created", Toast.LENGTH_LONG).show();

            return true;
        } else {
            return storedPasswordHash.contentEquals(incomingPasswordHash);
        }
    }

    /**
     * Returns a Base64 encoded SHA-256 hash of the String input
     */
    private String hashPassword(String password) {
        byte[] passwordHash;
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(password.getBytes(App.DEFAULT_CHARACTER_SET));
            passwordHash = messageDigest.digest();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "SHA-256 algorithm not found", e);
            return null;
        }

        return Base64.encodeToString(passwordHash, Base64.NO_WRAP);
    }
}
