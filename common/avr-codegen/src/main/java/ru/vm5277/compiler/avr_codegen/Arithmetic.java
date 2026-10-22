/*
 * Copyright 2026 konstantin@5277.ru
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0eunary
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.vm5277.compiler.avr_codegen;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import ru.vm5277.common.cg.MThState;
import ru.vm5277.common.VarType;
import ru.vm5277.common.cg.CGAccum;
import ru.vm5277.common.cg.CGActionHandler;
import ru.vm5277.common.cg.CGCells;
import static ru.vm5277.common.cg.CGCells.Type.ARGS;
import static ru.vm5277.common.cg.CGCells.Type.CONST;
import static ru.vm5277.common.cg.CGCells.Type.HEAP;
import static ru.vm5277.common.cg.CGCells.Type.REG;
import static ru.vm5277.common.cg.CGCells.Type.STACK;
import static ru.vm5277.common.cg.CGCells.Type.STACK_FRAME;
import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.common.cg.CodeExcsChecker;
import ru.vm5277.common.cg.CodeGenerator;
import static ru.vm5277.common.cg.CodeGenerator.getCurBlockScope;
import ru.vm5277.common.cg.items.CGIAsm;
import ru.vm5277.common.cg.items.CGIAsmCall;
import ru.vm5277.common.cg.items.CGIAsmIReg;
import ru.vm5277.common.cg.items.CGIAsmLd;
import ru.vm5277.common.cg.items.CGIAsmMv;
import ru.vm5277.common.cg.items.CGIContainer;
import ru.vm5277.common.cg.items.CGIRAsm;
import ru.vm5277.common.cg.items.CGIText;
import ru.vm5277.common.cg.scopes.CGBlockScope;
import ru.vm5277.common.cg.scopes.CGScope;
import static ru.vm5277.common.cg.scopes.CGScope.VERBOSE_LO;
import ru.vm5277.common.enums.J8BException;
import ru.vm5277.common.enums.RTOSLibs;
import ru.vm5277.common.enums.RtosFeature;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.lexer.Operator;
import static ru.vm5277.common.lexer.Operator.PLUS;
import static ru.vm5277.compiler.avr_codegen.Generator.CALL_INSTR;

// Здесь реализована осноаная логика арифметических операций (ADD, SUB, MUL, DIV, MOD, SHL, SHR)
// Метод может работать с
// аккумулятором в левой части(также будет добавлена работа с регисрами)
// с максимально любым типом ячеек в правой части (что не реализовано будет дополнено по необходимости)
// и сохраняет резуьтат в аккумуляторе (продумаю логику при реализации поддержки других регистров в левой части)

public interface Arithmetic {
	static CGIContainer opAdd(	int index, String dstReg, int size, boolean leftIsFixed, boolean rightIsFixed, boolean rightIsConstant,
								boolean ovfChk,	MThState mState, CGActionHandler handler)									throws CompileException {
		CGIContainer cont = new CGIContainer("arithmetic:add");
		if(leftIsFixed && !rightIsFixed) {
			if(0==index) return null;
			if(rightIsConstant) {
				if(ovfChk) {
					String srcReg = Utils.getConstReg(cont, Generator.REG_J8B_ATOM, handler.action(cont, index-1, null).intValue());
					if(srcReg.equals("r"+Generator.REG_J8B_ATOM)) {
						mState.setNotSafe();
					}
					cont.append(new CGIRAsm(0==index ? "add" : "adc", dstReg, srcReg));
				}
				else {
					cont.append(new CGIRAsm(1==index ? "subi" : "sbci", dstReg, "byte" + (index) + "(-"+handler.action(cont, -1, null) + ")"));
				}
			}
			else {
				cont.append(new CGIRAsm(1==index ? "add" : "adc", dstReg, "r"+handler.action(cont, index-1, null)));
			}
		}
		else if(!leftIsFixed && rightIsFixed) {
			if((index+1)<size) {
				if(rightIsConstant) {
					if(ovfChk) { // Не можем использовать subi/sbci
						String srcReg = Utils.getConstReg(cont, Generator.REG_J8B_ATOM, handler.action(cont, index+1, null).intValue());
						if(srcReg.equals("r"+Generator.REG_J8B_ATOM)) {
							mState.setNotSafe();
						}
						cont.append(new CGIRAsm(0==index ? "add" : "adc", dstReg, srcReg));
					}
					else {
						cont.append(new CGIRAsm(0==index ? "subi" : "sbci", dstReg, "byte" + (index+2) + "(-"+handler.action(cont, -1, null) + ")"));
					}
				}
				else {
					cont.append(new CGIRAsm(0==index ? "add" : "adc", dstReg, "r"+handler.action(cont, index+1, null)));
				}
			}
			else {
				if(rightIsConstant && !ovfChk) { // Если нет проверки на переполнение и это константа, то используем subi/sbci
					cont.append(new CGIRAsm(0==index ? "subi" : "sbci", dstReg, "byte" + (index+2) + "(-"+handler.action(cont, -1, null) + ")"));
				}
				else {
					cont.append(new CGIRAsm(0==index ? "add" : "adc", dstReg, "c0x00"));
				}
			}
		}
		else { // Или оба fixed или оба не fixed
			if(index<size) {
				if(rightIsConstant) {
					if(ovfChk) { // Не можем использовать subi/sbci
						String srcReg = Utils.getConstReg(cont, Generator.REG_J8B_ATOM, handler.action(cont, index, null).intValue());
						if(srcReg.equals("r"+Generator.REG_J8B_ATOM)) {
							mState.setNotSafe();
						}
						cont.append(new CGIRAsm(0==index ? "add" : "adc", dstReg, srcReg));
					}
					else {
						cont.append(new CGIRAsm(0==index ? "subi" : "sbci", dstReg, "byte" + (index+1) + "(-"+handler.action(cont, -1, null) + ")"));
					}
				}
				else {
					cont.append(new CGIRAsm(0==index ? "add" : "adc", dstReg, "r"+handler.action(cont, index, null)));
				}
			}
			else {
				if(rightIsConstant && !ovfChk) { // Если нет проверки на переполнение и это константа, то используем subi/sbci
					cont.append(new CGIRAsm(0==index ? "subi" : "sbci", dstReg, "byte" + (index+1) + "(-"+handler.action(cont, -1, null) + ")"));
				}
				else {
					cont.append(new CGIRAsm("adc", dstReg, "c0x00"));
				}
			}
		}
		return cont;
	}
	static CGIContainer opSub(	int index, String dstReg, int size, boolean leftIsFixed, boolean rightIsFixed, boolean rightIsConstant,
								boolean ovfChk,	CGActionHandler handler)															throws CompileException {
		CGIContainer cont = new CGIContainer("arithmetic:sub");
		if(index<size) {
			if(leftIsFixed && !rightIsFixed) {
				if(0==index) return null;
				if(rightIsConstant) {
					cont.append(new CGIRAsm(1==index ? "subi" : "sbci", dstReg, ""+handler.action(cont, index-1, null)));
				}
				else {
					cont.append(new CGIRAsm(1==index ? "sub" : "sbc", dstReg, "r"+handler.action(cont, index-1, null)));
				}
			}
			else if(!leftIsFixed && rightIsFixed) {
				if((index+1)<size) {
					if(rightIsConstant) {
						cont.append(new CGIRAsm(0==index ? "subi" : "sbci", dstReg, ""+handler.action(cont, index+1, null)));
					}
					else {
						cont.append(new CGIRAsm(0==index ? "sub" : "sbc", dstReg, "r"+handler.action(cont, index+1, null)));
					}
				}
				else {
					if(rightIsConstant) {
						cont.append(new CGIRAsm("sbci", dstReg, "0"));
					}
					else {
						cont.append(new CGIRAsm("sbc", dstReg, "c0x00"));
					}
				}
			}
			else {
				if(rightIsConstant) {
					cont.append(new CGIRAsm(0==index ? "subi" : "sbci", dstReg, ""+handler.action(cont, index, null)));
				}
				else {
					cont.append(new CGIRAsm(0==index ? "sub" : "sbc", dstReg, "r"+handler.action(cont, index, null)));
				}
			}
		}
		else {
			if(rightIsConstant) {
				cont.append(new CGIRAsm("sbci", dstReg, "0"));
			}
			else {
				cont.append(new CGIRAsm("sbc", dstReg, "c0x00"));
			}
		}
		return cont;
	}
	static CGIContainer opMul(	byte[] leftRegs, int leftSize, int rightSize, int resultSize, boolean leftIsFixed, boolean rightIsFixed,
								boolean rightIsConstant, boolean ovfChk, CGActionHandler handler)									throws CompileException {
		
		CGIContainer cont = new CGIContainer("arithmetic:mul");
		if(0x01==leftSize && 0x01==rightSize) {
			RTOSLibs.MATH_MUL8.setRequired();
			if(Generator.REG_ACCUM_L!=leftRegs[0]) {
				cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_L, "r"+leftRegs[0]));
			}
			if(rightIsConstant) {
				cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_ACCUM_H, ""+handler.action(cont, 0, null)));
			}
			else {
				handler.action(cont, 0, Generator.REG_ACCUM_H);
			}
			cont.append(new CGIAsmCall(CALL_INSTR, "os_mul8", true));
		}
		else if(0x02>=leftSize && 0x02>=rightSize) {
			if(leftIsFixed || rightIsFixed) {
				RTOSLibs.MATH_MULQ7N8.setRequired();
			}
			else {
				RTOSLibs.MATH_MUL16.setRequired();
			}
			
			if(!leftIsFixed && rightIsFixed) {
				cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_H, "r"+leftRegs[0]));
				cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_ACCUM_L, "0"));
			}
			else {
				if(Generator.REG_ACCUM_L!=leftRegs[0]) {
					cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_L, "r"+leftRegs[0]));
				}
				if(Generator.REG_ACCUM_H!=leftRegs[1]) {
					cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_H, "r"+leftRegs[1]));
				}
			}
			if(leftIsFixed && !rightIsFixed) {
				cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_ACCUM_EL, "0"));
				if(rightIsConstant) {
					cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_ACCUM_EH, ""+handler.action(cont, 0, null)));
				}
				else {
					handler.action(cont, 0, Generator.REG_ACCUM_EH);
				}
			}
			else {
				if(rightIsConstant) {
					cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_ACCUM_EL, ""+handler.action(cont, 0, null)));
					if(0x01==rightSize) {
						cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_ACCUM_EH, "0"));
					}
					else {
						cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_ACCUM_EH, ""+handler.action(cont, 1, null)));
					}
				}
				else {
					handler.action(cont, 0, Generator.REG_ACCUM_EL);
					if(0x01==rightSize) {
						cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_ACCUM_EH, "0"));
					}
					else {
						handler.action(cont, 1, Generator.REG_ACCUM_EH);
					}
				}
			}
			cont.append(new CGIAsmCall(CALL_INSTR, (leftIsFixed || rightIsFixed ? "os_mulq7n8" : "os_mul16"), true));
		}
		else {
			RTOSLibs.MATH_MUL32.setRequired();
			if(Generator.REG_ACCUM_L!=leftRegs[0]) {
				cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_L, "r"+leftRegs[0]));
			}
			if(2>leftSize) {
				cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_ACCUM_H, "0"));
			}
			else if(Generator.REG_ACCUM_H!=leftRegs[1]) {
				cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_H, "r"+leftRegs[1]));
			}
			if(3>leftSize) {
				cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_ACCUM_EL, "0"));
			}
			else if(Generator.REG_ACCUM_EL!=leftRegs[2]) {
				cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_EL, "r"+leftRegs[2]));
			}
			if(4>leftSize) {
				cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_ACCUM_EH, "0"));
			}
			else if(Generator.REG_ACCUM_EH!=leftRegs[3]) {
				cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_EH, "r"+leftRegs[3]));
			}
			cont.append(new CGIAsm("push", "r"+Generator.REG_TEMP_L));
			cont.append(new CGIAsm("push", "r"+Generator.REG_TEMP_H));
			cont.append(new CGIAsm("push", "r"+Generator.REG_TEMP_EL));
			cont.append(new CGIAsm("push", "r"+Generator.REG_TEMP_EH));
			if(rightIsConstant) {
				cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_TEMP_L, ""+handler.action(cont, 0, null)));
			}
			else {
				handler.action(cont, 0, Generator.REG_TEMP_L);
			}
			if(2>rightSize) {
				cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_TEMP_H, "0"));
			}
			else {
				if(rightIsConstant) {
					cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_TEMP_H, ""+handler.action(cont, 1, null)));
				}
				else {
					handler.action(cont, 1, Generator.REG_TEMP_H);
				}
			}
			if(3>rightSize) {
				cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_TEMP_EL, "0"));
			}
			else {
				if(rightIsConstant) {
					cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_TEMP_EL, ""+handler.action(cont, 2, null)));
				}
				else {
					handler.action(cont, 2, Generator.REG_TEMP_EL);
				}
			}
			if(4>rightSize) {
				cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_TEMP_EH, "0"));
			}
			else {
				if(rightIsConstant) {
					cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_TEMP_EH, ""+handler.action(cont, 3, null)));
				}
				else {
					handler.action(cont, 3, (byte)Generator.REG_TEMP_EH);
				}
			}
			cont.append(new CGIAsmCall(CALL_INSTR, "os_mul32_nr", true));
			cont.append(new CGIAsm("pop", "r"+Generator.REG_TEMP_EH));
			cont.append(new CGIAsm("pop", "r"+Generator.REG_TEMP_EL));
			cont.append(new CGIAsm("pop", "r"+Generator.REG_TEMP_H));
			cont.append(new CGIAsm("pop", "r"+Generator.REG_TEMP_L));
		}
		return cont;
	}
	static CGIContainer opDiv(	byte[] leftRegs, int leftSize, int rightSize, int resultSize, boolean leftIsFixed, boolean rightIsFixed,
								boolean rightIsConstant, boolean divZeroChk, CGActionHandler handler,
								CodeGenerator cg, CGScope scope, CodeExcsChecker codeExcsChecker, CGExcs excs)						throws CompileException {
		
		CGIContainer cont = new CGIContainer("arithmetic:div");
		if(!leftIsFixed && !rightIsFixed && 0x01==leftSize) {
			RTOSLibs.MATH_DIV8.setRequired();
			if(Generator.REG_ACCUM_L!=leftRegs[0]) {
				cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_L, "r"+leftRegs[0]));
			}
			if(rightIsConstant) {
				cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_ACCUM_H, ""+handler.action(cont, 0, null)));
			}
			else {
				handler.action(cont, 0, (byte)Generator.REG_ACCUM_H);
			}
			if(divZeroChk) {
				cont.append(codeExcsChecker.divByZero(cg, scope, excs, new byte[]{Generator.REG_ACCUM_H}, false));
			}
			
			cont.append(new CGIAsm("push", "r"+Generator.REG_TEMP_L));
			cont.append(new CGIAsmCall(CALL_INSTR, "os_div8", true));
			cont.append(new CGIAsm("pop", "r"+Generator.REG_TEMP_L));
		}
		else if(0x02==leftSize) {
			if(leftIsFixed || rightIsFixed) {
				RTOSLibs.MATH_DIVQ7N8.setRequired();
			}
			else {
				RTOSLibs.MATH_DIV16.setRequired();
			}
			
			if(rightIsFixed && !leftIsFixed) {
				cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_H, "r"+leftRegs[0]));
				cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_ACCUM_L, "0"));
			}
			else {
				if(Generator.REG_ACCUM_L!=leftRegs[0]) {
					cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_L, "r"+leftRegs[0]));
				}
				if(Generator.REG_ACCUM_H!=leftRegs[1]) {
					cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_H, "r"+leftRegs[1]));
				}
			}
			if(leftIsFixed && !rightIsFixed) {
				cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_ACCUM_EL, "0"));
				if(rightIsConstant) {
					cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_ACCUM_EH, ""+handler.action(cont, 0, null)));
				}
				else {
					handler.action(cont, 0, (byte)Generator.REG_ACCUM_EH);
				}
			}
			else {
				if(rightIsConstant) {
					cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_ACCUM_EL, ""+handler.action(cont, 0, null)));
					if(0x01==rightSize) {
						cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_ACCUM_EH, "0"));
					}
					else {
						cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_ACCUM_EH, ""+handler.action(cont, 1, null)));
					}
				}
				else {
					handler.action(cont, 0, (byte)Generator.REG_ACCUM_EL);
					if(0x01==rightSize) {
						cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_ACCUM_EH, "0"));
					}
					else {
						handler.action(cont, 1, (byte)Generator.REG_ACCUM_EH);
					}
				}
			}
			if(divZeroChk) {
				cont.append(codeExcsChecker.divByZero(cg, scope, excs, new byte[]{Generator.REG_ACCUM_EL, Generator.REG_ACCUM_EH}, false));
			}
			if(!leftIsFixed && !rightIsFixed) {
				cont.append(new CGIAsm("push", "r"+Generator.REG_TEMP_L));
				cont.append(new CGIAsm("push", "r"+Generator.REG_TEMP_H));
			}
			cont.append(new CGIAsmCall(CALL_INSTR, (leftIsFixed || rightIsFixed ? "os_divq7n8" : "os_div16"), true));
			if(!leftIsFixed && !rightIsFixed) {
				cont.append(new CGIAsm("pop", "r"+Generator.REG_TEMP_H));
				cont.append(new CGIAsm("pop", "r"+Generator.REG_TEMP_L));
			}
		}
		else {
			RTOSLibs.MATH_DIV32.setRequired();
			if(Generator.REG_ACCUM_L!=leftRegs[0]) {
				cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_L, "r"+leftRegs[0]));
			}
			if(2>leftSize) {
				cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_ACCUM_H, "0"));
			}
			else if(Generator.REG_ACCUM_H!=leftRegs[1]) {
				cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_H, "r"+leftRegs[1]));
			}
			if(3>leftSize) {
				cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_ACCUM_EL, "0"));
			}
			else if(Generator.REG_ACCUM_EL!=leftRegs[2]) {
				cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_EL, "r"+leftRegs[2]));
			}
			if(4>leftSize) {
				cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_ACCUM_EH, "0"));
			}
			else if(Generator.REG_ACCUM_EH!=leftRegs[3]) {
				cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_EH, "r"+leftRegs[3]));
			}
			cont.append(new CGIAsm("push", "r"+Generator.REG_TEMP_L));
			cont.append(new CGIAsm("push", "r"+Generator.REG_TEMP_H));
			cont.append(new CGIAsm("push", "r"+Generator.REG_TEMP_EL));
			cont.append(new CGIAsm("push", "r"+Generator.REG_TEMP_EH));
			if(rightIsConstant) {
				cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_TEMP_L, ""+handler.action(cont, 0, null)));
			}
			else {
				handler.action(cont, 0, Generator.REG_TEMP_L);
			}
			if(2>rightSize) {
				cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_TEMP_H, "0"));
			}
			else {
				if(rightIsConstant) {
					cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_TEMP_H, ""+handler.action(cont, 1, null)));
				}
				else {
					handler.action(cont, 1, Generator.REG_TEMP_H);
				}
			}
			if(3>rightSize) {
				cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_TEMP_EL, "0"));
			}
			else {
				if(rightIsConstant) {
					cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_TEMP_EL, ""+handler.action(cont, 2, null)));
				}
				else {
					handler.action(cont, 2, Generator.REG_TEMP_EL);
				}
			}
			if(4>rightSize) {
				cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_TEMP_EH, "0"));
			}
			else {
				if(rightIsConstant) {
					cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_TEMP_EH, ""+handler.action(cont, 3, null)));
				}
				else {
					handler.action(cont, 3, (byte)Generator.REG_TEMP_EH);
				}
			}
			if(divZeroChk) {
				cont.append(codeExcsChecker.divByZero(	cg, scope, excs,
														new byte[]{Generator.REG_TEMP_L, Generator.REG_TEMP_H, Generator.REG_TEMP_EL, Generator.REG_TEMP_EH},
														true));
			}

			cont.append(new CGIAsmCall(CALL_INSTR, "os_div32", true));
			
			cont.append(new CGIAsm("pop", "r"+Generator.REG_TEMP_EH));
			cont.append(new CGIAsm("pop", "r"+Generator.REG_TEMP_EL));
			cont.append(new CGIAsm("pop", "r"+Generator.REG_TEMP_H));
			cont.append(new CGIAsm("pop", "r"+Generator.REG_TEMP_L));
		}
		return cont;
	}
	static CGIContainer opMod(	byte[] leftRegs, int leftSize, int rightSize, int resultSize, boolean leftIsFixed, boolean rightIsFixed,
								boolean rightIsConstant, CGActionHandler handler)													throws CompileException {
		
		if(leftIsFixed || rightIsFixed) {
			throw new CompileException("MOD operation with FIXED operands is not supported");
		}
		
		CGIContainer cont = new CGIContainer("arithmetic:mod");
		if(!leftIsFixed && !rightIsFixed && 0x01==leftSize) {
			RTOSLibs.MATH_DIV8.setRequired();
			if(Generator.REG_ACCUM_L!=leftRegs[0]) {
				cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_L, "r"+leftRegs[0]));
			}
			if(rightIsConstant) {
				cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_ACCUM_H, ""+handler.action(cont, 0, null)));
			}
			else {
				handler.action(cont, 0, (byte)Generator.REG_ACCUM_H);
			}
			//TODO codeExcsChecker.divByZero(this, scope, excs, new byte[]{17}, false);
			cont.append(new CGIAsm("push", "r"+Generator.REG_TEMP_L));
			cont.append(new CGIAsmCall(CALL_INSTR, "os_div8", true));
			cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_L, "r"+Generator.REG_TEMP_L));
			cont.append(new CGIAsm("pop", "r"+Generator.REG_TEMP_L));
		}
		else if(0x02==leftSize) {
			RTOSLibs.MATH_DIV16.setRequired();
			if(Generator.REG_ACCUM_L!=leftRegs[0]) {
				cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_L, "r"+leftRegs[0]));
			}
			if(Generator.REG_ACCUM_H!=leftRegs[1]) {
				cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_H, "r"+leftRegs[1]));
			}
			if(rightIsConstant) {
				cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_ACCUM_EL, ""+handler.action(cont, 0, null)));
			}
			else {
				handler.action(cont, 0, (byte)Generator.REG_ACCUM_EL);
			}
			if(2>rightSize) {
				cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_ACCUM_EH, "0"));
			}
			else {
				if(rightIsConstant) {
					cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_ACCUM_EH, ""+handler.action(cont, 1, null)));
				}
				else {
					handler.action(cont, 1, Generator.REG_ACCUM_EH);
				}
			}
			//TODO codeExcsChecker.divByZero(this, scope, excs, new byte[]{18, 19}, false);
			cont.append(new CGIAsm("push", "r"+Generator.REG_TEMP_L));
			cont.append(new CGIAsm("push", "r"+Generator.REG_TEMP_H));
			cont.append(new CGIAsmCall(CALL_INSTR, "os_div16", true));
			cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_L, "r"+Generator.REG_TEMP_L));
			cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_H, "r"+Generator.REG_TEMP_H));
			cont.append(new CGIAsm("pop", "r"+Generator.REG_TEMP_H));
			cont.append(new CGIAsm("pop", "r"+Generator.REG_TEMP_L));
		}
		else {
			RTOSLibs.MATH_DIV32.setRequired();
			if(Generator.REG_ACCUM_L!=leftRegs[0]) {
				cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_L, "r"+leftRegs[0]));
			}
			if(2>leftSize) {
				cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_ACCUM_H, "0"));
			}
			else if(Generator.REG_ACCUM_H!=leftRegs[1]) {
				cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_H, "r"+leftRegs[1]));
			}
			if(3>leftSize) {
				cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_ACCUM_EL, "0"));
			}
			else if(Generator.REG_ACCUM_EL!=leftRegs[2]) {
				cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_EL, "r"+leftRegs[2]));
			}
			if(4>leftSize) {
				cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_ACCUM_EH, "0"));
			}
			else if(Generator.REG_ACCUM_EH!=leftRegs[3]) {
				cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_EH, "r"+leftRegs[3]));
			}
			cont.append(new CGIAsm("push", "r"+Generator.REG_TEMP_L));
			cont.append(new CGIAsm("push", "r"+Generator.REG_TEMP_H));
			cont.append(new CGIAsm("push", "r"+Generator.REG_TEMP_EL));
			cont.append(new CGIAsm("push", "r"+Generator.REG_TEMP_EH));
			if(rightIsConstant) {
				cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_TEMP_L, ""+handler.action(cont, 0, null)));
			}
			else {
				handler.action(cont, 0, Generator.REG_TEMP_L);
			}
			if(2>rightSize) {
				cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_TEMP_H, "0"));
			}
			else {
				if(rightIsConstant) {
					cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_TEMP_H, ""+handler.action(cont, 1, null)));
				}
				else {
					handler.action(cont, 1, Generator.REG_TEMP_H);
				}
			}
			if(3>rightSize) {
				cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_TEMP_EL, "0"));
			}
			else {
				if(rightIsConstant) {
					cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_TEMP_EL, ""+handler.action(cont, 2, null)));
				}
				else {
					handler.action(cont, 2, Generator.REG_TEMP_EL);
				}
			}
			if(4>rightSize) {
				cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_TEMP_EH, "0"));
			}
			else {
				if(rightIsConstant) {
					cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_TEMP_EH, ""+handler.action(cont, 3, null)));
				}
				else {
					handler.action(cont, 3, (byte)Generator.REG_TEMP_EH);
				}
			}
			//TODO codeExcsChecker.divByZero(this, scope, excs, new byte[]{23, 22, 25, 24}, true);
			cont.append(new CGIAsmCall(CALL_INSTR, "os_div32", true));
			cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_L, "r"+Generator.REG_TEMP_L));
			cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_H, "r"+Generator.REG_TEMP_H));
			cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_EL, "r"+Generator.REG_TEMP_EL));
			cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_EH, "r"+Generator.REG_TEMP_EH));
			cont.append(new CGIAsm("pop", "r"+Generator.REG_TEMP_EH));
			cont.append(new CGIAsm("pop", "r"+Generator.REG_TEMP_EL));
			cont.append(new CGIAsm("pop", "r"+Generator.REG_TEMP_H));
			cont.append(new CGIAsm("pop", "r"+Generator.REG_TEMP_L));
		}
		return cont;
	}
	static CGIContainer opShl(	byte[] leftRegs, int leftSize, int rightSize, int resultSize, boolean leftIsFixed, boolean rightIsFixed,
								boolean rightIsConstant, boolean ovfChk, MThState mState, CGActionHandler handler)					throws CompileException {
		
		CGIContainer cont = new CGIContainer("arithmetic:shl");
		if(leftIsFixed || rightIsFixed) {
			throw new CompileException("SHL operation with FIXED operands is not supported");
		}
		if(1<rightSize) {
			throw new CompileException("SHL operation with right operand of non-byte type is not supported");
		}

		if(rightIsConstant) {
/* старый код, для оптимизации
			if(0<octetOffset) {
					byte[] accRegs = Utils.getAccRegs(0x04);
					for(int i=acc.getBytes()-1; i>=0; i--) {
						int srcIdx = i-octetOffset;
						if(0<=srcIdx) {
							scope.append(new CGIAsmMv("mov", "r" + accRegs[i], "r" + accRegs[srcIdx]));
						}
						else {
							scope.append(new CGIAsmLd("ldi", "r" + accRegs[i], "0"));
						}
					}
				}
*/

			long k = handler.action(cont, 0, null);
			if(1==k && 0x02>=resultSize) {
				for(int index=0; index<resultSize; index++) {
					cont.append(new CGIRAsm(index==0 ? "lsl" : "rol", "r"+leftRegs[index]));
				}
			}
			else if(0x01==leftSize) {
				RTOSLibs.MATH_MUL8P2.setRequired();
				if(Generator.REG_ACCUM_L!=leftRegs[0]) {
					cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_L, "r"+leftRegs[0]));
				}
				cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_ACCUM_H, "0"));
				cont.append(new CGIAsmCall(CALL_INSTR, "os_mul8p2_x" + (1<<k), true));
			}
			else if(0x02==leftSize) {
				RTOSLibs.MATH_MUL16P2.setRequired();
				if(Generator.REG_ACCUM_L!=leftRegs[0]) {
					cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_L, "r"+leftRegs[0]));
				}
				if(2>leftSize) {
					cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_ACCUM_H, "0"));
				}
				else if(Generator.REG_ACCUM_H!=leftRegs[1]) {
					cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_H, "r"+leftRegs[1]));
				}
				if(ovfChk) {
					cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_ACCUM_EL, "0"));
				}
				cont.append(new CGIAsmCall(CALL_INSTR, "os_mul16p2_x" + (1<<k), true));
			}
			else {
				RTOSLibs.MATH_MUL32P2.setRequired();
				if(Generator.REG_ACCUM_L!=leftRegs[0]) {
					cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_L, "r"+leftRegs[0]));
				}
				if(2>leftSize) {
					cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_ACCUM_H, "0"));
				}
				else if(Generator.REG_ACCUM_H!=leftRegs[1]) {
					cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_H, "r"+leftRegs[1]));
				}
				if(3>leftSize) {
					cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_ACCUM_EL, "0"));
				}
				else if(Generator.REG_ACCUM_EL!=leftRegs[2]) {
					cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_EL, "r"+leftRegs[2]));
				}
				if(4>leftSize) {
					cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_ACCUM_EH, "0"));
				}
				else if(Generator.REG_ACCUM_EH!=leftRegs[3]) {
					cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_EH, "r"+leftRegs[3]));
				}
				
				if(mState.isEnabled()) { // Сохраняем регистр REG_J8B_ATOM_MOVF если многопоточный режим
					mState.setNotSafe();
					cont.append(new CGIRAsm("push", "r"+Generator.REG_J8B_ATOM_MOVF));
				}
				if(ovfChk) { // Инициируем регистр, если необходима проверка на переполнение (см нативную функцию OS_MUL32P2_X...)
					cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_J8B_ATOM_MOVF, "0"));
				}
				cont.append(new CGIAsmCall(CALL_INSTR, "os_mul32p2_x" + (1<<k), true));
				if(mState.isEnabled()) { // Восстанавливаем регистр REG_J8B_ATOM_MOVF если многопоточный режим
					cont.append(new CGIRAsm("pop", "r"+Generator.REG_J8B_ATOM_MOVF));
				}
			}
		}
		else {
			if(0x01==resultSize) {
				RTOSLibs.MATH_SHL8.setRequired();
				if(Generator.REG_ACCUM_L!=leftRegs[0]) {
					cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_L, "r"+leftRegs[0]));
				}
				handler.action(cont, 0, (byte)Generator.REG_ACCUM_H);
				cont.append(new CGIAsmCall(CALL_INSTR, "os_shl8", true));
			}
			else if(0x02==resultSize) {
				RTOSLibs.MATH_SHL16.setRequired();
				if(Generator.REG_ACCUM_L!=leftRegs[0]) {
					cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_L, "r"+leftRegs[0]));
				}
				if(2>leftSize) {
					cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_ACCUM_H, "0"));
				}
				else if(Generator.REG_ACCUM_H!=leftRegs[1]) {
					cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_H, "r"+leftRegs[1]));
				}
				handler.action(cont, 0, (byte)Generator.REG_ACCUM_EL);
				cont.append(new CGIAsmCall(CALL_INSTR, "os_shl16", true));
			}
			else {
				RTOSLibs.MATH_SHL32.setRequired();
				if(Generator.REG_ACCUM_L!=leftRegs[0]) {
					cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_L, "r"+leftRegs[0]));
				}
				if(2>leftSize) {
					cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_ACCUM_H, "0"));
				}
				else if(Generator.REG_ACCUM_H!=leftRegs[1]) {
					cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_L, "r"+leftRegs[1]));
				}
				if(3>leftSize) {
					cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_ACCUM_EL, "0"));
				}
				else if(Generator.REG_ACCUM_EL!=leftRegs[2]) {
					cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_EL, "r"+leftRegs[2]));
				}
				if(4>leftSize) {
					cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_ACCUM_EH, "0"));
				}
				else if(Generator.REG_ACCUM_EH!=leftRegs[3]) {
					cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_EH, "r"+leftRegs[3]));
				}
				cont.append(new CGIAsm("push", "r"+Generator.REG_TEMP_L));
				handler.action(cont, 0, (byte)Generator.REG_TEMP_L);
				cont.append(new CGIAsmCall(CALL_INSTR, "os_shl32", true));
				cont.append(new CGIAsm("pop", "r"+Generator.REG_TEMP_L));
			}
		}
		return cont;
	}
	static CGIContainer opShr(	byte[] leftRegs, int leftSize, int rightSize, int resultSize, boolean leftIsFixed, boolean rightIsFixed,
								boolean rightIsConstant, CGActionHandler handler)													throws CompileException {
		CGIContainer cont = new CGIContainer("arithmetic:shr");
		if(leftIsFixed || rightIsFixed) {
			throw new CompileException("SHR operation with FIXED operands is not supported");
		}
		if(1<rightSize) {
			throw new CompileException("SHR operation with right operand of non-byte type is not supported");
		}
		
		if(rightIsConstant) {
			long k = handler.action(cont, 0, null);	
			int octetOffset = (int)k / 8;

			if(0<octetOffset) {
				for(int index=0; index<leftSize; index++) {
					int srcIdx = index + octetOffset;
					if(srcIdx<leftSize) {
						cont.append(new CGIAsmMv("mov", "r" + leftRegs[index], "r" + leftRegs[srcIdx]));
					}
					else {
						cont.append(new CGIAsmLd("ldi", "r" + leftRegs[index], "0"));
					}
				}
			}

			k=k-(octetOffset*8);
			if(0!=k) {
				if(1==k && 0x02>=leftSize) {
					for(int index=leftSize-1; index>=0; index--) {
						cont.append(new CGIRAsm((index==leftSize-1) ? "lsr" : "ror", "r"+leftRegs[index]));
					}
				}
				else if(0x01==leftSize) {
					RTOSLibs.MATH_DIV8P2.setRequired();
					if(Generator.REG_ACCUM_L!=leftRegs[0]) {
						cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_L, "r"+leftRegs[0]));
					}
					cont.append(new CGIAsmCall(CALL_INSTR, "os_div8p2_x" + (1<<k), true));
				}
				else if(0x02==leftSize) {
					RTOSLibs.MATH_DIV16P2.setRequired();
					if(Generator.REG_ACCUM_L!=leftRegs[0]) {
						cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_L, "r"+leftRegs[0]));
					}
					if(2>leftSize) {
						cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_ACCUM_H, "0"));
					}
					else if(Generator.REG_ACCUM_H!=leftRegs[1]) {
						cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_H, "r"+leftRegs[1]));
					}
					cont.append(new CGIAsmCall(CALL_INSTR, "os_div16p2_x" + (1<<k), true));
				}
				else {
					RTOSLibs.MATH_DIV32P2.setRequired();
					if(Generator.REG_ACCUM_L!=leftRegs[0]) {
						cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_L, "r"+leftRegs[0]));
					}
					if(2>leftSize) {
						cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_ACCUM_H, "0"));
					}
					else if(Generator.REG_ACCUM_H!=leftRegs[1]) {
						cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_H, "r"+leftRegs[1]));
					}
					if(3>leftSize) {
						cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_ACCUM_EL, "0"));
					}
					else if(Generator.REG_ACCUM_EL!=leftRegs[2]) {
						cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_EL, "r"+leftRegs[2]));
					}
					if(4>leftSize) {
						cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_ACCUM_EH, "0"));
					}
					else if(Generator.REG_ACCUM_EH!=leftRegs[3]) {
						cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_EH, "r"+leftRegs[3]));
					}
					cont.append(new CGIAsmCall(CALL_INSTR, "os_div32p2_x" + (1<<k), true));
				}
			}
		}
		else {
			if(0x01==resultSize) {
				RTOSLibs.MATH_SHR8.setRequired();
				if(Generator.REG_ACCUM_L!=leftRegs[0]) {
					cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_L, "r"+leftRegs[0]));
				}
				handler.action(cont, 0, (byte)Generator.REG_ACCUM_H);
				cont.append(new CGIAsmCall(CALL_INSTR, "os_shr8", true));
			}
			else if(0x02==resultSize) {
				RTOSLibs.MATH_SHR16.setRequired();
				if(Generator.REG_ACCUM_L!=leftRegs[0]) {
					cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_L, "r"+leftRegs[0]));
				}
				if(2>leftSize) {
					cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_ACCUM_H, "0"));
				}
				else if(Generator.REG_ACCUM_H!=leftRegs[1]) {
					cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_H, "r"+leftRegs[1]));
				}
				handler.action(cont, 0, (byte)Generator.REG_ACCUM_EL);
				cont.append(new CGIAsmCall(CALL_INSTR, "os_shr16", true));
			}
			else {
				RTOSLibs.MATH_SHR32.setRequired();
				if(Generator.REG_ACCUM_L!=leftRegs[0]) {
					cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_L, "r"+leftRegs[0]));
				}
				if(2>leftSize) {
					cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_ACCUM_H, "0"));
				}
				else if(Generator.REG_ACCUM_H!=leftRegs[1]) {
					cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_L, "r"+leftRegs[1]));
				}
				if(3>leftSize) {
					cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_ACCUM_EL, "0"));
				}
				else if(Generator.REG_ACCUM_EL!=leftRegs[2]) {
					cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_EL, "r"+leftRegs[2]));
				}
				if(4>leftSize) {
					cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_ACCUM_EH, "0"));
				}
				else if(Generator.REG_ACCUM_EH!=leftRegs[3]) {
					cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_EH, "r"+leftRegs[3]));
				}
				cont.append(new CGIAsm("push", "r"+Generator.REG_TEMP_L));
				handler.action(cont, 0, (byte)Generator.REG_TEMP_L);
				cont.append(new CGIAsmCall(CALL_INSTR, "os_shr32", true));
				cont.append(new CGIAsm("pop", "r"+Generator.REG_TEMP_L));
			}
		}
		return cont;
	}
	
	public static void exec(CodeGenerator cg, final CGScope scope, CGCells leftCells, VarType leftType, Operator op, final CGCells rightCells, VarType rightType,
							VarType castType, CodeExcsChecker codeExcsChecker, CGExcs excs, int refSize, CGAccum acc, Set<RtosFeature> rtosFeatures,
							int verbose)																							throws CompileException {
		
		final CGIContainer cont = new CGIContainer();
		
		String info =	"Arithmetic:" + leftCells + "[" + leftType + "]" + " " + op + " " + rightCells + "[" + rightType + "]" +
						(null!=castType ? ", cast:" + castType : "");

		if(VERBOSE_LO <= verbose) {
			cont.append(new CGIText(";" + info + " at line " + scope.getSp().getLine()));
		}
		
		if(CGCells.Type.ACC==leftCells.getType() && null!=leftType && !leftType.isFixedPoint() && null!=rightType && rightType.isFixedPoint()) {
			cont.append(new CGIAsmMv("mov", "r"+Generator.REG_ACCUM_H, "r"+Generator.REG_ACCUM_L));
			cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_ACCUM_L, "0"));
			acc.setFixed();
			leftType = VarType.FIXED;
		}
		
		final boolean leftIsFixed = (null!=leftType && leftType.isFixedPoint()) || (null!=castType && castType.isFixedPoint());
		final boolean rightIsFixed = null!=rightType && rightType.isFixedPoint();
		
		CGIContainer dispLockCont = new CGIContainer();
		cont.append(dispLockCont);
		
		final Byte tmpReg = (!leftIsFixed && !rightIsFixed && 0x04==acc.getBytes()) ? Generator.REG_J8B_ATOM : Generator.REG_ACCUM_EH;

		final MThState mState = new MThState(rtosFeatures.contains(RtosFeature.OS_FT_MULTITHREADING));
		
		int leftSize = null==leftType ? leftCells.getSize() : (-1==leftType.getSize() ? refSize : leftType.getSize());				
		final int rightSize = null==rightType ? rightCells.getSize() : (-1==rightType.getSize() ? refSize : rightType.getSize());		
		
		switch(leftCells.getType()) {
//-ACC------------
			case ACC:
				byte[] accRegs = Utils.getAccRegs(0x04);
				
				if(0==leftSize && CGCells.Type.ACC==rightCells.getType()) {
					leftSize = acc.getBytes();
				}

				int size = 0x02;
				switch(op) {
//---PLUS---------
					case PLUS:
						if(leftIsFixed) {
							acc.setFixed();
						}
						else{
							int requiredBits = (null==castType ?	(leftSize>rightSize ? leftSize : rightSize)*8+1 :
																	(castType.isReferenceType() ? refSize : castType.getSize())*8);
							if(requiredBits!=acc.getBits()) {
								acc.setBits(requiredBits);
								Utils.accGrow(cont, acc, acc.getOldBytes(), acc.getBytes());
							}
							size = acc.getBytes();
						}
						
						// Определяем нужна ли проверка на переполнение (должен быть блок try-cathc с MathOverflowException(или родителем)
						// и максимальный типоразмер результата
						// В выражении присваивания (и других?) использующий выражения арифметики аналогично нужно проверять типоразмер на превышение
						// ожидаемого типаоразмера
						boolean ovfChk = (size==0x04 && null!=codeExcsChecker.getHandled(excs, J8BException.MathOverflowException.name()));

						for(int index=0; index<size; index++) {
							switch(rightCells.getType()) {
								case ARGS:
								case STACK_FRAME:
									cont.append(opAdd(	index, "r"+accRegs[index], rightSize, leftIsFixed, rightIsFixed, false, ovfChk, mState,
														new CGActionHandler() {
										@Override
										public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
											cont.append(new CGIAsmIReg('y', true, "r"+tmpReg, rightCells.getId(sn),
														getCurBlockScope(scope), (CGBlockScope)rightCells.getObject(),
														CGCells.Type.ARGS==rightCells.getType()));
											if(Generator.REG_J8B_ATOM==tmpReg) mState.setJ8bAtomUsed();
											return (long)tmpReg;
										}
									}));
									break;
								case ARRAY:
									cont.append(opAdd(	index, "r"+accRegs[index], rightSize, leftIsFixed, rightIsFixed, false, ovfChk, mState,
														new CGActionHandler() {
										@Override
										public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
											cont.append(new CGIAsmLd("ld", "r"+tmpReg, "x" + (sn!=(rightSize-1) ? "+" : "")));
											if(Generator.REG_J8B_ATOM==tmpReg) mState.setJ8bAtomUsed();
											return (long)tmpReg;
										}
									}));
									break;
								case CONST:
									cont.append(opAdd(	index, "r"+accRegs[index], rightSize, leftIsFixed, rightIsFixed, true, ovfChk, mState,
														new CGActionHandler() {
										@Override
										public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
											return (long)(-1==sn ? rightCells.getConst() : sn<rightCells.getSize() ? rightCells.getId(sn) : 0);
										}
									}));
									break;
								case HEAP:
									cont.append(opAdd(	index, "r"+accRegs[index], rightSize, leftIsFixed, rightIsFixed, false, ovfChk, mState,
														new CGActionHandler() {
										@Override
										public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
											cont.append(new CGIAsmIReg('z', true, "r"+tmpReg, rightCells.getId(sn)));
											if(Generator.REG_J8B_ATOM==tmpReg) mState.setJ8bAtomUsed();
											return (long)tmpReg;
										}
									}));
									break;
								case REG:
									cont.append(opAdd(	index, "r"+accRegs[index], rightSize, leftIsFixed, rightIsFixed, false, ovfChk, mState,
														new CGActionHandler() {
										@Override
										public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
											return (long)rightCells.getId(sn);
										}
									}));
									break;
								case STACK:
									if(index<rightCells.getSize()) {
										cont.append(new CGIAsm("pop", "r"+tmpReg));
									}
									cont.append(opAdd(	index, "r"+accRegs[index], rightSize, leftIsFixed, rightIsFixed, false, ovfChk, mState,
														new CGActionHandler() {
										@Override
										public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
											if(Generator.REG_J8B_ATOM==tmpReg) mState.setJ8bAtomUsed();
											return (long)tmpReg;
										}
									}));
									break;
								case STAT:
									cont.append(opAdd(	index, "r"+accRegs[index], rightSize, leftIsFixed, rightIsFixed, false, ovfChk, mState,
														new CGActionHandler() {
										@Override
										public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
											cont.append(new CGIAsmLd("lds", "r"+tmpReg, "_OS_STAT_POOL+" + rightCells.getId(sn)));
											if(Generator.REG_J8B_ATOM==tmpReg) mState.setJ8bAtomUsed();
											return (long)tmpReg;
										}
									}));
									break;
								default:
									throw new CompileException(info + " unsupported yet");
							}
						}
						
						if(ovfChk) {
							cont.append(codeExcsChecker.mathOverflow(cg, scope, excs, null, false, false));
						}
						break;
