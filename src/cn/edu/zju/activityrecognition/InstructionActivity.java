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
		
		stepTv = (TextView) findViewById(R.id.textView1);
		instructionTv = (TextView) findViewById(R.id.TextView01);
		
		btn = (Button) findViewById(R.id.button1);
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
						if(step == 1){
							stepTv.setText(R.string.step2);
							instructionTv.setText(R.string.step2_content);
							btn.setText(R.string.ready);
							step ++;
						}
						else if(step == 2){
							Intent intent = new Intent(InstructionActivity.this, LpmsBMainActivity.class);
							startActivity(intent);
						}
					}
				});
				v.startAnimation(animation);
			}
		});
	}
}
