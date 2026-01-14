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
import ru.vm5277.common.Platform;
import ru.vm5277.common.bldr.BldrRequest;
import ru.vm5277.common.bldr.BldrResult;
import ru.vm5277.common.bldr.BldrType;
import ru.vm5277.common.firmware.Segment;

public class BootloaderIface implements Closeable {
	public static class DeviceInfo {
		private	BldrType	bldrType;
		private	byte		bldrVersion;
		private	Platform	platform;
		private	int			signature;
		private byte[]		uid			= new byte[0x08];
		private	short		fwVersion;
		
		public void parse(byte[] data, int offset) {
			bldrType = BldrType.values()[(data[offset]>>>0x06)&0x03];
			bldrVersion = ((byte)(data[offset++]&0x3f));
			platform = Platform.values()[data[offset++]];
			signature = ((data[offset++]&0xff)<<0x18) + ((data[offset++]&0xff)<<0x10) + ((data[offset++]&0xff)<<0x08) + (data[offset++]&0xff);
			System.arraycopy(data, offset, uid, 0x00, 0x08);
			offset+=0x08;
			fwVersion = data[offset];
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
		
		public Platform getPlatform() {
			return platform;
		}
		
		public int getSignature() {
			return signature;
		}
		
		@Override
		public String toString() {
			return	"BLDR " + bldrType.toString() + " v." + bldrVersion + ": " + platform + ", sig:" + String.format("%08x", signature) + ", uid:" +
					DatatypeConverter.printHexBinary(uid);
		}
	}

	private			SerialPort	serialPort = null;
	private	final	float		cpuFreq;
	private	final	int			baudrate;
	private	final	String		deviceName;
	private	final	int			retries;
	private			DeviceInfo	deviceInfo;
	
	public BootloaderIface(float cpuFreq, String deviceName, int retries) {
		this.cpuFreq = cpuFreq;
		this.baudrate = (int)(14400*cpuFreq);
		this.deviceName = deviceName;
		this.retries = retries;
	}
	
