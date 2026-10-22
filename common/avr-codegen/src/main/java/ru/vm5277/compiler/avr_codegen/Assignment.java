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
import ru.vm5277.common.VarType;
import ru.vm5277.common.cg.CGAccum;
import ru.vm5277.common.cg.CGCells;
import static ru.vm5277.common.cg.CGCells.Type.ACC;
import static ru.vm5277.common.cg.CGCells.Type.ARGS;
import static ru.vm5277.common.cg.CGCells.Type.ARRAY;
import static ru.vm5277.common.cg.CGCells.Type.CONST;
import static ru.vm5277.common.cg.CGCells.Type.FLAG;
import static ru.vm5277.common.cg.CGCells.Type.HEAP;
import static ru.vm5277.common.cg.CGCells.Type.HEAP_ALT;
import static ru.vm5277.common.cg.CGCells.Type.LABEL;
import static ru.vm5277.common.cg.CGCells.Type.REG;
import static ru.vm5277.common.cg.CGCells.Type.STACK;
import static ru.vm5277.common.cg.CGCells.Type.STACK_FRAME;
import static ru.vm5277.common.cg.CGCells.Type.STAT;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.items.CGIAsm;
import ru.vm5277.common.cg.items.CGIAsmCall;
import ru.vm5277.common.cg.items.CGIAsmIReg;
import ru.vm5277.common.cg.items.CGIAsmLd;
import ru.vm5277.common.cg.items.CGIAsmLdLabel;
import ru.vm5277.common.cg.items.CGIAsmMv;
import ru.vm5277.common.cg.items.CGIContainer;
import ru.vm5277.common.cg.items.CGIRAsm;
import ru.vm5277.common.cg.items.CGIText;
import ru.vm5277.common.cg.scopes.CGBlockScope;
import ru.vm5277.common.cg.scopes.CGScope;
import static ru.vm5277.common.cg.scopes.CGScope.VERBOSE_LO;
import ru.vm5277.common.enums.RTOSLibs;
import ru.vm5277.common.enums.RtosFeature;
import ru.vm5277.common.exceptions.CompileException;
import static ru.vm5277.compiler.avr_codegen.Generator.CALL_INSTR;

