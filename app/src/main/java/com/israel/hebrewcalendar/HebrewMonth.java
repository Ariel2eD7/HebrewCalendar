package com.israel.hebrewcalendar;

public class HebrewMonth {

    String name;

    // היום בשבוע שבו מתחיל היום הראשון של החודש
// 0 = א׳
// 1 = ב׳
// 2 = ג׳
// 3 = ד׳
// 4 = ה׳
// 5 = ו׳
// 6 = שבת
    int startDayOfWeek;

    // מספר הימים בחודש: 29 או 30
    int daysInMonth;

    public HebrewMonth(
            String name,
            int startDayOfWeek,
            int daysInMonth) {

        this.name = name;
        this.startDayOfWeek = startDayOfWeek;
        this.daysInMonth = daysInMonth;
    }

}
