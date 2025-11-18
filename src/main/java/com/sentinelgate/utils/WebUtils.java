package com.sentinelgate.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;

@Component
public class WebUtils {

    @Autowired
    ObjectMapper objectMapper;

    Logger log = LoggerFactory.getLogger(WebUtils.class);

    public HashMap<String, String > getHeaders(){
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");
        return headers;
    }

    public String get(String url, HashMap<String, String> headers, Integer timeout){
        try{
            if(StringUtils.isEmpty(url)){
                log.error("Empty GET url");
                return null;
            }
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url)).GET();

            if(ObjectUtils.isEmpty(headers) || headers.isEmpty())
                headers = getHeaders();

            headers.forEach(builder::header);

            HttpRequest request = builder.build();
            String response = this.execute(request, timeout);
            log.info("Got GET response to endpoint: {}, response: {}", url, response);
            return response;
        } catch (Exception e){
            log.error("Error while GET request to endpoint: {}, error:", url, e);
        }
        return null;
    }

    public String post(String url, Object jsonObject, HashMap<String, String> headers, Integer timeout) {
        try{
            if(StringUtils.isEmpty(url)){
                log.error("Empty POST url");
                return null;
            }
            log.info("Sending POST request to endpoint: {}, request: {}", url, jsonObject);
            String jsonString = objectMapper.writeValueAsString(jsonObject);
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url)).POST(HttpRequest.BodyPublishers.ofString(jsonString));

            if(ObjectUtils.isEmpty(headers) || headers.isEmpty())
                headers = getHeaders();

            headers.forEach(builder::header);

            HttpRequest request = builder.build();
            String response = this.execute(request, timeout);
            log.info("Got POST response to endpoint: {}, response: {}", url, response);
            return response;
        } catch (Exception e){
            log.error("Error while POST request to endpoint: {}, request: {}, error: " , url, jsonObject, e);
        }
        return null;
    }

    public String put(String url, Object jsonObject, HashMap<String, String> headers, Integer timeout) {
        try{
            if(StringUtils.isEmpty(url)){
                log.error("Empty PUT url");
                return null;
            }
            log.info("Sending PUT request to endpoint: {}, request: {}", url, jsonObject);
            String jsonString = objectMapper.writeValueAsString(jsonObject);

            HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(url))
                            .PUT(HttpRequest.BodyPublishers.ofString(jsonString));

            if(ObjectUtils.isEmpty(headers) || headers.isEmpty())
                headers = getHeaders();

            headers.forEach(builder::header);
            HttpRequest request = builder.build();
            String response = this.execute(request, timeout);
            log.info("Got PUT response to endpoint: {}, response: {}", url, response);
            return response;
        } catch (Exception e){
            log.error("Error while PUT request to endpoint: {}, request: {}, error: " , url, jsonObject, e);
        }
        return null;
    }

    public String delete(String url, HashMap<String, String> headers, Integer timeout) {
        try{
            if(StringUtils.isEmpty(url)){
                log.error("Empty DELETE url");
                return null;
            }
            log.info("Sending DELETE request to endpoint: {}", url);

            HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(url))
                            .DELETE();

            if(ObjectUtils.isEmpty(headers) || headers.isEmpty())
                headers = getHeaders();

            headers.forEach(builder::header);
            HttpRequest request = builder.build();
            return this.execute(request, timeout);
        } catch (Exception e){
            log.error("Error while DELETE request to endpoint: {}, error: " , url, e);
        }
        return null;
    }

    private String execute(HttpRequest request, Integer timeout) {
        try {
            if(ObjectUtils.isEmpty(timeout))
                timeout = 3000;
            HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(timeout)).build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("Status: {}\nBody: {}", response.statusCode(), response.body());
            return response.body();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return "Request failed: " + e.getMessage();
        }
    }

}
