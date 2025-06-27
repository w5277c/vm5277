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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import ru.vm5277.common.NativeBinding;
import ru.vm5277.common.Operator;
import static ru.vm5277.common.Operator.BIT_AND;
import static ru.vm5277.common.Operator.BIT_NOT;
import static ru.vm5277.common.Operator.BIT_OR;
import static ru.vm5277.common.Operator.BIT_XOR;
import static ru.vm5277.common.Operator.MINUS;
import static ru.vm5277.common.Operator.PLUS;
import static ru.vm5277.common.Operator.POST_DEC;
import static ru.vm5277.common.Operator.POST_INC;
import static ru.vm5277.common.Operator.PRE_DEC;
import static ru.vm5277.common.Operator.PRE_INC;
import ru.vm5277.common.compiler.Case;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.compiler.Operand;
import ru.vm5277.common.RTOSFeature;
import ru.vm5277.common.SystemParam;
import ru.vm5277.common.cg.CGActionHandler;
import ru.vm5277.common.cg.CGCell;
import ru.vm5277.common.cg.CGLocalScope;
import ru.vm5277.common.compiler.OperandType;

public class Generator extends CodeGenerator {
	private	final	static	String				VERSION		= "0.1";
	private	final	static	byte[]				usableRegs	= new byte[]{20,21,22,23,24,25,26,27,   19,18,17};
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
	
	@Override
	public void loadRegs(int size) {
		long value = accum.getValue();
		scope.asmAppend("ldi r16," + (value&0xff) + "\n"); value >>= 0x08;
		if(size>1) scope.asmAppend("ldi r17," + (value&0xff) + "\n"); value >>= 0x08;
		if(size>2) scope.asmAppend("ldi r18," + (value&0xff) + "\n"); value >>= 0x08;
		if(size>3) scope.asmAppend("ldi r19," + (value&0xff) + "\n"); value >>= 0x08;
	}
	
	@Override
	public void loadRegsConst(byte[] regs, long value) {
		for(byte reg : regs) {
			scope.asmAppend("ldi r" + reg + "," + (value&0xff) + "\n"); value >>= 0x08;
		}
	}

	@Override
	public void storeAcc(int resId) throws Exception { //Записываем acc в переменную
		System.out.println("CG:storeAcc, dstId:" + resId);

		CGLocalScope lScope = getLocal(resId);
		if(null == lScope) throw new Exception("CG:storeAcc, unknown resource id: " + resId);
		
		if(!lScope.isConstant()) {
			localStoreRegs(lScope.getStackOffset(), lScope.getSize());
		}
	}
	
	@Override
	public void loadAcc(int resId) throws Exception {
		System.out.println("CG:loadAcc, srcId:" + resId);

		CGLocalScope lScope = getLocal(resId);
		if(null == lScope) throw new Exception("CG:storeAcc, unknown resource id: " + resId);

		if(!lScope.isConstant()) {
			switch(lScope.getSize()) {
				case 1: localLoadRegs(lScope, new byte[]{16}); break;
				case 2: localLoadRegs(lScope, new byte[]{16,17}); break;
				case 3: localLoadRegs(lScope, new byte[]{16,17,18}); break;
				default: localLoadRegs(lScope, new byte[]{16,17,18,19}); break;
			}
		}
	}

