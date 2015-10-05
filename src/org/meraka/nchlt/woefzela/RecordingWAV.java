/* 
 * Copyright (c) 2011 CSIR, Meraka, South Africa
 * Copyright (c) 2009-2010 urbanSTEW
 *
 * Contributors: 
 *   - The Department of Arts and Culture, The Government of South Africa.
 *   - urbanSTEW (http://urbanstew.org)
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
 * Developers:
 *   Stjepan Rajko
 *   Modified by: Nic de Vries - 2011 (various modifications to adapt for purpose)
 *   
 */

package org.meraka.nchlt.woefzela;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import android.media.AudioRecord;
import android.util.Log;

public class RecordingWAV {
	
	//HOUSEKEEPING
	private static final String classTAG = "RecordingWAV";
	
	//SWITCHES
	private static final boolean LOG_V = true;
	private static final boolean LOG_D = true;
	private static final boolean LOG_I = true;
	private static final boolean LOG_W = true;
	
	private static final boolean DEBUG_MODE = true; //Warning: Non-standard
	
	private static final short NUM_CHANNELS = 1;
	private static final short BITS_PER_SAMPLE = 16;
	
	//Note: state == State.ERROR is trapped by calling class
	public enum State {INITIALIZING, READY, RECORDING, ERROR, STOPPED};
	
	private AudioRecord audioRec = null;
	private int soundSource;
	private int sampRate;
	private int soundFormat;
	private int channelConf;
	private int bufferSizeInBytes;
	private int framePeriod;
	private byte[] buffer;


	private int totalPayloadSize;
	
	protected State state;
	protected int maxAmplitudeValue = 0;
	
	private String filePath = null;
	private RandomAccessFile fWriter;
	private boolean fileClosed;
	
	//Logging
	private Logging log = new Logging(LOG_V, LOG_D, LOG_I, LOG_W); //Note: No LOG_E!

	private AudioRecord.OnRecordPositionUpdateListener updateListener = new AudioRecord.OnRecordPositionUpdateListener() {
		public void onPeriodicNotification(AudioRecord recorder) {
			String dTag = "onPeriodicNotification";
						
			int result = audioRec.read(buffer, 0, buffer.length); //Note: buffer's size is defined in prepare()
			log.logI(classTAG, "AudioRecord.read() resulted in: " + result);
			

			try	{ 
				if (DEBUG_MODE) {
					Log.d(dTag, buffer.length + ":" + buffer.toString());
					Log.d(dTag, "current sRate = " + audioRec.getSampleRate());
					int st = audioRec.getState();
					Log.d(dTag, "state no: " + st);
					long temp_fp = fWriter.getFilePointer();
					Log.d(classTAG, "fp = " +  temp_fp);
				}
				
				totalPayloadSize += buffer.length;
				fWriter.write(buffer); //Write 'data' section of WAV file periodically

				for (int i=0; i<buffer.length/2; i++) {
					short t = getShort(buffer[i*2], buffer[i*2+1]);
					if (t > maxAmplitudeValue) {
						maxAmplitudeValue = t;
					}
				}
			}		
			catch (IOException e) {
				if (fileClosed == true) {
					//TODO Investigate better sometime...
					log.logI(classTAG, "File already closed, dropping last buffer...");
				}
			}
		}
		
		//Not used but must be 'implemented'
		public void onMarkerReached(AudioRecord recorder) {
			//Do nothing
		}
	};
	
