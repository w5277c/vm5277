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
package ru.vm5277.compiler.avr_codegen;

import ru.vm5277.common.cg.CGAccum;
import ru.vm5277.common.cg.items.CGIAsm;
import ru.vm5277.common.cg.items.CGIAsmLd;
import ru.vm5277.common.cg.items.CGIAsmMv;
import ru.vm5277.common.cg.items.CGIContainer;
import ru.vm5277.common.cg.items.CGIText;
import static ru.vm5277.common.cg.scopes.CGScope.VERBOSE_LO;
import ru.vm5277.common.exceptions.CompileException;

public class Utils {

	/**
	 * brInstrInvert - инвертируем br инструкцию
	 * @param branchInstr - оригинальная br инструкция
	 * @return - инвертированная br инструкция, null если инструкции нет
	 */
	static String brInstrInvert(String branchInstr) throws CompileException {
		switch(branchInstr) {
			case "brbc": return "brbs";
			case "brbs": return "brbc";
			case "brcc": return "brcs";
			case "brcs": return "brcc";
			case "breq": return "brne";
			case "brge": return "brlt";
			case "brhc": return "brhs";
			case "brhs": return "brhc";
			case "brid": return "brie";
			case "brie": return "brid";
			case "brlo": return "brcc";
			case "brlt": return "brge";
			case "brmi": return "brpl";
			case "brne": return "breq";
			case "brpl": return "brmi";
			case "brsh": return "brcs";
			case "brtc": return "brts";
			case "brts": return "brtc";
			case "brvc": return "brvs";
			case "brvs": return "brvc";
			default: 
				throw new CompileException("Unknown branch instruction: " + branchInstr);
		}
	}
	
	static byte[] getAccRegs(int size) {
		switch(size) {
			case 1: return new byte[]{16};
			case 2: return new byte[]{16,17};
			case 3: return new byte[]{16,17,18};
			default: return new byte[]{16,17,18,19};
		}
	}

	static byte[] getTempRegs(int size) {
		switch(size) {
			case 1: return new byte[]{30};
			case 2: return new byte[]{30,31};
			case 3: return new byte[]{16,17,18};
			default: return new byte[]{30,31,28,29};
		}
	}

	static CGIContainer accResize(CGAccum acc, int verbose) throws CompileException {
		CGIContainer cont = new CGIContainer();
		if(acc.isChanged()) {
			if(VERBOSE_LO <= verbose) {
				cont.append(new CGIText(";acc cast " + acc.oldToString() + "->" + acc.toString()));
			}
		
			if(acc.getOldFixed() && !acc.getFixed()) {
				cont.append(new CGIAsmMv("mov", "r16", "r17"));
				cont.append(new CGIAsmLd("ldi", "r17", "0x00"));
				cont.append(new CGIAsm("sbrc", "r16,0x07"));
				cont.append(new CGIAsm("inc", "r16"));
			}
			else if(!acc.getOldFixed() && acc.getFixed()) {
				cont.append(new CGIAsmMv("mov", "r17" , "r16"));
				cont.append(new CGIAsmLd("ldi", "r16", "0x00"));
			}
		}

		if(!acc.getFixed()) {
			accGrow(cont, acc, acc.getOldBytes(), acc.getBytes());
		}
		return cont;
	}

	static void accGrow(CGIContainer cont, CGAccum acc, int oldSize, int newSize) throws CompileException {
		if(oldSize<newSize) {
			byte[] accRegs = Utils.getAccRegs(4);
			for(int i=oldSize; i<newSize; i++) {
				cont.append(new CGIAsmLd("ldi", "r" + accRegs[i], "0"));
			}
			acc.setBytes(newSize);
		}
	}
	
	static String getRightOp(int sn, Long constant) { //возвращает либо байт константы, либо регистр аккумуляора, если constant is null
		if(null == constant) {
			switch(sn) {
				case 0: return "r16";
				case 1: return "r17";
				case 2: return "r18";
				default: return "r19";
			}
		}
		return Long.toString((constant>>(sn*8))&0xff);
	}
	
	public static String getConstReg(CGIContainer cont, byte reg, int index, long value) throws CompileException {
		return getConstReg(cont, reg, (int)(value>>(index*8)&0xff));
	}
	public static String getConstReg(CGIContainer cont, byte reg, int octet) throws CompileException {
		switch(octet) {
			case 0x00: return "c0x00";
			case 0x01: return "c0x01";
			case 0x02: return "c0x02";
			case 0xff: return "c0xff";
			default: {
				cont.append(new CGIAsmLd("ldi", "r"+reg, Integer.toString(octet)));
				return "r"+reg;
			}
		}
	}
}
