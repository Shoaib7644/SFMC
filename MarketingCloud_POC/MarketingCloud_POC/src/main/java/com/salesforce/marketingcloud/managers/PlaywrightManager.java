package com.salesforce.marketingcloud.managers;

import java.nio.file.Paths;
import java.util.ArrayList;
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
			switch (webBrowserType) {
			case FIREFOX:
				Constant.BROWSERCONTEXT = Constant.PLAYWRIGHT.firefox().launch().newContext();
				return Constant.BROWSERCONTEXT.newPage();

			case CHROME:
				LaunchOptions lOptions = new LaunchOptions();
				lOptions.setHeadless(false);
				lOptions.channel = "chrome";
				List<String> args = new ArrayList<>();
				args.add("--incognito");
				args.add("--start-maximized");
				args.add("--window-position=0,0");
				args.add("--window-size=1920,1080");
				lOptions.setArgs(args);
				Browser browser = Constant.PLAYWRIGHT.chromium().launch(lOptions);
				Constant.BROWSERCONTEXT = browser.newContext(new Browser.NewContextOptions().setViewportSize(new ViewportSize(1280, 585))
						.setStorageStatePath(Paths.get(Constant.SFAuthFileJSONPath)));
				return Constant.BROWSERCONTEXT.newPage();
				
			case WEBKIT:
				Constant.BROWSERCONTEXT = Constant.PLAYWRIGHT.webkit().launch().newContext();
				return Constant.BROWSERCONTEXT.newPage();

			case CHROMIUM:
				if(Constant.BROWSERCONTEXT != null)
				{
					Constant.BROWSERCONTEXT = Constant.BROWSER.newContext();
				}
				else {
				Constant.BROWSER = Constant.PLAYWRIGHT.chromium().launch();
				Constant.BROWSERCONTEXT = Constant.BROWSER.newContext();
				}
				return Constant.BROWSERCONTEXT.newPage();
				
			case EDGE:			
				LaunchOptions lpcOptionsEdge = new BrowserType.LaunchOptions();
				lpcOptionsEdge.setHeadless(false);
				lpcOptionsEdge.channel = "msedge";
				Constant.BROWSERCONTEXT = Constant.PLAYWRIGHT.chromium().launch(lpcOptionsEdge).newContext();
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
