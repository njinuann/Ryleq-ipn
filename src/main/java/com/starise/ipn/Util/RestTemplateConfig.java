//package com.starise.ipn.Util;
//
//
//
//import com.starise.ipn.model.ErrorData;
//import okhttp3.*;
//import org.json.JSONObject;
//import org.springframework.context.annotation.Configuration;
//
//import javax.net.ssl.*;
//import java.io.BufferedReader;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.net.URL;
//import java.security.SecureRandom;
//import java.security.cert.X509Certificate;
//import java.util.Map;
//import java.util.concurrent.TimeUnit;
//
//@Configuration
//public class RestTemplateConfig {
//    public String getRestWithUnSecureHttps(String urlString) {
//        String response = null;
//        HttpsURLConnection urlConnection = null;
//        try {
//            HostnameVerifier hostnameVerifier = new HostnameVerifier() {
//                @Override
//                public boolean verify(String hostname, SSLSession session) {
//                    return true;
//                }
//            };
//
//            URL url = new URL(urlString);
//            InputStream inStream = null;
//            try {
//
//                // Create a trust manager that does not validate certificate chains
//                TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
//                    public X509Certificate[] getAcceptedIssuers() {
//                        return null;
//                    }
//
//                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
//                    }
//
//                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
//                    }
//                }};
//
//                // Install the all-trusting trust manager
//                try {
//                    SSLContext sc = SSLContext.getInstance("TLS");
//                    sc.init(null, trustAllCerts, new SecureRandom());
//                    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
//                } catch (Exception e) {
//
//                }
//
//                urlConnection = (HttpsURLConnection) url.openConnection();
//                urlConnection.setRequestProperty("Content-Type", "application/json");
//                urlConnection.setConnectTimeout(5000); //set timeout to 5 seconds
//                urlConnection.setHostnameVerifier(hostnameVerifier);
//                inStream = urlConnection.getInputStream();
//                BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));
//                String output;
//                while ((output = reader.readLine()) != null) {
//                    response = output;
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            } finally {
//                if (inStream != null) {
//                    inStream.close();
//                }
//            }
//        } catch (Exception e) {
//            if (urlConnection != null) {
//                urlConnection.disconnect();
//            }
//        }
//        return response;
//    }
//
//    private OkHttpClient getUnsafeOkHttpClient() {
//        try {
//            // Create a trust manager that does not validate certificate chains
//            final TrustManager[] trustAllCerts = new TrustManager[]{
//                    new X509TrustManager() {
//                        @Override
//                        public void checkClientTrusted(X509Certificate[] chain,
//                                                       String authType) {
//                        }
//
//                        @Override
//                        public void checkServerTrusted(X509Certificate[] chain,
//                                                       String authType) {
//                        }
//
//                        @Override
//                        public X509Certificate[] getAcceptedIssuers() {
//                            return new X509Certificate[0];
//                        }
//                    }
//            };
//
//            // Install the all-trusting trust manager
//            final SSLContext sslContext = SSLContext.getInstance("SSL");
//            sslContext.init(null, trustAllCerts, new SecureRandom());
//            // Create an ssl socket factory with our all-trusting manager
//            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
//
//            return new OkHttpClient.Builder()
//                    .sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0])
//                    .hostnameVerifier(new HostnameVerifier() {
//                        @Override
//                        public boolean verify(String hostname, SSLSession session) {
//                            return true;
//                        }
//                    }).build();
//
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    public JSONObject post(Map<String, Object> requestBody, String url, String webserviceGatewayOwner) throws Exception {
//        // creates new client
//        MediaType MediaTypeJSON = MediaType.parse("application/json");
//        String response = null;
//        String jsonRequest = "";
//        JSONObject jsonObject = new JSONObject(requestBody);
//        jsonRequest = jsonObject.toString();
//        OkHttpClient httpclient;
//        if (url.contains("https"))
//            httpclient = getUnsafeOkHttpClient().newBuilder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(40, TimeUnit.SECONDS).build();
//        else
//            httpclient = new OkHttpClient().newBuilder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(40, TimeUnit.SECONDS).build();
//        // build a request
//        Request request1 = new Request.Builder().url(url)
//                .post(RequestBody.create(MediaTypeJSON, jsonRequest)).build();
//        try {
//            Response responseJson = httpclient.newCall(request1).execute();
//            if (responseJson.isSuccessful()) {
//                // Get back the response and convert it to a Book object
//                response = responseJson.body().string();
//            } else if (responseJson.code() == 404) {
//                throw new IpnException(ErrorData.builder()
//                        .code("404")
//                        .message("Service is unavailable,Please try again later @" + webserviceGatewayOwner).build());
//            }
//        } catch (Exception e) {
//            if (e.getMessage().toLowerCase().contains("connect timed out"))
//                throw new IpnException(ErrorData.builder()
//                        .code("408")
//                        .message("Request took long to return a response @" + webserviceGatewayOwner).build());
//            else
//                throw new IpnException(ErrorData.builder()
//                        .code("500")
//                        .message("Undefined error has occurred while querying " + webserviceGatewayOwner).build());
//        }
//        return new JSONObject(response);
//    }
//}
