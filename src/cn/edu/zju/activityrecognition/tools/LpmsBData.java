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

package cn.edu.zju.activityrecognition.tools;

// Contains orientation data retrieved from LPMS-B
public class LpmsBData {
	// IMU ID
	public int imuId = 0;

	// Data timestamp
	public float timestamp = 0.0f;

	// Gyroscope data in deg / s
	public float[] gyr = new float[3];
	
	// Accelerometer data in m/s^2
	public float[] acc = new float[3];
	
	// Magnetometer data in uT
	public float[] mag = new float[3];
	
	// Orientation quaternion
	public float[] quat = new float[4];
	
	// Euler angles in degrees
	public float[] euler = new float[3];

	// Linear acceleration in m/s^2
	public float[] linAcc = new float[3];
	
	// Standard constructor
	public LpmsBData() {
	}
	
	// Copy constructor
	public LpmsBData(LpmsBData d) {
		imuId = d.imuId;
		timestamp = d.timestamp;
		
		for (int i=0; i<3; i++) gyr[i] = d.gyr[i];
		for (int i=0; i<3; i++) acc[i] = d.acc[i];
		for (int i=0; i<3; i++) mag[i] = d.mag[i];
		for (int i=0; i<4; i++) quat[i] = d.quat[i];
		for (int i=0; i<3; i++) euler[i] = d.euler[i];
		for (int i=0; i<3; i++) linAcc[i] = d.linAcc[i];
	}	
}