package com.example.simon.irimaging;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Created by Vincent Michel & Simon Schweizer on 04.05.2017.
 * MainActivity contains the variables and functions needed in all of the fragments, as well as the
 * skeleton for the drawerNavigation layout.
 */

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    /**
     * Variables needed for the drawerNavigation layout.
     */
    NavigationView navigationView = null;
    Toolbar toolbar = null;
    View rootView;

    /**
     * Variables needed in multiple fragments for the bluetooth connection.
     */
    protected BluetoothDevice BDevice = null;
    protected ArrayList list;
    protected List<BluetoothDevice> deviceList;
    protected Set<BluetoothDevice> pairedDevices;
    protected BluetoothAdapter mBluetoothAdapter;
    protected BluetoothSocket mbluetoothSocket;

    /**
     * Variables needed in multiple fragments for the bluetooth data stream.
     */
    protected InputStream stream;
    protected BufferedReader mbufferedReader;
    protected Boolean streamOn = false;
    protected Boolean irCamOn = false;

    /**
     * Variable needed for transparency.
     */
    protected double gamma;

    /**
     * Variable needed for save management.
     */
    protected String folderName;

    /**
     * Variable needed for threshold selection.
     */
    protected int threshold;

    /**
     * Creates the Activity initialises the drawerNavigation layout showing the cameraFragment.
     * Further sets all variables that can be changed in the parameterFragment to there default
     * value and turns on the bluetooth adapter.
     * @param savedInstanceState: If non-null, this fragment is being re-constructed
     *                            from a previous saved state as given here.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rootView = findViewById(android.R.id.content);

        //Set the fragment initially
        CameraFragment fragment = new CameraFragment();
        FragmentTransaction fragmentTransaction =
                getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment,"CameraFragment");
        fragmentTransaction.commit();

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);

        navigationView.setNavigationItemSelectedListener(this);

        gamma = 0.5;

        folderName = "IR-Imaging";

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        turnOnBluetooth(mBluetoothAdapter);
    }

    /**
     * Checks if openCV Manager is installed on the smartphone as soon as the Activity is created.
     */
    @Override
    public void onResume(){
        super.onResume();
        if(!OpenCVLoader.initDebug()){
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0,this,mLoaderCallback);
        }else{
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

    }

    /**
     * Called on pressed back key. Close and open Navigation Drawer Layout
     */
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Initializes the content of the Activity's standard options menu
     * @param menu: menu items to show.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /**
     * Called when an item in the navigation menu is selected. Replaces Fragment in use.
     * @param item: selected item.
     */
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            //Set the fragment initially
            CameraFragment fragment = new CameraFragment();
            android.support.v4.app.FragmentTransaction fragmentTransaction =
                    getSupportFragmentManager().beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container, fragment,"CameraFragment");
            fragmentTransaction.commit();

        } else if (id == R.id.nav_gallery) {
            GalleryFragment fragment = new GalleryFragment();
            android.support.v4.app.FragmentTransaction fragmentTransaction =
                    getSupportFragmentManager().beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container, fragment);
            fragmentTransaction.commit();

        } else if (id == R.id.nav_bluetooth) {
            BluetoothFragment fragment = new BluetoothFragment();
            android.support.v4.app.FragmentTransaction fragmentTransaction =
                    getSupportFragmentManager().beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container, fragment);
            fragmentTransaction.commit();

        } else if (id == R.id.nav_parameter) {
            ParameterFragment fragment = new ParameterFragment();
            android.support.v4.app.FragmentTransaction fragmentTransaction =
                    getSupportFragmentManager().beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container, fragment);
            fragmentTransaction.commit();

        } else if (id == R.id.nav_about) {
            AboutFragment fragment = new AboutFragment();
            android.support.v4.app.FragmentTransaction fragmentTransaction =
                    getSupportFragmentManager().beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container, fragment);
            fragmentTransaction.commit();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Checks if bluetooth adapter is on, if not asks the user if
     * the application can enable bluetooth.
     * @param newConfig: new device configuration.
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {

        if(isCameraFragmentPost()){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }else{
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR); // otherwise lock in portrait
        }
        super.onConfigurationChanged(newConfig);
    }

    /**
     *
     */
    private boolean isCameraFragmentPost() {
        Fragment fr = getSupportFragmentManager().findFragmentByTag("CameraFragment");
        return (fr != null && fr.isResumed() && (fr.getClass() == CameraFragment.class));
    }

    /**
     * Checks if bluetooth adapter is on, if not asks the user if
     * the application can enable bluetooth.
     * @param bluetoothAdapter: bluetooth adapter that needs to be checked.
     */
    public void turnOnBluetooth(BluetoothAdapter bluetoothAdapter){
        if (!bluetoothAdapter.isEnabled()) {
            Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOn, 0);
            Toast.makeText(this, "Bluetooth turned on",Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Bluetooth already on", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Generates a lis with all bluetooth devices that were once paired with the smartphone.
     */
    public void searchPairedDevice(){

        pairedDevices = mBluetoothAdapter.getBondedDevices();

        list = new ArrayList();

        for(BluetoothDevice bt : pairedDevices) list.add(bt.getName());

        deviceList = new ArrayList<>(pairedDevices);
    }

    /**
     * Connects the smartphone to the selected bluetooth device.
     * @param bluetoothDevice: selected bluetooth device.
     */
    public void connectDevice(BluetoothDevice bluetoothDevice){
        try{
            ParcelUuid[] pUuid = bluetoothDevice.getUuids();
            String uuid = pUuid[0].getUuid().toString();
            mbluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString(uuid));
            mbluetoothSocket.connect(); // note: blocking call
            createStream();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Creates the data stream from the bluetooth device to the smartphone. Once created
     * checks if thermal camera is already connected, if so turns the red square on cameraFragment
     * screen green.
     */
    public void createStream(){
        try{
            stream = mbluetoothSocket.getInputStream();
            mbufferedReader = new BufferedReader(new InputStreamReader(stream));
            streamOn = true;
            ImageView statusView = (ImageView) rootView.findViewById(R.id.statusView);
            Toast.makeText(getApplicationContext(), "Bluetooth connected",
                    Toast.LENGTH_LONG).show();
            try {
                if (streamOn && irCamOn) {
                    statusView.setBackgroundColor(Color.parseColor("#ff669900")); //green
                }else{
                    statusView.setBackgroundColor(Color.parseColor("#ffff4444")); //red
                }
            }catch (Exception e1){
                //Error Suppression as it is not relevant
            }
        }catch(Exception e){
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "ERROR: Bluetooth not connected",
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Loads data from the OpenCV manager application.
     */
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i("OpenCV", "OpenCV loaded successfully");
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };
}