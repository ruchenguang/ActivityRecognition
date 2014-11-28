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


import java.util.ArrayList;

import android.app.*;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import android.os.*;

// Main activity. Connects to LPMS-B and displays orientation values
public class MainActivity extends Activity{
	public static final String TAG = "ActivityRecognition::MainActivity";
	public static final String EXTRA_ACTIVITY = "MainActivity::extra_activity";	
	public static final int REQUEST_CODE = 0;
	
	public static ArrayList<Activity> activities;
	public static ArrayList<String> activityTitles;
	public static int activityNum = 9; 
	
	ListView listView;
	
	// Initializes application
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);	
        
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
				startActivityForResult(intent, position);
			}
		});
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	Log.d(TAG, "DataCollectionActivity has returned");
    	TextView tv = (TextView) listView.getChildAt(requestCode).findViewById(R.id.textView1);
    	if(resultCode == RESULT_OK){
    		tv.setTextColor(getResources().getColor(android.R.color.darker_gray));
    	} else if(resultCode == RESULT_CANCELED){
    		tv.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
    	}
    	
    	super.onActivityResult(requestCode, resultCode, data);
    }
    
    void initActivitiesAndTitles(){
    	activities = new ArrayList<MainActivity.Activity>();
    	//initiate activities
    	ArrayList<Step> steps;
    	//sitting
    	steps = new ArrayList<Step>();
    	steps.add(new Step("Sit straight", 60));
    	activities.add(new Activity(
    			"sitting", R.string.sitting, 
    			R.string.instruction_activity_sitting,
    			R.drawable.sitting,
    			steps));
    	//relative sitting
    	steps = new ArrayList<Step>();
    	steps.add(new Step("Sit straight", 15));
    	steps.add(new Step("Lean forward", 5));
    	steps.add(new Step("Lean backward", 5));
    	steps.add(new Step("Rotate runk to right", 5));
    	steps.add(new Step("Rotate runk to left", 5));
    	steps.add(new Step("Stand up", 5));
    	activities.add(new Activity(
    			"relative_sitting", 
    			R.string.relative_sitting,
    			R.string.instruction_activity_relative_sitting,
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
    	//relative standing
    	steps = new ArrayList<Step>();
    	steps.add(new Step("Stand still", 15));
    	steps.add(new Step("Rotate runk to right", 5));
    	steps.add(new Step("Rotate runk to left", 5));
    	steps.add(new Step("Move up and down left arm", 5));
    	steps.add(new Step("Move up and down right arm", 5));
    	activities.add(new Activity(
    			"relative_standing", 
    			R.string.relative_standing,
    			R.string.instruction_activity_relative_standing,
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
    	//relative lying
    	steps = new ArrayList<Step>();
    	steps.add(new Step("Lying", 15));
    	steps.add(new Step("Turn to left", 5));
    	steps.add(new Step("Turn to right", 5));
    	steps.add(new Step("Sit up", 5));
    	activities.add(new Activity(
    			"relative_lying", 
    			R.string.relative_lying,
    			R.string.instruction_activity_relative_lying,
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
    	//climbing stairs
    	steps = new ArrayList<Step>();
    	steps.add(new Step("Climbing", 0));
    	activities.add(new Activity(
    			"climbingstairs", 
    			R.string.climbingstairs,
    			R.string.instruction_activity_climbing,
    			R.drawable.climbing,
    			steps
    	));
    	
    	//initiate activities' title
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