	@Override
	public void localLoadRegs(CGLocalScope lScope, byte[] registers) throws Exception {
		if(registers.length != lScope.getSize()) throw new Exception("TODO Different size");
		scope.asmAppend(";local " + lScope.getName() + " -> accum\n");
		//Map<Byte, String> popRegs = new HashMap<>();
		LinkedList<Byte> popRegs = new LinkedList<>();
		
		for(int i=0; i<registers.length; i++) {
			CGCell cell = lScope.getCells()[i];
			byte srcReg = (byte)cell.getNum();
			byte dstReg = registers[i];

			switch(cell.getType()) {
				case REG:
					if(srcReg == dstReg) continue; // регистры совпались, ничего не делаем
					
					if(!popRegs.contains(dstReg)) {
						if(	i != (registers.length-1) && isEven(srcReg) && isEven(dstReg) &&
							(srcReg+1)==lScope.getCells()[i+1].getNum() && (dstReg+1)==registers[i+1]) { // подходит для инструкции movw
						
							Integer pos = lScope.getRegCellPos(dstReg);
							if(null==pos || pos<=i) {
								scope.asmAppend("movw r" + dstReg + ", r" + srcReg + "\n");
								i++;
								continue;
							}
						}
						
						Integer pos = lScope.getRegCellPos(dstReg);
						if(null != pos && pos > i) {
							scope.asmAppend("push r" + dstReg + "\n");
							popRegs.addFirst(srcReg);
						}
						scope.asmAppend("mov r" + dstReg + ", r" + srcReg + "\n");
					}
					break;
				case STACK:
					if(cell.getNum() <= (64-lScope.getSize())) {
						scope.asmAppend("ldd r" + dstReg + ", y+" + cell.getNum() + "\n");
					}
					else {
						scope.asmAppend("push_y\n");
						scope.asmAppend("subi yl,low(0x10000-" + cell.getNum() + ")\n");
						scope.asmAppend("subi yh,high(0x10000-" + cell.getNum() + ")\n");
						scope.asmAppend("ld r" + dstReg + ",y\n");
						scope.asmAppend("pop_y\n");
					}
			}
		}
		for(Byte reg : popRegs) {
			scope.asmAppend("pop r" + reg + "\n");
		}
	}

	@Override
	public void localStore(int resId, long value) throws Exception {
		CGLocalScope lScope = getLocal(resId);
		scope.asmAppend(";" + value + "->local " + lScope.getName() + "\n");
		boolean usedY = false;
		for (CGCell cell : lScope.getCells()) {
			switch(cell.getType()) {
				case REG:
					if(usedY) {scope.asmAppend("pop_y\n"); usedY = false;}
					scope.asmAppend("ldi r" + cell.getNum() + ", " + (value&0xff) + "\n");
					break;
				case STACK:
					if(lScope.getStackOffset() <= (64-lScope.getSize())) {
						if(usedY) {scope.asmAppend("pop_y\n"); usedY = false;}
						scope.getMethodScope().putUsedReg((byte)16);
						scope.asmAppend("ldi r16, " + (value&0xff) + "\n");
						scope.asmAppend("std y+" + cell.getNum() + ",r16\n");
					}
					else {
						scope.getMethodScope().putUsedReg((byte)16);
						if(!usedY) {scope.asmAppend("push_y\n"); scope.getMethodScope().putUsedRegs(new byte[]{28,29}); usedY = true;}
						scope.asmAppend("subi yl,low(0x10000-" + cell.getNum() + ")\n"); //TODO можно оптимизировать
						scope.asmAppend("sbci yh,high(0x10000-" + cell.getNum() + ")\n");//Если это условие выполняет более 1 раза, подряд 
						scope.asmAppend("ldi r16, " + (value&0xff) + "\n");							
						scope.asmAppend("st y,r16\n");
					}
			}
			value >>= 0x08;
		}
		if(usedY) {scope.asmAppend("pop_y\n"); usedY = false;}
	}


	@Override
	public void localStoreRegs(int stackOffset, int size) {
		scope.asmAppend(";accum -> local\n");
		if(stackOffset <= (64-size)) {
			scope.asmAppend("std y+" + stackOffset + ",r16\n");
			if(size>1) scope.asmAppend("std y+" + (stackOffset+1) + ",r17\n");
			if(size>2) scope.asmAppend("std y+" + (stackOffset+2) + ",r18\n");
			if(size>3) scope.asmAppend("std y+" + (stackOffset+3) + ",r19\n");
		}
		else {
			scope.asmAppend("subi yl,low(0x10000-" + stackOffset + ")\n");
			scope.asmAppend("sbci yh,high(0x10000-" + stackOffset + ")\n");
			scope.asmAppend("st y,r16\n");
			if(size>1) scope.asmAppend("std y+1,r17\n");
			if(size>2) scope.asmAppend("std y+2,r18\n");
			if(size>3) scope.asmAppend("std y+3,r19\n");
			scope.asmAppend("subi yl," + stackOffset + "\n");
			scope.asmAppend("sbci yh," + stackOffset + "\n");
		}
	}

