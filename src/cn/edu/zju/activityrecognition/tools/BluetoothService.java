package cn.edu.zju.activityrecognition.tools;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.UUID;


import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class BluetoothService extends Service {
	public static final String TAG = "BluetoothService";
	public static final String  BLUETOOTH_ADDRES =  "00:06:66:63:B2:BF";
	
	public static final String ACTION_BT_CONNECTED = "Bluetooth::connected";
	public static final String ACTION_BT_NOT_CONNECTED = "Bluetooth::not_connected";
	
	public static final String EXTRA_REASON_NOT_CONNECTED = "Bluetooth::reason_not_connected";
	
	BluetoothAdapter adapter;
	static LpmsBThread lpThread;
	
	public static final boolean isDebug = true;
	public static boolean isConnected = false;
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		adapter = BluetoothAdapter.getDefaultAdapter();
		
		Log.d(TAG, "Bluetooth service is created, the adapter is: " + adapter);
		super.onCreate();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "Bluetooth service is started.");
		
		if (adapter != null) {
			// Creates LPMS-B controller object using Bluetooth adapter mAdapter
			lpThread = new LpmsBThread(adapter);
			// Sets acquisition paramters (Must be the same as set in LpmsControl app)
			lpThread.setAcquisitionParameters(true, true, true, true, true, false);			
			// Tries to connect to LPMS-B with Bluetooth ID 00:06:66:48:E3:7A
			lpThread.connect(BLUETOOTH_ADDRES, 0);
		}
		return super.onStartCommand(intent, flags, startId);
	}
	
	@Override
	public void onDestroy() {
		Log.d(TAG, "Bluetooth service is destroyed");

		if(lpThread != null) lpThread.close();
		super.onDestroy();
	}
	
	public static LpmsBData getSensorData(){
		return lpThread.getLpmsBData();
	}
	
	// Thread class to retrieve data from LPMS-B (and eventually control configuration of LPMS-B)
	public class LpmsBThread extends Thread {
		// Log tag
		final String TAG = "LpmsB";
		// Standard Bluetooth serial protocol UUID
		final UUID MY_UUID_INSECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");	
		
		// LpBus identifiers
		final int PACKET_ADDRESS0 = 0;
		final int PACKET_ADDRESS1 = 1;
		final int PACKET_FUNCTION0 = 2;
		final int PACKET_FUNCTION1 = 3;
		final int PACKET_RAW_DATA = 4;
		final int PACKET_LRC_CHECK0 = 5;
		final int PACKET_LRC_CHECK1 = 6;
		final int PACKET_END = 7;
		final int PACKET_LENGTH0 = 8;
		final int PACKET_LENGTH1 = 9;
		
		// LPMS-B function registers (most important ones only, currently only LPMS_GET_SENSOR_DATA is used)
		final int LPMS_ACK = 0;
		final int LPMS_NACK = 1;
		final int LPMS_GET_CONFIG = 4;	
		final int LPMS_GET_STATUS = 5;	
		final int LPMS_GOTO_COMMAND_MODE = 6;	
		final int LPMS_GOTO_STREAM_MODE = 7;	
	 	final int LPMS_GOTO_SLEEP_MODE = 8;	
		final int LPMS_GET_SENSOR_DATA = 9;
	 	final int LPMS_SET_TRANSMIT_DATA = 10;	
		
		// State machine states. Currently no states are supported
		final int STATE_IDLE = 0;
		
		// Class members
		int rxState = PACKET_END;
		byte[] rxBuffer = new byte[512];
		byte[] txBuffer = new byte[512];
		byte[] rawTxData = new byte[256];
		byte[] rawRxBuffer = new byte[256];	
		int currentAddress;
		int currentFunction;
		int currentLength;
		int rxIndex = 0;
		byte b = 0;
		int lrcCheck;
		int nBytes;
		int timeout;
		boolean waitForAck;
		boolean waitForData;
		int state;
		byte inBytes[] = new byte[2];    
		InputStream mInStream;
		OutputStream mOutStream;
		BluetoothSocket mSocket;
		BluetoothAdapter mAdapter;
		String mAddress;
		BluetoothDevice mDevice;
		boolean isGetGyroscope = true;
		boolean isGetAcceleration = true;
		boolean isGetMagnetometer = true;
		boolean isGetQuaternion = true;
		boolean isGetEulerAngler = true;
		boolean isGetLinearAcceleration = true;
		int imuId = 0;
		DataOutputStream dos;
		public float startStamp = 0.0f;
		public boolean resetTimestamp = true;
		
		LpmsBData mLpmsBData = new LpmsBData();

		// Initializes object with Bluetooth adapter adapter
		LpmsBThread(BluetoothAdapter adapter) {
			mAdapter = adapter;
		}

		// Connects to LPMS-B with Bluetooth address address
		public void connect(String address, int id) {
			mAddress = address;
			imuId = id;
			
			Intent intent = new Intent();
			intent.setAction(ACTION_BT_NOT_CONNECTED);
			
			Log.e(TAG, "[LpmsBThread] Connect to: " + mAddress);

	        if (mAdapter == null) {
				Log.e(TAG, "[LpmsBThread] Didn't find Bluetooth adapter");
				intent.putExtra(EXTRA_REASON_NOT_CONNECTED, "Didn't find Bluetooth adapter");
				sendBroadcast(intent);
				return;
	        }
		
			mAdapter.cancelDiscovery();

			Log.e(TAG, "[LpmsBThread] Getting device");
			try {
				mDevice = mAdapter.getRemoteDevice(mAddress);
			} catch (IllegalArgumentException e) {
				Log.e(TAG, "[LpmsBThread] Invalid Bluetooth address", e);
				intent.putExtra(EXTRA_REASON_NOT_CONNECTED, "Invalid Bluetooth address");
				sendBroadcast(intent);
				return;
			}

			mSocket = null;
			Log.e(TAG, "[LpmsBThread] Creating socket");
			try {
				mSocket = mDevice.createInsecureRfcommSocketToServiceRecord(MY_UUID_INSECURE);
			} catch (Exception e) {
				Log.e(TAG, "[LpmsBThread] Socket create() failed", e);
				intent.putExtra(EXTRA_REASON_NOT_CONNECTED, "Socket create() failed");
				sendBroadcast(intent);
				return;
			}
		
			Log.e(TAG, "[LpmsBThread] Trying to connect..");	
			try {
				mSocket.connect();
			} catch (IOException e) {
				Log.e(TAG, "[LpmsBThread] Couldn't connect to device", e);
				intent.putExtra(EXTRA_REASON_NOT_CONNECTED, "Couldn't connect to device");
				sendBroadcast(intent);
				return;
			}
		
			Log.e(TAG, "[LpmsBThread] Connected!");		
		
			try {
				mInStream = mSocket.getInputStream();
				mOutStream = mSocket.getOutputStream();
			} catch (IOException e) {
				Log.e(TAG, "[LpmsBThread] Streams not created", e);
				intent.putExtra(EXTRA_REASON_NOT_CONNECTED, "Couldn't connect to device");
				sendBroadcast(intent);
				return;
			}			
			
			isConnected = true;
			
			intent.setAction(ACTION_BT_CONNECTED);
			sendBroadcast(intent);
			
			// Starts new reader thread
			Thread t = new Thread(new ClientReadThread());
	        t.start();
		}
		
		// Class to continuously read data from LPMS-B
	    public class ClientReadThread implements Runnable {
	        public void run() {
				// Starts state machine thread
	        	Thread t = new Thread(new ClientStateThread());	
	        	t.start();	
		
				while (mSocket.isConnected() == true) {
					try {
						nBytes = mInStream.read(rawRxBuffer);
					} catch (Exception e) {
						break;
					}
					
					// Parses received LpBus data
					parse();
				}
			}
		}	
		
		// State machine thread class
	    public class ClientStateThread implements Runnable {
	        public void run() {
				try {				
					while (mSocket.isConnected() == true) {
						if (waitForAck == false && waitForData == false) {
							switch (state) {
							case STATE_IDLE:
							break;
							}
						} else if (timeout > 100) {
							Log.d(TAG, "[LpmsBThread] Receive timeout");
							timeout = 0;
							state = STATE_IDLE;
							waitForAck = false;
							waitForData = false;
						} else {
							Thread.sleep(10);
							++timeout;
						} 
				
					}
				} catch (Exception e) {
					Log.d(TAG, "[LpmsBThread] Connection interrupted");
					isConnected = false;			
				}
			}
		}
		
		// Sets acquisition parameters. Selects which data is to be sampled.
		// Important: These setting need to correspond with your sensor settings.
		void setAcquisitionParameters(	boolean isGetGyroscope,
										boolean isGetAcceleration,
										boolean isGetMagnetometer,
										boolean isGetQuaternion,
										boolean isGetEulerAngler,
										boolean isGetLinearAcceleration) {
			this.isGetGyroscope = isGetGyroscope;
			this.isGetAcceleration = isGetAcceleration;
			this.isGetMagnetometer = isGetMagnetometer;
			this.isGetQuaternion = isGetQuaternion;
			this.isGetEulerAngler = isGetEulerAngler;
			this.isGetLinearAcceleration = isGetLinearAcceleration;
		}
		
		// Parses received sensor data (received with function value LPMS_GET_SENSOR_DATA)
		void parseSensorData() {
			int o = 0;
			float r2d = 57.2958f;
		
			mLpmsBData.imuId = imuId;	

			if (resetTimestamp == true) {
				startStamp = convertRxbytesToFloat(o, rxBuffer);
				resetTimestamp = false;
			}
			
			mLpmsBData.timestamp = convertRxbytesToFloat(o, rxBuffer) - startStamp; o += 4;
			
		 	if (isGetGyroscope == true) {
				mLpmsBData.gyr[0] = convertRxbytesToFloat(o, rxBuffer) * r2d; o += 4;
				mLpmsBData.gyr[1] = convertRxbytesToFloat(o, rxBuffer) * r2d; o += 4;
				mLpmsBData.gyr[2] = convertRxbytesToFloat(o, rxBuffer) * r2d; o += 4;
			}
		
		 	if (isGetAcceleration == true) {	
				mLpmsBData.acc[0]	= convertRxbytesToFloat(o, rxBuffer); o += 4;
				mLpmsBData.acc[1]	= convertRxbytesToFloat(o, rxBuffer); o += 4;
				mLpmsBData.acc[2]	= convertRxbytesToFloat(o, rxBuffer); o += 4;
			}

		 	if (isGetMagnetometer == true) {
				mLpmsBData.mag[0]	= convertRxbytesToFloat(o, rxBuffer); o += 4;
				mLpmsBData.mag[1]	= convertRxbytesToFloat(o, rxBuffer); o += 4;
				mLpmsBData.mag[2]	= convertRxbytesToFloat(o, rxBuffer); o += 4;
			}
			
		 	if (isGetQuaternion == true) {
				mLpmsBData.quat[0]	= convertRxbytesToFloat(o, rxBuffer); o += 4;
				mLpmsBData.quat[1]	= convertRxbytesToFloat(o, rxBuffer); o += 4;
				mLpmsBData.quat[2]	= convertRxbytesToFloat(o, rxBuffer); o += 4;
				mLpmsBData.quat[3]	= convertRxbytesToFloat(o, rxBuffer); o += 4;
			}
			
		 	if (isGetEulerAngler == true) {
				mLpmsBData.euler[0] = convertRxbytesToFloat(o, rxBuffer) * r2d; o += 4;
				mLpmsBData.euler[1] = convertRxbytesToFloat(o, rxBuffer) * r2d; o += 4;
				mLpmsBData.euler[2] = convertRxbytesToFloat(o, rxBuffer) * r2d; o += 4;
			}
			
		 	if (isGetLinearAcceleration == true) {	
				mLpmsBData.linAcc[0] = convertRxbytesToFloat(o, rxBuffer); o += 4;
				mLpmsBData.linAcc[1] = convertRxbytesToFloat(o, rxBuffer); o += 4;
				mLpmsBData.linAcc[2] = convertRxbytesToFloat(o, rxBuffer); o += 4;
			}
		}
		
		// Returns LpmsBData structure with current sensor data
		LpmsBData getLpmsBData() {
			LpmsBData d = new LpmsBData(mLpmsBData);
			return d;
		}
		
		// Parses LpBus function
		void parseFunction() {	
			switch (currentFunction) {
			case LPMS_ACK:
				Log.d(TAG, "[LpmsBThread] Received ACK");
			break;

			case LPMS_NACK:
				Log.d(TAG, "[LpmsBThread] Received NACK");
			break;
			
			case LPMS_GET_CONFIG:
			break;
			
			case LPMS_GET_STATUS:
			break;
			
			case LPMS_GOTO_COMMAND_MODE:
			break;
			
			case LPMS_GOTO_STREAM_MODE:
			break;
	 	
			case LPMS_GOTO_SLEEP_MODE:
			break;
			
			// If new sensor data is received parse the data
		 	case LPMS_GET_SENSOR_DATA:
				parseSensorData();
			break;
			
	 		case LPMS_SET_TRANSMIT_DATA:
			break;
			}
			
			waitForAck = false;
			waitForData = false;
		}
		
		// Parses LpBus raw data
		void parse() {
			int lrcReceived = 0;
		
			for (int i=0; i<nBytes; i++) {
				b = rawRxBuffer[i];
			
				switch (rxState) {
				case PACKET_END:
					if (b == 0x3a) {
						rxState = PACKET_ADDRESS0;
					}
					break;
					
				case PACKET_ADDRESS0:
					inBytes[0] = b;
					rxState = PACKET_ADDRESS1;
					break;

				case PACKET_ADDRESS1:
					inBytes[1] = b;				
					currentAddress = convertRxbytesToInt16(0, inBytes);
					rxState = PACKET_FUNCTION0;
					break;

				case PACKET_FUNCTION0:
					inBytes[0] = b;
					rxState = PACKET_FUNCTION1;				
					break;

				case PACKET_FUNCTION1:
					inBytes[1] = b;				
					currentFunction = convertRxbytesToInt16(0, inBytes);			
					rxState = PACKET_LENGTH0;	
				break;

				case PACKET_LENGTH0:
					inBytes[0] = b;
					rxState = PACKET_LENGTH1;
				break;
						
				case PACKET_LENGTH1:
					inBytes[1] = b;				
					currentLength = convertRxbytesToInt16(0, inBytes);	
					rxState = PACKET_RAW_DATA;
					rxIndex = 0;
				break;
						
				case PACKET_RAW_DATA:
					if (rxIndex == currentLength) {
						lrcCheck = (currentAddress & 0xffff) + (currentFunction & 0xffff) + (currentLength & 0xffff);
						
						for (int j=0; j<currentLength; j++) {
							lrcCheck += (int) rxBuffer[j] & 0xff;
						}		
							
						inBytes[0] = b;		
						rxState = PACKET_LRC_CHECK1;								
					} else {	
						rxBuffer[rxIndex] = b;
						++rxIndex;
					}
				break;
					
				case PACKET_LRC_CHECK1:
					inBytes[1] = b;
					
					lrcReceived = convertRxbytesToInt16(0, inBytes);
					lrcCheck = lrcCheck & 0xffff;

					if (lrcReceived == lrcCheck) {	
						parseFunction();
					} else {
					}
					
					rxState = PACKET_END;
				break;
				
				default:
					rxState = PACKET_END;
				break;
				}
			}
		}
		
		// Sends data to sensor	
		void sendData(int address, int function, int length) {
			int txLrcCheck;

			txBuffer[0] = 0x3a;
			convertInt16ToTxbytes(address, 1, txBuffer);
			convertInt16ToTxbytes(function, 3, txBuffer);
			convertInt16ToTxbytes(length, 5, txBuffer);
			
			for (int i=0; i < length; ++i) {
				txBuffer[7+i] = rawTxData[i];
			}
			
			txLrcCheck = address;
			txLrcCheck += function;
			txLrcCheck += length;
			
			for (int i=0; i < length; i++) {
				txLrcCheck += (int) rawTxData[i];
			}
			
			convertInt16ToTxbytes(txLrcCheck, 7 + length, txBuffer);
			txBuffer[9 + length] = 0x0d;
			txBuffer[10 + length] = 0x0a;
				
			for (int i=0; i < 11 + length; i++) {
				Log.d(TAG, "[LpmsBThread] Sending: " + Byte.toString(txBuffer[i]));		
			}
				
			try {
				Log.d(TAG, "[LpmsBThread] Sending data");
				mOutStream.write(txBuffer, 0, length+11);
			} catch (Exception e) {
				Log.d(TAG, "[LpmsBThread] Error while sending data");
			}
		}
		
		// Sends ACK to sensor
		void sendAck() {
			sendData(0, LPMS_ACK, 0);
		}
		
		// Sends NACK to sensor
		void sendNack() {
			sendData(0, LPMS_NACK, 0);
		}
		
		// Converts received 32-bit word to float values
		float convertRxbytesToFloat(int offset, byte buffer[]) {
			byte[] t = new byte[4];
			
			for (int i=0; i<4; i++) {
				t[3-i] = buffer[i+offset];
			}
			
			return Float.intBitsToFloat(ByteBuffer.wrap(t).getInt(0)); 
		}
		
		// Converts received 32-bit word to int value
		int convertRxbytesToInt(int offset, byte buffer[]) {
			int v;
			byte[] t = new byte[4];
			
			for (int i=0; i<4; i++) {
				t[3-i] = buffer[i+offset];
			}
			
			v = ByteBuffer.wrap(t).getInt(0);
			
			return v; 
		}
		
		// Converts received 16-bit word to int value
		int convertRxbytesToInt16(int offset, byte buffer[]) {
			int v;
			byte[] t = new byte[2];
			
			for (int i=0; i<2; ++i) {
				t[1-i] = buffer[i+offset];
			}
			
			v = (int) ByteBuffer.wrap(t).getShort(0) & 0xffff;
			
			return v; 
		}	
		
		// Converts 32-bit int value to output bytes
		void convertIntToTxbytes(int v, int offset, byte buffer[]) {
			byte[] t = ByteBuffer.allocate(4).putInt(v).array();
		
			for (int i=0; i<4; i++) {
				buffer[3-i+offset] = t[i];
			}
		}
		
		// Converts 16-bit int value to output bytes
		void convertInt16ToTxbytes(int v, int offset, byte buffer[]) {
			byte[] t = ByteBuffer.allocate(2).putShort((short) v).array();
		
			for (int i=0; i<2; i++) {
				buffer[1-i+offset] = t[i];
			}
		}	

		// Converts 32-bit float value to output bytes
		void convertFloatToTxbytes(float f, int offset, byte buffer[]) {
			int v = Float.floatToIntBits(f);
			byte[] t = ByteBuffer.allocate(4).putInt(v).array();
		
			for (int i=0; i<4; i++) {
				buffer[3-i+offset] = t[i];
			}
		}
		
		// Closes connection to sensor
		public void close() {
			isConnected = false;	
		
			try {
				mSocket.close();
			} catch (Exception e) {
			}
			
			Log.d(TAG, "[LpmsBThread] Connection closed");		
		}
		
		public void resetTimestamp()
		{
			resetTimestamp = true;
		}
	}
}
