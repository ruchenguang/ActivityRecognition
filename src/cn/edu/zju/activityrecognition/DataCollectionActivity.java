package cn.edu.zju.activityrecognition;

import java.io.File;
import java.io.FileInputStream;
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

import cn.edu.zju.activityrecognition.MainActivity.HumanActivity;
import cn.edu.zju.activityrecognition.MainActivity.Step;
import cn.edu.zju.activityrecognition.tools.BluetoothService;
import cn.edu.zju.activityrecognition.tools.LpmsBData;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class DataCollectionActivity extends Activity {
	public static final String TAG = "ActivityRecognition::DataColletion";
	public static final int UNFINISHED = 0;
	public static final int FINISHED = 1;
	
	BroadcastReceiver homeButtonReceiver;
	WakeLock wakeLock; 
	
	Button startButton, redoButton;
	TextView currentTextView, nextTextView, next2TextView, pastTextView;
	MenuItem actionConnected, actionNotConnected, actionConnecting; 
	
	int activityIndex = 0;
	HumanActivity activity;
	int stepIndex = 0;
	ArrayList<Step> steps = new ArrayList<Step>();
	
	boolean isStarted = false;
	boolean isPaused = false;
	boolean isFinished = false;
	boolean isUserControl = false;
	boolean isLastSecond = false;
	boolean isActionScreenOff = false;
	
	File activityDir;
	FileOutputStream fos[] = null;
	
	Timer timer;
	TimerTask textViewUpdater, dataCollector;
	SoundPool soundPool;
	int clickSoundId, beepSoundId;
	
	//sensors inside the phone
	SensorManager sm;
	Sensor accelerometer;
	//Sensor gyroscope;
	MySensorEventListener sensorListener;
	float[] acceValues, gyroValues;
	FileOutputStream[] phoneSensorsFos = null;
	
	BroadcastReceiver bluetoothStateReceiver; 
	boolean isReceivingZeros;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_data_collection);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //get the connection state
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothService.ACTION_BT_CONNECTED);
        intentFilter.addAction(BluetoothService.ACTION_BT_NOT_CONNECTED);
        bluetoothStateReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if(action.equals(BluetoothService.ACTION_BT_CONNECTED)){
		            actionConnected.setVisible(true);
		            actionNotConnected.setVisible(false);
		            Toast.makeText(DataCollectionActivity.this, "Device is connected again", Toast.LENGTH_SHORT).show();
				}
				else if(action.equals(BluetoothService.ACTION_BT_NOT_CONNECTED)){
		            actionConnected.setVisible(false);
		            actionNotConnected.setVisible(true);
					Toast.makeText(DataCollectionActivity.this, "Warning! Device is not connected anymore", Toast.LENGTH_SHORT).show();
				}
				actionConnecting.setVisible(false);
			}
		};
        registerReceiver(bluetoothStateReceiver, intentFilter);
		
		//register a recivier for receiving screen on and off intent
        IntentFilter homeFilter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
		homeButtonReceiver = new HomeButtonBroadcastReceiver();
		registerReceiver(homeButtonReceiver, homeFilter);
		
		//get index from MainActivity
		Intent intent = getIntent();
		activityIndex = intent.getIntExtra(MainActivity.EXTRA_ACTIVITY, 0);
		activity = MainActivity.activities.get(activityIndex);
		
		activityDir = new File(InformationActivity.subjectDirPath, activity.name);
		
		if(activity.name.equals("climbing_upstairs") || activity.name.equals("climbing_downstairs"))
			isUserControl = true;
		
		//initiate the sensors inside the phone
		sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		sensorListener = new MySensorEventListener();
		accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		sm.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
