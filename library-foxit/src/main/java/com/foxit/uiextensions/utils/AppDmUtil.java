/**
 * Copyright (C) 2003-2019, Foxit Software Inc..
 * All Rights Reserved.
 * <p>
 * http://www.foxitsoftware.com
 * <p>
 * The following code is copyrighted and is the proprietary of Foxit Software Inc.. It is not allowed to
 * distribute any parts of Foxit PDF SDK to third party or public without permission unless an agreement
 * is signed between Foxit Software Inc. and customers to explicitly grant customers permissions.
 * Review legal.txt for additional license and legal information.
 */
package com.foxit.uiextensions.utils;

import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.format.Time;

import com.foxit.sdk.common.DateTime;

import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

public class AppDmUtil {

    private static final int MICROSECONDS_PER_MINUTE = 60000;
    private static final int MICROSECONDS_PER_HOUR = 3600000;
    private static final int BEGIN_YEAR = 1900;

    public static final String dateOriValue = "0000-00-00 00:00:00 GMT+00'00'";

    public static String getLocalDateString(DateTime dateTime){
        if(isZero(dateTime))
            return dateOriValue;
        return getLocalDateString(documentDateToJavaDate(dateTime));
    }

    public static boolean isZero(DateTime dateTime){
        return dateTime.getYear() == 0 &&
                    dateTime.getMonth() == 0 &&
                    dateTime.getDay() == 0 &&
                    dateTime.getHour() == 0 &&
                    dateTime.getMinute() == 0 &&
                    dateTime.getSecond() == 0 &&
                    dateTime.getUtc_hour_offset() == 0 &&
                    dateTime.getUtc_minute_offset() == 0;
    }

    public static String getLocalDateString(Date date){
        return date.toString();
    }

    public static DateTime javaDateToDocumentDate(long date){
        return javaDateToDocumentDate(new Date(date));
    }

    public static DateTime javaDateToDocumentDate(Date date){
        if(date == null)
            return null;

        int year = date.getYear() + BEGIN_YEAR;
        int month = date.getMonth() + 1;
        int day = date.getDate();
        int hour = date.getHours();
        int minute = date.getMinutes();
        int second = date.getSeconds();
        int timezone = TimeZone.getDefault().getRawOffset();
        int localHour = timezone / MICROSECONDS_PER_HOUR;
        int localMinute = timezone % MICROSECONDS_PER_HOUR / MICROSECONDS_PER_MINUTE;

        DateTime dateTime = new DateTime();
        dateTime.set(year, month, day, hour, minute, second, 0, (short) localHour, localMinute);

        return dateTime;
    }
    public static DateTime currentDateToDocumentDate() {
        Time now = new Time();
        now.setToNow();

        int year = now.year;
        int month = now.month + 1;
        int date = now.monthDay;
        int hour = now.hour;
        int minute = now.minute;
        int second = now.second;
        int timezone = TimeZone.getDefault().getRawOffset();
        int localHour = timezone / MICROSECONDS_PER_HOUR;
        int localMinute = timezone % MICROSECONDS_PER_HOUR / MICROSECONDS_PER_MINUTE;

        DateTime dateTime = new DateTime();
        dateTime.set(year, month, date, hour, minute, second, 0, (short) localHour, localMinute);

        return dateTime;
    }

    public static DateTime parseDocumentDate(String date){
        if(date == null)
            return null;
       return javaDateToDocumentDate(Date.parse(date));
    }

    public static Date documentDateToJavaDate(final DateTime dateTime){
        if(dateTime == null)
            return null;
        Date date = new Date();
        date.setYear(dateTime.getYear() - BEGIN_YEAR);
        date.setMonth(dateTime.getMonth() - 1);
        date.setDate(dateTime.getDay());
        date.setHours(dateTime.getHour());
        date.setMinutes(dateTime.getMinute());
        date.setSeconds(dateTime.getSecond());
        int rawOffset =dateTime.getUtc_minute_offset() * MICROSECONDS_PER_MINUTE + dateTime.getUtc_hour_offset() * MICROSECONDS_PER_HOUR - TimeZone.getDefault().getRawOffset();
        return new Date(date.getTime() - rawOffset);
    }

