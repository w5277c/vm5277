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

package ru.vm5277.common.cg;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import ru.vm5277.common.cg.scopes.CGClassScope;
import ru.vm5277.common.cg.scopes.CGInterfaceScope;
import ru.vm5277.common.cg.scopes.CGExpressionScope;
import ru.vm5277.common.cg.scopes.CGVarScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.cg.scopes.CGBlockScope;
import ru.vm5277.common.cg.scopes.CGMethodScope;
import ru.vm5277.common.cg.items.CGItem;
import ru.vm5277.common.cg.items.CGIText;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import ru.vm5277.common.ExcsThrowPoint;
import ru.vm5277.common.ImplementInfo;
import ru.vm5277.common.LabelNames;
import ru.vm5277.common.NativeBinding;
import ru.vm5277.common.Operator;
import ru.vm5277.common.Pair;
import ru.vm5277.common.RTOSFeature;
import ru.vm5277.common.RTOSLibs;
import ru.vm5277.common.RTOSParam;
import ru.vm5277.common.cg.scopes.CGCellsScope;
import ru.vm5277.common.cg.scopes.CGFieldScope;
import ru.vm5277.common.cg.scopes.CGLabelScope;
import ru.vm5277.common.compiler.Case;
import static ru.vm5277.common.cg.OperandType.LITERAL;
import ru.vm5277.common.cg.items.CGIContainer;
import ru.vm5277.common.cg.scopes.CGCommandScope;
import ru.vm5277.common.cg.scopes.CGLoopBlockScope;
import ru.vm5277.common.cg.scopes.CGTryBlockScope;
import ru.vm5277.common.compiler.Optimization;
import ru.vm5277.common.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;

public abstract class CodeGenerator extends CGScope {
	protected			final	String						platform;
	protected			final	Set<RTOSFeature>			RTOSFeatures		= new HashSet<>();
	protected			final	Set<String>					includes			= new HashSet<>();
	protected			final	Map<String, NativeBinding>	nbMap;
	//protected			final	Map<Integer, CGLabelScope>	labels				= new HashMap<>();
	protected			final	Map<RTOSParam, Object>		params;
	protected			final	TargetInfoBuilder			targetInfoBuilder	= new TargetInfoBuilder();
	protected					CodeOptimizer				codeOptimizer;
	protected					CodeFixer					codeFixer;
	protected			final	int							optLevel;
	protected	static	final	Map<Integer, DataSymbol>	flashData			= new HashMap<>();
	protected	static	final	Map<String, Integer>		flashStrings		= new HashMap<>();
	protected					CGScope						scope				= null;
	protected	final			CGAccum						accum				= new CGAccum();
	public				static	int							CLASS_HEADER_SIZE;
	protected			static	boolean						arraysUsage			= false;
	private				static	int							statPoolSize		= 0;	// Блок для хранения статических полей классов
	
	public CodeGenerator(String platform, int optLevel, Map<String, NativeBinding> nbMap, Map<RTOSParam, Object> params) {
		this.platform = platform;
		this.nbMap = nbMap;
		this.params = params;
		this.optLevel = optLevel;
	}
	
	public CGClassScope enterClass(VarType type, String name, List<ImplementInfo> impl, boolean isRoot) {
		scope = new CGClassScope(this, scope, genId(), type, name, impl, null != scope && isRoot);
		return (CGClassScope)scope;
	}
	public void leaveClass() {
		if(null != scope.getParent()) scope = scope.free(); // оставялем верхний класс в зоне видимости
		
		//TODO
	}

	public CGInterfaceScope enterInterface(VarType type, String name) {
		scope = new CGInterfaceScope(scope, genId(), type, name);
		return (CGInterfaceScope)scope;
	}
	public void leaveInterface() {
		scope = scope.free();
		//TODO
	}

	public CGMethodScope enterConstructor() {
		scope = new CGMethodScope(this, (CGClassScope)scope, genId(), null, 0, ((CGClassScope)scope).getName(), buildRegsPool());
		return (CGMethodScope)scope;
	}
	public void leaveConstructor() {
		scope = scope.free();
	}

