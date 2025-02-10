package com.starise.ipn.sms;

import com.starise.ipn.Util.AXWorker;
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
import java.util.List;
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
    private final String smsBalanceURL;
    private final String apiKey;
    private final String partnerID;
    private final String shortCode;
    private final String savingsAlert;
    private final String loanAlert;
    private final String reminderAlert;
    private final String balSmsRecipients;
    private final String smsBalanceAlert;
    private final String smsBalanceThreshold;

    public final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Autowired
    public ProcessAlert(Environment env) {
        this.env = env;
        this.httpClient = new OkHttpClient();
        this.decimalFormat = new DecimalFormat("#,##0.##");
        this.holderPattern = Pattern.compile("\\{.*?\\}");

        this.smsURL = env.getProperty("sms.url");
        this.smsBalanceURL = env.getProperty("sms.balanceUrl");
        this.apiKey = env.getProperty("sms.apiKey");
        this.partnerID = env.getProperty("sms.partnerId");
        this.shortCode = env.getProperty("sms.wpcode");
        this.savingsAlert = env.getProperty("sms.savingsAlert");
        this.loanAlert = env.getProperty("sms.loanAlert");
        this.reminderAlert = env.getProperty("sms.reminderAlert");
        this.smsBalanceAlert = env.getProperty("sms.smsBalanceAlert");
        this.balSmsRecipients = env.getProperty("sms.balSmsRecipients");
        this.smsBalanceThreshold = env.getProperty("sms.smsBalanceThreshold");
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

    public void sendBalanceAlert() {
        double balance = checkSMSBalance();
        ArrayList<String> numbers = extractNumbers(balSmsRecipients);
        if (balance <= Double.parseDouble(smsBalanceThreshold)) {
            for (String recipient : numbers) {
                AlertRequest alertRequest = new AlertRequest();
                alertRequest.setMessageType("BL");
                alertRequest.setMobileNo(recipient);
                alertRequest.setAmount(BigDecimal.valueOf(balance));
                alertRequest.setClientName("Admin");
                alertRequest.setThreshold(BigDecimal.valueOf(Double.parseDouble(smsBalanceThreshold)));
                alertRequest.setAmount(BigDecimal.valueOf(balance));

                String messageTemplate = processTemplate(alertRequest.getMessageType());
                String message = replaceMasks(messageTemplate, alertRequest);
                alertRequest.setMessage(message);

                if (sendAlert(alertRequest)) {
                    logger.error("Balance Alert sent");
                } else {
                    logger.error("Balance Alert Failed");
                }
            }
        }
    }

    public boolean sendAlert(String mobileNo, String message){

        JSONObject json = new JSONObject();
        json.put("apikey", apiKey);
        json.put("partnerID", partnerID);
        json.put("mobile", AXWorker.formatPhoneNumber(mobileNo));
        json.put("message", message);
        json.put("shortcode", shortCode);
        json.put("pass_type", "plain");

        try {
            sendRequest(smsURL, json.toString());
            return true;
        } catch (IOException e) {
            logger.error("Error Balance sending alert: {}", e.toString());
            return false;
        }
    }

    public double checkSMSBalance() {
        String requestBody = createBalanceRequestJson();
        try {
            String response = sendRequest(smsBalanceURL, requestBody);
            return processBalanceResponse(response);
        } catch (IOException e) {
            logger.error("Error Balance sending alert: {}", e.toString());
            return -1;
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

    public ArrayList<String> extractNumbers(String recipients) {

        String regex = "\\d+";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(recipients);
        ArrayList<String> numbers = new ArrayList<>();
        while (matcher.find()) {
            numbers.add(matcher.group());
        }

        if (numbers.size() > 1) {
            System.out.println("The string contains multiple numbers.");
            System.out.println("Extracted Numbers: " + numbers);
        } else if (numbers.size() == 1) {
            System.out.println("The string contains only one number: " + numbers.get(0));
        } else {
            System.out.println("No numbers found in the string.");
        }
        return numbers;
    }

    private String createRequestJson(AlertRequest alertRequest) {
        String messageTemplate = processTemplate(alertRequest.getMessageType());
        String message = replaceMasks(messageTemplate, alertRequest);

        logger.info("===============================================");
        logger.info("SMS Sent to: {}", alertRequest.getMobileNo());
        logger.info("SMS Sent: {}", message);
        logger.info("===============================================");

        JSONObject json = new JSONObject();
        json.put("apikey", apiKey);
        json.put("partnerID", partnerID);
        json.put("mobile", alertRequest.getMobileNo());
        json.put("message", message);
        json.put("shortcode", shortCode);
        json.put("pass_type", "plain");

        return json.toString();
    }

    private String createBalanceRequestJson() {

        JSONObject json = new JSONObject();
        json.put("apikey", apiKey);
        json.put("partnerID", partnerID);

        return json.toString();
    }

    private void processResponse(String jsonResponse) {
        JSONObject jsonObject = new JSONObject(jsonResponse);
        JSONArray responses = jsonObject.getJSONArray("responses");
        try {
            for (int i = 0; i < responses.length(); i++) {
                JSONObject response = responses.getJSONObject(i);
                int responseCode = response.getInt("response-code");

                String responseDescription = response.getString("response-description");
                long mobile = response.getLong("mobile");
                if (responseCode == 200) {
                    String messageId = response.getString("messageid");
                    int networkId = response.getInt("networkid");

                    logger.info("Response Code: {}", responseCode);
                    logger.info("Response Description: {}", responseDescription);
                    logger.info("Mobile: {}", mobile);
                    logger.info("Message ID: {}", messageId);
                    logger.info("Network ID: {}", networkId);
                } else {
                    logger.info("=================ALERT NOT SUCCESSFUL = {}  =========================", responseCode);
                    logger.info("E Response Code: {}", responseCode);
                    logger.info("E Response Description: {}", responseDescription);
                    logger.info("================= =========================");
                }
            }
        } catch (Exception ex) {
            logger.error("Error Encountered during processing of alerts", ex);
        }
    }

    private Double processBalanceResponse(String jsonResponse) {
        Double balance = 0.0;
        JSONObject jsonObject = new JSONObject(jsonResponse);
        JSONArray responses = jsonObject.getJSONArray("responses");
        try {
            for (int i = 0; i < responses.length(); i++) {
                JSONObject response = responses.getJSONObject(i);
                int responseCode = response.getInt("response-code");

                String responseDescription = response.getString("response-description");
                String credit = response.getString("credit");
                if (responseCode == 200) {
                    String messageId = response.getString("messageid");
                    int networkId = response.getInt("networkid");

                    logger.info("BL Response Code: {}", responseCode);
                    logger.info("BL Response Description: {}", responseDescription);
                    logger.info("credit: {}", credit);
                    balance = Double.valueOf(credit);


                } else {
                    logger.info("=================BAL QUERY NOT SUCCESSFUL = {}  =========================", responseCode);
                    logger.info("E B Response Code: {}", responseCode);
                    logger.info("E B Response Description: {}", responseDescription);
                    logger.info("================= END =========================");
                }
            }
        } catch (Exception ex) {
            logger.error("Error Encountered during processing of alerts", ex);
        }
        return balance;
    }


    private String processTemplate(String smsTypeCode) {
        switch (smsTypeCode.toUpperCase()) {
            case "SV":
                return savingsAlert;
            case "LD":
                return reminderAlert;
            case "BL":
                return smsBalanceAlert;
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
                        replacement = alertRequest.getTxnDate();
                        break;
                    case "{OFFICER}":
                        replacement = firstName(alertRequest.getOfficer());
                        break;
                    case "{BALANCE}":
                        replacement = (alertRequest.getBalance().compareTo(BigDecimal.ZERO) <= 0) ? "0" : alertRequest.getBalance().toPlainString();
                        break;
                    case "{LOANAMOUNT}":
                        replacement = (alertRequest.getLoanAmount().compareTo(BigDecimal.ZERO) <= 0) ? "0" : alertRequest.getLoanAmount().toPlainString();
                        break;
                    case "{REPAYMENTAMOUNT}":
                        replacement = (alertRequest.getRepaymentAmount().compareTo(BigDecimal.ZERO) <= 0) ? "0" : alertRequest.getRepaymentAmount().toPlainString();
                        break;
                    case "{TERMFREQUENCY}":
                        replacement = stringfy(alertRequest.getTermFrequency());
                        break;
                    case "{TERMCODE}":
                        replacement = firstName(alertRequest.getTermCode());
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
        return name != null && !name.trim().isEmpty() ? capitalize(name.trim().split("\\s")[0]) : name;
    }
    public String stringfy(Object name) {
        return String.valueOf(name);
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

    public String cleanSpaces(String text) {
        return !isBlank(text) ? text.replaceAll("\\s+", " ").trim() : text;
    }

}