//		gyroscope = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
//		sm.registerListener(sensorListener, gyroscope, SensorManager.SENSOR_DELAY_FASTEST);
		
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
			@SuppressWarnings("unused")
			@Override
			public void onClick(View v) {
				Animation animation = AnimationUtils.loadAnimation(DataCollectionActivity.this, R.anim.button_scale);			
				v.startAnimation(animation);
				if(isFinished){
					Toast.makeText(DataCollectionActivity.this, 
							"You have finished this test.", Toast.LENGTH_SHORT).show();
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
							"Error in the file system", Toast.LENGTH_SHORT).show();
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
				
				//write to file that finish didn't count
				writeCompletionState(UNFINISHED);
				
				delete(activityDir);
			}
		});
		
		//prepare the sound pool, you know, the "click" and the "beep-beep"
		soundPool = new SoundPool(3, AudioManager.STREAM_SYSTEM, 0);
		clickSoundId = soundPool.load(this, R.raw.click, 1);
		beepSoundId = soundPool.load(this, R.raw.beep, 0);
		
		prepareStartState();
		
		//if this activity is already finished
		if(intent.getBooleanExtra(MainActivity.EXTRA_ACTIVITY_ISFINISHED, false)){
			prepareFinishState();
		}
		
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DataCollection::WakeLock");
		wakeLock.acquire();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.action_connection, menu);
		actionConnected = menu.findItem(R.id.action_connected);
		actionNotConnected = menu.findItem(R.id.action_not_connected);
		actionConnecting = menu.findItem(R.id.action_connecting);
        if (BluetoothService.isConnected) {
            actionConnected.setVisible(true);
            actionNotConnected.setVisible(false);
        } else {
        	actionConnected.setVisible(false);
        	actionNotConnected.setVisible(true);
        }
        actionConnecting.setVisible(false);
        
        menu.findItem(R.id.action_settings).setEnabled(false);
		return super.onCreateOptionsMenu(menu);
	};
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(actionNotConnected.equals(item)){
			//change the actions on the action bar, from not-connected to connecting
			actionConnected.setVisible(false);
			actionNotConnected.setVisible(false);
			actionConnecting.setVisible(true);
			Toast.makeText(this, "Connecting the device, please wait.", Toast.LENGTH_SHORT).show();
			
			//stop the old service and start a new one 
			Intent serviceIntent = new Intent(this, BluetoothService.class);
			stopService(serviceIntent);
			startService(serviceIntent);
		}
		return super.onOptionsItemSelected(item);
	}
	
	// Called when activity is paused or screen orientation changes
    @SuppressLint("Wakelock") @Override
    protected void onDestroy() {
    	super.onDestroy();
    	
    	unregisterReceiver(homeButtonReceiver);
		destroy();
		if(!isFinished){
			delete(activityDir);
		}
		soundPool.release();
		wakeLock.release();
		unregisterReceiver(bluetoothStateReceiver);
    }
	
	void destroy(){
		timer.cancel();
		sm.unregisterListener(sensorListener);
		try {
			if(fos != null)
				for(int i=0; i<3; i++)
					if(fos[i] != null) fos[i].close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

    public static void delete(File file) {  
        if (file.isFile()) {  
            file.delete();  
            return;  
        }  
  
        if(file.isDirectory()){  
            File[] childFiles = file.listFiles();  
            if (childFiles == null || childFiles.length == 0) {  
               file.delete();  
                return;  
            }  
            for (int i = 0; i < childFiles.length; i++) {  
                delete(childFiles[i]);  
            }  
            file.delete();  
        }  
    } 
	
	void finishColleting(){
		writeCompletionState(FINISHED);
		
		prepareFinishState();
		
		timer.cancel();
		this.finish();
	}
	
	//init file for saving data
	void initDataFile(){
		if(!activityDir.exists())
			activityDir.mkdir();
		String dataPath = activityDir.getAbsolutePath();
		Date currentDate = new Date(System.currentTimeMillis()); 
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.getDefault()); 
		String fileName = formatter.format(currentDate) + ".txt";
		
		//init the data file for saving lpms-sensors data
		File[] activityDataFile = new File[3];
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
		
		//init the data file for saving sensors data inside the phone
		File phoneSensorDataDir = new File(activityDir, "phone_sensors");
		if(!phoneSensorDataDir.exists())
			phoneSensorDataDir.mkdir();
		String phoneSensorDataPath = phoneSensorDataDir.getAbsolutePath();
		
		File[] phoneSensorsDataFiles = new File[2];
		phoneSensorsDataFiles[0] = new File(phoneSensorDataPath, "acc_" + fileName);
//		phoneSensorsDataFiles[1] = new File(phoneSensorDataPath, "gyr_" + fileName);
		
		try {
			phoneSensorsFos = new FileOutputStream[2];
			phoneSensorsFos[0] = new FileOutputStream(phoneSensorsDataFiles[0]);
//			phoneSensorsFos[1] = new FileOutputStream(phoneSensorsDataFiles[1]);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
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
		if(currentStep.time == 0) currentTextView.setText(currentStep.stepDescription);
		else currentTextView.setText(currentStep.stepDescription + " " + currentStep.time + "s");
		if(steps.size()>(stepIndex+1)){
			Step nextStep = steps.get(stepIndex+1);
			if(nextStep.time == 0) nextTextView.setText(nextStep.stepDescription);
			else nextTextView.setText(nextStep.stepDescription + " " + nextStep.time + "s");
		} 
		else nextTextView.setText(" ");
		if(steps.size()>(stepIndex+2)){
			Step next2Step = steps.get(stepIndex+2);
			if(next2Step.time == 0) next2TextView.setText(next2Step.stepDescription);
			else next2TextView.setText(next2Step.stepDescription + " " + next2Step.time + "s");
		} 
		else next2TextView.setText(" ");

		//define the timer and timetasks
		timer = new Timer();
		textViewUpdater = new TimerTask() {
			@Override
			public void run() {
				if(isStarted && !isPaused && !isFinished){
					Handler handler = new Handler(Looper.getMainLooper());
					handler.post(new Runnable() {
						public void run() {
							int remainingTime = --steps.get(stepIndex).time;
							if(remainingTime == -1) {
								remainingTime = ++steps.get(stepIndex).time;
							}
							//update past step
							if(stepIndex>0) 
								pastTextView.setText(
										steps.get(stepIndex-1).stepDescription + " " + 
										steps.get(stepIndex-1).time + "s");
							//update current step
							currentTextView.setText(
									steps.get(stepIndex).stepDescription + " " + 
									steps.get(stepIndex).time + "s");
							//update next step
							if((stepIndex+1)<steps.size()) 
								nextTextView.setText(
										steps.get(stepIndex+1).stepDescription + " " + 
										steps.get(stepIndex+1).time + "s");
							else 
								nextTextView.setText(" ");
							//update next 2 step
							if((stepIndex+2)<steps.size())
								next2TextView.setText(
										steps.get(stepIndex+2).stepDescription + " " + 
										steps.get(stepIndex+2).time + "s");
							else 
								next2TextView.setText(" ");
							
							if(!isLastSecond)
								soundPool.play(clickSoundId, 1, 1, 1, 0, (float) 0.8);
							
							if(remainingTime == 0){ 
								if(stepIndex == steps.size()-1){
									if(isLastSecond){
										finishColleting();
										//play the beep sound for finish
										soundPool.play(beepSoundId, 1, 1, 1, 0, 1);
										//write the finish to file
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
			@SuppressWarnings("unused")
			@Override
			public void run() {
				if(isStarted && !isPaused && !isFinished){
					DecimalFormat f0 = new DecimalFormat("+000.00000;-000.00000");
					try {
						//collect data from lpms-b sensor
						LpmsBData d = BluetoothService.getSensorData();
						//Check the data correctness. With all zeros, there must be problems
						if (d.acc[0]==0 && d.acc[1]==0 && d.acc[2]==0
								&& d.gyr[0]==0 && d.gyr[1]==0 && d.gyr[2]==0
								&& d.mag[0]==0 && d.mag[1]==0 && d.mag[2]==0 
								&& !BluetoothService.isDebug) {
							if(!isReceivingZeros) isReceivingZeros = true;
							else{
								isReceivingZeros = false;
								Handler viewHandler = new Handler(Looper.getMainLooper());
								viewHandler.post(new Runnable() {
									@Override
									public void run() {
										startButton.setText("Continue");
										Toast.makeText(DataCollectionActivity.this, 
												"Too many 0s was received. Please check the Bluetooth connection!",  
												Toast.LENGTH_SHORT).show();
									}
								});
								isPaused = true;
							}
						} else 
							isReceivingZeros = false;
						
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
						
						//collect data from sensor inside the phone
						String accString = 
								f0.format(acceValues[0]) + " " +
								f0.format(acceValues[1]) + " " +
								f0.format(acceValues[2]) + " ";
						phoneSensorsFos[0].write(accString.getBytes());
						
//						String gyrString = 
//								f0.format(gyroValues[0]) + " " +
//								f0.format(gyroValues[1]) + " " +
//								f0.format(gyroValues[2]) + " ";
//						phoneSensorsFos[1].write(gyrString.getBytes());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		};
	}
	
	void prepareFinishState(){
		startButton.setText("Finished!");
		startButton.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
		isFinished = true;
		//return to main acitivty
		this.setResult(RESULT_OK);
	}
	
	void writeCompletionState(int state){
		File stateFile = MainActivity.activityCompletionStateFile;
		byte[] buffer = new byte[MainActivity.activityNum];
		
		FileInputStream fis;
		try {
			fis = new FileInputStream(stateFile);
			fis.read(buffer);
			
			for(int i=0; i<MainActivity.activityNum; i++){
				if(i == activityIndex) buffer[i] = (byte) (state + MainActivity.ASK_CODE_ZERO);
			}
			
			FileOutputStream fos = new FileOutputStream(stateFile);
			fos.write(buffer);
			
			fis.close();
			fos.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	class MySensorEventListener implements SensorEventListener{
		@Override
		public void onSensorChanged(SensorEvent event) {
			Sensor sensor = event.sensor;
			if(sensor.equals(accelerometer))
				acceValues = event.values;
//			if(sensor.equals(gyroscope))
//				gyroValues = event.values;
		}
		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}
	}

	class HomeButtonBroadcastReceiver extends BroadcastReceiver {
	    private static final String TAG = "HomeButtonReceiver";
	    private static final String SYSTEM_DIALOG_REASON_KEY = "reason";
	    private static final String SYSTEM_DIALOG_REASON_RECENT_APPS = "recentapps";	//Home button long-pressed
	    private static final String SYSTEM_DIALOG_REASON_HOME_KEY = "homekey";	//Home button pressed
//	    private static final String SYSTEM_DIALOG_REASON_ASSIST = "assist";
//	    private static final String SYSTEM_DIALOG_REASON_LOCK = "lock";
	    
	    @Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
	        Log.i(TAG, "onReceive(): action: " + action);
	        if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
	            String reason = intent.getStringExtra(SYSTEM_DIALOG_REASON_KEY);
	            Log.i(TAG, "reason: " + reason);

	            if (SYSTEM_DIALOG_REASON_HOME_KEY.equals(reason) ||
	            		SYSTEM_DIALOG_REASON_RECENT_APPS.equals(reason) ) {
	                // Home button pressed or long-pressed 
	                pauseRecord();
	            }
	        }
		}
	    
	    void pauseRecord(){
			if(isStarted && !isPaused && !isFinished){
				startButton.setText("Continue");
				isPaused = true;
			}
	    }
	}
}
