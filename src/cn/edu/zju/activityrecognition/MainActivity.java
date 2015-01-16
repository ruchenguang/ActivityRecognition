package cn.edu.zju.activityrecognition;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cn.edu.zju.activityrecognition.tools.BluetoothService;
import cn.edu.zju.activityrecognition.tools.ExitApplication;

import android.app.*;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation.AnimationListener;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import android.os.*;

// Main activity. Connects to LPMS-B and displays orientation values
public class MainActivity extends android.app.Activity{
	public static final String TAG = "ActivityRecognition::MainActivity";
	public static final String EXTRA_ACTIVITY = "MainActivity::extra_activity";	
	public static final String EXTRA_ACTIVITY_ISFINISHED = "MainActivity::extra_activity_isfinshed";	
	public static final String BUNDLE_FINISHED_ACTIVITIES = "MainActivity::bundle_finished_activities";
	public static final byte ASK_CODE_ZERO = 48;
	
	public static ArrayList<HumanActivity> activities;
	public static ArrayList<String> activityTitles;
	public static int activityNum; 

	ActivityAdapter adapter;
	
	public static File activityCompletionStateFile;
	int finishedActivities = 0;
	boolean isAllFinished = false;
	
	ListView listView;
	TextView tv;
	Button finishButton;

	@Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);	
        ExitApplication.activityList.add(this);
        
        //initiate activities' title and number
        activities = getActivities();
    	activityNum = activities.size();

    	listView = (ListView) findViewById(R.id.listView1);
        adapter = new ActivityAdapter(this, activities);
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
				if(isAllFinished){
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
							AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
							builder.setTitle(R.string.title_exit_finished);
							builder.setMessage(R.string.message_exit_finished);
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
						}
					});
					v.startAnimation(animation);
				}
				else {
					Toast.makeText(MainActivity.this, "Not all the activities are finished yet. Please go on.", Toast.LENGTH_SHORT).show();
				}
			}
		});
    }
    
    @Override
    protected void onResume() {
        activityCompletionStateFile = new File(InformationActivity.subjectDirPath, "activityCompletionState.txt");
        if(activityCompletionStateFile.exists()){
        	finishedActivities = 0;
        	try {
    			FileInputStream fis = new FileInputStream(activityCompletionStateFile);
            	byte[] buffer = new byte[activityNum];
            	fis.read(buffer);
            	fis.close();
            	for(int i=0; i<activityNum; i++){
            		if(buffer[i]-ASK_CODE_ZERO  == 1){
            			finishedActivities++;
            			activities.get(i).isFinished = true;
            		} else {
            			activities.get(i).isFinished = false;
            		}
            	}
            	Log.d(TAG, finishedActivities + " activities have been finished!");	
            } 
    		catch (FileNotFoundException e) {
    			e.printStackTrace();	
    		} 
        	catch (IOException e) {
				e.printStackTrace();	
    		}
        } 
        else {
        	//create a new file for saving finish state
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
        
		if(finishedActivities == activityNum){
    		finishButton.setText(R.string.button_finished);
    		finishButton.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
    		isAllFinished = true;
    	} 
    	else {
    		finishButton.setText(R.string.button_not_finished);
    		finishButton.setTextColor(getResources().getColor(android.R.color.darker_gray));
    		isAllFinished = false;
    	}	
        
        adapter.notifyDataSetChanged();
    	super.onResume();
    }
    
    ArrayList<HumanActivity> getActivities(){
    	ArrayList<HumanActivity> activities = new ArrayList<HumanActivity>();
    	
    	//initiate activities
    	ArrayList<Step> steps;
    	//********************simple activity********************
    	//sitting
    	steps = new ArrayList<Step>();
    	steps.add(new Step("Sit straight", 60));
    	activities.add(new HumanActivity(
    			"sitting", 
    			R.string.sitting, 
    			R.string.instruction_activity_sitting,
    			R.drawable.sitting,
    			steps));
    	//standing
    	steps = new ArrayList<Step>();
    	steps.add(new Step("Stand still", 60));
    	activities.add(new HumanActivity(
    			"standing", 
    			R.string.standing,
    			R.string.instruction_activity_standing,
    			R.drawable.standing,
    			steps));
    	//lying
    	steps = new ArrayList<Step>();
    	steps.add(new Step("Lying down", 60));
    	activities.add(new HumanActivity(
    			"lying", 
    			R.string.lying,
    			R.string.instruction_activity_lying,
    			R.drawable.lying,
    			steps));
    	//walking
    	steps = new ArrayList<Step>();
    	steps.add(new Step("Walking", 120));
    	activities.add(new HumanActivity(
    			"walking", 
    			R.string.walking,
    			R.string.instruction_activity_walking,
    			R.drawable.walking,
    			steps));
    	//running
    	steps = new ArrayList<Step>();
    	steps.add(new Step("Runnning", 120));
    	activities.add(new HumanActivity(
    			"running", 
    			R.string.running,
    			R.string.instruction_activity_running,
    			R.drawable.running,
    			steps));
    	//climbing upstairs
    	steps = new ArrayList<Step>();
    	steps.add(new Step("Climbing Upstairs", 0));
    	activities.add(new HumanActivity(
    			"climbing_upstairs", 
    			R.string.climbing_upstairs,
    			R.string.instruction_activity_climbing_upstairs,
    			R.drawable.climbing,
    			steps
    	));
    	//climbing downstairs
    	steps = new ArrayList<Step>();
    	steps.add(new Step("Climbing Downstairs", 0));
    	activities.add(new HumanActivity(
    			"climbing_downstairs", 
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
    	activities.add(new HumanActivity(
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
    	activities.add(new HumanActivity(
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
    	activities.add(new HumanActivity(
    			"relative_lying", 
    			R.string.relative_lying,
    			R.string.instruction_activity_relative_lying,
    			R.drawable.lying,
    			steps));
    	
    	//********************phone in pocket ********************
    	//sitting with phone in pocket
    	steps = new ArrayList<Step>();
    	steps.add(new Step("Put the phone in pocket and then sit straight", 10));
    	steps.add(new Step("Keep sitting straight", 60));
    	activities.add(new HumanActivity(
    			"sitting_with_phone_in_pocket", 
    			R.string.sitting_with_phone_in_pocket, 
    			R.string.instruction_activity_pocket_sitting,
    			R.drawable.sitting,
    			steps));
    	//standing with phone in pocket
    	steps = new ArrayList<Step>();
    	steps.add(new Step("Put the phone in pocket and then stand", 10));
    	steps.add(new Step("Keep standing", 60));
    	activities.add(new HumanActivity(
    			"standding_with_phone_in_pocket", 
    			R.string.standing_with_phone_in_pocket, 
    			R.string.instruction_activity_pocket_standing,
    			R.drawable.standing,
    			steps));
    	//sitting with phone in pocket
    	steps = new ArrayList<Step>();
    	steps.add(new Step("Put the phone in pocket and then sit straight", 10));
    	steps.add(new Step("Keep sitting straight", 60));
    	activities.add(new HumanActivity(
    			"lying_with_phone_in_pocket", 
    			R.string.lying_with_phone_in_pocket, 
    			R.string.instruction_activity_pocket_lying,
    			R.drawable.lying,
    			steps));
    	//sitting with phone in pocket
    	steps = new ArrayList<Step>();
    	steps.add(new Step("Put the phone in pocket and then sit straight", 10));
    	steps.add(new Step("Keep sitting straight", 60));
    	activities.add(new HumanActivity(
    			"walking_with_phone_in_pocket", 
    			R.string.walking_with_phone_in_pocket, 
    			R.string.instruction_activity_pocket_walking,
    			R.drawable.walking,
    			steps));
    	
    	return activities;
    }
    
    class HumanActivity {
    	public String title;
    	String name;
    	String instruction;
    	int picResourceId;
    	boolean isFinished = false;
    	
    	ArrayList<Step> steps = new ArrayList<Step>();
    	public HumanActivity(
    			String activityName, 
    			int titleResouceId, int instructionResouceId, int picResourceId, 
    			ArrayList<Step> steps) {
    		this.name = activityName;
    		this.title = getResources().getString(titleResouceId);
    		this.instruction = getResources().getString(instructionResouceId);
    		this.picResourceId = picResourceId;
    		this.steps.addAll(steps);
    	}
    	
    	public ArrayList<Step> getSteps() {
    		ArrayList<Step> stepsToCopy = new ArrayList<Step>();
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

	class ActivityAdapter extends BaseAdapter {
		Context context;
		LayoutInflater inflater;
		ArrayList<HumanActivity> activities;
		
		
		public ActivityAdapter(Context context, ArrayList<HumanActivity> activities) {
			this.context = context;
			inflater = LayoutInflater.from(context);
			this.activities = activities;
		}
		
		@Override
		public int getCount() {
			return activities.size();
		}

		@Override
		public Object getItem(int position) {
			return activities.get(position);
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if(convertView == null){
				convertView = inflater.inflate(R.layout.listview_string, parent, false);
				holder = new ViewHolder();
				holder.tv = (TextView) convertView.findViewById(R.id.textView1);
				convertView.setTag(holder);
			} 
			else {
				holder = (ViewHolder) convertView.getTag();
			}
			
			HumanActivity activity = activities.get(position);
			holder.tv.setText(activity.title);
			if(activity.isFinished) 
				holder.tv.setTextColor(getResources().getColor(android.R.color.darker_gray));
			else
				holder.tv.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
			return convertView;
		}

		class ViewHolder{
			public TextView tv;
		}
	}
}