	@Override
	public void constLoadRegs(String label, byte[] registers) {
		scope.asmAppend("ldi r" + registers[0] + ", low(" + label + "*2)\n");
		scope.asmAppend("ldi r" + registers[1] + ", high(" + label + "*2)\n");
	}


	@Override
	public void addAccum(int value) {
		if(0x00<value) {
			long tmp = 0x100000000l - value;
			scope.asmAppend("subi r16," + (tmp&0xff) + "\n"); tmp >>= 0x08;
			if(accum.getSize()>1) scope.asmAppend("subi r17," + (tmp&0xff) + "\n"); tmp >>= 0x08;
			if(accum.getSize()>2) scope.asmAppend("subi r18," + (tmp&0xff) + "\n"); tmp >>= 0x08;
			if(accum.getSize()>3) scope.asmAppend("subi r19," + (tmp&0xff) + "\n"); tmp >>= 0x08;
		}
	}

	public void add(byte[] regs, int value) { //TODO херня, должно быть как в localLoadRegs. Т.е. либо работаем с аккумулятором(если несколько действий, либо
		//полноценно единоразово, как в localLoadRegs. Нет проблемы, если переменная в регистрах, а если затронута память? Тогда лучше всего загрузить в аккум и не выебываться
		//НЕТ, при единой опрации всеравно экономнее разбирать побайтно, а не загружать в аккум. Но тогда нужно будет это сделать для каждой операции!!!
		//Хм, возможно получится взять один механизм а ему просто подставлять строку в определенном формате
		if(0x00<value) {
			long tmp = 0x100000000l - value;
			scope.asmAppend("subi r" + regs[0] + "," + (tmp&0xff) + "\n"); tmp >>= 0x08;
			if(regs.length>1) scope.asmAppend("subi r" + regs[1] + "," + (tmp&0xff) + "\n"); tmp >>= 0x08;
			if(regs.length>2) scope.asmAppend("subi r" + regs[2] + "," + (tmp&0xff) + "\n"); tmp >>= 0x08;
			if(regs.length>3) scope.asmAppend("subi r" + regs[3] + "," + (tmp&0xff) + "\n");
		}
	}


	@Override
	public void subAccum(int value) {
		if(0x00<value) {
			scope.asmAppend("subi r16," + (value&0xff) + ")n"); value >>= 0x08;
			if(accum.getSize()>1) scope.asmAppend("subi r17," + (value&0xff) + "\n"); value >>= 0x08;
			if(accum.getSize()>2) scope.asmAppend("subi r18," + (value&0xff) + "\n"); value >>= 0x08;
			if(accum.getSize()>3) scope.asmAppend("subi r19," + (value&0xff) + "\n");
		}
	}
	
