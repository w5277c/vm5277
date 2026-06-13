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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import ru.vm5277.common.Device;
import ru.vm5277.common.ExcsThrowPoint;
import ru.vm5277.common.LabelNames;
import ru.vm5277.common.NativeBinding;
import ru.vm5277.common.NumUtils;
import ru.vm5277.common.lexer.Operator;
import static ru.vm5277.common.lexer.Operator.BIT_AND;
import static ru.vm5277.common.lexer.Operator.BIT_OR;
import static ru.vm5277.common.lexer.Operator.BIT_XOR;
import static ru.vm5277.common.lexer.Operator.EQ;
import static ru.vm5277.common.lexer.Operator.LT;
import static ru.vm5277.common.lexer.Operator.MINUS;
import static ru.vm5277.common.lexer.Operator.PLUS;
import ru.vm5277.common.Pair;
import ru.vm5277.common.compiler.Case;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.enums.RTOSFeature;
import ru.vm5277.common.enums.RTOSHaltMode;
import ru.vm5277.common.StrUtils;
import ru.vm5277.common.enums.RTOSParam;
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
import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.common.cg.items.CGIAsmCall;
import ru.vm5277.common.cg.items.CGIAsmCondJump;
import ru.vm5277.common.cg.items.CGIAsmLd;
import ru.vm5277.common.cg.items.CGIAsmMv;
import ru.vm5277.common.cg.items.CGIAsmSkipInstr;
import ru.vm5277.common.cg.scopes.CGFieldScope;
import ru.vm5277.common.cg.scopes.CGLabelScope;
import ru.vm5277.common.cg.scopes.CGMethodScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.VarType;
import ru.vm5277.common.cg.CGConstActionHandler;
import ru.vm5277.common.cg.items.CGIMeta;
import ru.vm5277.common.cg.items.CGIRAsm;
import ru.vm5277.common.enums.J8BException;
import ru.vm5277.common.exceptions.CompileException;
import java.util.Stack;
import static ru.vm5277.common.cg.CGCells.Type.ACC;
import static ru.vm5277.common.cg.CGCells.Type.ARGS;
import static ru.vm5277.common.cg.CGCells.Type.CONST;
import static ru.vm5277.common.cg.CGCells.Type.REG;
import static ru.vm5277.common.cg.CGCells.Type.STACK;
import ru.vm5277.common.enums.InstanceType;
import ru.vm5277.common.enums.RTOSLibs;
import static ru.vm5277.common.lexer.Operator.ROL;

//Для исключения путаницы - используем Little endian, т.е. 256 будет представлено в памяти как 0x00,0x01, и в регистрах r16=0x00, r17=0x01
//Сравнение числел выполняется от большего к меньшему т.е. адрес и номер регистра должен уменьшаться
//TODO похоже в коде есть ошибки - применение BE

// Блокировка диспетчера(механизм атомарности) в MULTITHREADING режиме осуществляется за счет инструкций SET и CLT, т.е. флаг T отведен конкретно под эту задачу
// и более нигде использоваться не должен! Ну разве что для исключений, вполне могут разделять один флаг без конфликтов

//TODO проверить все методы на реализацию атомарности (забыл про многобайтные переменные в записи и вычислениях)


// TODO Кодогенератор НЕ ДОЛЖЕН выполнять работу оптимизатора, т.е. не нужно формировать movw вместо двух mov и тому подобное. Так как оптимизатор справится с
// этой задачей лучше, а кодогенератор может ему помешать.

public class Generator extends CodeGenerator {
	private	final	static	String				VERSION				= StrUtils.readVersion(Generator.class);
	private	final	static	byte[]				usableRegs			= new byte[]{20,21,22,23,24,25}; //Свободные регистры дял переменных
	private			static	boolean				def_sph				= true;
	private			static	boolean				def_jmp				= true;
	private	final	static	int					MIN_IREG_OFFSET		= 0;
	private	final	static	int					MAX_IREG_OFFSET		= 63;
	private	final	static	int					IREGS_SIZE			= 2;
	public			static	String				CALL_INSTR;
	public			static	String				JUMP_INSTR;
	private					Stack<byte[][]>		nativeMethodRegs	= new Stack<>();	//Временное хранилище списка регистров для формировния вызова к нативному методу
	
	static {
		SUPPORTED_FUNCT.add("pin_hi");
		SUPPORTED_FUNCT.add("pin_lo");
		SUPPORTED_FUNCT.add("pin_invert");
		SUPPORTED_FUNCT.add("port_set_state");
		SUPPORTED_FUNCT.add("pin_in");
		SUPPORTED_FUNCT.add("pin_out");
		SUPPORTED_FUNCT.add("pin_get");
		SUPPORTED_FUNCT.add("port_set_mode");
		SUPPORTED_FUNCT.add("dispatcher_lock");
		SUPPORTED_FUNCT.add("dispatcher_unlock");
		
		accRegs = new byte[]{16,17,18,19};
		arrRegs = new byte[]{26,27};
		stackRegs = new byte[]{28,29};
		heapRegs = new byte[]{30,31};
	}
	
	public Generator(Device device) {
		super(device);
		
		codeOptimizer = new Optimizer();
		codeFixer = new Fixer();
		codeExcsChecker = new ExcsChecker();
		
		Long num = device.getDefNum("JMP_SUPPORT");
		def_jmp = (null!=num && 1==num);
		
		num = device.getDefNum("SPH");
		def_sph = (null!=num);

		CALL_INSTR = (def_jmp ? "call" : "rcall");
		JUMP_INSTR = (def_jmp ? "jmp" : "rjmp");
		
		// Собираем RTOS
	}
	
