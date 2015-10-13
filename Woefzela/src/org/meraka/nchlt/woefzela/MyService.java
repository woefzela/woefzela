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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

public class MyService extends Service {
	
	//HOUSEKEEPING
	private static final String TAG = "MyService";
	
	//SWITCHES
	private static final boolean DO_NOT_CONTINUE_WITH_QC_AFTER_RECORDING_SESSION = true;
	
	private static final boolean LOG_V = true;
	private static final boolean LOG_D = true;
	private static final boolean LOG_I = true;
	private static final boolean LOG_W = true;
	
	//SWITCHES and directly related
	private static final boolean TESTING_MODE = false;
	//Used in TESTING_MODE:
	private static final String QC_TEST_FILENAME = "/sdcard/woefzela/outputdata/359444020078736_S20100921161953/af_ZA_8DA5C670747B87800965FAC7EF8A9292_20100921161953_training_00003.wav"; 
	
	private static final boolean DISPLAY_RMS_VALUES = false;
	private static final boolean READY_FOR_ANOTHER_SET_OF_FILES = true;
	private static final int SLEEP_TIME_IN_SECONDS = 5;
	private static String progressString = "";
	private static String qualityString = "";
	private static final String NEWLINE = "\n";
	private static final int ADVANCED_QC_OFF_VALUE = 1;
	
	//QUALITY CHECK PARAMETERS AND VARIABLES
	//PRESET PARAMETERS
	private static final double WINDOW_SIZE_IN_SECONDS = 0.05;
	private static final int WINDOW_SIZE_TO_SHIFT_SIZE_RATIO = 5; //i.e. 1/value = ratio
	private static final double WINDOW_ADVANCE_STEPSIZE_IN_SECONDS = WINDOW_SIZE_IN_SECONDS/WINDOW_SIZE_TO_SHIFT_SIZE_RATIO; //s
	private static final double END_OF_FIRST_SET_OF_WINDOWS_IN_SECONDS = 0.03;
	private static final double END_OF_LAST_SET_OF_WINDOWS_IN_SECONDS = 0.03;
	
	private static final int MAX_PEAK_VALUE = Short.MAX_VALUE-1;
	private static final int VOLUME_SUFFICIENT_RMS_LIMIT = 1800; //Last: 1600. HTC Magic/Dream = 600; HTC Wildfire = 1200 (some wildfires 1800 to match rms limit)
	private static final int TRUNCATION_RMS_LIMIT = 2000; //Last: 2000. HTC Magic/Dream = 300; HTC Wildfire = 600 (1100x2, 1300x1); some 1800
	private static final int SILENCE_THRESHOLD = 100; //Currently only used for utterance length calculation
	//PARAMETERS FROM WAVE FILE. TODO get from file dynamically
	private static final int FRAME_RATE = 16000; //Hz i.e. frames (info) per second
	private static final int FRAME_SIZE_IN_BYTES = 2; //i.e. number of bytes per sample
	//INTERNAL CONSTANTS
	private static final int BAD_AUDIO_FILE_SIZE = 0;
	private static final int BYTES_PER_SECOND = FRAME_RATE*FRAME_SIZE_IN_BYTES;
	private static final int FRAMES_PER_SECOND = FRAME_RATE;
	private static final int WINDOW_SIZE_IN_FRAMES = (int) (WINDOW_SIZE_IN_SECONDS*FRAMES_PER_SECOND);
	private static final int WINDOW_ADVANCE_STEPSIZE_IN_FRAMES = WINDOW_SIZE_IN_FRAMES/WINDOW_SIZE_TO_SHIFT_SIZE_RATIO;
	private static final int END_OF_FIRST_SET_OF_WINDOWS_IN_FRAMES = (int) (END_OF_FIRST_SET_OF_WINDOWS_IN_SECONDS*FRAMES_PER_SECOND);
	private static final int END_OF_LAST_SET_OF_WINDOWS_IN_FRAMES = (int) (END_OF_LAST_SET_OF_WINDOWS_IN_SECONDS*FRAMES_PER_SECOND);
	private short peakValue = 0;
	
	private final IBinder binder = new MyBinder(); //Here? Better place?
	//private IBinder binder;
	protected boolean serviceRunning = false;
	private int numberOfWindowShifts = 0;
    private boolean fatalQCFailurePending = false;
    