//---MINUS--------
					case MINUS:
						if(leftIsFixed) {
							acc.setFixed();
						}
						else{
							int requiredBits = (null==castType ?	(leftSize>rightSize ? leftSize : rightSize)*8 :
																	(castType.isReferenceType() ? refSize : castType.getSize())*8);
							if(requiredBits!=acc.getBits()) {
								acc.setBits(requiredBits);
								Utils.accGrow(cont, acc, acc.getOldBytes(), acc.getBytes());
							}
							size = acc.getBytes();
						}
						
						ovfChk = null!=codeExcsChecker.getHandled(excs, J8BException.MathOverflowException.name());
						
						for(int index=0; index<size; index++) {
							switch(rightCells.getType()) {
								case ARGS:
								case STACK_FRAME:
									cont.append(opSub(index, "r"+accRegs[index], rightSize, leftIsFixed, rightIsFixed, false, ovfChk, new CGActionHandler() {
										@Override
										public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
											cont.append(new CGIAsmIReg('y', true, "r"+tmpReg, rightCells.getId(sn),
														getCurBlockScope(scope), (CGBlockScope)rightCells.getObject(),
														CGCells.Type.ARGS==rightCells.getType()));
											if(Generator.REG_J8B_ATOM==tmpReg) mState.setJ8bAtomUsed();
											return (long)tmpReg;
										}
									}));
									break;
								case ARRAY:
									cont.append(opSub(index, "r"+accRegs[index], rightSize, leftIsFixed, rightIsFixed, false, ovfChk, new CGActionHandler() {
										@Override
										public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
											cont.append(new CGIAsmLd("ld", "r"+tmpReg, "x" + (sn!=(rightSize-1) ? "+" : "")));
											if(Generator.REG_J8B_ATOM==tmpReg) mState.setJ8bAtomUsed();
											return (long)tmpReg;
										}
									}));
									break;
								case CONST:
									cont.append(opSub(index, "r"+accRegs[index], rightSize, leftIsFixed, rightIsFixed, true, ovfChk, new CGActionHandler() {
										@Override
										public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
											return (long)rightCells.getId(sn);
										}
									}));
									break;
								case HEAP:
									cont.append(opSub(index, "r"+accRegs[index], rightSize, leftIsFixed, rightIsFixed, false, ovfChk, new CGActionHandler() {
										@Override
										public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
											cont.append(new CGIAsmIReg('z', true, "r"+tmpReg, rightCells.getId(sn)));
											if(Generator.REG_J8B_ATOM==tmpReg) mState.setJ8bAtomUsed();
											return (long)tmpReg;
										}
									}));
									break;
								case REG:
									cont.append(opSub(index, "r"+accRegs[index], rightSize, leftIsFixed, rightIsFixed, false, ovfChk, new CGActionHandler() {
										@Override
										public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
											return (long)rightCells.getId(sn);
										}
									}));
									break;
								case STACK:
									if(index<rightCells.getSize()) {
										cont.append(new CGIAsm("pop", "r"+tmpReg));
									}
									cont.append(opSub(index, "r"+accRegs[index], rightSize, leftIsFixed, rightIsFixed, false, ovfChk, new CGActionHandler() {
										@Override
										public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
											if(Generator.REG_J8B_ATOM==tmpReg) mState.setJ8bAtomUsed();
											return (long)tmpReg;
										}
									}));
									break;
								case STAT:
									cont.append(opSub(index, "r"+accRegs[index], rightSize, leftIsFixed, rightIsFixed, false, ovfChk, new CGActionHandler() {
										@Override
										public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
											cont.append(new CGIAsmLd("lds", "r"+tmpReg, "_OS_STAT_POOL+" + rightCells.getId(sn)));
											if(Generator.REG_J8B_ATOM==tmpReg) mState.setJ8bAtomUsed();
											return (long)tmpReg;
										}
									}));
									break;
								default:
									throw new CompileException(info + " unsupported yet");
							}
						}

						if(ovfChk) {
							cont.append(codeExcsChecker.mathOverflow(cg, scope, excs, null, false, false));
						}
						break;
