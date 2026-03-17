package com.salesforce.marketingcloud.steps;

import java.util.List;
import java.util.stream.Collectors;

import com.salesforce.marketingcloud.context.ValidationContext;
import com.salesforce.marketingcloud.constant.Constant;
import com.salesforce.marketingcloud.model.EmailValidationResult;
import com.salesforce.marketingcloud.model.LinkResult;
import com.salesforce.marketingcloud.validator.SmartLinkValidator;
import com.salesforce.marketingcloud.utils.MailosaurUtils;

import com.salesforce.marketingcloud.pageobjects.*;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Given;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.Assert;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.options.LoadState;
import io.qameta.allure.Allure;

public class MailSteps {

    private static final Logger LOG = LoggerFactory.getLogger(MailSteps.class);

    // ── Unchanged steps ───────────────────────────────────────────────────────────────────────

    @When("Get email from receiver's inbox for {string}")
    public void get_email_from_receivers_inbox_for(String emailName) throws Exception {
        try {
            String html = MailosaurUtils.getLatestEmailHtmlForSubject(emailName, 120);
            if (html == null) {
                throw new AssertionError("Email with name '" + emailName + "' was not found within timeout");
            }
            Constant.latestEmailHtml = html;
            Constant.latestEmailName = emailName;
            MailosaurUtils.openEmailInPlaywright(html);
        } catch (RuntimeException e) {
            throw new AssertionError("Email not received within timeout: " + e.getMessage());
        }
    }
    
