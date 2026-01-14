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
import java.util.Map;
import java.util.Scanner;

//Не умеет вычислять, не поддерживает даже простые арифметические операции, но понимает алиасы (.EQU SPMCR = SPMCSR)

public class DefReader {
	private	final	static	String				EQU_DIR			= ".equ";
	private	final	static	String				SET_DIR			= ".set";
	private	final			Map<String,String>	map				= new HashMap<>();
	
	public DefReader() {
	}
	
	public void parse(Path defPath) throws IOException {
		try(Scanner scan = new Scanner(defPath)) {
			while(scan.hasNextLine()) {
				String line = scan.nextLine();
				int commentPos = line.indexOf("#");
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
				}
			}
		}
		catch(IOException ex) {
			throw ex;
		}
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
}
