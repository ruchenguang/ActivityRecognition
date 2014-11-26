package cn.edu.zju.activityrecognition;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

public class InformationActivity extends Activity {
	RadioButton	femaleButton, maleButton;
	EditText heightEditText;
	Button nextButton;
	
	int id = -1;
	float height = -1;
	int gender = -1;
	static final int FEMALE = 0;
	static final int MALE = 1;
	
	File activityRecognitionDir;
	public static String subjectDirPath;
	String idKey = "id_number";
	final String EXTRA_PATH = "ActivityRecognition::SubjectDataPath";
	
	SharedPreferences sp;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_information);
		setTitle(R.string.title_information_activity);
		
		heightEditText = (EditText) findViewById(R.id.editText1);
		
		femaleButton = (RadioButton) findViewById(R.id.radioButton1);
		femaleButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				maleButton.setChecked(false);
				gender = FEMALE;
			}
		});
		maleButton = (RadioButton) findViewById(R.id.RadioButton01);
		maleButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				femaleButton.setChecked(false);
				gender = MALE;
			}
		});
		
		nextButton = (Button) findViewById(R.id.button1);
		nextButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Animation animation = AnimationUtils.loadAnimation(InformationActivity.this, R.anim.button_scale);
				animation.setAnimationListener(new AnimationListener() {
					@Override
					public void onAnimationStart(Animation animation) {
					}
					@Override
					public void onAnimationRepeat(Animation animation) {
					}
					@Override
					public void onAnimationEnd(Animation animation) {
						try{
							height = Float.parseFloat(heightEditText.getText().toString());
						} catch (Exception e){
							Toast.makeText(InformationActivity.this, getResources().getString(R.string.info_height_incomplete), Toast.LENGTH_LONG).show();
						}
						if(height>0 && gender>=0){
							initSubject();
							
							Intent intent = new Intent(InformationActivity.this, InstructionActivity.class);
							intent.putExtra(EXTRA_PATH, subjectDirPath);
							startActivity(intent);
						} else if(gender<0) {
							Toast.makeText(InformationActivity.this, getResources().getString(R.string.info_gender_incomplete), Toast.LENGTH_LONG).show();
						} else {
							Toast.makeText(InformationActivity.this, getResources().getString(R.string.info_height_incorrect), Toast.LENGTH_LONG).show();
						}
					}
				});
				v.startAnimation(animation);
			}
		});
		
		//create the root directory ActivityRecognitionTest
		activityRecognitionDir = new File(Environment.getExternalStorageDirectory(), "ActivityRecognitionExperiment");
		if(!activityRecognitionDir.exists()){
			activityRecognitionDir.mkdir();
		}
		
		sp = InformationActivity.this.getSharedPreferences("id_record", MODE_PRIVATE);
		id = sp.getInt(idKey, -1);
		if(id == -1) id = 1;
		else id++;
	}
	
	void initSubject(){
		//create a folder for this subject with its ID
		File subjectDir = new File(activityRecognitionDir.getAbsoluteFile(), "subject_"+id);
		if(!subjectDir.exists()){
			subjectDir.mkdir();
		}
		
		//save subject info to a file
		try {
			File subjectInfo = new File(subjectDir.getAbsoluteFile(), "subject_info.txt");
			FileOutputStream fos = new FileOutputStream(subjectInfo);
			
			//write the gender
			fos.write(getResources().getString(R.string.info_gender).getBytes());
			if(gender == FEMALE)
				fos.write(getResources().getString(R.string.gender_female).getBytes());
			else 
				fos.write(getResources().getString(R.string.gender_male).getBytes());
			fos.write(";".getBytes());
			
			//write the height
			fos.write(getResources().getString(R.string.info_height).getBytes());
			fos.write(String.valueOf(height).getBytes());
			fos.write("ft".getBytes());
			fos.write(";".getBytes());
			
			fos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		subjectDirPath = subjectDir.getAbsolutePath();
		
		//edit the subject number in the sp file
		Editor editor = sp.edit();
		editor.putInt(idKey, id);
		editor.commit();
	}
}
