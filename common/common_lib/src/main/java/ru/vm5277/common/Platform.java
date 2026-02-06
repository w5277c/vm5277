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
import java.nio.file.Path;
import java.util.Map;

public class Platform {
	private	PlatformType				type;
	private	int							optLevel;
	private	Map<String, NativeBinding>	nbMap;
	private	Map<RTOSParam, Object>		params;
	private	DefReader					defReader;
	
	public Platform(PlatformType type, Map<String, NativeBinding> nbMap, Map<RTOSParam, Object> params, Path asmDefsPath, int optLevel) throws IOException {
		this.type = type;
		this.nbMap = nbMap;
		this.params = params;
		this.optLevel = optLevel;
		this.defReader = new DefReader();
		if(null!=asmDefsPath) {
			defReader.parse(asmDefsPath);
		}
	}

	public PlatformType getType() {
		return type;
	}

	public void setParam(RTOSParam param, int value) {
		params.put(param, value);
	}

	public boolean containsParam(RTOSParam param) {
		return params.keySet().contains(param);
	}

	public Object getParam(RTOSParam param) {
		return params.get(param);
	}

	public int getOptLevel() {
		return optLevel;
	}

	public NativeBinding getNativeBinding(String signature) {
		return nbMap.get(signature);
	}
	
	public DefReader getDefReader() {
		return defReader;
	}
}
