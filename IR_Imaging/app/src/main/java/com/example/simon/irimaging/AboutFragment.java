package com.example.simon.irimaging;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Vincent Michel & Simon Schweizer on 04.05.2017.
 * Fragment used to show the a short descriptive text about the application and its purpose.
 * {@link Fragment}
 */
public class AboutFragment extends Fragment {

    /**
     * Required empty public constructor.
     */
    public AboutFragment(){
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment.
        return inflater.inflate(R.layout.fragment_about, container, false);
    }
}
