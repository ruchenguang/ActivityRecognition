package cn.edu.zju.activityrecognition;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import cn.edu.zju.activityrecognition.MainActivity.Step;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class DataCollectionActivity extends Activity {
	Button startButton, redoButton;
	TextView currentTextView, nextTextView, next2TextView, pastTextView;
	
	int activityIndex = 0;
	cn.edu.zju.activityrecognition.MainActivity.Activity activity;
	
	int stepIndex = 0;
	ArrayList<MainActivity.Step> steps = new ArrayList<MainActivity.Step>();
	
	boolean isStarted = false;
	boolean isPaused = false;
	boolean isFinished = false;
	boolean isUserControl = false;
	boolean isLastSecond = false;
	
	File activityDataFile[];
	FileOutputStream fos[] = null;
	
	Timer timer;
	TimerTask textViewUpdater, dataCollector;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_data_collection);
		
		Intent intent = getIntent();
		activityIndex = intent.getIntExtra(MainActivity.EXTRA_ACTIVITY, 0);
		activity = MainActivity.activities.get(activityIndex);
		if(activity.activity.equals("climbingstairs"))
			isUserControl = true;
		//initiate the user interface for different activity
		TextView instructionTextView = (TextView) findViewById(R.id.textView1);
		instructionTextView.setText(activity.instruction);
		ImageView imageView = (ImageView) findViewById(R.id.imageView1);
		imageView.setImageDrawable(getResources().getDrawable(activity.picResourceId));
		
		pastTextView = (TextView) findViewById(R.id.textView2);
		currentTextView = (TextView) findViewById(R.id.TextView01);
		nextTextView = (TextView) findViewById(R.id.TextView02);
		next2TextView = (TextView) findViewById(R.id.TextView03);
		
		startButton = (Button) findViewById(R.id.button2);
		redoButton = (Button) findViewById(R.id.buttonNext);
		startButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Animation animation = AnimationUtils.loadAnimation(DataCollectionActivity.this, R.anim.button_scale);			
				v.startAnimation(animation);
				if(isFinished){
					Toast.makeText(DataCollectionActivity.this, 
							"You have finished this test.", Toast.LENGTH_LONG).show();
				} else if(isPaused) {
					//change to continue state
					startButton.setText("Pause");
					isPaused = false;
				} else if(isStarted){
					//change to pause state
					startButton.setText("Continue");
					isPaused = true;
					
					if(isUserControl){
						isPaused = false;
						finishColleting();
					}
				} else {
					//start data collection
					initDataFile();
					
					if(fos[0] == null) Toast.makeText(DataCollectionActivity.this, 
							"Error in the file system", Toast.LENGTH_LONG).show();
					else if(!BluetoothService.isConnected && !BluetoothService.isDebug) 
						Toast.makeText(DataCollectionActivity.this, 
							"The sensor seems not connected, please go back and try to connect it again.", 
							Toast.LENGTH_LONG).show();
					else{
						startButton.setText("Pause");
						isStarted = true;
						
						timer.scheduleAtFixedRate(dataCollector, 5, 10);
						if(isUserControl){
							startButton.setText("Finish");
						} else						
							timer.scheduleAtFixedRate(textViewUpdater, 0, 1000);
					}
				}
			}
		});
		redoButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Animation animation = AnimationUtils.loadAnimation(DataCollectionActivity.this, R.anim.button_scale);			
				v.startAnimation(animation);
				
				destroy();				
				prepareStartState();
			}
		});
		prepareStartState();
	}
	
	@Override
    protected void onPause() {
		if(isStarted && !isPaused && !isFinished){
			startButton.setText("Continue");
			isPaused = true;
		}
		super.onPause();
    }
	
	// Called when activity is paused or screen orientation changes
    @Override
    protected void onDestroy() {
		destroy();
        super.onDestroy();
    }
	
	void destroy(){
		timer.cancel();
		try {
			if(fos != null)
				for(int i=0; i<3; i++)
					if(fos[i] != null) fos[i].close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	void finishColleting(){
		timer.cancel();
		startButton.setText("Finished!");
		startButton.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
		isFinished = true;
		Log.d("debug", "Cnt is " + debugCnt);
		
		//return to main acitivty
		this.setResult(RESULT_OK);
		this.finish();
	}
	
	void initDataFile(){
		//init the data file
		File activityDir = new File(InformationActivity.subjectDirPath, activity.activity);
		if(!activityDir.exists())
			activityDir.mkdir();
		String dataPath = activityDir.getAbsolutePath();
		Date currentDate = new Date(System.currentTimeMillis()); 
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.getDefault()); 
		String fileName = formatter.format(currentDate) + ".txt";
		
		activityDataFile = new File[3];
		activityDataFile[0] = new File(dataPath, "acc_" + fileName);
		activityDataFile[1] = new File(dataPath, "gyr_" + fileName);
		activityDataFile[2] = new File(dataPath, "mag_" + fileName);
		try {
			fos = new FileOutputStream[3];
			for(int i=0; i<3; i++)
				fos[i] = new FileOutputStream(activityDataFile[i]);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	void prepareStartState(){
		isStarted = false;
		isFinished = false;
		isPaused = false;
		isLastSecond = false;
		
		startButton.setText("Start");
		startButton.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
		
		stepIndex = 0;
		steps = activity.getSteps();
		pastTextView.setText(" ");
		Step currentStep = steps.get(stepIndex);
		currentTextView.setText(currentStep.stepDescription + " for " + currentStep.time + "s");
		if(steps.size()>(stepIndex+1)){
			Step nextStep = steps.get(stepIndex+1);
			nextTextView.setText(nextStep.stepDescription + " for " + nextStep.time + "s");
		} else nextTextView.setText(" ");
		if(steps.size()>(stepIndex+1)){
			Step next2Step = steps.get(stepIndex+2);
			next2TextView.setText(next2Step.stepDescription + " for " + next2Step.time + "s");
		} else next2TextView.setText(" ");

		//define the timer and timetasks
		timer = new Timer();
		textViewUpdater = new TimerTask() {
			@Override
			public void run() {
				if(isStarted && !isPaused && !isFinished){
					Handler handler = new Handler(Looper.getMainLooper());
					handler.post(new Runnable() {
						public void run() {
							Log.d("timer", "textViewUpdater running " + debugCnt);
							int remainingTime = --steps.get(stepIndex).time;
							if(remainingTime == -1) {
								remainingTime = ++steps.get(stepIndex).time;
							}
							//update past step
							if(stepIndex>0) 
								pastTextView.setText(
										steps.get(stepIndex-1).stepDescription + " for " + 
										steps.get(stepIndex-1).time + "s");
							//update current step
							currentTextView.setText(
									steps.get(stepIndex).stepDescription + " for " + 
									steps.get(stepIndex).time + "s");
							//update next step
							if((stepIndex+1)<steps.size()) 
								nextTextView.setText(
										steps.get(stepIndex+1).stepDescription + " for " + 
										steps.get(stepIndex+1).time + "s");
							else 
								nextTextView.setText(" ");
							//update next 2 step
							if((stepIndex+2)<steps.size())
								next2TextView.setText(
										steps.get(stepIndex+2).stepDescription + " for " + 
										steps.get(stepIndex+2).time + "s");
							else 
								next2TextView.setText(" ");
							
							if(remainingTime == 0){ 
								if(stepIndex == steps.size()-1){
									if(isLastSecond){
										finishColleting();
									}
									isLastSecond = true;
								} else 
									stepIndex++;
							}
						}
					});
				}
			}
		};
		dataCollector = new TimerTask() {
			@Override
			public void run() {
				if(isStarted && !isPaused && !isFinished){
					debugCnt ++;
					DecimalFormat f0 = new DecimalFormat("+000.0000;-000.0000");
					LpmsBData d = BluetoothService.getSensorData();
					try {
						String accData = 
								f0.format(d.acc[0]) + " " +
								f0.format(d.acc[1]) + " " +
								f0.format(d.acc[2]) + " ";
						fos[0].write(accData.getBytes());
						
						String gyrData = 
								f0.format(d.gyr[0]) + " " +
								f0.format(d.gyr[1]) + " " +
								f0.format(d.gyr[2]) + " ";
						fos[1].write(gyrData.getBytes());
						
						String magData = 
								f0.format(d.mag[0]) + " " +
								f0.format(d.mag[1]) + " " +
								f0.format(d.mag[2]) + " ";
						fos[2].write(magData.getBytes());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		};
	}
	
	long debugCnt = 0;
}