	public boolean open() {
		int retryCntr = 1;
		if(null!=deviceName) {
			for(; retryCntr<=retries; retryCntr++) {
				try {
					serialPort = new SerialPort(deviceName);
					if(serialPort.openPort()) {
						serialPort.setParams(baudrate, 8, 1, SerialPort.PARITY_NONE);
						for(; retryCntr<=retries; retryCntr++) {
							deviceInfo = requestDevicetInfo();
							if(null!=deviceInfo) {
								return true;
							}
						}
						Main.showErr(	"[ERROR] Bootloader handshake failed at " + cpuFreq + "MHz. Verify device " + deviceName  +
										" has vm5277 bootloader and is connected.");
						return false;
					}
					Thread.sleep(1000);
				}
				catch(Exception ex) {
					ex.printStackTrace();
					close();
				}
			}
			Main.showErr("[ERROR] Can't connect to device " + deviceName);
			return false;
		}
		else {
			for(; retryCntr<=retries; retryCntr++) {
				try {
					String[] serialPortsNames = SerialPortList.getPortNames();
					if(null!=serialPortsNames) {
						for(String spn : serialPortsNames) {
							SerialPort sp = new SerialPort(spn);
							if(!sp.isOpened()) {
								if(sp.openPort()) {
									sp.setParams(baudrate, 8, 1, SerialPort.PARITY_NONE);
									deviceInfo = requestDevicetInfo();
									if(null!=deviceInfo) {
										serialPort = sp;
										return true;
									}
									sp.closePort();
								}
							}
						}
					}
				}
				catch(Exception ex) {
				}
				try {Thread.sleep(1000);} catch(InterruptedException ex) {}
			}
			Main.showErr("[ERROR] No VM5277 bootloader found at " + cpuFreq + "MHz. Verify device has vm5277 bootloader and is connected");
			return false;
		}
	}

	
	private DeviceInfo requestDevicetInfo() throws SerialPortException, SerialPortTimeoutException {
		if(null!=serialPort && serialPort.isOpened()) {
			byte[] requestData = new byte[0x05];
			fillHeader(requestData, BldrRequest.INFO.getId(), 0x00, (byte)0x77);
			serialPort.writeBytes(requestData);
			byte[] recvData = readWithTimeout(serialPort, 1024, 20);
			if(null!= recvData && requestData.length+0x14==recvData.length) {
				if(	BldrResult.MAGIC==recvData[requestData.length] && checkXORSumm(recvData, requestData.length, 0x14) &&
					0x0f==getDataSize(recvData, requestData.length, recvData.length-requestData.length)) {
					
					DeviceInfo dInfo = new DeviceInfo();
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
		Main.showMsg(" ");
		for(int i=0; i<flasSize/pageSize/4; i++) {
			Main.showMsg("_");
		}
		Main.showMsg(" \n");
		for(int blockNum=0; blockNum<4; blockNum++) {
			int blockSize = flasSize/4;
			Main.showMsg("[");
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
					catch(Exception ex) {
					}
				}

				if(BldrResult.OK==bldrResult) {
					Main.showMsg(0==retryCntr ? "e" : "E");
				}
				else {
					Main.showMsg("E\n");
					Main.showErr(	"[ERROR] Page erase failed, bldr result:" + bldrResult + ", address " +	String.format("0x%04X", pageAddr) +
									", attempts: " + (retries+1));
					return false;
				}
			}
			Main.showMsg("]\n");
		}
		return true;
	}

	boolean flash(List<Segment> sourceSegments, byte[] sourceData, int pageSize, boolean secureMode) {
		Main.showMsg(" ");
		for(int i=0; i<sourceData.length/pageSize/4; i++) {
			Main.showMsg("_");
		}
		Main.showMsg(" \n");
		for(int blockNum=0; blockNum<4; blockNum++) {
			int blockSize = sourceData.length/4;
			Main.showMsg("[");
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
							catch(Exception ex) {
							}
						}
						
						if(BldrResult.OK==bldrResult) {
							Main.showMsg(0==retryCntr ? "w" : "W");
						}
						else if(BldrResult.IDENTICAL==bldrResult) {
							Main.showMsg(0==retryCntr ? "i" : "I");
						}
						else {
							Main.showMsg("E\n");
							Main.showErr(	"[ERROR] Page write failed, bldr result:" + bldrResult + ", address " +	String.format("0x%04X", pageAddr) +
											", attempts: " + (retries+1));
							return false;
						}
					}
					else {
						Main.showMsg("I");
					}
				}
				else {
					Main.showMsg("-");
				}
			}
			Main.showMsg("]\n");
		}
		return true;
	}

	boolean verify(List<Segment> sourceSegments, byte[] sourceData, int pageSize, boolean secureMode) {
		Main.showMsg(" ");
		for(int i=0; i<sourceData.length/pageSize/4; i++) {
			Main.showMsg("_");
		}
		Main.showMsg(" \n");
		for(int blockNum=0; blockNum<4; blockNum++) {
			int blockSize = sourceData.length/4;
			Main.showMsg("[");
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
							catch(Exception ex) {
							}
						}
						
						if(BldrResult.OK==bldrResult || BldrResult.IDENTICAL==bldrResult) {
							Main.showMsg(0==retryCntr ? "v" : "V");
						}
						else {
							Main.showMsg("E\n");
							Main.showErr(	"[ERROR] Page verify failed, bldr result:" + bldrResult + ", address " +	String.format("0x%04X", pageAddr) +
											", attempts: " + (retries+1));
							return false;
						}
					}
					else {
						Main.showMsg("I");
					}
				}
				else {
					Main.showMsg("-");
				}
			}
			Main.showMsg("]\n");
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
			catch(Exception ex) {
			}
		}

		if(BldrResult.OK!=bldrResult) {
			Main.showMsg("E\n");
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
}
