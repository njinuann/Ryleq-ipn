package com.starise.ipn.template;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.*;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Collections;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate(getClientHttpRequestFactory());

        // Adding Interceptors for basic authentication and headers
        restTemplate.setInterceptors(Collections.singletonList((request, body, execution) -> {
            // Basic Authentication Header
            String plainCreds = "mifos:mifos123";
            String base64Creds = Base64.getEncoder().encodeToString(plainCreds.getBytes());
            request.getHeaders().add("Authorization", "Basic " + base64Creds);

            // Custom Header
            request.getHeaders().add("Fineract-Platform-TenantId", "default");

            // Log headers before executing the request
            System.out.println("Request Headers: " + request.getHeaders());

            return execution.execute(request, body);
        }));

        return restTemplate;
    }

    private ClientHttpRequestFactory getClientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }}, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return factory;
    }
}