//---MULT---------
					case MULT:
						size = 0x02;
						if(leftIsFixed) {
							acc.setFixed();
						}
						else{
							int requiredBits = (null==castType ?	(leftSize+rightSize)*8 :
																	(castType.isReferenceType() ? refSize : castType.getSize())*8);
							if(requiredBits>=0x18) requiredBits = 0x20;
							if(requiredBits!=acc.getBits()) {
								acc.setBits(requiredBits);
								// Зачем? Utils.accGrow(cont, acc, acc.getOldBytes(), acc.getBytes());
								// Приводит к 
								//;Assignment:ACC[][byte]=ARGS[1][byte] at line 102
								//ldd r16,y+62
								//;Arithmetic:ACC[][byte] MULT ARGS[2][byte] at line 30
								//ldi r17,0
								//ldd r17,y+61
								//call os_mul8

							}
							size = acc.getBytes();
						}
						
						ovfChk = (size==0x04 && null!=codeExcsChecker.getHandled(excs, J8BException.MathOverflowException.name()));
						
						switch(rightCells.getType()) {
							case ARGS:
							case STACK_FRAME:
								cont.append(opMul(accRegs, leftSize, rightSize, size, leftIsFixed, rightIsFixed, false, ovfChk, new CGActionHandler() {
									@Override
									public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
										cont.append(new CGIAsmIReg('y', true, "r"+dstReg, rightCells.getId(sn),
													getCurBlockScope(scope), (CGBlockScope)rightCells.getObject(),
													CGCells.Type.ARGS==rightCells.getType()));
										return null;
									}
								}));
								break;
							case ARRAY:
								cont.append(opMul(accRegs, leftSize, rightSize, size, leftIsFixed, rightIsFixed, false, ovfChk, new CGActionHandler() {
									@Override
									public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
										cont.append(new CGIAsmLd("ld", "r"+dstReg, "x" + (sn!=(rightSize-1) ? "+" : "")));
										return null;
									}
								}));
								break;
							case CONST:
								cont.append(opMul(accRegs, leftSize, rightSize, size, leftIsFixed, rightIsFixed, true, ovfChk, new CGActionHandler() {
									@Override
									public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
										return (long)rightCells.getId(sn);
									}
								}));
								break;
							case HEAP:
								cont.append(opMul(accRegs, leftSize, rightSize, size, leftIsFixed, rightIsFixed, false, ovfChk, new CGActionHandler() {
									@Override
									public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
										cont.append(new CGIAsmIReg('z', true, "r"+dstReg, rightCells.getId(sn)));
										return null;
									}
								}));
								break;
							case REG:
								cont.append(opMul(accRegs, leftSize, rightSize, size, leftIsFixed, rightIsFixed, false, ovfChk, new CGActionHandler() {
									@Override
									public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
										if(rightCells.getId(sn)!=dstReg) {
											cont.append(new CGIAsmMv("mov", "r"+dstReg, "r"+rightCells.getId(sn)));
										}
										return null;
									}
								}));
								break;
							case STACK:
								final AtomicInteger stackCntr = new AtomicInteger(0);
								cont.append(opMul(accRegs, leftSize, rightSize, size, leftIsFixed, rightIsFixed, false, ovfChk, new CGActionHandler() {
									@Override
									public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
										if(sn<rightCells.getSize()) {
											cont.append(new CGIAsm("pop", "r"+dstReg));
										}
										stackCntr.incrementAndGet();
										return null;
									}
								}));
								for(int index=stackCntr.get(); index<rightCells.getSize(); index++) {
									cont.append(new CGIAsm("pop", "r"+tmpReg));
									if(Generator.REG_J8B_ATOM==tmpReg) mState.setJ8bAtomUsed();
								}
								break;
							case STAT:
								cont.append(opMul(accRegs, leftSize, rightSize, size, leftIsFixed, rightIsFixed, false, ovfChk, new CGActionHandler() {
									@Override
									public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
										cont.append(new CGIAsmLd("lds", "r"+dstReg, "_OS_STAT_POOL+" + rightCells.getId(sn)));
										return null;
									}
								}));
								break;
							default:
								throw new CompileException(info + " unsupported yet");
						}
						if(leftIsFixed || rightIsFixed) {
							acc.setFixed();
						}

						if(ovfChk) {
							cont.append(codeExcsChecker.mathOverflow(cg, scope, excs, null, false, false));
						}
						break;