    @When("Load the latest email HTML into the browser")
    public void load_latest_email_html_into_browser() {
        String html = Constant.latestEmailHtml;
        if (html == null || html.isEmpty()) {
            String msg = "No email HTML available to load into browser";
            LOG.error(msg);
            Allure.step(msg);
            throw new AssertionError(msg);
        }

        Page page = Constant.PAGE;
        try {
            Allure.step("Load latest email HTML into Playwright page");
            if (page == null) {
                if (Constant.BROWSERCONTEXT != null) {
                    page = Constant.BROWSERCONTEXT.newPage();
                } else if (Constant.BROWSER != null) {
                    Constant.BROWSERCONTEXT = Constant.BROWSER.newContext();
                    page = Constant.BROWSERCONTEXT.newPage();
                } else {
                    String msg = "Playwright Browser/Context not initialized (Constant.BROWSER or Constant.BROWSERCONTEXT is null)";
                    LOG.error(msg);
                    Allure.step(msg);
                    throw new AssertionError(msg);
                }
                Constant.PAGE = page;
            }

            page.setContent(html);
            page.waitForLoadState();

            LOG.info("Loaded latest email HTML into browser (page)");
            Allure.step("Loaded latest email HTML into browser");
        } catch (AssertionError ae) {
            throw ae;
        } catch (Exception e) {
            String msg = "Failed to load email HTML into browser: " + e.getMessage();
            LOG.error(msg, e);
            Allure.step(msg);
            throw new AssertionError(msg, e);
        }
    }

    
    @Then("validate broken links from email")
    public void validate_broken_links_from_email() throws Exception {
        String html = Constant.latestEmailHtml;
        if (html == null || html.isEmpty()) {
            String msg = "No email HTML available to validate links";
            LOG.error(msg);
            ValidationContext.addError(msg);
            Allure.step(msg);
            return;
        }

        try {
            List<String> links = extractLinksFromHtml(html);
            LOG.info("Found {} links in email", links.size());

            EmailValidationResult result = SmartLinkValidator.validateLinks(
                    Constant.latestEmailHtml == null ? "" : "EmailFromMailosaur", links);

            // Always attach the SmartLinkValidator HTML report to Allure — useful for QA
            SmartLinkValidator.attachReportToAllure(result);

            if (result.getBrokenLinks() > 0) {
                
                String err = "Broken links found: "
                        + result.getOnlyBrokenLinks().stream()
                                .map(LinkResult::getUrl)
                                .collect(Collectors.joining(", "));
                LOG.warn(err); // warn, not error — real broken-link FAIL comes from BrokenLinkValidator
                ValidationContext.addError(err + " - " + result.generateSummary());
                Allure.step(err);
                Allure.addAttachment("Broken Links Failure", "text/plain", result.generateSummary(), ".txt");
            } else {
                LOG.info("No broken links detected. Summary: {}", result.generateSummary());
                Allure.step("No broken links detected");
            }
        } catch (AssertionError ae) {
            String msg = "Link validation assertion failed: " + ae.getMessage();
            LOG.error(msg);
            ValidationContext.addError(msg);
            Allure.step(msg);
        } catch (Exception e) {
            String msg = "Failed to validate links from email: " + e.getMessage();
            LOG.error(msg, e);
            ValidationContext.addError(msg);
            Allure.step(msg);
            Allure.addAttachment("Broken Links Exception", "text/plain", msg + "\n" + e.toString(), ".txt");
        }
    }

    
    @Then("validate all images in the email are loading")
    public void validate_images_loading() {
        Page page = Constant.PAGE;
        if (page == null) {
            String msg = "Playwright Page is not initialized (Constant.PAGE is null)";
            LOG.error(msg);
            ValidationContext.addError(msg);
            Allure.step(msg);
            return;
        }

        try {
            page.waitForLoadState(LoadState.NETWORKIDLE);

            String evalScript = "(async () => {\n" +
                    "  const broken = [];\n" +
                    "  function normalizeUrl(u){ if(!u) return u; return u.replace(/(^\\\")|(\\\"$)/g, ''); }\n" +
                    "  const imgs = Array.from(document.querySelectorAll('img'));\n" +
                    "  for (let i=0;i<imgs.length;i++){\n" +
                    "    const el = imgs[i];\n" +
                    "    let src = el.currentSrc || el.src || el.getAttribute('src');\n" +
                    "    if(!src) continue;\n" +
                    "    src = normalizeUrl(src);\n" +
                    "    try{\n" +
                    "      const res = await new Promise(resolve => {\n" +
                    "        const img = new Image();\n" +
                    "        const timer = setTimeout(() => resolve(false), 5000);\n" +
                    "        img.onload = () => { clearTimeout(timer); resolve(true); };\n" +
                    "        img.onerror = () => { clearTimeout(timer); resolve(false); };\n" +
                    "        try{ img.src = new URL(src, document.baseURI).href; } catch(e){ img.src = src; }\n" +
                    "      });\n" +
                    "      if(!res) broken.push({type:'img', src: src, alt: el.alt || ''});\n" +
                    "    } catch(e){ broken.push({type:'img', src: src, alt: el.alt || '', error: String(e)}); }\n" +
                    "  }\n" +
                    "  const all = Array.from(document.querySelectorAll('*'));\n" +
                    "  for (const el of all){\n" +
                    "    try{\n" +
                    "      const style = getComputedStyle(el);\n" +
                    "      const bg = style.getPropertyValue('background-image');\n" +
                    "      if(bg && bg !== 'none') {\n" +
                    "        const urls = [];\n" +
                    "        bg.replace(/url\\(([^)]+)\\)/g, (m, u) => { urls.push(u.replace(/^\\\"|\\\"$/g,'')); return m; });\n" +
                    "        for (const u of urls){\n" +
                    "          let raw = u; if(!raw) continue; raw = raw.replace(/^\\\"|\\\"$/g,'');\n" +
                    "          let resolved = raw; try{ resolved = new URL(raw, document.baseURI).href; } catch(e){}\n" +
                    "          try{\n" +
                    "            const res = await new Promise(resolve => {\n" +
                    "              const img = new Image();\n" +
                    "              const timer = setTimeout(() => resolve(false), 5000);\n" +
                    "              img.onload = () => { clearTimeout(timer); resolve(true); };\n" +
                    "              img.onerror = () => { clearTimeout(timer); resolve(false); };\n" +
                    "              img.src = resolved;\n" +
                    "            });\n" +
                    "            if(!res) broken.push({type:'background', src: resolved, selector: el.tagName.toLowerCase(), class: el.className || ''});\n" +
                    "          } catch(e){ broken.push({type:'background', src: resolved, selector: el.tagName.toLowerCase(), class: el.className || '', error: String(e)}); }\n" +
                    "        }\n" +
                    "      }\n" +
                    "    } catch(e){}\n" +
                    "  }\n" +
                    "  return JSON.stringify(broken);\n" +
                    "})()";

            Object raw = page.evaluate(evalScript);
            String json = raw == null ? "[]" : raw.toString();

            com.google.gson.Gson gson = new com.google.gson.Gson();
            java.lang.reflect.Type listType =
                    new com.google.gson.reflect.TypeToken<java.util.List<java.util.Map<String, Object>>>(){}.getType();
            List<java.util.Map<String, Object>> broken = gson.fromJson(json, listType);

            StringBuilder brokenBuilder = new StringBuilder();
            int count = 0;
            for (java.util.Map<String, Object> item : broken) {
                count++;
                String type     = item.getOrDefault("type",     "unknown").toString();
                String src      = item.getOrDefault("src",      "").toString();
                String alt      = item.getOrDefault("alt",      "").toString();
                String selector = item.getOrDefault("selector", "").toString();
                String cls      = item.getOrDefault("class",    "").toString();
                String error    = item.getOrDefault("error",    "").toString();

                String msg;
                if ("img".equalsIgnoreCase(type)) {
                    msg = String.format("Broken Image [%d] Src: %s | Alt: %s", count, src, alt);
                } else if ("background".equalsIgnoreCase(type)) {
                    msg = String.format("Broken background-image [%d] Url: %s | Element: %s.%s",
                            count, src, selector, cls);
                } else {
                    msg = String.format("Broken resource [%d] Type: %s | Src: %s", count, type, src);
                }
                if (error != null && !error.isEmpty()) msg += " | Error: " + error;

                // Soft-assertion only — record in com.salesforce ValidationContext.
                // Do NOT write to framework ValidationContext (that was the source of
                // the "Runtime Error" key pollution in the debug log).
                ValidationContext.addError(msg);
                LOG.warn(msg); // warn not error — this is a test finding, not a system error
                brokenBuilder.append(msg).append('\n');
            }

            if (count > 0) {
                String brokenList = brokenBuilder.toString();
                Allure.step(count + " image(s) failed to load — see Image Loading Status in audit report");
                Allure.addAttachment("Broken Images Summary", "text/plain", brokenList, ".txt");
            } else {
                Allure.step("All images and background images loaded successfully");
                LOG.info("All images and background images loaded successfully");
            }

        } catch (Exception e) {
            String msg = "Failed to validate images: " + e.getMessage();
            LOG.error(msg, e);
            ValidationContext.addError(msg);
            Allure.step(msg);
            Allure.addAttachment("Image Validation Exception", "text/plain",
                    msg + "\n" + e.toString(), ".txt");
        }
    }

    // ── Unchanged helper ──────────────────────────────────────────────────────────────────────

    private List<String> extractLinksFromHtml(String html) {
        java.util.List<String> links = new java.util.ArrayList<>();
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "href=\\\"([^\\\"]+)\\\"", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher m = p.matcher(html);
        while (m.find()) {
            String url = m.group(1).trim();
            if (url.startsWith("mailto:")) continue;
            if (url.startsWith("#")) continue;
            if (url.startsWith("/") || !url.matches("^https?://.*")) continue;
            links.add(url);
        }
        return links;
    }
}