package com.israel.hebrewcalendar;

import android.content.Context;
import android.content.Intent;
import android.graphics.*;
import android.os.Bundle;
import android.view.*;
import android.view.ScaleGestureDetector;
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
    private HorizontalScrollView horizontalScroll;
    private ScrollView verticalScroll;

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

        calendarContainer =
                view.findViewById(R.id.calendarContainer);

        horizontalScroll =
                view.findViewById(R.id.horizontalScroll);

        verticalScroll =
                view.findViewById(R.id.verticalScroll);

        Spinner yearSpinner =
                view.findViewById(R.id.yearSpinner);


        calendarView =
                new CalendarTableView(requireContext());


        Integer[] years = new Integer[21];

        for (int i = 0; i < years.length; i++) {
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


        yearSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id) {

                        calendarView.selectedYear =
                                (Integer) parent.getItemAtPosition(position);

                        calendarView.invalidate();
                    }


                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent) {
                    }
                }
        );


        calendarContainer.removeAllViews();


        int w =
                calendarView.getCalendarWidth();

        int h =
                calendarView.getCalendarHeight();


        calendarContainer.addView(
                calendarView,
                new FrameLayout.LayoutParams(w, h)
        );


        FrameLayout.LayoutParams cp =
                (FrameLayout.LayoutParams)
                        calendarContainer.getLayoutParams();


        if (cp == null) {

            cp =
                    new FrameLayout.LayoutParams(w, h);

        } else {

            cp.width = w;
            cp.height = h;
        }


        calendarContainer.setLayoutParams(cp);


        horizontalScroll.post(() -> {

            horizontalScroll.requestLayout();
            verticalScroll.requestLayout();

            horizontalScroll.post(() -> {

                View child =
                        horizontalScroll.getChildAt(0);

                if (child == null) {
                    return;
                }


                int contentWidth =
                        child.getMeasuredWidth();


                if (contentWidth <= 0) {

                    contentWidth =
                            calendarContainer.getMeasuredWidth();
                }


                horizontalScroll.scrollTo(
                        Math.max(
                                0,
                                contentWidth -
                                        horizontalScroll.getWidth()
                        ),
                        0
                );
            });
        });


        loadCalendarEntries();
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

                        String date =
                                document.getString("gregorianDate");


                        if (date == null ||
                                date.trim().isEmpty()) {

                            continue;
                        }


                        CalendarEntry entry =
                                new CalendarEntry();


                        entry.id =
                                document.getId();


                        entry.gregorianDate =
                                date;


                        entry.hebrewDate =
                                document.getString("hebrewDate");


                        entry.type =
                                document.getString("type");


                        entry.title =
                                document.getString("title");


                        entry.time =
                                document.getString("time");


                        entry.description =
                                document.getString("description");


                        if (!calendarEntries.containsKey(date)) {

                            calendarEntries.put(
                                    date,
                                    new ArrayList<>()
                            );
                        }


                        calendarEntries
                                .get(date)
                                .add(entry);
                    }


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


    public static class CalendarEntry {

        String id;

        String gregorianDate;
        String hebrewDate;

        String type;

        String title;
        String time;
        String description;
    }


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


        private int selectedYear = 2026;

        private final int START_MONTH =
                Calendar.DECEMBER;


        private float scaleFactor = 1f;

        private final float MAX_SCALE = 3f;

        private final ScaleGestureDetector scaleDetector;


        private float downX;
        private float downY;

        private boolean moved;

        private static final int TOUCH_SLOP = 20;


        /*
         * צבעי הלוח
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


        /*
         * צבעי הנקודות
         */
        private final int COLOR_EVENT =
                Color.rgb(52, 120, 246);

        private final int COLOR_INCOME =
                Color.rgb(46, 160, 67);

        private final int COLOR_EXPENSE =
                Color.rgb(220, 53, 69);

        private final int COLOR_NOTE =
                Color.rgb(245, 180, 40);


        private final Paint fill =
                new Paint(Paint.ANTI_ALIAS_FLAG);

        private final Paint line =
                new Paint(Paint.ANTI_ALIAS_FLAG);

        private final Paint text =
                new Paint(Paint.ANTI_ALIAS_FLAG);

        private final Paint border =
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
                    Color.rgb(40, 40, 40)
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
                    (value * density + .5f);
        }


        int getCalendarWidth() {

            return Math.round(
                    TABLE_WIDTH *
                            scaleFactor
            );
        }


        int getCalendarHeight() {

            return Math.round(
                    TABLE_HEIGHT *
                            scaleFactor
            );
        }


        private float getMinimumScale() {

            if (horizontalScroll == null ||
                    verticalScroll == null) {

                return .1f;
            }


            int w =
                    horizontalScroll.getWidth();

            int h =
                    verticalScroll.getHeight();


            if (w <= 0 || h <= 0) {
                return .1f;
            }


            return Math.max(
                    .1f,
                    Math.min(
                            (float) w /
                                    TABLE_WIDTH,

                            (float) h /
                                    TABLE_HEIGHT
                    )
            );
        }


        private void updateViewSize() {

            int w =
                    getCalendarWidth();

            int h =
                    getCalendarHeight();


            ViewGroup.LayoutParams p =
                    getLayoutParams();


            if (p == null) {

                p =
                        new ViewGroup.LayoutParams(
                                w,
                                h
                        );

            } else {

                p.width = w;
                p.height = h;
            }


            setLayoutParams(p);


            if (calendarContainer != null) {

                ViewGroup.LayoutParams cp =
                        calendarContainer
                                .getLayoutParams();


                if (cp == null) {

                    cp =
                            new ViewGroup.LayoutParams(
                                    w,
                                    h
                            );

                } else {

                    cp.width = w;
                    cp.height = h;
                }


                calendarContainer
                        .setLayoutParams(cp);
            }


            requestLayout();


            if (horizontalScroll != null) {
                horizontalScroll.requestLayout();
            }


            if (verticalScroll != null) {
                verticalScroll.requestLayout();
            }


            invalidate();
        }


        @Override
        protected void onDraw(
                @NonNull Canvas canvas) {

            super.onDraw(canvas);


            canvas.drawColor(
                    Color.WHITE
            );


            canvas.save();


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


            drawOuterBorder(canvas);


            canvas.restore();
        }


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
                    cal.get(Calendar.YEAR);


            int month =
                    cal.get(Calendar.MONTH);


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
                    index * MONTH_HEIGHT;


            float monthX =
                    DAYS * DAY_WIDTH;


            float yearX =
                    monthX +
                            MONTH_WIDTH;


            /*
             * רקע כללי
             */
            fill.setColor(Color.WHITE);

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
                        (DAYS - pos - 1)
                                * DAY_WIDTH;


                int day =
                        pos - empty + 1;


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
                            ) == Calendar.SATURDAY
                                    ? COLOR_BLUE
                                    : COLOR_YELLOW;
                }


                fill.setColor(color);


                canvas.drawRect(
                        x,
                        top,
                        x + DAY_WIDTH,
                        top + MONTH_HEIGHT,
                        fill
                );


                /*
                 * אזור הנתונים הירוק
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
            fill.setColor(COLOR_PINK);

            canvas.drawRect(
                    monthX,
                    top,
                    monthX + MONTH_WIDTH,
                    top + MONTH_HEIGHT,
                    fill
            );


            fill.setColor(COLOR_YELLOW);

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


            fill.setColor(COLOR_GREEN);

            canvas.drawRect(
                    monthX,
                    top + ROW_HEIGHT * 4,
                    monthX + MONTH_WIDTH,
                    top + ROW_HEIGHT * 5,
                    fill
            );


            fill.setColor(COLOR_BLUE);

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
            fill.setColor(COLOR_GRAY);

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
             * הימים והנקודות
             */
            for (int pos = 0;
                 pos < DAYS;
                 pos++) {

                float x =
                        (DAYS - pos - 1)
                                * DAY_WIDTH;


                centeredText(
                        canvas,
                        hebrewWeekDays[pos % 7],
                        x,
                        top,
                        DAY_WIDTH,
                        ROW_HEIGHT,
                        true
                );


                int day =
                        pos - empty + 1;


                if (day < 1 ||
                        day > days) {

                    continue;
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


                String gregorianDate =
                        String.format(
                                Locale.getDefault(),
                                "%02d/%02d/%04d",
                                day,
                                month + 1,
                                year
                        );


                List<CalendarEntry> entries =
                        calendarEntries.get(
                                gregorianDate
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
            }


            drawHolidayRow(
                    canvas,
                    year,
                    month,
                    days,
                    top,
                    empty
            );


            drawGrid(
                    canvas,
                    top
            );


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
         * ============================================================
         * נקודות צבעוניות +N
         * ============================================================
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


            for (CalendarEntry entry : entries) {

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


            /*
             * כל סוג מקבל נקודה אחת.
             *
             * כך למשל אם יש:
             * 4 אירועים
             * 2 הכנסות
             * 1 הוצאה
             *
             * נראה:
             *
             * 🔵 🟢 🔴 +4
             */
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
                    x + DAY_WIDTH / 2f;


            float centerY =
                    top + ROW_HEIGHT * 5.45f;


            float radius =
                    dp(4);


            float spacing =
                    dp(10);


            /*
             * כמה נקודות נציג בפועל.
             */
            int visibleDots =
                    Math.min(
                            colors.size(),
                            3
                    );


            float startX =
                    centerX -
                            ((visibleDots - 1)
                                    * spacing / 2f);


            for (int i = 0;
                 i < visibleDots;
                 i++) {

                fill.setColor(
                        colors.get(i)
                );


                canvas.drawCircle(
                        startX + i * spacing,
                        centerY,
                        radius,
                        fill
                );
            }


            /*
             * כמה פריטים מעבר למה
             * שהנקודות מייצגות.
             */
            int totalTypes =
                    colors.size();


            int hiddenItems =
                    entries.size() -
                            visibleDots;


            if (totalTypes > 3) {

                hiddenItems =
                        entries.size() - 3;
            }


            /*
             * אם נשארו פריטים שלא הוצגו,
             * מציגים +N.
             */
            if (hiddenItems > 0) {

                text.setColor(Color.BLACK);
                text.setTypeface(Typeface.DEFAULT_BOLD);
                text.setTextSize(dp(9));
                text.setTextAlign(Paint.Align.LEFT);


                float plusX =
                        startX +
                                visibleDots * spacing +
                                dp(1);


                canvas.drawText(
                        "+" + hiddenItems,
                        plusX,
                        centerY +
                                dp(3),
                        text
                );
            }
        }


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
                    empty + day - 1;


            if (day < 1 ||
                    day > 31 ||
                    pos < 0 ||
                    pos >= DAYS) {

                return;
            }


            float x =
                    (DAYS - pos - 1)
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


        private void drawGrid(
                Canvas canvas,
                float top) {

            for (int i = 0;
                 i <= DAYS;
                 i++) {

                float x =
                        i * DAY_WIDTH;


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
                        top + i * ROW_HEIGHT;


                canvas.drawLine(
                        0,
                        y,
                        TABLE_WIDTH,
                        y,
                        line
                );
            }
        }


        private void drawOuterBorder(
                Canvas canvas) {

            border.setStrokeWidth(
                    dp(2)
            );


            canvas.drawRect(
                    0,
                    0,
                    TABLE_WIDTH,
                    TABLE_HEIGHT,
                    border
            );
        }


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


            text.setTextAlign(
                    Paint.Align.CENTER
            );


            text.setTextSize(dp(12));


            float centerX =
                    x + width / 2f;


            Paint.FontMetrics fm =
                    text.getFontMetrics();


            if (value.contains("\n")) {

                String[] lines =
                        value.split("\n");


                float lineHeight =
                        height / lines.length;


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
                                    (fm.ascent +
                                            fm.descent) / 2f,
                            text
                    );
                }

            } else {

                canvas.drawText(
                        value,
                        centerX,
                        y +
                                height / 2f -
                                (fm.ascent +
                                        fm.descent) / 2f,
                        text
                );
            }
        }


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


        private String hebrewNumber(int n) {

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
                        ? "י" + u[n - 10]
                        : u[n];
            }


            if (n < 30) {
                return "כ" + u[n - 20];
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
         * ============================================================
         * זום טבעי כמו Google Sheets
         * ============================================================
         *
         * הרעיון:
         *
         * אם האצבעות נמצאות למשל על תא מסוים,
         * התא הזה חייב להישאר מתחת לאצבעות
         * גם אחרי שהזום משתנה.
         *
         * לכן אנחנו שומרים את נקודת ה-focus
         * ומתקנים את מיקום שני ה-ScrollViews
         * אחרי שינוי הזום.
         */
        private class ScaleListener
                extends ScaleGestureDetector.SimpleOnScaleGestureListener {

            /*
             * נקודת ה-pinch במסך/ב-View
             */
            private float focusX;
            private float focusY;

            /*
             * מיקום הגלילה לפני שינוי הזום
             */
            private int oldScrollX;
            private int oldScrollY;

            /*
             * הזום שהיה לפני השינוי
             */
            private float oldScale;

            /*
             * נקודת התוכן שעליה המשתמש עושה pinch.
             *
             * היא נשמרת בקואורדינטות של הטבלה
             * לפני שינוי הזום.
             */
            private float contentFocusX;
            private float contentFocusY;


            @Override
            public boolean onScaleBegin(
                    ScaleGestureDetector detector) {

                oldScale =
                        scaleFactor;


                /*
                 * נקודת המרכז של שתי האצבעות
                 */
                focusX =
                        detector.getFocusX();

                focusY =
                        detector.getFocusY();


                /*
                 * מיקום הגלילה הנוכחי
                 */
                oldScrollX =
                        horizontalScroll != null
                                ? horizontalScroll.getScrollX()
                                : 0;


                oldScrollY =
                        verticalScroll != null
                                ? verticalScroll.getScrollY()
                                : 0;


                /*
                 * המרה מנקודת מסך
                 * לנקודה אמיתית בתוך הטבלה.
                 *
                 * לדוגמה:
                 *
                 * scrollX = 500
                 * focusX = 200
                 *
                 * כלומר המשתמש נוגע בנקודה 700
                 * בתוך התוכן.
                 */
                contentFocusX =
                        (oldScrollX + focusX)
                                / oldScale;


                contentFocusY =
                        (oldScrollY + focusY)
                                / oldScale;


                /*
                 * בזמן pinch ה-ScrollViews
                 * לא צריכים לגנוב את המגע.
                 */
                if (getParent() != null) {

                    getParent()
                            .requestDisallowInterceptTouchEvent(
                                    true
                            );
                }


                return true;
            }


            @Override
            public boolean onScale(
                    ScaleGestureDetector detector) {

                float old =
                        scaleFactor;


                float min =
                        getMinimumScale();


                /*
                 * שינוי זום חלק.
                 */
                float newScale =
                        scaleFactor *
                                detector.getScaleFactor();


                /*
                 * הגבלת הזום.
                 */
                newScale =
                        Math.max(
                                min,
                                Math.min(
                                        MAX_SCALE,
                                        newScale
                                )
                        );


                /*
                 * אם לא באמת השתנה הזום
                 * אין צורך לעשות כלום.
                 */
                if (Math.abs(
                        newScale - old
                ) < 0.0001f) {

                    return true;
                }


                scaleFactor =
                        newScale;


                /*
                 * עדכון גודל ה-View.
                 */
                updateViewSize();


                /*
                 * חשוב:
                 *
                 * updateViewSize() גורם ל-layout.
                 * לכן אנחנו מחכים לסיום ה-layout
                 * ורק אז מתקנים את הגלילה.
                 */
                post(() -> {

                    if (horizontalScroll == null ||
                            verticalScroll == null) {

                        return;
                    }


                    /*
                     * איפה נקודת ה-focus צריכה להיות
                     * אחרי הזום?
                     *
                     * contentFocusX/Y נשארים קבועים.
                     */
                    float newScrollX =
                            contentFocusX *
                                    scaleFactor -
                                    focusX;


                    float newScrollY =
                            contentFocusY *
                                    scaleFactor -
                                    focusY;


                    /*
                     * גבולות הגלילה האופקית.
                     */
                    int maxScrollX =
                            Math.max(
                                    0,
                                    calendarContainer.getWidth()
                                            -
                                            horizontalScroll.getWidth()
                            );


                    /*
                     * גבולות הגלילה האנכית.
                     */
                    int maxScrollY =
                            Math.max(
                                    0,
                                    calendarContainer.getHeight()
                                            -
                                            verticalScroll.getHeight()
                            );


                    int targetScrollX =
                            Math.round(
                                    newScrollX
                            );


                    int targetScrollY =
                            Math.round(
                                    newScrollY
                            );


                    /*
                     * לא לצאת מגבולות הטבלה.
                     */
                    targetScrollX =
                            Math.max(
                                    0,
                                    Math.min(
                                            maxScrollX,
                                            targetScrollX
                                    )
                            );


                    targetScrollY =
                            Math.max(
                                    0,
                                    Math.min(
                                            maxScrollY,
                                            targetScrollY
                                    )
                            );


                    /*
                     * הזזה למיקום החדש.
                     *
                     * זו השורה שהופכת את הזום
                     * ל"טבעי".
                     */
                    horizontalScroll.scrollTo(
                            targetScrollX,
                            0
                    );


                    verticalScroll.scrollTo(
                            0,
                            targetScrollY
                    );
                });


                return true;
            }


            @Override
            public void onScaleEnd(
                    ScaleGestureDetector detector) {

                super.onScaleEnd(detector);


                /*
                 * החזרת השליטה ל-ScrollViews.
                 */
                if (getParent() != null) {

                    getParent()
                            .requestDisallowInterceptTouchEvent(
                                    false
                            );
                }
            }
        }


        /*
         * ============================================================
         * Touch
         * ============================================================
         */
        @Override
        public boolean onTouchEvent(
                MotionEvent event) {

            /*
             * קודם כל מעבירים את האירוע
             * ל-ScaleGestureDetector.
             */
            scaleDetector.onTouchEvent(event);


            switch (
                    event.getActionMasked()
            ) {

                case MotionEvent.ACTION_DOWN:

                    downX =
                            event.getX();

                    downY =
                            event.getY();

                    moved =
                            false;


                    /*
                     * עדיין לא יודעים אם זה:
                     *
                     * לחיצה
                     * או גלילה.
                     */
                    if (getParent() != null) {

                        getParent()
                                .requestDisallowInterceptTouchEvent(
                                        false
                                );
                    }


                    return true;


                case MotionEvent.ACTION_POINTER_DOWN:

                    /*
                     * נכנסה אצבע שנייה.
                     *
                     * מעכשיו מדובר ב-pinch.
                     */
                    moved =
                            true;


                    if (getParent() != null) {

                        getParent()
                                .requestDisallowInterceptTouchEvent(
                                        true
                                );
                    }


                    return true;


                case MotionEvent.ACTION_MOVE:

                    /*
                     * בזמן pinch:
                     *
                     * ScaleGestureDetector מטפל בזום.
                     */
                    if (scaleDetector.isInProgress()) {

                        moved =
                                true;


                        if (getParent() != null) {

                            getParent()
                                    .requestDisallowInterceptTouchEvent(
                                            true
                                    );
                        }


                        return true;
                    }


                    /*
                     * אם זו אצבע אחת שנעה,
                     * לא מדובר בלחיצה.
                     */
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


                        /*
                         * במקרה של אצבע אחת,
                         * נותנים ל-ScrollView
                         * לטפל בגלילה.
                         */
                        if (getParent() != null) {

                            getParent()
                                    .requestDisallowInterceptTouchEvent(
                                            false
                                    );
                        }
                    }


                    return true;


                case MotionEvent.ACTION_POINTER_UP:

                    /*
                     * אם אצבע אחת יורדת,
                     * ScaleGestureDetector ימשיך לטפל
                     * במידת הצורך.
                     */
                    return true;


                case MotionEvent.ACTION_UP:

                    /*
                     * רק אם לא הייתה תנועה
                     * ולא היה pinch,
                     * זו לחיצה על יום.
                     */
                    if (!moved &&
                            !scaleDetector.isInProgress()) {

                        handleClick(
                                event.getX(),
                                event.getY()
                        );
                    }


                    if (getParent() != null) {

                        getParent()
                                .requestDisallowInterceptTouchEvent(
                                        false
                                );
                    }


                    performClick();

                    return true;


                case MotionEvent.ACTION_CANCEL:

                    if (getParent() != null) {

                        getParent()
                                .requestDisallowInterceptTouchEvent(
                                        false
                                );
                    }


                    return true;
            }


            return true;
        }


        private void handleClick(
                float x,
                float y) {

            float realX =
                    x / scaleFactor;


            float realY =
                    y / scaleFactor;


            int monthIndex =
                    (int)
                            (realY /
                                    MONTH_HEIGHT);


            if (monthIndex < 0 ||
                    monthIndex >= MONTHS) {

                return;
            }


            if (realX >= DAYS * DAY_WIDTH) {
                return;
            }


            int position =
                    DAYS - 1 -
                            (int)
                                    (realX /
                                            DAY_WIDTH);


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
                            Locale.getDefault(),
                            "%02d/%02d/%04d",
                            day,
                            month + 1,
                            year
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
                    hebrewNumber(hebrewDay)
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