//---DIV----------
					case DIV:
						size = 0x02;
						if(leftIsFixed) {
							acc.setFixed();
						}
						else{
							int requiredBits = (null==castType ?	leftSize*8 :
																	(castType.isReferenceType() ? refSize : castType.getSize())*8);
							if(requiredBits>=0x18) requiredBits = 0x20;
							if(requiredBits!=acc.getBits()) {
								acc.setBits(requiredBits);
								Utils.accGrow(cont, acc, acc.getOldBytes(), acc.getBytes());
							}
							size = acc.getBytes();
						}
						
						boolean zeroChk = null!=codeExcsChecker.getHandled(excs, J8BException.DivByZeroException.name());
						
						switch(rightCells.getType()) {
							case ARGS:
							case STACK_FRAME:
								cont.append(opDiv(accRegs, leftSize, rightSize, size, leftIsFixed, rightIsFixed, false, zeroChk, new CGActionHandler() {
									@Override
									public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
										cont.append(new CGIAsmIReg('y', true, "r"+dstReg, rightCells.getId(sn),
													getCurBlockScope(scope), (CGBlockScope)rightCells.getObject(),
													CGCells.Type.ARGS==rightCells.getType()));
										return null;
									}
								}, cg, scope, codeExcsChecker, excs));
								break;
							case ARRAY:
								cont.append(opDiv(accRegs, leftSize, rightSize, size, leftIsFixed, rightIsFixed, false, zeroChk, new CGActionHandler() {
									@Override
									public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
										cont.append(new CGIAsmLd("ld", "r"+dstReg, "x" + (sn!=(rightSize-1) ? "+" : "")));
										return null;
									}
								}, cg, scope, codeExcsChecker, excs));
								break;
							case CONST:
								cont.append(opDiv(accRegs, leftSize, rightSize, size, leftIsFixed, rightIsFixed, true, zeroChk, new CGActionHandler() {
									@Override
									public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
										return (long)rightCells.getId(sn);
									}
								}, cg, scope, codeExcsChecker, excs));
								break;
							case HEAP:
								cont.append(opDiv(accRegs, leftSize, rightSize, size, leftIsFixed, rightIsFixed, false, zeroChk, new CGActionHandler() {
									@Override
									public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
										cont.append(new CGIAsmIReg('z', true, "r"+dstReg, rightCells.getId(sn)));
										return null;
									}
								}, cg, scope, codeExcsChecker, excs));
								break;
							case REG:
								cont.append(opDiv(accRegs, leftSize, rightSize, size, leftIsFixed, rightIsFixed, false, zeroChk, new CGActionHandler() {
									@Override
									public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
										if(rightCells.getId(sn)!=dstReg) {
											cont.append(new CGIAsmMv("mov", "r"+dstReg, "r"+rightCells.getId(sn)));
										}
										return null;
									}
								}, cg, scope, codeExcsChecker, excs));
								break;
							case STACK:
								final AtomicInteger stackCntr = new AtomicInteger(0);
								cont.append(opDiv(accRegs, leftSize, rightSize, size, leftIsFixed, rightIsFixed, false, zeroChk, new CGActionHandler() {
									@Override
									public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
										if(sn<rightCells.getSize()) {
											cont.append(new CGIAsm("pop", "r"+dstReg));
										}
										stackCntr.incrementAndGet();
										return null;
									}
								}, cg, scope, codeExcsChecker, excs));
								for(int index=stackCntr.get(); index<rightCells.getSize(); index++) {
									cont.append(new CGIAsm("pop", "r"+tmpReg));
									if(Generator.REG_J8B_ATOM==tmpReg) mState.setJ8bAtomUsed();
								}
								break;
							case STAT:
								cont.append(opDiv(accRegs, leftSize, rightSize, size, leftIsFixed, rightIsFixed, false, zeroChk, new CGActionHandler() {
									@Override
									public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
										cont.append(new CGIAsmLd("lds", "r"+dstReg, "_OS_STAT_POOL+" + rightCells.getId(sn)));
										return null;
									}
								}, cg, scope, codeExcsChecker, excs));
								break;
							default:
								throw new CompileException(info + " unsupported yet");
						}
						if(leftIsFixed || rightIsFixed) {
							acc.setFixed();
						}
						break;