	public CGMethodScope enterMethod(VarType type, String name) {
		int size = (-1 == type.getSize() ? getRefSize() : type.getSize());
		scope = new CGMethodScope(this, (CGClassScope)scope, genId(), type, size, name, buildRegsPool());
		return (CGMethodScope)scope;
	}
	public void leaveMethod() {
		scope = scope.free();
	}

	public CGBlockScope enterBlock(boolean isTry) {
		scope = isTry ? new CGTryBlockScope(this, scope, genId()) : new CGBlockScope(this, scope, genId());
		return (CGBlockScope)scope;
	}
	public void leaveBlock() {
/*		CGBlockScope bScope = (CGBlockScope)scope;

		if(!bScope.getUsedRegs().isEmpty()) {
			bScope.begin();
			for(int i=0; i<bScope.getUsedRegs().size(); i++) {
				bScope.insert(pushRegAsm(bScope.getUsedRegs().get(i)));
			}
		}

		int size = bScope.getStackBlockSize();
		if(0x00 != size) {
			dpStackAlloc = true;
			bScope.prepend(stackAllocAsm(size));
		}

		if(!bScope.getUsedRegs().isEmpty()) {
			for(int i=bScope.getUsedRegs().size()-1; i>=0; i--) {
				bScope.append(popRegAsm(bScope.getUsedRegs().get(i)));
			}
		}

		if(0x00 != size) bScope.append(stackFreeAsm());*/
		scope = scope.free();
	}

	public CGBlockScope enterLoopBlock() {
		scope = new CGLoopBlockScope(this, scope, genId());
		return (CGBlockScope)scope;
	}
	public void leaveLoopBlock() {
		scope = scope.free();
	}

	public CGVarScope enterLocal(VarType type, int size, boolean isConstant, String name) throws CompileException {
		this.scope = new CGVarScope(scope, genId(), type, size, isConstant, name);

		return (CGVarScope)this.scope;
	}
	public void leaveLocal() {
/*		CGLocalScope lScope = (CGLocalScope)scope;
		if(lScope.isConstant()) {
			lScope.setDataSymbol(flashData.get(lScope.getResId()));
		}
		else {
//			lScope.setValue(accum.getValue());
//			loadRegs(lScope.getSize());
//			storeAcc(lScope.getResId());
		}
*/		
		scope = scope.free();
		//TODO
	}
	public CGFieldScope enterField(VarType type, boolean isStatic, String name) throws CompileException {
		scope = new CGFieldScope((CGClassScope)scope, genId(), type, (-1 == type.getSize() ? getRefSize() : type.getSize()), isStatic, name);

		return (CGFieldScope)scope;
	}
	public void leaveField() {
		scope = scope.free();
		//TODO
	}
	
	
	public CGExpressionScope enterExpression(String name) {
		scope = new CGExpressionScope(scope, name);
		return (CGExpressionScope)scope;
	}
	public void leaveExpression() {
		scope = scope.free();
	}

	public CGCellsScope enterArrayExpression(VarType type, int size, final CGCells cells) {
		scope = new CGCellsScope(scope, genId(), type, size, "arrExpr") {
			@Override
			public CGCells getCells() {return cells;}
		};
		return (CGCellsScope)scope;
	}
	public void leaveArrayExpression() {
		scope = scope.free();
	}

	public CGCommandScope enterCommand() {
		scope = new CGCommandScope(scope);
		return (CGCommandScope)scope;
	}
	public void leaveCommand() {
		scope = scope.free();
	}

	public DataSymbol defineData(int resId, int size, Object obj) {
		if(obj instanceof String) {
			Integer strResId = flashStrings.get((String)obj);
			if(null!=strResId) {
				return flashData.get(strResId);
			}
		}
		
		DataSymbol ds = new DataSymbol("j8bD" + resId, size, obj);
		flashData.put(resId, ds);
		return ds;
	}

	public TargetInfoBuilder getTargetInfoBuilder() {
		return targetInfoBuilder;
	}
	
	public abstract CGIContainer jump(CGScope scope, CGLabelScope lScope) throws CompileException;
	public abstract CGIContainer call(CGScope scope, CGLabelScope lScope) throws CompileException;
	public abstract CGIContainer accCast(VarType accType, VarType opType) throws CompileException;
	public abstract void accResize(VarType opType) throws CompileException;
	public abstract void cellsToAcc(CGScope scope, CGCells cells) throws CompileException;
	public abstract void cellsToAcc(CGScope scope, CGCellsScope cScope) throws CompileException;
	public abstract void accToCells(CGScope scope, CGCellsScope cScope) throws CompileException;
	
