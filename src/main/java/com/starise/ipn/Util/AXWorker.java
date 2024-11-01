package com.starise.ipn.Util;

import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AXWorker
{
    private final DecimalFormat decimalFormat;
    private final Pattern holderPattern;

    @Autowired
    public AXWorker() {

        this.decimalFormat = new DecimalFormat("#,##0.##");
        this.holderPattern = Pattern.compile("\\{.*?\\}");
    }



    public ArrayList<String> extractPlaceHolders(String text) {
        ArrayList<String> holdersList = new ArrayList<>();
        Matcher matcher = holderPattern.matcher(text);
        while (matcher.find()) {
            holdersList.add(matcher.group(0));
        }
        return holdersList;
    }

    public String formatAmount(BigDecimal amount) {
        return !isBlank(amount) ? decimalFormat.format(amount) : null;
    }

    public String replaceAll(String text, String placeHolder, String replacement) {
        return !isBlank(text) ? text.replaceAll(placeHolder.replace("{", "\\{").replace("}", "\\}"), cleanField(checkBlank(replacement, "<>"), false)) : text;
    }

    public String cleanField(String text, boolean space) {
        if (!isBlank(text)) {
            String prev = "";
            StringBuilder buffer = new StringBuilder();
            for (String t : (space ? spaceWords(text) : text).split("\\s+")) {
                if (!prev.equalsIgnoreCase(t)) {
                    buffer.append(" ").append(t);
                    prev = t;
                }
            }
            return buffer.toString().trim();
        }
        return text;
    }

    public String spaceWords(String text) {
        if (!isBlank(text)) {
            char p = ' ';
            StringBuilder builder = new StringBuilder();
            for (char c : text.toCharArray()) {
                builder.append((Character.isUpperCase(c) && !Character.isUpperCase(p)) || (Character.isDigit(c) && !Character.isDigit(p)) || (!Character.isDigit(c) && Character.isDigit(p)) ? (" " + c) : (builder.length() == 0 ? String.valueOf(c).toUpperCase() : c));
                p = c;
            }
            return builder.toString().trim();
        }
        return text;
    }

    public <T> T checkBlank(T value, T nillValue) {
        return isBlank(value) ? nillValue : value;
    }

    public <T> T checkBlank(Object checkField, T value, T nillValue) {
        return isBlank(checkField) ? nillValue : value;
    }

    public static boolean isBlank(Object object) {
        return object == null || "{}".equals(String.valueOf(object).trim()) || "[]".equals(String.valueOf(object).trim()) || "".equals(String.valueOf(object).trim()) || "null".equals(String.valueOf(object).trim()) || String.valueOf(object).trim().toLowerCase().contains("---select");
    }

    public String firstName(String name)
    {
        return name != null && name.trim().length() > 0 ? capitalize(name.trim().split("\\s")[0]) : name;
    }

    public String capitalize(String text)
    {
        return capitalize(text, true);
    }
    public String capitalize(String text, boolean convertAllXters)
    {
        if (text != null && text.length() > 0)
        {
            char p = '0';
            StringBuilder builder = new StringBuilder();
            for (char c : (convertAllXters ? text.toLowerCase() : text).toCharArray())
            {
                builder.append(p = (Character.isLetter(p) ? c : Character.toUpperCase(c)));
            }
            return cleanSpaces(builder.toString());
        }
        return text;
    }

    public String decapitalize(String text)
    {
        if (text != null && text.length() > 0)
        {
            StringBuilder builder = new StringBuilder();
            for (String word : text.split("\\s"))
            {
                builder.append(word.substring(0, 1).toLowerCase()).append(word.substring(1)).append(" ");
            }
            return builder.toString().trim();
        }
        return text;
    }
    public String cleanSpaces(String text)
    {
        return !isBlank(text) ? text.replaceAll("\\s+", " ").trim() : text;
    }
}
