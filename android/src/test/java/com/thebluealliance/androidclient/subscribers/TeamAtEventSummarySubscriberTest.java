package com.thebluealliance.androidclient.subscribers;

import android.content.res.Resources;

import com.google.gson.JsonArray;
import com.thebluealliance.androidclient.datafeed.framework.DatafeedTestDriver;
import com.thebluealliance.androidclient.datafeed.framework.ModelMaker;
import com.thebluealliance.androidclient.eventbus.EventMatchesEvent;
import com.thebluealliance.androidclient.models.BasicModel;
import com.thebluealliance.androidclient.models.Event;
import com.thebluealliance.androidclient.subscribers.TeamAtEventSummarySubscriber.Model;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class TeamAtEventSummarySubscriberTest {
    @Mock Resources mResources;
    @Mock Event mEvent;
    @Mock EventMatchesEvent mMatchesEvent;

    TeamAtEventSummarySubscriber mSubscriber;
    Model mData;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mResources.getString(anyInt())).thenReturn("");
        mSubscriber = new TeamAtEventSummarySubscriber(mResources);
        mSubscriber.setTeamKey("frc1519");
        mData = new Model(
          ModelMaker.getModel(JsonArray.class, "2015necmp_rankings"),
          ModelMaker.getModel(Event.class, "2015necmp"));
    }

    @Test
    public void testParseNullData() throws BasicModel.FieldNotDefinedException {
        DatafeedTestDriver.parseNullData(mSubscriber);
    }

    @Test
    public void testSimpleParsing() throws BasicModel.FieldNotDefinedException {
        mSubscriber.onEventAsync(mMatchesEvent);
        DatafeedTestDriver.testSimpleParsing(mSubscriber, mData);
    }
}