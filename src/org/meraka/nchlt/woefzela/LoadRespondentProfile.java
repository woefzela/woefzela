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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

public class LoadRespondentProfile {
	
	private static final String TAG = "LoadRespondentProfile";
	private static boolean PLAIN_TEXT = true;
	boolean mExternalStorageAvailable = false;
	boolean mExternalStorageWriteable = false;
	CharSequence xmlHeader;//CharSequence maintains formatting of resources. Always?
	
	//Info from file
	private String fileMimeType = null;
	private String fileContentType = null;
	private String name = null;
	private String surname = null;
	private String age = null;
	private String mobile = null;
	private String emailAddr = null;
	private String sAccent = null;
	private String sGender = null;
	private String sTerms = null;
	private String profileKey = null;

	public LoadRespondentProfile(Context context, Uri fName) {

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

		//Write file
		if (mExternalStorageWriteable != false) {
			Log.i(TAG,"Trying to write xml file...");
			try {
			    File root = Environment.getExternalStorageDirectory();
			    Log.i(TAG,"root = " + root.toString());
			    
			    if (root.canRead()) {
			    	Log.i(TAG,"filename to read is: " + fName);
			    	File fid = new File(fName.getEncodedPath());
			    	Log.i(TAG,"fid.<something>() =  " + fid.getPath()); //not want: fid.getAbsolutePath(); getCanonicalPath()

			    	FileReader fRead = new FileReader(fid);
			        BufferedReader in = new BufferedReader(fRead);
			        
			        fileMimeType = in.readLine();
			        fileContentType = in.readLine();
			        name = in.readLine();
			        surname = in.readLine();
			        age = in.readLine();
			        mobile = in.readLine();
			        emailAddr = in.readLine();
			        sAccent = in.readLine();
			        sGender = in.readLine();
			        sTerms = in.readLine();
			        profileKey = in.readLine();
			        
			        Log.i(TAG,">> Information read from file <<");
			        Log.i(TAG,"fileMimeType: " + fileMimeType);
			        Log.i(TAG,"fileContentType: " + fileContentType);
			        Log.i(TAG,"name: " + name);
			        Log.i(TAG,"surname: " + surname);
			        Log.i(TAG,"age: " + age);
			        Log.i(TAG,"mobile: " + mobile);
			        Log.i(TAG,"emailAddr: " + emailAddr);
			        Log.i(TAG,"lang/accent: " + sAccent);
			        Log.i(TAG,"gender: " + sGender);
			        Log.i(TAG,"sTerms: " + sTerms);	
			        Log.i(TAG,"profileKey: " + profileKey);

			        in.close();
			    }
			    else {
			    	Log.e(TAG, "root.canRead is false. Why?");
			    }
			} catch (IOException e) {
			    Log.e(TAG, "Could not read file " + e.getMessage());
			}
		}
		else {
			Log.e(TAG, "Sorry, but the SDcard is not ready/writable.");
		}
	}
	
	public String getName() {
		return name;
	}
	
	public String getSurname() {
		return surname;
	}
	
	public String getAge() {
		return age;
	}
	
	public String getProfileKey() {
		return profileKey;
	}
	
	public String getMobile() {
		return mobile;
	}
	
	public String getEmailAddr() {
		return emailAddr;
	}
	
	public String getAccent() {
		return sAccent;
	}
	
	public String getGender() {
		return sGender;
	}
	
	public String getTermStatus() {
		return sTerms;
	}
}
