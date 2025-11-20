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
import java.util.concurrent.atomic.AtomicInteger;
import ru.vm5277.common.LabelNames;
import ru.vm5277.common.NativeBinding;
import ru.vm5277.common.NumUtils;
import ru.vm5277.common.Operator;
import static ru.vm5277.common.Operator.BIT_AND;
import static ru.vm5277.common.Operator.BIT_OR;
import static ru.vm5277.common.Operator.BIT_XOR;
import static ru.vm5277.common.Operator.EQ;
import static ru.vm5277.common.Operator.LT;
import static ru.vm5277.common.Operator.MINUS;
import static ru.vm5277.common.Operator.PLUS;
import ru.vm5277.common.compiler.Case;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.RTOSFeature;
import ru.vm5277.common.RTOSLibs;
import ru.vm5277.common.StrUtils;
import ru.vm5277.common.SystemParam;
import ru.vm5277.common.cg.CGActionHandler;
import ru.vm5277.common.cg.CGArrCells;
import ru.vm5277.common.cg.items.CGIAsm;
import ru.vm5277.common.cg.items.CGIContainer;
import ru.vm5277.common.cg.items.CGIText;
import ru.vm5277.common.cg.scopes.CGBlockScope;
import ru.vm5277.common.cg.CGCells;
import ru.vm5277.common.cg.DataSymbol;
import ru.vm5277.common.cg.scopes.CGVarScope;
import ru.vm5277.common.cg.RegPair;
import ru.vm5277.common.cg.items.CGIAsmIReg;
import ru.vm5277.common.cg.items.CGIAsmJump;
import ru.vm5277.common.cg.items.CGIAsmLdLabel;
import ru.vm5277.common.cg.items.CGItem;
import ru.vm5277.common.cg.scopes.CGCellsScope;
import ru.vm5277.common.cg.CGBranch;
import ru.vm5277.common.cg.scopes.CGFieldScope;
import ru.vm5277.common.cg.scopes.CGLabelScope;
import ru.vm5277.common.cg.scopes.CGMethodScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;

// Блокировка диспетчера(механизм атомарности) в MULTITHREADING режиме очуществляется за счет инструкций SET и CLT, т.е. флаг T отведен конкретно под эту задачу
// и более нигде использоваться не должен!


// TODO Кодогенератор НЕ ДОЛЖЕН выполнять работу оптимизатора, т.е. не нужно формировать movw вместо двух mov и тому подобное. Так как оптимизатор справится с
// этой задачей лучше, а кодогенератор может ему помешать.

public class Generator extends CodeGenerator {
	private	final	static	String				VERSION				= StrUtils.readVersion(Generator.class);
	private	final	static	byte[]				usableRegs			= new byte[]{20,21,22,23,24,25,26,27}; //Используем регистры
	private	final	static	byte[]				usableRegsArr		= new byte[]{20,21,22,23,24,25}; //Используем регистры(X-под массивы)
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
	private					byte[][]			nativeMethodRegs;	//Временное хранилище списка регистров для формировния вызова к нативному методу
	
	public Generator(String platform, int optLevel, Map<String, NativeBinding> nbMap, Map<SystemParam, Object> params) {
		super(platform, optLevel, nbMap, params);
		
		refSize = 0x02; //TODO для МК с памятью <= 256 нужно возвращать 0x01;
		CLASS_HEADER_SIZE = (0x02==refSize ? 0x05 : 0x04);
		
		optimizer = new Optimizer();
		// Собираем RTOS
	}
	
	@Override
	public CGIContainer stackAlloc(boolean firstBlock, int argsSize, int varsSize) throws CompileException {
		CGIContainer cont = new CGIContainer();
		if(VERBOSE_LO <= verbose) cont.append(new CGIText(";alloc stack, size:" + varsSize));
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) cont.append(new CGIAsm("set"));

		if(firstBlock && (0!=argsSize || 0!=varsSize)) {
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
		
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) cont.append(new CGIAsm("clt"));
		return cont;
	}

