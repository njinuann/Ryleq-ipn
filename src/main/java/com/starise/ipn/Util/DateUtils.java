package com.starise.ipn.Util;


import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

public class DateUtils {

    private static SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy");
    private static SimpleDateFormat isoFormatter = new SimpleDateFormat("yyyy-MM-dd");
    private static SimpleDateFormat isoTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private SimpleDateFormat dateFormatterSlash = new SimpleDateFormat("dd/MM/yyyy");

    public static java.sql.Timestamp isoToSQLDateTime(String string) {
        Date parsedDate;
        try {
            if (string == null) {
                return null;
            } else if (string.equals("")) {
                return null;
            }
            if (string.length() == 10)
                parsedDate = isoFormatter.parse(string);
            else
                parsedDate = isoTimeFormatter.parse(string);
        } catch (ParseException e) {
            // LogUtils.getLogger().error("====================== convertToDate Date string
            // " + string);
            // LogUtils.getLogger().error(e);
            return null;
        }
        return new java.sql.Timestamp(parsedDate.getTime());
    }

    public static java.sql.Date isoToSQLDate(String string) {
        Date parsedDate;
        try {
            if (string == null) {
                return null;
            } else if (string.equals("")) {
                return null;
            }
            string = string.replaceAll("/", "-");
            parsedDate = isoFormatter.parse(string);
        } catch (ParseException e) {
            // LogUtils.getLogger().error("====================== convertToDate Date string
            // " + string);
            // LogUtils.getLogger().error(e);
            return null;
        }
        return new java.sql.Date(parsedDate.getTime());
    }

    public static Date convertToDate(String string) {
        Date parsedDate;
        try {
            parsedDate = dateFormatter.parse(string);
        } catch (ParseException e) {
//            LogUtils.getLogger().error("====================== convertToDate Date string " + string);
//            LogUtils.getLogger().error(e);
            parsedDate = new Date();
        }
        return parsedDate;
    }

    public static String toString(Date date) {
        String parsedDate = null;
        try {
            parsedDate = isoFormatter.format(date);
        } catch (Exception e) {
            //LogUtils.getLogger().error(e);
        }
        return parsedDate;
    }

    public static String toString(LocalDate dateInput){
        ZoneId defaultZoneId = ZoneId.systemDefault();
        Date date = Date.from(dateInput.atStartOfDay(defaultZoneId).toInstant());
        return isoFormatter.format(date);
    }

    public static String toTimeString(Date date) {
        String parsedDate = null;
        try {
            parsedDate = isoTimeFormatter.format(date);
        } catch (Exception e) {
            //LogUtils.getLogger().error(e);
        }
        return parsedDate;
    }

    public static XMLGregorianCalendar toGregorianDate(String dateString) throws ParseException, DatatypeConfigurationException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate localDate = LocalDate.parse(dateString, formatter);
        XMLGregorianCalendar xmlGregorianCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(localDate.toString());
        return xmlGregorianCalendar;
    }

    public static Date localeDateToDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
    }

    public static java.sql.Date getCurrentDate() {
        Date today = new Date();
        java.sql.Date date = new java.sql.Date(today.getTime());
        return date;
    }

    public static java.sql.Timestamp getCurrentTimeStamp() {
        Date today = new Date();
        java.sql.Timestamp date = new java.sql.Timestamp(today.getTime());
        return date;
    }

    public static Date getLastDayOfMonth() {
        Date today = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(today);
        calendar.add(Calendar.MONTH, 1);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.add(Calendar.DATE, -1);
        Date lastDayOfMonth = calendar.getTime();
        return lastDayOfMonth;
    }


    public static Date getDueDate(String day) throws ParseException {
        day = AXWorker.padLeftZeros(day, 2);
        final SimpleDateFormat monthFormat = new SimpleDateFormat("MM");
        final SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy");
        String year = yearFormat.format(new Date());
        String month = monthFormat.format(new Date());
        String fullDate = year + "-" + month + "-" + day;
        return isoFormatter.parse(fullDate);
    }
}

