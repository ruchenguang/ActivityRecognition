package cn.edu.zju.activityrecognition;

import android.R.color;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation.AnimationListener;
import android.widget.Button;
import android.widget.Toast;

public class ConnectionActivity extends Activity {
	Button btnNext, btnConnect; 
	Intent serviceIntent;
	
	private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if(action.equals(BluetoothService.ACTION_BT_CONNECTED) || BluetoothService.isDebug){
				btnConnect.setText(R.string.connected);
				btnConnect.setTextColor(getResources().getColor(color.holo_blue_dark));
				
				btnNext.setTextColor(getResources().getColor(color.holo_blue_dark));
				btnNext.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						Animation animation = AnimationUtils.loadAnimation(ConnectionActivity.this, R.anim.button_scale);
						animation.setAnimationListener(new AnimationListener() {
							@Override
							public void onAnimationStart(Animation animation) {
							}
							@Override
							public void onAnimationRepeat(Animation animation) {
							}
							@Override
							public void onAnimationEnd(Animation animation) {
								Intent intent = new Intent(ConnectionActivity.this, InstructionActivity.class);
								startActivity(intent);
							}
						});
						v.startAnimation(animation);
					}
				});				
			}
			if(action.equals(BluetoothService.ACTION_BT_NOT_CONNECTED) && !BluetoothService.isDebug){
				stopService(serviceIntent);
				
				btnConnect.setText(R.string.connection_failed);
				btnConnect.setClickable(true);
				
				Toast.makeText(ConnectionActivity.this, 
						intent.getStringExtra(BluetoothService.EXTRA_REASON_NOT_CONNECTED), 
						Toast.LENGTH_SHORT).show();				
			}
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_connection);
		ExitApplication.activityList.add(this);
	}
	
	@Override
	protected void onResume() {
		btnConnect = (Button) findViewById(R.id.Button02);
		btnConnect.setText(R.string.button_connect);
		btnConnect.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
		btnConnect.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Animation animation = AnimationUtils.loadAnimation(ConnectionActivity.this, R.anim.button_scale);
				animation.setAnimationListener(new AnimationListener() {
					@Override
					public void onAnimationStart(Animation animation) {
					}
					@Override
					public void onAnimationRepeat(Animation animation) {
					}
					@Override
					public void onAnimationEnd(Animation animation) {
						//connect the bluetooth
						btnConnect.setText(R.string.connecting);
						btnConnect.setClickable(false);
						serviceIntent = new Intent(getApplicationContext(), BluetoothService.class);
						startService(serviceIntent);
					}
				});
				v.startAnimation(animation);
			}
		});
		
		btnNext = (Button) findViewById(R.id.buttonNext);
		btnNext.setClickable(false);
		btnNext.setTextColor(getResources().getColor(android.R.color.darker_gray));
		
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothService.ACTION_BT_CONNECTED);
		intentFilter.addAction(BluetoothService.ACTION_BT_NOT_CONNECTED);
		
		registerReceiver(broadcastReceiver, intentFilter);
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		unregisterReceiver(broadcastReceiver);
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		stopService(serviceIntent);
		super.onDestroy();
	}
}