	/*
	 * Constructor 
	 */
	public RecordingWAV(int audioSource, int sampleRateInHz, int channelConfig, int audioFormat) {
		
		String methodTAG = "RecordingWAV>Constructor";
		int minBufferSize;
		
		try {
			soundSource = audioSource;
			sampRate   = sampleRateInHz;
			channelConf = channelConfig;
			soundFormat = audioFormat;

			framePeriod = sampRate*120/1000;
			log.logV(classTAG,"framePeriod before = " + framePeriod);
			
			//Pre-determine buffer size
			bufferSizeInBytes = framePeriod*2*BITS_PER_SAMPLE*NUM_CHANNELS/8;
			log.logV(classTAG,"bufferSizeInBytes before = " + bufferSizeInBytes);
			
			//Test if pre-determined buffer size is OK, or get a new value for it
			minBufferSize = AudioRecord.getMinBufferSize(sampRate, channelConf, soundFormat);
			log.logV(classTAG,"minBufferSize = " + minBufferSize);
			
			bufferSizeInBytes = minBufferSize;
			framePeriod = bufferSizeInBytes/(2*BITS_PER_SAMPLE*1/8);
			
			if (bufferSizeInBytes < minBufferSize) {
				bufferSizeInBytes = minBufferSize;
				
				if (bufferSizeInBytes == AudioRecord.ERROR_BAD_VALUE) {
					log. logE(methodTAG, "The recording parameters are not supported by the hardware, or an invalid parameter was passed.");
				}
				if (bufferSizeInBytes == AudioRecord.ERROR) {
					log.logE(methodTAG, "The implementation was unable to query the hardware for its output properties or the minimum buffer size.");
				}
				framePeriod = bufferSizeInBytes/(2*BITS_PER_SAMPLE*1/8);
				log.logW(classTAG, "Increasing buffer size to " + Integer.toString(bufferSizeInBytes));
			}
			
			audioRec = new AudioRecord(soundSource, sampRate, channelConf, soundFormat, bufferSizeInBytes);
			if (audioRec.getState() != AudioRecord.STATE_INITIALIZED) {
				log.logD(methodTAG,"AudioRecord.STATE_UNINITIALIZED after attempting to create AudioRecord object audioRec.");
				maxAmplitudeValue = 0;
				filePath = null;
				state = State.ERROR;
				log.logD(methodTAG,"RecordingWAV.state changed to: State.ERROR (Constructor: create obj)");
				throw new Exception("AudioRecord initialization failed");
			}
			else { //OK
				log.logD(methodTAG,"AudioRecord.STATE_INITIALIZED after create AudioRecord object audioRec.");
				audioRec.setRecordPositionUpdateListener(updateListener);
				audioRec.setPositionNotificationPeriod(framePeriod);
				state = State.INITIALIZING;
				log.logD(methodTAG,"RecordingWAV.state changed to: State.INITIALIZING (Constructor)");
			}
		} 
		catch (Exception e) {
			if (e.getMessage() != null)	{
				log.logE(classTAG, e.getMessage());
			}
			else {
				log.logE(classTAG, "Unknown error occured while initialising recording");
			}
			state = State.ERROR;
			log.logD(methodTAG,"RecordingWAV.state changed to: State.ERROR (Constructor: catch e)");
		}
	}
	
	public void setOutputFile(String path) {
		
		String methodTAG = "setOutputFile";
		
		log.logI(methodTAG, classTAG + " setOutputFile entered.");
		
		if (state == State.INITIALIZING) {
			filePath = path;
		}	
	}

	public void prepare() {
		
		String methodTAG = "prepare";
		
		try {
			if (state == State.INITIALIZING) {
				if ((audioRec.getState() == AudioRecord.STATE_INITIALIZED) & (filePath != null)) {
					log.logI(classTAG, "Writing file header...");
					
					fWriter = new RandomAccessFile(filePath, "rw");
					fileClosed = false;
					fWriter.setLength(0); //Set file length. Unknown in real-time.
					
					//Write WAV file header:
					//See https://ccrma.stanford.edu/courses/422/projects/WaveFormat/ for WAV format reference
					fWriter.writeBytes("RIFF"); //ChunkID
					fWriter.writeInt(0); //ChunkSize. Unknown. Update later
					fWriter.writeBytes("WAVE"); //Format
					
					//The "WAVE" format consists of two subchunks: "fmt " and "data"
					//The "fmt " subchunk describes the sound data's format:
					fWriter.writeBytes("fmt "); //Subchunk1ID. NOTE: trailing space!
					fWriter.writeInt(Integer.reverseBytes(16)); //Subchunk1Size
					fWriter.writeShort(Short.reverseBytes((short) 1)); //AudioFormat (PCM=1)
					fWriter.writeShort(Short.reverseBytes(NUM_CHANNELS)); //NumChannels
					fWriter.writeInt(Integer.reverseBytes(sampRate)); //SampleRate
					fWriter.writeInt(Integer.reverseBytes(sampRate*BITS_PER_SAMPLE*NUM_CHANNELS/8)); //ByteRate
					fWriter.writeShort(Short.reverseBytes((short)(NUM_CHANNELS*BITS_PER_SAMPLE/8))); //BlockAlign
					fWriter.writeShort(Short.reverseBytes(BITS_PER_SAMPLE)); //BitsPerSample
					
					//The "data" subchunk contains the size of the data and the actual sound:
					fWriter.writeBytes("data"); //Subchunk2ID
					fWriter.writeInt(0); //Subchunk2Size. Unknown. Update later
					
					buffer = new byte[framePeriod*BITS_PER_SAMPLE/8*NUM_CHANNELS];
					state = State.READY;
					log.logD(methodTAG,"RecordingWAV.state changed to: State.READY (prepare())");
				}
				else {
					log.logE(classTAG, "prepare() while uninitialized recorder");
					state = State.ERROR;
					log.logD(methodTAG,"RecordingWAV.state changed to: State.ERROR (prepare())");
				}
			}
			else {
				log.logE(classTAG, "prepare() called on illegal state");
				release();
				state = State.ERROR;
				log.logD(methodTAG,"RecordingWAV.state changed to: State.ERROR (prepare())");
			}
		}
		catch(Exception e) {
			if (e.getMessage() != null)	{
				log.logE(classTAG, e.getMessage());
			}
			else {
				log.logE(classTAG, "Unknown error occured in prepare()");
			}
			state = State.ERROR;
			log.logD(methodTAG,"RecordingWAV.state changed to: State.ERROR (prepare())");
		}
	}
	
