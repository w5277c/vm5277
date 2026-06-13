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

import ru.vm5277.common.lexer.SourceBuffer;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

//Не умеет вычислять, не поддерживает даже простые арифметические операции, но понимает алиасы (.EQU SPMCR = SPMCSR)

public class DefReader {
	private	final	static	String				EQU_DIR			= ".equ";
	private	final	static	String				SET_DIR			= ".set";
	private	final	static	String				DEF_DIR			= ".def";
	private	final	static	String				MACRO_DIR		= ".macro";
	private	final			Map<String,Integer>	defs			= new HashMap<>();
	private	final			Map<String,String>	map				= new HashMap<>();
	private	final			Set<String>			macros			= new HashSet<>();
	
	public DefReader() {
	}
	
	public void parse(Path defPath) throws IOException {
		try(Scanner scan = new Scanner(defPath)) {
			while(scan.hasNextLine()) {
				String line = scan.nextLine();
				int commentPos = line.indexOf(";"); ;//TODO добавить полноценную проверку на комментарий, в том числе много строчный
				if(-1!=commentPos) {
					line = line.substring(0, commentPos);
				}
				line = line.trim().toLowerCase();
				if(!line.isEmpty()) {
					if(line.startsWith(EQU_DIR)) {
						String dir = line.substring(EQU_DIR.length());
						String parts[] = dir.split("=");
						if(0x02==parts.length) {
							map.put(parts[0].toLowerCase().trim(), parts[1].trim());
						}
					}
					else if(line.startsWith(SET_DIR)) {
						String dir = line.substring(SET_DIR.length());
						String parts[] = dir.split("=");
						if(0x02==parts.length) {
							map.put(parts[0].toLowerCase().trim(), parts[1].trim());
						}
					}
					else if(line.startsWith(MACRO_DIR)) {
						String dir = line.substring(MACRO_DIR.length());
						String parts[] = dir.split("\\s");
						if(0x02<=parts.length) {
							macros.add(parts[1].toLowerCase().trim());
						}
					}
					else if(line.startsWith(DEF_DIR)) {
						String dir = line.substring(DEF_DIR.length());
						String parts[] = dir.split("=");
						if(0x02==parts.length) {
							String register = parts[1].toLowerCase().trim();
							if(register.startsWith("r")) {
								try {
									defs.put(parts[0].trim(), Integer.parseInt(register.substring(0x01)));
								}
								catch(Exception ex) {
								}
							}
						}
					}
				}
			}
		}
		catch(IOException ex) {
			throw ex;
		}
	}
	
	public Set<String> getMacros() {
		return macros;
	}
	
	public Long getNum(String key) {
		String value = map.get(key.toLowerCase());
		while(null!=value) {
			try (SourceBuffer sb = new SourceBuffer(value, 0x04)) {
				Object obj = StrUtils.parseNum(sb, null);
				if(obj instanceof Number) {
					return ((Number)obj).longValue();
				}
			}
			catch(Exception ex) {
			}
			
			value = map.get(key.toLowerCase());
		}
		return null;
	}
	
	public Integer getRegister(String key) {
		return defs.get(key.toLowerCase());
	}
}
