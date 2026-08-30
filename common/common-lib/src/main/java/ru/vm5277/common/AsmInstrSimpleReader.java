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

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import ru.vm5277.common.enums.PlatformType;
import ru.vm5277.common.exceptions.CompileException;

public class AsmInstrSimpleReader {
	private					Set<String>		instrs	= new HashSet<>();
	
	public AsmInstrSimpleReader(Path defsPath) throws CompileException {
		try(BufferedReader br = new BufferedReader(new FileReader(defsPath.resolve("full.instr").normalize().toFile()))) {
			while(true) {
				String line = br.readLine();
				if(null == line) break;
				int commentPos = line.indexOf("#");
				if(-1 != commentPos) {
					line = line.substring(0, commentPos);
				}
				line = line.trim().toLowerCase();
				if(!line.isEmpty()) {
					String parts[] = line.trim().toLowerCase().split("\\s+");
					if(0x02>parts.length) {
						throw new CompileException("Incorrect line format for instruction, got [" + parts.length + "] in: " + line);
					}
					String mnemonic = parts[0x01];
					instrs.add(mnemonic);
				}
			}
		}
		catch(Exception ex) {
			throw new CompileException("Cannot read AVR instruction definitions file:" + defsPath);
		}
	}

	public AsmInstrSimpleReader(Path defsPath, String mcu) throws CompileException {
		try(BufferedReader br = new BufferedReader(new FileReader(defsPath.resolve(mcu.toLowerCase() + ".instr").normalize().toFile()))) {
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
							instrs.add(part);
						}
					}
					catch(Exception e) {
						throw new CompileException(e.getMessage());
					}
				}
			}
		}
		catch(Exception ex) {
			throw new CompileException("Failed to load instruction set for " + mcu.toLowerCase() + " (file: " + defsPath + ")");
		}
	}
	
	public Set<String> getInstrs() {
		return instrs;
	}
}
