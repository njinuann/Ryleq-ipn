package com.starise.ipn.sms;

import com.starise.ipn.model.AlertRequest;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ProcessAlert {
    private static final Logger logger = LoggerFactory.getLogger(ProcessAlert.class);

    private final Environment env;
    private final OkHttpClient httpClient;
    private final DecimalFormat decimalFormat;
    private final Pattern holderPattern;

    private final String smsURL;
    private final String apiKey;
    private final String partnerID;
    private final String shortCode;
    private final String savingsAlert;
    private final String loanAlert;
    private final String reminderAlert;

    public final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Autowired
    public ProcessAlert(Environment env) {
        this.env = env;
        this.httpClient = new OkHttpClient();
        this.decimalFormat = new DecimalFormat("#,##0.##");
        this.holderPattern = Pattern.compile("\\{.*?\\}");

        this.smsURL = env.getProperty("sms.url");
        this.apiKey = env.getProperty("sms.apiKey");
        this.partnerID = env.getProperty("sms.partnerId");
        this.shortCode = env.getProperty("sms.wpcode");
        this.savingsAlert = env.getProperty("sms.savingsAlert");
        this.loanAlert = env.getProperty("sms.loanAlert");
        this.reminderAlert = env.getProperty("sms.reminderAlert");
    }

    public boolean sendAlert(AlertRequest alertRequest) {
        String requestBody = createRequestJson(alertRequest);
        try {
            String response = sendRequest(smsURL, requestBody);
            processResponse(response);
            return true;
        } catch (IOException e) {
            logger.error("Error sending alert: {}", e.toString());
            return false;
        }
    }

    private String sendRequest(String url, String jsonBody) throws IOException {
        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            return response.body().string();
        }
    }

    private String createRequestJson(AlertRequest alertRequest) {
        String messageTemplate = processTemplate(alertRequest.getMessageType());
        String message = replaceMasks(messageTemplate, alertRequest);

        JSONObject json = new JSONObject();
        json.put("apikey", apiKey);
        json.put("partnerID", partnerID);
        json.put("mobile", alertRequest.getMobileNo());
        json.put("message", message);
        json.put("shortcode", shortCode);
        json.put("pass_type", "plain");

        return json.toString();
    }

    private void processResponse(String jsonResponse) {
        JSONObject jsonObject = new JSONObject(jsonResponse);
        JSONArray responses = jsonObject.getJSONArray("responses");

        for (int i = 0; i < responses.length(); i++) {
            JSONObject response = responses.getJSONObject(i);
            int responseCode = response.getInt("response-code");
            String responseDescription = response.getString("response-description");
            long mobile = response.getLong("mobile");
            String messageId = response.getString("messageid");
            int networkId = response.getInt("networkid");

            logger.info("Response Code: {}", responseCode);
            logger.info("Response Description: {}", responseDescription);
            logger.info("Mobile: {}", mobile);
            logger.info("Message ID: {}", messageId);
            logger.info("Network ID: {}", networkId);
        }
    }

    private String processTemplate(String smsTypeCode) {
        switch (smsTypeCode.toUpperCase()) {
            case "SV":
                return savingsAlert;
            case "LD":
                return reminderAlert;
            default:
                return loanAlert;
        }
    }

    public String replaceMasks(String text, AlertRequest alertRequest) {
        if (!isBlank(text)) {
            ArrayList<String> holdersList = extractPlaceHolders(text);
            for (String placeHolder : holdersList) {
                String replacement = "<>";
                switch (placeHolder.toUpperCase()) {
                    case "{CLIENTNAME}":
                        replacement = firstName(alertRequest.getClientName());
                        break;
                    case "{CLIENTID}":
                        replacement = alertRequest.getClientId();
                        break;
                    case "{MOBILE}":
                        replacement = alertRequest.getMobileNo();
                        break;
                    case "{CURRENCYCODE}":
                        replacement = "KES"; // Hardcoded as KES, adjust if dynamic
                        break;
                    case "{AMOUNT}":
                        replacement = alertRequest.getAmount().toPlainString();
                        break;
                    case "{RECEIPT}":
                        replacement = alertRequest.getReceipt();
                        break;
                    case "{DETAIL}":
                        replacement = alertRequest.getDetail();
                        break;
                    case "{SENDER}":
                        replacement = alertRequest.getSenderName();
                        break;
                    case "{TXNDATE}":
                        replacement = alertRequest.getTxnDate().toString();
                        break;
                    case "{BALANCE}":
                        replacement = (alertRequest.getBalance().compareTo(BigDecimal.ZERO) < 0) ? "<>" : alertRequest.getBalance().toPlainString();
                        break;
                }
                text = replaceAll(text, placeHolder, replacement);
            }
            return text.replaceAll("~<>", "").replaceAll(" ~ <>", "").trim();
        }
        return text;
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

    public String firstName(String name) {
        return name != null && name.trim().length() > 0 ? capitalize(name.trim().split("\\s")[0]) : name;
    }

    public String capitalize(String text) {
        return capitalize(text, true);
    }

    public String capitalize(String text, boolean convertAllXters) {
        if (text != null && text.length() > 0) {
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
        if (text != null && text.length() > 0) {
            StringBuilder builder = new StringBuilder();
            for (String word : text.split("\\s")) {
                builder.append(word.substring(0, 1).toLowerCase()).append(word.substring(1)).append(" ");
            }
            return builder.toString().trim();
        }
        return text;
    }

    public String cleanSpaces(String text) {
        return !isBlank(text) ? text.replaceAll("\\s+", " ").trim() : text;
    }

}
