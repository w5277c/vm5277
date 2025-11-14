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

package ru.vm5277.avr_jemul.memory;

import java.util.Arrays;
import ru.vm5277.avr_jemul.config.Config;

public class FlashMemory {
	private	Config	config;
	private	int[]	data;
	
	public FlashMemory(Config config) {
		this.config = config;
		this.data = new int[config.getFlashWSize()];
		Arrays.fill(data, 0xffff);
	}
	
	public void writeData(int wAddr, byte[] srcData) {
		if(0>wAddr || data.length<=wAddr) {
			throw new IllegalArgumentException("Flash address out of range: " + wAddr);
		}
		System.arraycopy(srcData, 0x00, data, wAddr*2, srcData.length);
	}
	public void setOpcode(int wAddr, int opcode) {
		if(0>wAddr || data.length<=wAddr) {
			throw new IllegalArgumentException("Flash address out of range: " + wAddr);
		}
		data[wAddr*0x02+0x00] = (byte)((opcode>>0x08)&0xff);
		data[wAddr*0x02+0x01] = (byte)(opcode&0xff);
	}
	
	public int getOpcode(int wAddr) {
		if(0>wAddr || data.length<=wAddr) {
			throw new IllegalArgumentException("Flash address out of range: " + wAddr);
		}
		return data[wAddr*2+0x00]*0x100 + data[wAddr*2+0x01];
	}
}
