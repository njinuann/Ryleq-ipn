package com.starise.ipn.template;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

public class LoggingInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        // Log request method, URI, and headers
        System.out.println("Request Method: " + request.getMethod());
        System.out.println("Request URI: " + request.getURI());
        System.out.println("Request Headers: " + request.getHeaders());

        // Log authentication (assuming Basic Authentication)
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader != null) {
            System.out.println("Authorization Header: " + authHeader);
        }

        // Proceed with the request execution and get the response
        ClientHttpResponse response = execution.execute(request, body);

        // Log response status code and headers
        System.out.println("Response Status Code: " + response.getStatusCode());
        System.out.println("Response Headers: " + response.getHeaders());

        return response;
    }
}
