package com.jay.zcy.app;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by fan on 15-12-6.
 */
public class CurentTimeString {

    public static String getTime(){
        Calendar c=Calendar.getInstance();
        DateFormat dateFormat=new SimpleDateFormat("MM-dd,hh:mm:ss:SSS");
        return dateFormat.format(c.getTimeInMillis());
    }
}
