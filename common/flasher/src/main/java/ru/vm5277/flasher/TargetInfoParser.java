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

package ru.vm5277.flasher;

import java.io.File;
import java.util.HashMap;
import java.util.Scanner;

public class TargetInfoParser {
	private	final	static	String	EXCEPTION_TYPES_TITLE			= "[EXCEPTION_TYPES]";
	private	final	static	String	EXCEPTION_CODES_TITLE			= "[EXCEPTION_CODES]";
	private	final	static	String	THROW_POINTS_TITLE				= "[THROW_POINTS]";
	
	private	final			HashMap<Integer,String>						exceptions		= new HashMap<>();
	private	final			HashMap<Integer,HashMap<Integer, String>>	exceptionCodes	= new HashMap<>();
	private	final			HashMap<Integer,String>						thPoints		= new HashMap<>();
	
	private	boolean	isReady	= false;

	public TargetInfoParser() {
	}

	public boolean parse(File file) {
		try (Scanner scan = new Scanner(file);) {
			while(scan.hasNextLine()) {
				String line = scan.nextLine();
				if(line.equals(EXCEPTION_TYPES_TITLE)) {
					parseExceptionTypes(scan);
				}
				else if(line.equals(EXCEPTION_CODES_TITLE)) {
					parseExceptionCodes(scan);
				}
				else if(line.equals(THROW_POINTS_TITLE)) {
					parseThrowPoints(scan);
				}
			}
		}
		catch(Exception ex) {
			return false;
		}
		isReady = true;
		return true;
	}
	
	public boolean isReady() {
		return isReady;
	}

	private void parseExceptionTypes(Scanner scan) {
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if(line.isEmpty()) break;
			String parts[] = line.split("\\s");
			if(0x02<=parts.length) {
				try {
					Integer id = Integer.parseInt(parts[0x00]);
					exceptions.put(id, parts[0x01]);
				}
				catch(Exception ex) {
				}
			}
		}
	}

	private void parseExceptionCodes(Scanner scan) {
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if(line.isEmpty()) break;
			String parts[] = line.split("\\s");
			if(0x03<=parts.length) {
				try {
					Integer exId = Integer.parseInt(parts[0x00]);
					Integer code = Integer.parseInt(parts[0x01]);
					HashMap<Integer, String> codes = exceptionCodes.get(exId);
					if(null==codes) {
						codes = new HashMap<>();
						exceptionCodes.put(exId, codes);
					}
					codes.put(code, parts[0x02]);
				}
				catch(Exception ex) {
				}
			}
		}
	}

	private void parseThrowPoints(Scanner scan) {
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if(line.isEmpty()) break;
			String parts[] = line.split("\\s", 0x02);
			if(0x02<=parts.length) {
				try {
					Integer id = Integer.parseInt(parts[0x00]);
					thPoints.put(id, parts[0x01]);
				}
				catch(Exception ex) {
				}
			}
		}
	}
	
	public String getException(int id) {
		return exceptions.get(id);
	}
	
	public String getExceptionCode(int exId, int code) {
		HashMap<Integer, String> codes = exceptionCodes.get(exId);
		if(null==codes) return null;
		return codes.get(code);
	}

	public String getThrowPoint(int id) {
		return thPoints.get(id);
	}
}
