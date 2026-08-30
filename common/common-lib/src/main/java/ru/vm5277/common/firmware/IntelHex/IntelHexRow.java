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

import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import ru.vm5277.common.DatatypeConverter;

public class IntelHexRow {
	private	int					length;
	private	int					address;
	private	EIntelRecordType	eirt;
	private	int					offset	= 0x0000;
	private	byte[]				data;

	public IntelHexRow(String hexString) throws Exception {
		if(!hexString.startsWith(":")) {
			throw new ParseException(hexString, 0x00);
		}

		byte[] tmp = DatatypeConverter.parseHexBinary(hexString.substring(0x01));

		length = tmp[0x00]&0xff;
		if(length!=(tmp.length - 0x05)) {
			throw new Exception("Incorrect data length");
		}
		address = ((tmp[0x01]&0xff) * 0x100) + (tmp[0x02]&0xff);
		eirt = EIntelRecordType.fromInt(tmp[0x03]&0xff);
		if(EIntelRecordType.UNSUPPORTED==eirt) {
			throw new Exception("Unsupported record type");
		}
		if(0 != length) {
			data = new byte[length];
			System.arraycopy(tmp, 0x04, data, 0x00, length);
		}
		int checksum = 0x00;
		for(int pos=0x00; pos<(tmp.length-0x01); pos++) {
			checksum += tmp[pos];
		}
		if(((0x01 + ~(checksum ))&0xff) != (tmp[tmp.length-0x01]&0xff)) {
			throw new Exception("Incorrect checksum, addr:" + Integer.toHexString(address));
		}
	}

	public IntelHexRow(EIntelRecordType eirt, int address, byte[] data, int offset, int length) {
		this.length = length;
		this.address = address;
		this.eirt = eirt;
		this.data = data;
		this.offset = offset;
	}

	public void write(OutputStream os) throws IOException {
		byte[] tmp = new byte[0x01+0x02+0x01+length+0x01];
		tmp[0x00] = (byte)length;
		tmp[0x01] = (byte)((address/0x100)&0x0ff);
		tmp[0x02] = (byte)(address&0xff);
		tmp[0x03] = (byte)eirt.getId();
		if(null!=data && 0!=length) {
			System.arraycopy(data, offset, tmp, 0x04, length);
		}
		int checksum = 0x00;
		for(int pos=0x00; pos<(tmp.length-0x01); pos++) {
			checksum += tmp[pos];
		}
		tmp[tmp.length-0x01] = (byte)((0x01 + ~(checksum ))&0xff);
		os.write((":" + DatatypeConverter.printHexBinary(tmp).toUpperCase()+"\r\n").getBytes());
	}

	public int getLength() {
		return length;
	}

	public int getAddress() {
		return address;
	}

	public EIntelRecordType getType() {
		return eirt;
	}

	public byte[] getData() {
		return data;
	}
}
