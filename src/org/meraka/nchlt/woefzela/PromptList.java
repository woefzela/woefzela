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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Random;
import android.content.Context;
import android.content.res.Resources;
import android.os.Environment;
import android.util.Log;

public class PromptList {
	
	//HOUSEKEEPING
	private static final String TAG = "PromptList";
	
	//SWITCHES
	private static final boolean RANDOM_STARTPOINT = true; 	//Randomize start of batch or always start batch at zero
															//WARNING: if the starting point is not random only the first-N prompts will ever be displayed regardless of the prompts in the input corpus!
	private static final boolean RANDOMIZE = false; //Randomize prompts FROM within batch as presented.
	private static final boolean LOG_V = false;
	private static final boolean LOG_D = false;
	private static final boolean LOG_I = false;
	private static final boolean LOG_W = false;
	
	//CONSTANTS
	private String methodTAG = "methodTAG"; //Default
	
	private static final boolean DEBUG = true;
	private static final int BAD_FILE_LINE_NUMBER = -1;
	private static final int CORPUS_STARTING_LINE_NUMBER = 0;
	private static final String NEWLINE = "\n";
	
	//CONSTANTS: Get from resources
	private static String PROGRAM_FOLDER_NAME = null;
	private static String CORPUS_FOLDER_NAME = null;
	private static String TRACKING_FOLDER_NAME  = null;
	private static String CORPUS_FILENAME_EXTENSION = null;
	private static String CORPUS_POS_COUNTER_FILENAME_SUFFIX = null;
	private static String CORPUS_WRAP_COUNTER_FILENAME_SUFFIX = null;
	private static String CORPUS_INPUT_PATH = null;
	private static String CORPUS_TRACKING_PATH = null;
	
	//VARIABLES
	private String corpusFilename = null;
	private String corpusFilenameFQ = null;
	private String posTrackingFilenameFQ = null;
	private String wrapTrackingFilenameFQ = null;
	
	//Position tracking
	//Note: all lines use zero-based indexing i.e. first line is line 0
	private int initialLineNumberToStartReadingFrom = BAD_FILE_LINE_NUMBER;
	private int nextCorpusLineLoadingPosition = 0; //file access
	private int nextCorpusRestartPosition = 0; //remove?
	private int wrapCounter = 0;
	private int numberOfLinesInCorpus = 0;
	
	private File root = null;
	private Context mAppContext = null;
	private String mCorpusName = null;
	private int numberOfExtraPromptsToLoad = 0;
	
	ArrayList<String> utteranceList = null;
	String line = null;
	private int r = -1;
	String removedItem = null;
	
	//Logging
	private Logging log = new Logging(LOG_V, LOG_D, LOG_I, LOG_W); //Note: No LOG_E!

	/**
	 * Constructor
	 */
	public PromptList(Context appContext, String corpusName, int targetNumberOfLinesToRead) {
		mAppContext = appContext;
		mCorpusName = corpusName;
		mainConstructorLogic(mAppContext, mCorpusName, targetNumberOfLinesToRead);
	}
	
