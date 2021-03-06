package com.mistapp.mistandroid;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;

public class WelcomeActivity extends AppCompatActivity implements View.OnTouchListener{

    private static final String TAG = RegisterAuth.class.getSimpleName();

    //changed when the activity changes states - onPause() and onResume(). Used to correctly show notifications
    public static boolean isInForeground;

    TextView studentText;
    TextView coachText;
    TextView guestText;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private CacheHandler cacheHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);



        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.app_package_name), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        cacheHandler = CacheHandler.getInstance(getApplication(), sharedPref, editor);

        studentText = (TextView)findViewById(R.id.student);
        coachText = (TextView)findViewById(R.id.coach);
        guestText = (TextView)findViewById(R.id.guest);

        mAuth = FirebaseAuth.getInstance();

        //NOTIFICATION SUBSCRIPTION: everyone gets subscribed to "user"
        FirebaseMessaging.getInstance().subscribeToTopic("user");
        FirebaseMessaging.getInstance().subscribeToTopic("android");
        Log.d(TAG, "Subscribing to android as well");
        //gets the user's token
        String token = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "User's token: " + token);

        //User is signed in or not already
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();

                //set to true if intent extra contains key=title (happens when a notification is received)
                boolean arrivingFromNotificaiton = false;
                Intent prevIntent = getIntent();
                Bundle bundle = prevIntent.getExtras();
                if (bundle != null) {
                    if (bundle.containsKey("title")){
                        arrivingFromNotificaiton = true;
                    }
                }

                if (user != null) {

                    String json = cacheHandler.getUserJson();
                    Log.d(TAG, "current user: " + json);

                    //error catching -> if for some reason, If shared preferences are null, log user out, and delete topics
                    //even if the user is still logged in
                    if (json == null) {
                        FirebaseAuth.getInstance().signOut();
                        cacheHandler.removeCachedTeammates();
                        cacheHandler.removeCachedNotificationFields();
                        cacheHandler.removeCachedUserFields();
                        Log.d(TAG,"SIGNING OUTTTT");
                        return;
                    }

                    String userType = cacheHandler.getUserType();

                    // put uid in cache
                    cacheHandler.cacheUserUid(user.getUid());
                    cacheHandler.commitToCache();
                    Intent intent = new Intent(getApplicationContext(), MyMistActivity.class);
                    intent.putExtra(getString(R.string.user_uid_key), user.getUid());
                    if (arrivingFromNotificaiton){
                        intent.putExtra(getString(R.string.received_notification), getString(R.string.received_notification));
                    }
                    startActivity(intent);
                } else {
                    Log.d(TAG, "NOT FAIL");
                    // if user is guest, go to guest's page
                    if (cacheHandler.getUserType().equals("guest")){
                        //if we got to this page because guest clicked sign-in
                        if (getIntent().hasExtra("guest_sign_in")){
//                        if (getIntent().getExtras().containsKey("guest_sign_in")){
                            Log.d(TAG, "guest has clicked sign in");
                            //do nothing - stay on welcome activity page
                        }
                        //if we got to the page because use opened app, go to my mist activity page
                        else{
                            Intent intent = new Intent(getApplicationContext(), MyMistActivity.class);
                            intent.putExtra(getString(R.string.current_user_type), "guest");
                            if (arrivingFromNotificaiton){
                                intent.putExtra(getString(R.string.received_notification), getString(R.string.received_notification));
                            }
                            startActivity(intent);
                        }

                    }
                    else{
                        Log.d(TAG, "FAIL");
                    }
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
            }
        };

    }


    @Override
    protected void onStart(){
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
        studentText.setOnTouchListener(this);
        coachText.setOnTouchListener(this);
        guestText.setOnTouchListener(this);
    }
    /**
     * End Firebase authentication
     */
    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        //Any time the activity is started from a hidden state, check for updates in notifications
        isInForeground =true;
    }

    @Override
    public void onPause() {
        super.onPause();
        isInForeground = false;
    }

    /**
     * When the register button is clicked, we check if the filled out form is valid
     * @param view
     */
    @Override
    public boolean onTouch(View view, MotionEvent event) {

        //pressed any of the three options
        if (view == studentText || view == coachText || view == guestText) {
            if (event.ACTION_UP == 1) {
                Intent intent;
                if (view == guestText){
                    cacheHandler.cacheUserType("guest");
                    cacheHandler.commitToCache();
                    Log.d(TAG, "guest was pressed - going to Guest mist activity");
                    intent = new Intent(this, MyMistActivity.class);
                    intent.putExtra(getString(R.string.current_user_type), "guest");
                    FirebaseMessaging.getInstance().subscribeToTopic("guest");
                }
                else{
                    intent = new Intent(this, LogInAuth.class);
                }

                startActivity(intent);
                view.setOnTouchListener(null);
            }
        }
        return true;

    }

    @Override
    public void onBackPressed() {
        Log.d("~~~~~~~~~~~`", "back, has been pressed from welcome page- nothing should happen!");

    }

}