package org.meraka.nchlt.woefzela;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;

public class SplashActivity extends Activity {
	
    long splashShowTime = 1000; //ms
    boolean paused = false;
    boolean splashActive = true;
    private static final String TAG= "SplashActivity";
	
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);
        
        Thread splashTimer = new Thread() { //Make own thread
    			public void run() {
    				try {
    					//Wait loop
    					long ms = 0;
    					while(splashActive && ms < splashShowTime) {
    						sleep(100);
    						//Advance the timer only if we're running.
    						if(!paused)
    							ms += 100;
    					}
    					//Advance to the next screen
    					startActivity(new Intent("org.meraka.nchlt.woefzela.splash.CLEARSPLASH"));
    				}
    				catch(Exception e) {
    					Log.e("Splash: ", e.toString());
    				}
    				finally {
    					finish();
    				}
    			}
    	};
    	splashTimer.start();
    }
    
	protected void onPause() {
		super.onPause();
		paused = true;
	}

	protected void onResume() {
		super.onResume();
		paused = false;
	}
	
	protected void onStop()	{
		super.onStop();
	}
	
	protected void onDestroy() {
		super.onDestroy();
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		super.onKeyDown(keyCode, event);
		if (keyCode == 5 || keyCode ==23) { //23 = action-button/track-ball
			splashActive = false;
		}
		if (keyCode == 4) {
			finish();
		}
		Log.i(TAG, "Pressed keyCode " + keyCode);
		return true;
	}
}