    QCQueue qcQueue = null;
    QCTask asyncQCTask = null;
    QCObject qcObject = null;
    
    protected boolean waitForEachQCResult = true; //manipulate from main activity
    
    private int numberOfFilesSubmitted = 0;
    protected int numberOfFilesPassed = 0;
    protected int numberOfFilesQCDone = 0;
    
    Context context =  null;
    ProgressDialog mProgressDialog = null;
    private boolean finalWaitingForQCsToBeDone = false;
    TextView tProgressStatusBar = null;
    TextView tQCStatusBar = null;
    
    private Context ctx = null;
    private TextView tStatusBarQC = null;
    
    protected int numberOfObjectsReceivedForQC = 0;
    protected int numberOfObjectsDoneWithQC = 0;
    private int numberOfPromptsSkipped = 0;
    private int numberOfPromptsTarget = 0;
    protected int numberOfObjectsFailingQC = 0;
    protected int numberOfObjectsPassingQC = 0;
    private int qcResultVolumeTooLow = 0;
    private int qcResultStartTooEarlyOrTooLate = 0;
    private int qcResultUtteranceTooShortOrTooLong = 0;
    private QCResults qcResultSet = null; //Hide all results for one file in a single object for easy passing.
    protected boolean continueWithCalibrationCalculations = false;
    protected boolean recordingSessionType = false;
    
    private static boolean ADVANCED_QC;
    private static String advancedQCSwitchFilenameFQ = null;
    private static int advancedQCSwitchValue = ADVANCED_QC_OFF_VALUE;
    
	//Loaded from resources
	private String programFoldername = null;
	private String trackingFolderName = null;
	private String advancedQCSwitchFilename = null;
	
	//Logging
	private Logging log = new Logging(LOG_V, LOG_D, LOG_I, LOG_W); //Note: No LOG_E!
	
	/*A separate class*/
	public class MyBinder extends Binder {
		MyService getService() {
			return MyService.this;
		}
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		log.logD(TAG, "MyService.onBind has fired...");
		return binder; //will now call onCreate automatically
	}
	
	@Override
	public void onCreate() { //called by onBind
		
		log.logD(TAG,"MyService.onCreate has fired...");

		Context context = getApplicationContext();
		CharSequence text = "QC service started";
		int duration = Toast.LENGTH_SHORT;
		Toast toast = Toast.makeText(context, text, duration);
		toast.setGravity(Gravity.TOP, 0, 0);
		toast.show();
		
		qcQueue = new QCQueue();
		
		serviceRunning = true; //serviceRunning = true;
		numberOfFilesSubmitted = 0;
		numberOfFilesPassed = 0;
		numberOfObjectsFailingQC = 0;
		numberOfObjectsPassingQC = 0;
	    numberOfFilesQCDone = 0;
	    waitForEachQCResult = true;
	    finalWaitingForQCsToBeDone = false;
	    continueWithCalibrationCalculations = false;
	    numberOfWindowShifts = 0;
	    
		//Statusbars
		numberOfObjectsReceivedForQC = 0;
		numberOfObjectsDoneWithQC = 0;
		numberOfPromptsSkipped = 0;
		numberOfPromptsTarget = 0;
		
		qcResultVolumeTooLow = 0;
	    qcResultStartTooEarlyOrTooLate = 0;
	    qcResultUtteranceTooShortOrTooLong = 0;
	    
	    recordingSessionType = false;
	    
	    qcResultSet = new QCResults();
	    
	    //Default
	    ADVANCED_QC = false;
	    advancedQCSwitchValue = ADVANCED_QC_OFF_VALUE;
	    
    	Resources res = getResources(); //Get instance of a resource and not just the resource ID (e.g. R.id.xxxx)
    	programFoldername = res.getString(R.string.PROGRAM_FOLDER_NAME);
    	trackingFolderName = res.getString(R.string.TRACKING_FOLDER_NAME);
    	advancedQCSwitchFilename = res.getString(R.string.EXTERNAL_ADVANCED_QC_SWITCH_FILENAME);
	    
		//Attemp to get switch from an external file: No file or value of 0 means OFF, rest is ON
		advancedQCSwitchFilenameFQ = "/sdcard" + programFoldername + trackingFolderName + "/" + advancedQCSwitchFilename;
		log.logI(TAG,"Looking for external file '" + advancedQCSwitchFilenameFQ + "' for ADVANCED_QC switch value.");
		GetValueFromFileOnSDCard v = new GetValueFromFileOnSDCard(advancedQCSwitchFilenameFQ);
		advancedQCSwitchValue = v.getValue();

		//Logic: See manifest file
		if (advancedQCSwitchValue != ADVANCED_QC_OFF_VALUE) {
			ADVANCED_QC = true;
			log.logD(TAG,"ADVANCED_QC switch in file '" + advancedQCSwitchFilenameFQ + "' is ON (" + advancedQCSwitchValue + ") or no file existed.");
		}
		else {
			ADVANCED_QC = false;
			log.logD(TAG,"ADVANCED_QC switch in file '" + advancedQCSwitchFilenameFQ + "' is OFF (" + advancedQCSwitchValue + ")");
		}
	}
	
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		
		serviceRunning = false;
		
