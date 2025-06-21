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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import ru.vm5277.common.NativeBinding;
import ru.vm5277.common.compiler.Case;
import ru.vm5277.common.compiler.CodeGenerator;
import ru.vm5277.common.compiler.Operand;
import ru.vm5277.common.compiler.OperandType;
import ru.vm5277.common.Operator;
import ru.vm5277.common.RTOSFeature;
import ru.vm5277.common.SystemParam;
import ru.vm5277.common.compiler.VarType;

public class Generator extends CodeGenerator {
	private	final	static	String				VERSION		= "0.1";
	private	final	static	boolean				def_sph		= true; // TODO нерератор avr должен знать характеристики МК(чтобы не писать асм код с .ifdef)
	private	final	static	boolean				def_call	= true; // TODO нерератор avr должен знать характеристики МК(чтобы не писать асм код с .ifdef)
	
	public Generator(String genName, Map<String, NativeBinding> nbMap, Map<SystemParam, Object> params) {
		super(genName, nbMap, params);
		// Собираем RTOS
	}
	
//---------- геттеры ассемблерных инструкций ----------
	@Override
	public String stackAllocAsm(int size) {
		StringBuilder sb = new StringBuilder();
		if(def_sph) {
			sb.append("push_y\n");
			sb.append("ldi_y ").append(size).append("\n");
		}
		else {
			sb.append("push yl\n");
			sb.append("ldi yl,").append(size).append("\n");
		}
		sb.append(def_call ? "call" : "rcall").append(" stk_alloc\n"); //mcall макрос
		return sb.toString();
	}
	@Override
	public String stackFreeAsm() {
		StringBuilder sb = new StringBuilder();
		sb.append(def_call ? "call" : "rcall").append(" stk_free\n"); //mcall макрос
		return sb.toString();
	}
	@Override
	public String pushRegAsm(int reg) {return "push r"+reg;}
	@Override
	public String popRegAsm(int reg) {return "pop r"+reg;}

	
//--------------------	
	
	@Override
	public void eNew(int typeId, Operand[] parameters, boolean canThrow) {
		System.out.println("CG:eNew, " + typeId +", params:" + Arrays.toString(parameters));
	}

	@Override
	public void eFree(Operand op) {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}


	@Override
	public void invokeMethod(int id, int typeId, Operand[] args) {
		System.out.println("CG:invokeMethod, id:" + id + ", typeId:" + typeId + ", args:" + Arrays.toString(args));
	}
	
