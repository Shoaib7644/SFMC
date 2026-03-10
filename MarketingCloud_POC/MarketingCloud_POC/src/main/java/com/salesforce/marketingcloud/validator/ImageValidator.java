package com.salesforce.marketingcloud.validator;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.salesforce.marketingcloud.context.ValidationContext;
import com.salesforce.marketingcloud.model.ValidationResult;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ImageValidator implements EmailValidator {
    private static final Logger logger = LoggerFactory.getLogger(ImageValidator.class);
    private static final int TIMEOUT_MS = 10000;
    private final ValidationContext context;

    public ImageValidator(ValidationContext context) {
        this.context = context;
    }

    @Override
    public ValidationResult validate(String emailHtml) {
        logger.info("Running ImageValidator");
        Document doc = Jsoup.parse(emailHtml == null ? "" : emailHtml);
        Elements imgs = doc.select("img");
        List<String> missingAlt = new ArrayList<>();
        List<String> broken = new ArrayList<>();

        for (Element img : imgs) {
            String src = img.attr("src");
            String alt = img.attr("alt");
            if (alt == null || alt.trim().isEmpty()) {
                missingAlt.add(src == null ? "unknown-src" : src);
            }
            if (src != null && !src.trim().isEmpty()) {
                try {
                    int code = checkImage(src);
                    if (code >= 400) {
                        broken.add(src + " (" + code + ")");
                        context.addBrokenImage(src + " (" + code + ")");
                    }
                } catch (IOException e) {
                    logger.warn("Error loading image {}: {}", src, e.getMessage());
                    broken.add(src + " (error:" + e.getMessage() + ")");
                    context.addBrokenImage(src + " (error:" + e.getMessage() + ")");
                }
            }
        }

        // Image loading status
        boolean loadPass = broken.isEmpty();
        String loadNotes = loadPass ? "All images loaded successfully" : String.join("; ", broken);
        context.addResult("Image Loading Status", loadPass, loadNotes, loadPass ? "INFO" : "CRITICAL");

        // Image alt text
        boolean altPass = missingAlt.isEmpty();
        String altNotes = altPass ? "All images have alt text" : String.join("; ", missingAlt);
        context.addResult("Image Alt Text Compliance", altPass, altNotes, altPass ? "INFO" : "MAJOR");

        return new ValidationResult("Images", loadPass && altPass, loadNotes + "; " + altNotes, (loadPass && altPass) ? "INFO" : "MAJOR");
    }

    private int checkImage(String src) throws IOException {
        URL url = new URL(src);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestMethod("HEAD");
            int code = conn.getResponseCode();
            if (code >= 400 && conn.getRequestMethod().equals("HEAD")) {
                conn.disconnect();
                HttpURLConnection conn2 = (HttpURLConnection) url.openConnection();
                try {
                    conn2.setConnectTimeout(TIMEOUT_MS);
                    conn2.setReadTimeout(TIMEOUT_MS);
                    conn2.setInstanceFollowRedirects(true);
                    conn2.setRequestMethod("GET");
                    return conn2.getResponseCode();
                } finally {
                    conn2.disconnect();
                }
            }
            return code;
        } finally {
            conn.disconnect();
        }
    }
}
