package com.salesforce.marketingcloud.utils;

import java.util.Arrays;
import java.util.regex.Pattern;

public class SOQLUtils {

	/**
	 * Method to get All Select Fields from a Query defined in getQuery(String objName, Object... params) method of SOQL Constant Class
	 * This is only for straight forward SOQL Queries, Functioal Queries like COUNT(Field_Name) won't be supported
	 * @return - List of Select Fields
	 */
	public static String[] getSelectFieldsFromSOQLQuery(String query) {
		
		Pattern selectPattern = Pattern.compile("select", Pattern.CASE_INSENSITIVE);
		Pattern fromPattern = Pattern.compile("from", Pattern.CASE_INSENSITIVE);
		String tempQuery = query.replaceAll("'%s'", "");
		String temp = selectPattern.split(tempQuery)[1];
		String temp2 = fromPattern.split(temp)[0];
		String[] fields = temp2.split(",");
		for(int i=0; i<fields.length; i++) {
			fields[i] = fields[i].trim();
		}
		return fields;
	}
	
	/**
	 * Helper Method for getSelectFieldsFromSOQLQuery(String objName)(from SOQLUtils Class) to remove 'Id' Field from the given array
	 * @return - Array of Fields with 'Id' String removed from it
	 */
	public static String[] removedIdFieldFromSOQLSelectFields(String[] fields) {
		String[] fieldsWithoutIdString = Arrays.stream(fields)
												.filter(s -> !s.equalsIgnoreCase("id"))
												.toArray(String[]::new);
		return fieldsWithoutIdString;
	}
}
