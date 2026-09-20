package com.israel.hebrewcalendar;






public class HebrewMonth {

    String name;

    String sunday;
    String monday;
    String tuesday;
    String wednesday;
    String thursday;
    String friday;
    String saturday;

    public HebrewMonth(
            String name,
            String sunday,
            String monday,
            String tuesday,
            String wednesday,
            String thursday,
            String friday,
            String saturday) {

        this.name = name;

        this.sunday = sunday;
        this.monday = monday;
        this.tuesday = tuesday;
        this.wednesday = wednesday;
        this.thursday = thursday;
        this.friday = friday;
        this.saturday = saturday;
    }
}
