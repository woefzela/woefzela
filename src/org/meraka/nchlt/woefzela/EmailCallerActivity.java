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

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class EmailCallerActivity extends Activity {
	
	//Housekeeping
	private final String TAG = "EmailCallerActivity";
	private static boolean sendWithEmail = true;

	private boolean emailHasBeenSent = false;

    /** Called when the activity is first created. */
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.blank);
        
        if (!emailHasBeenSent) {
        	Log.d(TAG,"Sending an email with the profile...");
        }
    }
}