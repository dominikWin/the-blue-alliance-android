package com.thebluealliance.androidclient.helpers;

import com.thebluealliance.androidclient.TbaLogger;
import com.thebluealliance.androidclient.Utilities;
import com.thebluealliance.androidclient.comparators.EventSortByDateComparator;
import com.thebluealliance.androidclient.comparators.EventSortByTypeAndDateComparator;
import com.thebluealliance.androidclient.eventbus.LiveEventUpdateEvent;
import com.thebluealliance.androidclient.models.Event;
import com.thebluealliance.androidclient.types.EventType;
import com.thebluealliance.androidclient.viewmodels.ListSectionHeaderViewModel;

import org.greenrobot.eventbus.EventBus;

import android.content.Context;

import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

public final class EventHelper {

    public static final String CHAMPIONSHIP_LABEL = "Championship Event";
    public static final String CITY_CHAMPIONSHIP_LABEL = "%1$s Championship";
    public static final String REGIONAL_LABEL = "Week %1$d";
    public static final String FLOAT_REGIONAL_LABEL = "Week %1$.1f";
    public static final String WEEKLESS_LABEL = "Other Official Events";
    public static final String OFFSEASON_LABEL = "%1$s Offseason Events";
    public static final String PRESEASON_LABEL = "Preseason Events";
    private static final Pattern EVENT_KEY_PATTERN = Pattern.compile("[a-zA-Z]+");

    private static final Pattern DISTRICT_EVENT_NAME_PATTERN = Pattern.compile("[A-Z]{2,3} District -(.+)");
    private static final Pattern EVENT_EVENT_NAME_PATTERN = Pattern.compile("(.+)Event");
    private static final Pattern REGIONAL_EVENT_NAME_PATTERN =
            Pattern.compile("\\s*(?:MAR |PNW |)(?:FIRST Robotics|FRC|)(.+)(?:(?:District|Regional|Region|State|Tournament|FRC|Field)\\b)");
    private static final Pattern FRC_EVENT_NAME_PATTERN = Pattern.compile("(.+)(?:FIRST Robotics|FRC)");

    private EventHelper() {
        // not used
    }

    public static boolean validateEventKey(String key) {
        if (key == null || key.isEmpty()) return false;
        return key.matches("^[1-9]\\d{3}[a-z,0-9]+$");
    }

    /**
     * Extracts a short name like "Silicon Valley" from an event name like "Silicon Valley Regional
     * sponsored by Google.org".
     * <p>
     * <p/>See <a href="https://github.com/the-blue-alliance/the-blue-alliance/blob/master/helpers/event_helper.py"
     * >the server's event_helper.py</a>.
     */
    public static String shortName(String eventName) {
        Matcher m1 = DISTRICT_EVENT_NAME_PATTERN.matcher(eventName); // XYZ District - NAME
        if (m1.matches()) {
            String partial = m1.group(1).trim();
            Matcher m2 = EVENT_EVENT_NAME_PATTERN.matcher(partial); // NAME Event...
            if (m2.lookingAt()) {
                return m2.group(1).trim();
            }
            return partial;
        }

        Matcher m3 = REGIONAL_EVENT_NAME_PATTERN.matcher(eventName); // ... NAME Regional...
        if (m3.lookingAt()) {
            String partial = m3.group(1);
            Matcher m4 = FRC_EVENT_NAME_PATTERN.matcher(partial); // NAME FIRST Robotics/FRC...
            if (m4.lookingAt()) {
                return m4.group(1).trim();
            } else {
                return partial.trim();
            }
        }

        return eventName.trim();
    }

    public static int getYear(String eventKey) {
        if (!EventHelper.validateEventKey(eventKey)) return -1;
        return Integer.parseInt(eventKey.substring(0, 4));
    }

    public static int getYearWeek(Date date) {
        if (date == null) return -1;
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(date.getTime());
        return cal.get(Calendar.WEEK_OF_YEAR);
    }

