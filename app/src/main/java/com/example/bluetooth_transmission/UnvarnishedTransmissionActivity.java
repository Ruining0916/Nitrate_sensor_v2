package com.example.bluetooth_transmission;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
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
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.method.KeyListener;
import android.util.Log;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;

import com.example.bluetooth_transmission.R;

import android.view.inputmethod.InputMethodManager;
import android.widget.Button;

import android.widget.EditText;

import android.widget.TextView;

import android.widget.Toast;




import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;


public class UnvarnishedTransmissionActivity extends Activity {
    // Debug
    private static final String TAG = "UTransmissionActivity";
    private boolean D = true;

    // Intent request codes
    private static final int REQUEST_SEARCH_BLE_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    // msg for ui update
    private static final int MSG_EDIT_RX_STRING_UPDATE = 1;
    private static final int MSG_POWER_UPDATE = 2;
    private static final int MSG_CONNECTION_STATE_UPDATE = 4;

    private static final int MSG_SENDBUTTON_UPDATE = 6;

    // MTU size
    private static int MTU_SIZE_EXPECT = 300;
    private static int MTU_PAYLOAD_SIZE_LIMIT = 20;

    // Return flag
    public static final String EXTRAS_DEVICE = "DEVICE";
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";


    // flag for connect state
    private boolean isConnectDevice = false;


    // Tx interval
    private int mTxInterval;
    private int MIN_TX_INTERVAL = 20;


    // should add volatile
    private volatile boolean isWriteCharacteristicOk = true;
    private volatile boolean writeCharacteristicError = false;


    private ThreadUnpackSend mUnpackThread;


    private DecimalFormat speedFormate = new DecimalFormat("###,###,###.##");


    // RX string builder, store the rx data
    // max rx speed is MAX_RX_BUFFER/RX_STRING_UPDATE_TIME = 40KByte/s
    private StringBuilder mStringBuilder_ovl;
    private StringBuilder mStringBuilder_tmp;
    private int MAX_RX_BUFFER = 2000;
    private int RX_STRING_UPDATE_TIME = 50;
    private int MAX_RX_SHOW_BUFFER = 5000;

    // Unpack sending flag
    private boolean isUnpackSending = false;

    // Selected Device information
    private String mDeviceName;
    private String mDeviceAddress;
    private BluetoothDevice mDevice;

    // bluetooth control
    private BluetoothAdapter mBluetoothAdapter;


    // Bluetooth GATT
    private BluetoothGatt mBluetoothGatt;

    // Bluetooth GATT Service
    private BluetoothGattService mBluetoothGattService;

    // Test Characteristic
    private BluetoothGattCharacteristic mTestCharacter;

    // bluetooth Manager
    private BluetoothManager mBluetoothManager;
    // Button
    private Button mbtnTx, mbtnSave,mbtnTxIst;
    private Button mbtnClearAll, mbtnClearLast;
    private TextView tv_text_ovl,tv_text_tmp;
    private EditText other_txt,number_edt,time_edt;
    private List<String> dataList;
    SaveData saveData ;
    private int thisLength_tmp = 0;
    private int thisLength_ovl = 0;
    // State control
    private GattConnectState mConnectionState;
    private int number = 5;
    private int time = 100;
    private int sendCount = 0;
    private boolean isIstSend = false;
    boolean firstReceive = false;
    private enum GattConnectState {
        STATE_INITIAL,
        STATE_DISCONNECTED,
        STATE_CONNECTING,
        STATE_CONNECTED,
        STATE_CHARACTERISTIC_CONNECTED,
        STATE_CHARACTERISTIC_NOT_FOUND;
    }

    // UUID, modify for new spec
    //private final static UUID TEST_SERVICE_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");      //ffe0
    private final static UUID TEST_SERVICE_UUID = UUID.fromString("0000e0ff-3c17-d293-8e48-14fe2e4da212");      //123bit
    private final static UUID TEST_CHARACTERISTIC_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");

