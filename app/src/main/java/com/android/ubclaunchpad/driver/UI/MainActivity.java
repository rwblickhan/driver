package com.android.ubclaunchpad.driver.UI;

import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.Toast;

import com.android.ubclaunchpad.driver.R;
import com.android.ubclaunchpad.driver.session.SessionActivity;
import com.android.ubclaunchpad.driver.user.UserManager;
import com.android.ubclaunchpad.driver.util.FirebaseUtils;
import com.android.ubclaunchpad.driver.util.StringUtils;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends BaseMenuActivity {


    @BindView(R.id.i_am_a_passenger_button)
    RadioButton mPassengerButton;
    @BindView(R.id.i_am_a_driver_button)
    RadioButton mDriverButton;
    @BindView(R.id.button3)
    Button mSessionButton;

    private final static String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mPassengerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // this is a debug statement, delete this when load screen view is implemented
                Toast.makeText(v.getContext(), "I AM A PASSENGER", Toast.LENGTH_SHORT).show();

                if (FirebaseUtils.getFirebaseUser() != null) {
                    String uid = FirebaseUtils.getFirebaseUser().getUid();
                    Log.d(TAG, "got uid: " + uid);
                    FirebaseUtils.getDatabase().child(StringUtils.FirebaseUserEndpoint).child(uid).child(StringUtils.isDriverEndpoint).setValue(false);
                    FirebaseUtils.getDatabase().child(StringUtils.FirebaseUserEndpoint).child(uid).child(StringUtils.numPassengersEndpoint).setValue(0);
                }
                // TODO: at this point, take user to load screen, so they can wait to be matched
            }
        });

        mDriverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment numPassengersFragment = new NumPassengersFragment();
                numPassengersFragment.show(getFragmentManager(), "num_passengers");
            }
        });

        try {
            if (UserManager.getInstance().getUser().isDriver) {
                mDriverButton.setSelected(true);
            } else {
                mPassengerButton.setSelected(true);
            }
        } catch (Exception e) {
            Log.e(TAG, "Caught exception");
            mPassengerButton.callOnClick();
        }

        final Intent SessionIntent = new Intent(this, SessionActivity.class);                           // session intent
        mSessionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(SessionIntent);
            }
        });
    }
}