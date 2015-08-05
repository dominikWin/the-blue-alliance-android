package com.thebluealliance.androidclient.fragments.event;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.thebluealliance.androidclient.Constants;
import com.thebluealliance.androidclient.activities.TeamAtEventActivity;
import com.thebluealliance.androidclient.adapters.ListViewAdapter;
import com.thebluealliance.androidclient.fragments.ListvVewFragment;
import com.thebluealliance.androidclient.helpers.AnalyticsHelper;
import com.thebluealliance.androidclient.helpers.EventTeamHelper;
import com.thebluealliance.androidclient.listitems.ListElement;
import com.thebluealliance.androidclient.models.Team;
import com.thebluealliance.androidclient.subscribers.TeamListSubscriber;

import java.util.List;

import rx.Observable;

public class EventTeamsFragment extends ListvVewFragment<List<Team>, TeamListSubscriber> {

    private static final String KEY = "event_key";

    private String mEventKey;

    public static EventTeamsFragment newInstance(String eventKey) {
        EventTeamsFragment f = new EventTeamsFragment();
        Bundle data = new Bundle();
        data.putString(KEY, eventKey);
        f.setArguments(data);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (getArguments() != null) {
            mEventKey = getArguments().getString(KEY, "");
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        mListView.setOnItemClickListener((adapterView, view1, position, id) -> {
            Log.d(Constants.LOG_TAG, "Team clicked!");
            String teamKey = ((ListElement) ((ListViewAdapter) adapterView.getAdapter()).getItem(position)).getKey();
            Intent intent = TeamAtEventActivity.newInstance(getActivity(), mEventKey, teamKey);

            /* Track the call */
            AnalyticsHelper.sendClickUpdate(getActivity(), "team@event_click", "EventTeamsFragment", EventTeamHelper.generateKey(mEventKey, teamKey));

            startActivity(intent);
        });
        return view;
    }

    @Override
    protected void inject() {
        mComponent.inject(this);
    }

    @Override
    protected Observable<List<Team>> getObservable() {
        return mDatafeed.fetchEventTeams(mEventKey);
    }
}