	@Override
	public void emitUnary(Operator op, Integer resId) throws Exception {
		if(null == resId) throw new Exception("CG: emitUrany, unsupported accum mode");
		CGLocalScope lScope = getLocal(resId);
		
		switch(op) {
			case PLUS:
				break;
			case MINUS:
				localActionAsm(lScope, (int sn, String reg) -> ("com " + reg));
				localActionAsm(lScope, (int sn, String reg) -> (0 == sn ? "sub " + reg + ", C0x01" : "sbc " + reg + ", C0x00"));
				break;
			case BIT_NOT:
				localActionAsm(lScope, (int sn, String reg) -> ("com " + reg));
			case PRE_INC:
			case POST_INC:
				localActionAsm(lScope, (int sn, String reg) -> (0 == sn ? "add " + reg + ", C0x01" : "adc " + reg + ", C0x00"));
				break;
			case PRE_DEC:
			case POST_DEC:
				localActionAsm(lScope, (int sn, String reg) -> (0 == sn ? "sub " + reg + ", C0x01" : "sbc " + reg + ", C0x00"));
				break;
			default:
				throw new Exception("CG:emitUnary: unsupported operator: " + op);
		}
	}
		
		
	// Работаем с аккумулятором!
	@Override
	public void localAction(Operator op, int resId, Long k) throws Exception {
		CGLocalScope lScope = getLocal(resId);
		scope.asmAppend(";accum " + op + " local " + lScope.getName() + " -> accum\n");

		System.out.println("CG:localAction, op:" + op);
		switch(op) {
			case PLUS:
				localActionAsm(lScope, (int sn, String reg) -> (0 == sn ? "add " : "adc ") + getRightOp(sn, null) + ", " + reg);
				break;
			case MINUS:
				localActionAsm(lScope, (int sn, String reg) -> (0 == sn ? "sub " : "sbc ") + getRightOp(sn, null) + ", " + reg);
				break;
			case BIT_AND:
				localActionAsm(lScope, (int sn, String reg) -> "and " + getRightOp(sn, null) + ", " + reg);
				break;
			case BIT_OR:
				localActionAsm(lScope, (int sn, String reg) -> "or " + getRightOp(sn, null) + ", " + reg);
				break;
			case BIT_XOR:
				localActionAsm(lScope, (int sn, String reg) -> "eor " + getRightOp(sn, null) + ", " + reg);
				break;
			default:
				throw new Exception("CG:localAction: unsupported operator: " + op);
		}
	}

