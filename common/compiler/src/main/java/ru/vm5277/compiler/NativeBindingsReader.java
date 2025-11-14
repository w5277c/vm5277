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
package ru.vm5277.compiler;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import ru.vm5277.common.NativeBinding;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.messages.ErrorMessage;
import ru.vm5277.common.messages.MessageContainer;

public class NativeBindingsReader {
	private					MessageContainer						mc;
	private					Path									runtimePath;
	private					Map<String, NativeBinding>				map		= new HashMap<>();
	
	public NativeBindingsReader(Path runtimePath, MessageContainer mc) {
		this.mc = mc;
		this.runtimePath = runtimePath;
		
		try(BufferedReader br = new BufferedReader(new FileReader(runtimePath.resolve("native_bindings.cfg").normalize().toFile()))) {
			while(true) {
				String line = br.readLine();
				if(null == line) break;
				int commentPos = line.indexOf("#");
				if(-1 != commentPos) {
					line = line.substring(0, commentPos);
				}
				line = line.trim();
				if(!line.isEmpty()) {
					try {
						NativeBinding nb = new NativeBinding(line);
						map.put(nb.getSignature(), nb);
					}
					catch(Exception e) {
						mc.add(new ErrorMessage(e.getMessage(), null));
					}
				}
			}
		}
		catch(Exception ex) {
			mc.add(new ErrorMessage("Cannot read native bindings map file:" + runtimePath, null));
		}
		
		// Создадим дубликаты методов с меньшими типами
		for(NativeBinding nb : new ArrayList<>(map.values())) {
			if(null!=nb.getMethodParams()) {
				boolean action = true;
				while(action) {
					action=false;
					VarType[] paramTypes = nb.getMethodParams();
					for(int i=0; i<paramTypes.length; i++) {
						VarType newType = reduceType(paramTypes[i]);
						if(null!=newType) {
							VarType[] newParamTypes = new VarType[paramTypes.length];
							for(int j=0; j<paramTypes.length; j++) { 
								newParamTypes[j] = paramTypes[j];
							}
							newParamTypes[i] = newType;
							NativeBinding newNB = new NativeBinding(nb, newParamTypes);
							if(null==map.get(newNB.getSignature())) {
								map.put(newNB.getSignature(), newNB);
								action = true;
							}
						}
					}
				}
			}
		}
	}
	
	private VarType reduceType(VarType type) {
		if(VarType.INT==type) {
			return VarType.SHORT;
		}
		if(VarType.SHORT==type) {
			return VarType.BYTE;
		}
		return null;
	}
	
	public NativeBinding get(String methodPath) {
		return map.get(methodPath);
	}
	
	public Map<String, NativeBinding> getMap() {
		return map;
	}
}
