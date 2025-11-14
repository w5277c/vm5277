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

import java.util.HashMap;
import java.util.Map;
import ru.vm5277.avr_jemul.config.Config;

public class MemoryController {
	private	FlashMemory					flash;
	private SRAM						sram;
	private EEPROM						eeprom;
	private Map<Integer, Peripheral>		ioRegisters	= new HashMap<>();

	public int readFlash(int address) {
		return flash.read(address);
	}

	public int readSRAM(Config cfg, int address) {
		if (address<cfg.getSDRAMSTart()) {
			// Регистры ввода-вывода
			Peripheral p = ioRegisters.get(address);
			return null != p ? p.read(address) : 0;
		}
		return sram.read(address);
	}

	public void writeSRAM(Config cfg, int address, int value) {
		if (address<cfg.getSDRAMStart()) {
			// Регистры ввода-вывода
			Peripheral p = ioRegisters.get(address);
			if (null!=p) p.write(address, value);
		}
		else {
			sram.write(address, value);
		}
	}

	public void mapPeripheral(int address, Peripheral peripheral) {
		ioRegisters.put(address, peripheral);
	}
}