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
import ru.vm5277.common.cg.CodeOptimizer;
import ru.vm5277.common.cg.scopes.CGVarScope;
import ru.vm5277.common.cg.OperandType;
import ru.vm5277.common.cg.RegPair;
import ru.vm5277.common.cg.items.CGIAsmIReg;
import ru.vm5277.common.cg.items.CGIAsmJump;
import ru.vm5277.common.cg.items.CGIAsmLdLabel;
import ru.vm5277.common.cg.items.CGIConstrInit;
import ru.vm5277.common.cg.items.CGItem;
import ru.vm5277.common.cg.scopes.CGCellsScope;
import ru.vm5277.common.cg.scopes.CGBranchScope;
import ru.vm5277.common.cg.scopes.CGClassScope;
import ru.vm5277.common.cg.scopes.CGFieldScope;
import ru.vm5277.common.cg.scopes.CGLabelScope;
import ru.vm5277.common.cg.scopes.CGMethodScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;

// TODO Кодогенератор НЕ ДОЛЖЕН выполнять работу оптимизатора, т.е. не нужно формировать movw вместо двух mov и тому подобное. Так как оптимизатор справится с
// этой задачей лучше, а кодогенератор может ему помешать.

public class Generator extends CodeGenerator {
	private	final	static	String				VERSION				= "0.1";
	private	final	static	byte[]				usableRegs			= new byte[]{20,21,22,23,24,25,26,27,   19,18,17}; //Используем регистры
	//private	final	static	byte[]				usableRegs	= new byte[]{}; 	//Без регистров для локальных переменных
	//private	final	static	byte[]				usableRegs	= new byte[]{20}; 	//С одним регистром
	//private	final	static	byte[]				usableRegs	= new byte[]{20,21,22,23}; 	//С 4 регистрами
	private	final	static	boolean				def_sph				= true; // TODO генератор avr должен знать характеристики МК(чтобы не писать асм код с .ifdef)
	private	final	static	boolean				def_call			= true; // TODO генератор avr должен знать характеристики МК(чтобы не писать асм код с .ifdef)
	private	final	static	String				INSTR_JUMP			= "rjmp";
	private	final	static	String				INSTR_CALL			= "rcall";
	private	final	static	int					MIN_IREG_OFFSET		= 0;
	private	final	static	int					MAX_IREG_OFFSET		= 63;
	private	final			int					refSize;
	
	public Generator(String platform, Map<String, NativeBinding> nbMap, Map<SystemParam, Object> params) {
		super(platform, nbMap, params);
		
		refSize = 0x02; //TODO для МК с памятью <= 256 нужно возвращать 0x01;
		CLASS_HEADER_SIZE = (0x02==refSize ? 0x05 : 0x04);
		
		optimizer = new Optimizer();
		// Собираем RTOS
	}
	
