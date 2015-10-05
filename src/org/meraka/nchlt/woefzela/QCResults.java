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

public class QCResults {
	
	private static final String TAG = "QCResults";
	
	//QC result set
	//Boolean
	private boolean audioIsClipped = false;
	private boolean audioVolumeTooLow = false;
	private boolean audioTruncatedAtStart = false;
	private boolean audioTruncatedAtEnd = false;
	private boolean audioUtteranceTooShort = false;
	private boolean audioUtteranceTooLong = false;
	//Extra
	private double audioUtteranceLength = 0.0;
	
	/**
	 * Constructor
	 */
	public QCResults() {
		Log.d(TAG,"Constructor called for QCResults.");
		//Boolean
		audioIsClipped = false;
		audioVolumeTooLow = false;
		audioTruncatedAtStart = false;
		audioTruncatedAtEnd = false;
		audioUtteranceTooShort = false;
		audioUtteranceTooLong = false;
		//Extra
		audioUtteranceLength = 0.0;
	}

	protected void storeResults(boolean clip, boolean tooSoft, boolean truncStart, boolean truncEnd, boolean tooShort, boolean tooLong, double length) {
		//Boolean
		audioIsClipped = clip;
		audioVolumeTooLow = tooSoft;
		audioTruncatedAtStart = truncStart;
		audioTruncatedAtEnd = truncEnd;
		audioUtteranceTooShort = tooShort;
		audioUtteranceTooLong = tooLong;
		//Extra
		audioUtteranceLength = length;
		Log.d(TAG,"storeResults: " + audioIsClipped + audioVolumeTooLow + audioTruncatedAtStart + audioTruncatedAtEnd + audioUtteranceTooShort + audioUtteranceTooLong);
	}
	
	protected boolean getQcResultIsClipped() {
		return audioIsClipped;
	}
	
	protected boolean getQcResultVolumeTooLow() {
		return audioVolumeTooLow;
	}
	
	protected boolean getQcResultTruncatedAtStart() {
		return audioTruncatedAtStart;
	}
	
	protected boolean getQcResultTruncatedAtEnd() {
		return audioTruncatedAtEnd;
	}
	
	protected boolean getQcResultUtteranceTooShort() {
		return audioUtteranceTooShort;
	}
	
	protected boolean getQcResultUtteranceTooLong() {
		return audioUtteranceTooLong;
	}
	
	protected double getQcResultUtteranceLength() {
		return audioUtteranceLength;
	}
}
