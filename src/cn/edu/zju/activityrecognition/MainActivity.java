/***********************************************************************
** Copyright (C) 2012 LP-Research
** All rights reserved.
** Contact: LP-Research (klaus@lp-research.com)
**
** Redistribution and use in source and binary forms, with 
** or without modification, are permitted provided that the 
** following conditions are met:
**
** Redistributions of source code must retain the above copyright 
** notice, this list of conditions and the following disclaimer.
** Redistributions in binary form must reproduce the above copyright 
** notice, this list of conditions and the following disclaimer in 
** the documentation and/or other materials provided with the 
** distribution.
**
** THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
** "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT 
** LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS 
** FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
** HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
** SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
** LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
** DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
** THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
** (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE 
** OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
***********************************************************************/

/* 	Before using this application please ensure the following:
	1. 	The Bluetooth ID in this program equals the Bluetooth ID of your 
		LPMS-B sensor (LpmsBThread.connect)
	2. 	The data acquisition settings correspond to the settings in 
		the LpmsControl application (LpmsBThread.setAcquisitionParameters) */

package cn.edu.zju.activityrecognition;


import android.app.*;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.*;
import android.os.*;
import android.content.Intent;
import android.bluetooth.*;

// Main activity. Connects to LPMS-B and displays orientation values
public class MainActivity extends Activity
{
	BluetoothAdapter mAdapter;
	
	Button sittingButton, standingButton, lyingButton, walkingButton, runningButton, climbingButton;
	// Initializes application
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);	
		
        sittingButton = (Button) findViewById(R.id.Button05);
        standingButton = (Button) findViewById(R.id.Button01);
        lyingButton = (Button) findViewById(R.id.Button02);
        walkingButton = (Button) findViewById(R.id.Button03);
        runningButton = (Button) findViewById(R.id.Button04);
        climbingButton = (Button) findViewById(R.id.Button06);
        
        sittingButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Animation animation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.button_scale);
				animation.setAnimationListener(new AnimationListener() {
					@Override
					public void onAnimationStart(Animation animation) {
					}
					@Override
					public void onAnimationRepeat(Animation animation) {
					}
					@Override
					public void onAnimationEnd(Animation animation) {
						Intent intent = new Intent(MainActivity.this, DataCollectionActivity.class);
						startActivity(intent);		
					}
				});
				v.startAnimation(animation);
			}
		});
        
		// Gets default Bluetooth adapter
		mAdapter = BluetoothAdapter.getDefaultAdapter();				
    }
}