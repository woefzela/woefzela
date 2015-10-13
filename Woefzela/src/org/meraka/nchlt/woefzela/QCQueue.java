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

import java.util.ArrayList;

import android.util.Log;

public class QCQueue {
	
	//HOUSEKEEPING
	private static final String TAG = "QCQueue";
	
	//SWITCHES
	private static final boolean LOG_V = false;
	private static final boolean LOG_D = false;
	private static final boolean LOG_I = false;
	private static final boolean LOG_W = false;
	
	private static final int STARTING_ARRAY_SIZE = 100;
	private static final int QUEUE_NOT_INITIALIZED = -1;
	
	ArrayList<QCObject> qcQueue = null;
	
	protected int mQueueSize; //access from outside
	
	//Logging
	private Logging log = new Logging(LOG_V, LOG_D, LOG_I, LOG_W); //Note: No LOG_E!
	
	//Constructor
	public QCQueue () {
		mQueueSize = 0; //empty
		qcQueue = new ArrayList<QCObject>(STARTING_ARRAY_SIZE);
	}
	
	public void addItemToQueue(QCObject obj) {
		qcQueue.add(obj);
		mQueueSize++;
	}

	public QCObject getObject(int pos) {
		QCObject o = qcQueue.get(pos);
		log.logD(TAG,"getObject:qcQueue.get(" + pos + ") = " + o);
		return qcQueue.get(pos);
	}
	
	public QCObject removeObject(int pos) {
		mQueueSize--;
		log.logD(TAG,"removeObject pos = " + pos);
		QCObject o = qcQueue.get(pos);
		qcQueue.remove(pos);
		log.logD(TAG,"removeObject:qcQueue.get(" + pos + ") = " + o);
		return o;
	}
}