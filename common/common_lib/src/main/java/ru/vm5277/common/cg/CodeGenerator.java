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

import java.util.ArrayList;
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
import ru.vm5277.common.ImplementInfo;
import ru.vm5277.common.NativeBinding;
import ru.vm5277.common.Operator;
import ru.vm5277.common.RTOSFeature;
import ru.vm5277.common.RTOSLibs;
import ru.vm5277.common.SystemParam;
import ru.vm5277.common.cg.scopes.CGCellsScope;
import ru.vm5277.common.cg.scopes.CGFieldScope;
import ru.vm5277.common.cg.scopes.CGLabelScope;
import ru.vm5277.common.compiler.Case;
import static ru.vm5277.common.cg.OperandType.LITERAL;
import ru.vm5277.common.cg.items.CGIAsm;
import ru.vm5277.common.cg.items.CGIContainer;
import ru.vm5277.common.cg.scopes.CGCommandScope;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;

public abstract class CodeGenerator extends CGScope {
	protected			final	String						platform;
	protected			final	Set<RTOSFeature>			RTOSFeatures		= new HashSet<>();
	protected			final	Set<String>					includes			= new HashSet<>();
	protected			final	Map<String, NativeBinding>	nbMap;
	protected			final	Map<Integer, CGLabelScope>	labels				= new HashMap<>();
	protected			final	Map<SystemParam, Object>	params;
	protected	static	final	Map<Integer, DataSymbol>	flashData			= new HashMap<>();
	protected					CGScope						scope				= null;
	protected	final			CGAccum						accum				= new CGAccum();
	
	public CodeGenerator(String platform, Map<String, NativeBinding> nbMap, Map<SystemParam, Object> params) {
		this.platform = platform;
		this.nbMap = nbMap;
		this.params = params;
	}
	