    public static RectF rectToRectF(Rect rect) {
        return new RectF(rect);
    }

    public static Rect rectFToRect(RectF rect) {
        return new Rect((int) rect.left, (int) rect.top, (int) rect.right, (int) rect.bottom);
    }

    public static int opacity100To255(int opacity) {
        if (opacity < 0 || opacity >= 100) {
            return 255;
        }
        return Math.min(255, (int) (opacity / 100.0f * 256));
    }

    public static int opacity255To100(int opacity) {
        if (opacity < 0 || opacity >= 255) {
            return 100;
        }
        return (int) (opacity / 256.0f * 100);
    }

    public static String randomUUID(String separator) {
        String uuid = UUID.randomUUID().toString();
        if (separator != null) {
            uuid = uuid.replace("-", separator);
        }
        return uuid;
    }

    public static int calColorByMultiply(int color, int alpha) {
        int rColor = color | 0xFF000000;
        int r = (rColor & 0xFF0000) >> 16;
        int g = (rColor & 0xFF00) >> 8;
        int b = (rColor & 0xFF);
        float opacity = alpha / 255.0f;
        r = (int) (r * opacity + 255 * (1 - opacity));
        g = (int) (g * opacity + 255 * (1 - opacity));
        b = (int) (b * opacity + 255 * (1 - opacity));
        rColor = (rColor & 0xFF000000) | (r << 16) | (g << 8) | (b);
        return rColor;
    }

    public static float distanceOfTwoPoints(PointF p1, PointF p2) {
        return (float)(Math.sqrt((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y)));
    }

    public static float distanceFromPointToLine(float x, float y, float x1, float y1, float x2, float y2) {
        float k = 0;
        float b = 0;
        if (x1 == x2) {
            return Math.abs(x - x1);
        } else if (y1 == y2) {
            return Math.abs(y - y1);
        } else {
            k = (y2 - y1) / (x2 - x1);
            b = y2 - k * x2;
            return (float)(Math.abs(k * x - y + b) / Math.sqrt(k * k + 1));
        }
    }

    public static float distanceFromPointToLine(PointF p, PointF p1, PointF p2) {
        return distanceFromPointToLine(p.x, p.y, p1.x, p1.y, p2.x, p2.y);
    }

    public static boolean isPointVerticalIntersectOnLine(float x, float y, float x1, float y1, float x2, float y2) {
        boolean result = false;
        double cross = (x2 - x1) * (x - x1) + (y2 - y1) * (y - y1);
        double d = (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1);
        double r = cross / d;
        if (r > 0 && r < 1) {
            result = true;
        }
        return result;
    }

    public static boolean isPointVerticalIntersectOnLine(PointF p, PointF p1, PointF p2) {
        return isPointVerticalIntersectOnLine(p.x, p.y, p1.x, p1.y, p2.x, p2.y);
    }

    public static String getAnnotAuthor() {
        return "foxit sdk";
    }

    public static boolean isValidDateTime(DateTime dateTime) {
        if (dateTime.getMonth() < 1 ||
                dateTime.getMonth() > 12 ||
                dateTime.getDay() < 1 ||
                dateTime.getDay() > 31 ||
                dateTime.getHour() > 23 ||
                dateTime.getMinute() > 59 ||
                dateTime.getMilliseconds() > 999 ||
                dateTime.getSecond() > 60 ||
                dateTime.getUtc_hour_offset() < -12 ||
                dateTime.getUtc_hour_offset() > 12 ||
                dateTime.getMilliseconds() > 59)
            return false;
        else
            return true;

    }
}
