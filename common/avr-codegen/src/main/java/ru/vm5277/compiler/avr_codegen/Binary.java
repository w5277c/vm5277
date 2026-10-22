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
import java.util.concurrent.atomic.AtomicBoolean;
import ru.vm5277.common.VarType;
import ru.vm5277.common.cg.CGAccum;
import ru.vm5277.common.cg.CGActionHandler;
import ru.vm5277.common.cg.CGCells;
import static ru.vm5277.common.cg.CGCells.Type.ACC;
import static ru.vm5277.common.cg.CGCells.Type.ARGS;
import static ru.vm5277.common.cg.CGCells.Type.ARRAY;
import static ru.vm5277.common.cg.CGCells.Type.CONST;
import static ru.vm5277.common.cg.CGCells.Type.HEAP;
import static ru.vm5277.common.cg.CGCells.Type.REG;
import static ru.vm5277.common.cg.CGCells.Type.STACK;
import static ru.vm5277.common.cg.CGCells.Type.STACK_FRAME;
import static ru.vm5277.common.cg.CGCells.Type.STAT;
import ru.vm5277.common.cg.CGExcs;
import static ru.vm5277.common.cg.CodeGenerator.getCurBlockScope;
import ru.vm5277.common.cg.items.CGIAsm;
import ru.vm5277.common.cg.items.CGIAsmIReg;
import ru.vm5277.common.cg.items.CGIAsmLd;
import ru.vm5277.common.cg.items.CGIContainer;
import ru.vm5277.common.cg.items.CGIRAsm;
import ru.vm5277.common.cg.items.CGIText;
import ru.vm5277.common.cg.scopes.CGBlockScope;
import ru.vm5277.common.cg.scopes.CGScope;
import static ru.vm5277.common.cg.scopes.CGScope.VERBOSE_LO;
import ru.vm5277.common.enums.RtosFeature;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.lexer.Operator;

