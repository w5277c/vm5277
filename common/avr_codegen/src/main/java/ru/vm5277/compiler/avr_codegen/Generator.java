/*
 * Copyright 2025 konstantin@5277.ru
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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import ru.vm5277.common.LabelNames;
import ru.vm5277.common.NativeBinding;
import ru.vm5277.common.Operator;
import static ru.vm5277.common.Operator.BIT_AND;
import static ru.vm5277.common.Operator.BIT_NOT;
import static ru.vm5277.common.Operator.BIT_OR;
import static ru.vm5277.common.Operator.BIT_XOR;
import static ru.vm5277.common.Operator.EQ;
import static ru.vm5277.common.Operator.LT;
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
import ru.vm5277.common.RTOSLibs;
import ru.vm5277.common.StrUtils;
import ru.vm5277.common.SystemParam;
import ru.vm5277.common.cg.CGActionHandler;
import ru.vm5277.common.cg.items.CGIAsm;
import ru.vm5277.common.cg.items.CGIContainer;
import ru.vm5277.common.cg.items.CGIText;
import ru.vm5277.common.cg.scopes.CGBlockScope;
import ru.vm5277.common.cg.CGCells;
import ru.vm5277.common.cg.scopes.CGVarScope;
import ru.vm5277.common.cg.OperandType;
import ru.vm5277.common.cg.RegPair;
import ru.vm5277.common.cg.items.CGIAsmJump;
import ru.vm5277.common.cg.items.CGIAsmLdLabel;
import ru.vm5277.common.cg.scopes.CGCellsScope;
import ru.vm5277.common.cg.scopes.CGClassScope;
import ru.vm5277.common.cg.scopes.CGBranchScope;
import ru.vm5277.common.cg.scopes.CGFieldScope;
import ru.vm5277.common.cg.scopes.CGLabelScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;

// TODO Кодогенератор НЕ ДОЛЖЕН выполнять работу оптимизатора, т.е. не нужно формировать movw вместо двух mov и тому подобное. Так как оптимизатор справится с
// этой задачей лучше, а кодогенератор может ему помешать.

public class Generator extends CodeGenerator {
	private	final	static	String				VERSION		= "0.1";
	private	final	static	byte[]				usableRegs	= new byte[]{20,21,22,23,24,25,26,27,   19,18,17}; //Используем регистры
	//private	final	static	byte[]				usableRegs	= new byte[]{}; 	//Без регистров для локальных переменных
	//private	final	static	byte[]				usableRegs	= new byte[]{20}; 	//С одним регистром
	//private	final	static	byte[]				usableRegs	= new byte[]{20,21,22,23}; 	//С 4 регистрами
	private	final	static	boolean				def_sph		= true; // TODO генератор avr должен знать характеристики МК(чтобы не писать асм код с .ifdef)
	private	final	static	boolean				def_call	= true; // TODO генератор avr должен знать характеристики МК(чтобы не писать асм код с .ifdef)
	private	final	static	String				INSTR_JUMP	= "rjmp";
	private	final	static	String				INSTR_CALL	= "rcall";
	
	public Generator(String platform, Map<String, NativeBinding> nbMap, Map<SystemParam, Object> params) {
		super(platform, nbMap, params);
		
		optimizer = new Optimizer();
		// Собираем RTOS
	}
	
	@Override
	public CGIContainer stackAlloc(CGScope scope, int size) {
		CGIContainer result = new CGIContainer();
		
		if(VERBOSE_LO <= verbose) result.append(new CGIText(";alloc stack, size:" + size));
//		result.append(new CGIAsm("push yl"));
//		if(def_sph) result.append(new CGIAsm("push yh"));
		
		if(size<5) {
			for(int i=0; i<size; i++) {
				result.append(new CGIAsm("push c0x00"));
			}
			result.append(new CGIAsm("sbiw yl," + size));
		}
		else {
			result.append(new CGIAsm("ldi yl,low(" + size + ")"));
			result.append(new CGIAsm("ldi yh,high(" + size + ")"));
			result.append(new CGIAsm("push c0x00"));
			result.append(new CGIAsm("sbiw yl,0x01"));
			result.append(new CGIAsm("brne pc-0x02"));//TODO проверить
			result.append(new CGIAsm("lds yl,SPL")); //TODO проверить адрес ячейки
			result.append(new CGIAsm(def_sph ? "lds yh,SPH" : "ldi yh,0x00"));
		}
		
		if(null != scope) scope.append(result);
		return result;
	}
	@Override
	public CGIContainer blockFree(CGScope scope, int size) {
		CGIContainer result = new CGIContainer();
		
		if(VERBOSE_LO <= verbose) result.append(new CGIText(";block free, size:" + size));
		result.append(new CGIAsm("subi yl,low(" + size*(-1) + ")"));
		if(def_sph) result.append(new CGIAsm("sbci yh,high(" + size*(-1) + ")"));
		if(def_sph) result.append(new CGIAsm("lds zl,SREG"));
		if(def_sph) result.append(new CGIAsm("cli"));
		result.append(new CGIAsm("sts SPL,yl"));
		if(def_sph) result.append(new CGIAsm("sts SPH,yh"));
		if(def_sph) result.append(new CGIAsm("sts SREG,zl"));
		
		/*
		if(VERBOSE_LO <= verbose) result.append(new CGIText(";free stack, size:" + size));
		if(size<9) {
			for(int i=0; i<size; i++) {
				result.append(new CGIAsm("pop yl"));
			}
		}
		else {
			if(def_sph) result.append(new CGIAsm("lds yl,SREG"));
			if(def_sph) result.append(new CGIAsm("cli"));
			result.append(new CGIAsm("lds _SPL,SPL"));
			if(def_sph) result.append(new CGIAsm("lds _SPH,SPH"));
			result.append(new CGIAsm("subi _SPL,low(-" + size + ")"));
			if(def_sph) result.append(new CGIAsm("sbci _SPH,high(-" + size + ")"));
			result.append(new CGIAsm("sts SPL,_SPL"));
			if(def_sph) result.append(new CGIAsm("sts SPH,_SPH"));
			if(def_sph) result.append(new CGIAsm("sts SREG,yl"));
		}
//		if(def_sph) result.append(new CGIAsm("pop yh"));
//		result.append(new CGIAsm("pop yl"));
*/
		if(null != scope) scope.append(result);
		return result;
	}
	
	public CGIAsm pushRegAsm(byte reg) {return new CGIAsm("push r"+reg+"");}
	public CGIAsm popRegAsm(byte reg) {return new CGIAsm("pop r"+reg+"");}

	@Override
	public CGIContainer pushHeapReg(CGScope scope, boolean half) {
		CGIContainer result = new CGIContainer();
		if(VERBOSE_LO <= verbose) result.append(new CGIText(";push heap iReg"));
		result.append(new CGIAsm("push zl"));
		if(!half) result.append(new CGIAsm("push zh"));
		if(null != scope) scope.append(result);
		return result;
	}
	@Override
	public CGIContainer popHeapReg(CGScope scope, boolean half) {
		CGIContainer result = new CGIContainer();
		if(VERBOSE_LO <= verbose) result.append(new CGIText(";pop heap iReg"));
		if(!half) result.append(new CGIAsm("pop zh"));
		result.append(new CGIAsm("pop zl"));
		if(null != scope) scope.append(result);
		return result;
	}

	@Override
	public CGIContainer pushStackReg(CGScope scope) {
		CGIContainer result = new CGIContainer();
		if(VERBOSE_LO <= verbose) result.append(new CGIText(";push stack iReg"));
		result.append(new CGIAsm("push_y"));
		if(null != scope) scope.append(result);
		return result;
	}
	@Override
	public CGIContainer popStackReg(CGScope scope) {
		CGIContainer result = new CGIContainer();
		if(VERBOSE_LO <= verbose) result.append(new CGIText(";pop stack iReg"));
		result.append(new CGIAsm("pop_y"));
		if(null != scope) scope.append(result);
		return result;
	}
	
	@Override
	public CGIContainer stackToStackReg(CGScope scope) {
		CGIContainer result = new CGIContainer();
		if(VERBOSE_LO <= verbose) result.append(new CGIText(";stack addr to stack iReg"));
		result.append(new CGIAsm("lds yl,SPL")); //TODO проверить адрес ячейки
		result.append(new CGIAsm(def_sph ? "lds yh,SPH" : "ldi yh,0x00"));
		if(null != scope) scope.append(result);
		return result;
	}

	
	/**
	 * pushConst используетя для вызова метода(передачи констант)
	 * @param scope - область видимости для генерации кода
	 * @param size - необходимый размер в стеке
	 * @param value - значение константы
	 */
	@Override
	public void pushConst(CGScope scope, int size, long value) {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";push const '" + value + "'"));
		for(int i=0; i<size; i++) {
			scope.append(new CGIAsm("ldi r30," + (value&0xff)));
			scope.append(new CGIAsm("push r30"));
			value >>= 0x08;
		}
	}

	/**
	 *
	 * @param scope - область видимости для генерации кода
	 * @param label - метка объекта
	 */
	@Override
	public void pushLabel(CGScope scope, String label) {
		scope.append(new CGIAsmLdLabel("ldi zl,low", label, "*2"));
		scope.append(new CGIAsm("push zl"));
		if(0x02==getRefSize()) {
			scope.append(new CGIAsmLdLabel("ldi zl,high", label, "*2"));
			scope.append(new CGIAsm("push zl"));
		}
	}
	
	
	/**
	 * pushCells используетя для вызова метода(передачи параметров)
	 * @param scope - область видимости для генерации кода
	 * @param offset - смещение в стеке(если CGCellType.STACK), или смещение в HEAP(если CGCellType.HEAP)
	 * @param size - необходимый размер в стеке
	 * @param cells - ячейки указывающие источник данных
	 * @throws CompileException
	 */
	@Override
	public void pushCells(CGScope scope, int offset, int size, CGCells cells) throws CompileException {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";push cells: " + cells));

		switch(cells.getType()) {
			case REG:
				for(int i=0; i<cells.getSize(); i++) scope.append(new CGIAsm("push r" + cells.getId(i)));
				break;
			case STACK_FRAME:
				if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) {
					scope.append(new CGIAsm("rcall os_dispatcher_lock"));
				}
				int addr = cells.getId(0);
				boolean needRestoreIReg = Generator.this.moveIReg(scope, 'y', offset+addr, cells.getSize());
				for(int i=0; i<cells.getSize(); i++) {
					scope.append(new CGIAsm("ldd j8b_atom,y+" + (cells.getId(i) + (needRestoreIReg ? -addr : offset))));
					scope.append(new CGIAsm("push j8b_atom"));
				}
				if(needRestoreIReg) moveIReg(scope, 'y', offset+addr);
				if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) {
					scope.append(new CGIAsm("rcall os_dispatcher_unlock"));
				}
				break;
			case HEAP:
				if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) {
					scope.append(new CGIAsm("rcall os_dispatcher_lock"));
				}
				addr = cells.getId(0);
				needRestoreIReg = Generator.this.moveIReg(scope, 'z', offset+addr, cells.getSize());
				for(int i=0; i<cells.getSize(); i++) {
					scope.append(new CGIAsm("ldd j8b_atom,z+" + (cells.getId(i) + (needRestoreIReg ? -addr : offset))));
					scope.append(new CGIAsm("push j8b_atom"));
				}
				if(needRestoreIReg) moveIReg(scope, 'z', offset+addr);
				if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) {
					scope.append(new CGIAsm("rcall os_dispatcher_unlock"));
				}
				break;
			default:
				throw new CompileException("Unsupported cell type:" + cells.getType());
		}
		
		for(int i=cells.getSize(); i<size; i++) {
			scope.append(new CGIAsm("push c0x00"));
		}
	}

	@Override
	public void constToAcc(CGScope scope, int size, long value) {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";const '" + value + "'->accum "));
		accum.setSize(size);
		scope.append(new CGIAsm("ldi r16," + (value&0xff))); value >>= 0x08;
		if(size>1) scope.append(new CGIAsm("ldi r17," + (value&0xff))); value >>= 0x08;
		if(size>2) scope.append(new CGIAsm("ldi r18," + (value&0xff))); value >>= 0x08;
		if(size>3) scope.append(new CGIAsm("ldi r19," + (value&0xff)));
	}
	
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
			if(VERBOSE_LO <= verbose) scope.append(new CGIText(";accum->var '" + vScope.getName() + "'"));
		
			if(!vScope.isConstant()) {
				regsToVar(scope, getAccRegs(0x04), size, vScope.getStackOffset(), vScope.getCells());
			}
		}
		else if(cScope instanceof CGFieldScope) {
			CGFieldScope fScope = (CGFieldScope)cScope;
			if(VERBOSE_LO <= verbose) scope.append(new CGIText(";accum->field " + fScope.getName()));
			if(!fScope.isStatic()) {
				accToField(scope, size, ((CGClassScope)fScope.getParent()).getHeapHeaderSize(), fScope.getCells());
			}
		}
		else throw new CompileException("Unsupported scope:" + cScope);
	}
	
	@Override
	public void setHeapReg(CGScope scope, int offset, CGCells cells) throws CompileException {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";setHeap " + cells));
		cellsToRegs(scope, offset, cells, getTempRegs(getCallSize()));
		//cellsActionAsm(scope, offset, cells, (int sn, String rOp) -> new CGIAsm("mov " + (0==sn ? "zl" : "zh") + "," + rOp));
	}

	@Override
	public void cellsToAcc(CGScope scope, CGCellsScope cScope) throws CompileException {
		accum.setSize(cScope.getSize());
		if(cScope instanceof CGVarScope) {
			CGVarScope vScope = (CGVarScope)cScope;
			if(!vScope.isConstant()) {
				if(VERBOSE_LO <= verbose) scope.append(new CGIText(";var '" + vScope.getName() + "'->accum"));
				cellsToRegs(scope, vScope.getStackOffset(), vScope.getCells(), getAccRegs(cScope.getSize()));
			}
		}
		else if(cScope instanceof CGFieldScope) {
			CGFieldScope fScope = (CGFieldScope)cScope;
			if(VERBOSE_LO <= verbose) scope.append(new CGIText(";field '" + fScope.getName() + "'->accum"));
			int heapHeaderSize = ((CGClassScope)fScope.getParent()).getHeapHeaderSize();
			cellsToRegs(scope, heapHeaderSize, fScope.getCells(), getAccRegs(cScope.getSize()));
		}
		else throw new CompileException("Unsupported scope:" + cScope);
	}
	
	@Override
	public CGIContainer jump(CGScope scope, CGLabelScope lScope) throws CompileException {
		CGIContainer result = new CGIContainer();
		result.append(new CGIAsmJump(INSTR_JUMP, lScope));
		if(null != scope) scope.append(result);
		return result;
	}

	@Override
	public CGIContainer accCast(CGScope scope, int size) throws CompileException {
		CGIContainer result = new CGIContainer();
		if(accum.getSize() != size) {
			if(VERBOSE_LO <= verbose) result.append(new CGIText(";acc cast " + accum.getSize() + "->" + size));
			if(accum.getSize() < size) {
				for(int i=accum.getSize(); i<size; i++) {
					result.append(new CGIAsm("ldi r" + getAccRegs(4)[i] + ",0x00"));
				}
			}
			accum.setSize(size);
		}
		if(null != scope) scope.append(result);
		return result;
	}

	public void cellsToRegs(CGScope scope, int offset, CGCells cells, byte[] registers) throws CompileException {
		switch(cells.getType()) {
			case REG:
				LinkedList<Byte> popRegs = new LinkedList<>();
				for(int i=0; i<registers.length; i++) {
					byte srcReg = (byte)cells.getId(i);
					byte dstReg = registers[i];
					if(srcReg == dstReg) continue; // регистры совпались, ничего не делаем
					if(!popRegs.contains(dstReg)) {
						int pos = getCellsPos(cells, dstReg);
						if(pos>i) {
							scope.append(new CGIAsm("push r" + dstReg));
							popRegs.addFirst(srcReg);
						}
						scope.append(new CGIAsm("mov r" + dstReg + ",r" + srcReg));
					}
				}
				for(Byte reg : popRegs) scope.append(new CGIAsm("pop r" + reg));
				break;
			case STACK_FRAME:
				int addr = cells.getId(0);
				boolean needRestoreIReg = Generator.this.moveIReg(scope, 'y', offset+addr, cells.getSize());
				for(int i=0; i<cells.getSize(); i++) {
					scope.append(new CGIAsm("ldd r" + registers[i] + ",y+" + (cells.getId(i) + (needRestoreIReg ? -addr : offset))));
				}
				if(needRestoreIReg) moveIReg(scope, 'y', offset+addr);
				break;
			case HEAP:
				addr = cells.getId(0);
				needRestoreIReg = Generator.this.moveIReg(scope, 'z', offset+addr, cells.getSize());
				for(int i=0; i<cells.getSize(); i++) {
					scope.append(new CGIAsm("ldd r" + registers[i] + ",z+" + (cells.getId(i) + (needRestoreIReg ? -addr : offset))));
				}
				if(needRestoreIReg) moveIReg(scope, 'z', offset+addr);
				break;
			default:
				throw new CompileException("Unsupported cell type:" + cells.getType());
		}

		for(int i=cells.getSize(); i<registers.length; i++) {
			scope.append(new CGIAsm("ldi r" + registers[i] + ",0x00"));
		}
	}

	@Override
	public void constToCells(CGScope scope, int offset, long value, CGCells cells) throws CompileException {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";const '" + value + "'->cells " + cells));
		switch(cells.getType()) {
			case REG:
				for(int i=0; i<cells.getSize(); i++) scope.append(new CGIAsm("ldi r" + cells.getId(i)+ "," + ((value>>(i*8))&0xff)));
				break;
			case STACK_FRAME:
				int addr = cells.getId(0);
				scope.append(new CGIAsm("push zl"));
				boolean needRestoreIReg = Generator.this.moveIReg(scope, 'y', offset+addr, cells.getSize());
				for(int i=0; i<cells.getSize(); i++) {
					scope.append(new CGIAsm("ldi zl," + ((value>>(i*8))&0xff)));
					scope.append(new CGIAsm("std y+" + (cells.getId(i) + (needRestoreIReg ? -addr : offset)) + ",zl"));
				}
				scope.append(new CGIAsm("pop zl"));
				if(needRestoreIReg) moveIReg(scope, 'y', offset+addr);
				break;
			case HEAP:
				addr = cells.getId(0);
				scope.append(new CGIAsm("push yl"));
				needRestoreIReg = Generator.this.moveIReg(scope, 'z', offset+addr, cells.getSize());
				for(int i=0; i<cells.getSize(); i++) {
					scope.append(new CGIAsm("ldi yl," + ((value>>(i*8))&0xff)));
					scope.append(new CGIAsm("std z+" + (cells.getId(i) + (needRestoreIReg ? -addr : offset)) + ",yl"));
				}
				scope.append(new CGIAsm("pop yl"));
				if(needRestoreIReg) moveIReg(scope, 'z', offset+addr);
				break;
			default:
				throw new CompileException("Unsupported cell type:" + cells.getType());
		}
	}

	/**
	 * regsToVar записывает регистры в ячейки локальной переменной 
	 * @param scope - область видимости для генерации кода
	 * @param regs - полный набор регистров переменной/поля/аккумулятора
	 * @param usedRegsQnt - количество действительно используемых регистров, в не используемые будет записан 0x00
	 * @param offset - смещение в стеке или HEAP'е
	 * @param cells - ячейки указывающие источник данных
	 * @throws CompileException
	 */
	public void regsToVar(CGScope scope, byte[] regs, int usedRegsQnt, int offset, CGCells cells) throws CompileException {
		switch(cells.getType()) {
			case REG:
				LinkedList<Byte> popRegs = new LinkedList<>();
				for(int i=0; i<cells.getSize(); i++) {
					byte srcReg = regs[i];
					byte dstReg = (byte)cells.getId(i);
					if(srcReg == dstReg) continue; // регистры совпались, ничего не делаем
					if(!popRegs.contains(dstReg)) {
						int pos = getRegPos(regs, i, dstReg);
						if(pos>i) {
							scope.append(new CGIAsm("push r" + dstReg));
							popRegs.addFirst(srcReg);
							//continue; //TODO прповерить
						}
						scope.append(new CGIAsm("mov r" + dstReg + ",r" + srcReg));
					}
				}
				for(Byte reg : popRegs) scope.append(new CGIAsm("pop r" + reg));
				break;
			case STACK_FRAME:
				int addr = cells.getId(0);
				boolean needRestoreIReg = Generator.this.moveIReg(scope, 'y', offset+addr, cells.getSize());
				for(int i=0; i<cells.getSize(); i++) {
					scope.append(new CGIAsm("std y+" + (cells.getId(i) + (needRestoreIReg ? -addr : offset)) + ",r" + regs[i]));
				}
				if(needRestoreIReg) moveIReg(scope, 'y', offset+addr);
				break;
			default:
				throw new CompileException("Unsupported cell type:" + cells.getType());
		}
		
		for(int i=cells.getSize(); i<usedRegsQnt; i++) scope.append(new CGIAsm("ldi r" + cells.getId(i) + ",0x00"));
	}

	public int getRegPos(byte[] regs, int offset, byte reg) {
		for(int i=offset; i<regs.length; i++) {
			if(regs[i] == reg) return i;
		}
		return -1;
	}
	
	public int getCellsPos(CGCells cells, byte reg) {
		for(int pos=0; pos<cells.getSize();pos++) {
			if(cells.getId(pos)==reg) return pos;
		}
		return -1;
	}

	public void accToField(CGScope scope, int size, int offset, CGCells cells) {
		int addr = cells.getId(0);
		boolean needRestoreIReg = Generator.this.moveIReg(scope, 'z', offset+addr, cells.getSize());
		
		scope.append(new CGIAsm("std z+" + (cells.getId(0) + (needRestoreIReg ? -addr : offset)) + ",r16"));
		if(size>1) scope.append(new CGIAsm("std z+" + (cells.getId(1) + (needRestoreIReg ? -addr : offset)) + ",r17"));
		if(size>2) scope.append(new CGIAsm("std z+" + (cells.getId(2) + (needRestoreIReg ? -addr : offset)) + ",r18"));
		if(size>3) scope.append(new CGIAsm("std z+" + (cells.getId(3) + (needRestoreIReg ? -addr : offset)) + ",r19"));
		
		if(needRestoreIReg) moveIReg(scope, 'z', offset+addr);
	}

	@Override
	public void pushAccBE(CGScope scope, int size) {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";push accum(BE)"));
		byte[] regs = getAccRegs(size);
		for(int i=size-1; i>=0; i--) {
			scope.append(new CGIAsm("push r" + regs[i]));
		}
	}
	@Override
	public void popAccBE(CGScope scope, int size) {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";pop accum(BE)"));
		byte[] regs = getAccRegs(size);
		for(int i=0; i<size; i++) scope.append(new CGIAsm("pop r" + regs[i]));
	}

	@Override
	public void emitUnary(CGScope scope, Operator op, int offset, CGCells cells) throws CompileException {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";eUnary " + op + " cells " + cells + " -> accum"));
		switch(op) {
			case PLUS:
				break;
			case MINUS:
				cellsActionAsm(scope, offset, cells, (int sn, String reg) -> new CGIAsm("com " + reg));
				cellsActionAsm(scope, offset, cells, (int sn, String reg) -> new CGIAsm(0 == sn ? "sub " + reg + ",C0x01" : "sbc " + reg + ",C0x00"));
				break;
			case BIT_NOT:
				cellsActionAsm(scope, offset, cells, (int sn, String reg) -> new CGIAsm("com " + reg));
			case PRE_INC:
			case POST_INC:
				cellsActionAsm(scope, offset, cells, (int sn, String reg) -> new CGIAsm(0 == sn ? "add " + reg + ",C0x01" : "adc " + reg + ",C0x00"));
				break;
			case PRE_DEC:
			case POST_DEC:
				cellsActionAsm(scope, offset, cells, (int sn, String reg) -> new CGIAsm(0 == sn ? "sub " + reg + ",C0x01" :	"sbc " + reg + ",C0x00"));
				break;
			case NOT:
				cellsActionAsm(scope, offset, cells, (int sn, String reg) -> new CGIAsm("com " + reg));
				break;
			default:
				throw new CompileException("CG:emitUnary: unsupported operator: " + op);
		}
	}
		
		
	// Работаем с аккумулятором!
	@Override
	public void cellsAction(CGScope scope, int offset, CGCells cells, Operator op) throws CompileException {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";accum " + op + " cells " + cells + " -> accum"));

		switch(op) {
			case PLUS:
				cellsActionAsm(scope, offset, cells, (int sn, String reg) -> new CGIAsm((0 == sn ? "add " : "adc ") + getRightOp(sn, null) + "," + reg));
				break;
			case MINUS:
				cellsActionAsm(scope, offset, cells, (int sn, String reg) -> new CGIAsm((0 == sn ? "sub " : "sbc ") + getRightOp(sn, null) + "," + reg));
				break;
			case BIT_AND:
				cellsActionAsm(scope, offset, cells, (int sn, String reg) -> new CGIAsm("and " + getRightOp(sn, null) + "," + reg));
				break;
			case BIT_OR:
				cellsActionAsm(scope, offset, cells, (int sn, String reg) -> new CGIAsm("or " + getRightOp(sn, null) + "," + reg));
				break;
			case BIT_XOR:
				cellsActionAsm(scope, offset, cells, (int sn, String reg) -> new CGIAsm("eor " + getRightOp(sn, null) + "," + reg));
				break;
			default:
				throw new CompileException("CG:cellsAction: unsupported operator: " + op);
		}
	}

	// Работаем с аккумулятором!
	// Оптиммизировано для экономии памяти, для экономии FLASH можно воспользоваться CPC инструкцией
	// В операциях сравнения резултат во флаге C, в остальных результат в аккумуляторе
	@Override
	public void constAction(CGScope scope, Operator op, long k) throws CompileException {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";accum " + op + " " + k + " -> accum"));
		CGIContainer cont = new CGIContainer("constAction:" + op);
		switch(op) {
			case PLUS:
				constActionAsm(scope, 0==k?0:~k+1, (int sn, String rOp) -> new CGIAsm((0 == sn ? "subi " : "sbci ") + getRightOp(sn, null) + "," + rOp));
				break;
			case MINUS:
				constActionAsm(scope, k, (int sn, String rOp) -> new CGIAsm((0 == sn ? "subi " : "sbci ") + getRightOp(sn, null) + "," + rOp));
			case BIT_AND:
				constActionAsm(scope, k,  (int sn, String reg) -> new CGIAsm("andi " + getRightOp(sn, null) + "," + reg));
				break;
			case BIT_OR:
				constActionAsm(scope, k, (int sn, String reg) -> new CGIAsm("ori " + getRightOp(sn, null) + "," + reg));
				break;
			case BIT_XOR:
				cont.append(new CGIAsm("push yl"));
				for(int i=accum.getSize()-1; i>=0; i--) {
					cont.append(new CGIAsm("ldi yl," + ((k>>(i*8))&0xff)));
					cont.append(new CGIAsm("eor " + getRightOp(i, null) + ",r16"));
				}
				cont.append(new CGIAsm("pop yl"));
				break;
			default:
				throw new CompileException("CG:constAction: unsupported operator: " + op);
		}
	}

	@Override
	public void constCond(CGScope scope, int offset, CGCells cells, Operator op, long k, boolean isNot, boolean isOr, CGBranchScope branchScope)
																																	throws CompileException {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";accum " + op + " " + k + " -> accum (isOr:" + isOr + ", isNot:" + isNot));
		CGIContainer cont = new CGIContainer("constCondition:" + op);
		
		if(isNot) { // Инвертируем выражение заменя AND<->OR и инвертируем операторы сравнения
			isOr = !isOr;
			switch(op) {
				case LT: op = Operator.GTE; break;
				case LTE: op = Operator.GT; break;
				case GT: op = Operator.LTE; break;
				case GTE: op = Operator.LT; break;
				case EQ: op = Operator.NEQ; break;
				case NEQ: op = Operator.EQ; break;
				default: throw new CompileException("CG:constAction: unsupported operator: " + op);
			}
		}
		
		// Задаем метку на конец блока сравнения байт в мультибайтном значении
		CGLabelScope lbScope = (0x01 == accum.getSize() ? null : new CGLabelScope(null, null, LabelNames.MULTIBYTE_CP_END, false));
		
		Character restoreIReg = null;
		int addr = 0;
		if(CGCells.Type.STACK_FRAME == cells.getType() || CGCells.Type.HEAP == cells.getType()) {
			if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) scope.append(new CGIAsm("rcall os_dispatcher_lock"));
			addr = cells.getId(0);
			if(CGCells.Type.STACK_FRAME == cells.getType()) {
				if(Generator.this.moveIReg(scope, 'y', offset+addr, cells.getSize())) restoreIReg = 'y';
			}
			else {
				if(Generator.this.moveIReg(scope, 'z', offset+addr, cells.getSize())) restoreIReg = 'z';
			}
		}

		for(int i=accum.getSize()-1; i>=0; i--) {
			switch(cells.getType()) {
				case ACC:
					cont.append(new CGIAsm("cpi r" + getAccRegs(accum.getSize())[i] + "," + ((k>>(i*8))&0xff)));
					break;
				case REG:
					cont.append(new CGIAsm("cpi r" + cells.getId(i) + "," + ((k>>(i*8))&0xff)));
					break;
				case STACK_FRAME:
					scope.append(new CGIAsm("ldd j8b_atom,y+" + (cells.getId(i) + (null!=restoreIReg ? -addr : offset))));
					cont.append(new CGIAsm("cpi j8b_atom," + ((k>>(i*8))&0xff)));
					break;
				case HEAP:
					scope.append(new CGIAsm("ldd j8b_atom,z+" + (cells.getId(i) + (null!=restoreIReg ? -addr : offset))));
					cont.append(new CGIAsm("cpi j8b_atom," + ((k>>(i*8))&0xff)));
					break;
				default:
					throw new CompileException("Unsupported cell type:" + cells.getType());
			}

			switch(op) {
				case LT:
					if(isOr) { // В связке с OR
						cont.append(new CGIAsmJump("brcs", branchScope.getEnd())); // True
						if(0!=i) {  // Старшие байты
							cont.append(new CGIAsmJump("brne", lbScope));
							lbScope.setUsed();
							// Если == - продолжаем без перехода
						}
					}
					else { // В связке с AND 
						if(0!=i) {  // Старшие байты
							cont.append(new CGIAsm("breq pc+0x02"));
						}
						cont.append(new CGIAsmJump("brcc", branchScope.getEnd())); // False
					}
					break;
				case GTE:
					if(isOr) { // В связке с OR
						cont.append(new CGIAsmJump("brcc", branchScope.getEnd())); // Если >=
						if(0!=i) {  // Старшие байты
							cont.append(new CGIAsmJump("brne", lbScope)); // Если < - продолжить
							lbScope.setUsed();
						}
					}
					else { // В связке с AND 
						cont.append(new CGIAsmJump("brcs", branchScope.getEnd())); // Если <
					}
					break;
				case GT:
					if(isOr) { // В связке с OR
						if(0!=i) {  // Старшие байты
							cont.append(new CGIAsm("breq pc+0x03")); // Если =, пропустить байт
							cont.append(new CGIAsmJump("brcc", branchScope.getEnd())); // Если >
							cont.append(new CGIAsmJump("brne", lbScope)); // Если < - продолжить
							lbScope.setUsed();
						}
						else { // Младший байт
							cont.append(new CGIAsm("breq pc+0x02")); // Если =, пропустить brcc
							cont.append(new CGIAsmJump("brcc", branchScope.getEnd())); // Если >
						}
					}
					else { // В связке с AND 
						cont.append(new CGIAsmJump("brcs", branchScope.getEnd())); // Если <
						if(0!=i) {  // Старшие байты
							cont.append(new CGIAsmJump("brne", lbScope)); // Если > - продолжить
							lbScope.setUsed();
						}
						else { // Младший байт
							cont.append(new CGIAsmJump("breq", branchScope.getEnd())); // Если =
						}
					}
					break;
				case LTE:
					if(isOr) { // В связке с OR
						cont.append(new CGIAsmJump("brcs", branchScope.getEnd())); // Если <
						if(0!=i) {  // Старшие байты
							cont.append(new CGIAsmJump("brne", lbScope)); // Если > - продолжить
							lbScope.setUsed();
						}
						else { // Младший байт
							cont.append(new CGIAsmJump("breq", branchScope.getEnd())); // Если =
						}
					}
					else { // В связке с AND 
						if(0!=i) {  // Старшие байты
							cont.append(new CGIAsm("breq pc+0x03")); // Если =, пропустить brcc
							cont.append(new CGIAsmJump("brcc", branchScope.getEnd())); // Если >
							cont.append(new CGIAsmJump("brne", lbScope)); // Если < - продолжить
							lbScope.setUsed();
						}
						else { // Младший байт
							cont.append(new CGIAsm("breq pc+0x02")); // Если =, пропустить brcc
							cont.append(new CGIAsmJump("brcc", branchScope.getEnd())); // Если >
						}
					}
					break;
				case NEQ:
					if(isOr) { // В связке с OR
						cont.append(new CGIAsmJump("brne", branchScope.getEnd()));
					}
					else { // В связке с AND 
						if(0!=i) {  //Старшие байты
							cont.append(new CGIAsmJump("brne", lbScope));
							lbScope.setUsed();
						}
						else { // Младший байт
							cont.append(new CGIAsmJump("breq", branchScope.getEnd()));
						}
					}
					break;
				case EQ:
					if(isOr) { // В связке с OR
						if(0!=i) {  //Старшие байты
							cont.append(new CGIAsmJump("brne", lbScope));
							lbScope.setUsed();
						}
						else { // Младший байт
							cont.append(new CGIAsmJump("breq", branchScope.getEnd()));
						}
					}
					else { // В связке с AND 
						cont.append(new CGIAsmJump("brne", branchScope.getEnd()));
					}
					break;
				default:
					throw new CompileException("CG:constAction: unsupported operator: " + op);
			}
			
			if(null != restoreIReg) {
				moveIReg(scope, restoreIReg, offset+addr);
				if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) scope.append(new CGIAsm("rcall os_dispatcher_unlock"));
			}
		}
		if(0x01 != accum.getSize()) cont.append(lbScope);
		scope.append(cont);
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
	
	public void cellsActionAsm(CGScope scope, int offset, CGCells cells, CGActionHandler handler) throws CompileException {
		switch(cells.getType()) {
			case REG:
				for(int i=0; i<cells.getSize(); i++) scope.append(handler.action(i, "r" + cells.getId(i)));
				break;
			case STACK_FRAME:
				int addr = cells.getId(0);
				boolean needRestoreIReg = Generator.this.moveIReg(scope, 'y', offset+addr, cells.getSize());
				scope.append(new CGIAsm("push zl"));
				for(int i=0; i<cells.getSize(); i++) {
					scope.append(new CGIAsm("ldd zl,y+" + (cells.getId(i) + (needRestoreIReg ? -addr : offset))));
					scope.append(handler.action(i, "zl"));
				}
				scope.append(new CGIAsm("pop zl"));
				if(needRestoreIReg) moveIReg(scope, 'y', offset+addr);
				break;
			case HEAP:
				addr = cells.getId(0);
				needRestoreIReg = Generator.this.moveIReg(scope, 'z', offset+addr, cells.getSize());
				scope.append(new CGIAsm("push yl"));
				for(int i=0; i<cells.getSize(); i++) {
					scope.append(new CGIAsm("ldd yl,z+" + (cells.getId(i) + (needRestoreIReg ? -addr : offset))));
					scope.append(handler.action(i, "yl"));
				}
				scope.append(new CGIAsm("pop yl"));
				if(needRestoreIReg) moveIReg(scope, 'z', offset+addr);
				break;
			case STACK:
				if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) {
					scope.append(new CGIAsm("rcall os_dispatcher_lock"));
				}
				//Значение лежит на вершине стека(BigEndian)
				for(int i=0; i<cells.getSize(); i++) {
					scope.append(new CGIAsm("pop j8b_atom"));
					scope.append(handler.action(i, "j8b_atom"));
				}
				if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) {
					scope.append(new CGIAsm("rcall os_dispatcher_unlock"));
				}
				break;

			default:
				throw new CompileException("Unsupported cell type:" + cells.getType());
		}
	};
	
	public void constActionAsm(CGScope scope, long k, CGActionHandler handler) throws CompileException {
		for(int i=0; i<accum.getSize(); i++) {
			scope.append(handler.action(i, Long.toString((k>>(i*8))&0xff)));
		}
	};


	// TODO Лучше вынести данный код в виде RTOS функции.
	@Override
	public CGIContainer eNewInstance(int size, CGLabelScope iidLabel, VarType type, boolean launchPoint, boolean canThrow) throws CompileException {
		CGIContainer result = new CGIContainer();
		if(VERBOSE_LO <= verbose) result.append(new CGIText(launchPoint ? ";Launch " : (";eNewInstance '" + type + "', heap size:" + size)));
		RTOSFeatures.add(RTOSFeature.OS_FT_DRAM); //TODO сейчас реализуется классвовая модель, но что если нужно собрать просто функцию?
		result.append(new CGIAsm("ldi r16," + (size&0xff)));
		if(0x02==getRefSize()) result.append(new CGIAsm("ldi r17," + ((size>>0x08)&0xff)));
		result.append(new CGIAsm("rcall os_dram_alloc"));
		int offset=0;
		result.append(new CGIAsm("std z+" + offset++ +",r16"));
		if(0x02==getRefSize()) {
			result.append(new CGIAsm("std z+" + offset++ +",r17"));
		}
		//Запись кол-ва ссылок
		result.append(new CGIAsm("std z+" + offset++ + (launchPoint ? ",c0xff" : ",c0x00")));
		//Запись блока ид типов класса и интерфейсов
		result.append(new CGIAsmLdLabel("ldi r16,low", iidLabel.getName(), "*2"));
		result.append(new CGIAsm("std z+" + offset++ +",r16"));
		result.append(new CGIAsmLdLabel("ldi r16,high", iidLabel.getName(), "*2"));
		result.append(new CGIAsm("std z+" + offset++ +",r16"));

		int stackFrameSize = size-offset;
		if(0x00!=stackFrameSize) {
			RTOSLibs.CLEAR_FIELDS.setRequired();
			result.append(new CGIAsm("rcall j8bproc_clear_fields_nr"));
		}
		return result;
	}

	@Override
	public void eFree(Operand op) {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}


	@Override
	public void invokeClassMethod(CGScope scope, String className, String methodName, VarType type, VarType[] types, CGLabelScope lbScope, boolean isInternal)																														throws CompileException {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";invokeClassMethod " + type + " " + className + "." + methodName));
		if(0 != types.length || !("getClassTypeId".equals(methodName) || "getClassId".equals(methodName))) {
			if(0!=types.length || !isInternal) scope.prepend(pushHeapReg(null, isInternal));
			scope.append(new CGIAsmJump(INSTR_CALL, lbScope));
			if(def_sph) scope.append(new CGIAsm("lds zl,SREG"));
			if(def_sph) scope.append(new CGIAsm("cli"));
			scope.append(new CGIAsm("sts SPL,yl"));
			if(def_sph) scope.append(new CGIAsm("sts SPH,yh"));
			if(def_sph) scope.append(new CGIAsm("sts SREG,zl"));
			if(0!=types.length || !isInternal) scope.append(popHeapReg(null, isInternal));
		}
	}
	@Override
	public void invokeInterfaceMethod(CGScope scope, String className, String methodName, VarType type, VarType[] types, VarType ifaceType, int methodSN)																										throws CompileException {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";invokeInterfaceMethod " + type + " " + className + "." + methodName));
		RTOSLibs.INVOKE_METHOD.setRequired();
		scope.append(new CGIAsm("ldi r16," + ifaceType.getId()));
		scope.append(new CGIAsm("ldi r17," + methodSN));
		scope.append(new CGIAsmJump(INSTR_CALL, "j8bproc_invoke_method_nr"));
	}
	@Override
	public void invokeNative(CGScope scope, String className, String methodName, String paramTypes, VarType type, Operand[] ops) throws CompileException {
		String methodQName = className + "." + methodName + (null != paramTypes ? " " + paramTypes : "");
		if(VERBOSE_LO <= verbose) {
			scope.append(new CGIText(";invokeNative " + type + " " + methodQName + (null != ops ? ", params:" + StrUtils.toString(ops) : "")));
		}
		
		if(methodQName.equals("System.setParam [byte, byte]")) {
			SystemParam sp = SystemParam.values()[(int)getNum(ops[0x00])];
			switch(sp) {
				case CORE_FREQ:
					params.put(sp, (int)getNum(ops[0x01]));
					break;
				case STDOUT_PORT:
					params.put(sp, (int)getNum(ops[0x01]));
					break;
				case MULTITHREADING:
					if(0x00 != (int)getNum(ops[0x01])) {
						RTOSFeatures.add(RTOSFeature.OS_FT_MULTITHREADING);
					}
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
				throw new CompileException("CG: InvokeNative, not found native binding for method: " + methodQName);
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

			CGBlockScope bScope = scope.getBlockScope();
			
			if(null != ops) {
				for(int i=0; i<ops.length; i++) {
					byte[] regs = regIds[i];
					if(0 == regs.length || 4 < regs.length) throw new CompileException("CG: Invalid parameters for invoke method " + methodQName);

					//TODO scope.getBlockScope().putUsedRegs(regs);
					
					long value = 0;
					Operand op = ops[i];
					if(OperandType.LOCAL_RES == op.getOperandType()) {
						CGVarScope vScope = getLocal(scope, (int)op.getValue());
						if(null != vScope) {
							if(!vScope.isConstant()) {
								if(regs.length > vScope.getSize()) { //TODO Переменная может быть больше размером(использовано сокращение)
									throw new CompileException("CG: InvokeNative: params qnt not equlas regs qnt in method: " + methodQName);
								}

								for(int f=0; f<regs.length;f++) if(!bScope.isFreeReg(regs[f])) scope.append(new CGIAsm("push r"+regs[f]));
								if(VERBOSE_LO <= verbose) scope.append(new CGIText("\t;load method param"));
								cellsToRegs(scope, vScope.getStackOffset(), vScope.getCells(), regs);
								for(int f=regs.length-1; f>=0;f--) if(!bScope.isFreeReg(regs[f])) scope.append(new CGIAsm("pop r"+regs[f]));
							}
							else {
								scope.append(new CGIAsmLdLabel("ldi r" + regs[0] + ",low", vScope.getDataSymbol().getLabel(), "*2"));
								scope.append(new CGIAsmLdLabel("ldi r" + regs[1] + ",high", vScope.getDataSymbol().getLabel(), "*2"));
							}
						}
						else {
							throw new CompileException("CG: InvokeNative: local not found resId: " + op.getValue());
						}
					}
					else if(OperandType.ACCUM == op.getOperandType()) {
						byte[] accRegs=getAccRegs(regs.length);
						for(int f=0; f<regs.length;f++) {
							if(!bScope.isFreeReg(regs[f])) scope.append(new CGIAsm("push r"+regs[f]));
//							cells[f] = new CGCellsScope(CGCellsScope.Type.REG, accRegs[f]);
						}
						if(VERBOSE_LO <= verbose) scope.append(new CGIText("\t;load method param"));
						cellsToRegs(scope, 0, new CGCells(CGCells.Type.REG, accRegs), regs);
						for(int f=regs.length-1; f>=0;f--) if(!bScope.isFreeReg(regs[f])) scope.append(new CGIAsm("pop r"+regs[f]));
					}
					else if(OperandType.FLASH_RES == op.getOperandType()) {
						if(0x00 == regs.length || 0x02<regs.length) {
							throw new CompileException("Invalid registers quantity for flash constant");
						}
						if(0x00<regs.length) {
							scope.append(new CGIAsmLdLabel("ldi r" + regs[0] + ",low", flashData.get((Integer)op.getValue()).getLabel(), "*2"));
						}
						if(0x01<regs.length) {
							scope.append(new CGIAsmLdLabel("ldi r" + regs[1] + ",high", flashData.get((Integer)op.getValue()).getLabel(), "*2"));
						}
					}
					else {
						value = getNum(ops[i]);
						if(VERBOSE_LO <= verbose) scope.append(new CGIText("\t;load method param"));
						loadRegsConst(scope, regs, value);
					}
				}
			}
			scope.append(new CGIAsm("rcall "+ nb.getRTOSFunction()));
		}
	}
	
	@Override
	public void updateRefCount(CGScope scope, int offset, CGCells cells, boolean isInc) throws CompileException {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";refCount" + (isInc ? "++" : "--") + " for " + cells));
	
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) scope.append(new CGIAsm("rcall os_dispatcher_lock"));
		scope.append(new CGIAsm("push zl"));
		scope.append(new CGIAsm("push zh"));

		switch(cells.getType()) {
			case REG:
				scope.append(new CGIAsm("mov zl,r" + cells.getId(0x00)));
				if(0x01 == getRefSize()) {
					scope.append(new CGIAsm("ldi zh,0x00"));
				}
				else {
					scope.append(new CGIAsm("mov zh,r" + cells.getId(0x01)));
				}
				break;
			case STACK_FRAME:
				int addr = cells.getId(0);
				boolean needRestoreIReg = Generator.this.moveIReg(scope, 'y', offset+addr, cells.getSize());
				scope.append(new CGIAsm("ldd zl,y+" + (cells.getId(0) + (needRestoreIReg ? -addr : offset))));
				if(0x01 == getRefSize()) {
					scope.append(new CGIAsm("ldi zh,0x00"));
				}
				else {
					scope.append(new CGIAsm("ldd zh,y+" + (cells.getId(1) + (needRestoreIReg ? -addr : offset))));					
				}
				if(needRestoreIReg) moveIReg(scope, 'y', offset+addr);
				break;
			case HEAP:
				addr = cells.getId(0);
				needRestoreIReg = Generator.this.moveIReg(scope, 'z', offset+addr, cells.getSize());
				if(0x01 == getRefSize()) {
					scope.append(new CGIAsm("ldd zl,z+" + (cells.getId(0) + (needRestoreIReg ? -addr : offset))));
					scope.append(new CGIAsm("ldi zh,0x00"));
				}
				else {
					scope.append(new CGIAsm("ldd j8b_atom,z+" + (cells.getId(0) + (needRestoreIReg ? -addr : offset))));
					scope.append(new CGIAsm("ldd zh,z+" + (cells.getId(1) + (needRestoreIReg ? -addr : offset))));
					scope.append(new CGIAsm("mov zl,j8b_atom"));
				}
				break;
			default:
				throw new CompileException("Unsupported cell type:" + cells.getType());
		}

		scope.append(new CGIAsm("rcall j8bproc_" + (isInc ? "inc" : "dec") + "_refcount"));
		scope.append(new CGIAsm("pop zh"));
		scope.append(new CGIAsm("pop zl"));
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) scope.append(new CGIAsm("rcall os_dispatcher_unlock"));
	}
	
	@Override
	public void eInstanceof(CGScope scope, VarType type) throws CompileException {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";eInstanceOf '" + type + "'"));
		RTOSLibs.INCTANCEOF.setRequired();
		// TODO не сохраняем Z, так как обращение к полю/переменной уже изменило Z, но нужно учитывать работу с this
