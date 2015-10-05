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

public class Logging {
	
	private final String classTAG = "Logging";
	
	private boolean verboseFlag = false;
	private boolean debugFlag = false;
	private boolean infoFlag = false;
	private boolean warningFlag = false;
	
	//Constructor
	public Logging(boolean verbose, boolean debug, boolean info, boolean warning) {
		Log.i(classTAG, "Constructor of Logging.java class was invoked."); //Unconditional
		verboseFlag = verbose;
		debugFlag = debug;
		infoFlag = info;
		warningFlag = warning;
	}
	
	//Unconditional
	protected void logCriticalError(String classTAG, String methodTAG, String message) {
		String s = classTAG + ":" + methodTAG + "::" + message;
		Log.e(classTAG, s);
		
		new CreateErrorLogThenDie(s); //Write to LOG file
		
	}
	
	//Unconditional
	protected void logE(String tag, String msg) {
		Log.e(tag, msg);
	}
	
	//Conditional
	protected void logV(String tag, String msg) {
		if (verboseFlag) {
			Log.v(tag, msg);
		}
	}
	
	//Conditional
	protected void logD(String tag, String msg) {
		if (debugFlag) {
			Log.d(tag, msg + " (Logging)");
		}
	}
	
	//Conditional
	protected void logI(String tag, String msg) {
		if (infoFlag) {
			Log.i(tag, msg);
		}
	}
	
	//Conditional
	protected void logW(String tag, String msg) {
		if (warningFlag) {
			Log.w(tag, msg);
		}
	}
}