	public abstract void computeArrCellAddr(CGScope scope, CGCells arrAddrCells, CGArrCells arrCells, CGExcs excs) throws CompileException;
	public abstract void arrToAcc(CGScope scope, CGArrCells arrCells) throws CompileException;
	public abstract void accToArr(CGScope scope, CGArrCells arrScope) throws CompileException;
	public abstract void arrToCells(CGScope scope, CGArrCells arrCells, CGCells cells, CGExcs excs) throws CompileException;
	public abstract void flashDataToArr(CGScope scope, DataSymbol symbol, int offset) throws CompileException;
	public abstract void cellsToArrReg(CGScope cgScope, CGCells cells) throws CompileException;
	public abstract void arrSizetoAcc(CGScope cgScope, boolean isView) throws CompileException;
	public abstract void arrRegToCells(CGScope scope, CGCells cells) throws CompileException;
	public abstract void arrRegToAcc(CGScope scope) throws CompileException;
	public abstract void accToArrReg(CGScope scope) throws CompileException;
	
//	public abstract void retToCells(CGScope scope, CGCellsScope cScope) throws CompileException;
	public abstract void setHeapReg(CGScope scope, CGCells cells) throws CompileException;
	public abstract void constToAcc(CGScope scope, int size, long value, boolean isFixed) throws CompileException;
	public abstract CGIContainer constToCells(CGScope scope, long value, CGCells cells, boolean isFixed) throws CompileException;
	public abstract void cellsAction(CGScope scope, CGCells cells, Operator op, boolean isFixed, CGExcs excs) throws CompileException;
	public abstract void constAction(CGScope scope, Operator op, long k, boolean isFixed, CGExcs excs) throws CompileException;
	public abstract void cellsCond(CGScope scope, CGCells cells, Operator op, boolean isNot, boolean isOr, CGBranch branchScope) throws CompileException;
	public abstract void constCond(CGScope scope, CGCells cells, Operator op, long k, boolean isNot, boolean isOr, CGBranch branchScope) throws CompileException;
	public abstract void boolCond(CGScope scope, CGBranch branchScope, boolean byBit0) throws CompileException;
	public abstract	CGIContainer pushAccBE(CGScope scope, Integer size) throws CompileException;
	public abstract	void popAccBE(CGScope scope, Integer size) throws CompileException;
	public abstract	CGIContainer pushHeapReg(CGScope scope) throws CompileException;
	public abstract	CGIContainer popHeapReg(CGScope scope) throws CompileException;
	public abstract	CGIContainer pushStackReg(CGScope scope)throws CompileException;
	public abstract	CGIContainer popStackReg(CGScope scope) throws CompileException;
	public abstract	CGIContainer pushArrReg(CGScope scope) throws CompileException;
	public abstract	CGIContainer popArrReg(CGScope scope) throws CompileException;
	public abstract void thisToAcc(CGScope scope) throws CompileException;
	public abstract void pushLabel(CGScope scope, String label) throws CompileException;
	public abstract void updateClassRefCount(CGScope scope, CGCells cells, boolean isInc) throws CompileException;
	public abstract void updateArrRefCount(CGScope cgScope, CGCells cells, boolean isInc, boolean isView) throws CompileException;
	public abstract void invokeInterfaceMethod(CGScope scope, String className, String methodName, VarType type, VarType[] types, VarType ifaceType, int methodSN) throws CompileException;
	
	//TODO добавить необходимые прамаметры
	public abstract void nativeMethodInit(CGScope scope, String signature) throws CompileException;
	public abstract void nativeMethodSetArg(CGScope scope, String signature, int index) throws CompileException;
	public abstract void nativeMethodInvoke(CGScope scope, String signature) throws CompileException;

	public abstract void eInstanceof(CGScope scope, VarType type) throws CompileException;
	public abstract void eUnary(CGScope scope, Operator op, CGCells cells, boolean toAccum, CGExcs excs) throws CompileException;
	
