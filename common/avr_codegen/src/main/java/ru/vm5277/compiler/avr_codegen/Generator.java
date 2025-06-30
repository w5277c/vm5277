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
import ru.vm5277.common.cg.items.CGIAsm;
import ru.vm5277.common.cg.items.CGIContainer;
import ru.vm5277.common.cg.items.CGIText;
import ru.vm5277.common.cg.scopes.CGBlockScope;
import ru.vm5277.common.cg.CGCell;
import ru.vm5277.common.cg.scopes.CGLocalScope;
import ru.vm5277.common.compiler.OperandType;
import ru.vm5277.common.cg.items.CGItem;
import ru.vm5277.common.cg.scopes.CGLabelScope;

public class Generator extends CodeGenerator {
	private	final	static	String				VERSION		= "0.1";
	private	final	static	byte[]				usableRegs	= new byte[]{20,21,22,23,24,25,26,27,   19,18,17};
	private	final	static	boolean				def_sph		= true; // TODO нерератор avr должен знать характеристики МК(чтобы не писать асм код с .ifdef)
	private	final	static	boolean				def_call	= true; // TODO нерератор avr должен знать характеристики МК(чтобы не писать асм код с .ifdef)
	
	public Generator(String platform, Map<String, NativeBinding> nbMap, Map<SystemParam, Object> params) {
		super(platform, nbMap, params);
		// Собираем RTOS
	}
	
//---------- геттеры ассемблерных инструкций ----------
	@Override
	public CGItem stackAllocAsm(int size) {
		CGIContainer cgb = new CGIContainer();
		cgb.append(new CGIAsm("push yl"));
		cgb.append(new CGIAsm("ldi yl,low(" + size + ")"));
		if(def_sph) {
			cgb.append(new CGIAsm("push yh"));
			cgb.append(new CGIAsm("ldi yh,high(" + size + ")"));
		}
		
		if(def_call) {
			cgb.append(new CGIAsm("call stk_alloc", 2));
		}
		else {
			cgb.append(new CGIAsm("rcall stk_alloc"));
		}
		return cgb;
	}
	@Override
	public CGItem stackFreeAsm() {
		if(def_call) {
			return new CGIAsm("call stk_free", 2);
		}
		return new CGIAsm("rcall stk_free");
	}
	@Override
	public CGIAsm pushRegAsm(int reg) {return new CGIAsm("push r"+reg+"");}
	@Override
	public CGIAsm popRegAsm(int reg) {return new CGIAsm("pop r"+reg+"");}
	
	@Override
	public void loadRegs(int size) {
		long value = accum.getValue();
		scope.append(new CGIAsm("ldi r16," + (value&0xff))); value >>= 0x08;
		if(size>1) scope.append(new CGIAsm("ldi r17," + (value&0xff))); value >>= 0x08;
		if(size>2) scope.append(new CGIAsm("ldi r18," + (value&0xff))); value >>= 0x08;
		if(size>3) scope.append(new CGIAsm("ldi r19," + (value&0xff))); value >>= 0x08;
	}
	
	@Override
	public void loadRegsConst(byte[] regs, long value) {
		for(byte reg : regs) {
			scope.append(new CGIAsm("ldi r" + reg + "," + (value&0xff))); value >>= 0x08;
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
		accum.setSize(lScope.getSize());
		scope.append(new CGIText(";local " + lScope.getName() + " -> accum"));
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
								scope.append(new CGIAsm("movw r" + dstReg + ", r" + srcReg));
								i++;
								continue;
							}
						}
						
