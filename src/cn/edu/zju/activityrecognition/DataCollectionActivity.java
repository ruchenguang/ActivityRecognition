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

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class DataCollectionActivity extends Activity {
	BluetoothAdapter mAdapter;
	LpmsBThread mLpmsB;
	
	Button startButton, redoButton;
	TextView currentTextView, nextTextView, next2TextView, pastTextView;

	int step = 0;
	ArrayList<String> sittingStepsArrayList = new ArrayList<String>();
	ArrayList<Integer> sittingStepsPeriodArrayList = new ArrayList<Integer>(); 
	
	boolean isStarted = false;
	boolean isPaused = false;
	boolean isFinished = false;
	
	File activityDataFile;
	FileOutputStream fos;
	
	Timer timer;
	TimerTask textViewUpdater, dataCollector;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_data_collection);
		
		pastTextView = (TextView) findViewById(R.id.textView2);
		currentTextView = (TextView) findViewById(R.id.TextView01);
		nextTextView = (TextView) findViewById(R.id.TextView02);
		next2TextView = (TextView) findViewById(R.id.TextView03);
		
		startButton = (Button) findViewById(R.id.button2);
		redoButton = (Button) findViewById(R.id.button1);
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
				} else {
					//start data collection
					if(fos == null) Toast.makeText(DataCollectionActivity.this, 
							"Error in the file system", Toast.LENGTH_LONG).show();
					else if(!mLpmsB.isConnected) 
						Toast.makeText(DataCollectionActivity.this, 
							"The sensor is not connected", Toast.LENGTH_LONG).show();
					else{
						timer.schedule(dataCollector, 10, 10);
						startButton.setText("Pause");
						isStarted = true;
						
						timer.schedule(textViewUpdater, 0, 1000);
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
		
		mAdapter = BluetoothAdapter.getDefaultAdapter();	
		
		prepareStartState();
	}
	
	// Everytime the activity is resumed re-connect to LPMS-B
    @Override
    protected void onResume() {
		if (mAdapter != null) {
			// Creates LPMS-B controller object using Bluetooth adapter mAdapter
			mLpmsB = new LpmsBThread(mAdapter);
			// Sets acquisition paramters (Must be the same as set in LpmsControl app)
			mLpmsB.setAcquisitionParameters(true, true, true, true, true, false);			
			// Tries to connect to LPMS-B with Bluetooth ID 00:06:66:48:E3:7A
			mLpmsB.connect("00:06:66:63:B2:BF", 0);
		}	
		
        super.onResume();
    }
    
	@Override
    protected void onPause() {
		// Disconnects LPMS-B
		if (mLpmsB != null) mLpmsB.close();
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
	
	void initSteps(){
		step = 0;
		sittingStepsArrayList.clear();
		sittingStepsPeriodArrayList.clear();
		
		sittingStepsArrayList.add("Sitting straight ");
		sittingStepsPeriodArrayList.add(20);
		
		sittingStepsArrayList.add("Lean forward ");
		sittingStepsPeriodArrayList.add(5);
		
		sittingStepsArrayList.add("Lean backward ");
		sittingStepsPeriodArrayList.add(5);
		
		sittingStepsArrayList.add("Rotate trunk to left ");
		sittingStepsPeriodArrayList.add(5);
		
		sittingStepsArrayList.add("Rotate trunk to right ");
		sittingStepsPeriodArrayList.add(5);
		
		sittingStepsArrayList.add("Stand up ");
		sittingStepsPeriodArrayList.add(10);
	}
	
	void initDataFile(){
		//init the data file
		Date currentDate = new Date(System.currentTimeMillis()); 
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.getDefault()); 
		String fileName = "sitting" + formatter.format(currentDate) + ".txt";
		activityDataFile = new File(InformationActivity.subjectDirPath, fileName);
		try {
			fos = new FileOutputStream(activityDataFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	void prepareStartState(){
		initSteps();
		initDataFile();
		
		isStarted = false;
		isFinished = false;
		isPaused = false;
		startButton.setText("Start");
		
		pastTextView.setText(" ");
		currentTextView.setText(sittingStepsArrayList.get(step) + "for " + sittingStepsPeriodArrayList.get(step) + "s");
		nextTextView.setText(sittingStepsArrayList.get(step+1) + "for " + sittingStepsPeriodArrayList.get(step+1) + "s");
		next2TextView.setText(sittingStepsArrayList.get(step+2) + "for " + sittingStepsPeriodArrayList.get(step+2) + "s");
		
		//define the timer and timetasks
		timer = new Timer();
		textViewUpdater = new TimerTask() {
			@Override
			public void run() {
				if(isStarted && !isPaused && !isFinished){
					Handler handler = new Handler(Looper.getMainLooper());
					handler.post(new Runnable() {
						public void run() {
							int remainingTime = sittingStepsPeriodArrayList.get(step)-1;
							sittingStepsPeriodArrayList.set(step, remainingTime);
							if(step>0) 
								pastTextView.setText(sittingStepsArrayList.get(step-1) + "for " + sittingStepsPeriodArrayList.get(step-1) + "s");
							currentTextView.setText(sittingStepsArrayList.get(step) + "for " + sittingStepsPeriodArrayList.get(step) + "s");
							if((step+1)<sittingStepsArrayList.size()) 
								nextTextView.setText(sittingStepsArrayList.get(step+1) + "for " + sittingStepsPeriodArrayList.get(step+1) + "s");
							else 
								nextTextView.setText(" ");
							if((step+2)<sittingStepsArrayList.size())
								next2TextView.setText(sittingStepsArrayList.get(step+2) + "for " + sittingStepsPeriodArrayList.get(step+2) + "s");
							else 
								next2TextView.setText(" ");
							
							if(remainingTime == 0) 
								if(step == sittingStepsArrayList.size()-1){
									timer.cancel();
									startButton.setText("Finished!");
									isFinished = true;
								}
								else 
									step++;
						}
					});
				}
			}
		};
		dataCollector = new TimerTask() {
			@Override
			public void run() {
				if(isStarted && !isPaused && !isFinished){
					DecimalFormat f0 = new DecimalFormat("+000.0000;-000.0000");
					LpmsBData d = mLpmsB.getLpmsBData();
					try {
						fos.write((f0.format(d.acc[0])+" ").getBytes());
						fos.write((f0.format(d.acc[1])+" ").getBytes());
						fos.write((f0.format(d.acc[2])+" ").getBytes());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		};
	}
	
	void destroy(){
		timer.cancel();
		try {
			fos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
