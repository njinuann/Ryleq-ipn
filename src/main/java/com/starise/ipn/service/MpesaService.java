package com.starise.ipn.service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class MpesaService {

    private static final Logger logger = LoggerFactory.getLogger(MpesaService.class);
    @Autowired
    private Environment env;

    @Autowired
    private ConfigurableEnvironment configurableEnvironment;

    @PostConstruct
    public void registerUrl() {
        if ("Yes".equalsIgnoreCase(env.getProperty("mpesa.update.url"))) {
            String paybill = env.getProperty("mpesa.paybill");
            String consumerKey = env.getProperty("mpesa.consumer.key");
            String consumerSecret = env.getProperty("mpesa.consumer.secret");

            try {
                String tokenUrl = env.getProperty("mpesa.token.url");
                String registerUrl = env.getProperty("mpesa.register.url");
                String timeoutAction = env.getProperty("mpesa.timeout.action");
                String confirmationUrl = env.getProperty("mpesa.confirmation.url");
                String validationUrl = env.getProperty("mpesa.validation.url");

                String credentials = Base64.getEncoder().encodeToString((consumerKey + ":" + consumerSecret).getBytes(StandardCharsets.ISO_8859_1));

                OkHttpClient client = new OkHttpClient();

                // Fetch token
                Request tokenRequest = new Request.Builder()
                        .url(tokenUrl != null ? tokenUrl : "https://api.safaricom.co.ke/mpesa/c2b/v2/registerurl")
                        .addHeader("Authorization", "Basic " + credentials)
                        .build();

                try (Response tokenResponse = client.newCall(tokenRequest).execute()) {
                    if (!tokenResponse.isSuccessful()) throw new IOException("Unexpected code " + tokenResponse);
                    String tokenBody = tokenResponse.body().string();

                    JSONObject tokenJson = new JSONObject(tokenBody);
                    String accessToken = tokenJson.getString("access_token");

                    // Register URL
                    JSONObject requestJson = new JSONObject();
                    requestJson.put("ShortCode", paybill);
                    requestJson.put("ResponseType", timeoutAction);
                    requestJson.put("ConfirmationURL", confirmationUrl);
                    requestJson.put("ValidationURL", validationUrl);

                    logger.info("register url requestJson::=> {}", requestJson.toString(4));

                    RequestBody requestBody = RequestBody.create(
                            requestJson.toString(),
                            MediaType.parse("application/json")
                    );

                    Request registerRequest = new Request.Builder()
                            .url(registerUrl)
                            .addHeader("Authorization", "Bearer " + accessToken)
                            .post(requestBody)
                            .build();

                    try (Response registerResponse = client.newCall(registerRequest).execute()) {
                        if (!registerResponse.isSuccessful())
                            throw new IOException("Unexpected code " + registerResponse);
                        String responseBody = registerResponse.body().string();

                        ObjectMapper objectMapper = new ObjectMapper();
                        JsonNode responseNode = objectMapper.readTree(responseBody);

                        logger.info("register url response::=> {}", responseNode.toString());

                        if (responseNode.get("ResponseDescription").asText().toLowerCase().contains("success")) {
                            configurableEnvironment.getSystemProperties().put("mpesa.update.url", "No");
                        }
                    }
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                // Log your exception here
            }
        }
    }
}

