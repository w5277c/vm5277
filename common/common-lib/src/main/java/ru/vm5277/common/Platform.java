/*
 * Copyright 2026 konstantin@5277.ru
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
import ru.vm5277.common.enums.PlatformType;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ru.vm5277.common.exceptions.CompileException;

public class Platform {
	public	final	static	String						DEVICES_DIR			= "devices";
	public	final	static	String						DEVICE_EXTENSION	= ".def";
	private	final			Path						rtosPath;
	private	final			PlatformType				type;
	private					Map<String, NativeBinding>	nbMap;
	private					Set<String>					registers			= new HashSet<>();
	
	public Platform(Path rtosPath, PlatformType type) throws CompileException {
		this.rtosPath = rtosPath;
		this.type = type;
		
		if(null!=rtosPath) {
			NativeBindingsReader nbr = new NativeBindingsReader(rtosPath);
			nbMap = nbr.getMap();
		}
		
		if(PlatformType.AVR==type) {
			for(int i=0; i<32; i++) {
				registers.add("r" + i);
			}
		}
	}

	public List<String> getSupportedDeviceIds() {
		List<String> result = new ArrayList<>();
		if(null!=rtosPath) {
			File devicesDir = rtosPath.resolve(DEVICES_DIR).normalize().toFile();
			if(devicesDir.exists()) {
				for(File file : devicesDir.listFiles()) {
					if(!file.isDirectory()) {
						String name = file.getName().toLowerCase();
						if(!name.startsWith("_") && name.endsWith(DEVICE_EXTENSION)) {
							result.add(name.substring(0, name.length()-DEVICE_EXTENSION.length()));
						}
					}
				}
			}
		}
		Collections.sort(result);
		return result;
	}
	
	public PlatformType getType() {
		return type;
	}

	public NativeBinding getNativeBinding(String signature) {
		return (null == nbMap ? null : nbMap.get(signature));
	}
	
	public Set<String> getRegisters() {
		return registers;
	}
}