	public void release() {
		
		String methodTAG = "release";
		
		if (state == State.RECORDING) {
			stop();
		}
		else {
			if (state == State.READY) {
				try	{
					fWriter.close();
					fileClosed = true;
					log.logI(classTAG, "fWriter was closed by release()");
				}
				catch (IOException e) {
					log.logE(methodTAG, "I/O error while closing output file");
				}
				(new File(filePath)).delete();
			}
		}
		
		if (audioRec != null) {
			audioRec.release();
			audioRec = null;
		}
	}
	
	public void reset() {
		String methodTAG = "reset";
		try {
			if (state != State.ERROR) {
				release();
				filePath = null;
				audioRec = null;
				audioRec = new AudioRecord(soundSource, sampRate, channelConf, soundFormat, bufferSizeInBytes);
				if (audioRec.getState() != AudioRecord.STATE_INITIALIZED) {
					log.logD(methodTAG,"AudioRecord.STATE_UNINITIALIZED after attempting to create AudioRecord object audioRec.");
					maxAmplitudeValue = 0;
					filePath = null;
					state = State.ERROR;
					log.logD(methodTAG,"RecordingWAV.state changed to: State.ERROR (reset())");
					throw new Exception("AudioRecord initialization failed");
				}
				else { //OK
					log.logD(methodTAG,"AudioRecord.STATE_INITIALIZED after create AudioRecord object audioRec.");
					audioRec.setRecordPositionUpdateListener(updateListener);
					audioRec.setPositionNotificationPeriod(framePeriod);
					state = State.INITIALIZING;
					log.logD(methodTAG,"RecordingWAV.state changed to: State.INITIALIZING (reset())");
				}
			}
		}
		catch (Exception e)	{
			log.logE(classTAG, e.getMessage());
			state = State.ERROR;
			log.logD(methodTAG,"RecordingWAV.state changed to: State.ERROR (reset())");
		}
	}
	
	public void start() {
		
		String methodTAG = "start";
		
		if (state == State.READY) {
			totalPayloadSize = 0;
			audioRec.startRecording();
			int result = audioRec.read(buffer, 0, buffer.length);
			log.logI(classTAG, "AudioRecord.read() in start() gives: " + result + " expected " + buffer.length);
			maxAmplitudeValue = 0;
			state = State.RECORDING;
			log.logD(methodTAG,"RecordingWAV.state changed to: State.RECORDING (start())");
		}
		else {
			log.logE(classTAG, "start() called on illegal state");
			state = State.ERROR;
			log.logD(methodTAG,"RecordingWAV.state changed to: State.ERROR (start())");
		}
	}
	
	public void stop() {
		
		String methodTAG = "stop";
		log.logD(classTAG, "stop() was called.");
		
		if (state == State.RECORDING) {
			
			audioRec.stop();
			
			try	{
				//Prepare and update ChunkSize at offset 4 in file:
				fWriter.seek(4);
				fWriter.writeInt(Integer.reverseBytes(36+totalPayloadSize));
				
				//Prepare and update Subchunk2Size at offset 40 in file:
				fWriter.seek(40);
				fWriter.writeInt(Integer.reverseBytes(totalPayloadSize));
				
				fWriter.close();
				fileClosed = true;
				log.logI(classTAG, "fWriter was closed by stop()");
			}
			catch(IOException e) {
				log.logE(classTAG, "I/O exception occured while closing output file");
				state = State.ERROR;
				log.logD(methodTAG,"RecordingWAV.state changed to: State.ERROR (stop())");
			}
			state = State.STOPPED;
			log.logD(methodTAG,"RecordingWAV.state changed to: State.STOPPED (stop())");
		}
		else {
			log.logE(classTAG, "stop() called on state: " + state + ", while expected State.RECORDING.");
			state = State.ERROR;
			log.logD(methodTAG,"RecordingWAV.state changed to: State.ERROR (stop())");
		}
	}
	
	public State getState() {
		return state;
	}
	
	//Converts 2 bytes to a short (little endian)
	private short getShort(byte b1, byte b2) {
		return (short)(b1 | (b2 << 8));
	}
}