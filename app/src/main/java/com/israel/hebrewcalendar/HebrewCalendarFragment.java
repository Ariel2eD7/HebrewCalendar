package com.israel.hebrewcalendar;

import android.content.Context;
import android.graphics.*;
import android.os.Bundle;
import android.view.*;
import android.view.ScaleGestureDetector;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import android.icu.util.HebrewCalendar;
import java.util.Calendar;

public class HebrewCalendarFragment extends Fragment {

    private CalendarTableView calendarView;
    private FrameLayout calendarContainer;
    private HorizontalScrollView horizontalScroll;
    private ScrollView verticalScroll;

    public HebrewCalendarFragment() {
        super(R.layout.fragment_calendar);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        calendarContainer = view.findViewById(R.id.calendarContainer);
        horizontalScroll = view.findViewById(R.id.horizontalScroll);
        verticalScroll = view.findViewById(R.id.verticalScroll);
        Spinner yearSpinner = view.findViewById(R.id.yearSpinner);

        calendarView = new CalendarTableView(requireContext());

        Integer[] years = new Integer[21];
        for (int i = 0; i < years.length; i++) years[i] = 2020 + i;

        ArrayAdapter<Integer> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                years
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        yearSpinner.setAdapter(adapter);

        yearSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int position, long id) {
                calendarView.selectedYear = (Integer) p.getItemAtPosition(position);
                calendarView.invalidate();
            }

