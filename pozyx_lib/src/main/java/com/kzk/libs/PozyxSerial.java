package com.kzk.libs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
//import java.util.logging.Logger;
import java.util.Objects;

import com.fazecast.jSerialComm.SerialPort;
import com.kzk.libs.structures.generic.Data;
import com.kzk.libs.structures.generic.DeviceDetailes;
import com.kzk.libs.structures.generic.Generic;
import com.kzk.libs.structures.generic.SingleRegister;

import com.kzk.libs.definitions.Constants;
import com.kzk.libs.definitions.Registers;;

/*
 * This Java source file was generated by the Gradle 'init' task.
 */
public class PozyxSerial extends Lib {

	public static final int DEFAULT_BAUDRATE = 115200;
	public static final int DEFAULT_TIMEOUT = 100; //ms
	public static final int DEFAULT_WRITE_TIMEOUT = 100;
	public static final boolean DEFAULT_PRINT_OUTPUT = false;
	public static final boolean DEFAULT_DEBUG_TRACE = false;
	public static final boolean DEFAULT_SHOW_TRACE = false;
	public static final boolean DEFAULT_SUPPERSS_WARNINGS = false;
	public static final int DEFAULT_DATABITS = 8;
	public static final int DEFAULT_STOPBITS = 1;
	
	public static final String NEW_LINE = "\r";

	private static final String POZYX_COM_NAME_MACOS = "Pozyx Virtual";
	private static final String POZYX_COM_NAME_WIN = "STMicroelectronics Virtual COM Port";

//	private static final Logger LOGGER = Logger.getLogger(PozyxSerial.class.getName());
	
	private String port;
	private int baudrate = DEFAULT_BAUDRATE;
	private int timeout = DEFAULT_TIMEOUT;
	private int writeTimeout = DEFAULT_WRITE_TIMEOUT;
	private boolean printOutput = DEFAULT_PRINT_OUTPUT;
	private boolean debugTrace = DEFAULT_DEBUG_TRACE;
	private boolean showTrace = DEFAULT_SHOW_TRACE;
	private boolean suppressWarnings = DEFAULT_SUPPERSS_WARNINGS;
	private int dataBits = DEFAULT_DATABITS;
	private int stopBits = DEFAULT_STOPBITS;
	
	private SerialPort ser = null;
	
	private BufferedReader buffReader = null;

	public PozyxSerial(String port, int baudrate, int timeout, int writeTimeout, boolean printOutput,
			boolean debugTrace, boolean showTrace, boolean suppressWarnings, int dataBits, int stopBits) {
		this.port = port;
		this.baudrate = baudrate;
		this.timeout = timeout;
		this.writeTimeout = writeTimeout;
		this.printOutput = printOutput;
		this.debugTrace = debugTrace;
		this.showTrace = showTrace;
		this.suppressWarnings = suppressWarnings;
		this.dataBits = dataBits;
		this.stopBits = stopBits;
		 
		 this.connectToPozyx(port, baudrate, timeout, writeTimeout);
		 
		 try { // not good これをメインスレッドで回すのはよくない．
			Thread.sleep(250);  // can use openPort -> check web of JSerialComm
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		 this.validatePozyx();
	}

	public PozyxSerial(String port) {
		this(port, DEFAULT_BAUDRATE, DEFAULT_TIMEOUT, DEFAULT_WRITE_TIMEOUT, DEFAULT_PRINT_OUTPUT, DEFAULT_DEBUG_TRACE,
				DEFAULT_SHOW_TRACE, DEFAULT_SUPPERSS_WARNINGS, DEFAULT_DATABITS, DEFAULT_STOPBITS);
	}

	@Deprecated
	public static void listSerialPort() {
		LOGGER.warning("listSerialPorts now deprecated, use printAllSerialPorts instead");
		SerialPort[] ports = SerialPort.getCommPorts();

		for (SerialPort port : ports) {
			System.out.println(port.getSystemPortName() 
					+ " - " + port.getDescriptivePortName()
					+ " - " + port.getPortDescription());
		}

	}

	public static void printAllSerialPorts() {
		SerialPort[] ports = SerialPort.getCommPorts();
		for (SerialPort port : ports) {
			System.out.println(port.getSystemPortName() + 
					" - " + port.getDescriptivePortName() + 
					" - " + port.getPortDescription());
		}
	}

	public static SerialPort[] getSerialPorts() {
		return SerialPort.getCommPorts(); 
	}

	public static boolean isPozyxPort(SerialPort port) {
		try {
			if ((port.getDescriptivePortName().contains(POZYX_COM_NAME_MACOS) || port.getDescriptivePortName().contains(POZYX_COM_NAME_WIN))) {
				return true;
			} else {
				return false;
			}
		} catch (NullPointerException npe) {
			return false;
		}
	}

	public static SerialPort getPortObject(String device) {  // return  
		for(SerialPort port : getSerialPorts()) {
			if (port.getSystemPortName().equals(device)) {
				return port;
			}
		}
		return null; // this can be a problem
	}

	public static boolean isPozyx(String device) {
		SerialPort port = getPortObject(device);
		if(port != null && isPozyxPort(port)) {
			return true;
		}
		return false;
	}

	public static ArrayList<String> getPozyxPorts() {
		ArrayList<String> pozyxPorts = new ArrayList<String>();
		for(SerialPort port : getSerialPorts()) {
			if(isPozyxPort(port)) {
				pozyxPorts.add(port.getSystemPortName());
			}
		}
		return pozyxPorts;
	}

	public static String getFirstPozyxSerialPort() {   // should Implement try catch
		for(SerialPort port : getSerialPorts()) {
			if(isPozyxPort(port)) {
				return port.getSystemPortName();
			}
		}
		return null;
	}

	public static String getPozyxSerialPort() {
		return "a";
	}

	public static ArrayList<String> getPoxyzPortWindows() {
		// this method was made following get_pozyx_ports_windows in pozyx_serial.py
		SerialPort[] ports = getSerialPorts();
		ArrayList<String> pozyxPorts = new ArrayList<String>();
		for(SerialPort port : ports) {
			if(port.getDescriptivePortName().contains("Pozyx Virtual ComPort in FS Mode")) {
				pozyxPorts.add(port.getSystemPortName());
			}
		}
		return pozyxPorts;
	}

	public void connectToPozyx(String port, int baudrate, int timeOut, int writeTimeOut) {
		this.port = port;
		this.baudrate = baudrate;
		this.timeout = timeOut;
		this.writeTimeout = writeTimeOut;

		if(!isPozyx(port) && !suppressWarnings) {
			LOGGER.warning("The passed device is not a recognized Pozyx device, is " + getPortObject(port).getDescriptivePortName());
		} else {
			this.ser = getPortObject(port);
			this.ser.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, timeout, writeTimeout);
			this.ser.setComPortParameters(baudrate, DEFAULT_DATABITS, DEFAULT_STOPBITS, SerialPort.NO_PARITY);
			// TODO: check timeout mode
		}
		this.ser.openPort();
		
		buffReader = new BufferedReader(new InputStreamReader(this.ser.getInputStream()));
	}

