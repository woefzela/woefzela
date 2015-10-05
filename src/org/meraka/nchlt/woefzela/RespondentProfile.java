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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.database.Cursor;
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
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class RespondentProfile extends Activity {
	
	//Housekeeping
	private static final String TAG = "RespondentProfile";
	private String methodTAG = "SaveRespondentProfile"; //Default
	private static final String email1 = "someone@gmail.com";
	private static final String contactPrefix = "NCHLT"; //get from resources later!
	private static final String NEWLINE = "\n";
	private static final int BAD_FILE_LINE_NUMBER = -1;
	private static final String CORPUS_STARTING_LINE_NUMBER = "0"; //string for human's
	private static final String CORPUS_FILENAME_EXTENSION = ".txt";
	private static final int MINIMUM_AGE_FIELD_INPUT_LENGTH = 2; //was 6 for id
	private static String TERMS_TEXT = null;
	
	//Logging
	private static final boolean LOG_V = true;
	private static final boolean LOG_D = true;
	private static final boolean LOG_I = true;
	private static final boolean LOG_W = true;
	private Logging log = new Logging(LOG_V, LOG_D, LOG_I, LOG_W); //Note: No LOG_E!
	
	//TODO: get all from resource
	private static final String programFoldername = "/Woefzela";
	private static final String profileFolderName = "/Profiles";
	private static final String profileFolderSubdir = "/Respondents";
	private static final String dataOutputFolderName = "/OutputData";
	private static final String corpusFolderName = "/CorpusInput";
	private static final String trackingFolderName = "/Tracking";
	private static final String statsOutputFolderName = "/OutputStats";
	
	private static final String CORPUS_INDEX_TRACKING_FILENAME_SUFFIX = "_lineToStartReadingFrom.dat"; //remove?
	private static String TRAINING_CORPUS_SUFFIX = null;
	
	private static boolean saveInContacts = false;
	private static boolean sendWithEmail = false;
	
	//UI elements
	private EditText eName;
	private EditText eSurname;
	private EditText eAge;
	private EditText eMobile;
	private EditText eEmail;
	
	private Spinner spAccent;
	private Spinner spGender;
	
	private Button bReadTerms;
	private CheckBox cTerms;
	
	private TextView tProfileKey;
	private TextView tStatusBar;
	
	//Action buttons
	private Button bLoadProfile;
	private Button bReset;
	private Button bNext;
	boolean readyForNextStage = false;
	boolean minimalFieldsOK = false;
	
	private boolean termsHasBeenRead = false;
	private boolean bTermsAccepted = false;
	private String sTermsString = null;
	
	private String notesString;
	private String sFWProfileKeyString = null;
	
	private boolean corpusFileAvailable = false;
	private String currentCorpusPosition = null;
	private boolean sureToQuit = false;
	
	//FILE HANDLING
	private String filename = null;
	private String fullFilenameWithPath = null;
	private File root = null;
	private String lineReadFromFile = null;
	private int lineNumberToStartReadingFrom = -1;
	String corpusFilename = null; //corpusFilename must cater for more than one file/version per corpus
	
	public static final int PICK_CONTACT = 1;
	public static final int PICK_FILE = 2;
	public static final int TERMS_AND_CONDITIONS = 3; 
	public static final int SEND_PROFILE_WITH_EMAIL = 5;
	
	public static final String TERM_STATUS_ACCEPTED = "Terms have been accepted.";
	public static final String TERM_STATUS_REJECTED = "Terms rejected!";
	public static final int ITEM_NOT_FOUND = -1;
	
	private static final boolean DUMMY_TRUE = true;
	private String appVersion = "0.0";
	
    /** Called when the activity is first created. */
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.respondent_profile);
        
        //Get extras passed with the calling intent
        Bundle extras = getIntent().getExtras();
        sFWProfileKeyString = extras.getString("fieldworkerProfileKey");
        
        //Get UI handles
        eName = (EditText) findViewById(R.id.eRespName);
        eSurname = (EditText) findViewById(R.id.eRespSurname);
        eAge = (EditText) findViewById(R.id.eRespAge);
        eMobile = (EditText) findViewById(R.id.eRespMobile);
        eEmail = (EditText) findViewById(R.id.eRespEmail);
        spAccent = (Spinner) findViewById(R.id.spAccent);
        spGender = (Spinner) findViewById(R.id.spGender);
        bReadTerms = (Button) findViewById(R.id.bReadTerms);
        cTerms = (CheckBox) findViewById(R.id.cTerms);
        
        tProfileKey = (TextView) findViewById(R.id.tProfileKey);
        tStatusBar = (TextView) findViewById(R.id.tStatusBar);
        
        bLoadProfile = (Button) findViewById(R.id.bLoadProfile);
    	bReset = (Button) findViewById(R.id.bReset);
    	bNext = (Button) findViewById(R.id.bNext);
    	
    	//SET UP INITIAL UI VALUES
    	cTerms.setFocusable(false);
    	cTerms.setChecked(false); //explicit
    	cTerms.setClickable(false);
    	
    	//OTHER INITIAL VALUES 
    	//TODO maybe move to onXXXX to ensure re-trigger
    	readyForNextStage = false;
    	minimalFieldsOK = false;
    	currentCorpusPosition = null;
    	
		//INIT RESOURCES
		Resources res = this.getResources();
		TERMS_TEXT = res.getString(R.string.TERMS_TEXT);
    	
    	//Register UI listeners
    	//spAccent
        class spAccentListener implements OnItemSelectedListener {

        	public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {        	
            	
            	String selectedAccent = parent.getItemAtPosition(pos).toString();

            	//Check if corpus available and set a flag
            	//TODO get corpus filename...newest? only one? present a list? Settings? etc.
            	corpusFilename = selectedAccent; //TODO at least for now
            	
        	    if (ifCorpusExists(corpusFilename)) { //TODO any for now, later specific
        	    	Log.d(TAG,"ifCorpusExists(corpusFilename) was true.");
    				corpusFileAvailable = true;
        	    }
            	else {
            		corpusFileAvailable = false;
            	}
            }

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub
				Toast.makeText(getApplicationContext(), "No accent was selected. Please try again.\n(Current: " + (String) (spAccent.getItemAtPosition(spAccent.getSelectedItemPosition())) + ")", Toast.LENGTH_SHORT).show();
			}
        }
        spAccent.setOnItemSelectedListener(new spAccentListener()); //could be done as anon class as well
    	
    	//Only triggers when touched not when changed programmatically. Quite sure?
    	cTerms.setOnCheckedChangeListener(new OnCheckedChangeListener() {
    	    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    	        if (isChecked) {
    	        	bNext.setText("Next...");
    	        	bTermsAccepted =  true;
    	        	sTermsString = TERM_STATUS_ACCEPTED;
    	        	Log.d(TAG,"onCheckedChanged is checked");
    	        }
    	        else {
    	        	bNext.setText("Quit!");
    	        	bTermsAccepted = false;
    	        	sTermsString = TERM_STATUS_REJECTED;
    	        	Log.d(TAG,"onCheckedChanged is NOT checked");
    	        }
    	    }
    	});
    	
    	bReadTerms.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				termsHasBeenRead = true; //safer check?
				cTerms.setFocusable(true);
				cTerms.setClickable(true);
				termsDialog();
			}
		});
    	
    	bLoadProfile.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				if (saveInContacts) {
					Intent intent = new Intent(Intent.ACTION_PICK, People.CONTENT_URI);
					startActivityForResult(intent, PICK_CONTACT);
				}
				
				Intent intent = new Intent("org.openintents.action.PICK_FILE");
				Uri path =  Uri.parse("file:///sdcard/" + programFoldername + "/" + profileFolderName + "/" + profileFolderSubdir); // + "/");

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
				//TODO make this section more elegant and also not fetch twice for checking and for saving

				//Check values in fields -in sequence of display - and warn in same sequence
				minimalFieldsOK = checkMinimalRespondentProfileFields();
				
				//TODO merge with next section, but large logic here easier to read
				if (bTermsAccepted & corpusFileAvailable & minimalFieldsOK) {
					readyForNextStage = true;
				}
				else {
					readyForNextStage = false;
					if (!bTermsAccepted) {
			    		Log.i(TAG, "Terms and conditions were not accepted, thus not navigations to next activity and can only quit.");
			    		minimalFieldsOK = false; //TODO Make more robust later
						warningDialog("Terms and conditions have not been accepted. If this is your final decision then please press OK and then the 'BACK' key to close the application as you have chosen not to proceed.");
					}
					else if (!corpusFileAvailable) { //not case or separate if's as want sequential-effect. Go from top of UI
			    		Log.i(TAG, "Corpus is not available for this accent, thus not navigations to next activity (but still store profile as-is.");
						warningDialog("The corpus for your accent is currently not available on this device. Please notify your fieldworker immediately.");
					}
				}

				if (minimalFieldsOK) { //save profile at least
					
					//Store profile regardless, as long as minimal info present TODO add some prechecks.
			    	String sName = (eName.getText()).toString();
			    	String sSurname = (eSurname.getText()).toString();
			    	String sAge = (eAge.getText()).toString();
			    	String sMobile = (eMobile.getText()).toString();
			    	String sEmail = (eEmail.getText()).toString();
			    	String sAccent = (String) (spAccent.getItemAtPosition(spAccent.getSelectedItemPosition()));
			    	String sGender = (String) (spGender.getItemAtPosition(spGender.getSelectedItemPosition()));
			    	//create unique profile hash
			    	String profileKey = calcMD5(sName + sSurname + sAge); //unique key
			    	Log.d(TAG,"profileKey = " + profileKey);
				    	
			    	//Commit profile to a file
			    	appVersion = getVersionInfo();
			    	SaveRespondentProfile p1 = new SaveRespondentProfile(getApplicationContext(), sName, sSurname, sAge, sMobile, sEmail, sAccent, sGender, sTermsString, profileKey, appVersion);
					filename = p1.getFilename();
					fullFilenameWithPath = p1.getFullFilenameWithPath();
			    	Log.i(TAG,"Profile filename = " + filename);
			    	
			    	notesString = "" + sAge + "\n" + "" + filename + "\n" +  "" + profileKey + "\n";
			    	
			    	if (saveInContacts) {
						//Insert 'profile' into contacts for easy sync and access
						Log.d(TAG,"Inserting into contacts: " + sName);
						
						//Create a new record
						ContentValues values = new ContentValues();
						values.put(People.NAME, contactPrefix + "-" + sName + " " + sSurname);
						values.put(People.STARRED, 1); //1=favourite			
						values.put(People.NOTES, notesString); //Notes are stored in main record!!
						Uri uri = getContentResolver().insert(People.CONTENT_URI, values);
						
						//Add detail to new record
						Uri phoneUri = Uri.withAppendedPath(uri, People.Phones.CONTENT_DIRECTORY); //Get specific uri for phone number
						values.clear();
						values.put(People.Phones.TYPE, People.Phones.TYPE_MOBILE);
						values.put(People.Phones.NUMBER, sMobile);
						getContentResolver().insert(phoneUri, values); //insert it
						
						Uri emailUri = Uri.withAppendedPath(uri, People.ContactMethods.CONTENT_DIRECTORY);
						values.clear();
						values.put(People.ContactMethods.KIND, Contacts.KIND_EMAIL);
						values.put(People.ContactMethods.DATA, sEmail);
						values.put(People.ContactMethods.TYPE, People.ContactMethods.TYPE_WORK);
						getContentResolver().insert(emailUri, values);  
			    	}
	
			    	if (readyForNextStage) {

			    		Log.i(TAG, "Terms was accepted and corpus is available and minimal field info OK, thus navigating to next activity, and killing this one.");

				    	if (sendWithEmail) {
							Intent sendIntent = new Intent(Intent.ACTION_SEND);
							sendIntent.setType("text/plain");
							Log.d(TAG,"fullFilenameWithPath = " + fullFilenameWithPath);
							String fPath = "file://" + fullFilenameWithPath;
							Log.d(TAG,"fPath = " + fPath);
							sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(fPath));
							sendIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{email1});
							sendIntent.putExtra(Intent.EXTRA_SUBJECT, "MerakaWoefzela - Respondent Profile"); 				
							sendIntent.putExtra(Intent.EXTRA_TEXT, "See attachment for Respondent Profile.\n\nMeraka HLT Team"); 
							sendIntent.setType("message/rfc822"); 
							//will block till return??
							startActivityForResult(Intent.createChooser(sendIntent, "Email:"), SEND_PROFILE_WITH_EMAIL);
							Log.d(TAG, "Do you ever return here?");
				    	}
				    	else {
				    		//Call next activity, with extras
					    	Intent editSessionInfo = new Intent(RespondentProfile.this, SessionInfo.class);
					    	editSessionInfo.putExtra("fieldworkerProfileKey",sFWProfileKeyString);
					    	editSessionInfo.putExtra("respondentProfileKey", profileKey);
					    	editSessionInfo.putExtra("corpusName", sAccent);
					    	
					    	//Added 2011-06-21:
					    	//- Age (derived) - eRespID
					    	//- Spoken accent - spAccent - already as corpusName
					    	//- Gender - spGender
					    	editSessionInfo.putExtra("age", sAge);
					    	editSessionInfo.putExtra("gender", sGender);
					    	
					    	startActivity(editSessionInfo);
					    	finish();
				    	}
				    Log.d(TAG,"reached end of ready for next stage...");
					}//readyForNextStage
					else {//NOT readyForNextStage
						Log.d(TAG, "readyForNextButton = false, thus warning user with dialog-type box.");
						//TODO give more specific info
					}
				}//minimalValueOK
			}//onClick
		});
    }//onCreate
	
	//Credit: http://snippets.dzone.com/posts/show/3686
	public static String calcMD5(String pass) {
		//Note: To do same in linux: $ echo -n What I want to Hash | md5sum  --text
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
		
		super.onActivityResult(requestCode, resultCode, data);
		
		String methodTAG = "onActivityResult";
		
		String name = null;
        String number = null;
        String notes = null;
        String age = null;
        String mobile = null;
        String email = null;
        String profileName = null;
        String profileKey = null;
        String names = null;
        String surnames = null;
        String accent = null;
        String gender = null;
        String delims = null;
        String filename = null;
        
        switch (requestCode) {
            	
			case (PICK_CONTACT) : {
				
				Log.d(TAG, "PICK_CONTACT" + resultCode);
				
				if (resultCode == Activity.RESULT_OK) {
		
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
							LOG_E(TAG, methodTAG, "Error in navigating contacts.");
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
		            String[] tokens = name.split(delims,2); //fix constant
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
		            			age = tokens[i];
		            	        break;
		            	  case 1: 
		            		  profileName = tokens[i];
		            	        break;
		            	  case 2: 
		            		  profileKey = tokens[i];
		            	        break;
		            	  default:
		            	        Log.e(TAG,"too many fields");
		            	}
		            }
		            Log.i(TAG,"age: " + tokens[0]);
		            Log.i(TAG,"profileName: " + tokens[1]);
		            Log.i(TAG,"profileKey: " + tokens[2]);
		            
		            //Update ui fields
		        	eName.setText(names);
		        	eSurname.setText(surnames);
		        	eAge.setText(age);
		        	eMobile.setText(number);
		        	eEmail.setText(email);
			    	tProfileKey.setText(profileKey);
			    	Log.i(TAG,"end of PICK_CONTACT");
				}
			    break;
			}
			
			case (PICK_FILE) : {
				
				Log.d(TAG, "PICK_FILE" + resultCode);
	
				if (resultCode == Activity.RESULT_OK) {
				  	//TODO
					Log.i(TAG,"Picked a file...");
					Uri filenameUri = data.getData();
					filename = filenameUri.toString();
					
		    		if (filename.endsWith(".txt")) {	
		    			Log.i(TAG,"Loading file: " + filename);
		    			
						LoadRespondentProfile p1 = new LoadRespondentProfile(getApplicationContext(), filenameUri);
						
							//Unpack file
		    				names = p1.getName();
		    				surnames = p1.getSurname();
		    				age = p1.getAge();
		    				profileKey = p1.getProfileKey();
		    				mobile = p1.getMobile();
		    				email = p1.getEmailAddr();
		    				accent = p1.getAccent();
		    				gender = p1.getGender();
		    				sTermsString = p1.getTermStatus();
		 
		    				if (sTermsString.equals(TERM_STATUS_ACCEPTED)) {
		    					bTermsAccepted = true;
		    					termsHasBeenRead = true;
		    					cTerms.setFocusable(true);
		    					cTerms.setClickable(true);
		    					cTerms.setChecked(true);
		    					Log.d(TAG,"cTerms.setChecked(true) has occured.");
						}
						else {
							bTermsAccepted = false;
							termsHasBeenRead = true;
							cTerms.setFocusable(true);
							cTerms.setClickable(true);
							cTerms.setChecked(false);
							Log.d(TAG,"cTerms.setChecked(false) has occured.");
						}
		    			
		    			//Update UI fields
				    	eName.setText(names);
				    	eSurname.setText(surnames);
				    	eAge.setText(age);
				    	eMobile.setText(mobile);
				    	eEmail.setText(email);
				    	
				    	int pos1 = getStringPosInSpinner(spAccent, accent);
				    	if (pos1 == ITEM_NOT_FOUND) {
				    		//TODO: make error dialog and bomb
				    		String msg = "FATAL ERROR: Item stored in file was not found in spinner array. Stopped.";
				    		Log.e(TAG, msg);
				    		finish(); //call some error finish to pass to calling activity if needed
				    	}
				    	
				    	spAccent.setSelection(pos1, true); //0, 1, 2...
				    	
				    	int pos2 = getStringPosInSpinner(spGender, gender);
				    	if (pos2 == ITEM_NOT_FOUND) {
				    		//TODO: make error dialog and bomb
				    		String msg = "FATAL ERROR: Item stored in file was not found in spinner array. Stopped.";
				    		Log.e(TAG, msg);
				    		finish(); //call some error finish to pass to calling activity if needed
				    	}
				    	
				    	spGender.setSelection(pos2, true); //0, 1, 2...
				    	
				    	tProfileKey.setText(profileKey);
				    	
				    	//trace
				    	Log.d(TAG,">> Imported from profile <<");
				    	Log.d(TAG,"accent = " + accent);
				    	Log.d(TAG,"gender = " + gender);
				    	Log.d(TAG,"sTermsString = " + sTermsString);
		    		}//ends with txt
		    		Log.i(TAG,"end of PICK_FILE");
				}	
		    	break;
			}
				
			case (SEND_PROFILE_WITH_EMAIL) : {
				
				Log.d(TAG, "SEND_PROFILE_WITH_EMAIL" + resultCode);
				
				if (resultCode == 0) {	
					
					Log.d(TAG,"Returned from sending email");
			    	String sAccent = (String) (spAccent.getItemAtPosition(spAccent.getSelectedItemPosition()));
		    		//Call next activity, with extras
			    	Intent editSessionInfo = new Intent(RespondentProfile.this, SessionInfo.class);
			    	editSessionInfo.putExtra("fieldworkerProfileKey",sFWProfileKeyString);
			    	editSessionInfo.putExtra("respondentProfileKey", profileKey);
			    	editSessionInfo.putExtra("corpusName", sAccent);
			    	startActivity(editSessionInfo);
			    	Log.d(TAG,"end of SEND_PROFILE_WITH_EMAIL");
					finish();
				}
				break;
			}
			
			default:
		        Log.e(TAG,"too many fields");
		        break;
	    }//switch
	}
	
	//Maybe move to own class AND maybe more elegant way to do
	private int getStringPosInSpinner(Spinner sp, String pattern){
		
		String s = null;
		
    	int count = sp.getCount();
    	Log.d(TAG,"count = " + count);
    	
    	for (int i = 0; i < count; i++) {
    		s = (String) (sp.getItemAtPosition(i));
    		Log.d(TAG, "item [" + i + "] = " + s);
    		if (s.equals(pattern)) {
    			Log.d(TAG, "Found pattern at: " + i);
    			return i;
    		}
    	}
		return ITEM_NOT_FOUND;
	}
	
	private boolean ifCorpusExists(String corpusFilename) {
		//TODO make smarter
		Log.d(TAG,"corpusFilename = " + corpusFilename);
		//INIT RESOURCES
		
		Resources res = getApplicationContext().getResources(); 
		TRAINING_CORPUS_SUFFIX = res.getString(R.string.TRAINING_CORPUS_SUFFIX);
		//Check training corpus
		File fidTrain = new File("/sdcard" + programFoldername + corpusFolderName + "/" + corpusFilename + TRAINING_CORPUS_SUFFIX + CORPUS_FILENAME_EXTENSION);
		File fidRecording = new File("/sdcard" + programFoldername + corpusFolderName + "/" + corpusFilename + CORPUS_FILENAME_EXTENSION);
		
		if (fidTrain.exists()) {
			Log.d(TAG,"File: " + fidTrain.getAbsolutePath() + " does exist.");
			if (fidRecording.exists()) {
				Log.d(TAG,"File: " + fidRecording.getAbsolutePath() + " does exist.");
				return true;
			}
			warningDialog("Sorry, but there is not recording corpus available for this accent. Please contact your fieldworker immediately.");
			return false;
		}
		else {
			warningDialog("Sorry, but there is not training corpus available for this accent. Please contact your fieldworker immediately.");
			Log.d(TAG,"SYSTEM ERROR: Either the training or the recording corpus does not exist for this accent.");
			return false;
		}
	}
	
	private int getLineNumberToStartReadingFrom(String filename) {
		//Includes the subdirs and write 0 if not exist i.e. new tracking file
		
		int lineNumber = BAD_FILE_LINE_NUMBER;
		
		FileReader fRead = null;
		FileWriter fWrite = null;
		
		if (ifCanReadAndWriteSDCARD()) {
			Log.d(TAG, "Can read/write to SDCARD.");
			String corpusTrackingFilename = filename + CORPUS_INDEX_TRACKING_FILENAME_SUFFIX;
			Log.d(TAG, "corpusTrackingFilename = " + corpusTrackingFilename);
			
			root = Environment.getExternalStorageDirectory();
			Log.i(TAG,"root = " + root.toString());
			
			//Create folders if not exist
		    new File("/sdcard" + programFoldername).mkdir(); //error checking?? TODO move earlier in app
		    new File("/sdcard" + programFoldername + trackingFolderName).mkdir(); //error checking??
		    //NOTE: line below is unrelated, but easier for now to do here
		    new File("/sdcard" + programFoldername + corpusFolderName).mkdir(); //error checking??
		        
		    File fid = new File("/sdcard" + programFoldername + trackingFolderName + "/" + corpusTrackingFilename);
		    
		    if (root.canRead()) {
				try {
					fRead = new FileReader(fid);
					BufferedReader in = new BufferedReader(fRead);
					
					try {
						lineReadFromFile = in.readLine();
						in.close();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					
					Log.i(TAG,"lineReadFromFile as String = " + lineReadFromFile);
					
					try {
						lineNumber = Integer.parseInt(lineReadFromFile);
						Log.d(TAG,"lineReadFromFile (as int) = " +  lineNumber);
						
						return lineNumber;
					} 
					catch (NumberFormatException e) {
						// TODO alert about file format error
						Log.e(TAG,"FATAL ERROR: Line read from file could not be parsed into an integer!");
						e.printStackTrace();
					}
				} 
				catch (FileNotFoundException e) { //i.e first time on this sdcard. create and write 0 in
					Log.i(TAG,"The expected corpusTrackingFile was not found...trying to create one");
				    if (root.canWrite()) {
						try {
							fWrite = new FileWriter(fid);
							BufferedWriter out = new BufferedWriter(fWrite);
			            	try {
								out.write(CORPUS_STARTING_LINE_NUMBER); out.write(NEWLINE); //must be 0 to start
								out.close();
								currentCorpusPosition = CORPUS_STARTING_LINE_NUMBER; //if bomb now, start here
								return 0;
							} 
			            	catch (IOException e3) {
								// TODO Auto-generated catch block
								e3.printStackTrace();
							}
						} 
						catch (IOException e2) {
							// TODO Auto-generated catch block
							e2.printStackTrace();
						}
				    }
				    else {
				    	//TODO say could no write and exit
				    	Log.e(TAG, "FATAL ERROR: Could not write file.");
				    }
				}
		    }
		    else {
		    	//TODO say could no read and exit
		    	Log.e(TAG, "FATAL ERROR: Could not read file.");
		    }
		}
		else {
			Log.e(TAG,"SDCARD: Not able to either read and/or write from/to SDCARD.");
		}
		return BAD_FILE_LINE_NUMBER;
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
	
	private boolean checkMinimalRespondentProfileFields() {
		//Fetch values
    	String sName = (eName.getText()).toString();
    	String sSurname = (eSurname.getText()).toString();
    	String sAge = (eAge.getText()).toString();
    	String sMobile = (eMobile.getText()).toString();
    	String sEmail = (eEmail.getText()).toString();
    	
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
		else if ((sAge == null) || (sAge.length() < MINIMUM_AGE_FIELD_INPUT_LENGTH)) { //&& for booleans. & for bitwise
			Log.i(TAG,"sAge field is empty or too short.");
			warningDialog("Age field is empty or too short.");
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
	
	private void termsDialog() {
		Log.d(TAG,"warningDialog was triggered.");
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("TERMS AND CONDITIONS");
		builder.setMessage(TERMS_TEXT);
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
	
}