	public CGClassScope enterClass(VarType type, String name, List<ImplementInfo> impl, boolean isRoot) {
		scope = new CGClassScope(this, scope, genId(), type, name, impl, null != scope && isRoot);
		RTOSLibs.INC_REFCOUNT.setRequired();
		RTOSLibs.DEC_REFCOUNT.setRequired();
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

	public CGMethodScope enterConstructor(VarType[] types) {
		CGMethodScope mScope = new CGMethodScope(this, (CGClassScope)scope, genId(), null, types, ((CGClassScope)scope).getName(), buildRegsPool());
		((CGClassScope)scope).addMethod(mScope);
		scope = mScope;
		return mScope;
	}
	public void leaveConstructor() {
		scope = scope.free();
	}

	public CGMethodScope enterMethod(VarType type, VarType[] types, String name) {
		CGMethodScope mScope = new CGMethodScope(this, (CGClassScope)scope, genId(), type, types, name, buildRegsPool());
		((CGClassScope)scope).addMethod(mScope);
		scope = mScope;
		return mScope;
	}
	public void leaveMethod() {
		scope = scope.free();
	}

	public CGBlockScope enterBlock(CGScope parent) {
		scope = new CGBlockScope(this, scope, genId());
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
	
	
	public CGExpressionScope enterExpression() throws CompileException {
/*		if(isExpressionScope()) {
			((CGExpressionScope)scope).enter();
		}
		else {
			accum.setSize(0x01);
*/
			scope = new CGExpressionScope(scope);
//		}
		return (CGExpressionScope)scope;
	}
	
	public void leaveExpression() {
//		if(0==((CGExpressionScope)scope).leave()) {
			scope = scope.free();
//		}
	}

	public CGCommandScope enterCommand() {
		scope = new CGCommandScope(scope);
		return (CGCommandScope)scope;
	}
	
	public void leaveCommand() {
		scope = scope.free();
	}

	public DataSymbol defineData(int resId, Object obj) {
		DataSymbol ds = new DataSymbol("j8bD" + resId, -1, obj);
		flashData.put(resId, ds);
		return ds;
	}

	public CGLabelScope makeLabel(CGScope scope, String name, boolean isUsed) {
		CGLabelScope lScope = new CGLabelScope(scope, null, name, isUsed);
		labels.put(lScope.getResId(), lScope);
		return lScope;
	}

	public abstract void jump(CGScope scope, CGLabelScope lScope) throws CompileException;
	public abstract CGIContainer accCast(CGScope scope, int size) throws CompileException;
	public abstract void cellsToAcc(CGScope scope, CGCellsScope cScope) throws CompileException;
	public abstract void accToCells(CGScope scope, CGCellsScope cScope) throws CompileException;
//	public abstract void retToCells(CGScope scope, CGCellsScope cScope) throws CompileException;
	public abstract void setHeapReg(CGScope scope, int offset, CGCells cells) throws CompileException;
	public abstract void constToAcc(CGScope scope, int size, long value);
	public abstract void constToCells(CGScope scope, int offset, long value, CGCells cells) throws CompileException;
	public abstract void cellsAction(CGScope scope, int offset, CGCells cells, Operator op) throws CompileException;
	public abstract void constAction(CGScope scope, Operator op, long k) throws CompileException;
	public abstract	void pushAccBE(CGScope scope, int size);
	public abstract	void popAccBE(CGScope scope, int size);
	public abstract	void pushHeapReg(CGScope scope);
	public abstract	void popHeapReg(CGScope scope);
	public abstract	CGIContainer pushStackReg(CGScope scope);
	public abstract	CGIContainer popStackReg(CGScope scope);
	public abstract CGIContainer stackToStackReg(CGScope scope);
	public abstract void pushLabel(CGScope scope, String label);
	public abstract void updateRefCount(CGScope scope, int offset, CGCells cells, boolean isInc) throws CompileException;
	
	public abstract void invokeClassMethod(CGScope scope, String className, String methodName, VarType type, VarType[] types, CGLabelScope lbScope) throws CompileException;
	public abstract void invokeInterfaceMethod(CGScope scope, String className, String methodName, VarType type, VarType[] types, VarType ifaceType, int methodSN) throws CompileException;
	public abstract void invokeNative(CGScope scope, String className, String methodName, String params, VarType type, Operand[] operands)
																																	throws CompileException;
	public abstract void eInstanceof(CGScope scope, VarType type) throws CompileException;
	public abstract void emitUnary(CGScope scope, Operator op, int offset, CGCells cells) throws CompileException;
	
	//TODO набор методов для реализации команд if, switch, for, loop и .т.д
	public abstract CGIContainer eNewInstance(int size, CGLabelScope iidLabel, VarType type, boolean launchPoint, boolean canThrow) throws CompileException;
	public abstract void eFree(Operand op);
	public abstract void eIf(CGScope scope, CGBlockScope thenBlock, CGBlockScope elseBlock);
	public abstract void eTry(CGBlockScope blockScope, List<Case> cases, CGBlockScope defaultBlockScope);
	public abstract void eWhile(CGScope scope, CGScope condScope, CGBlockScope bodyScope) throws CompileException;
	public abstract CGIContainer eReturn(CGScope scope, int size) throws CompileException;
	public abstract void eThrow();
	
	public abstract int getRefSize();
	public abstract int getCallSize();
	public abstract ArrayList<RegPair> buildRegsPool();
	public abstract void pushConst(CGScope scope, int size, long value);
	public abstract void pushCells(CGScope scope, int offset, int size, CGCells cells) throws CompileException;
//	public abstract void setValueByIndex(Operand op, int size, List<Byte> tempRegs) throws CompileException;
	public abstract CGItem stackAlloc(CGScope scope, int size);
	public abstract CGItem stackFree(CGScope scope, int size);
	public abstract String getVersion();

	protected long getNum(Operand op) throws CompileException {
		switch(op.getOperandType()) {
			case LITERAL:
				if(op.getValue() instanceof Character) {
					return (long)(Character)op.getValue();
				}
				return ((Number)op.getValue()).longValue();
			default: throw new CompileException("CG: getNum, unsupported operand:" + op);
		}
	}

	protected CGVarScope getLocal(CGScope scope, int resId) throws CompileException {
		if(scope instanceof CGVarScope && scope.getResId() == resId) return (CGVarScope)scope;

		CGBlockScope bScope = scope.getBlockScope();
		if(null != bScope) {
			return bScope.getLocal(resId);
		}
		else throw new CompileException("CG: getNum, can't access to method scope, for resId:" + resId);
	}
	
	public void build(VarType classType, int fieldsSize) throws CompileException {
		append(new CGIText("; vm5277." + platform + " v" + getVersion() + " at " + new Date().toString()));
		
		// Ниже используется eNew, поэтому всегда необходим RTOSFeature.OS_FT_DRAM
		RTOSFeatures.add(RTOSFeature.OS_FT_DRAM);
		
		if(params.keySet().contains(SystemParam.CORE_FREQ)) {
			append(new CGIText(".equ core_freq = " + params.get(SystemParam.CORE_FREQ)));
		}
		if(params.keySet().contains(SystemParam.STDOUT_PORT)) {
			append(new CGIText(".equ stdout_port = " + params.get(SystemParam.STDOUT_PORT)));
		}
		append(new CGIText(""));

		for(RTOSFeature feature : RTOSFeatures) {
			append(new CGIText(".set " + feature + " = 1"));
		}
		append(new CGIText(""));

		append(new CGIText(".include \"devices/" + params.get(SystemParam.MCU) + ".def\""));
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
			else throw new CompileException("CG: build, unsupported value:" + ds.getValue());
			append(new CGIText(""));
		}
		
		append(new CGIText("Main:"));

		ArrayList<VarType> classTypes = new ArrayList<>();
		classTypes.add(classType);
		//append(eNew(null, 0x05, classTypes, true, false)); // TODO canThrow
		//TODO заглушка
		append(new CGIAsm("jmp j8bCMainMmain"));
	}
	
	public String getAsm() {
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
	
	public CGScope getScope() {
		return scope;
	}
	public CGScope setScope(CGScope scope) {
		CGScope old = this.scope;
		this.scope = scope;
		return old;
	}
	
	public void addLabel(CGLabelScope lbScope) {
		labels.put(lbScope.getResId(), lbScope);
	}
}