//---MOD----------
					case MOD:
						size = 0x02;
						if(leftIsFixed) {
							acc.setFixed();
						}
						else{
							int requiredBits = (null==castType ?	leftSize*8 :
																	(castType.isReferenceType() ? refSize : castType.getSize())*8);
							if(requiredBits>=0x18) requiredBits = 0x20;
							if(requiredBits!=acc.getBits()) {
								acc.setBits(requiredBits);
								Utils.accGrow(cont, acc, acc.getOldBytes(), acc.getBytes());
							}
							size = acc.getBytes();
						}
						
						switch(rightCells.getType()) {
							case ARGS:
							case STACK_FRAME:
								cont.append(opMod(accRegs, leftSize, rightSize, size, leftIsFixed, rightIsFixed, false, new CGActionHandler() {
									@Override
									public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
										cont.append(new CGIAsmIReg('y', true, "r"+dstReg, rightCells.getId(sn),
													getCurBlockScope(scope), (CGBlockScope)rightCells.getObject(),
													CGCells.Type.ARGS==rightCells.getType()));
										return null;
									}
								}));
								break;
							case ARRAY:
								cont.append(opMod(accRegs, leftSize, rightSize, size, leftIsFixed, rightIsFixed, false, new CGActionHandler() {
									@Override
									public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
										cont.append(new CGIAsmLd("ld", "r"+dstReg, "x" + (sn!=(rightSize-1) ? "+" : "")));
										return null;
									}
								}));
								break;
							case CONST:
								cont.append(opMod(accRegs, leftSize, rightSize, size, leftIsFixed, rightIsFixed, true, new CGActionHandler() {
									@Override
									public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
										return (long)rightCells.getId(sn);
									}
								}));
								break;
							case HEAP:
								cont.append(opMod(accRegs, leftSize, rightSize, size, leftIsFixed, rightIsFixed, false, new CGActionHandler() {
									@Override
									public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
										cont.append(new CGIAsmIReg('z', true, "r"+dstReg, rightCells.getId(sn)));
										return null;
									}
								}));
								break;
							case REG:
								cont.append(opMod(accRegs, leftSize, rightSize, size, leftIsFixed, rightIsFixed, false, new CGActionHandler() {
									@Override
									public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
										if(rightCells.getId(sn)!=dstReg) {
											cont.append(new CGIAsmMv("mov", "r"+dstReg, "r"+rightCells.getId(sn)));
										}
										return null;
									}
								}));
								break;
							case STACK:
								final AtomicInteger stackCntr = new AtomicInteger(0);
								cont.append(opMod(accRegs, leftSize, rightSize, size, leftIsFixed, rightIsFixed, false, new CGActionHandler() {
									@Override
									public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
										if(sn<rightCells.getSize()) {
											cont.append(new CGIAsm("pop", "r"+dstReg));
										}
										stackCntr.incrementAndGet();
										return null;
									}
								}));
								for(int index=stackCntr.get(); index<rightCells.getSize(); index++) {
									cont.append(new CGIAsm("pop", "r"+tmpReg));
									if(Generator.REG_J8B_ATOM==tmpReg) mState.setJ8bAtomUsed();
								}
								break;
							case STAT:
								cont.append(opMod(accRegs, leftSize, rightSize, size, leftIsFixed, rightIsFixed, false, new CGActionHandler() {
									@Override
									public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
										cont.append(new CGIAsmLd("lds", "r"+dstReg, "_OS_STAT_POOL+" + rightCells.getId(sn)));
										return null;
									}
								}));
								break;
							default:
								throw new CompileException(info + " unsupported yet");
						}
						break;
