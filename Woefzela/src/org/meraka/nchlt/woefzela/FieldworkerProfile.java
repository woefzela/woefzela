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
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Contacts;
import android.provider.Contacts.People;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class FieldworkerProfile extends Activity {
	
	//Housekeeping
	private static final String TAG = "FieldworkerProfile";
	private static final String email1 = "someone@gmail.com";
	private static final String contactPrefix = "NCHLT"; //get from resources later!
	private static final int MINIMUM_ID_FIELD_INPUT_LENGTH = 6; //was 13
	
	//Externalized values
	private String PROGRAM_FOLDER_NAME = null;
	private String CORPUS_FOLDER_NAME = null;
	private String PROFILE_FOLDER_NAME = null;
	private String DATA_OUTPUT_FOLDER_NAME = null;
	private String TRACKING_FOLDER_NAME = null;
	private String PROFILE_FOLDER_FIELDWORKERS = null;
	private String PROFILE_FOLDER_RESPONDENTS = null;
	private String PROFILE_FOLDER_SESSIONS = null;
	
	private static boolean saveInContacts = false;
	private static boolean sendWithEmail = false;
	
	private String methodTAG = "metodTAG"; //Default
	
	//UI elements
	private EditText eFWName;
	private EditText eFWSurname;
	private EditText eFWID;
	private EditText eFWMobile;
	private EditText eFWEmail;
	private TextView tProfileID;
	private TextView tStatusBar;
	
	//Action buttons
	private Button bLoadProfile;
	private Button bReset;
	private Button bNext;
	boolean readyForNextStage = false;
	boolean minimalFieldsOK = false;
	
	private String notesString;
	private String filename = null;
	private boolean sureToQuit = false;
	
	public static final int PICK_CONTACT = 1;
	public static final int PICK_FILE = 2;
	
    /** Called when the activity is first created. */
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fieldworker_profile);
        
        //Get UI handles
        eFWName = (EditText) findViewById(R.id.eFWName);
        eFWSurname = (EditText) findViewById(R.id.eFWSurname);
        eFWID = (EditText) findViewById(R.id.eFWID);
        eFWMobile = (EditText) findViewById(R.id.eFWMobile);
        eFWEmail = (EditText) findViewById(R.id.eFWEmail);
        
        tProfileID = (TextView) findViewById(R.id.tProfileID);
        tStatusBar= (TextView) findViewById(R.id.tStatusBar);
        
        bLoadProfile = (Button) findViewById(R.id.bLoadProfile);
    	bReset = (Button) findViewById(R.id.bReset);
    	bNext = (Button) findViewById(R.id.bNext);
    	
    	//Get externalized values
    	Resources res = getResources(); //Get instance of a resource and not just the resource ID (e.g. R.id.xxxx)
    	PROGRAM_FOLDER_NAME = res.getString(R.string.PROGRAM_FOLDER_NAME);
    	CORPUS_FOLDER_NAME = res.getString(R.string.CORPUS_FOLDER_NAME);
    	PROFILE_FOLDER_NAME = res.getString(R.string.PROFILE_FOLDER_NAME);
    	DATA_OUTPUT_FOLDER_NAME = res.getString(R.string.DATA_OUTPUT_FOLDER_NAME);
    	TRACKING_FOLDER_NAME = res.getString(R.string.TRACKING_FOLDER_NAME);
    	PROFILE_FOLDER_FIELDWORKERS = res.getString(R.string.PROFILE_FOLDER_FIELDWORKERS);
    	PROFILE_FOLDER_RESPONDENTS = res.getString(R.string.PROFILE_FOLDER_RESPONDENTS);
    	PROFILE_FOLDER_SESSIONS = res.getString(R.string.PROFILE_FOLDER_SESSIONS);
    	
    	//Set up initial UI values
    	readyForNextStage = false;
    	minimalFieldsOK = false;
    	
    	//Create needed folders
    	createAllRequiredFolders();
    	
    	//Register UI listeners
    	bLoadProfile.setOnClickListener(new View.OnClickListener() {
    		
    		private String methodTAG = "bLoadProfile.setOnClickListener";
			
			@Override
			public void onClick(View v) {
				
				if (saveInContacts) {
					Intent intent = new Intent(Intent.ACTION_PICK, People.CONTENT_URI);
					startActivityForResult(intent, PICK_CONTACT);
				}
				
				Intent intent = new Intent("org.openintents.action.PICK_FILE");
				
				Uri path =  Uri.parse("file:///sdcard/" + PROGRAM_FOLDER_NAME + "/" + PROFILE_FOLDER_NAME + "/" + PROFILE_FOLDER_FIELDWORKERS); // + "/");
				
				intent.setData(path);
				intent.putExtra("org.openintents.extra.TITLE", "Pick a profile");
				intent.putExtra("org.openintents.extra.BUTTON_TEXT", "Go");
				//TODO: check installed before call
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
				
			minimalFieldsOK = checkMinimalFieldworkerProfileFields();
		
			boolean specialChecks = true;
			
			if (specialChecks & minimalFieldsOK) {
				readyForNextStage = true;
			}
			else {
				readyForNextStage = false;
			}
				
			if (minimalFieldsOK) { //save profile at least
		    	String sFWName = (eFWName.getText()).toString();
		    	String sFWSurname = (eFWSurname.getText()).toString();
		    	String sFWID = (eFWID.getText()).toString();
		    	String sFWMobile = (eFWMobile.getText()).toString();
		    	String sFWEmail = (eFWEmail.getText()).toString();

		    	//create unique profile hash
		    	String profileKey = calcMD5(sFWName + sFWSurname + sFWID); //unique key
		    	Log.d(TAG,"profileKey = " + profileKey);
		    	
		    	//Commit profile to a file
		    	SaveFieldworkerProfile p1 = new SaveFieldworkerProfile(getApplicationContext(), sFWName, sFWSurname, sFWID, sFWMobile, sFWEmail, profileKey);
				filename = p1.getFilename();
		    	Log.i(TAG,"Profile filename = " + filename);
		    	
		    	notesString = "" + sFWID + "\n" + "" + filename + "\n" +  "" + profileKey + "\n";
		    	
		    	if (saveInContacts) {
					//Insert 'profile' into contacts for easy sync and access
					Log.d(TAG,"Inserting into contacts: " + sFWName);
					
					//Create a new record
					ContentValues values = new ContentValues();
					values.put(People.NAME, contactPrefix + "-" + sFWName + " " +sFWSurname);
					values.put(People.STARRED, 1); //1=favourite			
					values.put(People.NOTES, notesString); //Notes are stored in main record!!
					Uri uri = getContentResolver().insert(People.CONTENT_URI, values);
					
					//Add detail to new record
					Uri phoneUri = Uri.withAppendedPath(uri, People.Phones.CONTENT_DIRECTORY); //Get specific uri for phone number
					values.clear();
					values.put(People.Phones.TYPE, People.Phones.TYPE_MOBILE);
					values.put(People.Phones.NUMBER, sFWMobile);
					getContentResolver().insert(phoneUri, values); //insert it
					
					Uri emailUri = Uri.withAppendedPath(uri, People.ContactMethods.CONTENT_DIRECTORY);
					values.clear();
					values.put(People.ContactMethods.KIND, Contacts.KIND_EMAIL);
					values.put(People.ContactMethods.DATA, sFWEmail);
					values.put(People.ContactMethods.TYPE, People.ContactMethods.TYPE_WORK);
					getContentResolver().insert(emailUri, values);  
		    	}

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
			    if (readyForNextStage) {	
			    	//Call next activity
			    	Intent editRespondentProfile = new Intent(FieldworkerProfile.this, RespondentProfile.class);
			    	editRespondentProfile.putExtra("fieldworkerProfileKey", profileKey);
			    	startActivity(editRespondentProfile);
			    	Log.d(TAG,"Killing FieldworkerProfile activity as not need and not want to go back.");
			    	finish();
			    	
					}
				}//next stage
			}//minimalOK
		});
    }
	
	private boolean ifCanReadAndWriteSDCARD() {
		//TODO maybe move to own class and instantiate an object

		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
		    // We can read and write the media
		    return true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
		    // We can only read the media
		    return false;
		} else {
		    //Something else is wrong. It may be one of many other states, but all we need
		    //to know is we can neither read nor write
		    return false;
		}
	}
	
	private void createAllRequiredFolders() {
		if (ifCanReadAndWriteSDCARD()) {
				
			    //Cannot create in one go! Pity.
				//TODO Maybe add error checking...
			    new File("/sdcard" + PROGRAM_FOLDER_NAME).mkdir();
				
				new File("/sdcard" + PROGRAM_FOLDER_NAME + CORPUS_FOLDER_NAME).mkdir();
				new File("/sdcard" + PROGRAM_FOLDER_NAME + DATA_OUTPUT_FOLDER_NAME).mkdir();
				new File("/sdcard" + PROGRAM_FOLDER_NAME + TRACKING_FOLDER_NAME).mkdir();
				new File("/sdcard" + PROGRAM_FOLDER_NAME + PROFILE_FOLDER_NAME).mkdir();
				
				new File("/sdcard" + PROGRAM_FOLDER_NAME + PROFILE_FOLDER_NAME + PROFILE_FOLDER_FIELDWORKERS).mkdir();
				new File("/sdcard" + PROGRAM_FOLDER_NAME + PROFILE_FOLDER_NAME + PROFILE_FOLDER_RESPONDENTS).mkdir();
				new File("/sdcard" + PROGRAM_FOLDER_NAME + PROFILE_FOLDER_NAME + PROFILE_FOLDER_SESSIONS).mkdir();
		}
		else {
			//TODO Show pop-up message for user
			//Note: Cannot write an error log on SDcard as well :-)
			Log.e(TAG, "Could not write to SDcard!");
		}
	}
	
	public static String calcMD5(String pass) {
		//Note to do same in linux: echo -n What I want to Hash | md5sum  --text
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
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		String methodTAG = "onActivityResult";
		
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
        	
            	switch (requestCode) {
        		case PICK_CONTACT: {

	            	Uri contactData = data.getData();

	            	//TODO Variables just for explanation: remove later
	            	//Explanation: which columns to return. null=all columns
	            	String[] projection = { People._ID, People.NAME, People.NUMBER, People.NOTES}; //NB: Only these can then be 'got'
	            	
	            	//Explanation: null= SQL WHERE. Which rows to return. null=all rows for the given URI.
	            	String selection = People.NAME + " LIKE ?"; //People.NAME + " = ? AND " + People.NUMBER + " = ?"; 
	            	
	            	//Explanation: You may include ?s in selection, which will be replaced by the values from selectionArgs, in the order that they appear in the selection. The values will be bound as Strings.
	            	final String pattern = "%aaaNCHLT-%"; // contains an "A"
	            	String[] selectionArgs = new String[] {pattern}; //{"aaaNCHLT-Asa Nbhh","0097654326"};
	            	
	            	//Explanation: SQL ORDER BY
	            	String sortOrder = null;
	                Cursor contact = getContentResolver().query(contactData, projection, selection, selectionArgs, sortOrder);
	                if (contact.moveToFirst()) {
			              name = contact.getString(contact.getColumnIndexOrThrow(People.NAME)); //name(s) and surname(s)
			              number = contact.getString(contact.getColumnIndexOrThrow(People.NUMBER)); //mobile
			              try {
							notes = contact.getString(contact.getColumnIndexOrThrow(People.NOTES)); //id, filename, profilekey
						} catch (IllegalArgumentException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							LOG_E(TAG, methodTAG,"Something went wrong in navigating contacts.");
						}
		            }
	                else {
	                	//TODO do something
	                }
	                
	                Uri emailUri = Uri.withAppendedPath(data.getData(), People.ContactMethods.CONTENT_DIRECTORY);
	                Cursor curEmail = getContentResolver().query(emailUri, null, null, null, null);
	                curEmail.moveToFirst();
	                email = curEmail.getString(curEmail.getColumnIndexOrThrow("data"));

	                Log.i(TAG,"email =  " + email);
	                Toast.makeText(this, "You have selected: " + " " + name + "\n" +number+ "\n" + notes + "\nEmail:" + email , Toast.LENGTH_LONG).show();
	                
	                //Split names and surnames on space
	                names = null;
	                surnames = null;
	                delims = "[ ]+";
	                String[] tokens = name.split(delims,2); //TODO fix constant
	                for (int i = 0; i < tokens.length; i++) {
	                	//TODO thorough checking
	                	switch (i) {
	                		case 0:
	                			names = tokens[i];
	                	        break;
	                		case 1: 
	                		  	surnames = tokens[i];
	                	        break;
	                		default:
	                	        Log.e(TAG,"too many fields");
	                	}
	                }
                    Log.i(TAG,"names: " + tokens[0]);
                    Log.i(TAG,"surnames: " + tokens[1]);
	                
	                //Split Notes field up
	                delims = "[\n]+";
	                tokens = notes.split(delims,3); //fix constant
	                for (int i = 0; i < tokens.length; i++) {
	                	//TODO thorough checking
	                	switch (i) {
	                		case 0:
	                			idNumber = tokens[i];
	                	        break;
	                	  case 1: 
	                		  profileName = tokens[i];
	                	        break;
	                	  case 2: 
	                		  profileKey = tokens[i];
	                	        break;
	                	  default:
	                		  LOG_E(TAG, methodTAG, "Too many columns in corpus.");
	                	}
	                }
                    Log.i(TAG,"idNumber: " + tokens[0]);
                    Log.i(TAG,"profileName: " + tokens[1]);
                    Log.i(TAG,"profileKey: " + tokens[2]);
	                
	                //update ui fields
			    	eFWName.setText(names);
			    	eFWSurname.setText(surnames);
			    	eFWID.setText(idNumber);
			    	eFWMobile.setText(number);
			    	eFWEmail.setText(email);
			    	tProfileID.setText(profileKey);
			    	
			    	break;
            	} //case 0
        		
        		case PICK_FILE: 
        		  	//TODO
        			Log.i(TAG,"Picked a file...");
        			Uri filenameUri = data.getData();
        			filename = filenameUri.toString();
        			
	        		if (filename.endsWith(".txt")) {
	        			Log.i(TAG,"Loading file: " + filename);
	        			
	    				LoadFieldworkerProfile p1 = new LoadFieldworkerProfile(getApplicationContext(), filenameUri);
	    				
	    				//Unpack file
	    				names = p1.getName();
	    				surnames = p1.getSurname();
	    				idNumber = p1.getIdNumber();
	    				profileKey = p1.getProfileKey();
	    				mobile = p1.getMobile();
	    				email = p1.getEmailAddr();
	        			
	        			//Update UI fields
				    	eFWName.setText(names);
				    	eFWSurname.setText(surnames);
				    	eFWID.setText(idNumber);
				    	eFWMobile.setText(mobile);
				    	eFWEmail.setText(email);
				    	tProfileID.setText(profileKey);
	        		}
        	        break;
        	        
        		default:
        			LOG_E(TAG, methodTAG, "Returned from unknown activity.");
            	
	        } //resultOK
	    }
	}
	
	private boolean checkMinimalFieldworkerProfileFields() {
		//Fetch values
    	String sName = (eFWName.getText()).toString();
    	String sSurname = (eFWSurname.getText()).toString();
    	String sIDNumber = (eFWID.getText()).toString();
    	String sMobile = (eFWMobile.getText()).toString();
    	String sEmail = (eFWEmail.getText()).toString();
    	
		if ((sName == null) || (sName.length() == 0)) {
			Log.i(TAG,"Name field is empty.");
			warningDialog("Name field is empty.");
			return false;
		}
		else if ((sSurname == null) || (sSurname.length() == 0)) { //&& for booleans. & for bitwise
			Log.i(TAG,"Surname field is empty.");
			warningDialog("Surname field is empty.");
			return false;
		}
		else if ((sIDNumber == null) || (sIDNumber.length() < MINIMUM_ID_FIELD_INPUT_LENGTH)) { //&& for booleans. & for bitwise
			Log.i(TAG,"sIDNumber field is empty or too short.");
			warningDialog("IDNumber field is empty or too short.");
			return false;
		}
		else {
			Log.d(TAG, "minimalFieldsOK = true");
			return true;
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
	        // cancelled flag for the following up event.
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
		
		//Trying to hog screen, but may not succeed if pending or new activities launched
		
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
