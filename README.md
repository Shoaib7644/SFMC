# Building an Enterprise-Grade Email Testing Framework: Salesforce Marketing Cloud + Java Playwright + Mailosaur + ADA Compliance + Allure Reports

> *A comprehensive, end-to-end guide to automating email validation, link checking, ADA/WCAG accessibility auditing, and consolidated Word report generation — all in one Java-based framework.*

---

## Table of Contents

1. [Introduction & Problem Statement](#introduction)
2. [Architecture Overview](#architecture)
3. [Tech Stack & Prerequisites](#tech-stack)
4. [Project Structure](#project-structure)
5. [Step 1 — Triggering Emails via SFMC REST API](#step-1)
6. [Step 2 — Receiving & Fetching Emails via Mailosaur](#step-2)
7. [Step 3 — Opening Emails as HTML in Playwright](#step-3)
8. [Step 4 — Link & URL Validation (Response Codes)](#step-4)
9. [Step 5 — ADA Compliance with Axe-Core JS Injection](#step-5)
10. [Step 6 — Broken Image Detection](#step-6)
11. [Step 7 — Screenshot Capture](#step-7)
12. [Step 8 — Consolidated Word Document Reports](#step-8)
13. [Step 9 — Allure Reporting Integration](#step-9)
14. [Step 10 — Cucumber BDD Glue](#step-10)
15. [Improvements & Best Practices](#improvements)
16. [CI/CD Integration](#cicd)
17. [Conclusion](#conclusion)

---

## 1. Introduction & Problem Statement {#introduction}

Email marketing is the lifeblood of modern digital commerce. Salesforce Marketing Cloud (SFMC) sends billions of emails a year — but most QA pipelines treat email testing as an afterthought. Manual testing means:

- **Broken links go undetected** until customers complain
- **ADA/WCAG violations** expose the business to legal liability
- **Broken images** silently degrade campaign performance
- **No consolidated audit trail** for compliance teams

This article walks you through building a **fully automated, production-ready Java testing framework** that:

1. **Triggers emails** directly from SFMC via its REST API
2. **Receives emails** in an isolated inbox using Mailosaur
3. **Opens the raw HTML** of each email in a real browser via Playwright
4. **Validates every link** — capturing HTTP response codes
5. **Audits accessibility** using axe-core injected as JavaScript
6. **Detects broken images** using network interception
7. **Captures screenshots** of the rendered email
8. **Generates a Word document** consolidating all findings with screenshots attached
9. **Produces Allure reports** for stakeholder-friendly dashboards

---

## 2. Architecture Overview {#architecture}

```
┌─────────────────────┐
│   Cucumber Feature  │
│   Files (.feature)  │
└────────┬────────────┘
         │ triggers
         ▼
┌─────────────────────┐     REST API      ┌──────────────────────┐
│   SFMC Trigger      │ ─────────────────▶│  Salesforce Marketing │
│   Service           │                   │  Cloud (Journey /     │
│                     │                   │  Transactional Send)  │
└─────────────────────┘                   └──────────┬───────────┘
                                                     │ delivers email
                                                     ▼
┌─────────────────────┐     REST API      ┌──────────────────────┐
│   Mailosaur         │ ◀─────────────────│  Mailosaur Inbox     │
│   Client Service    │                   │  (isolated per test) │
└────────┬────────────┘                   └──────────────────────┘
         │ returns raw HTML
         ▼
┌─────────────────────────────────────────────────────────────────┐
│                  Playwright Browser Session                      │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────────────────┐ │
│  │  Link Check  │  │  Axe-Core JS │  │  Broken Image         │ │
│  │  (HTTP calls)│  │  Injection   │  │  Detection            │ │
│  └──────────────┘  └──────────────┘  └───────────────────────┘ │
│  ┌──────────────┐                                               │
│  │  Screenshot  │                                               │
│  │  Capture     │                                               │
│  └──────────────┘                                               │
└────────────────────────────┬────────────────────────────────────┘
                             │ results
                             ▼
              ┌──────────────────────────────┐
              │   Report Generation Layer    │
              │  ┌─────────────┐  ┌────────┐ │
              │  │  Word .docx │  │ Allure │ │
              │  │  Report     │  │ Report │ │
              │  └─────────────┘  └────────┘ │
              └──────────────────────────────┘
```

---

## 3. Tech Stack & Prerequisites {#tech-stack}

| Component | Technology | Version |
|---|---|---|
| Language | Java | 17+ |
| BDD Framework | Cucumber | 7.x |
| Browser Automation | Playwright for Java | 1.44+ |
| Email Inbox | Mailosaur | mailosaur-java 8.x |
| Accessibility Audit | axe-core (injected JS) | 4.9.x |
| HTTP Client | Java HttpClient / OkHttp | Built-in |
| Report - Word | Apache POI / docx4j | 5.x |
| Report - Allure | Allure Cucumber | 2.x |
| Build Tool | Maven | 3.8+ |

### Maven `pom.xml` — Key Dependencies

```xml
<dependencies>
    <!-- Playwright -->
    <dependency>
        <groupId>com.microsoft.playwright</groupId>
        <artifactId>playwright</artifactId>
        <version>1.44.0</version>
    </dependency>

    <!-- Cucumber -->
    <dependency>
        <groupId>io.cucumber</groupId>
        <artifactId>cucumber-java</artifactId>
        <version>7.15.0</version>
    </dependency>
    <dependency>
        <groupId>io.cucumber</groupId>
        <artifactId>cucumber-junit-platform-engine</artifactId>
        <version>7.15.0</version>
    </dependency>

    <!-- Mailosaur -->
    <dependency>
        <groupId>com.mailosaur</groupId>
        <artifactId>mailosaur-java</artifactId>
        <version>8.1.0</version>
    </dependency>

    <!-- Allure Cucumber -->
    <dependency>
        <groupId>io.qameta.allure</groupId>
        <artifactId>allure-cucumber7-jvm</artifactId>
        <version>2.27.0</version>
    </dependency>

    <!-- Apache POI (Word Documents) -->
    <dependency>
        <groupId>org.apache.poi</groupId>
        <artifactId>poi-ooxml</artifactId>
        <version>5.2.5</version>
    </dependency>

    <!-- OkHttp (Link Checking) -->
    <dependency>
        <groupId>com.squareup.okhttp3</groupId>
        <artifactId>okhttp</artifactId>
        <version>4.12.0</version>
    </dependency>

    <!-- Jackson (JSON parsing for SFMC API) -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
        <version>2.17.0</version>
    </dependency>
</dependencies>
```

---

## 4. Project Structure {#project-structure}

```
src/
├── main/java/com/email/automation/
│   ├── config/
│   │   └── ConfigManager.java           # Reads environment config
│   ├── sfmc/
│   │   ├── SFMCAuthService.java          # OAuth 2.0 token management
│   │   └── SFMCEmailTriggerService.java  # Journey/Transactional send
│   ├── mailosaur/
│   │   └── MailosaurService.java         # Fetch emails from inbox
│   ├── playwright/
│   │   ├── BrowserManager.java           # Playwright lifecycle
│   │   ├── LinkValidator.java            # HTTP link checking
│   │   ├── AxeCoreInjector.java          # ADA/WCAG audit
│   │   └── BrokenImageDetector.java      # Image validation
│   └── reporting/
│       ├── WordReportGenerator.java      # .docx consolidated report
│       └── AllureReportHelper.java       # Allure attachment helpers
├── test/java/com/email/automation/
│   ├── steps/
│   │   └── EmailValidationSteps.java     # Cucumber step definitions
│   └── runners/
│       └── CucumberTestRunner.java
├── test/resources/
│   ├── features/
│   │   └── email_validation.feature
│   └── axe.min.js                        # bundled axe-core
└── config/
    └── application.properties
```

---

## 5. Step 1 — Triggering Emails via SFMC REST API {#step-1}

SFMC offers two primary mechanisms for programmatic email sends: **Journey Builder Entry Events** (for automated journeys) and **Transactional Send** (for on-demand sends). We use the REST API with OAuth 2.0.

### 5.1 SFMC OAuth Token Service

```java
// SFMCAuthService.java
package com.email.automation.sfmc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

public class SFMCAuthService {

    private static final String TOKEN_ENDPOINT = 
        "https://{YOUR_SUBDOMAIN}.auth.marketingcloudapis.com/v2/token";

    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private String cachedToken;
    private long tokenExpiry;

    public String getAccessToken() throws Exception {
        // Return cached token if still valid (with 60s buffer)
        if (cachedToken != null && System.currentTimeMillis() < tokenExpiry - 60_000) {
            return cachedToken;
        }
        
        String clientId     = System.getenv("SFMC_CLIENT_ID");
        String clientSecret = System.getenv("SFMC_CLIENT_SECRET");
        String accountId    = System.getenv("SFMC_ACCOUNT_ID");

        RequestBody body = new FormBody.Builder()
            .add("grant_type",    "client_credentials")
            .add("client_id",     clientId)
            .add("client_secret", clientSecret)
            .add("account_id",    accountId)
            .build();

        Request request = new Request.Builder()
            .url(TOKEN_ENDPOINT)
            .post(body)
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("SFMC Auth failed: " + response.code());
            }
            JsonNode json = mapper.readTree(response.body().string());
            cachedToken  = json.get("access_token").asText();
            int expiresIn = json.get("expires_in").asInt(); // seconds
            tokenExpiry  = System.currentTimeMillis() + (expiresIn * 1000L);
            return cachedToken;
        }
    }
}
```

### 5.2 Triggering a Transactional Email Send

```java
// SFMCEmailTriggerService.java
package com.email.automation.sfmc;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.util.Map;

public class SFMCEmailTriggerService {

    private static final String SEND_ENDPOINT =
        "https://{YOUR_SUBDOMAIN}.rest.marketingcloudapis.com/messaging/v1/email/messages/{messageKey}";

    private final OkHttpClient httpClient;
    private final SFMCAuthService authService;
    private final ObjectMapper mapper = new ObjectMapper();

    public SFMCEmailTriggerService(SFMCAuthService authService) {
        this.authService = authService;
        this.httpClient  = new OkHttpClient();
    }

    /**
     * Triggers a Transactional Send to a given recipient email address.
     *
     * @param toEmail     - The Mailosaur inbox address (e.g., abc123@srv.mailosaur.net)
     * @param messageKey  - SFMC Transactional Send Definition key
     * @param attributes  - Dynamic substitution attributes for personalization
     */
    public void triggerEmail(String toEmail, String messageKey, Map<String, String> attributes) 
            throws Exception {

        String token = authService.getAccessToken();

        // Build the payload
        Map<String, Object> payload = Map.of(
            "definitionKey", messageKey,
            "recipient", Map.of(
                "address",    toEmail,
                "attributes", attributes
            )
        );

        String json = mapper.writeValueAsString(payload);

        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
        Request request = new Request.Builder()
            .url(SEND_ENDPOINT)
            .addHeader("Authorization", "Bearer " + token)
            .post(body)
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.code() != 202) {
                throw new RuntimeException("SFMC email trigger failed [" + 
                    response.code() + "]: " + response.body().string());
            }
            System.out.println("✅ SFMC email triggered to: " + toEmail);
        }
    }
}
```

> **Pro Tip:** For Journey-based sends, use the `POST /interaction/v1/events` endpoint instead and pass the `ContactKey` and `EventDefinitionKey`. The authentication pattern remains identical.

---

## 6. Step 2 — Receiving & Fetching Emails via Mailosaur {#step-2}

Mailosaur gives each test run an isolated inbox (`@{serverId}.mailosaur.net`). You poll for the email and retrieve its full HTML body.

```java
// MailosaurService.java
package com.email.automation.mailosaur;

import com.mailosaur.MailosaurClient;
import com.mailosaur.models.*;

import java.time.Duration;
import java.util.Date;

public class MailosaurService {

    private final MailosaurClient client;
    private final String serverId;

    public MailosaurService() {
        this.client   = new MailosaurClient(System.getenv("MAILOSAUR_API_KEY"));
        this.serverId = System.getenv("MAILOSAUR_SERVER_ID");
    }

    /**
     * Generates a unique Mailosaur inbox address for this test run.
     * Pattern: {testId}@{serverId}.mailosaur.net
     */
    public String generateInboxAddress(String testId) {
        return testId + "@" + serverId + ".mailosaur.net";
    }

    /**
     * Waits for and fetches the first email matching the subject.
     * Times out after 60 seconds.
     *
     * @param sentToAddress - The Mailosaur inbox address
     * @param subject       - Partial subject match
     * @return              - Full email body including raw HTML
     */
    public Message waitForEmail(String sentToAddress, String subject) throws Exception {
        MessageSearchCriteria criteria = new MessageSearchCriteria();
        criteria.withSentTo(sentToAddress);
        criteria.withSubject(subject);

        SearchOptions options = new SearchOptions();
        options.withTimeout(60_000);     // Wait up to 60 seconds
        options.withErrorOnTimeout(true);

        MessageListResult results = client.messages().search(serverId, criteria, options);
        if (results.items().isEmpty()) {
            throw new RuntimeException("No email received matching subject: " + subject);
        }

        // Fetch the full message (search only returns summaries)
        String messageId = results.items().get(0).id();
        return client.messages().getById(messageId);
    }

    /**
     * Extracts the raw HTML body from the email message.
     */
    public String getHtmlBody(Message message) {
        if (message.html() == null || message.html().body() == null) {
            throw new RuntimeException("Email has no HTML body");
        }
        return message.html().body();
    }

    /**
     * Clean up — delete all messages in the server to keep inbox tidy.
     */
    public void purgeInbox() throws Exception {
        client.messages().deleteAll(serverId);
    }
}
```

---

## 7. Step 3 — Opening Email HTML in Playwright {#step-3}

Rather than validating the HTML statically, we load it in a **real browser** for fully rendered, JavaScript-aware validation.

```java
// BrowserManager.java
package com.email.automation.playwright;

import com.microsoft.playwright.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BrowserManager {

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page page;

    public void initialize() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(
            new BrowserType.LaunchOptions()
                .setHeadless(true)
                .setArgs(java.util.List.of(
                    "--no-sandbox",
                    "--disable-setuid-sandbox",
                    "--disable-web-security"      // Allow cross-origin resource loading in email
                ))
        );
        context = browser.newContext(
            new Browser.NewContextOptions()
                .setViewportSize(1440, 900)
                .setIgnoreHTTPSErrors(true)
        );
        page = context.newPage();
    }

    /**
     * Loads raw HTML email content into Playwright.
     * Writes the HTML to a temp file and navigates to it
     * to ensure all relative assets load correctly.
     */
    public Page loadEmailHtml(String htmlContent) throws Exception {
        // Write HTML to a temporary file
        Path tempHtml = Files.createTempFile("email-", ".html");
        Files.writeString(tempHtml, htmlContent);

        // Navigate to the file:// URL
        page.navigate("file://" + tempHtml.toAbsolutePath());

        // Wait for the page to fully settle
        page.waitForLoadState(LoadState.NETWORKIDLE);

        return page;
    }

    public Page getPage() { return page; }

    public void close() {
        if (context != null)    context.close();
        if (browser != null)    browser.close();
        if (playwright != null) playwright.close();
    }
}
```

---

## 8. Step 4 — Link & URL Validation {#step-4}

We extract all `<a href>` links from the rendered page and fire real HTTP HEAD requests to capture response codes.

```java
// LinkValidator.java
package com.email.automation.playwright;

import com.microsoft.playwright.Page;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.*;
import java.util.concurrent.*;

public class LinkValidator {

    public record LinkResult(String url, int statusCode, String statusMessage, boolean isBroken) {}

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build();

    /**
     * Extracts all hyperlinks from the rendered Playwright page and validates each one.
     */
    public List<LinkResult> validateAllLinks(Page page) {
        // Extract all href attributes using Playwright's evaluate
        List<String> rawLinks = (List<String>) page.evaluate("""
            () => {
                const anchors = document.querySelectorAll('a[href]');
                return Array.from(anchors)
                    .map(a => a.href)
                    .filter(href => href.startsWith('http'));
            }
        """);

        // Remove duplicates
        Set<String> uniqueLinks = new LinkedHashSet<>(rawLinks);

        // Validate in parallel with a thread pool
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<Future<LinkResult>> futures = new ArrayList<>();

        for (String url : uniqueLinks) {
            futures.add(executor.submit(() -> checkLink(url)));
        }

        List<LinkResult> results = new ArrayList<>();
        for (Future<LinkResult> future : futures) {
            try {
                results.add(future.get(30, TimeUnit.SECONDS));
            } catch (Exception e) {
                // If we can't even reach the URL, mark as broken
                results.add(new LinkResult("Unknown", 0, "Connection error: " + e.getMessage(), true));
            }
        }

        executor.shutdown();
        return results;
    }

    private LinkResult checkLink(String url) {
        try {
            Request request = new Request.Builder()
                .url(url)
                .method("HEAD", null)
                .addHeader("User-Agent", "Mozilla/5.0 (EmailQA-Bot/1.0)")
                .build();

            try (Response response = httpClient.newCall(request).execute()) {
                boolean isBroken = response.code() >= 400;
                return new LinkResult(url, response.code(), response.message(), isBroken);
            }
        } catch (Exception e) {
            return new LinkResult(url, 0, "Error: " + e.getMessage(), true);
        }
    }
}
```

> **Why HEAD instead of GET?** HEAD requests are faster and less bandwidth-intensive. However, some servers reject HEAD requests — in that case, fall back to GET with a small timeout.

---

## 9. Step 5 — ADA Compliance with Axe-Core JS Injection {#step-5}

Axe-core is the industry-standard open-source accessibility testing engine. We bundle `axe.min.js` locally and inject it into the Playwright page.

### 9.1 Download axe-core

Add this to your Maven build or download manually:

```xml
<!-- Add axe-core npm bundle to src/test/resources/axe.min.js -->
<!-- Or use the CDN URL: https://cdnjs.cloudflare.com/ajax/libs/axe-core/4.9.1/axe.min.js -->
```

### 9.2 AxeCoreInjector.java

```java
// AxeCoreInjector.java
package com.email.automation.playwright;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Page;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class AxeCoreInjector {

    public record AxeViolation(
        String id,
        String impact,
        String description,
        String helpUrl,
        List<String> affectedNodes
    ) {}

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Injects axe-core into the page and runs WCAG 2.1 AA audit.
     */
    public List<AxeViolation> runAccessibilityAudit(Page page) throws Exception {
        // Read bundled axe-core from classpath
        String axeScript = Files.readString(
            Paths.get(getClass().getClassLoader()
                .getResource("axe.min.js").toURI())
        );

        // Inject axe-core into the page
        page.evaluate(axeScript);

        // Run axe against WCAG 2.1 Level AA
        String axeResultJson = (String) page.evaluate("""
            async () => {
                const results = await axe.run(document, {
                    runOnly: {
                        type: 'tag',
                        values: ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'best-practice']
                    }
                });
                return JSON.stringify(results);
            }
        """);

        return parseViolations(axeResultJson);
    }

    private List<AxeViolation> parseViolations(String json) throws Exception {
        JsonNode root       = mapper.readTree(json);
        JsonNode violations = root.get("violations");
        List<AxeViolation> results = new ArrayList<>();

        if (violations == null) return results;

        for (JsonNode v : violations) {
            List<String> nodes = new ArrayList<>();
            for (JsonNode node : v.get("nodes")) {
                // Capture the CSS selector of each failing element
                nodes.add(node.get("target").toString());
            }

            results.add(new AxeViolation(
                v.get("id").asText(),
                v.get("impact").asText(),      // critical, serious, moderate, minor
                v.get("description").asText(),
                v.get("helpUrl").asText(),
                nodes
            ));
        }

        return results;
    }
}
```

**Example violations you will commonly catch in SFMC emails:**

| Violation ID | Impact | Common Cause |
|---|---|---|
| `color-contrast` | serious | Low contrast text on colored backgrounds |
| `image-alt` | critical | `<img>` tags with no `alt` attribute |
| `link-name` | serious | `<a>` tags with empty or generic text |
| `html-lang-valid` | serious | Missing `lang` attribute on `<html>` |
| `meta-viewport` | critical | `user-scalable=no` preventing zoom |

---

## 10. Step 6 — Broken Image Detection {#step-6}

We intercept network requests in Playwright to flag images returning non-200 responses.

```java
// BrokenImageDetector.java
package com.email.automation.playwright;

import com.microsoft.playwright.*;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class BrokenImageDetector {

    public record ImageResult(String url, int statusCode, boolean isBroken) {}

    /**
     * Attaches a response listener to detect broken images.
     * Call this BEFORE navigating to the page.
     */
    public List<ImageResult> detectBrokenImages(Page page, String htmlContent) throws Exception {
        List<ImageResult> results = new CopyOnWriteArrayList<>();

        // Listen to all responses
        page.onResponse(response -> {
            String url = response.url();
            // Capture only image resources
            if (isImageUrl(url)) {
                boolean isBroken = response.status() >= 400 || response.status() == 0;
                results.add(new ImageResult(url, response.status(), isBroken));
            }
        });

        // Also check images already in the DOM that may have loaded with errors
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Check naturalWidth = 0 which indicates a failed image load
        List<Map<String, Object>> domImageCheck = (List<Map<String, Object>>) page.evaluate("""
            () => {
                return Array.from(document.images).map(img => ({
                    src:      img.src,
                    broken:   img.naturalWidth === 0 && img.complete,
                    hasAlt:   img.hasAttribute('alt'),
                    altText:  img.alt
                }));
            }
        """);

        for (Map<String, Object> img : domImageCheck) {
            boolean broken = (boolean) img.get("broken");
            if (broken) {
                results.add(new ImageResult((String) img.get("src"), 0, true));
            }
        }

        return results;
    }

    private boolean isImageUrl(String url) {
        String lower = url.toLowerCase();
        return lower.contains(".jpg")  || lower.contains(".jpeg") ||
               lower.contains(".png")  || lower.contains(".gif")  ||
               lower.contains(".webp") || lower.contains(".svg")  ||
               lower.contains("image");
    }
}
```

---

## 11. Step 7 — Screenshot Capture {#step-7}

```java
// Inside BrowserManager or a dedicated ScreenshotService

/**
 * Captures a full-page screenshot of the email.
 * Returns the file path for attachment in the Word report.
 */
public Path captureFullPageScreenshot(Page page, String emailSubject) throws Exception {
    // Sanitize filename
    String safeName = emailSubject.replaceAll("[^a-zA-Z0-9]", "_");
    Path screenshotPath = Paths.get("target/screenshots", safeName + "_" + 
        System.currentTimeMillis() + ".png");

    Files.createDirectories(screenshotPath.getParent());

    page.screenshot(new Page.ScreenshotOptions()
        .setPath(screenshotPath)
        .setFullPage(true)                 // Captures entire scrollable content
        .setType(ScreenshotType.PNG)
    );

    System.out.println("📸 Screenshot saved: " + screenshotPath);
    return screenshotPath;
}
```

---

## 12. Step 8 — Consolidated Word Document Reports {#step-8}

All findings — links, accessibility violations, broken images, and screenshots — are consolidated into a professional Word `.docx` report using Apache POI.

```java
// WordReportGenerator.java
package com.email.automation.reporting;

import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class WordReportGenerator {

    private final XWPFDocument document;

    public WordReportGenerator() {
        this.document = new XWPFDocument();
    }

    public void addTitle(String emailSubject) {
        XWPFParagraph title = document.createParagraph();
        title.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = title.createRun();
        run.setText("Email Validation Report");
        run.setBold(true);
        run.setFontSize(20);
        run.addBreak();

        XWPFParagraph subtitle = document.createParagraph();
        subtitle.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun subRun = subtitle.createRun();
        subRun.setText("Subject: " + emailSubject);
        subRun.setFontSize(14);
        subRun.addBreak();
        subRun.setText("Generated: " + LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        subRun.setFontSize(12);
    }

    public void addSectionHeader(String header) {
        XWPFParagraph p = document.createParagraph();
        p.setStyle("Heading1");
        XWPFRun run = p.createRun();
        run.setText(header);
        run.setBold(true);
        run.setFontSize(14);
        run.setColor("1F4E79");
    }

    public void addLinkValidationResults(List<LinkValidator.LinkResult> results) {
        addSectionHeader("🔗 Link Validation Results");

        // Summary row
        long broken = results.stream().filter(r -> r.isBroken()).count();
        addParagraph(String.format("Total: %d links checked | ✅ OK: %d | ❌ Broken: %d",
            results.size(), results.size() - broken, broken));

        // Table
        XWPFTable table = document.createTable();
        addTableHeader(table, new String[]{"URL", "Status Code", "Status", "Result"});

        for (LinkValidator.LinkResult result : results) {
            XWPFTableRow row = table.createRow();
            row.getCell(0).setText(truncate(result.url(), 70));
            row.getCell(1).setText(String.valueOf(result.statusCode()));
            row.getCell(2).setText(result.statusMessage());
            row.getCell(3).setText(result.isBroken() ? "❌ BROKEN" : "✅ OK");
        }
    }

    public void addAxeResults(List<AxeCoreInjector.AxeViolation> violations) {
        addSectionHeader("♿ ADA / WCAG 2.1 AA Compliance Results");

        if (violations.isEmpty()) {
            addParagraph("✅ No accessibility violations found.");
            return;
        }

        addParagraph("❌ " + violations.size() + " violation(s) found:");

        XWPFTable table = document.createTable();
        addTableHeader(table, new String[]{"Rule ID", "Impact", "Description", "Affected Elements"});

        for (AxeCoreInjector.AxeViolation v : violations) {
            XWPFTableRow row = table.createRow();
            row.getCell(0).setText(v.id());
            row.getCell(1).setText(v.impact().toUpperCase());
            row.getCell(2).setText(v.description());
            row.getCell(3).setText(String.join("\n", v.affectedNodes()));
        }
    }

    public void addBrokenImageResults(List<BrokenImageDetector.ImageResult> images) {
        addSectionHeader("🖼️ Image Validation Results");

        long broken = images.stream().filter(i -> i.isBroken()).count();
        addParagraph(String.format("Total: %d images | ✅ OK: %d | ❌ Broken: %d",
            images.size(), images.size() - broken, broken));

        for (BrokenImageDetector.ImageResult img : images.stream()
                .filter(BrokenImageDetector.ImageResult::isBroken).toList()) {
            addParagraph("  ❌ " + img.url() + " [HTTP " + img.statusCode() + "]");
        }
    }

    public void addScreenshot(Path screenshotPath) throws Exception {
        addSectionHeader("📸 Email Screenshot");

        XWPFParagraph imgPara = document.createParagraph();
        imgPara.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = imgPara.createRun();

        try (InputStream is = Files.newInputStream(screenshotPath)) {
            run.addPicture(
                is,
                XWPFDocument.PICTURE_TYPE_PNG,
                screenshotPath.getFileName().toString(),
                Units.toEMU(500),  // width in points (~6.9 inches)
                Units.toEMU(350)   // height
            );
        }
    }

    public void save(String outputPath) throws Exception {
        Files.createDirectories(Paths.get(outputPath).getParent());
        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            document.write(fos);
        }
        System.out.println("📄 Word report saved: " + outputPath);
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private void addParagraph(String text) {
        XWPFParagraph p = document.createParagraph();
        XWPFRun run = p.createRun();
        run.setText(text);
    }

    private void addTableHeader(XWPFTable table, String[] headers) {
        XWPFTableRow headerRow = table.getRow(0);
        for (int i = 0; i < headers.length; i++) {
            XWPFTableCell cell = (i == 0) ? headerRow.getCell(0) : headerRow.addNewTableCell();
            cell.setText(headers[i]);
            cell.getParagraphs().get(0).getRuns().get(0).setBold(true);
        }
    }

    private String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }
}
```

---

## 13. Step 9 — Allure Reporting Integration {#step-9}

Add Allure listeners to Cucumber so every scenario gets rich, interactive reports.

```java
// AllureReportHelper.java
package com.email.automation.reporting;

import io.qameta.allure.Allure;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class AllureReportHelper {

    public static void attachLinkResults(List<LinkValidator.LinkResult> results) {
        StringBuilder sb = new StringBuilder("URL,StatusCode,Status,IsBroken\n");
        for (var r : results) {
            sb.append(String.format("%s,%d,%s,%s\n",
                r.url(), r.statusCode(), r.statusMessage(), r.isBroken()));
        }
        Allure.addAttachment("Link Validation Results", "text/csv",
            new ByteArrayInputStream(sb.toString().getBytes()), ".csv");
    }

    public static void attachAxeResults(List<AxeCoreInjector.AxeViolation> violations) {
        StringBuilder sb = new StringBuilder();
        if (violations.isEmpty()) {
            sb.append("✅ No violations found.\n");
        } else {
            for (var v : violations) {
                sb.append(String.format("[%s] %s — %s\n  %s\n\n",
                    v.impact().toUpperCase(), v.id(), v.description(), v.helpUrl()));
            }
        }
        Allure.addAttachment("ADA / WCAG Violations", "text/plain",
            new ByteArrayInputStream(sb.toString().getBytes()), ".txt");
    }

    public static void attachScreenshot(Path screenshotPath) throws Exception {
        Allure.addAttachment("Email Screenshot", "image/png",
            new ByteArrayInputStream(Files.readAllBytes(screenshotPath)), ".png");
    }

    public static void attachWordReport(Path reportPath) throws Exception {
        Allure.addAttachment("Full Word Report",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            new ByteArrayInputStream(Files.readAllBytes(reportPath)), ".docx");
    }
}
```

### Allure Cucumber Plugin Configuration

```java
// CucumberTestRunner.java
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = 
    "io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm," +
    "pretty," +
    "json:target/cucumber-reports/cucumber.json")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.email.automation.steps")
public class CucumberTestRunner {}
```

---

## 14. Step 10 — Cucumber BDD Glue {#step-10}

### Feature File

```gherkin
# email_validation.feature
Feature: SFMC Email Validation

  Background:
    Given the email testing framework is initialized

  Scenario Outline: Validate email for <campaignName>
    When  I trigger the SFMC email for definition "<definitionKey>" to a test inbox
    And   I wait for the email with subject "<subject>"
    Then  I open the email in a browser
    And   I validate all links and capture response codes
    And   I run ADA accessibility audit on the email
    And   I check for broken images
    And   I capture a full-page screenshot
    And   I generate a consolidated Word report
    And   the email should have no critical ADA violations
    And   the email should have no broken links

    Examples:
      | campaignName          | definitionKey              | subject                     |
      | Welcome Email         | WELCOME_EMAIL_SEND         | Welcome to Our Platform     |
      | Password Reset        | PASSWORD_RESET_SEND        | Reset Your Password         |
      | Order Confirmation    | ORDER_CONFIRM_SEND         | Your Order is Confirmed     |
```

### Step Definitions

```java
// EmailValidationSteps.java
package com.email.automation.steps;

import com.email.automation.sfmc.*;
import com.email.automation.mailosaur.*;
import com.email.automation.playwright.*;
import com.email.automation.reporting.*;
import com.mailosaur.models.Message;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.*;
import io.qameta.allure.Step;
import org.junit.jupiter.api.Assertions;

import java.nio.file.Path;
import java.util.*;

public class EmailValidationSteps {

    private SFMCAuthService        authService;
    private SFMCEmailTriggerService triggerService;
    private MailosaurService       mailosaurService;
    private BrowserManager         browserManager;
    private LinkValidator          linkValidator;
    private AxeCoreInjector        axeInjector;
    private BrokenImageDetector    imageDetector;
    private WordReportGenerator    reportGenerator;

    // Test state
    private String  inboxAddress;
    private Message receivedEmail;
    private String  emailHtml;
    private Path    screenshotPath;
    private Path    wordReportPath;

    private List<LinkValidator.LinkResult>      linkResults    = new ArrayList<>();
    private List<AxeCoreInjector.AxeViolation>  axeViolations  = new ArrayList<>();
    private List<BrokenImageDetector.ImageResult> imageResults  = new ArrayList<>();

    @Before
    public void setup() {
        authService     = new SFMCAuthService();
        triggerService  = new SFMCEmailTriggerService(authService);
        mailosaurService = new MailosaurService();
        browserManager  = new BrowserManager();
        linkValidator   = new LinkValidator();
        axeInjector     = new AxeCoreInjector();
        imageDetector   = new BrokenImageDetector();
        reportGenerator = new WordReportGenerator();
        browserManager.initialize();
    }

    @After
    public void teardown() {
        browserManager.close();
    }

    @Given("the email testing framework is initialized")
    public void frameworkInitialized() {
        System.out.println("✅ Framework initialized");
    }

    @When("I trigger the SFMC email for definition {string} to a test inbox")
    @Step("Trigger SFMC email: {definitionKey}")
    public void triggerSfmcEmail(String definitionKey) throws Exception {
        inboxAddress = mailosaurService.generateInboxAddress(
            "test-" + System.currentTimeMillis()
        );
        Map<String, String> attrs = Map.of(
            "FirstName", "QA Tester",
            "TestRun", String.valueOf(System.currentTimeMillis())
        );
        triggerService.triggerEmail(inboxAddress, definitionKey, attrs);
    }

    @When("I wait for the email with subject {string}")
    @Step("Wait for email with subject: {subject}")
    public void waitForEmail(String subject) throws Exception {
        receivedEmail = mailosaurService.waitForEmail(inboxAddress, subject);
        emailHtml     = mailosaurService.getHtmlBody(receivedEmail);
        System.out.println("📧 Email received: " + receivedEmail.subject());
    }

    @Then("I open the email in a browser")
    @Step("Open email HTML in Playwright browser")
    public void openEmailInBrowser() throws Exception {
        browserManager.loadEmailHtml(emailHtml);
    }

    @Then("I validate all links and capture response codes")
    @Step("Validate all links")
    public void validateLinks() throws Exception {
        linkResults = linkValidator.validateAllLinks(browserManager.getPage());
        AllureReportHelper.attachLinkResults(linkResults);
        System.out.println("🔗 " + linkResults.size() + " links validated");
    }

    @Then("I run ADA accessibility audit on the email")
    @Step("Run ADA/WCAG audit with axe-core")
    public void runAdaAudit() throws Exception {
        axeViolations = axeInjector.runAccessibilityAudit(browserManager.getPage());
        AllureReportHelper.attachAxeResults(axeViolations);
        System.out.println("♿ " + axeViolations.size() + " ADA violations found");
    }

    @Then("I check for broken images")
    @Step("Detect broken images")
    public void checkBrokenImages() throws Exception {
        imageResults = imageDetector.detectBrokenImages(browserManager.getPage(), emailHtml);
        System.out.println("🖼️ " + imageResults.size() + " images checked");
    }

    @Then("I capture a full-page screenshot")
    @Step("Capture screenshot")
    public void captureScreenshot() throws Exception {
        screenshotPath = browserManager.captureFullPageScreenshot(
            browserManager.getPage(), receivedEmail.subject()
        );
        AllureReportHelper.attachScreenshot(screenshotPath);
    }

    @Then("I generate a consolidated Word report")
    @Step("Generate Word report")
    public void generateWordReport() throws Exception {
        reportGenerator.addTitle(receivedEmail.subject());
        reportGenerator.addLinkValidationResults(linkResults);
        reportGenerator.addAxeResults(axeViolations);
        reportGenerator.addBrokenImageResults(imageResults);
        reportGenerator.addScreenshot(screenshotPath);

        String reportFilename = "target/reports/email-report-" +
            System.currentTimeMillis() + ".docx";
        reportGenerator.save(reportFilename);
        wordReportPath = Path.of(reportFilename);
        AllureReportHelper.attachWordReport(wordReportPath);
    }

    @Then("the email should have no critical ADA violations")
    @Step("Assert no critical ADA violations")
    public void assertNoCriticalADA() {
        long critical = axeViolations.stream()
            .filter(v -> "critical".equals(v.impact()))
            .count();
        Assertions.assertEquals(0, critical,
            "Found " + critical + " critical ADA violations!");
    }

    @Then("the email should have no broken links")
    @Step("Assert no broken links")
    public void assertNoBrokenLinks() {
        long broken = linkResults.stream().filter(r -> r.isBroken()).count();
        Assertions.assertEquals(0, broken,
            "Found " + broken + " broken links!");
    }
}
```

---

## 15. Improvements & Best Practices {#improvements}

### 15.1 Parallel Test Execution

Configure Cucumber to run scenarios in parallel to dramatically cut test time:

```properties
# junit-platform.properties
cucumber.execution.parallel.enabled=true
cucumber.execution.parallel.config.strategy=fixed
cucumber.execution.parallel.config.fixed.parallelism=4
```

Make sure each scenario uses a **unique Mailosaur inbox address** (as shown with `System.currentTimeMillis()`).

### 15.2 Config Externalization

Never hardcode API keys. Use a proper config hierarchy:

```java
// ConfigManager.java
public class ConfigManager {
    private static final Properties props = new Properties();

    static {
        // Load from env first, then fallback to properties file
        try {
            props.load(ConfigManager.class.getResourceAsStream("/application.properties"));
        } catch (Exception ignored) {}
    }

    public static String get(String key) {
        String env = System.getenv(key.toUpperCase().replace(".", "_"));
        return env != null ? env : props.getProperty(key);
    }
}
```

### 15.3 Retry Flaky Link Checks

Some CDN links may transiently return 5xx. Add exponential backoff:

```java
private LinkResult checkLinkWithRetry(String url, int maxRetries) {
    for (int attempt = 1; attempt <= maxRetries; attempt++) {
        try {
            LinkResult result = checkLink(url);
            if (result.statusCode() < 500) return result;
            Thread.sleep(1000L * attempt); // exponential backoff
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    return new LinkResult(url, 0, "Max retries exceeded", true);
}
```

### 15.4 Axe-Core Rule Exclusions

Some SFMC-injected markup may cause false positives. Exclude rules selectively:

```javascript
const results = await axe.run(document, {
    runOnly: { type: 'tag', values: ['wcag2aa'] },
    rules: {
        'region': { enabled: false },     // email layouts rarely need landmarks
        'bypass':  { enabled: false }
    }
});
```

### 15.5 Email Client Rendering Preview (Bonus)

Integrate **Email on Acid** or **Litmus** API to extend your report with cross-client rendering previews (Outlook, Gmail, Apple Mail) alongside your existing validations.

---

## 16. CI/CD Integration {#cicd}

### GitHub Actions Workflow

```yaml
# .github/workflows/email-qa.yml
name: Email Automation QA

on:
  schedule:
    - cron: '0 6 * * 1-5'    # Weekdays at 6am
  workflow_dispatch:          # Manual trigger

jobs:
  email-qa:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Install Playwright browsers
        run: mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI \
             -D exec.args="install chromium"

      - name: Run Email Automation Tests
        env:
          SFMC_CLIENT_ID:     ${{ secrets.SFMC_CLIENT_ID }}
          SFMC_CLIENT_SECRET: ${{ secrets.SFMC_CLIENT_SECRET }}
          SFMC_ACCOUNT_ID:    ${{ secrets.SFMC_ACCOUNT_ID }}
          MAILOSAUR_API_KEY:  ${{ secrets.MAILOSAUR_API_KEY }}
          MAILOSAUR_SERVER_ID: ${{ secrets.MAILOSAUR_SERVER_ID }}
        run: mvn test -Dtest=CucumberTestRunner

      - name: Generate Allure Report
        if: always()
        run: mvn allure:report

      - name: Upload Reports
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: email-qa-reports
          path: |
            target/reports/*.docx
            target/allure-report/**
            target/screenshots/**
```

---

## 17. Conclusion {#conclusion}

You now have a **full-stack email testing framework** that goes far beyond what most QA teams have ever built. Let's recap what this covers end-to-end:

- **SFMC REST API** — Programmatic email sends via OAuth 2.0 with token caching
- **Mailosaur** — Isolated per-test inboxes with polling and full HTML retrieval
- **Playwright** — Real browser rendering of email HTML for accurate validation
- **Link Checking** — Concurrent HTTP HEAD requests with response code capture
- **Axe-Core** — WCAG 2.1 AA accessibility auditing via JS injection
- **Broken Images** — DOM inspection + network interception
- **Word Reports** — Consolidated `.docx` with tables, findings, and screenshots
- **Allure** — Interactive dashboards with CSV, screenshot, and report attachments
- **CI/CD** — Scheduled GitHub Actions with secure secret management

### What's Next?

- **Email on Acid / Litmus integration** for cross-client rendering
- **Slack/Teams notifications** on test failure with screenshot previews
- **Historical trending** — track ADA violations and broken links over time
- **Load testing** via Gatling to validate SFMC sends under high volume
- **AI-powered assertion suggestions** using LLMs to flag suspicious content changes

---

*If this helped you build something great, clap 👏 and share it with your QA team. Have questions or want to extend this framework? Drop a comment below!*

---

**Tags:** `#java` `#playwright` `#salesforcem marketingcloud` `#emailtesting` `#automation` `#ada` `#wcag` `#cucumber` `#qa` `#mailosaur` `#allure`
