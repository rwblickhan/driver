package com.android.ubclaunchpad.driver.session;

import android.location.Location;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.ubclaunchpad.driver.R;

import com.android.ubclaunchpad.driver.routingAlgorithms.FindBestRouteAlgorithm;
import com.android.ubclaunchpad.driver.session.models.SessionModel;
import com.android.ubclaunchpad.driver.user.User;
import com.android.ubclaunchpad.driver.user.UserManager;


import com.android.ubclaunchpad.driver.util.FirebaseUtils;
import com.android.ubclaunchpad.driver.util.StringUtils;

import com.google.android.gms.maps.model.LatLng;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class SessionInfoActivity extends AppCompatActivity {

    private static final String TAG = "SessionInfoActivity";
    final String passengerDistance = "\nP\n\t\t\t\t";
    final String driverDistance = "\nD\n\t\t\t\t";
    final ArrayList<String> itemsArray = new ArrayList<String>();
    private DatabaseReference session;
    // private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_info);

        final ListView listView = (ListView) findViewById(R.id.sessionItemsList);
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, android.R.id.text1, itemsArray);
        listView.setAdapter(adapter);
        final String sessionName = getIntent().getStringExtra(StringUtils.SESSION_NAME);
        TextView textViewSessionName = (TextView) findViewById(R.id.viewSessionName);
        textViewSessionName.setText(sessionName);
        final Button goButton = (Button) findViewById(R.id.go_Button);
        session = FirebaseUtils.getDatabase()
                .child(StringUtils.FirebaseSessionEndpoint)
                .child(sessionName);
        final String UID = FirebaseUtils.getFirebaseUser().getUid();
        final User currentUser;
        try {
            currentUser = UserManager.getInstance().getUser();

            session.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    String uid = dataSnapshot.child("sessionHostUid").getValue(String.class);
                    if (uid.equals(FirebaseUtils.getFirebaseUser().getUid())) {
                        Toast.makeText(getBaseContext(), "You are the chosen one!", Toast.LENGTH_LONG).show();
                        goButton.setVisibility(View.VISIBLE);
                    }
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });

            //add current user's UID to the current session's driver or passenger list
            session.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    SessionModel updatedSession = dataSnapshot.getValue(SessionModel.class);
                    if (updatedSession == null) {
                        finish();
                        Toast.makeText(getBaseContext(), "Session no longer exists", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (currentUser.isDriver &&
                            !updatedSession.getDrivers().contains(UID)) {
                        updatedSession.addDriver(UID);
                        session.setValue(updatedSession);
                        Log.v(TAG, "new driver added");
                    }
                    if (!currentUser.isDriver &&
                            !updatedSession.getPassengers().contains(UID)) {
                        updatedSession.addPassenger(UID);
                        session.setValue(updatedSession);
                        Log.v(TAG, "new passenger added");
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }


        //listen to changes in Firebase to update the driver passenger info list
        //the displayed "driver" or "passenger" label is not dependent on the isDriver
        //field but the list the user is in
        session.child(StringUtils.FirebaseSessionDriverEndpoint)
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                        addUserToAdapter(dataSnapshot, adapter, true);
                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                    }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {
                        removeUserFromAdapter(dataSnapshot, adapter, true);
                    }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
        session.child(StringUtils.FirebaseSessionPassengerEndpoint)
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                        addUserToAdapter(dataSnapshot, adapter, false);
                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {
                        removeUserFromAdapter(dataSnapshot, adapter, false);
                    }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
        goButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Starting the goddamn algorithm
                List<User> users = retrieveUsersFromSession();
                LatLng startLocation = retrieveStartLocation();
                FindBestRouteAlgorithm algorithm = new FindBestRouteAlgorithm(startLocation);
            }
        });
    }


    private void addUserToAdapter(DataSnapshot dataSnapshot, final ArrayAdapter<String> adapter, final boolean inDriverList) {
        FirebaseUtils.getDatabase()
                .child(StringUtils.FirebaseUserEndpoint)
                .child(dataSnapshot.getValue().toString()) //get the added user's UID
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        User user = dataSnapshot.getValue(User.class);
                        if (user != null) {
                            String distance = inDriverList ? driverDistance : passengerDistance;
                            String username = distance + user.getName();
                            itemsArray.add(username);
                            adapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
        Log.d(TAG, "Child " + dataSnapshot.getValue() + " is added to " + (inDriverList ? "drivers" : "passengers"));
    }

    private void removeUserFromAdapter(DataSnapshot dataSnapshot, final ArrayAdapter<String> adapter, final boolean inDriverList) {
        final String removedUID = dataSnapshot.getValue().toString();
        FirebaseUtils.getDatabase().child(StringUtils.FirebaseUserEndpoint)
                .child(removedUID)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        User user = dataSnapshot.getValue(User.class);
                        if (user != null) {
                            String distance = inDriverList ? driverDistance : passengerDistance;
                            itemsArray.remove(distance + user.getName());
                            adapter.notifyDataSetChanged();
                            Log.d(TAG, removedUID + " is removed from" + (inDriverList ? "drivers" : "passengers"));
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
    }

    /**
     * Retrieve all drivers and passengers from the current session
     * @return ArrayList of all Users in the current session
     */
    private List<User> retrieveUsersFromSession() {
        final List<User> users = new ArrayList<>();
        final DatabaseReference usersReference = FirebaseUtils.getDatabase().child("Users");
        // Getting reference to the users in Firebase
        usersReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot dataSnapshotUsers) {
                // Getting reference to the drivers in current session
                session.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshotDrivers) {
                        for (DataSnapshot driver : dataSnapshotDrivers.child("drivers").getChildren()) {
                            String driverUid = driver.getValue(String.class);
                            User uDriver = dataSnapshotUsers.child(driverUid).getValue(User.class);
                            uDriver.setIsDriver(true);
                            users.add(uDriver);
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
                // Getting reference to the passengers in current session
                session.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshotDrivers) {
                        for (DataSnapshot driver : dataSnapshotDrivers.child("passengers").getChildren()) {
                            String passengerUid = driver.getValue(String.class);
                            User uPassenger = dataSnapshotUsers.child(passengerUid).getValue(User.class);
                            users.add(uPassenger);
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
        return users;
    }
    /**
     * Get the start location for the current session
     * @return LatLng of the location of the start
     */
    private LatLng retrieveStartLocation() {
        // It is so ugly for a reason
        final Location startLocation = new Location("");
        session.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String location = dataSnapshot.child("location").getValue(String.class);
                LatLng data = StringUtils.stringToLatLng(location);
                startLocation.setLatitude(data.latitude);
                startLocation.setLongitude(data.longitude);
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
        return new LatLng(startLocation.getLatitude(), startLocation.getLongitude());
    }
}