		Context context = getApplicationContext();
		CharSequence text = "QC service stopped";
		int duration = Toast.LENGTH_SHORT;
		Toast toast = Toast.makeText(context, text, duration);
		toast.setGravity(Gravity.TOP, 0, 0);
		toast.show();
		log.logD(TAG,"MyService.onDestroy has fired...");
	}


	
	public void addObjectToQCQueue(String audiofileWithPath, String baseFilename, String promptString, String wordCategory, double expectedUtteranceDuration) {
		
		if (TESTING_MODE) {
			qcObject = new QCObject(QC_TEST_FILENAME, baseFilename, promptString, wordCategory, expectedUtteranceDuration); //Create for each object
			log.logD(TAG,"TESTING_MODE active. QC_TEST_FILENAME = '" + QC_TEST_FILENAME + "'");
		}
		else {
			qcObject = new QCObject(audiofileWithPath, baseFilename, promptString, wordCategory, expectedUtteranceDuration); //Create for each object
		}
		
		qcQueue.addItemToQueue(qcObject);
		numberOfObjectsReceivedForQC++;
		log.logD(TAG, "Just added another object to QC queue. Total number received for QC = " + numberOfObjectsReceivedForQC);
		log.logD(TAG, "Current qcQueue in addItemToQueue is length: " + numberOfFilesSubmitted);
	    for (int i = 0; i < qcQueue.mQueueSize; i++) {
	    	log.logD(TAG, "mQueueSize = " + qcQueue.mQueueSize);
	    	QCObject o = (QCObject) qcQueue.getObject(i);
	        log.logD(TAG, "qcQueue[" + i + "] = " + o.getAudiofileWithPath());
	    }
	}
	
	public void increaseNumberOfSkippedPrompts() {
		log.logD(TAG,"Increased number of skipped prompts by one.");
		numberOfPromptsSkipped++;
	}
	
	public void sendNumberOfPromptsTargetToBeDisplayed(int current) {
		numberOfPromptsTarget = current;
	}
	
	protected void sendProgressDialogReference(ProgressDialog dlg) {
		mProgressDialog =  dlg;
	}
	
	protected void setFinalWaitingForQCsToBeDone() {
		finalWaitingForQCsToBeDone = true;
		log.logD(TAG,"finalWaitingForQCsToBeDone was set to true by method setFinalWaitingForQCsToBeDone.");
	}
	
	protected void setProgressStatusBarObjectReference(TextView tv) {
		tProgressStatusBar =  tv;
		log.logD(TAG, "setProgressStatusBarObjectReference was set in service.");
		progressString = "    PROGRESS: Done: 0000       Skipped: 0000      Target: 0000";
		tProgressStatusBar.setText(progressString);
	}
	
	protected void setQCStatusBarObjectReference(TextView tv) {
		tQCStatusBar =  tv;
		log.logD(TAG, "setQCStatusBarObjectReference was set in service.");
		qualityString = "    QUALITY:  Volume: 0000    Start/Stop: 0000   Length: 0000";
		tQCStatusBar.setText(qualityString);
	}
	
