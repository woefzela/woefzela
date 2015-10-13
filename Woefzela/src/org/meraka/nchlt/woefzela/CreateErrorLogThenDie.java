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

import android.os.Environment;
import android.util.Log;

public class CreateErrorLogThenDie {
	
	//HOUSEKEEPING
	private static final String TAG = "CreateErrorLogThenDie";
	private static final String NEWLINE = "\n";
	private static final boolean APPEND_TO_FILE = true;
	
	private boolean mExternalStorageWriteable = true;
	private File root = null;
	private String programFoldername = null;
	FileWriter fWrite = null;
	private CharSequence dateTimeNow = "yyyyMMddHHmmss";

	/** Constructor */
	public CreateErrorLogThenDie(String msg) {
		if(ifCanReadAndWriteSDCARD()) {
			if (mExternalStorageWriteable != false) {
				root = Environment.getExternalStorageDirectory();
			    Log.i(TAG,"root = " + root.toString());
			    
			    //TODO Get properly from external resources (location and filename)
			    File fid = new File("/sdcard/Woefzela/Tracking/WoefzelaErrorLOG.txt");
			    
			    if (root.canWrite()) {
					try {
						fWrite = new FileWriter(fid, APPEND_TO_FILE);
						BufferedWriter out = new BufferedWriter(fWrite);
		            	try {
		            		GetDateTimeString dtObject = new GetDateTimeString();
		            		dateTimeNow = dtObject.getString();
							out.write(dateTimeNow +  msg); 
							out.write(NEWLINE);
							out.close();
						} 
		            	catch (IOException e3) {
							e3.printStackTrace();
							Log.e(TAG,"errorLOG.txt could not be written.");
							System.exit(0);
						}
					} 
					catch (IOException e2) {
						e2.printStackTrace();
						Log.e(TAG,"errorLOG.txt could not be written.");
						System.exit(0);
					}
			    }
			    else {
			    	Log.e(TAG,"errorLOG.txt could not be written.");
					System.exit(0);
			    }
		}
		else {
			Log.e(TAG, "Sorry, but the SDcard is not ready/writable.");
		}
		}
		else {
			//TODO Write to internal storage
		}
		System.exit(0);
	}
	
	private boolean ifCanReadAndWriteSDCARD() {
		//TODO maybe move to own class and instantiate an object

		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
			mExternalStorageWriteable = true;
		    return true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
		    return false;
		} else {
		    //Something else is wrong. It may be one of many other states, but all we need
		    //to know is we can neither read nor write
		    return false;
		}
	}
}
