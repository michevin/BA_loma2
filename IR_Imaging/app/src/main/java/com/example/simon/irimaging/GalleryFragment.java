package com.example.simon.irimaging;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.Loader;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.net.Uri;
import android.widget.ScrollView;
import android.widget.LinearLayout;
import android.widget.Button;
import android.util.Log;
import android.content.Intent;
import android.provider.MediaStore;

/**
 * Created by Vincent Michel & Simon Schweizer on 04.05.2017.
 * The Class GalleryFragment implements a simple gallery that enables the user to view the
 * pictures saved in the internal storage
 */

public class GalleryFragment extends Fragment {
    public static final String TAG = "Test";
    private MainActivity mActivity;

    private ScrollView mScrollView;
    private LinearLayout mFormView;

    private static int RESULT_LOAD_IMAGE = 1;

    private static int sId = 0;

    private static int id() {
        return sId++;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d(TAG, "onAttach(): activity = " + activity);
    }

    /**
     * Initializes the fragment.
     * @param savedInstanceState: If the fragment is being re-created from a previous saved state,
     *                          this is the state.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate(): savedInstanceState = " + savedInstanceState);
        setRetainInstance(true);
    }

    /**
     *  Creates and returns the view hierarchy associated with the fragment.
     * @param savedInstanceState: If non-null, this fragment is being re-constructed from a
     *                          previous saved state as given here.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView(): container = " + container
                + "savedInstanceState = " + savedInstanceState);
        mActivity = (MainActivity) getActivity();

        if (mScrollView == null) {
            // normally inflate the view hierarchy
            mScrollView = (ScrollView) inflater.inflate(R.layout.fragment_gallery,container, false);
            mFormView = (LinearLayout) mScrollView.findViewById(R.id.form);
            Intent i = new Intent(Intent.ACTION_PICK,MediaStore.Images.Media.INTERNAL_CONTENT_URI);
            startActivityForResult(i, RESULT_LOAD_IMAGE);
        } else {

            ViewGroup parent = (ViewGroup) mScrollView.getParent();
            parent.removeView(mScrollView);
        }
        return mScrollView;
    }

    /**
     *  Called when an activity you launched exits, giving you the requestCode you started it with,
     *  the resultCode it returned, and any additional data from it.
     * @param requestCode:  int: The integer request code originally supplied to
     *                   startActivityForResult(), allowing you to identify who this result came from.
     * @param resultCode:   int: The integer result code returned by the child activity through
     *                  its setResult().
     * @param data:         Intent: An Intent, which can return result data to the caller
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mActivity.finishActivity(RESULT_LOAD_IMAGE);
        DrawerLayout drawer = (DrawerLayout) mActivity.findViewById(R.id.drawer_layout);
        drawer.openDrawer(Gravity.LEFT);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "onActivityCreated(): savedInstanceState = "
                + savedInstanceState);

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView()");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(TAG, "onDetach()");
    }
}