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
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import android.media.AudioFormat;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaRecorder.AudioSource;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainRecordingActivity extends Activity {
	
	//HOUSEKEEPING
	private final String TAG = "MainRecordingActivity";
	
	//SWITCHES
	private static final boolean LOG_V = true;
	private static final boolean LOG_D = true;
	private static final boolean LOG_I = true;
	private static final boolean LOG_W = true;
	
	//Constants
	private static final int BAD_POSITIVE_VALUE = 0;
	
	//get all from resources rather!!!
	private static final String FILE_EXTENSION = ".wav";
	//TODO Optimize and Externalize:
	private static final int MINIMUM_SOUND_LEVEL = 50;
	private static final int MAXIMUM_SOUND_LEVEL = Short.MAX_VALUE-1000;
	private static final int ESTIMATED_QC_TIME_PER_PROMPT_IN_SECONDS = 8; //TODO adjust based on language and uni/bi/trigrams
	private static final String TRAINING_SESSION_TYPE_STRING = "training";
	private static final String RECORDING_SESSION_TYPE_STRING = "recording";
	
	private static final String CORPUS_FILENAME_EXTENSION = ".txt";
	private static final String NEWLINE = "\n";
	
	private static final String currentBusySessionFilename = "currentBusySession.txt"; //Later from res
	
	//Loaded from resources
	private String programFoldername = null;
	private String dataOutputFolderName = null;
	private String corpusFolderName = null;
	private String CORPUS_WRAP_COUNTER_FILENAME_SUFFIX = null;
	private String trackingFolderName = null;
	private String externalTargetFilename = null;
	private int TARGET_NUMBER_OF_LINES_TO_READ_TRAINING = -1;
	private int TARGET_NUMBER_OF_LINES_TO_READ_RECORDING = -1;
	private String TRAINING_CORPUS_SUFFIX = null;
	
	private String corpusName;
	private String respondentProfileKey;
	private String deviceIMEI;
	private String sessionDateTimeStamp;
	private String sessionType;
	
	private String accent;
	private String age;
	private String gender;
	private String location;
	private String environment;
	private String comments;
	
	private String sessionFolderName = null;

	private TextView tPromptString;
	private Button bPlayback;
	private Button bRecordStop;
	private Button bNext;
	private Button bSkip;
	
	private boolean recordingState = false;
	private boolean playingState = false;
	public File audiofile;
	public String filenameFixed = null; //the part fixed per session
	public String filenameVar = null;
	private int utteranceNumber = -1;
	public MediaPlayer mplay = null;
	public int maxAmplitude = -1;
	
	private RecordingWAV recWAV;
	private String currentCorpusPosition = null;
	
	boolean mExternalStorageAvailable = false;
	boolean mExternalStorageWriteable = false;
	private boolean sureToQuit = false;
	
	//Output files
	private File root = null;
	private String fPath = null;
	private String busyTrackingFile = null;
	WriteCurrentSessionNameToFile busySessionName = null;

	//Input files
	private File rootPromptList = null;
	private String fPathPrompts = null;
	private String corpusFilename = null;

	private String promptString = null;
	private String lineReadFromFile = null;
	
	Context appContext = null;
	private PromptList promptList = null;
	private boolean skipReasonProvided = false;
	HashSet<String> skipReasons = new HashSet<String>();
	
	public int sampleFreq = 16000; //Hz Valid entries depends on hardware: select {8000, 11025, 12000, 16000, 22050, 24000, 32000, 44100, 48000}
	public String sampleResText = null;
	public TextView tProgressStatusBar = null;
	public TextView tQCStatusBar = null;
	
	//Running stats
	private boolean waitForEachQCResult = true; //mimics remote var for independent access.
	public int statRecorded = 0; //Even if skipped after recording
	public int statSkipped = 0;
	public int statQCPassed = 0;
	public int statQCFailed = 0;
	public int statQCQueued = 0;
	
	private int numberOfObjectsDispatchedForQC = 0;
	private int numberOfExtraPromptsToLoad = 0;
	private int numberOfObjectsPassingQC = 0;
	private boolean calibrationSuccessful = false;
	private ProgressDialog dialogCal = null;
	private ProgressDialog dialogQC = null;
	
	private int numberOfGoodPromptsTarget = -1; //Actual goal excluding failures and skips i.e. only good stuff
	//Effective goal by adding more prompts if QC failed
	//Note: skips was not sent for QC thus automatically discounted
	private int numberOfGoodPromptsMovingTarget = -1; 
	
	//Logging
	private Logging log = new Logging(LOG_V, LOG_D, LOG_I, LOG_W); //Note: No LOG_E!
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        String methodTAG = "onCreate";
        
        setContentView(R.layout.main_recording_screen);
        
        log.logD(TAG,"MainRecordingActivity.onCreate started...");
        
        //GET EXTRAS PASSED ALONG WITH THE CALLING INTENT
        Bundle extras = getIntent().getExtras();        
        corpusName = extras.getString("corpusName");
        respondentProfileKey = extras.getString("respondentProfileKey");
        deviceIMEI = extras.getString("IMEI");
        sessionDateTimeStamp = extras.getString("sessionDateTimeStamp");
        sessionType = extras.getString("sessionType");
        
    	//Added 2011-06-21
        accent = extras.getString("accent");
        age = extras.getString("age");
        gender = extras.getString("gender");
        location = extras.getString("location");
        environment = extras.getString("environment");
        comments = extras.getString("comments");
        
        log.logD(TAG,"corpusName received from intent data: " + corpusName);
        log.logD(TAG,"respondentProfileKey: " + respondentProfileKey);
        log.logD(TAG,"deviceIMEI: " + deviceIMEI);
        log.logD(TAG,"sessionDateTimeStamp: " + sessionDateTimeStamp );
        log.logD(TAG,"sessionType: " + sessionType);
        
        log.logD(TAG,"accent: " + accent);
        log.logD(TAG,"age: " + age);
        log.logD(TAG,"gender: " + gender);
        log.logD(TAG,"location: " + location);
        log.logD(TAG,"environment: " + environment);
        log.logD(TAG,"comments: " + comments);
        
    	//GET UI HANDLES
        tPromptString = (TextView) findViewById(R.id.tPromptString);
        tProgressStatusBar = (TextView) findViewById(R.id.tProgressStatusBar);
        tQCStatusBar = (TextView) findViewById(R.id.tQCStatusBar);
    	bRecordStop = (Button) findViewById(R.id.bRecordStop);
    	bPlayback = (Button) findViewById(R.id.bPlayback);
    	bNext = (Button) findViewById(R.id.bNext);
    	bSkip = (Button) findViewById(R.id.bSkip);
        
    	//SET INIT VALUES
    	//Values from resources
    	safeToCallService = false;
    	appContext = getApplicationContext();

    	Resources res = getResources(); //Get instance of a resource and not just the resource ID (e.g. R.id.xxxx)
    	
    	TARGET_NUMBER_OF_LINES_TO_READ_TRAINING = res.getInteger(R.integer.TARGET_NUMBER_OF_LINES_TO_READ_TRAINING);
    	TARGET_NUMBER_OF_LINES_TO_READ_RECORDING = res.getInteger(R.integer.TARGET_NUMBER_OF_LINES_TO_READ_RECORDING);
    	TRAINING_CORPUS_SUFFIX = res.getString(R.string.TRAINING_CORPUS_SUFFIX);
  
    	programFoldername = res.getString(R.string.PROGRAM_FOLDER_NAME); //TODO fix case
    	dataOutputFolderName = res.getString(R.string.DATA_OUTPUT_FOLDER_NAME);
    	corpusFolderName = res.getString(R.string.CORPUS_FOLDER_NAME);
    	trackingFolderName = res.getString(R.string.TRACKING_FOLDER_NAME);
    	externalTargetFilename = res.getString(R.string.EXTERNAL_TARGET_NUMBER_OF_LINES_TO_READ_RECORDING_FILENAME);
    	
    	//Internal values
    	sessionFolderName = "/" + deviceIMEI + "_S" + sessionDateTimeStamp;
        
        utteranceNumber = 0;
        bPlayback.setEnabled(false);
        bNext.setEnabled(false);
        skipReasonProvided = false;
        allQCComplete = false;
        totalNumberOfFilesSentForQC = 0;
        numberOfObjectsDispatchedForQC = 0;
        numberOfObjectsPassingQC = 0;
        calibrationSuccessful = false;
                
        //Init Corpus position
	    if (ifCorpusExists(corpusName)) { //TODO any for now, later specific
	    	log.logD(TAG,"ifCorpusExists(corpusName) was true.");
	    }
	    else {
	    	log.logCriticalError(TAG, methodTAG, "FATAL ERROR: Corpus " + corpusName + " disappeared.");
	    	warningDialog("FATAL ERROR: Corpus disappeared.");
	    	finish();
	    }
    	
		//Check if SDcard is ready to write to
		String stateSD = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(stateSD)) {
		    // We can read and write the media
		    mExternalStorageAvailable = mExternalStorageWriteable = true;
		    log.logI(TAG,"SDCARD: Yay, we can read and write to it!");
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(stateSD)) {
		    // We can only read the media
		    mExternalStorageAvailable = true;
		    mExternalStorageWriteable = false;
		    log.logI(TAG,"SDCARD: Nope, we can only read it.");
		} else {
		    // Something else is wrong. It may be one of many other states, but all we need
		    //  to know is we can neither read nor write
		    mExternalStorageAvailable = mExternalStorageWriteable = false;
		    log.logI(TAG,"SDCARD: Hmmm...we can neither read nor write to it!");
		}
		
		if (mExternalStorageWriteable != false) {
				root = Environment.getExternalStorageDirectory();
			    log.logI(TAG,"root = " + root.toString());
			    
			    //Learn: Cannot create all in one go. Do one at a time.
			    new File("/sdcard" + programFoldername).mkdir(); //error checking??
			    new File("/sdcard" + programFoldername + dataOutputFolderName).mkdir(); //error checking??
			    new File("/sdcard" + programFoldername + dataOutputFolderName + sessionFolderName).mkdir(); //error checking??
			    new File("/sdcard" + programFoldername + trackingFolderName).mkdir();
			    
			    //Write tracking file
			    busyTrackingFile = root + programFoldername + trackingFolderName + "/" + currentBusySessionFilename;
			    busySessionName = new WriteCurrentSessionNameToFile(busyTrackingFile);
			    busySessionName.writeName(sessionFolderName.substring(1, sessionFolderName.length())); //rm slash hack
		}
		else {
			log.logE(TAG, "Sorry, but the SDcard is not ready/writable.");
		}
		
		if (sessionType.equals(TRAINING_SESSION_TYPE_STRING) ) {
			numberOfGoodPromptsTarget = TARGET_NUMBER_OF_LINES_TO_READ_TRAINING; //Starting target
			corpusFilename = corpusName + TRAINING_CORPUS_SUFFIX;
		}
		else if (sessionType.equals(RECORDING_SESSION_TYPE_STRING)) {
			corpusFilename = corpusName;
			
			//Attempt to get target from an external file
			String tmpFileFQ = "/sdcard" + programFoldername + trackingFolderName + "/" + externalTargetFilename;//TODO fix
			log.logD(TAG,"Looking for external file '" + tmpFileFQ + "' for target number of recording lines.");
			GetValueFromFileOnSDCard v = new GetValueFromFileOnSDCard(tmpFileFQ);
			int temp = v.getValue();
			if (temp != BAD_POSITIVE_VALUE) {
				numberOfGoodPromptsTarget = temp;
				log.logD(TAG,"numberOfGoodPromptsTarget from file = " + numberOfGoodPromptsTarget);
			}
			else {
				numberOfGoodPromptsTarget = TARGET_NUMBER_OF_LINES_TO_READ_RECORDING; //from resources
				log.logD(TAG,"numberOfGoodPromptsTarget from resources = " + numberOfGoodPromptsTarget);
			}
			//TODO corpusFilename must cater for more than one file/version per corpus	
		}
		else {
			log.logE(TAG, "Illegal sessionType string.");
		}
		
		//Init output filename
		filenameFixed = constructFilenameFixed(corpusName, respondentProfileKey, sessionDateTimeStamp, sessionType);
		log.logD(TAG,"filenameFixed = " + filenameFixed);
		filenameVar = String.format("%05d", utteranceNumber); //99 999 max per session ;-)
		log.logD(TAG,"filenameVar = " + filenameVar);  	
		fPath = root + programFoldername + dataOutputFolderName + sessionFolderName + "/" + filenameFixed + filenameVar + FILE_EXTENSION;
		log.logD(TAG,"fPath = " + fPath);
		log.logD(TAG,"I am recording to file = " + fPath);

		//Init prompts
	    rootPromptList = Environment.getExternalStorageDirectory();
	    log.logI(TAG,"root = " + rootPromptList.toString());
		
	    log.logD(TAG,"Loading prompts from file: " + corpusFilename);
		promptList = new PromptList(appContext, corpusFilename, numberOfGoodPromptsTarget);
		
		log.logI(TAG,"Extracting prompts from list...(first time)");
		if ( (promptString = promptList.extractNextString()) != null) {
	        log.logI(TAG,"promptString = '" + promptString + "'");
	        tPromptString.setText(promptString);
        }
        else {
        	log.logI(TAG,"End of prompts.");
        	warningDialog("No prompts to load.");
        }
        
        bRecordStop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	
            	if (recordingState == false) { //requested to start recording
	            	bRecordStop.setText("Stop recording");
	            	bPlayback.setEnabled(false);
	            	bNext.setEnabled(true);
	            	bSkip.setEnabled(false);
	            	recordingState = true;
	            	bPlayback.setText("Start playback...");
	            	try {
						startRecordingPCM();
					} 
	            	catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
            	}
            	else
            	{ //requested to stop recording
					stopRecordingPCM(); //recordingState still true here
					recordingState = false;
            		bRecordStop.setText("Start re-recording");
	            	bPlayback.setEnabled(true);
	            	bNext.setEnabled(true);
	            	bSkip.setEnabled(true);
            	}
            }
        });
        
        bPlayback.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	
            	if (playingState == false) { //requested to play while not already playing
            		//prepare to start playing
	            	bPlayback.setText("Stop playing");
	            	bRecordStop.setEnabled(false);
	            	bSkip.setEnabled(false);
	            	bNext.setEnabled(false);
	        		playingState = true;
	            	try {
	            		log.logI(TAG, "Called startPlaying()");
						startPlaying(); //locks till done
					} 
	            	catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
            	}
            	else //requested to stop playing
            	{
	            	stopPlaying();
	            	bNext.setEnabled(true);
	            	bSkip.setEnabled(true);
            	}          	
            }
        });
        
        /** 
         * Next button listener
         */
        bNext.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {       	
            	
				stopRecordingPCM(); //recordingState may be true or false
				recordingState = false;
				bRecordStop.setText("Start recording");
            	bPlayback.setEnabled(false);
            	bNext.setEnabled(false);
            	bSkip.setEnabled(true);
            	
            	if (passingRealTimeQC()) {
            		addCurrentObjectToQCQueue();
            		
                	//Update moving target
            		numberOfGoodPromptsMovingTarget = numberOfGoodPromptsTarget + serviceBinder.numberOfObjectsFailingQC; 
                	log.logD(TAG,"numberOfGoodPromptsMovingTarget = " + numberOfGoodPromptsMovingTarget);
            		
            		//Note: Assumed at this point dispatched = good. OK if QC results not lag behind to much.
            		if (numberOfObjectsDispatchedForQC < numberOfGoodPromptsMovingTarget) { //... Effective target not reached
            			log.logD(TAG,"Target? numberOfObjectsDispatchedForQC = " + numberOfObjectsDispatchedForQC);
            			log.logD(TAG,"Target? numberOfGoodPromptsMovingTarget = " + numberOfGoodPromptsMovingTarget);
            			prepareForNextPrompt();
            		}
            		else { //Target reached (at least in terms of sending for QC, not passed QC
            			log.logV(TAG,"Target? numberOfObjectsDispatchedForQC = " + numberOfObjectsDispatchedForQC);
            			log.logV(TAG,"Target? numberOfGoodPromptsMovingTarget = " + numberOfGoodPromptsMovingTarget);
            			log.logV(TAG,"Target met. Wrapping things up...");
            			if ( sessionType.equals(TRAINING_SESSION_TYPE_STRING) ) {
            				log.logD(TAG,"End of training session.");
            				thankYouTrainingDialog();

        					dialogCal = ProgressDialog.show(MainRecordingActivity.this, "CALIBRATION", "Calibration in progress...", true);
        					dialogQC = ProgressDialog.show(MainRecordingActivity.this, "QC INFO", "QC operation in progress...", true);
    			        	
    			        	//Get object reference so can dismiss from service when all QCs done.
    			        	if (safeToCallService) {
    							serviceBinder.sendProgressDialogReference(dialogQC);
    			        	}
    			        	
    			        	if (safeToCallService) {
    			        		log.logD(TAG,"numberOfObjectsDoneWithQC = " + serviceBinder.numberOfObjectsDoneWithQC + "; numberOfObjectsReceivedForQC = " + serviceBinder.numberOfObjectsReceivedForQC);
    			        	}
    			        	
    			        	calculateCalibrationConstants();
            			}
            			else if (sessionType.equals(RECORDING_SESSION_TYPE_STRING)) {
            				log.logD(TAG,"End of recording session.");
            				thankYouRecordingDialog();

            				dialogQC = ProgressDialog.show(MainRecordingActivity.this, "QC INFO", "QC operation in progress...", true);

    			        	//Get object reference so can dismiss from service when all QCs done.
    			        	if (safeToCallService) {
    							serviceBinder.sendProgressDialogReference(dialogQC);
    							serviceBinder.recordingSessionType = true;
    			        	}
    			        	
    			        	if (safeToCallService) {
    			        		log.logD(TAG,"numberOfObjectsDoneWithQC = " + serviceBinder.numberOfObjectsDoneWithQC + "; numberOfObjectsReceivedForQC = " + serviceBinder.numberOfObjectsReceivedForQC);
    			        	}
    			        	
    			        	finalizeApplication();
            			}
            			else {
            				//FATAL ERROR
            			}
            		}
            	}
            	else { //...failed passingRealTimeQC
            		Toast.makeText(getApplicationContext(), "Sorry, but the volume was detected as either too soft or too loud. Please record again.", Toast.LENGTH_LONG).show();
            		prepareToRedoPrompt();
            	}
            	log.logV(TAG,"The end of bNext.setOnClickListener was reached.");
            }
        });
        
        /**
         * Skip button listener
         */
        bSkip.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	
            	log.logD(TAG, "Skipping a prompt");
            	
	            if (skipReasonProvided) { 
	            	//TODO: ??
	            }
	            else { //still to say/pick why
	            	skipDialog();
	            	//TODO get reason(s) [checkboxes] and comments
	            }
            }
        });
        
        // Bind to the service
        bindIntent = new Intent(MainRecordingActivity.this, MyService.class);
		if (bindService(bindIntent, mConnection, Context.BIND_AUTO_CREATE)) {
			log.logD(TAG,"Requesting service to be bound to this activity...");
		}
		else {
			log.logD(TAG,"Service could not be bound to this activity.");
		}
    }//onCreate
        
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
    	super.onRestoreInstanceState(savedInstanceState);
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	log.logI(TAG,"I am in onResume");
    	

    }
    
    @Override
	protected void onPause() {
		super.onPause();
	}
    
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		if (safeToCallService) {
			if (serviceBinder.serviceRunning) {
				safeToCallService = false;
				unbindService(mConnection);
				log.logD(TAG, "A request was submitted to stop the service...might take time.");
			}
			else {
				log.logD(TAG, "The requested service did not need to be stopped as it was not running. (onPause)");
			}
		}
		
	}

	public void startRecordingPCM() throws IOException {
    	
    	String methodTAG = "startRecordingPCM";
  	    	
    	//Create record object
    	recWAV = new RecordingWAV(AudioSource.MIC, sampleFreq, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
    	log.logD(TAG,"recWAV.State after constructor is: " + recWAV.getState());
    	if (recWAV.state == RecordingWAV.State.ERROR) {
    		log.logD(methodTAG, "recWAV.State after constructor is ERROR, thus shutting down. Writing a log.");
    		log.logCriticalError(TAG, methodTAG, "recWAV.State after constructor is ERROR, thus shutting down.");
    	}

    	recWAV.setOutputFile(fPath);
    	
    	log.logI(TAG,"recWAV.State after setOutputFile() is: " + recWAV.getState());
    	recWAV.prepare();
    	log.logI(TAG,"recWAV.State after prepare() is: " + recWAV.getState());
    	
    	tPromptString.setTextColor(getResources().getColor(R.color.hltGreen));
    	
    	recWAV.start();
    	log.logI(TAG,"recWAV.State after start() is: " + recWAV.getState());
    }
    
    protected void stopRecordingPCM(){
    	
    	//Note, maybe called twice for each prompt. When user presses stop (1st) and then next (2nd).
    	
    	String methodTAG = "stopRecordingPCM";
    	
    	tPromptString.setTextColor(getResources().getColor(R.color.red));
    	
    	log.logI(TAG,"recWAV.State before stop() is: " + recWAV.getState());
    	if (recordingState == true) {
    		recWAV.stop();
    		log.logI(TAG,"recWAV.State after stop() is (1): " + recWAV.getState());
    	}
    	log.logI(TAG,"recWAV.State after stop() is (2): " + recWAV.getState());
    	maxAmplitude = recWAV.maxAmplitudeValue;
    	log.logD(methodTAG,"maxAmplitude = " + maxAmplitude);
    }
    
    protected void startPlaying() throws IOException {
    	
    	String methodTAG = "startPlaying";
    	
    	mplay = new MediaPlayer();
    	
    	try {
    		log.logD(methodTAG,"fPath = " + fPath);
			mplay.setDataSource(fPath);
		} 
    	catch (IllegalArgumentException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} 
    	catch (IllegalStateException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} 
    	
    	try {
    		mplay.prepare();
    		mplay.setLooping(false);
    		mplay.setOnCompletionListener(new OnCompletionListener() {

			@Override
			public void onCompletion(MediaPlayer mp) {
        		bPlayback.setText("Restart playing");
            	playingState = false;
            	bRecordStop.setEnabled(true);
            	bNext.setEnabled(true);
            	bSkip.setEnabled(true);
            	mplay.stop();
			}});
    		
    		mplay.start();
    	} 
    	catch (Exception e) {
    		log.logE(methodTAG, "Audio Playback failed.");
    	}
    }
    
    protected void stopPlaying() {
		bPlayback.setText("Restart playing");
    	playingState = false;
    	bRecordStop.setEnabled(true);
    	mplay.stop();
    	mplay.reset();
    }
    
	private boolean ifCorpusExists(String corpusFilename) {
		//TODO make smarter
		log.logD(TAG,"corpusFilename in ifCorpusExists = " + corpusFilename);
		
		if (sessionType.equals(TRAINING_SESSION_TYPE_STRING)) {
			log.logD(TAG,"Doing a training session...");
		}
		
		File fid = new File("/sdcard" + programFoldername + corpusFolderName + "/" + corpusFilename + CORPUS_FILENAME_EXTENSION);
		log.logD(TAG,"fid.getAbsoluteFile() = " + fid.getAbsoluteFile());
		
		if (fid.exists()) {
			log.logD(TAG,"File: " + fid.getAbsolutePath() + " does exist.");
			return true;
		}
		else {
			log.logI(TAG,"File: " + fid.getAbsolutePath() + " does NOT exist.");
			return false;
		}
	}
    
	public String constructFilenameFixed(String Corpus, String respondentProfileKey, String sDateTimeSession, String sessionTypeString) {
		String temp = null;
		
		temp = Corpus + "_" + respondentProfileKey + "_" + sDateTimeSession + "_" + sessionTypeString + "_";
		log.logI(TAG,"constructFilenameFixed() = " + temp);
		return temp;
	}
	
	private void calculateCalibrationConstants() {
		
		log.logV(TAG,"calculateCalibrationConstants started...");
		
		final Handler handler = new Handler() {
			public void handleMessage(Message msg) {
				log.logV(TAG,"msg = " + msg.what);
				log.logV(TAG,"msg = " + msg.arg1);
				log.logV(TAG,"msg = " + msg.arg2);
				calibrationSuccessful = true;
				dialogCal.dismiss();
		    }
		};
				   
		Thread calcCal = new Thread() {
			//Keep this thread's declarations in it
			int estimatedSleepTimeInSeconds = 1; //Default as 0 will cause processor hog
			public void run() {
				
				log.logV(TAG,"Trying to calcCal, but may have to wait for QCs to be done.");
				
				try {
					//TODO implement actual calibration
					//Wait for all QC's to be done, then calculate calibration constants
		        	if (safeToCallService) {
						serviceBinder.setFinalWaitingForQCsToBeDone();
						log.logD(TAG,"setFinalWaitingForQCsToBeDone was called.");
						serviceBinder.startAsyncQCTask(); //In case another queue waiting to be QCed
						log.logD(TAG,"startAsyncQCTask was called for the last time.");
		        	}
		        	
		        	if ( (safeToCallService & !serviceBinder.continueWithCalibrationCalculations) ) { //Just to print
		        		log.logD(TAG,"safeToCallService & !serviceBinder.continueWithCalibrationCalculations == true");

		        		estimatedSleepTimeInSeconds = 1;
		        		log.logD(TAG,"estimatedSleepTimeInSeconds = " + estimatedSleepTimeInSeconds + " because of:");
		        		log.logD(TAG,"numberOfObjectsDispatchedForQC = " + numberOfObjectsDispatchedForQC);
		        		log.logD(TAG,"serviceBinder.numberOfObjectsDoneWithQC = " + serviceBinder.numberOfObjectsDoneWithQC);
		        	}
		        	
					while ( (safeToCallService & !serviceBinder.continueWithCalibrationCalculations) ) {
						//Do nothing
						Thread.sleep(estimatedSleepTimeInSeconds*1000); //Do not hog processor while waiting
						log.logD(TAG,"Re-checking if more QCs needs to be done...");
						serviceBinder.startAsyncQCTask(); //In case another queue waiting to be QCed
					}
					
					//TODO calculations here
					log.logI(TAG,"Finished waiting for QCs to be done. All done now. Starting calc...");
				} 
				catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				log.logI(TAG,"Done with calc. Sending results of calc...");
				handler.sendMessage(handler.obtainMessage(1, 2, 3));
			}
		};
		calcCal.start();
		log.logV(TAG,"calculateCalibrationConstants ended.");
	}
	
	private void finalizeApplication() {
		
		log.logV(TAG,"finalizeApplication started...");
		
		final Handler handler = new Handler() {
			public void handleMessage(Message msg) {
				log.logV(TAG,"msg = " + msg.what);
				log.logV(TAG,"msg = " + msg.arg1);
				log.logV(TAG,"msg = " + msg.arg2);
				dialogQC.dismiss();
		    }
		};
				   
		Thread calcCal = new Thread() {
			//Keep this thread's declarations in it
			int estimatedSleepTimeInSeconds = 1; //Warning: Default of 0 will cause processor hog
			public void run() {
				
				log.logD(TAG,"Trying to finalizeApplication, but may have to wait for QCs to be done.");
				
				try {
					//TODO implement actual calibration
					//Wait for all QC's to be done, then calculate calibration constants
		        	if (safeToCallService) {
						serviceBinder.setFinalWaitingForQCsToBeDone();
						log.logD(TAG,"setFinalWaitingForQCsToBeDone was called.");
		        	}
		        	
		        	if ( (safeToCallService & !serviceBinder.continueWithCalibrationCalculations) ) {//print only
		        		log.logD(TAG,"1100 safeToCallService & !serviceBinder.continueWithCalibrationCalculations == true");
		        		//Estimate waiting time
		        		estimatedSleepTimeInSeconds = ESTIMATED_QC_TIME_PER_PROMPT_IN_SECONDS*(numberOfObjectsDispatchedForQC - serviceBinder.numberOfObjectsDoneWithQC);
		        		log.logD(TAG,"estimatedSleepTimeInSeconds = " + estimatedSleepTimeInSeconds + " because of:");
		        		log.logD(TAG,"numberOfObjectsDispatchedForQC = " + numberOfObjectsDispatchedForQC);
		        		log.logD(TAG,"serviceBinder.numberOfObjectsDoneWithQC = " + serviceBinder.numberOfObjectsDoneWithQC);
		        	}
		        	
					while ( (safeToCallService & !serviceBinder.continueWithCalibrationCalculations) ) {
						//Do nothing
						estimatedSleepTimeInSeconds = 2;
						Thread.sleep(estimatedSleepTimeInSeconds*1000); //Do not hog processor while waiting
					}
					
					//TODO calculations here
					log.logD(TAG,"Finished waiting for QCs to be done. All done now. Closing app.");
					
					//fake some processing for now		
					Thread.sleep(1000);
				} 
				catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				log.logD(TAG,"Done with finalizing...");
				handler.sendMessage(handler.obtainMessage(1, 2, 3));
			}
		};
		calcCal.start();
		log.logV(TAG,"finalizeApplication ended.");
	}
	

	private void warningDialog(String message) {
		log.logD(TAG,"warningDialog was triggered.");
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("WARNING");
		builder.setMessage(message);
		builder.setCancelable(false);
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   log.logD(TAG,"warningDialog was dismissed");
		           }
		       });
		AlertDialog alert = builder.create();
		alert.show();
	}
	
	private void thankYouTrainingDialog() {
		log.logD(TAG,"thankYouTrainingDialog was triggered.");
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("THANK YOU");
		builder.setMessage("You have successfully completed the training session.\nPlease hand the phone to the Fieldworker at this point so that they are aware of your progress.");
		builder.setCancelable(false);
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   log.logD(TAG,"thankYouDialog was dismissed");
						if (safeToCallService) {
							if (serviceBinder.serviceRunning) {
								safeToCallService = false;
								unbindService(mConnection);
								log.logD(TAG, "A request was submitted to stop the service...might take time.");
							}
							else {
								log.logD(TAG, "The requested service did not need to be stopped as it was not running. (thankYouTrainingDialog)");
							}
						}
						
						if (calibrationSuccessful) {
					    	//CALL NEXT ACTIVITY...itself here, but with different parameters
					    	Intent mainRecordingActivity = new Intent(MainRecordingActivity.this, MainRecordingActivity.class);
					    	
					    	//Add extras to pass on
					    	mainRecordingActivity.putExtra("corpusName", corpusName);
					    	mainRecordingActivity.putExtra("respondentProfileKey", respondentProfileKey);
					    	mainRecordingActivity.putExtra("IMEI", deviceIMEI);
					    	mainRecordingActivity.putExtra("sessionDateTimeStamp", sessionDateTimeStamp);
					    	mainRecordingActivity.putExtra("sessionType", RECORDING_SESSION_TYPE_STRING);
					    	
					    	//Added 2012-01-18 (for transfer between training and recording session)
					    	mainRecordingActivity.putExtra("accent", accent);
					    	mainRecordingActivity.putExtra("age", age);
					    	mainRecordingActivity.putExtra("gender", gender);
					    	mainRecordingActivity.putExtra("location", location);
					    	mainRecordingActivity.putExtra("environment", environment);
					    	mainRecordingActivity.putExtra("comments", comments);
					    	
					    	startActivity(mainRecordingActivity);
					    	
			                finish();
						}
						else {
							log.logD(TAG,"Cal was not successful!");
							fatalErrorDialog("I am sorry, but the calibration process did not complete successfully. Please consult your Fieldworker immediately.");
						}
		           }
		});
		AlertDialog alert = builder.create();
		alert.show();
	}
	
	private void thankYouRecordingDialog() {
		log.logD(TAG,"thankYouRecordingDialog was triggered.");
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("THANK YOU");
		builder.setMessage("Thank you for your time and perseverance in donating data for speech applications for South Africa!\nWe value your contribution.\n\nThe Meraka HLT Team");
		builder.setCancelable(false);
		busySessionName.writeName("");
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   log.logD(TAG,"thankYouDialog was dismissed");
						if (safeToCallService) {
							if (serviceBinder.serviceRunning) {
								safeToCallService = false;
								unbindService(mConnection);
								log.logD(TAG,"A request was submitted to stop the service...might take time.");
							}
							else {
								log.logD(TAG,"The requested service did not need to be stopped as it was not running. (thankYouRecordingDialog)");
							}
						}
						log.logI(TAG,"Calling finish()...");
		                finish();
		           }
		});
		AlertDialog alert = builder.create();
		alert.show();
	}
	
	private void skipDialog() {
		log.logD(TAG,"skipDialog was triggered.");
    	skipReasons.clear();
    	log.logD(TAG,"Skip reasons: " + skipReasons);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("SKIP REASON");
		
		Resources myResources = getResources();
		final CharSequence[] items = myResources.getTextArray(R.array.skipReasonList);
		
		//Learn: NOTE: cannot use setMessage and setMultiChoiceItems in same dialog!!!
		builder.setMultiChoiceItems(items, null, new DialogInterface.OnMultiChoiceClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which, boolean isChecked) {
				
				//Keep as linked list as check/uncheck
				if (skipReasons.contains((String) items[which])) {
					skipReasons.remove((String) items[which]);
				}
				else {
					skipReasons.add((String) items[which]);
				}
			}
		});

		builder.setCancelable(false);
		
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {

		                //Write 'statistical' file for ref
		        	   log.logD(TAG,"Skip reasons: " + skipReasons);
		            	String statsFQFilename = root + programFoldername + dataOutputFolderName + sessionFolderName + "/" + filenameFixed + filenameVar + ".skipped";
		            	MetadataFile metaFile = new MetadataFile(statsFQFilename);

		            	statSkipped++;
		            	if (safeToCallService) {
		            		serviceBinder.increaseNumberOfSkippedPrompts();
		            		serviceBinder.updateStatusBars();
		            	}
		            	//Note: does not make target more, just make me need record more as recorded does not advance
		            	
		            	log.logD(TAG,"numberOfGoodPromptsTarget = " + numberOfGoodPromptsTarget);
		            	metaFile.writeMetadataFileForSkippedItem(promptString, skipReasons);

	            		prepareForNextPrompt(); //No need to check target as just increased it
	            		skipReasonProvided = false; //reset for next time
		           }
		       });
		
		AlertDialog alert = builder.create();
		alert.show();
	}

	private boolean ifCanReadAndWriteSDCARD() {
		//TODO maybe move to own class and instantiate an object

		//TODO find out WHY do I get this warning of variables no being used when they are according to me
		boolean mExternalStorageAvailable = false;
		boolean mExternalStorageWriteable = false;
		
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
		    // We can read and write the media
		    mExternalStorageAvailable = mExternalStorageWriteable = true;
		    return true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
		    // We can only read the media
		    mExternalStorageAvailable = true;
		    mExternalStorageWriteable = false;
		    return false;
		} else {
		    // Something else is wrong. It may be one of many other states, but all we need
		    //  to know is we can neither read nor write
		    mExternalStorageAvailable = mExternalStorageWriteable = false;
		    return false;
		}
	}
	
	/*
	 * SECTION: Override hardware keys
	 * 
	 */
	@Override
	public void onBackPressed() {
	    // This will be called either automatically for you on 2.0
	    // or later, or by the code above on earlier versions of the
	    // platform.
		log.logI(TAG,"BACK key was detected by onBackPressed.");
	    return;
	}

	@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent event) {
	    if (keyCode == KeyEvent.KEYCODE_BACK) {
	        // a long press of the call key.
	        // do our work, returning true to consume it.  by
	        // returning true, the framework knows an action has
	        // been performed on the long press, so will set the
	        // canceled flag for the following up event.
	    	log.logI(TAG,"BACK key was detected by onKeyLongPress.");
	    	return true;
	    }
	    else if (keyCode == KeyEvent.KEYCODE_MENU) {
	    	log.logI(TAG,"MENU key was detected by onKeyLongPress.");
	        return true;
	    }
	    else if (keyCode == KeyEvent.KEYCODE_HOME) {
	    	log.logI(TAG,"HOME key was detected by onKeyLongPress.");
	        return true;
	    }
	    else if (keyCode == KeyEvent.KEYCODE_CALL) {
	    	log.logI(TAG,"CALL key was detected by onKeyLongPress.");
	        return true;
	    }
	    else if (keyCode == KeyEvent.KEYCODE_SEARCH) {
	    	log.logI(TAG,"SEARCH key was detected by onKeyLongPress.");
	        return true;
	    }
	    return super.onKeyLongPress(keyCode, event);
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
	    if (keyCode == KeyEvent.KEYCODE_BACK && event.isTracking()
	            && !event.isCanceled()) {
	        // if the call key is being released, AND we are tracking
	        // it from an initial key down, AND it is not cancelled,
	        // then handle it.
	    	log.logI(TAG,"BACK key was detected by onKeyUp.");
	    	
	    	if (!sureToQuit) {
		    	quitDialog("Are you 100% sure that you want to quit?");
		    	log.logD(TAG,"BACK key was detected by onKeyUp...still reading...");
		    	return true; //I handled it already
	    	}
	    	else if (sureToQuit) {
	    		//Note: finish() was already called by this time
	    		log.logD(TAG,"sureToQuit in onKeyUp was triggered...");
	    		return true; //I handled it already
	    	}
	    }
	    else if (keyCode == KeyEvent.KEYCODE_MENU && event.isTracking()
	            && !event.isCanceled()) {
	    	log.logI(TAG,"MENU key was detected by onKeyUp.");
	        return true;
	    }
	    else if (keyCode == KeyEvent.KEYCODE_HOME && event.isTracking()
	            && !event.isCanceled()) {
	    	log.logI(TAG,"HOME key was detected by onKeyUp.");
	        return true;
	    }
	    else if (keyCode == KeyEvent.KEYCODE_CALL && event.isTracking()
	            && !event.isCanceled()) {
	    	log.logI(TAG,"CALL key was detected by onKeyUp.");
	        return true;
	    }
	    else if (keyCode == KeyEvent.KEYCODE_SEARCH && event.isTracking()
	            && !event.isCanceled()) {
	    	log.logI(TAG,"SEARCH key was detected by onKeyUp.");
	        return true;
	    }
	    return super.onKeyUp(keyCode, event);
	}
	
	//Stay away for later versions of Android! Rather use onKeyUp
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)  {
		boolean COMPLETELY_HANDLED = true;
		
	    if (keyCode == KeyEvent.KEYCODE_HOME) {
	        // this tells the framework to start tracking for
	        // a long press and eventual key up.  it will only
	        // do so if this is the first down (not a repeat).
	        event.startTracking();
	        log.logI(TAG,"HOME key was detected by onKeyDown.");
	        return COMPLETELY_HANDLED;
	    }
	    return super.onKeyDown(keyCode, event);
	}
	
	private void quitDialog(String message) {
		log.logD(TAG,"quitDialog was triggered.");
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("WARNING");
		builder.setMessage(message);
		builder.setCancelable(false);
		sureToQuit = false;
		builder.setPositiveButton("Do NOT quit!", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		                log.logD(TAG,"quitDialog was dismissed with 'Do NOT quit!' response.");
		                sureToQuit = false;
		           }
		       });
		builder.setNegativeButton("Quit", new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) {
	        	   log.logD(TAG,"quitDialog was dismissed with 'Quit!' response");
	                sureToQuit = true;
					if (safeToCallService) { //still bound?
						busySessionName.writeName(""); //clear it
						if (serviceBinder.serviceRunning) {
							safeToCallService = false;
							unbindService(mConnection);
							Log.d(TAG, "A request was submitted to stop the service...might take time.");
						}
						else {
							log.logD(TAG, "The requested service did not need to be stopped as it was not running. (setNegativeButton)");
						}
						finish();
					}
	           }
	       });
		AlertDialog alert = builder.create();
		alert.show();
	}
	
	private void waitQCDialog(String message) {
		log.logD(TAG,"waitQCDialog was triggered.");
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("QC PROGRESS INFO");
		builder.setMessage(message);
		builder.setCancelable(false);
		AlertDialog alert = builder.create();
		alert.show();
}
	
	public boolean passingRealTimeQC() {
		String methodTAG = "passingRealTimeQC";
		//TODO may also implement other test later such as minimum variance etc.
		log.logD(methodTAG,"passingRealTimeQC sees maxAmplitude as: " + maxAmplitude);
		if ( (maxAmplitude > MINIMUM_SOUND_LEVEL) & (maxAmplitude < MAXIMUM_SOUND_LEVEL) ) {
			log.logD(TAG,"passingRealTimeQC: Passed");
			return true;
		}
		log.logI(TAG,"passingRealTimeQC: Failed");
		return false;
	}
	
	public void prepareToRedoPrompt() {
		//TODO
	}
	
	public void addCurrentObjectToQCQueue() {
		
		log.logD(TAG,"Adding the current object to the QC queue...");
		
		//Write metadata to file
		String metadataFQFilename = root + programFoldername + dataOutputFolderName + sessionFolderName + "/" + filenameFixed + filenameVar + ".xml";
		MetadataFile metaFile = new MetadataFile(metadataFQFilename);
		//String prompt, accent, age, gender, environment, comments
		String appVersion = getVersionInfo();
		metaFile.writeMetadataFile(promptString, accent, age, gender, location, environment, comments, appVersion); 

		//Add object to queue for QC
		String baseFilename = root + programFoldername + dataOutputFolderName + sessionFolderName + "/" + filenameFixed + filenameVar;
		String filenameFQ = baseFilename + ".wav"; //fix later
		String wordCategory = "TODO";
		double expectedUtteranceDuration = 0.0;
		
		if (safeToCallService) {
			serviceBinder.addObjectToQCQueue(filenameFQ, baseFilename, promptString, wordCategory, expectedUtteranceDuration);
			log.logD(TAG,"File " + filenameFQ + " queued for QC");
			numberOfObjectsDispatchedForQC++;
			log.logD(TAG,"numberOfObjectsDispatchedForQC = " + numberOfObjectsDispatchedForQC);
			serviceBinder.startAsyncQCTask();
		}
	}
	
	public void prepareForNextPrompt() {
		
		String methodTAG = "prepareForNextPrompt";

		//Prepare for next file
		utteranceNumber++;
		
		//Set next filename
		filenameVar = String.format("%05d", utteranceNumber); //99 999 max per session ;-)
		log.logD(TAG,"filenameVar = " + filenameVar);  	
		fPath = root + programFoldername + dataOutputFolderName + sessionFolderName + "/" + filenameFixed + filenameVar + FILE_EXTENSION;
		log.logV(TAG,"fPath = " + fPath);
		log.logV(TAG,"I am recording to file = " + fPath);
		
		//Get next prompt
		log.logI(TAG,"Extracting prompts from list...(subsequent times)");
		if ( (promptString = promptList.extractNextString()) != null) {
		    log.logI(TAG,"promptString = '" + promptString + "'");
		    tPromptString.setText(promptString);
		}
		else { //End of prompts from this PrompList object
			if (safeToCallService) {
				numberOfObjectsPassingQC = serviceBinder.numberOfObjectsPassingQC;
			}
			numberOfExtraPromptsToLoad = numberOfGoodPromptsTarget -  numberOfObjectsPassingQC; //Assume at this point those sent for QC will pass
			log.logD(TAG,"numberOfExtraPromptsToLoad = " + numberOfExtraPromptsToLoad);
			if (numberOfExtraPromptsToLoad > 0) {	
				log.logD(TAG,"Requesting " + numberOfExtraPromptsToLoad + " extra prompts to be loaded (" + numberOfGoodPromptsTarget + ";" + statRecorded + ")." );
				promptList.loadNewSetOfPrompts(numberOfExtraPromptsToLoad);
				if ( (promptString = promptList.extractNextString()) != null) {
				    log.logI(TAG,"promptString = '" + promptString + "'");
				    tPromptString.setText(promptString);
				}
			}
			log.logD(TAG, "No need to load more prompts.");
		}
		bPlayback.setEnabled(false);
		bNext.setEnabled(false);
		bRecordStop.setText("Start recording");
	}
	
	private String getVersionInfo() {

		PackageInfo pInfo;
		
		try {
			pInfo = getPackageManager().getPackageInfo("org.meraka.nchlt.woefzela", 0);
	        int versionNumber = pInfo.versionCode;
	        String versionName = pInfo.versionName;
	        log.logD(TAG,"Woefzela versionNumber: '" + versionNumber + "'");
	        log.logD(TAG,"Woefzela  versionName: '" + versionName + "'");
	        return versionName;
		} 
		catch (NameNotFoundException e) {
			log.logW(TAG,"No Woefzela installation detected. Please install first, then retry.");
			return "versionName not found";
		}
	}
	
	/**
	 * SERVICE CODE
	 * 
	 * 
	 * 
	 */
	
	//SERVICE: MOVE TO TOP
	private MyService serviceBinder; // Reference to the service
	private boolean safeToCallService = false;
	private Intent bindIntent = null;
	private ComponentName service = null;
	private int totalNumberOfFilesSentForQC = 0;
	private boolean allQCComplete = false;
	
    // Handles the connection between the service and activity
    private ServiceConnection mConnection = new ServiceConnection() {
    	public void onServiceConnected(ComponentName className, IBinder service) {
    		// Called when the connection is made.
    		serviceBinder = ((MyService.MyBinder)service).getService();
    		log.logD(TAG,"onServiceConnected has fired");
    		safeToCallService = true;
    		serviceBinder.setProgressStatusBarObjectReference(tProgressStatusBar);
    		serviceBinder.setQCStatusBarObjectReference(tQCStatusBar);
        	serviceBinder.sendNumberOfPromptsTargetToBeDisplayed(numberOfGoodPromptsTarget);
        	serviceBinder.updateStatusBars();
    	}

    	@Override
    	public void onServiceDisconnected(ComponentName className) {
    		// Received when the service unexpectedly disconnects.
    		
			if (safeToCallService) {
				if (serviceBinder.serviceRunning) {
					safeToCallService = false;
					unbindService(mConnection);
					Log.d(TAG, "A request was submitted to stop the service...might take time.");
				}
				else {
					log.logD(TAG, "The requested service did not need to be stopped as it was not running.");
				}
			}
    		
    		safeToCallService = false;
    		log.logD(TAG,"onServiceDisconnected has fired.");
    	}
    }; 

    private boolean checkIfAllQCsDone() {
    	if (totalNumberOfFilesSentForQC == serviceBinder.numberOfFilesQCDone) {
    		return true;
    	}
    	else return false;
    }
    
	private void fatalErrorDialog(String message) {
		
		//Try to hog screen, but may not succeed if pending or new activities launched
		
		ProgressDialog.show(this, "SYSTEM ERROR", message + "\n\nPlease write down this error message (verbatim) and then reboot the phone.", true);
			
		Thread checkUpdate = new Thread() {  
			public void run() {
				while (true) {
					//DO NOTHING...as this is a terminal error
				}
			}
		};
		checkUpdate.start();
	}
}