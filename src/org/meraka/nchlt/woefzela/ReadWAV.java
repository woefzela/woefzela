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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import android.os.Environment;
import android.util.Log;

public class ReadWAV {
	
	//HOUSEKEEPING
	private static final String TAG = "ReadWAV";
	
	private static final int MAX_AUDIO_LENGTH_IN_SECONDS = 30;
	private static final int BAD_AUDIO_FILE_SIZE = 0;
	
	private File sdCardRoot = null;
	private String fileWithPath = null;
	private RandomAccessFile fReader;
	private short value = 0;
	
	WAVWindow mem;
	
	private int expectedNumberOfFrames = -1;
	private int expectedDataSizeInBytes = -1;
	private int sampleRate = 0;
	
	//Constructor
	public ReadWAV(String audioFilenameFQ) {
	
		boolean OK = readFileIntoMemory(audioFilenameFQ);
		if (!OK) {
			Log.w(TAG, "File could not be completely read into memory!");
		}
	}

	public WAVWindow getPointer() {
		return mem;
	}
	
	public int getDataSize() {
		return expectedNumberOfFrames;
	}
	
	private boolean readFileIntoMemory(String fileWithPath) {
		
		String methodTAG = "readFileIntoMemory";
		
		byte b = 0;
		int i = 0;
		int ir = 0;
		int fileSize = 0;
		
		if (ifCanReadAndWriteSDCARD()) {
			
			File fid = new File(fileWithPath);

			try {
				fReader = new RandomAccessFile(fid, "r");
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

	    		/*Notes:
	    		 * byte = 8
	    		 * short =  16-bit
	    		 * int = 32
	    		 */
				try {
					fReader.seek(4);
					i = fReader.readInt();
					ir = Integer.reverseBytes(i);
					fileSize = ir + 8;

					fReader.seek(24);
					i = fReader.readInt();
					sampleRate = Integer.reverseBytes(i);
					
					fReader.seek(40);
					i = fReader.readInt();
					ir = Integer.reverseBytes(i);
					expectedDataSizeInBytes = ir;
					expectedNumberOfFrames = ir/2; //short is 2 bytes
					Log.d(TAG,"Data size inside file (frames) = " + expectedNumberOfFrames); //short is 2 bytes
					if (expectedDataSizeInBytes > MAX_AUDIO_LENGTH_IN_SECONDS*32000) {//TODO tidy
						Log.i(TAG,"Audio file was too long to read into memory. Skipped file for QC.");
						fReader.close();
						expectedNumberOfFrames = BAD_AUDIO_FILE_SIZE; //Catch later
						return false;
					}
					
					fReader.seek(44);
					mem = new WAVWindow(expectedNumberOfFrames*2);
					for (int frameCounter = 0; frameCounter < expectedNumberOfFrames; frameCounter++) {
						value = Short.reverseBytes(fReader.readShort());
						mem.insertIntoBuffer(frameCounter, value);
					}						
					fReader.close();
					return true;
				} 
				catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return false;
				}
		}
		else {
			LOG_E(TAG, methodTAG, "SDCARD: Not able to either read and/or write from/to SDCARD.");
			return false;
		}
	}
	
	private boolean ifCanReadAndWriteSDCARD() {
		
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
		    return true;
		} 
		else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
		    return false;
		} 
		else {
		    return false;
		}
	}
	private void LOG_E(String classTAG, String methodTAG, String message) {
		String s = classTAG + ":" + methodTAG + "::" + message;
		Log.e(classTAG, s);
		
		//Write to LOG file
		new CreateErrorLogThenDie(s);
	}
}