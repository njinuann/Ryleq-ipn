package com.starise.ipn.Util;

//import com.starise.ipn.model.ErrorData;

import okhttp3.OkHttpClient;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.math.BigDecimal;
import java.security.cert.X509Certificate;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AXWorker {
    private static final Logger logger = LoggerFactory.getLogger(AXWorker.class);
    private final DecimalFormat decimalFormat;
    private final Pattern holderPattern;

    public String externalUrl = "https://evolve.wonderful.co.ke/fineract-provider/api/v1/";
    public String tenantId = "default";
    //    public String username = "Admin";
//    public String password = "Ryl3q@#1";
    public String username = "mifos";
    public String password = "mifos123";
    public String credentials = username + ":" + password;
    public int keySequence = 0;
    public String OTPKey;

    @Autowired
    public AXWorker() {

        this.decimalFormat = new DecimalFormat("#,##0.##");
        this.holderPattern = Pattern.compile("\\{.*?\\}");
        this.OTPKey = "01234567890";
    }

    public String basicAuthToken() {
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
    }

    public String generateUniqueKey(String prefix) {
        return prefix + generateUniqueKey();
    }

    public String generateUniqueKey() {
        keySequence = ((keySequence + 1) % 999) + 1;
        return new SimpleDateFormat("yyMMddHHmmss").format(new Date()) + String.format("%03d", keySequence);
    }

    public String generateOTP(int length) {
        int len = length;
        Random rndm_method = new Random();

        char[] otp = new char[len];
        for (int i = 0; i < len; i++) {
            otp[i] = OTPKey.charAt(rndm_method.nextInt(OTPKey.length()));
        }
        return String.valueOf(otp);
    }


    public String formatMpesaDate(String timestamp) {
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        LocalDateTime dateTime = LocalDateTime.parse(timestamp, inputFormatter);
        return dateTime.format(outputFormatter);
    }

    public String formatMobileNumber(String mobileNumber) {
        if (mobileNumber.startsWith("+254")) {
            return mobileNumber.substring(1); // Remove the "+" from the beginning
        } else if (mobileNumber.startsWith("0")) {
            return "254" + mobileNumber.substring(1); // Replace the leading "0" with "254"
        } else {
            return mobileNumber; // Return the number as is if no special case applies
        }
    }

    public ArrayList<String> extractPlaceHolders(String text) {
        ArrayList<String> holdersList = new ArrayList<>();
        Matcher matcher = holderPattern.matcher(text);
        while (matcher.find()) {
            holdersList.add(matcher.group(0));
        }
        return holdersList;
    }

    public String convertMifosDate(String inputDate) {
        try {
            DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            LocalDateTime dateTime = LocalDateTime.parse(inputDate, inputFormatter);
            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
            return dateTime.format(outputFormatter);
        } catch (DateTimeParseException e) {
            System.err.println("Invalid date format. Expected format: yyyyMMddHHmmss");
            return new SimpleDateFormat("dd MMM yyyy").format(new Date());
        }
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

    public boolean isBlank(Object object) {
        return object == null || "{}".equals(String.valueOf(object).trim()) || "[]".equals(String.valueOf(object).trim()) || "".equals(String.valueOf(object).trim()) || "null".equals(String.valueOf(object).trim()) || String.valueOf(object).trim().toLowerCase().contains("---select");
    }

    public String firstName(String name) {
        return name != null && !name.trim().isEmpty() ? capitalize(name.trim().split("\\s")[0]) : name;
    }

    public String capitalize(String text) {
        return capitalize(text, true);
    }

    public String capitalize(String text, boolean convertAllXters) {
        if (text != null && !text.isEmpty()) {
            char p = '0';
            StringBuilder builder = new StringBuilder();
            for (char c : (convertAllXters ? text.toLowerCase() : text).toCharArray()) {
                builder.append(p = (Character.isLetter(p) ? c : Character.toUpperCase(c)));
            }
            return cleanSpaces(builder.toString());
        }
        return text;
    }

    public String decapitalize(String text) {
        if (text != null && !text.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            for (String word : text.split("\\s")) {
                builder.append(word.substring(0, 1).toLowerCase()).append(word.substring(1)).append(" ");
            }
            return builder.toString().trim();
        }
        return text;
    }

    public String capitalizeFirst(String text) {
        if (text != null) {
            try {
                StringBuilder builder = new StringBuilder();
                for (String word : text.toLowerCase().replace(",", " ").split("\\s+")) {
                    if (!word.isEmpty()) {
                        builder.append(word.substring(0, 1).toUpperCase()).append(word.length() > 1 ? word.substring(1).toLowerCase() : "").append(" ");
                    }
                }
                return builder.toString().trim();
            } catch (Exception ex) {
                return text;
            }
        }
        return text;
    }

    public String formatJson(String json) {
        try {
            if (!isBlank(json)) {
                return new JSONObject(json).toString(4).trim().replaceAll("    ", "\t");
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage());
        }
        return checkBlank(json, "");
    }

    public static String generateRandomString(int numberOfCharacters) {
        String AlphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "0123456789";
        StringBuilder sb = new StringBuilder(numberOfCharacters);
        for (int i = 0; i < numberOfCharacters; i++) {
            int index = (int) (AlphaNumericString.length() * Math.random());
            sb.append(AlphaNumericString.charAt(index));
        }
        return sb.toString();
    }

    public static String generateRandomNumber(int numberOfCharacters) {
        String AlphaNumericString = "0123456789";
        StringBuilder sb = new StringBuilder(numberOfCharacters);
        for (int i = 0; i < numberOfCharacters; i++) {
            int index = (int) (AlphaNumericString.length() * Math.random());
            sb.append(AlphaNumericString.charAt(index));
        }
        return sb.toString();
    }

    public static String padLeftZeros(String inputString, int length) {
        if (inputString.length() >= length) {
            return inputString;
        }
        StringBuilder sb = new StringBuilder();
        while (sb.length() < length - inputString.length()) {
            sb.append('0');
        }
        sb.append(inputString);

        return sb.toString();
    }

    private static boolean isNullEmpty(String str) {
        if (str == null) {
            return true;
        } else return str.isEmpty();
    }

    public static String formatPhoneNumber(String phoneNo) {
        if (isNullEmpty(phoneNo))
            throw new RuntimeException();
        phoneNo = phoneNo.trim();
        phoneNo = phoneNo.replace("+", "");
        return phoneNo;
    }

//    public static String formatPhoneNumber(String phoneNo) throws IpnException {
//        if (isNullEmpty(phoneNo))
//            throw new IpnException(ErrorData.builder()
//                    .code("-99")
//                    .message("Invalid mobile phone number, format should be 254XXXXXXXXX").build());
//        phoneNo = phoneNo.trim();
//        phoneNo = phoneNo.replace("+", "");
//        return phoneNo;
//    }

    public static BigDecimal getBigDecimal(BigDecimal value) {
        if (value == null)
            return BigDecimal.ZERO;
        else
            return value;
    }

    public String cleanSpaces(String text) {
        return !isBlank(text) ? text.replaceAll("\\s+", " ").trim() : text;
    }

    private OkHttpClient getUnsafeOkHttpClient() {
        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[]{};
                        }
                    }
            };

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
            builder.hostnameVerifier((hostname, session) -> true);

            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
