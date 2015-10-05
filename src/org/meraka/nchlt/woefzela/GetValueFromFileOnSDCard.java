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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import android.os.Environment;
import android.util.Log;

public class GetValueFromFileOnSDCard {
	
	//HOUSEKEEPING
	private static final String TAG = "GetValueFromFileOnSDCard";
	
	private static final int DEFAULT_QC_VALUE = 0; //See MyService.java
	private static final int BAD_POSITIVE_VALUE = 0;
	
	private File root = null;
	private String mFilenameFQ;

	/** Constructor */
	public GetValueFromFileOnSDCard(String filenameFQ) {
		mFilenameFQ = filenameFQ;
	}
	
	protected int getValue() {
		
		int value = 0;
		String lineReadFromFile = null;
		
		FileReader fRead = null;
		
		if (ifCanReadAndWriteSDCARD()) {
	
			root = Environment.getExternalStorageDirectory();
			Log.i(TAG,"root = " + root.toString());
			
		    File fid = new File(mFilenameFQ);
		    
		    if (root.canRead()) {
				try {
					fRead = new FileReader(fid);
					BufferedReader in = new BufferedReader(fRead);
					
					try {
						lineReadFromFile = in.readLine();
						in.close();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					
					try {
						value = Integer.parseInt(lineReadFromFile);
						Log.i(TAG,"Value read from file: " + mFilenameFQ + " was (as int) " + value);
						return value;
					} 
					catch (NumberFormatException e) {
						// TODO alert about file format error
						Log.w(TAG,"FATAL ERROR: Line read from file could not be parsed into an integer!");
						return BAD_POSITIVE_VALUE;
					}
				} 
				catch (FileNotFoundException e) { //No file
					return DEFAULT_QC_VALUE; //was BAD_POSITIVE_VALUE;
				}
		    }
		    else {
		    	//TODO say could no read and exit
		    	Log.e(TAG, "FATAL ERROR: Could not read file.");
		    }
		}
		else {
			Log.e(TAG,"SDCARD: Not able to either read and/or write from/to SDCARD.");
		}
		return BAD_POSITIVE_VALUE;
	}
	
	private boolean ifCanReadAndWriteSDCARD() {
		//TODO maybe move to own class and instantiate an object
		
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
		    return true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
		    return false;
		} else {
		    return false;
		}
	}
}
