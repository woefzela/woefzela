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

public class MetadataFile {
	
	//HOUSEKEEPING
	private final String TAG = "MetadataFile";
	
	//SWITCHES
	private static final boolean LOG_V = false;
	private static final boolean LOG_D = false;
	private static final boolean LOG_I = false;
	private static final boolean LOG_W = false;
	
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
	public MetadataFile(String fPath) {
		mfPath = fPath;
	}
	
	public void writeMetadataFile (String prompt, String accent, String age, String gender, String location, String environment, String comments, String appVersion) {
		log.logD(TAG,"Writing metadata file for item.");
		
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
						out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"); //header
				        
				        //write values
				        out.write("<Woefzela>\n");
				        out.write("\t<prompt>" + prompt + "</prompt>\n");
				        
				        //Added 2011-06-21
				        out.write("\t<accent>" + accent + "</accent>\n");
				        out.write("\t<age>" + age + "</age>\n");
				        out.write("\t<gender>" + gender + "</gender>\n");
				        out.write("\t<location>" + location + "</location>\n");
				        out.write("\t<environment>" + environment + "</environment>\n");
				        out.write("\t<comments>" + comments + "</comments>\n");
				        out.write("\t<appVersion>" + appVersion + "</appVersion>\n");
				        
				        out.write("</Woefzela>\n");
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
	
	public void writeMetadataFileForSkippedItem(String prompt, HashSet<String> reasons) {
		log.logD(TAG,"Writing metadata file for skipped item.");
		log.logV(TAG,"Reason's given was: " + reasons);
	
		FileWriter fWrite = null;
		
		if (ifCanReadAndWriteSDCARD()) {
			log.logD(TAG, "Can read/write to SDCARD.");
			
			root = Environment.getExternalStorageDirectory();
			log.logI(TAG,"root = " + root.toString());
   
		    File fid = new File(mfPath);

			if (root.canWrite()) {
				try {
					fWrite = new FileWriter(fid);
					BufferedWriter out = new BufferedWriter(fWrite);
	            	try {
						out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"); //header
				        
				        //write values
				        out.write("<Woefzela>\n");
				        out.write("\t<prompt>" + prompt + "</prompt>\n");
				        out.write("\t<reasons>" + reasons + "</reasons>\n");
				        out.write("</Woefzela>\n");
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