	//TODO набор методов для реализации команд if, switch, for, loop и .т.д
	public abstract void eNewInstance(CGScope scope, int fieldsSize, CGLabelScope iidLabel, VarType type, boolean launchPoint, CGExcs excs) throws CompileException;
	public abstract CGIContainer eNewArray(VarType type, int depth, int[] cDims, CGExcs excs) throws CompileException;
	public abstract CGIContainer eNewArrView(int depth, CGExcs excs) throws CompileException;
	public abstract void eTry(CGBlockScope blockScope, List<Case> cases, CGBlockScope defaultBlockScope);
	public abstract CGIContainer eReturn(CGScope scope, int argsSize, int varsSize, VarType retType) throws CompileException;
	public abstract CGIContainer finMethod(VarType type, int argsSize, int varsSize, int lastStackOffset) throws CompileException;

	public abstract void eThrow(CGScope cgs, int exceptioId, boolean isMethodLeave, CGLabelScope lbScope, boolean withCode, ExcsThrowPoint point) throws CompileException;
	public abstract void throwCheck(CGScope scope, List<Pair<CGLabelScope, Set<Integer>>> exceptionHandlers, CGLabelScope methodEndLbScope,
									ExcsThrowPoint point) throws CompileException;
	public abstract void terminate(CGScope scope, boolean systemStop, boolean excsCheck) throws CompileException;
	
	public abstract int getRefSize();
	public abstract int getCallSize();
	public abstract int getCallStackSize();
	public abstract ArrayList<RegPair> buildRegsPool();
	public abstract CGIContainer pushConst(CGScope scope, int size, long value, boolean isFixed) throws CompileException;
	public abstract CGIContainer pushCells(CGScope scope, int size, CGCells cells) throws CompileException;
//	public abstract void setValueByIndex(Operand op, int size, List<Byte> tempRegs) throws CompileException;
	public abstract CGIContainer stackPrepare(boolean firstBlock, int argsSize, int varsSize, CGExcs excs) throws CompileException ;
	public abstract CGIContainer blockFree(int varsSize) throws CompileException ;
	public abstract boolean normalizeIRegConst(CGMethodScope mScope, char iReg, AtomicInteger lastOffset) throws CompileException;
	public abstract String getVersion();

	public void setParam(RTOSParam sp, int valueId) {
		params.put(sp, valueId);
	}
	
	public void setFeature(RTOSFeature feature) {
		RTOSFeatures.add(feature);
	}
	public void resetFeature(RTOSFeature feature) {
		RTOSFeatures.remove(feature);
	}
	
	protected long getNum(Operand op) throws CompileException {
		switch(op.getOperandType()) {
			case LITERAL:
				if(op.getValue() instanceof Character) {
					return (long)(Character)op.getValue();
				}
				return ((Number)op.getValue()).longValue();
			case LITERAL_FIXED:
				return ((Number)op.getValue()).longValue();
			default: throw new CompileException("CG: getNum, unsupported operand:" + op);
		}
	}

	protected CGCellsScope getVarField(CGScope scope, int resId) throws CompileException {
		if(scope instanceof CGVarScope && scope.getResId() == resId) return (CGCellsScope)scope;
		if(scope instanceof CGFieldScope && scope.getResId() == resId) return (CGCellsScope)scope;
		CGCellsScope result = null;
		
		CGBlockScope bScope = (CGBlockScope)scope.getScope(CGBlockScope.class);
		if(null != bScope) {
			result = bScope.getVar(resId);
		}
		if(null == result) {
			CGClassScope cScope = (CGClassScope)scope.getScope(CGClassScope.class);
			result = cScope.getField(resId);
		}
		if(null == result) {
			throw new CompileException("CG: getNum, can't access to method scope, for resId:" + resId);
		}
		return result;
	}
	