	@Override
	public void stackPrepare(CGIContainer scope, boolean firstBlock, int argsSize, int varsSize, CGExcs excs) throws CompileException {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";stack prepare, size:" + varsSize));

		if(firstBlock && (0!=argsSize || 0!=varsSize)) {
			scope.append(new CGIAsm("push", "r28")); //TODO проверить адрес ячейки
			if(def_sph) scope.append(new CGIAsm("push", "r29")); // Если SPH отсутствует, то yh всегда должен быть равен 0
			scope.append(new CGIAsmLd("lds", "r28", "SPL")); //TODO проверить адрес ячейки
			if(def_sph) scope.append(new CGIAsmLd("lds", "r29", "SPH"));
		}

		if(0!=varsSize) {
			// Не просто выделяем память, также инициализируем ее нулями
			if(varsSize<5) {
				for(int i=0; i<varsSize; i++) {
					scope.append(new CGIAsm("push", "c0x00"));
				}
//TODO Проверить в рамках общей задачи проверки переполнения стека!
			}
			else {
				// Осуществляем проверку переполнения стека, но не выполняем ее если размер <5
				setFeature(RTOSFeature.OS_FT_ETRACE);
				codeExcsChecker.stackOverflow(this, scope, excs, varsSize, def_sph ? new byte[]{29, 28} : new byte[]{29});
				if(varsSize<0x100) {
					scope.append(new CGIAsmLd("ldi", "r16", Integer.toString(varsSize)));
					scope.append(new CGIAsm("push", "c0x00"));
					scope.append(new CGIAsm("dec", "r16"));
					scope.append(new CGIAsm("brne", "pc-0x02"));
				}
				else {
					scope.append(new CGIAsmLd("ldi", "r16", "low(" + varsSize + ")"));
					scope.append(new CGIAsmLd("ldi",  "r17", "high(" + varsSize + ")"));
					scope.append(new CGIAsm("push", "c0x00"));
					scope.append(new CGIAsm("dec", "r16"));
					scope.append(new CGIAsm("brne", "pc-0x02"));
					scope.append(new CGIAsm("dec", "r17"));
					scope.append(new CGIAsm("brne", "pc-0x04"));
				}
			}
		}
	}

	@Override
	public CGIContainer blockFree(int varsSize) throws CompileException {
		CGIContainer cont = new CGIContainer();
		if(VERBOSE_LO <= verbose) cont.append(new CGIText(";block free, stack size:" + varsSize));
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) cont.append(new CGIAsm("set"));
		
		if(0!=varsSize) {
			if((def_sph && varsSize<9) || (!def_sph && varsSize<3)) {
				for(int i=0; i<varsSize; i++) {
					cont.append(new CGIAsm("pop", "j8b_atom"));
				}
			}
			else {
				if(def_sph) cont.append(new CGIAsm("cli"));
				cont.append(new CGIAsm("lds", "_SPL,SPL"));
				if(def_sph) cont.append(new CGIAsm("lds", "_SPH,SPH"));
				cont.append(new CGIAsm("subi", "_SPL,low(-" + varsSize + ")")); // Оптимизатор по возможности приведет к интрукции adiw
				if(def_sph) cont.append(new CGIAsm("sbci", "_SPH,high(-" + varsSize + ")"));
				cont.append(new CGIAsm("sts", "SPL,_SPL"));
				if(def_sph) cont.append(new CGIAsm("sts", "SPH,_SPH"));
				if(def_sph) cont.append(new CGIAsm("sei")); // На верхнем уровне прерывания всегда резрешены, проверка(сохранение бита) не требуется
			}
		}
		
		if(def_sph) cont.append(new CGIAsm("pop", "r29"));
		cont.append(new CGIAsm("pop", "r28"));
		
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
		//Не требуется, более того, никто не сбрасывает флаг T
		//if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) cont.append(new CGIAsm("set"));
		
		// Есть переменные или аргументы, значит Y сохранен в стеке и его значение - адрес SP перед локальными переменными(с корректировкой lastStackOffset)
		if(0!=varsSize || 0!=argsSize) {
			if(0!=lastStackOffset) {
				int offset = lastStackOffset+0x02+0x02+argsSize; //Находимся после Z
				if(64>offset) {
					cont.append(new CGIAsm("sbiw", "r28," + offset));
				}
				else {
					cont.append(new CGIAsm("subi", "r28,low(" + offset + ")"));
					cont.append(new CGIAsm("subi", "r29,high(" + offset + ")"));
				}
			}
			
			if(null==type) {
				cont.append(new CGIAsmIReg('z', true, null, 0)); // Восстановление изначального Z (this)
				cont.append(new CGIAsmMv("movw", "r16", "r30")); // Помещаем this в accum			
			}
			// Передаю размер аргументов
			if(128>argsSize) {
				cont.append(new CGIAsmLd("ldi", "r30", Integer.toString(argsSize)));
			}
			else if(32768>argsSize) {
				int octet = (argsSize&0x7f) + 0x80;
				cont.append(new CGIAsmLd("ldi", "r30", Integer.toString(octet)));
				cont.append(new CGIAsmLd("ldi", "r31", Integer.toString(argsSize>>0x07)));
			}
			else {
				throw new CompileException("Too big args size:" + argsSize);
			}
			RTOSLibs.METHOD_FIN_SF.setRequired();
			//Перехожу на процедуру завершения метода(с учетом Y в стеке)
			cont.append(new CGIAsmJump(JUMP_INSTR, "j8bproc_mfin_sf", true));
		}
		else {
			if(null==type) {
				cont.append(new CGIAsmIReg('z', true, null, 0)); // Восстановление изначального Z (this)
				cont.append(new CGIAsmMv("movw", "r16", "r30")); // Помещаем this в accum
			}

			RTOSLibs.METHOD_FIN.setRequired();
			cont.append(new CGIAsmJump(JUMP_INSTR, "j8bproc_mfin", true));
		}
		return cont;
	}

	@Override
	public CGIContainer pushHeapReg(CGScope scope) throws CompileException {
		CGIContainer result = new CGIContainer();
		result.append(new CGIAsm("push", "r30"));
		result.append(new CGIAsm("push", "r31"));
		if(null != scope) scope.append(result);
		return result;
	}
	@Override
	public CGIContainer popHeapReg(CGScope scope) throws CompileException {
		CGIContainer result = new CGIContainer();
		if(VERBOSE_LO <= verbose) result.append(new CGIText(";pop heap iReg"));
		result.append(new CGIAsm("pop", "r31"));
		result.append(new CGIAsm("pop", "r30"));
		if(null != scope) scope.append(result);
		return result;
	}

	@Override
	public CGIContainer pushStackReg(CGScope scope) throws CompileException {
		CGIContainer result = new CGIContainer();
		if(VERBOSE_LO <= verbose) result.append(new CGIText(";push stack iReg"));
		result.append(new CGIAsm("push", "r28"));
		result.append(new CGIAsm("push", "r29"));
		if(null != scope) scope.append(result);
		return result;
	}
	@Override
	public CGIContainer popStackReg(CGScope scope) throws CompileException {
		CGIContainer result = new CGIContainer();
		if(VERBOSE_LO <= verbose) result.append(new CGIText(";pop stack iReg"));
		result.append(new CGIAsm("pop", "r29"));
		result.append(new CGIAsm("pop", "r28"));
		if(null != scope) scope.append(result);
		return result;
	}
	
	@Override
	public CGIContainer pushArrReg(CGScope scope) throws CompileException {
		CGIContainer result = new CGIContainer();
		if(VERBOSE_LO <= verbose) result.append(new CGIText(";push arr iReg"));
		result.append(new CGIAsm("push", "r26"));
		result.append(new CGIAsm("push", "r27"));
		if(null != scope) scope.append(result);
		return result;
	}
	@Override
	public CGIContainer popArrReg(CGScope scope) throws CompileException {
		CGIContainer result = new CGIContainer();
		if(VERBOSE_LO <= verbose) result.append(new CGIText(";pop arr iReg"));
		result.append(new CGIAsm("pop", "r27"));
		result.append(new CGIAsm("pop", "r26"));
		if(null != scope) scope.append(result);
		return result;
	}

	
	@Override
	public CGIAsm pushReg(byte reg) throws CompileException {
		return new CGIAsm("push", "r" + reg);
	}
	
	@Override
	public CGIAsm popReg(byte reg) throws CompileException {
		return new CGIAsm("pop", "r" + reg);
	}
	
	@Override
	public void thisToAcc(CGScope scope) throws CompileException {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";This to acc"));
		scope.append(new CGIAsmMv("mov", "r16", "r30"));
		scope.append(new CGIAsmMv("mov", "r17", "r31"));
		acc.setBytes(0x02);
	}
	
	/**
	 * pushConst используетя для вызова метода(передачи констант)
	 * @param scope - область видимости для генерации кода
	 * @param size - необходимый размер в стеке
	 * @param value - значение константы
	 * @param isFixed - fixed значение
	 * @return 
	 * @throws ru.vm5277.common.exceptions.CompileException 
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
					cont.append(new CGIAsm("push", "c0x00"));
					break;
				case 0x01:
					cont.append(new CGIAsm("push", "c0x01"));
					break;
				case 0xff:
					cont.append(new CGIAsm("push", "c0xff"));
					break;
				default:
					if(0x04!=acc.getBytes()) {
						cont.append(new CGIAsmLd("ldi", "r19", Integer.toString(octet)));
						cont.append(new CGIAsm("push", "r19"));
					}
					else {
						cont.append(new CGIAsmLd("ldi", "r30", Integer.toString(octet)));
						cont.append(new CGIAsm("push", "r30"));
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
	 * @throws ru.vm5277.common.exceptions.CompileException
	 */
	@Override
	public void pushLabel(CGScope scope, String label) throws CompileException {
		scope.append(new CGIAsmLdLabel("ldi", "zl", "low(" + label + "*2)"));
		scope.append(new CGIAsm("push", "zl"));
		if(0x02==getRefSize()) {
			scope.append(new CGIAsmLdLabel("ldi", "zl", "high(" + label + "*2)"));
			scope.append(new CGIAsm("push", "zl"));
		}
	}
	
	
	/**
	 * pushCells используетя для вызова метода(передачи параметров)
	 * @param scope - область видимости для генерации кода
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
		
		// Сначала сохраняем старшие байты (так как LE + работа со стеком)
		int offset = 0;
		for(int i=cells.getSize(); i<size; i++) {
			cont.append(new CGIAsm("push", "c0x00"));
		}

		size = (size - cells.getSize());
		if(0!=size) {
			switch(cells.getType()) {
				case ARRAY:
					if(1<size) {
						cont.append(new CGIAsm("adiw", "xl," + size));
					}
					for(int i=offset; i<cells.getSize(); i++) { // Порядок в цикле не имеет значения
						cont.append(new CGIAsmLd("ld", "j8b_atom", "-x"));
						cont.append(new CGIAsm("push", "j8b_atom"));
					}
					break;
				case REG:
					for(int i=cells.getSize()-1; i>=0; i--) cont.append(new CGIAsm("push", "r" + cells.getId(i)));
					break;
				case ARGS:
				case STACK_FRAME:
					for(int i=cells.getSize()-1; i>=0; i--) {
						cont.append(new CGIAsmIReg(	'y', true, "j8b_atom", cells.getId(i), getCurBlockScope(scope), (CGBlockScope)cells.getObject(),
													CGCells.Type.ARGS == cells.getType()));
						cont.append(new CGIAsm("push", "j8b_atom"));
					}
					break;
				case HEAP:
					for(int i=cells.getSize()-1; i>=0; i--) {
						cont.append(new CGIAsmIReg('z', true, "j8b_atom", cells.getId(i)));
						cont.append(new CGIAsm("push", "j8b_atom"));
					}
					break;
				case STAT:
					for(int i=cells.getSize()-1; i>=0; i--) {
						cont.append(new CGIAsmLd("lds", "j8b_atom", "_OS_STAT_POOL+" + cells.getId(i)));
						cont.append(new CGIAsm("push", "j8b_atom"));
					}
					break;
				default:
					throw new CompileException("puch cells, unsupported cell type:" + cells.getType());
			}
		}
		
		
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) cont.append(new CGIAsm("clt"));
		if(null!=scope) scope.append(cont);
		return cont;
	}

	@Override
	public void constToAcc(CGScope scope, int sizeInBytes, long value, boolean isFixed) throws CompileException {
		int constBits = NumUtils.getBytesRequired(value)*8;
		if(0!=sizeInBytes) {
			if(isFixed) acc.setFixed();
			else acc.setBytes(-1==sizeInBytes ? refSize : sizeInBytes);
			if(constBits>acc.getBits()) throw new CompileException("COMPILER BUG: " + acc + " overflow by const:" + value);
		}
		else if(0==sizeInBytes) {
			if(isFixed) acc.setFixed();
			else acc.setBytes(NumUtils.getBytesRequired(value));
			if(constBits>acc.getBits()) throw new CompileException("COMPILER BUG: " + acc + " overflow by const:" + value);
		}
		else {
			if(isFixed) acc.setFixed();
			else acc.setBytes(sizeInBytes);
		}

		scope.append(new CGIAsmLd("ldi", "r16", Long.toString(value&0xff))); value >>= 0x08;
		if(acc.getBytes()>1) scope.append(new CGIAsmLd("ldi", "r17", Long.toString(value&0xff))); value >>= 0x08;
		if(acc.getBytes()>2) scope.append(new CGIAsmLd("ldi", "r18", Long.toString(value&0xff))); value >>= 0x08;
		if(acc.getBytes()>3) scope.append(new CGIAsmLd("ldi", "r19", Long.toString(value&0xff)));
	}

	@Override
	public void accToCells(CGScope scope, VarType varType, CGCellsScope cScope) throws CompileException { //Записываем acc в переменную
		// Этот метод вызывается при инициализации переменной, поля. И похоже, что легче разместить запись размера аккумулятора здесь, чем разрешать
		// работу с размером аккумуятора в FRONTEND в блоках кодогенерации init выражения
		acc.setSize(varType);
		
		scope.append(accResize()); // Необходимо при приведении типа, когда аккумулятор меньше назначения
		
		if(cScope instanceof CGVarScope) {
			CGVarScope vScope = (CGVarScope)cScope;
			if(VERBOSE_LO <= verbose) scope.append(new CGIText(";" + acc + "->var '" + vScope.getName() + "' cells size:" + cScope.getSize()));
		
			if(!vScope.isConstant()) {
				regsToVar(scope, getAccRegs(0x04), acc.getBytes(), vScope.getCells());
			}
		}
		else if(cScope instanceof CGFieldScope) {
			CGFieldScope fScope = (CGFieldScope)cScope;
			if(VERBOSE_LO <= verbose) scope.append(new CGIText(";accum->field '" + fScope.getName() + "' size:" + cScope.getSize()));
			accToField(scope, acc.getBytes(), fScope.getCells());
		}
		else if(cScope.getCells() instanceof CGArrCells) {
			if(VERBOSE_LO <= verbose) scope.append(new CGIText(";accum->arr " + cScope.getCells()));
			accToArr(scope, (CGArrCells)cScope.getCells());
		}
		else throw new CompileException("Unsupported scope:" + cScope);
	}
	
	@Override
	public void accToArrReg(CGScope scope) throws CompileException {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";" + acc + "->arrReg"));
		if(0x02<acc.getBytes()) {
			throw new CompileException("COMPILER BUG: Uxpected 0x02 size for accumulator. Developer attention required - please report this case");
		}
		scope.append(new CGIAsmMv("mov", "r26", "r16"));
		if(0x02==acc.getBytes()) {
			scope.append(new CGIAsmMv("mov", "r27", "r17"));
		}
		else {
			scope.append(new CGIAsmLd("ldi", "r27", "0x00"));
		}
	}

	@Override
	public void computeArrViewCellsAddr(CGScope scope, CGCells arrAddrCells, CGArrCells arrCells, CGExcs excs) throws CompileException { 
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";compute array addr " + arrAddrCells + "->X"));
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) scope.append(new CGIAsm("set"));
		
		if(null!=arrAddrCells) {
			switch(arrAddrCells.getType()) {
				case ACC:
					scope.append(new CGIAsmMv("mov", "r26", "r16"));
					scope.append(new CGIAsmMv("mov", "r27", "r17"));
					break;
				case REG:
					scope.append(new CGIAsmMv("mov", "r26", "r" + arrAddrCells.getId(0)));
					scope.append(new CGIAsmMv("mov", "r27", "r" + arrAddrCells.getId(1)));
					break;
				case ARGS:
				case STACK_FRAME:
					CGIContainer _cont = new CGIContainer();
						_cont.append(new CGIAsmIReg('y', true, "r26", arrAddrCells.getId(0), getCurBlockScope(scope), (CGBlockScope)arrAddrCells.getObject(),
													CGCells.Type.ARGS == arrAddrCells.getType()));
						_cont.append(new CGIAsmIReg('y', true, "r27", arrAddrCells.getId(1), getCurBlockScope(scope), (CGBlockScope)arrAddrCells.getObject(),
													CGCells.Type.ARGS == arrAddrCells.getType()));
					scope.append(_cont);
					break;
				case HEAP:
					_cont = new CGIContainer();
					_cont.append(new CGIAsmIReg('z', true, "r26", arrAddrCells.getId(0)));
					_cont.append(new CGIAsmIReg('z', true, "r27", arrAddrCells.getId(1)));
					scope.append(_cont);
					break;
				case STAT:
					scope.append(new CGIAsmLd("lds", "r26", "_OS_STAT_POOL+" + arrAddrCells.getId(0)));
					scope.append(new CGIAsmLd("lds", "r27", "_OS_STAT_POOL+" + arrAddrCells.getId(1)));
					break;
				default:
					throw new CompileException("Unsupported cell type:" + arrAddrCells.getType());
			}
		}

		RTOSLibs.ARRVIEW_CELLADDR.setRequired();
		scope.append(new CGIAsm(CALL_INSTR, "j8bproc_arrview_celladdr")); //TODO портит аккумулятор - все 4 байта
		codeExcsChecker.invalidIndex(this, scope, excs);
		
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) scope.append(new CGIAsm("clt"));
	}
	
	@Override //Диспетчер должен быть заблокирован заранее
	public void computeArrCellAddr(CGScope scope, CGCells arrAddrCells, CGArrCells arrCells, CGExcs excs) throws CompileException { 
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";compute array addr " + arrAddrCells + "->X"));
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) scope.append(new CGIAsm("set"));
		
		if(null!=arrAddrCells) {
			switch(arrAddrCells.getType()) {
				case ACC:
					scope.append(new CGIAsmMv("mov", "r26", "r16"));
					scope.append(new CGIAsmMv("mov", "r27", "r17"));
					break;
				case REG:
					scope.append(new CGIAsmMv("mov", "r26", "r" + arrAddrCells.getId(0)));
					scope.append(new CGIAsmMv("mov", "r27", "r" + arrAddrCells.getId(1)));
					break;
				case ARGS:
				case STACK_FRAME:
					CGIContainer _cont = new CGIContainer();
						_cont.append(new CGIAsmIReg('y', true, "r26", arrAddrCells.getId(0), getCurBlockScope(scope), (CGBlockScope)arrAddrCells.getObject(),
													CGCells.Type.ARGS == arrAddrCells.getType()));
						_cont.append(new CGIAsmIReg('y', true, "r27", arrAddrCells.getId(1), getCurBlockScope(scope), (CGBlockScope)arrAddrCells.getObject(),
													CGCells.Type.ARGS == arrAddrCells.getType()));
					scope.append(_cont);
					break;
				case HEAP:
					_cont = new CGIContainer();
					_cont.append(new CGIAsmIReg('z', true, "r26", arrAddrCells.getId(0)));
					_cont.append(new CGIAsmIReg('z', true, "r27", arrAddrCells.getId(1)));
					scope.append(_cont);
					break;
				case STAT:
					scope.append(new CGIAsmLd("lds", "r26", "_OS_STAT_POOL+" + arrAddrCells.getId(0)));
					scope.append(new CGIAsmLd("lds", "r27", "_OS_STAT_POOL+" + arrAddrCells.getId(1)));
					break;
				default:
					throw new CompileException("Unsupported cell type:" + arrAddrCells.getType());
			}
		}

		if(arrCells.canComputeStatic()) {
			int cellAddr = arrCells.computeStaticAddr();
			scope.append(new CGIAsm("subi", "r26,low(-" + cellAddr + ")"));
			scope.append(new CGIAsm("sbci", "r27,high(-" + cellAddr + ")"));
		}
		else {
			RTOSLibs.ARR_CELLADDR.setRequired();
			scope.append(new CGIAsm(CALL_INSTR, "j8bproc_arr_celladdr"));
			codeExcsChecker.invalidIndex(this, scope, excs);
		}
		
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) scope.append(new CGIAsm("clt"));
	}

	
	// Адрес уже должен быть в X(см. computeArrAddr)
	@Override
	public void arrToAcc(CGScope scope, CGArrCells arrCells, boolean isFixed) throws CompileException {
		int size = accumStack.isEmpty() ? arrCells.getSize() : (-1==accumStack.peek().getK().getSize() ? getRefSize() : accumStack.peek().getK().getSize());
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";arr '" + arrCells + "'->" + acc + ", size:" + size));
		byte[] accRegs = getAccRegs(size);
		//TODO почему был обратный цикл? Точно ошибка?
		for(int i=0; i<size; i++) {
			if(i<arrCells.getSize()) {
				scope.append(new CGIAsmLd("ld", "r" + accRegs[i], "x+"));
			}
			else {
				scope.append(new CGIAsmLd("ldi", "r" + accRegs[i], "0"));
			}
		}
		if(isFixed) acc.setFixed();
		else acc.setBytes(size);
	}
	
	@Override
	public void accToArr(CGScope scope, CGArrCells arrCells) throws CompileException {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";" + acc + "->arr'" + arrCells + "'"));
		int size = arrCells.getSize();
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING) && 1!=size) scope.append(new CGIAsm("set"));
		
		byte[] accRegs = getAccRegs(size);
		//TODO почему был обратный цикл? Точно ошибка?
		for(int i=0; i<size; i++) {
			scope.append(new CGIAsm("st", "x+,r" + accRegs[i]));
		}
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING) && 1!=size) scope.append(new CGIAsm("clt"));
	}

	@Override
	public void setHeapReg(CGScope scope, CGCells cells) throws CompileException {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";setHeap " + cells));
		cellsToRegs(scope, cells, getTempRegs(getCallSize()), false, false);
		//cellsActionAsm(scope, offset, cells, (int sn, String rOp) -> new CGIAsm("mov " + (0==sn ? "zl" : "zh") + "," + rOp));
	}

	@Override
	public void cellsToCells(CGScope scope, CGCells leftCells, VarType leftType, CGCells rightCells, VarType rightType) throws CompileException {
		CGIContainer cont = new CGIContainer();
				
		String info = leftCells + "[" + leftType + "]" + "=" + rightCells + "[" + rightType + "]";
		if(VERBOSE_LO <= verbose) cont.append(new CGIText(";cells " + info));

		if(CGCells.Type.ACC==leftCells.getType())  {
			leftType = accumStack.peek().getK();
		}

		int leftSize = null==leftType ? leftCells.getSize() : (-1==leftType.getSize() ? refSize : leftType.getSize());		
		int rightSize = null==rightType ? rightCells.getSize() : (-1==rightType.getSize() ? refSize : rightType.getSize());
		int size = Math.max(leftSize, rightSize);
		
		boolean leftIsFixed = null!=leftType && leftType.isFixedPoint();
		boolean rightIsFixed = null!=rightType && rightType.isFixedPoint();

		
		if(leftSize<rightSize) {
			if(CGCells.Type.ACC==leftCells.getType()) {
				// Аккумулятор можно расширить
				if(leftIsFixed) {
					acc.setFixed();
				}
				else {
					acc.setBytes(size);
				}
				cont.append(accResize());
			}
			else {
				throw new CompileException("cellTocells " + info + " left operand size < right operand size");
			}
		}
		
		// Запись 1 байта в переменную или в аккумулятор потокобезопасна
		boolean threadSafe = (1 == size || CGCells.Type.ACC == leftCells.getType());
		// Проверяем на аккумулятор левую часть если представлена в виде регистров
		if(!threadSafe && CGCells.Type.REG == leftCells.getType()) {
			boolean accRegOnly=true;
			for(int index=0; index<leftCells.getSize(); index++) {
				if(!isAccumReg((byte)leftCells.getId(index))) {
					accRegOnly = false;
					break;
				}
			}
			if(accRegOnly) threadSafe = true;
		}
		
		// Необходимо блокировать диспетчер для атомарности (если значение больше одного байта)
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING) && !threadSafe) cont.append(new CGIAsm("set"));
		
		
		byte[] accRegs = getAccRegs(size);
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
															getCurBlockScope(scope), (CGBlockScope)leftCells.getObject(),
															CGCells.Type.ARGS == leftCells.getType()));

							}
							else if(!leftIsFixed && rightIsFixed) {
								cont.append(new CGIAsmIReg(	'y', false, regsStr[index+1], leftCells.getId(index), getCurBlockScope(scope),
															(CGBlockScope)leftCells.getObject(), CGCells.Type.ARGS == leftCells.getType()));

							}
							else {
								cont.append(new CGIAsmIReg(	'y', false, regsStr[index], leftCells.getId(index), getCurBlockScope(scope),
															(CGBlockScope)leftCells.getObject(), CGCells.Type.ARGS == leftCells.getType()));

							}
							break;
						default:
							throw new CompileException("cellTocells " + info + " unsupported yet");
					}
					break;
				case ARGS:
				case STACK_FRAME:
					switch(leftCells.getType()) {
						case ARRAY:
							if(leftIsFixed && !rightIsFixed) {
								if(0!=index && index<rightSize) {
									cont.append(new CGIAsmIReg(	'y', true, "r16", rightCells.getId(index-1), getCurBlockScope(scope),
														(CGBlockScope)rightCells.getObject(), CGCells.Type.ARGS == rightCells.getType()));
									cont.append(new CGIAsm("st", "x+,r16"));
								}
								else {
									cont.append(new CGIAsm("st", "x+,c0x00"));
								}
							}
							else if(!leftIsFixed && rightIsFixed) {
								if((index+1)<rightSize) {
									cont.append(new CGIAsmIReg(	'y', true, "r16", rightCells.getId(index+1), getCurBlockScope(scope),
														(CGBlockScope)rightCells.getObject(), CGCells.Type.ARGS == rightCells.getType()));
									cont.append(new CGIAsm("st", "x+,r16"));
								}
								else {
									cont.append(new CGIAsm("st", "x+,c0x00"));
								}
							}
							else {
								if(index<rightSize) {
									cont.append(new CGIAsmIReg(	'y', true, "r16", rightCells.getId(index), getCurBlockScope(scope),
														(CGBlockScope)rightCells.getObject(), CGCells.Type.ARGS == rightCells.getType()));
									cont.append(new CGIAsm("st", "x+,r16"));
								}
								else {
									cont.append(new CGIAsm("st", "x+,c0x00"));
								}
							}
							break;
						case HEAP:
							if(leftIsFixed && !rightIsFixed) {
								if(0!=index && index<rightSize) {
									cont.append(new CGIAsmIReg(	'y', true, "r16", rightCells.getId(index-1), getCurBlockScope(scope),
																(CGBlockScope)rightCells.getObject(), CGCells.Type.ARGS == rightCells.getType()));
									cont.append(new CGIAsmIReg('z', false, "r16", leftCells.getId(index)));
								}
								else {
									cont.append(new CGIAsmIReg('z', false, "c0x00", leftCells.getId(index)));
								}
							}
							else if(!leftIsFixed && rightIsFixed) {
								if((index+1)<rightSize) {
									cont.append(new CGIAsmIReg(	'y', true, "r16", rightCells.getId(index+1), getCurBlockScope(scope),
																(CGBlockScope)rightCells.getObject(), CGCells.Type.ARGS == rightCells.getType()));
									cont.append(new CGIAsmIReg('z', false, "r16", leftCells.getId(index)));
								}
								else {
									cont.append(new CGIAsmIReg('z', false, "c0x00", leftCells.getId(index)));
								}
							}
							else {
								if(index<rightSize) {
									cont.append(new CGIAsmIReg(	'y', true, "r16", rightCells.getId(index), getCurBlockScope(scope),
																(CGBlockScope)rightCells.getObject(), CGCells.Type.ARGS == rightCells.getType()));
									cont.append(new CGIAsmIReg('z', false, "r16", leftCells.getId(index)));
								}
								else {
									cont.append(new CGIAsmIReg('z', false, "c0x00", leftCells.getId(index)));
								}
							}
							break;
						case REG:
							if(leftIsFixed && !rightIsFixed) {
								if(0!=index && index<rightSize) {
									cont.append(new CGIAsmIReg(	'y', true, "r"+leftCells.getId(index), rightCells.getId(index-1), getCurBlockScope(scope),
																(CGBlockScope)rightCells.getObject(), CGCells.Type.ARGS == rightCells.getType()));
								}
								else {
									cont.append(new CGIAsmLd("ldi", "r"+leftCells.getId(index), "0"));
								}
							}
							else if(!leftIsFixed && rightIsFixed) {
								if((index+1)<rightSize) {
									cont.append(new CGIAsmIReg(	'y', true, "r"+leftCells.getId(index), rightCells.getId(index+1), getCurBlockScope(scope),
																(CGBlockScope)rightCells.getObject(), CGCells.Type.ARGS == rightCells.getType()));
								}
								else {
									cont.append(new CGIAsmLd("ldi", "r"+leftCells.getId(index), "0"));
								}
							}
							else {
								if(index<rightSize) {
									cont.append(new CGIAsmIReg(	'y', true, "r"+leftCells.getId(index), rightCells.getId(index), getCurBlockScope(scope),
																(CGBlockScope)rightCells.getObject(), CGCells.Type.ARGS == rightCells.getType()));
								}
								else {
									cont.append(new CGIAsmLd("ldi", "r"+leftCells.getId(index), "0"));
								}
							}
							break;
						default:
							throw new CompileException("cellTocells " + info + " unsupported yet");
					}
					break;
				case ARRAY:
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
						default:
							throw new CompileException("cellTocells " + info + " unsupported yet");
					}
					break;
				case CONST:
					switch(leftCells.getType()) {
						case ARRAY:
							if(leftIsFixed && !rightIsFixed) {
								if(0!=index && index<=rightCells.getSize()) {
									cont.append(new CGIAsm("st", "x+," + getConstReg(cont, (byte)19, rightCells.getId(index-1))));
								}
								else {
									cont.append(new CGIAsm("st", "x+,c0x00"));
								}
							}
							else if(!leftIsFixed && rightIsFixed) {
								if((index+1)<rightCells.getSize()) {
									cont.append(new CGIAsm("st", "x+," + getConstReg(cont, (byte)19, rightCells.getId(index+1))));
								}
								else {
									cont.append(new CGIAsm("st", "x+,c0x00"));
								}
							}
							else {
								if(index<rightCells.getSize()) {
									cont.append(new CGIAsm("st", "x+," + getConstReg(cont, (byte)19, rightCells.getId(index))));
								}
								else {
									cont.append(new CGIAsm("st", "x+,c0x00"));
								}
							}
							break;
						case HEAP:
							if(leftIsFixed && !rightIsFixed) {
								if(0!=index && index<=rightCells.getSize()) {
									String reg = getConstReg(cont, (byte)19, rightCells.getId(index-1));
									cont.append(new CGIAsmIReg('z', false, reg, leftCells.getId(index)));
								}
								else {
									cont.append(new CGIAsmIReg('z', false, "c0x00", leftCells.getId(index)));
								}
							}
							else if(!leftIsFixed && rightIsFixed) {
								if((index+1)<rightCells.getSize()) {
									String reg = getConstReg(cont, (byte)19, rightCells.getId(index+1));
									cont.append(new CGIAsmIReg('z', false, reg, leftCells.getId(index)));
								}
								else {
									cont.append(new CGIAsmIReg('z', false, "c0x00", leftCells.getId(index)));
								}
							}
							else {
								if(index<rightCells.getSize()) {
									String reg = getConstReg(cont, (byte)19, rightCells.getId(index));
									cont.append(new CGIAsmIReg('z', false, reg, leftCells.getId(index)));
								}
								else {
									cont.append(new CGIAsmIReg('z', false, "c0x00", leftCells.getId(index)));
								}
							}
							break;
						case HEAP_ALT:
							if(0==index && 0!=leftCells.getId(0)) {
								cont.append(new CGIAsm("subi", "r26,low(-" + leftCells.getId(0) + ")"));
								cont.append(new CGIAsm("sbci", "r27,high(-" + leftCells.getId(0) + ")"));
							}
							if(leftIsFixed && !rightIsFixed) {
								if(0!=index && index<=rightCells.getSize()) {
									cont.append(new CGIAsm("st", "x+," + getConstReg(cont, (byte)19, rightCells.getId(index-1))));
								}
								else {
									cont.append(new CGIAsm("st", "x+,c0x00"));
								}
							}
							else if(!leftIsFixed && rightIsFixed) {
								if((index+1)<rightCells.getSize()) {
									cont.append(new CGIAsm("st", "x+," + getConstReg(cont, (byte)19, rightCells.getId(index+1))));
								}
								else {
									cont.append(new CGIAsm("st", "x+,c0x00"));
								}
							}
							else {
								if(index<rightCells.getSize()) {
									cont.append(new CGIAsm("st", "x+," + getConstReg(cont, (byte)19, rightCells.getId(index))));
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
							if(leftIsFixed && !rightIsFixed) {
								if(0!=index && index<=rightCells.getSize()) {
									String reg = getConstReg(cont, (byte)19, rightCells.getId(index-1));
									cont.append(new CGIAsm("sts", "_OS_STAT_POOL+" + leftCells.getId(index) + "," + reg));
								}
								else {
									cont.append(new CGIAsm("sts", "_OS_STAT_POOL+" + leftCells.getId(index) + ",C0x00"));
								}
							}
							else if(!leftIsFixed && rightIsFixed) {
								if((index+1)<rightCells.getSize()) {
									String reg = getConstReg(cont, (byte)19, rightCells.getId(index+1));
									cont.append(new CGIAsm("sts", "_OS_STAT_POOL+" + leftCells.getId(index) + "," + reg));
								}
								else {
									cont.append(new CGIAsm("sts", "_OS_STAT_POOL+" + leftCells.getId(index) + ",C0x00"));
								}
							}
							else {
								if(index<rightCells.getSize()) {
									String reg = getConstReg(cont, (byte)19, rightCells.getId(index));
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
									cont.append(new CGIAsmIReg(	'y', false, getConstReg(cont, (byte)19, rightCells.getId(index-1)), leftCells.getId(index),
																getCurBlockScope(scope), (CGBlockScope)leftCells.getObject(),
																CGCells.Type.ARGS == leftCells.getType()));
								}
								else {
									cont.append(new CGIAsmIReg(	'y', false, "c0x00", leftCells.getId(index),
																getCurBlockScope(scope), (CGBlockScope)leftCells.getObject(),
																CGCells.Type.ARGS == leftCells.getType()));
								}

							}
							else if(!leftIsFixed && rightIsFixed) {
								if((index+1)<rightCells.getSize()) {
									cont.append(new CGIAsmIReg(	'y', false, getConstReg(cont, (byte)19, rightCells.getId(index+1)), leftCells.getId(index),
																getCurBlockScope(scope), (CGBlockScope)leftCells.getObject(),
																CGCells.Type.ARGS == leftCells.getType()));
								}
								else {
									cont.append(new CGIAsmIReg(	'y', false, "c0x00", leftCells.getId(index),
																getCurBlockScope(scope), (CGBlockScope)leftCells.getObject(),
																CGCells.Type.ARGS == leftCells.getType()));
								}
							}
							else {
								if(index<rightCells.getSize()) {
									cont.append(new CGIAsmIReg(	'y', false, getConstReg(cont, (byte)19, rightCells.getId(index)), leftCells.getId(index),
																getCurBlockScope(scope), (CGBlockScope)leftCells.getObject(),
																CGCells.Type.ARGS == leftCells.getType()));
								}
								else {
									cont.append(new CGIAsmIReg(	'y', false, "c0x00", leftCells.getId(index),
																getCurBlockScope(scope), (CGBlockScope)leftCells.getObject(),
																CGCells.Type.ARGS == leftCells.getType()));
								}
							}
							break;
						default:
							throw new CompileException("cellTocells " + info + " unsupported yet");
					}
					break;
				case REG:
					switch(leftCells.getType()) {
						case ACC:
							if(leftIsFixed && !rightIsFixed) {
								if(0!=index && index<rightSize) {
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
									cont.append(new CGIAsmIReg(	'y', false, "r"+rightCells.getId(index-1), leftCells.getId(index), getCurBlockScope(scope),
																(CGBlockScope)leftCells.getObject(), CGCells.Type.ARGS == leftCells.getType()));
								}
								else {
									cont.append(new CGIAsmLd("ldi", "r"+rightCells.getId(index), "0"));
								}
							}
							else if(!leftIsFixed && rightIsFixed) {
								if((index+1)<rightSize) {
									cont.append(new CGIAsmIReg(	'y', false, "r"+rightCells.getId(index+1), leftCells.getId(index), getCurBlockScope(scope),
																(CGBlockScope)leftCells.getObject(), CGCells.Type.ARGS == leftCells.getType()));
								}
								else {
									cont.append(new CGIAsmLd("ldi", "r"+rightCells.getId(index), "0"));
								}
							}
							else {
								if(index<rightSize) {
									cont.append(new CGIAsmIReg(	'y', false, "r"+rightCells.getId(index), leftCells.getId(index), getCurBlockScope(scope),
																(CGBlockScope)leftCells.getObject(), CGCells.Type.ARGS == leftCells.getType()));
								}
								else {
									cont.append(new CGIAsmLd("ldi", "r"+rightCells.getId(index), "0"));
								}
							}
							break;
						case ARRAY:
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
							throw new CompileException("cellTocells " + info + " unsupported yet");
					}
					break;
				case STAT:
					switch(leftCells.getType()) {
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
							throw new CompileException("cellTocells " + info + " unsupported yet");
					}
					break;
				case HEAP_ALT:
					switch(leftCells.getType()) {
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
							throw new CompileException("cellTocells " + info + " unsupported yet");
					}
					break;
				case HEAP:
					switch(leftCells.getType()) {
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
							throw new CompileException("cellTocells " + info + " unsupported yet");
					}
					break;
				default:
					throw new CompileException("cellTocells " + info + " unsupported yet");
			}
		}
		
		// Снимаем блокировку диспетчера
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING) && !threadSafe) cont.append(new CGIAsm("clt"));		
		scope.append(cont);
	}
	
	@Override
	public void cellsToAcc(CGScope scope, CGCellsScope cScope) throws CompileException {
		if(CGCells.Type.LABEL==cScope.getCells().getType()) cScope.getCells().setSize(0x02);
		
		if(cScope instanceof CGVarScope) {
			CGVarScope vScope = (CGVarScope)cScope;
//			if(!vScope.isConstant()) {
				// При жесткой блокировке аккумулятора устанавливаем размер указанный при блокировке, даже если размер переменной больше
				if(!accumStack.isEmpty() && accumStack.peek().getV()) {
					if(accumStack.peek().getK().isFixedPoint()) {
						acc.setFixed();
					}
					else {
						acc.setSize(accumStack.peek().getK());
					}
				}
				else {
					if(vScope.getType().isFixedPoint()) {
						acc.setFixed();
					}
					else {
						acc.setBytes(vScope.getCells().getSize());
					}
				}
				if(VERBOSE_LO <= verbose) scope.append(new CGIText(	";var '" + vScope.getName() + "->" + acc + ", cells size:" + vScope.getCells().getSize()));
//				//TODO расширение аккумулятора. Не уверен что на своем месте
//				if(accum.getSize()<vScope.getCells().getSize()) accum.setSize(vScope.getCells().getSize());
				//cellsToRegs(scope, vScope.getCells(), getAccRegs(accum.getSize()));
				// Более того, сама запись значения задает размер аккумулятора
				cellsToRegs(scope, vScope.getCells(), getAccRegs(acc.getBytes()), vScope.getType().isFixedPoint(), acc.getFixed());
//			}
		}
		else if(cScope instanceof CGFieldScope) {
			CGFieldScope fScope = (CGFieldScope)cScope;
			// При жесткой блокировке аккумулятора устанавливаем размер указанный при блокировке, даже если размер поля больше
			if(!accumStack.isEmpty() && accumStack.peek().getV()) {
				if(accumStack.peek().getK().isFixedPoint()) {
					acc.setFixed();
				}
				else {
					acc.setSize(accumStack.peek().getK());
				}
			}
			else {
				if(fScope.getType().isFixedPoint()) {
					acc.setFixed();
				}
				else {
					acc.setBytes(fScope.getCells().getSize());
				}
			}
			if(VERBOSE_LO <= verbose) scope.append(new CGIText(";field '" + fScope.getName() + "'->" + acc + " , cells size:" + fScope.getCells().getSize()));
//			//TODO расширение аккумулятора. Не уверен что на своем месте
//			if(accum.getSize()<fScope.getCells().getSize()) accum.setSize(fScope.getCells().getSize());
			// Более того, сама запись значения задает размер аккумулятора
			cellsToRegs(scope, fScope.getCells(), getAccRegs(acc.getBytes()), fScope.getType().isFixedPoint(), acc.getFixed());
		}
		else throw new CompileException("Unsupported scope:" + cScope);
	}
	
	@Override
	public void cellsToAcc(CGScope scope, CGCells cells, boolean isFixed) throws CompileException {
		if(CGCells.Type.LABEL==cells.getType()) cells.setSize(0x02);
		if(isFixed) acc.setFixed();
		else acc.setBytes(cells.getSize());
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";cells '" + cells + "'->" + acc));
		cellsToRegs(scope, cells, getAccRegs(cells.getSize()), acc.getFixed(), acc.getFixed());
	}
	
	@Override
	public void cellsToArrReg(CGScope scope, CGCells cells) throws CompileException {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";cells '" + cells + "'->ArrReg"));
		cellsToRegs(scope, cells, new byte[]{(byte)26,(byte)27}, false, false);
	}
	
	@Override
	public void arrSizetoAcc(CGScope cgScope, boolean isView) throws CompileException {
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) cgScope.append(new CGIAsm("set"));
		
		RTOSLibs.ARR_SIZE.setRequired();
		if(isView) {
			RTOSLibs.ARRVIEW_ARRADDR.setRequired();
			cgScope.append(new CGIAsmCall(CALL_INSTR, "j8bproc_arrview_arraddr", true));
		}
		cgScope.append(new CGIAsmCall(CALL_INSTR, "j8bproc_arr_size", true));
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) cgScope.append(new CGIAsm("clt"));
	}
	
	@Override
	public CGIContainer jump(CGScope scope, CGLabelScope lbScope) throws CompileException {
		CGIContainer result = new CGIContainer();
		// Только здесь мы точно уверены, что метка будет использована.
		lbScope.setUsed();
		result.append(new CGIAsmJump(JUMP_INSTR, lbScope, false));
		if(null!=scope) scope.append(result);
		return result;
	}
	@Override
	public CGIContainer call(CGScope scope, CGLabelScope lbScope) throws CompileException {
		CGIContainer result = new CGIContainer();
		// Только здесь мы точно уверены, что метка будет использована.
		lbScope.setUsed();
		result.append(new CGIAsmCall(CALL_INSTR, lbScope.getName(), false));
		if(null!=scope) scope.append(result);
		return result;
	}

/*	@Override
	public CGIContainer accCast(VarType accType, VarType opType) throws CompileException {
		CGIContainer cont = new CGIContainer();
		int accSize = null == accType ? acc.getSize() : (-1==accType.getSize() ? getRefSize() : accType.getSize());
		int opSize = (-1==opType.getSize() ? getRefSize() : opType.getSize());
		if(VERBOSE_LO <= verbose) {
			if(accSize != opSize || VarType.FIXED == accType && VarType.FIXED != opType || VarType.FIXED != accType && VarType.FIXED == opType) {
				cont.append(new CGIText(";acc cast " + accType + "[" + accSize + "]->" + opType + "[" + opSize + "]"));
			}
		}
		
		if(null != accType) {
			if(VarType.FIXED == accType && VarType.FIXED != opType) {
				cont.append(new CGIAsmMv("mov", "r16", "r17"));
				cont.append(new CGIAsmLd("ldi", "r17", "0x00"));
				cont.append(new CGIAsm("sbrc", "r16,0x07"));
				cont.append(new CGIAsm("inc", "r16"));
				accSize = 0x02;
			}
			else if(VarType.FIXED != accType && VarType.FIXED == opType) {
				cont.append(new CGIAsmMv("mov", "r17" , "r16"));
				cont.append(new CGIAsmLd("ldi", "r16", "0x00"));
				accSize = 0x02;
			}
		}
		cont.append(accgrow(accSize, opSize, VarType.FIXED==opType));
		return cont;
	}*/
	
	private CGIContainer accResize() throws CompileException {
		CGIContainer cont = new CGIContainer();
		if(acc.isChanged()) {
			if(VERBOSE_LO <= verbose) {
				cont.append(new CGIText(";acc cast " + acc.oldToString() + "->" + acc.toString()));
			}
		
			if(acc.getOldFixed() && !acc.getFixed()) {
				cont.append(new CGIAsmMv("mov", "r16", "r17"));
				cont.append(new CGIAsmLd("ldi", "r17", "0x00"));
				cont.append(new CGIAsm("sbrc", "r16,0x07"));
				cont.append(new CGIAsm("inc", "r16"));
			}
			else if(!acc.getOldFixed() && acc.getFixed()) {
				cont.append(new CGIAsmMv("mov", "r17" , "r16"));
				cont.append(new CGIAsmLd("ldi", "r16", "0x00"));
			}
		}

		if(!acc.getFixed()) {
			accGrow(cont, acc.getOldBytes(), acc.getBytes());
		}
		return cont;
	}
	
	private void accGrow(CGIContainer cont, int oldSize, int newSize) throws CompileException {
		if(oldSize<newSize) {
			byte[] accRegs = getAccRegs(4);
			for(int i=oldSize; i<newSize; i++) {
				cont.append(new CGIAsmLd("ldi", "r" + accRegs[i], "0"));
			}
			acc.setBytes(newSize);
		}
	}

	public void cellsToRegs(CGScope scope, CGCells cells, byte[] registers, boolean leftIsFixed, boolean rightIsFixed) throws CompileException {
		//Нет необходимости блокировать диспетчер
		
		int size = (registers.length<cells.getSize() ? registers.length : cells.getSize());

		int leftIndex=0x00;
		int rightIndex=0x00;
		if(leftIsFixed && !rightIsFixed) {
			leftIndex++;
			size=0x01;
		}
		else if(!leftIsFixed && rightIsFixed) {
			scope.append(new CGIAsmLd("ldi", "r" + registers[0], "0x00"));
			rightIndex++;
			size=0x01;
		}
		
		switch(cells.getType()) {
			case LABEL:
				if(leftIsFixed) throw new CompileException("COMPILER ERROR: Can't set label addr to fixed var");
				scope.append(new CGIAsmLdLabel("ldi", "r" + registers[0x00], "low(" + (String)cells.getObject() + "*2)"));
				scope.append(new CGIAsmLdLabel("ldi", "r" + registers[0x01], "high(" + (String)cells.getObject() + "*2)"));
				break;
			case ACC:
				byte [] accRegs = getAccRegs(registers.length);
				for(int i=0; i<size; i++) {
					if(accRegs[leftIndex+i]!=registers[rightIndex+i]) {
						scope.append(new CGIAsmMv("mov", "r" + registers[rightIndex+i], "r" + accRegs[leftIndex+i]));
					}
				}
				break;
			case REG:
				LinkedList<Byte> popRegs = new LinkedList<>();
				for(int i=0; i<size; i++) {
					byte srcReg = (byte)cells.getId(leftIndex+i);
					byte dstReg = registers[rightIndex+i];
					if(srcReg == dstReg) continue; // регистры совпались, ничего не делаем
					if(!popRegs.contains(dstReg)) {
						int pos = getCellsPos(cells, dstReg);
						if(pos>i) {
							scope.append(new CGIAsm("push", "r" + dstReg));
							popRegs.addFirst(srcReg);
						}
						scope.append(new CGIAsmMv("mov", "r" + dstReg, "r" + srcReg));
					}
				}
				for(Byte reg : popRegs) scope.append(new CGIAsm("pop", "r" + reg));
				break;
			case STACK_FRAME:
			case ARGS:
				for(int i=0; i<size; i++) {
					scope.append(new CGIAsmIReg('y', true, "r" + registers[rightIndex+i], cells.getId(leftIndex+i), getCurBlockScope(scope),
												(CGBlockScope)cells.getObject(), CGCells.Type.ARGS == cells.getType()));
				}
				break;
			case HEAP:
				for(int i=0; i<size; i++) {
					scope.append(new CGIAsmIReg('z', true, "r" + registers[rightIndex+i], cells.getId(leftIndex+i)));
				}
				break;
			case HEAP_ALT:
				if(0!=cells.getId(0)) {
					scope.append(new CGIAsm("subi", "r26,low(-" + (cells.getId(0)&0xff) + ")"));
					scope.append(new CGIAsm("sbci", "r27,high(-" + ((cells.getId(0)>>8)&0xff) + ")"));
				}
				for(int i=0; i<size; i++) {
					scope.append(new CGIAsmLd("ld", "r" + registers[rightIndex+i], "x+"));
				}
				break;
			case STAT:
				for(int i=0; i<size; i++) {
					scope.append(new CGIAsmLd("lds", "r" + registers[rightIndex+i], "_OS_STAT_POOL+" + cells.getId(leftIndex+i)));
				}
				break;
			default:
				throw new CompileException("Unsupported cell type:" + cells.getType());
		}

		for(int i=size; i<registers.length-rightIndex; i++) {
			scope.append(new CGIAsmLd("ldi", "r" + registers[i], "0x00"));
		}
	}

	@Override
	public CGIContainer constToCells(CGScope scope, long value, CGCells cells, boolean isFixed) throws CompileException {
		CGIContainer cont = new CGIContainer();
		if(VERBOSE_LO <= verbose) cont.append(new CGIText(";const '" + (isFixed ? value/256.0 : value) + "'->cells " + cells));
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) cont.append(new CGIAsm("set"));

		byte tmpReg=19;
		if(0x04==acc.getBytes() && CGCells.Type.REG!=cells.getType()) {
			tmpReg = (CGCells.Type.HEAP == cells.getType() ? (byte)28 : (byte)30);
			if(19!=tmpReg) cont.append(new CGIAsmMv("mov", "j8b_atom", "r"+tmpReg));
		}

		switch(cells.getType()) {
			case REG: //TODO при cells.getSize()==1 атомарность не нужна
				for(int i=0; i<cells.getSize(); i++) cont.append(new CGIAsmLd("ldi", "r" + cells.getId(i), Long.toString(value>>(i*8)&0xff)));
				break;
			case ARGS:
			case STACK_FRAME:
				for(int i=0; i<cells.getSize(); i++) {
					String cReg = getConstReg(cont, tmpReg, i, value);
					cont.append(new CGIAsmIReg('y', false, cReg, cells.getId(i), getCurBlockScope(scope), (CGBlockScope)cells.getObject(),
												CGCells.Type.ARGS == cells.getType()));
				}
				break;
			case HEAP:
				for(int i=0; i<cells.getSize(); i++) {
					String cReg = getConstReg(cont, tmpReg, i, value);
					cont.append(new CGIAsmIReg('z', false, cReg, cells.getId(i)));
				}
				break;
			case HEAP_ALT:
				if(0!=cells.getId(0)) {
					cont.append(new CGIAsm("subi", "r26,low(-" + cells.getId(0) + ")"));
					cont.append(new CGIAsm("sbci", "r27,high(-" + cells.getId(0) + ")"));
				}
				for(int i=0; i<cells.getSize(); i++) {
					String cReg = getConstReg(cont, tmpReg, i, value);
					cont.append(new CGIAsm("st", "x+,"+cReg)); //TODO вероятно нужно поменять порядок в цикле (аналогично ARRAY)
				}
				break;
			case ARRAY:
				CGArrCells arrCells = (CGArrCells)cells;
				int size = arrCells.getSize();
				//TODO почему был обратный порядок? 
				for(int i=0; i<size; i++) {
					String cReg = getConstReg(cont, tmpReg, i, value);
					cont.append(new CGIAsm("st", "x+,"+cReg));
				}
				break;
			case STAT:
				for(int i=0; i<cells.getSize(); i++) {
					String cReg = getConstReg(cont, tmpReg, i, value);
					cont.append(new CGIAsm("sts", "_OS_STAT_POOL+" + cells.getId(i) + ","+cReg));
				}
				break;
			default:
				throw new CompileException("Unsupported cell type:" + cells.getType());
		}

		if(19!=tmpReg) cont.append(new CGIAsmMv("mov", "r"+tmpReg, "j8b_atom"));
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) cont.append(new CGIAsm("clt"));
		return cont;
	}

	
	private void regsMv(CGScope scope, byte[] srcRegs, byte[] dstRegs) throws CompileException {
		LinkedList<Byte> popRegs = new LinkedList<>();
		for(int i=0; i<Math.min(srcRegs.length, dstRegs.length); i++) {
			byte srcReg = srcRegs[i];
			byte dstReg = dstRegs[i];
			if(srcReg==dstReg) continue; // регистры совпались, ничего не делаем
			int pos = getRegPos(srcRegs, i, dstReg);
			if(-1==pos) {
				scope.append(new CGIAsmMv("mov", "r" + dstReg, "r" + srcReg));
			}
			else {
				scope.append(new CGIAsm("push", "r" + srcReg));
				popRegs.addFirst(dstReg);
			}
		}
		for(Byte reg : popRegs) scope.append(new CGIAsm("pop", "r" + reg));
/*			if(!popRegs.contains(dstReg)) {
				int pos = getRegPos(srcRegs, i, dstReg);
				if(pos>i) {
					scope.append(new CGIAsm("push", "r" + dstReg));
					popRegs.addFirst(srcReg);
				}
				scope.append(new CGIAsmMv("mov", "r" + dstReg, "r" + srcReg));
			}
		}
		for(Byte reg : popRegs) scope.append(new CGIAsm("pop", "r" + reg));*/
	}
	
	private byte[] cellsToRegs(CGCells cells) {
		byte[] result = new byte[cells.getSize()];
		for(int i=0; i<result.length; i++) {
			result[i] = (byte)cells.getId(i);
		}
		return result;
	}
	
	/**
	 * regsToVar записывает регистры в ячейки локальной переменной 
	 * @param scope - область видимости для генерации кода
	 * @param regs - полный набор регистров переменной/поля/аккумулятора
	 * @param usedRegsQnt - количество действительно используемых регистров, в не используемые будет записан 0x00
	 * @param cells - ячейки указывающие источник данных
	 * @throws CompileException
	 */
	
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
				regsMv(scope, regs, cellsToRegs(cells));
