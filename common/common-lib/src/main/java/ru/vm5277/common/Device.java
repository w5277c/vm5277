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

import java.io.IOException;
import ru.vm5277.common.enums.RtosParam;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import ru.vm5277.common.enums.OptimizationType;
import ru.vm5277.common.enums.PlatformType;
import ru.vm5277.common.enums.StrictLevel;
import ru.vm5277.common.exceptions.CompileException;

public class Device {
	public	final	static	int								LOW_RAM_THRESHOLD				= 256;
	public	final	static	int								MIN_FLASH_SIZE_FOR_BOOTLOADER	= 2048;
	private	final	static	HashMap<PlatformType, Platform>	cachedPlatforms					= new HashMap<>();
	private					Platform						platform;
	private					String							mcuName;
	private					OptimizationType				optimizationType;
	private					StrictLevel						strictLevel;
	private					Map<RtosParam, Object>			params;
	private					Path							defsPath;
	private					Path							rtosDefsPath;
	private					DefReader						defReader;
	private					Map<String, NativeBinding>		localNbMap;
	
	private					Long							ramSize;
	private					Long							flashSize;
	private					boolean							allowsBootloader;
	
	public Device(PlatformType platformType, String mcuName) throws IOException, CompileException {
		this(null, platformType, mcuName, null, null, OptimizationType.SIZE, StrictLevel.LIGHT);
	}
	public Device(	Path rtosPath, PlatformType platformType, String mcuName, Map<RtosParam, Object> params,
					Map<String, NativeBinding> localNbMap, OptimizationType optimizationType, StrictLevel strictLevel) throws IOException, CompileException {
		this.mcuName = mcuName;
		this.params = params;
		this.localNbMap = localNbMap;
		this.optimizationType = optimizationType;
		this.strictLevel = strictLevel;
		
		synchronized(cachedPlatforms) {
			platform = cachedPlatforms.get(platformType);
			if(null==platform) {
				platform = new Platform(rtosPath, platformType);
				cachedPlatforms.put(platformType, platform);
			}
		}
		
		if(!"stub".equals(mcuName)) {
			Path toolkitPath = FSUtils.getToolkitPath();
			defsPath = toolkitPath.resolve("defs").resolve(platformType.name().toLowerCase());
			rtosDefsPath = toolkitPath.resolve("rtos").resolve(platformType.name().toLowerCase()).resolve("devices");

			this.defReader = new DefReader();
			if(null!=defsPath) {
				defReader.parse(defsPath.resolve(mcuName + ".asm"));
			}
			if(null!=rtosDefsPath) {
				defReader.parse(rtosDefsPath.resolve(mcuName + ".def"));
			}

			ramSize = defReader.getNum("SRAM_SIZE");
			flashSize = defReader.getNum("FLASH_SIZE");
			allowsBootloader = (null!=flashSize && flashSize>=MIN_FLASH_SIZE_FOR_BOOTLOADER);
		}
	}
	
	public Platform getPlatform() {
		return platform;
	}
	
	public OptimizationType getOptimizationType() {
		return optimizationType;
	}
	
	public StrictLevel getStrictLevel() {
		return strictLevel;
	}
	
	public Object getParam(RtosParam param) {
		return params.get(param);
	}
	public void setParam(RtosParam param, int value) {
		params.put(param, value);
	}
	public boolean containsParam(RtosParam param) {
		return params.keySet().contains(param);
	}

	public Long getDefNum(String name) {
		if(null==defReader) return null;
		return defReader.getNum(name);
	}
	
	public Path getDefsPath() {
		return defsPath;
	}
	
	public boolean isLowRAM() {
		return null!=ramSize && ramSize<LOW_RAM_THRESHOLD;
	}
	
	public boolean allowsBootloader() {
		return allowsBootloader;
	}
	
	public String getUniqueName() {
		return platform.getType().name().toLowerCase() + ":" + mcuName;
	}

	public long getRamSize() {
		return ramSize;
	}
	public long getFlashSize() {
		return flashSize;
	}
	
	public Map<String, NativeBinding> getLocalNbMap() {
		return localNbMap;
	}
	
	@Override
	public String toString() {
		return platform.getType().name() + ":" + mcuName + ", opt:" + optimizationType.name() + ", strict:" + strictLevel.name();
	}
}