	@Override
	public void invokeNative(String methodQName, int typeId, Operand[] parameters) throws Exception {
		System.out.println("CG:invokeNative " + methodQName + ", typeId:" + typeId + ", params" + Arrays.toString(parameters));
		
		if(methodQName.equals("System.setParam [byte, byte]")) {
			SystemParam sp = SystemParam.values()[(int)parameters[0x00].getValue()];
			switch(sp) {
				case CORE_FREQ:
					params.put(sp, (int)parameters[0x01].getValue());
					break;
				case STDOUT_PORT:
					params.put(sp, (int)parameters[0x01].getValue());
					break;
				case SHOW_WELCOME:
					if(0x00 == (int)parameters[0x01].getValue()) {
						RTOSFeatures.remove(RTOSFeature.OS_FT_WELCOME);
					}
					else {
						RTOSFeatures.add(RTOSFeature.OS_FT_WELCOME);
					}
					break;
			}
		}
		else {
			NativeBinding nb = nbMap.get(methodQName);
			if(null == nb) {
				throw new Exception("CG:InvokeNative, not found native binding for method: " + methodQName);
			}
			byte[][] regIds = nb.getRegs();
			if(	null == parameters && null != regIds || null != parameters && null == regIds ||
				(null != parameters && null != regIds && parameters.length != regIds.length)) {
				throw new Exception("CG:InvokeNative, invalid parameter count for method " + methodQName + ", expected " + regIds.length + ", got " +
									parameters.length);
			}

			includes.add(nb.getRTOSFilePath());

			if(null != nb.getRTOSFeatures()) {
				for(RTOSFeature feature : nb.getRTOSFeatures()) {
					RTOSFeatures.add(feature);
				}
			}

			StringBuilder sb = new StringBuilder();
			if(null != parameters) {
				for(int i=0; i<parameters.length; i++) {
					byte[] registers = regIds[i];
					if(0 == registers.length || 4 < registers.length) throw new Exception("CG:Invalid parameters for invoke method " + methodQName);

					Operand op = parameters[i];
					if(OperandType.LITERAL == op.getOperandType()) {
						long value = 0;
						if(op.getValue() instanceof Number) {
							value = ((Number)op.getValue()).longValue();
						}
						else if(op.getValue() instanceof Character) {
							value = ((Character)op.getValue());
						}
						else throw new Exception("CG:InvokeNative: literal must be a number for method " + methodQName);
						
						for(int j=0; j<registers.length; j++) {
							int reg = registers[j] & 0xff;
							if (reg<16 || reg>31) throw new Exception("CG:InvokeNative: invalid register R" + reg + " for method " + methodQName);
							scope.getMethodScope().putReg(reg);
							sb.append("ldi r").append(reg).append(",").append(value&0xff).append("\n");
							value >>>= 8;
						}
					}
					else if(OperandType.ADDR == op.getOperandType()) {
						int resId = (int)op.getValue();
						String value = flashData.get(resId).getLabel();
						if(0x02 != registers.length) throw new Exception("CG:InvokeNative: expected 16b reg. pair for method " + methodQName);
						int regL = registers[0x00] & 0xff;
						int regH = registers[0x01] & 0xff;
						if (regL<16 || regL>31) throw new Exception("CG:InvokeNative: invalid first register R" + regL + " for method " + methodQName);
						if (regH<16 || regH>31) throw new Exception("CG:InvokeNative: invalid second register R" + regH + " for method " + methodQName);
						if(regL == regH) throw new Exception("CG:InvokeNative: registers is equlas, R" + regL + " for method " + methodQName);
						scope.getMethodScope().putReg(regL);
						scope.getMethodScope().putReg(regH);
						sb.append("ldi r").append(regL).append(",").append("low(").append(value).append(")\n");
						sb.append("ldi r").append(regH).append(",").append("high(").append(value).append(")\n");
					}
					else throw new Exception("CG:InvokeNative: unsupported operand type: " + op.getOperandType());
					
				}
			}
			sb.append("call ").append(nb.getRTOSFunction()).append("\n");
			scope.asmAppend(sb.toString());
		}
	}
	
	@Override
	public Operand emitInstanceof(Operand op, int typeId) {
		System.out.println("CG:emitInstanceOf, op:" + op + ", typeId:" + typeId);
		return new Operand(VarType.BOOL.getId(), OperandType.LITERAL, true);
	}
	
	@Override
	public void emitUnary(Operator op) {
		System.out.println("CG:emitUnary, op:" + op);
	}

	@Override
	public void eIf(int conditionBlockId, int thenBlockId, Integer elseBlockId) {
		System.out.println("CG:if, condBlockId:" + conditionBlockId + ", thenBlockId:" + thenBlockId + ", elseBlockId:" + elseBlockId);
	}

	@Override
	public void eTry(int blockId, List<Case> cases, Integer defaultBlockId) {
		System.out.println("CG:try, blockId:" + blockId + ", casesBlocks:" + cases + ", defaultBlockId:" + defaultBlockId);
	}
	
	@Override
	public void eWhile(int conditionBlockId, int bodyBlockId) {
		System.out.println("CG:while, condBlockId:" + conditionBlockId + ", bodyBlockId:" + bodyBlockId);
	}
	
	@Override
	public void eReturn() {
		System.out.println("CG:return");
	}
	
	@Override
	public void eThrow() {
		System.out.println("CG:throw");
	}
	
	@Override
	public int getRefSize() {
		return 0x02;
		//TODO для МК с памятью <= 256 нужно возвращать 0x01;
	}
	
	
	
	@Override
	public String getVersion() {
		return VERSION;
	}
}
