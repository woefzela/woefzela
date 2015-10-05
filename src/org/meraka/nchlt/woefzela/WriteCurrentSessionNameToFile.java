/* 
 * Copyright (c) 2011 CSIR, Meraka, South Africa
 *
 * Contributors: 
 *   - The Department of Arts and Culture, The Government of South Africa.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *  
 * Developer: Nic de Vries
 *   
 */

package org.meraka.nchlt.woefzela;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;

import android.content.pm.PackageInfo;
import android.os.Environment;
import android.util.Log;

public class WriteCurrentSessionNameToFile {
	
	//HOUSEKEEPING
	private final String TAG = "WriteCurrentSessionNameToFile";
	
	//SWITCHES
	private static final boolean LOG_V = true;
	private static final boolean LOG_D = true;
	private static final boolean LOG_I = true;
	private static final boolean LOG_W = true;
	
	private static final String NEWLINE = "\n";
	private static final int BAD_FILE_LINE_NUMBER = -1;
	
	private static final String programFoldername = "/Woefzela";
	private static final String dataOutputFolderName = "/OutputData";
	private static final String statsOutputFolderName = "/OutputStats";
	
	private File root = null;
	private String mfPath = null;
	
	//Logging
	private Logging log = new Logging(LOG_V, LOG_D, LOG_I, LOG_W); //Note: No LOG_E!
	
	//Constructor
	public WriteCurrentSessionNameToFile(String fPath) {
		mfPath = fPath;
	}
	
	public void writeName(String sessionFolderName) {
		log.logD(TAG,"Writing to busy session file: " + sessionFolderName);
		
		FileWriter fWrite = null;
		
		if (ifCanReadAndWriteSDCARD()) {
			log.logD(TAG, "Can read/write to SDCARD.");
			
			root = Environment.getExternalStorageDirectory();
			log.logV(TAG,"root = " + root.toString());
   
		    File fid = new File(mfPath);

			if (root.canWrite()) {
				try {
					fWrite = new FileWriter(fid);
					BufferedWriter out = new BufferedWriter(fWrite);
	            	try {
						out.write(sessionFolderName + "\n");
						out.close();
					} 
	            	catch (IOException e3) {
						// TODO Auto-generated catch block
						e3.printStackTrace();
					}
				} 
				catch (IOException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
		    }
			else {
		    	//TODO say could no write and exit
		    	Log.e(TAG, "FATAL ERROR: Could not write file.");
			}
		}
		else {
			Log.e(TAG,"SDCARD: Not able to either read and/or write from/to SDCARD.");
		}
	}

	private boolean ifCanReadAndWriteSDCARD() {
		//TODO maybe move to own class and instantiate an object
	
		//TODO find out WHY do I get this warning of variables no being used when they are according to me
		boolean mExternalStorageAvailable = false;
		boolean mExternalStorageWriteable = false;
		
		String state = Environment.getExternalStorageState();
	
		if (Environment.MEDIA_MOUNTED.equals(state)) {
		    // We can read and write the media
		    mExternalStorageAvailable = mExternalStorageWriteable = true;
		    return true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
		    // We can only read the media
		    mExternalStorageAvailable = true;
		    mExternalStorageWriteable = false;
		    return false;
		} else {
		    // Something else is wrong. It may be one of many other states, but all we need
		    //  to know is we can neither read nor write
		    mExternalStorageAvailable = mExternalStorageWriteable = false;
		    return false;
		}
	}
}
