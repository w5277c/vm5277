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

package ru.vm5277.avr_jemul.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import ru.vm5277.common.messages.ErrorMessage;
import ru.vm5277.common.messages.MessageContainer;

public class IntelHexReader {
	private		MessageContainer	mc;
	private		Path				hexPath;
	
	public IntelHexReader(Path hexPath, MessageContainer mc) {
		this.mc = mc;
		this.hexPath = hexPath;
		
		int lineNumber = 0;
		try(BufferedReader br = new BufferedReader(new FileReader(hexPath.toFile()))) {
			while(true) {
				String line = br.readLine();
				if(null==line) break;
				
				lineNumber++;
				line = line.trim();

				if(line.isEmpty()) continue;

				if(!line.startsWith(":")) {
					throw new IOException("Invalid HEX format at line " + lineNumber + ": missing colon");
				}

				try {
					processLine(line, lineNumber);
				}
				catch (Exception e) {
					throw new IOException("Error processing line " + lineNumber + ": " + e.getMessage(), e);
				}
			}
		}
		catch(Exception ex) {
			mc.add(new ErrorMessage("Cannot read Intel hex file:" + hexPath, null));
		}
	}
	
	private void processLine(String line, int lineNumber) throws IOException {
		// Проверяем минимальную длину
		if (11>line.length()) {
			throw new IOException("Line too short");
		}

		int pos = 1;
		// Читаем длину данных
		int dataLength = hexByteToInt(line, pos, 0x01); pos+=0x02;

		// Проверяем общую длину строки
		int expectedLength = 2+4+2+dataLength*2+2;
		if(line.length()!=expectedLength) {
			throw new IOException(String.format("Invalid line length: expected %d, got %d", expectedLength, line.length()));
		}

		// Читаем адрес
		int address = hexByteToInt(line, pos, 0x02); pos+=0x04;

		// Читаем тип записи
		int recordType = hexByteToInt(line, pos, 0x01); pos+=0x02;

		// Читаем данные
		byte[] recordData = new byte[dataLength];
		for(int i=0; i<dataLength; i++) {
			recordData[i] = (byte)hexByteToInt(line, pos, 0x01); pos+=0x02;
		}

		// Читаем и проверяем контрольную сумму
		int checksum = hexByteToInt(line, pos, 0x01);
		if (!validateChecksum(dataLength, address, recordType, recordData, checksum)) {
			throw new IOException("Checksum validation failed");
		}

		// Обрабатываем запись в зависимости от типа
		processRecord(recordType, address, recordData, lineNumber);
	}
	
	private int hexByteToInt(String str, int offset, int bytes) throws IllegalArgumentException {
		int result = 0;
		for(int i=0; i<bytes*2; i++) {
			result = result*0x10+parseHexDigit(str.charAt(offset+i));
		}
		return result;
	}
	private int parseHexDigit(char ch) throws IllegalArgumentException {
		if(Character.isDigit(ch)) {
			return ch-'0';
		}
		else if('a'<=ch && 'f'>=ch) {
			return 10+ch-'a';
		}
		else if('A'<=ch && 'F'>=ch) {
			return 10+ch-'A';
		}
		throw new IllegalArgumentException("Invalid hex digit: " + ch);
	}
}