/*				LinkedList<Byte> popRegs = new LinkedList<>();
				for(int i=0; i<cells.getSize(); i++) {
					byte srcReg = regs[i];
					byte dstReg = (byte)cells.getId(i);
					if(srcReg == dstReg) continue; // регистры совпались, ничего не делаем
					if(!popRegs.contains(dstReg)) {
						int pos = getRegPos(regs, i, dstReg);
						if(pos>i) {
							scope.append(new CGIAsm("push", "r" + dstReg));
							popRegs.addFirst(srcReg);
							//continue; //TODO проверить
						}
						scope.append(new CGIAsmMv("mov", "r" + dstReg, "r" + srcReg));
					}
				}
				for(Byte reg : popRegs) scope.append(new CGIAsm("pop", "r" + reg));*/
				break;
			case ARGS:
			case STACK_FRAME:
				for(int i=0; i<cells.getSize(); i++) {
					scope.append(new CGIAsmIReg('y', false, "r" + regs[i], cells.getId(i), getCurBlockScope(scope),
												(CGBlockScope)cells.getObject(), CGCells.Type.ARGS == cells.getType()));
				}
				break;
			case STAT:
				for(int i=0; i<cells.getSize(); i++) {
					scope.append(new CGIAsm("sts", "_OS_STAT_POOL+" + cells.getId(i) + ",r" + regs[i]));
				}
				break;
			default:
				throw new CompileException("Unsupported cell type:" + cells.getType());
		}
		
		for(int i=cells.getSize(); i<usedRegsQnt; i++) {
			scope.append(new CGIAsmLd("ldi", "r" + cells.getId(i), "0x00"));
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
			scope.append(new CGIAsm("sts", "_OS_STAT_POOL+" + cells.getId(0) + ",r16"));
			if(size>1) scope.append(new CGIAsm("sts", "_OS_STAT_POOL+" + cells.getId(1) + ",r17"));
			if(size>2) scope.append(new CGIAsm("sts", "_OS_STAT_POOL+" + cells.getId(2) + ",r18"));
			if(size>3) scope.append(new CGIAsm("sts", "_OS_STAT_POOL+" + cells.getId(3) + ",r19"));
		}
		else if(CGCells.Type.HEAP==cells.getType()) {
			scope.append(new CGIAsmIReg('z', false, "r16", cells.getId(0)));
			if(size>1) scope.append(new CGIAsmIReg('z', false, "r17", cells.getId(1)));
			if(size>2) scope.append(new CGIAsmIReg('z', false, "r18", cells.getId(2)));
			if(size>3) scope.append(new CGIAsmIReg('z', false, "r19", cells.getId(3)));
		}
		else {
			throw new CompileException("Unsupported cell type:" + cells.getType());
		}
		
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING) && 1!=size) scope.append(new CGIAsm("clt"));
	}

	@Override
	public CGIContainer pushAccBE(CGScope scope, int size) throws CompileException {
		CGIContainer result = new CGIContainer();
		if(VERBOSE_LO <= verbose) result.append(new CGIText(";push accum(BE), size:" + size));
		byte[] regs = getAccRegs(size);
		for(int i=regs.length-1; i>=0; i--) {
			result.append(new CGIAsm("push", "r" + regs[i]));
		}
		if(null != scope) scope.append(result);
		return result;
	}

	@Override
	public CGIContainer pushAccLE(CGScope scope, int size) throws CompileException {
		CGIContainer result = new CGIContainer();
		if(VERBOSE_LO <= verbose) result.append(new CGIText(";push accum(LE), size:" + size));
		byte[] regs = getAccRegs(size);
		for(int i=0; i<regs.length; i++) {
			if(i>(acc.getBytes()-0x01)) {
				result.append(new CGIAsm("push", "C0x00"));
			}
			else {
				result.append(new CGIAsm("push", "r" + regs[i]));
			}
		}
		if(null != scope) scope.append(result);
		return result;
	}

	@Override
	public void popAccBE(CGScope scope, int size) throws CompileException {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";pop accum(BE)"));
		byte[] regs = getAccRegs(size);
		for(int i=0; i<regs.length; i++) scope.append(new CGIAsm("pop", "r" + regs[i]));
	}

	//TODO актуализировать
	@Override
	public void eUnary(CGScope scope, Operator op, CGCells cells, boolean toAccum, CGExcs excs) throws CompileException {
		//3 блока: 1 считывает значение, 2 выполняет опреацию, 3 записывает значение
		//для регистров все просто, для аккумулятора не доступны POST* и PRE*
		//POST и PRE влияют на аккумулятор, возвращая перед операцией значение или после операции.

		int accMaxBits = (null==codeExcsChecker.getHandled(excs, J8BException.MathOverflowException.name()) ? 
							(-1==accumStack.peek().getK().getSize() ? refSize : accumStack.peek().getK().getSize())*0x08 : 0x20);
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";eUnary " + op + " cells " + cells + (toAccum ? "->" + acc + "/" + accMaxBits : "")));

		if(	CGCells.Type.REG != cells.getType() && CGCells.Type.ARGS != cells.getType() && CGCells.Type.STACK_FRAME != cells.getType() &&
			CGCells.Type.HEAP != cells.getType() && CGCells.Type.ARRAY != cells.getType() && CGCells.Type.STAT != cells.getType()) {
			
			throw new CompileException("Unsupported cell type:" + cells.getType());
		}

		byte tmpReg=19;
		if(0x04==acc.getBytes() && CGCells.Type.REG!=cells.getType()) {
			tmpReg = (CGCells.Type.HEAP == cells.getType() ? (byte)28 : (byte)30);
			scope.append(new CGIAsm("push", "r" + tmpReg));
		}
		
		byte[] accRegs = getAccRegs(cells.getSize());
		if(Operator.PRE_INC == op || Operator.PRE_DEC == op || Operator.POST_INC == op || Operator.POST_DEC == op) {
			if(Operator.PRE_INC == op || Operator.POST_INC == op) {
				if(!acc.getFixed()) {
					int requiredBits = acc.getBits()+1;
					acc.setBits(requiredBits>accMaxBits ? accMaxBits : requiredBits);
					scope.append(accResize());
				}
			}
			switch(cells.getType()) {
				case ARRAY:
					for(int i=0; i<cells.getSize(); i++) {
						scope.append(new CGIAsmLd("ld", "r" + tmpReg, "x"));
						if(toAccum && (Operator.POST_INC == op || Operator.POST_DEC == op)) {
							scope.append(new CGIAsmMv("mov", "r" + accRegs[i], "r" + tmpReg));
						}
						if(0==i) scope.append(new CGIAsm((Operator.PRE_INC==op || Operator.POST_INC==op ? "add" : "sub"), "r" + tmpReg + ",C0x01"));
						else scope.append(new CGIAsm((Operator.PRE_INC==op || Operator.POST_INC==op ? "adc" : "sbc"), "r" + tmpReg + ",C0x00"));
						codeExcsChecker.mathOverflow(this, scope, excs, op, null, false, false);
						if(toAccum && (Operator.PRE_INC == op || Operator.PRE_DEC == op)) {
							scope.append(new CGIAsmMv("mov", "r" + accRegs[i], "r" + tmpReg));
						}
						scope.append(new CGIAsm("st", "x+,r" + tmpReg));
					}
					break;
				case STAT:
					for(int i=0; i<cells.getSize(); i++) {
						scope.append(new CGIAsmLd("lds", "r" + tmpReg, "_OS_STAT_POOL+" + cells.getId(i)));
						if(toAccum && (Operator.POST_INC == op || Operator.POST_DEC == op)) {
							scope.append(new CGIAsmMv("mov", "r" + accRegs[i], "r" + tmpReg));
						}
						if(0==i) scope.append(new CGIAsm((Operator.PRE_INC==op || Operator.POST_INC==op ? "add" : "sub"), "r" + tmpReg + ",C0x01"));
						else scope.append(new CGIAsm((Operator.PRE_INC==op || Operator.POST_INC==op ? "adc" : "sbc"), "r" + tmpReg + ",C0x00"));
						codeExcsChecker.mathOverflow(this, scope, excs, op, null, false, false);
						if(toAccum && (Operator.PRE_INC == op || Operator.PRE_DEC == op)) {
							scope.append(new CGIAsmMv("mov", "r" + accRegs[i], "r" + tmpReg));
						}
						scope.append(new CGIAsm("sts", "_OS_STAT_POOL+" + cells.getId(i) + ",r" + tmpReg));
					}
					break;
				case REG:
					if(toAccum && (Operator.POST_INC == op || Operator.POST_DEC == op)) {
						for(int i=0; i<cells.getSize(); i++) scope.append(new CGIAsmMv("mov", "r" + accRegs[i], "r" + cells.getId(i)));
					}
					scope.append(new CGIAsm((Operator.PRE_INC==op || Operator.POST_INC==op ? "add" : "sub"), "r" + cells.getId(0) + ",C0x01"));
					for(int i=1; i<cells.getSize(); i++) {
						scope.append(new CGIAsm((Operator.PRE_INC==op || Operator.POST_INC==op ? "adc" : "sbc"), "r" + cells.getId(i) + ",C0x00"));
					}
					codeExcsChecker.mathOverflow(this, scope, excs, op, null, false, false);
					if(toAccum && (Operator.PRE_INC == op || Operator.PRE_DEC == op)) {
						for(int i=0; i<cells.getSize(); i++) scope.append(new CGIAsmMv("mov", "r" + accRegs[i], "r" + cells.getId(i)));
					}
					break;
				case ARGS:
				case STACK_FRAME:
					for(int i=0; i<cells.getSize(); i++) {
						scope.append(new CGIAsmIReg('y', true, "r" + tmpReg, cells.getId(i), getCurBlockScope(scope),
													(CGBlockScope)cells.getObject(), CGCells.Type.ARGS == cells.getType()));
						if(toAccum && (Operator.POST_INC == op || Operator.POST_DEC == op)) {
							scope.append(new CGIAsmMv("mov", "r" + accRegs[i], "r" + tmpReg));
						}
						if(0==i) scope.append(new CGIAsm((Operator.PRE_INC==op || Operator.POST_INC==op ? "add" : "sub"), "r" + tmpReg + ",C0x01"));
						else scope.append(new CGIAsm((Operator.PRE_INC==op || Operator.POST_INC==op ? "adc" : "sbc"), "r" + tmpReg + ",C0x00"));
						codeExcsChecker.mathOverflow(this, scope, excs, op, null, false, false);
						if(toAccum && (Operator.PRE_INC == op || Operator.PRE_DEC == op)) {
							scope.append(new CGIAsmMv("mov", "r" + accRegs[i], "r" + tmpReg));
						}
						scope.append(new CGIAsmIReg('y', false, "r" + tmpReg, cells.getId(i), getCurBlockScope(scope), (CGBlockScope)cells.getObject(),
													CGCells.Type.ARGS == cells.getType()));
					}
					break;
				case HEAP:
					for(int i=0; i<cells.getSize(); i++) {
						scope.append(new CGIAsmIReg('z', true, "r" + tmpReg, cells.getId(i)));
						if(toAccum && (Operator.POST_INC == op || Operator.POST_DEC == op)) {
							scope.append(new CGIAsmMv("mov", "r" + accRegs[i], "r" + tmpReg));
						}
						if(0==i) scope.append(new CGIAsm((Operator.PRE_INC==op || Operator.POST_INC==op ? "add" : "sub"), "r" + tmpReg + ",C0x01"));
						else scope.append(new CGIAsm((Operator.PRE_INC==op || Operator.POST_INC==op ? "adc" : "sbc"), "r" + tmpReg + ",C0x00"));
						codeExcsChecker.mathOverflow(this, scope, excs, op, null, false, false);
						if(toAccum && (Operator.PRE_INC == op || Operator.PRE_DEC == op)) {
							scope.append(new CGIAsmMv("mov", "r" + accRegs[i], "r"+tmpReg));
						}
						scope.append(new CGIAsmIReg('z', false, "r"+tmpReg, cells.getId(i)));
					}
					break;
			}
		}
		else if(Operator.NOT==op || Operator.BIT_NOT==op || Operator.MINUS==op) {
			switch(cells.getType()) {
				case ARRAY:
					for(int i=0; i<cells.getSize(); i++) {
						if(toAccum) {
							scope.append(new CGIAsmLd("ld", "r" + accRegs[i], "x+"));
							scope.append(new CGIAsm("com", "r" + accRegs[i]));
						}
						else {
							//TODO првести к единому виду tmpReg
							scope.append(new CGIAsmLd("ld", "r"+tmpReg, "x"));
							scope.append(new CGIAsm("com", "r"+tmpReg));
							scope.append(new CGIAsm("st", "x+,r"+tmpReg));
						}
					}
					break;
				case STAT:
					for(int i=0; i<cells.getSize(); i++) {
						if(toAccum) {
							scope.append(new CGIAsmLd("lds", "r" + tmpReg, "_OS_STAT_POOL+" + cells.getId(i)));
							scope.append(new CGIAsm("com", "r" + accRegs[i]));
						}
						else {
							scope.append(new CGIAsmLd("lds", "r" + tmpReg, "_OS_STAT_POOL+" + cells.getId(i)));
							scope.append(new CGIAsm("com", "r"+tmpReg));
							scope.append(new CGIAsm("sts", "_OS_STAT_POOL+" + cells.getId(i) + ",r" + tmpReg));
						}
					}
					break;
				case REG:
					for(int i=0; i<cells.getSize(); i++) {
						if(toAccum) {
							scope.append(new CGIAsmMv("mov", "r" + accRegs[i], "r" + cells.getId(i)));
							scope.append(new CGIAsm("com", "r" + accRegs[i]));
						}
						else {
							scope.append(new CGIAsm("com", "r" + cells.getId(i)));
						}
					}
					break;
				case ARGS:
				case STACK_FRAME:
					for(int i=0; i<cells.getSize(); i++) {
						if(toAccum) {
							scope.append(new CGIAsmIReg('y', true, "r" + accRegs[i], cells.getId(i), getCurBlockScope(scope),
														(CGBlockScope)cells.getObject(), CGCells.Type.ARGS == cells.getType()));
							scope.append(new CGIAsm("com", "r" + accRegs[i]));
						}
						else {
							scope.append(new CGIAsmIReg('y', true, "r" + tmpReg, cells.getId(i), getCurBlockScope(scope),
														(CGBlockScope)cells.getObject(), CGCells.Type.ARGS == cells.getType()));
							scope.append(new CGIAsm("com", "r"+tmpReg));
							scope.append(new CGIAsmIReg('y', false, "r" + tmpReg, cells.getId(i), getCurBlockScope(scope),
														(CGBlockScope)cells.getObject(), CGCells.Type.ARGS == cells.getType()));
						}
					}
					break;
				case HEAP:
					for(int i=0; i<cells.getSize(); i++) {
						if(toAccum) {
							scope.append(new CGIAsmIReg('z', true, "r" + accRegs[i], cells.getId(i)));
							scope.append(new CGIAsm("com", "r" + accRegs[i]));
						}
						else {
							scope.append(new CGIAsmIReg('z', true, "r" + tmpReg, cells.getId(i)));
							scope.append(new CGIAsm("com", "r"+tmpReg));
							scope.append(new CGIAsmIReg('z', false, "r" + tmpReg, cells.getId(i)));
						}
					}
					break;
			}
		}
		if(Operator.MINUS==op) {
			scope.append(new CGIAsm("add", "r" + accRegs[0] + ",C0x01"));
			for(int i=1; i<cells.getSize(); i++) scope.append(new CGIAsm("adc", "r" + accRegs[i] + ",C0x00"));
		}
		if(19!=tmpReg) scope.append(new CGIAsm("pop", "r" + tmpReg));
	}
		
	@Override
	public void cellsAction(CGScope scope, CGCells leftCells, Operator op, CGCells rightCells, boolean leftIsFixed, boolean rightIsFixed, CGExcs excs)
																																	throws CompileException {
		switch (leftCells.getType()) {
			case ACC:
				accAndCellsAction(scope, op, rightCells, rightIsFixed, excs);
				break;
			case REG:
				regsAndCellsAction(scope, leftCells, op, rightCells, leftIsFixed, rightIsFixed, excs);
				break;
			default:
				throw new CompileException("Cells action, left cells type:" + leftCells.getType() + " - unsupported yet");
		}
	}

	// Эксперимент
	public void regsAndCellsAction(CGScope scope, CGCells leftCells, Operator op, CGCells rightCells, boolean leftIsFixed, boolean rightIsFixed, CGExcs excs)
																																	throws CompileException {
		CGIContainer cont = new CGIContainer();
		if(VERBOSE_LO <= verbose) {
			cont.append(new CGIText(";action " + leftCells + (leftIsFixed ? "[FIXED]" : " ") + op + " " + rightCells + (rightIsFixed ? "[FIXED]" : " ")));
		}
		
		if(leftCells.getSize()!=rightCells.getSize()) {
			throw new CompileException("cells action, unsupported different cells size");
		}
		
		for(int i=0; i<leftCells.getSize(); i++) {
			switch(op) {
				case PLUS:
					switch(rightCells.getType()) {
						case REG:
							cont.append(new CGIRAsm((0==i ? "add" : "adc"), "r" + leftCells.getId(i), "r" + rightCells.getId(i)));
							break;
						default:
							throw new CompileException("cells action, unsupported right cells type:" + rightCells.getType());
					}
				case INC:
					switch(rightCells.getType()) {
						case REG:
							cont.append(new CGIRAsm((0==i ? "add" : "adc"), "r" + leftCells.getId(i), (0==i ? "C0x01" : "C0x00")));
							break;
						default:
							throw new CompileException("cells action, unsupported right cells type:" + rightCells.getType());
					}
				case MINUS:
					switch(rightCells.getType()) {
						case REG:
							cont.append(new CGIRAsm((0==i ? "sub" : "sbc"), "r" + leftCells.getId(i), "r" + rightCells.getId(i)));
							break;
						default:
							throw new CompileException("cells action, unsupported right cells type:" + rightCells.getType());
					}
				case DEC:
					switch(rightCells.getType()) {
						case REG:
							cont.append(new CGIRAsm((0==i ? "subi" : "sbci"), "r" + leftCells.getId(i), (0==i ? "1" : "0")));
							break;
						default:
							throw new CompileException("cells action, unsupported right cells type:" + rightCells.getType());
					}
				case BIT_AND:
					switch(rightCells.getType()) {
						case REG:
							cont.append(new CGIRAsm("and", "r" + leftCells.getId(i), "r" + leftCells.getId(i)));
							break;
						default:
							throw new CompileException("cells action, unsupported right cells type:" + rightCells.getType());
					}
				case BIT_OR:
					switch(rightCells.getType()) {
						case REG:
							cont.append(new CGIRAsm("or", "r" + leftCells.getId(i), "r" + leftCells.getId(i)));
							break;
						default:
							throw new CompileException("cells action, unsupported right cells type:" + rightCells.getType());
					}
				case BIT_XOR:
					switch(rightCells.getType()) {
						case REG:
							cont.append(new CGIRAsm("eor", "r" + leftCells.getId(i), "r" + leftCells.getId(i)));
							break;
						case ARGS:
						case STACK_FRAME:
							cont.append(new CGIAsmIReg('y', true, "r19", rightCells.getId(0), getCurBlockScope(scope),
														(CGBlockScope)rightCells.getObject(), CGCells.Type.ARGS == rightCells.getType()));
							cont.append(new CGIRAsm("eor", "r" + leftCells.getId(i), "r19"));
							break;
						default:
							throw new CompileException("cells action, unsupported right cells type:" + rightCells.getType());
					}
					
					break;
				case BIT_NOT:
					switch(rightCells.getType()) {
						case REG:
							cont.append(new CGIRAsm("com", "r" + leftCells.getId(i)));
							break;
						default:
							throw new CompileException("cells action, unsupported right cells type:" + rightCells.getType());
					}
				default:
					throw new CompileException("cells action, unsupported operator:" + op);
			}
		}
		scope.append(cont);
	}

	public void accAndCellsAction(CGScope scope, Operator op, CGCells rightCells, boolean rightIsFixed, CGExcs excs) throws CompileException {
		int accMaxBits = (null==codeExcsChecker.getHandled(excs, J8BException.MathOverflowException.name()) ? 
				(accumStack.isEmpty() ? acc.getBytes() : -1==accumStack.peek().getK().getSize() ? refSize : accumStack.peek().getK().getSize())*0x08 : 0x20);
		
		if(!accumStack.isEmpty() && !accumStack.peek().getV() && acc.getBits()>accMaxBits) {
			//TODO костыль - иначе мягкие ограничения аккумулятора уменьшают его размер, что должно быть только при жестком ограничении
			accMaxBits = acc.getBits();
		}
		
		if(VERBOSE_LO <= verbose) {
			scope.append(new CGIText(";" + acc + "/" + accMaxBits + " " + op + " cells " + rightCells + (rightIsFixed ? "[FIXED]" : "") +  "->acc"));
		}
		
		switch(op) {
			case PLUS:
				if(!acc.getFixed()) {
					int requiredBits = Math.max(acc.getBits(), rightCells.getSize()*8)+1;
					acc.setBits(requiredBits>accMaxBits ? accMaxBits : requiredBits);
					scope.append(accResize());
				}

				// Передаем размер аккумулятора чуть больше
				cellsActionAsm(scope, rightCells, acc.getBytes(), new CGActionHandler() {
					String lastReg;
					@Override
					public CGItem action(int sn, String reg) throws CompileException {
						if(null==lastReg) lastReg = reg;
						if(acc.getFixed() && !rightIsFixed) {
							return 0x00==sn ? null : new CGIRAsm("add", getRightOp(sn, null), lastReg);
						}
						if(null!=reg) {
							return new CGIRAsm((0 == sn ? "add" : "adc"), getRightOp(sn, null), reg);
						}
						return new CGIRAsm("adc", getRightOp(sn, null), "C0x00");
					}
				});
				// Проверка на переполнение полного аккумулятора (4 байта)
				if(0x20==acc.getBits()) {
					codeExcsChecker.mathOverflow(this, scope, excs, op, null, false, false);
				}
				break;
			case MINUS:
				if(!acc.getFixed()) {
					int requiredBits = Math.max(acc.getBits(), rightCells.getSize()*8);
					acc.setBits(requiredBits>accMaxBits ? accMaxBits : requiredBits);
					scope.append(accResize());
				}
				cellsActionAsm(scope, rightCells, acc.getBytes(), new CGActionHandler() {
					String lastReg;
					@Override
					public CGItem action(int sn, String reg) throws CompileException {
						if(null==lastReg) lastReg = reg;
						if(acc.getFixed() && !rightIsFixed) {
							return 0x00==sn ? null : new CGIAsm("sub", getRightOp(sn, null) + "," + lastReg);
						}
						if(null!=reg) {
							return new CGIAsm((0 == sn ? "sub" : "sbc"), getRightOp(sn, null) + "," + reg);
						}
						return new CGIAsm("sbc", getRightOp(sn, null) + ",C0xFF");
					}
				});
				codeExcsChecker.mathOverflow(this, scope, excs, op, null, false, false);
				break;
			case MULT:
				/* не нужно, с этим справится код ниже
				// Расширяем, если размер аккумулятора оказался меньше аргумента
				if(!acc.getFixed() && acc.getBytes()<cells.getSize()) {
					accGrow(scope, acc.getBytes(), cells.getSize());
					acc.setBytes(cells.getSize());
				}*/
				
				int argSize = (acc.getBytes()>rightCells.getSize() ? acc.getBytes() : rightCells.getSize());
				if(!acc.getFixed()) {
					int requiredBits = (acc.getBits()+rightCells.getSize()*0x08);
					if(requiredBits>=0x18) requiredBits = 0x20;
					acc.setBits(requiredBits>accMaxBits ? accMaxBits : requiredBits);
					scope.append(accResize());
				}

				if(0x01==argSize) {
					RTOSLibs.MATH_MUL8.setRequired();
					switch(rightCells.getType()) {
						case REG:
							scope.append(new CGIAsmMv("mov", "r17", "r"+rightCells.getId(0)));
							break;
						case ARGS:
						case STACK_FRAME:
							scope.append(new CGIAsmIReg('y', true, "r17", rightCells.getId(0), getCurBlockScope(scope),
														(CGBlockScope)rightCells.getObject(), CGCells.Type.ARGS == rightCells.getType()));
							break;
						case HEAP:
							scope.append(new CGIAsmIReg('z', true, "r17", rightCells.getId(0), getCurBlockScope(scope),
														(CGBlockScope)rightCells.getObject(), false));
							break;
						case STACK:
							scope.append(new CGIAsm("pop", "r17"));
							break;
						default:
							throw new CompileException("Unsupported cell type:" + rightCells.getType());
					}
					scope.append(new CGIAsmCall(CALL_INSTR, "os_mul8", true));
				}
				else if(0x02==argSize) {
					if(rightIsFixed) RTOSLibs.MATH_MULQ7N8.setRequired();
					else RTOSLibs.MATH_MUL16.setRequired();
					switch(rightCells.getType()) {
						case REG:
							scope.append(new CGIAsmMv("mov", "r18", "r"+rightCells.getId(0)));
							scope.append(0x01<rightCells.getSize() ? new CGIAsmMv("mov", "r19", "r"+rightCells.getId(1)) : new CGIAsmLd("ldi", "r19", "0"));
							break;
						case ARGS:
						case STACK_FRAME:
							scope.append(new CGIAsmIReg('y', true, "r18", rightCells.getId(0), getCurBlockScope(scope),
														(CGBlockScope)rightCells.getObject(), CGCells.Type.ARGS == rightCells.getType()));
							scope.append(	0x01<rightCells.getSize() ?
											new CGIAsmIReg('y', true, "r19", rightCells.getId(1), getCurBlockScope(scope),
											(CGBlockScope)rightCells.getObject(), CGCells.Type.ARGS == rightCells.getType())
											: new CGIAsmLd("ldi", "r19", "0x00"));
							break;
						case HEAP:
							scope.append(new CGIAsmIReg('z', true, "r18", rightCells.getId(0), getCurBlockScope(scope),
														(CGBlockScope)rightCells.getObject(), false));
							scope.append(	0x01<rightCells.getSize() ?
											new CGIAsmIReg('z', true, "r19", rightCells.getId(1), getCurBlockScope(scope), (CGBlockScope)rightCells.getObject(), false)
											: new CGIAsmLd("ldi", "r19", "0x00"));
							break;
						case STACK:
							scope.append(new CGIAsm("pop", "r18"));
							scope.append(0x01<rightCells.getSize() ? new CGIAsm("pop", "r19") : new CGIAsmLd("ldi", "r19", "0x00"));//TODO проверить порядок
							break;
						default:
							throw new CompileException("Unsupported cell type:" + rightCells.getType());
					}
					scope.append(new CGIAsmCall(CALL_INSTR, (rightIsFixed ? "os_mulq7n8" : "os_mul16"), true));
				}
				else {
					RTOSLibs.MATH_MUL32.setRequired();
					scope.append(new CGIAsm("push", "r24"));
					scope.append(new CGIAsm("push", "r25"));
					scope.append(new CGIAsm("push", "r22"));
					scope.append(new CGIAsm("push", "r23"));
					switch(rightCells.getType()) {
						case REG:
							regsMv(scope, cellsToRegs(rightCells), new byte[]{24,25,22,23});
							if(rightCells.getSize()<4) {
								scope.append(new CGIAsm("ldi", "r23,0"));
								if(rightCells.getSize()<3) {
									scope.append(new CGIAsm("ldi", "r22,0"));
									if(rightCells.getSize()<2) {
										scope.append(new CGIAsm("ldi", "r25,0"));
									}
								}
							}

/*							scope.append(new CGIAsmMv("mov", "r24", "r"+rightCells.getId(0)));
							scope.append(0x01<rightCells.getSize() ? new CGIAsmMv("mov", "r25", "r"+rightCells.getId(1)) : new CGIAsm("ldi", "r25,0x00"));
							scope.append(0x02<rightCells.getSize() ? new CGIAsmMv("mov", "r22", "r"+rightCells.getId(2)) : new CGIAsm("ldi", "r22,0x00"));
							scope.append(0x03<rightCells.getSize() ? new CGIAsmMv("mov", "r23", "r"+rightCells.getId(3)) : new CGIAsm("ldi", "r23,0x00"));
*/
							break;
						//TODO размер ячеек может быть меньше чем 4
						case ARGS:
						case STACK_FRAME:
							scope.append(new CGIAsmIReg('y', true, "r24", rightCells.getId(0), getCurBlockScope(scope),
														(CGBlockScope)rightCells.getObject(), CGCells.Type.ARGS == rightCells.getType()));
							scope.append(new CGIAsmIReg('y', true, "r25", rightCells.getId(1), getCurBlockScope(scope),
														(CGBlockScope)rightCells.getObject(), CGCells.Type.ARGS == rightCells.getType()));
							if(0x02<rightCells.getSize()) {
								scope.append(new CGIAsmIReg('y', true, "r22", rightCells.getId(2), getCurBlockScope(scope),
															(CGBlockScope)rightCells.getObject(), CGCells.Type.ARGS == rightCells.getType()));
							}
							else {
								scope.append(new CGIAsm("ldi", "r22,0x00"));
							}
							if(0x03<rightCells.getSize()) {
								scope.append(new CGIAsmIReg('y', true, "r23", rightCells.getId(3), getCurBlockScope(scope),
															(CGBlockScope)rightCells.getObject(), CGCells.Type.ARGS == rightCells.getType()));
							}
							else {
								scope.append(new CGIAsm("ldi", "r23,0x00"));
							}
							break;
						case HEAP:
							scope.append(new CGIAsmIReg('z', true, "r24", rightCells.getId(0), getCurBlockScope(scope), (CGBlockScope)rightCells.getObject(), false));
							scope.append(new CGIAsmIReg('z', true, "r25", rightCells.getId(1), getCurBlockScope(scope), (CGBlockScope)rightCells.getObject(), false));
							if(0x02<rightCells.getSize()) {
								scope.append(new CGIAsmIReg('z', true, "r22", rightCells.getId(2), getCurBlockScope(scope), (CGBlockScope)rightCells.getObject(), false));
							}
							else {
								scope.append(new CGIAsm("ldi", "r22,0x00"));
							}
							if(0x03<rightCells.getSize()) {
								scope.append(new CGIAsmIReg('z', true, "r23", rightCells.getId(3), getCurBlockScope(scope), (CGBlockScope)rightCells.getObject(), false));
							}
							else {
								scope.append(new CGIAsm("ldi", "r23,0x00"));
							}
							break;
						case STACK:
							scope.append(new CGIAsm("pop", "r24"));
							scope.append(new CGIAsm("pop", "r25"));
							scope.append(new CGIAsm("pop", "r22"));
							scope.append(new CGIAsm("pop", "r23"));//TODO проверить порядок
							break;
						default:
							throw new CompileException("Unsupported cell type:" + rightCells.getType());
					}
					scope.append(new CGIAsmCall(CALL_INSTR, "os_mul32_nr", true));
					scope.append(new CGIAsm("pop", "r23"));
					scope.append(new CGIAsm("pop", "r22"));
					scope.append(new CGIAsm("pop", "r25"));
					scope.append(new CGIAsm("pop", "r24"));
					codeExcsChecker.mathOverflow(this, scope, excs, op, null, false, false);
				}
				break;
			case DIV:
				if(!acc.getFixed()) {
					int requiredBits = Math.max(acc.getBits(), rightCells.getSize()*8);
					if(requiredBits>=0x18) requiredBits = 0x20;
					acc.setBits(requiredBits>accMaxBits ? accMaxBits : requiredBits);
					scope.append(accResize());
				}

				CGLabelScope lbEndScope = new CGLabelScope(null, null, LabelNames.DIV_END, true);
				if(0x01==acc.getBytes()) {
					RTOSLibs.MATH_DIV8.setRequired();
					switch(rightCells.getType()) {
						case REG:
							scope.append(new CGIAsmMv("mov", "r17", "r"+rightCells.getId(0)));
							break;
						case ARGS:
						case STACK_FRAME:
							scope.append(new CGIAsmIReg('y', true, "r17", rightCells.getId(0), getCurBlockScope(scope),
														(CGBlockScope)rightCells.getObject(), CGCells.Type.ARGS == rightCells.getType()));
							break;
						case HEAP:
							scope.append(new CGIAsmIReg('z', true, "r17", rightCells.getId(0), getCurBlockScope(scope),
														(CGBlockScope)rightCells.getObject(), false));
							break;
						case STACK:
							scope.append(new CGIAsm("pop", "r17"));
							break;
						default:
							throw new CompileException("Unsupported cell type:" + rightCells.getType());
					}
					codeExcsChecker.divByZero(this, scope, excs, new byte[]{17}, false);
					scope.append(new CGIAsm("push", "r24"));
					scope.append(new CGIAsmCall(CALL_INSTR, "os_div8", true));
					scope.append(new CGIAsm("pop", "r24"));
					scope.append(lbEndScope);
				}
				else if(0x02==acc.getBytes()) {
					if(rightIsFixed) RTOSLibs.MATH_DIVQ7N8.setRequired();
					else RTOSLibs.MATH_DIV16.setRequired();
					switch(rightCells.getType()) {
						case REG:
							scope.append(new CGIAsmMv("mov", "r18", "r"+rightCells.getId(0)));
							scope.append(new CGIAsmMv("mov", "r19", "r"+rightCells.getId(1)));
							break;
						case ARGS:
						case STACK_FRAME:
							scope.append(new CGIAsmIReg('y', true, "r18", rightCells.getId(0), getCurBlockScope(scope),
														(CGBlockScope)rightCells.getObject(), CGCells.Type.ARGS == rightCells.getType()));
							scope.append(new CGIAsmIReg('y', true, "r19", rightCells.getId(1), getCurBlockScope(scope),
														(CGBlockScope)rightCells.getObject(), CGCells.Type.ARGS == rightCells.getType()));
							break;
						case HEAP:
							scope.append(new CGIAsmIReg('z', true, "r18", rightCells.getId(0), getCurBlockScope(scope), (CGBlockScope)rightCells.getObject(), false));
							scope.append(new CGIAsmIReg('z', true, "r19", rightCells.getId(1), getCurBlockScope(scope), (CGBlockScope)rightCells.getObject(), false));
							break;
						case STACK:
							scope.append(new CGIAsm("pop", "r18"));
							scope.append(new CGIAsm("pop", "r19"));//TODO проверить порядок
							break;
						default:
							throw new CompileException("Unsupported cell type:" + rightCells.getType());
					}
					codeExcsChecker.divByZero(this, scope, excs, new byte[]{18, 19}, false);
					scope.append(new CGIAsm("push", "r24"));
					scope.append(new CGIAsm("push", "r25"));
					scope.append(new CGIAsmCall(CALL_INSTR, (rightIsFixed ? "os_divq7n8" : "os_div16"), true));
					scope.append(new CGIAsm("pop", "r25"));
					scope.append(new CGIAsm("pop", "r24"));
					scope.append(lbEndScope);
				}
				else {
					RTOSLibs.MATH_DIV32.setRequired();
					scope.append(new CGIAsm("push", "r24"));
					scope.append(new CGIAsm("push", "r25"));
					scope.append(new CGIAsm("push", "r22"));
					scope.append(new CGIAsm("push", "r23"));
					switch(rightCells.getType()) {
						case REG:
							regsMv(scope, cellsToRegs(rightCells), new byte[]{24,25,22,23});
/*							scope.append(new CGIAsmMv("mov", "r24", "r"+rightCells.getId(0)));
							scope.append(new CGIAsmMv("mov", "r25", "r"+rightCells.getId(1)));
							scope.append(new CGIAsmMv("mov", "r22", "r"+rightCells.getId(2)));
							scope.append(new CGIAsmMv("mov", "r23", "r"+rightCells.getId(3)));*/
							break;
						case ARGS:
						case STACK_FRAME:
							scope.append(new CGIAsmIReg('y', true, "r24", rightCells.getId(0), getCurBlockScope(scope),
														(CGBlockScope)rightCells.getObject(), CGCells.Type.ARGS == rightCells.getType()));
							scope.append(new CGIAsmIReg('y', true, "r25", rightCells.getId(1), getCurBlockScope(scope),
														(CGBlockScope)rightCells.getObject(), CGCells.Type.ARGS == rightCells.getType()));
							scope.append(new CGIAsmIReg('y', true, "r22", rightCells.getId(2), getCurBlockScope(scope),
														(CGBlockScope)rightCells.getObject(), CGCells.Type.ARGS == rightCells.getType()));
							scope.append(new CGIAsmIReg('y', true, "r23", rightCells.getId(3), getCurBlockScope(scope),
														(CGBlockScope)rightCells.getObject(), CGCells.Type.ARGS == rightCells.getType()));
							break;
						case HEAP:
							scope.append(new CGIAsmIReg('z', true, "r24", rightCells.getId(0), getCurBlockScope(scope), (CGBlockScope)rightCells.getObject(), false));
							scope.append(new CGIAsmIReg('z', true, "r25", rightCells.getId(1), getCurBlockScope(scope), (CGBlockScope)rightCells.getObject(), false));
							scope.append(new CGIAsmIReg('z', true, "r22", rightCells.getId(2), getCurBlockScope(scope), (CGBlockScope)rightCells.getObject(), false));
							scope.append(new CGIAsmIReg('z', true, "r23", rightCells.getId(3), getCurBlockScope(scope), (CGBlockScope)rightCells.getObject(), false));
							break;
						case STACK:
							scope.append(new CGIAsm("pop", "r24"));
							scope.append(new CGIAsm("pop", "r25"));
							scope.append(new CGIAsm("pop", "r22"));
							scope.append(new CGIAsm("pop", "r23"));//TODO проверить порядок
							break;
						default:
							throw new CompileException("Unsupported cell type:" + rightCells.getType());
					}
					codeExcsChecker.divByZero(this, scope, excs, new byte[]{23, 22, 25, 24}, true);
					scope.append(new CGIAsmCall(CALL_INSTR, "os_div32", true));
					scope.append(lbEndScope);
					scope.append(new CGIAsm("pop", "r23"));
					scope.append(new CGIAsm("pop", "r22"));
					scope.append(new CGIAsm("pop", "r25"));
					scope.append(new CGIAsm("pop", "r24"));
				}
				break;
			case MOD:
				if(rightIsFixed) throw new CompileException("CG:cellsAction: unsupported operator: " + op);
				int requiredBits = Math.max(acc.getBits(), rightCells.getSize()*8);
				if(requiredBits>=0x18) requiredBits = 0x20;
				acc.setBits(requiredBits>accMaxBits ? accMaxBits : requiredBits);
				scope.append(accResize());

				// Расширяем, если размер аккумулятора оказался меньше аргумента
				if(acc.getBytes()<rightCells.getSize()) {
					accGrow(scope, acc.getBytes(), rightCells.getSize());
					acc.setBytes(rightCells.getSize());
				}

				lbEndScope = new CGLabelScope(null, null, LabelNames.DIV_END, true);
				if(0x01==acc.getBytes()) {
					RTOSLibs.MATH_DIV8.setRequired();
					switch(rightCells.getType()) {
						case REG:
							scope.append(new CGIAsmMv("mov", "r17", "r"+rightCells.getId(0)));
							break;
						case ARGS:
						case STACK_FRAME:
							scope.append(new CGIAsmIReg('y', true, "r17", rightCells.getId(0), getCurBlockScope(scope),
														(CGBlockScope)rightCells.getObject(), CGCells.Type.ARGS == rightCells.getType()));
							break;
						case HEAP:
							scope.append(new CGIAsmIReg('z', true, "r17", rightCells.getId(0), getCurBlockScope(scope),
														(CGBlockScope)rightCells.getObject(), false));
							break;
						case STACK:
							scope.append(new CGIAsm("pop", "r17"));
							break;
						default:
							throw new CompileException("Unsupported cell type:" + rightCells.getType());
					}
					codeExcsChecker.divByZero(this, scope, excs, new byte[]{17}, false);
					scope.append(new CGIAsm("push", "r24"));
					scope.append(new CGIAsmCall(CALL_INSTR, "os_div8", true));
					scope.append(new CGIAsmMv("mov", "r16", "r24"));
					scope.append(new CGIAsm("pop", "r24"));
					scope.append(lbEndScope);
				}
				else if(0x02==acc.getBytes()) {
					RTOSLibs.MATH_DIV16.setRequired();
					switch(rightCells.getType()) {
						case REG:
							scope.append(new CGIAsmMv("mov", "r18", "r"+rightCells.getId(0)));
							scope.append(new CGIAsmMv("mov", "r19", "r"+rightCells.getId(1)));
							break;
						case ARGS:
						case STACK_FRAME:
							scope.append(new CGIAsmIReg('y', true, "r18", rightCells.getId(0), getCurBlockScope(scope),
														(CGBlockScope)rightCells.getObject(), CGCells.Type.ARGS == rightCells.getType()));
							scope.append(new CGIAsmIReg('y', true, "r19", rightCells.getId(1), getCurBlockScope(scope),
														(CGBlockScope)rightCells.getObject(), CGCells.Type.ARGS == rightCells.getType()));
							break;
						case HEAP:
							scope.append(new CGIAsmIReg('z', true, "r18", rightCells.getId(0), getCurBlockScope(scope), (CGBlockScope)rightCells.getObject(), false));
							scope.append(new CGIAsmIReg('z', true, "r19", rightCells.getId(1), getCurBlockScope(scope), (CGBlockScope)rightCells.getObject(), false));
							break;
						case STACK:
							scope.append(new CGIAsm("pop", "r18"));
							scope.append(new CGIAsm("pop", "r19"));//TODO проверить порядок
							break;
						default:
							throw new CompileException("Unsupported cell type:" + rightCells.getType());
					}
					codeExcsChecker.divByZero(this, scope, excs, new byte[]{18, 19}, false);
					scope.append(new CGIAsm("push", "r24"));
					scope.append(new CGIAsm("push", "r25"));
					scope.append(new CGIAsmCall(CALL_INSTR, "os_div16", true));
					scope.append(new CGIAsmMv("mov", "r16", "r24"));
					scope.append(new CGIAsmMv("mov", "r17", "r25"));
					scope.append(new CGIAsm("pop", "r25"));
					scope.append(new CGIAsm("pop", "r24"));
					scope.append(lbEndScope);
				}
				else {
					RTOSLibs.MATH_DIV32.setRequired();
					scope.append(new CGIAsm("push", "r24"));
					scope.append(new CGIAsm("push", "r25"));
					scope.append(new CGIAsm("push", "r22"));
					scope.append(new CGIAsm("push", "r23"));
					switch(rightCells.getType()) {
						case REG:
							scope.append(new CGIAsmMv("mov", "r24", "r"+rightCells.getId(0)));
							scope.append(new CGIAsmMv("mov", "r25", "r"+rightCells.getId(1)));
							scope.append(new CGIAsmMv("mov", "r22", "r"+rightCells.getId(2)));
							scope.append(new CGIAsmMv("mov", "r23", "r"+rightCells.getId(3)));
							break;
						case ARGS:
						case STACK_FRAME:
							scope.append(new CGIAsmIReg('y', true, "r24", rightCells.getId(0), getCurBlockScope(scope),
														(CGBlockScope)rightCells.getObject(), CGCells.Type.ARGS == rightCells.getType()));
							scope.append(new CGIAsmIReg('y', true, "r25", rightCells.getId(1), getCurBlockScope(scope),
														(CGBlockScope)rightCells.getObject(), CGCells.Type.ARGS == rightCells.getType()));
							scope.append(new CGIAsmIReg('y', true, "r22", rightCells.getId(2), getCurBlockScope(scope),
														(CGBlockScope)rightCells.getObject(), CGCells.Type.ARGS == rightCells.getType()));
							scope.append(new CGIAsmIReg('y', true, "r23", rightCells.getId(3), getCurBlockScope(scope),
														(CGBlockScope)rightCells.getObject(), CGCells.Type.ARGS == rightCells.getType()));
							break;
						case HEAP:
							scope.append(new CGIAsmIReg('z', true, "r24", rightCells.getId(0), getCurBlockScope(scope), (CGBlockScope)rightCells.getObject(), false));
							scope.append(new CGIAsmIReg('z', true, "r25", rightCells.getId(1), getCurBlockScope(scope), (CGBlockScope)rightCells.getObject(), false));
							scope.append(new CGIAsmIReg('z', true, "r22", rightCells.getId(2), getCurBlockScope(scope), (CGBlockScope)rightCells.getObject(), false));
							scope.append(new CGIAsmIReg('z', true, "r23", rightCells.getId(3), getCurBlockScope(scope), (CGBlockScope)rightCells.getObject(), false));
							break;
						case STACK:
							scope.append(new CGIAsm("pop", "r24"));
							scope.append(new CGIAsm("pop", "r25"));
							scope.append(new CGIAsm("pop", "r22"));
							scope.append(new CGIAsm("pop", "r23"));//TODO проверить порядок
							break;
						default:
							throw new CompileException("Unsupported cell type:" + rightCells.getType());
					}
					codeExcsChecker.divByZero(this, scope, excs, new byte[]{23, 22, 25, 24}, true);
					scope.append(new CGIAsmCall(CALL_INSTR, "os_div32", true));
					scope.append(new CGIAsmMv("mov", "r16", "r24"));
					scope.append(new CGIAsmMv("mov", "r17", "r25"));
					scope.append(new CGIAsmMv("mov", "r18", "r22"));
					scope.append(new CGIAsmMv("mov", "r19", "r23"));
					scope.append(new CGIAsm("pop", "r23"));
					scope.append(new CGIAsm("pop", "r22"));
					scope.append(new CGIAsm("pop", "r25"));
					scope.append(new CGIAsm("pop", "r24"));
					scope.append(lbEndScope);
				}
				break;
			case AND:
			case BIT_AND:
				if(!acc.getFixed() && !rightIsFixed) {
					requiredBits = Math.max(acc.getBits(), rightCells.getSize()*8);
					acc.setBits(requiredBits>accMaxBits ? accMaxBits : requiredBits);
					scope.append(accResize());
	
					cellsActionAsm(scope,  rightCells, acc.getBytes(), new CGActionHandler() {
						@Override
						public CGItem action(int sn, String reg) throws CompileException {
							if(null!=reg) {
								return new CGIAsm("and", getRightOp(sn, null) + "," + reg);
							}
							return new CGIAsm("and", getRightOp(sn, null) + ",C0x00");
						}
					});
				}
				else {
					throw new CompileException("CG:cellsAction: unsupported operator: " + op);
				}
				break;
			case OR:
			case BIT_OR:
				if(!acc.getFixed() && !rightIsFixed) {
					requiredBits = Math.max(acc.getBits(), rightCells.getSize()*8);
					acc.setBits(requiredBits>accMaxBits ? accMaxBits : requiredBits);
					scope.append(accResize());

					cellsActionAsm(scope,  rightCells, acc.getBytes(), new CGActionHandler() {
						@Override
						public CGItem action(int sn, String reg) throws CompileException {
							if(null!=reg) {
								return new CGIAsm("or", getRightOp(sn, null) + "," + reg);
							}
							return null;
						}
					});
				}
				else {
					throw new CompileException("CG:cellsAction: unsupported operator: " + op);
				}
				break;
			case BIT_XOR:
				if(!acc.getFixed() && !rightIsFixed) {
					requiredBits = Math.max(acc.getBits(), rightCells.getSize()*8);
					acc.setBits(requiredBits>accMaxBits ? accMaxBits : requiredBits);
					scope.append(accResize());

					cellsActionAsm(scope,  rightCells, acc.getBytes(), new CGActionHandler() {
						@Override
						public CGItem action(int sn, String reg) throws CompileException {
							if(null!=reg) {
								return new CGIAsm("eor", getRightOp(sn, null) + "," + reg);
							}
							return new CGIAsm("eor", getRightOp(sn, null) + ",C0x00");
						}
					});
				}
				else {
					throw new CompileException("CG:cellsAction: unsupported operator: " + op);
				}
				break;
			case SHR:
				if(!rightIsFixed) {
					if(!acc.getFixed()) {
						requiredBits = acc.getBits()>=0x18 ? 0x20 : acc.getBits();
						acc.setBits(requiredBits>accMaxBits ? accMaxBits : requiredBits);
						scope.append(accResize());
					}

					cellsActionAsm(scope,  rightCells, acc.getBytes(), new CGActionHandler() {
						@Override
						public CGItem action(int sn, String reg) throws CompileException {
							if(null!=reg && 0==sn) {
								CGIContainer cont = new CGIContainer();
								if(0x01==acc.getBytes()) {
									cont.append(new CGIAsmMv("mov", "r17", reg));
									RTOSLibs.MATH_SHR8.setRequired();
									cont.append(new CGIAsmCall(CALL_INSTR, "os_shr8", true));
								}
								else if(0x02==acc.getBytes()) {
									cont.append(new CGIAsmMv("mov", "r18", reg));
									RTOSLibs.MATH_SHR16.setRequired();
									cont.append(new CGIAsmCall(CALL_INSTR, "os_shr16", true));
								}
								else {
									cont.append(new CGIAsm("push", "r22"));
									RTOSLibs.MATH_SHR32.setRequired();
									scope.append(new CGIAsmCall(CALL_INSTR, "os_shr32", true));
									cont.append(new CGIAsm("pop", "r22"));
								}
								return cont;
							}
							return null;
						}
					});
				}
				else {
					throw new CompileException("CG:cellsAction: unsupported operator: " + op);
				}
				break;
			case SHL:
				if(!rightIsFixed) {
					if(!acc.getFixed()) {
						requiredBits = 0x20;
						acc.setBits(requiredBits>accMaxBits ? accMaxBits : requiredBits);
						scope.append(accResize());
					}

					cellsActionAsm(scope,  rightCells, acc.getBytes(), new CGActionHandler() {
						@Override
						public CGItem action(int sn, String reg) throws CompileException {
							if(null!=reg && 0==sn) {
								CGIContainer cont = new CGIContainer();
								if(0x01==acc.getBytes()) {
									cont.append(new CGIAsmMv("mov", "r17", "r16")); //TODO костыль
									cont.append(new CGIAsmMv("mov", "r16", reg));
									RTOSLibs.MATH_SHL8.setRequired();
									cont.append(new CGIAsmCall(CALL_INSTR, "os_shl8", true));
								}
								else if(0x02==acc.getBytes()) {
									cont.append(new CGIAsmMv("mov", "r18", reg));
									RTOSLibs.MATH_SHL16.setRequired();
									cont.append(new CGIAsmCall(CALL_INSTR, "os_shl16", true));
								}
								else {
									cont.append(new CGIAsm("push", "r22"));
									RTOSLibs.MATH_SHL32.setRequired();
									scope.append(new CGIAsmCall(CALL_INSTR, "os_shl32", true));
									cont.append(new CGIAsm("pop", "r22"));
								}
								return cont;
							}
							return null;
						}
					});
				}
				else {
					throw new CompileException("CG:cellsAction: unsupported operator: " + op);
				}
				break;
			default:
				throw new CompileException("CG:cellsAction: unsupported operator: " + op);
		}
	}

	@Override
	public void constAction(CGScope scope, CGCells leftCells, Operator op, long k, boolean rightIsFixed, CGExcs excs) throws CompileException {
		switch (leftCells.getType()) {
			case ACC:
				accAndConstAction(scope, op, k, rightIsFixed, excs);
				break;
			case REG:
				regsAndConstAction(scope, leftCells, op, k, rightIsFixed, excs);
				break;
			default:
				throw new CompileException("Unsupported constAction, leftCells type:" + leftCells.getType());
		}
	}
	
	// Эксперимент	
	private void regsAndConstAction(CGScope scope, CGCells leftCells, Operator op, long k, boolean rightIsFixed, CGExcs excs) throws CompileException {
		CGIContainer cont = new CGIContainer();
		if(VERBOSE_LO <= verbose) {
			cont.append(new CGIText(";action [???] " + op + " " + + (rightIsFixed ? k/256.0 : k)));
		}

		for(int i=0; i<leftCells.getSize(); i++) {
			int octet = (int)(k>>(i*8)) & 0xff;
			
			switch(op) {
//				case PLUS:
//					cont.append(new CGIRAsm((0==i ? "add" : "adc"), "r" + leftCells.getId(i), "r" + rightCells.getId(i)));
//					break;
				case INC:
					cont.append(new CGIRAsm((0==i ? "add" : "adc"), "r" + leftCells.getId(i), (0==i ? "C0x01" : "C0x00")));
					break;
//				case MINUS:
//					cont.append(new CGIRAsm((0==i ? "sub" : "sbc"), "r" + leftCells.getId(i), "r" + rightCells.getId(i)));
//					break;
				case DEC:
					cont.append(new CGIRAsm((0==i ? "subi" : "sbci"), "r" + leftCells.getId(i), (0==i ? "1" : "0")));
				case BIT_AND:
					cont.append(new CGIRAsm("and", "r" + leftCells.getId(i), "r" + leftCells.getId(i)));
					break;
				case BIT_OR:
					cont.append(new CGIRAsm("or", "r" + leftCells.getId(i), "r" + leftCells.getId(i)));
					break;
				case BIT_XOR:
					cont.append(new CGIRAsm("eor", "r" + leftCells.getId(i), "r" + leftCells.getId(i)));
					break;
				case BIT_NOT:
					cont.append(new CGIRAsm("com", "r" + leftCells.getId(i)));
					break;
				//TODO учесть направление
				case SHL:
					if(1==k) {
						cont.append(new CGIRAsm((0==1 ? "lsl" : "rol"), "r" + leftCells.getId(i)));
					}
					else {
						throw new CompileException("Operator " + op + " and const !=1 in cellsConstAction - unsupported yet");
					}
					break;
				//TODO учесть направление
				case SHR:
					if(1==k) {
						cont.append(new CGIRAsm((0==1 ? "lsr" : "ror"), "r" + leftCells.getId(i)));
					}
					else {
						throw new CompileException("Operator " + op + " and const !=1 in cellsConstAction - unsupported yet");
					}
					break;
				default:
					throw new CompileException("Operator " + op + " in cellsConstAction - unsupported yet");
			}
		}
		scope.append(cont);
	}
	
	private void accAndConstAction(CGScope scope, Operator op, long k, boolean rightIsFixed, CGExcs excs) throws CompileException {
		int accMaxBits =	(!accumStack.isEmpty() && null==codeExcsChecker.getHandled(excs, J8BException.MathOverflowException.name()) ? 
							(-1==accumStack.peek().getK().getSize() ? refSize : accumStack.peek().getK().getSize())*0x08 : 0x20);
		if(VERBOSE_LO <= verbose) {
			scope.append(new CGIText(";" + acc + "/" + accMaxBits + " " + op + " " + (rightIsFixed ? k/256.0 : k) + "->acc"));
		}

		if(!acc.getFixed() && rightIsFixed) {
			acc.setFixed();
			scope.append(accResize());
		}
		
		int constSize = NumUtils.getBytesRequired(k);
		
		switch(op) {
			case PLUS:
				if(!acc.getFixed()) {
					int requiredBits = Math.max(acc.getBits(), constSize*8)+1;
					acc.setBits(requiredBits>accMaxBits ? accMaxBits : requiredBits);
					scope.append(accResize());
				}

				boolean isMathOverflow = null!=codeExcsChecker.getHandled(excs, "MathOverflowException");
				boolean cUsed = false;
				for(int i=0; i<acc.getBytes(); i++) {
					if(isMathOverflow) {
						int octet = (int)((k>>(i*8))&0xff);
						if(0x00==octet) {
							scope.append(new CGIRAsm(i==0 ? "add" : "adc", getRightOp(i, null), "C0x00"));
						}
						else if(0x01==octet) {
							scope.append(new CGIRAsm(i==0 ? "add" : "adc", getRightOp(i, null), "C0x01"));
						}
						else if(0xff==octet) {
							scope.append(new CGIRAsm(i==0 ? "add" : "adc", getRightOp(i, null), "C0xff"));
						}
						else {
							if(0x04!=acc.getBytes()) {
								scope.append(new CGIAsmLd("ldi", "r19", Integer.toString(octet)));
								scope.append(new CGIRAsm(i==0 ? "add" : "adc", getRightOp(i, null), "r19"));
							}
							else {
								scope.append(new CGIAsm("push", "r31"));
								scope.append(new CGIAsmLd("ldi", "r31", Integer.toString(octet)));
								scope.append(new CGIRAsm(i==0 ? "add" : "adc", getRightOp(i, null), "r31"));
								scope.append(new CGIAsm("pop", "r31"));
							}
						}
					}
					else {
						int octet = (int)(((0==k?0:~k+1)>>(i*8))&0xff);
						if(!cUsed) {
							if(0!=octet) {
								scope.append(new CGIAsm("subi", getRightOp(i, null) + "," + Integer.toString(octet)));
								cUsed = true;
							}
						}
						else {
							scope.append(new CGIAsm("sbci", getRightOp(i, null) + "," + Integer.toString(octet)));
						}
					}
				}
				// Проверка на переполнение полного аккумулятора (4 байта)
				if(0x20==acc.getBits()) {
					codeExcsChecker.mathOverflow(this, scope, excs, op, null, false, false);
				}
				break;
			case MINUS:
				if(!acc.getFixed()) {
					int requiredBits = Math.max(acc.getBits(), constSize*8);
					acc.setBits(requiredBits>accMaxBits ? accMaxBits : requiredBits);
					scope.append(accResize());
				}

				for(int i=0; i<acc.getBytes(); i++) {
					scope.append(new CGIAsm(i==0 ? "subi" : "sbci", getRightOp(i, null) + "," + Long.toString((k>>(i*8))&0xff)));
				}
				codeExcsChecker.mathOverflow(this, scope, excs, op, null, false, false);
				break;
			case SHL:
				if(rightIsFixed) throw new CompileException("CG:constAction: unsupported operator: " + op);
				
				if(!acc.getFixed()) {
					int requiredBits = acc.getBits() + (int)k;
					if(requiredBits>=0x18) requiredBits = 0x20;
					acc.setBits(requiredBits>accMaxBits ? accMaxBits : requiredBits);
					scope.append(accResize());
				}

				int octetOffset = (int)k/8;
				
				// Проверка переполнения для сдвигов НЕ выполняется.
				// Причины:
				// 1. Сдвиг вправо (>>) не может вызвать переполнение — значение уменьшается.
				// 2. Проверка для сдвига влево (<<) замедлила бы быструю битовую операцию.
				// 3. Асимметрия проверок (<< проверяем, >> нет) неочевидна для пользователя.
				// 4. Сдвиги часто используются как быстрый аналог */^, а не как арифметика.
				// 
				// TODO реализовать решение: FRONTEND предупреждает, если сдвиг находится внутри try-catch.
				/*
				if(null!=codeExcsChecker.getHandled(excs, J8BException.MathOverflowException.name()) && 0x20==acc.getBits()) {
					byte[] ovfRegs = getOverflowCheckRegs(acc.getBytes(), octetOffset);
					if(0!=ovfRegs.length) {
						codeExcsChecker.mathOverflow(this, scope, excs, op, ovfRegs, false, true);
					}
				}
				*/
				
				if(0<octetOffset) {
					byte[] accRegs = getAccRegs(0x04);
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
				
				k=k-(octetOffset*8);
				if(0!=k) {
					if(0x01==acc.getBytes()) {
						if(1==k) {
							scope.append(new CGIAsm("lsl", "r16"));
						}
						else {
							RTOSLibs.MATH_MUL8P2.setRequired();
							scope.append(new CGIAsmCall(CALL_INSTR, "os_mul8p2_x" + (1<<k), true));
						}
					}
					else if(0x02==acc.getBytes()) {
						RTOSLibs.MATH_MUL16P2.setRequired();
						scope.append(new CGIAsmLd("ldi", "r18", "0x00"));
						scope.append(new CGIAsmCall(CALL_INSTR, "os_mul16p2_x" + (1<<k), true));
					}
					else {
						RTOSLibs.MATH_MUL32P2.setRequired();
						scope.append(new CGIAsmLd("ldi", "r20", "0x00")); //result
						scope.append(new CGIAsmCall(CALL_INSTR, "os_mul32p2_x" + (1<<k), true));
					}
				}
				break;
			case SHR:
				if(!acc.getFixed()) {
					int requiredBits = acc.getBits();
					if(requiredBits>=0x18) requiredBits = 0x20;
					acc.setBits(requiredBits>accMaxBits ? accMaxBits : requiredBits);
					scope.append(accResize());
				}

				octetOffset = (int)k / 8;

				if(0<octetOffset) {
					byte[] accRegs = getAccRegs(0x04);
					for(int i=0; i<acc.getBytes(); i++) {
						int srcIdx = i + octetOffset;
						if(srcIdx<acc.getBytes()) {
							scope.append(new CGIAsmMv("mov", "r" + accRegs[i], "r" + accRegs[srcIdx]));
						}
						else {
							scope.append(new CGIAsmLd("ldi", "r" + accRegs[i], "0"));
						}
					}
				}				

				k=k-(octetOffset*8);
				if(0!=k) {
					if(0x01==acc.getBytes()) {
						if(0x01==k) {
							scope.append(new CGIAsm("lsr", "r16"));
						}
						else {
							RTOSLibs.MATH_DIV8P2.setRequired();
							scope.append(new CGIAsmCall(CALL_INSTR, "os_div8p2_x" + (1<<k), true));
						}
					}
					else if(0x02==acc.getBytes()) {
						RTOSLibs.MATH_DIV16P2.setRequired();
						scope.append(new CGIAsmCall(CALL_INSTR, "os_div16p2_x" + (1<<k), true));
					}
					else {
						RTOSLibs.MATH_DIV32P2.setRequired();
						scope.append(new CGIAsmCall(CALL_INSTR, "os_div32p2_x" + (1<<k), true));
					}
				}
				break;
			case MULT:
				int argSize = (acc.getBytes()>constSize ? acc.getBytes() : constSize);
				if(!acc.getFixed()) {
					int requiredBits = (acc.getBits()+constSize*0x08);
					if(requiredBits>=0x18) requiredBits = 0x20;
					acc.setBits(requiredBits>accMaxBits ? accMaxBits : requiredBits);
					scope.append(accResize());
				}

				if(0x01==argSize) {
					RTOSLibs.MATH_MUL8.setRequired();
					scope.append(new CGIAsmLd("ldi", "r17", Long.toString(k&0xff)));
					scope.append(new CGIAsmCall(CALL_INSTR, "os_mul8", true));
				}
				else if(0x02==argSize) {
					if(rightIsFixed) RTOSLibs.MATH_MULQ7N8.setRequired();
					else RTOSLibs.MATH_MUL16.setRequired();
					scope.append(new CGIAsmLd("ldi", "r18", Long.toString(k&0xff)));
					scope.append(new CGIAsmLd("ldi", "r19", Long.toString((k>>8)&0xff)));
					scope.append(new CGIAsmCall(CALL_INSTR, (rightIsFixed ? "os_mulq7n8" : "os_mul16"), true));
				}
				else {
					RTOSLibs.MATH_MUL32.setRequired();
					scope.append(new CGIAsm("push", "r24"));//TODO добавить проверку на занятось регистров
					scope.append(new CGIAsm("push", "r25"));
					scope.append(new CGIAsm("push", "r22"));
					scope.append(new CGIAsm("push", "r23"));
					scope.append(new CGIAsmLd("ldi", "r24", Long.toString(k&0xff)));
					scope.append(new CGIAsmLd("ldi", "r25", Long.toString((k>>8)&0xff)));
					scope.append(new CGIAsmLd("ldi", "r22", Long.toString((k>>16)&0xff)));
					scope.append(new CGIAsmLd("ldi", "r23", Long.toString((k>>24)&0xff)));
					scope.append(new CGIAsmCall(CALL_INSTR, "os_mul32_nr", true));
					scope.append(new CGIAsm("pop", "r23"));
					scope.append(new CGIAsm("pop", "r22"));
					scope.append(new CGIAsm("pop", "r25"));
					scope.append(new CGIAsm("pop", "r24"));
					codeExcsChecker.mathOverflow(this, scope, excs, op, null, false, false);
				}
				break;
			case DIV:
				if(!acc.getFixed()) {
					int requiredBits = Math.max(acc.getBits(), constSize*8);
					acc.setBits(requiredBits>accMaxBits ? accMaxBits : requiredBits);
					scope.append(accResize());
				}

				// Проверка на 0 уже должна была быть выполнена в frontend компилятора

				// TODO Это должно быть в FRONTEND оптимизации
				if(acc.getBits()<(constSize*8)) {
					byte[] accRegs = getAccRegs(acc.getBytes());
					for(byte reg : accRegs) {
						scope.append(new CGIAsmLd("ldi", "r" + reg, "0x00"));
					}
				}
				else {
					if(!acc.getFixed()) {
						int requiredBits = Math.max(acc.getBits(), constSize*8);
						if(requiredBits>=0x18) requiredBits = 0x20;
						acc.setBits(requiredBits>accMaxBits ? accMaxBits : requiredBits);
						scope.append(accResize());
					}

					if(0x01==acc.getBytes()) {
						RTOSLibs.MATH_DIV8.setRequired();
						scope.append(new CGIAsm("push", "r24"));
						scope.append(new CGIAsmLd("ldi", "r17", Long.toString(k&0xff)));
						scope.append(new CGIAsmCall(CALL_INSTR, "os_div8", true));
						scope.append(new CGIAsm("pop", "r24"));
					}
					else if(0x02==acc.getBytes()) {
						if(rightIsFixed) RTOSLibs.MATH_DIVQ7N8.setRequired();
						else RTOSLibs.MATH_DIV16.setRequired();
						scope.append(new CGIAsm("push", "r24"));
						scope.append(new CGIAsm("push", "r25"));
						scope.append(new CGIAsmLd("ldi", "r18", Long.toString(k&0xff)));
						scope.append(new CGIAsmLd("ldi", "r19", Long.toString((k>>8)&0xff)));
						scope.append(new CGIAsmCall(CALL_INSTR, (rightIsFixed ? "os_divq7n8" : "os_div16"), true));
						scope.append(new CGIAsm("pop", "r25"));
						scope.append(new CGIAsm("pop", "r24"));
					}
					else {
						RTOSLibs.MATH_DIV32.setRequired();
						scope.append(new CGIAsm("push", "r24"));//TODO добавить проверку на занятось регистров
						scope.append(new CGIAsm("push", "r25"));
						scope.append(new CGIAsm("push", "r22"));
						scope.append(new CGIAsm("push", "r23"));
						scope.append(new CGIAsmLd("ldi", "r24", Long.toString(k&0xff)));
						scope.append(new CGIAsmLd("ldi", "r25", Long.toString((k>>8)&0xff)));
						scope.append(new CGIAsmLd("ldi", "r22", Long.toString((k>>16)&0xff)));
						scope.append(new CGIAsmLd("ldi", "r23", Long.toString((k>>24)&0xff)));
						scope.append(new CGIAsmCall(CALL_INSTR, "os_div32", true));
						scope.append(new CGIAsm("pop", "r23"));
						scope.append(new CGIAsm("pop", "r22"));
						scope.append(new CGIAsm("pop", "r25"));
						scope.append(new CGIAsm("pop", "r24"));
					}
				}
				break;
			// Проверка на 0 уже должна была быть выполнена в frontend компилятора
			case MOD:
				if(!acc.getFixed()) {
					int requiredBits = Math.max(acc.getBits(), constSize*8);
					acc.setBits(requiredBits>accMaxBits ? accMaxBits : requiredBits);
					scope.append(accResize());
				}

				if(rightIsFixed) throw new CompileException("CG:constAction: unsupported operator: " + op);
				
				if(acc.getBits()>=(constSize*8)) {	// TODO Это должно быть в FRONTEND оптимизации
					if(!acc.getFixed()) {
						int requiredBits = Math.max(acc.getBits(), constSize*8);
						if(requiredBits>=0x18) requiredBits = 0x20;
						acc.setBits(requiredBits>accMaxBits ? accMaxBits : requiredBits);
						scope.append(accResize());
					}

					if(0x01==acc.getBytes()) {
						RTOSLibs.MATH_DIV8.setRequired();
						scope.append(new CGIAsm("push", "r24"));
						scope.append(new CGIAsmLd("ldi", "r17", Long.toString(k&0xff)));
						scope.append(new CGIAsmCall(CALL_INSTR, "os_div8", true));
						scope.append(new CGIAsmMv("mov", "r16", "r24"));
						scope.append(new CGIAsm("pop", "r24"));
					}
					else if(0x02==acc.getBytes()) {
						RTOSLibs.MATH_DIV16.setRequired();
						scope.append(new CGIAsm("push", "r24"));
						scope.append(new CGIAsm("push", "r25"));
						scope.append(new CGIAsmLd("ldi", "r18", Long.toString(k&0xff)));
						scope.append(new CGIAsmLd("ldi", "r19", Long.toString((k>>8)&0xff)));
						scope.append(new CGIAsmCall(CALL_INSTR, "os_div16", true));
						scope.append(new CGIAsmMv("mov", "r16", "r24"));
						scope.append(new CGIAsmMv("mov", "r17", "r25"));
						scope.append(new CGIAsm("pop", "r25"));
						scope.append(new CGIAsm("pop", "r24"));
					}
					else {
						RTOSLibs.MATH_DIV32.setRequired();
						scope.append(new CGIAsm("push", "r24"));//TODO добавить проверку на занятось регистров
						scope.append(new CGIAsm("push", "r25"));
						scope.append(new CGIAsm("push", "r22"));
						scope.append(new CGIAsm("push", "r23"));
						scope.append(new CGIAsmLd("ldi", "r24", Long.toString(k&0xff)));
						scope.append(new CGIAsmLd("ldi", "r25", Long.toString((k>>8)&0xff)));
						scope.append(new CGIAsmLd("ldi", "r22", Long.toString((k>>16)&0xff)));
						scope.append(new CGIAsmLd("ldi", "r23", Long.toString((k>>24)&0xff)));
						scope.append(new CGIAsmCall(CALL_INSTR, "os_div32", true));
						scope.append(new CGIAsmMv("mov", "r16", "r24"));
						scope.append(new CGIAsmMv("mov", "r17", "r25"));
						scope.append(new CGIAsmMv("mov", "r18", "r22"));
						scope.append(new CGIAsmMv("mov", "r19", "r23"));
						scope.append(new CGIAsm("pop", "r23"));
						scope.append(new CGIAsm("pop", "r22"));
						scope.append(new CGIAsm("pop", "r25"));
						scope.append(new CGIAsm("pop", "r24"));
					}
				}
				break;
			case BIT_AND:
				if(!rightIsFixed) {
					int requiredBits = Math.max(acc.getBits(), constSize*8);
					acc.setBits(requiredBits>accMaxBits ? accMaxBits : requiredBits);
					scope.append(accResize());

					for(int i=0; i<acc.getBytes(); i++) {
						scope.append(new CGIAsm("andi", getRightOp(i, null) + "," + Long.toString((k>>(i*8))&0xff)));
					}
				}
				else {
					throw new CompileException("CG:constAction: unsupported operator: " + op);
				}
				break;
			case BIT_OR:
				if(!rightIsFixed) {
					int requiredBits = Math.max(acc.getBits(), constSize*8);
					acc.setBits(requiredBits>accMaxBits ? accMaxBits : requiredBits);
					scope.append(accResize());

					for(int i=0; i<acc.getBytes(); i++) {
						scope.append(new CGIAsm("ori", getRightOp(i, null) + "," + Long.toString((k>>(i*8))&0xff)));
					}
				}
				else {
					throw new CompileException("CG:constAction: unsupported operator: " + op);
				}
				break;
			case BIT_XOR:
				if(!rightIsFixed) {
					int requiredBits = Math.max(acc.getBits(), constSize*8);
					acc.setBits(requiredBits>accMaxBits ? accMaxBits : requiredBits);
					scope.append(accResize());

					for(int i=0; i<acc.getBytes(); i++) {
						int octet = (int)((k>>(i*8))&0xff);
						if(0x00==octet) {
							scope.append(new CGIAsm("eor", getRightOp(i, null) + ",C0x00"));
						}
						else if(0x01==octet) {
							scope.append(new CGIAsm("eor", getRightOp(i, null) + ",C0x01"));
						}
						else if(0xff==octet) {
							scope.append(new CGIAsm("eor", getRightOp(i, null) + ",C0xff"));
						}
						else {
							if(0x04!=acc.getBytes()) {
								scope.append(new CGIAsmLd("ldi", "r19", Integer.toString(octet)));
								scope.append(new CGIAsm("eor", getRightOp(i, null) + ",r19"));
							}
							else {
								scope.append(new CGIAsm("push", "r31"));
								scope.append(new CGIAsmLd("ldi", "r31", Integer.toString(octet)));
								scope.append(new CGIAsm("eor", getRightOp(i, null) + ",r31"));
								scope.append(new CGIAsm("pop", "r31"));
							}
						}
					}
				}
				else {
					throw new CompileException("CG:constAction: unsupported operator: " + op);
				}
				break;
			default:
				throw new CompileException("CG:constAction: unsupported operator: " + op);
		}
	}
	
/*	РУДИМЕНТ
	private byte[] getOverflowCheckRegs(int accBytes, int octetOffset) {
		// Вытесняются старшие octetOffset байт
		int checkCount = Math.min(octetOffset, accBytes);

		if(checkCount <= 0) {
			return new byte[0];
		}

		byte[] regs = new byte[checkCount];
		byte[] accRegs = getAccRegs(4);  // [r16, r17, r18, r19]

		// Старшие байты: от accBytes-1 вниз до accBytes-octetOffset
		for(int i = 0; i < checkCount; i++) {
			regs[i] = accRegs[accBytes - 1 - i];
		}

		return regs;
	}*/
	
//Пытаемся оптимизировать, пока просто набросок
	@Override
	public void cellsOpCells(CGScope scope, CGCells leftCells, VarType leftType, Operator op, CGCells rightCells, VarType rightType)
																																	throws CompileException {
		CGIContainer cont = new CGIContainer();
		String info =	leftCells + "[" + leftType + "] " + op + " " + rightCells + "[" + rightType + "])";
		if(VERBOSE_LO <= verbose) cont.append(new CGIText(";cells cp " + info));

		int leftSize = null==leftType ? leftCells.getSize() : (-1==leftType.getSize() ? refSize : leftType.getSize());
		int rightSize = null==rightType ? rightCells.getSize() : (-1==rightType.getSize() ? refSize : rightType.getSize());
		int size = Math.max(leftSize, rightSize);

		byte[] accRegs = getAccRegs(size);
		switch(op) {
			case PLUS:
				switch(leftCells.getType()) {
					case REG:
						switch(rightCells.getType()) {
							case CONST:
								if(leftSize<rightSize) throw new CompileException("cellOpCells " + info + " left operand size < const size");
								if(leftType.isFixedPoint() && !rightType.isFixedPoint()) {
									if(0x01!=rightSize) throw new CompileException("cellOpCells " + info + " const size!=1");
									cont.append(new CGIRAsm("subi", "r"+leftCells.getId(0x01), "-"+rightCells.getId(0)));
								}
								else {
									for(int index=0; index<size; index++) {
										if(index<rightSize) {
											cont.append(new CGIRAsm(0==index ? "subi" : "sbci", "r"+leftCells.getId(index), "-"+rightCells.getId(index)));
										}
										else {
											cont.append(new CGIRAsm("sbci", "r"+leftCells.getId(index), "0x00"));
										}
									}
								}
								break;
							default:
								throw new CompileException("cellOpCells " + info + " unsupported yet");
						}
						break;
					default:
						throw new CompileException("cellOpCells " + info + " unsupported yet");
				}
				break;
			case ROL:
				switch(leftCells.getType()) {
					case REG:
						switch(rightCells.getType()) {
							case FLAG:
								if(1!=size) throw new CompileException("cellOpCells " + info + " unsupported yet");
								cont.append(new CGIRAsm("rol", "r"+leftCells.getId(0)));
								break;
							default:
								throw new CompileException("cellOpCells " + info + " unsupported yet");
						}
						break;
					default:
						throw new CompileException("cellOpCells " + info + " unsupported yet");
				}
				break;
			case ROR:
				switch(leftCells.getType()) {
					case REG:
						switch(rightCells.getType()) {
							case FLAG:
								if(1!=size) throw new CompileException("cellOpCells " + info + " unsupported yet");
								cont.append(new CGIRAsm("ror", "r"+leftCells.getId(0)));
								break;
							default:
								throw new CompileException("cellOpCells " + info + " unsupported yet");
						}
						break;
					default:
						throw new CompileException("cellOpCells " + info + " unsupported yet");
				}
				break;
			default:
				throw new CompileException("cellOpCells " + info + " unsupported yet");
		}
		
		scope.append(cont);
	}

	@Override
	public void cellsCpCells(	CGScope scope, CGCells leftCells, VarType leftType, CGCells rightCells, VarType rightType,
								Operator op, boolean isNot, boolean isOr, CGBranch branchScope) throws CompileException {
		CGIContainer cont = new CGIContainer();
				
		String info =	leftCells + "[" + leftType + "] " + op + " " + rightCells + "[" + rightType + "]" +
						" (isOr:" + isOr + ", isNot:" + isNot + ", branch:" + branchScope.getEnd() + ")";
		if(VERBOSE_LO <= verbose) cont.append(new CGIText(";cells cp " + info));

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
		
		int leftSize = null==leftType ? leftCells.getSize() : (-1==leftType.getSize() ? refSize : leftType.getSize());
		int rightSize = null==rightType ? rightCells.getSize() : (-1==rightType.getSize() ? refSize : rightType.getSize());
		int size = Math.max(leftSize, rightSize);
		
		// Для сравнения не имеет значения какие ограничения установлены аккумулятору - приводим к общему размеру
		if(CGCells.Type.ACC==leftCells.getType() && acc.getBytes()<size) {
			accGrow(cont, acc.getBytes(), size);
			cont.append(accResize());
		}
		
		if(CGCells.Type.STACK==leftCells.getType() && CGCells.Type.STACK==rightCells.getType()) {
			throw new CompileException("cellCpcells " + info + " unsupported both operands in stack");
		}

		if(CGCells.Type.LABEL==leftCells.getType() || CGCells.Type.LABEL==rightCells.getType()) {
			throw new CompileException("cellCpcells " + info + " unsupported label cell type");
		}

		if(CGCells.Type.CONST==leftCells.getType()) {
			// Переносим константу вправо
			CGCells cells = rightCells;
			VarType type = rightType;
			rightCells = leftCells;
			rightType = leftType;
			leftCells = cells;
			leftType = type;
			
			switch(op) {
				case LT: op = Operator.GTE; break;
				case LTE: op = Operator.GT; break;
				case GT: op = Operator.LTE; break;
				case GTE: op = Operator.LT; break;
			}
		}

		if(leftType.isFixedPoint() && (null==rightType || !rightType.isFixedPoint()) && CGCells.Type.CONST==rightCells.getType()) {
			rightType=VarType.FIXED;
			rightCells.setConst(rightCells.getConst() * 256);
		}
		
		Set<String> pushRegs = new HashSet<>();
		String leftReg = "";
		if(0x03>=acc.getBytes()) leftReg = "r19";
		else if(0x02>=acc.getBytes()) leftReg = "r18";
		
		boolean leftIsFixed = (null==leftType ? false : leftType.isFixedPoint());
		boolean rightIsFixed = (null==rightType ? false : rightType.isFixedPoint());
		
		byte[] accRegs = getAccRegs(size);
		for(int index=0; index<size; index++) {
			switch(leftCells.getType()) { //src
				case ACC:
					if(!leftIsFixed && rightIsFixed) {
						leftReg = (0==index ? "C0x00" : "r"+accRegs[index-1]);
						leftIsFixed = true;
					}
					else {
						leftReg = (index<accRegs.length ? "r" + accRegs[index] : "C0x00");
					}
					if(!cellsCpCellsCommon(scope, cont, leftReg, index, rightCells, size, pushRegs, leftIsFixed, rightIsFixed)) {
						throw new CompileException("cellCpcells " + info + " unsupported yet");
					}
					break;
				case ARGS:
				case STACK_FRAME:
					if(!leftIsFixed && rightIsFixed) {
						if(0==index) {
							leftReg = "C0x00";
						}
						else {
							if(leftReg.isEmpty()) leftReg = "zl";
							cont.append(new CGIAsmIReg( 'y', true, leftReg, leftCells.getId(index-1), getCurBlockScope(scope),
														(CGBlockScope)leftCells.getObject(), CGCells.Type.ARGS==leftCells.getType()));
						}
						leftIsFixed = true;
					}
					else {
						if(leftReg.isEmpty()) leftReg = "zl";
						cont.append(new CGIAsmIReg( 'y', true, leftReg, leftCells.getId(index), getCurBlockScope(scope),
													(CGBlockScope)leftCells.getObject(), CGCells.Type.ARGS==leftCells.getType()));
					}
					if(!cellsCpCellsCommon(scope, cont, leftReg, index, rightCells, size, pushRegs, leftIsFixed, rightIsFixed)) {
						throw new CompileException("cellCpcells " + info + " unsupported yet");
					}
					break;
				case ARRAY:
					if(!leftIsFixed && rightIsFixed) {
						if(0==index) {
							leftReg = "C0x00";
						}
						else {
							if(leftReg.isEmpty()) leftReg = "zl";
							cont.append(new CGIRAsm("ld", leftReg, "x+"));
						}
						leftIsFixed = true;
					}
					else {
						if(leftReg.isEmpty()) leftReg = "zl";
						cont.append(new CGIRAsm("ld", leftReg, "x+"));
					}
					if(!cellsCpCellsCommon(scope, cont, leftReg, index, rightCells, size, pushRegs, leftIsFixed, rightIsFixed)) {
						throw new CompileException("cellCpcells " + info + " unsupported yet");
					}
					break;
				case HEAP:
					if(!leftIsFixed && rightIsFixed) {
						if(0==index) {
							leftReg = "C0x00";
						}
						else {
							if(leftReg.isEmpty()) leftReg = "yl";
							cont.append(new CGIAsmIReg('z', true, leftReg, leftCells.getId(index-1)));
						}
						leftIsFixed = true;
					}
					else {
						if(leftReg.isEmpty()) leftReg = "yl";
						cont.append(new CGIAsmIReg('z', true, leftReg, leftCells.getId(index)));
					}
					if(!cellsCpCellsCommon(scope, cont, leftReg, index, rightCells, size, pushRegs, leftIsFixed, rightIsFixed)) {
						throw new CompileException("cellCpcells " + info + " unsupported yet");
					}
					break;
				case HEAP_ALT:
					if(!leftIsFixed && rightIsFixed) {
						if(0==index) {
							leftReg = "C0x00";
						}
						else {
							if(1==index) {
								scope.append(new CGIAsm("subi", "r26,low(-" + (leftCells.getId(0)) + ")"));
								scope.append(new CGIAsm("sbci", "r27,high(-" + (leftCells.getId(1)) + ")"));
							}
							cont.append(new CGIRAsm("ld", leftReg, "x+"));
						}
						leftIsFixed = true;
					}
					else {
						if(leftReg.isEmpty()) leftReg = "zl";
						if(0==index) {
							scope.append(new CGIAsm("subi", "r26,low(-" + (leftCells.getId(0)) + ")"));
							scope.append(new CGIAsm("sbci", "r27,high(-" + (leftCells.getId(1)) + ")"));
						}
						cont.append(new CGIRAsm("ld", leftReg, "x+"));
					}
					if(!cellsCpCellsCommon(scope, cont, leftReg, index, rightCells, size, pushRegs, leftIsFixed, rightIsFixed)) {
						throw new CompileException("cellCpcells " + info + " unsupported yet");
					}
					break;
				case REG:
					if(!leftIsFixed && rightIsFixed) {
						leftReg = (0==index ? "C0x00" : "r"+leftCells.getId(index-1));
						leftIsFixed = true;
					}
					else {
						leftReg = (index<leftCells.getSize() ? "r"+leftCells.getId(index) : "C0x00");
					}
					if(!cellsCpCellsCommon(scope, cont, leftReg, index, rightCells, size, pushRegs, leftIsFixed, rightIsFixed)) {
						throw new CompileException("cellCpcells " + info + " unsupported yet");
					}
					break;
				case STACK:
					if(!leftIsFixed && rightIsFixed) {
						if(0==index) {
							leftReg = "C0x00";
						}
						else {
							if(leftReg.isEmpty()) leftReg = "j8b_atom";
							cont.append(new CGIRAsm("pop", leftReg));
						}
						leftIsFixed = true;
					}
					else {
						if(leftReg.isEmpty()) leftReg = "j8b_atom";
						cont.append(new CGIRAsm("pop", leftReg));
					}
					if(!cellsCpCellsCommon(scope, cont, leftReg, index, rightCells, size, pushRegs, leftIsFixed, rightIsFixed)) {
						throw new CompileException("cellCpcells " + info + " unsupported yet");
					}
					break;
				case STAT:
					if(!leftIsFixed && rightIsFixed) {
						if(0==index) {
							leftReg = "C0x00";
						}
						else {
							if(leftReg.isEmpty()) leftReg = "zl";
							cont.append(new CGIRAsm("lds", leftReg, "_OS_STAT_POOL+" + rightCells.getId(index-1)));
						}
						leftIsFixed = true;
					}
					else {
						if(leftReg.isEmpty()) leftReg = "zl";
						cont.append(new CGIRAsm("lds", leftReg, "_OS_STAT_POOL+" + rightCells.getId(index)));
					}
					if(!cellsCpCellsCommon(scope, cont, leftReg, index, rightCells, size, pushRegs, leftIsFixed, rightIsFixed)) {
						throw new CompileException("cellCpcells " + info + " unsupported yet");
					}
					break;
				default:
					throw new CompileException("cellCpcells " + info + " unsupported yet");
			}
		}
		
		if(leftReg.equals("yl") || leftReg.equals("zl") || leftReg.equals("j8b_atom")) {
			pushRegs.add(leftReg);
		}
		if(pushRegs.contains("j8b_atom") && RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) {
			cont.prepend(new CGIAsm("set"));
			cont.append(new CGIAsm("clt"));
		}
		
		// Сохраняю вначале и восстанавливаю перед условным переходом использованные регистры
		List<String> tmp = new ArrayList<>(pushRegs);
		Collections.sort(tmp);
		for(String reg : tmp) {
			cont.prepend(new CGIRAsm("push" , reg));
			cont.append(new CGIRAsm("pop" , reg));
		}

		// Выполняются все сравнения (в том числе и с fixed)
		// далее реализация условных переходов с учетом знаковых операндов (fixed)
		// TODO основной код сравнений готов, но требует тщательной проверки
		switch(op) {
			case LT:
				if(rightIsFixed) {
					cont.append(new CGIAsmCondJump(isOr ? "brlt" : "brge", branchScope.getEnd()));
				}
				else {
					cont.append(new CGIAsmCondJump(isOr ? "brcs" : "brcc", branchScope.getEnd()));
				}
				break;
			case GTE:
				if(rightIsFixed) {
					cont.append(new CGIAsmCondJump(isOr ? "brge" : "brlt", branchScope.getEnd())); // Если >=
				}
				else {
					cont.append(new CGIAsmCondJump(isOr ? "brcc" : "brcs", branchScope.getEnd())); // Если >=
				}
				break;
			case GT:
				if(rightIsFixed) {
					if(isOr) { // В связке с OR
						cont.append(new CGIAsm("breq", "pc+0x02"));
						cont.append(new CGIAsmCondJump("brge", branchScope.getEnd()));
					}
					else { // В связке с AND 
						cont.append(new CGIAsmCondJump("brlt", branchScope.getEnd()));
						cont.append(new CGIAsmCondJump("breq", branchScope.getEnd()));
					}
				}
				else {
					if(isOr) { // В связке с OR
						cont.append(new CGIAsm("breq", "pc+0x02")); // Если =, пропустить brcc
						cont.append(new CGIAsmCondJump("brcc", branchScope.getEnd())); // Если >
					}
					else { // В связке с AND 
						cont.append(new CGIAsmCondJump("brcs", branchScope.getEnd())); // Если <
						cont.append(new CGIAsmCondJump("breq", branchScope.getEnd())); // Если =
					}
				}
				break;
			case LTE:
				if(rightIsFixed) {
					if(isOr) {
						cont.append(new CGIAsmCondJump("brlt", branchScope.getEnd()));
						cont.append(new CGIAsmCondJump("breq", branchScope.getEnd()));
					}
					else {
						cont.append(new CGIAsm("breq", "pc+0x02"));
						cont.append(new CGIAsmCondJump("brge", branchScope.getEnd()));
					}
				}
				else {
					if(isOr) {
						cont.append(new CGIAsmCondJump("brcs", branchScope.getEnd())); // Если <
						cont.append(new CGIAsmCondJump("breq", branchScope.getEnd())); // Если =
					}
					else {
						cont.append(new CGIAsm("breq", "pc+0x02")); // Если =, пропустить brcc
						cont.append(new CGIAsmCondJump("brcc", branchScope.getEnd())); // Если >
					}
				}
				break;
			case NEQ:
				cont.append(new CGIAsmCondJump(isOr ? "brne" : "breq", branchScope.getEnd()));
				break;
			case EQ:
				cont.append(new CGIAsmCondJump(isOr ? "breq" : "brne", branchScope.getEnd()));
				break;
			default:
				throw new CompileException("CG:constAction: unsupported operator: " + op);
		}
		scope.append(cont);
	}

	/**
	 * cellsCpCellsCommon Загружаем правый операнд в регистр и выполняем инструкци сравнения (cp+cpc)
	 * @param scope - область видимости
	 * @param cont - контейнер для генерации кода
	 * @param leftReg - аодготовленный регистр левого операнда
	 * @param index - индекс ячейки
	 * @param rightCells - ячейки правого операнда
	 * @param size - количество ячеек
	 * @param pushRegs - коллекция регистров нуждающихся в сохранении (push/pop)
	 * @param leftIsFixed - признак типа fixed в левом операнде
	 * @param rightIsFixed - признак типа fixed в правом операнде
	 * @return признак успешного выполнения (true)
	 * @throws ru.vm5277.common.exceptions.CompileException 
	 */

	private boolean cellsCpCellsCommon(	CGScope scope, CGIContainer cont, String leftReg, int index, CGCells rightCells, int size, Set<String> pushRegs,
										boolean leftIsFixed, boolean rightIsFixed) throws CompileException {
		String rightReg = "";
		if(!leftReg.equals("r19") && 0x03>=acc.getBytes()) rightReg = "r19";
		else if(!leftReg.equals("r18") && 0x02>=acc.getBytes()) rightReg = "r18";
		
		byte[] accRegs = getAccRegs(acc.getBytes());
		switch(rightCells.getType()) {
			case ACC:
				if(leftIsFixed && !rightIsFixed) {
					if(0==index) {
						rightReg = "C0x00";
					}
					else {
						rightReg = (index<accRegs.length ? "r"+accRegs[index-1] : "C0x00");
					}
				}
				else {
					rightReg = (index<accRegs.length ? "r"+accRegs[index] : "C0x00");
				}
				cont.append(new CGIRAsm((0==index ? "cp" : "cpc"), leftReg, rightReg));
				break;
			case ARGS:
			case STACK_FRAME:
				if(leftIsFixed && !rightIsFixed) {
					if(0==index) {
						rightReg = "C0x00";
					}
					else {
						if(rightReg.isEmpty()) rightReg = "zh";
						cont.append(new CGIAsmIReg( 'y', true, rightReg, rightCells.getId(index-1), getCurBlockScope(scope),
													(CGBlockScope)rightCells.getObject(), CGCells.Type.ARGS == rightCells.getType()));
					}
				}
				else {
					if(index<rightCells.getSize()) {
						if(rightReg.isEmpty()) rightReg = "zh";
						cont.append(new CGIAsmIReg( 'y', true, rightReg, rightCells.getId(index), getCurBlockScope(scope),
													(CGBlockScope)rightCells.getObject(), CGCells.Type.ARGS == rightCells.getType()));
					}
					else {
						rightReg = "C0x00";
					}
				}
				cont.append(new CGIRAsm((0==index ? "cp" : "cpc"), leftReg, rightReg));
				break;
			case ARRAY:
				if(leftIsFixed && !rightIsFixed && 0==index) {
					rightReg = "C0x00";
				}
				else {
					if(rightReg.isEmpty()) rightReg = "zh";
					cont.append(new CGIRAsm("ld", rightReg, "x+"));
				}
				cont.append(new CGIRAsm((0==index ? "cp" : "cpc"), leftReg, rightReg));
				break;
			case CONST:
				if(leftIsFixed && !rightIsFixed) {
					if(0==index) {
						rightReg = "C0x00";
					}
					else {
						if((index-1)>=rightCells.getSize()) rightReg = "C0x00";
						else if(0x00==rightCells.getId(index-1)) rightReg = "C0x00";
						else if(0x01==rightCells.getId(index-1)) rightReg = "C0x01";
						else if(0x02==rightCells.getId(index-1)) rightReg = "C0x02";
						else if(0xff==rightCells.getId(index-1)) rightReg = "C0xff";
						else {
							if(rightReg.isEmpty()) rightReg = "zh";
							cont.append(new CGIAsmLd("ldi", rightReg, Integer.toString(rightCells.getId(index-1))));
						}
					}
					cont.append(new CGIRAsm((0==index ? "cp" : "cpc"), leftReg, rightReg));
				}
				else {
					if(0==index) {
						cont.append(new CGIRAsm("cpi", leftReg, Integer.toString(rightCells.getId(index))));
					}
					else {
						if(index>=rightCells.getSize()) rightReg = "C0x00";
						else if(0x00==rightCells.getId(index)) rightReg = "C0x00";
						else if(0x01==rightCells.getId(index)) rightReg = "C0x01";
						else if(0x02==rightCells.getId(index)) rightReg = "C0x02";
						else if(0xff==rightCells.getId(index)) rightReg = "C0xff";
						else {
							if(rightReg.isEmpty()) rightReg = "zh";
							cont.append(new CGIAsmLd("ldi", rightReg, Integer.toString(rightCells.getId(index))));
						}
						cont.append(new CGIRAsm("cpc", leftReg, rightReg));
					}
				}
				break;
			case HEAP:
				if(leftIsFixed && !rightIsFixed) {
					if(0==index) {
						rightReg = "C0x00";
					}
					else {
						if(rightReg.isEmpty()) rightReg = "yh";
						cont.append(new CGIAsmIReg('z', true, rightReg, rightCells.getId(index-1)));
					}
				}
				else {
					if(index<rightCells.getSize()) {
						if(rightReg.isEmpty()) rightReg = "yh";
						cont.append(new CGIAsmIReg('z', true, rightReg, rightCells.getId(index)));
					}
					else {
						rightReg = "C0x00";
					}
				}
				cont.append(new CGIRAsm((0==index ? "cp" : "cpc"), leftReg, rightReg));
				break;
			case HEAP_ALT:
				if(leftIsFixed && !rightIsFixed) {
					if(0==index) {
						rightReg = "C0x00";
					}
					else {
						if(rightReg.isEmpty()) rightReg = "zh";
						if(1==index) {
							scope.append(new CGIAsm("subi", "r26,low(-" + (rightCells.getId(0)) + ")"));
							scope.append(new CGIAsm("sbci", "r27,high(-" + (rightCells.getId(1)) + ")"));
						}
						cont.append(new CGIRAsm("ld", rightReg, "x+"));
					}
				}
				else {
					if(rightReg.isEmpty()) rightReg = "zh";
					if(0==index) {
						scope.append(new CGIAsm("subi", "r26,low(-" + (rightCells.getId(0)) + ")"));
						scope.append(new CGIAsm("sbci", "r27,high(-" + (rightCells.getId(1)) + ")"));
					}
					cont.append(new CGIRAsm("ld", rightReg, "x+"));
				}
				cont.append(new CGIRAsm((0==index ? "cp" : "cpc"), leftReg, rightReg));
				break;
			case REG:
				if(leftIsFixed && !rightIsFixed) {
					if(0==index) {
						rightReg = "C0x00";
					}
					else {
						rightReg = (index<rightCells.getSize() ? "r"+rightCells.getId(index-1) : "C0x00");
					}
				}
				else {
					rightReg = (index<rightCells.getSize() ? "r"+rightCells.getId(index) : "C0x00");
				}
				cont.append(new CGIRAsm((0==index ? "cp" : "cpc"), leftReg, rightReg));
				break;
			case STACK:
				if(leftIsFixed && !rightIsFixed) {
					if(0==index) {
						rightReg = "C0x00";
					}
					else {
						if(rightReg.isEmpty()) rightReg = "j8b_atom";
						cont.append(new CGIRAsm("pop", rightReg));
					}
				}
				else {
					if(index<rightCells.getSize()) {
						if(rightReg.isEmpty()) rightReg = "j8b_atom";
						cont.append(new CGIRAsm("pop", rightReg));
					}
					else {
						rightReg = "C0x00";
					}
				}
				cont.append(new CGIRAsm((0==index ? "cp" : "cpc"), leftReg, rightReg));
				break;
			case STAT:
				if(leftIsFixed && !rightIsFixed) {
					if(0==index) {
						rightReg = "C0x00";
					}
					else {
						if(rightReg.isEmpty()) rightReg = "zh";
						cont.append(new CGIRAsm("lds", rightReg, "_OS_STAT_POOL+" + rightCells.getId(index-1)));
					}
				}
				else {
					if(index<rightCells.getSize()) {
						if(rightReg.isEmpty()) rightReg = "zh";
						cont.append(new CGIRAsm("lds", rightReg, "_OS_STAT_POOL+" + rightCells.getId(index)));
					}
					else {
						rightReg = "C0x00";
					}
				}
				cont.append(new CGIRAsm((0==index ? "cp" : "cpc"), leftReg, rightReg));
				break;
			default:
				return false;
		}
		
		if(rightReg.equals("yh") || rightReg.equals("zh") || rightReg.equals("j8b_atom")) {
			pushRegs.add(rightReg);
		}
		return true;
	}
	
	@Override
	public void boolAccCond(CGScope scope, CGBranch branchScope, boolean byBit0) throws CompileException {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";accum bool is true"));
		if(!byBit0) {
			scope.append(new CGIAsm("tst", "r16"));
			scope.append(new CGIAsmCondJump("breq", branchScope.getEnd()));
		}
		else {
			scope.append(new CGIAsmSkipInstr("sbrs", "r16,0x00"));
			scope.append(new CGIAsmJump(JUMP_INSTR, branchScope.getEnd(), false));
		}
	}

	@Override
	public void boolFlagCond(CGScope scope, boolean inverted, CGBranch branchScope) throws CompileException {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";flag is set!"));
		scope.append(new CGIAsmCondJump(inverted ? "brcs" : "brcc", branchScope.getEnd()));
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
	public void cellsActionAsm(CGScope scope, CGCells cells, int size, CGActionHandler handler) throws CompileException {
		switch(cells.getType()) {
			case REG:
				for(int i=0; i<size; i++) {
					if(cells.getSize()>i) {
						scope.append(handler.action(i, "r" + cells.getId(i)));
					}
					else {
						scope.append(handler.action(i, null));
					}
				}
				break;
			case ARGS:
			case STACK_FRAME:
				for(int i=0; i<size; i++) {
					if(cells.getSize()>i) {
						scope.append(new CGIAsmIReg('y', true, "j8b_atom", cells.getId(i), getCurBlockScope(scope), (CGBlockScope)cells.getObject(),
													CGCells.Type.ARGS == cells.getType()));
						scope.append(handler.action(i, "j8b_atom"));
					}
					else {
						scope.append(handler.action(i, null));
					}
				}
				break;
			case HEAP:
				for(int i=0; i<size; i++) {
					if(cells.getSize()>i) {
						scope.append(new CGIAsmIReg('z', true, "j8b_atom", cells.getId(i)));
						scope.append(handler.action(i, "j8b_atom"));
					}
					else {
						scope.append(handler.action(i, null));
					}
				}
				break;
			case STACK:
				//Значение лежит на вершине стека(BigEndian)
				for(int i=0; i<size; i++) {
					if(cells.getSize()>i) {
						scope.append(new CGIAsm("pop", "j8b_atom"));
						scope.append(handler.action(i, "j8b_atom"));//TODO не могу использовать tmpReg так как он влияет на stack!!!
					}
					else {
						scope.append(handler.action(i, null));
					}
				}
				break;
			case ARRAY:
				for(int i=0; i<size; i++) {
					if(cells.getSize()>i) {
						scope.append(new CGIAsmLd("ld", "j8b_atom", "x" + (i!=(size-1) ? "+" : "")));
						scope.append(handler.action(i, "j8b_atom"));
					}
					else {
						scope.append(handler.action(i, null));
					}
				}
				break;
			case STAT:
				for(int i=0; i<size; i++) {
					if(cells.getSize()>i) {
						scope.append(new CGIAsmLd("lds", "j8b_atom", "_OS_STAT_POOL+" + cells.getId(i)));
						scope.append(handler.action(i, "j8b_atom"));
					}
					else {
						scope.append(handler.action(i, null));
					}
				}
				break;
			default:
				throw new CompileException("Unsupported cell type:" + cells.getType());
		}
	};
	
	public void constActionAsm(CGScope scope, Object k, CGConstActionHandler handler) throws CompileException {
		if(k instanceof byte[]) {
			for(int i=0; i<acc.getBytes(); i++) {
				scope.append(handler.action(i, ((byte[])k)[i]&0xff));
			}
		}
		else {
			for(int i=0; i<acc.getBytes(); i++) {
				scope.append(handler.action(i, (int)(((long)k)>>(i*8))&0xff));
			}
		}
	};

	@Override
	public CGIContainer eNewInstance(	CGScope scope, int heapSize, CGLabelScope metaLabel, VarType type, InstanceType instType,  CGIContainer postfixCont,
										CGExcs excs) throws CompileException {
		CGIContainer cont = new CGIContainer();
		
		if(VERBOSE_LO <= verbose) cont.append(new CGIText(";eNewInstance " + instType + (null!=type  ? "[" + type + "]" : "") + ", heap size:" + heapSize));
		
		setFeature(RTOSFeature.OS_FT_DRAM);
		
//TODO добавить расширенные заголовки для Thread и TimerTask
/*
;---Структура заголовка класса Thread для refSize=0x02---
;---0x00-2B heap size (low/high) - с учетом multithreading данных
;---0x02-1B link counter
;---0x03-2B metadata addr (low/high)
;---данные-для-multithreading---
;---0x05-1B bits(старший ниббл) and state(младший ниббл)
;---0x06-2B next thread (low/high)
;---0x08-2B stack addr (low/high)
;---0x0a-2B stack size (low/high)
;---0x0c-17B high regs + sreg block
;---0x1c-1B sreg
;---0x1d-2B ret point (low/high)

;---Структура заголовка класса TimerTask для refSize=0x02---
;---0x00-2B heap size (low/high) - с учетом multithreading данных
;---0x02-1B link counter
;---0x03-2B metadata addr (low/high)
;---данные-для-multithreading---
;---0x05-1B bits(старший ниббл) and state(младший ниббл)
;---0x06-2B next thread (low/high)
;---0x08-2B stack addr (low/high)
;---0x0a-2B stack size (low/high)
;---0x0c-17B high regs + sreg block
;---0x1c-1B sreg
;---0x1d-2B ret point (low/high)
;---0x1f-2B timestamp
;---0x22-2B period (x1ms)
*/

		if(InstanceType.FIRST_THREAD==instType) {
			RTOSLibs.NEW_THREAD.setRequired();
			cont.append(new CGIAsmLd("ldi", "r16", ""+getThreadHeaderSize()));
			cont.append(new CGIAsmLd("ldi", "r17", "0x00"));
			cont.append(new CGIAsmCall(CALL_INSTR, "j8bproc_new_thread", true));
			codeExcsChecker.outOfMemory(this, cont, excs);
			cont.append(new CGIAsm("std", "z+0x05,C0x02")); // STATEBITS Выполняется
			cont.append(new CGIRAsm("movw", "pid_l", "zl")); // Устанавливаем pid

			// Точка возврата в конце основной нити
			cont.append(new CGIAsmLdLabel("ldi", "r19", "low(_OS_TASK_ENDPOINT)"));
			cont.append(new CGIRAsm("push", "r19"));
			cont.append(new CGIAsmLdLabel("ldi", "r19", "high(_OS_TASK_ENDPOINT)"));
			cont.append(new CGIRAsm("push", "r19"));

		}
		else if(InstanceType.NO_PARENT==instType) {
			RTOSLibs.NEW_CLASS.setRequired();
			cont.append(new CGIAsmLd("ldi", "r16", "low(" + heapSize + ")"));
			cont.append(new CGIAsmLd("ldi", "r17", "high(" + heapSize + ")"));
			cont.append(new CGIAsmCall(CALL_INSTR, "j8bproc_new_class", true));
			codeExcsChecker.outOfMemory(this, cont, excs);
			cont.append(new CGIAsm("std", "z+0x02,c0xff")); //refCntr
			cont.append(new CGIAsmLdLabel("ldi", "r19", "low(" + metaLabel.getName() + "*2)"));
			cont.append(new CGIAsm("std", "z+0x03,r19"));
			cont.append(new CGIAsmLdLabel("ldi", "r19", "high(" + metaLabel.getName()+ "*2)"));
			cont.append(new CGIAsm("std", "z+0x04,r19"));
		}
		else if(InstanceType.CLASS==instType) {
			RTOSLibs.NEW_CLASS.setRequired();
			cont.append(new CGIAsmLd("ldi", "r16", "low(" + heapSize + ")"));
			cont.append(new CGIAsmLd("ldi", "r17", "high(" + heapSize + ")"));
			cont.append(new CGIAsmCall(CALL_INSTR, "j8bproc_new_class", true));
			codeExcsChecker.outOfMemory(this, cont, excs);
			cont.append(new CGIAsm("std", "z+0x02,c0x00"));
			cont.append(new CGIAsmLdLabel("ldi", "r19", "low(" + metaLabel.getName() + "*2)"));
			cont.append(new CGIAsm("std", "z+0x03,r19"));
			cont.append(new CGIAsmLdLabel("ldi", "r19", "high(" + metaLabel.getName()+ "*2)"));
			cont.append(new CGIAsm("std", "z+0x04,r19"));
		}
		else if(InstanceType.THREAD==instType) {
			RTOSLibs.NEW_THREAD.setRequired();
			cont.append(new CGIAsmLd("ldi", "r16", "low(" + heapSize + ")"));
			cont.append(new CGIAsmLd("ldi", "r17", "high(" + heapSize + ")"));
			cont.append(new CGIAsmCall(CALL_INSTR, "j8bproc_new_thread", true));
			codeExcsChecker.outOfMemory(this, cont, excs);
			cont.append(new CGIAsmLdLabel("ldi", "r19", "low(" + metaLabel.getName() + "*2)"));
			cont.append(new CGIAsm("std", "z+0x03,r19"));
			cont.append(new CGIAsmLdLabel("ldi", "r19", "high(" + metaLabel.getName()+ "*2)"));
			cont.append(new CGIAsm("std", "z+0x04,r19"));
			cont.append(new CGIAsm("std", "z+0x05,C0x00")); // STATEBITS Не готова, ждем инициализации

		}
		else if(InstanceType.TIMER==instType) {
			RTOSLibs.NEW_THREAD.setRequired();
			cont.append(new CGIAsmLd("ldi", "r16", "low(" + heapSize + ")"));
			cont.append(new CGIAsmLd("ldi", "r17", "high(" + heapSize + ")"));
			cont.append(new CGIAsmCall(CALL_INSTR, "j8bproc_new_thread", true));
			codeExcsChecker.outOfMemory(this, cont, excs);
			cont.append(new CGIAsmLdLabel("ldi", "r19", "low(" + metaLabel.getName() + "*2)"));
			cont.append(new CGIAsm("std", "z+0x03,r19"));
			cont.append(new CGIAsmLdLabel("ldi", "r19", "high(" + metaLabel.getName()+ "*2)"));
			cont.append(new CGIAsm("std", "z+0x04,r19"));
			cont.append(new CGIAsm("std", "z+0x05,C0x00")); // STATEBITS Не готова, ждем инициализации
			//TODO записать период
		}

		if(null!=postfixCont && (InstanceType.THREAD==instType || InstanceType.TIMER==instType)) {
			postfixCont.append(new CGIAsmLdLabel("ldi", "r19", "0x00")); //TODO костыль. Не менять порядок и не упрощать, см CGCLassScope.getSource
			postfixCont.append(new CGIAsm("std", "z+0x1d,r19"));
			postfixCont.append(new CGIAsmLdLabel("ldi", "r19", "0x00"));
			postfixCont.append(new CGIAsm("std", "z+0x1e,r19"));
			cont.append(postfixCont);
		}

		int headerSize;
		switch(instType) {
			case THREAD:
			case FIRST_THREAD:
				headerSize = getThreadHeaderSize();
				break;
			case TIMER:
				headerSize = getTimerHeaderSize();
				break;
			default:
				headerSize = CLASS_HEADER_SIZE;
		}
		if(InstanceType.FIRST_THREAD!=instType && heapSize>headerSize) {
			int procOffset = headerSize-CLASS_HEADER_SIZE;
			int fieldsSize = heapSize-headerSize;
			// При offset=0 вызов процедуры состоит из 3х инструкций, иначе из 4 или 5 (4 если оптимизатор заменить сложение на ADIW)
			if(((0==procOffset && fieldsSize<=0x03) || (0!=procOffset && fieldsSize<=0x04)) && (heapSize-1)<=MAX_IREG_OFFSET) {
				// Дорого вызывать процедуру, просто размещаем несколько инструкций для инициализации полей
				for(int i=0; i<fieldsSize; i++) {
					cont.append(new CGIRAsm("std", "z+" + ((heapSize-fieldsSize) + i), "C0x00"));
				}
			}
			else {
				RTOSLibs.CLEAR_FIELDS.setRequired();
				cont.append(new CGIAsmLd("ldi", "r16", "low(" + (heapSize-headerSize-1) + ")")); //Размер полей -1 байт (оптимизация функции)
				cont.append(new CGIAsmLd("ldi", "r17", "high(" + (heapSize-headerSize-1) + ")"));
				if(0!=procOffset) {
					//Необходимо сместить Z - пропустить байты расширенного заголовка
					cont.append(new CGIRAsm("add", "zl", "low(" + (headerSize-CLASS_HEADER_SIZE) + ")"));
					cont.append(new CGIRAsm("adc", "zh", "high(" + (headerSize-CLASS_HEADER_SIZE) + ")"));
				}
				cont.append(new CGIAsmCall(CALL_INSTR, "j8bproc_clear_fields_nr", true));
			}
		}

		if(null!=scope) scope.append(cont);
		return cont;
	}
	
	@Override
	public void invokeConstr(CGScope scope, CGLabelScope lScope) throws CompileException {
		call(scope, lScope);
		acc.setSize(VarType.CLASS);
	}
	
	@Override
	public void eNewArray(CGScope scope, VarType type, int depth, int[] cDims, CGExcs excs) throws CompileException {
		setFeature(RTOSFeature.OS_FT_DRAM);
		RTOSLibs.NEW_ARRAY.setRequired();
		
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";eNewArray " + type));
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
		if(null!=cDims) { //Размеры массива известны на момент компиляции
			// Флаги и глубина(1 байт) + refCount(1байт) + тип данных(1байт) + размеры(2/4/6 байт) + тело(данные x байт) данных
			//ARR:
			//HEADER:
			//1B flags&depth
			//1B refCount
			//1B type
			//2B size d1 (h/l)
			//0/2B size d2 (h/l)
			//0/2B size d3 (h/l)
			//DATA

			int dataSize = cDims[0] * (0!=cDims[1] ? cDims[1] : 0x01) * (0!=cDims[2] ? cDims[2] : 0x01) * typeSize;

			//TODO добавить проверку переполнения
			scope.append(new CGIAsmLd("ldi", "r16", Long.toString((headerSize+dataSize)&0xff)));
			scope.append(new CGIAsmLd("ldi", "r17", Long.toString(((headerSize+dataSize)>>0x08)&0xff)));
			scope.append(new CGIAsmCall(CALL_INSTR, "j8bproc_new_array", true)); //Возвращает в аккумуляторе(r16,r17) адрес выделенной памяти
			codeExcsChecker.arrOutOfMemory(this, scope, excs, null);
			scope.append(new CGIAsmLd("ldi", "r19", Long.toString((typeBits<<0x04)+(depth-1)))); //TODO учет только 1 байтового размера типов
			scope.append(new CGIAsm("st", "x+,r19")); // Флаги и глубина
			scope.append(new CGIAsm("st", "x+,C0x01")); // Количество ссылок(массив не может быть создан без присваивания)
			scope.append(new CGIAsmLd("ldi", "r19", Long.toString(vt.getId()&0xff))); //TODO учет только 1 байтового размера типов
			scope.append(new CGIAsm("st", "x+,r19"));
			for(int i=0; i<depth; i++) {
				// Размер каждого измерения (BE - старший байт по младшему адресу)
				// Update - меняю на LE для общего стандарта (большая часть функций RTOS ожидает LE)
				scope.append(new CGIAsmLd("ldi", "r19", Long.toString(cDims[i]&0xff)));
				scope.append(new CGIAsm("st", "x+,r19"));
				scope.append(new CGIAsmLd("ldi", "r19", Long.toString((cDims[i]>>0x08)&0xff)));
				scope.append(new CGIAsm("st", "x+,r19"));
			}
			// Дальше идут данные массива
		}
		else {  //Размеры массива не известны на момент компиляции (в стеке лежат размеры - каждый 2 байта)
			byte[] popRegIds = new byte[depth*2];
			for(int i=0; i<popRegIds.length; i++) {
				popRegIds[i] = 19; //Подготавливаю список регистров в стеке, для возврата стека на корректный адрес
			}
			
			scope.append(new CGIAsmLd("lds", "r28", "SPL"));
			if(def_sph) scope.append(new CGIAsmLd("lds", "r29", "SPH"));
			scope.append(new CGIAsm("adiw", "r28,0x01"));
			// Вычисляем размер данных
			if(1==depth) {
				scope.append(new CGIAsmLd("ld", "r17", "y+"));
				scope.append(new CGIAsmLd("ld", "r16", "y+"));
			}
			else {
				RTOSLibs.MATH_MUL16.setRequired();
				scope.append(new CGIAsmLd("ldi", "r16", "0x01"));
				scope.append(new CGIAsmLd("ldi", "r17", "0x00"));
				scope.append(new CGIAsmLd("ldi", "j8b_atom", Long.toString(depth)));
				CGLabelScope lbScope = new CGLabelScope(null, null, LabelNames.LOOP, true);
				scope.append(lbScope);
				scope.append(new CGIAsmLd("ld", "r19", "y+"));
				scope.append(new CGIAsmLd("ld", "r18", "y+"));
				scope.append(new CGIAsmCall(CALL_INSTR, "os_mul16", true));
				codeExcsChecker.arrMathOverflow(this, scope, excs, popRegIds);
				scope.append(new CGIAsm("dec", "j8b_atom"));
				scope.append(new CGIAsmCondJump("brne" , lbScope));
			}
			// Учитываем размер типа
			if(0x01!=typeSize) {
				scope.append(new CGIAsm("lsl", "r16"));
				scope.append(new CGIAsm("rol", "r17"));
				if(0x04==typeSize) {
					scope.append(new CGIAsm("lsl", "r16"));
					scope.append(new CGIAsm("rol", "r17"));
				}
				codeExcsChecker.arrMathOverflow(this, scope, excs, popRegIds);
			}
			// Учитываем заголовок массива
			scope.append(new CGIAsm("subi", "r16,low(-" + headerSize + ")"));
			scope.append(new CGIAsm("sbci", "r17,high(-" + headerSize + ")"));
			scope.append(new CGIAsmCall(CALL_INSTR, "j8bproc_new_array", true));
			codeExcsChecker.arrOutOfMemory(this, scope, excs, popRegIds);
			scope.append(new CGIAsmLd("ldi", "r19", Long.toString((typeBits<<0x04)+depth-1))); //TODO учет только 1 байтового размера типов
			scope.append(new CGIAsm("st", "x+,r19")); // Флаги и глубина
			scope.append(new CGIAsm("st", "x+,C0x01")); // Количество ссылок(массив не может быть создан без присваивания)
			scope.append(new CGIAsmLd("ldi", "r19",  Long.toString(vt.getId()&0xff))); //TODO учет только 1 байтового размера типов
			scope.append(new CGIAsm("st", "x+,r19"));
			for(int i=0; i<depth; i++) {
				// Размер каждого измерения (BE - старший байт по младшему адресу)
				//TODO проверить и переделать на LE
				scope.append(new CGIAsm("pop", "r19"));
				scope.append(new CGIAsm("st", "x+,r19"));
				scope.append(new CGIAsm("pop", "r19"));
				scope.append(new CGIAsm("st", "x+,r19"));
			}
			// Дальше идут данные массива
		}

		acc.setBytes(0x02);
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) scope.append(new CGIAsm("clt"));
	}

	@Override
	public void eNewArrView(CGScope scope, int depth, CGExcs excs) throws CompileException {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";eNewArrView depth: " + depth));
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) scope.append(new CGIAsm("set"));

		// Флаги: 0x07-isView, 0x06-расширенный тип данных(2байта), 0x05,0x04-размер ячейки-1, 0x02-0x00-глубина-1		
		// Флаги и глубина(1 байт) + адрес массива + размеры(2/4 байта)
		//ARR:
		//HEADER:
		//1B flags&depth
		//2B arr addr
		//2B index 0
		//0/2B index 1

		
		setFeature(RTOSFeature.OS_FT_DRAM);
		setFeature(RTOSFeature.OS_FT_ARRVIEW);
		RTOSLibs.ARRVIEW_MAKE.setRequired();
		
		scope.append(new CGIRAsm("push", "r24"));
		scope.append(new CGIAsmLd("ldi", "r24", Long.toString(0x80+depth)));
		scope.append(new CGIAsmCall(CALL_INSTR, "j8bproc_arrview_make", true)); // Результат в ACCUM
		scope.append(new CGIRAsm("pop", "r24"));
		codeExcsChecker.arrOutOfMemory(this, scope, excs, null);
		acc.setBytes(0x02);
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) scope.append(new CGIAsm("clt"));
	}

	@Override
	public void invokeInterfaceMethod(CGScope scope, String className, String methodName, VarType type, VarType[] types, VarType ifaceType, int methodSN)																										throws CompileException {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";invokeInterfaceMethod " + type + " " + className + "." + methodName));
		
		RTOSLibs.INVOKE_METHOD.setRequired();
		scope.append(new CGIAsmLd("ldi", "r16", Long.toString(ifaceType.getId())));
		scope.append(new CGIAsmLd("ldi", "r17", Long.toString(methodSN))); // Порядковый номер метода без статических и нативных методов
		scope.append(new CGIAsmCall(CALL_INSTR, "j8bproc_invoke_method_nr", true));
