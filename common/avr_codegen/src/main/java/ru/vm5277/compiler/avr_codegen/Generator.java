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
import ru.vm5277.common.cg.Operand;
import ru.vm5277.common.RTOSFeature;
import ru.vm5277.common.SystemParam;
import ru.vm5277.common.cg.CGActionHandler;
import ru.vm5277.common.cg.items.CGIAsm;
import ru.vm5277.common.cg.items.CGIContainer;
import ru.vm5277.common.cg.items.CGIText;
import ru.vm5277.common.cg.scopes.CGBlockScope;
import ru.vm5277.common.cg.CGCell;
import ru.vm5277.common.cg.scopes.CGVarScope;
import ru.vm5277.common.cg.OperandType;
import ru.vm5277.common.cg.items.CGItem;
import ru.vm5277.common.cg.scopes.CGCellsScope;
import ru.vm5277.common.cg.scopes.CGExpressionScope;
import ru.vm5277.common.cg.scopes.CGFieldScope;
import ru.vm5277.common.cg.scopes.CGLabelScope;
import ru.vm5277.common.cg.scopes.CGMethodScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;

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
	public void loadConstToAcc(CGScope scope, int size, long value) {
		scope.append(new CGIText(";const '" + value + "'->accum "));
		accum.setSize(size);
		scope.append(new CGIAsm("ldi r16," + (value&0xff))); value >>= 0x08;
		if(size>1) scope.append(new CGIAsm("ldi r17," + (value&0xff))); value >>= 0x08;
		if(size>2) scope.append(new CGIAsm("ldi r18," + (value&0xff))); value >>= 0x08;
		if(size>3) scope.append(new CGIAsm("ldi r19," + (value&0xff)));
	}
	
	@Override
	public void loadRegsConst(CGScope scope, byte[] regs, long value) {
		for(byte reg : regs) {
			scope.append(new CGIAsm("ldi r" + reg + "," + (value&0xff))); value >>= 0x08;
		}
	}

	@Override
	public void accToCells(CGScope scope, CGCellsScope cScope) throws CompileException { //Записываем acc в переменную
		//Размер значения в аккумуляторе может быть меньше переменной/поля
		int size = (cScope.getSize() < accum.getSize() ? cScope.getSize() : accum.getSize());
		if(cScope instanceof CGVarScope) {
			CGVarScope vScope = (CGVarScope)cScope;
			scope.append(new CGIText(";accum->var '" + vScope.getName() + "'"));
		
			if(!vScope.isConstant()) {
				regsToVar(scope, getAccRegs(0x04), size, vScope);
			}
		}
		else if(cScope instanceof CGFieldScope) {
			CGFieldScope fScope = (CGFieldScope)cScope;
			scope.append(new CGIText(";accum->field " + fScope.getName()));
			if(!fScope.isStatic()) {
				accToField(scope, size, fScope);
			}
		}
		else throw new CompileException("Unsupported scope:" + cScope);
	}
	
	@Override
	public void retToCells(CGScope scope, CGCellsScope cScope) throws CompileException { //Записываем acc в переменную
		if(cScope instanceof CGVarScope) {
			CGVarScope vScope = (CGVarScope)cScope;
			if(verbose) scope.append(new CGIText("CG: ret->var '" + vScope.getName() + "'"));
		
			if(!vScope.isConstant()) {
				int size = vScope.getCells().length;
				regsToVar(scope, getRetRegs(size), size, vScope);
			}
		}
		else if(cScope instanceof CGFieldScope) {
			CGFieldScope fScope = (CGFieldScope)cScope;
			if(verbose) scope.append(new CGIText("CG: ret->field " + fScope.getName()));
			if(!fScope.isStatic()) {
				accToField(scope, fScope.getSize(), fScope);
			}
		}
		else throw new CompileException("Unsupported scope:" + cScope);
	}

	@Override
	public void cellsToAcc(CGScope scope, CGCellsScope cScope) throws CompileException {
		if(cScope instanceof CGVarScope) {
			CGVarScope vScope = (CGVarScope)cScope;
			if(!vScope.isConstant()) {
				if(verbose) scope.append(new CGIText("CG: var '" + vScope.getName() + "'->accum"));
				varToRegs(scope, vScope, getAccRegs(vScope.getSize()));
			}
			else {
// Не нужно, invokeNative сразу записыапет значение в нужные регистры				
//				scope.append(new CGIAsm("ldi zl,low(" + vScope.getDataSymbol().getLabel() + ")"));
//				scope.append(new CGIAsm("ldi zh,high(" + vScope.getDataSymbol().getLabel() + ")"));
			}
		}
		else if(cScope instanceof CGFieldScope) {
			CGFieldScope fScope = (CGFieldScope)cScope;
			scope.append(new CGIText(";field '" + fScope.getName() + "'->accum"));
			fieldToRegs(scope, fScope, getAccRegs(fScope.getSize()));
		}
		else throw new CompileException("Unsupported scope:" + cScope);
	}
	
	@Override
	public void accCast(CGScope scope, VarType type) throws CompileException {
		scope.append(new CGIText(";acc cast " + accum.getSize() + "->" + type.getSize()));
		if(accum.getSize() < type.getSize()) {
			for(int i=accum.getSize(); i<type.getSize(); i++) {
				scope.append(new CGIAsm("ldi r" + getAccRegs(4)[i] + ",0x00"));
			}
		}
		accum.setSize(type.getSize());
	}

	public void varToRegs(CGScope scope, CGVarScope vScope, byte[] registers) throws CompileException {
		accum.setSize(vScope.getSize());
		//Map<Byte, String> popRegs = new HashMap<>();
		LinkedList<Byte> popRegs = new LinkedList<>();
		
		for(int i=0; i<registers.length; i++) {
			CGCell cell = vScope.getCells()[i];
			byte srcReg = (byte)cell.getNum();
			byte dstReg = registers[i];

			switch(cell.getType()) {
				case REG:
					if(srcReg == dstReg) continue; // регистры совпались, ничего не делаем
					
					if(!popRegs.contains(dstReg)) {
						if(	i != (registers.length-1) && isEven(srcReg) && isEven(dstReg) && CGCell.Type.REG == vScope.getCells()[i+1].getType() &&
							(srcReg+1)==vScope.getCells()[i+1].getNum() && (dstReg+1)==registers[i+1]) { // подходит для инструкции movw
						
							Integer pos = vScope.getRegCellPos(dstReg);
							if(null==pos || pos<=i) {
								scope.append(new CGIAsm("movw r" + dstReg + ",r" + srcReg));
								i++;
								continue;
							}
						}
						
						Integer pos = vScope.getRegCellPos(dstReg);
						if(null != pos && pos > i) {
							scope.append(new CGIAsm("push r" + dstReg));
							popRegs.addFirst(srcReg);
						}
						scope.append(new CGIAsm("mov r" + dstReg + ",r" + srcReg));
					}
					break;
				case STACK:
					if(cell.getNum() <= (64-vScope.getSize())) {
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

	public void fieldToRegs(CGScope scope, CGFieldScope fScope, byte[] registers) throws CompileException {
		accum.setSize(fScope.getSize());
		
		if(CGCell.Type.HEAP == fScope.getCells()[0].getType()) {
			int addr = fScope.getCells()[0].getNum();
			scope.append(new CGIAsm("ldi zl,low(" + addr + ")"));
			scope.append(new CGIAsm("ldi zh,high(" + addr + ")"));

			for (int i=0; i<fScope.getCells().length; i++) {
				scope.append(new CGIAsm("ldd r" + registers[i] + ",z+" + (fScope.getCells()[i].getNum()-addr)));
			}
		}
	}
	
	@Override
	public void varStore(CGVarScope scope, long value) throws CompileException {
		if(verbose) scope.append(new CGIText("CG: value '" + value + "'->var " + scope.getName()));
		boolean usedY = false;
		for (CGCell cell : scope.getCells()) {
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
					if(scope.getStackOffset() <= (64-scope.getSize())) {
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

	/**
	 *
	 * @param scope
	 * @param regs - полный набор регистров переменной/поля/аккумулятора
	 * @param usedRegsQnt - количество действительно используемых регистров, в не используемые будет записан 0x00
	 * @param vScope
	 * @throws CompileException
	 */
	public void regsToVar(CGScope scope, byte[] regs, int usedRegsQnt, CGVarScope vScope) throws CompileException {
		int size = vScope.getCells().length;
		LinkedList<Byte> popRegs = new LinkedList<>();
		Integer addr = null;
		
		for(int i=0; i<size; i++) {
			CGCell cell = vScope.getCells()[i];
			byte srcReg = regs[i];
			if(i >= usedRegsQnt) {
				byte dstReg = (byte)cell.getNum();
				scope.append(new CGIAsm("ldi r" + dstReg + ",0x00"));
			}
			else {
				switch(cell.getType()) {
					case REG:
						byte dstReg = (byte)cell.getNum();
						if(dstReg == srcReg) continue;

						int pos = getRegPos(regs, i, dstReg);
						if(-1 != pos) {
							scope.append(new CGIAsm("push r" + dstReg));
							popRegs.add(srcReg);
							continue;
						}

						// Подходит для инструкции movw
						if(	i != (regs.length-1)  && i < (usedRegsQnt-1) && isEven(dstReg) && isEven(srcReg) &&
							CGCell.Type.REG == vScope.getCells()[i+1].getType() && dstReg+1==vScope.getCells()[i+1].getNum() && (srcReg+1)==regs[i+1]) {

							pos = getRegPos(regs, i, dstReg);
							if(-1 == pos) {
								scope.append(new CGIAsm("movw r" + dstReg + ",r" + srcReg));
								i++;
								continue;
							}
						}

						scope.append(new CGIAsm("mov r" + dstReg + ",r" + srcReg));
						break;
					case STACK:
						if(null == addr) {
							addr = vScope.getStackOffset() + vScope.getCells()[0].getNum();
							scope.append(new CGIAsm("ldi yl,low(" + addr + ")"));
							scope.append(new CGIAsm("ldi yh,high(" + addr + ")"));
						}
						scope.append(new CGIAsm("std y+" + (vScope.getCells()[i].getNum()-addr) + ",r" + srcReg));
						break;
					default:
						throw new CompileException("Unsupported cell type:" + cell.getType());
				}
			}
		}
		for(Byte reg : popRegs) {
			scope.append(new CGIAsm("pop r" + reg));
		}
	}
	
	
	public int getRegPos(byte[] regs, int offset, byte reg) {
		for(int i=offset; i<regs.length; i++) {
			if(regs[i] == reg) return i;
		}
		return -1;
	}

	public void accToField(CGScope scope, int size, CGFieldScope fScope) {
		scope.append(new CGIAsm("ldi zl,low(" + fScope.getCells()[0].getNum() + ")"));
		scope.append(new CGIAsm("ldi zh,high(" + fScope.getCells()[0].getNum() + ")"));
		scope.append(new CGIAsm("st z,r16"));
		if(size>1) scope.append(new CGIAsm("std z+" + fScope.getCells()[1].getNum() + ",r17"));
		if(size>2) scope.append(new CGIAsm("std z+" + fScope.getCells()[2].getNum() + ",r18"));
		if(size>3) scope.append(new CGIAsm("std z+" + fScope.getCells()[3].getNum() + ",r19"));
	}

	@Override
	public void constLoadRegs(String label, byte[] registers) {
		scope.append(new CGIAsm("ldi r" + registers[0] + ", low(" + label + "*2)"));
		scope.append(new CGIAsm("ldi r" + registers[1] + ", high(" + label + "*2)"));
	}


	@Override
	public void emitUnary(Operator op, Integer resId) throws CompileException {
		if(null == resId) throw new CompileException("CG: emitUrany, unsupported accum mode");
		CGVarScope lScope = getLocal(scope, resId);//TODO
		
		switch(op) {
			case PLUS:
				break;
			case MINUS:
				cellsActionAsm(lScope, lScope, (int sn, String reg) -> new CGIAsm("com " + reg));
				cellsActionAsm(lScope, lScope, (int sn, String reg) -> new CGIAsm(0 == sn ? "sub " + reg + ", C0x01" : "sbc " + reg + ", C0x00"));
				break;
			case BIT_NOT:
				cellsActionAsm(lScope, lScope, (int sn, String reg) -> new CGIAsm("com " + reg));
			case PRE_INC:
			case POST_INC:
				cellsActionAsm(lScope, lScope, (int sn, String reg) -> new CGIAsm(0 == sn ? "add " + reg + ", C0x01" : "adc " + reg + ", C0x00"));
				break;
			case PRE_DEC:
			case POST_DEC:
				cellsActionAsm(lScope, lScope, (int sn, String reg) -> new CGIAsm(0 == sn ? "sub " + reg + ", C0x01" : "sbc " + reg + ", C0x00"));
				break;
			default:
				throw new CompileException("CG:emitUnary: unsupported operator: " + op);
		}
	}
		
		
	// Работаем с аккумулятором!
	@Override
	public void cellsAction(CGScope scope, CGCellsScope cScope, Operator op) throws CompileException {
		scope.append(new CGIText(";accum " + op + " cells " + cScope.getName() + " -> accum"));

		switch(op) {
			case PLUS:
				cellsActionAsm(scope, cScope, (int sn, String reg) -> new CGIAsm((0 == sn ? "add " : "adc ") + getRightOp(sn, null) + ", " + reg));
				break;
			case MINUS:
				cellsActionAsm(scope, cScope, (int sn, String reg) -> new CGIAsm((0 == sn ? "sub " : "sbc ") + getRightOp(sn, null) + ", " + reg));
				break;
			case BIT_AND:
				cellsActionAsm(scope, cScope, (int sn, String reg) -> new CGIAsm("and " + getRightOp(sn, null) + ", " + reg));
				break;
			case BIT_OR:
				cellsActionAsm(scope, cScope, (int sn, String reg) -> new CGIAsm("or " + getRightOp(sn, null) + ", " + reg));
				break;
			case BIT_XOR:
				cellsActionAsm(scope, cScope, (int sn, String reg) -> new CGIAsm("eor " + getRightOp(sn, null) + ", " + reg));
				break;
			default:
				throw new CompileException("CG:cellsAction: unsupported operator: " + op);
		}
	}

	// Работаем с аккумулятором!
	// Оптиммизировано для экономии памяти, для экономии FLASH можно воспользоваться CPC инструкцией
	@Override
	public void constAction(CGScope scope, Operator op, long k) throws CompileException {
		scope.append(new CGIText(";accum " + op + " " + k + " -> accum"));
		System.out.println("CG:constAction, op:" + op);
		CGIContainer cont = new CGIContainer("constAction:" + op);
		switch(op) {
			case PLUS:
				constActionAsm(scope, ~k, (int sn, String rOp) -> new CGIAsm((0 == sn ? "subi " : "sbci ") + getRightOp(sn, null) + ", " + rOp));
				break;
			case MINUS:
				constActionAsm(scope, k, (int sn, String rOp) -> new CGIAsm((0 == sn ? "subi " : "sbci ") + getRightOp(sn, null) + ", " + rOp));
			case LT:
				CGLabelScope lbScope = makeLabel(null, "end", true);
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
				lbScope = makeLabel(null, "end", true);
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
				lbScope = makeLabel(null, "end", true);
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
				lbScope = makeLabel(null, "end", true);
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
				lbScope = makeLabel(null, "end", true);
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
				throw new CompileException("CG:constAction: unsupported operator: " + op);
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
	
	public void cellsActionAsm(CGScope scope, CGCellsScope cHolder, CGActionHandler handler) throws CompileException {
		boolean usedY = false;
		if(CGCell.Type.HEAP == cHolder.getCells()[0].getType()) {
			int addr = cHolder.getCells()[0].getNum();
			scope.append(new CGIAsm("push yl")); //TODO проверять свободный регистр, если все занято, использовать YL
			scope.append(new CGIAsm("ldi zl,low(" + addr + ")"));
			scope.append(new CGIAsm("ldi zh,high(" + addr + ")"));
			
			for (int i=0; i<cHolder.getCells().length; i++) {
				CGCell cell = cHolder.getCells()[i];
				scope.append(new CGIAsm("ldd yl,z+" + (cell.getNum()-addr)));
				scope.append(handler.action(i, "yl"));
			}
			scope.append(new CGIAsm("pop yl"));
			return;
		}
		
		if(CGCell.Type.RET == cHolder.getCells()[0].getType()) {
			int size = cHolder.getCells().length;
			if(0<size) scope.append(handler.action(0, "zl"));
			if(1<size) scope.append(handler.action(1, "zh"));
			if(4==size) {
				scope.append(handler.action(2, "yl"));
				scope.append(handler.action(3, "yh"));
			}
		}

		for (int i=0; i<cHolder.getCells().length; i++) {
			CGCell cell = cHolder.getCells()[i];
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
					if(((CGVarScope)cHolder).getStackOffset() <= (64-cHolder.getCells().length)) {
						if(usedY) {
							scope.append(new CGIAsm("pop yh"));
							scope.append(new CGIAsm("pop yl"));
							usedY = false;
						}
						cHolder.getBlockScope().putUsedReg((byte)16);
						scope.append(handler.action(i, "r16"));
						scope.append(new CGIAsm("std y+" + cell.getNum() + ",r16"));
					}
					else {
						cHolder.getBlockScope().putUsedReg((byte)16);
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
					break;
				default:
					throw new CompileException("Unsupported cell type:" + cell.getType());
			}
		}
		if(usedY) {
			scope.append(new CGIAsm("pop yh"));
			scope.append(new CGIAsm("pop yl"));
		}
	};
	
	public void constActionAsm(CGScope scope, long k, CGActionHandler handler) throws CompileException {
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
	public void invokeMethod(CGScope scope, String className, String methodName, VarType type, CGMethodScope mScope) throws CompileException {
		if(verbose) scope.append(new CGIText("CG:invokeMethod " + type + " " + className + "." + methodName));

		scope.append(new CGIAsm("call " + mScope.getLabel().getName()));
	}
	
	@Override
	public void invokeNative(CGScope scope, String className, String methodName, String paramTypes, VarType type, Operand[] ops) throws CompileException {
		String methodQName = className + "." + methodName + (null != paramTypes ? " " + paramTypes : "");
		if(verbose) scope.append(new CGIText("CG:invokeNative " + type + " " + methodQName + (null != ops ? ", params:" + Arrays.toString(ops) : "")));
		
		if(methodQName.equals("System.setParam [byte, byte]")) {
			SystemParam sp = SystemParam.values()[(int)getNum(ops[0x00])];
			switch(sp) {
				case CORE_FREQ:
					params.put(sp, (int)getNum(ops[0x01]));
					break;
				case STDOUT_PORT:
					params.put(sp, (int)getNum(ops[0x01]));
					break;
				case SHOW_WELCOME:
					if(0x00 == (int)getNum(ops[0x01])) {
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
				throw new CompileException("CG:InvokeNative, not found native binding for method: " + methodQName);
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

			if(null != ops) {
				for(int i=0; i<ops.length; i++) {
					byte[] regs = regIds[i];
					if(0 == regs.length || 4 < regs.length) throw new CompileException("CG:Invalid parameters for invoke method " + methodQName);

					scope.getBlockScope().putUsedRegs(regs);
					
					long value = 0;
					Operand op = ops[i];
					if(OperandType.LOCAL_RES == op.getOperandType()) {
						CGVarScope vScope = getLocal(scope, (int)op.getValue());
						if(null != vScope) {
							if(!vScope.isConstant()) {
								if(regs.length > vScope.getSize()) { //TODO Переменная может быть больше размером(использовано сокращение)
									throw new CompileException("CG:InvokeNative: params qnt not equlas regs qnt in method: " + methodQName);
								}
								varToRegs(vScope, vScope, regs);
							}
							else {
								constLoadRegs(vScope.getDataSymbol().getLabel(), regs);
							}
						}
						else {
							throw new CompileException("CG:InvokeNative: local not found resId: " + op.getValue());
						}
					}
					else if(OperandType.FLASH_RES == op.getOperandType()) {
						if(0x00 == regs.length || 0x02<regs.length) {
							throw new CompileException("Invalid registers quantity for flash constant");
						}
						if(0x00<regs.length) {
							scope.append(new CGIAsm("ldi r" + regs[0] + ",low(" + flashData.get((Integer)op.getValue()).getLabel() + ")"));
						}
						if(0x01<regs.length) {
							scope.append(new CGIAsm("ldi r" + regs[1] + ",high(" + flashData.get((Integer)op.getValue()).getLabel() + ")"));
						}
					}
					else if(OperandType.RETURN == op.getOperandType()) {
						if(0x01==regs.length) {
							scope.append(new CGIAsm("mov r" + regs[0] + ",zl"));
						}
						if(0x02<=regs.length) {
							if(0x00 == (regs[0]&0x01) && regs[0]==(regs[1]-1)) {
								scope.append(new CGIAsm("movw r" + regs[0] + ",zl"));
							}
							else {
								scope.append(new CGIAsm("mov r" + regs[0] + ",zl"));
								scope.append(new CGIAsm("mov r" + regs[1] + ",zh"));
							}
						}
						if(0x04==regs.length) { 
							if(0x00 == (regs[2]&0x01) && regs[2]==(regs[3]-1)) {
								scope.append(new CGIAsm("movw r" + regs[2] + ",yl"));
							}
							else {
								scope.append(new CGIAsm("mov r" + regs[2] + ",yl"));
								scope.append(new CGIAsm("mov r" + regs[3] + ",yh"));
							}
						}
					}
					else {
						value = getNum(ops[i]);
						loadRegsConst(scope, regs, value);
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
	public void eIf(CGBlockScope condBlockScope, CGBlockScope thenBlockScope, CGBlockScope elseBlockScope) {
		System.out.println("CG:if, condBlockId:" + condBlockScope + ", thenBlockId:" + thenBlockScope + ", elseBlockId:" + elseBlockScope);
		
		CGLabelScope beginLbScope = makeLabel(null, "if_begin", true);
		CGLabelScope thenLbScope = makeLabel(null, "if_then", true);
		CGLabelScope elseLbScope = makeLabel(null, "if_else", true);
		CGLabelScope endLbScope = makeLabel(null, "if_end", true);
		
		condBlockScope.prepend(beginLbScope);
		thenBlockScope.prepend(thenLbScope);
		

		String optimizedBrInst = Optimizator.localWithConstantComparingCondition((CGExpressionScope)condBlockScope.getItems().get(1));
		boolean optimized = false;
		String brInstr = null == optimizedBrInst ? "brne" : optimizedBrInst;
		

		if(null == optimizedBrInst) condBlockScope.append(new CGIAsm("cpi r16,0x01"));
		if(null != elseBlockScope) {
			condBlockScope.append(new CGIAsm(brInstr + " " + elseLbScope.getName())); //TODO необходима проверка длины прыжка
			thenBlockScope.append(new CGIAsm("rjmp " + endLbScope.getName())); //TODO необходима проверка длины прыжка
			elseBlockScope.prepend(elseLbScope);
			elseBlockScope.append(endLbScope);
		}
		else {
			condBlockScope.append(new CGIAsm(brInstr + " " + endLbScope.getName())); //TODO необходима проверка длины прыжка
			thenBlockScope.append(endLbScope);
		}
	}

	@Override
	public void eTry(CGBlockScope blockScope, List<Case> cases, CGBlockScope defaultBlockScope) {
		//System.out.println("CG:try, block:" + blockScope + ", casesBlocks:" + cases + ", defaultBlock:" + defaultBlockScope);
	}
	
	//TODO основано на блоках! Т.е. каждая итерация это вход в блок(и выход) выделяет память в стеке для переменных и сохраняет регистры
	//Хотя, вероятно можно вынести вне блока, работать со стеком перед меткой входа и после метки выхода из блока.
	//Это вопрос ключа оптимизации по памяти или по коду, текущий вариант съест меньше памяти чем стандартный сишный.
	@Override
	public void eWhile(CGBlockScope condBlockScope, CGBlockScope bodyBlockScope) throws CompileException {
		int _resId = genId();
		System.out.println(	"CG:while, resId:" + _resId + ", cond:" + condBlockScope + ", body:" + bodyBlockScope);
		
		CGLabelScope beginlbScope = makeLabel(null, "while_begin", true);
		CGLabelScope bodylbScope = makeLabel(null, "while_body", true);
		CGLabelScope endlbScope = makeLabel(null, "while_end", true);
		
		if(null != condBlockScope) {
			condBlockScope.prepend(beginlbScope);
		}
		bodyBlockScope.prepend(bodylbScope);
		bodyBlockScope.append(new CGIAsm("jmp " + bodylbScope.getName(), 2)); //TODO нужно будет в оптимизировать(если расстояние укладыввается в rjmp)
		bodyBlockScope.append(endlbScope);
	}
	
	@Override
	public void eReturn(CGScope scope, int size) {
		if(verbose) scope.append(new CGIText(";CG:eReturn, size:" + size));
		if(1==size) scope.append(new CGIAsm("mov zl,r16"));
		if(2<=size) scope.append(new CGIAsm("movw zl,r16"));
		if(4==size) scope.append(new CGIAsm("movw yl,r18"));
		scope.append(new CGIAsm("ret"));
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
	
	public byte[] getAccRegs(int size) {
		switch(size) {
			case 1: return new byte[]{16};
			case 2: return new byte[]{16,17};
			case 4: return new byte[]{16,17,18,19};
		}
		return null;
	}
	
	public byte[] getRetRegs(int size) {
		switch(size) {
			case 1: return new byte[]{30};
			case 2: return new byte[]{30,31};
			case 4: return new byte[]{30,31,28,29};
		}
		return null;
	}

	@Override
	public String getVersion() {
		return VERSION;
	}
}