//		scope.append(new CGIAsm("push r17")); В теории не нужно, если и используется аккумулятор, то только в выражениях с булевой логикой, т.е. только r16
		scope.append(new CGIAsm("ldi r17,"+type.getId()));
		scope.append(new CGIAsm("rcall j8bproc_instanceof_nr"));
//		scope.append(new CGIAsm("pop r17"));
	}
	
	@Override
	public void eIf(CGBranchScope branchScope, CGScope condScope, CGBlockScope thenScope, CGBlockScope elseScope) throws CompileException {
		if(VERBOSE_LO <= verbose) {
			condScope.prepend(new CGIText(";eIf"));
		}
		
		if(null == elseScope) {
			thenScope.append(branchScope.getEnd());
		}
		else {
			elseScope.prepend(branchScope.getEnd());
			thenScope.append(new CGIAsmJump(INSTR_JUMP, elseScope.getELabel()));
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
	public void eWhile(CGScope scope, CGScope condScope, CGBlockScope bodyScope) throws CompileException {
		if(VERBOSE_HI == verbose) scope.append(new CGIText(";CG: while, cond:" + condScope + ", body:" + bodyScope));
		
		//TODO нужен CGBranchScope
		CGLabelScope lbLoop = new CGLabelScope(null, null, LabelNames.LOOP, true);
		CGLabelScope lbEndLoop = new CGLabelScope(null, null, LabelNames.LOOP_END, true);
		
		bodyScope.prepend(lbLoop);
		bodyScope.append(new CGIAsmJump(INSTR_JUMP, lbLoop));
		bodyScope.append(lbEndLoop);
	}
	
	@Override
	public CGIContainer eReturn(CGScope scope, int argsSize, int varsSize, int retSize) throws CompileException {
		CGIContainer result = new CGIContainer();
		if(0 != retSize) result.append(accCast(null, retSize));


		if(VERBOSE_LO <= verbose) result.append(new CGIText(";return, argsSize:" + argsSize + ", varsSize:" + varsSize + ", retSize:" + retSize));
		if(0!=varsSize) {
			result.append(new CGIAsm("subi yl,low(" + varsSize*(-1) + ")"));
			if(def_sph) result.append(new CGIAsm("sbci yh,high(" + varsSize*(-1) + ")"));
		}
		result.append(new CGIAsm("movw zl,yl")); //TODO проверить смещения
//		if(0!=argsSize) {
			argsSize+=2; // Адрес возврата
			result.append(new CGIAsm("subi yl,low(" + argsSize*(-1) + ")")); 
			if(def_sph) result.append(new CGIAsm("sbci yh,high(" + argsSize*(-1) + ")"));
//		}
		
		if(0!=argsSize || 0!=varsSize) {
			result.append(new CGIAsm("ijmp"));
		}
		else {
			result.append(new CGIAsm("ret"));
		}
		
		
		if(null != scope) scope.append(result);
		return result;
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
	public int getCallSize() {
		return def_call ? 0x02 : 0x01;
	}

	@Override
	public ArrayList<RegPair> buildRegsPool() {
		ArrayList<RegPair> result = new ArrayList<>();
		for(byte b : usableRegs) {result.add(new RegPair(b));}
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
	
	public byte[] getTempRegs(int size) {
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

	public boolean moveIReg(CGScope scope, char iReg, int offset, int size) {
		if((offset+size) >= 64) {
			scope.append(new CGIAsm("subi " + iReg + "l,low(-" + offset + ")")); //TODO проверить сложение
			scope.append(new CGIAsm("sbci " + iReg + "h,high(-" + offset + ")"));
			return true;
		}
		return false;
	}
	@Override
	public CGIContainer moveIReg(CGScope scope, char iReg, int offset) {
		CGIContainer cont = new CGIContainer();
		cont.append(new CGIAsm("subi " + iReg + "l,low(" + offset + ")"));
		cont.append(new CGIAsm("sbci " + iReg + "h,high(" + offset + ")"));
		if(null != scope) scope.append(cont);
		return cont;
	}
}