//---SHL----------
					case SHL:
						size = 0x02;
						if(leftIsFixed) {
							acc.setFixed();
						}
						else{
							int requiredBits = (null==castType ?	0x20 :
																	(castType.isReferenceType() ? refSize : castType.getSize())*8);
							if(requiredBits>=0x18) requiredBits = 0x20;
							if(requiredBits!=acc.getBits()) {
								acc.setBits(requiredBits);
								Utils.accGrow(cont, acc, acc.getOldBytes(), acc.getBytes());
							}
							size = acc.getBytes();
						}
						
						ovfChk = (size==0x04 && null!=codeExcsChecker.getHandled(excs, J8BException.MathOverflowException.name()));
						
						switch(rightCells.getType()) {
							case ARGS:
							case STACK_FRAME:
								cont.append(opShl(	accRegs, leftSize, rightSize, size, leftIsFixed, rightIsFixed, false, ovfChk, mState,
													new CGActionHandler() {
									@Override
									public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
										cont.append(new CGIAsmIReg('y', true, "r"+dstReg, rightCells.getId(sn),
													getCurBlockScope(scope), (CGBlockScope)rightCells.getObject(),
													CGCells.Type.ARGS==rightCells.getType()));
										return null;
									}
								}));
								break;
							case ARRAY:
								cont.append(opShl(	accRegs, leftSize, rightSize, size, leftIsFixed, rightIsFixed, false, ovfChk, mState,
													new CGActionHandler() {
									@Override
									public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
										cont.append(new CGIAsmLd("ld", "r"+dstReg, "x" + (sn!=(rightSize-1) ? "+" : "")));
										return null;
									}
								}));
								break;
							case CONST:
								cont.append(opShl(	accRegs, leftSize, rightSize, size, leftIsFixed, rightIsFixed, true, ovfChk, mState,
													new CGActionHandler() {
									@Override
									public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
										return (long)rightCells.getId(sn);
									}
								}));
								break;
							case HEAP:
								cont.append(opShl(	accRegs, leftSize, rightSize, size, leftIsFixed, rightIsFixed, false, ovfChk, mState,
													new CGActionHandler() {
									@Override
									public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
										cont.append(new CGIAsmIReg('z', true, "r"+dstReg, rightCells.getId(sn)));
										return null;
									}
								}));
								break;
							case REG:
								cont.append(opShl(	accRegs, leftSize, rightSize, size, leftIsFixed, rightIsFixed, false, ovfChk, mState,
													new CGActionHandler() {
									@Override
									public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
										if(rightCells.getId(sn)!=dstReg) {
											cont.append(new CGIAsmMv("mov", "r"+dstReg, "r"+rightCells.getId(sn)));
										}
										return null;
									}
								}));
								break;
							case STACK:
								final AtomicInteger stackCntr = new AtomicInteger(0);
								cont.append(opShl(	accRegs, leftSize, rightSize, size, leftIsFixed, rightIsFixed, false, ovfChk, mState,
													new CGActionHandler() {
									@Override
									public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
										if(sn<rightCells.getSize()) {
											cont.append(new CGIAsm("pop", "r"+dstReg));
										}
										stackCntr.incrementAndGet();
										return null;
									}
								}));
								for(int index=stackCntr.get(); index<rightCells.getSize(); index++) {
									cont.append(new CGIAsm("pop", "r"+tmpReg));
									if(Generator.REG_J8B_ATOM==tmpReg) mState.setJ8bAtomUsed();
								}
								break;
							case STAT:
								cont.append(opShl(	accRegs, leftSize, rightSize, size, leftIsFixed, rightIsFixed, false, ovfChk, mState,
													new CGActionHandler() {
									@Override
									public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
										cont.append(new CGIAsmLd("lds", "r"+dstReg, "_OS_STAT_POOL+" + rightCells.getId(sn)));
										return null;
									}
								}));
								break;
							default:
								throw new CompileException(info + " unsupported yet");
						}

						if(ovfChk) {
							cont.append(codeExcsChecker.mathOverflow(cg, scope, excs, null, false, false));
						}
						break;