            @Override
            public void onNothingSelected(AdapterView<?> p) {}
        });

        calendarContainer.removeAllViews();

        int w = calendarView.getCalendarWidth();
        int h = calendarView.getCalendarHeight();

        calendarContainer.addView(
                calendarView,
                new FrameLayout.LayoutParams(w, h)
        );

        FrameLayout.LayoutParams cp =
                (FrameLayout.LayoutParams) calendarContainer.getLayoutParams();

        if (cp == null) cp = new FrameLayout.LayoutParams(w, h);
        else {
            cp.width = w;
            cp.height = h;
        }

        calendarContainer.setLayoutParams(cp);

        horizontalScroll.post(() -> {
            horizontalScroll.requestLayout();
            verticalScroll.requestLayout();

            horizontalScroll.post(() -> {
                View child = horizontalScroll.getChildAt(0);
                if (child == null) return;

                int contentWidth = child.getMeasuredWidth();
                if (contentWidth <= 0) contentWidth = calendarContainer.getMeasuredWidth();

                horizontalScroll.scrollTo(
                        Math.max(0, contentWidth - horizontalScroll.getWidth()),
                        0
                );
            });
        });
    }

    private class CalendarTableView extends View {

        private final float density;
        private final int DAYS = 37;
        private final int DAY_WIDTH, ROW_HEIGHT, MONTH_WIDTH, YEAR_WIDTH;
        private final int MONTH_HEIGHT, MONTHS = 13;
        private final int TABLE_WIDTH, TABLE_HEIGHT;

        private int selectedYear = 2026;
        private final int START_MONTH = Calendar.DECEMBER;

        private float scaleFactor = 1f;
        private final float MAX_SCALE = 3f;
        private final ScaleGestureDetector scaleDetector;

        private float downX, downY;
        private boolean moved;
        private static final int TOUCH_SLOP = 20;

        private final int COLOR_YELLOW = Color.rgb(255, 230, 153);
        private final int COLOR_BLUE = Color.rgb(189, 215, 238);
        private final int COLOR_PINK = Color.rgb(255, 153, 255);
        private final int COLOR_GREEN = Color.rgb(198, 224, 180);
        private final int COLOR_GRAY = Color.rgb(174, 170, 170);

        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);

        private final String[] hebrewMonths = {
                "ינואר","פברואר","מרץ","אפריל","מאי","יוני",
                "יולי","אוגוסט","ספטמבר","אוקטובר","נובמבר","דצמבר"
        };

        private final String[] englishMonths = {
                "Jan","Feb","Mar","Apr","May","Jun",
                "Jul","Aug","Sep","Oct","Nov","Dec"
        };

        private final String[] hebrewWeekDays = {"א","ב","ג","ד","ה","ו","ש"};

        CalendarTableView(Context context) {
            super(context);

            density = getResources().getDisplayMetrics().density;

            DAY_WIDTH = dp(55);
            ROW_HEIGHT = dp(34);
            MONTH_WIDTH = dp(120);
            YEAR_WIDTH = dp(85);
            MONTH_HEIGHT = ROW_HEIGHT * 7;

            TABLE_WIDTH = DAYS * DAY_WIDTH + MONTH_WIDTH + YEAR_WIDTH;
            TABLE_HEIGHT = MONTHS * MONTH_HEIGHT;

            fill.setStyle(Paint.Style.FILL);

            line.setStyle(Paint.Style.STROKE);
            line.setStrokeWidth(dp(1));
            line.setColor(Color.rgb(40, 40, 40));

            text.setColor(Color.BLACK);
            text.setTextSize(dp(12));
            text.setAntiAlias(true);

            border.setStyle(Paint.Style.STROKE);
            border.setColor(Color.BLACK);

            setBackgroundColor(Color.WHITE);
            setClickable(true);

            scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        }

        private int dp(int value) {
            return (int) (value * density + .5f);
        }

        int getCalendarWidth() {
            return Math.round(TABLE_WIDTH * scaleFactor);
        }

        int getCalendarHeight() {
            return Math.round(TABLE_HEIGHT * scaleFactor);
        }

        private float getMinimumScale() {
            if (horizontalScroll == null || verticalScroll == null) return .1f;

            int w = horizontalScroll.getWidth();
            int h = verticalScroll.getHeight();
            if (w <= 0 || h <= 0) return .1f;

            return Math.max(
                    .1f,
                    Math.min((float) w / TABLE_WIDTH, (float) h / TABLE_HEIGHT)
            );
        }

        private void updateViewSize() {
            int w = getCalendarWidth(), h = getCalendarHeight();

            ViewGroup.LayoutParams p = getLayoutParams();
            if (p == null) p = new ViewGroup.LayoutParams(w, h);
            else {
                p.width = w;
                p.height = h;
            }
            setLayoutParams(p);

            if (calendarContainer != null) {
                ViewGroup.LayoutParams cp = calendarContainer.getLayoutParams();
                if (cp == null) cp = new ViewGroup.LayoutParams(w, h);
                else {
                    cp.width = w;
                    cp.height = h;
                }
                calendarContainer.setLayoutParams(cp);
            }

            requestLayout();
            if (horizontalScroll != null) horizontalScroll.requestLayout();
            if (verticalScroll != null) verticalScroll.requestLayout();
            invalidate();
        }

        @Override
        protected void onDraw(@NonNull Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawColor(Color.WHITE);
            canvas.save();
            canvas.scale(scaleFactor, scaleFactor);

            for (int i = 0; i < MONTHS; i++) drawMonth(canvas, i);

            drawOuterBorder(canvas);
            canvas.restore();
        }

        private void drawMonth(Canvas canvas, int index) {
            Calendar cal = Calendar.getInstance();
            cal.set(selectedYear, START_MONTH, 1);
            cal.add(Calendar.MONTH, index);

            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH);
            int days = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

            Calendar first = Calendar.getInstance();
            first.set(year, month, 1);

            int empty = first.get(Calendar.DAY_OF_WEEK) - 1;
            float top = index * MONTH_HEIGHT;
            float monthX = DAYS * DAY_WIDTH;
            float yearX = monthX + MONTH_WIDTH;

            fill.setColor(Color.WHITE);
            canvas.drawRect(0, top, TABLE_WIDTH, top + MONTH_HEIGHT, fill);

            for (int pos = 0; pos < DAYS; pos++) {
                float x = (DAYS - pos - 1) * DAY_WIDTH;
                int day = pos - empty + 1;
                int color = Color.WHITE;

                if (day >= 1 && day <= days) {
                    Calendar current = Calendar.getInstance();
                    current.set(year, month, day);
                    color = current.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY
                            ? COLOR_BLUE : COLOR_YELLOW;
                }

                fill.setColor(color);
                canvas.drawRect(x, top, x + DAY_WIDTH, top + MONTH_HEIGHT, fill);

                if (day >= 1 && day <= days) {
                    fill.setColor(COLOR_GREEN);
                    canvas.drawRect(
                            x,
                            top + ROW_HEIGHT * 4,
                            x + DAY_WIDTH,
                            top + ROW_HEIGHT * 5,
                            fill
                    );
                }
            }

            fill.setColor(COLOR_PINK);
            canvas.drawRect(monthX, top, monthX + MONTH_WIDTH, top + MONTH_HEIGHT, fill);

            fill.setColor(COLOR_YELLOW);
            canvas.drawRect(monthX, top, monthX + MONTH_WIDTH, top + ROW_HEIGHT, fill);
            canvas.drawRect(monthX, top + ROW_HEIGHT * 2, monthX + MONTH_WIDTH, top + ROW_HEIGHT * 3, fill);
            canvas.drawRect(monthX, top + ROW_HEIGHT * 3, monthX + MONTH_WIDTH, top + ROW_HEIGHT * 4, fill);

            fill.setColor(COLOR_GREEN);
            canvas.drawRect(monthX, top + ROW_HEIGHT * 4, monthX + MONTH_WIDTH, top + ROW_HEIGHT * 5, fill);

            fill.setColor(COLOR_BLUE);
            canvas.drawRect(monthX, top + ROW_HEIGHT * 5, monthX + MONTH_WIDTH, top + ROW_HEIGHT * 7, fill);

            fill.setColor(COLOR_GRAY);
            canvas.drawRect(yearX, top, yearX + YEAR_WIDTH, top + MONTH_HEIGHT, fill);

            centeredText(canvas, "יום בשבוע", monthX, top, MONTH_WIDTH, ROW_HEIGHT, true);
            centeredText(
                    canvas,
                    englishMonths[month] + "-" + String.valueOf(year).substring(2),
                    monthX, top + ROW_HEIGHT, MONTH_WIDTH, ROW_HEIGHT, true
            );
            centeredText(
                    canvas,
                    getHebrewMonthName(year, month),
                    monthX, top + ROW_HEIGHT * 2, MONTH_WIDTH, ROW_HEIGHT, true
            );
            centeredText(canvas, "חגים ומועדים", monthX, top + ROW_HEIGHT * 3, MONTH_WIDTH, ROW_HEIGHT, false);
            centeredText(canvas, "הוצאות\nחזויות", monthX, top + ROW_HEIGHT * 5, MONTH_WIDTH, ROW_HEIGHT, true);
            centeredText(canvas, "הכנסות\nחזויות", monthX, top + ROW_HEIGHT * 6, MONTH_WIDTH, ROW_HEIGHT, true);
            centeredText(canvas, String.valueOf(year), yearX, top, YEAR_WIDTH, MONTH_HEIGHT, true);

            for (int pos = 0; pos < DAYS; pos++) {
                float x = (DAYS - pos - 1) * DAY_WIDTH;
                centeredText(canvas, hebrewWeekDays[pos % 7], x, top, DAY_WIDTH, ROW_HEIGHT, true);

                int day = pos - empty + 1;
                if (day < 1 || day > days) continue;

                centeredText(
                        canvas, String.valueOf(day),
                        x, top + ROW_HEIGHT, DAY_WIDTH, ROW_HEIGHT, true
                );

                centeredText(
                        canvas,
                        getHebrewDay(year, month, day),
                        x, top + ROW_HEIGHT * 2, DAY_WIDTH, ROW_HEIGHT, false
                );
            }

            drawHolidayRow(canvas, year, month, days, top, empty);
            drawGrid(canvas, top);

            border.setStrokeWidth(dp(3));
            canvas.drawRect(0, top, TABLE_WIDTH, top + MONTH_HEIGHT, border);
        }

        private void drawHolidayRow(
                Canvas canvas, int year, int month, int days, float top, int empty) {

            Calendar g = Calendar.getInstance();

            for (int day = 1; day <= days; day++) {
                g.set(year, month, day);

                HebrewCalendar h = new HebrewCalendar();
                h.setTimeInMillis(g.getTimeInMillis());

                int hm = h.get(HebrewCalendar.MONTH);
                int hd = h.get(HebrewCalendar.DAY_OF_MONTH);
                String holiday = null;

                if (hm == 0) {
                    if (hd <= 2) holiday = "ראש השנה";
                    else if (hd == 3) holiday = "צום גדליה";
                    else if (hd == 10) holiday = "יום כיפור";
                    else if (hd >= 15 && hd <= 21) holiday = "סוכות";
                    else if (hd == 22) holiday = "שמיני עצרת";
                } else if (hm == 2 && hd >= 25) {
                    holiday = "חנוכה";
                } else if (hm == 3 && hd <= 2) {
                    holiday = "חנוכה";
                } else if (hm == 4 && hd == 15) {
                    holiday = "ט״ו בשבט";
                } else if (hm == 5 && hd == 14) {
                    holiday = "פורים";
                } else if (hm == 6 && hd == 14) {
                    holiday = "פורים";
                } else if (hm == 6 && hd == 14) {
                    holiday = "ערב פסח";
                } else if (hm == 7 && hd >= 15 && hd <= 21) {
                    holiday = "פסח";
                } else if (hm == 8 && hd == 18) {
                    holiday = "ל״ג בעומר";
                } else if (hm == 9 && hd == 6) {
                    holiday = "שבועות";
                }

                if (holiday != null) drawHoliday(canvas, day, holiday, top, empty);
            }
        }

        private void drawHoliday(
                Canvas canvas, int day, String holiday, float top, int empty) {

            int pos = empty + day - 1;
            if (day < 1 || day > 31 || pos < 0 || pos >= DAYS) return;

            float x = (DAYS - pos - 1) * DAY_WIDTH;

            fill.setColor(COLOR_PINK);
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

        private void drawGrid(Canvas canvas, float top) {
            for (int i = 0; i <= DAYS; i++) {
                float x = i * DAY_WIDTH;
                canvas.drawLine(x, top, x, top + MONTH_HEIGHT, line);
            }

            for (int i = 0; i <= 7; i++) {
                float y = top + i * ROW_HEIGHT;
                canvas.drawLine(0, y, TABLE_WIDTH, y, line);
            }
        }

        private void drawOuterBorder(Canvas canvas) {
            border.setStrokeWidth(dp(2));
            canvas.drawRect(0, 0, TABLE_WIDTH, TABLE_HEIGHT, border);
        }

        private void centeredText(
                Canvas canvas, String value, float x, float y,
                float width, float height, boolean bold) {

            if (value == null || value.isEmpty()) return;

            text.setTypeface(bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            text.setTextAlign(Paint.Align.CENTER);

            float centerX = x + width / 2f;
            Paint.FontMetrics fm = text.getFontMetrics();

            if (value.contains("\n")) {
                String[] lines = value.split("\n");
                float lineHeight = height / lines.length;

                for (int i = 0; i < lines.length; i++) {
                    float centerY = y + lineHeight * i + lineHeight / 2f;
                    fm = text.getFontMetrics();

                    canvas.drawText(
                            lines[i],
                            centerX,
                            centerY - (fm.ascent + fm.descent) / 2f,
                            text
                    );
                }
            } else {
                canvas.drawText(
                        value,
                        centerX,
                        y + height / 2f - (fm.ascent + fm.descent) / 2f,
                        text
                );
            }
        }

        private String getHebrewMonthName(int year, int month) {
            Calendar g = Calendar.getInstance();
            g.set(year, month, 1);

            HebrewCalendar h = new HebrewCalendar();
            h.setTimeInMillis(g.getTimeInMillis());

            int m = h.get(HebrewCalendar.MONTH);
            boolean leap = h.getActualMaximum(HebrewCalendar.MONTH) == 12;

            if (leap && m >= 5) {
                String[] months = {
                        "תשרי","חשון","כסלו","טבת","שבט",
                        "אדר א","אדר ב","ניסן","אייר","סיון","תמוז","אב","אלול"
                };
                return months[m];
            }

            String[] months = {
                    "תשרי","חשון","כסלו","טבת","שבט",
                    "אדר","ניסן","אייר","סיון","תמוז","אב","אלול"
            };

            return m < months.length ? months[m] : "";
        }

        private String getHebrewDay(int year, int month, int day) {
            Calendar g = Calendar.getInstance();
            g.set(year, month, day);

            HebrewCalendar h = new HebrewCalendar();
            h.setTimeInMillis(g.getTimeInMillis());

            return hebrewNumber(h.get(HebrewCalendar.DAY_OF_MONTH));
        }

        private String hebrewNumber(int n) {
            if (n <= 0) return "";

            String[] u = {"","א","ב","ג","ד","ה","ו","ז","ח","ט"};

            if (n < 20) {
                if (n == 15) return "טו";
                if (n == 16) return "טז";
                return n >= 10 ? "י" + u[n - 10] : u[n];
            }

            if (n < 30) return "כ" + u[n - 20];
            if (n == 30) return "ל";
            if (n == 31) return "לא";

            return String.valueOf(n);
        }

        private class ScaleListener
                extends ScaleGestureDetector.SimpleOnScaleGestureListener {

            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float old = scaleFactor;
                float min = getMinimumScale();

                scaleFactor = Math.max(
                        min,
                        Math.min(MAX_SCALE, scaleFactor * detector.getScaleFactor())
                );

                if (Math.abs(scaleFactor - old) > .001f) updateViewSize();
                return true;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                return true;
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            scaleDetector.onTouchEvent(event);

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX = event.getX();
                    downY = event.getY();
                    moved = false;
                    return true;

                case MotionEvent.ACTION_POINTER_DOWN:
                    moved = true;
                    getParent().requestDisallowInterceptTouchEvent(true);
                    return true;

                case MotionEvent.ACTION_MOVE:
                    if (scaleDetector.isInProgress()) {
                        moved = true;
                        getParent().requestDisallowInterceptTouchEvent(true);
                        return true;
                    }

                    if (Math.abs(event.getX() - downX) > TOUCH_SLOP ||
                            Math.abs(event.getY() - downY) > TOUCH_SLOP) {

                        moved = true;
                        getParent().requestDisallowInterceptTouchEvent(false);
                    }
                    return true;

                case MotionEvent.ACTION_POINTER_UP:
                    return true;

                case MotionEvent.ACTION_UP:
                    if (!moved && !scaleDetector.isInProgress())
                        handleClick(event.getX(), event.getY());

                    performClick();
                    return true;
            }

            return true;
        }

        private void handleClick(float x, float y) {
            float realX = x / scaleFactor;
            float realY = y / scaleFactor;

            int monthIndex = (int) (realY / MONTH_HEIGHT);
            if (monthIndex < 0 || monthIndex >= MONTHS) return;
            if (realX >= DAYS * DAY_WIDTH) return;

            int position = DAYS - 1 - (int) (realX / DAY_WIDTH);
            if (position < 0 || position >= DAYS) return;

            Calendar selected = Calendar.getInstance();
            selected.set(selectedYear, START_MONTH, 1);
            selected.add(Calendar.MONTH, monthIndex);

            int year = selected.get(Calendar.YEAR);
            int month = selected.get(Calendar.MONTH);

            Calendar first = Calendar.getInstance();
            first.set(year, month, 1);

            int empty = first.get(Calendar.DAY_OF_WEEK) - 1;
            int day = position - empty + 1;

            if (day < 1 || day > selected.getActualMaximum(Calendar.DAY_OF_MONTH))
                return;

            Toast.makeText(
                    requireContext(),
                    day + " " + hebrewMonths[month] + " " + year,
                    Toast.LENGTH_SHORT
            ).show();
        }

        @Override
        public boolean performClick() {
            super.performClick();
            return true;
        }
    }
}
