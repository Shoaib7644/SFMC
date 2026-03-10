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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates HTTP status of all links found in email HTML.
 *
 * FIX: Added SFMC_TRACKING_DOMAINS skip-list.
 *
 * SFMC click-tracking URLs (cl.s12.exct.net, links.sfmc.co, etc.) are redirect
 * wrappers that only resolve in the context of a real delivered email — they carry
 * per-recipient encrypted tokens and refuse connections from servers/CI environments
 * with "Connection refused: getsockopt". They are NOT broken links; skipping them
 * eliminates false-positive FAIL results on HTTP Link Status.
 *
 * To add more domains to the skip-list, append to SFMC_TRACKING_DOMAINS below.
 */
public class BrokenLinkValidator implements EmailValidator {

    private static final Logger logger = LoggerFactory.getLogger(BrokenLinkValidator.class);

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS    = 10_000;

    /**
     * Domains that should be skipped during link validation.
     * These are SFMC / ESP tracking redirect domains that only resolve
     * in the context of a real delivered email.
     *
     * Add any additional tracking domains your ESP uses here.
     */
    private static final Set<String> SKIP_DOMAINS = new HashSet<>(Arrays.asList(
            // SFMC click-tracking redirect domains
            "cl.s12.exct.net",
            "cl.exct.net",
            "links.sfmc.co",
            "links.exacttarget.com",
            "click.sfmc.co",
            "click.exacttarget.com",
            // Common ESP tracking domains — add yours as needed
            "track.sendgrid.net",
            "click.sendgrid.net",
            "mailchimp.com/track",
            "list-manage.com"
    ));

    private final ValidationContext context;

    public BrokenLinkValidator(ValidationContext context) {
        this.context = context;
    }

    @Override
    public ValidationResult validate(String emailHtml) {
        logger.info("Running BrokenLinkValidator");
        Document doc = Jsoup.parse(emailHtml == null ? "" : emailHtml);
        Elements anchors = doc.select("a[href]");
        List<String> broken  = new ArrayList<>();
        List<String> skipped = new ArrayList<>();

        for (Element a : anchors) {
            String href = a.attr("href").trim();
            if (href.isEmpty()) continue;

            String lower = href.toLowerCase();

            // Skip non-HTTP schemes
            if (lower.startsWith("mailto:") || lower.startsWith("tel:")
                    || lower.startsWith("javascript:") || lower.startsWith("#")) {
                logger.debug("Skipping non-HTTP link: {}", href);
                continue;
            }

            // FIX: skip known SFMC tracking / ESP redirect domains
            if (isTrackingDomain(href)) {
                logger.debug("Skipping SFMC/ESP tracking domain link: {}", abbreviate(href, 80));
                skipped.add(abbreviate(href, 80));
                continue;
            }

            try {
                int code = checkUrlStatus(href);
                if (code >= 400) {
                    String entry = abbreviate(href, 100) + " (" + code + ")";
                    broken.add(entry);
                    context.addBrokenLink(entry);
                } else {
                    logger.debug("Link OK [{}]: {}", code, abbreviate(href, 80));
                }
            } catch (IOException e) {
                String entry = abbreviate(href, 100) + " (error:" + condensedError(e) + ")";
                broken.add(entry);
                context.addBrokenLink(entry);
                logger.warn("Error checking link {}: {}", abbreviate(href, 80), e.getMessage());
            }
        }

        if (!skipped.isEmpty()) {
            logger.info("Skipped {} SFMC/ESP tracking link(s) — not real broken links", skipped.size());
        }

        boolean passed = broken.isEmpty();
        String notes = passed
                ? "All HTTP links returned OK status"
                        + (skipped.isEmpty() ? "" : " (" + skipped.size() + " tracking link(s) skipped)")
                : "Broken links detected: " + String.join("; ", broken);

        ValidationResult result = new ValidationResult(
                "HTTP Link Status", passed, notes, passed ? "INFO" : "CRITICAL");
        context.addResult(result.getItem(), result.isPassed(), result.getNotes(), result.getSeverity());
        return result;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────────────────

    /**
     * Returns true if the URL's host matches any domain in SKIP_DOMAINS.
     * Uses suffix-matching so subdomains are also caught
     * (e.g. "foo.cl.s12.exct.net" is matched by "cl.s12.exct.net").
     */
    private static boolean isTrackingDomain(String href) {
        try {
            String host = new URL(href).getHost().toLowerCase();
            for (String domain : SKIP_DOMAINS) {
                if (host.equals(domain) || host.endsWith("." + domain)) {
                    return true;
                }
            }
        } catch (Exception e) {
            // malformed URL — don't skip, let the main validator handle it
        }
        return false;
    }

    private int checkUrlStatus(String href) throws IOException {
        URL url = new URL(href);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestMethod("HEAD");
            conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (compatible; EmailAuditBot/1.0)");
            int code = conn.getResponseCode();
            // Some servers reject HEAD — retry with GET
            if (code >= 400) {
                conn.disconnect();
                HttpURLConnection conn2 = (HttpURLConnection) url.openConnection();
                try {
                    conn2.setConnectTimeout(CONNECT_TIMEOUT_MS);
                    conn2.setReadTimeout(READ_TIMEOUT_MS);
                    conn2.setInstanceFollowRedirects(true);
                    conn2.setRequestMethod("GET");
                    conn2.setRequestProperty("User-Agent",
                            "Mozilla/5.0 (compatible; EmailAuditBot/1.0)");
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

    /** Truncates long URLs for notes/logging — avoids multi-line log spam */
    private static String abbreviate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    /** Returns just the root exception message, stripping package prefixes */
    private static String condensedError(Exception e) {
        String msg = e.getMessage();
        if (msg == null) return e.getClass().getSimpleName();
        // "java.net.ConnectException: Connection refused: getsockopt" → "Connection refused"
        int colon = msg.lastIndexOf(':');
        return colon >= 0 ? msg.substring(colon + 1).trim() : msg;
    }
}