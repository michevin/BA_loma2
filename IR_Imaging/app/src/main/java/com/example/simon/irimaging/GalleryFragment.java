package com.example.simon.irimaging;



import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import java.io.File;


/**
 * A simple {@link Fragment} subclass.
 */
public class GalleryFragment extends Fragment {


    //private String path;

    //get the path
    String photoPath = Environment.getExternalStorageDirectory()+"/abc.jpg";

    //get bitmap
    //BitmapFactory.Options options = new BitmapFactory.Options();
    //options.inSampleSize = 8;
    //final Bitmap b = BitmapFactory.decodeFile(photoPath, options);



    public GalleryFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View galleryView = inflater.inflate(R.layout.fragment_gallery, container, false);

//        //find GridView in layout fragment_gallery
//        GridView gridView = (GridView) galleryView.findViewById(R.id.galleryView);
//
//        //create ImageAdapter object that holds the images to show
//        gridView.setAdapter(new ImageAdapter(getActivity()));


        return galleryView;
    }

}
