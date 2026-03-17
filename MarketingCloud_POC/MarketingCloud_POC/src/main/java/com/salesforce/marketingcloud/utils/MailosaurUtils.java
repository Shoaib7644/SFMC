package com.salesforce.marketingcloud.utils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import com.salesforce.marketingcloud.constant.Constant;
import com.microsoft.playwright.Page;

/**
 * Utility for fetching emails from Mailosaur and opening them in Playwright.
 *
 * Usage:
 *  - Mailosaur server name: sfmc
 *  - API key: KQpwhVZTrgPO7cJQf1cIuLiioJGObamk
 */
public class MailosaurUtils {

    // Mailosaur credentials (as provided)
    private static final String MAILOSAUR_SERVER = "brpuvaer";
    private static final String MAILOSAUR_APIKEY = "0hh2kcVTKoCf4ll4MQG2qRP5TcdpQ7Hl";

    private static final String MAILOSAUR_BASE = "https://mailosaur.com/api";
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    /**
     * Fetches the latest email for the configured server and returns its HTML body.
     * This method calls Mailosaur's messages endpoint, selects the most recent message,
     * then fetches the message by id and returns the HTML body if present.
     *
     * @return HTML body as a String, or null if not found.
     * @throws Exception when HTTP or parsing errors occur.
     */
    public static String getLatestEmailHtml() throws Exception {
        // 1) List messages for the server (limit=1 sorts by received behaviour)
        String listUrl = MAILOSAUR_BASE + "/messages?server=" + MAILOSAUR_SERVER + "&page=0&itemsPerPage=1";
        HttpRequest listRequest = HttpRequest.newBuilder()
                .uri(URI.create(listUrl))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", basicAuthHeader(MAILOSAUR_APIKEY))
                .GET()
                .build();

        HttpResponse<String> listResponse = httpClient.send(listRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (listResponse.statusCode() >= 400) {
            throw new RuntimeException("Failed to list Mailosaur messages: " + listResponse.statusCode() + " " + listResponse.body());
        }

        JSONObject listJson = new JSONObject(listResponse.body());
        JSONArray items = listJson.optJSONArray("items");
        if (items == null || items.length() == 0) {
            return null; // no messages
        }

        // Get the most recent message id
        JSONObject message = items.getJSONObject(0);
        String messageId = message.optString("id", null);
        if (messageId == null) {
            return null;
        }

        // 2) Fetch message by id
        String msgUrl = MAILOSAUR_BASE + "/messages/" + messageId;
        HttpRequest msgRequest = HttpRequest.newBuilder()
                .uri(URI.create(msgUrl))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", basicAuthHeader(MAILOSAUR_APIKEY))
                .GET()
                .build();

        HttpResponse<String> msgResponse = httpClient.send(msgRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (msgResponse.statusCode() >= 400) {
            throw new RuntimeException("Failed to get Mailosaur message: " + msgResponse.statusCode() + " " + msgResponse.body());
        }

        JSONObject msgJson = new JSONObject(msgResponse.body());
        // Mailosaur returns an array 'html' or content in 'html' object; check common fields
        // The API includes 'html' with 'data' and 'links' or 'attachments'. We'll try to get 'html' -> 'body' or 'html' -> 'data'.

        // First try: msgJson.html.body
        if (msgJson.has("html")) {
            JSONObject html = msgJson.optJSONObject("html");
            if (html != null) {
                String body = html.optString("body", null);
                if (body != null && !body.isEmpty()) {
                    return body;
                }
                // sometimes Mailosaur provides 'data' field (base64) — check
                String data = html.optString("data", null);
                if (data != null && !data.isEmpty()) {
                    byte[] decoded = java.util.Base64.getDecoder().decode(data);
                    return new String(decoded, StandardCharsets.UTF_8);
                }
            }
        }

        // Fallback: Mailosaur often includes 'content' array with items having 'type' and 'body'
        if (msgJson.has("content")) {
            JSONArray content = msgJson.optJSONArray("content");
            if (content != null) {
                for (int i = 0; i < content.length(); i++) {
                    JSONObject c = content.getJSONObject(i);
                    String type = c.optString("type", "");
                    if (type.contains("html")) {
                        String body = c.optString("body", null);
                        if (body != null && !body.isEmpty()) {
                            return body;
                        }
                    }
                }
            }
        }

        // Last resort: look for 'body' top-level
        String bodyTop = msgJson.optString("body", null);
        if (bodyTop != null && !bodyTop.isEmpty()) {
            return bodyTop;
        }

        return null;
    }

    /**
     * Polls Mailosaur for up to timeoutSeconds waiting for a message whose subject (or content) contains expectedSubject.
     * Returns the HTML body when found. Throws a RuntimeException with a clear message on timeout.
     */
    public static String getLatestEmailHtmlForSubject(String expectedSubject, int timeoutSeconds) throws Exception {
        long endTime = System.currentTimeMillis() + timeoutSeconds * 1000L;
        int pollIntervalMs = 5000; // poll every 5 seconds

        while (System.currentTimeMillis() < endTime) {
            // list recent messages (try to fetch a page of recent messages)
            String listUrl = MAILOSAUR_BASE + "/messages?server=" + MAILOSAUR_SERVER + "&page=0&itemsPerPage=50";
            HttpRequest listRequest = HttpRequest.newBuilder()
                    .uri(URI.create(listUrl))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", basicAuthHeader(MAILOSAUR_APIKEY))
                    .GET()
                    .build();

            HttpResponse<String> listResponse = httpClient.send(listRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (listResponse.statusCode() >= 400) {
                throw new RuntimeException("Failed to list Mailosaur messages: " + listResponse.statusCode() + " " + listResponse.body());
            }

            JSONObject listJson = new JSONObject(listResponse.body());
            JSONArray items = listJson.optJSONArray("items");
            if (items != null && items.length() > 0) {
                for (int i = 0; i < items.length(); i++) {
                    JSONObject msgSummary = items.getJSONObject(i);
                    String subject = msgSummary.optString("subject", "");
                    String id = msgSummary.optString("id", null);

                    // match subject contains expectedSubject (case-insensitive)
                    if (subject != null && subject.toLowerCase().contains(expectedSubject.toLowerCase())) {
                        return fetchMessageHtmlById(id);
                    }

                    // fallback: check snippet/preview or other fields that may contain text
                    String preview = msgSummary.optString("preview", "");
                    if (preview != null && preview.toLowerCase().contains(expectedSubject.toLowerCase())) {
                        return fetchMessageHtmlById(id);
                    }
                }
            }

            // not found yet - wait and retry
            Thread.sleep(pollIntervalMs);
        }

        throw new RuntimeException("Email with name '" + expectedSubject + "' not received within timeout of " + timeoutSeconds + " seconds");
    }

    /**
     * Opens provided HTML content in the shared Playwright Page by setting the page content.
     * This navigates to 'about:blank' then sets the content so it's visible in the browser.
     *
     * @param html HTML content to open
     * @throws Exception on Playwright errors
     */
    public static void openEmailInPlaywright(String html) throws Exception {
        if (html == null) {
            throw new IllegalArgumentException("html content is null");
        }
        Page page = Constant.PAGE;
        if (page == null) {
            throw new IllegalStateException("Playwright Page (Constant.PAGE) is not initialized");
        }
        // Navigate to a blank page and set content
        page.navigate("about:blank");
        // Use setContent to render html string
        page.setContent(html);
    }

    private static String fetchMessageHtmlById(String messageId) throws Exception {
        if (messageId == null) return null;
        String msgUrl = MAILOSAUR_BASE + "/messages/" + messageId;
        HttpRequest msgRequest = HttpRequest.newBuilder()
                .uri(URI.create(msgUrl))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", basicAuthHeader(MAILOSAUR_APIKEY))
                .GET()
                .build();

        HttpResponse<String> msgResponse = httpClient.send(msgRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (msgResponse.statusCode() >= 400) {
            throw new RuntimeException("Failed to get Mailosaur message: " + msgResponse.statusCode() + " " + msgResponse.body());
        }

        JSONObject msgJson = new JSONObject(msgResponse.body());

        // Try typical html locations in the message JSON
        if (msgJson.has("html")) {
            JSONObject html = msgJson.optJSONObject("html");
            if (html != null) {
                String body = html.optString("body", null);
                if (body != null && !body.isEmpty()) {
                    return body;
                }
                String data = html.optString("data", null);
                if (data != null && !data.isEmpty()) {
                    byte[] decoded = java.util.Base64.getDecoder().decode(data);
                    return new String(decoded, StandardCharsets.UTF_8);
                }
            }
        }

        if (msgJson.has("content")) {
            JSONArray content = msgJson.optJSONArray("content");
            if (content != null) {
                for (int i = 0; i < content.length(); i++) {
                    JSONObject c = content.getJSONObject(i);
                    String type = c.optString("type", "");
                    if (type.contains("html")) {
                        String body = c.optString("body", null);
                        if (body != null && !body.isEmpty()) {
                            return body;
                        }
                    }
                }
            }
        }

        String bodyTop = msgJson.optString("body", null);
        if (bodyTop != null && !bodyTop.isEmpty()) {
            return bodyTop;
        }

        return null;
    }

    private static String basicAuthHeader(String apiKey) {
        // Mailosaur uses 'Basic' auth with API key as username and blank password
        String token = apiKey + ":";
        String encoded = Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encoded;
    }
}