	@Override
	public CGIContainer stackAlloc(boolean firstBlock, int argsSize, int varsSize) {
		CGIContainer cont = new CGIContainer();

		if(VERBOSE_LO <= verbose) cont.append(new CGIText(";alloc stack, size:" + varsSize));

		if(firstBlock) {
			cont.append(new CGIAsm("push yl")); //TODO проверить адрес ячейки
			if(def_sph) cont.append(new CGIAsm("push yh")); // Если SPH отсутствует, то yh всегда должен быть равен 0
			cont.append(new CGIAsm("lds yl,SPL")); //TODO проверить адрес ячейки
			if(def_sph) cont.append(new CGIAsm("lds yh,SPH"));
//			if(argsSize<MAX_IREG_OFFSET) { //TODO проверить на граничное значение
				 //Стек растет вниз, берем максимально минимальный адрес с доступностью переменных через y+XX
//				cont.append(new CGIAsm("sbiw yl," + (MAX_IREG_OFFSET-(argsSize+0x04)))); //0x04 - push y, адрес возврата и учет позиции SP
//			}
		}

		// Не просто выделяем память, также инициализируем ее нулями
		if(varsSize<5) {
			for(int i=0; i<varsSize; i++) {
				cont.append(new CGIAsm("push c0x00"));
			}
		}
		else {
			cont.append(new CGIAsm("ldi r16,low(" + varsSize + ")"));
			cont.append(new CGIAsm("ldi r17,high(" + varsSize + ")"));
			cont.append(new CGIAsm("push c0x00"));
			cont.append(new CGIAsm("dec r16"));
			cont.append(new CGIAsm("brne pc-0x02"));
			cont.append(new CGIAsm("dec r17"));
			cont.append(new CGIAsm("brne pc-0x04"));
		}
		return cont;
	}

//Продумать освобождение памяти с учетом normalizeIRegConst
	@Override
	public CGIContainer blockFree(int size) {
		CGIContainer cont = new CGIContainer();
		
		if(0==size) return cont;
		if(VERBOSE_LO <= verbose) cont.append(new CGIText(";block free, size:" + size));
		if((def_sph && size<9) || (!def_sph && size<3)) {
			for(int i=0; i<size; i++) {
				cont.append(new CGIAsm("pop j8b_atom"));
			}
		}
		else {
/*			result.append(new CGIAsm("subi yl,low(" + size*(-1) + ")"));
			if(def_sph) result.append(new CGIAsm("sbci yh,high(" + size*(-1) + ")"));
			if(def_sph) result.append(new CGIAsm("lds zl,SREG"));
			if(def_sph) result.append(new CGIAsm("cli"));
			result.append(new CGIAsm("sts SPL,yl"));
			if(def_sph) result.append(new CGIAsm("sts SPH,yh"));
			if(def_sph) result.append(new CGIAsm("sts SREG,zl"));*/

			if(def_sph) cont.append(new CGIAsm("cli"));
			cont.append(new CGIAsm("lds _SPL,SPL"));
			if(def_sph) cont.append(new CGIAsm("lds _SPH,SPH"));
			cont.append(new CGIAsm("subi _SPL,low(" + size*(-1) + ")")); // Оптимизатор по возможности приведет к интрукции adiw
			if(def_sph) cont.append(new CGIAsm("sbci _SPH,high(" + size*(-1) + ")"));
			cont.append(new CGIAsm("sts SPL,_SPL"));
			if(def_sph) cont.append(new CGIAsm("sts SPH,_SPH"));
			if(def_sph) cont.append(new CGIAsm("sei")); // На верхнем уровне прерывания всегда резешены, проверка(созранение бита) не требуется
		}
		return cont;
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
	
	/**
	 * pushConst используетя для вызова метода(передачи констант)
	 * @param scope - область видимости для генерации кода
	 * @param size - необходимый размер в стеке
	 * @param value - значение константы
	 * @return 
	 */
	@Override
	public CGIContainer pushConst(CGScope scope, int size, long value) {
		CGIContainer cont = new CGIContainer();
		if(VERBOSE_LO <= verbose) cont.append(new CGIText(";push const '" + value + "'"));
		for(int i=0; i<size; i++) {
			cont.append(new CGIAsm("ldi r30," + (value&0xff)));
			cont.append(new CGIAsm("push r30"));
			value >>= 0x08;
		}
		if(null != scope) scope.append(cont);
		return cont;
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
	 * @return 
	 * @throws CompileException
	 */
	@Override
	public CGIContainer pushCells(CGScope scope, int size, CGCells cells) throws CompileException {
		CGIContainer cont = new CGIContainer();
		if(VERBOSE_LO <= verbose) cont.append(new CGIText(";push cells: " + cells));

		switch(cells.getType()) {
			case REG:
				for(int i=0; i<cells.getSize(); i++) cont.append(new CGIAsm("push r" + cells.getId(i)));
				break;
			case ARGS:
			case STACK_FRAME:
				for(int i=0; i<cells.getSize(); i++) {
					cont.append(new CGIAsmIReg(	'y', "ldd j8b_atom,y+", cells.getId(i), "", getCurBlockScope(scope), (CGBlockScope)cells.getObject(),
												CGCells.Type.ARGS == cells.getType()));
					cont.append(new CGIAsm("push j8b_atom"));
				}
				break;
			case HEAP:
				for(int i=0; i<cells.getSize(); i++) {
					cont.append(new CGIAsmIReg('z', "ldd j8b_atom,z+", cells.getId(i), ""));
					cont.append(new CGIAsm("push j8b_atom"));
				}
				break;
			default:
				throw new CompileException("Unsupported cell type:" + cells.getType());
		}
		
		for(int i=cells.getSize(); i<size; i++) {
			cont.append(new CGIAsm("push c0x00"));
		}
		
		if(null!=scope) scope.append(cont);
		return cont;
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
				regsToVar(scope, getAccRegs(0x04), size, vScope.getCells());
			}
		}
		else if(cScope instanceof CGFieldScope) {
			CGFieldScope fScope = (CGFieldScope)cScope;
			if(VERBOSE_LO <= verbose) scope.append(new CGIText(";accum->field " + fScope.getName()));
			if(!fScope.isStatic()) {
				accToField(scope, size, fScope.getCells());
			}
		}
		else throw new CompileException("Unsupported scope:" + cScope);
	}
	
