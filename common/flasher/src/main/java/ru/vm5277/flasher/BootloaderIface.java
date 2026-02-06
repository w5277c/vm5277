/*
 * Copyright 2025 konstantin@5277.ru
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.vm5277.flasher;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.util.List;
import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortList;
import jssc.SerialPortTimeoutException;
import ru.vm5277.common.DatatypeConverter;
import ru.vm5277.common.PlatformType;
import ru.vm5277.common.bldr.BldrRequest;
import ru.vm5277.common.bldr.BldrResult;
import ru.vm5277.common.bldr.BldrType;
import ru.vm5277.common.firmware.Segment;

public class BootloaderIface implements Closeable {
	public static class DeviceInfo {
		private	BldrType		bldrType;
		private	byte			bldrVersion;
		private	PlatformType	platform;
		private	int				signature;
		private byte[]			uid				= new byte[0x08];
		private	short			fwVersion;
		private	boolean			singleWireMode	= true;
		
		public DeviceInfo(boolean singleWireMode) {
			this.singleWireMode = singleWireMode;
		}
		
		public void parse(byte[] data, int offset) {
			bldrType = BldrType.values()[(data[offset]>>>0x06)&0x03];
			bldrVersion = ((byte)(data[offset++]&0x3f));
			platform = PlatformType.values()[data[offset++]];
			signature = ((data[offset++]&0xff)<<0x18) + ((data[offset++]&0xff)<<0x10) + ((data[offset++]&0xff)<<0x08) + (data[offset++]&0xff);
			System.arraycopy(data, offset, uid, 0x00, 0x08);
			offset+=0x08;
			fwVersion = data[offset];
		}
		
		public boolean isSingleWireMode() {
			return singleWireMode;
		}
		
		public BldrType getBldrType() {
			return bldrType;
		}
		
		public byte getBldrVersion() {
			return bldrVersion;
		}
		
		public byte[] getUID() {
			return uid;
		}
		
		public PlatformType getPlatform() {
			return platform;
		}
		
		public int getSignature() {
			return signature;
		}
		
		@Override
		public String toString() {
			return	"BLDR " + bldrType.toString() + " v." + String.format("%02x", bldrVersion) + ": " +
					platform + ", sig:" + String.format("%08x", signature) + ", uid:" + DatatypeConverter.printHexBinary(uid);
		}
	}

	private			SerialPort	serialPort = null;
	private	final	float		cpuFreq;
	private	final	int			baudrate;
	private	final	String		deviceName;
	private	final	int			waitTime;
	private	final	int			retries;
	private			DeviceInfo	deviceInfo;
	
	public BootloaderIface(float cpuFreq, String deviceName, int waitTime, int retries) {
		this.cpuFreq = cpuFreq;
		this.baudrate = (int)(14400*cpuFreq);
		this.deviceName = deviceName;
		this.waitTime = waitTime;
		this.retries = retries;
	}
	
	public boolean open() {
		int retryCntr = 1;
		if(null!=deviceName) {
			SerialPort sp = open(deviceName, waitTime);
			if(null!=sp) {
				serialPort = sp;
				Main.showMsg("\n", true);
				return true;
			}
			else {
				Main.showMsg("\n", true);
				Main.showErr(	"[ERROR] Bootloader handshake failed at " + cpuFreq + "MHz. Verify device " + deviceName  +
								" has vm5277 bootloader and is connected.");
			}
			return false;
		}
		else {
			boolean firstIter = true;
			long timestamp = System.currentTimeMillis();
			String progress = "|/-\\";
			int progressPos=0;
			Main.showMsg(""+progress.charAt(progressPos++), true);
			while(firstIter || ((System.currentTimeMillis()-timestamp) < waitTime)) {
				Main.showMsg("\b"+progress.charAt(progressPos++), true);
				if(progressPos==progress.length()) progressPos=0;
				firstIter = false;
				String[] serialPortsNames = SerialPortList.getPortNames();
				if(null!=serialPortsNames) {
					for(String spName : serialPortsNames) {
						SerialPort sp = open(spName, -1);
						if(null!=sp) {
							serialPort = sp;
							Main.showMsg("\b", true);
							return true;
						}
					}
				}
				try {Thread.sleep(20);}catch(Exception ex) {}
			}
			Main.showMsg("\b", true);
			Main.showErr("[ERROR] No VM5277 bootloader found at " + cpuFreq + "MHz. Verify device has vm5277 bootloader and is connected");
			return false;
		}
	}
	
	private SerialPort open(String spName, int _waitTime) {
		int retryCntr = 1;
		for(; retryCntr<=retries; retryCntr++) {
			try {
				SerialPort _serialPort = new SerialPort(spName);
				if(_serialPort.openPort()) {
					long timestamp = System.currentTimeMillis();
					_serialPort.setParams(baudrate, 8, 1, SerialPort.PARITY_NONE);
					while(_serialPort.isOpened()) {
						if(0==_waitTime && retryCntr>retries) {
							try {_serialPort.closePort();}catch(Exception ex2) {}
							return null;
						}
						requestBootloader(_serialPort);
						deviceInfo = requestDevicetInfo(_serialPort);
						if(null!=deviceInfo) {
							return _serialPort;
						}
						if(-1==_waitTime) {
							try {_serialPort.closePort();}catch(Exception ex2) {}
							return null;
						}
						if(0!=_waitTime && ((System.currentTimeMillis()-timestamp)) > _waitTime) {
							try {_serialPort.closePort();}catch(Exception ex2) {}
							return null;
						}
						if(0==_waitTime) retryCntr++;
						Thread.sleep(10);
					}
				}
				Thread.sleep(50);
			}
			catch(Exception ex) {
			}
			try {serialPort.closePort();}catch(Exception ex2) {}
		}
		return null;
	}


	private void requestBootloader(SerialPort sp) throws SerialPortException, SerialPortTimeoutException {
		if(null!=sp && sp.isOpened()) {
			byte[] requestData = new byte[] {0x12};
			sp.writeBytes(requestData);
			readWithTimeout(sp, 0x01, 20);
		}
	}
	
	private DeviceInfo requestDevicetInfo(SerialPort sp) throws SerialPortException, SerialPortTimeoutException {
		if(null!=sp && sp.isOpened()) {
			byte[] requestData = new byte[0x05];
			fillHeader(requestData, BldrRequest.INFO.getId(), 0x00, (byte)0x77);
			sp.writeBytes(requestData);
			byte[] recvData = readWithTimeout(sp, 1024, 20);
			if(null!=recvData) {
				if(0x14==recvData.length && BldrResult.MAGIC==recvData[0x00] && checkXORSumm(recvData, 0x00, 0x14) && 0x0f==getDataSize(recvData, 0x00,
																																		recvData.length)) {
					DeviceInfo dInfo = new DeviceInfo(false);
					dInfo.parse(recvData, 0x04);
					return dInfo;
				}
				if(requestData.length+0x14==recvData.length && BldrResult.MAGIC==recvData[requestData.length] &&
					checkXORSumm(recvData, requestData.length, 0x14) &&
					0x0f==getDataSize(recvData, requestData.length, recvData.length-requestData.length)) {

					DeviceInfo dInfo = new DeviceInfo(true);
					dInfo.parse(recvData, requestData.length+0x04);
					return dInfo;
				}
			}
		}
		return null;
	}
	
	public boolean checkSignature(int signature) {
		return (null!=deviceInfo && deviceInfo.getSignature()==signature);
	}

	boolean erase(int flasSize, int pageSize) {
		Main.showMsg(" ", false);
		for(int i=0; i<flasSize/pageSize/4; i++) {
			Main.showMsg("_", false);
		}
		Main.showMsg(" \n", false);
		for(int blockNum=0; blockNum<4; blockNum++) {
			int blockSize = flasSize/4;
			Main.showMsg("[", false);
			for(int i=0; i<(blockSize/pageSize); i++) {
				int pageAddr = blockNum*blockSize + i*pageSize;
				
				byte[] requestData = new byte[0x05 + 0x02];
				fillHeader(requestData, BldrRequest.PAGE_ERASE.getId(), 0x02, null);
				requestData[0x04+0x00] = (byte)(pageAddr>>0x08);
				requestData[0x04+0x01] = (byte)(pageAddr&0xff);
				byte xorSum = getXORSumm(requestData, 0x00, requestData.length-0x01);
				requestData[requestData.length-0x01] = xorSum;

				BldrResult bldrResult = null;
				int retryCntr=0;
				for(; retryCntr<=retries; retryCntr++) {
					try {
						serialPort.writeBytes(requestData);
						if(deviceInfo.isSingleWireMode()) {
							byte[] recvData = readWithTimeout(serialPort, requestData.length+0x05, 200);
							if(null!=recvData && requestData.length+0x05==recvData.length) {
								if(	BldrResult.MAGIC==recvData[requestData.length] && checkXORSumm(recvData, requestData.length, 0x05) &&
									0x00==getDataSize(recvData, requestData.length, recvData.length-requestData.length)) {

									bldrResult = BldrResult.fromByte(recvData[requestData.length+0x01]);
									if(BldrResult.OK==bldrResult) {
										break;
									}
								}
							}
						}
						else {
							byte[] recvData = readWithTimeout(serialPort, 0x05, 200);
							if(null!=recvData && 0x05==recvData.length) {
								if(BldrResult.MAGIC==recvData[0x00] && checkXORSumm(recvData, 0x00, 0x05) && 0x00==getDataSize(	recvData, 0x00,
																																recvData.length)) {
									bldrResult = BldrResult.fromByte(recvData[0x01]);
									if(BldrResult.OK==bldrResult) {
										break;
									}
								}
							}
						}
					}
					catch(Exception ex) {
					}
				}

				if(BldrResult.OK==bldrResult) {
					Main.showMsg(0==retryCntr ? "e" : "E", false);
				}
				else {
					Main.showMsg("E\n", false);
					Main.showErr(	"[ERROR] Page erase failed, bldr result:" + bldrResult + ", address " +	String.format("0x%04X", pageAddr) +
									", attempts: " + (retries+1));
					return false;
				}
			}
			Main.showMsg("]\n", false);
		}
		return true;
	}

	boolean flash(List<Segment> sourceSegments, byte[] sourceData, int pageSize, boolean secureMode) {
		Main.showMsg(" ", false);
		for(int i=0; i<sourceData.length/pageSize/4; i++) {
			Main.showMsg("_", false);
		}
		Main.showMsg(" \n", false);
		for(int blockNum=0; blockNum<4; blockNum++) {
			int blockSize = sourceData.length/4;
			Main.showMsg("[", false);
			for(int i=0; i<(blockSize/pageSize); i++) {
				int pageAddr = blockNum*blockSize + i*pageSize;
				Segment segment = findSegment(sourceSegments, pageAddr, pageSize);
				if(secureMode || null!=segment) {
					if(null==segment || segment.isModified()) {
						byte[] requestData = new byte[0x05 + 0x02 + pageSize];
						fillHeader(requestData, BldrRequest.PAGE_WRITE.getId(), 0x02+pageSize, null);
						requestData[0x04+0x00] = (byte)(pageAddr>>0x08);
						requestData[0x04+0x01] = (byte)(pageAddr&0xff);
						System.arraycopy(sourceData, pageAddr, requestData, 0x04+0x02, pageSize);
						byte xorSum = getXORSumm(requestData, 0x00, requestData.length-0x01);
						requestData[requestData.length-0x01] = xorSum;
						
						BldrResult bldrResult = null;
						int retryCntr=0;
						for(; retryCntr<=retries; retryCntr++) {
							try {
								serialPort.writeBytes(requestData);
								if(deviceInfo.isSingleWireMode()) {
									byte[] recvData = readWithTimeout(serialPort, requestData.length+0x05, 200);
									if(null!=recvData && requestData.length+0x05==recvData.length) {
										if(	BldrResult.MAGIC==recvData[requestData.length] && checkXORSumm(recvData, requestData.length, 0x05) &&
											0x00==getDataSize(recvData, requestData.length, recvData.length-requestData.length)) {

											bldrResult = BldrResult.fromByte(recvData[requestData.length+0x01]);
											if(BldrResult.OK==bldrResult || BldrResult.IDENTICAL==bldrResult) {
												break;
											}
										}
									}
								}
								else {
									byte[] recvData = readWithTimeout(serialPort, 0x05, 200);
									if(null!=recvData && 0x05==recvData.length) {
										if(	BldrResult.MAGIC==recvData[0x00] && checkXORSumm(recvData, 0x00, 0x05) && 0x00==getDataSize(recvData, 0x00,
																																		recvData.length)) {
											bldrResult = BldrResult.fromByte(recvData[0x01]);
											if(BldrResult.OK==bldrResult || BldrResult.IDENTICAL==bldrResult) {
												break;
											}
										}
									}
								}
							}
							catch(Exception ex) {
							}
						}
						
						if(BldrResult.OK==bldrResult) {
							Main.showMsg(0==retryCntr ? "w" : "W", false);
						}
						else if(BldrResult.IDENTICAL==bldrResult) {
							Main.showMsg(0==retryCntr ? "i" : "I", false);
						}
						else {
							Main.showMsg("E\n", false);
							Main.showErr(	"[ERROR] Page write failed, bldr result:" + bldrResult + ", address " +	String.format("0x%04X", pageAddr) +
											", attempts: " + (retries+1));
							return false;
						}
					}
					else {
						Main.showMsg("I", false);
					}
				}
				else {
					Main.showMsg("-", false);
				}
			}
			Main.showMsg("]\n", false);
		}
		return true;
	}

	boolean verify(List<Segment> sourceSegments, byte[] sourceData, int pageSize, boolean secureMode) {
		Main.showMsg(" ", false);
		for(int i=0; i<sourceData.length/pageSize/4; i++) {
			Main.showMsg("_", false);
		}
		Main.showMsg(" \n", false);
		for(int blockNum=0; blockNum<4; blockNum++) {
			int blockSize = sourceData.length/4;
			Main.showMsg("[", false);
			for(int i=0; i<(blockSize/pageSize); i++) {
				int pageAddr = blockNum*blockSize + i*pageSize;
				Segment segment = findSegment(sourceSegments, pageAddr, pageSize);
				if(secureMode || null!=segment) {
					if(null==segment || segment.isModified()) {
						byte[] requestData = new byte[0x05 + 0x02 + pageSize];
						fillHeader(requestData, BldrRequest.PAGE_VERIFY.getId(), 0x02+pageSize, null);
						requestData[0x04+0x00] = (byte)(pageAddr>>0x08);
						requestData[0x04+0x01] = (byte)(pageAddr&0xff);
						System.arraycopy(sourceData, pageAddr, requestData, 0x04+0x02, pageSize);
						byte xorSum = getXORSumm(requestData, 0x00, requestData.length-0x01);
						requestData[requestData.length-0x01] = xorSum;
						
						BldrResult bldrResult = null;
						int retryCntr=0;
						for(; retryCntr<=retries; retryCntr++) {
							try {
								serialPort.writeBytes(requestData);
								if(deviceInfo.isSingleWireMode()) {
									byte[] recvData = readWithTimeout(serialPort, requestData.length+0x05, 200);
									if(null!=recvData && requestData.length+0x05==recvData.length) {
										if(	BldrResult.MAGIC==recvData[requestData.length] && checkXORSumm(recvData, requestData.length, 0x05) &&
											0x00==getDataSize(recvData, requestData.length, recvData.length-requestData.length)) {

											bldrResult = BldrResult.fromByte(recvData[requestData.length+0x01]);
											if(BldrResult.OK==bldrResult || BldrResult.IDENTICAL==bldrResult) {
												break;
											}
										}
									}
								}
								else {
									byte[] recvData = readWithTimeout(serialPort, 0x05, 200);
									if(null!=recvData && 0x05==recvData.length) {
										if(	BldrResult.MAGIC==recvData[0x00] && checkXORSumm(recvData, 0x00, 0x05) && 0x00==getDataSize(recvData, 0x00,
																																		recvData.length)) {
											bldrResult = BldrResult.fromByte(recvData[0x01]);
											if(BldrResult.OK==bldrResult || BldrResult.IDENTICAL==bldrResult) {
												break;
											}
										}
									}
								}
							}
							catch(Exception ex) {
							}
						}
						
						if(BldrResult.OK==bldrResult || BldrResult.IDENTICAL==bldrResult) {
							Main.showMsg(0==retryCntr ? "v" : "V", false);
						}
						else {
							Main.showMsg("E\n", false);
							Main.showErr(	"[ERROR] Page verify failed, bldr result:" + bldrResult + ", address " +	String.format("0x%04X", pageAddr) +
											", attempts: " + (retries+1));
							return false;
						}
					}
					else {
						Main.showMsg("I", false);
					}
				}
				else {
					Main.showMsg("-", false);
				}
			}
			Main.showMsg("]\n", false);
		}
		return true;
	}

	boolean reboot() {
		byte[] requestData = new byte[0x05];
		fillHeader(requestData, BldrRequest.REBOOT.getId(), 0x00, null);
		byte xorSum = getXORSumm(requestData, 0x00, requestData.length-0x01);
		requestData[requestData.length-0x01] = xorSum;

		BldrResult bldrResult = null;
		int retryCntr=0;
		for(; retryCntr<=retries; retryCntr++) {
			try {
				serialPort.writeBytes(requestData);
				if(deviceInfo.isSingleWireMode()) {
					byte[] recvData = readWithTimeout(serialPort, requestData.length+0x05, 200);
					if(null!=recvData && requestData.length+0x05==recvData.length) {
						if(	BldrResult.MAGIC==recvData[requestData.length] && checkXORSumm(recvData, requestData.length, 0x05) &&
							0x00==getDataSize(recvData, requestData.length, recvData.length-requestData.length)) {

							bldrResult = BldrResult.fromByte(recvData[requestData.length+0x01]);
							if(BldrResult.OK==bldrResult) {
								break;
							}
						}
					}
				}
				else {
					byte[] recvData = readWithTimeout(serialPort, 0x05, 20);
					if(null!=recvData && 0x05==recvData.length) {
						if(	BldrResult.MAGIC==recvData[0x00] && checkXORSumm(recvData, 0x00, 0x05) && 0x00==getDataSize(recvData, 0x00, recvData.length)) {
							bldrResult = BldrResult.fromByte(recvData[0x01]);
							if(BldrResult.OK==bldrResult) {
								break;
							}
						}
					}
				}
			}
			catch(Exception ex) {
			}
		}

		if(BldrResult.OK!=bldrResult) {
			Main.showMsg("E\n", false);
			Main.showErr(	"[ERROR] Go to program failed, bldr result:" + bldrResult + ", attempts: " + (retries+1));
			return false;
		}
		return true;
	}


	public DeviceInfo getDeviceInfo() {
		return deviceInfo;
	}

	private void fillHeader(byte[] requestData, byte requestCode, int bodySize, Byte xorSum) {
		requestData[0x00] = BldrRequest.MAGIC;
		requestData[0x01] = requestCode;
		requestData[0x02] = (byte)(bodySize>>0x08);
		requestData[0x03] = (byte)(bodySize&0xff);
		
		if(null!=xorSum) {
			requestData[0x04+bodySize] = xorSum;
		}
	}

	
	private boolean checkXORSumm(byte[] buffer, int offset, int length) {
		int sum = 0x00;
		for(int i=offset; i<offset+length; i++) {
			sum ^= (buffer[i]&0xff);
		}
		return 0x00==(sum&0xff);
	}
	
	private byte getXORSumm(byte[] buffer, int offset, int length) {
		int sum = 0x00;
		for(int i=offset; i<offset+length; i++) {
			sum ^= (buffer[i]&0xff);
		}
		return (byte)(sum&0xff);
	}

	private int getDataSize(byte[] buffer, int offset, int length) {
		return ((buffer[offset+0x02]&0xff)<<0x08) + (buffer[offset+0x03]&0xff);
	}
	
	private Segment findSegment(List<Segment> sourceSegments, int addr, int size) {
		int blockStart = addr;
		int blockEnd = addr+size-1;

		for(Segment segment : sourceSegments) {
			int segStart = segment.getAddr();
			int segEnd = segment.getAddr() + segment.getSize()-1;

			if(blockStart<=segEnd && segStart<=blockEnd) {
				return segment;
			}
		}
		return null;
	}
	
	
	public byte[] readWithTimeout(SerialPort sp, int maxBytes, int timeout) {
		try {
			long startTime = System.currentTimeMillis();
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();

			boolean skipData = false;
			int offset = 0;
			while(offset!=maxBytes && (System.currentTimeMillis()-startTime)<timeout) {
				int available = sp.getInputBufferBytesCount();

				if(offset+available>maxBytes) {
					available = maxBytes-offset;
					if(0<available) {
						buffer.write(sp.readBytes(available));
						offset+=available;
					}
					skipData = true;
					available=0;
				}

				if(0<available) {
					byte[] chunk = sp.readBytes(available);
					if(!skipData) {
						buffer.write(chunk);
						offset+=available;
					}
				}

				Thread.sleep(1);
			}

			return skipData ? null : buffer.toByteArray();
		}
		catch(Exception ex) {
		}
		return null;
	}

	@Override
	public void close() {
		if(null!=serialPort && serialPort.isOpened()) {
			try {
				serialPort.closePort();
			}
			catch(Exception ex) {
			}
		}
	}
	
	public SerialPort getSerialPort() {
		return serialPort;
	}
}
