package com.example.admin.a2018iotkorea.ble;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class PolarBleService extends Service {
	public static final int CLICKER_CMD_RESET=0;
	public static final int CLICKER_CMD_SETTIME=1;
	public static final int CLICKER_CMD_BLE_ON=2;
	public static final int CLICKER_CMD_BLE_OFF=3;
	public static final int CLICKER_CMD_LED_BLINK=4;
	public static final int CLICKER_CMD_UPLOAD=5;
    
    private final static String TAG = PolarBleService.class.getSimpleName();
    //http://mbientlab.com/blog/bluetooth-low-energy-introduction/
    //Two very common 16-bit UUIDs that you will see are 2901, the Characteristic 
    //User Description attribute and 2902, the Client Characteristic Configuration attribute (to enable either �notify� or �indicate� on a characteristic).
    static final UUID CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "edu.ucsd.healthware.fw.device.ble.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "edu.ucsd.healthware.fw.device.ble.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "edu.ucsd.healthware.fw.device.ble.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "edu.ucsd.healthware.fw.device.ble.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "edu.ucsd.healthware.fw.device.ble.EXTRA_DATA";
    public final static String CLICKER_CMD =
            "edu.ucsd.healthware.fw.device.ble.CLICKER_CMD";
    public final static String UPLOAD_TO_GOOGLE_DOC =
            "edu.ucsd.healthware.fw.device.ble.UPLOAD_TO_GOOGLE_DOC";

    public final static String ACTION_HR_DATA_AVAILABLE =
            "edu.ucsd.healthware.fw.device.ble.ACTION_HR_DATA_AVAILABLE";

    
    public final static UUID UUID_HEART_RATE_MEASUREMENT =
            UUID.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT);

    public final static UUID UUID_BATTERY_LEVEL_MEASUREMENT =
            UUID.fromString(SampleGattAttributes.BATTERY_LEVEL_UUID);

    //private BluetoothGattCharacteristic mNotifyCharacteristic;
    private BluetoothGattCharacteristic mWriteCharacteristic;
    
    BioHarnessSessionData bioHarnessSessionData = new BioHarnessSessionData();
	public static final int BEAT_PERIOD_START = 10;
	public static final int BEAT_PERIOD_END = 400;
	public int rrX=50;
	public int beatPeriod=400;	//0 for the complete cycle, range 10 - 400;	after the period, oldest item is removed

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            //BluetoothProfile.STATE_CONNECTED not reliable, received it even without real device, use ACTION_GATT_SERVICES_DISCOVERED instead
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.w(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                //Log.w(TAG, "Attempting to start service discovery:" + mBluetoothGatt.discoverServices());
            	mBluetoothGatt.discoverServices();
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                //Log.w(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.w(TAG, "onServicesDiscovered received: " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            	getNotifyCharacteristic();
            } else {
               // Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
        	Log.w(TAG, "onCharacteristicRead received: " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
            	if (UUID_BATTERY_LEVEL_MEASUREMENT.equals(characteristic.getUuid())) {
            		Log.w(TAG, "Received battry level: "+characteristic.toString());
            	}
            	
            	Log.w(this.getClass().getName(), "## onCharacteristicRead() "+characteristic);
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
        	//Log.w(this.getClass().getName(), "onCharacteristicChanged() "+characteristic);
        	//Spec: https://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        	if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
        		boolean rrEnabled=false;
        		int valSize = characteristic.getValue().length;
    			int flag = characteristic.getProperties();
    			int format = -1;
    			int offset=0;
    			int heartRate;
    			int pnnValue=0;
    			int nnValue=0;
    			if ((flag & 0x01) != 0) {
    				format = BluetoothGattCharacteristic.FORMAT_UINT16;
    				//Log.w(TAG, "Heart rate format UINT16.");
    				heartRate = characteristic.getIntValue(format, 1);        			
        			offset=offset+2;
    			} else {
    				format = BluetoothGattCharacteristic.FORMAT_UINT8;
    				//Log.w(TAG, "Heart rate format UINT8.");
    				heartRate = characteristic.getIntValue(format, 1);        			
        			offset=offset+1;
    			}
    			
    			//Two energy bytes
    			offset=offset+2;

    			if ((flag & 0x10) != 0) {
    				rrEnabled=true;
    				//Log.w(TAG, "One or more RR-Interval values are present.");
    			} else {
    				rrEnabled=false;
    				//Log.w(TAG, "RR-Interval values are not present.");
    			}
    			
    			
    			Log.w(TAG, "Received heart rate: "+heartRate+" RR Enabled: "+rrEnabled+" Size:"+valSize+" Offset: "+offset);
    			//Parse RR value
    			//http://stackoverflow.com/questions/17422218/bluetooth-low-energy-how-to-parse-r-r-interval-value
    			int rrValue=0;	//in 1/1024 seconds
    			if(rrEnabled && (offset==(valSize-3))){
        			Log.w(TAG, "characteristic: "+characteristic);
        			bioHarnessSessionData.totalNN++;        			        		
        			rrValue = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset);
        			rrValue=rrValue/1024;
      				if(Math.abs(bioHarnessSessionData.lastRRvalue-rrValue)*1000>rrX){
      					bioHarnessSessionData.totalpNNx++;
      					nnValue = bioHarnessSessionData.totalpNNx; 					
      					pnnValue = (int)(100*bioHarnessSessionData.totalpNNx)/bioHarnessSessionData.totalNN;;
      					Log.w("pNNx", "rrValue: "+rrValue+" totalpNNx: "+bioHarnessSessionData.totalpNNx+" totalNN: "+bioHarnessSessionData.totalNN+" pNNvalue: "+pnnValue+" rrX: "+rrX);
      					if(beatPeriod>0){
      						bioHarnessSessionData.updateBeat(beatPeriod, new Integer(1));
      					}      					
      				}
      				bioHarnessSessionData.lastRRvalue=rrValue;
    			}
    			//rr=rr/1024;	
    			//Log.w(TAG, "Received heart rate: "+heartRate+" RR Enabled: "+rrEnabled+" RR Value: "+rr+" in Seconds: "+rr/1024+" Offset: "+offset);
    			//intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));
    			broadcastUpdate(ACTION_HR_DATA_AVAILABLE, heartRate+";"+pnnValue+";"+nnValue+";"+rrX);
    		} 
        	
        	if (UUID_BATTERY_LEVEL_MEASUREMENT.equals(characteristic.getUuid())) {
        		Log.w(TAG, "Received battry level: "+characteristic.toString());
        	}	
        }
        
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
           if(status == BluetoothGatt.GATT_SUCCESS) {
        	   //Log.w(TAG, "onCharacteristicWrite GATT_SUCCESS received: " + status+" Value: "+characteristic.getProperties());
           }else {
        	   //Log.w(TAG, "onCharacteristicWrite !GATT_SUCCESS received: " + status+" Value: "+characteristic.getProperties());
           }

        };
    };

    private void broadcastUpdate(final String action) {
        //final Intent intent = new Intent(action);
        //sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        final byte[] data = characteristic.getValue();
        if (data != null && data.length > 0) {
        	intent.putExtra(EXTRA_DATA, data);
        	
            final StringBuilder stringBuilder = new StringBuilder(data.length);
            for(byte byteChar : data)stringBuilder.append(String.format("%02X", byteChar));
        	//Log.w(TAG, "####Received size: " +data.length+" characteristic value:"+characteristic.getUuid()+" Value: "+stringBuilder);            
            intent.putExtra(EXTRA_DATA, new String(stringBuilder));
            
        }else{
        	//Log.w(TAG, "####Received characteristic:"+characteristic.getUuid());
        }	
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, String data) {
    	final Intent intent = new Intent(action);
    	intent.putExtra(EXTRA_DATA, data);
    	sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        public PolarBleService getService() {
        	//Log.w(TAG, "#### getService()");
            return PolarBleService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
    	Log.w(TAG, "#### onBind()");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
    	Log.w(TAG, "#### onUnbind()");
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    

    public boolean connect(final String address) {
        Log.w(TAG, "connect at address: "+address);
        if (mBluetoothAdapter == null || address == null) {
            //Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.w(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    public boolean connect() {
    	String address="00:07:80:78:9F:8B";
    	
        if (mBluetoothAdapter == null || address == null) {
            //Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            //Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            //Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        //Log.w(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            //Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.disconnect();
        //mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            //Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }
    
    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
    	//Log.w(TAG, "setCharacteristicNotification()");
		
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            //Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        
        // This is specific to Heart Rate Measurement.
    	//List<BluetoothGattDescriptor> list = characteristic.getDescriptors();
    	//Log.w(TAG, "BluetoothGattDescriptor size: "+list.size());    		
    	//for(int i=0; i<list.size(); i++){
    	//	BluetoothGattDescriptor desc = list.get(i);
        	//Log.w(TAG, "BluetoothGattDescriptor["+i+"] uuid: "+desc.getUuid());    		
    	//}
        
        //http://developer.android.com/guide/topics/connectivity/bluetooth-le.html#notification
        //http://stackoverflow.com/questions/17910322/android-ble-api-gatt-notification-not-received
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        Log.w(TAG, "setCharacteristicNotification() ENABLE_NOTIFICATION_VALUE");
		
        //descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);
        
    }
       
    public void writeDataToCharacteristic(byte[] dataToWrite) {
    	if (mWriteCharacteristic==null || mBluetoothAdapter == null || mBluetoothGatt == null) {
    		Log.w(TAG, "BluetoothAdapter or mWriteCharacteristic not initialized");
    		return;
    	}
    	mWriteCharacteristic.setValue(dataToWrite);
        mBluetoothGatt.writeCharacteristic(mWriteCharacteristic);
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

    void getNotifyCharacteristic(){
    	//mUuid	UUID  (id=830041990424)	
    	List<BluetoothGattService> gattServices = getSupportedGattServices();
        String uuid = null;
        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                uuid = gattCharacteristic.getUuid().toString();
                Log.w(TAG, "##gattCharacteristic.getUuid() "+uuid);
                
                if(uuid.compareTo(SampleGattAttributes.HEART_RATE_MEASUREMENT)==0){
                	//mNotifyCharacteristic = gattCharacteristic;
                    setCharacteristicNotification(gattCharacteristic, true);
                	//return;
                }
                
                if(uuid.compareTo(SampleGattAttributes.BATTERY_LEVEL_UUID)==0){
                	//mNotifyCharacteristic = gattCharacteristic;
                    //setCharacteristicNotification(gattCharacteristic, true);
                	readCharacteristic(gattCharacteristic);  
                	//return;
                }
                /*
                if(uuid.compareTo(SampleGattAttributes.CLICKER_WRITE_CHARACTERISTIC)==0){
                	mWriteCharacteristic = gattCharacteristic;
                	Log.w(TAG, "Set Write "+SampleGattAttributes.CLICKER_WRITE_CHARACTERISTIC+" vs "+uuid);
                }
                */
            }
        }

    }

    public String getmBluetoothDeviceAddress() {
        if (mBluetoothDeviceAddress == null) {
            return "00:22:D0:9C:FA:7A";
        }
        else {
            return mBluetoothDeviceAddress;
        }
    }
}