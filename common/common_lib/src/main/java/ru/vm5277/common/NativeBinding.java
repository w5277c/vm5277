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
package ru.vm5277.common;

import java.io.File;
import java.text.ParseException;
import java.util.Arrays;

public class NativeBinding {
	private	String			signature;
	private	String			path;
	private	VarType[]		paramTypes;
	private	String			rtosFilePath;
	private	String			rtosFunction;
	private	byte[][]		regs;
	private	RTOSFeature[]	rtosFeatures;
	
	public NativeBinding(NativeBinding nb, VarType[] paramTypes) {
		this.path = nb.getPath();
		this.paramTypes = paramTypes;
		this.rtosFilePath = nb.getRTOSFilePath();
		this.rtosFunction = nb.getRTOSFunction();
		this.regs = nb.getRegs();
		this.rtosFeatures = nb.getRTOSFeatures();
		signature = path + " " + (null==paramTypes ? "()" : Arrays.toString(paramTypes));
	}
	
	public NativeBinding(String line) throws ParseException {
		String parts[] = line.trim().split("\\s+");
		if(0x02!=parts.length &&  0x03!=parts.length) {
			throw new ParseException("Expected 2 or 3 blocks in native binding raw, got [" + parts.length + "] in: " + line, 0);
		}
		
		String methodPart = parts[0x00].trim();
		int paramsPos = methodPart.indexOf("(");
		if(-1 == paramsPos) {
			throw new ParseException("Invalid method declaration in: " + line, 0);
		}
		int paramsPos2 = methodPart.indexOf(")");
		if(paramsPos2<=paramsPos || !methodPart.endsWith(")")) {
			throw new ParseException("Invalid method declaration in: " + line, 0);
		}
		path = methodPart.substring(0, paramsPos);

		String params = methodPart.substring(paramsPos+1, paramsPos2).trim();
		if(!params.isEmpty()) {
			String[] paramsParts = params.split(",");
			if(0 != paramsParts.length) {
				paramTypes = new VarType[paramsParts.length];
				for(int i=0; i<paramsParts.length; i++) {
					switch(paramsParts[i].trim().toLowerCase()) {
						case "bool": paramTypes[i] = VarType.BOOL; break;
						case "byte": paramTypes[i] = VarType.BYTE; break;
						case "short": paramTypes[i] = VarType.SHORT; break;
						case "int": paramTypes[i] = VarType.INT; break;
						case "fixed": paramTypes[i] = VarType.FIXED; break;
						case "cstr": paramTypes[i] = VarType.CSTR; break;
						case "exception": paramTypes[i] = VarType.EXCEPTION; break;
						default:
							throw new ParseException("Invalid method parameter type " + paramsParts[i].trim() + "in: " + line, 0);
					}
				}
			}
		}

		String[] rtosParts = parts[0x01].trim().split(":");
		if(0x02!=parts.length &&  0x03!=parts.length) {
			throw new ParseException("Expected 2 or 3 parameters in rtos block, got [" + parts.length + "] in: " + line, 0);
		}
		rtosFilePath = rtosParts[0x00].trim();
		
		int pos = rtosFilePath.lastIndexOf(".");
		if(-1 != pos) {
			rtosFilePath = rtosFilePath.substring(0, pos).replace(".", File.separator) + rtosFilePath.substring(pos);
		}
		
		rtosFunction = rtosParts[0x01].toLowerCase().trim();
		try {
			if(0x03 == rtosParts.length) {
				String[] regStrs = rtosParts[0x02].trim().split(",");
				if(0 != regStrs.length) {
					regs = new byte[regStrs.length][];
					for(int i=0; i<regStrs.length; i++) {
						regs[i] = getSeparated(regStrs[i].trim().toLowerCase());
					}
				}
			}
		}
		catch(Exception e) {
			throw new ParseException("Expected registers numbers with coma separated in: " + line, 0);
		}
		
		if(0x03 == parts.length) {
			String[] features = parts[0x02].trim().split(",");
			rtosFeatures = new RTOSFeature[features.length];
			for(int i=0; i<features.length; i++) {
				RTOSFeature feature = RTOSFeature.valueOf(features[i].toUpperCase());
				if(null == feature) throw new ParseException("Unsupported RTOS feature: " + features[i] , 0);
				rtosFeatures[i] = feature;
			}
		}
		signature = path + "(";
		
		if(null!=paramTypes) {
			for(int i=0; i<paramTypes.length; i++) {
				VarType argType = paramTypes[i];
				signature += argType.getName();
				if(i!=(paramTypes.length-1)) {
					signature+=",";
				}
			}
		}
		signature+=")";
	}
	
	private byte[] getSeparated(String str) {
		byte[] result = null;
		if(!str.isEmpty()) {
			String[] parts = str.split("\\.");
			if(0 != parts.length) {
				result = new byte[parts.length];
				for(int i=0; i<parts.length; i++) {
					result[i] = Byte.parseByte(parts[i]);
				}
			}
		}
		return result;
	}

	public String getSignature() {
		return signature;
	}
	public String getPath() {
		return path;
	}
	
	public VarType[] getMethodParams() {
		return paramTypes;
	}
	
	public String getRTOSFilePath() {
		return rtosFilePath;
	}
	
	public String getRTOSFunction() {
		return rtosFunction;
	}
	
	public byte[][] getRegs() {
		return regs;
	}
	
	public RTOSFeature[] getRTOSFeatures() {
		return rtosFeatures;
	}
	
	@Override
	public String toString() {
		return signature;
	}
}
