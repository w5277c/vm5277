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
package ru.vm5277.avr_asm.scope;

import ru.vm5277.common.exceptions.CompileException;


public class CodeBlock {
	private	final	static	int					PART_SIZE	= 0x80;
	private final			int					startWAddress;
	private					int					offset		= 0;
	private					int					length;
	private					byte[]				data		= new byte[PART_SIZE];
	private					int					overlap		= 0;
	
	public CodeBlock(int startWAddress) {
		this.startWAddress = startWAddress;
	}

	public void writeOpcode(int opcode) throws CompileException {
		byte[] bdata = new byte[0x02];
		bdata[0x01] = (byte)((opcode >> 0x08) & 0xff);
		bdata[0x00] = (byte)(opcode & 0xff);
		write(bdata, 0x01);
	}
	public void writeDoubleOpcode(long opcode) throws CompileException {
		byte[] bdata = new byte[0x04];
		bdata[0x01] = (byte)((opcode >> 0x18) & 0xff);
		bdata[0x00] = (byte)((opcode >> 0x10) & 0xff);
		bdata[0x03] = (byte)((opcode >> 0x08) & 0xff);
		bdata[0x02] = (byte)(opcode & 0xff);
		write(bdata, 0x02);
	}
	
	public void write(byte[] bdata, int wSize) {
		try {
			int _bytesSize = wSize*2;
			if(data.length-offset < _bytesSize) {
				byte[] _data = new byte[offset + _bytesSize + PART_SIZE];
				System.arraycopy(data, 0x00, _data, 0x00, offset);
				data = _data;
			}
			System.arraycopy(bdata, 0x00, data, offset, _bytesSize);
			offset += _bytesSize;
			if(offset>length) length=offset;
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

/*	public void append(CodeBlock block) {
		byte _data[] = new byte[offset + block.getLength()];
		System.arraycopy(data, 0x00, _data, 0x00, offset);
		System.arraycopy(block.getData(), 0x00, _data, offset, block.getLength());
		data = _data;
		offset = data.length;
		if(offset>length) length=offset;
	}*/

	public int getStartWAddress() {
		return startWAddress;
	}
	public void setOffset(int wAddr) {
		offset = (wAddr-startWAddress)*2;	
	}
	
	public int getWSize() {
		return length/2;
	}
	
	public byte[] getData() {
		return data;
	}

	public void setOverlap(int overtlap) {
		this.overlap = overtlap;
	}
	public int getOverlap() {
		return overlap;
	}
}
