package com.israel.hebrewcalendar;



import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Calendar;


import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.AdapterView;

import android.icu.util.HebrewCalendar;


public class HebrewCalendarFragment extends Fragment {

    private CalendarTableView calendarView;

    private Spinner yearSpinner;

    private FrameLayout calendarContainer;
    private HorizontalScrollView horizontalScroll;
    private ScrollView verticalScroll;

    public HebrewCalendarFragment() {
        super(R.layout.fragment_calendar);
    }

    // ============================================================
    // ON VIEW CREATED
    // ============================================================

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        calendarContainer =
                view.findViewById(R.id.calendarContainer);

        horizontalScroll =
                view.findViewById(R.id.horizontalScroll);

        verticalScroll =
                view.findViewById(R.id.verticalScroll);

        yearSpinner =
                view.findViewById(R.id.yearSpinner);

        // ------------------------------------------------------------
        // יצירת הטבלה
        // ------------------------------------------------------------

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
                                (Integer)
                                        parent.getItemAtPosition(position);

                        calendarView.invalidate();
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent) {
                    }
                }
        );

        calendarContainer.removeAllViews();

        int calendarWidth =
                calendarView.getCalendarWidth();

        int calendarHeight =
                calendarView.getCalendarHeight();

        // ------------------------------------------------------------
        // CalendarView
        // ------------------------------------------------------------

        FrameLayout.LayoutParams viewParams =
                new FrameLayout.LayoutParams(
                        calendarWidth,
                        calendarHeight
                );

        calendarContainer.addView(
                calendarView,
                viewParams
        );

        // ------------------------------------------------------------
        // Container
        // ------------------------------------------------------------

        FrameLayout.LayoutParams containerParams =
                (FrameLayout.LayoutParams)
                        calendarContainer.getLayoutParams();

        if (containerParams == null) {

            containerParams =
                    new FrameLayout.LayoutParams(
                            calendarWidth,
                            calendarHeight
                    );

        } else {

            containerParams.width =
                    calendarWidth;

            containerParams.height =
                    calendarHeight;
        }

        calendarContainer.setLayoutParams(
                containerParams
        );

        // ------------------------------------------------------------
        // מחכים שה-ScrollView והטבלה יקבלו מידות אמיתיות
        // ------------------------------------------------------------

        horizontalScroll.post(() -> {

            horizontalScroll.requestLayout();

            verticalScroll.requestLayout();

            horizontalScroll.post(() -> {

                /*
                 * הילד הישיר של HorizontalScrollView
                 * הוא ה-ScrollView האנכי.
                 */
                View horizontalChild =
                        horizontalScroll.getChildAt(0);

                if (horizontalChild == null) {
                    return;
                }

                /*
                 * רוחב התוכן האמיתי.
                 *
                 * אנחנו לוקחים את הרוחב של ה-ScrollView
                 * האנכי, שבתוכו נמצא כל הלוח.
                 */
                int contentWidth =
                        horizontalChild.getMeasuredWidth();

                /*
                 * אם מסיבה כלשהי הרוחב של ה-ScrollView
                 * עדיין לא נקלט, משתמשים ברוחב הטבלה.
                 */
                if (contentWidth <= 0) {

                    contentWidth =
                            calendarContainer.getMeasuredWidth();
                }

                /*
                 * רוחב החלון הנראה.
                 */
                int viewportWidth =
                        horizontalScroll.getWidth();

                /*
                 * המרחק המקסימלי האפשרי בגלילה.
                 */
                int maxScrollX =
                        Math.max(
                                0,
                                contentWidth -
                                        viewportWidth
                        );

                /*
                 * מתחילים בצד הימני ביותר של הטבלה.
                 *
                 * מכיוון שהטבלה עצמה מצוירת כך שהצד
                 * הימני נמצא בתחילת ה-Canvas, אנחנו
                 * משתמשים ב-maxScrollX של ה-ScrollView
                 * כדי להגיע לקצה הימני הפיזי.
                 */
                horizontalScroll.scrollTo(
                        maxScrollX,
                        0
                );
            });
        });
    }

    // ============================================================
    // CALENDAR VIEW
    // ============================================================

    private class CalendarTableView extends View {

        // ========================================================
        // DIMENSIONS
        // ========================================================


        private String getHebrewMonthName(
                int year,
                int month) {

            HebrewCalendar hebrew =
                    new HebrewCalendar();

            Calendar gregorian =
                    Calendar.getInstance();

            gregorian.set(
                    year,
                    month,
                    1
            );

            hebrew.setTimeInMillis(
                    gregorian.getTimeInMillis()
            );

            int hebrewMonth =
                    hebrew.get(
                            HebrewCalendar.MONTH
                    );

            boolean leapYear =
                    hebrew.getActualMaximum(
                            HebrewCalendar.MONTH
                    ) == 12;

            if (leapYear) {

                switch (hebrewMonth) {

                    case 0:
                        return "תשרי";

                    case 1:
                        return "חשון";

                    case 2:
                        return "כסלו";

                    case 3:
                        return "טבת";

                    case 4:
                        return "שבט";

                    case 5:
                        return "אדר א";

                    case 6:
                        return "אדר ב";

                    case 7:
                        return "ניסן";

                    case 8:
                        return "אייר";

                    case 9:
                        return "סיון";

                    case 10:
                        return "תמוז";

                    case 11:
                        return "אב";

                    case 12:
                        return "אלול";
                }

            } else {

                switch (hebrewMonth) {

                    case 0:
                        return "תשרי";

                    case 1:
                        return "חשון";

                    case 2:
                        return "כסלו";

                    case 3:
                        return "טבת";

                    case 4:
                        return "שבט";

                    case 5:
                        return "אדר";

                    case 6:
                        return "ניסן";

                    case 7:
                        return "אייר";

                    case 8:
                        return "סיון";

                    case 9:
                        return "תמוז";

                    case 10:
                        return "אב";

                    case 11:
                        return "אלול";
                }
            }

            return "";
        }

        private final float density;

        /*
         * חשוב מאוד:
         *
         * 31 ימים בלבד אינם מספיקים.
         *
         * אם חודש מתחיל בשבת:
         *
         * 6 תאים ריקים
         * +
         * 31 ימים
         * =
         * 37 עמודות.
         *
         * לכן אנחנו משתמשים ב-37.
         */
        private final int DAYS = 37;

        /*
         * רוחב כל עמודת יום.
         */
        private final int DAY_WIDTH;

        /*
         * גובה שורה.
         */
        private final int ROW_HEIGHT;

        /*
         * עמודת המידע של החודש.
         */
        private final int MONTH_WIDTH;

        /*
         * עמודת השנה.
         */
        private final int YEAR_WIDTH;

        /*
         * חודש אחד = 7 שורות.
         */
        private final int MONTH_HEIGHT;

        /*
         * דצמבר 2022
         * +
         * ינואר-דצמבר 2023.
         */
        private final int MONTHS = 13;

        private final int TABLE_WIDTH;

        private final int TABLE_HEIGHT;

        // ========================================================
        // START DATE
        // ========================================================

        private int selectedYear = 2026;

        private final int START_MONTH =
                Calendar.DECEMBER;

        // ========================================================
        // ZOOM
        // ========================================================

        private float scaleFactor = 1.0f;

        private final float MIN_SCALE = 0.25f;

        private final float MAX_SCALE = 3.0f;

        private final ScaleGestureDetector scaleDetector;

        // ========================================================
        // TOUCH
        // ========================================================

        private float downX;

        private float downY;

        private boolean moved = false;

        private static final int TOUCH_SLOP = 20;

        // ========================================================
        // COLORS
        // ========================================================

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

        private final int COLOR_WHITE =
                Color.WHITE;

        // ========================================================
        // PAINTS
        // ========================================================

        private final Paint fill =
                new Paint(Paint.ANTI_ALIAS_FLAG);

        private final Paint line =
                new Paint(Paint.ANTI_ALIAS_FLAG);

        private final Paint text =
                new Paint(Paint.ANTI_ALIAS_FLAG);

        // ========================================================
        // HEBREW MONTHS
        // ========================================================

        private final String[] hebrewMonths = {

                "ינואר",
                "פברואר",
                "מרץ",
                "אפריל",
                "מאי",
                "יוני",
                "יולי",
                "אוגוסט",
                "ספטמבר",
                "אוקטובר",
                "נובמבר",
                "דצמבר"
        };

        // ========================================================
        // ENGLISH MONTHS
        // ========================================================

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

        // ========================================================
        // HEBREW DAYS
        // ========================================================

        private final String[] hebrewWeekDays = {

                "א",
                "ב",
                "ג",
                "ד",
                "ה",
                "ו",
                "ש"
        };

        // ========================================================
        // CONSTRUCTOR
        // ========================================================

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

            /*
             * עכשיו יש 37 עמודות ימים.
             *
             * 37 * DAY_WIDTH
             * +
             * MONTH_WIDTH
             * +
             * YEAR_WIDTH
             */
            TABLE_WIDTH =
                    DAYS * DAY_WIDTH
                            + MONTH_WIDTH
                            + YEAR_WIDTH;

            TABLE_HEIGHT =
                    MONTHS * MONTH_HEIGHT;

            // ----------------------------------------------------
            // PAINT
            // ----------------------------------------------------

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

            text.setAntiAlias(true);

            setBackgroundColor(
                    Color.WHITE
            );

            setClickable(true);

            // ----------------------------------------------------
            // SCALE
            // ----------------------------------------------------

            scaleDetector =
                    new ScaleGestureDetector(
                            context,
                            new ScaleListener()
                    );
        }

        // ========================================================
        // DP
        // ========================================================

        private int dp(int value) {

            return (int)
                    (value * density + 0.5f);
        }

        // ========================================================
        // WIDTH
        // ========================================================

        int getCalendarWidth() {

            return Math.round(
                    TABLE_WIDTH *
                            scaleFactor
            );
        }

        // ========================================================
        // HEIGHT
        // ========================================================

        int getCalendarHeight() {

            return Math.round(
                    TABLE_HEIGHT *
                            scaleFactor
            );
        }


        private float getMinimumScale() {

            if (horizontalScroll == null ||
                    verticalScroll == null) {

                return 0.10f;
            }

            int availableWidth =
                    horizontalScroll.getWidth();

            int availableHeight =
                    verticalScroll.getHeight();

            if (availableWidth <= 0 ||
                    availableHeight <= 0) {

                return 0.10f;
            }

            float widthScale =
                    (float) availableWidth /
                            TABLE_WIDTH;

            float heightScale =
                    (float) availableHeight /
                            TABLE_HEIGHT;

            /*
             * מאפשר זום אאוט עד לגודל שבו
             * כל הטבלה יכולה להיכנס למסך.
             *
             * 0.10 הוא גבול ביטחון כדי שלא
             * נגיע לגודל זעיר לחלוטין.
             */
            return Math.max(
                    0.10f,
                    Math.min(
                            widthScale,
                            heightScale
                    )
            );
        }


        // ========================================================
        // UPDATE VIEW SIZE
        // ========================================================

        private void updateViewSize() {

            int newWidth =
                    getCalendarWidth();

            int newHeight =
                    getCalendarHeight();

            // ----------------------------------------------------
            // CalendarView
            // ----------------------------------------------------

            ViewGroup.LayoutParams params =
                    getLayoutParams();

            if (params == null) {

                params =
                        new ViewGroup.LayoutParams(
                                newWidth,
                                newHeight
                        );

            } else {

                params.width =
                        newWidth;

                params.height =
                        newHeight;
            }

            setLayoutParams(params);

            // ----------------------------------------------------
            // Container
            // ----------------------------------------------------

            if (calendarContainer != null) {

                ViewGroup.LayoutParams containerParams =
                        calendarContainer.getLayoutParams();

                if (containerParams == null) {

                    containerParams =
                            new ViewGroup.LayoutParams(
                                    newWidth,
                                    newHeight
                            );

                } else {

                    containerParams.width =
                            newWidth;

                    containerParams.height =
                            newHeight;
                }

                calendarContainer.setLayoutParams(
                        containerParams
                );
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

        // ========================================================
        // DRAW
        // ========================================================

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

            for (int index = 0;
                 index < MONTHS;
                 index++) {

                drawMonth(
                        canvas,
                        index
                );
            }

            drawOuterBorder(
                    canvas
            );

            canvas.restore();
        }

        // ========================================================
        // DRAW MONTH
        // ========================================================

        private void drawMonth(
                Canvas canvas,
                int index) {

            Calendar monthCalendar =
                    Calendar.getInstance();

            monthCalendar.set(
                    selectedYear,
                    START_MONTH,
                    1
            );

            monthCalendar.add(
                    Calendar.MONTH,
                    index
            );

            int actualYear =
                    monthCalendar.get(
                            Calendar.YEAR
                    );

            int actualMonth =
                    monthCalendar.get(
                            Calendar.MONTH
                    );

            int daysInMonth =
                    monthCalendar.getActualMaximum(
                            Calendar.DAY_OF_MONTH
                    );

            // ----------------------------------------------------
            // יום התחלת החודש
            // ----------------------------------------------------

            Calendar firstDay =
                    Calendar.getInstance();

            firstDay.set(
                    actualYear,
                    actualMonth,
                    1
            );

            int firstWeekDay =
                    firstDay.get(
                            Calendar.DAY_OF_WEEK
                    );

            /*
             * Calendar:
             *
             * Sunday = 1
             * Monday = 2
             * ...
             * Saturday = 7
             *
             * לכן:
             *
             * Sunday = 0 תאים ריקים
             * Monday = 1
             * ...
             * Saturday = 6
             */
            int emptyDays =
                    firstWeekDay - 1;

            float top =
                    index * MONTH_HEIGHT;

            // ====================================================
            // BACKGROUND
            // ====================================================

            fill.setColor(
                    COLOR_WHITE
            );

            canvas.drawRect(
                    0,
                    top,
                    TABLE_WIDTH,
                    top + MONTH_HEIGHT,
                    fill
            );

            // ====================================================
            // DAY CELLS
            // ====================================================

            for (int position = 0;
                 position < DAYS;
                 position++) {

                /*
                 * הצד הימני הוא position=0.
                 *
                 * לכן:
                 *
                 * position 0 = העמודה הימנית
                 * position 1 = שמאלה ממנה
                 * וכו'.
                 */
                float x =
                        (DAYS - position - 1)
                                * DAY_WIDTH;

                /*
                 * היום האמיתי.
                 *
                 * אם emptyDays = 5:
                 *
                 * position 0 -> -4
                 * position 1 -> -3
                 * ...
                 * position 5 -> 1
                 *
                 * כלומר יום 1 נמצא בעמודה הנכונה.
                 */
                int day =
                        position -
                                emptyDays +
                                1;

                int color =
                        COLOR_WHITE;

                if (day >= 1 &&
                        day <= daysInMonth) {

                    Calendar current =
                            Calendar.getInstance();

                    current.set(
                            actualYear,
                            actualMonth,
                            day
                    );

                    int weekDay =
                            current.get(
                                    Calendar.DAY_OF_WEEK
                            );

                    if (weekDay ==
                            Calendar.SATURDAY) {

                        color =
                                COLOR_BLUE;

                    } else {

                        color =
                                COLOR_YELLOW;
                    }
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
            }


            // ====================================================
// GREEN ROW - SAME AS "ירוק" IN MONTH INFO COLUMN
// ====================================================

            for (int position = 0;
                 position < DAYS;
                 position++) {

                int day =
                        position -
                                emptyDays +
                                1;

                // תאים ריקים נשארים לבנים
                if (day < 1 ||
                        day > daysInMonth) {

                    continue;
                }

                float x =
                        (DAYS - position - 1)
                                * DAY_WIDTH;

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


            // ====================================================
            // MONTH INFO COLUMN
            // ====================================================

            float monthX =
                    DAYS * DAY_WIDTH;

            // ----------------------------------------------------
            // רקע ורוד
            // ----------------------------------------------------

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

            // ----------------------------------------------------
            // יום בשבוע
            // ----------------------------------------------------

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

            centeredText(
                    canvas,
                    "יום בשבוע",
                    monthX,
                    top,
                    MONTH_WIDTH,
                    ROW_HEIGHT,
                    true
            );

            // ----------------------------------------------------
            // תאריך לועזי
            // ----------------------------------------------------

            fill.setColor(
                    COLOR_PINK
            );

            canvas.drawRect(
                    monthX,
                    top + ROW_HEIGHT,
                    monthX + MONTH_WIDTH,
                    top + ROW_HEIGHT * 2,
                    fill
            );

            // ----------------------------------------------------
            // תאריך עברי
            // ----------------------------------------------------

            fill.setColor(
                    COLOR_YELLOW
            );

            canvas.drawRect(
                    monthX,
                    top + ROW_HEIGHT * 2,
                    monthX + MONTH_WIDTH,
                    top + ROW_HEIGHT * 3,
                    fill
            );

            // ----------------------------------------------------
            // חגים
            // ----------------------------------------------------

            fill.setColor(
                    COLOR_YELLOW
            );

            canvas.drawRect(
                    monthX,
                    top + ROW_HEIGHT * 3,
                    monthX + MONTH_WIDTH,
                    top + ROW_HEIGHT * 4,
                    fill
            );

            // ----------------------------------------------------
            // ירוק
            // ----------------------------------------------------

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

            // ----------------------------------------------------
            // הוצאות
            // ----------------------------------------------------

            fill.setColor(
                    COLOR_BLUE
            );

            canvas.drawRect(
                    monthX,
                    top + ROW_HEIGHT * 5,
                    monthX + MONTH_WIDTH,
                    top + ROW_HEIGHT * 6,
                    fill
            );

            // ----------------------------------------------------
            // הכנסות
            // ----------------------------------------------------

            fill.setColor(
                    COLOR_BLUE
            );

            canvas.drawRect(
                    monthX,
                    top + ROW_HEIGHT * 6,
                    monthX + MONTH_WIDTH,
                    top + ROW_HEIGHT * 7,
                    fill
            );

            // ====================================================
            // YEAR COLUMN
            // ====================================================

            float yearX =
                    DAYS * DAY_WIDTH +
                            MONTH_WIDTH;

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

            centeredText(
                    canvas,
                    String.valueOf(actualYear),
                    yearX,
                    top,
                    YEAR_WIDTH,
                    MONTH_HEIGHT,
                    true
            );

            // ====================================================
            // DAY OF WEEK
            // ====================================================

            for (int position = 0;
                 position < DAYS;
                 position++) {

                /*
                 * יום השבוע אינו תלוי במספר היום.
                 *
                 * העמודה הראשונה מימין:
                 *
                 * א
                 *
                 * אחריה:
                 *
                 * ב
                 * ג
                 * ד
                 * ה
                 * ו
                 * ש
                 *
                 * ואז שוב.
                 */
                int weekDay =
                        position % 7;

                float x =
                        (DAYS - position - 1)
                                * DAY_WIDTH;

                centeredText(
                        canvas,
                        hebrewWeekDays[weekDay],
                        x,
                        top,
                        DAY_WIDTH,
                        ROW_HEIGHT,
                        true
                );
            }

            // ====================================================
            // GREGORIAN DATE
            // ====================================================

            for (int position = 0;
                 position < DAYS;
                 position++) {

                int day =
                        position -
                                emptyDays +
                                1;

                if (day < 1 ||
                        day > daysInMonth) {

                    continue;
                }

                float x =
                        (DAYS - position - 1)
                                * DAY_WIDTH;

                centeredText(
                        canvas,
                        String.valueOf(day),
                        x,
                        top + ROW_HEIGHT,
                        DAY_WIDTH,
                        ROW_HEIGHT,
                        true
                );
            }

            // ====================================================
            // HEBREW DATE
            // ====================================================

            for (int position = 0;
                 position < DAYS;
                 position++) {

                int day =
                        position -
                                emptyDays +
                                1;

                if (day < 1 ||
                        day > daysInMonth) {

                    continue;
                }

                float x =
                        (DAYS - position - 1)
                                * DAY_WIDTH;

                String hebrewDate =
                        getHebrewDay(
                                actualYear,
                                actualMonth,
                                day
                        );

                centeredText(
                        canvas,
                        hebrewDate,
                        x,
                        top + ROW_HEIGHT * 2,
                        DAY_WIDTH,
                        ROW_HEIGHT,
                        false
                );
            }

            // ====================================================
            // HOLIDAYS
            // ====================================================

            drawHolidayRow(
                    canvas,
                    actualYear,
                    actualMonth,
                    daysInMonth,
                    top,
                    emptyDays
            );

            // ====================================================
            // MONTH INFO
            // ====================================================

            String englishMonth =
                    englishMonths[
                            actualMonth
                            ];

            String shortYear =
                    String.valueOf(
                            actualYear
                    ).substring(2);

            centeredText(
                    canvas,
                    englishMonth +
                            "-" +
                            shortYear,
                    monthX,
                    top + ROW_HEIGHT,
                    MONTH_WIDTH,
                    ROW_HEIGHT,
                    true
            );

            centeredText(
                    canvas,
                    getHebrewMonthName(
                            actualYear,
                            actualMonth
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
                    "",
                    monthX,
                    top + ROW_HEIGHT * 4,
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

            // ====================================================
            // GRID
            // ====================================================

            drawGrid(
                    canvas,
                    top
            );

            // ====================================================
            // MONTH BORDER
            // ====================================================

            Paint monthBorder =
                    new Paint(
                            Paint.ANTI_ALIAS_FLAG
                    );

            monthBorder.setStyle(
                    Paint.Style.STROKE
            );

            monthBorder.setStrokeWidth(
                    dp(3)
            );

            monthBorder.setColor(
                    Color.BLACK
            );

            canvas.drawRect(
                    0,
                    top,
                    TABLE_WIDTH,
                    top + MONTH_HEIGHT,
                    monthBorder
            );
        }

        // ========================================================
        // HOLIDAYS
        // ========================================================




        // ========================================================
        // DRAW HOLIDAY
        // ========================================================


        // ========================================================
// HOLIDAYS
// ========================================================

// ========================================================
// DRAW HOLIDAY
// ========================================================

        private void drawHoliday(
                Canvas canvas,
                int day,
                String holiday,
                float top,
                int emptyDays) {

            if (day < 1 ||
                    day > 31) {

                return;
            }

            int position =
                    emptyDays +
                            day -
                            1;

            if (position < 0 ||
                    position >= DAYS) {

                return;
            }

            float x =
                    (DAYS - position - 1)
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




        private void drawHolidayRow(
                Canvas canvas,
                int actualYear,
                int actualMonth,
                int daysInMonth,
                float top,
                int emptyDays) {

            Calendar gregorian =
                    Calendar.getInstance();

            for (int day = 1;
                 day <= daysInMonth;
                 day++) {

                gregorian.set(
                        actualYear,
                        actualMonth,
                        day
                );

                HebrewCalendar hebrew =
                        new HebrewCalendar();

                hebrew.setTimeInMillis(
                        gregorian.getTimeInMillis()
                );

                int hebrewMonth =
                        hebrew.get(HebrewCalendar.MONTH);

                int hebrewDay =
                        hebrew.get(HebrewCalendar.DAY_OF_MONTH);

                String holiday = null;

                /*
                 * תשרי
                 */
                if (hebrewMonth == 0) {

                    if (hebrewDay == 1 ||
                            hebrewDay == 2) {

                        holiday = "ראש השנה";

                    } else if (hebrewDay == 3) {

                        holiday = "צום גדליה";

                    } else if (hebrewDay == 10) {

                        holiday = "יום כיפור";

                    } else if (hebrewDay == 15) {

                        holiday = "סוכות";

                    } else if (hebrewDay == 16) {

                        holiday = "סוכות";

                    } else if (hebrewDay >= 17 &&
                            hebrewDay <= 21) {

                        holiday = "סוכות";

                    } else if (hebrewDay == 22) {

                        holiday = "שמיני עצרת";

                    }
                }

                /*
                 * כסלו / טבת
                 *
                 * חנוכה מתחיל בכ"ה בכסלו
                 * ונמשך שמונה ימים.
                 */
                else if (hebrewMonth == 2 &&
                        hebrewDay >= 25) {

                    holiday = "חנוכה";

                } else if (hebrewMonth == 3 &&
                        hebrewDay <= 2) {

                    holiday = "חנוכה";
                }

                /*
                 * שבט
                 */
                else if (hebrewMonth == 4 &&
                        hebrewDay == 15) {

                    holiday = "ט״ו בשבט";
                }

                /*
                 * אדר / אדר א / אדר ב
                 */
                else if (hebrewMonth == 5 &&
                        hebrewDay == 14) {

                    holiday = "פורים";

                } else if (hebrewMonth == 6 &&
                        hebrewDay == 14) {

                    holiday = "פורים";
                }

                /*
                 * ניסן
                 */
                else if (hebrewMonth == 6 &&
                        hebrewDay == 14) {

                    holiday = "ערב פסח";

                } else if (hebrewMonth == 7 &&
                        hebrewDay >= 15 &&
                        hebrewDay <= 21) {

                    holiday = "פסח";
                }

                /*
                 * אייר
                 */
                else if (hebrewMonth == 8 &&
                        hebrewDay == 18) {

                    holiday = "ל״ג בעומר";
                }

                /*
                 * סיון
                 */
                else if (hebrewMonth == 9 &&
                        hebrewDay == 6) {

                    holiday = "שבועות";
                }

                /*
                 * אם נמצא חג —
                 * מציירים אותו בתא המתאים.
                 */
                if (holiday != null) {

                    drawHoliday(
                            canvas,
                            day,
                            holiday,
                            top,
                            emptyDays
                    );
                }
            }
        }

        // ========================================================
        // GRID
        // ========================================================

        private void drawGrid(
                Canvas canvas,
                float top) {

            // ----------------------------------------------------
            // קווים אנכיים של הימים
            // ----------------------------------------------------

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

            // ----------------------------------------------------
            // גבול בין ימים לחודש
            // ----------------------------------------------------

            float monthX =
                    DAYS * DAY_WIDTH;

            canvas.drawLine(
                    monthX,
                    top,
                    monthX,
                    top + MONTH_HEIGHT,
                    line
            );

            // ----------------------------------------------------
            // גבול בין חודש לשנה
            // ----------------------------------------------------

            float yearX =
                    DAYS * DAY_WIDTH +
                            MONTH_WIDTH;

            canvas.drawLine(
                    yearX,
                    top,
                    yearX,
                    top + MONTH_HEIGHT,
                    line
            );

            // ----------------------------------------------------
            // גבול ימני
            // ----------------------------------------------------

            canvas.drawLine(
                    TABLE_WIDTH,
                    top,
                    TABLE_WIDTH,
                    top + MONTH_HEIGHT,
                    line
            );

            // ----------------------------------------------------
            // קווים אופקיים
            // ----------------------------------------------------

            for (int i = 0;
                 i <= 7;
                 i++) {

                float y =
                        top +
                                i * ROW_HEIGHT;

                canvas.drawLine(
                        0,
                        y,
                        TABLE_WIDTH,
                        y,
                        line
                );
            }
        }

        // ========================================================
        // OUTER BORDER
        // ========================================================

        private void drawOuterBorder(
                Canvas canvas) {

            Paint border =
                    new Paint(
                            Paint.ANTI_ALIAS_FLAG
                    );

            border.setStyle(
                    Paint.Style.STROKE
            );

            border.setStrokeWidth(
                    dp(2)
            );

            border.setColor(
                    Color.BLACK
            );

            canvas.drawRect(
                    0,
                    0,
                    TABLE_WIDTH,
                    TABLE_HEIGHT,
                    border
            );
        }

        // ========================================================
        // TEXT
        // ========================================================

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

            float centerX =
                    x +
                            width / 2f;

            Paint.FontMetrics metrics =
                    text.getFontMetrics();

            float centerY =
                    y +
                            height / 2f -
                            (metrics.ascent +
                                    metrics.descent) /
                                    2f;

            // ----------------------------------------------------
            // שתי שורות
            // ----------------------------------------------------

            if (value.contains("\n")) {

                String[] lines =
                        value.split("\n");

                float lineHeight =
                        height /
                                lines.length;

                for (int i = 0;
                     i < lines.length;
                     i++) {

                    float lineCenter =
                            y +
                                    lineHeight * i +
                                    lineHeight / 2f;

                    Paint.FontMetrics fm =
                            text.getFontMetrics();

                    float baseline =
                            lineCenter -
                                    (fm.ascent +
                                            fm.descent) /
                                            2f;

                    canvas.drawText(
                            lines[i],
                            centerX,
                            baseline,
                            text
                    );
                }

                return;
            }

            canvas.drawText(
                    value,
                    centerX,
                    centerY,
                    text
            );
        }

        // ========================================================
        // HEBREW MONTH NAME
        // ========================================================


        // ========================================================
        // HEBREW DAY
        // ========================================================

        private String getHebrewDay(
                int year,
                int month,
                int day) {

            Calendar gregorian =
                    Calendar.getInstance();

            gregorian.set(
                    year,
                    month,
                    day
            );

            HebrewCalendar hebrew =
                    new HebrewCalendar();

            hebrew.setTimeInMillis(
                    gregorian.getTimeInMillis()
            );

            int hebrewDay =
                    hebrew.get(
                            HebrewCalendar.DAY_OF_MONTH
                    );

            return hebrewNumber(
                    hebrewDay
            );
        }
        // ========================================================
        // HEBREW NUMBER
        // ========================================================

        private String hebrewNumber(
                int number) {

            if (number <= 0) {

                return "";
            }

            String[] units = {

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

            if (number < 20) {

                if (number == 15) {
                    return "טו";
                }

                if (number == 16) {
                    return "טז";
                }

                if (number >= 10) {

                    return "י" +
                            units[number - 10];
                }

                return units[number];
            }

            if (number < 30) {

                return "כ" +
                        units[number - 20];
            }

            if (number == 30) {

                return "ל";
            }

            if (number == 31) {

                return "לא";
            }

            return String.valueOf(
                    number
            );
        }

        // ========================================================
        // SCALE LISTENER
        // ========================================================

        private class ScaleListener
                extends ScaleGestureDetector
                .SimpleOnScaleGestureListener {

            @Override
            public boolean onScale(
                    ScaleGestureDetector detector) {

                float oldScale =
                        scaleFactor;

                float minimumScale =
                        getMinimumScale();

                float newScale =
                        scaleFactor *
                                detector.getScaleFactor();

                newScale =
                        Math.max(
                                minimumScale,
                                Math.min(
                                        MAX_SCALE,
                                        newScale
                                )
                        );

                if (Math.abs(
                        newScale - oldScale
                ) < 0.001f) {

                    return true;
                }

                scaleFactor =
                        newScale;

                updateViewSize();

                return true;
            }

            @Override
            public boolean onScaleBegin(
                    ScaleGestureDetector detector) {

                return true;
            }
        }

        // ========================================================
        // TOUCH
        // ========================================================

        @Override
        public boolean onTouchEvent(
                MotionEvent event) {

            scaleDetector.onTouchEvent(
                    event
            );

            switch (event.getActionMasked()) {

                case MotionEvent.ACTION_DOWN:

                    downX =
                            event.getX();

                    downY =
                            event.getY();

                    moved = false;

                    return true;

                case MotionEvent.ACTION_POINTER_DOWN:

                    moved = true;

                    getParent()
                            .requestDisallowInterceptTouchEvent(
                                    true
                            );

                    return true;

                case MotionEvent.ACTION_MOVE:

                    if (scaleDetector.isInProgress()) {

                        moved = true;

                        getParent()
                                .requestDisallowInterceptTouchEvent(
                                        true
                                );

                        return true;
                    }

                    float dx =
                            Math.abs(
                                    event.getX() -
                                            downX
                            );

                    float dy =
                            Math.abs(
                                    event.getY() -
                                            downY
                            );

                    if (dx > TOUCH_SLOP ||
                            dy > TOUCH_SLOP) {

                        moved = true;

                        /*
                         * מאפשר ל-HorizontalScrollView
                         * ול-ScrollView לבצע את הגלילה.
                         */
                        getParent()
                                .requestDisallowInterceptTouchEvent(
                                        false
                                );
                    }

                    return true;

                case MotionEvent.ACTION_POINTER_UP:

                    return true;

                case MotionEvent.ACTION_UP:

                    if (!moved &&
                            !scaleDetector.isInProgress()) {

                        handleClick(
                                event.getX(),
                                event.getY()
                        );
                    }

                    performClick();

                    return true;
            }

            return true;
        }

        // ========================================================
        // CLICK
        // ========================================================

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

            /*
             * אם לחצו באזור החודש או השנה,
             * אין כאן יום.
             */
            if (realX >= DAYS * DAY_WIDTH) {

                return;
            }

            /*
             * בגלל שהימים מצוירים מימין לשמאל:
             *
             * x=0 הוא הצד הימני.
             */
            int position =
                    DAYS -
                            1 -
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

            int selectedYear =
                    selected.get(
                            Calendar.YEAR
                    );

            int selectedMonth =
                    selected.get(
                            Calendar.MONTH
                    );

            Calendar firstDay =
                    Calendar.getInstance();

            firstDay.set(
                    selectedYear,
                    selectedMonth,
                    1
            );

            int emptyDays =
                    firstDay.get(
                            Calendar.DAY_OF_WEEK
                    ) - 1;

            /*
             * ממירים את מיקום העמודה
             * ליום אמיתי.
             */
            int day =
                    position -
                            emptyDays +
                            1;

            int daysInMonth =
                    selected.getActualMaximum(
                            Calendar.DAY_OF_MONTH
                    );

            if (day < 1 ||
                    day > daysInMonth) {

                return;
            }

            Toast.makeText(
                    requireContext(),
                    day +
                            " " +
                            hebrewMonths[
                                    selectedMonth
                                    ] +
                            " " +
                            selectedYear,
                    Toast.LENGTH_SHORT
            ).show();
        }

        // ========================================================
        // ACCESSIBILITY / CLICK
        // ========================================================

        @Override
        public boolean performClick() {

            super.performClick();

            return true;
        }
    }
}