    /**
     * Client configuration descriptor that will allow us to enable notifications and indications
     */
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");   //geyun:just need support ccc

    // InputMethodManager
    InputMethodManager mInputMethodManager;

    private double slopeValue;
    private double interceptValue;

    private EditText ediSlope;
    private EditText editIntercept;

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unvarnished_transmission);
        if (D) Log.d(TAG, "-------onCreate-------");
        // Set Title

        //getActionBar().setTitle("impedance test");
        //getActionBar().setDisplayHomeAsUpEnabled(true);
        // get the bluetooth adapter
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // judge whether android have bluetooth
        if (null == mBluetoothAdapter) {
            if (D) Log.e(TAG, "This device do not support Bluetooth");
            Dialog alertDialog = new AlertDialog.Builder(this).
                    setMessage("This device do not support Bluetooth").
                    create();
            alertDialog.show();
        }
        saveData = new SaveData();
        // ensure that Bluetooth exists
        if (!EnsureBleExist()) {
            if (D) Log.e(TAG, "This device do not support BLE");
            finish();
        }

        dataList = new ArrayList<String>();
        // Button
        mbtnTx = (Button) findViewById(R.id.btn_scan);
        mbtnTxIst = (Button) findViewById(R.id.btn_send_ist);
        time_edt = (EditText) findViewById(R.id.time);
        number_edt = (EditText) findViewById(R.id.number);
        mbtnClearAll = (Button) findViewById(R.id.btn_clear_all);
        mbtnClearLast = (Button) findViewById(R.id.btn_clear_last);
        tv_text_ovl = (TextView) findViewById(R.id.tv_text_ovl);
        tv_text_tmp = (TextView) findViewById(R.id.tv_text_tmp);
        other_txt = (EditText) findViewById(R.id.other_txt);
        mbtnSave = (Button) findViewById(R.id.btn_save);
        // Button listener 
        mbtnTx.setOnClickListener(new ButtonClick());
        mbtnClearAll.setOnClickListener(new ButtonClick());
        mbtnClearLast.setOnClickListener(new ButtonClick());
        mbtnSave.setOnClickListener(new ButtonClick());
        mbtnTxIst.setOnClickListener(new ButtonClick());

        // RX string builder initial capacity with MAX_RX_BUFFER
        mStringBuilder_ovl = new StringBuilder();
        mStringBuilder_tmp = new StringBuilder();
        // Get the bluetooth manager
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        // Update the connection state
        SetConnectionState(GattConnectState.STATE_INITIAL);

        //InputMethodManager
        mInputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        slopeValue = 0;
        interceptValue = 0;
        ediSlope = (EditText)findViewById(R.id.text2);
        editIntercept = (EditText)findViewById(R.id.text3);

        ediSlope.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                String input_value = ediSlope.getText().toString();
                slopeValue = Double.parseDouble(input_value);
            }
        });

        editIntercept.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                String input_value = editIntercept.getText().toString();
                interceptValue = Double.parseDouble(input_value);
            }
        });

    }

    @Override
    protected void onResume() {
        if (D) Log.d(TAG, "-------onResume-------");
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                if (D) Log.d(TAG, "start bluetooth");
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menu.clear();
        // if not find the special characteristic
        if (GattConnectState.STATE_CHARACTERISTIC_CONNECTED != mConnectionState) {
            menu.add("1").setIcon(R.drawable.select).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        } else {
            menu.add("2").setIcon(R.drawable.disconnect).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        if (D) Log.d(TAG, "-------onOptionsItemSelected-------");
        switch (item.getItemId()) {
            case 0:
                // if have a gatt connection, disconnect and unregister it
                if (GattConnectState.STATE_CHARACTERISTIC_CONNECTED == mConnectionState
                        || GattConnectState.STATE_CONNECTED == mConnectionState
                        || GattConnectState.STATE_CHARACTERISTIC_NOT_FOUND == mConnectionState) {

                    // only find the characteristic, then return, disconnect will very soon change the state, so do like this
                    if (GattConnectState.STATE_CHARACTERISTIC_CONNECTED == mConnectionState) {
                        // Try to disconnect the gatt server, ensure unregister the callback (in the callback register it)
                        if (BluetoothProfile.STATE_CONNECTED == mBluetoothManager.getConnectionState(mDevice, BluetoothProfile.GATT)) {
                            mBluetoothGatt.disconnect();
                        }
                        return true;
                    }

                    // Try to disconnect the gatt server, ensure unregister the callback (in the callback register it)
                    // here din't return
                    if (BluetoothProfile.STATE_CONNECTED == mBluetoothManager.getConnectionState(mDevice, BluetoothProfile.GATT)) {
                        mBluetoothGatt.disconnect();
                    }
                }

                // if didn't hava a gatt connect, find a device and create it
                if (D) Log.i(TAG, "start select ble device");

                // start the device search activity for select device
                Intent intent = new Intent(this, DeviceSearchActivity.class);
                startActivityForResult(intent, REQUEST_SEARCH_BLE_DEVICE);

                break;

            default:
                if (D) Log.e(TAG, "something may error");
                break;
        }
        return true;
    }

    @Override
    public void onStop() {
        if (D) Log.d(TAG, "-------onStop-------");
        // Do something when activity on stop
        // kill the auto unpack thread
        if (null != mUnpackThread) {
            mUnpackThread.interrupt();
        }

        super.onStop();
    }

    @Override
    public void onDestroy() {
        if (D) Log.d(TAG, "-------onDestroy-------");

        // Try to disconnect the gatt server
        if (BluetoothProfile.STATE_CONNECTED == mBluetoothManager.getConnectionState(mDevice, BluetoothProfile.GATT)) {
            mBluetoothGatt.disconnect();
        }

        mTestCharacter = null;

        super.onDestroy();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (D) Log.d(TAG, "-------onActivityResult-------");
        if (D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
            case REQUEST_SEARCH_BLE_DEVICE:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    //Stroe the information
                    //mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
                    //mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
                    mDevice = intent.getParcelableExtra(EXTRAS_DEVICE);
                    if (D)
                        Log.e(TAG, "select a device, the name is " + mDevice.getName() + ", addr is " + mDevice.getAddress());

                    // Update the connection state
                    SetConnectionState(GattConnectState.STATE_CONNECTING);

                    ConnectDevice(mDevice);
                } else {
                    // do nothing
                    if (D) Log.e(TAG, "the result code is error!");
                }
                break;

            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    //do nothing
                    Toast.makeText(this, "Bt is enabled!", Toast.LENGTH_SHORT).show();
                } else {
                    // User did not enable Bluetooth or an error occured
                    Toast.makeText(this, "Bt is not enabled!", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;

            default:
                break;
        }
        super.onActivityResult(requestCode, resultCode, intent);
    }

    // judge the support of ble in android  
    private boolean EnsureBleExist() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }


    // cloae the input method
    private void closeInputMethod() {
        boolean isOpen = mInputMethodManager.isActive();
        if (isOpen) {
            // imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);//û����ʾ����ʾ
            mInputMethodManager.hideSoftInputFromWindow(UnvarnishedTransmissionActivity.this.getCurrentFocus().getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    class ButtonClick implements OnClickListener {
        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            switch (v.getId()) {
                //If the Tx button click
                case R.id.btn_scan:
                    // send message to remote
                    isIstSend = false;
                    firstReceive = true;
                    SendMessageToRemote();
                    mbtnTx.setEnabled(false);
                    mbtnTxIst.setEnabled(false);
                    break;
                case R.id.btn_send_ist:
                    sendCount = 0;
                    isIstSend = true;
                    if(time_edt.getText().toString().equals("")||time_edt.getText().toString().equals("0")){
                        time = 100;
                    }else{
                        time = Integer.parseInt(time_edt.getText().toString());
                        if(time<100){
                            time = 100;
                        }
                    }
                    if(number_edt.getText().toString().equals("")||number_edt.getText().toString().equals("0")){
                        number = 5;
                    }else{
                        number = Integer.parseInt(number_edt.getText().toString());
                    }
                    firstReceive = true;
                    SendMessageToRemote();
                    mbtnTx.setEnabled(false);
                    mbtnTxIst.setEnabled(false);
                    break;
                //If the Tx button click
                case R.id.btn_clear_all:
                    //Clear the rx buffer
                    mStringBuilder_ovl.delete(0, mStringBuilder_ovl.length());
                    mStringBuilder_tmp.delete(0, mStringBuilder_tmp.length());
                    tv_text_ovl.setText(mStringBuilder_ovl.toString());
                    tv_text_tmp.setText(mStringBuilder_tmp.toString());
                    thisLength_tmp = 0;
                    thisLength_ovl = 0;
                    dataList.clear();

                    break;
                case R.id.btn_clear_last:
                    if(dataList.size()==0){
                        return;
                    }
                    mStringBuilder_ovl.delete(mStringBuilder_ovl.length()-thisLength_ovl, mStringBuilder_ovl.length());
                    mStringBuilder_tmp.delete(mStringBuilder_tmp.length()-thisLength_tmp, mStringBuilder_tmp.length());
                    dataList.remove(dataList.size()-1);
                    tv_text_ovl.setText(mStringBuilder_ovl.toString());
                    tv_text_tmp.setText(mStringBuilder_tmp.toString());
                    break;
                case R.id.btn_save:
                    if(dataList.size()==0){
                        Toast.makeText(UnvarnishedTransmissionActivity.this,"无数据",Toast.LENGTH_SHORT).show();
                        return;
                    }
                    saveData.saveData(dataList,UnvarnishedTransmissionActivity.this);
                    dataList.clear();
                    mStringBuilder_ovl.delete(0, mStringBuilder_ovl.length());
                    mStringBuilder_tmp.delete(0, mStringBuilder_tmp.length());
                    tv_text_ovl.setText(mStringBuilder_ovl.toString());
                    tv_text_tmp.setText(mStringBuilder_tmp.toString());
                    thisLength_tmp = 0;
                    thisLength_ovl = 0;
                    break;
            }

        }
    }

    // send data to the remote device and update ui
    public void SendMessageToRemote() {
        byte[] sendData;


        sendData = StringByteTrans.Str2Bytes("#2000000400@");
        //sendData = DigitalTrans.AsciiString2Byte(medtTxString.getText().toString());


        // send data to the remote device
        mUnpackThread = new ThreadUnpackSend(sendData);
        mUnpackThread.start();

        if (D) Log.d(TAG, "send data is: " + Arrays.toString(sendData));


    }


    public void SendMessageToRemote(String s) {
        byte[] sendData;

        sendData = StringByteTrans.Str2Bytes(s);

        // send data to the remote device
        mUnpackThread = new ThreadUnpackSend(sendData);
        mUnpackThread.start();

        if (D) Log.d(TAG, "send data is: " + Arrays.toString(sendData));


    }

    public boolean ConnectDevice(BluetoothDevice device) {
        if (device == null) {
            if (D) Log.e(TAG, "Device not found.  Unable to connect.");
            return false;
        }

        // Try to connect the gatt server
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        if (D) Log.d(TAG, "Trying to create a new connection.");
        return D;

    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            if (D) Log.e(TAG, "onMtuChanged new mtu is " + mtu);
            if (D) Log.e(TAG, "onMtuChanged new status is " + String.valueOf(status));
            // change the mtu real payloaf size
            MTU_PAYLOAD_SIZE_LIMIT = mtu - 3;

            // Attempts to discover services after successful connection.
            if (D) Log.i(TAG, "Attempting to start service discovery:" +
                    mBluetoothGatt.discoverServices());
        }


        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (D) Log.d(TAG, "the new staus is " + newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // Update the connection state
                SetConnectionState(GattConnectState.STATE_CONNECTED);

                if (D) Log.i(TAG, "Connected to GATT server.");

                // only android 5.0 add the requestMTU feature
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    // Attempts to discover services after successful connection.
                    if (D) Log.i(TAG, "Attempting to start service discovery:" +
                            mBluetoothGatt.discoverServices());
                } else {
                    if (D)
                        Log.i(TAG, "Attempting to request mtu size, expect mtu size is: " + String.valueOf(MTU_SIZE_EXPECT));
                    mBluetoothGatt.requestMtu(MTU_SIZE_EXPECT);
                }

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // Update the connection state
                SetConnectionState(GattConnectState.STATE_DISCONNECTED);

                // Try to close the gatt server, ensure unregister the callback
                if (null != mBluetoothGatt) {
                    mBluetoothGatt.close();
                }

                if (D) Log.i(TAG, "Disconnected from GATT server.");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // get all service on the remote device
                final List<BluetoothGattService> services = gatt.getServices();

                for (BluetoothGattService service : services) {
                    if (D) Log.d(TAG, "the GATT server uuid is " + service.getUuid());
                    // get all Characteristics in the service, then we can use the UUID or the attribute handle to get the Characteristics's value
                    List<BluetoothGattCharacteristic> characters = service.getCharacteristics();
                    for (BluetoothGattCharacteristic character : characters) {
                        if (D)
                            Log.d(TAG, "the GATT server include Characteristics uuid is " + character.getUuid());
                        if (D)
                            Log.d(TAG, "the Characteristics's permissoion is " + character.getPermissions());
                        if (D)
                            Log.d(TAG, "the Characteristics's properties is 0x" + Integer.toHexString(character.getProperties()));
                        // get all descriptors in the characteristic
                        List<BluetoothGattDescriptor> descriptors = character.getDescriptors();
                        for (BluetoothGattDescriptor descriptor : descriptors) {
                            if (D)
                                Log.d(TAG, "the Characteristics include descriptor uuid is " + descriptor.getUuid());
                        }

                        if (character.getUuid().equals(TEST_CHARACTERISTIC_UUID)) {
                            // find the test character, then we can use it to set value or get value.
                            mTestCharacter = character;
                            enableNotification(mBluetoothGatt, mTestCharacter);
                            // Update the connection state
                            SetConnectionState(GattConnectState.STATE_CHARACTERISTIC_CONNECTED);
                        }
                    }
                }

                // if not find the special characteristic
                if (GattConnectState.STATE_CHARACTERISTIC_CONNECTED != mConnectionState) {
                    // Update the connection state
                    SetConnectionState(GattConnectState.STATE_CHARACTERISTIC_NOT_FOUND);
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            byte[] data;
            if (TEST_CHARACTERISTIC_UUID.equals(characteristic.getUuid())) {
                data = characteristic.getValue();
                // call function to deal the data
                onDataReceive(data);
            } else {
                if (D) Log.w(TAG, "receive other notification");
                return;
            }
        }

        @Override
        public void onDescriptorWrite(final BluetoothGatt gatt, final BluetoothGattDescriptor descriptor, final int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID.equals(descriptor.getUuid())) {
                    if (D) Log.d(TAG, "CCC:  ok,try to write test----> ");
                }
            } else {
                if (D) Log.e(TAG, "Descriptor write error: " + status);
            }
        }

        ;

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            // Here can do something to verify the write

            if (TEST_CHARACTERISTIC_UUID.equals(characteristic.getUuid())) {
                if (D) Log.d(TAG, "onCharacteristicWrite UUID is: " + characteristic.getUuid());
                if (D)
                    Log.d(TAG, "onCharacteristicWrite data value:" + Arrays.toString(characteristic.getValue()));

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    writeCharacteristicError = false;
                } else {
                    writeCharacteristicError = true;
                    if (D) Log.e(TAG, "Characteristic write error: " + status + "try again.");
                }

                isWriteCharacteristicOk = true;
            }

        }
    };

    String test_data = "";
    boolean flag = false;

    // data receive
    public void onDataReceive(byte[] data) {
        if(D) Log.d(TAG,"Slope:"+slopeValue+"intercept:"+interceptValue);
        if (D) Log.d(TAG, "receive data is: " + StringByteTrans.Byte2String(data));


        if ((StringByteTrans.Byte2String(data).contains("#") || flag)&&firstReceive) {
            test_data = test_data + StringByteTrans.Byte2String(data);
            flag = true;
            System.out.println("here" + test_data);
            if (StringByteTrans.Byte2String(data).contains("@")) {
                flag = false;

                String final_data = test_data;
                test_data = "";
                if (getData(final_data)) {
                    firstReceive = false;
                    if(final_data.trim().length()==6){
                        String power_data = final_data.replace("#", "").replace("@", "").trim();
                        int power = Integer.parseInt(power_data.substring(1, 2));
                        System.out.println("power"+power);
                        if(power<2){
                            Message message = new Message();
                            message.what = MSG_POWER_UPDATE;
                            message.arg1 = 1;
                            handler.sendMessage(message);

                        }else{
                            Message message = new Message();
                            message.what = MSG_POWER_UPDATE;
                            message.arg1 = 2;
                            handler.sendMessage(message);

                        }

                        SendMessageToRemote("#@G");
                        Message msg = new Message();
                        msg.what = MSG_SENDBUTTON_UPDATE;
                        handler.sendMessage(msg);
                    }else{
                        SendMessageToRemote("G");
                        sendCount++;
                        if(isIstSend&&sendCount==number){
                            isIstSend = false;
                        }
                        if(!isIstSend){
                            Message msg = new Message();
                            msg.what = MSG_SENDBUTTON_UPDATE;
                            handler.sendMessage(msg);
                        }
                    }


                    if(final_data.length()>6){

                        String o_txt = other_txt.getText().toString().length()==0?"default":other_txt.getText().toString();
                        float old_youmi = Float.parseFloat(final_data.replace("#", "").replace("@", "").trim().substring(1,5));
                        float new_youmi = (2500-1500*((old_youmi*2f-1f)/3520f))/(((old_youmi*2f-1f)/3520f)-1);
                        String str = "impedance:"+(int)new_youmi+
                                "|temperature:"+final_data.replace("#", "").replace("@", "").trim().substring(5,7)+"."+final_data.replace("#", "").replace("@", "").trim().substring(7,8)
                                +"|test condition:"+o_txt+"\n";
                        Log.d(TAG,"current: "+new_youmi);
                        String ovl_str = "concentration:"+Double.toString(Math.pow(1.0*new_youmi/slopeValue,1.0/interceptValue))+"\n";
                        //String ovl_str = "impedance:"+(int)new_youmi+"\n";
                        String tmp_str = "temperature:"+final_data.replace("#", "").replace("@", "").trim().substring(5,7)+"."+final_data.replace("#", "").replace("@", "").trim().substring(7,8)+"\n";
                        mStringBuilder_ovl.append(ovl_str);
                        mStringBuilder_tmp.append(tmp_str);
                        dataList.add(str);
                        thisLength_ovl = ovl_str.length();
                        thisLength_tmp = tmp_str.length();
                        Message message = new Message();
                        message.what = MSG_EDIT_RX_STRING_UPDATE;
                        handler.sendMessage(message);
                    }

                }
            }

        }


        // send the msg, here may send less times MSG, so we use the StringBuildler
        //Message message = new Message();
        //message.what = MSG_EDIT_RX_STRING_UPDATE;
        //handler.sendMessage(message);

    }

    public boolean getData(String s) {

        String s_data = s.replace("#", "").replace("@", "").trim();

        List<Integer> ch_data = new ArrayList<Integer>();
        for (int i = 0; i < s_data.trim().length(); i++) {
            try {

                ch_data.add(Integer.parseInt(s_data.substring(i, i + 1)));
            } catch (NumberFormatException e) {
                return false;
            }


        }
        System.out.println("s_data_length" + s_data.length() + ",,,ch_data" + ch_data.get(0));
        if (s_data.length() == ch_data.get(0) + 3) {
            int code = 0;
            for (int i = 0; i < ch_data.get(0); i++) {
                code = code + ch_data.get(i + 1);
            }
            System.out.println("code" + code + ",,," + Integer.parseInt(String.valueOf(ch_data.get(ch_data.get(0) + 1) + String.valueOf(ch_data.get(ch_data.get(0) + 2)))));
            if (code == Integer.parseInt(String.valueOf(ch_data.get(ch_data.get(0) + 1) + String.valueOf(ch_data.get(ch_data.get(0) + 2))))) {

                return true;
            }
            return false;
        }
        return false;
    }

    // for ui update
    final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == MSG_EDIT_RX_STRING_UPDATE) {
                tv_text_ovl.setText(mStringBuilder_ovl.toString());
                tv_text_tmp.setText(mStringBuilder_tmp.toString());
                if(isIstSend){
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            firstReceive = true;
                            SendMessageToRemote();
                        }
                    },time);

                }
            } else if (msg.what == MSG_CONNECTION_STATE_UPDATE) {
                if (GattConnectState.STATE_INITIAL == mConnectionState) {

                    if (D) Log.i(TAG, "The connection state now is STATE_INITIAL");
                    // Initial state

                    mbtnTx.setEnabled(false);
                    mbtnTxIst.setEnabled(false);
                } else if (GattConnectState.STATE_CONNECTING == mConnectionState) {
                    if (D) Log.i(TAG, "The connection state now is STATE_CONNECTING");
                    mbtnTx.setEnabled(false);
                    mbtnTxIst.setEnabled(false);
                } else if (GattConnectState.STATE_DISCONNECTED == mConnectionState) {
                    if (D) Log.i(TAG, "The connection state now is STATE_DISCONNECTED");

                    mbtnTx.setEnabled(false);
                    mbtnTxIst.setEnabled(false);
                    // change the connect state
                    isConnectDevice = false;

                    // Update Menu
                    invalidateOptionsMenu();


                } else if (GattConnectState.STATE_CONNECTED == mConnectionState) {
                    if (D) Log.i(TAG, "The connection state now is STATE_CONNECTED");
                    mbtnTx.setEnabled(false);
                    mbtnTxIst.setEnabled(false);
                } else if (GattConnectState.STATE_CHARACTERISTIC_CONNECTED == mConnectionState) {
                    if (D) Log.i(TAG, "The connection state now is STATE_CHARACTERISTIC_CONNECTED");

                    mbtnTx.setEnabled(false);
                    mbtnTxIst.setEnabled(false);
                    // change the connect state
                    isConnectDevice = true;

                    // Update Menu
                    invalidateOptionsMenu();

                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            firstReceive = true;
                            SendMessageToRemote("#2000000499@");
                        }
                    },100);

                } else if (GattConnectState.STATE_CHARACTERISTIC_NOT_FOUND == mConnectionState) {
                    if (D) Log.i(TAG, "The connection state now is STATE_CHARACTERISTIC_NOT_FOUND");

                    mbtnTx.setEnabled(false);
                    mbtnTxIst.setEnabled(false);

                }
            }else if(msg.what == MSG_SENDBUTTON_UPDATE){
                mbtnTx.setEnabled(true);
                mbtnTxIst.setEnabled(true);
            }else if(msg.what == MSG_POWER_UPDATE){
                if(msg.arg1==1){
                    Toast.makeText(UnvarnishedTransmissionActivity.this,"battery low",Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(UnvarnishedTransmissionActivity.this,"connected",Toast.LENGTH_SHORT).show();
                }
            }


        }
    };







    // unpack and send thread
    public class ThreadUnpackSend extends Thread {
        byte[] sendData;

        ThreadUnpackSend(byte[] data) {
            sendData = data;
        }

        public void run() {
            if (D) Log.d(TAG, "ThreadUnpackSend is run");
            // time test
            if (D) Log.e("TIME_thread run", String.valueOf(System.currentTimeMillis()));
            // set the unpack sending flag
            isUnpackSending = true;
            // send data to the remote device
            if (null != mTestCharacter) {
                // unpack the send data, because of the MTU size is limit
                int length = sendData.length;
                int unpackCount = 0;
                byte[] realSendData;
                do {

                    if (length <= MTU_PAYLOAD_SIZE_LIMIT) {
                        realSendData = new byte[length];
                        System.arraycopy(sendData, unpackCount * MTU_PAYLOAD_SIZE_LIMIT, realSendData, 0, length);

                        // update length value
                        length = 0;
                    } else {
                        realSendData = new byte[MTU_PAYLOAD_SIZE_LIMIT];
                        System.arraycopy(sendData, unpackCount * MTU_PAYLOAD_SIZE_LIMIT, realSendData, 0, MTU_PAYLOAD_SIZE_LIMIT);

                        // update length value
                        length = length - MTU_PAYLOAD_SIZE_LIMIT;
                    }

                    SendData(realSendData);

                    // unpack counter increase
                    unpackCount++;


                } while (length != 0);

                // set the unpack sending flag
                isUnpackSending = false;

                if (D) Log.d(TAG, "ThreadUnpackSend stop");
            }//if(null != mTestCharacter)
        }//run
    }

    private void SendData(byte[] realData) {
        // for GKI get buffer error exit
        long timeCost = 0;


        // initial the status
        writeCharacteristicError = true;

        while (true == writeCharacteristicError) {
            // mBluetoothGatt.getConnectionState(mDevice) can not use in thread, so we use a flag to
            // break the circulate when disconnect the connect
            if (true == isConnectDevice) {
                // for GKI get buffer error exit
                timeCost = System.currentTimeMillis();

                if (D)
                    Log.e(TAG, "writeCharacteristicError start Status:" + writeCharacteristicError);

                // initial the status
                isWriteCharacteristicOk = false;

                // Set the send type(command)
                mTestCharacter.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                // include the send data
                System.out.println("发送的realDATA" + StringByteTrans.Byte2String(realData));
                mTestCharacter.setValue(realData);
                // send the data
                mBluetoothGatt.writeCharacteristic(mTestCharacter);

                // wait for characteristic write ok
                while (isWriteCharacteristicOk != true) {
                    if (false == isConnectDevice) {
                        if (D)
                            Log.e(TAG, "break the circulate when disconnect the connect, no callback");
                        break;
                    }

                    // for GKI get buffer error exit
                    // if 10 seconds no callback we think GKI get buffer error
                    if ((System.currentTimeMillis() - timeCost) / 1000 > 10) {
                        if (D) Log.e(TAG, "GKI get buffer error close the BT and exit");
                        // becouse GKI error, so we should close the bt
                        if (mBluetoothAdapter.isEnabled()) {
                            if (D) Log.d(TAG, "close bluetooth");
                            mBluetoothAdapter.disable();
                        }

                        // close the activity
                        finish();
                    }
                }
                ;

                if (D)
                    Log.e(TAG, "writeCharacteristicError stop Status:" + writeCharacteristicError);
            } else {
                // break the circulate when disconnect the connect
                if (D) Log.e(TAG, "break the circulate when disconnect the connect, write error");
                break;
            }
        }

        if (D) Log.d(TAG, "send data is: " + Arrays.toString(realData));

        // update tx counter, only write ok then update the counter




    }

    // set the connect state
    private void SetConnectionState(GattConnectState state) {
        // Update the connect state
        mConnectionState = state;

        // send the msg
        Message message = new Message();
        message.what = MSG_CONNECTION_STATE_UPDATE;
        handler.sendMessage(message);
    }

    /*geyun send ccc---------notify characteristic*/
    private void enableNotification(final BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (D) Log.i(TAG, "geyun write CCC    +");
        gatt.setCharacteristicNotification(characteristic, true);
        // enable remote notification
        final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        gatt.writeDescriptor(descriptor);
        if (D) Log.i(TAG, "geyun write CCC   -");
    }
}
