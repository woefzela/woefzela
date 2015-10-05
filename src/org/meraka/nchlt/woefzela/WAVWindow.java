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

public class WAVWindow {
	
	private static final String TAG = "WAVWindow";
	
	private short[] wavBuffer;
	
	public WAVWindow(long windowSizeInit) {
		wavBuffer = new short[(int) windowSizeInit];
	}
	
	public short getValueAtPos(int pos) {
		short value;
		
		if (pos < (wavBuffer.length)) {
		value = wavBuffer[pos];
		return value;
		}
		Log.d(TAG,"Overrun buffer @ pos " + pos);
		return 0;
	}

	public void insertIntoBuffer(int pos, short value) {
		wavBuffer[pos] = value;
	}
}