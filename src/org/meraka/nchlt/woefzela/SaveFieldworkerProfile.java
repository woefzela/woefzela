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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Environment;
import android.util.Log;

public class SaveFieldworkerProfile {
	
	//fix lowercase later
	private static final String TAG = "SaveFieldworkerProfile";
	private static boolean PLAIN_TEXT = true;
	private static final String primaryFoldername = "/Woefzela"; //get later from resource
	private static final String profileFolderName = "/Profiles"; //get later from resource
	private static final String profileFolderSubdir = "/Fieldworkers"; //get later from resource
	
	private static final String FILENAME_BASE = ""; 
	private static final String FILE_EXTENSION = ".txt";
	
	boolean mExternalStorageAvailable = false;
	boolean mExternalStorageWriteable = false;
	CharSequence xmlHeader;//CharSequence maintains formatting of resources. Always?
	private String filename;

	private static String NEWLINE = "\n";
	private String methodTAG = "SaveRespondentProfile"; //Default

	public SaveFieldworkerProfile(Context context, String sFWName, String sFWSurname, String sFWID, String sFWMobile, String sFWEmail, String profileKey) {

		String methodTAG = "SaveRespondentProfile>Constructor";
		
		String localeString = java.util.Locale.getDefault().getDisplayName();
		Log.i(TAG,"localeString" + localeString);
		
		//Check if SDcard is ready to write to
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
		    // We can read and write the media
		    mExternalStorageAvailable = mExternalStorageWriteable = true;
		    Log.i(TAG,"SDCARD: Yay, we can read and write to it!");
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
		    // We can only read the media
		    mExternalStorageAvailable = true;
		    mExternalStorageWriteable = false;
		    Log.i(TAG,"SDCARD: Nope, we can only read it.");
		} else {
		    // Something else is wrong. It may be one of many other states, but all we need
		    //  to know is we can neither read nor write
		    mExternalStorageAvailable = mExternalStorageWriteable = false;
		    Log.i(TAG,"SDCARD: Hmmm...we can neither read nor write to it!");
		}
		
		//Prepare filename
    	SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmmss");
    	Date currentDateTime = new Date();
    	String dateTimeString = formatter.format(currentDateTime);
    	
    	filename = constructFilename(sFWName, sFWSurname, sFWID);
    	Log.i(TAG,"saving in file = " + filename + FILE_EXTENSION);
		
		//Write file
		if (mExternalStorageWriteable != false) {
			Log.i(TAG,"Trying to write xml file...");
			try {
			    File root = Environment.getExternalStorageDirectory();
			    Log.i(TAG,"root = " + root.toString());
			    
			    //Cannot create in one go!?
			    new File("/sdcard" + primaryFoldername).mkdir(); //error checking??
			    new File("/sdcard" + primaryFoldername + profileFolderName).mkdir(); //error checking??
			    new File("/sdcard" + primaryFoldername + profileFolderName + profileFolderSubdir).mkdir(); //error checking??
			    
			    if (root.canWrite()){
			    	File fid = new File(root + primaryFoldername + profileFolderName + profileFolderSubdir, filename + FILE_EXTENSION);
			        String p = fid.getAbsolutePath();
			        FileWriter fWrite = new FileWriter(fid);
			        BufferedWriter out = new BufferedWriter(fWrite);
			        
			        // Get the string resource
			        //xmlHeader = ctx.getText(R.string.xmlHeader);
	            	Log.i(TAG,"filename = " + filename);
	            	Log.i(TAG,"fid.getAbsolutePath() returns " + p);

	            	//write header section...TODO from resource
	            	out.write("MIME-Version: 1.0"); out.write(NEWLINE);
	            	out.write("Content-Type: text/plain"); out.write(NEWLINE);
	            	
			        //write values
	        		out.write(sFWName); out.write(NEWLINE);
	        		out.write(sFWSurname); out.write(NEWLINE);
	        		out.write(sFWID); out.write(NEWLINE);
	        		out.write(sFWMobile); out.write(NEWLINE);
	        		out.write(sFWEmail); out.write(NEWLINE);
	        		out.write(profileKey); out.write(NEWLINE);
			        			        
			        out.close();
			    }
			    else {
			    	LOG_E(TAG, methodTAG, "root.canWrite is false.");
			    }
			} 
			catch (IOException e) {
			    LOG_E(TAG, methodTAG, "Could not write file " + e.getMessage());
			}
		}
		else {
			LOG_E(TAG, methodTAG, "Sorry, but the SDcard is not ready/writable.");
		}
	}
	
	public String getFilename() {
		return filename;
	}
	
	public String constructFilename(String sFWName, String sFWSurname, String sFWID) {
		String name = removeSpaces(sFWName);
		String surname = removeSpaces(sFWSurname);
		String dob = sFWID.substring(0,6); 
		Log.i(TAG,"constructFilename = " + name + surname + dob);
		return name + surname + dob;
	}
	
	//Credit: http://www.rgagnon.com/javadetails/java-0352.html
	public String removeSpaces(String s) {
		  StringTokenizer st = new StringTokenizer(s," ",false);
		  String t="";
		  while (st.hasMoreElements()) t += st.nextElement();
		  return t;
	}
	
	private void LOG_E(String classTAG, String methodTAG, String message) {
		String s = classTAG + ":" + methodTAG + "::" + message;
		Log.e(classTAG, s);
		
		//Write to LOG file
		new CreateErrorLogThenDie(s);
	}
}