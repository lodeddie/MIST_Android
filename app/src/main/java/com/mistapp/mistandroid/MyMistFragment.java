package com.mistapp.mistandroid;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;
import com.mistapp.mistandroid.model.Coach;
import com.mistapp.mistandroid.model.Competitor;
import com.roughike.bottombar.BottomBar;

public class MyMistFragment extends Fragment{

    private static final String TAG = LogInAuth.class.getSimpleName();
    FragmentTransaction transaction;
    private CacheHandler cacheHandler;



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getActivity().setTitle(getResources().getString(R.string.my_mist_page_title));
        setHasOptionsMenu(true);

        View view = inflater.inflate(R.layout.fragment_my_mist, container, false);

        SharedPreferences sharedPref = getActivity().getSharedPreferences(getString(R.string.app_package_name), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        cacheHandler = CacheHandler.getInstance(getActivity().getApplication(), sharedPref, editor);

        RadioGroup group = (RadioGroup)view.findViewById(R.id.radioGroup);
        final RadioButton myTeamButton = (RadioButton) view.findViewById(R.id.my_team_button);
        final RadioButton myScheduleButton = (RadioButton) view.findViewById(R.id.my_schedule_button);
        final MyTeamFragment myTeamFragment = new MyTeamFragment();
        final MyScheduleFragment myScheduleFragment = new MyScheduleFragment();

        if (cacheHandler.getUserType().equals("coach")){
            group.setVisibility(View.GONE);
        }

        showMyTeamFragment(myTeamFragment);

        Log.d(TAG, "Mah Niga");
        myTeamButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                final int version = Build.VERSION.SDK_INT;

                if (isChecked == true){
                    showMyTeamFragment(myTeamFragment);
                    if (version >= 23) {
                        myTeamButton.setTextColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.mistGray));
                    } else {
                        myTeamButton.setTextColor(getResources().getColor(R.color.mistGray));
                    }
                }
                else{
                    if (version >= 23) {
                        myTeamButton.setTextColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.mistRed));
                    } else {
                        myTeamButton.setTextColor(getResources().getColor(R.color.mistRed));
                    }
                }
            }
        });
        myScheduleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                final int version = Build.VERSION.SDK_INT;
                Log.d("123", version+" ~~~~~~~~~~~~~~~`");
                if (isChecked == true) {
                    showMyScheduleFragment(myScheduleFragment);
                    if (version >= 23) {
                        myScheduleButton.setTextColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.mistGray));
                    } else {
                        myScheduleButton.setTextColor(getResources().getColor(R.color.mistGray));
                    }
                }
                else{
                    if (version >= 23) {
                        myScheduleButton.setTextColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.mistRed));
                    } else {
                        myScheduleButton.setTextColor(getResources().getColor(R.color.mistRed));
                    }
                }
            }
        });
        return view;
    }

    public void showMyTeamFragment(MyTeamFragment fragment){
        transaction = getActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.my_mist_frame_layout, fragment, "myteam");
        //dont want to add this fragment to the backstack- will never hit back to go to these
        transaction.commit();
    }

    public void showMyScheduleFragment(MyScheduleFragment fragment){
        transaction = getActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.my_mist_frame_layout, fragment, "myschedule");
        //dont want to add this fragment to the backstack- will never hit back to go to these
        transaction.commit();
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        //adding signout text to actionbar
        inflater.inflate(R.menu.actionbar_menu_nonguest, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle item selection
        switch (item.getItemId()) {
            case R.id.signout_item:
                FirebaseAuth.getInstance().signOut();
                Toast.makeText(getActivity(), "peace out ", Toast.LENGTH_LONG).show();

                Gson gson = new Gson();
                String json = cacheHandler.getUserJson();
                String currentUserType = cacheHandler.getUserType();

                //remove user, user uid, user's type, teammates, and notifications from the cache
                cacheHandler.removeCachedUserFields();
                cacheHandler.removeCachedNotificationFields();
                cacheHandler.removeCachedEvents();
                cacheHandler.removeCachedTeammates();
                cacheHandler.commitToCache();

                //unsibscribing from topics that were previously subscribed to when logged in
                if (currentUserType.equals("coach")){
                    Coach currentUser = gson.fromJson(json, Coach.class);
                    unSubscribeFromCoachTopics(currentUser);
                }
                else if(currentUserType.equals("competitor")){
                    Competitor currentUser = gson.fromJson(json, Competitor.class);
                    unSubscribeFromCompetitorTopics(currentUser);
                }

                Intent intent = new Intent(getActivity(), WelcomeActivity.class);
                startActivity(intent);
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    //subscribe to team name, competitions, and "competitor"
    public void unSubscribeFromCompetitorTopics(Competitor currentUser){

        //subscribe to the current user's team name (replaces spaces with underscores in team name)
        String teamName = currentUser.getTeam();
        String underScoreTeamName = teamName.replaceAll(" ", "_");
        FirebaseMessaging.getInstance().unsubscribeFromTopic(underScoreTeamName);

        //subscribe to the current user type
        FirebaseMessaging.getInstance().unsubscribeFromTopic("competitor");

        String[] compArray = {
                (currentUser).getGroupProject(),
                (currentUser).getArt(),
                (currentUser).getSports(),
                (currentUser).getBrackets(),
                (currentUser).getWriting(),
                (currentUser).getKnowledge()
        };

        //subscribe to user's competitions (replaces spaces with underscores in competition name)
        for (String competition: compArray){
            if (!competition.equals("")) {
                String underScoreCompName = competition.replaceAll(" ", "_");
                underScoreCompName = underScoreCompName.replaceAll("'", "_");
                underScoreCompName = underScoreCompName.replaceAll("/", "_");
                FirebaseMessaging.getInstance().unsubscribeFromTopic(underScoreCompName);
            }
        }
    }

    //suscribes to team name and "coach"
    public void unSubscribeFromCoachTopics(Coach currentCoach){

        //subscribe to the current user's team name (replaces spaces with underscores in team name)
        String teamName = currentCoach.getTeam();
        String underScoreTeamName = teamName.replaceAll(" ", "_");
        FirebaseMessaging.getInstance().unsubscribeFromTopic(underScoreTeamName);

        //subscribe to the current user type
        FirebaseMessaging.getInstance().unsubscribeFromTopic("coach");

    }

    @Override
    public void onResume(){
        super.onResume();
        BottomBar bbar = ((MyMistActivity)getActivity()).getBottomBar();
        bbar.selectTabAtPosition(2);
    }

}