	@Override
	public void setHeapReg(CGScope scope, CGCells cells) throws CompileException {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";setHeap " + cells));
		cellsToRegs(scope, cells, getTempRegs(getCallSize()));
		//cellsActionAsm(scope, offset, cells, (int sn, String rOp) -> new CGIAsm("mov " + (0==sn ? "zl" : "zh") + "," + rOp));
	}

	@Override
	public void cellsToAcc(CGScope scope, CGCellsScope cScope) throws CompileException {
		accum.setSize(cScope.getSize());
		if(cScope instanceof CGVarScope) {
			CGVarScope vScope = (CGVarScope)cScope;
			if(!vScope.isConstant()) {
				if(VERBOSE_LO <= verbose) scope.append(new CGIText(";var '" + vScope.getName() + "'->accum"));
				cellsToRegs(scope, vScope.getCells(), getAccRegs(cScope.getSize()));
			}
		}
		else if(cScope instanceof CGFieldScope) {
			CGFieldScope fScope = (CGFieldScope)cScope;
			if(VERBOSE_LO <= verbose) scope.append(new CGIText(";field '" + fScope.getName() + "'->accum"));
			cellsToRegs(scope, fScope.getCells(), getAccRegs(cScope.getSize()));
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
	public CGIContainer call(CGScope scope, CGLabelScope lScope) throws CompileException {
		CGIContainer result = new CGIContainer();
		result.append(new CGIAsmJump(INSTR_CALL, lScope));
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

	public void cellsToRegs(CGScope scope, CGCells cells, byte[] registers) throws CompileException {
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
			case ARGS:
				for(int i=0; i<cells.getSize(); i++) {
					scope.append(new CGIAsmIReg('y', "ldd r" + registers[i] + ",y+", cells.getId(i), "", getCurBlockScope(scope),
												(CGBlockScope)cells.getObject(), CGCells.Type.ARGS == cells.getType()));
				}
				break;
			case HEAP:
				for(int i=0; i<cells.getSize(); i++) {
					scope.append(new CGIAsmIReg('z', "ldd r" + registers[i] + ",z+", cells.getId(i), ""));
				}
				break;
			default:
				throw new CompileException("Unsupported cell type:" + cells.getType());
		}

		for(int i=cells.getSize(); i<registers.length; i++) {
			scope.append(new CGIAsm("ldi r" + registers[i] + ",0x00"));
		}
	}

	@Override
	public CGIContainer constToCells(CGScope scope, long value, CGCells cells) throws CompileException {
		CGIContainer cont = new CGIContainer();
		if(VERBOSE_LO <= verbose) cont.append(new CGIText(";const '" + value + "'->cells " + cells));
		switch(cells.getType()) {
			case REG:
				for(int i=0; i<cells.getSize(); i++) cont.append(new CGIAsm("ldi r" + cells.getId(i)+ "," + ((value>>(i*8))&0xff)));
				break;
			case ARGS:
			case STACK_FRAME:
				CGIContainer _cont = new CGIContainer();
				//cont.append(new CGIAsm("push zl")); //Похоже эта опреация не используется в обработке выражений, а значит r16 свободен
				for(int i=0; i<cells.getSize(); i++) {
					_cont.append(new CGIAsm("ldi r16," + ((value>>(i*8))&0xff)));
					_cont.append(new CGIAsmIReg('y', "std y+", cells.getId(i), ",r16", getCurBlockScope(scope), (CGBlockScope)cells.getObject(),
												CGCells.Type.ARGS == cells.getType()));
				}
				//cont.append(new CGIAsm("pop zl"));
				cont.append(_cont);
				break;
			case HEAP:
				_cont = new CGIContainer();
				//cont.append(new CGIAsm("push yl")); //Похоже эта опреация не используется в обработке выражений, а значит r16 свободен
				for(int i=0; i<cells.getSize(); i++) {
					_cont.append(new CGIAsm("ldi r16," + ((value>>(i*8))&0xff)));
					_cont.append(new CGIAsmIReg('z', "std z+", cells.getId(i), ",r16"));
				}
				//cont.append(new CGIAsm("pop yl"));
				cont.append(_cont);
				break;
			default:
				throw new CompileException("Unsupported cell type:" + cells.getType());
		}
		return cont;
	}

	/**
	 * regsToVar записывает регистры в ячейки локальной переменной 
	 * @param scope - область видимости для генерации кода
	 * @param regs - полный набор регистров переменной/поля/аккумулятора
	 * @param usedRegsQnt - количество действительно используемых регистров, в не используемые будет записан 0x00
	 * @param cells - ячейки указывающие источник данных
	 * @throws CompileException
	 */
	public void regsToVar(CGScope scope, byte[] regs, int usedRegsQnt, CGCells cells) throws CompileException {
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
							//continue; //TODO проверить
						}
						scope.append(new CGIAsm("mov r" + dstReg + ",r" + srcReg));
					}
				}
				for(Byte reg : popRegs) scope.append(new CGIAsm("pop r" + reg));
				break;
			case ARGS:
			case STACK_FRAME:
				for(int i=0; i<cells.getSize(); i++) {
					scope.append(new CGIAsmIReg('y', "std y+", cells.getId(i), ",r" + regs[i], getCurBlockScope(scope),
												(CGBlockScope)cells.getObject(), CGCells.Type.ARGS == cells.getType()));
				}
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

	public void accToField(CGScope scope, int size, CGCells cells) {
		scope.append(new CGIAsmIReg('z', "std z+", cells.getId(0), ",r16"));
		if(size>1) scope.append(new CGIAsmIReg('z', "std z+", cells.getId(1), ",r17"));
		if(size>2) scope.append(new CGIAsmIReg('z', "std z+", cells.getId(2), ",r18"));
		if(size>3) scope.append(new CGIAsmIReg('z', "std z+", cells.getId(3), ",r19"));
	}

	@Override
	public CGIContainer pushAccBE(CGScope scope, int size) {
		CGIContainer result = new CGIContainer();
		if(VERBOSE_LO <= verbose) result.append(new CGIText(";push accum(BE)"));
		byte[] regs = getAccRegs(size);
		for(int i=size-1; i>=0; i--) {
			result.append(new CGIAsm("push r" + regs[i]));
		}
		if(null != scope) scope.append(result);
		return result;
	}
	@Override
	public void popAccBE(CGScope scope, int size) {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";pop accum(BE)"));
		byte[] regs = getAccRegs(size);
		for(int i=0; i<size; i++) scope.append(new CGIAsm("pop r" + regs[i]));
	}

	@Override
	public void emitUnary(CGScope scope, Operator op, CGCells cells) throws CompileException {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";eUnary " + op + " cells " + cells + " -> accum"));
		switch(op) {
			case PLUS:
				break;
			case MINUS:
				cellsActionAsm(scope, cells, (int sn, String reg) -> new CGIAsm("com " + reg));
				cellsActionAsm(scope, cells, (int sn, String reg) -> new CGIAsm(0 == sn ? "sub " + reg + ",C0x01" : "sbc " + reg + ",C0x00"));
				break;
			case BIT_NOT:
				cellsActionAsm(scope, cells, (int sn, String reg) -> new CGIAsm("com " + reg));
			case PRE_INC:
			case POST_INC:
				cellsActionAsm(scope, cells, (int sn, String reg) -> new CGIAsm(0 == sn ? "add " + reg + ",C0x01" : "adc " + reg + ",C0x00"));
				break;
			case PRE_DEC:
			case POST_DEC:
				cellsActionAsm(scope, cells, (int sn, String reg) -> new CGIAsm(0 == sn ? "sub " + reg + ",C0x01" :	"sbc " + reg + ",C0x00"));
				break;
			case NOT:
				cellsActionAsm(scope, cells, (int sn, String reg) -> new CGIAsm("com " + reg));
				break;
			default:
				throw new CompileException("CG:emitUnary: unsupported operator: " + op);
		}
	}
		
		
	// Работаем с аккумулятором!
	@Override
	public void cellsAction(CGScope scope, CGCells cells, Operator op) throws CompileException {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";accum " + op + " cells " + cells + " -> accum"));

		switch(op) {
			case PLUS:
				cellsActionAsm(scope, cells, (int sn, String reg) -> new CGIAsm((0 == sn ? "add " : "adc ") + getRightOp(sn, null) + "," + reg));
				break;
			case MINUS:
				cellsActionAsm(scope, cells, (int sn, String reg) -> new CGIAsm((0 == sn ? "sub " : "sbc ") + getRightOp(sn, null) + "," + reg));
				break;
			case BIT_AND:
				cellsActionAsm(scope,  cells, (int sn, String reg) -> new CGIAsm("and " + getRightOp(sn, null) + "," + reg));
				break;
			case BIT_OR:
				cellsActionAsm(scope,  cells, (int sn, String reg) -> new CGIAsm("or " + getRightOp(sn, null) + "," + reg));
				break;
			case BIT_XOR:
				cellsActionAsm(scope,  cells, (int sn, String reg) -> new CGIAsm("eor " + getRightOp(sn, null) + "," + reg));
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
		CGIContainer cont = new CGIContainer();
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
	public void constCond(CGScope scope, CGCells cells, Operator op, long k, boolean isNot, boolean isOr, CGBranchScope branchScope)
																																	throws CompileException {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";accum " + op + " " + k + " -> accum (isOr:" + isOr + ", isNot:" + isNot));
		CGIContainer cont = new CGIContainer();
		
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
		
		for(int i=accum.getSize()-1; i>=0; i--) {
			switch(cells.getType()) {
				case ACC:
					cont.append(new CGIAsm("cpi r" + getAccRegs(accum.getSize())[i] + "," + ((k>>(i*8))&0xff)));
					break;
				case REG:
					cont.append(new CGIAsm("cpi r" + cells.getId(i) + "," + ((k>>(i*8))&0xff)));
					break;
				case ARGS:
				case STACK_FRAME:
					scope.append(new CGIAsmIReg('y', "ldd j8b_atom,y+", cells.getId(i), "", getCurBlockScope(scope), (CGBlockScope)cells.getObject(),
												CGCells.Type.ARGS == cells.getType()));
					cont.append(new CGIAsm("cpi j8b_atom," + ((k>>(i*8))&0xff)));
					break;
				case HEAP:
					scope.append(new CGIAsmIReg('z', "ldd j8b_atom,z+",cells.getId(i), ""));
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
	
	public void cellsActionAsm(CGScope scope, CGCells cells, CGActionHandler handler) throws CompileException {
		switch(cells.getType()) {
			case REG:
				for(int i=0; i<cells.getSize(); i++) scope.append(handler.action(i, "r" + cells.getId(i)));
				break;
			case ARGS:
			case STACK_FRAME:
				scope.append(new CGIAsm("push zl"));
				for(int i=0; i<cells.getSize(); i++) {
					scope.append(new CGIAsmIReg('y', "ldd zl,y+", cells.getId(i), "", getCurBlockScope(scope), (CGBlockScope)cells.getObject(),
												CGCells.Type.ARGS == cells.getType()));
					scope.append(handler.action(i, "zl"));
				}
				scope.append(new CGIAsm("pop zl"));
				break;
			case HEAP:
				scope.append(new CGIAsm("push yl"));
				for(int i=0; i<cells.getSize(); i++) {
					scope.append(new CGIAsmIReg('z', "ldd yl,z+", cells.getId(i), ""));
					scope.append(handler.action(i, "yl"));
				}
				scope.append(new CGIAsm("pop yl"));
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
	public CGIContainer eNewInstance(int heapSize, CGLabelScope iidLabel, VarType type, boolean launchPoint, boolean canThrow) throws CompileException {
		CGIContainer result = new CGIContainer();
		if(VERBOSE_LO <= verbose) result.append(new CGIText(launchPoint ? ";Launch " : (";eNewInstance '" + type + "', heap size:" + heapSize)));
		RTOSFeatures.add(RTOSFeature.OS_FT_DRAM);
		result.append(new CGIAsm("ldi r16,low(" + heapSize + ")"));
		result.append(new CGIAsm("ldi r17,high(" + heapSize + ")"));
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

		if(CLASS_HEADER_SIZE!=heapSize) {
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
			int size = 0;
			for(VarType argType : types) {
				size += (-1 == argType.getSize() ? getRefSize() : argType.getSize());
			}
			if(0 != size) scope.append(blockFree(size));
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
		int size = 0;
		for(VarType argType : types) {
			size += (-1 == argType.getSize() ? getRefSize() : argType.getSize());
		}
		if(0 != size) scope.append(blockFree(size));
		if(0!=types.length) scope.append(popHeapReg(null, false));

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
								cellsToRegs(scope, vScope.getCells(), regs);
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
						cellsToRegs(scope, new CGCells(CGCells.Type.REG, accRegs), regs);
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
	public void updateRefCount(CGScope scope, CGCells cells, boolean isInc) throws CompileException {
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
			case ARGS:
			case STACK_FRAME:
				scope.append(new CGIAsmIReg('y', "ldd zl,y+", cells.getId(0), "", getCurBlockScope(scope), (CGBlockScope)cells.getObject(),
											CGCells.Type.ARGS == cells.getType()));
				if(0x01 == getRefSize()) {
					scope.append(new CGIAsm("ldi zh,0x00"));
				}
				else {
					scope.append(new CGIAsmIReg('y', "ldd zh,y+", cells.getId(1), ""));
				}
				break;
			case HEAP:
				if(0x01 == getRefSize()) {
					scope.append(new CGIAsmIReg('z', "ldd zl,z+", cells.getId(0), ""));
					scope.append(new CGIAsm("ldi zh,0x00"));
				}
				else {
					scope.append(new CGIAsmIReg('z', "ldd j8b_atom,z+", cells.getId(0), ""));
					scope.append(new CGIAsmIReg('z', "ldd zh,z+", cells.getId(1), ""));
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
			result.append(new CGIAsm("pop yh"));
			result.append(new CGIAsm("pop yl"));
		}
		result.append(new CGIAsm("ret"));
		
/*		if(0!=argsSize) {
			result.append(new CGIAsm("pop zh"));
			result.append(new CGIAsm("pop zl"));
			result.append(new CGIAsm("subi yl,low(" + argsSize*(-1) + ")")); 
			if(def_sph) result.append(new CGIAsm("sbci yh,high(" + argsSize*(-1) + ")"));
			result.append(new CGIAsm("ijmp"));
		}
		else {
			result.append(new CGIAsm("ret"));
		}*/
		
		if(null != scope) scope.append(result);
		return result;
	}
	
	@Override
	public void eThrow() {
		System.out.println("CG:throw");
	}
	
	@Override
	public int getRefSize() {
		return refSize;
	}
	
	@Override
	public int getCallSize() {
		return def_call ? 0x02 : 0x01;
	}
	@Override
	public int getCallStackSize() {
		return 0x02;
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

	@Override
	public void normalizeIRegConst(CGMethodScope mScope) {
		List<CGItem> list = new ArrayList<>();
		treeToList(mScope, list);

		int yOffset = mScope.getArgsStackSize() + 0x04;
		int zOffset = 0;
		
		for(CGItem item : list) {
			if(item instanceof CGIAsmIReg) {
				CGIAsmIReg instr = (CGIAsmIReg)item;
				if(!instr.isArg() && 'y'==instr.getIreg()) {
					int varsSize=mScope.getArgsStackSize() + 0x04;
					for(CGBlockScope bScope : mScope.getBlockScopes()) {
						if(bScope == instr.getVarBlockScope()) break;
						varsSize += bScope.getVarsStackSize();
					}
					instr.setOffset(varsSize + instr.getOffset());
				}
			}
		}
		for(CGItem item : list) {
			if(item instanceof CGIAsmIReg) {
				CGIAsmIReg instr = (CGIAsmIReg)item;
				if('y'==instr.getIreg()) {
					int delta = yOffset-instr.getOffset();
					if(MIN_IREG_OFFSET>delta) {
						moveIReg(instr.getPrefCont(), instr.getIreg(), delta-32);
						yOffset -= (delta-32); 
					}
					if(MAX_IREG_OFFSET<delta) {
						moveIReg(instr.getPrefCont(), instr.getIreg(), delta-32);
						yOffset -= (delta-32); 
					}
					instr.setOffset(yOffset-instr.getOffset());
				}
				else {
					int delta = instr.getOffset()-zOffset;
					if(MIN_IREG_OFFSET>delta) {
						moveIReg(instr.getPrefCont(), instr.getIreg(), delta-32);
						yOffset += (delta-32); 
					}
					if(MAX_IREG_OFFSET<delta) {
						moveIReg(instr.getPrefCont(), instr.getIreg(), delta+32);
						yOffset += (delta+32);
					}
					instr.setOffset(instr.getOffset()-zOffset);
				}
			}
		}
	}
	private void moveIReg(CGIContainer cont, char iReg, int offset) {
		cont.append(new CGIAsm("subi " + iReg + "l,low(" + offset*(-1) + ")"));
		cont.append(new CGIAsm("sbci " + iReg + "h,high(" + offset*(-1) + ")"));
	}
}
