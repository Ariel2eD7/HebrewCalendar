        package com.israel.hebrewcalendar;

import android.content.Context;
import android.content.Intent;
import android.graphics.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.icu.util.HebrewCalendar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class HebrewCalendarFragment extends Fragment {

    private CalendarTableView calendarView;

    private FrameLayout calendarContainer;
    private FrameLayout calendarViewport;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private final Map<String, List<CalendarEntry>> calendarEntries =
            new HashMap<>();


    public HebrewCalendarFragment() {
        super(R.layout.fragment_calendar);
    }


    @Override
    public void onResume() {
        super.onResume();

        if (mAuth != null) {
            loadCalendarEntries();
        }
    }


    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);


        mAuth = FirebaseAuth.getInstance();

        db = FirebaseFirestore.getInstance();


        calendarViewport =
                view.findViewById(R.id.calendarViewport);


        calendarContainer =
                view.findViewById(R.id.calendarContainer);


        Spinner yearSpinner =
                view.findViewById(R.id.yearSpinner);


        Button logoutButton =
                view.findViewById(R.id.logoutButton);


        logoutButton.setOnClickListener(v -> {

            mAuth.signOut();

            Intent intent =
                    new Intent(
                            requireContext(),
                            LoginActivity.class
                    );

            intent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
            );

            startActivity(intent);
        });


        /*
         * ============================================================
         * יצירת הלוח
         * ============================================================
         */

        calendarView =
                new CalendarTableView(requireContext());


        calendarContainer.removeAllViews();


        calendarContainer.addView(
                calendarView,
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );


        /*
         * ============================================================
         * שנים לבחירה
         * ============================================================
         */

        Integer[] years =
                new Integer[21];


        for (int i = 0;
             i < years.length;
             i++) {

            years[i] = 2020 + i;
        }


        ArrayAdapter<Integer> adapter =
                new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        years
                );


        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );


        yearSpinner.setAdapter(adapter);


        /*
         * ============================================================
         * פתיחה תמיד על 2025
         *
         * 2020 הוא position 0
         * לכן 2025 הוא position 5.
         * ============================================================
         */

        final int DEFAULT_YEAR = 2025;

        int defaultPosition = 0;

        for (int i = 0; i < years.length; i++) {

            if (years[i] == DEFAULT_YEAR) {

                defaultPosition = i;

                break;
            }
        }


        /*
         * מגדירים קודם את השנה בלוח.
         */
        calendarView.selectedYear =
                DEFAULT_YEAR;


        /*
         * לאחר שה-Spinner מוגדר,
         * בוחרים בו את 2025.
         */
        yearSpinner.setSelection(
                defaultPosition,
                false
        );


        yearSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id) {

                        Integer selected =
                                (Integer)
                                        parent.getItemAtPosition(
                                                position
                                        );


                        if (selected == null) {
                            return;
                        }


                        calendarView.selectedYear =
                                selected;


                        calendarView.invalidate();


                        /*
                         * אם עברנו ל-2025,
                         * מחזירים את הלוח אל היום הנוכחי
                         * אם הוא נמצא בתוך 2025.
                         */
                        if (selected == 2025) {

                            calendarView.post(() ->
                                    calendarView.focusOnToday()
                            );
                        }
                    }


                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent) {
                    }
                }
        );


        /*
         * ============================================================
         * טעינת הנתונים
         * ============================================================
         */

        loadCalendarEntries();


        /*
         * ============================================================
         * לאחר שה-View קיבל גודל אמיתי:
         *
         * פותחים על 2025
         * ומנסים להתמקד ביום הנוכחי.
         *
         * אם היום אינו ב-2025,
         * הלוח נשאר במיקום ההתחלתי.
         * ============================================================
         */

        calendarView.post(() -> {

            calendarView.selectedYear =
                    DEFAULT_YEAR;

            calendarView.focusOnToday();
        });
    }



    private void loadCalendarEntries() {

        FirebaseUser currentUser =
                mAuth.getCurrentUser();


        if (currentUser == null) {
            return;
        }


        String userId =
                currentUser.getUid();


        db.collection("users")
                .document(userId)
                .collection("calendarEntries")
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    calendarEntries.clear();


                    for (DocumentSnapshot document :
                            querySnapshot.getDocuments()) {

                        /*
                         * ====================================================
                         * אירוע חדש
                         *
                         * startDate / endDate
                         *
                         * לדוגמה:
                         *
                         * startDate = 2026-10-01
                         * endDate   = 2026-10-14
                         * ====================================================
                         */

                        String startDate =
                                document.getString(
                                        "startDate"
                                );


                        String endDate =
                                document.getString(
                                        "endDate"
                                );


                        /*
                         * ====================================================
                         * תמיכה בנתונים הישנים
                         *
                         * אם קיים מסמך ישן שיש בו רק
                         * gregorianDate, עדיין נציג אותו.
                         * ====================================================
                         */

                        if ((startDate == null ||
                                startDate.trim().isEmpty()) &&
                                (endDate == null ||
                                        endDate.trim().isEmpty())) {

                            String oldDate =
                                    document.getString(
                                            "gregorianDate"
                                    );


                            if (oldDate == null ||
                                    oldDate.trim().isEmpty()) {

                                continue;
                            }


                            /*
                             * ממירים dd/MM/yyyy
                             * ל-yyyy-MM-dd.
                             */

                            startDate =
                                    convertOldDateToNewFormat(
                                            oldDate
                                    );


                            endDate =
                                    startDate;
                        }


                        if (startDate == null ||
                                endDate == null) {

                            continue;
                        }


                        /*
                         * ====================================================
                         * יצירת האובייקט
                         * ====================================================
                         */

                        CalendarEntry entry =
                                new CalendarEntry();


                        entry.id =
                                document.getId();


                        entry.startDate =
                                startDate;


                        entry.endDate =
                                endDate;


                        entry.hebrewDate =
                                document.getString(
                                        "hebrewDate"
                                );


                        entry.type =
                                document.getString(
                                        "type"
                                );


                        entry.title =
                                document.getString(
                                        "title"
                                );


                        entry.startTime =
                                document.getString(
                                        "startTime"
                                );


                        entry.endTime =
                                document.getString(
                                        "endTime"
                                );


                        Boolean allDay =
                                document.getBoolean(
                                        "allDay"
                                );


                        entry.allDay =
                                allDay != null &&
                                        allDay;


                        entry.location =
                                document.getString(
                                        "location"
                                );


                        entry.link =
                                document.getString(
                                        "link"
                                );


                        entry.reminder =
                                document.getString(
                                        "reminder"
                                );


                        entry.description =
                                document.getString(
                                        "description"
                                );


                        /*
                         * ====================================================
                         * מוסיפים את האירוע לכל יום שנמצא בטווח.
                         *
                         * לדוגמה:
                         *
                         * 01/10 → 14/10
                         *
                         * האירוע ייכנס ל-14 מפתחות שונים.
                         * ====================================================
                         */

                        addEntryToDateRange(
                                entry
                        );
                    }


                    /*
                     * רענון הלוח.
                     */

                    if (calendarView != null) {

                        calendarView.invalidate();
                    }
                })
                .addOnFailureListener(e -> {

                    if (!isAdded()) {
                        return;
                    }


                    Toast.makeText(
                            requireContext(),
                            "טעינת הנתונים נכשלה: "
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });

    }

    /*

     * ================================================================
     * הוספת אירוע לכל הימים בטווח
     * ================================================================
     */

    private void addEntryToDateRange(
            CalendarEntry entry) {

        if (entry == null ||
                entry.startDate == null ||
                entry.endDate == null) {

            return;
        }


        Calendar start =
                parseDate(
                        entry.startDate
                );


        Calendar end =
                parseDate(
                        entry.endDate
                );


        if (start == null ||
                end == null) {

            return;
        }


        /*
         * מוודאים שאין שעות שמפריעות
         * להשוואת התאריכים.
         */

        normalizeDate(
                start
        );


        normalizeDate(
                end
        );


        /*
         * אם מסיבה כלשהי הסיום לפני ההתחלה,
         * לא מציגים טווח הפוך.
         */

        if (end.before(start)) {
            return;
        }


        /*
         * עוברים יום-יום.
         */

        Calendar current =
                (Calendar) start.clone();


        while (!current.after(end)) {

            String dateKey =
                    formatDateKey(
                            current
                    );


            if (!calendarEntries.containsKey(
                    dateKey
            )) {

                calendarEntries.put(
                        dateKey,
                        new ArrayList<>()
                );
            }


            calendarEntries
                    .get(dateKey)
                    .add(entry);


            current.add(
                    Calendar.DAY_OF_MONTH,
                    1
            );
        }

    }

    /*

     * ================================================================
     * המרת תאריך ישן:
     *
     * dd/MM/yyyy
     *
     * ל:
     *
     * yyyy-MM-dd
     * ================================================================
     */

    private String convertOldDateToNewFormat(
            String oldDate) {

        if (oldDate == null ||
                oldDate.trim().isEmpty()) {

            return null;
        }


        try {

            java.text.SimpleDateFormat oldFormat =
                    new java.text.SimpleDateFormat(
                            "dd/MM/yyyy",
                            Locale.US
                    );


            oldFormat.setLenient(false);


            java.util.Date date =
                    oldFormat.parse(
                            oldDate
                    );


            if (date == null) {
                return null;
            }


            java.text.SimpleDateFormat newFormat =
                    new java.text.SimpleDateFormat(
                            "yyyy-MM-dd",
                            Locale.US
                    );


            return newFormat.format(
                    date
            );

        } catch (Exception e) {

            return null;
        }


    }

    /*

     * ================================================================
     * יצירת Calendar מתוך yyyy-MM-dd
     * ================================================================
     */

    private Calendar parseDate(
            String value) {

        if (value == null ||
                value.trim().isEmpty()) {

            return null;
        }


        try {

            java.text.SimpleDateFormat format =
                    new java.text.SimpleDateFormat(
                            "yyyy-MM-dd",
                            Locale.US
                    );


            format.setLenient(false);


            java.util.Date date =
                    format.parse(
                            value
                    );


            if (date == null) {
                return null;
            }


            Calendar calendar =
                    Calendar.getInstance();


            calendar.setTime(
                    date
            );


            return calendar;

        } catch (Exception e) {

            return null;
        }


    }

    /*

     * ================================================================
     * איפוס שעה
     * ================================================================
     */

    private void normalizeDate(
            Calendar calendar) {

        calendar.set(
                Calendar.HOUR_OF_DAY,
                0
        );

        calendar.set(
                Calendar.MINUTE,
                0
        );

        calendar.set(
                Calendar.SECOND,
                0
        );

        calendar.set(
                Calendar.MILLISECOND,
                0
        );

    }

    /*

     * ================================================================
     * מפתח תאריך ללוח
     *
     * yyyy-MM-dd
     * ================================================================
     */

    private String formatDateKey(
            Calendar calendar) {


        return String.format(
                Locale.US,
                "%04d-%02d-%02d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH)
        );

    }




    /*
     * ============================================================
     * מודל אירוע
     * ============================================================
     */


    public static class CalendarEntry {


        String id;

        String startDate;
        String endDate;

        String hebrewDate;

        String type;

        String title;

        String startTime;
        String endTime;

        boolean allDay;

        String location;

        String link;

        String reminder;

        String description;

    }




    /*
     * ============================================================
     * הלוח
     * ============================================================
     */

    private class CalendarTableView extends View {

        private final float density;


        private final int DAYS = 37;

        private final int DAY_WIDTH;
        private final int ROW_HEIGHT;
        private final int MONTH_WIDTH;
        private final int YEAR_WIDTH;

        private final int MONTH_HEIGHT;

        private final int MONTHS = 13;

        private final int TABLE_WIDTH;
        private final int TABLE_HEIGHT;


        private int selectedYear = 2025;


        private final int START_MONTH =
                Calendar.DECEMBER;


        /*
         * ========================================================
         * יום נוכחי
         * ========================================================
         */

        private boolean isToday(
                int year,
                int month,
                int day) {

            Calendar today =
                    Calendar.getInstance();


            return today.get(Calendar.YEAR) == year
                    && today.get(Calendar.MONTH) == month
                    && today.get(Calendar.DAY_OF_MONTH) == day;
        }


        /*
         * ========================================================
         * זום
         * ========================================================
         */

        private float scaleFactor = 1f;

        private final float MAX_SCALE = 1.3f;


        private float getMinScale() {

            if (getWidth() <= 0 ||
                    getHeight() <= 0) {

                return 0.25f;
            }


            float scaleX =
                    getWidth() /
                            (float) TABLE_WIDTH;


            float scaleY =
                    getHeight() /
                            (float) TABLE_HEIGHT;


            return Math.min(
                    scaleX,
                    scaleY
            );
        }

        private void drawTodayBorder(
                Canvas canvas,
                int year,
                int month,
                int days,
                float top,
                int empty) {

            Calendar today =
                    Calendar.getInstance();


            int todayYear =
                    today.get(Calendar.YEAR);

            int todayMonth =
                    today.get(Calendar.MONTH);

            int todayDay =
                    today.get(Calendar.DAY_OF_MONTH);


            /*
             * האם החודש שאנחנו מציירים
             * הוא החודש הנוכחי?
             */
            if (year != todayYear ||
                    month != todayMonth) {

                return;
            }


            /*
             * מיקום היום בתוך השורה.
             */
            int position =
                    empty +
                            todayDay -
                            1;


            if (position < 0 ||
                    position >= DAYS) {

                return;
            }


            /*
             * בגלל שהלוח מצויר מימין לשמאל:
             */
            float x =
                    (DAYS - position - 1)
                            * DAY_WIDTH;


            /*
             * המסגרת מקיפה את כל 7 השורות
             * של אותו יום.
             */
            float y =
                    top;


            float right =
                    x +
                            DAY_WIDTH;


            float bottom =
                    top +
                            MONTH_HEIGHT;


            /*
             * מסגרת בולטת וברורה.
             */
            border.setStyle(
                    Paint.Style.STROKE
            );


            border.setColor(
                    Color.rgb(
                            25,
                            118,
                            210
                    )
            );


            border.setStrokeWidth(
                    dp(3)
            );


            /*
             * מעט שקיפות כדי שהצבעים
             * שבתוך היום עדיין יישארו נראים.
             */
            border.setAlpha(255);


            canvas.drawRect(
                    x + dp(2),
                    y + dp(2),
                    right - dp(2),
                    bottom - dp(2),
                    border
            );
        }




        private final ScaleGestureDetector scaleDetector;


        /*
         * ========================================================
         * Frozen columns
         * ========================================================
         */

        private float getFrozenWidth() {

            return (MONTH_WIDTH + YEAR_WIDTH)
                    * scaleFactor;
        }


        private float getScrollableWidth() {

            return DAYS
                    * DAY_WIDTH
                    * scaleFactor;
        }


        /*
         * ========================================================
         * Pan
         * ========================================================
         */

        private float panX = 0f;
        private float panY = 0f;


        private float lastTouchX;
        private float lastTouchY;


        private float downX;
        private float downY;


        private boolean moved;


        private static final int TOUCH_SLOP = 12;


        /*
         * ========================================================
         * צבעים
         * ========================================================
         */

        private final int COLOR_YELLOW =
                Color.rgb(255, 230, 153);


        private final int COLOR_BLUE =
                Color.rgb(189, 215, 238);


        private final int COLOR_PINK =
                Color.rgb(255, 153, 255);


        private final int COLOR_GREEN =
                Color.rgb(198, 224, 180);


        private final int COLOR_GRAY =
                Color.rgb(174, 170, 170);


        private final int COLOR_EVENT =
                Color.rgb(52, 120, 246);


        private final int COLOR_INCOME =
                Color.rgb(46, 160, 67);


        private final int COLOR_EXPENSE =
                Color.rgb(220, 53, 69);


        private final int COLOR_NOTE =
                Color.rgb(245, 180, 40);


        /*
         * צבע סימון היום
         */

        private final int COLOR_TODAY =
                Color.rgb(25, 118, 210);


        private final int COLOR_TODAY_BACKGROUND =
                Color.rgb(225, 240, 255);


        /*
         * ========================================================
         * Paint
         * ========================================================
         */

        private final Paint fill =
                new Paint(Paint.ANTI_ALIAS_FLAG);


        private final Paint line =
                new Paint(Paint.ANTI_ALIAS_FLAG);


        private final Paint text =
                new Paint(Paint.ANTI_ALIAS_FLAG);


        private final Paint border =
                new Paint(Paint.ANTI_ALIAS_FLAG);


        private final Paint todayPaint =
                new Paint(Paint.ANTI_ALIAS_FLAG);


        private final String[] englishMonths = {

                "Jan",
                "Feb",
                "Mar",
                "Apr",
                "May",
                "Jun",
                "Jul",
                "Aug",
                "Sep",
                "Oct",
                "Nov",
                "Dec"
        };


        private final String[] hebrewWeekDays = {

                "א",
                "ב",
                "ג",
                "ד",
                "ה",
                "ו",
                "ש"
        };


        @Override
        protected void onSizeChanged(
                int width,
                int height,
                int oldWidth,
                int oldHeight) {

            super.onSizeChanged(
                    width,
                    height,
                    oldWidth,
                    oldHeight
            );


            if (oldWidth == 0 &&
                    oldHeight == 0) {

                scaleFactor =
                        getMinScale();

                panX = 0;
                panY = 0;

                clampPan();
            }
        }


        /*
         * ========================================================
         * התמקדות ביום הנוכחי
         * ========================================================
         */


        private void focusOnToday() {

            Calendar today =
                    Calendar.getInstance();

            int todayYear =
                    today.get(Calendar.YEAR);

            int todayMonth =
                    today.get(Calendar.MONTH);

            int todayDay =
                    today.get(Calendar.DAY_OF_MONTH);


            /*
             * ========================================================
             * הלוח מתחיל בדצמבר של selectedYear
             * ומציג 13 חודשים.
             *
             * לדוגמה:
             *
             * 2025:
             * דצמבר 2025
             * ינואר 2026
             * ...
             * דצמבר 2026
             * ========================================================
             */

            Calendar calendarStart =
                    Calendar.getInstance();

            calendarStart.set(
                    selectedYear,
                    START_MONTH,
                    1
            );


            Calendar calendarEnd =
                    (Calendar) calendarStart.clone();

            calendarEnd.add(
                    Calendar.MONTH,
                    MONTHS
            );

            calendarEnd.add(
                    Calendar.DAY_OF_MONTH,
                    -1
            );


            Calendar todayDate =
                    Calendar.getInstance();

            todayDate.set(
                    todayYear,
                    todayMonth,
                    todayDay
            );


            /*
             * היום חייב להיות בתוך 13 החודשים
             * שמוצגים בלוח.
             */
            if (todayDate.before(calendarStart) ||
                    todayDate.after(calendarEnd)) {

                invalidate();

                return;
            }


            /*
             * ========================================================
             * מציאת החודש שבו נמצא היום.
             * ========================================================
             */

            Calendar monthStart =
                    Calendar.getInstance();

            monthStart.set(
                    selectedYear,
                    START_MONTH,
                    1
            );


            int monthIndex = 0;


            while (monthIndex < MONTHS) {

                Calendar currentMonth =
                        (Calendar) monthStart.clone();

                currentMonth.add(
                        Calendar.MONTH,
                        monthIndex
                );


                if (currentMonth.get(Calendar.YEAR)
                        == todayYear
                        &&
                        currentMonth.get(Calendar.MONTH)
                                == todayMonth) {

                    break;
                }


                monthIndex++;
            }


            if (monthIndex >= MONTHS) {

                invalidate();

                return;
            }


            /*
             * ========================================================
             * מציאת המיקום של היום בתוך החודש.
             * ========================================================
             */

            Calendar first =
                    Calendar.getInstance();

            first.set(
                    todayYear,
                    todayMonth,
                    1
            );


            int empty =
                    first.get(
                            Calendar.DAY_OF_WEEK
                    ) - 1;


            int position =
                    empty +
                            todayDay -
                            1;


            if (position < 0 ||
                    position >= DAYS) {

                invalidate();

                return;
            }


            /*
             * הלוח מצויר מימין לשמאל.
             */
            float dayX =
                    (DAYS - position - 1)
                            * DAY_WIDTH;


            float dayY =
                    monthIndex *
                            MONTH_HEIGHT;


            /*
             * זום התחלתי.
             *
             * בגלל ששתי עמודות קפואות נמצאות
             * בצד ימין, לא כדאי לבצע זום גדול מדי.
             *
             * 1.25 נותן הגדלה נעימה ועדיין משאיר
             * את היום הנוכחי גלוי באזור הימים.
             */
            float targetScale =
                    Math.max(
                            getMinScale(),
                            0.75f
                    );

            targetScale =
                    Math.min(
                            targetScale,
                            MAX_SCALE
                    );

            scaleFactor =
                    targetScale;



            /*
             * ========================================================
             * האזור שבו באמת אפשר לראות
             * את עמודות הימים.
             *
             * שתי העמודות הקפואות נמצאות מימין,
             * ולכן אסור למרכז את היום בתוך
             * כל רוחב ה-View.
             * ========================================================
             */

            float frozenWidth =
                    getFrozenWidth();


            float daysViewportWidth =
                    getWidth() -
                            frozenWidth;


            /*
             * מרכז אזור הימים בלבד.
             */
            float daysViewportCenterX =
                    daysViewportWidth / 2f;


            /*
             * מרכז התא של היום.
             */
            float targetX =
                    dayX +
                            DAY_WIDTH / 2f;


            /*
             * ========================================================
             * מרכז אנכי.
             *
             * כאן אין עמודות קפואות אנכיות,
             * לכן אפשר להשתמש בכל גובה ה-View.
             * ========================================================
             */

            float targetY =
                    dayY +
                            MONTH_HEIGHT / 2f;


            /*
             * ========================================================
             * מיקום אופקי.
             *
             * חשוב:
             *
             * היום צריך להיות במרכז אזור הימים,
             * לא במרכז כל המסך.
             * ========================================================
             */

            panX =
                    daysViewportCenterX
                            - targetX * scaleFactor;


            /*
             * מיקום אנכי.
             */
            panY =
                    getHeight() / 2f
                            - targetY * scaleFactor;


            /*
             * מגבילים את המיקום
             * כדי שלא נצא מגבולות הלוח.
             */
            clampPan();


            invalidate();
        }




        CalendarTableView(Context context) {

            super(context);


            density =
                    getResources()
                            .getDisplayMetrics()
                            .density;


            DAY_WIDTH =
                    dp(55);


            ROW_HEIGHT =
                    dp(34);


            MONTH_WIDTH =
                    dp(120);


            YEAR_WIDTH =
                    dp(85);


            MONTH_HEIGHT =
                    ROW_HEIGHT * 7;


            TABLE_WIDTH =
                    DAYS * DAY_WIDTH
                            + MONTH_WIDTH
                            + YEAR_WIDTH;


            TABLE_HEIGHT =
                    MONTHS * MONTH_HEIGHT;


            fill.setStyle(
                    Paint.Style.FILL
            );


            line.setStyle(
                    Paint.Style.STROKE
            );


            line.setStrokeWidth(
                    dp(1)
            );


            line.setColor(
                    Color.rgb(
                            40,
                            40,
                            40
                    )
            );


            text.setColor(
                    Color.BLACK
            );


            text.setTextSize(
                    dp(12)
            );


            text.setAntiAlias(
                    true
            );


            border.setStyle(
                    Paint.Style.STROKE
            );


            border.setColor(
                    Color.BLACK
            );


            todayPaint.setStyle(
                    Paint.Style.STROKE
            );


            todayPaint.setStrokeWidth(
                    dp(3)
            );


            todayPaint.setColor(
                    COLOR_TODAY
            );


            setBackgroundColor(
                    Color.WHITE
            );


            setClickable(true);


            scaleDetector =
                    new ScaleGestureDetector(
                            context,
                            new ScaleListener()
                    );
        }


        private int dp(int value) {

            return (int)
                    (
                            value *
                                    density
                                    + .5f
                    );
        }


        int getCalendarWidth() {

            return TABLE_WIDTH;
        }


        int getCalendarHeight() {

            return TABLE_HEIGHT;
        }


        private void clampPan() {

            float viewportWidth =
                    getWidth();


            float viewportHeight =
                    getHeight();


            float frozenWidth =
                    getFrozenWidth();


            float scrollableWidth =
                    getScrollableWidth();


            float availableWidth =
                    viewportWidth -
                            frozenWidth;


            if (scrollableWidth <= availableWidth) {

                panX = 0;

            } else {

                float minX =
                        availableWidth -
                                scrollableWidth;


                if (panX < minX) {
                    panX = minX;
                }


                if (panX > 0) {
                    panX = 0;
                }
            }


            float contentHeight =
                    TABLE_HEIGHT *
                            scaleFactor;


            if (contentHeight <= viewportHeight) {

                panY =
                        (viewportHeight -
                                contentHeight) / 2f;

            } else {

                float minY =
                        viewportHeight -
                                contentHeight;


                if (panY < minY) {
                    panY = minY;
                }


                if (panY > 0) {
                    panY = 0;
                }
            }
        }


        /*
         * ========================================================
         * Zoom סביב נקודה
         * ========================================================
         */

        private void zoomAround(
                float newScale,
                float focusX,
                float focusY) {

            float minScale =
                    getMinScale();


            newScale =
                    Math.max(
                            minScale,
                            Math.min(
                                    MAX_SCALE,
                                    newScale
                            )
                    );


            float oldScale =
                    scaleFactor;


            if (Math.abs(
                    newScale -
                            oldScale
            ) < .0001f) {

                return;
            }


            float contentX =
                    (focusX - panX)
                            / oldScale;


            float contentY =
                    (focusY - panY)
                            / oldScale;


            scaleFactor =
                    newScale;


            panX =
                    focusX -
                            contentX *
                                    scaleFactor;


            panY =
                    focusY -
                            contentY *
                                    scaleFactor;


            clampPan();


            invalidate();
        }


        /*
         * ========================================================
         * ציור
         * ========================================================
         */

        @Override
        protected void onDraw(
                @NonNull Canvas canvas) {

            super.onDraw(canvas);


            canvas.drawColor(
                    Color.WHITE
            );


            float frozenWidth =
                    getFrozenWidth();


            float frozenLeft =
                    getWidth() -
                            frozenWidth;


            /*
             * חלק נגלל
             */

            canvas.save();


            canvas.clipRect(
                    0,
                    0,
                    frozenLeft,
                    getHeight()
            );


            canvas.translate(
                    panX,
                    panY
            );


            canvas.scale(
                    scaleFactor,
                    scaleFactor
            );


            for (int i = 0;
                 i < MONTHS;
                 i++) {

                drawMonth(
                        canvas,
                        i
                );
            }


            canvas.restore();


            /*
             * עמודות קפואות
             */

            canvas.save();


            canvas.clipRect(
                    frozenLeft,
                    0,
                    getWidth(),
                    getHeight()
            );


            float originalFrozenLeft =
                    DAYS * DAY_WIDTH;


            float frozenTranslationX =
                    frozenLeft -
                            originalFrozenLeft *
                                    scaleFactor;


            canvas.translate(
                    frozenTranslationX,
                    panY
            );


            canvas.scale(
                    scaleFactor,
                    scaleFactor
            );


            for (int i = 0;
                 i < MONTHS;
                 i++) {

                drawMonth(
                        canvas,
                        i
                );
            }


            canvas.restore();
        }


        /*
         * ========================================================
         * חודש
         * ========================================================
         */

        private void drawMonth(
                Canvas canvas,
                int index) {

            Calendar cal =
                    Calendar.getInstance();


            cal.set(
                    selectedYear,
                    START_MONTH,
                    1
            );


            cal.add(
                    Calendar.MONTH,
                    index
            );


            int year =
                    cal.get(
                            Calendar.YEAR
                    );


            int month =
                    cal.get(
                            Calendar.MONTH
                    );


            int days =
                    cal.getActualMaximum(
                            Calendar.DAY_OF_MONTH
                    );


            Calendar first =
                    Calendar.getInstance();


            first.set(
                    year,
                    month,
                    1
            );


            int empty =
                    first.get(
                            Calendar.DAY_OF_WEEK
                    ) - 1;


            float top =
                    index *
                            MONTH_HEIGHT;


            float monthX =
                    DAYS *
                            DAY_WIDTH;


            float yearX =
                    monthX +
                            MONTH_WIDTH;


            /*
             * רקע
             */

            fill.setColor(
                    Color.WHITE
            );


            canvas.drawRect(
                    0,
                    top,
                    TABLE_WIDTH,
                    top + MONTH_HEIGHT,
                    fill
            );


            /*
             * ימי החודש
             */

            for (int pos = 0;
                 pos < DAYS;
                 pos++) {

                float x =
                        (DAYS -
                                pos -
                                1)
                                * DAY_WIDTH;


                int day =
                        pos -
                                empty +
                                1;


                int color =
                        Color.WHITE;


                if (day >= 1 &&
                        day <= days) {

                    Calendar current =
                            Calendar.getInstance();


                    current.set(
                            year,
                            month,
                            day
                    );


                    color =
                            current.get(
                                    Calendar.DAY_OF_WEEK
                            ) ==
                                    Calendar.SATURDAY
                                    ? COLOR_BLUE
                                    : COLOR_YELLOW;
                }


                fill.setColor(
                        color
                );


                canvas.drawRect(
                        x,
                        top,
                        x + DAY_WIDTH,
                        top + MONTH_HEIGHT,
                        fill
                );


                /*
                 * אזור ירוק
                 */

                if (day >= 1 &&
                        day <= days) {

                    fill.setColor(
                            COLOR_GREEN
                    );


                    canvas.drawRect(
                            x,
                            top + ROW_HEIGHT * 4,
                            x + DAY_WIDTH,
                            top + ROW_HEIGHT * 5,
                            fill
                    );
                }
            }


            /*
             * עמודת החודש
             */

            fill.setColor(
                    COLOR_PINK
            );


            canvas.drawRect(
                    monthX,
                    top,
                    monthX + MONTH_WIDTH,
                    top + MONTH_HEIGHT,
                    fill
            );


            fill.setColor(
                    COLOR_YELLOW
            );


            canvas.drawRect(
                    monthX,
                    top,
                    monthX + MONTH_WIDTH,
                    top + ROW_HEIGHT,
                    fill
            );


            canvas.drawRect(
                    monthX,
                    top + ROW_HEIGHT * 2,
                    monthX + MONTH_WIDTH,
                    top + ROW_HEIGHT * 3,
                    fill
            );


            canvas.drawRect(
                    monthX,
                    top + ROW_HEIGHT * 3,
                    monthX + MONTH_WIDTH,
                    top + ROW_HEIGHT * 4,
                    fill
            );


            fill.setColor(
                    COLOR_GREEN
            );


            canvas.drawRect(
                    monthX,
                    top + ROW_HEIGHT * 4,
                    monthX + MONTH_WIDTH,
                    top + ROW_HEIGHT * 5,
                    fill
            );


            fill.setColor(
                    COLOR_BLUE
            );


            canvas.drawRect(
                    monthX,
                    top + ROW_HEIGHT * 5,
                    monthX + MONTH_WIDTH,
                    top + ROW_HEIGHT * 7,
                    fill
            );


            /*
             * עמודת השנה
             */

            fill.setColor(
                    COLOR_GRAY
            );


            canvas.drawRect(
                    yearX,
                    top,
                    yearX + YEAR_WIDTH,
                    top + MONTH_HEIGHT,
                    fill
            );


            /*
             * כותרות
             */

            centeredText(
                    canvas,
                    "יום בשבוע",
                    monthX,
                    top,
                    MONTH_WIDTH,
                    ROW_HEIGHT,
                    true
            );


            centeredText(
                    canvas,
                    englishMonths[month]
                            + "-"
                            + String.valueOf(year)
                            .substring(2),
                    monthX,
                    top + ROW_HEIGHT,
                    MONTH_WIDTH,
                    ROW_HEIGHT,
                    true
            );


            centeredText(
                    canvas,
                    getHebrewMonthName(
                            year,
                            month
                    ),
                    monthX,
                    top + ROW_HEIGHT * 2,
                    MONTH_WIDTH,
                    ROW_HEIGHT,
                    true
            );


            centeredText(
                    canvas,
                    "חגים ומועדים",
                    monthX,
                    top + ROW_HEIGHT * 3,
                    MONTH_WIDTH,
                    ROW_HEIGHT,
                    false
            );


            centeredText(
                    canvas,
                    "הוצאות\nחזויות",
                    monthX,
                    top + ROW_HEIGHT * 5,
                    MONTH_WIDTH,
                    ROW_HEIGHT,
                    true
            );


            centeredText(
                    canvas,
                    "הכנסות\nחזויות",
                    monthX,
                    top + ROW_HEIGHT * 6,
                    MONTH_WIDTH,
                    ROW_HEIGHT,
                    true
            );


            centeredText(
                    canvas,
                    String.valueOf(year),
                    yearX,
                    top,
                    YEAR_WIDTH,
                    MONTH_HEIGHT,
                    true
            );


            /*
             * ימים
             */

            for (int pos = 0;
                 pos < DAYS;
                 pos++) {

                float x =
                        (DAYS -
                                pos -
                                1)
                                * DAY_WIDTH;


                centeredText(
                        canvas,
                        hebrewWeekDays[
                                pos % 7
                                ],
                        x,
                        top,
                        DAY_WIDTH,
                        ROW_HEIGHT,
                        true
                );


                int day =
                        pos -
                                empty +
                                1;


                if (day < 1 ||
                        day > days) {

                    continue;
                }


                /*
                 * ==================================================
                 * סימון היום הנוכחי
                 * ==================================================
                 */

                if (isToday(
                        year,
                        month,
                        day
                )) {

                    /*
                     * רקע עדין
                     */

                    fill.setColor(
                            COLOR_TODAY_BACKGROUND
                    );


                    canvas.drawRect(
                            x + dp(2),
                            top + ROW_HEIGHT + dp(2),
                            x + DAY_WIDTH - dp(2),
                            top + ROW_HEIGHT * 2 - dp(2),
                            fill
                    );


                    /*
                     * מסגרת כחולה סביב היום
                     */

                    todayPaint.setStrokeWidth(
                            dp(3)
                    );


                    canvas.drawRect(
                            x + dp(2),
                            top + ROW_HEIGHT + dp(2),
                            x + DAY_WIDTH - dp(2),
                            top + ROW_HEIGHT * 2 - dp(2),
                            todayPaint
                    );
                }


                centeredText(
                        canvas,
                        String.valueOf(day),
                        x,
                        top + ROW_HEIGHT,
                        DAY_WIDTH,
                        ROW_HEIGHT,
                        true
                );


                centeredText(
                        canvas,
                        getHebrewDay(
                                year,
                                month,
                                day
                        ),
                        x,
                        top + ROW_HEIGHT * 2,
                        DAY_WIDTH,
                        ROW_HEIGHT,
                        false
                );



                String dateKey =
                        String.format(
                                Locale.US,
                                "%04d-%02d-%02d",
                                year,
                                month + 1,
                                day
                        );

                List<CalendarEntry> entries =
                        calendarEntries.get(
                                dateKey
                        );

                if (entries != null &&
                        !entries.isEmpty()) {

                    drawEntryIndicators(
                            canvas,
                            entries,
                            x,
                            top
                    );


                }




                if (entries != null &&
                        !entries.isEmpty()) {

                    drawEntryIndicators(
                            canvas,
                            entries,
                            x,
                            top
                    );
                }
            }


            /*
             * חגים
             */

            drawHolidayRow(
                    canvas,
                    year,
                    month,
                    days,
                    top,
                    empty
            );


            /*
             * ========================================================
             * סימון היום הנוכחי
             *
             * המסגרת מקיפה את כל התאים של אותו יום:
             *
             * יום בשבוע
             * תאריך לועזי
             * תאריך עברי
             * חגים ומועדים
             * הוצאות חזויות
             * הכנסות חזויות
             *
             * כלומר עמודה שלמה בגובה החודש.
             * ========================================================
             */
            drawTodayBorder(
                    canvas,
                    year,
                    month,
                    days,
                    top,
                    empty
            );


            /*
             * חגים.
             */
            drawHolidayRow(
                    canvas,
                    year,
                    month,
                    days,
                    top,
                    empty
            );


            /*
             * רשת.
             */
            drawGrid(
                    canvas,
                    top
            );




            /*
             * רשת
             */

            drawGrid(
                    canvas,
                    top
            );


            /*
             * סימון היום מצויר שוב
             * מעל הרשת כדי שהמסגרת תהיה ברורה.
             */

            for (int day = 1;
                 day <= days;
                 day++) {

                if (!isToday(
                        year,
                        month,
                        day
                )) {

                    continue;
                }


                int position =
                        empty +
                                day -
                                1;


                float x =
                        (DAYS -
                                position -
                                1)
                                * DAY_WIDTH;


                todayPaint.setStrokeWidth(
                        dp(3)
                );


                canvas.drawRect(
                        x + dp(2),
                        top + ROW_HEIGHT + dp(2),
                        x + DAY_WIDTH - dp(2),
                        top + ROW_HEIGHT * 2 - dp(2),
                        todayPaint
                );
            }


            border.setStrokeWidth(
                    dp(3)
            );


            canvas.drawRect(
                    0,
                    top,
                    TABLE_WIDTH,
                    top + MONTH_HEIGHT,
                    border
            );
        }


        /*
         * ========================================================
         * נקודות אירועים
         * ========================================================
         */

        private void drawEntryIndicators(
                Canvas canvas,
                List<CalendarEntry> entries,
                float x,
                float top) {

            int eventCount = 0;
            int incomeCount = 0;
            int expenseCount = 0;
            int noteCount = 0;


            for (CalendarEntry entry :
                    entries) {

                String type =
                        entry.type == null
                                ? "event"
                                : entry.type;


                switch (type) {

                    case "income":
                        incomeCount++;
                        break;

                    case "expense":
                        expenseCount++;
                        break;

                    case "note":
                        noteCount++;
                        break;

                    default:
                        eventCount++;
                        break;
                }
            }


            List<Integer> colors =
                    new ArrayList<>();


            if (eventCount > 0) {
                colors.add(COLOR_EVENT);
            }


            if (incomeCount > 0) {
                colors.add(COLOR_INCOME);
            }


            if (expenseCount > 0) {
                colors.add(COLOR_EXPENSE);
            }


            if (noteCount > 0) {
                colors.add(COLOR_NOTE);
            }


            float centerX =
                    x +
                            DAY_WIDTH / 2f;


            float centerY =
                    top +
                            ROW_HEIGHT * 5.45f;


            float radius =
                    dp(4);


            float spacing =
                    dp(10);


            int visibleDots =
                    Math.min(
                            colors.size(),
                            3
                    );


            float startX =
                    centerX -
                            (
                                    (visibleDots - 1)
                                            * spacing
                                            / 2f
                            );


            for (int i = 0;
                 i < visibleDots;
                 i++) {

                fill.setColor(
                        colors.get(i)
                );


                canvas.drawCircle(
                        startX +
                                i * spacing,
                        centerY,
                        radius,
                        fill
                );
            }


            int hiddenItems =
                    entries.size() -
                            visibleDots;


            if (hiddenItems > 0) {

                text.setColor(
                        Color.BLACK
                );


                text.setTypeface(
                        Typeface.DEFAULT_BOLD
                );


                text.setTextSize(
                        dp(9)
                );


                text.setTextAlign(
                        Paint.Align.LEFT
                );


                float plusX =
                        startX +
                                visibleDots *
                                        spacing +
                                dp(1);


                canvas.drawText(
                        "+" +
                                hiddenItems,
                        plusX,
                        centerY +
                                dp(3),
                        text
                );
            }
        }


        /*
         * ========================================================
         * חגים
         * ========================================================
         */

        private void drawHolidayRow(
                Canvas canvas,
                int year,
                int month,
                int days,
                float top,
                int empty) {

            Calendar g =
                    Calendar.getInstance();


            for (int day = 1;
                 day <= days;
                 day++) {

                g.set(
                        year,
                        month,
                        day
                );


                HebrewCalendar h =
                        new HebrewCalendar();


                h.setTimeInMillis(
                        g.getTimeInMillis()
                );


                int hm =
                        h.get(
                                HebrewCalendar.MONTH
                        );


                int hd =
                        h.get(
                                HebrewCalendar.DAY_OF_MONTH
                        );


                String holiday =
                        null;


                if (hm == 0) {

                    if (hd <= 2) {

                        holiday = "ראש השנה";

                    } else if (hd == 3) {

                        holiday = "צום גדליה";

                    } else if (hd == 10) {

                        holiday = "יום כיפור";

                    } else if (hd >= 15 &&
                            hd <= 21) {

                        holiday = "סוכות";

                    } else if (hd == 22) {

                        holiday = "שמיני עצרת";
                    }

                } else if (hm == 2 &&
                        hd >= 25) {

                    holiday = "חנוכה";

                } else if (hm == 3 &&
                        hd <= 2) {

                    holiday = "חנוכה";

                } else if (hm == 4 &&
                        hd == 15) {

                    holiday = "ט״ו בשבט";

                } else if (hm == 5 &&
                        hd == 14) {

                    holiday = "פורים";

                } else if (hm == 6 &&
                        hd == 14) {

                    holiday = "פורים";

                } else if (hm == 7 &&
                        hd >= 15 &&
                        hd <= 21) {

                    holiday = "פסח";

                } else if (hm == 8 &&
                        hd == 18) {

                    holiday = "ל״ג בעומר";

                } else if (hm == 9 &&
                        hd == 6) {

                    holiday = "שבועות";
                }


                if (holiday != null) {

                    drawHoliday(
                            canvas,
                            day,
                            holiday,
                            top,
                            empty
                    );
                }
            }
        }


        private void drawHoliday(
                Canvas canvas,
                int day,
                String holiday,
                float top,
                int empty) {

            int pos =
                    empty +
                            day -
                            1;


            if (day < 1 ||
                    day > 31 ||
                    pos < 0 ||
                    pos >= DAYS) {

                return;
            }


            float x =
                    (DAYS -
                            pos -
                            1)
                            * DAY_WIDTH;


            fill.setColor(
                    COLOR_PINK
            );


            canvas.drawRect(
                    x,
                    top + ROW_HEIGHT * 3,
                    x + DAY_WIDTH,
                    top + ROW_HEIGHT * 4,
                    fill
            );


            centeredText(
                    canvas,
                    holiday,
                    x,
                    top + ROW_HEIGHT * 3,
                    DAY_WIDTH,
                    ROW_HEIGHT,
                    false
            );
        }


        /*
         * ========================================================
         * Grid
         * ========================================================
         */

        private void drawGrid(
                Canvas canvas,
                float top) {

            for (int i = 0;
                 i <= DAYS;
                 i++) {

                float x =
                        i *
                                DAY_WIDTH;


                canvas.drawLine(
                        x,
                        top,
                        x,
                        top + MONTH_HEIGHT,
                        line
                );
            }


            for (int i = 0;
                 i <= 7;
                 i++) {

                float y =
                        top +
                                i *
                                        ROW_HEIGHT;


                canvas.drawLine(
                        0,
                        y,
                        TABLE_WIDTH,
                        y,
                        line
                );
            }
        }


        /*
         * ========================================================
         * טקסט
         * ========================================================
         */

        private void centeredText(
                Canvas canvas,
                String value,
                float x,
                float y,
                float width,
                float height,
                boolean bold) {

            if (value == null ||
                    value.isEmpty()) {

                return;
            }


            text.setTypeface(
                    bold
                            ? Typeface.DEFAULT_BOLD
                            : Typeface.DEFAULT
            );


            text.setColor(
                    Color.BLACK
            );


            text.setTextAlign(
                    Paint.Align.CENTER
            );


            text.setTextSize(
                    dp(12)
            );


            float centerX =
                    x +
                            width / 2f;


            Paint.FontMetrics fm =
                    text.getFontMetrics();


            if (value.contains("\n")) {

                String[] lines =
                        value.split("\n");


                float lineHeight =
                        height /
                                lines.length;


                for (int i = 0;
                     i < lines.length;
                     i++) {

                    float centerY =
                            y +
                                    lineHeight * i +
                                    lineHeight / 2f;


                    fm =
                            text.getFontMetrics();


                    canvas.drawText(
                            lines[i],
                            centerX,
                            centerY -
                                    (
                                            fm.ascent +
                                                    fm.descent
                                    ) / 2f,
                            text
                    );
                }

            } else {

                canvas.drawText(
                        value,
                        centerX,
                        y +
                                height / 2f -
                                (
                                        fm.ascent +
                                                fm.descent
                                ) / 2f,
                        text
                );
            }
        }


        /*
         * ========================================================
         * חודש עברי
         * ========================================================
         */

        private String getHebrewMonthName(
                int year,
                int month) {

            Calendar g =
                    Calendar.getInstance();


            g.set(
                    year,
                    month,
                    1
            );


            HebrewCalendar h =
                    new HebrewCalendar();


            h.setTimeInMillis(
                    g.getTimeInMillis()
            );


            int m =
                    h.get(
                            HebrewCalendar.MONTH
                    );


            boolean leap =
                    h.getActualMaximum(
                            HebrewCalendar.MONTH
                    ) == 12;


            if (leap && m >= 5) {

                String[] months = {

                        "תשרי",
                        "חשון",
                        "כסלו",
                        "טבת",
                        "שבט",
                        "אדר א",
                        "אדר ב",
                        "ניסן",
                        "אייר",
                        "סיון",
                        "תמוז",
                        "אב",
                        "אלול"
                };


                return months[m];
            }


            String[] months = {

                    "תשרי",
                    "חשון",
                    "כסלו",
                    "טבת",
                    "שבט",
                    "אדר",
                    "ניסן",
                    "אייר",
                    "סיון",
                    "תמוז",
                    "אב",
                    "אלול"
            };


            return m < months.length
                    ? months[m]
                    : "";
        }


        /*
         * ========================================================
         * יום עברי
         * ========================================================
         */

        private String getHebrewDay(
                int year,
                int month,
                int day) {

            Calendar g =
                    Calendar.getInstance();


            g.set(
                    year,
                    month,
                    day
            );


            HebrewCalendar h =
                    new HebrewCalendar();


            h.setTimeInMillis(
                    g.getTimeInMillis()
            );


            return hebrewNumber(
                    h.get(
                            HebrewCalendar.DAY_OF_MONTH
                    )
            );
        }


        private String hebrewNumber(
                int n) {

            if (n <= 0) {
                return "";
            }


            String[] u = {

                    "",
                    "א",
                    "ב",
                    "ג",
                    "ד",
                    "ה",
                    "ו",
                    "ז",
                    "ח",
                    "ט"
            };


            if (n < 20) {

                if (n == 15) {
                    return "טו";
                }


                if (n == 16) {
                    return "טז";
                }


                return n >= 10
                        ? "י" +
                        u[n - 10]
                        : u[n];
            }


            if (n < 30) {

                return "כ" +
                        u[n - 20];
            }


            if (n == 30) {
                return "ל";
            }


            if (n == 31) {
                return "לא";
            }


            return String.valueOf(n);
        }


        /*
         * ========================================================
         * Scale Listener
         * ========================================================
         */

        private class ScaleListener
                extends ScaleGestureDetector
                .SimpleOnScaleGestureListener {


            @Override
            public boolean onScaleBegin(
                    ScaleGestureDetector detector) {

                getParent()
                        .requestDisallowInterceptTouchEvent(
                                true
                        );


                return true;
            }


            @Override
            public boolean onScale(
                    ScaleGestureDetector detector) {

                float newScale =
                        scaleFactor *
                                detector.getScaleFactor();


                zoomAround(
                        newScale,
                        detector.getFocusX(),
                        detector.getFocusY()
                );


                return true;
            }


            @Override
            public void onScaleEnd(
                    ScaleGestureDetector detector) {

                getParent()
                        .requestDisallowInterceptTouchEvent(
                                false
                        );
            }
        }


        /*
         * ========================================================
         * Touch
         * ========================================================
         */

        @Override
        public boolean onTouchEvent(
                MotionEvent event) {

            scaleDetector.onTouchEvent(event);


            switch (
                    event.getActionMasked()
            ) {

                case MotionEvent.ACTION_DOWN:

                    downX =
                            event.getX();


                    downY =
                            event.getY();


                    lastTouchX =
                            event.getX();


                    lastTouchY =
                            event.getY();


                    moved =
                            false;


                    getParent()
                            .requestDisallowInterceptTouchEvent(
                                    true
                            );


                    return true;


                case MotionEvent.ACTION_POINTER_DOWN:

                    moved =
                            true;


                    getParent()
                            .requestDisallowInterceptTouchEvent(
                                    true
                            );


                    return true;


                case MotionEvent.ACTION_MOVE:

                    if (scaleDetector.isInProgress()) {

                        moved =
                                true;


                        return true;
                    }


                    float dx =
                            event.getX() -
                                    lastTouchX;


                    float dy =
                            event.getY() -
                                    lastTouchY;


                    if (
                            Math.abs(
                                    event.getX() -
                                            downX
                            ) > TOUCH_SLOP ||

                                    Math.abs(
                                            event.getY() -
                                                    downY
                                    ) > TOUCH_SLOP
                    ) {

                        moved =
                                true;
                    }


                    panX += dx;

                    panY += dy;


                    clampPan();


                    lastTouchX =
                            event.getX();


                    lastTouchY =
                            event.getY();


                    invalidate();


                    return true;


                case MotionEvent.ACTION_POINTER_UP:

                    if (event.getPointerCount() > 1) {

                        int remainingIndex =
                                event.getActionIndex() == 0
                                        ? 1
                                        : 0;


                        if (remainingIndex <
                                event.getPointerCount()) {

                            lastTouchX =
                                    event.getX(
                                            remainingIndex
                                    );


                            lastTouchY =
                                    event.getY(
                                            remainingIndex
                                    );
                        }
                    }


                    return true;


                case MotionEvent.ACTION_UP:

                    getParent()
                            .requestDisallowInterceptTouchEvent(
                                    false
                            );


                    if (!moved &&
                            !scaleDetector.isInProgress()) {

                        handleClick(
                                event.getX(),
                                event.getY()
                        );
                    }


                    performClick();


                    return true;


                case MotionEvent.ACTION_CANCEL:

                    getParent()
                            .requestDisallowInterceptTouchEvent(
                                    false
                            );


                    return true;
            }


            return true;
        }


        /*
         * ========================================================
         * לחיצה על יום
         * ========================================================
         */

        private void handleClick(
                float screenX,
                float screenY) {

            float realX =
                    (
                            screenX -
                                    panX
                    ) /
                            scaleFactor;


            float realY =
                    (
                            screenY -
                                    panY
                    ) /
                            scaleFactor;


            int monthIndex =
                    (int)
                            (
                                    realY /
                                            MONTH_HEIGHT
                            );


            if (monthIndex < 0 ||
                    monthIndex >= MONTHS) {

                return;
            }


            if (realX >=
                    DAYS * DAY_WIDTH) {

                return;
            }


            int position =
                    DAYS -
                            1 -
                            (int)
                                    (
                                            realX /
                                                    DAY_WIDTH
                                    );


            if (position < 0 ||
                    position >= DAYS) {

                return;
            }


            Calendar selected =
                    Calendar.getInstance();


            selected.set(
                    selectedYear,
                    START_MONTH,
                    1
            );


            selected.add(
                    Calendar.MONTH,
                    monthIndex
            );


            int year =
                    selected.get(
                            Calendar.YEAR
                    );


            int month =
                    selected.get(
                            Calendar.MONTH
                    );


            Calendar first =
                    Calendar.getInstance();


            first.set(
                    year,
                    month,
                    1
            );


            int empty =
                    first.get(
                            Calendar.DAY_OF_WEEK
                    ) - 1;


            int day =
                    position -
                            empty +
                            1;


            int maxDay =
                    selected.getActualMaximum(
                            Calendar.DAY_OF_MONTH
                    );


            if (day < 1 ||
                    day > maxDay) {

                return;
            }



            String gregorianDate =
                    String.format(
                            Locale.US,
                            "%04d-%02d-%02d",
                            year,
                            month + 1,
                            day
                    );




            Calendar selectedDate =
                    Calendar.getInstance();


            selectedDate.set(
                    year,
                    month,
                    day
            );


            HebrewCalendar hebrewCalendar =
                    new HebrewCalendar();


            hebrewCalendar.setTimeInMillis(
                    selectedDate.getTimeInMillis()
            );


            int hebrewDay =
                    hebrewCalendar.get(
                            HebrewCalendar.DAY_OF_MONTH
                    );


            int hebrewYear =
                    hebrewCalendar.get(
                            HebrewCalendar.YEAR
                    );


            String hebrewMonth =
                    getHebrewMonthName(
                            year,
                            month
                    );


            String hebrewDate =
                    hebrewNumber(
                            hebrewDay
                    )
                            + " ב"
                            + hebrewMonth
                            + " "
                            + hebrewYear;


            Intent intent =
                    new Intent(
                            requireContext(),
                            DayDetailsActivity.class
                    );


            intent.putExtra(
                    "gregorian_date",
                    gregorianDate
            );


            intent.putExtra(
                    "hebrew_date",
                    hebrewDate
            );


            startActivity(intent);
        }


        @Override
        public boolean performClick() {

            super.performClick();

            return true;
        }
    }
}
