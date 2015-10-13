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
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Contacts;
import android.provider.Contacts.ContactMethodsColumns;
import android.provider.Contacts.People;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class SessionInfo extends Activity {
	
	//Housekeeping
	private final String TAG = "SessionInfo";
	private final boolean DEBUG_MODE = false;
	
	private static final String email1 = "someone@gmail.com";
	private static final String TRAINING_SESSION_TYPE_STRING = "training";
	private static final String CALIBRATING_SESSION_TYPE_STRING = "calibrating";
	private static final String RECORDING_SESSION_TYPE_STRING = "recording";
	
	//CONSTANTS: Get from resources
	private static String PROGRAM_FOLDER_NAME = null;
	private static String CORPUS_FOLDER_NAME = null;
	private static String PROFILE_FOLDER_NAME = null;
	private static String TRACKING_FOLDER_NAME  = null;
	private static String CORPUS_FILENAME_EXTENSION = null;
	private static String TRAINING_CORPUS_SUFFIX = null;
	private static String CORPUS_INPUT_PATH = null;
	
	private String corpusFilename = null;
	private String corpusFilenameFQ = null;
	private boolean sessionTypeSelected = false;
	private boolean trainingCorpusAvailable = false;
	
	private static String primaryFoldername = null;
	private static String profileFolderName = null;
	private static final String profileFolderSubdir = "/Sessions"; //get later from resources
	
	private static boolean sendWithEmail = false;
	
	private String IMEI = "IMEI";
	
	//UI elements
	private TextView tFWProfileKey;
	private String sFWProfileKeyString;
	private TextView tRespProfileKey;
	private String sRespProfileKeyString;
	private String sAccent;
	private String sAge;
	private String sGender;
	private String sessionTypeString;
	
	private Spinner spEnvironment;
	private Spinner spRecordingLocationArea;
	
	private TextView tDateTimeInfo;
	private CharSequence dateTimeStringNow = null;
	private TextView tStatusBar;
	private EditText eFee;
	private EditText eComments;
	
	//Action buttons
	private Button bLoadProfile;
	private Button bReset;
	private Button bNext;
	
	private String filename = null;
	private boolean sureToQuit = false;
	
	public static final int PICK_FILE = 2;
	
    /** Called when the activity is first created. */
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.session_info);
        
        //GET EXTRAS PASSED ALONG WITH THE CALLING INTENT
        Bundle extras = getIntent().getExtras();
        sFWProfileKeyString = extras.getString("fieldworkerProfileKey");
        sRespProfileKeyString = extras.getString("respondentProfileKey");
        sAccent = extras.getString("corpusName");
        sAge = extras.getString("age");
        sGender = extras.getString("gender");
        
        Log.d(TAG,"corpusName received from intent data: " + sAccent);
        
        //GET UI HANDLES
        tFWProfileKey = (TextView) findViewById(R.id.tFWProfileKey);
        tRespProfileKey = (TextView) findViewById(R.id.tRespProfileKey);
        spRecordingLocationArea = (Spinner) findViewById(R.id.spRecordingLocationArea);
        spEnvironment = (Spinner) findViewById(R.id.sEnvironment);
        eFee = (EditText) findViewById(R.id.eAgreedSessionFee);
        eComments = (EditText) findViewById(R.id.eComments);
        
        tDateTimeInfo = (TextView) findViewById(R.id.tDateTimeInfo);
        tStatusBar = (TextView) findViewById(R.id.tStatusBar);
        
        bLoadProfile = (Button) findViewById(R.id.bLoadProfile);
    	bReset = (Button) findViewById(R.id.bReset);
    	bNext = (Button) findViewById(R.id.bNext);
    	
    	//Set up initial UI values
    	tFWProfileKey.setText(sFWProfileKeyString);
    	tRespProfileKey.setText(sRespProfileKeyString);
    	GetDateTimeString now =  new GetDateTimeString(); //change to uppercase: class
    	dateTimeStringNow = now.getString();
    	tDateTimeInfo.setText(dateTimeStringNow);
    	
		//INIT RESOURCES
		Resources res = getApplicationContext().getResources(); 
		PROGRAM_FOLDER_NAME = res.getString(R.string.PROGRAM_FOLDER_NAME);
		CORPUS_FOLDER_NAME = res.getString(R.string.CORPUS_FOLDER_NAME);
		PROFILE_FOLDER_NAME = res.getString(R.string.PROFILE_FOLDER_NAME);
		TRACKING_FOLDER_NAME = res.getString(R.string.TRACKING_FOLDER_NAME);
		CORPUS_FILENAME_EXTENSION = res.getString(R.string.CORPUS_FILENAME_EXTENSION);
		TRAINING_CORPUS_SUFFIX = res.getString(R.string.TRAINING_CORPUS_SUFFIX);

    	//Own rule: ...Path ends in /
    	//Building filenames and paths in one place
    	CORPUS_INPUT_PATH = "/sdcard" + PROGRAM_FOLDER_NAME + CORPUS_FOLDER_NAME + "/";

    	//TODO Clean up
    	primaryFoldername = PROGRAM_FOLDER_NAME;
    	profileFolderName = PROFILE_FOLDER_NAME;
    	
    	sessionTypeSelected = false;
    	trainingCorpusAvailable = false;
    	
    	bLoadProfile.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				Intent intent = new Intent("org.openintents.action.PICK_FILE");
				Uri path =  Uri.parse("file:///sdcard/" + primaryFoldername + "/" + profileFolderName + "/" + profileFolderSubdir); // + "/");
				
				intent.setData(path);
				intent.putExtra("org.openintents.extra.TITLE", "Pick a profile");
				intent.putExtra("org.openintents.extra.BUTTON_TEXT", "Go");
				startActivityForResult(intent, PICK_FILE);
			}
		});
    	
    	bReset.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
			}
		});
    	
    	bNext.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				if (minimalInfoOK()) {
					String sFWProfileKey = (tFWProfileKey.getText()).toString();
					String sRespProfileKey = (tRespProfileKey.getText()).toString();
					String sRecordingLocationArea = (String) (spRecordingLocationArea.getItemAtPosition(spRecordingLocationArea.getSelectedItemPosition()));
					String sEnvironment = (String) (spEnvironment.getItemAtPosition(spEnvironment.getSelectedItemPosition()));
					String sFee = (eFee.getText()).toString();
					String sComments = (eComments.getText()).toString();
					
					sessionTypeString = TRAINING_SESSION_TYPE_STRING;
	
					String sDateTimeInfo = (tDateTimeInfo.getText()).toString();
	
			    	//create unique session ID hash
			    	Log.d(TAG,"sessionKey parts (sFWProfileKey) = " + sFWProfileKey);
			    	Log.d(TAG,"sessionKey parts (sRespProfileKey) = " + sRespProfileKey);
			    	Log.d(TAG,"sessionKey parts (sDateTimeInfo) = " + sDateTimeInfo);
			    	Log.d(TAG,"I am ready to write session info to a file...");
			    	
			    	//Get IMEI or...
					TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
					IMEI = telephonyManager.getDeviceId();
					if (IMEI == null) {
						Log.w(TAG,"Could not get Device Id such as IMEI for GSM and the MEID for CDMA phones!");
						IMEI = "UNKNOWN_IMEI";
					}
					Log.d(TAG,"IMEI = " + IMEI);
			    	
			    	//Commit info to a file
			    	SaveSessionInfo info = new SaveSessionInfo(getApplicationContext(), IMEI, sFWProfileKeyString, sRespProfileKeyString, sessionTypeString, (String) dateTimeStringNow, sRecordingLocationArea, sEnvironment, sFee, sComments);
					filename = info.getFilename();
			    	Log.i(TAG,"Profile filename = " + filename);
	
					//experimental: send email with profile attached
			    	if (sendWithEmail) {
						Intent sendIntent = new Intent(Intent.ACTION_SEND);
						sendIntent.setType("text/plain");
						String path = "file:///sdcard/" + filename;
						sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(path));
						sendIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{email1});
						sendIntent.putExtra(Intent.EXTRA_SUBJECT, "MerakaWoefzela data"); 				
						sendIntent.putExtra(Intent.EXTRA_TEXT, "See attachment for Fieldworker Profile.\n\nMe"); 
						sendIntent.setType("message/rfc822"); 
						startActivity(Intent.createChooser(sendIntent, "Email:")); 
			    	}
			    	
			    	//CALL NEXT ACTIVITY
			    	Intent mainRecordingActivity = new Intent(SessionInfo.this, MainRecordingActivity.class);
			    	
			    	//Add extras to pass on
			    	mainRecordingActivity.putExtra("corpusName", sAccent);
			    	Log.d(TAG,"corpusName sent via intent: " + sAccent);
			    	mainRecordingActivity.putExtra("respondentProfileKey", sRespProfileKeyString);
			    	mainRecordingActivity.putExtra("IMEI", IMEI);
			    	mainRecordingActivity.putExtra("sessionDateTimeStamp", (String) dateTimeStringNow);
			    	mainRecordingActivity.putExtra("sessionType", sessionTypeString);
			    	
			    	//Added 2011-06-21
			    	mainRecordingActivity.putExtra("accent", sAccent);
			    	mainRecordingActivity.putExtra("age", sAge);
			    	mainRecordingActivity.putExtra("gender", sGender);
			    	mainRecordingActivity.putExtra("location", sRecordingLocationArea);
			    	mainRecordingActivity.putExtra("environment", sEnvironment);
			    	mainRecordingActivity.putExtra("comments", sComments);
			    	
			    	startActivity(mainRecordingActivity);
			    	Log.d(TAG,"Killing RespondentProfile activity as not need and not want to go back.");
			    	finish();
				}//minimal
				else {
					if (sessionTypeSelected)
						if (!trainingCorpusAvailable) {
						warningDialog("Sorry, but there is not training corpus available for this accent. Please contact your fieldworker immediately.");
					}
				}
			}
		});
    }
	
	//Credit: ...
	public static String calcMD5(String pass) {
		//Note to do same in linux: $ echo -n What I want to Hash | md5sum  --text
		MessageDigest m = null;
		try {
			m = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		byte[] data = pass.getBytes(); 
		m.update(data,0,data.length);
		BigInteger i = new BigInteger(1,m.digest());
		return String.format("%1$032X", i);
	}
	
	public void shortCheers(CharSequence s) {
		Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
	}
	
	public boolean minimalInfoOK() {
		corpusFilename = sAccent + TRAINING_CORPUS_SUFFIX + CORPUS_FILENAME_EXTENSION;
		corpusFilenameFQ = CORPUS_INPUT_PATH + corpusFilename;
		Log.d(TAG,"corpusFilenameFQ = " + corpusFilenameFQ);
		trainingCorpusAvailable = checkTrainingCorpusAvailable(corpusFilenameFQ);
		return trainingCorpusAvailable;
	}
	
	private boolean checkTrainingCorpusAvailable(String corpusFilenameFQ) {
		if (ifCanReadAndWriteSDCARD()) {
			File fid = new File(corpusFilenameFQ);
			if (fid.exists()) {
				return true;
			}
			else {
				return false;
			}
		}
		return false;
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
	
	private void warningDialog(String message) {
		Log.d(TAG,"warningDialog was triggered.");
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("WARNING");
		builder.setMessage(message);
		builder.setCancelable(false);
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		                Log.d(TAG,"warningDialog was dismissed");
		           }
		       });
		AlertDialog alert = builder.create();
		alert.show();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		String name = null;
        String number = null;
        String notes = null;
        String idNumber = null;
        String mobile = null;
        String email = null;
        String profileName = null;
        String profileKey = null;
        String names = null;
        String surnames = null;
        String delims = null;
        String filename = null;
        
        if (resultCode == Activity.RESULT_OK) {
        	Log.d(TAG,"resultCode == Activity.RESULT_OK is not implemented.");
	    } //resultOK
	}
	
	@Override
	public void onBackPressed() {
	    // This will be called either automatically for you on 2.0
	    // or later, or by the code above on earlier versions of the
	    // platform.
		Log.i(TAG,"BACK key was detected by onBackPressed.");
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
	    	Log.i(TAG,"BACK key was detected by onKeyLongPress.");
	    	return true;
	    }
	    else if (keyCode == KeyEvent.KEYCODE_MENU) {
	    	Log.i(TAG,"MENU key was detected by onKeyLongPress.");
	        return true;
	    }
	    else if (keyCode == KeyEvent.KEYCODE_HOME) {
	    	Log.i(TAG,"HOME key was detected by onKeyLongPress.");
	        return true;
	    }
	    else if (keyCode == KeyEvent.KEYCODE_CALL) {
	    	Log.i(TAG,"CALL key was detected by onKeyLongPress.");
	        return true;
	    }
	    else if (keyCode == KeyEvent.KEYCODE_SEARCH) {
	    	Log.i(TAG,"SEARCH key was detected by onKeyLongPress.");
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
	    	Log.i(TAG,"BACK key was detected by onKeyUp.");
	    	
	    	if (!sureToQuit) {
		    	quitDialog("Are you 100% sure that you want to quit?");
		    	Log.d(TAG,"BACK key was detected by onKeyUp...still reading...");
		    	return true; //I handled it already
	    	}
	    	else if (sureToQuit) {
	    		//Note: finish() was already called, so just do nothing here till activity gets killed
	    		return true; //I handled it already
	    	}
	    }
	    else if (keyCode == KeyEvent.KEYCODE_MENU && event.isTracking()
	            && !event.isCanceled()) {
	    	Log.i(TAG,"MENU key was detected by onKeyUp.");
	        return true;
	    }
	    else if (keyCode == KeyEvent.KEYCODE_HOME && event.isTracking()
	            && !event.isCanceled()) {
	    	Log.i(TAG,"HOME key was detected by onKeyUp.");
	        return true;
	    }
	    else if (keyCode == KeyEvent.KEYCODE_CALL && event.isTracking()
	            && !event.isCanceled()) {
	    	Log.i(TAG,"CALL key was detected by onKeyUp.");
	        return true;
	    }
	    else if (keyCode == KeyEvent.KEYCODE_SEARCH && event.isTracking()
	            && !event.isCanceled()) {
	    	Log.i(TAG,"SEARCH key was detected by onKeyUp.");
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
	        Log.i(TAG,"HOME key was detected by onKeyDown.");
	        return COMPLETELY_HANDLED;
	    }
	    return super.onKeyDown(keyCode, event);
	}
	
	private void quitDialog(String message) {
		Log.d(TAG,"quitDialog was triggered.");
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("WARNING");
		builder.setMessage(message);
		builder.setCancelable(false);
		sureToQuit = false;
		builder.setPositiveButton("Do NOT quit!", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		                Log.d(TAG,"quitDialog was dismissed with 'Do NOT quit!' response.");
		                sureToQuit = false;
		           }
		       });
		builder.setNegativeButton("Quit", new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) {
	                Log.d(TAG,"quitDialog was dismissed with 'Quit!' response");
	                sureToQuit = true;
	                finish();
	           }
	       });
		AlertDialog alert = builder.create();
		alert.show();
	}
	
	private void LOG_E(String classTAG, String methodTAG, String message) {
		String s = classTAG + ":" + methodTAG + "::" + message;
		Log.e(classTAG, s);
		
		//Write to LOG file
		new CreateErrorLogThenDie(s);
		
		//Display msg if anyone will see it...might be booted by pending/new activity or dialog
		fatalErrorDialog(s);
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