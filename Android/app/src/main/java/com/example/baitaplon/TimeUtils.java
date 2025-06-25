package com.example.baitaplon;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
public class TimeUtils {
    public static String getRelativeTime(String isoCreatedAt) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        try {
            Date created = sdf.parse(isoCreatedAt);
            long diffMillis = new Date().getTime() - created.getTime();

            long hours = TimeUnit.MILLISECONDS.toHours(diffMillis);
            if (hours < 24) return hours + " giờ trước";

            long days = TimeUnit.MILLISECONDS.toDays(diffMillis);
            return days + " ngày trước";
        } catch (ParseException e) {
            e.printStackTrace();
            return "";
        }
    }
}
