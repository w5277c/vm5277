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

package ru.vm5277.avr_asm;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import ru.vm5277.common.messages.ErrorMessage;
import ru.vm5277.common.messages.MessageContainer;

public class InstrReader {
	private					MessageContainer						mc;
	private					Path									basePath;
	private					Map<String, Instruction>				instrById	= new HashMap<>();
	private					Map<String, Map<String, Instruction>>	instrByMn	= new HashMap<>();
	private					Set<String>								supported;	// Поддерживаемые инструкции
	
	public InstrReader(Path instrPath, MessageContainer mc) {
		this.mc = mc;
		this.basePath = instrPath;
		
		try(BufferedReader br = new BufferedReader(new FileReader(basePath.resolve("full.instr").normalize().toFile()))) {
			while(true) {
				String line = br.readLine();
				if(null == line) break;
				int commentPos = line.indexOf("#");
				if(-1 != commentPos) {
					line = line.substring(0, commentPos);
				}
				line = line.trim().toLowerCase();
				if(!line.isEmpty()) {
					try {
						Instruction instr = new Instruction(line);
						instrById.put(instr.getId(), instr);
						
						Map<String, Instruction> ids = instrByMn.get(instr.getMnemonic());
						if(null == ids) {
							ids = new HashMap<>();
							instrByMn.put(instr.getMnemonic(), ids);
						}
						ids.put(instr.getId(), instr);
						instrByMn.put(instr.getMnemonic(), ids);
					}
					catch(Exception e) {
						mc.add(new ErrorMessage(e.getMessage(), null));
					}
				}
			}
		}
		catch(Exception ex) {
			mc.add(new ErrorMessage("Cannot read AVR instruction definitions file:" + instrPath, null));
		}
	}

	public void setMCU(String mcu) {
		if(null==supported) {
			supported = new HashSet<>();
			try(BufferedReader br = new BufferedReader(new FileReader(basePath.resolve(mcu.toLowerCase() + ".instr").normalize().toFile()))) {
				while(true) {
					String line = br.readLine();
					if(null == line) break;
					int commentPos = line.indexOf("#");
					if(-1 != commentPos) {
						line = line.substring(0, commentPos);
					}
					line = line.trim().toLowerCase();
					if(!line.isEmpty()) {
						try {
							String[] parts = line.split(",");
							for(String part : parts) {
								part = part.trim().toLowerCase();
								supported.add(part);
							}
						}
						catch(Exception e) {
							mc.add(new ErrorMessage(e.getMessage(), null));
						}
					}
				}
			}
			catch(Exception ex) {
				mc.add(new ErrorMessage("Failed to load instruction set for " + mcu.toLowerCase() + " (file: " + basePath + ")", null));
			}
		}
		else {
			mc.add(new ErrorMessage("MCU already specified, .DEVICE cannot be redefined", null));
		}
	}
	
	public Map<String, Map<String, Instruction>> getInstrByMn() {
		return instrByMn;
	}
	public Map<String, Instruction> getInstrById() {
		return instrById;
	}
	
	public Set<String> getSupported() {
		return supported;
	}
}
