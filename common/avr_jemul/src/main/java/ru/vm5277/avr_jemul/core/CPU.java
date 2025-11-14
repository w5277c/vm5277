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

package ru.vm5277.avr_jemul.core;

import ru.vm5277.avr_jemul.memory.MemoryController;
import java.util.Random;
import ru.vm5277.avr_jemul.config.Config;
import ru.vm5277.avr_jemul.instructions.Instruction;

public class CPU {
	private	final	short[]	registers		= new short[32]; // R0-R31
	private			int		pc;		// Program Counter
	private			int		sp;		// Stack Pointer
	private			int		sReg;	// SREG

	// Status register bits
	private	static	final	int	SREG_C	= 0; // Carry
	private	static	final	int	SREG_Z	= 1; // Zero
	private	static	final	int	SREG_N	= 2; // Negative
	// ... остальные флаги

	private					MemoryController	mCont;
	private					InstructionDecoder	iDecoder;
	private					Config				config;
	
	public CPU(Config config, MemoryController mCont) {
		this.config = config;
		this.mCont = mCont;
		this.iDecoder = new InstructionDecoder();

		reset();
	}

	public void reset() {
		new Random().nextBytes(registers);
		pc = 0;
		sp = config.getSDRAMStart()-0x01;
		sReg = 0;
	}

	public void executeCycle() {
		try {
			int opcode = mCont.readFlash(pc);
			Instruction instr = iDecoder.decode(opcode);
			instr.execute(this, mCont);
			pc += instr.getSize(); // 2 или 4 байта
		}
		catch(Exception e) {
			handleException(e);
		}
	}

	public int getRegister(short regNum) {
		return registers[regNum];
	}
	public void setRegister(short regNum, short octet) {
		registers[regNum] = (short)(octet&0xFF);
	}

	public void setFlag(int flag, boolean value) {
		if(value) {
			sReg |= (1<<flag);
		}
		else {
			sReg &= ~(1<<flag);
		}
	}

	public boolean getFlag(int flag) {
		return 0!=(sReg&(1<<flag));
	}
}