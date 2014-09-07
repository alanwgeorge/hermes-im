package com.alangeorge.android.hermes;



import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

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

    public void signOn(View view) {
        Log.d(TAG, "signOn(" + view + ")");
        Log.d(TAG, "password entered: " + passwordView.getText().toString());

        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(passwordView.getWindowToken(), 0);

        passwordView.setText("");
    }
}
