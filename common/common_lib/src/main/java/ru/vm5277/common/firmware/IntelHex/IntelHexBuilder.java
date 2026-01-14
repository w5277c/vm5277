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

package ru.vm5277.common.firmware.IntelHex;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class IntelHexBuilder {
	private	File	file;

	public IntelHexBuilder(File file) {
		this.file = file;
	}

	public void write(int address, byte[] data) throws IOException {
		FileOutputStream fos = new FileOutputStream(file);
		IntelHexRow ihr = new IntelHexRow(EIntelRecordType.EXT_SEGMENT, 0x0000, new byte[]{0x00, 0x00}, 0x0000, 0x02);
		ihr.write(fos);
		int offset = 0x0000;
		for(; offset<data.length; offset+=0x10) {
			int delta = (data.length - offset);
			ihr = new IntelHexRow(EIntelRecordType.DATA, address + offset, data, offset, delta >= 0x10 ? 0x10 : delta);
			ihr.write(fos);
		}
		ihr = new IntelHexRow(EIntelRecordType.EOF, 0x0000, null, 0x0000, 0x00);
		ihr.write(fos);

		fos.flush();
		fos.close();
	}
}