	public void mainConstructorLogic(Context appContext, String corpusName, int targetNumberOfLinesToRead) {
		
		int linesReadFromFile = 0;
		
		String methodTAG = "mainConstructorLogic";
		
		log.logD(TAG, "Loading " + targetNumberOfLinesToRead + " prompts from corpus: " + corpusName);
		
		//INIT RESOURCES
		Resources res = appContext.getResources();
		PROGRAM_FOLDER_NAME = res.getString(R.string.PROGRAM_FOLDER_NAME); //e.g. /Woefzela
		CORPUS_FOLDER_NAME = res.getString(R.string.CORPUS_FOLDER_NAME); //e.g. /CorpusInput
		TRACKING_FOLDER_NAME = res.getString(R.string.TRACKING_FOLDER_NAME); //e.g. /Tracking
		CORPUS_FILENAME_EXTENSION = res.getString(R.string.CORPUS_FILENAME_EXTENSION);
		
		CORPUS_POS_COUNTER_FILENAME_SUFFIX = res.getString(R.string.CORPUS_POS_COUNTER_FILENAME_SUFFIX);
		CORPUS_WRAP_COUNTER_FILENAME_SUFFIX = res.getString(R.string.CORPUS_WRAP_COUNTER_FILENAME_SUFFIX);
    	
    	//Own rule: ...Path ends in /
    	//Building filenames and paths in one place
    	CORPUS_INPUT_PATH = "/sdcard" + PROGRAM_FOLDER_NAME + CORPUS_FOLDER_NAME + "/"; //e.g. /sdcard/Woefzela/CorpusInput/
		corpusFilename = corpusName + CORPUS_FILENAME_EXTENSION; //e.g. af_ZA.txt
		corpusFilenameFQ = CORPUS_INPUT_PATH + corpusFilename; //e.g. /sdcard/Woefzela/CorpusInput/af_ZA.txt
		
		CORPUS_TRACKING_PATH = "/sdcard" + PROGRAM_FOLDER_NAME + TRACKING_FOLDER_NAME + "/"; //e.g. /sdcard/Woefzela/Tracking/
		posTrackingFilenameFQ = CORPUS_TRACKING_PATH + corpusName + CORPUS_POS_COUNTER_FILENAME_SUFFIX; //e.g.  /sdcard/Woefzela/Tracking/af_ZA_lineToStartReadingFrom.dat
		wrapTrackingFilenameFQ = CORPUS_TRACKING_PATH + corpusName + CORPUS_WRAP_COUNTER_FILENAME_SUFFIX; //e.g. /sdcard/Woefzela/Tracking/af_ZA_numberOfTimesCorpusHasWrapped.dat
    	
		if (DEBUG){
			log.logD(TAG, "corpusFilenameFQ = " + corpusFilenameFQ);
			log.logD(TAG, "posTrackingFilenameFQ = " + posTrackingFilenameFQ);
			log.logD(TAG, "wrapTrackingFilenameFQ = " + wrapTrackingFilenameFQ);
		}
		
		//INIT OTHER VARIABLES
		utteranceList = new ArrayList<String>(targetNumberOfLinesToRead);
		wrapCounter = 0;
		nextCorpusRestartPosition = 0;

		if (!ifCanReadAndWriteSDCARD()) {
			log.logE(methodTAG, "FATAL ERROR: CANNOT READ/WRITE SDCARD.");
		}

		log.logI(TAG,"Trying to read corpus file...");
		try {
		    	File fid = new File(corpusFilenameFQ);
		    	log.logI(TAG,"fid.getPath() =  " + fid.getPath());

				initialLineNumberToStartReadingFrom = getLineNumberToStartReadingFrom(); //Now random
				log.logI(TAG,"Loading from line " + initialLineNumberToStartReadingFrom + " for " + targetNumberOfLinesToRead + " lines.");
				updateLineIndexStartedReadingFrom(initialLineNumberToStartReadingFrom);//New purpose since random start point

		    	//Note: RandomAccessFile does not seem to read UTF-8 properly. Have to prove later to log a bug. Thus following line could not be used...
		    	//RandomAccessFile in = new RandomAccessFile(fid, "r");

		    	int BUFFER_SIZE = 8192;
		    	BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fid), getUTF8Charset()),BUFFER_SIZE);
		    	
		    	if (br.markSupported()) {
		    		log.logD(TAG,"BufferedReader does support mark functionality.");
		    		br.mark(Integer.MAX_VALUE-1); //never
		    	}
		    	else {
		    		log.logCriticalError(TAG, methodTAG, "BufferedReader does NOT support mark functionality!");
		    	}
		    	
		    	//Advance to correct line
		    	//TODO Fix later to move pointer elegantly: skip to line number X
		    	nextCorpusLineLoadingPosition = initialLineNumberToStartReadingFrom;
		    	nextCorpusRestartPosition = initialLineNumberToStartReadingFrom;
		    	log.logD(TAG,"Initial setting: nextCorpusRestartPosition = " + nextCorpusRestartPosition);
	        	for (int j = 0; j < (initialLineNumberToStartReadingFrom); j++) { //?-1. Tested = NO
			        if (((line = br.readLine()) != null)) {
			        	//DO NOTHING
			        }
			        else {
			        	log.logI(TAG, "Corpus file ended before the requested starting line could be reached. Resetting to line 0.");
			        	nextCorpusLineLoadingPosition = 0; 
			        	nextCorpusRestartPosition = 0;
			        	break; //from for-loop
			        }
	        	}
		    	
	        	//Read target number of lines into a linked list. If have to wrap to get it done, do it.
		        linesReadFromFile = 0;

			    while ((linesReadFromFile < targetNumberOfLinesToRead)) { //...target not met
			        if ( (line = br.readLine()) != null) {
		                String durationField = null;
		                String promptCategory = null;
		                String promptText = null;
		                String delims = "[;]+"; //Notes: + means one or more times
		                int MAX_NUMBER_OF_COLUMNS = 3;//rest ends up in column 'last'
		                String[] tokens = line.split(delims,MAX_NUMBER_OF_COLUMNS);
		                for (int i = 0; i < tokens.length; i++) {
		                	//TODO thorough checking
		                	switch (i) {
		                		case 0:
		                			durationField = tokens[i];
		                	        break;
		                		case 1: 
		                			promptCategory = tokens[i];
		                	        break;
		                		case 2: 
		                			promptText = tokens[i];
		                	        break;
		                		default:
		                	        log.logCriticalError(TAG, methodTAG, "Too many columns in corpus.");
		                	}
		                }
	                    log.logD(TAG,"Prompt added to utteranceList: '" + promptText + "'" + " at line: " + nextCorpusLineLoadingPosition);
				        
	                    utteranceList.add(promptText);
	                    linesReadFromFile++; //number of lines read from file, not prompts spoken
				        nextCorpusLineLoadingPosition++;
			        }
			        else { //End of corpus reached, but not target number of lines reached
			        	wrapCounter++;
			        	br.reset(); //...to last mark (which is 0 here)
			        	nextCorpusLineLoadingPosition = 0; //Start again
			        }
			    }
		        br.close();
		} 
		catch (IOException e) {
		    log.logE(methodTAG, "Could not read file " + e.getMessage());
		}
	}
	
	public String extractNextString() {
	      //prepare to remove a random item
	      Random rnd = new Random();
	      
	      int maxIndex = utteranceList.size();
	      log.logI(TAG, "maxIndex = " + maxIndex);
	      
	      if (maxIndex != 0) {
	    	  if (RANDOMIZE) {
	    		  r = rnd.nextInt(maxIndex);
	    		  log.logI(TAG, "r = " + r + " in range [0.." + (maxIndex-1) + "]");
	    		  removedItem = (String) utteranceList.get(r);
	    		  log.logI(TAG, "element [" + r + "] to be removed is = " + removedItem);
	    		  utteranceList.remove(r);
	    	  }
	    	  else {
	    		  log.logI(TAG, "Removing first element in list.");
	    		  removedItem = (String) utteranceList.get(0); //Always the first one
	    		  log.logI(TAG, "element [" + 0 + "] to be removed is = " + removedItem);
	    		  utteranceList.remove(0);
	    	  }
	    	  nextCorpusRestartPosition++; //Where to resume next batch or session
	    	  log.logD(TAG,"Setting: nextCorpusRestartPosition = " + nextCorpusRestartPosition);
		      return removedItem;
	      }
	      else { //successfully made this target number of prompts in set when created this PromptList object
	    	  log.logD(TAG,"Current promptlist has been emptied. Updating tracking file.");
	    	  return null;
	      }
	}
	
	protected void loadNewSetOfPrompts(int numberOfExtraPromptsToLoad) {
		mainConstructorLogic(mAppContext, mCorpusName, numberOfExtraPromptsToLoad);
		log.logI(TAG,"Loaded a new set of prompts with " + numberOfExtraPromptsToLoad + " items.");
	}
	
	protected void setNumberOfExtraPromptsToLoad(int number) {
		numberOfExtraPromptsToLoad = number;
	}
	
	private int getLineNumberToStartReadingFrom() {
		
		//NOTE: This used to be read from a 'tracking file'. Now random to avoid
		//dependency on tracking file.
		
		String methodTAG = "getLineNumberToStartReadingFrom";
		
		int upperExclusiveBoundary = 0;
		int randomLineIndex = 0;
		
    	//Determine number of lines in corpus
    	try {
			numberOfLinesInCorpus = countLinesInTextfile(corpusFilenameFQ);
			log.logI(TAG, "Number of lines in corpus '" + corpusFilenameFQ + "' = " + numberOfLinesInCorpus);
		} catch (IOException e) {
			e.printStackTrace();
			log.logCriticalError(TAG, methodTAG, "FATAL ERROR: Unable to count number of lines in current corpus.");
		}
		
		//Generate random number for start of block of N prompts within corpus size
		Random rndUniform = new Random();
		if (RANDOM_STARTPOINT) { 
			randomLineIndex = rndUniform.nextInt(numberOfLinesInCorpus); //Range: [0 - numberOfLinesInCorpus).
		}
		else {
			randomLineIndex = 0;
		}
		log.logI(methodTAG,"randomLineIndex = " + randomLineIndex);
		return randomLineIndex;
	}
	
	private void updateLineIndexStartedReadingFrom(int pos) { //Was called updateLineNumberToStartReadingFrom
		
		String methodTAG = "updateLineIndexStartedReadingFrom";
		
		log.logD(TAG, "updateLineIndexStartedReadingFrom filename = " + posTrackingFilenameFQ);
		
		FileWriter fWrite = null;
		
		if (ifCanReadAndWriteSDCARD()) {
			log.logD(TAG, "Can read/write to SDCARD.");
			root = Environment.getExternalStorageDirectory();
			log.logI(TAG,"root = " + root.toString());
		        
		    File fid = new File(posTrackingFilenameFQ);
		    
					if (root.canWrite()) {
						log.logD(TAG, "updateLineIndexStartedReadingFrom canWrite = true");
						try {
							fWrite = new FileWriter(fid, true);
							BufferedWriter out = new BufferedWriter(fWrite);
			            	try {
			            		String s = String.valueOf(pos); //Should it be +1? No, already ++ when reach here.
			            		log.logD(TAG,"lineIndexStartedReadingFrom = " + s);
			            		out.write(s);
								out.write(NEWLINE); 
								out.close();
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
				    	log.logE(methodTAG, "Could not write file.");
				    }//can write
		}
		else {
			log.logE(methodTAG, "SDCARD: Not able to either read and/or write from/to SDCARD.");
		}
	}
	
	private void addValueToWrapCounterFile() {
		
		String methodTAG = "addValueToWrapCounterFile";
		
		FileReader fRead = null;
		FileWriter fWrite = null;
		int localWrapCounterValue = -1;
		String lineReadFromFile = null;
		
		if (ifCanReadAndWriteSDCARD()) {
			log.logD(TAG, "Can read/write to SDCARD.");
			root = Environment.getExternalStorageDirectory();
			log.logI(TAG,"root = " + root.toString());

		    File fid = new File(wrapTrackingFilenameFQ);
		    
		    if (root.canRead()) {
				try { //...to open this file
					fRead = new FileReader(fid);
					BufferedReader in = new BufferedReader(fRead);
					
					try {
						lineReadFromFile = in.readLine();
						in.close();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					
					log.logI(TAG,"wrapCounter read from file (as String) = " + lineReadFromFile);
					
					try { //to parse
						localWrapCounterValue = Integer.parseInt(lineReadFromFile);
						log.logD(TAG,"wrapCounterValue read from file (as int) = " +  localWrapCounterValue);
						localWrapCounterValue = localWrapCounterValue + wrapCounter;
						overwriteWrapCounterInFile(localWrapCounterValue, fid);
					} 
					catch (NumberFormatException e) {
						// TODO alert about file format error
						log.logCriticalError(TAG, methodTAG, "Line read from file could not be parsed into an integer!");
						e.printStackTrace();
					}
				} 
				catch (FileNotFoundException e) {
					log.logI(TAG,"The corpusWrapFile was not found...trying to create one");
				    if (root.canWrite()) {
						try {
							fWrite = new FileWriter(fid);
							BufferedWriter out = new BufferedWriter(fWrite);
			            	try {
								out.write(String.valueOf(wrapCounter));
								out.write(NEWLINE); //first time wrap counter = 0
								out.close();
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
				    	log.logE(methodTAG, "Could not write file.");
				    }
				}
		    }
		    else {
		    	//TODO say could no read and exit
		    	log.logE(methodTAG, "Could not read file.");
		    }
		}
		else {
			log.logE(methodTAG, "SDCARD: Not able to either read and/or write from/to SDCARD.");
		}
	}

	//Assuming file already there
	private void overwriteWrapCounterInFile(int wrapCounterValue, File fid) {
		
		FileWriter fWrite = null;
		
		log.logI(TAG,"Increading CorpusWrapCounter with one to become " + wrapCounterValue);
		root = Environment.getExternalStorageDirectory();
		log.logI(TAG,"root = " + root.toString());
		
	    if (root.canWrite()) {
			try {
				fWrite = new FileWriter(fid);
				BufferedWriter out = new BufferedWriter(fWrite);
            	try {
					out.write(String.valueOf(wrapCounterValue));
					out.write(NEWLINE);
					out.close();
					wrapCounterValue = 0;
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
	    	log.logE(methodTAG, "Could not write file.");
	    }
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
		    // Something else is wrong. It may be one of many other states, but all we need
		    //  to know is we can neither read nor write
		    return false;
		}
	}
	
	//Source: http://stackoverflow.com/questions/453018/number-of-lines-in-a-file-in-java
	private int countLinesInTextfile(String filenameFQ) throws IOException {
		int c = 0;
		
		@SuppressWarnings("unused")
		String lineRead;
		
		LineNumberReader lineReader  = new LineNumberReader(new FileReader(filenameFQ));

		while ((lineRead = lineReader.readLine()) != null) {
			//Do nothing
		}
		
		//NOTE: preferably last line must not end in \n, but if does rest of program must cope.
		c = lineReader.getLineNumber();
		lineReader.close();
		log.logI(TAG,"countLinesInTextfile() returns: " + (c-1) );
		return (c-1);
	}
	
	private Charset getUTF8Charset() {
		
		methodTAG = "getUTF8Charset()";
		
		Charset c = null;
		
		try {
			c = Charset.forName("UTF-8");
		} catch (IllegalCharsetNameException e) {
			log.logE(methodTAG, "IllegalCharsetNameException");
		} catch (UnsupportedCharsetException e) {
			log.logE(methodTAG, "UnsupportedCharsetException");
		}
		return c;
	}
}