/*	
    Format: AsyncTask<Params, Progress, Result>
	Generalised example:
	private class QCTask extends AsyncTask<A, B, C> {
	protected void onPreExecute() { //TODO }
	protected C doInBackground(A... aaa) { //TODO publishProgress(B); return C; }
	protected void onProgressUpdate(B... bbb) { //TODO }
	protected void onPostExecute(C ccc) { //TODO }
	Note: 
		doInBackground must return a single, once-off result to onPostExectute when it is done
		If still struggle, check what type the call to publishProgress (in doInBackground) is passing to onProgressUpdate
		And check that doInBackground is returning the type it promised in it's declaration.
*/	
	private class QCTask extends AsyncTask<QCQueue, QCResults, Boolean> {
		
		//Runs on GUI thread
		//Scope here is that of the service
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			log.logV(TAG,"onPreExecute...");
			
			log.logD(TAG, "Current qcQueue in onPreExecute...");
		    for (int i = 0; i < qcQueue.mQueueSize; i++) {
		    	log.logV(TAG, "mQueueSize = " + qcQueue.mQueueSize);
		    	QCObject o = (QCObject) qcQueue.getObject(i);
		        log.logV(TAG, "qcQueue[" + i + "] = " + o.getAudiofileWithPath());
		    }
		}
		
		//Runs in own thread...stay away from interaction with GUI thread
		//Scope here is that of the service
		@Override
		protected Boolean doInBackground(QCQueue... queue) {
			
			//Local variables for this thread. Report via publishProgress(.)
			int numberOfFiles = queue[0].mQueueSize;
			QCObject obj = null;
			ReadWAV loadFile = null;
			WAVWindow mem = null;
		    String audioFilenameFQ = null;
		    int dataSizeInFrames = 0;
	        int windowStartPos = 0;
	        double speechDurationAccumulator = 0.0;
	        double maxRMSValue = 0;
	        double windowRMSValue = 0;
	        
			//QC result set
			//Boolean
			boolean audioIsClipped = false;
			boolean audioVolumeTooLow = true; //guilty until proven innocent
			boolean audioTruncatedAtStart = false;
			boolean audioTruncatedAtEnd = false;
			boolean audioUtteranceTooShort = false;
			boolean audioUtteranceTooLong = false;
			//Extra
			double audioUtteranceLength = 0.0;
			
			int c = 0;
			//While more files left in this queue
			while ( (fatalQCFailurePending ==  false) & (c < numberOfFiles) ) {
				
				//get next file
		    	obj = (QCObject) queue[0].getObject(0);
				audioFilenameFQ = obj.getAudiofileWithPath();
				log.logD(TAG,"Starting QC for file[" + (c+1) + "]: " + audioFilenameFQ);
				
				//analyze file
				/** Actual QC checks */
				if (ADVANCED_QC) {
					loadFile = new ReadWAV(audioFilenameFQ);
					mem = loadFile.getPointer();
					
			        log.logV(TAG,"Inspecting constants...");
			        log.logV(TAG,"WINDOW_SIZE_IN_SECONDS = " + WINDOW_SIZE_IN_SECONDS);
			        log.logV(TAG,"WINDOW_SIZE_IN_FRAMES = " + WINDOW_SIZE_IN_FRAMES);
			        log.logV(TAG,"WINDOW_ADVANCE_STEPSIZE_IN_SECONDS = " + WINDOW_ADVANCE_STEPSIZE_IN_SECONDS);
			        log.logV(TAG,"WINDOW_ADVANCE_STEPSIZE_IN_FRAMES = " + WINDOW_ADVANCE_STEPSIZE_IN_FRAMES);
			        log.logV(TAG,"END_OF_FIRST_SET_OF_WINDOWS_IN_SECONDS = " + END_OF_FIRST_SET_OF_WINDOWS_IN_SECONDS);
			        log.logV(TAG,"END_OF_FIRST_SET_OF_WINDOWS_IN_FRAMES = " + END_OF_FIRST_SET_OF_WINDOWS_IN_FRAMES);
			        
			        dataSizeInFrames = loadFile.getDataSize();
			        log.logV(TAG,"dataSizeInFrames = " + dataSizeInFrames);
			        if (dataSizeInFrames == BAD_AUDIO_FILE_SIZE) {
			        	log.logI(TAG,"dataSizeInFrames indicates BAD_AUDIO_FILE_SIZE");
			        }
				}
		        
		        //Reset for each file
		        log.logD(TAG, "Resetting flags for this file...");
				//Boolean
				audioIsClipped = false;
				audioVolumeTooLow = true; //guilty until proven innocent
				audioTruncatedAtStart = false;
				audioTruncatedAtEnd = false;
				audioUtteranceTooShort = false;
				audioUtteranceTooLong = false;
				audioUtteranceLength = 0.0;
		        speechDurationAccumulator = 0.0;
		        maxRMSValue = 0;
		        windowStartPos = 0;
		        peakValue = 0;
		        numberOfWindowShifts = 0;
		        
			    if (ADVANCED_QC) {
			    	
			        //While more windows left in this audio file
			        while ( (windowStartPos + WINDOW_SIZE_IN_FRAMES) < dataSizeInFrames ) { //indexing: short
	
			        	log.logV(TAG,"numberOfWindowShifts = " + numberOfWindowShifts);
			        	log.logV(TAG,"windowStartPos = " + windowStartPos);
			        	
			        	//Calculate RMS for this window
			        	windowRMSValue = calcRMS(mem, windowStartPos);
				        if (DISPLAY_RMS_VALUES) {
				        	log.logV(TAG,"windowStartPos: " + windowStartPos+ "; windowRMSValue = " + windowRMSValue);
				        }
			        	
			        	//Clipping detection
				        //TODO...do in Recorder?
			        	if (peakValue > MAX_PEAK_VALUE) {
			        		audioIsClipped = true;
			        	}
			        	
			        	//Volume sufficient
			        	if (windowRMSValue > VOLUME_SUFFICIENT_RMS_LIMIT) {
			        		audioVolumeTooLow = false; //proven innocent
			        	}
			        	
			        	//Truncating of speech at start
			        	if (windowStartPos < (END_OF_FIRST_SET_OF_WINDOWS_IN_FRAMES)) { //first x seconds of audio
			        		if (windowRMSValue > TRUNCATION_RMS_LIMIT) { 
			        			audioTruncatedAtStart = true;
			        			log.logV(TAG,"Truncation at start with rms = " + windowRMSValue);
			        		}
			        	}
			        	
			        	//Truncating of speech at end
			        	int windowEndPos = windowStartPos + WINDOW_SIZE_IN_FRAMES;
			        	int startEndCheckingPos = dataSizeInFrames-END_OF_LAST_SET_OF_WINDOWS_IN_FRAMES;
			        	if (windowEndPos >= startEndCheckingPos) {
			        		if (windowRMSValue > TRUNCATION_RMS_LIMIT) {
			        			audioTruncatedAtEnd = true;
			        			log.logV(TAG,"Truncation at end with rms = " + windowRMSValue);
			        		}
			        	}
	
			        	//Accumulators (across windows)
			        	if (windowRMSValue > maxRMSValue) { //keep max rms of all windows
			        		maxRMSValue = windowRMSValue;
			        	}
			        	
			        	if (windowRMSValue > SILENCE_THRESHOLD) { //i.e. a 'speech window' 
			        		speechDurationAccumulator = speechDurationAccumulator + ((float) WINDOW_ADVANCE_STEPSIZE_IN_FRAMES/(float) FRAMES_PER_SECOND);
			        	}	
				        windowStartPos = windowStartPos + WINDOW_ADVANCE_STEPSIZE_IN_FRAMES;
				        
				        numberOfWindowShifts++;
				        
			        }//while
			    }//if advanced qc
			    else { //not advanced qc
			    	audioVolumeTooLow = false; //fake it as OK
			    }
		        log.logV(TAG,"QC result: numberOfWindowShifts = " + numberOfWindowShifts);
		        
		        //Check utterance length
		        audioUtteranceLength = speechDurationAccumulator;
		        audioUtteranceTooShort = false;
				audioUtteranceTooLong = false;

		        //Display QC result set
		        log.logI(TAG,"QC result: audioIsClipped = " + audioIsClipped);
		        log.logI(TAG,"QC result: audioVolumeTooLow = " + audioVolumeTooLow);
		        log.logI(TAG,"QC result: audioTruncatedAtStart = " + audioTruncatedAtStart);
		        log.logI(TAG,"QC result: audioTruncatedAtEnd = " + audioTruncatedAtEnd);
		        log.logI(TAG,"QC result: audioUtteranceTooShort = " + audioUtteranceTooShort);
		        log.logI(TAG,"QC result: audioUtteranceTooLong = " + audioUtteranceTooLong);
		        log.logI(TAG,"QC result: (info) audioUtteranceLength = " + audioUtteranceLength);

				//Give feedback about this file
		        qcResultSet.storeResults(audioIsClipped, audioVolumeTooLow, audioTruncatedAtStart, audioTruncatedAtEnd, audioUtteranceTooShort, audioUtteranceTooLong, audioUtteranceLength);
			    publishProgress(qcResultSet);
					    
				//write results to xml file
				String f = obj.getBaseFilename();
				writeStatsToXMLFile(obj, qcResultSet);
				
				numberOfObjectsDoneWithQC++;
				log.logD(TAG,"Completed QC on file: " + audioFilenameFQ);
				log.logD(TAG,"numberOfObjectsDoneWithQC = " + numberOfObjectsDoneWithQC);
				
				//Remove from queue
				queue[0].removeObject(0);
				log.logD(TAG,"Removed: " + audioFilenameFQ);
				
				//LEARN: Log.d(TAG,"Removed: " + queue[0].removeObject(0)); DO NOOOOOT DO THIS EVER AGAIN!! (Processing inside an output statement)
			
				log.logI(TAG,"Finished QC for another file. " + (numberOfFiles-c) + " more to go in current queue.");
				
				c++;

			}//while not done with current queue
			log.logD(TAG,"Done with current QC queue.");
			return READY_FOR_ANOTHER_SET_OF_FILES; //Value to be passed to onPostExecute
		}
		
		/** NB: PART OF doInBackground thread. Do not access other threads!*/
	    private double calcRMS(WAVWindow mem, int bufferStart) {
	    	double sum = 0;
	    	short value = 0;
	    	
	    	for (int i = bufferStart; i < (bufferStart + WINDOW_SIZE_IN_FRAMES); i++) {
	    		value = mem.getValueAtPos(i); //short
	    		sum = sum + Math.pow(value,2);
	    		if (value > peakValue) {
	    			peakValue = value;
	    		}
	    	}
	    	sum = sum / WINDOW_SIZE_IN_FRAMES;

	    	return Math.sqrt(sum);
	    }
	
		//Runs on GUI thread
		//Scope here is that of the service
		@Override
		protected void onProgressUpdate(QCResults... resultSet) { //was Boolean
			super.onProgressUpdate(resultSet); //TODO why?
			
			Context context = getApplicationContext();
			if (finalWaitingForQCsToBeDone) {
				int numberToGo = (numberOfObjectsReceivedForQC - numberOfObjectsDoneWithQC);
				if (numberToGo > 1) {
					log.logI(TAG,"Finished QC for another file.\nNow " + numberToGo + " more files to QC. (Displaying Toast)");
					Toast.makeText(context, "Finished QC for another file.\nNow " + numberToGo + " more files to QC.", Toast.LENGTH_SHORT).show();
				}
			}
			
			boolean audioVolumeTooLow = false;
			boolean audioTruncatedAtStart = false;
			boolean audioTruncatedAtEnd = false;
			boolean audioUtteranceTooShort = false;
			boolean audioUtteranceTooLong = false;

			//Learn: Can Toast with very little context.
			//Context context = getApplicationContext();
			//Toast.makeText(context, "I am in onProgressUpdate", Toast.LENGTH_SHORT).show();
			
			audioVolumeTooLow = resultSet[0].getQcResultVolumeTooLow();
			audioTruncatedAtStart = resultSet[0].getQcResultTruncatedAtStart();
			audioTruncatedAtEnd = resultSet[0].getQcResultTruncatedAtEnd();
			audioUtteranceTooShort = resultSet[0].getQcResultUtteranceTooShort();
			audioUtteranceTooLong = resultSet[0].getQcResultUtteranceTooLong();
			
			if (false == audioVolumeTooLow == audioTruncatedAtStart == audioTruncatedAtEnd == audioUtteranceTooShort == audioUtteranceTooLong) {
				numberOfObjectsPassingQC++;
				log.logD(TAG,"numberOfObjectsPassingQC = " + numberOfObjectsPassingQC);
			}
			else {
				numberOfObjectsFailingQC++;
			    //Increment specific failure reason counters
			    if (audioVolumeTooLow) {
			    	qcResultVolumeTooLow++;
			    }
			    if (audioTruncatedAtStart | audioTruncatedAtEnd) {
			    	qcResultStartTooEarlyOrTooLate++;
			    }
			    if (audioUtteranceTooShort | audioUtteranceTooLong) {
			    	qcResultUtteranceTooShortOrTooLong++;
			    }
			}
		    updateStatusBars();
		}
	
		//Runs on GUI thread
		//Scope here is that of the service
	    @Override
	    protected void onPostExecute(Boolean result) { 
	    	super.onPostExecute(result);
	    	if (result == READY_FOR_ANOTHER_SET_OF_FILES) {
	    		log.logD(TAG,"I am ready for a next set of files to QC");
	    		if (finalWaitingForQCsToBeDone) { //all systems go
	    			log.logD(TAG,"onPostExecute.finalWaitingForQCsToBeDone was true");
	    			startAsyncQCTask();
	    			log.logD(TAG,"numberOfObjectsDoneWithQC = " + numberOfObjectsDoneWithQC + "; numberOfObjectsReceivedForQC = " + numberOfObjectsReceivedForQC);

	    			if (numberOfObjectsDoneWithQC >= numberOfObjectsReceivedForQC) {
		    			updateStatusBars();
		    			mProgressDialog.dismiss();
		    			continueWithCalibrationCalculations = true;
		    			stopSelf();
		    		}
		    		else {
		    		}
	    			
	    			if (recordingSessionType & DO_NOT_CONTINUE_WITH_QC_AFTER_RECORDING_SESSION) {
		    			updateStatusBars();
		    			mProgressDialog.dismiss();
		    			continueWithCalibrationCalculations = true;
		    			boolean cancelled = asyncQCTask.cancel(true);
		    			log.logD(TAG,"asyncQCTask.cancel(true) returned: " + cancelled);
		    			log.logD(TAG,"asyncQCTask.isCancelled() returns: " + asyncQCTask.isCancelled());
		    			stopSelf();
	    			}
	    		}
	    		//else...do another batch/queue if there is one
	    		startAsyncQCTask();
	    	}
	    	else {
	    		log.logD(TAG,"I am NOT ready for a next set of files to QC");
	    	}
	    }
	    
		//Runs on GUI thread
		//Scope here is that of the service
	    @Override
	    protected void onCancelled() { 
	    	super.onCancelled();
	    	log.logD(TAG,"onCancelled was called.");
	    	
			Context context = getApplicationContext();
			CharSequence text = "QC service cancelled";
			int duration = Toast.LENGTH_SHORT;
			Toast toast = Toast.makeText(context, text, duration);
			toast.setGravity(Gravity.TOP, 0, 0);
			toast.show();
			
	    }
	 }//AsyncTask

	protected void startAsyncQCTask() {
		log.logD(TAG,"Trying to start asyncQCTask...");
		log.logD(TAG,"asyncQCTask current q length = " + qcQueue.mQueueSize);
		if (qcQueue.mQueueSize > 0) {
			//Create and start only if brand new or if previous one finished it's job(s)
			if (asyncQCTask == null ||
				asyncQCTask.getStatus().equals(AsyncTask.Status.FINISHED)) { //not busy
					asyncQCTask =  new QCTask();
					asyncQCTask.execute(qcQueue);
					log.logD(TAG,"starting asyncQCTask with q length of " + qcQueue.mQueueSize);
			}
			else {
				log.logD(TAG,"startAsyncQCTask did not start...");
				if (asyncQCTask != null) {
					log.logD(TAG,"startAsyncQCTask did not start as asyncQCTask was NOT null.");
				}
				if (!asyncQCTask.getStatus().equals(AsyncTask.Status.FINISHED)) {
					log.logD(TAG,"startAsyncQCTask did not start as asyncQCTask was not FINISHED.");
				}
			}
		}
		else {
			log.logD(TAG,"asyncQCTask did not start as queue was empty.");
		}
	}
	
	protected void updateStatusBars() {
		
		//Progress feedback
		String formattedNumberOfObjectsPassingQC =  String.format("%04d", numberOfObjectsPassingQC);
		String formattedNumberOfPromptsSkipped =  String.format("%04d", numberOfPromptsSkipped);
		String formattedNumberOfPromptsTarget =  String.format("%04d", numberOfPromptsTarget);
		String s1 = "    PROGRESS: Done: " + formattedNumberOfObjectsPassingQC + "      Skipped: " + formattedNumberOfPromptsSkipped + "     Target: " + formattedNumberOfPromptsTarget;
		tProgressStatusBar.setText(s1);
		log.logD(TAG,s1);
		
		//Quality feedback
		String formattedVolumeError =  String.format("%04d", qcResultVolumeTooLow);
		String formattedTruncationError =  String.format("%04d", qcResultStartTooEarlyOrTooLate);
		String formattedUtteranceLengthError =  String.format("%04d", qcResultUtteranceTooShortOrTooLong);
		String s2 = "    QUALITY:  Volume: " + formattedVolumeError + "   Start/Stop: " + formattedTruncationError + "   Length: " + formattedUtteranceLengthError;
		tQCStatusBar.setText(s2);
		log.logD(TAG,s2);
	}
	
    private void writeStatsToXMLFile(QCObject o, QCResults qcResultSet) {
    	
    	String methodTAG = "writeStatsToXMLFile";
    	
    	boolean mExternalStorageWriteable = false;
    	
		//Check if SDcard is ready to write to
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
		    // We can read and write the media
		    log.logV(TAG,"SDCARD: Yay, we can read and write to it!");
		    mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
		    // We can only read the media
		    log.logI(TAG,"SDCARD: Nope, we can only read it.");
		} else {
		    // Something else is wrong. It may be one of many other states, but all we need
		    //  to know is we can neither read nor write
		    log.logI(TAG,"SDCARD: Hmmm...we can neither read nor write to it!");
		}

		if (mExternalStorageWriteable != false) {
			String f = o.getBaseFilename();
			File root = Environment.getExternalStorageDirectory();
			if (root.canWrite()){
		    	File fid = new File(f + "_stats.xml");
		    	
		        FileWriter fWrite;
				try {
					fWrite = new FileWriter(fid);
					BufferedWriter out = new BufferedWriter(fWrite);
		        	//Write header section...TODO from resource
					out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
				
		        	//Write data
			        out.write("<Woefzela>\n");
			        out.write("\t<statistics>\n");
			        out.write("\t\t<prompt>" + o.getPromptString() + "</prompt>\n");
			        out.write("\t\t<audioIsClipped>" + qcResultSet.getQcResultIsClipped() + "</audioIsClipped>\n");
			        out.write("\t\t<audioVolumeTooLow>" + qcResultSet.getQcResultVolumeTooLow() + "</audioVolumeTooLow>\n");
			        out.write("\t\t<audioTruncatedAtStart>" + qcResultSet.getQcResultTruncatedAtStart() + "</audioTruncatedAtStart>\n");
			        out.write("\t\t<audioTruncatedAtEnd>" + qcResultSet.getQcResultTruncatedAtEnd() + "</audioTruncatedAtEnd>\n");
			        out.write("\t\t<audioUtteranceTooShort>" + qcResultSet.getQcResultUtteranceTooShort() + "</audioUtteranceTooShort>\n");
			        out.write("\t\t<audioUtteranceTooLong>" + qcResultSet.getQcResultUtteranceTooLong() + "</audioUtteranceTooLong>\n");
			        out.write("\t\t<audioUtteranceLength>" + qcResultSet.getQcResultUtteranceLength() + "</audioUtteranceLength>\n");
			        out.write("\t</statistics>\n");
			        out.write("</Woefzela>\n");
			        out.close();
				} 
				catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else {
			    	log.logE(methodTAG, "root.canWrite is false.");
			}
		}
		else {
			log.logE(methodTAG, "Sorry, but the SDcard is not ready/writable.");
		}
    }
}