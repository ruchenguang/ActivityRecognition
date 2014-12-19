package cn.edu.zju.activityrecognition.tools;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Application;

public class ExitApplication extends Application {
	public static List<Activity> activityList = new ArrayList<Activity>();
}
