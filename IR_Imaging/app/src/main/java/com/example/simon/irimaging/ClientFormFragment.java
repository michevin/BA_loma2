package com.example.simon.irimaging;



import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.net.Uri;
import android.widget.ScrollView;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.ImageView;
import android.util.Log;
import android.content.Intent;
import android.provider.MediaStore;
import android.database.Cursor;
import android.graphics.BitmapFactory;

import java.io.File;


/**
 * A simple {@link Fragment} subclass.
 */
public class ClientFormFragment extends Fragment {
    public static final String TAG = "Test";

    private ScrollView mScrollView;
    private LinearLayout mFormView;

    private static int RESULT_LOAD_IMAGE = 1;
    Uri myPicture = null;
    Button buttonLoadImage;


    private static int sId = 0;

    private static int id() {
        return sId++;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d(TAG, "onAttach(): activity = " + activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate(): savedInstanceState = " + savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView(): container = " + container
                + "savedInstanceState = " + savedInstanceState);

        if (mScrollView == null) {
            // normally inflate the view hierarchy
            mScrollView = (ScrollView) inflater.inflate(R.layout.fragment_gallery_2,container, false);
            mFormView = (LinearLayout) mScrollView.findViewById(R.id.form);

            buttonLoadImage = (Button) mScrollView.findViewById(R.id.show_images);

            buttonLoadImage.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View arg0) {

                    Intent i = new Intent(
                            Intent.ACTION_PICK,
                            MediaStore.Images.Media.INTERNAL_CONTENT_URI);

                    startActivityForResult(i, RESULT_LOAD_IMAGE);
                }
            });


        } else {

            ViewGroup parent = (ViewGroup) mScrollView.getParent();
            parent.removeView(mScrollView);
        }
        return mScrollView;
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_LOAD_IMAGE  && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };

            Cursor cursor = getActivity().getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();

            ImageView imageView = (ImageView) mFormView.findViewById(R.id.imageView);
            imageView.setImageBitmap(BitmapFactory.decodeFile(picturePath));

        }


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



    public void onLoadFinished(Loader<Void> id, Void result) {
        Log.d(TAG, "onLoadFinished(): id=" + id);
    }

    public void onLoaderReset(Loader<Void> loader) {
        Log.d(TAG, "onLoaderReset(): id=" + loader.getId());
    }



}