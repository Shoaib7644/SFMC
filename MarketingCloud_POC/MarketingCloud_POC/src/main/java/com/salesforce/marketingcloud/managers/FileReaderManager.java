/**Author Name : RafterOne QA
 * Date : 11-Mar-2022
 * Description: 
 * Last Edited By :RafterOne QA
 * Last Edited Date : 11-Mar-2022
 */
package com.salesforce.marketingcloud.managers;

import com.salesforce.marketingcloud.dataprovider.ConfigFileReader;

/**
 * @author RafterOne QA
 *
 */
public class FileReaderManager {
	private static FileReaderManager fileReaderManager = new FileReaderManager();
    private static ConfigFileReader configFileReader;

    private FileReaderManager() {
    }

    public static FileReaderManager getInstance( ) {
        return fileReaderManager;
    }

    public ConfigFileReader getConfigReader() {
        return (configFileReader == null) ? new ConfigFileReader() : configFileReader;
    }
}

