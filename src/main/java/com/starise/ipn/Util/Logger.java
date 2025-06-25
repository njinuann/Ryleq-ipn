package com.starise.ipn.Util;


import com.starise.ipn.service.MediumsControllerService;
import org.springframework.stereotype.Component;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

@Component("LoggerService")
public class Logger {
    static String indent = "    ";
    private static final LogFile brFile = new LogFile();
    private static String realm = "ipn", logsPath = "IPN" + File.separator + "logs";
    public static void logError(Object event)
    {
        logEvent(null, null, event);
    }
    public static void logInfo(Object event)
    {
        logEvent(null, null, event);
    }
    public static void logEvent(Object event) {
        logEvent("", null, event);
    }

    public static void logEvent(String eventKey, Object event)
    {
        logEvent(eventKey, null, event);
    }

    public  static void  logError(String message, Throwable ex)
    {
        logEvent(null, message, ex);
    }

    public static void logEvent(String eventKey, String message, final Object event)
    {
        try
        {
            final StringBuilder logEvent = new StringBuilder("<event realm=\"" + realm + "\" " + (eventKey != null ? "key=\"" + eventKey + "\" " : "") + "datetime=\"" + new Date() + "\">");
            if (event instanceof Throwable)
            {
                logEvent.append("\r\n").append(indent).append("<error>");
                logEvent.append("\r\n").append(indent).append(indent).append("<class>").append(((Exception) event).getClass().getSimpleName()).append("</class>");

                logEvent.append("\r\n").append(indent).append(indent).append("<message>").append(message == null ? "" : message).append("[ ").append(cleanText(((Exception) event).getMessage())).append(" ]").append("</message>");
                logEvent.append("\r\n").append(indent).append(indent).append("<stacktrace>");
                for (StackTraceElement s : ((Throwable) event).getStackTrace())
                {
                    logEvent.append("\r\n").append(indent).append(indent).append(indent).append("at ").append(s.toString());
                }
                logEvent.append("\r\n").append(indent).append(indent).append("</stacktrace>");
                logEvent.append("\r\n").append(indent).append("</error>");
                logEvent.append("\r\n").append("</event>\r\n");
            }
            else if (String.valueOf(event).trim().startsWith("<") && String.valueOf(event).trim().endsWith(">"))
            {
                logEvent.append("\r\n").append(indentAllLines(String.valueOf(event))).append("\r\n");
                logEvent.append("</event>\r\n");
            }
            else
            {
                logEvent.append("\r\n").append(indent).append("<info>").append(String.valueOf(event)).append("</info>");
                logEvent.append("\r\n").append("</event>\r\n");
            }
            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    writeToLog(logEvent.toString(), event instanceof Throwable);
                }
            }).start();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    public static String indentAllLines(String text)
    {
        String line = "", buffer = "";
        try
        {
            InputStream is = new ByteArrayInputStream(text.getBytes());
            BufferedReader bis = new BufferedReader(new InputStreamReader(is));
            while (line != null)
            {
                buffer += indent + line + "\r\n";
                line = bis.readLine();
            }
        }
        catch (IOException ex)
        {
            return buffer;
        }

        return indent + buffer.trim();
    }

    private static String cleanText(String text)
    {
        String line, buffer = "";
        InputStream is = new ByteArrayInputStream(String.valueOf(text).getBytes());
        try
        {
            BufferedReader bis = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            while ((line = bis.readLine()) != null)
            {
                buffer += line;
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        return buffer;
    }

    private static void archiveOldLog(String lastDate, File logs, File logFile)
    {
        rotateExistingLogs(lastDate, logs);
        try
        {
            brFile.append(logFile, "</logger>");
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        File oldLog = new File(logs, "events-" + lastDate + "-0.log");
        logFile.renameTo(oldLog);
        try
        {
            brFile.compressFileToGzip(oldLog);
            brFile.deleteFile(oldLog);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        purgeOldLogs();
    }

    private static void rotateExistingLogs(String lastDate, File logs)
    {
        int count = 99;
        while (count >= 0)
        {
            try
            {
                File prev = new File(logs, "events-" + lastDate + "-" + count + ".log.gz");
                if (prev.exists())
                {
                    if (count >= 99)
                    {
                        brFile.deleteFile(prev);
                    }
                    else
                    {
                        prev.renameTo(new File(logs, "events-" + lastDate + "-" + (count + 1) + ".log.gz"));
                    }
                }
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
            count--;
        }
    }

    private static  File getNewLog(File logsDir, File logFile)
    {
        logFile = new File(logsDir, "events.log");
        try
        {
            logFile.createNewFile();
            brFile.append(logFile, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            brFile.append(logFile, "<logger class=\"" + LogFile.class.getName() + "\" datetime=\"" + new Date() + "\">");
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        return logFile;
    }

    private static synchronized void writeToLog(String logEvent, boolean error)
    {
        try
        {
            (error ? System.err : System.out).println(logEvent);
            brFile.append(getLog(), logEvent);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private static File getLog()
    {
        File logs = new File(logsPath);
        if (!logs.exists())
        {
            logs.mkdirs();
        }
        File logFile = new File(logsPath, "events.log");
        if (logFile.exists())
        {
            String lastDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date(logFile.lastModified()));
            String currentDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            if (!lastDate.equals(currentDate))
            {
                archiveOldLog(lastDate, logs, logFile);
                return getNewLog(logs, logFile);
            }
            return logFile;
        }
        return getNewLog(logs, logFile);
    }

    static void purgeOldLogs()
    {
        File logs = new File(logsPath);
        for (File log : logs.listFiles())
        {
            Calendar c1 = Calendar.getInstance();
            c1.setTime(new Date(log.lastModified()));
            if (Calendar.getInstance().get(Calendar.MONTH) - c1.get(Calendar.MONTH) >= 0 && Calendar.getInstance().get(Calendar.YEAR) - c1.get(Calendar.YEAR) >= MediumsControllerService.schemaConfig .getYearsToKeepLog())
            {
                try
                {
                    brFile.deleteFile(log);
                }
                catch (Exception ex)
                {
                    ex.printStackTrace();
                }
            }
        }
    }
}

