package com.example.simon.irimaging;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by Vincent Michel & Simon Schweizer on 04.05.2017.
 * Fragment used to show a list that allows to select from the already paired bluetooth devices.
 * {@link Fragment}
 */
public class BluetoothFragment extends Fragment {
    private MainActivity mActivity;

    /**
     * Required empty public constructor
     */
    public BluetoothFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mActivity = (MainActivity) getActivity();
        View bluetoothView = inflater.inflate(R.layout.fragment_bluetooth, container, false);
        selectConnect(bluetoothView);
        return bluetoothView;
    }

    /**
     * Allows to pick a bluetooth device out of the list of already paired device.
     * @param v: the view on which the list is projected.
     */
    public void selectConnect(View v){
        ListView lv = (ListView) v.findViewById(R.id.listViewBluetooth);

        mActivity.searchPairedDevice();

        final ArrayAdapter adapter = new  ArrayAdapter(getContext(),
                android.R.layout.simple_list_item_1, mActivity.list);
        lv.setAdapter(adapter);

        Toast.makeText(getContext(), "Showing Paired Devices",Toast.LENGTH_SHORT).show();

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                // When clicked, show a toast with the connected device
                Toast.makeText(getContext(), ((TextView) view).getText(),
                        Toast.LENGTH_SHORT).show();
                String val =(String) parent.getItemAtPosition(position);
                if (!(mActivity.BDevice.getName().equals(
                        mActivity.deviceList.get(position).getName()))){
                mActivity.BDevice = mActivity.deviceList.get(position);
                try {
                    mActivity.connectDevice(mActivity.BDevice);
                }catch(Exception e){
                    Toast.makeText(getContext(), "ERROR: no Bluetooth device found",
                            Toast.LENGTH_SHORT).show();
                }
                }else{
                    Toast.makeText(getContext(), "already selected",Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