						Integer pos = lScope.getRegCellPos(dstReg);
						if(null != pos && pos > i) {
							scope.append(new CGIAsm("push r" + dstReg));
							popRegs.addFirst(srcReg);
						}
						scope.append(new CGIAsm("mov r" + dstReg + ", r" + srcReg));
					}
					break;
				case STACK:
					if(cell.getNum() <= (64-lScope.getSize())) {
						scope.append(new CGIAsm("ldd r" + dstReg + ", y+" + cell.getNum()));
					}
					else {
						scope.append(new CGIAsm("push yl"));
						scope.append(new CGIAsm("push yh"));
						scope.append(new CGIAsm("subi yl,low(0x10000-" + cell.getNum() + ")"));
						scope.append(new CGIAsm("subi yh,high(0x10000-" + cell.getNum() + ")"));
						scope.append(new CGIAsm("ld r" + dstReg + ",y"));
						scope.append(new CGIAsm("pop yh"));
						scope.append(new CGIAsm("pop yl"));
					}
			}
		}
		for(Byte reg : popRegs) {
			scope.append(new CGIAsm("pop r" + reg));
		}
	}

	@Override
	public void localStore(int resId, long value) throws Exception {
		CGLocalScope lScope = getLocal(resId);
		scope.append(new CGIText(";" + value + "->local " + lScope.getName()));
		boolean usedY = false;
		for (CGCell cell : lScope.getCells()) {
			switch(cell.getType()) {
				case REG:
					if(usedY) {
						scope.append(new CGIAsm("pop yh"));
						scope.append(new CGIAsm("pop yl"));
						usedY = false;
					}
					scope.append(new CGIAsm("ldi r" + cell.getNum() + ", " + (value&0xff)));
					break;
				case STACK:
					if(lScope.getStackOffset() <= (64-lScope.getSize())) {
						if(usedY) {
							scope.append(new CGIAsm("pop yh"));
							scope.append(new CGIAsm("pop yl"));
							usedY = false;
						}
						scope.getBlockScope().putUsedReg((byte)16);
						scope.append(new CGIAsm("ldi r16, " + (value&0xff)));
						scope.append(new CGIAsm("std y+" + cell.getNum() + ",r16"));
					}
					else {
						scope.getBlockScope().putUsedReg((byte)16);
						if(!usedY) {
							scope.append(new CGIAsm("push yl"));
							scope.append(new CGIAsm("push yh"));
							scope.getBlockScope().putUsedRegs(new byte[]{28,29});
							usedY = true;
						}
						scope.append(new CGIAsm("subi yl,low(0x10000-" + cell.getNum() + ")")); //TODO можно оптимизировать
						scope.append(new CGIAsm("sbci yh,high(0x10000-" + cell.getNum() + ")"));//Если это условие выполняет более 1 раза, подряд 
						scope.append(new CGIAsm("ldi r16, " + (value&0xff)));
						scope.append(new CGIAsm("st y,r16"));
					}
			}
			value >>= 0x08;
		}
		if(usedY) {
			scope.append(new CGIAsm("pop yh"));
			scope.append(new CGIAsm("pop yl"));
		}
	}

	@Override
	public void localStoreRegs(int stackOffset, int size) {
		scope.append(new CGIText(";accum -> local"));
		if(stackOffset <= (64-size)) {
			scope.append(new CGIAsm("std y+" + stackOffset + ",r16"));
			if(size>1) scope.append(new CGIAsm("std y+" + (stackOffset+1) + ",r17"));
			if(size>2) scope.append(new CGIAsm("std y+" + (stackOffset+2) + ",r18"));
			if(size>3) scope.append(new CGIAsm("std y+" + (stackOffset+3) + ",r19"));
		}
		else {
			scope.append(new CGIAsm("subi yl,low(0x10000-" + stackOffset + ")"));
			scope.append(new CGIAsm("sbci yh,high(0x10000-" + stackOffset + ")"));
			scope.append(new CGIAsm("st y,r16"));
			if(size>1) scope.append(new CGIAsm("std y+1,r17"));
			if(size>2) scope.append(new CGIAsm("std y+2,r18"));
			if(size>3) scope.append(new CGIAsm("std y+3,r19"));
			scope.append(new CGIAsm("subi yl," + stackOffset));
			scope.append(new CGIAsm("sbci yh," + stackOffset));
		}
	}

	@Override
	public void constLoadRegs(String label, byte[] registers) {
		scope.append(new CGIAsm("ldi r" + registers[0] + ", low(" + label + "*2)"));
		scope.append(new CGIAsm("ldi r" + registers[1] + ", high(" + label + "*2)"));
	}


	@Override
	public void addAccum(int value) {
		if(0x00<value) {
			long tmp = 0x100000000l - value;
			scope.append(new CGIAsm("subi r16," + (tmp&0xff))); tmp >>= 0x08;
			if(accum.getSize()>1) scope.append(new CGIAsm("subi r17," + (tmp&0xff))); tmp >>= 0x08;
			if(accum.getSize()>2) scope.append(new CGIAsm("subi r18," + (tmp&0xff))); tmp >>= 0x08;
			if(accum.getSize()>3) scope.append(new CGIAsm("subi r19," + (tmp&0xff))); tmp >>= 0x08;
		}
	}

	public void add(byte[] regs, int value) { //TODO херня, должно быть как в localLoadRegs. Т.е. либо работаем с аккумулятором(если несколько действий, либо
		//полноценно единоразово, как в localLoadRegs. Нет проблемы, если переменная в регистрах, а если затронута память? Тогда лучше всего загрузить в аккум и не выебываться
		//НЕТ, при единой опрации всеравно экономнее разбирать побайтно, а не загружать в аккум. Но тогда нужно будет это сделать для каждой операции!!!
		//Хм, возможно получится взять один механизм а ему просто подставлять строку в определенном формате
		if(0x00<value) {
			long tmp = 0x100000000l - value;
			scope.append(new CGIAsm("subi r" + regs[0] + "," + (tmp&0xff))); tmp >>= 0x08;
			if(regs.length>1) scope.append(new CGIAsm("subi r" + regs[1] + "," + (tmp&0xff))); tmp >>= 0x08;
			if(regs.length>2) scope.append(new CGIAsm("subi r" + regs[2] + "," + (tmp&0xff))); tmp >>= 0x08;
			if(regs.length>3) scope.append(new CGIAsm("subi r" + regs[3] + "," + (tmp&0xff)));
		}
	}


	@Override
	public void subAccum(int value) {
		if(0x00<value) {
			scope.append(new CGIAsm("subi r16," + (value&0xff) + ")n")); value >>= 0x08;
			if(accum.getSize()>1) scope.append(new CGIAsm("subi r17," + (value&0xff))); value >>= 0x08;
			if(accum.getSize()>2) scope.append(new CGIAsm("subi r18," + (value&0xff))); value >>= 0x08;
			if(accum.getSize()>3) scope.append(new CGIAsm("subi r19," + (value&0xff)));
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
				localActionAsm(lScope, (int sn, String reg) -> new CGIAsm("com " + reg));
				localActionAsm(lScope, (int sn, String reg) -> new CGIAsm(0 == sn ? "sub " + reg + ", C0x01" : "sbc " + reg + ", C0x00"));
				break;
			case BIT_NOT:
				localActionAsm(lScope, (int sn, String reg) -> new CGIAsm("com " + reg));
			case PRE_INC:
			case POST_INC:
				localActionAsm(lScope, (int sn, String reg) -> new CGIAsm(0 == sn ? "add " + reg + ", C0x01" : "adc " + reg + ", C0x00"));
				break;
			case PRE_DEC:
			case POST_DEC:
				localActionAsm(lScope, (int sn, String reg) -> new CGIAsm(0 == sn ? "sub " + reg + ", C0x01" : "sbc " + reg + ", C0x00"));
				break;
			default:
				throw new Exception("CG:emitUnary: unsupported operator: " + op);
		}
	}
		
		
	// Работаем с аккумулятором!
	@Override
	public void localAction(Operator op, int resId) throws Exception {
		CGLocalScope lScope = getLocal(resId);
		scope.append(new CGIText(";accum " + op + " local " + lScope.getName() + " -> accum"));

		System.out.println("CG:localAction, op:" + op);
		switch(op) {
			case PLUS:
				localActionAsm(lScope, (int sn, String reg) -> new CGIAsm((0 == sn ? "add " : "adc ") + getRightOp(sn, null) + ", " + reg));
				break;
			case MINUS:
				localActionAsm(lScope, (int sn, String reg) -> new CGIAsm((0 == sn ? "sub " : "sbc ") + getRightOp(sn, null) + ", " + reg));
				break;
			case BIT_AND:
				localActionAsm(lScope, (int sn, String reg) -> new CGIAsm("and " + getRightOp(sn, null) + ", " + reg));
				break;
			case BIT_OR:
				localActionAsm(lScope, (int sn, String reg) -> new CGIAsm("or " + getRightOp(sn, null) + ", " + reg));
				break;
			case BIT_XOR:
				localActionAsm(lScope, (int sn, String reg) -> new CGIAsm("eor " + getRightOp(sn, null) + ", " + reg));
				break;
			default:
				throw new Exception("CG:localAction: unsupported operator: " + op);
		}
	}

	// Работаем с аккумулятором!
	// Оптиммизировано для экономии памяти, для экономии FLASH можно воспользоваться CPC инструкцией
	@Override
	public void constAction(Operator op, long k) throws Exception {
		scope.append(new CGIText(";accum " + op + " " + k + " -> accum"));

		System.out.println("CG:localAction, op:" + op);
		switch(op) {
			case PLUS:
				constActionAsm(~k, (int sn, String rOp) -> new CGIAsm((0 == sn ? "subi " : "sbci ") + getRightOp(sn, null) + ", " + rOp));
				break;
			case MINUS:
				constActionAsm(k, (int sn, String rOp) -> new CGIAsm((0 == sn ? "subi " : "sbci ") + getRightOp(sn, null) + ", " + rOp));
			case LT:
				CGIContainer cont = new CGIContainer();
				CGLabelScope lbScope = makeLabel("end");
				for(int i=accum.getSize()-1; i>=0; i--) {
					cont.append(new CGIAsm("cpi " + getRightOp(i, null) + ", " + ((k>>(i*8))&0xff)));
					if(i!=0) {
						cont.append(new CGIAsm("brcs " + lbScope.getName()));
						cont.append(new CGIAsm("brne " + lbScope.getName()));
					}
				}
				if(1!=accum.getSize()) {
					cont.append(lbScope);
				}
				cont.append(new CGIAsm("rol r16"));
				scope.append(cont);
				break;
			case LTE:
				cont = new CGIContainer();
				lbScope = makeLabel("end");
				for(int i=accum.getSize()-1; i>=0; i--) {
					cont.append(new CGIAsm("cpi " + getRightOp(i, null) + ", " + ((k>>(i*8))&0xff)));
					if(i!=0) {
						cont.append(new CGIAsm("brcs " + lbScope.getName()));
						cont.append(new CGIAsm("brne " + lbScope.getName()));
					}
					else {
						cont.append(new CGIAsm("brne " + lbScope.getName())); //TODO не нужно?
					}
				}
				cont.append(lbScope);
				cont.append(new CGIAsm("rol r16"));
				scope.append(cont);
				break;
			case GTE:
				cont = new CGIContainer();
				lbScope = makeLabel("end");
				for(int i=accum.getSize()-1; i>=0; i--) {
					cont.append(new CGIAsm("cpi " + getRightOp(i, null) + ", " + ((k>>(i*8))&0xff)));
					if(i!=0) {
						cont.append(new CGIAsm("brcs " + lbScope.getName()));
						cont.append(new CGIAsm("brne " + lbScope.getName()));
					}
					if(1!=accum.getSize()) {
						cont.append(lbScope);
					}
					cont.append(new CGIAsm("rol r16"));
					cont.append(new CGIAsm("com r16"));
				}
				scope.append(cont);
				break;
			case GT:
				cont = new CGIContainer();
				lbScope = makeLabel("end");
				for(int i=accum.getSize()-1; i>=0; i--) {
					cont.append(new CGIAsm("cpi " + getRightOp(i, null) + ", " + ((k>>(i*8))&0xff)));
					if(i!=0) {
						cont.append(new CGIAsm("brcs " + lbScope.getName()));
						cont.append(new CGIAsm("brne " + lbScope.getName()));
					}
					else {
						cont.append(new CGIAsm("brne " + lbScope.getName()));
					}
				}
				cont.append(lbScope);
				cont.append(new CGIAsm("rol r16"));
				cont.append(new CGIAsm("com r16"));
				scope.append(cont);
				break;
			case EQ:
			case NEQ:
				cont = new CGIContainer();
				lbScope = makeLabel("end");
				for(int i=accum.getSize()-1; i>=0; i--) {
					cont.append(new CGIAsm("cpi " + getRightOp(i, null) + ", " + ((k>>(i*8))&0xff)));
					if(i!=0) {
						cont.append(new CGIAsm("brne " + lbScope.getName()));
					}
					else {
						cont.append(lbScope);
						cont.append(new CGIAsm("ldi r16," + (Operator.EQ == op ? "0x00" : "0x01")));
						cont.append(new CGIAsm("brne pc+0x02"));
						cont.append(new CGIAsm("ldi r16," + (Operator.EQ == op ? "0x01" : "0x00")));
					}
				}
				scope.append(cont);
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
					if(usedY) {
						scope.append(new CGIAsm("pop yh"));
						scope.append(new CGIAsm("pop yl"));
						usedY = false;
					}
					scope.append(handler.action(i, "r" + cell.getNum()));
					break;
				case STACK:
					if(lScope.getStackOffset() <= (64-lScope.getSize())) {
						if(usedY) {
							scope.append(new CGIAsm("pop yh"));
							scope.append(new CGIAsm("pop yl"));
							usedY = false;
						}
						scope.getBlockScope().putUsedReg((byte)16);
						scope.append(handler.action(i, "r16"));
						scope.append(new CGIAsm("std y+" + cell.getNum() + ",r16"));
					}
					else {
						scope.getBlockScope().putUsedReg((byte)16);
						if(!usedY) {
							scope.append(new CGIAsm("push yl"));
							scope.append(new CGIAsm("push yh"));
							usedY = false;
						}
						scope.append(new CGIAsm("subi yl,low(0x10000-" + cell.getNum() + ")")); //TODO можно оптимизировать
						scope.append(new CGIAsm("sbci yh,high(0x10000-" + cell.getNum() + ")"));//Если это условие выполняет более 1 раза, подряд 
						scope.append(handler.action(i, "r16"));
						scope.append(new CGIAsm("st y,r16"));
					}
			}
		}
		if(usedY) {
			scope.append(new CGIAsm("pop yh"));
			scope.append(new CGIAsm("pop yl"));
		}
	};
	
	public void constActionAsm(long k, CGActionHandler handler) throws Exception {
		for(int i=0; i<accum.getSize(); i++) {
			scope.append(handler.action(i, Long.toString((k>>(i*8))&0xff)));
		}
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

			if(null != parameters) {
				for(int i=0; i<parameters.length; i++) {
					byte[] regs = regIds[i];
					if(0 == regs.length || 4 < regs.length) throw new Exception("CG:Invalid parameters for invoke method " + methodQName);

					scope.getBlockScope().putUsedRegs(regs);
					
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
							throw new Exception("CG:InvokeNative: local not found resId: " + op.getValue());
						}
					}
					else {
						value = getNum(parameters[i]);
						loadRegsConst(regs, value);
					}
				}
			}
			scope.append(new CGIAsm("call "+ nb.getRTOSFunction()));
		}
	}
	
	