	String getRightOp(int sn, Long constant) { //возвращает либо байт константы, либо регистр аккумуляора, если constant is null
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
	
	public void localActionAsm(CGLocalScope lScope, CGActionHandler handler) throws Exception {
		boolean usedY = false;
		for (int i=0; i<lScope.getSize(); i++) {
			CGCell cell = lScope.getCells()[i];
			switch(cell.getType()) {
				case REG:
					if(usedY) {scope.asmAppend("pop_y\n"); usedY = false;}
					scope.asmAppend(handler.action(i, "r" + cell.getNum()) + "\n");
					break;
				case STACK:
					if(lScope.getStackOffset() <= (64-lScope.getSize())) {
						if(usedY) {scope.asmAppend("pop_y\n"); usedY = false;}
						scope.getMethodScope().putUsedReg((byte)16);
						scope.asmAppend(handler.action(i, "r16") + "\n");
						scope.asmAppend("std y+" + cell.getNum() + ",r16\n");
					}
					else {
						scope.getMethodScope().putUsedReg((byte)16);
						if(!usedY) {scope.asmAppend("push_y\n"); usedY = true;}
						scope.asmAppend("subi yl,low(0x10000-" + cell.getNum() + ")\n"); //TODO можно оптимизировать
						scope.asmAppend("sbci yh,high(0x10000-" + cell.getNum() + ")\n");//Если это условие выполняет более 1 раза, подряд 
						scope.asmAppend(handler.action(i, "r16") + "\n");
						scope.asmAppend("st y,r16\n");
					}
			}
		}
		if(usedY) {scope.asmAppend("pop_y\n"); usedY = false;}
	};
//--------------------	
	
	@Override
	public void eNew(int typeId, long[] parameters, boolean canThrow) {
		System.out.println("CG:eNew, " + typeId +", params:" + Arrays.toString(parameters));
	}

	@Override
	public void eFree(Operand op) {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}


	@Override
	public void invokeMethod(String methodQName, int id, int typeId, Operand[] operands) {
		System.out.println("CG:invokeMethod " + methodQName + ", id:" + id + ", typeId:" + typeId + ", reeIds:" + Arrays.toString(operands));
	}
	
	@Override
	public void invokeNative(String methodQName, int typeId, Operand[] parameters) throws Exception {
		System.out.println("CG:invokeNative " + methodQName + ", typeId:" + typeId + ", resIds" + Arrays.toString(parameters));
		
		if(methodQName.equals("System.setParam [byte, byte]")) {
			SystemParam sp = SystemParam.values()[(int)getNum(parameters[0x00])];
			switch(sp) {
				case CORE_FREQ:
					params.put(sp, (int)getNum(parameters[0x01]));
					break;
				case STDOUT_PORT:
					params.put(sp, (int)getNum(parameters[0x01]));
					break;
				case SHOW_WELCOME:
					if(0x00 == (int)getNum(parameters[0x01])) {
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
			//TODO не корректная проверка, так как проверяем не на число, а на операнд, который может хранить не толкьо число
//			if(	null == parameters && null != regIds || null != parameters && null == regIds ||
//				(null != parameters && null != regIds && parameters.length != regIds.length)) {
//				throw new Exception("CG:InvokeNative, invalid parameter count for method " + methodQName + ", expected " + regIds.length + ", got " +
//									parameters.length);
//			}

			includes.add(nb.getRTOSFilePath());

			if(null != nb.getRTOSFeatures()) {
				for(RTOSFeature feature : nb.getRTOSFeatures()) {
					RTOSFeatures.add(feature);
				}
			}

			StringBuilder sb = new StringBuilder();
			if(null != parameters) {
				for(int i=0; i<parameters.length; i++) {
					byte[] regs = regIds[i];
					if(0 == regs.length || 4 < regs.length) throw new Exception("CG:Invalid parameters for invoke method " + methodQName);

					scope.getMethodScope().putUsedRegs(regs);
					
					long value = 0;
					Operand op = parameters[i];
					if(OperandType.LOCAL_RESID == op.getOperandType()) {
						CGLocalScope lScope = getLocal((int)op.getValue());
						if(null != lScope) {
							if(!lScope.isConstant()) {
								if(regs.length != lScope.getSize()) {
									throw new Exception("CG:InvokeNative: params qnt not equlas regs qnt in method: " + methodQName);
								}
								localLoadRegs(lScope, regs);
							}
							else {
								constLoadRegs(lScope.getDataSymbol().getLabel(), regs);
							}
						}
						else {
							int t=4454;
						}
					}
					else {
						value = getNum(parameters[i]);
						loadRegsConst(regs, value);
					}
//							break;
/*						case RESOURCE_ID:
							int resId = (int)op.getValue();
							String strValue = flashData.get(resId).getLabel();
							if(0x02 != registers.length) throw new Exception("CG:InvokeNative: expected 16b reg. pair for method " + methodQName);
							byte regL = registers[0x00];
							byte regH = registers[0x01];
							if (regL<16 || regL>31) throw new Exception("CG:InvokeNative: invalid first register R" + regL + " for method " + methodQName);
							if (regH<16 || regH>31) throw new Exception("CG:InvokeNative: invalid second register R" + regH + " for method " + methodQName);
							if(regL == regH) throw new Exception("CG:InvokeNative: registers is equlas, R" + regL + " for method " + methodQName);
							scope.getMethodScope().putReg(regL);
							scope.getMethodScope().putReg(regH);
							sb.append("ldi r").append(regL).append(",").append("low(").append(strValue).append(")\n");
							sb.append("ldi r").append(regH).append(",").append("high(").append(strValue).append(")\n");
							break;
						default:
							throw new Exception("CG:InvokeNative: unsupported operand type: " + op.getOperandType());
					}*/
					
				}
			}
			sb.append("call ").append(nb.getRTOSFunction()).append("\n");
			scope.asmAppend(sb.toString());
		}
	}
	
	
/*	@Override
	public void setValueByIndex(Operand op, int size, List<Byte> tempRegs) throws Exception {
		String params[] = op.getParams(size, flashData);
		for(int i=0; i<size; i++) {
			scope.asmAppend("ldi temp_l, " + params[i] + "\n");
			scope.asmAppend("lds y+" + i + ", temp_l\n");
		}
	}
*/	
	
	@Override
	public boolean emitInstanceof(long resId, int typeId) {
		System.out.println("CG:emitInstanceOf, op:" + resId + ", typeId:" + typeId);
		return false;//TODO new Operand(VarType.BOOL, OperandType.LITERAL, true);
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
	public List<Byte> buildRegsPool() {
		List<Byte> result = new ArrayList<>();
		for(byte b : usableRegs) {result.add(b);}
		return result;
	}
	
	
	@Override
	public String getVersion() {
		return VERSION;
	}
}
