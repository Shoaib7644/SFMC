package com.salesforce.marketingcloud.managers;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.BrowserType.LaunchOptions;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.ViewportSize;
import com.salesforce.marketingcloud.constant.Constant;
import com.salesforce.marketingcloud.enums.EnvironmentType;
import com.salesforce.marketingcloud.enums.WebBrowserType;
import com.salesforce.marketingcloud.exception.CustomException;

public class PlaywrightManager {

	public static WebBrowserType webBrowserType;
	public static EnvironmentType environmentType;
	
	public PlaywrightManager() {
		
		webBrowserType = FileReaderManager.getInstance().getConfigReader().getWebBrowser();
		environmentType = FileReaderManager.getInstance().getConfigReader().getEnvironment();
	}
	
	public static Page intializePlaywright() {
		try {
			if (Constant.PLAYWRIGHT == null) {
				Constant.PLAYWRIGHT = Playwright.create();
			}
			Constant.PAGE = intializeBrowser();
			return Constant.PAGE;
			}
			catch(Exception e) {
				throw new CustomException("Playwright is not created");
			}
			
	}
	
	public static Page intializeBrowser() {
		try {
			int width = 1920;
			int height = 1080;

			// Common launch options
			LaunchOptions lOptions = new LaunchOptions();
			lOptions.setHeadless(false);
			List<String> args = new ArrayList<>();
			args.add("--incognito");
			args.add("--start-maximized");
			args.add("--window-position=0,0");
			args.add("--window-size=" + width + "," + height);
			lOptions.setArgs(args);

			switch (webBrowserType) {
			case FIREFOX:
				Constant.BROWSER = Constant.PLAYWRIGHT.firefox().launch(lOptions);
				Constant.BROWSERCONTEXT = Constant.BROWSER.newContext(new Browser.NewContextOptions().setViewportSize(new ViewportSize(width, height)));
				return Constant.BROWSERCONTEXT.newPage();

			case CHROME:
			    lOptions.channel = "chrome";
			    Constant.BROWSER = Constant.PLAYWRIGHT.chromium().launch(lOptions);
			    // Added Permissions for clipboard access
			    Constant.BROWSERCONTEXT = Constant.BROWSER.newContext(new Browser.NewContextOptions()
			            .setViewportSize(new ViewportSize(width, height))
			            .setStorageStatePath(Paths.get(Constant.SFAuthFileJSONPath))
			            .setPermissions(Arrays.asList("clipboard-read", "clipboard-write"))); 
			    return Constant.BROWSERCONTEXT.newPage();
				
			case WEBKIT:
				Constant.BROWSER = Constant.PLAYWRIGHT.webkit().launch(lOptions);
				Constant.BROWSERCONTEXT = Constant.BROWSER.newContext(new Browser.NewContextOptions().setViewportSize(new ViewportSize(width, height)));
				return Constant.BROWSERCONTEXT.newPage();

			case CHROMIUM:
				if(Constant.BROWSER != null)
				{
					Constant.BROWSERCONTEXT = Constant.BROWSER.newContext(new Browser.NewContextOptions().setViewportSize(new ViewportSize(width, height)));
				}
				else {
				Constant.BROWSER = Constant.PLAYWRIGHT.chromium().launch(lOptions);
				Constant.BROWSERCONTEXT = Constant.BROWSER.newContext(new Browser.NewContextOptions().setViewportSize(new ViewportSize(width, height)));
				}
				return Constant.BROWSERCONTEXT.newPage();
				
			case EDGE:			
				lOptions.channel = "msedge";
				Constant.BROWSER = Constant.PLAYWRIGHT.chromium().launch(lOptions);
				Constant.BROWSERCONTEXT = Constant.BROWSER.newContext(new Browser.NewContextOptions().setViewportSize(new ViewportSize(width, height)));
				return Constant.BROWSERCONTEXT.newPage();
			
			default:
				throw new CustomException("Playwright is not created");
			}
		}
		catch(Exception e) {
			throw new CustomException("Playwright is not created");
		}			
	}
	
	public static void closePlaywright() {
		try {
			Constant.PAGE.close();
			Constant.BROWSERCONTEXT.close();
//			Constant.PLAYWRIGHT.close();
			
		}
		catch(Exception e) {
				throw new CustomException("Playwright is not created");
		}	
	}
}