/*	@Override
	public void setValueByIndex(Operand op, int size, List<Byte> tempRegs) throws Exception {
		String params[] = op.getParams(size, flashData);
		for(int i=0; i<size; i++) {
			scope.append("ldi temp_l, " + params[i]);
			scope.append("lds y+" + i + ", temp_l");
		}
	}
*/	
	
	@Override
	public boolean emitInstanceof(long resId, int typeId) {
		System.out.println("CG:emitInstanceOf, op:" + resId + ", typeId:" + typeId);
		return false;//TODO new Operand(VarType.BOOL, OperandType.LITERAL, true);
	}
	
	@Override
	public void eIf(int condBlockResId, int thenBlockResId, Integer elseBlockResId) {
		System.out.println("CG:if, condBlockId:" + condBlockResId + ", thenBlockId:" + thenBlockResId + ", elseBlockId:" + elseBlockResId);
		CGBlockScope condBScope = (CGBlockScope)scope.getScope(condBlockResId);
		CGBlockScope thenBScope = (CGBlockScope)scope.getScope(thenBlockResId);
		CGBlockScope elseBScope = (null == elseBlockResId ? null : (CGBlockScope)scope.getScope(elseBlockResId));
		
		CGLabelScope beginLbScope = makeLabel("if_begin");
		CGLabelScope thenLbScope = makeLabel("if_then");
		CGLabelScope elseLbScope = makeLabel("if_else");
		CGLabelScope endLbScope = makeLabel("if_end");
		
		condBScope.prepend(beginLbScope);
		thenBScope.prepend(thenLbScope);
		condBScope.append(new CGIAsm("cpi r16,0x01")); //TODO в конечном оптимизаторе можно упростить выражения вида rol r16;cpi r16,0x01;brne ...end
		if(null != elseBScope) {
			condBScope.append(new CGIAsm("brne " + elseLbScope.getName())); //TODO необходима проверка длины прыжка
			thenBScope.append(new CGIAsm("rjmp " + endLbScope.getName())); //TODO необходима проверка длины прыжка
			elseBScope.prepend(elseLbScope);
			elseBScope.append(endLbScope);
		}
		else {
			condBScope.append(new CGIAsm("brne " + endLbScope.getName())); //TODO необходима проверка длины прыжка
			thenBScope.append(endLbScope);
		}
	}

	@Override
	public void eTry(int blockId, List<Case> cases, Integer defaultBlockId) {
		System.out.println("CG:try, blockId:" + blockId + ", casesBlocks:" + cases + ", defaultBlockId:" + defaultBlockId);
	}
	
	//TODO основано на блоках! Т.е. каждая итерация это вход в блок(и выход) выделяет память в стеке для переменных и сохраняет регистры
	//Хотя, вероятно можно вынести вне блока, работать со стеком перед меткой входа и после метки выхода из блока.
	//Это вопрос ключа оптимизации по памяти или по коду, текущий вариант съест меньше памяти чем стандартный сишный.
	@Override
	public void eWhile(Integer condResId, int bodyResId) throws Exception {
		int _resId = genId();
		System.out.println(	"CG:while, resId:" + _resId + ", condResId:" + condResId + ", bodyResId:" + bodyResId);
		CGBlockScope condBScope = null == condResId ? null : (CGBlockScope)getScope(condResId);
		CGBlockScope bodyBScope = (CGBlockScope)getScope(bodyResId);
		
		CGLabelScope beginlbScope = makeLabel("while_begin");
		CGLabelScope bodylbScope = makeLabel("while_body");
		CGLabelScope endlbScope = makeLabel("while_end");
		
		if(null != condBScope) {
			condBScope.prepend(beginlbScope);
		}
		bodyBScope.prepend(bodylbScope);
		bodyBScope.append(new CGIAsm("jmp " + bodylbScope.getName(), 2)); //TODO нужно будет в оптимизировать(если расстояние укладыввается в rjmp)
		bodyBScope.append(endlbScope);
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
