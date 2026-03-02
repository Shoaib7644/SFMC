package com.salesforce.marketingcloud.dataprovider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import com.salesforce.marketingcloud.constant.Constant;
import com.salesforce.marketingcloud.exception.CustomException;


public class LocatorsFileReader {
	
	@SuppressWarnings("unchecked")
	public static void readLocatorProperties() {
		
		Properties properties = new Properties();
		BufferedReader reader;
		boolean flag = false;
		try {
			reader = new BufferedReader(new FileReader(Constant.LOCATORSFILEPATH));
			properties.load(reader);
			File file = new File(Constant.LOCATORSFILEPATH);
			ArrayList<String> keysList = new ArrayList<>();
	        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
	            String line;
	            while ((line = br.readLine()) != null) {
	            	if(line.contains("=")) {
	            		keysList.add(line.split("=")[0]);
	            	}
	            }
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	        for (int i = 0; i < keysList.size()-1; i++){
	            for (int j = i+1; j < keysList.size(); j++){
	                if( (keysList.get(i).equals(keysList.get(j))) && (i != j) ){
	                	flag = true;
	                    System.out.println("Duplicate Key found : "+keysList.get(j));
	                }
	            }
	        }
	        if(flag == false) {
		        Iterator<?> itr = properties.entrySet().iterator();  
		        while(itr.hasNext()){  
		        	Map.Entry<String, String> entry = (Map.Entry<String, String>)itr.next();
		        	Constant.locatorsMap.put(entry.getKey(), entry.getValue());
		        }
	        }
	        else
	        	throw new CustomException("Duplicate Elements found in locator.properties");
		} catch (IOException e) {
			throw new CustomException("locators.properties file not found");
		}
	}

}
