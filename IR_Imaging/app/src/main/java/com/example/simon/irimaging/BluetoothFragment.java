package com.example.simon.irimaging;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/**
 * A simple {@link Fragment} subclass.
 */
public class BluetoothFragment extends Fragment {


    private Set<BluetoothDevice> pairedDevices;
    private ArrayList list;
    private List<BluetoothDevice> deviceList;
    private BluetoothAdapter bluetoothAdapter;


    public BluetoothFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View bluetoothView = inflater.inflate(R.layout.fragment_bluetooth, container, false);


        //pairedDevices = bluetoothAdapter.getBondedDevices();

        //list = new ArrayList();

        //for(BluetoothDevice bt : pairedDevices) list.add(bt.getName());

        //deviceList = new ArrayList<>(pairedDevices);




        return bluetoothView;
    }

}