public interface Assignment {
	public static void exec(CGScope scope, CGCells leftCells, VarType leftType, CGCells rightCells, VarType rightType, VarType castType,
									int refSize, CGAccum acc, Set<RtosFeature> rtosFeatures, int verbose) throws CompileException {
		//TODO Последние акутальные изменения  cellTocells STACK_FRAME[0,1][short]=STACK[x0][short]
		CGIContainer cont = new CGIContainer();
		
		String info =	"Assignment:" + leftCells + "[" + leftType + "]" + "=" + rightCells + "[" + rightType + "]" +
						(null!=castType ? ", cast:" + castType : "");
		if(VERBOSE_LO <= verbose) {
			cont.append(new CGIText(";" + info + " at line " + scope.getSp().getLine()));
		}

		if(null!=castType && (null==leftType || castType!=leftType)) {
			leftType = castType;
			if(CGCells.Type.ACC==leftCells.getType()) {
				if(leftType.isFixedPoint()) {
					acc.setFixed();
				}
				else {
					acc.setSize(leftType);
				}
				//Utils.accGrow(cont, acc, acc.getOldBytes(), acc.getBytes());
			}
		}
		
		int leftSize = null==leftType ? leftCells.getSize() : (-1==leftType.getSize() ? refSize : leftType.getSize());		
		int rightSize = null==rightType ? rightCells.getSize() : (-1==rightType.getSize() ? refSize : rightType.getSize());
		if(0==rightSize && CGCells.Type.ACC==rightCells.getType()) {
			rightSize = acc.getBytes();
		}
		else if (0==rightSize && CGCells.Type.LABEL==rightCells.getType()) {
			rightSize = 0x02;
		}
		
		int size = (null==castType ? leftSize>rightSize ? leftSize : rightSize : (castType.isReferenceType() ? refSize : castType.getSize()));
		
		boolean leftIsFixed = null!=leftType && leftType.isFixedPoint();
		boolean rightIsFixed = null!=rightType && rightType.isFixedPoint();

		CGIContainer dispLockCont = new CGIContainer();
		cont.append(dispLockCont);

		if(leftCells.getSize()<size && CGCells.Type.ACC==leftCells.getType()) {
			// Аккумулятор можно расширить
			if(leftIsFixed) {
				acc.setFixed();
			}
			else {
				acc.setBytes(size);
			}
			//cont.append(accResize());
		}

//		if(CGCells.Type.ACC==leftCells.getType() && acc.getBytes()<leftSize) { //TODO костыль
//			acc.setBytes(leftSize);
//		}
		
		// Потоко не безопасо, если используется j8b_atom, если один из операндов HEAP, ARRAY, STAT или HEAP_ALT и общий размер больше байта
		boolean threadSafe = true;
		
		byte[] accRegs = Utils.getAccRegs(0x04);
		if(CGCells.Type.ACC==leftCells.getType() && null!=leftCells.getIds()) {
			// Указаны конкретные регистры аккумулятора
			byte[] newAcRegs = new byte[leftCells.getSize()];
			for(int i=0; i<leftCells.getSize(); i++) {
				newAcRegs[i] = accRegs[leftCells.getId(i)];
			}
			accRegs = newAcRegs;
		}
		
		for(int index=0; index<size; index++) {
			switch(rightCells.getType()) { //src
				case ACC:
					// Если размеры операндов равны, то записываем регистры аккумулятора, но если аккумулятор меньше, то на свободные ячейки пишем c0x00
					String[] regsStr = new String[size+1]; 
					for(int i=0; i<=size; i++) { //Специально берем на 1 больше - для преобразований с fixed
						if(i<rightSize) {
							regsStr[i] = "r" + accRegs[i];
						}
						else  {
							regsStr[i] = "c0x00";
						}
					}
					// Теперь размеры одинаковые
					switch(leftCells.getType()) {
						case ARRAY:
							if(1<size) threadSafe = false;
							
							if(leftIsFixed && !rightIsFixed) {
								cont.append(new CGIAsm("st", "x+," + (0==index ? "c0x00" : regsStr[index-1])));
							}
							else if(!leftIsFixed && rightIsFixed) {
								cont.append(new CGIAsm("st", "x+," + regsStr[index+1]));
							}
							else {
								cont.append(new CGIAsm("st", "x+," + regsStr[index]));
							}
							break;
						case HEAP:
							if(1<size) threadSafe = false;
							
							if(leftIsFixed && !rightIsFixed) {
								cont.append(new CGIAsmIReg('z', false, (0==index ? "c0x00" : regsStr[index-1]), leftCells.getId(index)));
							}
							else if(!leftIsFixed && rightIsFixed) {
								cont.append(new CGIAsmIReg('z', false, regsStr[index+1], leftCells.getId(index)));
							}
							else {
								cont.append(new CGIAsmIReg('z', false, regsStr[index], leftCells.getId(index)));
							}
							break;
						case REG:
							if(leftIsFixed && !rightIsFixed) {
								if(0==index) {
									cont.append(new CGIAsmLd("ldi", "r"+leftCells.getId(index), "0"));
								}
								else {
									cont.append(new CGIAsmMv("mov", "r"+leftCells.getId(index), regsStr[index-1]));
								}
							}
							else if(!leftIsFixed && rightIsFixed) {
								if((index+1)<rightSize) {
									cont.append(new CGIAsmMv("mov", "r"+leftCells.getId(index), regsStr[index+1]));
								}
								else {
									cont.append(new CGIAsmLd("ldi", "r"+leftCells.getId(index), "0"));
								}
							}
							else {
								if(index<rightSize) {
									cont.append(new CGIAsmMv("mov", "r"+leftCells.getId(index), regsStr[index]));
								}
								else {
									cont.append(new CGIAsmLd("ldi", "r"+leftCells.getId(index), "0"));
								}
							}
							break;
						case ARGS:
						case STACK_FRAME:
							if(leftIsFixed && !rightIsFixed) {
								cont.append(new CGIAsmIReg(	'y', false, (0==index ? "c0x00" : regsStr[index-1]), leftCells.getId(index),
															CodeGenerator.getCurBlockScope(scope), (CGBlockScope)leftCells.getObject(),
															CGCells.Type.ARGS == leftCells.getType()));

							}
							else if(!leftIsFixed && rightIsFixed) {
								cont.append(new CGIAsmIReg(	'y', false, regsStr[index+1], leftCells.getId(index), CodeGenerator.getCurBlockScope(scope),
															(CGBlockScope)leftCells.getObject(), CGCells.Type.ARGS == leftCells.getType()));

							}
							else {
								cont.append(new CGIAsmIReg(	'y', false, regsStr[index], leftCells.getId(index), CodeGenerator.getCurBlockScope(scope),
															(CGBlockScope)leftCells.getObject(), CGCells.Type.ARGS == leftCells.getType()));

							}
							break;
						case STAT:
							if(1<size) threadSafe = false;
							
							if(leftIsFixed && !rightIsFixed) {
								if(0==index) {
									cont.append(new CGIAsm("sts", "_OS_STAT_POOL+" + leftCells.getId(index) + ",c0x00"));
								}
								else {
									cont.append(new CGIAsm("sts", "_OS_STAT_POOL+" + leftCells.getId(index) + "," + regsStr[index-1]));
								}
							}
							else if(!leftIsFixed && rightIsFixed) {
								if((index+1)<rightSize) {
									cont.append(new CGIAsm("sts", "_OS_STAT_POOL+" + leftCells.getId(index) + "," + regsStr[index+1]));
								}
								else {
									cont.append(new CGIAsm("sts", "_OS_STAT_POOL+" + leftCells.getId(index) + ",c0x00"));
								}
							}
							else {
								if(index<rightSize) {
									cont.append(new CGIAsm("sts", "_OS_STAT_POOL+" + leftCells.getId(index) + "," + regsStr[index]));
								}
								else {
									cont.append(new CGIAsm("sts", "_OS_STAT_POOL+" + leftCells.getId(index) + ",c0x00"));
								}
							}
							break;
						default:
							throw new CompileException(info + " unsupported yet");
					}
					break;
				case ARGS:
					//TODO проверить порядок - процедура вызова кладет в стек старший затем младший, т.е. процедура чтения должна поступать стандартно (LE)
					//Но! Учитываем что normalizeIRegConst меняет порядок для инструкций работы со стеком
					//Соответственно на этом уровне мы работаем со стеком как с обычной памятью, а процедура вызова должна клась в стек стандартно - сначала младший
				case STACK_FRAME:
					switch(leftCells.getType()) {
						case ACC:
							if(leftIsFixed && !rightIsFixed) {
								if(0!=index && index<rightSize) {
									cont.append(new CGIAsmIReg(	'y', true, "r"+accRegs[index], rightCells.getId(index-1), CodeGenerator.getCurBlockScope(scope),
														(CGBlockScope)rightCells.getObject(), CGCells.Type.ARGS == rightCells.getType()));
								}
								else {
									cont.append(new CGIAsmLd("ldi", "r"+accRegs[index], "0"));
								}
							}
							else if(!leftIsFixed && rightIsFixed) {
								if((index+1)<rightSize) {
									cont.append(new CGIAsmIReg(	'y', true, "r"+accRegs[index], rightCells.getId(index+1), CodeGenerator.getCurBlockScope(scope),
														(CGBlockScope)rightCells.getObject(), CGCells.Type.ARGS == rightCells.getType()));
								}
								else {
									cont.append(new CGIAsmLd("ldi", "r"+accRegs[index], "0"));
								}
							}
							else {
								if(index<rightSize) {
									cont.append(new CGIAsmIReg(	'y', true, "r"+accRegs[index], rightCells.getId(index), CodeGenerator.getCurBlockScope(scope),
														(CGBlockScope)rightCells.getObject(), CGCells.Type.ARGS == rightCells.getType()));
								}
								else {
									cont.append(new CGIAsmLd("ldi", "r"+accRegs[index], "0"));
								}
							}
							break;
						case ARRAY:
							if(1<size) threadSafe = false;
							
							if(leftIsFixed && !rightIsFixed) {
								if(0!=index && index<rightSize) {
									cont.append(new CGIAsmIReg(	'y', true, "r16", rightCells.getId(index-1), CodeGenerator.getCurBlockScope(scope),
														(CGBlockScope)rightCells.getObject(), CGCells.Type.ARGS == rightCells.getType()));
									cont.append(new CGIAsm("st", "x+,r16"));
								}
								else {
									cont.append(new CGIAsm("st", "x+,c0x00"));
								}
							}
							else if(!leftIsFixed && rightIsFixed) {
								if((index+1)<rightSize) {
									cont.append(new CGIAsmIReg(	'y', true, "r16", rightCells.getId(index+1), CodeGenerator.getCurBlockScope(scope),
														(CGBlockScope)rightCells.getObject(), CGCells.Type.ARGS == rightCells.getType()));
									cont.append(new CGIAsm("st", "x+,r16"));
								}
								else {
									cont.append(new CGIAsm("st", "x+,c0x00"));
								}
							}
							else {
								if(index<rightSize) {
									cont.append(new CGIAsmIReg(	'y', true, "r16", rightCells.getId(index), CodeGenerator.getCurBlockScope(scope),
														(CGBlockScope)rightCells.getObject(), CGCells.Type.ARGS == rightCells.getType()));
									cont.append(new CGIAsm("st", "x+,r16"));
								}
								else {
									cont.append(new CGIAsm("st", "x+,c0x00"));
								}
							}
							break;
						case HEAP:
							if(1<size) threadSafe = false;
							
							if(leftIsFixed && !rightIsFixed) {
								if(0!=index && index<rightSize) {
									cont.append(new CGIAsmIReg(	'y', true, "r16", rightCells.getId(index-1), CodeGenerator.getCurBlockScope(scope),
																(CGBlockScope)rightCells.getObject(), CGCells.Type.ARGS == rightCells.getType()));
									cont.append(new CGIAsmIReg('z', false, "r16", leftCells.getId(index)));
								}
								else {
									cont.append(new CGIAsmIReg('z', false, "c0x00", leftCells.getId(index)));
								}
							}
							else if(!leftIsFixed && rightIsFixed) {
								if((index+1)<rightSize) {
									cont.append(new CGIAsmIReg(	'y', true, "r16", rightCells.getId(index+1), CodeGenerator.getCurBlockScope(scope),
																(CGBlockScope)rightCells.getObject(), CGCells.Type.ARGS == rightCells.getType()));
									cont.append(new CGIAsmIReg('z', false, "r16", leftCells.getId(index)));
								}
								else {
									cont.append(new CGIAsmIReg('z', false, "c0x00", leftCells.getId(index)));
								}
							}
							else {
								if(index<rightSize) {
									cont.append(new CGIAsmIReg(	'y', true, "r16", rightCells.getId(index), CodeGenerator.getCurBlockScope(scope),
																(CGBlockScope)rightCells.getObject(), CGCells.Type.ARGS == rightCells.getType()));
									cont.append(new CGIAsmIReg('z', false, "r16", leftCells.getId(index)));
								}
								else {
									cont.append(new CGIAsmIReg('z', false, "c0x00", leftCells.getId(index)));
								}
							}
							break;
						case REG:
							boolean sameRegs = (2<=size && 28==leftCells.getId(0) && 29==leftCells.getId(1)); // Определяет запись в эту же регистровую пару
							
							if(leftIsFixed && !rightIsFixed) {
								if(0!=index && index<rightSize) {
									cont.append(new CGIAsmIReg(	'y', true, "r"+leftCells.getId(index), rightCells.getId(index-1), CodeGenerator.getCurBlockScope(scope),
																(CGBlockScope)rightCells.getObject(), CGCells.Type.ARGS == rightCells.getType()));
									if(sameRegs) {
										cont.append(new CGIAsmLd("ldi", "r"+leftCells.getId(0), "0"));
									}
								}
								else {
									if(!sameRegs) cont.append(new CGIAsmLd("ldi", "r"+leftCells.getId(index), "0"));
								}
							}
							else if(!leftIsFixed && rightIsFixed) {
								if((index+1)<rightSize) {
									cont.append(new CGIAsmIReg(	'y', true, "r"+leftCells.getId(index), rightCells.getId(index+1), CodeGenerator.getCurBlockScope(scope),
																(CGBlockScope)rightCells.getObject(), CGCells.Type.ARGS == rightCells.getType()));
								}
								else {
									cont.append(new CGIAsmLd("ldi", "r"+leftCells.getId(index), "0"));
								}
							}
							else {
								if(!sameRegs) {
									if(index<rightSize) {
										cont.append(new CGIAsmIReg(	'y', true, "r"+leftCells.getId(index), rightCells.getId(index), CodeGenerator.getCurBlockScope(scope),
																	(CGBlockScope)rightCells.getObject(), CGCells.Type.ARGS == rightCells.getType()));
									}
									else {
										cont.append(new CGIAsmLd("ldi", "r"+leftCells.getId(index), "0"));
									}
								}
								else if(0==index) {
									threadSafe = false;
									
									cont.append(new CGIAsmIReg(	'y', true, "j8b_atom", rightCells.getId(0), CodeGenerator.getCurBlockScope(scope),
																(CGBlockScope)rightCells.getObject(), CGCells.Type.ARGS == rightCells.getType()));
									cont.append(new CGIAsmIReg(	'y', true, "r"+leftCells.getId(1), rightCells.getId(1), CodeGenerator.getCurBlockScope(scope),
																(CGBlockScope)rightCells.getObject(), CGCells.Type.ARGS == rightCells.getType()));
									cont.append(new CGIAsmMv("mov", "r"+leftCells.getId(0), "j8b_atom"));
								}
							}
							break;
						case ARGS:
						case STACK_FRAME:
							if(leftIsFixed && !rightIsFixed) {
								if(0!=index && index<rightSize) {
									cont.append(new CGIAsmIReg(	'y', true, "j8b_atom", rightCells.getId(index-1), CodeGenerator.getCurBlockScope(scope),
																(CGBlockScope)rightCells.getObject(), CGCells.Type.ARGS == rightCells.getType()));
									cont.append(new CGIAsmIReg(	'y', false, "j8b_atom", leftCells.getId(index), CodeGenerator.getCurBlockScope(scope),
																(CGBlockScope)leftCells.getObject(), CGCells.Type.ARGS == leftCells.getType()));
									threadSafe=false;
								}
								else {
									cont.append(new CGIAsmIReg(	'y', false, "c0x00", leftCells.getId(index), CodeGenerator.getCurBlockScope(scope),
																(CGBlockScope)leftCells.getObject(), CGCells.Type.ARGS == leftCells.getType()));
								}
							}
							else if(!leftIsFixed && rightIsFixed) {
								if((index+1)<rightSize) {
									cont.append(new CGIAsmIReg(	'y', true, "j8b_atom", rightCells.getId(index+1), CodeGenerator.getCurBlockScope(scope),
																(CGBlockScope)rightCells.getObject(), CGCells.Type.ARGS == rightCells.getType()));
									cont.append(new CGIAsmIReg(	'y', false, "j8b_atom", leftCells.getId(index), CodeGenerator.getCurBlockScope(scope),
																(CGBlockScope)leftCells.getObject(), CGCells.Type.ARGS == leftCells.getType()));
									threadSafe=false;
								}
								else {
									cont.append(new CGIAsmIReg(	'y', false, "c0x00", leftCells.getId(index), CodeGenerator.getCurBlockScope(scope),
																(CGBlockScope)leftCells.getObject(), CGCells.Type.ARGS == leftCells.getType()));
								}
							}
							else {
								if(index<rightSize) {
									cont.append(new CGIAsmIReg(	'y', true, "j8b_atom", rightCells.getId(index), CodeGenerator.getCurBlockScope(scope),
																(CGBlockScope)rightCells.getObject(), CGCells.Type.ARGS == rightCells.getType()));
									cont.append(new CGIAsmIReg(	'y', false, "j8b_atom", leftCells.getId(index), CodeGenerator.getCurBlockScope(scope),
																(CGBlockScope)leftCells.getObject(), CGCells.Type.ARGS == leftCells.getType()));
									threadSafe=false;
								}
								else {
									cont.append(new CGIAsmIReg(	'y', false, "c0x00", leftCells.getId(index), CodeGenerator.getCurBlockScope(scope),
																(CGBlockScope)leftCells.getObject(), CGCells.Type.ARGS == leftCells.getType()));
								}
							}
							break;
						default:
							throw new CompileException(info + " unsupported yet");
					}
					break;
				case ARRAY:
					if(1<size) threadSafe = false;
					
					switch(leftCells.getType()) {
						case ACC:
							if(leftIsFixed && !rightIsFixed) {
								if(0!=index && index<rightSize) {
									cont.append(new CGIRAsm("ld", "r"+accRegs[index], "x+"));
								}
								else {
									cont.append(new CGIAsmLd("ldi", "r"+accRegs[index], "0"));
								}
							}
							else if(!leftIsFixed && rightIsFixed) {
								if(0==index) {
									cont.append(new CGIRAsm("adiw", "xl,1"));
								}
								if((index+1)<rightSize) {
									cont.append(new CGIRAsm("ld", "r"+accRegs[index], "x+"));
								}
								else {
									cont.append(new CGIAsmLd("ldi", "r"+accRegs[index], "0"));
								}
							}
							else {
								if(index<rightSize) {
									cont.append(new CGIAsmLd("ld", "r"+accRegs[index], "x+"));
								}
								else {
									cont.append(new CGIAsmLd("ldi", "r"+accRegs[index], "0"));
								}
							}
							break;
						case HEAP:
							if(leftIsFixed && !rightIsFixed) {
								if(0!=index && index<rightSize) {
									cont.append(new CGIAsmIReg('z', false, "x+", leftCells.getId(index)));
								}
								else {
									cont.append(new CGIAsmIReg('z', false, "C0x00", leftCells.getId(index)));
								}
							}
							else if(!leftIsFixed && rightIsFixed) {
								if(0==index) {
									cont.append(new CGIRAsm("adiw", "xl,1"));
								}
								if((index+1)<rightSize) {
									cont.append(new CGIAsmIReg('z', false, "x+", leftCells.getId(index)));
								}
								else {
									cont.append(new CGIAsmIReg('z', false, "C0x00", leftCells.getId(index)));
								}
							}
							else {
								if(index<rightSize) {
									cont.append(new CGIAsmIReg('z', false, "x+", leftCells.getId(index)));
								}
								else {
									cont.append(new CGIAsmIReg('z', false, "C0x00", leftCells.getId(index)));
								}
							}
							break;
						case REG:
							if(leftIsFixed && !rightIsFixed) {
								if(0!=index && index<rightSize) {
									cont.append(new CGIRAsm("ld", "r" + leftCells.getId(index), "x+"));
								}
								else {
									cont.append(new CGIAsmLd("ldi", "r"+leftCells.getId(index), "0"));
								}
							}
							else if(!leftIsFixed && rightIsFixed) {
								if(0==index) {
									cont.append(new CGIRAsm("adiw", "xl,1"));
								}
								if((index+1)<rightSize) {
									cont.append(new CGIRAsm("ld", "r" + leftCells.getId(index), "x+"));
								}
								else {
									cont.append(new CGIAsmLd("ldi", "r"+leftCells.getId(index), "0"));
								}
							}
							else {
								if(index<rightSize) {
									cont.append(new CGIRAsm("ld", "r" + leftCells.getId(index), "x+"));
								}
								else {
									cont.append(new CGIAsmLd("ldi", "r"+leftCells.getId(index), "0"));
								}
							}
							break;
						case ARGS:
						case STACK_FRAME:
							if(leftIsFixed && !rightIsFixed) {
								if(0!=index && index<rightSize) {
									cont.append(new CGIRAsm("ld", "r19", "x+"));
								}
								else {
									cont.append(new CGIAsmLd("ldi", "r19", "0"));
								}
							}
							else if(!leftIsFixed && rightIsFixed) {
								if(0==index) {
									cont.append(new CGIRAsm("adiw", "xl,1"));
								}
								if((index+1)<rightSize) {
									cont.append(new CGIRAsm("ld", "r19", "x+"));
								}
								else {
									cont.append(new CGIAsmLd("ldi", "r19", "0"));
								}
							}
							else {
								if(index<rightSize) {
									cont.append(new CGIRAsm("ld", "r19", "x+"));
								}
								else {
									cont.append(new CGIAsmLd("ldi", "r19", "0"));
								}
							}
							cont.append(new CGIAsmIReg(	'y', false, "r19", leftCells.getId(index), CodeGenerator.getCurBlockScope(scope),
														(CGBlockScope)leftCells.getObject(), CGCells.Type.ARGS == leftCells.getType()));
							break;
						default:
							throw new CompileException(info + " unsupported yet");
					}
					break;
				case CONST:
					switch(leftCells.getType()) {
						case ACC:
							if(leftIsFixed && !rightIsFixed) {
								if(0!=index && index<rightSize) {
									cont.append(new CGIAsmLd("ldi", "r"+accRegs[index], ""+rightCells.getId(index-1)));
								}
								else {
									cont.append(new CGIAsmLd("ldi", "r"+accRegs[index], "0"));
								}
							}
							else if(!leftIsFixed && rightIsFixed) {
								if((index+1)<rightSize) {
									cont.append(new CGIAsmLd("ldi", "r"+accRegs[index], ""+rightCells.getId(index+1)));
								}
								else {
									cont.append(new CGIAsmLd("ldi", "r"+accRegs[index], "0"));
								}
							}
							else {
								if(index<rightCells.getSize()) {
									cont.append(new CGIAsmLd("ldi", "r"+accRegs[index], ""+rightCells.getId(index)));
								}
								else {
									cont.append(new CGIAsmLd("ldi", "r"+accRegs[index], "0"));
								}
							}
							break;
						case ARRAY:
							if(1<size) threadSafe = false;
							
							if(leftIsFixed && !rightIsFixed) {
								if(0!=index && index<=rightCells.getSize()) {
									cont.append(new CGIAsm("st", "x+," + Utils.getConstReg(cont, (byte)19, rightCells.getId(index-1))));
								}
								else {
									cont.append(new CGIAsm("st", "x+,c0x00"));
								}
							}
							else if(!leftIsFixed && rightIsFixed) {
								if((index+1)<rightCells.getSize()) {
									cont.append(new CGIAsm("st", "x+," + Utils.getConstReg(cont, (byte)19, rightCells.getId(index+1))));
								}
								else {
									cont.append(new CGIAsm("st", "x+,c0x00"));
								}
							}
							else {
								if(index<rightCells.getSize()) {
									cont.append(new CGIAsm("st", "x+," + Utils.getConstReg(cont, (byte)19, rightCells.getId(index))));
								}
								else {
									cont.append(new CGIAsm("st", "x+,c0x00"));
								}
							}
							break;
						case HEAP:
							if(1<size) threadSafe = false;
							
							if(leftIsFixed && !rightIsFixed) {
								if(0!=index && index<=rightCells.getSize()) {
									String reg = Utils.getConstReg(cont, (byte)19, rightCells.getId(index-1));
									cont.append(new CGIAsmIReg('z', false, reg, leftCells.getId(index)));
								}
								else {
									cont.append(new CGIAsmIReg('z', false, "c0x00", leftCells.getId(index)));
								}
							}
							else if(!leftIsFixed && rightIsFixed) {
								if((index+1)<rightCells.getSize()) {
									String reg = Utils.getConstReg(cont, (byte)19, rightCells.getId(index+1));
									cont.append(new CGIAsmIReg('z', false, reg, leftCells.getId(index)));
								}
								else {
									cont.append(new CGIAsmIReg('z', false, "c0x00", leftCells.getId(index)));
								}
							}
							else {
								if(index<rightCells.getSize()) {
									String reg = Utils.getConstReg(cont, (byte)19, rightCells.getId(index));
									cont.append(new CGIAsmIReg('z', false, reg, leftCells.getId(index)));
								}
								else {
									cont.append(new CGIAsmIReg('z', false, "c0x00", leftCells.getId(index)));
								}
							}
							break;
						case HEAP_ALT:
							if(1<size) threadSafe = false;
							
							if(0==index && 0!=leftCells.getId(0)) {
								cont.append(new CGIAsm("subi", "r26,low(-" + leftCells.getId(0) + ")"));
								cont.append(new CGIAsm("sbci", "r27,high(-" + leftCells.getId(0) + ")"));
							}
							if(leftIsFixed && !rightIsFixed) {
								if(0!=index && index<=rightCells.getSize()) {
									cont.append(new CGIAsm("st", "x+," + Utils.getConstReg(cont, (byte)19, rightCells.getId(index-1))));
								}
								else {
									cont.append(new CGIAsm("st", "x+,c0x00"));
								}
							}
							else if(!leftIsFixed && rightIsFixed) {
								if((index+1)<rightCells.getSize()) {
									cont.append(new CGIAsm("st", "x+," + Utils.getConstReg(cont, (byte)19, rightCells.getId(index+1))));
								}
								else {
									cont.append(new CGIAsm("st", "x+,c0x00"));
								}
							}
							else {
								if(index<rightCells.getSize()) {
									cont.append(new CGIAsm("st", "x+," + Utils.getConstReg(cont, (byte)19, rightCells.getId(index))));
								}
								else {
									cont.append(new CGIAsm("st", "x+,c0x00"));
								}
							}
							break;
						case REG:
							if(leftIsFixed && !rightIsFixed) {
								if(0!=index && index<=rightCells.getSize()) {
									cont.append(new CGIAsmLd("ldi", "r"+leftCells.getId(index), Integer.toString(rightCells.getId(index-1))));
								}
								else {
									cont.append(new CGIAsmLd("ldi", "r"+leftCells.getId(index), "0"));
								}
							}
							else if(!leftIsFixed && rightIsFixed) {
								if((index+1)<rightCells.getSize()) {
									cont.append(new CGIAsmLd("ldi", "r"+leftCells.getId(index), Integer.toString(rightCells.getId(index+1))));
								}
								else {
									cont.append(new CGIAsmLd("ldi", "r"+leftCells.getId(index), "0"));
								}
							}
							else {
								if(index<rightCells.getSize()) {
									cont.append(new CGIAsmLd("ldi", "r"+leftCells.getId(index), Integer.toString(rightCells.getId(index))));
								}
								else {
									cont.append(new CGIAsmLd("ldi", "r"+leftCells.getId(index), "0"));
								}
							}
							break;
						case STAT:
							if(1<size) threadSafe = false;
							
							if(leftIsFixed && !rightIsFixed) {
								if(0!=index && index<=rightCells.getSize()) {
									String reg = Utils.getConstReg(cont, (byte)19, rightCells.getId(index-1));
									cont.append(new CGIAsm("sts", "_OS_STAT_POOL+" + leftCells.getId(index) + "," + reg));
								}
								else {
									cont.append(new CGIAsm("sts", "_OS_STAT_POOL+" + leftCells.getId(index) + ",C0x00"));
								}
							}
							else if(!leftIsFixed && rightIsFixed) {
								if((index+1)<rightCells.getSize()) {
									String reg = Utils.getConstReg(cont, (byte)19, rightCells.getId(index+1));
									cont.append(new CGIAsm("sts", "_OS_STAT_POOL+" + leftCells.getId(index) + "," + reg));
								}
								else {
									cont.append(new CGIAsm("sts", "_OS_STAT_POOL+" + leftCells.getId(index) + ",C0x00"));
								}
							}
							else {
								if(index<rightCells.getSize()) {
									String reg = Utils.getConstReg(cont, (byte)19, rightCells.getId(index));
									cont.append(new CGIAsm("sts", "_OS_STAT_POOL+" + leftCells.getId(index) + "," + reg));
								}
								else {
									cont.append(new CGIAsm("sts", "_OS_STAT_POOL+" + leftCells.getId(index) + ",C0x00"));
								}
							}
							break;
						case ARGS:
						case STACK_FRAME:
							if(leftIsFixed && !rightIsFixed) {
								if(0!=index && index<=rightCells.getSize()) {
									cont.append(new CGIAsmIReg(	'y', false, Utils.getConstReg(cont, (byte)19, rightCells.getId(index-1)), leftCells.getId(index),
																CodeGenerator.getCurBlockScope(scope), (CGBlockScope)leftCells.getObject(),
																CGCells.Type.ARGS == leftCells.getType()));
								}
								else {
									cont.append(new CGIAsmIReg(	'y', false, "c0x00", leftCells.getId(index),
																CodeGenerator.getCurBlockScope(scope), (CGBlockScope)leftCells.getObject(),
																CGCells.Type.ARGS == leftCells.getType()));
								}

							}
							else if(!leftIsFixed && rightIsFixed) {
								if((index+1)<rightCells.getSize()) {
									cont.append(new CGIAsmIReg(	'y', false, Utils.getConstReg(cont, (byte)19, rightCells.getId(index+1)), leftCells.getId(index),
																CodeGenerator.getCurBlockScope(scope), (CGBlockScope)leftCells.getObject(),
																CGCells.Type.ARGS == leftCells.getType()));
								}
								else {
									cont.append(new CGIAsmIReg(	'y', false, "c0x00", leftCells.getId(index),
																CodeGenerator.getCurBlockScope(scope), (CGBlockScope)leftCells.getObject(),
																CGCells.Type.ARGS == leftCells.getType()));
								}
							}
							else {
								if(index<rightCells.getSize()) {
									cont.append(new CGIAsmIReg(	'y', false, Utils.getConstReg(cont, (byte)19, rightCells.getId(index)), leftCells.getId(index),
																CodeGenerator.getCurBlockScope(scope), (CGBlockScope)leftCells.getObject(),
																CGCells.Type.ARGS == leftCells.getType()));
								}
								else {
									cont.append(new CGIAsmIReg(	'y', false, "c0x00", leftCells.getId(index),
																CodeGenerator.getCurBlockScope(scope), (CGBlockScope)leftCells.getObject(),
																CGCells.Type.ARGS == leftCells.getType()));
								}
							}
							break;
						default:
							throw new CompileException(info + " unsupported yet");
					}
					break;
				case FLAG:
					switch(leftCells.getType()) {
						case REG:
							cont.append(new CGIAsmLd("ldi", "r"+leftCells.getId(index), "0"));
							if((!leftIsFixed && 0==index) || (leftIsFixed && 1==index)) {
								cont.append(new CGIAsm("adc", "r"+leftCells.getId(index) + ",c0x00"));
							}
							break;
						case ARGS:
						case STACK_FRAME:
							cont.append(new CGIAsmMv("mov", "j8b_atom", "c0x00"));
							if((!leftIsFixed && 0==index) || (leftIsFixed && 1==index)) {
								cont.append(new CGIAsm("adc", "j8b_atom" + ",c0x00"));
							}
							cont.append(new CGIAsmIReg(	'y', false, "j8b_atom", leftCells.getId(index),
														CodeGenerator.getCurBlockScope(scope), (CGBlockScope)leftCells.getObject(),
														CGCells.Type.ARGS == leftCells.getType()));
							threadSafe=false;
							break;
						default:
							throw new CompileException(info + " unsupported yet");
					}
					break;
				case LABEL:
					if(leftIsFixed || 0x02!=leftSize) throw new CompileException("COMPILER ERROR: Can't set label addr to not short var");
					
					switch(leftCells.getType()) {
						case ACC:
							if(0==index) cont.append(new CGIAsmLdLabel("ldi", "r" + accRegs[0], "low(" + (String)rightCells.getObject() + "*2)"));
							if(1==index) cont.append(new CGIAsmLdLabel("ldi", "r" + accRegs[1], "high(" + (String)rightCells.getObject() + "*2)"));
							break;
						case REG:
							if(0==index) cont.append(new CGIAsmLdLabel("ldi", "r" + leftCells.getId(0), "low(" + (String)rightCells.getObject() + "*2)"));
							if(1==index) cont.append(new CGIAsmLdLabel("ldi", "r" + leftCells.getId(1), "high(" + (String)rightCells.getObject() + "*2)"));
							break;
						default:
							throw new CompileException(info + " unsupported yet");
					}
					break;
				case REG:
					switch(leftCells.getType()) {
						case ACC:
							if(leftIsFixed && !rightIsFixed) {
								if(0!=index && index<=rightSize) {
									cont.append(new CGIAsmMv("mov", "r"+accRegs[index], "r"+rightCells.getId(index-1)));
								}
								else {
									cont.append(new CGIAsmLd("ldi", "r"+accRegs[index], "0"));
								}
							}
							else if(!leftIsFixed && rightIsFixed) {
								if((index+1)<rightSize) {
									cont.append(new CGIAsmMv("mov", "r"+accRegs[index], "r"+rightCells.getId(index+1)));
								}
								else {
									cont.append(new CGIAsmLd("ldi", "r"+accRegs[index], "0"));
								}
							}
							else {
								if(index<rightSize) {
									cont.append(new CGIAsmMv("mov", "r"+accRegs[index], "r"+rightCells.getId(index)));
								}
								else {
									cont.append(new CGIAsmLd("ldi", "r"+accRegs[index], "0"));
								}
							}
							break;
						case ARGS:
						case STACK_FRAME:
							if(leftIsFixed && !rightIsFixed) {
								if(0!=index && index<rightSize) {
									cont.append(new CGIAsmIReg(	'y', false, "r"+rightCells.getId(index-1), leftCells.getId(index), CodeGenerator.getCurBlockScope(scope),
																(CGBlockScope)leftCells.getObject(), CGCells.Type.ARGS == leftCells.getType()));
								}
								else {
									cont.append(new CGIAsmIReg(	'y', false, "c0x00", leftCells.getId(index),
																CodeGenerator.getCurBlockScope(scope), (CGBlockScope)leftCells.getObject(),
																CGCells.Type.ARGS == leftCells.getType()));
								}
							}
							else if(!leftIsFixed && rightIsFixed) {
								if((index+1)<rightSize) {
									cont.append(new CGIAsmIReg(	'y', false, "r"+rightCells.getId(index+1), leftCells.getId(index), CodeGenerator.getCurBlockScope(scope),
																(CGBlockScope)leftCells.getObject(), CGCells.Type.ARGS == leftCells.getType()));
								}
								else {
									cont.append(new CGIAsmIReg(	'y', false, "c0x00", leftCells.getId(index),
																CodeGenerator.getCurBlockScope(scope), (CGBlockScope)leftCells.getObject(),
																CGCells.Type.ARGS == leftCells.getType()));
								}
							}
							else {
								if(index<rightSize) {
									cont.append(new CGIAsmIReg(	'y', false, "r"+rightCells.getId(index), leftCells.getId(index), CodeGenerator.getCurBlockScope(scope),
																(CGBlockScope)leftCells.getObject(), CGCells.Type.ARGS == leftCells.getType()));
								}
								else {
									cont.append(new CGIAsmIReg(	'y', false, "c0x00", leftCells.getId(index),
																CodeGenerator.getCurBlockScope(scope), (CGBlockScope)leftCells.getObject(),
																CGCells.Type.ARGS == leftCells.getType()));
								}
							}
							break;
						case ARRAY:
							if(1<size) threadSafe = false;
							
							if(leftIsFixed && !rightIsFixed) {
								if(0!=index && index<rightSize) {
									cont.append(new CGIRAsm("st", "x+", "r" + rightCells.getId(index-1)));
								}
								else {
									cont.append(new CGIRAsm("st", "x+,c0x00"));
								}
							}
							else if(!leftIsFixed && rightIsFixed) {
								if((index+1)<rightSize) {
									cont.append(new CGIRAsm("st", "x+", "r" + rightCells.getId(index+1)));
								}
								else {
									cont.append(new CGIRAsm("st", "x+,c0x00"));
								}
							}
							else {
								if(index<rightSize) {
									cont.append(new CGIRAsm("st", "x+", "r" + rightCells.getId(index)));
								}
								else {
									cont.append(new CGIRAsm("st", "x+,c0x00"));
								}
							}
							break;
						case HEAP:
							if(1<size) threadSafe = false;
							
							if(leftIsFixed && !rightIsFixed) {
								if(0!=index && index<=rightCells.getSize()) {
									cont.append(new CGIAsmIReg('z', false, "r"+rightCells.getId(index-1), leftCells.getId(index)));
								}
								else {
									cont.append(new CGIAsmIReg('z', false, "c0x00", leftCells.getId(index)));
								}
							}
							else if(!leftIsFixed && rightIsFixed) {
								if((index+1)<rightCells.getSize()) {
									cont.append(new CGIAsmIReg('z', false, "r"+rightCells.getId(index+1), leftCells.getId(index)));
								}
								else {
									cont.append(new CGIAsmIReg('z', false, "c0x00", leftCells.getId(index)));
								}
							}
							else {
								if(index<rightCells.getSize()) {
									cont.append(new CGIAsmIReg('z', false, "r"+rightCells.getId(index), leftCells.getId(index)));
								}
								else {
									cont.append(new CGIAsmIReg('z', false, "c0x00", leftCells.getId(index)));
								}
							}
							break;
						case REG:
							if(leftIsFixed && !rightIsFixed) {
								if(0!=index && index<rightSize) {
									cont.append(new CGIAsmMv("mov", "r"+leftCells.getId(index), "r"+rightCells.getId(index-1)));
								}
								else {
									cont.append(new CGIAsmLd("ldi", "r"+leftCells.getId(index), "0"));
								}
							}
							else if(!leftIsFixed && rightIsFixed) {
								if((index+1)<rightSize) {
									cont.append(new CGIAsmMv("mov", "r"+leftCells.getId(index), "r"+rightCells.getId(index+1)));
								}
								else {
									cont.append(new CGIAsmLd("ldi", "r"+leftCells.getId(index), "0"));
								}
							}
							else {
								if(index<rightSize) {
									cont.append(new CGIAsmMv("mov", "r"+leftCells.getId(index), "r"+rightCells.getId(index)));
								}
								else {
									cont.append(new CGIAsmLd("ldi", "r"+leftCells.getId(index), "0"));
								}
							}
							break;
						default:
							throw new CompileException(info + " unsupported yet");
					}
					break;
				case STACK:
					switch(leftCells.getType()) {
						case ARGS:
						case STACK_FRAME:
							if(leftIsFixed && !rightIsFixed) {
								if(0!=index && index<rightSize) {
									cont.append(new CGIRAsm("pop", "j8b_atom"));
									cont.append(new CGIAsmIReg(	'y', false, "j8b_atom", leftCells.getId(index), CodeGenerator.getCurBlockScope(scope),
																(CGBlockScope)leftCells.getObject(), CGCells.Type.ARGS == leftCells.getType()));
									threadSafe=false;
								}
								else {
									cont.append(new CGIAsmLd("ldi", "r"+rightCells.getId(index), "0"));
								}
							}
							else if(!leftIsFixed && rightIsFixed) {
								if(0==index) {
									cont.append(new CGIRAsm("pop", "j8b_atom"));
									threadSafe=false;
								}
								else if((index+1)<rightSize) {
									cont.append(new CGIRAsm("pop", "j8b_atom"));
									cont.append(new CGIAsmIReg(	'y', false, "j8b_atom", leftCells.getId(index), CodeGenerator.getCurBlockScope(scope),
																(CGBlockScope)leftCells.getObject(), CGCells.Type.ARGS == leftCells.getType()));
									threadSafe=false;
								}
								else {
									cont.append(new CGIAsmIReg(	'y', false, "c0x00", leftCells.getId(index), CodeGenerator.getCurBlockScope(scope),
																(CGBlockScope)leftCells.getObject(), CGCells.Type.ARGS == leftCells.getType()));
								}
							}
							else {
								if(index<rightSize) {
									cont.append(new CGIRAsm("pop", "j8b_atom"));
									cont.append(new CGIAsmIReg(	'y', false, "j8b_atom", leftCells.getId(index), CodeGenerator.getCurBlockScope(scope),
																(CGBlockScope)leftCells.getObject(), CGCells.Type.ARGS == leftCells.getType()));
									threadSafe=false;
								}
								else {
									cont.append(new CGIAsmIReg(	'y', false, "c0x00", leftCells.getId(index), CodeGenerator.getCurBlockScope(scope),
																(CGBlockScope)leftCells.getObject(), CGCells.Type.ARGS == leftCells.getType()));
								}
							}
							break;
						case ARRAY:
							if(1<size) threadSafe = false;
						
							if(leftIsFixed && !rightIsFixed) {
								if(0!=index && index<rightSize) {
									cont.append(new CGIRAsm("pop", "j8b_atom"));
									cont.append(new CGIRAsm("st", "x+", "j8b_atom"));
									threadSafe=false;
								}
								else {
									cont.append(new CGIRAsm("st", "x+,c0x00"));
								}
							}
							else if(!leftIsFixed && rightIsFixed) {
								if(0==index) {
									cont.append(new CGIRAsm("pop", "j8b_atom"));									
								}
								else if((index+1)<rightSize) {
									cont.append(new CGIRAsm("pop", "j8b_atom"));
									cont.append(new CGIRAsm("st", "x+", "j8b_atom"));
									threadSafe=false;
								}
								else {
									cont.append(new CGIRAsm("st", "x+,c0x00"));
								}
							}
							else {
								if(index<rightSize) {
									cont.append(new CGIRAsm("pop", "j8b_atom"));
									cont.append(new CGIRAsm("st", "x+", "j8b_atom"));
									threadSafe=false;
								}
								else {
									cont.append(new CGIRAsm("st", "x+,c0x00"));
								}
							}
							break;
						case HEAP:
							if(1<size) threadSafe = false;
							
							if(leftIsFixed && !rightIsFixed) {
								if(0!=index && index<rightSize) {
									cont.append(new CGIRAsm("pop", "j8b_atom"));
									cont.append(new CGIAsmIReg('z', false, "j8b_atom", leftCells.getId(index)));
									threadSafe=false;
								}
								else {
									cont.append(new CGIAsmMv("mov", "j8b_atom", "C0x00"));
									cont.append(new CGIAsmIReg('z', false, "j8b_atom", leftCells.getId(index)));
								}
							}
							else if(!leftIsFixed && rightIsFixed) {
								if(0==index) {
									cont.append(new CGIRAsm("pop", "j8b_atom"));
									threadSafe=false;
								}
								else if((index+1)<rightSize) {
									cont.append(new CGIRAsm("pop", "j8b_atom"));
									cont.append(new CGIAsmIReg('z', false, "j8b_atom", leftCells.getId(index)));
									threadSafe=false;
								}
								else {
									cont.append(new CGIAsmMv("mov", "j8b_atom", "C0x00"));
									cont.append(new CGIAsmIReg('z', false, "j8b_atom", leftCells.getId(index)));
									threadSafe=false;
								}
							}
							else {
								if(index<rightSize) {
									cont.append(new CGIRAsm("pop", "j8b_atom"));
									cont.append(new CGIAsmIReg('z', false, "j8b_atom", leftCells.getId(index)));
									threadSafe=false;
								}
								else {
									cont.append(new CGIAsmMv("mov", "j8b_atom", "C0x00"));
									cont.append(new CGIAsmIReg('z', false, "j8b_atom", leftCells.getId(index)));
									threadSafe=false;
								}
							}
							break;
						case REG:
							if(leftIsFixed && !rightIsFixed) {
								if(0!=index && index<rightSize) {
									cont.append(new CGIRAsm("pop", "j8b_atom"));
									cont.append(new CGIAsmMv("mov", "r"+leftCells.getId(index), "j8b_atom"));
									threadSafe=false;
								}
								else {
									cont.append(new CGIAsmLd("ldi", "r"+leftCells.getId(index), "0x00"));
								}
							}
							else if(!leftIsFixed && rightIsFixed) {
								if(0==index) {
									cont.append(new CGIRAsm("pop", "j8b_atom"));									
								}
								else if((index+1)<rightSize) {
									cont.append(new CGIRAsm("pop", "j8b_atom"));
									cont.append(new CGIAsmMv("mov", "r"+leftCells.getId(index), "j8b_atom"));
									threadSafe=false;
								}
								else {
									cont.append(new CGIAsmLd("ldi", "r"+leftCells.getId(index), "0x00"));
								}
							}
							else {
								if(index<rightSize) {
									cont.append(new CGIRAsm("pop", "j8b_atom"));
									cont.append(new CGIAsmMv("mov", "r"+leftCells.getId(index), "j8b_atom"));
									threadSafe=false;
								}
								else {
									cont.append(new CGIAsmLd("ldi", "r"+leftCells.getId(index), "0x00"));
								}
							}
							break;
						case STAT:
							if(1<size) threadSafe = false;
							
							if(leftIsFixed && !rightIsFixed) {
								if(0!=index && index<=rightCells.getSize()) {
									cont.append(new CGIRAsm("pop", "j8b_atom"));
									cont.append(new CGIAsm("sts", "_OS_STAT_POOL+" + leftCells.getId(index) + ",j8b_atom"));
								}
								else {
									cont.append(new CGIAsm("sts", "_OS_STAT_POOL+" + leftCells.getId(index) + ",c0x00"));
								}
							}
							else if(!leftIsFixed && rightIsFixed) {
								if(0==index) {
									cont.append(new CGIRAsm("pop", "j8b_atom"));
								}
								else if((index+1)<rightCells.getSize()) {
									cont.append(new CGIRAsm("pop", "j8b_atom"));
									cont.append(new CGIAsm("sts", "_OS_STAT_POOL+" + leftCells.getId(index) + ",j8b_atom"));
								}
								else {
									cont.append(new CGIAsm("sts", "_OS_STAT_POOL+" + leftCells.getId(index) + ",c0x00"));
								}
							}
							else {
								if(index<rightCells.getSize()) {
									cont.append(new CGIRAsm("pop", "j8b_atom"));
									cont.append(new CGIAsm("sts", "_OS_STAT_POOL+" + leftCells.getId(index) + ",j8b_atom"));
								}
								else {
									cont.append(new CGIAsm("sts", "_OS_STAT_POOL+" + leftCells.getId(index) + ",c0x00"));
								}
							}
							break;
						default:
							throw new CompileException(info + " unsupported yet");
					}
					break;
				case STAT:
					if(1<size) threadSafe = false;
					
					switch(leftCells.getType()) {
						case ACC:
							if(leftIsFixed && !rightIsFixed) {
								if(0!=index && index<rightSize) {
									cont.append(new CGIAsmLd("lds", "r"+accRegs[index], "_OS_STAT_POOL+" + rightCells.getId(index-1)));
								}
								else {
									cont.append(new CGIAsmLd("ldi", "r"+accRegs[index], "0"));
								}
							}
							else if(!leftIsFixed && rightIsFixed) {
								if((index+1)<rightSize) {
									cont.append(new CGIAsmLd("lds", "r"+accRegs[index], "_OS_STAT_POOL+" + rightCells.getId(index+1)));
								}
								else {
									cont.append(new CGIAsmLd("ldi", "r"+accRegs[index], "0"));
								}
							}
							else {
								if(index<rightSize) {
									cont.append(new CGIAsmLd("lds", "r"+accRegs[index], "_OS_STAT_POOL+" + rightCells.getId(index)));
								}
								else {
									cont.append(new CGIAsmLd("ldi", "r"+accRegs[index], "0"));
								}
							}
							break;
						case REG:
							if(leftIsFixed && !rightIsFixed) {
								if(0!=index && index<rightSize) {
									cont.append(new CGIAsmLd("lds", "r"+leftCells.getId(index), "_OS_STAT_POOL+" + rightCells.getId(index-1)));
								}
								else {
									cont.append(new CGIAsmLd("ldi", "r"+leftCells.getId(index), "0"));
								}
							}
							else if(!leftIsFixed && rightIsFixed) {
								if((index+1)<rightSize) {
									cont.append(new CGIAsmLd("lds", "r"+leftCells.getId(index), "_OS_STAT_POOL+" + rightCells.getId(index+1)));
								}
								else {
									cont.append(new CGIAsmLd("ldi", "r"+leftCells.getId(index), "0"));
								}
							}
							else {
								if(index<rightSize) {
									cont.append(new CGIAsmLd("lds", "r"+leftCells.getId(index), "_OS_STAT_POOL+" + rightCells.getId(index)));
								}
								else {
									cont.append(new CGIAsmLd("ldi", "r"+leftCells.getId(index), "0"));
								}
							}
							break;
						default:
							throw new CompileException(info + " unsupported yet");
					}
					break;
				case HEAP_ALT:
					if(1<size) threadSafe = false;
					
					switch(leftCells.getType()) {
						case ACC:
							if(0==index) {
								if(leftIsFixed && !rightIsFixed) {
									cont.append(new CGIAsm("subi", "r26,low(-" + (rightCells.getId(0)+1) + ")"));
									cont.append(new CGIAsm("sbci", "r27,high(-" + (rightCells.getId(0)+1) + ")"));
								}
								else if(0!=rightCells.getId(0)) {
									cont.append(new CGIAsm("subi", "r26,low(-" + rightCells.getId(0) + ")"));
									cont.append(new CGIAsm("sbci", "r27,high(-" + rightCells.getId(0) + ")"));
								}
							}
							if(leftIsFixed && !rightIsFixed) {
								if(0!=index && index<rightSize) {
									cont.append(new CGIRAsm("ld", "r"+accRegs[index], "x+"));
								}
								else {
									cont.append(new CGIAsmLd("ldi", "r"+accRegs[index], "0"));
								}
							}
							else if(!leftIsFixed && rightIsFixed) {
								if((index+1)<rightSize) {
									cont.append(new CGIRAsm("ld", "r"+accRegs[index], "x+"));
								}
								else {
									cont.append(new CGIAsmLd("ldi", "r"+accRegs[index], "0"));
								}
							}
							else {
								if(index<rightSize) {
									cont.append(new CGIRAsm("ld", "r"+accRegs[index], "x+"));
								}
								else {
									cont.append(new CGIAsmLd("ldi", "r"+accRegs[index], "0"));
								}
							}
							break;
						case REG:
							if(0==index) {
								if(leftIsFixed && !rightIsFixed) {
									cont.append(new CGIAsm("subi", "r26,low(-" + (rightCells.getId(0)+1) + ")"));
									cont.append(new CGIAsm("sbci", "r27,high(-" + (rightCells.getId(0)+1) + ")"));
								}
								else if(0!=rightCells.getId(0)) {
									cont.append(new CGIAsm("subi", "r26,low(-" + rightCells.getId(0) + ")"));
									cont.append(new CGIAsm("sbci", "r27,high(-" + rightCells.getId(0) + ")"));
								}
							}
							if(leftIsFixed && !rightIsFixed) {
								if(0!=index && index<rightSize) {
									cont.append(new CGIRAsm("ld", "r" + leftCells.getId(index), "x+"));
								}
								else {
									cont.append(new CGIAsmLd("ldi", "r"+leftCells.getId(index), "0"));
								}
							}
							else if(!leftIsFixed && rightIsFixed) {
								if((index+1)<rightSize) {
									cont.append(new CGIRAsm("ld", "r" + leftCells.getId(index), "x+"));
								}
								else {
									cont.append(new CGIAsmLd("ldi", "r"+leftCells.getId(index), "0"));
								}
							}
							else {
								if(index<rightSize) {
									cont.append(new CGIRAsm("ld", "r"+leftCells.getId(index), "x+"));
								}
								else {
									cont.append(new CGIAsmLd("ldi", "r"+leftCells.getId(index), "0"));
								}
							}
							break;
						default:
							throw new CompileException(info + " unsupported yet");
					}
					break;
				case HEAP:
					if(1<size) threadSafe = false;
					
					switch(leftCells.getType()) {
						case ACC:
							if(accRegs.length<=index) {
								continue;
							}
							
							if(leftIsFixed && !rightIsFixed) {
								if(0!=index && index<rightSize) {
									cont.append(new CGIAsmIReg('z', true, "r"+accRegs[index], rightCells.getId(index-1)));
								}
								else {
									cont.append(new CGIAsmLd("ldi", "r"+accRegs[index], "0"));
								}
							}
							else if(!leftIsFixed && rightIsFixed) {
								if((index+1)<rightSize) {
									cont.append(new CGIAsmIReg('z', true, "r"+accRegs[index], rightCells.getId(index+1)));
								}
								else {
									cont.append(new CGIAsmLd("ldi", "r"+accRegs[index], "0"));
								}
							}
							else {
								if(index<rightSize) {
									cont.append(new CGIAsmIReg('z', true, "r"+accRegs[index], rightCells.getId(index)));
								}
								else {
									cont.append(new CGIAsmLd("ldi", "r"+accRegs[index], "0"));
								}
							}
							break;
						case HEAP:
							if(leftIsFixed && !rightIsFixed) {
								if(0!=index && index<rightSize) {
									cont.append(new CGIAsmIReg('z', true, "j8b_atom", rightCells.getId(index-1)));
								}
								else {
									cont.append(new CGIAsmMv("mov", "j8b_atom", "C0x00"));
								}
							}
							else if(!leftIsFixed && rightIsFixed) {
								if((index+1)<rightSize) {
									cont.append(new CGIAsmIReg('z', true, "j8b_atom", rightCells.getId(index+1)));
								}
								else {
									cont.append(new CGIAsmMv("mov", "j8b_atom", "C0x00"));
								}
							}
							else {
								if(index<rightSize) {
									cont.append(new CGIAsmIReg('z', true, "j8b_atom", rightCells.getId(index)));
								}
								else {
									cont.append(new CGIAsmMv("mov", "j8b_atom", "C0x00"));
								}
							}
							cont.append(new CGIAsmIReg('z', false, "j8b_atom", leftCells.getId(index)));
							threadSafe=false;
						break;
						case REG:
							if(leftIsFixed && !rightIsFixed) {
								if(0!=index && index<rightSize) {
									cont.append(new CGIAsmIReg('z', true, "r"+leftCells.getId(index), rightCells.getId(index-1)));
								}
								else {
									cont.append(new CGIAsmLd("ldi", "r"+leftCells.getId(index), "0"));
								}
							}
							else if(!leftIsFixed && rightIsFixed) {
								if((index+1)<rightSize) {
									cont.append(new CGIAsmIReg('z', true, "r"+leftCells.getId(index), rightCells.getId(index+1)));
								}
								else {
									cont.append(new CGIAsmLd("ldi", "r"+leftCells.getId(index), "0"));
								}
							}
							else {
								if(index<rightSize) {
									cont.append(new CGIAsmIReg('z', true, "r"+leftCells.getId(index), rightCells.getId(index)));
								}
								else {
									cont.append(new CGIAsmLd("ldi", "r"+leftCells.getId(index), "0"));
								}
							}
							break;
							
						default:
							throw new CompileException(info + " unsupported yet");
					}
					break;
				default:
					throw new CompileException(info + " unsupported yet");
			}
		}
		
		if(rtosFeatures.contains(RtosFeature.OS_FT_MULTITHREADING) && !threadSafe) {
			dispLockCont.append(new CGIAsmCall(CALL_INSTR, RTOSLibs.DISPATCHER_PROCS.OS_DISPATCHER_LOCK.name().toLowerCase(), true));
			cont.append(new CGIAsmCall(CALL_INSTR, RTOSLibs.DISPATCHER_PROCS.OS_DISPATCHER_UNLOCK.name().toLowerCase(), true));
		}
		scope.append(cont);
	}

}
