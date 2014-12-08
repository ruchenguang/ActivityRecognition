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


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.List;

import android.R.anim;
import android.app.*;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.style.BulletSpan;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation.AnimationListener;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import android.os.*;

// Main activity. Connects to LPMS-B and displays orientation values
public class MainActivity extends Activity{
	public static final String TAG = "ActivityRecognition::MainActivity";
	public static final String EXTRA_ACTIVITY = "MainActivity::extra_activity";	
	public static final String EXTRA_ACTIVITY_ISFINISHED = "MainActivity::extra_activity_isfinshed";	
	public static final String BUNDLE_FINISHED_ACTIVITIES = "MainActivity::bundle_finished_activities";
	public static final byte ASK_CODE_ZERO = 48;
	
	public static ArrayList<Activity> activities;
	public static ArrayList<String> activityTitles;
	public static int activityNum; 

	public static File activityCompletionStateFile;
	ListView listView;
	TextView tv;
	
	boolean isAllFinished = false;
	Button finishButton;
	// Initializes application
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);	
        ExitApplication.activityList.add(this);
        
        initActivitiesAndTitles();
        
        listView = (ListView) findViewById(R.id.listView1);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), 
        		R.layout.listview_string, R.id.textView1, activityTitles);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Intent intent = new Intent(MainActivity.this, DataCollectionActivity.class);
				intent.putExtra(EXTRA_ACTIVITY, position);
				intent.putExtra(EXTRA_ACTIVITY_ISFINISHED, activities.get(position).isFinished);
				startActivity(intent);
			}
		});
        
        finishButton = (Button) findViewById(R.id.button1);
        finishButton.setOnClickListener(new OnClickListener() {
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
						if(isAllFinished){
							AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
							if(isAllFinished){
								builder.setTitle(R.string.title_exit_finished);
								builder.setMessage(R.string.message_exit_finished);
							}
							else{
								builder.setTitle(R.string.title_exit_not_finished);
								builder.setMessage(R.string.message_exit_not_finished);
							}
							builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									dialog.cancel();
								}
							});
							builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									List<android.app.Activity> activityList = ExitApplication.activityList;
									for(int i=0; i<activityList.size(); i++){
										if(activityList.get(i) != null)
											activityList.get(i).finish();
									}
								}
							});
							builder.create().show();
							stopService(new Intent(MainActivity.this, BluetoothService.class));
						} else {
							Toast.makeText(MainActivity.this, "Not all the activities are finished yet. Please go on.", Toast.LENGTH_SHORT).show();
						}
					}
				});
				v.startAnimation(animation);
			}
		});
    }
    
    @Override
    protected void onResume() {
        activityCompletionStateFile = new File(InformationActivity.subjectDirPath, "activityCompletionState.txt");
        if(activityCompletionStateFile.exists()){
        	Handler handler = new Handler(Looper.getMainLooper());
        	handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					try {
						FileInputStream fis = new FileInputStream(activityCompletionStateFile);
			        	byte[] buffer = new byte[activityNum];
			        	fis.read(buffer);
			        	fis.close();
			        	int cnt = 0;
			        	for(int i=0; i<activityNum; i++){
			        		tv = (TextView) listView.getChildAt(i).findViewById(R.id.textView1);
			        		if(buffer[i]-ASK_CODE_ZERO  == 1){
			        			cnt++;
			        			activities.get(i).isFinished = true;
			            		tv.setTextColor(getResources().getColor(android.R.color.darker_gray));
			        		} else {
			        			activities.get(i).isFinished = false;
			            		tv.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
			        		}
			        	}
			        	Log.d(TAG, cnt + " activities have been finished!");
			        	if(cnt == activityNum){
			        		finishButton.setText(R.string.button_finished);
			        		finishButton.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
			        		isAllFinished = true;
			        	} else {
			        		finishButton.setText(R.string.button_not_finished);
			        		isAllFinished = false;
			        	}
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}, 100);
			
        } else {
			try {
				FileOutputStream fos = new FileOutputStream(activityCompletionStateFile);
	        	byte[] buffer = new byte[activityNum];
	        	for(int i=0; i<activityNum; i++){
	        		buffer[i] = 0 + ASK_CODE_ZERO;
	        	}
	        	fos.write(buffer);
	        	fos.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

        }
    	super.onResume();
    }
    
    @Override
    protected void onDestroy() {
    	stopService(new Intent(this, BluetoothService.class));
    	super.onDestroy();
    }
    
    void initActivitiesAndTitles(){
    	activities = new ArrayList<MainActivity.Activity>();
    	//initiate activities
    	ArrayList<Step> steps;
    	//********************simple activity********************
    	//sitting
    	steps = new ArrayList<Step>();
    	steps.add(new Step("Sit straight", 60));
    	activities.add(new Activity(
    			"sitting", R.string.sitting, 
    			R.string.instruction_activity_sitting,
    			R.drawable.sitting,
    			steps));
    	//standing
    	steps = new ArrayList<Step>();
    	steps.add(new Step("Stand still", 60));
    	activities.add(new Activity(
    			"standing", 
    			R.string.standing,
    			R.string.instruction_activity_standing,
    			R.drawable.standing,
    			steps));
    	//lying
    	steps = new ArrayList<Step>();
    	steps.add(new Step("Lying down", 60));
    	activities.add(new Activity(
    			"lying", 
    			R.string.lying,
    			R.string.instruction_activity_lying,
    			R.drawable.lying,
    			steps));
    	//walking
    	steps = new ArrayList<Step>();
    	steps.add(new Step("Walking", 120));
    	activities.add(new Activity(
    			"walking", 
    			R.string.walking,
    			R.string.instruction_activity_walking,
    			R.drawable.walking,
    			steps));
    	//running
    	steps = new ArrayList<Step>();
    	steps.add(new Step("Runnning", 120));
    	activities.add(new Activity(
    			"running", 
    			R.string.running,
    			R.string.instruction_activity_running,
    			R.drawable.running,
    			steps));
    	//climbing upstairs
    	steps = new ArrayList<Step>();
    	steps.add(new Step("Climbing Upstairs", 0));
    	activities.add(new Activity(
    			"climbingstairs", 
    			R.string.climbing_upstairs,
    			R.string.instruction_activity_climbing_upstairs,
    			R.drawable.climbing,
    			steps
    	));
    	//climbing downstairs
    	steps = new ArrayList<Step>();
    	steps.add(new Step("Climbing Downstairs", 0));
    	activities.add(new Activity(
    			"climbingstairs", 
    			R.string.climbing_downstairs,
    			R.string.instruction_activity_climbing_downstairs,
    			R.drawable.climbing,
    			steps
    	));
    	
    	//********************relative ********************
    	//relative sitting
    	steps = new ArrayList<Step>();
    	steps.add(new Step("Sit straight", 15));
    	steps.add(new Step("Lean forward", 5));
    	steps.add(new Step("Sit back straight", 5));
    	steps.add(new Step("Lean backward", 5));
    	steps.add(new Step("Sit back straight", 5));
    	steps.add(new Step("Rotate trunk to right", 5));
    	steps.add(new Step("Rotate back to straight", 5));
    	steps.add(new Step("Rotate trunk to left", 5));
    	steps.add(new Step("Rotate back to straight", 5));
    	steps.add(new Step("Stand up", 5));
    	activities.add(new Activity(
    			"relative_sitting", 
    			R.string.relative_sitting,
    			R.string.instruction_activity_relative_sitting,
    			R.drawable.sitting,
    			steps));
    	//relative standing
    	steps = new ArrayList<Step>();
    	steps.add(new Step("Stand still", 15));
    	steps.add(new Step("Rotate trunk to right", 5));
    	steps.add(new Step("Rotate back to the front", 5));
    	steps.add(new Step("Rotate trunk to left", 5));
    	steps.add(new Step("Rotate back to the front", 5));
    	steps.add(new Step("Move up and down left arm", 5));
    	steps.add(new Step("Stand still", 5));
    	steps.add(new Step("Move up and down right arm", 5));
    	steps.add(new Step("Stand still", 5));
    	activities.add(new Activity(
    			"relative_standing", 
    			R.string.relative_standing,
    			R.string.instruction_activity_relative_standing,
    			R.drawable.standing, 
    			steps));
    	//relative lying
    	steps = new ArrayList<Step>();
    	steps.add(new Step("Lying with face up", 15));
    	steps.add(new Step("Turn to left", 5));
    	steps.add(new Step("Lying back with face up", 5));
    	steps.add(new Step("Turn to right", 5));
    	steps.add(new Step("Lying back with face up", 5));
    	steps.add(new Step("Sit up", 5));
    	activities.add(new Activity(
    			"relative_lying", 
    			R.string.relative_lying,
    			R.string.instruction_activity_relative_lying,
    			R.drawable.lying,
    			steps));
    	
//    	//********************phone in pocket ********************
//    	//sitting with phone in pocket
//    	steps = new ArrayList<Step>();
//    	steps.add(new Step("Put the phone in pocket and then sit straight", 10));
//    	steps.add(new Step("Keep sitting straight", 60));
//    	activities.add(new Activity(
//    			"sitting_with_phone_in_pocket", R.string.sitting_with_phone_in_pocket, 
//    			R.string.instruction_activity_sitting_with_phone_in_pocket,
//    			R.drawable.sitting,
//    			steps));
//    	//sitting with phone in pocket
//    	steps = new ArrayList<Step>();
//    	steps.add(new Step("Put the phone in pocket and then sit straight", 10));
//    	steps.add(new Step("Keep sitting straight", 60));
//    	activities.add(new Activity(
//    			"sitting_with_phone_in_pocket", R.string.sitting_with_phone_in_pocket, 
//    			R.string.instruction_activity_sitting_with_phone_in_pocket,
//    			R.drawable.sitting,
//    			steps));
//    	//sitting with phone in pocket
//    	steps = new ArrayList<Step>();
//    	steps.add(new Step("Put the phone in pocket and then sit straight", 10));
//    	steps.add(new Step("Keep sitting straight", 60));
//    	activities.add(new Activity(
//    			"sitting_with_phone_in_pocket", R.string.sitting_with_phone_in_pocket, 
//    			R.string.instruction_activity_sitting_with_phone_in_pocket,
//    			R.drawable.sitting,
//    			steps));
//    	//sitting with phone in pocket
//    	steps = new ArrayList<Step>();
//    	steps.add(new Step("Put the phone in pocket and then sit straight", 2));
//    	steps.add(new Step("Keep sitting straight", 3));
//    	activities.add(new Activity(
//    			"sitting_with_phone_in_pocket", R.string.sitting_with_phone_in_pocket, 
//    			R.string.instruction_activity_sitting_with_phone_in_pocket,
//    			R.drawable.sitting,
//    			steps));
    	
    	//initiate activities' title and number
    	activityNum = activities.size();
    	activityTitles = new ArrayList<String>();
    	for(int i=0; i<activityNum; i++){
    		activityTitles.add(activities.get(i).title);
    	}
    }
    
    class Activity{
    	String title;
    	String activity;
    	String instruction;
    	int picResourceId;
    	boolean isFinished = false;
    	
    	ArrayList<Step> steps = new ArrayList<Step>();
    	public Activity(String activity, int titleResouceId, int instructionResouceId, int picResourceId, 
    			ArrayList<Step> steps) {
    		this.activity = activity;
    		this.title = getResources().getString(titleResouceId);
    		this.instruction = getResources().getString(instructionResouceId);
    		this.picResourceId = picResourceId;
    		this.steps.addAll(steps);
		}
    	
    	public ArrayList<Step> getSteps() {
    		ArrayList<Step> stepsToCopy = new ArrayList<MainActivity.Step>();
    		for(int i=0; i<steps.size(); i++){
    			Step step = steps.get(i);
    			stepsToCopy.add(new Step(step.stepDescription, step.time));
    		}
			return stepsToCopy;
		}
    }
    
	class Step{
		String stepDescription;
		int time;
		public Step(String description, int seconds) {
			time = seconds;
			stepDescription = description;
		}
	}
}