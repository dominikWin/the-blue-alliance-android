package com.thebluealliance.androidclient.subscribers;

import com.google.gson.JsonObject;

import com.thebluealliance.androidclient.binders.MatchBreakdownBinder;
import com.thebluealliance.androidclient.models.BasicModel;
import com.thebluealliance.androidclient.models.Match;

public class MatchBreakdownSubscriber extends BaseAPISubscriber<Match, MatchBreakdownBinder.Model> {

    @Override
    public void parseData() throws BasicModel.FieldNotDefinedException {
        if (mAPIData.getYear() == 2016) {
            // Currently only support 2016 matches
            try {
                JsonObject scoreBreakdown = mAPIData.getBreakdown();
                JsonObject alliances = mAPIData.getAlliances();
                if (scoreBreakdown.entrySet().isEmpty() || alliances.entrySet().isEmpty()) {
                    mDataToBind = null;
                }

                mDataToBind = new MatchBreakdownBinder.Model(alliances, scoreBreakdown);
            } catch (BasicModel.FieldNotDefinedException ex) {
                // Match is unplayed or doesn't have a breakdown. Fail gracefully
                mDataToBind = null;
            }
        } else {
            mDataToBind = null;
        }
    }
}