public class Binary {
	static CGIContainer opAnd(int index, String dstReg, int size, boolean notUsed1, boolean notUsed2, boolean rightIsConstant, CGActionHandler handler)
																																	throws CompileException {
		CGIContainer cont = new CGIContainer("binary:and");
		if(index<size) {
			if(rightIsConstant) {
				cont.append(new CGIRAsm("andi", dstReg, ""+handler.action(cont, index, null)));
			}
			else {
				cont.append(new CGIRAsm("and", dstReg, "r"+handler.action(cont, index, null)));
			}
		}
		else {
			cont.append(new CGIRAsm("andi", dstReg, "0"));
		}
		return cont;
	}
	static CGIContainer opOr(int index, String dstReg, int size, boolean notUsed1, boolean notUsed2, boolean rightIsConstant, CGActionHandler handler)
																																	throws CompileException {
		CGIContainer cont = new CGIContainer("binary:or");
		if(index<size) {
			if(rightIsConstant) {
				cont.append(new CGIRAsm("ori", dstReg, ""+handler.action(cont, index, null)));
			}
			else {
				cont.append(new CGIRAsm("or", dstReg, "r"+handler.action(cont, index, null)));
			}
		}
		else {
			cont.append(new CGIRAsm("ori", dstReg, "0"));
		}
		return cont;
	}
	static CGIContainer opXor(int index, String dstReg, int size, boolean regAccumEhisFree, boolean notUsed2, boolean rightIsConstant, CGActionHandler handler)
																																	throws CompileException {
		CGIContainer cont = new CGIContainer("binary:xor");
		if(index<size) {
			if(rightIsConstant) {
				short octet = handler.action(cont, index, null).shortValue();
				if(0x00==octet) {
					cont.append(new CGIRAsm("eor", dstReg, "c0x00"));
				}
				else if(0x01==octet) {
					cont.append(new CGIRAsm("eor", dstReg, "c0x01"));
				}
				else if(0xff==octet) {
					cont.append(new CGIRAsm("eor", dstReg, "c0xff"));
				}
				else if(regAccumEhisFree) {
					cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_ACCUM_EH, ""+octet));
					cont.append(new CGIRAsm("eor", dstReg, "r"+Generator.REG_ACCUM_EH));
				}
				else {
					cont.append(new CGIAsm("push", "r31"));
					cont.append(new CGIAsmLd("ldi", "r31", ""+octet));
					cont.append(new CGIRAsm("eor", dstReg, "r31"));
					cont.append(new CGIAsm("pop", "r31"));
				}
			}
			else {
				cont.append(new CGIRAsm("eor", dstReg, "r"+handler.action(cont, index, null)));
			}
		}
		else {
			cont.append(new CGIRAsm("eor", dstReg, "c0x00"));
		}
		return cont;
	}

	
	public static void exec(final CGScope scope, CGCells leftCells, VarType leftType, Operator op, final CGCells rightCells, VarType rightType,
							VarType castType, CGExcs excs, int refSize, CGAccum acc, Set<RtosFeature> rtosFeatures, int verbose) throws CompileException {
		
		final CGIContainer cont = new CGIContainer();
		
		String info =	"Binary:" + leftCells + "[" + leftType + "]" + " " + op + " " + rightCells + "[" + rightType + "]" +
						(null!=castType ? ", cast:" + castType : "");

		if(VERBOSE_LO <= verbose) {
			cont.append(new CGIText(";" + info + " at line " + scope.getSp().getLine()));
		}
		
		boolean isFixed =	(null!=leftType && leftType.isFixedPoint()) || (null!=rightType && rightType.isFixedPoint() ||
							(null!=castType && castType.isFixedPoint()));
		if(isFixed) {
			throw new CompileException("Binary operation '" + op + "' with FIXED operands is not supported");
		}
		
		CGIContainer dispLockCont = new CGIContainer();
		cont.append(dispLockCont);
		
		final Byte tmpReg = (0x04==acc.getBytes()) ? Generator.REG_J8B_ATOM : Generator.REG_ACCUM_EH;
		// Может быть зименено в коде ниже, если блок требует блокировки потоков
		boolean threadSafe = true;
		// Говорит об использовании временного регистра, если в итоге назначенный регистр 'j8b_atom' и использован, то нужно блокировать потоки
		final AtomicBoolean tmpRegUsed = new AtomicBoolean(false); 
		
		switch(leftCells.getType()) {
//-ACC------------
			case ACC:
				byte[] accRegs = Utils.getAccRegs(0x04);
				
				int leftSize = null==leftType ? leftCells.getSize() : (-1==leftType.getSize() ? refSize : leftType.getSize());		
				if(0==leftSize && CGCells.Type.ACC==rightCells.getType()) {
					leftSize = acc.getBytes();
				}
				final int rightSize = null==rightType ? rightCells.getSize() : (-1==rightType.getSize() ? refSize : rightType.getSize());

				int requiredBits = (null==castType ?	(leftSize>rightSize ? leftSize : rightSize)*8 :
														(castType.isReferenceType() ? refSize : castType.getSize())*8);
				if(requiredBits>acc.getBits()) {
					acc.setBits(requiredBits);
					Utils.accGrow(cont, acc, acc.getOldBytes(), acc.getBytes());
				}
				int size = acc.getBytes();

				switch(op) {
//---AND----------
					case BIT_AND:
						for(int index=0; index<size; index++) {
							switch(rightCells.getType()) {
								case ARGS:
								case STACK_FRAME:
									cont.append(opAnd(index, "r"+accRegs[index], rightSize, false, false, false, new CGActionHandler() {
										@Override
										public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
											cont.append(new CGIAsmIReg('y', true, "r"+tmpReg, rightCells.getId(sn),
														getCurBlockScope(scope), (CGBlockScope)rightCells.getObject(),
														CGCells.Type.ARGS==rightCells.getType()));
											tmpRegUsed.set(true);
											return (long)tmpReg;
										}
									}));
									break;
								case ARRAY:
									cont.append(opAnd(index, "r"+accRegs[index], rightSize, false, false, false, new CGActionHandler() {
										@Override
										public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
											cont.append(new CGIAsmLd("ld", "r"+tmpReg, "x" + (sn!=(rightSize-1) ? "+" : "")));
											tmpRegUsed.set(true);
											return (long)tmpReg;
										}
									}));
									break;
								case CONST:
									cont.append(opAnd(index, "r"+accRegs[index], rightSize, false, false, true, new CGActionHandler() {
										@Override
										public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
											return (long)rightCells.getId(sn);
										}
									}));
									break;
								case HEAP:
									cont.append(opAnd(index, "r"+accRegs[index], rightSize, false, false, false, new CGActionHandler() {
										@Override
										public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
											cont.append(new CGIAsmIReg('z', true, "r"+tmpReg, rightCells.getId(sn)));
											tmpRegUsed.set(true);
											return (long)tmpReg;
										}
									}));
									break;
								case REG:
									cont.append(opAnd(index, "r"+accRegs[index], rightSize, false, false, false, new CGActionHandler() {
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
									cont.append(opAnd(index, "r"+accRegs[index], rightSize, false, false, false, new CGActionHandler() {
										@Override
										public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
											tmpRegUsed.set(true);
											return (long)tmpReg;
										}
									}));
									break;
								case STAT:
									cont.append(opAnd(index, "r"+accRegs[index], rightSize, false, false, false, new CGActionHandler() {
										@Override
										public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
											cont.append(new CGIAsmLd("lds", "r"+tmpReg, "_OS_STAT_POOL+" + rightCells.getId(sn)));
											tmpRegUsed.set(true);
											return (long)tmpReg;
										}
									}));
									break;
								default:
									throw new CompileException(info + " unsupported yet");
							}
						}
						break;