	public void validatePozyx() { // confirm if the received data is 43 or not.		
		SingleRegister data = new SingleRegister();
		
		if (getWhoAmI(data) !=  Constants.POZYX_SUCCESS) {
			LOGGER.severe("Connected to device, but couldn't read serial data. Is it a Pozyx?");
		}
//		System.out.println("DATA!!: " + data.getData());
		if(!data.getData().equals("43")) {
			LOGGER.severe("POZYX_WHO_AM_I returned " + data.getData() + "something is wrong with Pozyx.");
		}
	}
	
	public String serialExchanging(String s) {
		String outString = "";
		String newString = s + NEW_LINE;
		byte[] newStringB = newString.getBytes();
	
		this.ser.writeBytes(newStringB, newStringB.length);
		
		try {
			outString = buffReader.readLine().substring(2);  // Delete the initial "D,"
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		return outString;
	}

	@Override
	public int regRead(byte address, Data data) {
		String newData = "R," + (String.format("%02x", address)).toUpperCase() +","+ Integer.toString(data.getDataSize());
		String result = serialExchanging(newData);  // send a readMessage, and get a response.
		
		data.setData(result); // update the data
		return Constants.POZYX_SUCCESS;  // // TODO: implement try catch
	}
	
	@Override
	public int regWrite(byte address, Data data) {
		String newData = "W" + (String.format("%02x", address)).toUpperCase() +","+ Integer.toString(data.getDataSize());
		byte[] newStringB = newData.getBytes();
		this.ser.writeBytes(newStringB, newStringB.length);
		return Constants.POZYX_SUCCESS;
	}
	@Override
	public int remoteRegWrite(String remoteId, byte address, Data data) {
		return 10;
	}

	@Override
	public void waitForFlag(boolean interruptFlag, float timeoutS, boolean interrupt) {

	}

	@Override
	public int regFunction(byte address, Data params, Data data){
		String newData = "F," + (String.format("%02x", address)).toUpperCase() +","+ params.getData() +","+ Integer.toString(params.getDataSize());
		System.out.println(newData);
		String result = serialExchanging(newData);
		data.setData(result);
		return Integer.parseInt(result.substring(0, 2), 16);  // TODO: implement try catch
	}
	
	@Override
	public int remoteRegRead(String destination, byte address, Data data) { // TODO: Implementation
		return Constants.POZYX_SUCCESS;
	}
	
	@Override
	public int useFunction(byte function, Data params, Data data, String remoteId) {
		
		if(!isFunctionCall(function)) {
			if(!this.suppressWarnings) {  // TODO: what is suppressWarning ?
				LOGGER.severe("Register " + function + " isn't a function register");
			}
		}
//		params = (params == null) ? new SingleRegister() : params;
//		data = (data == null) ?	new SingleRegister(): data;
		if(remoteId.equals("None")) {
			return this.regFunction(function, params, data);
		}else {
			return this.regRemoteFunction(remoteId, function, params, data);
		}
	}
	
	public int regRemoteFunction(String distination, byte adress, Data params, Data data) {
		return 10;
	}
	
	public boolean isFunctionCall(byte reg) {
//		if((0xB0 <= reg && reg < 0xBC) || (0xC0 <= reg) && (reg < 0xC9)) {
		if((-80 <= reg && reg < -68) || (-64 <= reg) && (reg < -55)) {
			return true;
		}
		return false;
	}
	
	public void deviceCheck(DeviceDetailes device) {
		System.out.println(device.firmware_version_string());
		System.out.println(device.hardware_version_string());
	}
	
	public int setWrite(byte address, Data data, String remoteId, double localDelay, double remoteDelay) {
		int status;
		if(remoteId.equals("None")) {
			status = this.regWrite(address, data);
//			Thread.sleep(250); sleep(localDeray)
		}else {
			status = this.remoteRegWrite(remoteId, address, data);
//			Thread.sleep(250); sleep(remoteDeray)
		}
		return status;
	}
}