	public void build(VarType classType, int fieldsSize) throws CompileException {
		append(new CGIText("; vm5277." + platform + " v" + getVersion()));
		
		CGExcs excs = new CGExcs();
		// Во время кодогенерации не знаем какие поля будут использованы в итоге, поэтому размер HEAP можно вычислить только здесь.
/*		List<CGItem> list = new ArrayList<>();
		//TODO не оптимально
		treeToList(scope, list);
		for(CGItem item : list) {
			if(item instanceof CGIConstrInit) {
				CGIConstrInit constrInit = (CGIConstrInit)item;
				CGClassScope cScope = (CGClassScope)constrInit.getMScope().getParent();
				//TODO архитектурная ошибка, формируем код различных конструкторов, в которых есть динамическое выделение памяти, а следовательно и исключения
				//При этом не учитывая контекста - формиуем вне метода в котором объявлен new
				excs.setMethodEndLabel(constrInit.getMScope().getBlockScopes().get(0).getELabel());
				constrInit.getCont().append(eNewInstance(cScope.getHeapOffset(), cScope.getIIDLabel(), cScope.getType(), false, excs));
			}
		}
*/		
		if(params.keySet().contains(RTOSParam.CORE_FREQ)) {
			append(new CGIText(".equ CORE_FREQ = " + params.get(RTOSParam.CORE_FREQ)));
		}
		if(params.keySet().contains(RTOSParam.STDOUT_PORT)) {
			append(new CGIText(".equ STDOUT_PORT = " + params.get(RTOSParam.STDOUT_PORT)));
		}
		if(params.keySet().contains(RTOSParam.ACTLED_PORT)) {
			append(new CGIText(".equ ACTLED_PORT = " + params.get(RTOSParam.ACTLED_PORT)));
		}
		if(0!=statPoolSize) {
			append(new CGIText(".set OS_STAT_POOL_SIZE = " + statPoolSize));
		}
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_ETRACE)) {
			append(new CGIText(".set OS_ETRACE_POINT_BITSIZE = " + Integer.toString(128>targetInfoBuilder.getExcsThrowPointQuantity() ? 7 : 15)));
		}
		append(new CGIText(""));

		targetInfoBuilder.chooseExcsThrowPointContainer();
		
		if(!RTOSFeatures.isEmpty()) {
			List<RTOSFeature> features = new ArrayList<>(RTOSFeatures);
			Collections.sort(features);
			for(RTOSFeature feature : features) {
				append(new CGIText(".set " + feature + " = 1"));
			}
		}
		append(new CGIText(""));

		append(new CGIText(".include \"devices/" + params.get(RTOSParam.MCU) + ".def\""));
		append(new CGIText(".include \"core/core.asm\""));

		if(RTOSFeatures.contains(RTOSFeature.OS_FT_DRAM)) {
			RTOSLibs.DRAM.setRequired();
		}
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_MULTITHREADING)) {
			RTOSLibs.DISPATCHER.setRequired();
		}
		
		for(RTOSLibs lib : RTOSLibs.values()) {
			if(lib.isRequired()) {
				append(new CGIText(".include \"" + lib.getPath() + "\""));
			}
		}
		
		for(String include : includes) {
			append(new CGIText(".include \"" + include + "\""));
		}
		append(new CGIText(""));
		
		for(int resId : flashData.keySet()) {
			DataSymbol ds = flashData.get(resId);
			append(new CGIText(ds.getLabel() + ":"));
			if(ds.getValue() instanceof String) {
				append(new CGIText(CGKOI8R.decode(ds.getValue().toString())));
			}
			else if(ds.getValue() instanceof Number) {
				//TODO рудимент
				StringBuilder sb = new StringBuilder(".db ");
				long value = ((Number)ds.getValue()).longValue();
				for(int i=0; i<ds.getSize(); i++) {
					sb.append((value&0xff)).append(",");
					value >>= 0x08;
				}
				sb.deleteCharAt(sb.length()-1);
				if(0 != (ds.getSize()&0x01)) sb.append(",0x00");
				append(new CGIText(sb.toString()));
			}
			else if(ds.getValue() instanceof Pair) {
				Pair pair = (Pair)ds.getValue();
				if(pair.getK() instanceof VarType) {
					StringBuilder sb = new StringBuilder();
					int size = ((VarType)pair.getK()).getSize();
					if(-1==size) size = getRefSize();

					switch(((VarType)pair.getK()).getSize()) {
						case 4:
							sb.append(".dq ");
							break;

						case 2:
							sb.append(".dw ");
							break;
						default:
							sb.append(".db ");
					}
					
					for(Long value : (ArrayList<Long>)pair.getV()) {
						sb.append(value).append(",");
					}
					sb.deleteCharAt(sb.length()-1);
					if(1==size && 0!=(((ArrayList<Object>)pair.getV()).size()&0x01)) {
						sb.append(",0x00");
					}
					append(new CGIText(sb.toString()));
				}
				else throw new CompileException("CG: build, unsupported value:" + ds.getValue());
			}
			else throw new CompileException("CG: build, unsupported value:" + ds.getValue());
			append(new CGIText(""));
		}
		
		append(new CGIText("Main:"));

		// Очистка статического блока выполняется самой RTOS
		
		jump(this, new CGLabelScope(null, -1, LabelNames.MAIN, true));
	}
	
	public String getAsm(Path asmInstrPath, MessageContainer mc) throws CompileException {
		
		if(null!=codeFixer && null!=asmInstrPath) {
			List<CGItem> codeList = codeFixer.resolveInstructionsSize(scope, asmInstrPath, mc, (String)params.get(RTOSParam.MCU));
			codeFixer.branchRangeFixer(codeList);

			if(Optimization.FRONT<optLevel) {
				boolean modified = true;
				while(modified) {
					modified = false;
					modified|=codeOptimizer.optimizeJumpInstr(codeList);
					modified|=codeOptimizer.optimizeBranchInstr(codeList);
				}
			}
		}

		if(Optimization.FRONT<optLevel && null!=codeOptimizer) {
			codeOptimizer.removeUnusedLabels(scope);
		}

		StringBuilder sb = new StringBuilder(getSource());
		sb.append(scope.getSource());
		return sb.toString();
	}
	
	public boolean isExpressionScope() {
		return (scope instanceof CGExpressionScope);
	}
	
	protected boolean isEven(int reg) {
		return 0 == (reg&0x01);
	}
	