//---OR-----------
					case BIT_OR:
						for(int index=0; index<size; index++) {
							switch(rightCells.getType()) {
								case ARGS:
								case STACK_FRAME:
									cont.append(opOr(index, "r"+accRegs[index], rightSize, false, false, false, new CGActionHandler() {
										@Override
										public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
											cont.append(new CGIAsmIReg('y', true, "r"+tmpReg, rightCells.getId(sn),
														getCurBlockScope(scope), (CGBlockScope)rightCells.getObject(),
														CGCells.Type.ARGS==rightCells.getType()));
											tmpRegUsed.set(true);
											return (long)tmpReg;
										}
									}));
									break;
								case ARRAY:
									cont.append(opOr(index, "r"+accRegs[index], rightSize, false, false, false, new CGActionHandler() {
										@Override
										public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
											cont.append(new CGIAsmLd("ld", "r"+tmpReg, "x" + (sn!=(rightSize-1) ? "+" : "")));
											tmpRegUsed.set(true);
											return (long)tmpReg;
										}
									}));
									break;
								case CONST:
									cont.append(opOr(index, "r"+accRegs[index], rightSize, false, false, true, new CGActionHandler() {
										@Override
										public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
											return (long)rightCells.getId(sn);
										}
									}));
									break;
								case HEAP:
									cont.append(opOr(index, "r"+accRegs[index], rightSize, false, false, false, new CGActionHandler() {
										@Override
										public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
											cont.append(new CGIAsmIReg('z', true, "r"+tmpReg, rightCells.getId(sn)));
											tmpRegUsed.set(true);
											return (long)tmpReg;
										}
									}));
									break;
								case REG:
									cont.append(opOr(index, "r"+accRegs[index], rightSize, false, false, false, new CGActionHandler() {
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
									cont.append(opOr(index, "r"+accRegs[index], rightSize, false, false, false, new CGActionHandler() {
										@Override
										public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
											tmpRegUsed.set(true);
											return (long)tmpReg;
										}
									}));
									break;
								case STAT:
									cont.append(opOr(index, "r"+accRegs[index], rightSize, false, false, false, new CGActionHandler() {
										@Override
										public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
											cont.append(new CGIAsmLd("lds", "r"+tmpReg, "_OS_STAT_POOL+" + rightCells.getId(sn)));
											tmpRegUsed.set(true);
											return (long)tmpReg;
										}
									}));
									break;
								default:
									throw new CompileException(info + " unsupported yet");
							}
						}
						break;
//---XOR----------
					case BIT_XOR:
						for(int index=0; index<size; index++) {
							switch(rightCells.getType()) {
								case ARGS:
								case STACK_FRAME:
									cont.append(opXor(index, "r"+accRegs[index], rightSize, false, false, false, new CGActionHandler() {
										@Override
										public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
											cont.append(new CGIAsmIReg('y', true, "r"+tmpReg, rightCells.getId(sn),
														getCurBlockScope(scope), (CGBlockScope)rightCells.getObject(),
														CGCells.Type.ARGS==rightCells.getType()));
											tmpRegUsed.set(true);
											return (long)tmpReg;
										}
									}));
									break;
								case ARRAY:
									cont.append(opXor(index, "r"+accRegs[index], rightSize, false, false, false, new CGActionHandler() {
										@Override
										public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
											cont.append(new CGIAsmLd("ld", "r"+tmpReg, "x" + (sn!=(rightSize-1) ? "+" : "")));
											tmpRegUsed.set(true);
											return (long)tmpReg;
										}
									}));
									break;
								case CONST:
									cont.append(opXor(index, "r"+accRegs[index], rightSize, false, false, true, new CGActionHandler() {
										@Override
										public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
											return (long)rightCells.getId(sn);
										}
									}));
									break;
								case HEAP:
									cont.append(opXor(index, "r"+accRegs[index], rightSize, false, false, false, new CGActionHandler() {
										@Override
										public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
											cont.append(new CGIAsmIReg('z', true, "r"+tmpReg, rightCells.getId(sn)));
											tmpRegUsed.set(true);
											return (long)tmpReg;
										}
									}));
									break;
								case REG:
									cont.append(opXor(index, "r"+accRegs[index], rightSize, false, false, false, new CGActionHandler() {
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
									cont.append(opXor(index, "r"+accRegs[index], rightSize, false, false, false, new CGActionHandler() {
										@Override
										public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
											tmpRegUsed.set(true);
											return (long)tmpReg;
										}
									}));
									break;
								case STAT:
									cont.append(opXor(index, "r"+accRegs[index], rightSize, false, false, false, new CGActionHandler() {
										@Override
										public Long action(CGIContainer cont, int sn, Byte dstReg) throws CompileException {
											cont.append(new CGIAsmLd("lds", "r"+tmpReg, "_OS_STAT_POOL+" + rightCells.getId(sn)));
											tmpRegUsed.set(true);
											return (long)tmpReg;
										}
									}));
									break;
								default:
									throw new CompileException(info + " unsupported yet");
							}
						}
						break;
					default:
						throw new CompileException(info + " unsupported yet");
				}
				break;
			default:
				throw new CompileException(info + ", unsupported left cells type:" + leftCells.getType());
		}
		scope.append(cont);
	}
}
