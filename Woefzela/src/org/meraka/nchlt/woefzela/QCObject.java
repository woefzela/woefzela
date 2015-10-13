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

import android.util.Log;

public class QCObject {
	
	private final String TAG = "QCObject";
	
	String mAudiofileWithPath = null;
	String mBaseFilename = null;
	String mPromptString = null;
	String mWordCategory = null;
	double mExpectedUtteranceDuration = 0.0;

	public QCObject(String audiofileWithPath, String baseFilename, String promptString, String wordCategory, double expectedUtteranceDuration) {
		mAudiofileWithPath = audiofileWithPath;
		mBaseFilename = baseFilename;
		mPromptString = promptString;
		mWordCategory = wordCategory;
		mExpectedUtteranceDuration = expectedUtteranceDuration;
		Log.d(TAG,"A QCObject was created.");
	}
	
	protected String getAudiofileWithPath() {
		return mAudiofileWithPath;
	}
	
	protected String getBaseFilename() {
		return mBaseFilename;
	}
	
	protected String getPromptString() {
		return mPromptString;
	}
	
	protected String getWordCategory() {
		return mWordCategory;
	}
	
	protected double getExpectedUtteranceDuration() {
		return mExpectedUtteranceDuration;
	}
}
