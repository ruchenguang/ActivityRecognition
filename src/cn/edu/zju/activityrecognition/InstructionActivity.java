package cn.edu.zju.activityrecognition;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation.AnimationListener;
import android.widget.Button;
import android.widget.TextView;

public class InstructionActivity extends Activity {
	TextView stepTv, instructionTv;
	Button btn;
	
	Context context;
	
	int step = 1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_instruction);
		ExitApplication.activityList.add(this);
		
		stepTv = (TextView) findViewById(R.id.textView1);
		instructionTv = (TextView) findViewById(R.id.TextView01);
		
		btn = (Button) findViewById(R.id.buttonNext);
		btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Animation animation = AnimationUtils.loadAnimation(InstructionActivity.this, R.anim.button_scale);
				animation.setAnimationListener(new AnimationListener() {
					@Override
					public void onAnimationStart(Animation animation) {
					}
					@Override
					public void onAnimationRepeat(Animation animation) {
					}
					@Override
					public void onAnimationEnd(Animation animation) {
						Intent intent = new Intent(InstructionActivity.this, MainActivity.class);
						startActivity(intent);
					}
				});
				v.startAnimation(animation);
			}
		});
	}
}