//Продумать освобождение памяти с учетом normalizeIRegConst
	@Override
	public CGIContainer blockFree(int varsSize) throws CompileException {
		CGIContainer cont = new CGIContainer();
		if(VERBOSE_LO <= verbose) cont.append(new CGIText(";block free, stack size:" + varsSize));
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) cont.append(new CGIAsm("set"));
		
		if(0!=varsSize) {
			if((def_sph && varsSize<9) || (!def_sph && varsSize<3)) {
				for(int i=0; i<varsSize; i++) {
					cont.append(new CGIAsm("pop j8b_atom"));
				}
			}
			else {
				if(def_sph) cont.append(new CGIAsm("cli"));
				cont.append(new CGIAsm("lds _SPL,SPL"));
				if(def_sph) cont.append(new CGIAsm("lds _SPH,SPH"));
				cont.append(new CGIAsm("subi _SPL,low(-" + varsSize + ")")); // Оптимизатор по возможности приведет к интрукции adiw
				if(def_sph) cont.append(new CGIAsm("sbci _SPH,high(-" + varsSize + ")"));
				cont.append(new CGIAsm("sts SPL,_SPL"));
				if(def_sph) cont.append(new CGIAsm("sts SPH,_SPH"));
				if(def_sph) cont.append(new CGIAsm("sei")); // На верхнем уровне прерывания всегда резешены, проверка(созранение бита) не требуется
			}
		}
		
		if(def_sph) cont.append(new CGIAsm("pop yh"));
		cont.append(new CGIAsm("pop yl"));
		
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) cont.append(new CGIAsm("clt"));
		return cont;
	}
	
	@Override
	public CGIContainer finMethod(VarType type, int argsSize, int varsSize, int lastStackOffset) throws CompileException {
		/* Структура данных в стеке
		/  2Б	-регистровая пара Z
		/  XБ	-аргументы
		/  2Б	-адрес возврата
		/  2Б	-регистровая пара Y(опционально - зависит от наличия локальных переменных и аргументов)
		/  XБ	-локальные переменные=стек фрейм(опционально)
		*/
		
		CGIContainer cont = new CGIContainer();
		if(VERBOSE_LO <= verbose) cont.append(new CGIText(	";finish method, type:" + type + ", args size:" + argsSize + ", vars size:" + varsSize));
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) cont.append(new CGIAsm("set"));
		
		// Есть переменные или аргументы, значит Y сохранен в стеке и его значение - адрес SP перед локальными переменными(с корректировкой lastStackOffset)
		if(0!=varsSize || 0!=argsSize) {
			if(0!=lastStackOffset) {
				int offset = lastStackOffset+0x02+0x02+argsSize; //Находимся после Z
				if(64>offset) {
					cont.append(new CGIAsm("sbiw r28," + offset));
				}
				else {
					cont.append(new CGIAsm("subi r28,low(" + offset + ")"));
					cont.append(new CGIAsm("subi r29,high(" + offset + ")"));
				}
			}
			// Передаю размер аргументов
			if(128>argsSize) {
				cont.append(new CGIAsm("ldi r30," + argsSize));
			}
			else if(32768>argsSize) {
				int octet = (argsSize&0x7f) + 0x80;
				cont.append(new CGIAsm("ldi r30," + octet));
				cont.append(new CGIAsm("ldi r31," + (argsSize>>0x07)));
			}
			else {
				throw new CompileException("Too big args size:" + argsSize);
			}
			RTOSLibs.METHOD_FIN_SF.setRequired();
			//Перехожу на процедуру завершения метода(с учетом Y в стеке)
			cont.append(new CGIAsmJump(INSTR_JUMP, "j8bproc_mfin_sf"));
		}
		else {
			RTOSLibs.METHOD_FIN.setRequired();
			cont.append(new CGIAsmJump(INSTR_JUMP, "j8bproc_mfin"));
		}
		return cont;
	}

	@Override
	public CGIContainer pushHeapReg(CGScope scope) throws CompileException {
		CGIContainer result = new CGIContainer();
		result.append(new CGIAsm("push r30"));
		result.append(new CGIAsm("push r31"));
		if(null != scope) scope.append(result);
		return result;
	}
	@Override
	public CGIContainer popHeapReg(CGScope scope) throws CompileException {
		CGIContainer result = new CGIContainer();
		if(VERBOSE_LO <= verbose) result.append(new CGIText(";pop heap iReg"));
		result.append(new CGIAsm("pop r31"));
		result.append(new CGIAsm("pop r30"));
		if(null != scope) scope.append(result);
		return result;
	}

	@Override
	public CGIContainer pushStackReg(CGScope scope) throws CompileException {
		CGIContainer result = new CGIContainer();
		if(VERBOSE_LO <= verbose) result.append(new CGIText(";push stack iReg"));
		result.append(new CGIAsm("push r28"));
		result.append(new CGIAsm("push r29"));
		if(null != scope) scope.append(result);
		return result;
	}
	@Override
	public CGIContainer popStackReg(CGScope scope) throws CompileException {
		CGIContainer result = new CGIContainer();
		if(VERBOSE_LO <= verbose) result.append(new CGIText(";pop stack iReg"));
		result.append(new CGIAsm("pop r29"));
		result.append(new CGIAsm("pop r28"));
		if(null != scope) scope.append(result);
		return result;
	}
	
	@Override
	public CGIContainer pushArrReg(CGScope scope) throws CompileException {
		CGIContainer result = new CGIContainer();
		if(VERBOSE_LO <= verbose) result.append(new CGIText(";push arr iReg"));
		result.append(new CGIAsm("push r26"));
		result.append(new CGIAsm("push r27"));
		if(null != scope) scope.append(result);
		return result;
	}
	@Override
	public CGIContainer popArrReg(CGScope scope) throws CompileException {
		CGIContainer result = new CGIContainer();
		if(VERBOSE_LO <= verbose) result.append(new CGIText(";pop arr iReg"));
		result.append(new CGIAsm("pop r27"));
		result.append(new CGIAsm("pop r26"));
		if(null != scope) scope.append(result);
		return result;
	}

	
	@Override
	public void thisToAcc(CGScope scope) throws CompileException {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";This to acc"));
		scope.append(new CGIAsm("movw r16,r30"));
	}
	
	/**
	 * pushConst используетя для вызова метода(передачи констант)
	 * @param scope - область видимости для генерации кода
	 * @param size - необходимый размер в стеке
	 * @param value - значение константы
	 * @param isFixed - fixed значение
	 * @return 
	 */
	@Override
	public CGIContainer pushConst(CGScope scope, int size, long value, boolean isFixed) throws CompileException {
		CGIContainer cont = new CGIContainer();
		if(VERBOSE_LO <= verbose) cont.append(new CGIText(";push const '" + (isFixed ? value/256.0 : value) + "'"));
		
		for(int i=0; i<size; i++) {
			int octet = (int)(value&0xff);
			value >>= 0x08;
			
			switch (octet) {
				case 0x00:
					cont.append(new CGIAsm("push c0x00"));
					break;
				case 0x01:
					cont.append(new CGIAsm("push c0x01"));
					break;
				case 0xff:
					cont.append(new CGIAsm("push c0xff"));
					break;
				default:
					if(0x04!=accum.getSize()) {
						cont.append(new CGIAsm("ldi r19," + octet));
						cont.append(new CGIAsm("push r19"));
					}
					else {
						cont.append(new CGIAsm("ldi r30," + octet));
						cont.append(new CGIAsm("push r30"));
					}
					break;
			}
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
	public void pushLabel(CGScope scope, String label) throws CompileException {
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
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) cont.append(new CGIAsm("set")); //Можем свободно использовать j8b_atom
		
		switch(cells.getType()) {
			case ARRAY:
				for(int i=0; i<cells.getSize(); i++) {
					cont.append(new CGIAsm("ld j8b_atom,x+"));
					cont.append(new CGIAsm("push j8b_atom"));
				}
				break;
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
			case STAT:
				for(int i=0; i<cells.getSize(); i++) {
					scope.append(new CGIAsm("lds j8b_atom,_OS_STAT_POOL+" + cells.getId(i)));
					cont.append(new CGIAsm("push j8b_atom"));
				}
				break;
			default:
				throw new CompileException("Unsupported cell type:" + cells.getType());
		}
		
		for(int i=cells.getSize(); i<size; i++) {
			cont.append(new CGIAsm("push c0x00"));
		}
		
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) cont.append(new CGIAsm("clt"));
		if(null!=scope) scope.append(cont);
		return cont;
	}

	@Override
	public void constToAcc(CGScope scope, int size, long value, boolean isFixed) throws CompileException {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";const '" + (isFixed ? value/256.0 : value) + "'->accum "));
		if(-1!=size) {
			accum.setSize(size);
		}
		scope.append(new CGIAsm("ldi r16," + (value&0xff))); value >>= 0x08;
		if(accum.getSize()>1) scope.append(new CGIAsm("ldi r17," + (value&0xff))); value >>= 0x08;
		if(accum.getSize()>2) scope.append(new CGIAsm("ldi r18," + (value&0xff))); value >>= 0x08;
		if(accum.getSize()>3) scope.append(new CGIAsm("ldi r19," + (value&0xff)));
	}

	@Override
	public void accToCells(CGScope scope, CGCellsScope cScope) throws CompileException { //Записываем acc в переменную
		if(accum.getSize() != cScope.getSize()) {
			throw new CompileException("COMPILER BUG: Missing accCast() before accToCells. Developer attention required - please report this case");
		}
		
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
			accToField(scope, size, fScope.getCells());
		}
		else if(cScope.getCells() instanceof CGArrCells) {
			if(VERBOSE_LO <= verbose) scope.append(new CGIText(";accum->arr " + cScope.getCells()));
			accToArr(scope, (CGArrCells)cScope.getCells());
		}
		else throw new CompileException("Unsupported scope:" + cScope);
	}
	
	
	@Override
	public void arrRegToAcc(CGScope scope) throws CompileException {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";arrReg->acc"));
		scope.append(new CGIAsm("movw r26,r16"));
		accum.setSize(0x02);
	}
	@Override
	public void accToArrReg(CGScope scope) throws CompileException {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";acc->arrReg"));
		if(0x02!=accum.getSize()) {
			throw new CompileException("COMPILER BUG: Uxpected 0x02 size for accumulator. Developer attention required - please report this case");
		}
		scope.append(new CGIAsm("movw r16,r26"));
	}

	@Override
	public void arrToCells(CGScope scope, CGArrCells arrCells, CGCells cells) throws CompileException {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";arr '" + arrCells + "'->cells " + cells));
		
		computeArrCellAddr(scope, new CGCells(CGCells.Type.ACC), arrCells);
		
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) scope.append(new CGIAsm("set"));
		int size = arrCells.getSize();

		switch(cells.getType()) {
			case REG: //TODO при size==1, атомарность не нужна
				for(int i=0; i<size; i++) scope.append(new CGIAsm("ld r" + cells.getId(i) + ",x" + (i!=size-1 ? "+" : "")));
				break;
			case ARGS:
			case STACK_FRAME:
				CGIContainer _cont = new CGIContainer();
				for(int i=0; i<size; i++) {
					_cont.append(new CGIAsm("ld j8b_atom,x" + (i!=size-1 ? "+" : "")));
					_cont.append(new CGIAsmIReg('y', "std y+", cells.getId(i), ",j8b_atom", getCurBlockScope(scope), (CGBlockScope)cells.getObject(),
												CGCells.Type.ARGS == cells.getType()));
				}
				scope.append(_cont);
				break;
			case HEAP:
				_cont = new CGIContainer();
				for(int i=0; i<size; i++) {
					_cont.append(new CGIAsm("ld j8b_atom,x" + (i!=size-1 ? "+" : "")));
					_cont.append(new CGIAsmIReg('z', "std z+", cells.getId(i), "j8b_atom"));
				}
				scope.append(_cont);
				break;
			case STAT:
				for(int i=0; i<size; i++) {
					scope.append(new CGIAsm("ld j8b_atom,x" + (i!=size-1 ? "+" : "")));
					scope.append(new CGIAsm("sts _OS_STAT_POOL+" + cells.getId(i) + ",j8b_atom"));
				}
				break;
			default:
				throw new CompileException("Unsupported cell type:" + cells.getType());
		}
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) scope.append(new CGIAsm("clt"));
	}

	@Override
	public void arrRegToCells(CGScope scope, CGCells cells) throws CompileException {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";arrReg->cells " + cells));
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) scope.append(new CGIAsm("set"));
		
		switch(cells.getType()) {
			case ACC:
				scope.append(new CGIAsm("movw r16,r26"));
				break;
			case REG:
				scope.append(new CGIAsm("mov r" + cells.getId(0) + ",r26"));
				scope.append(new CGIAsm("mov r" + cells.getId(1) + ",r27"));
				break;
			case ARGS:
			case STACK_FRAME:
				CGIContainer _cont = new CGIContainer();
				_cont.append(new CGIAsmIReg('y', "std y+", cells.getId(0), ",r26", getCurBlockScope(scope), (CGBlockScope)cells.getObject(),
											CGCells.Type.ARGS == cells.getType()));
				_cont.append(new CGIAsmIReg('y', "std y+", cells.getId(1), ",r27", getCurBlockScope(scope), (CGBlockScope)cells.getObject(),
											CGCells.Type.ARGS == cells.getType()));
				scope.append(_cont);
				break;
			case HEAP:
				_cont = new CGIContainer();
				_cont.append(new CGIAsmIReg('z', "std z+", cells.getId(0), "r26"));
				_cont.append(new CGIAsmIReg('z', "std z+", cells.getId(1), "r27"));
				scope.append(_cont);
				break;
			case STAT:
				scope.append(new CGIAsm("sts _OS_STAT_POOL+" + cells.getId(0) + ",r26"));
				scope.append(new CGIAsm("sts _OS_STAT_POOL+" + cells.getId(1) + ",r27"));
				break;
			default:
				throw new CompileException("Unsupported cell type:" + cells.getType());
		}
		
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) scope.append(new CGIAsm("clt"));
	}

	@Override //Диспетчер должне быть хаблокирован заранее
	public void computeArrCellAddr(CGScope scope, CGCells arrAddrCells, CGArrCells arrCells) throws CompileException { 
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";compute array addr " + arrAddrCells + "->X"));
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) scope.append(new CGIAsm("set"));
		
		if(null!=arrAddrCells) {
			switch(arrAddrCells.getType()) {
				case ACC:
					scope.append(new CGIAsm("movw xl,r16"));
					break;
				case REG:
					scope.append(new CGIAsm("mov xl,r" + arrAddrCells.getId(0)));
					scope.append(new CGIAsm("mov xh,r" + arrAddrCells.getId(1)));
					break;
				case ARGS:
				case STACK_FRAME:
					CGIContainer _cont = new CGIContainer();
						_cont.append(new CGIAsmIReg('y', "ldd xl,y+", arrAddrCells.getId(0), "", getCurBlockScope(scope), (CGBlockScope)arrAddrCells.getObject(),
													CGCells.Type.ARGS == arrAddrCells.getType()));
						_cont.append(new CGIAsmIReg('y', "ldd xh,y+", arrAddrCells.getId(1), "", getCurBlockScope(scope), (CGBlockScope)arrAddrCells.getObject(),
													CGCells.Type.ARGS == arrAddrCells.getType()));
					scope.append(_cont);
					break;
				case HEAP:
					_cont = new CGIContainer();
					_cont.append(new CGIAsmIReg('z', "ldd xl,z+", arrAddrCells.getId(0), ""));
					_cont.append(new CGIAsmIReg('z', "ldd xh,z+", arrAddrCells.getId(1), ""));
					scope.append(_cont);
					break;
				case STAT:
					scope.append(new CGIAsm("lds xl,_OS_STAT_POOL+" + arrAddrCells.getId(0)));
					scope.append(new CGIAsm("lds xh,_OS_STAT_POOL+" + arrAddrCells.getId(1)));
					break;
				default:
					throw new CompileException("Unsupported cell type:" + arrAddrCells.getType());
			}
		}

		if(arrCells.canComputeStatic()) {
			int cellAddr = arrCells.computeStaticAddr();
			scope.append(new CGIAsm("subi xl,low(-" + cellAddr + ")"));
			scope.append(new CGIAsm("sbci xh,high(-" + cellAddr + ")"));
		}
		else {
			RTOSLibs.ARR_CELLADDR.setRequired();
			scope.append(new CGIAsm("rcall j8bproc_arr_celladdr"));
		}
		
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) scope.append(new CGIAsm("clt"));
	}

	
	// Адрес уже должен быть в X(см. computeArrAddr)
	@Override
	public void arrToAcc(CGScope scope, CGArrCells arrCells) throws CompileException {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";arr '" + arrCells + "'->acc"));
		
		int size = arrCells.getSize();
		byte[] accRegs = getAccRegs(size);
		for(int i=0; i<size; i++) {
			scope.append(new CGIAsm("ld r" + accRegs[i] + ",x+"));
		}
		accum.setSize(size);
	}
	
	@Override
	public void accToArr(CGScope scope, CGArrCells arrCells) throws CompileException {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";acc ->arr'" + arrCells + "'"));
		int size = arrCells.getSize();
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING) && 1!=size) scope.append(new CGIAsm("set"));
		
		byte[] accRegs = getAccRegs(size);
		for(int i=0; i<size; i++) {
			scope.append(new CGIAsm("st x+,r" + accRegs[i]));
		}
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING) && 1!=size) scope.append(new CGIAsm("clt"));
	}

	@Override
	public void setHeapReg(CGScope scope, CGCells cells) throws CompileException {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";setHeap " + cells));
		cellsToRegs(scope, cells, getTempRegs(getCallSize()));
		//cellsActionAsm(scope, offset, cells, (int sn, String rOp) -> new CGIAsm("mov " + (0==sn ? "zl" : "zh") + "," + rOp));
	}

	@Override
	public void cellsToAcc(CGScope scope, CGCellsScope cScope) throws CompileException {
		if(CGCells.Type.LABEL==cScope.getCells().getType()) cScope.getCells().setSize(0x02);
		
		if(cScope instanceof CGVarScope) {
			CGVarScope vScope = (CGVarScope)cScope;
//			if(!vScope.isConstant()) {
				if(VERBOSE_LO <= verbose) scope.append(new CGIText(";var '" + vScope.getName() + "'->accum"));
//				//TODO расширение аккумулятора. Не уверен что на своем месте
//				if(accum.getSize()<vScope.getCells().getSize()) accum.setSize(vScope.getCells().getSize());
				cellsToRegs(scope, vScope.getCells(), getAccRegs(accum.getSize()));
//			}
		}
		else if(cScope instanceof CGFieldScope) {
			CGFieldScope fScope = (CGFieldScope)cScope;
			if(VERBOSE_LO <= verbose) scope.append(new CGIText(";field '" + fScope.getName() + "'->accum"));
//			//TODO расширение аккумулятора. Не уверен что на своем месте
//			if(accum.getSize()<fScope.getCells().getSize()) accum.setSize(fScope.getCells().getSize());
			cellsToRegs(scope, fScope.getCells(), getAccRegs(accum.getSize()));
		}
		else throw new CompileException("Unsupported scope:" + cScope);
	}
	
	@Override
	public void cellsToAcc(CGScope scope, CGCells cells) throws CompileException {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";cells '" + cells + "'->accum"));
		if(CGCells.Type.LABEL==cells.getType()) cells.setSize(0x02);
		cellsToRegs(scope, cells, getAccRegs(cells.getSize()));
		accum.setSize(cells.getSize());
	}
	
	@Override
	public void cellsToArrReg(CGScope scope, CGCells cells) throws CompileException {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";cells '" + cells + "'->ArrReg"));
		cellsToRegs(scope, cells, new byte[]{(byte)26,(byte)27});
	}
	
	@Override
	public void arrSizetoAcc(CGScope cgScope, boolean isView) throws CompileException {
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) scope.append(new CGIAsm("set"));
		
		RTOSLibs.ARR_SIZE.setRequired();
		if(isView) {
			RTOSLibs.ARRVIEW_ARRADDR.setRequired();
			cgScope.append(new CGIAsm("rcall j8bproc_arrview_arraddr"));
		}
		cgScope.append(new CGIAsm("rcall j8bproc_arr_size"));
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) scope.append(new CGIAsm("clt"));
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
	public void accResize(VarType type) throws CompileException {
		accum.setSize(-1==type.getSize() ? getRefSize() : type.getSize());
	}
	@Override
	public CGIContainer accCast(VarType accType, VarType opType) throws CompileException {
		CGIContainer cont = new CGIContainer();
		int accSize = null == accType ? accum.getSize() : (-1==accType.getSize() ? getRefSize() : accType.getSize());
		int opSize = (-1==opType.getSize() ? getRefSize() : opType.getSize());
		if(VERBOSE_LO <= verbose) {
			if(accSize != opSize || VarType.FIXED == accType && VarType.FIXED != opType || VarType.FIXED != accType && VarType.FIXED == opType) {
				cont.append(new CGIText(";acc cast " + accType + "[" + accSize + "]->" + opType + "[" + opSize + "]"));
			}
		}
		
		int offset = opSize;
		if(null != accType) {
			if(VarType.FIXED == accType && VarType.FIXED != opType) {
				cont.append(new CGIAsm("mov r16,r17"));
				cont.append(new CGIAsm("clr r17"));
				offset = 0x02;
			}
			else if(VarType.FIXED != accType && VarType.FIXED == opType) {
				cont.append(new CGIAsm("mov r17,r16"));
				cont.append(new CGIAsm("clr r16"));
				offset = 0x01;
			}
		}
		if(accSize<offset) {
			byte[] accRegs = getAccRegs(4);
			for(int i=accSize; i<offset; i++) {
				cont.append(new CGIAsm("ldi r" + accRegs[i] + ",0x00"));
			}
		}
		accum.setSize(opSize);
		return cont;
	}

	public void cellsToRegs(CGScope scope, CGCells cells, byte[] registers) throws CompileException {
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING) &&1!=registers.length) scope.append(new CGIAsm("set"));
		
		int size = (registers.length<cells.getSize() ? registers.length : cells.getSize());
		
		switch(cells.getType()) {
			case LABEL:
				scope.append(new CGIAsmLdLabel("ldi r" + registers[0x00] + ",low", (String)cells.getObject(), "*2"));
				scope.append(new CGIAsmLdLabel("ldi r" + registers[0x01] + ",high", (String)cells.getObject(), "*2"));
				break;
			case ACC:
				byte [] accRegs = getAccRegs(registers.length);
				for(int i=0; i<size; i++) {
					if(accRegs[i]!=registers[i]) {
						scope.append(new CGIAsm("mov r" + registers[i] + ",r" + accRegs[i]));
					}
				}
				break;
			case REG:
				LinkedList<Byte> popRegs = new LinkedList<>();
				for(int i=0; i<size; i++) {
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
				for(int i=0; i<size; i++) {
					scope.append(new CGIAsmIReg('y', "ldd r" + registers[i] + ",y+", cells.getId(i), "", getCurBlockScope(scope),
												(CGBlockScope)cells.getObject(), CGCells.Type.ARGS == cells.getType()));
				}
				break;
			case HEAP:
				for(int i=0; i<size; i++) {
					scope.append(new CGIAsmIReg('z', "ldd r" + registers[i] + ",z+", cells.getId(i), ""));
				}
				break;
			case HEAP_ALT:
				if(0!=cells.getId(0)) {
					scope.append(new CGIAsm("subi xl,-" + (cells.getId(0)&0xff)));
					scope.append(new CGIAsm("sbci xh,-" + ((cells.getId(0)>>8)&0xff)));
				}
				for(int i=0; i<size; i++) {
					scope.append(new CGIAsm("ld r" + registers[i] + ",x+"));
				}
				break;
			case STAT:
				for(int i=0; i<size; i++) {
					scope.append(new CGIAsm("lds r" + registers[i] + ",_OS_STAT_POOL+" + cells.getId(i)));
				}
				break;
			default:
				throw new CompileException("Unsupported cell type:" + cells.getType());
		}

		for(int i=size; i<registers.length; i++) {
			scope.append(new CGIAsm("ldi r" + registers[i] + ",0x00"));
		}
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING) && 1!=registers.length) scope.append(new CGIAsm("clt"));
	}

	@Override
	public CGIContainer constToCells(CGScope scope, long value, CGCells cells, boolean isFixed) throws CompileException {
		CGIContainer cont = new CGIContainer();
		if(VERBOSE_LO <= verbose) cont.append(new CGIText(";const '" + (isFixed ? value/256.0 : value) + "'->cells " + cells));
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) scope.append(new CGIAsm("set"));

		byte tmpReg=19;
		if(0x04==accum.getSize() && CGCells.Type.REG!=cells.getType()) {
			tmpReg = (CGCells.Type.HEAP == cells.getType() ? (byte)28 : (byte)30);
			if(19!=tmpReg) scope.append(new CGIAsm("mov j8b_atom,r"+tmpReg));
		}

		switch(cells.getType()) {
			case REG: //TODO при cells.getSize()==1 атомарность не нужна
				for(int i=0; i<cells.getSize(); i++) cont.append(new CGIAsm("ldi r" + cells.getId(i)+ "," + (value>>(i*8)&0xff)));
				break;
			case ARGS:
			case STACK_FRAME:
				for(int i=0; i<cells.getSize(); i++) {
					String cReg = getConstReg(cont, tmpReg, i, value);
					cont.append(new CGIAsmIReg('y', "std y+", cells.getId(i), ","+cReg, getCurBlockScope(scope), (CGBlockScope)cells.getObject(),
												CGCells.Type.ARGS == cells.getType()));
				}
				break;
			case HEAP:
				for(int i=0; i<cells.getSize(); i++) {
					String cReg = getConstReg(cont, tmpReg, i, value);
					cont.append(new CGIAsmIReg('z', "std z+", cells.getId(i), ","+cReg));
				}
				break;
			case HEAP_ALT:
				if(0!=cells.getId(0)) {
					cont.append(new CGIAsm("subi xl,-" + (cells.getId(0)&0xff)));
					cont.append(new CGIAsm("sbci xh,-" + ((cells.getId(0)>>8)&0xff)));
				}
				for(int i=0; i<cells.getSize(); i++) {
					String cReg = getConstReg(cont, tmpReg, i, value);
					cont.append(new CGIAsm("st x+,"+cReg));
				}
				break;
			case ARRAY:
				CGArrCells arrCells = (CGArrCells)cells;
				int size = arrCells.getSize();
				for(int i=0; i<size; i++) {
					String cReg = getConstReg(cont, tmpReg, i, value);
					cont.append(new CGIAsm("st x+,"+cReg));
				}
				break;
			case STAT:
				for(int i=0; i<cells.getSize(); i++) {
					String cReg = getConstReg(cont, tmpReg, i, value);
					cont.append(new CGIAsm("sts _OS_STAT_POOL+" + cells.getId(i) + ","+cReg));
				}
				break;
			default:
				throw new CompileException("Unsupported cell type:" + cells.getType());
		}

		if(19!=tmpReg) scope.append(new CGIAsm("mov r"+tmpReg+",j8b_atom"));
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) scope.append(new CGIAsm("clt"));
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
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) scope.append(new CGIAsm("set"));
		
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
			case STAT:
				for(int i=0; i<cells.getSize(); i++) {
					scope.append(new CGIAsm("sts _OS_STAT_POOL+" + cells.getId(i) + ",r" + regs[i]));
				}
				break;
			default:
				throw new CompileException("Unsupported cell type:" + cells.getType());
		}
		
		for(int i=cells.getSize(); i<usedRegsQnt; i++) {
			scope.append(new CGIAsm("ldi r" + cells.getId(i) + ",0x00"));
		}
		
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) scope.append(new CGIAsm("clt"));
	}

	public int getRegPos(byte[] regs, int offset, byte reg) {
		for(int i=offset; i<regs.length; i++) {
			if(regs[i]==reg) return i;
		}
		return -1;
	}
	
	public int getCellsPos(CGCells cells, byte reg) {
		for(int pos=0; pos<cells.getSize();pos++) {
			if(cells.getId(pos)==reg) return pos;
		}
		return -1;
	}

	public void accToField(CGScope scope, int size, CGCells cells) throws CompileException {
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING) && 1!=size) scope.append(new CGIAsm("set"));
		
		if(CGCells.Type.STAT==cells.getType()) {
			scope.append(new CGIAsm("sts _OS_STAT_POOL+" + cells.getId(0) + ",r16"));
			if(size>1) scope.append(new CGIAsm("sts _OS_STAT_POOL+" + cells.getId(1) + ",r17"));
			if(size>2) scope.append(new CGIAsm("sts _OS_STAT_POOL+" + cells.getId(2) + ",r18"));
			if(size>3) scope.append(new CGIAsm("sts _OS_STAT_POOL+" + cells.getId(3) + ",r19"));
		}
		else if(CGCells.Type.HEAP==cells.getType()) {
			scope.append(new CGIAsmIReg('z', "std z+", cells.getId(0), ",r16"));
			if(size>1) scope.append(new CGIAsmIReg('z', "std z+", cells.getId(1), ",r17"));
			if(size>2) scope.append(new CGIAsmIReg('z', "std z+", cells.getId(2), ",r18"));
			if(size>3) scope.append(new CGIAsmIReg('z', "std z+", cells.getId(3), ",r19"));
		}
		else {
			throw new CompileException("Unsupported cell type:" + cells.getType());
		}
		
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING) && 1!=size) scope.append(new CGIAsm("clt"));
	}

	@Override
	public CGIContainer pushAccBE(CGScope scope, Integer size) throws CompileException {
		CGIContainer result = new CGIContainer();
		if(VERBOSE_LO <= verbose) result.append(new CGIText(";push accum(BE)"));
		byte[] regs = getAccRegs(null==size ? accum.getSize() : size);
		for(int i=regs.length-1; i>=0; i--) {
			result.append(new CGIAsm("push r" + regs[i]));
		}
		if(null != scope) scope.append(result);
		return result;
	}
	@Override
	public void popAccBE(CGScope scope, Integer size) throws CompileException {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";pop accum(BE)"));
		byte[] regs = getAccRegs(null==size ? accum.getSize() : size);
		for(int i=0; i<regs.length; i++) scope.append(new CGIAsm("pop r" + regs[i]));
	}

	//TODO актуализировать
	@Override
	public void eUnary(CGScope scope, Operator op, CGCells cells, boolean toAccum) throws CompileException {
		//3 блока: 1 считывает значение, 2 выполняет опреацию, 3 записывает значение
		//для регистров все просто, для аккумулятора не доступны POST* и PRE*
		//POST и PRE влияют на аккумулятор, возвращая перед операцией значение или после операции.

		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";eUnary " + op + " cells " + cells + (toAccum ? " -> accum" : "")));

		if(	CGCells.Type.REG != cells.getType() && CGCells.Type.ARGS != cells.getType() && CGCells.Type.STACK_FRAME != cells.getType() &&
			CGCells.Type.HEAP != cells.getType() && CGCells.Type.ARRAY != cells.getType() && CGCells.Type.STAT != cells.getType()) {
			
			throw new CompileException("Unsupported cell type:" + cells.getType());
		}

		byte tmpReg=19;
		if(0x04==accum.getSize() && CGCells.Type.REG!=cells.getType()) {
			tmpReg = (CGCells.Type.HEAP == cells.getType() ? (byte)28 : (byte)30);
			scope.append(new CGIAsm("push r" + tmpReg));
		}
		
		byte[] accRegs = getAccRegs(cells.getSize());
		if(Operator.PRE_INC == op || Operator.PRE_DEC == op || Operator.POST_INC == op || Operator.POST_DEC == op) {
			switch(cells.getType()) {
				case ARRAY:
					for(int i=0; i<cells.getSize(); i++) {
						scope.append(new CGIAsm("ld r" + tmpReg + ",x"));
						if(toAccum && (Operator.POST_INC == op || Operator.POST_DEC == op)) {
							scope.append(new CGIAsm("mov r" + accRegs[i] + ",r" + tmpReg));
						}
						if(0==i) scope.append(new CGIAsm((Operator.PRE_INC==op || Operator.POST_INC==op ? "add" : "sub") + " r" + tmpReg + ",C0x01"));
						else scope.append(new CGIAsm((Operator.PRE_INC==op || Operator.POST_INC==op ? "adc" : "sbc") + " r" + tmpReg + ",C0x00"));
						if(toAccum && (Operator.PRE_INC == op || Operator.PRE_DEC == op)) {
							scope.append(new CGIAsm("mov r" + accRegs[i] + ",r" + tmpReg));
						}
						scope.append(new CGIAsm("st x+,r" + tmpReg));
					}
					break;
				case STAT:
					for(int i=0; i<cells.getSize(); i++) {
						scope.append(new CGIAsm("lds r" + tmpReg + ",_OS_STAT_POOL+" + cells.getId(i)));
						if(toAccum && (Operator.POST_INC == op || Operator.POST_DEC == op)) {
							scope.append(new CGIAsm("mov r" + accRegs[i] + ",r" + tmpReg));
						}
						if(0==i) scope.append(new CGIAsm((Operator.PRE_INC==op || Operator.POST_INC==op ? "add" : "sub") + " r" + tmpReg + ",C0x01"));
						else scope.append(new CGIAsm((Operator.PRE_INC==op || Operator.POST_INC==op ? "adc" : "sbc") + " r" + tmpReg + ",C0x00"));
						if(toAccum && (Operator.PRE_INC == op || Operator.PRE_DEC == op)) {
							scope.append(new CGIAsm("mov r" + accRegs[i] + ",r" + tmpReg));
						}
						scope.append(new CGIAsm("sts _OS_STAT_POOL+" + cells.getId(i) + ",r" + tmpReg));
					}
					break;
				case REG:
					if(toAccum && (Operator.POST_INC == op || Operator.POST_DEC == op)) {
						for(int i=0; i<cells.getSize(); i++) scope.append(new CGIAsm("mov r" + accRegs[i] + ",r" + cells.getId(i)));
					}
					scope.append(new CGIAsm((Operator.PRE_INC==op || Operator.POST_INC==op ? "add" : "sub") + " r" + cells.getId(0) + ",C0x01"));
					for(int i=1; i<cells.getSize(); i++) {
						scope.append(new CGIAsm((Operator.PRE_INC==op || Operator.POST_INC==op ? "adc" : "sbc") + " r" + cells.getId(i) + ",C0x00"));
					}
					if(toAccum && (Operator.PRE_INC == op || Operator.PRE_DEC == op)) {
						for(int i=0; i<cells.getSize(); i++) scope.append(new CGIAsm("mov r" + accRegs[i] + ",r" + cells.getId(i)));
					}
					break;
				case ARGS:
				case STACK_FRAME:
					for(int i=0; i<cells.getSize(); i++) {
						scope.append(new CGIAsmIReg('y', "ldd r" + tmpReg + ",y+", cells.getId(i), "", getCurBlockScope(scope), (CGBlockScope)cells.getObject(),
													CGCells.Type.ARGS == cells.getType()));
						if(toAccum && (Operator.POST_INC == op || Operator.POST_DEC == op)) {
							scope.append(new CGIAsm("mov r" + accRegs[i] + ",r" + tmpReg));
						}
						if(0==i) scope.append(new CGIAsm((Operator.PRE_INC==op || Operator.POST_INC==op ? "add" : "sub") + " r" + tmpReg + ",C0x01"));
						else scope.append(new CGIAsm((Operator.PRE_INC==op || Operator.POST_INC==op ? "adc" : "sbc") + " r" + tmpReg + ",C0x00"));
						if(toAccum && (Operator.PRE_INC == op || Operator.PRE_DEC == op)) {
							scope.append(new CGIAsm("mov r" + accRegs[i] + ",r" + tmpReg));
						}
						scope.append(new CGIAsmIReg('y', "std y+", cells.getId(i), ",r" + tmpReg, getCurBlockScope(scope), (CGBlockScope)cells.getObject(),
													CGCells.Type.ARGS == cells.getType()));
					}
					break;
				case HEAP:
					for(int i=0; i<cells.getSize(); i++) {
						scope.append(new CGIAsmIReg('z', "ldd r" + tmpReg + ",z+",cells.getId(i), ""));
						if(toAccum && (Operator.POST_INC == op || Operator.POST_DEC == op)) {
							scope.append(new CGIAsm("mov r" + accRegs[i] + ",r" + tmpReg));
						}
						if(0==i) scope.append(new CGIAsm((Operator.PRE_INC==op || Operator.POST_INC==op ? "add" : "sub") + " r" + tmpReg + ",C0x01"));
						else scope.append(new CGIAsm((Operator.PRE_INC==op || Operator.POST_INC==op ? "adc" : "sbc") + " r" + tmpReg + ",C0x00"));
						if(toAccum && (Operator.PRE_INC == op || Operator.PRE_DEC == op)) {
							scope.append(new CGIAsm("mov r" + accRegs[i] + ",r"+tmpReg));
						}
						scope.append(new CGIAsmIReg('z', "std z+",cells.getId(i), ",r"+tmpReg));
					}
					break;
			}
		}
		else if(Operator.NOT==op || Operator.BIT_NOT==op || Operator.MINUS==op) {
			switch(cells.getType()) {
				case ARRAY:
					for(int i=0; i<cells.getSize(); i++) {
						if(toAccum) {
							scope.append(new CGIAsm("ld r" + accRegs[i] + ",x+"));
							scope.append(new CGIAsm("com r" + accRegs[i]));
						}
						else {
							//TODO првести к единому виду tmpReg
							scope.append(new CGIAsm("ld r"+tmpReg+",x"));
							scope.append(new CGIAsm("com r"+tmpReg));
							scope.append(new CGIAsm("st x+,r"+tmpReg));
						}
					}
					break;
				case STAT:
					for(int i=0; i<cells.getSize(); i++) {
						if(toAccum) {
							scope.append(new CGIAsm("lds r" + tmpReg + ",_OS_STAT_POOL+" + cells.getId(i)));
							scope.append(new CGIAsm("com r" + accRegs[i]));
						}
						else {
							scope.append(new CGIAsm("lds r" + tmpReg + ",_OS_STAT_POOL+" + cells.getId(i)));
							scope.append(new CGIAsm("com r"+tmpReg));
							scope.append(new CGIAsm("sts _OS_STAT_POOL+" + cells.getId(i) + ",r" + tmpReg));
						}
					}
					break;
				case REG:
					for(int i=0; i<cells.getSize(); i++) {
						if(toAccum) {
							scope.append(new CGIAsm("mov r" + accRegs[i] + ",r" + cells.getId(i)));
							scope.append(new CGIAsm("com r" + accRegs[i]));
						}
						else {
							scope.append(new CGIAsm("com r" + cells.getId(i)));
						}
					}
					break;
				case ARGS:
				case STACK_FRAME:
					for(int i=0; i<cells.getSize(); i++) {
						if(toAccum) {
							scope.append(new CGIAsmIReg('y', "ldd r" + accRegs[i] + ",y+", cells.getId(i), "", getCurBlockScope(scope),
														(CGBlockScope)cells.getObject(), CGCells.Type.ARGS == cells.getType()));
							scope.append(new CGIAsm("com r" + accRegs[i]));
						}
						else {
							scope.append(new CGIAsmIReg('y', "ldd r"+tmpReg+",y+", cells.getId(i), "", getCurBlockScope(scope),
														(CGBlockScope)cells.getObject(), CGCells.Type.ARGS == cells.getType()));
							scope.append(new CGIAsm("com r"+tmpReg));
							scope.append(new CGIAsmIReg('y', "std y+", cells.getId(i), ",r"+tmpReg, getCurBlockScope(scope),
														(CGBlockScope)cells.getObject(), CGCells.Type.ARGS == cells.getType()));
						}
					}
					break;
				case HEAP:
					for(int i=0; i<cells.getSize(); i++) {
						if(toAccum) {
							scope.append(new CGIAsmIReg('z', "ldd r" + accRegs[i] + ",z+",cells.getId(i), ""));
							scope.append(new CGIAsm("com r" + accRegs[i]));
						}
						else {
							scope.append(new CGIAsmIReg('z', "ldd r" + tmpReg + ",z+",cells.getId(i), ""));
							scope.append(new CGIAsm("com r"+tmpReg));
							scope.append(new CGIAsmIReg('z', "std z+", cells.getId(i), "r"+tmpReg));
						}
					}
					break;
			}
		}
		if(Operator.MINUS==op) {
			scope.append(new CGIAsm("add r" + accRegs[0] + ",C0x01"));
			for(int i=1; i<cells.getSize(); i++) scope.append(new CGIAsm("adc r" + accRegs[i] + ",C0x00"));
		}
		if(19!=tmpReg) scope.append(new CGIAsm("pop r" + tmpReg));
	}
		

	//TODO актуализировать		
	// Работаем с аккумулятором!
	@Override
	public void cellsAction(CGScope scope, CGCells cells, Operator op, boolean isFixed) throws CompileException {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";accum " + op + " cells " + cells + " -> accum" + (isFixed ? "[FIXED]" : "")));
		if(accum.getSize() != cells.getSize()) {
			throw new CompileException("COMPILER BUG: Missing accCast() before accToCells. Developer attention required - please report this case");
		}

		switch(op) {
			case PLUS:
				cellsActionAsm(scope, cells, (int sn, String reg) -> new CGIAsm((0 == sn ? "add " : "adc ") + getRightOp(sn, null) + "," + reg));
				break;
			case MINUS:
				cellsActionAsm(scope, cells, (int sn, String reg) -> new CGIAsm((0 == sn ? "sub " : "sbc ") + getRightOp(sn, null) + "," + reg));
				break;
			case MULT:
				int maxSize = Math.max(cells.getSize(), accum.getSize());
				if(0x01==maxSize) {
					RTOSLibs.MATH_MUL8.setRequired();
					switch(cells.getType()) {
						case REG:
							scope.append(new CGIAsm("mov r17,r"+cells.getId(0)));
							break;
						case ARGS:
						case STACK_FRAME:
							scope.append(new CGIAsmIReg('y', "ldd r17,y+", cells.getId(0), "", getCurBlockScope(scope),
														(CGBlockScope)cells.getObject(), CGCells.Type.ARGS == cells.getType()));
							break;
						case HEAP:
							scope.append(new CGIAsmIReg('z', "ldd r17,z+", cells.getId(0), "", getCurBlockScope(scope),
														(CGBlockScope)cells.getObject(), false));
							break;
						case STACK:
							scope.append(new CGIAsm("pop r17"));
							break;
						default:
							throw new CompileException("Unsupported cell type:" + cells.getType());
					}
					scope.append(new CGIAsm("rcall os_mul8"));
				}
				else if(0x02==maxSize) {
					if(isFixed) RTOSLibs.MATH_MULQ7N8.setRequired();
					else RTOSLibs.MATH_MUL16.setRequired();
					switch(cells.getType()) {
						case REG:
							scope.append(new CGIAsm("mov r18,r"+cells.getId(0)));
							scope.append(new CGIAsm("mov r19,r"+cells.getId(1)));
							break;
						case ARGS:
						case STACK_FRAME:
							scope.append(new CGIAsmIReg('y', "ldd r18,y+", cells.getId(0), "", getCurBlockScope(scope),
														(CGBlockScope)cells.getObject(), CGCells.Type.ARGS == cells.getType()));
							scope.append(new CGIAsmIReg('y', "ldd r19,y+", cells.getId(1), "", getCurBlockScope(scope),
														(CGBlockScope)cells.getObject(), CGCells.Type.ARGS == cells.getType()));
							break;
						case HEAP:
							scope.append(new CGIAsmIReg('z', "ldd r18,z+", cells.getId(0), "", getCurBlockScope(scope),
														(CGBlockScope)cells.getObject(), false));
							scope.append(new CGIAsmIReg('z', "ldd r19,z+", cells.getId(1), "", getCurBlockScope(scope),
														(CGBlockScope)cells.getObject(), false));
							break;
						case STACK:
							scope.append(new CGIAsm("pop r18"));
							scope.append(new CGIAsm("pop r19"));//TODO проверить порядок
							break;
						default:
							throw new CompileException("Unsupported cell type:" + cells.getType());
					}
					scope.append(new CGIAsm("rcall " + (isFixed ? "os_mulq7n8" : "os_mul16")));
				}
				else if(0x04==maxSize) {
					RTOSLibs.MATH_MUL32.setRequired();
					scope.append(new CGIAsm("push r24"));
					scope.append(new CGIAsm("push r25"));
					scope.append(new CGIAsm("push r22"));
					scope.append(new CGIAsm("push r23"));
					switch(cells.getType()) {
						case REG:
							scope.append(new CGIAsm("mov r24,r"+cells.getId(0)));
							scope.append(new CGIAsm("mov r25,r"+cells.getId(1)));
							scope.append(new CGIAsm("mov r22,r"+cells.getId(2)));
							scope.append(new CGIAsm("mov r23,r"+cells.getId(3)));
							break;
						case ARGS:
						case STACK_FRAME:
							scope.append(new CGIAsmIReg('y', "ldd r24,y+", cells.getId(0), "", getCurBlockScope(scope),
														(CGBlockScope)cells.getObject(), CGCells.Type.ARGS == cells.getType()));
							scope.append(new CGIAsmIReg('y', "ldd r25,y+", cells.getId(1), "", getCurBlockScope(scope),
														(CGBlockScope)cells.getObject(), CGCells.Type.ARGS == cells.getType()));
							scope.append(new CGIAsmIReg('y', "ldd r22,y+", cells.getId(2), "", getCurBlockScope(scope),
														(CGBlockScope)cells.getObject(), CGCells.Type.ARGS == cells.getType()));
							scope.append(new CGIAsmIReg('y', "ldd r23,y+", cells.getId(3), "", getCurBlockScope(scope),
														(CGBlockScope)cells.getObject(), CGCells.Type.ARGS == cells.getType()));
							break;
						case HEAP:
							scope.append(new CGIAsmIReg('z', "ldd r24,z+", cells.getId(0), "", getCurBlockScope(scope),
														(CGBlockScope)cells.getObject(), false));
							scope.append(new CGIAsmIReg('z', "ldd r25,z+", cells.getId(1), "", getCurBlockScope(scope),
														(CGBlockScope)cells.getObject(), false));
							scope.append(new CGIAsmIReg('z', "ldd r22,z+", cells.getId(2), "", getCurBlockScope(scope),
														(CGBlockScope)cells.getObject(), false));
							scope.append(new CGIAsmIReg('z', "ldd r23,z+", cells.getId(3), "", getCurBlockScope(scope),
														(CGBlockScope)cells.getObject(), false));
							break;
						case STACK:
							scope.append(new CGIAsm("pop r24"));
							scope.append(new CGIAsm("pop r25"));
							scope.append(new CGIAsm("pop r22"));
							scope.append(new CGIAsm("pop r23"));//TODO проверить порядок
							break;
						default:
							throw new CompileException("Unsupported cell type:" + cells.getType());
					}
					scope.append(new CGIAsm("rcall os_mul32_nr"));
					scope.append(new CGIAsm("pop r23"));
					scope.append(new CGIAsm("pop r22"));
					scope.append(new CGIAsm("pop r25"));
					scope.append(new CGIAsm("pop r24"));
				}
				break;
			case DIV:
				maxSize = Math.max(cells.getSize(), accum.getSize());
				CGLabelScope lbEndScope = new CGLabelScope(null, null, LabelNames.DIV_END, true);
				CGLabelScope lbNoErrScope = new CGLabelScope(null, null, LabelNames.DIV_NO_ERR, true);
				if(0x01==maxSize) {
					RTOSLibs.MATH_DIV8.setRequired();
					switch(cells.getType()) {
						case REG:
							scope.append(new CGIAsm("mov r17,r"+cells.getId(0)));
							break;
						case ARGS:
						case STACK_FRAME:
							scope.append(new CGIAsmIReg('y', "ldd r17,y+", cells.getId(0), "", getCurBlockScope(scope),
														(CGBlockScope)cells.getObject(), CGCells.Type.ARGS == cells.getType()));
							break;
						case HEAP:
							scope.append(new CGIAsmIReg('z', "ldd r17,z+", cells.getId(0), "", getCurBlockScope(scope),
														(CGBlockScope)cells.getObject(), false));
							break;
						case STACK:
							scope.append(new CGIAsm("pop r17"));
							break;
						default:
							throw new CompileException("Unsupported cell type:" + cells.getType());
					}
					scope.append(new CGIAsm("tst r17"));
					scope.append(new CGIAsmJump("brne", lbNoErrScope.getName()));
					scope.append(new CGIText(";TODO Division by zero"));
					scope.append(new CGIAsm("ldi r16,0xff"));//TODO
					scope.append(new CGIAsmJump("rjmp", lbEndScope.getName()));
					scope.append(lbNoErrScope);
					scope.append(new CGIAsm("push r24"));
					scope.append(new CGIAsm("rcall os_div8"));
					scope.append(new CGIAsm("pop r24"));
					scope.append(lbEndScope);
				}
				else if(0x02==maxSize) {
					if(isFixed) RTOSLibs.MATH_DIVQ7N8.setRequired();
					else RTOSLibs.MATH_DIV16.setRequired();
					switch(cells.getType()) {
						case REG:
							scope.append(new CGIAsm("mov r18,r"+cells.getId(0)));
							scope.append(new CGIAsm("mov r19,r"+cells.getId(1)));
							break;
						case ARGS:
						case STACK_FRAME:
							scope.append(new CGIAsmIReg('y', "ldd r18,y+", cells.getId(0), "", getCurBlockScope(scope),
														(CGBlockScope)cells.getObject(), CGCells.Type.ARGS == cells.getType()));
							scope.append(new CGIAsmIReg('y', "ldd r19,y+", cells.getId(1), "", getCurBlockScope(scope),
														(CGBlockScope)cells.getObject(), CGCells.Type.ARGS == cells.getType()));
							break;
						case HEAP:
							scope.append(new CGIAsmIReg('z', "ldd r18,z+", cells.getId(0), "", getCurBlockScope(scope),
														(CGBlockScope)cells.getObject(), false));
							scope.append(new CGIAsmIReg('z', "ldd r19,z+", cells.getId(1), "", getCurBlockScope(scope),
														(CGBlockScope)cells.getObject(), false));
							break;
						case STACK:
							scope.append(new CGIAsm("pop r18"));
							scope.append(new CGIAsm("pop r19"));//TODO проверить порядок
							break;
						default:
							throw new CompileException("Unsupported cell type:" + cells.getType());
					}
					scope.append(new CGIAsm("tst r18"));
					scope.append(new CGIAsmJump("brne", lbNoErrScope.getName()));
					scope.append(new CGIAsm("tst r19"));
					scope.append(new CGIAsmJump("brne", lbNoErrScope.getName()));
					scope.append(new CGIText(";TODO Division by zero"));
					scope.append(new CGIAsm("ldi r16,0xff"));//TODO
					scope.append(new CGIAsm("ldi r17,0xff"));
					scope.append(new CGIAsmJump("rjmp", lbEndScope.getName()));
					scope.append(lbNoErrScope);
					scope.append(new CGIAsm("push r24"));
					scope.append(new CGIAsm("push r25"));
					scope.append(new CGIAsm("rcall " + (isFixed ? "os_divq7n8" : "os_div16")));
					scope.append(new CGIAsm("pop r25"));
					scope.append(new CGIAsm("pop r24"));
					scope.append(lbEndScope);
				}
				else if(0x04==maxSize) {
					RTOSLibs.MATH_DIV32.setRequired();
					scope.append(new CGIAsm("push r24"));
					scope.append(new CGIAsm("push r25"));
					scope.append(new CGIAsm("push r22"));
					scope.append(new CGIAsm("push r23"));
					switch(cells.getType()) {
						case REG:
							scope.append(new CGIAsm("mov r24,r"+cells.getId(0)));
							scope.append(new CGIAsm("mov r25,r"+cells.getId(1)));
							scope.append(new CGIAsm("mov r22,r"+cells.getId(2)));
							scope.append(new CGIAsm("mov r23,r"+cells.getId(3)));
							break;
						case ARGS:
						case STACK_FRAME:
							scope.append(new CGIAsmIReg('y', "ldd r24,y+", cells.getId(0), "", getCurBlockScope(scope),
														(CGBlockScope)cells.getObject(), CGCells.Type.ARGS == cells.getType()));
							scope.append(new CGIAsmIReg('y', "ldd r25,y+", cells.getId(1), "", getCurBlockScope(scope),
														(CGBlockScope)cells.getObject(), CGCells.Type.ARGS == cells.getType()));
							scope.append(new CGIAsmIReg('y', "ldd r22,y+", cells.getId(2), "", getCurBlockScope(scope),
														(CGBlockScope)cells.getObject(), CGCells.Type.ARGS == cells.getType()));
							scope.append(new CGIAsmIReg('y', "ldd r23,y+", cells.getId(3), "", getCurBlockScope(scope),
														(CGBlockScope)cells.getObject(), CGCells.Type.ARGS == cells.getType()));
							break;
						case HEAP:
							scope.append(new CGIAsmIReg('z', "ldd r24,z+", cells.getId(0), "", getCurBlockScope(scope),
														(CGBlockScope)cells.getObject(), false));
							scope.append(new CGIAsmIReg('z', "ldd r25,z+", cells.getId(1), "", getCurBlockScope(scope),
														(CGBlockScope)cells.getObject(), false));
							scope.append(new CGIAsmIReg('z', "ldd r22,z+", cells.getId(2), "", getCurBlockScope(scope),
														(CGBlockScope)cells.getObject(), false));
							scope.append(new CGIAsmIReg('z', "ldd r23,z+", cells.getId(3), "", getCurBlockScope(scope),
														(CGBlockScope)cells.getObject(), false));
							break;
						case STACK:
							scope.append(new CGIAsm("pop r24"));
							scope.append(new CGIAsm("pop r25"));
							scope.append(new CGIAsm("pop r22"));
							scope.append(new CGIAsm("pop r23"));//TODO проверить порядок
							break;
						default:
							throw new CompileException("Unsupported cell type:" + cells.getType());
					}
					scope.append(new CGIAsm("tst r24"));
					scope.append(new CGIAsmJump("brne", lbNoErrScope.getName()));
					scope.append(new CGIAsm("tst r25"));
					scope.append(new CGIAsmJump("brne", lbNoErrScope.getName()));
					scope.append(new CGIAsm("tst r22"));
					scope.append(new CGIAsmJump("brne", lbNoErrScope.getName()));
					scope.append(new CGIAsm("tst r23"));
					scope.append(new CGIAsmJump("brne", lbNoErrScope.getName()));
					scope.append(new CGIText(";TODO Division by zero"));
					scope.append(new CGIAsm("ldi r16,0xff"));//TODO
					scope.append(new CGIAsm("ldi r17,0xff"));
					scope.append(new CGIAsm("ldi r18,0xff"));
					scope.append(new CGIAsm("ldi r19,0xff"));
					scope.append(new CGIAsmJump("rjmp", lbEndScope.getName()));
					scope.append(lbNoErrScope);
					scope.append(new CGIAsm("rcall os_div32_nr"));
					scope.append(lbEndScope);
					scope.append(new CGIAsm("pop r23"));
					scope.append(new CGIAsm("pop r22"));
					scope.append(new CGIAsm("pop r25"));
					scope.append(new CGIAsm("pop r24"));
				}
				break;
			case MOD:
				if(isFixed) throw new CompileException("CG:cellsAction: unsupported operator: " + op);
				maxSize = Math.max(cells.getSize(), accum.getSize());
				lbEndScope = new CGLabelScope(null, null, LabelNames.DIV_END, true);
				lbNoErrScope = new CGLabelScope(null, null, LabelNames.DIV_NO_ERR, true);
				if(0x01==maxSize) {
					RTOSLibs.MATH_DIV8.setRequired();
					switch(cells.getType()) {
						case REG:
							scope.append(new CGIAsm("mov r17,r"+cells.getId(0)));
							break;
						case ARGS:
						case STACK_FRAME:
							scope.append(new CGIAsmIReg('y', "ldd r17,y+", cells.getId(0), "", getCurBlockScope(scope),
														(CGBlockScope)cells.getObject(), CGCells.Type.ARGS == cells.getType()));
							break;
						case HEAP:
							scope.append(new CGIAsmIReg('z', "ldd r17,z+", cells.getId(0), "", getCurBlockScope(scope),
														(CGBlockScope)cells.getObject(), false));
							break;
						case STACK:
							scope.append(new CGIAsm("pop r17"));
							break;
						default:
							throw new CompileException("Unsupported cell type:" + cells.getType());
					}
					scope.append(new CGIAsm("tst r17"));
					scope.append(new CGIAsmJump("brne", lbNoErrScope.getName()));
					scope.append(new CGIText(";TODO Division by zero"));
					scope.append(new CGIAsm("ldi r16,0xff"));//TODO
					scope.append(new CGIAsmJump("rjmp", lbEndScope.getName()));
					scope.append(lbNoErrScope);
					scope.append(new CGIAsm("push r24"));
					scope.append(new CGIAsm("rcall os_div8"));
					scope.append(new CGIAsm("mov r16,r24"));
					scope.append(new CGIAsm("pop r24"));
					scope.append(lbEndScope);
				}
				else if(0x02==maxSize) {
					RTOSLibs.MATH_DIV16.setRequired();
					switch(cells.getType()) {
						case REG:
							scope.append(new CGIAsm("mov r18,r"+cells.getId(0)));
							scope.append(new CGIAsm("mov r19,r"+cells.getId(1)));
							break;
						case ARGS:
						case STACK_FRAME:
							scope.append(new CGIAsmIReg('y', "ldd r18,y+", cells.getId(0), "", getCurBlockScope(scope),
														(CGBlockScope)cells.getObject(), CGCells.Type.ARGS == cells.getType()));
							scope.append(new CGIAsmIReg('y', "ldd r19,y+", cells.getId(1), "", getCurBlockScope(scope),
														(CGBlockScope)cells.getObject(), CGCells.Type.ARGS == cells.getType()));
							break;
						case HEAP:
							scope.append(new CGIAsmIReg('z', "ldd r18,z+", cells.getId(0), "", getCurBlockScope(scope),
														(CGBlockScope)cells.getObject(), false));
							scope.append(new CGIAsmIReg('z', "ldd r19,z+", cells.getId(1), "", getCurBlockScope(scope),
														(CGBlockScope)cells.getObject(), false));
							break;
						case STACK:
							scope.append(new CGIAsm("pop r18"));
							scope.append(new CGIAsm("pop r19"));//TODO проверить порядок
							break;
						default:
							throw new CompileException("Unsupported cell type:" + cells.getType());
					}
					scope.append(new CGIAsm("tst r17"));
					scope.append(new CGIAsmJump("brne", lbNoErrScope.getName()));
					scope.append(new CGIAsm("tst r18"));
					scope.append(new CGIAsmJump("brne", lbNoErrScope.getName()));
					scope.append(new CGIText(";TODO Division by zero"));
					scope.append(new CGIAsm("ldi r16,0xff"));//TODO
					scope.append(new CGIAsm("ldi r17,0xff"));
					scope.append(new CGIAsmJump("rjmp", lbEndScope.getName()));
					scope.append(lbNoErrScope);
					scope.append(new CGIAsm("push r24"));
					scope.append(new CGIAsm("push r25"));
					scope.append(new CGIAsm("rcall os_div16"));
					scope.append(new CGIAsm("movw r16,r24"));
					scope.append(new CGIAsm("pop r25"));
					scope.append(new CGIAsm("pop r24"));
					scope.append(lbEndScope);
				}
				else if(0x04==maxSize) {
					RTOSLibs.MATH_DIV32.setRequired();
					scope.append(new CGIAsm("push r24"));
					scope.append(new CGIAsm("push r25"));
					scope.append(new CGIAsm("push r22"));
					scope.append(new CGIAsm("push r23"));
					switch(cells.getType()) {
						case REG:
							scope.append(new CGIAsm("mov r24,r"+cells.getId(0)));
							scope.append(new CGIAsm("mov r25,r"+cells.getId(1)));
							scope.append(new CGIAsm("mov r22,r"+cells.getId(2)));
							scope.append(new CGIAsm("mov r23,r"+cells.getId(3)));
							break;
						case ARGS:
						case STACK_FRAME:
							scope.append(new CGIAsmIReg('y', "ldd r24,y+", cells.getId(0), "", getCurBlockScope(scope),
														(CGBlockScope)cells.getObject(), CGCells.Type.ARGS == cells.getType()));
							scope.append(new CGIAsmIReg('y', "ldd r25,y+", cells.getId(1), "", getCurBlockScope(scope),
														(CGBlockScope)cells.getObject(), CGCells.Type.ARGS == cells.getType()));
							scope.append(new CGIAsmIReg('y', "ldd r22,y+", cells.getId(2), "", getCurBlockScope(scope),
														(CGBlockScope)cells.getObject(), CGCells.Type.ARGS == cells.getType()));
							scope.append(new CGIAsmIReg('y', "ldd r23,y+", cells.getId(3), "", getCurBlockScope(scope),
														(CGBlockScope)cells.getObject(), CGCells.Type.ARGS == cells.getType()));
							break;
						case HEAP:
							scope.append(new CGIAsmIReg('z', "ldd r24,z+", cells.getId(0), "", getCurBlockScope(scope),
														(CGBlockScope)cells.getObject(), false));
							scope.append(new CGIAsmIReg('z', "ldd r25,z+", cells.getId(1), "", getCurBlockScope(scope),
														(CGBlockScope)cells.getObject(), false));
							scope.append(new CGIAsmIReg('z', "ldd r22,z+", cells.getId(2), "", getCurBlockScope(scope),
														(CGBlockScope)cells.getObject(), false));
							scope.append(new CGIAsmIReg('z', "ldd r23,z+", cells.getId(3), "", getCurBlockScope(scope),
														(CGBlockScope)cells.getObject(), false));
							break;
						case STACK:
							scope.append(new CGIAsm("pop r24"));
							scope.append(new CGIAsm("pop r25"));
							scope.append(new CGIAsm("pop r22"));
							scope.append(new CGIAsm("pop r23"));//TODO проверить порядок
							break;
						default:
							throw new CompileException("Unsupported cell type:" + cells.getType());
					}
					scope.append(new CGIAsm("tst r24"));
					scope.append(new CGIAsmJump("brne", lbNoErrScope.getName()));
					scope.append(new CGIAsm("tst r25"));
					scope.append(new CGIAsmJump("brne", lbNoErrScope.getName()));
					scope.append(new CGIAsm("tst r22"));
					scope.append(new CGIAsmJump("brne", lbNoErrScope.getName()));
					scope.append(new CGIAsm("tst r23"));
					scope.append(new CGIAsmJump("brne", lbNoErrScope.getName()));
					scope.append(new CGIText(";TODO Division by zero"));
					scope.append(new CGIAsm("ldi r16,0xff"));//TODO
					scope.append(new CGIAsm("ldi r17,0xff"));
					scope.append(new CGIAsm("ldi r18,0xff"));
					scope.append(new CGIAsm("ldi r19,0xff"));
					scope.append(new CGIAsmJump("rjmp", lbEndScope.getName()));
					scope.append(lbNoErrScope);
					scope.append(new CGIAsm("rcall os_div32"));
					scope.append(new CGIAsm("movw r16,r24"));
					scope.append(new CGIAsm("movw r18,r22"));
					scope.append(new CGIAsm("pop r23"));
					scope.append(new CGIAsm("pop r22"));
					scope.append(new CGIAsm("pop r25"));
					scope.append(new CGIAsm("pop r24"));
					scope.append(lbEndScope);
				}
				break;
			case AND:
			case BIT_AND:
				if(!isFixed) {
					cellsActionAsm(scope,  cells, (int sn, String reg) -> new CGIAsm("and " + getRightOp(sn, null) + "," + reg));
				}
				else {
					throw new CompileException("CG:cellsAction: unsupported operator: " + op);
				}
				break;
			case OR:
			case BIT_OR:
				if(!isFixed) {
					cellsActionAsm(scope,  cells, (int sn, String reg) -> new CGIAsm("or " + getRightOp(sn, null) + "," + reg));
				}
				else {
					throw new CompileException("CG:cellsAction: unsupported operator: " + op);
				}
				break;
			case BIT_XOR:
				if(!isFixed) {
					cellsActionAsm(scope,  cells, (int sn, String reg) -> new CGIAsm("eor " + getRightOp(sn, null) + "," + reg));
				}
				else {
					throw new CompileException("CG:cellsAction: unsupported operator: " + op);
				}
				break;
			default:
				throw new CompileException("CG:cellsAction: unsupported operator: " + op);
		}
	}

	//TODO актуализировать
	// Работаем с аккумулятором!
	// Оптиммизировано для экономии памяти, для экономии FLASH можно воспользоваться CPC инструкцией
	// В операциях сравнения резултат во флаге C, в остальных результат в аккумуляторе
	@Override
	public void constAction(CGScope scope, Operator op, long k, boolean isFixed) throws CompileException {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";accum " + op + " " + (isFixed ? k/256.0 : k) + " -> accum"));
		switch(op) {
			case PLUS:
				constActionAsm(scope, 0==k?0:~k+1, (int sn, String rOp) -> new CGIAsm((0 == sn ? "subi " : "sbci ") + getRightOp(sn, null) + "," + rOp));
				break;
			case MINUS:
				constActionAsm(scope, k, (int sn, String rOp) -> new CGIAsm((0 == sn ? "subi " : "sbci ") + getRightOp(sn, null) + "," + rOp));
				break;
			case SHL:
				int octetOffset = (int)k/8;
				if(0x03==octetOffset) {
					scope.append(new CGIAsm("mov r19,r16"));
					scope.append(new CGIAsm("ldi r18,0x00"));
					scope.append(new CGIAsm("ldi r17,0x00"));
					scope.append(new CGIAsm("ldi r16,0x00"));
				}
				else if(0x02==octetOffset) {
					scope.append(new CGIAsm("movw r18,r16"));
					scope.append(new CGIAsm("ldi r17,0x00"));
					scope.append(new CGIAsm("ldi r16,0x00"));
				}
				else if(0x01==octetOffset) {
					scope.append(new CGIAsm("mov r19,r18"));
					scope.append(new CGIAsm("mov r18,r17"));
					scope.append(new CGIAsm("mov r17,r16"));
					scope.append(new CGIAsm("ldi r16,0x00"));
				}
				
				k=k-(octetOffset*8);
				if(0!=k) {
					if(0x01==accum.getSize()) {
						RTOSLibs.MATH_MUL8P2.setRequired();
					}
					else if(0x02==accum.getSize()) {
						RTOSLibs.MATH_MUL16P2.setRequired();
					}
					else {
						RTOSLibs.MATH_MUL32P2.setRequired();
					}

					scope.append(new CGIAsm(INSTR_CALL + " " + "os_mul" + (accum.getSize()*8) + "p2_x" + (1<<k)));
				}
				break;
			case SHR:
				octetOffset = (int)k/8;
				if(0x03==octetOffset) {
					scope.append(new CGIAsm("mov r16,r19"));
					scope.append(new CGIAsm("ldi r17,0x00"));
					scope.append(new CGIAsm("ldi r18,0x00"));
					scope.append(new CGIAsm("ldi r19,0x00"));
				}
				else if(0x02==octetOffset) {
					scope.append(new CGIAsm("movw r16,r18"));
					scope.append(new CGIAsm("ldi r18,0x00"));
					scope.append(new CGIAsm("ldi r19,0x00"));
				}
				else if(0x01==octetOffset) {
					scope.append(new CGIAsm("mov r16,r17"));
					scope.append(new CGIAsm("mov r17,r18"));
					scope.append(new CGIAsm("mov r18,r19"));
					scope.append(new CGIAsm("ldi r19,0x00"));
				}
				
				k=k-(octetOffset*8);
				if(0!=k) {
					if(0x01==accum.getSize()) {
						RTOSLibs.MATH_DIV8P2.setRequired();
					}
					else if(0x02==accum.getSize()) {
						RTOSLibs.MATH_DIV16P2.setRequired();
					}
					else {
						RTOSLibs.MATH_DIV32P2.setRequired();
					}

					scope.append(new CGIAsm(INSTR_CALL + " " + "os_div" + (accum.getSize()*8) + "p2_x" + (1<<k)));
				}
				break;
			case MULT:
				//TODO ранее размер брал с аккумулятора, его размер точно отражает размер константы?
				int maxSize = Math.max(NumUtils.getBytesRequired(k), accum.getSize());
				if(0x01==maxSize) {
					RTOSLibs.MATH_MUL8.setRequired();
					scope.append(new CGIAsm("ldi r17,"+(k&0xff)));
					scope.append(new CGIAsm("rcall os_mul8"));
				}
				else if(0x02==maxSize) {
					if(isFixed) RTOSLibs.MATH_MULQ7N8.setRequired();
					else RTOSLibs.MATH_MUL16.setRequired();
					scope.append(new CGIAsm("ldi r18,"+(k&0xff)));
					scope.append(new CGIAsm("ldi r19,"+((k>>8)&0xff)));
					scope.append(new CGIAsm("rcall " + (isFixed ? "os_mulq7n8" : "os_mul16")));
				}
				else if(0x04==maxSize) {
					RTOSLibs.MATH_MUL32.setRequired();
					scope.append(new CGIAsm("push r24"));//TODO добавить проверку на занятось регистров
					scope.append(new CGIAsm("push r25"));
					scope.append(new CGIAsm("push r22"));
					scope.append(new CGIAsm("push r23"));
					scope.append(new CGIAsm("ldi r24,"+(k&0xff)));
					scope.append(new CGIAsm("ldi r25,"+((k>>8)&0xff)));
					scope.append(new CGIAsm("ldi r22,"+((k>>16)&0xff)));
					scope.append(new CGIAsm("ldi r23,"+((k>>24)&0xff)));
					scope.append(new CGIAsm("rcall os_mul32_nr"));
					scope.append(new CGIAsm("pop r23"));
					scope.append(new CGIAsm("pop r22"));
					scope.append(new CGIAsm("pop r25"));
					scope.append(new CGIAsm("pop r24"));
				}
				break;
			case DIV:
				if(accum.getSize()<NumUtils.getBytesRequired(k)) {
					scope.append(new CGIAsm("ldi r16,0x00"));
					accum.setSize(0x01);
				}
				else {
					maxSize = Math.max(NumUtils.getBytesRequired(k), accum.getSize());
					if(0x01==maxSize) {
						RTOSLibs.MATH_DIV8.setRequired();
						scope.append(new CGIAsm("push r24"));
						scope.append(new CGIAsm("ldi r17,"+(k&0xff)));
						scope.append(new CGIAsm("rcall os_div8"));
						scope.append(new CGIAsm("pop r24"));
					}
					else if(0x02==maxSize) {
						if(isFixed) RTOSLibs.MATH_DIVQ7N8.setRequired();
						else RTOSLibs.MATH_DIV16.setRequired();
						scope.append(new CGIAsm("push r24"));
						scope.append(new CGIAsm("push r25"));
						scope.append(new CGIAsm("ldi r18,"+(k&0xff)));
						scope.append(new CGIAsm("ldi r19,"+((k>>8)&0xff)));
						scope.append(new CGIAsm("rcall " + (isFixed ? "os_divq7n8" : "os_div16")));
						scope.append(new CGIAsm("pop r25"));
						scope.append(new CGIAsm("pop r24"));
					}
					else if(0x04==maxSize) {
						RTOSLibs.MATH_DIV32.setRequired();
						scope.append(new CGIAsm("push r24"));//TODO добавить проверку на занятось регистров
						scope.append(new CGIAsm("push r25"));
						scope.append(new CGIAsm("push r22"));
						scope.append(new CGIAsm("push r23"));
						scope.append(new CGIAsm("ldi r24,"+(k&0xff)));
						scope.append(new CGIAsm("ldi r25,"+((k>>8)&0xff)));
						scope.append(new CGIAsm("ldi r22,"+((k>>16)&0xff)));
						scope.append(new CGIAsm("ldi r23,"+((k>>24)&0xff)));
						scope.append(new CGIAsm("rcall os_div32"));
						scope.append(new CGIAsm("pop r23"));
						scope.append(new CGIAsm("pop r22"));
						scope.append(new CGIAsm("pop r25"));
						scope.append(new CGIAsm("pop r24"));
					}
				}
				break;
			case MOD:
				if(isFixed) throw new CompileException("CG:constAction: unsupported operator: " + op);
				if(accum.getSize()<NumUtils.getBytesRequired(k)) {
					scope.append(new CGIAsm("ldi r16,0x00"));
					accum.setSize(0x01);
				}
				else {
					maxSize = Math.max(NumUtils.getBytesRequired(k), accum.getSize());
					if(0x01==maxSize) {
						RTOSLibs.MATH_DIV8.setRequired();
						scope.append(new CGIAsm("push r24"));
						scope.append(new CGIAsm("ldi r17,"+(k&0xff)));
						scope.append(new CGIAsm("rcall os_div8"));
						scope.append(new CGIAsm("mov r16,r24"));
						scope.append(new CGIAsm("pop r24"));
					}
					else if(0x02==maxSize) {
						RTOSLibs.MATH_DIV16.setRequired();
						scope.append(new CGIAsm("push r24"));
						scope.append(new CGIAsm("push r25"));
						scope.append(new CGIAsm("ldi r18,"+(k&0xff)));
						scope.append(new CGIAsm("ldi r19,"+((k>>8)&0xff)));
						scope.append(new CGIAsm("rcall os_div16"));
						scope.append(new CGIAsm("movw r16,r24"));
						scope.append(new CGIAsm("pop r25"));
						scope.append(new CGIAsm("pop r24"));
					}
					else if(0x04==maxSize) {
						RTOSLibs.MATH_DIV32.setRequired();
						scope.append(new CGIAsm("push r24"));//TODO добавить проверку на занятось регистров
						scope.append(new CGIAsm("push r25"));
						scope.append(new CGIAsm("push r22"));
						scope.append(new CGIAsm("push r23"));
						scope.append(new CGIAsm("ldi r24,"+(k&0xff)));
						scope.append(new CGIAsm("ldi r25,"+((k>>8)&0xff)));
						scope.append(new CGIAsm("ldi r22,"+((k>>16)&0xff)));
						scope.append(new CGIAsm("ldi r23,"+((k>>24)&0xff)));
						scope.append(new CGIAsm("rcall os_div32"));
						scope.append(new CGIAsm("movw r16,r24"));
						scope.append(new CGIAsm("movw r18,r22"));
						scope.append(new CGIAsm("pop r23"));
						scope.append(new CGIAsm("pop r22"));
						scope.append(new CGIAsm("pop r25"));
						scope.append(new CGIAsm("pop r24"));
					}
				}
				break;
			case BIT_AND:
				if(!isFixed) {
					constActionAsm(scope, k,  (int sn, String reg) -> new CGIAsm("andi " + getRightOp(sn, null) + "," + reg));
				}
				else {
					throw new CompileException("CG:constAction: unsupported operator: " + op);
				}
				break;
			case BIT_OR:
				if(!isFixed) {
					constActionAsm(scope, k, (int sn, String reg) -> new CGIAsm("ori " + getRightOp(sn, null) + "," + reg));
				}
				else {
					throw new CompileException("CG:constAction: unsupported operator: " + op);
				}
				break;
			case BIT_XOR:
				if(!isFixed) {
					scope.append(new CGIAsm("push yl"));
					for(int i=accum.getSize()-1; i>=0; i--) {
						scope.append(new CGIAsm("ldi yl," + ((k>>(i*8))&0xff)));
						scope.append(new CGIAsm("eor " + getRightOp(i, null) + ",r"+getAccRegs(accum.getSize())[i]));
					}
					scope.append(new CGIAsm("pop yl"));
				}
				else {
					throw new CompileException("CG:constAction: unsupported operator: " + op);
				}
				break;
			default:
				throw new CompileException("CG:constAction: unsupported operator: " + op);
		}
	}

	//TODO актуализировать
	@Override
	public void constCond(CGScope scope, CGCells cells, Operator op, long k, boolean isNot, boolean isOr, CGBranch branchScope) throws CompileException {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(	";cells " + cells.toString() + " " + op + " " + k + " -> accum (isOr:" + isOr + ", isNot:" +
															isNot + ")"));
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
		
		int size = cells.getSize();
		byte accRegs[] = null;
		if(CGCells.Type.ACC == cells.getType()) {
			size = accum.getSize();
			accRegs = getAccRegs(size);
		}
		else if(CGCells.Type.ARRAY == cells.getType()) {
			CGArrCells arrCells = (CGArrCells)cells;
			size = arrCells.getSize();
			cont.append(new CGIAsm("adiw r26," + size));
		}

		// Задаем метку на конец блока сравнения байт в мультибайтном значении
		CGLabelScope lbScope = (0x01 == size ? null : new CGLabelScope(null, null, LabelNames.MULTIBYTE_CP_END, false));
		
		for(int i=size-1; i>=0; i--) {
			switch(cells.getType()) {
//	Размер аккумулятора может быть меньше чем cells
				case ACC:
					cont.append(new CGIAsm("cpi r" + accRegs[i] + "," + ((k>>(i*8))&0xff)));
					break;
				case REG:
					cont.append(new CGIAsm("cpi r" + cells.getId(i) + "," + ((k>>(i*8))&0xff)));
					break;
				case ARGS:
				case STACK_FRAME:
					scope.append(new CGIAsmIReg('y', "ldd r16,y+", cells.getId(i), "", getCurBlockScope(scope), (CGBlockScope)cells.getObject(),
												CGCells.Type.ARGS == cells.getType()));
					cont.append(new CGIAsm("cpi r16," + ((k>>(i*8))&0xff)));
					break;
				case HEAP:
					scope.append(new CGIAsmIReg('z', "ldd r16,z+",cells.getId(i), ""));
					cont.append(new CGIAsm("cpi r16," + ((k>>(i*8))&0xff)));
					break;
				case ARRAY:
					cont.append(new CGIAsm("ld r16,-x"));
					cont.append(new CGIAsm("cpi r16," + ((k>>(i*8))&0xff)));
					break;
				default:
					throw new CompileException("Unsupported cell type:" + cells.getType());
			}
			cond(cont, i, op, isOr, lbScope, branchScope);
		}
		if(0x01 != size) cont.append(lbScope);
		scope.append(cont);
	}

	//TODO актуализировать
	@Override
	public void cellsCond(CGScope scope, CGCells cells, Operator op, boolean isNot, boolean isOr, CGBranch branchScope) throws CompileException {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";accum " + op + " " + cells.toString() + " -> accum (isOr:" + isOr + ", isNot:" + isNot + ")"));
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
		
		
		int size = cells.getSize();
		
		if(CGCells.Type.ARRAY == cells.getType()) {
			CGArrCells arrCells = (CGArrCells)cells;
			size = arrCells.getSize();
		}

		// Задаем метку на конец блока сравнения байт в мультибайтном значении
		CGLabelScope lbScope = (0x01 == accum.getSize() ? null : new CGLabelScope(null, null, LabelNames.MULTIBYTE_CP_END, false));
		
		//TODO выполнить аналогичную логику функций, используемых в выражениях
		byte tmpReg=19;
		if(0x04==accum.getSize()) {
			tmpReg = (CGCells.Type.HEAP == cells.getType() ? (byte)28 : (byte)30);
		}
		
		for(int i=accum.getSize()-1; i>=0; i--) {
			byte racc = getAccRegs(accum.getSize())[i];
			if(i>=cells.getSize()) {
				//TODO похоже не оптимальные код сравнений, если известно, что правый операнд = 0
				cont.append(new CGIAsm("cpi r" + racc + ",0x00"));
			}
			else {
				switch(cells.getType()) {
					case REG:
						cont.append(new CGIAsm("cp r" + racc + ",r" + cells.getId(i)));
						break;
					case ARGS:
					case STACK_FRAME:
						if(i==accum.getSize()-1 && 19!=tmpReg) cont.append(new CGIAsm("push r" + tmpReg));
						scope.append(new CGIAsmIReg('y', "ldd r" + tmpReg + ",y+", cells.getId(i), "", getCurBlockScope(scope), (CGBlockScope)cells.getObject(),
													CGCells.Type.ARGS == cells.getType()));
						cont.append(new CGIAsm("cp r" + racc + ",r" + tmpReg));
						if(0==i && 19!=tmpReg) cont.append(new CGIAsm("pop r" + tmpReg));
						break;
					case HEAP:
						if(i==accum.getSize()-1 && 19!=tmpReg) cont.append(new CGIAsm("push r" + tmpReg));
						scope.append(new CGIAsmIReg('z', "ldd r" + tmpReg + ",z+",cells.getId(i), ""));
						cont.append(new CGIAsm("cp r" + racc + ",r" + tmpReg));
						if(0==i && 19!=tmpReg) cont.append(new CGIAsm("pop r" + tmpReg));
						break;
					case ARRAY:
						if(i==accum.getSize()-1 && 19!=tmpReg) cont.append(new CGIAsm("push r" + tmpReg));
						cont.append(new CGIAsm("ld r" + tmpReg + ",-x"));
						cont.append(new CGIAsm("cp r" + racc + ",r" + tmpReg));
						if(0==i && 19!=tmpReg) cont.append(new CGIAsm("pop r" + tmpReg));
						break;
					default:
						throw new CompileException("Unsupported cell type:" + cells.getType());
				}
			}
			cond(cont, i, op, isOr, lbScope, branchScope);
		}
		if(0x01 != accum.getSize()) cont.append(lbScope);
		scope.append(cont);
	}
	
	@Override
	public void boolCond(CGScope scope, CGBranch branchScope, boolean byBit0) throws CompileException {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";accum bool is true"));
		if(!byBit0) {
			scope.append(new CGIAsm("tst r16"));
			scope.append(new CGIAsmJump("breq", branchScope.getEnd()));
		}
		else {
			scope.append(new CGIAsm("sbrs r16,0x00"));
			scope.append(new CGIAsmJump("rjmp", branchScope.getEnd()));
		}
	}

	private void cond(CGIContainer cont, int index, Operator op, boolean isOr, CGLabelScope lbScope, CGBranch branchScope) throws CompileException {
		switch(op) {
			case LT:
				if(isOr) { // В связке с OR
					cont.append(new CGIAsmJump("brcs", branchScope.getEnd())); // True
					if(0!=index) {  // Старшие байты
						cont.append(new CGIAsmJump("brne", lbScope));
						lbScope.setUsed();
						// Если == - продолжаем без перехода
					}
				}
				else { // В связке с AND 
					if(0!=index) {  // Старшие байты
						cont.append(new CGIAsm("breq pc+0x02"));
					}
					cont.append(new CGIAsmJump("brcc", branchScope.getEnd())); // False
				}
				break;
			case GTE:
				if(isOr) { // В связке с OR
					cont.append(new CGIAsmJump("brcc", branchScope.getEnd())); // Если >=
					if(0!=index) {  // Старшие байты
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
					if(0!=index) {  // Старшие байты
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
					if(0!=index) {  // Старшие байты
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
					if(0!=index) {  // Старшие байты
						cont.append(new CGIAsmJump("brne", lbScope)); // Если > - продолжить
						lbScope.setUsed();
					}
					else { // Младший байт
						cont.append(new CGIAsmJump("breq", branchScope.getEnd())); // Если =
					}
				}
				else { // В связке с AND 
					if(0!=index) {  // Старшие байты
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
					if(0!=index) {  //Старшие байты
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
					if(0!=index) {  //Старшие байты
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
	
	// Атомарность должна быть обеспечена вызывающей стороной
	public void cellsActionAsm(CGScope scope, CGCells cells, CGActionHandler handler) throws CompileException {
		switch(cells.getType()) {
			case REG:
				for(int i=0; i<cells.getSize(); i++) {
					scope.append(handler.action(i, "r" + cells.getId(i)));
				}
				break;
			case ARGS:
			case STACK_FRAME:
				for(int i=0; i<cells.getSize(); i++) {
					scope.append(new CGIAsmIReg('y', "ldd j8b_atom,y+", cells.getId(i), "", getCurBlockScope(scope), (CGBlockScope)cells.getObject(),
												CGCells.Type.ARGS == cells.getType()));
					scope.append(handler.action(i, "j8b_atom"));
				}
				break;
			case HEAP:
				for(int i=0; i<cells.getSize(); i++) {
					scope.append(new CGIAsmIReg('z', "ldd j8b_atom,z+", cells.getId(i), ""));
					scope.append(handler.action(i, "j8b_atom"));
				}
				break;
			case STACK:
				//Значение лежит на вершине стека(BigEndian)
				for(int i=0; i<cells.getSize(); i++) {
					scope.append(new CGIAsm("pop j8b_atom"));
					scope.append(handler.action(i, "j8b_atom"));//TODO не могу использовать tmpReg так как он влияет на stack!!!
				}
				break;
			case ARRAY:
				for(int i=0; i<cells.getSize(); i++) {
					scope.append(new CGIAsm("ld j8b_atom,x" + (i!=cells.getSize()-1 ? "+" : "")));
					scope.append(handler.action(i, "j8b_atom"));
				}
				break;
			case STAT:
				for(int i=0; i<cells.getSize(); i++) {
					scope.append(new CGIAsm("lds j8b_atom,_OS_STAT_POOL+" + cells.getId(i)));
					scope.append(handler.action(i, "j8b_atom"));
				}
				break;
			default:
				throw new CompileException("Unsupported cell type:" + cells.getType());
		}
	};
	
	public void constActionAsm(CGScope scope, Object k, CGActionHandler handler) throws CompileException {
		if(k instanceof byte[]) {
			for(int i=0; i<accum.getSize(); i++) {
				scope.append(handler.action(i, Long.toString(((byte[])k)[i]&0xff)));
			}
		}
		else {
			for(int i=0; i<accum.getSize(); i++) {
				scope.append(handler.action(i, Long.toString((((long)k)>>(i*8))&0xff)));
			}
		}
	};


	// TODO Лучше вынести данный код в виде RTOS функции.
	@Override
	public CGIContainer eNewInstance(int heapSize, CGLabelScope iidLabel, VarType type, boolean launchPoint, boolean canThrow) throws CompileException {
		CGIContainer result = new CGIContainer();
		if(VERBOSE_LO <= verbose) result.append(new CGIText(launchPoint ? ";Launch " : (";eNewInstance '" + type + "', heap size:" + heapSize)));
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) scope.append(new CGIAsm("set"));
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

		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) scope.append(new CGIAsm("clt"));
		return result;
	}
	
	@Override
	public CGIContainer eNewArray(VarType type, int depth, int[] cDims) throws CompileException {
		RTOSLibs.NEW_ARRAY.setRequired();
		
		CGIContainer result = new CGIContainer();
		if(VERBOSE_LO <= verbose) result.append(new CGIText(";eNewArray " + type));
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) scope.append(new CGIAsm("set"));
		
		VarType vt = type;
		while(vt.isArray()) {
			vt = vt.getElementType();
		}
		int typeSize = (-1 == vt.getSize() ? getRefSize() : vt.getSize());
		int typeBits = (0x04==typeSize ? 0x02 : typeSize-1);

		//TODO 0x02 - количество байт под размер массива
		// Флаги: 0x07-isView, 0x06-расширенный тип данных(2байта), 0x05,0x04-размер ячейки-1, 0x02-0x00-глубина-1
		int headerSize = 0x01+0x01+0x01+depth*0x02; //TODO учет только 1 байтового размера типов
		if(null!=cDims) {
			// Флаги и глубина(1 байт) + refCount(1байт) + тип данных(1байт) + размеры(2/4/6 байт) + тело(данные x байт) данных
			//ARR:
			//HEADER:
			//1B flags&depth
			//1B refCount
			//1B type
			//2B size d1
			//0/2B size d2
			//0/2B size d3
			//DATA

			int dataSize = cDims[0] * (0!=cDims[1] ? cDims[1] : 0x01) * (0!=cDims[2] ? cDims[2] : 0x01) * typeSize;

			//TODO добавить проверку переполнения
			result.append(new CGIAsm("ldi r16," + ((headerSize+dataSize)&0xff)));
			result.append(new CGIAsm("ldi r17," + (((headerSize+dataSize)>>0x08)&0xff)));
			result.append(new CGIAsm("rcall j8bproc_new_array"));
			result.append(new CGIAsm("ldi r19," + ((typeBits<<0x04)+(depth-1)))); //TODO учет только 1 байтового размера типов
			result.append(new CGIAsm("st x+,r19")); // Флаги и глубина
			result.append(new CGIAsm("st x+,C0x01")); // Количество ссылок(массив не может быть создан без присваивания)
			result.append(new CGIAsm("ldi r19," + (type.getId()&0xff))); //TODO учет только 1 байтового размера типов
			result.append(new CGIAsm("st x+,r19"));
			for(int i=0; i<depth; i++) {
				// Размер каждого измерения
				result.append(new CGIAsm("ldi r19," + (cDims[i]&0xff)));
				result.append(new CGIAsm("st x+,r19"));
				result.append(new CGIAsm("ldi r19," + ((cDims[i]>>0x08)&0xff)));
				result.append(new CGIAsm("st x+,r19"));
			}
			// Дальше идут данные массива
		}
		else {
			result.append(new CGIAsm("lds yl,SPL"));
			if(def_sph) result.append(new CGIAsm("lds yh,SPH"));
			result.append(new CGIAsm("adiw yl,0x01"));
			// Вычисляем размер данных
			if(1==depth) {
				result.append(new CGIAsm("ld r16,y+"));
				result.append(new CGIAsm("ld r17,y+"));
			}
			else {
				RTOSLibs.MATH_MUL16.setRequired();
				result.append(new CGIAsm("ldi r16,0x01"));
				result.append(new CGIAsm("ldi r17,0x00"));
				result.append(new CGIAsm("ldi j8b_atom," + depth));
				CGLabelScope lbScope = new CGLabelScope(null, null, LabelNames.LOOP, true);
				result.append(lbScope);
				result.append(new CGIAsm("ld r18,y+"));
				result.append(new CGIAsm("ld r19,y+"));
				result.append(new CGIAsm("rcall os_mul16"));
				result.append(new CGIAsm("dec j8b_atom"));
				result.append(new CGIAsmJump("brne" , lbScope));
			}
			// Учитываем размер типа
			if(0x01!=typeSize) {
				result.append(new CGIAsm("lsl r16"));
				result.append(new CGIAsm("rol r17"));
				if(0x04==typeSize) {
					result.append(new CGIAsm("lsl r16"));
					result.append(new CGIAsm("rol r17"));
				}
			}
			// Учитываем заголовок массива
			result.append(new CGIAsm("subi r16,low(-" + headerSize + ")"));
			result.append(new CGIAsm("sbci r17,high(-" + headerSize + ")"));
			result.append(new CGIAsm("rcall j8bproc_new_array"));
			result.append(new CGIAsm("ldi r19," + ((typeBits<<0x04)+depth-1))); //TODO учет только 1 байтового размера типов
			result.append(new CGIAsm("st x+,r19")); // Флаги и глубина
			result.append(new CGIAsm("st x+,C0x01")); // Количество ссылок(массив не может быть создан без присваивания)
			result.append(new CGIAsm("ldi r19," + (type.getId()&0xff))); //TODO учет только 1 байтового размера типов
			result.append(new CGIAsm("st x+,r19"));
			for(int i=0; i<depth; i++) {
				// Размер каждого измерения
				result.append(new CGIAsm("pop r19"));
				result.append(new CGIAsm("st x+,r19"));
				result.append(new CGIAsm("pop r19"));
				result.append(new CGIAsm("st x+,r19"));
			}
			// Дальше идут данные массива
		}

		accum.setSize(0x02);
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) scope.append(new CGIAsm("clt"));
		return result;
	}

	@Override
	public CGIContainer eNewArrView(int depth) throws CompileException {
		CGIContainer result = new CGIContainer();
		if(VERBOSE_LO <= verbose) result.append(new CGIText(";eNewArrView depth: " + depth));
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) scope.append(new CGIAsm("set"));

		RTOSLibs.ARRVIEW_MAKE.setRequired();
		result.append(new CGIAsm("ldi r19," + (0x80+depth)));
		result.append(new CGIAsm("rcall j8bproc_arrview_make")); // Результат в ACCUM

		accum.setSize(0x02);
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) scope.append(new CGIAsm("clt"));
		return result;
	}

	@Override
	public void invokeInterfaceMethod(CGScope scope, String className, String methodName, VarType type, VarType[] types, VarType ifaceType, int methodSN)																										throws CompileException {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";invokeInterfaceMethod " + type + " " + className + "." + methodName));
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) scope.append(new CGIAsm("set"));
		
		RTOSLibs.INVOKE_METHOD.setRequired();
		scope.append(new CGIAsm("ldi r16," + ifaceType.getId()));
		scope.append(new CGIAsm("ldi r17," + methodSN));
		scope.append(new CGIAsmJump(INSTR_CALL, "j8bproc_invoke_method_nr"));
//Похоже рудимент, код перенесен в CGBlockScope
/*		int argsSize = 0;
		for(VarType argType : types) {
			argsSize += (-1 == argType.getSize() ? getRefSize() : argType.getSize());
		}
		if(0 != argsSize) scope.append(blockFree(argsSize));
		if(0!=types.length) scope.append(popHeapReg(null, false));*/

		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) scope.append(new CGIAsm("clt"));
	}
	
	@Override
	public void nativeMethodInit(CGScope scope, String signature) throws CompileException {
		NativeBinding nb = nbMap.get(signature);
		if(null==nb) {
			throw new CompileException("Not found native method binding for method: " + signature);
		}
		
		nativeMethodRegs = nb.getRegs();
		
		includes.add(nb.getRTOSFilePath());
		if(null != nb.getRTOSFeatures()) {
			for(RTOSFeature feature : nb.getRTOSFeatures()) {
				RTOSFeatures.add(feature);
			}
		}
	}
	
	@Override
	public void nativeMethodSetArg(CGScope scope, String signature, int index) throws CompileException {
		if(index>=nativeMethodRegs.length) {
			throw new CompileException("Invalid number of arguments for '" + signature);
		}
		byte[] regs = nativeMethodRegs[index];
		if(accum.getSize()>regs.length) {
			throw new CompileException("Accum value too large for argument N" + index + " passing, method: " + signature);
		}
		
		//TODO не учитываем что регистр может уже использоваться!
		byte[] accRegs = getAccRegs(0x04);
		for(int i=0; i<regs.length; i++) {
			
			if(accum.getSize()>i) {
				if(regs[i]==accRegs[i]) continue;
				scope.append(new CGIAsm("mov r" + regs[i] + ",r" + accRegs[i]));
			}
			else {
				scope.append(new CGIAsm("ldi r" + regs[i] + ",0x00"));
			}
		}
	}
	@Override
	public void nativeMethodInvoke(CGScope scope, String signature) throws CompileException {
		if(VERBOSE_LO <= verbose) {
			scope.append(new CGIText(";invokeNative " + signature));
		}
		nativeMethodRegs = null;
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) scope.append(new CGIAsm("set"));
		scope.append(new CGIAsm("rcall "+ nbMap.get(signature).getRTOSFunction()));
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) scope.append(new CGIAsm("clt"));
	}
	
	@Override
	public void updateClassRefCount(CGScope scope, CGCells cells, boolean isInc) throws CompileException {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";class refCount" + (isInc ? "++" : "--") + " for " + cells));
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) scope.append(new CGIAsm("set"));
		
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
			case STAT:
				if(0x01 == getRefSize()) {
					scope.append(new CGIAsm("lds zl,_OS_STAT_POOL" + cells.getId(0)));
					scope.append(new CGIAsm("ldi zh,0x00"));
				}
				else {
					scope.append(new CGIAsm("lds j8b_atom,_OS_STAT_POOL+" + cells.getId(0)));
					scope.append(new CGIAsm("lds zh,_OS_STAT_POOL+" + cells.getId(1)));
					scope.append(new CGIAsm("mov zl,j8b_atom"));
				}
				break;
			default:
				throw new CompileException("Unsupported cell type:" + cells.getType());
		}

		RTOSLibs.CLASS_REFCOUNT.setRequired();
		scope.append(new CGIAsm("rcall j8bproc_class_refcount_" + (isInc ? "inc" : "dec")));
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) scope.append(new CGIAsm("clt"));
	}
	
	@Override
	public void updateArrRefCount(CGScope scope, CGCells cells, boolean isInc, boolean isView) throws CompileException {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";arr " + (isView ? "view " : "") + "refCount" + (isInc ? "++" : "--") + " for " + cells));
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) scope.append(new CGIAsm("set"));

		// Иначе адрес уже в X
		if(null != cells) {
			switch(cells.getType()) {
				case ARRAY:
					// Адрес уже в X
					break;
				case REG:
					scope.append(new CGIAsm("mov xl,r" + cells.getId(0x00)));
					if(0x01 == getRefSize()) {
						scope.append(new CGIAsm("ldi xh,0x00"));
					}
					else {
						scope.append(new CGIAsm("mov xh,r" + cells.getId(0x01)));
					}
					break;
				case ARGS:
				case STACK_FRAME:
					scope.append(new CGIAsmIReg('y', "ldd xl,y+", cells.getId(0), "", getCurBlockScope(scope), (CGBlockScope)cells.getObject(),
												CGCells.Type.ARGS == cells.getType()));
					if(0x01 == getRefSize()) {
						scope.append(new CGIAsm("ldi xh,0x00"));
					}
					else {
						scope.append(new CGIAsmIReg('y', "ldd xh,y+", cells.getId(1), ""));
					}
					break;
				case HEAP:
					if(0x01 == getRefSize()) {
						scope.append(new CGIAsmIReg('z', "ldd xl,z+", cells.getId(0), ""));
						scope.append(new CGIAsm("ldi xh,0x00"));
					}
					else {
						scope.append(new CGIAsmIReg('z', "ldd j8b_atom,z+", cells.getId(0), ""));
						scope.append(new CGIAsmIReg('z', "ldd xh,z+", cells.getId(1), ""));
						scope.append(new CGIAsm("mov xl,j8b_atom"));
					}
					break;
				case STAT:
					if(0x01 == getRefSize()) {
						scope.append(new CGIAsm("lds xl,_OS_STAT_POOL" + cells.getId(0)));
						scope.append(new CGIAsm("ldi xh,0x00"));
					}
					else {
						scope.append(new CGIAsm("lds j8b_atom,_OS_STAT_POOL+" + cells.getId(0)));
						scope.append(new CGIAsm("lds xh,_OS_STAT_POOL+" + cells.getId(1)));
						scope.append(new CGIAsm("mov xl,j8b_atom"));
					}
					break;
				default:
					throw new CompileException("Unsupported cell type:" + cells.getType());
			}
		}

		//TODO Добавить статическую реализацию(кода размер известен на уровне компиляции)
		RTOSLibs.ARR_REFCOUNT.setRequired();
		if(isView) {
			RTOSLibs.ARRVIEW_ARRADDR.setRequired();
			scope.append(new CGIAsm("rcall j8bproc_arrview_arraddr"));
			scope.append(new CGIAsm("rcall j8bproc_arr_refcount_" + (isInc ? "inc" : "dec")));
		}
		else if(isInc || !(cells instanceof CGArrCells) || !((CGArrCells)cells).canComputeStatic()) {
			scope.append(new CGIAsm("rcall j8bproc_arr_refcount_" + (isInc ? "inc" : "dec")));
		}
		else {
			int size = ((CGArrCells)cells).computeStaticSize();
			scope.append(new CGIAsm("ldi r16," + (size&0xff)));
			scope.append(new CGIAsm("ldi r17," + ((size>>0x08)&0xff)));
			scope.append(new CGIAsm("rcall j8bproc_arr_refcount_dec_const"));
		}

		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) scope.append(new CGIAsm("clt"));
	}

	@Override
	public void eInstanceof(CGScope scope, VarType type) throws CompileException {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";eInstanceOf '" + type + "'"));
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) scope.append(new CGIAsm("set"));
		RTOSLibs.INCTANCEOF.setRequired();
		
		if(0x04==accum.getSize()) scope.append(new CGIAsm("mov j8b_atom,r19"));
		scope.append(new CGIAsm("ldi r19,"+type.getId()));
		scope.append(new CGIAsm("rcall j8bproc_instanceof_nr"));
		if(0x04==accum.getSize()) scope.append(new CGIAsm("mov r19,j8b_atom"));
		
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) scope.append(new CGIAsm("clt"));
	}
	
	@Override
	public void eTry(CGBlockScope blockScope, List<Case> cases, CGBlockScope defaultBlockScope) {
		//System.out.println("CG:try, block:" + blockScope + ", casesBlocks:" + cases + ", defaultBlock:" + defaultBlockScope);
	}
	
	@Override
	public CGIContainer eReturn(CGScope scope, int argsSize, int varsSize, VarType retType) throws CompileException {
		CGIContainer result = new CGIContainer();
		int retSize = 0;
		if(null != retType && VarType.VOID != retType) {
			retSize = (-1==retType.getSize() ? getRefSize() : retType.getSize());
			result.append(accCast(null, retType));
		}


		//if(VERBOSE_LO <= verbose) result.append(new CGIText(";return, argsSize:" + argsSize + ", varsSize:" + varsSize + ", retSize:" + retSize));
		if(VERBOSE_LO <= verbose) result.append(new CGIText(";return, argsSize:" + argsSize + ", retSize:" + retSize));
//		if(0!=varsSize) {
//			result.append(new CGIAsm("pop yh"));
//			result.append(new CGIAsm("pop yl"));
//		}
		result.append(new CGIAsm("ret"));
		
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
		//TODO X также задействован в AST inlining(более приоритетно)
		//if(CodeGenerator.arraysUsage) {
			//Используем укороченный пул с резервированием X для массивов
			for(byte b : usableRegsArr) {result.add(new RegPair(b));}
		//}
		//else {
		//	for(byte b : usableRegs) {result.add(new RegPair(b));}
		//}
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
	public boolean normalizeIRegConst(CGMethodScope mScope, char iReg, AtomicInteger lastOffset) throws CompileException {
		boolean heapIRegModified = false;
		
		List<CGItem> list = new ArrayList<>();
		treeToList(mScope, list);

		int yOffset = mScope.getArgsStackSize() + 0x04;
		int zOffset = 0;
		
		if('y'==iReg) {
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
		}
		for(CGItem item : list) {
			if(item instanceof CGIAsmIReg) {
				CGIAsmIReg instr = (CGIAsmIReg)item;
				if('y'==iReg && 'y'==instr.getIreg()) {
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
				else if('z'==iReg) {
					int delta = instr.getOffset()-zOffset;
					if(MIN_IREG_OFFSET>delta) {
						moveIReg(instr.getPrefCont(), instr.getIreg(), delta-32);
						yOffset += (delta-32);
						heapIRegModified = true;
					}
					if(MAX_IREG_OFFSET<delta) {
						moveIReg(instr.getPrefCont(), instr.getIreg(), delta+32);
						yOffset += (delta+32);
						heapIRegModified = true;
					}
					instr.setOffset(instr.getOffset()-zOffset);
				}
			}
		}

		lastOffset.set('y'==iReg ? yOffset : zOffset);
	
		return heapIRegModified;
	}
	private void moveIReg(CGIContainer cont, char iReg, int offset) throws CompileException {
		cont.append(new CGIAsm("subi " + iReg + "l,low(" + offset*(-1) + ")"));
		cont.append(new CGIAsm("sbci " + iReg + "h,high(" + offset*(-1) + ")"));
	}

	@Override
	public void flashDataToArr(CGScope scope, DataSymbol symbol, int offset) throws CompileException {
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) scope.append(new CGIAsm("set"));
		RTOSLibs.ROM_READ16.setRequired();
		scope.append(new CGIAsm("push zl"));
		scope.append(new CGIAsm("push zh"));
		scope.append(new CGIAsmLdLabel("ldi zl,low", symbol.getLabel(), "*2"));
		scope.append(new CGIAsmLdLabel("ldi zh,high", symbol.getLabel(), "*2"));
		scope.append(new CGIAsm("ldi r16,low(" + symbol.getSize() + ")"));
		scope.append(new CGIAsm("ldi r17,high(" + symbol.getSize() + ")"));
		scope.append(new CGIAsm("rcall os_rom_read16_nr"));
		scope.append(new CGIAsm("pop zh"));
		scope.append(new CGIAsm("pop zl"));
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) scope.append(new CGIAsm("clt"));
	}

	private String getConstReg(CGIContainer cont, byte reg, int index, long value) throws CompileException {
		int octet = (int)(value>>(index*8)&0xff);
		switch(octet) {
			case 0x00: return "c0x00";
			case 0x01: return "c0x01";
			case 0xff: return "c0xff";
			default: {
				cont.append(new CGIAsm("ldi r"+reg+"," + octet));
				return "r"+reg;
			}
		}
	}
}