//Похоже рудимент, код перенесен в CGBlockScope
/*		int argsSize = 0;
		for(VarType argType : types) {
			argsSize += (-1 == argType.getSize() ? getRefSize() : argType.getSize());
		}
		if(0 != argsSize) scope.append(blockFree(argsSize));
		if(0!=types.length) scope.append(popHeapReg(null, false));*/

		acc.setSize(type);
	}
	
	@Override
	public void methodInvoke(CGScope scope, CGLabelScope lbScope, String signature, VarType type) throws CompileException {
		if(VERBOSE_LO <= verbose) {
			scope.append(new CGIText(";invoke " + signature));
		}

		lbScope.setUsed();
		scope.append(new CGIAsmCall(CALL_INSTR, lbScope.getName(), false));
		acc.setSize(type);
	}

	// Возвращаем имя функции кодогенератора, если не null, вместо вызова нативной функции
	@Override
	public NativeBinding nativeMethodInit(CGScope scope, String signature, boolean isConstants) throws CompileException {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";Native method " + signature));
		NativeBinding nb = device.getPlatform().getNativeBinding(signature);
		byte[][] regs = null;
		if(null!=nb) {
			regs = nb.getRegs();

			if(null!=regs) {
				StringBuilder sb = new StringBuilder();
				for(int i=0; i<regs.length; i++) {
					sb.append(StrUtils.printHexBinary(regs[i]));
				}

				scope.append(new CGIMeta("START_METHOD:" + sb.toString() + " " + signature));
			}

			//TODO добавить проверку после ввода виртуальных портов.
			if(!isConstants || null==nb.getCgFunctName() || nb.getCgFunctName().isEmpty() || !isSupportedFunct(nb.getCgFunctName())) {
				if(!"null".equals(nb.getRTOSFilePath())) {
					includes.add(nb.getRTOSFilePath());
				}
				if(null != nb.getRTOSFeatures()) {
					for(RTOSFeature feature : nb.getRTOSFeatures()) {
						setFeature(feature);
					}
				}
			}	
		}
		nativeMethodRegs.add(regs);
		return nb;
	}
	
	@Override
	public void nativeMethodSetArg(CGScope scope, String signature, List<Byte> storedRegs, int index, boolean isFixed) throws CompileException {
		if(0!=index || null!=nativeMethodRegs.peek()) {
			if(null==nativeMethodRegs.peek() || index>=nativeMethodRegs.peek().length) {
				throw new CompileException("Invalid number of arguments for '" + signature);
			}

			byte[] regs = nativeMethodRegs.peek()[index];
			if(acc.getBytes()>regs.length) {
				scope.append(new CGIText(";TODO check acc overflow: " + acc + ", expected size:" + regs.length));
			}
			if(isFixed) acc.setFixed();
			else acc.setBytes(regs.length);
			scope.append(accResize());

//			Аккумулятор может быть больше, это ни о чем не говорит
//			if(accum.getSize()>regs.length) {
//				throw new CompileException("Accum value too large for argument N" + index + " passing, method: " + signature);
//			}


			// Сохраняем регистры которые перезаписываем для передачи результата
			for(int i=0; i<regs.length; i++) {
				byte dstReg = regs[i];
				//TODO Игнорируем регистровую пару X, так как используется только кратковременно - для работы с ячейкой массива
				if(26==dstReg || 27==dstReg || (acc.getBytes()>i && dstReg==accRegs[i])) continue;

				boolean dstIsUsedAcc = false;
				int accSize = (accumStack.isEmpty() ? acc.getBytes() : (-1==accumStack.peek().getK().getSize() ? refSize : accumStack.peek().getK().getSize()));
				for(byte accReg : getAccRegs(accSize)) {
					if(dstReg==accReg) {
						dstIsUsedAcc = true;
						break;
					}
				}
				if(!dstIsUsedAcc && !storedRegs.contains(dstReg)) {
					scope.append(new CGIRAsm("push", "r" + dstReg));
					storedRegs.add(dstReg);
				}
			}

			//TODO не учитываем что регистр может уже использоваться!
			byte[] accRegs = getAccRegs(acc.getBytes());
			for(int i=0; i<regs.length; i++) {
				byte dstReg = regs[i];
				if(acc.getBytes()>i) {
					if(dstReg==accRegs[i]) continue;
					scope.append(new CGIAsmMv("mov", "r" + dstReg, "r" + accRegs[i]));
				}
				else {
					scope.append(new CGIAsmLd("ldi", "r" + dstReg, "0"));
				}
			}
		}
	}

	@Override
	public void nativeMethodArrArgPrepare(CGScope scope, String signature, List<Byte> storedRegs, int index, boolean checkedByCompiler, CGLabelScope lbScope) throws CompileException {
		// Здесь оказываемся сразу после подготовленных регистров передающих адрес массива и длину обрабатываемых данных
		// Необходимо сместить адрес на начало данных - сейчас установлен на начало заголовка (в котором есть длина массива)
		// А также необходимо проверить выход за пределы массива (если checkedByCompiler=false)

		byte[] arrAddrRegs = nativeMethodRegs.peek()[index-1];

		if(checkedByCompiler) {
			// Если индекс проверен, то достаточно просто задать адрес начал данных
			scope.append(new CGIRAsm("subi", "r"+arrAddrRegs[0], "low(-5)"));
			scope.append(new CGIRAsm("sbci", "r"+arrAddrRegs[1], "high(-5)"));
		}
		else {
			byte[] lengthRegs = nativeMethodRegs.peek()[index];
			
			// Включаем для генерации исключения
			scope.append(new CGIAsm("set"));

			Set<Byte> usedRegs = new HashSet<>();
			if(2<nativeMethodRegs.peek().length) {
				for(int i=0; i<index-1; i++) {
					byte[] regs = nativeMethodRegs.peek()[i];
					for(Byte reg : regs) {
						usedRegs.add(reg);
					}
				}
			}

			// Необходимо убедиться что arrAddrRegs находится в паре индексных регистров или есть пара индексных регистров не задейстованных в arrAddrRegs и
			// lengthRegs

			char indexReg = (char)-1;
			char tmpReg = (char)-1;
			if(26==arrAddrRegs[0] && 27==arrAddrRegs[1]) {
				indexReg = 'x';
			}
			else if(28==arrAddrRegs[0] && 29==arrAddrRegs[1]) {
				indexReg = 'y';
			}
			else if(30==arrAddrRegs[0] && 31==arrAddrRegs[1]) {
				indexReg = 'z';
			}
			else if(26!=arrAddrRegs[0] && 26!=arrAddrRegs[1] && 27!=arrAddrRegs[0] && 27!=arrAddrRegs[1] &&
					26!=lengthRegs[0] && 26!=lengthRegs[1] && 27!=lengthRegs[0] && 27!=lengthRegs[1]) {
				tmpReg = 'x';
			}
			else if(28!=arrAddrRegs[0] && 28!=arrAddrRegs[1] && 29!=arrAddrRegs[0] && 29!=arrAddrRegs[1] &&
					28!=lengthRegs[0] && 28!=lengthRegs[1] && 29!=lengthRegs[0] && 29!=lengthRegs[1]) {
				tmpReg = 'y';
			}
			else if(30!=arrAddrRegs[0] && 30!=arrAddrRegs[1] && 31!=arrAddrRegs[0] && 31!=arrAddrRegs[1] &&
					30!=lengthRegs[0] && 30!=lengthRegs[1] && 31!=lengthRegs[0] && 31!=lengthRegs[1]) {
				tmpReg = 'z';
			}
			if(-1==indexReg && -1==tmpReg) throw new CompileException("Bounds check impossible: index pairs occupied by arguments (suboptimal register allocation).");


			if(-1==indexReg) {
				scope.append(new CGIRAsm("push", tmpReg+"l"));
				scope.append(new CGIAsmMv("mov", tmpReg+"l", "r"+arrAddrRegs[0]));
				scope.append(new CGIRAsm("push", tmpReg+"h"));
				scope.append(new CGIAsmMv("mov", tmpReg+"h", "r"+arrAddrRegs[1]));
			}

			if('x'==indexReg) {
				scope.append(new CGIRAsm("adiw", "xl", "0x03"));
				scope.append(new CGIAsmLd("ld", "j8b_atom", "x+"));
				scope.append(new CGIRAsm("cp", "j8b_atom", "r"+lengthRegs[0]));
				scope.append(new CGIAsmLd("ld", "j8b_atom", "x+"));
			}
			else if(-1!=indexReg) {
				scope.append(new CGIAsmLd("ld", "j8b_atom", indexReg+"+0x03"));
				scope.append(new CGIRAsm("cp", "j8b_atom", "r"+lengthRegs[0]));
				scope.append(new CGIAsmLd("ld", "j8b_atom", indexReg+"+0x04"));
			}
			else if('x'==tmpReg) {
				scope.append(new CGIRAsm("sbiw", "xl", "0x02"));
				scope.append(new CGIAsmLd("ld", "j8b_atom", "x+"));
				scope.append(new CGIRAsm("cp", "j8b_atom", "r"+lengthRegs[0]));
				scope.append(new CGIAsmLd("ld", "j8b_atom", "x+"));
			}
			else {
				scope.append(new CGIAsmLd("ld", "j8b_atom", tmpReg+"+0x03"));
				scope.append(new CGIRAsm("cp", "j8b_atom", "r"+lengthRegs[0]));
				scope.append(new CGIAsmLd("ld", "j8b_atom", tmpReg+"+0x04"));
			}
			scope.append(new CGIRAsm("cpc", "j8b_atom", "r"+lengthRegs[1]));

			if(-1==indexReg) {
				scope.append(new CGIRAsm("pop", tmpReg+"h"));
				scope.append(new CGIRAsm("pop", tmpReg+"l"));
			}

			scope.append(new CGIAsmCondJump("brcs", lbScope)); // При обнаружении выхода за границы прыгаем на код после call не выключая флаг T

			// Выключаем для отключения генерации исключения
			scope.append(new CGIAsm("clt"));

			if('x'!=indexReg && -1!=indexReg) {
				scope.append(new CGIRAsm("adiw", ""+indexReg+"l", "5"));
			}
			else if(-1==indexReg) {
				scope.append(new CGIRAsm("subi", "r"+arrAddrRegs[0], "low(-5)"));
				scope.append(new CGIRAsm("sbci", "r"+arrAddrRegs[1], "high(-5)"));
			}
		}
	}

	@Override
	public void nativeMethodInvoke(CGScope scope, String signature, List<Byte> storedRegs, VarType type) throws CompileException {
		if(VERBOSE_LO <= verbose) {
			scope.append(new CGIText(";invokeNative " + signature));
		}

		nativeMethodRegs.pop();
		
		// signature может быть null, если вызываемого метода не существует, а задача метода сводится только к подготовке данных в конкретные регистры
		if(null!=signature) {
			scope.append(new CGIAsmCall(CALL_INSTR, device.getPlatform().getNativeBinding(signature).getRTOSFunction(), true));
		}
		for(int i = storedRegs.size()-1; i>=0; i--) {
			scope.append(new CGIRAsm("pop", "r"+storedRegs.get(i)));
		}

		acc.setSize(type);
		scope.append(new CGIMeta("END_METHOD " + signature));
	}
	
	@Override
	public void functApply(CGScope scope, String funct, long[] constants) throws CompileException {
		if(VERBOSE_LO <= verbose) {
			scope.append(new CGIText(";functApply " + funct + ", const:" + StrUtils.toString(constants)));
		}
		nativeMethodRegs.pop();

		//TODO для множественных иснтрукций не забывать про:
		//if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) scope.append(new CGIAsm("set"));
		//if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) scope.append(new CGIAsm("clt"));
		
		switch(funct) {
			case "pin_hi":
				if(0x01!=constants.length) throw new CompileException(toString() + " invalid parameters for funct:" + funct);
				String param = "PORT" + (char)('A'+(constants[0x00]>>0x04));
				scope.append(new CGIAsm("sbi", param + ", " + (constants[0x00]&0x0f)));
				break;
			case "pin_lo":
				if(0x01!=constants.length) throw new CompileException(toString() + " invalid parameters for funct:" + funct);
				param = "PORT" + (char)('A'+(constants[0x00]>>0x04));
				scope.append(new CGIAsm("cbi", param + ", " + (constants[0x00]&0x0f)));
				break;
			case "pin_invert":
				if(0x01!=constants.length) throw new CompileException(toString() + " invalid parameters for funct:" + funct);
				param = "PIN" + (char)('A'+(constants[0x00]>>0x04));
				scope.append(new CGIAsm("sbi", param + ", " + (constants[0x00]&0x0f)));
				break;
			case "port_set_state":
				if(0x02!=constants.length) throw new CompileException(toString() + " invalid parameters for funct:" + funct);
				param = "PORT" + (char)('A'+(constants[0x00]));
				if(0x00==constants[0x01]) {
					scope.append(new CGIAsm("out", param + ",C0x00"));
				}
				else if(0x01==constants[0x01]) {
					scope.append(new CGIAsm("out", param + ",C0x01"));
				}
				else if(0x01==constants[0x01]) {
					scope.append(new CGIAsm("out", param + ",C0xFF"));
				}
				else {
					scope.append(new CGIAsmLd("ldi", "r16", Long.toString(constants[0x01])));
					scope.append(new CGIAsm("out", param + ", r16"));
				}
				break;
			case "pin_out":
				if(0x01!=constants.length) throw new CompileException(toString() + " invalid parameters for funct:" + funct);
				param = "DDR" + (char)('A'+(constants[0x00]>>0x04));
				scope.append(new CGIAsm("sbi", param + ", " + (constants[0x00]&0x0f)));
				break;
			case "pin_get":
				if(0x01!=constants.length) throw new CompileException(toString() + " invalid parameters for funct:" + funct);
				param = "PIN" + (char)('A'+(constants[0x00]>>0x04));
				scope.append(new CGIAsm("sez"));
				scope.append(new CGIAsm("sbis", param + ", " + (constants[0x00]&0x0f)));
				scope.append(new CGIAsm("clz"));
				break;
			case "pin_in":
				if(0x01!=constants.length) throw new CompileException(toString() + " invalid parameters for funct:" + funct);
				param = "DDR" + (char)('A'+(constants[0x00]>>0x04));
				scope.append(new CGIAsm("cbi", param + ", " + (constants[0x00]&0x0f)));
				break;
			case "port_set_mode":
				if(0x02!=constants.length) throw new CompileException(toString() + " invalid parameters for funct:" + funct);
				param = "DDR" + (char)('A'+(constants[0x00]));
				if(0x00==constants[0x01]) {
					scope.append(new CGIAsm("out", param + ",C0x00"));
				}
				else if(0x01==constants[0x01]) {
					scope.append(new CGIAsm("out", param + ",C0x01"));
				}
				else if(0x01==constants[0x01]) {
					scope.append(new CGIAsm("out", param + ",C0xFF"));
				}
				else {
					scope.append(new CGIAsmLd("ldi", "r16", Long.toString(constants[0x01])));
					scope.append(new CGIAsm("out", param + ", r16"));
				}
				break;
			case "dispatcher_lock":
				if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) scope.append(new CGIAsm("set"));
				break;
			case "dispatcher_unlock":
				if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) scope.append(new CGIAsm("clt"));
				break;
			default:
				throw new CompileException(toString() + " unsupported funct:" + funct);
		}
	}
	
	@Override
	public void updateClassRefCount(CGScope scope, CGCells cells, boolean isInc) throws CompileException {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";class refCount" + (isInc ? "++" : "--") + " for " + cells));
		
		switch(cells.getType()) {
			case REG:
				regsMv(scope, cellsToRegs(cells), getAccRegs(0x02));
				break;
			case ARGS:
			case STACK_FRAME:
				scope.append(new CGIAsmIReg('y', true, "r16", cells.getId(0), getCurBlockScope(scope), (CGBlockScope)cells.getObject(),
											CGCells.Type.ARGS==cells.getType()));
				scope.append(new CGIAsmIReg('y', true, "r17", cells.getId(1), getCurBlockScope(scope), (CGBlockScope)cells.getObject(),
											CGCells.Type.ARGS==cells.getType()));
				break;
			case HEAP:
				if(0x01 == getRefSize()) {
					scope.append(new CGIAsmIReg('z', true, "r16", cells.getId(0)));
					scope.append(new CGIAsmIReg('z', true, "r17", cells.getId(1)));
				}
				break;
			case STAT:
				scope.append(new CGIAsmLd("lds", "r16", "_OS_STAT_POOL" + cells.getId(0)));
				scope.append(new CGIAsmLd("lds", "r17", "_OS_STAT_POOL" + cells.getId(1)));
				break;
			default:
				throw new CompileException("Unsupported cell type:" + cells.getType());
		}

		RTOSLibs.CLASS_REFCOUNT.setRequired();
		if(isInc) {
			scope.append(new CGIAsmCall(CALL_INSTR, RTOSLibs.CLASS_REFCOUNT_PROCS.J8BPROC_CLASS_REFCOUNT_INC_NR.name(), true));
		}
		else {
			scope.append(new CGIAsmCall(CALL_INSTR, RTOSLibs.CLASS_REFCOUNT_PROCS.J8BPROC_CLASS_REFCOUNT_DEC_NR.name(), true));
		}
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
					scope.append(new CGIAsmMv("mov", "r26", "r" + cells.getId(0x00)));
					if(0x01 == getRefSize()) {
						scope.append(new CGIAsmLd("ldi", "r27", "0"));
					}
					else {
						scope.append(new CGIAsmMv("mov", "r27", "r" + cells.getId(0x01)));
					}
					break;
				case ARGS:
				case STACK_FRAME:
					scope.append(new CGIAsmIReg('y', true, "r26", cells.getId(0), getCurBlockScope(scope), (CGBlockScope)cells.getObject(),
												CGCells.Type.ARGS == cells.getType()));
					if(0x01 == getRefSize()) {
						scope.append(new CGIAsmLd("ldi", "r27", "0x00"));
					}
					else {
						scope.append(new CGIAsmIReg('y', true, "r27", cells.getId(1)));
					}
					break;
				case HEAP:
					if(0x01 == getRefSize()) {
						scope.append(new CGIAsmIReg('z', true, "r26", cells.getId(0)));
						scope.append(new CGIAsmLd("ldi", "r27", "0x00"));
					}
					else {
						scope.append(new CGIAsmIReg('z', true, "j8b_atom", cells.getId(0)));
						scope.append(new CGIAsmIReg('z', true, "r27", cells.getId(1)));
						scope.append(new CGIAsmMv("mov", "r26", "j8b_atom"));
					}
					break;
				case STAT:
					if(0x01 == getRefSize()) {
						scope.append(new CGIAsmLd("lds", "r26", "_OS_STAT_POOL" + cells.getId(0)));
						scope.append(new CGIAsmLd("ldi", "r27", "0x00"));
					}
					else {
						scope.append(new CGIAsmLd("lds", "j8b_atom", "_OS_STAT_POOL+" + cells.getId(0)));
						scope.append(new CGIAsmLd("lds", "r27", "_OS_STAT_POOL+" + cells.getId(1)));
						scope.append(new CGIAsmMv("mov", "r26", "j8b_atom"));
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
			scope.append(new CGIAsmCall(CALL_INSTR, "j8bproc_arrview_arraddr", true));
			scope.append(new CGIAsmCall(CALL_INSTR, "j8bproc_arr_refcount_" + (isInc ? "inc" : "dec"), true));
		}
		else if(isInc || !(cells instanceof CGArrCells) || !((CGArrCells)cells).canComputeStatic()) {
			scope.append(new CGIAsmCall(CALL_INSTR, "j8bproc_arr_refcount_" + (isInc ? "inc" : "dec"), true));
		}
		else {
			int size = ((CGArrCells)cells).computeStaticSize();
			scope.append(new CGIAsmLd("ldi", "r16", Long.toString(size&0xff)));
			scope.append(new CGIAsmLd("ldi", "r17", Long.toString((size>>0x08)&0xff)));
			scope.append(new CGIAsmCall(CALL_INSTR, "j8bproc_arr_refcount_dec_const", true));
		}

		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) scope.append(new CGIAsm("clt"));
	}

	@Override
	public void eInstanceof(CGScope scope, VarType type) throws CompileException {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";eInstanceOf '" + type + "'"));
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) scope.append(new CGIAsm("set"));
		RTOSLibs.INCTANCEOF.setRequired();
		
		if(0x04==acc.getBytes()) scope.append(new CGIAsmMv("mov", "j8b_atom", "r19"));
		scope.append(new CGIAsmLd("ldi", "r19", Long.toString(type.getId())));
		scope.append(new CGIAsmCall(CALL_INSTR, "j8bproc_instanceof_nr", true));
		if(0x04==acc.getBytes()) scope.append(new CGIAsmMv("mov", "r19", "j8b_atom"));
		
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) scope.append(new CGIAsm("clt"));
	}
	
	@Override
	public void eTry(CGBlockScope blockScope, List<Case> cases, CGBlockScope defaultBlockScope) {
		//System.out.println("CG:try, block:" + blockScope + ", casesBlocks:" + cases + ", defaultBlock:" + defaultBlockScope);
	}
	@Override
	public void eCatch(CGScope scope) throws CompileException {
		scope.append(new CGIAsm("clt")); // Исключение обработано
	}
	
	@Override
	public CGIContainer eReturn(CGScope scope, VarType retType, CGLabelScope lScope) throws CompileException {
		CGIContainer result = new CGIContainer();

		if(VERBOSE_LO <= verbose) result.append(new CGIText(";return [" + retType + "] " + (null==lScope ? "" : lScope)));
		
		if(VarType.VOID!=retType) {
			int size = (-1==retType.getSize() ? refSize : retType.getSize());
			if(acc.getBytes()>size) {
				result.append(new CGIText(";TODO check acc overflow: " + acc + ", expected size:" + size));
			}
			if(retType.isFixedPoint()) {
				acc.setFixed();
			}
			else {
				acc.setBytes(size);
			}
			result.append(accResize());
		}
		
		if(null==lScope) {
			result.append(new CGIAsm("ret"));			
		}
		else {
			result.append(jump(null, lScope));
		}
		
		if(null!=scope) scope.append(result);
		return result;
	}
	
	@Override
	public void eThrow(CGScope scope, int exceptionId, boolean isMethodLeave, CGLabelScope lbScope, boolean withCode, ExcsThrowPoint point)
																																	throws CompileException {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(	";eThrow exId:" + exceptionId + ", leave " + (isMethodLeave ? "method" : "try") +
															(withCode ? " [with code]" : "")));
		
		setFeature(RTOSFeature.OS_FT_ETRACE);
		RTOSLibs.ETRACE_ADD.setRequired();
		
		scope.append(new CGIAsmLd("ldi", "r16", Integer.toString(exceptionId)));
		if(!withCode) {
			scope.append(new CGIAsmLd("ldi", "r17", "0x00"));
		}

		CGIContainer cont7b = new CGIContainer();
		// Нельзя применять CGAsmLd, иначе оптимизатор вырежет 'лишний' ldi
		cont7b.append(new CGIAsm("ldi", "r18," + (point.getId()&0x7f)));
		CGIContainer cont15b = new CGIContainer();
		cont15b.append(new CGIAsm("ldi", "r18,low(" + (point.getId()&0x7fff) + ")"));
		cont15b.append(new CGIAsm("ldi", "r19,high(" + (point.getId()&0x7fff) + ")"));
		scope.append(point.makeContainer(cont7b, cont15b));
		scope.append(new CGIAsmCall(CALL_INSTR, "j8bproc_etrace_addfirst", true));
		
		if(isMethodLeave) {
			scope.append(new CGIAsm("set")); // При выходе из метода включаем флаг T, он занят для блокировки диспетчера, но это не существенно
		}
		jump(scope, lbScope);
	}
	
	@Override
	public void throwCheck(	CGScope scope, List<Pair<CGLabelScope, Set<Integer>>> exceptionHandlers, CGLabelScope methodEndLbScope,
							ExcsThrowPoint point)																					throws CompileException {
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";throwsCheck"));

		CGLabelScope lbSkipThrow = new CGLabelScope(null, genId(), LabelNames.THROW_SKIP, true);
		scope.append(new CGIAsmCondJump("brtc", lbSkipThrow));

		// Необходимо добавлять точку в трассировку в любом случае
		RTOSLibs.ETRACE_ADD.setRequired();
		CGIContainer cont7b = new CGIContainer();
		cont7b.append(new CGIAsm("ldi", "r18," + (point.getId()&0x7f)));
		CGIContainer cont15b = new CGIContainer();
		cont15b.append(new CGIAsm("ldi", "r18,low(" + (point.getId()&0x7fff) + ")"));
		cont15b.append(new CGIAsm("ldi", "r19,high(" + (point.getId()&0x7fff) + ")"));
		scope.append(point.makeContainer(cont7b, cont15b));
		scope.append(new CGIAsmCall(CALL_INSTR, "j8bproc_etrace_add", true));
		
		boolean exceptionhandled = false;
		for(Pair<CGLabelScope, Set<Integer>> pair : exceptionHandlers) {
			if(null==pair.getV()) {
				jump(scope, pair.getK());
				exceptionhandled = true;
				break;
			}
			
			List<Integer> ids = new ArrayList<>(pair.getV());
			Collections.sort(ids);
			for(Integer id : ids) {
				scope.append(new CGIAsm("cpi", "r16," + id));
				scope.append(new CGIAsmCondJump("breq", pair.getK()));
			}
		}
		
		if(!exceptionhandled) {
/*			RTOSLibs.ETRACE_ADD.setRequired();
			CGIContainer cont7b = new CGIContainer();
			cont7b.append(new CGIAsm("ldi", "r18," + (point.getId()&0x7f)));
			CGIContainer cont15b = new CGIContainer();
			cont15b.append(new CGIAsm("ldi", "r18,low(" + (point.getId()&0x7fff) + ")"));
			cont15b.append(new CGIAsm("ldi", "r19,high(" + (point.getId()&0x7fff) + ")"));
			scope.append(point.makeContainer(cont7b, cont15b));
			scope.append(new CGIAsmCall(CALL_INSTR, "j8bproc_etrace_add", true));*/
			jump(scope, methodEndLbScope);
		}
		scope.append(lbSkipThrow);
		if(VERBOSE_LO <= verbose) scope.append(new CGIText(";throwsCheck end"));
	}

	@Override
	public void exTypeIdToAcc(CGScope scope) throws CompileException {
		acc.setSize(VarType.BYTE);
		scope.append(new CGIAsmLd("lds", "r16", "_os_etrace_buffer+0x00"));
	}
	@Override
	public void exCodeToAcc(CGScope scope) throws CompileException {
		acc.setSize(VarType.BYTE);
		scope.append(new CGIAsmLd("lds", "r16", "_os_etrace_buffer+0x01"));
	}
	@Override
	public void exCodeToAccH(CGScope scope, long numValue) throws CompileException {
		scope.append(new CGIAsmLd("ldi", "r17", Long.toString(numValue)));
	}
	
	@Override
	public void terminate(CGScope scope, boolean systemStop, boolean excsCheck) throws CompileException {
		Object obj = device.getParam(RTOSParam.HALT_OK_MODE);
		RTOSHaltMode okMode = (null==obj ? RTOSHaltMode.HALT : RTOSHaltMode.values()[(Integer)obj]);
		obj = device.getParam(RTOSParam.HALT_ERR_MODE);
		RTOSHaltMode errMode = (null==obj ? RTOSHaltMode.HALT : RTOSHaltMode.values()[(Integer)obj]);

		if(RTOSHaltMode.HALT==okMode || RTOSHaltMode.HALT==errMode) {
			RTOSLibs.MCU_HALT.setRequired();
		}
		if(RTOSHaltMode.BLINK==okMode || RTOSHaltMode.BLINK==errMode) {
			RTOSLibs.MCU_BLINK_FOREVER.setRequired();
		}
		if(RTOSHaltMode.RESET==okMode || RTOSHaltMode.RESET==errMode) {
			RTOSLibs.MCU_RESET.setRequired();
		}
		if(RTOSHaltMode.BLINK_AND_RESET==okMode || RTOSHaltMode.BLINK_AND_RESET==errMode) {
			RTOSLibs.MCU_BLINK_N_RESET.setRequired();
		}

		if(systemStop && !excsCheck) {
			if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) {
				// При многопоточности мы должны завершить поток, если нет ошибок
				//TODO Вероятно потом стоит добавить переход на процедуру останова в диспетчере, когда все нити будут завершены
				scope.append(new CGIAsm("ret"));
			}
			else {
				scope.append(new CGIAsmJump(JUMP_INSTR, okMode.getProcName(), true));
			}
		}
		else {
			CGLabelScope lbScope = new CGLabelScope(null, genId(), LabelNames.SKIP, true);
			scope.append(new CGIAsmCondJump("brts", lbScope));
			scope.append(new CGIAsmJump(JUMP_INSTR, okMode.getProcName(), true));
			scope.append(lbScope);
//TODO Нужна STDOUT FEATURE
			if(device.containsParam(RTOSParam.STDIO_PORT)) {
				RTOSLibs.ETRACE_OUT.setRequired();
				scope.append(new CGIAsmCall(CALL_INSTR, "j8bproc_etrace_out", true));
			}
			scope.append(new CGIAsmJump(JUMP_INSTR, errMode.getProcName(), true));
		}
	}

	@Override
	public int getRefSize() {
		return refSize;
	}
	
	@Override
	public int getCallSize() {
		return def_jmp ? 0x02 : 0x01;
	}
	@Override
	public int getCallStackSize() {
		return 0x02;
	}

	@Override
	public HashMap<Byte, RegPair> buildRegsPool() {
		HashMap<Byte, RegPair> result = new HashMap<>();
		//TODO X также задействован в AST inlining(более приоритетно)
		//if(CodeGenerator.arraysUsage) {
			//Используем укороченный пул с резервированием X для массивов
			for(byte b : usableRegs) {result.put(b, new RegPair(b));}
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
					int delta = (null==instr.getReg() ? -zOffset : instr.getOffset()-zOffset);
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
		if(0==offset) return;
		if(0<offset && offset<=MAX_IREG_OFFSET) {
			cont.append(new CGIRAsm("adiw", iReg + "l", "" + offset));
		}
		else if(0>offset && (offset*(-1))<=MAX_IREG_OFFSET) {
			cont.append(new CGIRAsm("sbiw", iReg + "l", "" + offset*(-1)));
		}
		else {
			cont.append(new CGIRAsm("subi", iReg + "l", "low(" + offset*(-1) + ")"));
			cont.append(new CGIRAsm("sbci", iReg + "h", "high(" + offset*(-1) + ")"));
		}
	}

	@Override
	public void flashDataToArr(CGScope scope, DataSymbol symbol, int offset) throws CompileException {
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) scope.append(new CGIAsm("set"));
		RTOSLibs.ROM_READ16.setRequired();
		scope.append(new CGIAsm("push", "zl"));
		scope.append(new CGIAsm("push", "zh"));
		scope.append(new CGIAsmLdLabel("ldi", "zl", "low(" + symbol.getLabel() + "*2)"));
		scope.append(new CGIAsmLdLabel("ldi", "zh", "high(" + symbol.getLabel() + "*2)"));
		scope.append(new CGIAsmLd("ldi", "r16", "low(" + symbol.getSize() + ")"));
		scope.append(new CGIAsmLd("ldi", "r17", "high(" + symbol.getSize() + ")"));
		scope.append(new CGIAsmCall(CALL_INSTR, "os_rom_read16_nr", true));
		scope.append(new CGIAsm("pop", "zh"));
		scope.append(new CGIAsm("pop", "zl"));
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) scope.append(new CGIAsm("clt"));
	}

	private String getConstReg(CGIContainer cont, byte reg, int index, long value) throws CompileException {
		return getConstReg(cont, reg, (int)(value>>(index*8)&0xff));
	}
	private String getConstReg(CGIContainer cont, byte reg, int octet) throws CompileException {
		switch(octet) {
			case 0x00: return "c0x00";
			case 0x01: return "c0x01";
			case 0x02: return "c0x02";
			case 0xff: return "c0xff";
			default: {
				cont.append(new CGIAsmLd("ldi", "r"+reg, Integer.toString(octet)));
				return "r"+reg;
			}
		}
	}
	
	@Override
	public int getThreadHeaderSize() {
		// Стандартный заголовок + блок для нити + блок для сохранения 16 старших регистров + SREG + ret point + timestamp + resource id
		return CLASS_HEADER_SIZE + 0x07 + 0x10 + 0x01 + 0x02 + 0x02 + 0x02;
	}

	@Override
	public int getTimerHeaderSize() {
		 // Стандартный заголовок + блок для нити + блок для сохранения 16 старших регистров + SREG + ret point + timestamp + period
		return CLASS_HEADER_SIZE + 0x07 + 0x10 + 0x01 + 0x02 + 0x02 + 0x02;
	}
}
