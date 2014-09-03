package com.alangeorge.android.hermes;



import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 *
 */
public class SignOnFragment extends Fragment {

    public SignOnFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        @SuppressWarnings("UnnecessaryLocalVariable") View view = inflater.inflate(R.layout.fragment_sign_on, container, false);

        return view;
    }


    @Override
    public void onResume() {
        super.onResume();
    }
}