//---SHR----------
					case SHR:
						size = 0x02;
						if(leftIsFixed) {
							acc.setFixed();
						}
						else{
							int requiredBits = (null==castType ?	leftSize*8 :
																	(castType.isReferenceType() ? refSize : castType.getSize())*8);
							if(requiredBits>=0x18) requiredBits = 0x20;
							if(requiredBits!=acc.getBits()) {
								acc.setBits(requiredBits);
								Utils.accGrow(cont, acc, acc.getOldBytes(), acc.getBytes());
							}
							size = acc.getBytes();
						}
						
						switch(rightCells.getType()) {
							case ARGS:
							case STACK_FRAME:
								cont.append(opShr(accRegs, leftSize, rightSize, size, leftIsFixed, rightIsFixed, false, new CGActionHandler() {
									@Override
									public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
										cont.append(new CGIAsmIReg('y', true, "r"+dstReg, rightCells.getId(sn),
													getCurBlockScope(scope), (CGBlockScope)rightCells.getObject(),
													CGCells.Type.ARGS==rightCells.getType()));
										return null;
									}
								}));
								break;
							case ARRAY:
								cont.append(opShr(accRegs, leftSize, rightSize, size, leftIsFixed, rightIsFixed, false, new CGActionHandler() {
									@Override
									public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
										cont.append(new CGIAsmLd("ld", "r"+dstReg, "x" + (sn!=(rightSize-1) ? "+" : "")));
										return null;
									}
								}));
								break;
							case CONST:
								cont.append(opShr(accRegs, leftSize, rightSize, size, leftIsFixed, rightIsFixed, true, new CGActionHandler() {
									@Override
									public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
										return (long)rightCells.getId(sn);
									}
								}));
								break;
							case HEAP:
								cont.append(opShr(accRegs, leftSize, rightSize, size, leftIsFixed, rightIsFixed, false, new CGActionHandler() {
									@Override
									public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
										cont.append(new CGIAsmIReg('z', true, "r"+dstReg, rightCells.getId(sn)));
										return null;
									}
								}));
								break;
							case REG:
								cont.append(opShr(accRegs, leftSize, rightSize, size, leftIsFixed, rightIsFixed, false, new CGActionHandler() {
									@Override
									public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
										if(rightCells.getId(sn)!=dstReg) {
											cont.append(new CGIAsmMv("mov", "r"+dstReg, "r"+rightCells.getId(sn)));
										}
										return null;
									}
								}));
								break;
							case STACK:
								final AtomicInteger stackCntr = new AtomicInteger(0);
								cont.append(opShr(accRegs, leftSize, rightSize, size, leftIsFixed, rightIsFixed, false, new CGActionHandler() {
									@Override
									public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
										if(sn<rightCells.getSize()) {
											cont.append(new CGIAsm("pop", "r"+dstReg));
										}
										stackCntr.incrementAndGet();
										return null;
									}
								}));
								for(int index=stackCntr.get(); index<rightCells.getSize(); index++) {
									cont.append(new CGIAsm("pop", "r"+tmpReg));
									if(Generator.REG_J8B_ATOM==tmpReg) mState.setJ8bAtomUsed();
								}
								break;
							case STAT:
								cont.append(opShr(accRegs, leftSize, rightSize, size, leftIsFixed, rightIsFixed, false, new CGActionHandler() {
									@Override
									public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
										cont.append(new CGIAsmLd("lds", "r"+dstReg, "_OS_STAT_POOL+" + rightCells.getId(sn)));
										return null;
									}
								}));
								break;
							default:
								throw new CompileException(info + " unsupported yet");
						}
						break;
					default:
						throw new CompileException(info + " unsupported yet");
				}
				break;
