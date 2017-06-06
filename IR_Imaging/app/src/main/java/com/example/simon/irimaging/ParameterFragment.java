package com.example.simon.irimaging;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Created by Vincent Michel & Simon Schweizer on 04.05.2017.
 * Fragment used to allow the user to change the save folder name, the weighting of the
 * optical image in the merged image and the threshold type used.
 * {@link Fragment}
 */

public class ParameterFragment extends Fragment {
    private MainActivity mActivity;

    /**
     * Required empty public constructor
     */
    public ParameterFragment() {
    }

    /**
     * Inflates the parameterFragment with one seek bar (for the transparency level),
     * one edit text (for the folder name) and a set of three radio buttons (for the
     * threshold type).
     * @param inflater: The LayoutInflater object that can be used to inflate
     *                  any views in the fragment
     * @param container: If non-null, this is the parent view that the fragment's
     *                   UI should be attached to. The fragment should not add the view itself,
     *                   but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState: If non-null, this fragment is being re-constructed
     *                            from a previous saved state as given here.
     * @return parameterView -> view that is inflated into MainActivity.
     */
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mActivity = (MainActivity) getActivity();
        final View parameterView = inflater.inflate(R.layout.fragment_parameter, container, false);
        final SeekBar seekBar = (SeekBar) parameterView.findViewById(R.id.seekBar);
        final TextView valueGamma = (TextView) parameterView.findViewById(R.id.text_valueAlpha);
        seekBar.setProgress((int) (mActivity.gamma*100));
        valueGamma.setText("gamma = " + String.valueOf(0.5));

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                double doubleAlpha = (seekBar.getProgress()/100.0);
                valueGamma.setText("gamma = " + String.valueOf(doubleAlpha));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mActivity.gamma = seekBar.getProgress()/100.0;
            }
        });

        final EditText folderNameEdit = (EditText) parameterView.findViewById(R.id.editText_folderName);
        folderNameEdit.setText(mActivity.folderName);

        folderNameEdit.addTextChangedListener(new TextWatcher(){
            @Override
            public void afterTextChanged(Editable s) {
                if (!(folderNameEdit.getText().toString().equals(""))) {
                    mActivity.folderName = folderNameEdit.getText().toString();
                }else{
                    mActivity.folderName = "IR-Imaging";
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        })
        ;

        final RadioGroup radioButtons = (RadioGroup) parameterView.findViewById(R.id.radioGroup);
        radioButtons.check(R.id.radio_meanWhite + mActivity.threshold);

        radioButtons.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(RadioGroup radioGroup,int value){
                switch (value){
                    case R.id.radio_meanWhite:
                        mActivity.threshold = 0;
                        break;

                    case R.id.radio_meanBlack:
                        mActivity.threshold = 1;
                        break;

                    case R.id.radio_Flood:
                        mActivity.threshold = 2;
                        break;

                    default:
                        mActivity.threshold = 0;
                        break;
                }
            }
        });
        return parameterView;
    }
}