    public static int competitionWeek(Date date) {
        if (date == null) return -1;

        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DAY_OF_YEAR, -1);
        int week = getYearWeek(cal.getTime()) - Utilities.getFirstCompWeek(cal.get(Calendar.YEAR));
        return week < 0 ? 0 : week;
    }

    public static String generateLabelForEvent(Event e)  {
        switch (e.getEventTypeEnum()) {
            case CMP_DIVISION:
            case CMP_FINALS:
                if (e.getYear() >= 2017 && e.getCity() != null) {
                    // #2Champs
                    return String.format(CITY_CHAMPIONSHIP_LABEL, e.getCity());
                } else {
                    return CHAMPIONSHIP_LABEL;
                }
            case REGIONAL:
            case DISTRICT:
            case DISTRICT_CMP:
                /**
                 * Special cases for 2016:
                 * Week 1 is actually Week 0.5, everything else is one less
                 * See http://www.usfirst.org/roboticsprograms/frc/blog-The-Palmetto-Regional
                 */
                @Nullable Integer week = e.getWeek();
                if (e.getYear() == 2016) {
                    if (week == null) {
                        return String.format(REGIONAL_LABEL, 0);
                    } else if ("2016scmb".equals(e.getKey())) {
                        return String.format(FLOAT_REGIONAL_LABEL, 0.5);
                    } else {
                        return String.format(REGIONAL_LABEL, week - 1);
                    }
                } else if (week != null){
                    return String.format(REGIONAL_LABEL, week);
                } else {
                    return String.format(REGIONAL_LABEL, 0);
                }
            case OFFSEASON:
                String month = ThreadSafeFormatters.renderEventMonth(e.getFormattedStartDate());
                return String.format(OFFSEASON_LABEL, month);
            case PRESEASON:
                return PRESEASON_LABEL;
            default:
                return WEEKLESS_LABEL;
        }
    }

    public static String weekLabelFromNum(int year, @Nullable Integer weekNum) {

        if (weekNum == null) {
            return WEEKLESS_LABEL;
        }

        if (weekNum <= 0) {
            return PRESEASON_LABEL;
        }

        //let's find the week of CMP and base everything else off that
        //there should always be something in the CMP set for every year
        int cmpWeek = Utilities.getCmpWeek(year);

        /**
         * Special cases for 2016:
         * Week 1 is actually Week 0.5, everything else is one less
         * See http://www.usfirst.org/roboticsprograms/frc/blog-The-Palmetto-Regional
         */
        if (year == 2016 && weekNum == 1) {
            return String.format(FLOAT_REGIONAL_LABEL, 0.5);
        }
        if (year == 2016 && weekNum > 1 && weekNum < cmpWeek) {
            weekNum--;
        }

        if (weekNum > 0 && weekNum < cmpWeek) {
            return String.format(REGIONAL_LABEL, weekNum);
        }
        if (weekNum == cmpWeek) {
            return CHAMPIONSHIP_LABEL;
        }
        if (weekNum > cmpWeek) {
            return OFFSEASON_LABEL;
        }
        return WEEKLESS_LABEL;
    }

    public static int weekNumFromLabel(int year, String label) {
        for (int i = 0; i < 20; i++) {
            if (weekLabelFromNum(year, i).equals(label)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns a list of events sorted by start date and type. This is optimal for viewing a team's
     * season schedule.
     *
     * @param events a list of events to render
     * @param output list to render events into
     */
    public static void renderEventListForTeam(Context context,List<Event> events, List<Object> output) {
        renderEventListWithComparator(context, events, output, new EventSortByTypeAndDateComparator());
    }

    /**
     * Returns a list of events sorted by name and type. This is optimal for quickly finding a
     * particular event within a given week.
     *
     * @param events a list of events to render
     * @param output list to render events into
     */
    public static void renderEventListForWeek(Context context, List<Event> events, List<Object> output) {
        renderEventListWithComparator(context, events, output, new EventSortByTypeAndDateComparator());
    }

    private static void renderEventListWithComparator(
            Context context,
            List<Event> events,
            List<Object> output,
            Comparator<Event> comparator) {
        Collections.sort(events, comparator);
        EventType lastType = null, currentType = null;
        int lastDistrict = -1, currentDistrict = -1;
        for (Event event : events) {
            currentType = event.getEventTypeEnum();
            currentDistrict = event.getEventDistrictEnum() != null
                              ? event.getEventDistrictEnum().ordinal()
                              : -1;
            if (currentType != lastType
                || (currentType == EventType.DISTRICT && currentDistrict != lastDistrict)) {
                if (currentType == EventType.DISTRICT) {
                    output.add(new ListSectionHeaderViewModel(event.getEventDistrictString()
                                                              + " District Events"));
                } else {
                    output.add(new ListSectionHeaderViewModel(currentType.toString()));
                }
            }
            output.add(event.renderToViewModel(context, Event.RENDER_BASIC));

            if (event.isHappeningNow()) {
                //send out that there are live matches happening for other things to pick up
                TbaLogger.d("Sending live event broadcast: " + event.getKey());
                EventBus.getDefault().post(new LiveEventUpdateEvent(event));
            }

            lastType = currentType;
            lastDistrict = currentDistrict;
        }
    }

    public static void renderEventListForDistrict(
            Context context,
            List<Event> events,
            List<Object> output) {
        Collections.sort(events, new EventSortByDateComparator());
        String lastHeader = null, currentHeader = null;
        for (Event event : events) {
            currentHeader = weekLabelFromNum(event.getYear(), event.getWeek());
            if (!currentHeader.equals(lastHeader)) {
                output.add(new ListSectionHeaderViewModel(currentHeader + " Events"));
            }
            output.add(event.renderToViewModel(context, Event.RENDER_BASIC));

            if (event.isHappeningNow()) {
                //send out that there are live matches happening for other things to pick up
                TbaLogger.d("Sending live event broadcast: " + event.getKey());
                EventBus.getDefault().post(new LiveEventUpdateEvent(event));
            }
            lastHeader = currentHeader;
        }
    }

    public static String getDateString(Date startDate, Date endDate) {
        if (startDate == null || endDate == null) return "";
        if (startDate.equals(endDate)) {
            return ThreadSafeFormatters.renderEventDate(startDate);
        }
        return ThreadSafeFormatters.renderEventShortFormat(startDate) + " to "
                + ThreadSafeFormatters.renderEventDate(endDate);
    }


    public static String getShortCodeForEventKey(String eventKey) {
        if (validateEventKey(eventKey)) {
            return eventKey.replaceAll("[0-9]+", "");
        } else {
            return eventKey;
        }
    }

    /**
     * Returns an abbreviated event or district code like "CALB" from a match key like
     * "2014calb_qm17" or event key like "2014necmp" or district key like "2014pnw". Returns "" if
     * the argument doesn't parse as containing an event/district code.
     */
    public static String getEventCode(String matchOrEventOrDistrictKey) {
        Matcher m = EVENT_KEY_PATTERN.matcher(matchOrEventOrDistrictKey);

        return m.find() ? m.group().toUpperCase(Locale.US) : "";
    }

}