//-REG------------
			case REG:
				switch(op) {
					case ROL:
						size = (null==castType ? leftSize : (castType.isReferenceType() ? refSize : castType.getSize())*8);

						boolean ovfChk = (size==0x04 && null!=codeExcsChecker.getHandled(excs, J8BException.MathOverflowException.name()));

						for(int index=0; index<size; index++) {
							switch(rightCells.getType()) {
								case FLAG:
									cont.append(new CGIRAsm("rol", "r"+leftCells.getId(size-index-1)));
									break;
								default:
									throw new CompileException(info + " unsupported yet");
							}
						}
						break;
					case ROR:
						size = (null==castType ? leftSize : (castType.isReferenceType() ? refSize : castType.getSize())*8);
						
						for(int index=0; index<size; index++) {
							switch(rightCells.getType()) {
								case FLAG:
									cont.append(new CGIRAsm("ror", "r"+leftCells.getId(index)));
									break;
								default:
									throw new CompileException(info + " unsupported yet");
							}
						}
						break;
					default:
						throw new CompileException(info + ", unsupported left cells type");
				}
				break;
			default:
				throw new CompileException(info + ", unsupported left cells type");
		}
		
		if(mState.isEnabled() && (!mState.isSafe() || mState.getJ8bAtomUsed())) {
			dispLockCont.append(new CGIAsmCall(CALL_INSTR, RTOSLibs.DISPATCHER_PROCS.OS_DISPATCHER_LOCK.name().toLowerCase(), true));
			cont.append(new CGIAsmCall(CALL_INSTR, RTOSLibs.DISPATCHER_PROCS.OS_DISPATCHER_UNLOCK.name().toLowerCase(), true));
		}

		scope.append(cont);
	}
}
