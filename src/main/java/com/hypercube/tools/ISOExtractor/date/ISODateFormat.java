package com.hypercube.tools.ISOExtractor.date;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ISODateFormat {
    int year;
    int month;
    int day;
    int hour;
    int minute;
    int second;
    int secondFrac;
    int gmtOffset;

    @Override
    public String toString() {
        int gmt = (-48 + gmtOffset) * 15;
        int gmtHour = (int) Math.floor(gmt / 60f);
        int gmtMinute = gmt % 60;
        return String.format("%d.%d.%d %d:%d:%d GMT%d:%d",
                day, month, year, hour, minute, second, gmtHour, gmtMinute);
    }


}