//	public CGScope getScope() {
//		return scope;
//	}
	public CGScope setScope(CGScope scope) {
		CGScope old = this.scope;
		this.scope = scope;
		return old;
	}
	
	public CodeOptimizer getOptimizer() {
		return codeOptimizer;
	}
	
	protected CGBlockScope getCurBlockScope(CGScope scope) {
		CGScope result = scope;
		while(null!=result && !(result instanceof CGBlockScope)) {
			result = result.getParent();
		}
		return (CGBlockScope)result;
	}

	//public void addLabel(CGLabelScope lbScope) {
	//	labels.put(lbScope.getResId(), lbScope);
	//}
	
	public static void treeToList(CGIContainer cont, List<CGItem> list) {
		treeToList(cont, list, null);
	}
	public static void treeToList(CGIContainer cont, List<CGItem> list, String ignoreScanTag) {
		for(CGItem item : cont.getItems()) {
			if(item.isDisabled()) continue;
			
			if(item instanceof CGLabelScope) {
				list.add(item);
			}
			else if(item instanceof CGIContainer) {
				if(null != ignoreScanTag && ignoreScanTag.equalsIgnoreCase(((CGIContainer)item).getTag())) {
					// Пропускаем анализ вложенности для контейнеров с тегом(контейнер должен рассматриваться как единое целое)
					list.add(item);
				}
				else {
					//TODO убрал if(((CGIContainer)item).getItems().isEmpty()) { list.add(item); ... добавлял мусор типа CGExpressionScope, возможно что-то поломал
					if(!((CGIContainer)item).getItems().isEmpty()) {
						treeToList((CGIContainer)item, list, ignoreScanTag);
					}
				}
			}
			else {
				list.add(item);
			}
		}
	}

	public int getOptLevel() {
		return optLevel;
	}
	
	public static void setArraysUsage() {
		arraysUsage = true;
	}
	public static boolean isArraysUsage() {
		return arraysUsage;
	}
	
	public int getAccumSize() {
		return accum.getSize();
	}
	public void setAccumSize(int size) {
		accum.setSize(size);
	}
	
	public int getStatPoolSize() {
		return statPoolSize;
	}
	public int getAndAddStatPoolSize(int size) {
		int tmp = statPoolSize;
		statPoolSize+=size;
		return tmp;
	}
	
	public Map<RTOSParam, Object> getRTOSParams() {
		return params;
	}
}