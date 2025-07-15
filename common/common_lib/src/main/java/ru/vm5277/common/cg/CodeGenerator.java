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

import ru.vm5277.common.cg.scopes.CGClassScope;
import ru.vm5277.common.cg.scopes.CGInterfaceScope;
import ru.vm5277.common.cg.scopes.CGExpressionScope;
import ru.vm5277.common.cg.scopes.CGVarScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.cg.scopes.CGBlockScope;
import ru.vm5277.common.cg.scopes.CGMethodScope;
import ru.vm5277.common.cg.items.CGItem;
import ru.vm5277.common.cg.items.CGIText;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ru.vm5277.common.NativeBinding;
import ru.vm5277.common.Operator;
import ru.vm5277.common.RTOSFeature;
import ru.vm5277.common.SystemParam;
import ru.vm5277.common.cg.items.CGIAsm;
import ru.vm5277.common.cg.scopes.CGCellsScope;
import ru.vm5277.common.cg.scopes.CGDepScope;
import ru.vm5277.common.cg.scopes.CGFieldScope;
import ru.vm5277.common.cg.scopes.CGLabelScope;
import ru.vm5277.common.compiler.Case;
import static ru.vm5277.common.cg.OperandType.LITERAL;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.SemanticException;

public abstract class CodeGenerator extends CGScope {
	protected			final	String						platform;
	protected			final	Set<RTOSFeature>			RTOSFeatures		= new HashSet<>();
	protected			final	Set<String>					includes			= new HashSet<>();
	protected			final	Map<String, NativeBinding>	nbMap;
	protected			final	Map<Integer, CGLabelScope>	labels				= new HashMap<>();
	protected			final	Map<SystemParam, Object>	params;
	protected	static	final	Map<Integer, DataSymbol>	flashData			= new HashMap<>();
	protected	static			Map<String, CGDepScope>		dependies			= new HashMap<>();
	protected					CGScope						scope				= null;
	protected	final			CGAccum						accum				= new CGAccum();
	protected					boolean						dpStackAlloc		= false; // зависимость от процедур выделения и освобождения памяти в стеке

	public CodeGenerator(String platform, Map<String, NativeBinding> nbMap, Map<SystemParam, Object> params) {
		this.platform = platform;
		this.nbMap = nbMap;
		this.params = params;
	}
	
	public CGClassScope enterClass(VarType type, int[] intrerfaceIds, String name) {
		scope = new CGClassScope(scope, genId(), type, intrerfaceIds, name);
		return (CGClassScope)scope;
	}
	public void leaveClass() {
		if(null != scope.getParent()) scope = scope.free(); // оставялем верхний класс в зоне видимости
		
		//TODO
	}

	public CGInterfaceScope enterInterface(VarType type, int[] intrerfaceIds, String name) {
		scope = new CGInterfaceScope(scope, genId(), type, intrerfaceIds, name);
		return (CGInterfaceScope)scope;
	}
	public void leaveInterface() {
		scope = scope.free();
		//TODO
	}

	public CGMethodScope enterConstructor(VarType[] types) {
		scope = new CGMethodScope((CGClassScope)scope, null, genId(), VarType.NULL, types, "constr", buildRegsPool());
		return (CGMethodScope)scope;
	}
	public void leaveConstructor() {
		scope = scope.free();
	}

	public CGMethodScope enterMethod(VarType type, VarType[] types, String name) {
		scope = new CGMethodScope((CGClassScope)scope, makeLabel(scope, null, false), genId(), type, types, name, buildRegsPool());
		return (CGMethodScope)scope;
	}
	public void leaveMethod() {
		scope = scope.free();
	}

	public CGBlockScope enterBlock() {
		scope = new CGBlockScope(scope, genId(), "");		
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

	
	public CGVarScope enterLocal(VarType type, boolean isConstant, String name) throws SemanticException { //TODO сделать индексацию вместо имен
		scope = new CGVarScope(scope, genId(), type, (-1 == type.getSize() ? getRefSize() : type.getSize()), isConstant, name);

		return (CGVarScope)scope;
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
	public abstract void varStore(CGVarScope lScope, long value) throws Exception;
	
	public CGFieldScope enterField(VarType type, boolean isStatic, String name) throws SemanticException {
		scope = new CGFieldScope((CGClassScope)scope, genId(), type, (-1 == type.getSize() ? getRefSize() : type.getSize()), isStatic, name);

		return (CGFieldScope)scope;
	}
	public void leaveField() {
		scope = scope.free();
		//TODO
	}
	
	
	public CGExpressionScope enterExpression() throws SemanticException {
		//int _resId = genId();
		if(isExpressionScope()) {
			((CGExpressionScope)scope).enter();
		}
		else {
			accum.set(null, 0x01, 0x00); // Сброс аккумулятора

			scope = new CGExpressionScope(scope);
		}
		return (CGExpressionScope)scope;
	}
	
	public void leaveExpression() {
		if(0==((CGExpressionScope)scope).leave()) {
			scope = scope.free();
			//TODO
		}
	}

	public void setAcc(CGScope scope, int size, long value) throws Exception {
		accum.set(scope, size, value);
		scope.append(new CGIText("CG:value '" + value + "'->acc from " + scope));
		scope.append(new CGIAsm("ldi r16," + (value&0xff))); value >>= 0x08;
		if(size>1) scope.append(new CGIAsm("ldi r17," + (value&0xff))); value >>= 0x08;
		if(size>2) scope.append(new CGIAsm("ldi r18," + (value&0xff))); value >>= 0x08;
		if(size>3) scope.append(new CGIAsm("ldi r19," + (value&0xff)));
	}

	public long getAcc() {
		System.out.println("CG:getAcc, acc:" + accum);
		return accum.getValue();
	}
	public abstract void cellsToAcc(CGScope scope, CGCellsScope vScope) throws Exception;
	public abstract void accToCells(CGScope scope, CGCellsScope vScope) throws Exception;
		
	public abstract void emitUnary(Operator op, Integer resId) throws Exception ;
	
	public void defineStr(CGCellsScope cScope, String text) {
		//flashData.put(lScope.getResId(), new DataSymbol(lScope, "javl_" + scope.getParent().getPath("_")+"_r"+resId, -1, text));
		DataSymbol ds = new DataSymbol("javl_" + scope.getPath("_") + "_r" + cScope.getResId(), -1, text);
		flashData.put(cScope.getResId(), ds);
		cScope.setDataSymbol(ds);
	}
	public void defineData(CGCellsScope cScope, int size, long value) {
		DataSymbol ds = new DataSymbol("javl_" + scope.getPath("_") + "_r" + cScope.getResId(), size, value);
		flashData.put(cScope.getResId(), ds);
		cScope.setDataSymbol(ds);
	}

	protected CGLabelScope makeLabel(CGScope scope, String name, boolean isUsed) {
		CGLabelScope lScope = new CGLabelScope(scope, name, isUsed);
		labels.put(lScope.getResId(), lScope);
		if(null != scope) scope.append(lScope);
		return lScope;
	}

	//public abstract void invokeMethod(String className, String methodName, String params, int id, int typeId, Operand[] operands);
	public abstract void invokeMethod(String className, String methodName, int typeId);
	public abstract void invokeNative(CGScope scope, String className, String methodName, String params, VarType type, Operand[] operands) throws Exception; 
	public abstract boolean emitInstanceof(long op, int typeId);
	
	//TODO набор методов для реализации команд if, switch, for, loop и .т.д
	public abstract void eNew(int typeId, long[] parameters, boolean canThrow);
	public abstract void eFree(Operand op);
	
	public abstract void eIf(CGBlockScope conditionBlock, CGBlockScope thenBlock, CGBlockScope elseBlock);
	public abstract void eTry(CGBlockScope blockScope, List<Case> cases, CGBlockScope defaultBlockScope);
	public abstract void eWhile(CGBlockScope condScope, CGBlockScope bodyScope) throws Exception;
	public abstract void eReturn(CGScope scope, int size);
	public abstract void eThrow();
	
	public abstract int getRefSize();
	public abstract void loadConstToAcc(CGScope scope, int size, long value);
	
	public abstract List<Byte> buildRegsPool();
	public abstract CGItem pushRegAsm(int reg);
	public abstract CGItem popRegAsm(int reg);
//	public abstract void setValueByIndex(Operand op, int size, List<Byte> tempRegs) throws Exception;
	public abstract CGItem stackAllocAsm(int size);
	public abstract CGItem stackFreeAsm();
	public abstract String getVersion();

	public abstract void cellsAction(CGScope scope, CGCellsScope cScope, Operator op) throws Exception;
	public abstract void constAction(CGScope scope, Operator op, long k) throws Exception;
	public abstract void loadRegsConst(CGScope scope, byte[] regs, long value);
	public abstract void constLoadRegs(String label, byte[] registers);
	
	public void postBuild() throws Exception {
		scope.prepend(new CGIText("; vm5277." + platform + " v" + getVersion() + " at " + new Date().toString()));
		
		if(params.keySet().contains(SystemParam.CORE_FREQ)) {
			scope.insert(new CGIText(".equ core_freq = " + params.get(SystemParam.CORE_FREQ)));
		}
		if(params.keySet().contains(SystemParam.STDOUT_PORT)) {
			scope.insert(new CGIText(".equ stdout_port = " + params.get(SystemParam.STDOUT_PORT)));
		}
		scope.insert(new CGIText(""));

		for(RTOSFeature feature : RTOSFeatures) {
			scope.insert(new CGIText(".set " + feature + " = 1"));
		}
		scope.insert(new CGIText(""));

		scope.insert(new CGIText(".include \"devices/" + params.get(SystemParam.MCU) + ".def\""));
		scope.insert(new CGIText(".include \"core/core.asm\""));
		if(dpStackAlloc) {
			scope.insert(new CGIText(".include \"sys/stack_alloc.asm\""));
			scope.insert(new CGIText(".include \"sys/stack_free.asm\""));	
		}
		for(String include : includes) {
			scope.insert(new CGIText(".include \"" + include + "\""));
		}
		scope.insert(new CGIText(""));

		for(int resId : flashData.keySet()) {
			DataSymbol ds = flashData.get(resId);
			scope.insert(new CGIText(ds.getLabel() + ":"));
			if(ds.getValue() instanceof String) {
				scope.insert(new CGIText(CGKOI8R.decode(ds.getValue().toString())));
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
				scope.insert(new CGIText(sb.toString()));
			}
			else throw new Exception("CG:postBuild, unsupported value:" + ds.getValue());
			scope.insert(new CGIText(""));
		}
		scope.insert(new CGIText("main:"));
	}
	
	protected long getNum(Operand op) throws Exception {
		switch(op.getOperandType()) {
			case LITERAL:
				if(op.getValue() instanceof Character) {
					return (long)(Character)op.getValue();
				}
				return ((Number)op.getValue()).longValue();
			default: throw new Exception("CG: getNum, unsupported operand:" + op);
		}
	}

	protected CGVarScope getLocal(CGScope scope, int resId) throws Exception {
		if(scope instanceof CGVarScope && scope.getResId() == resId) return (CGVarScope)scope;

		CGBlockScope bScope = scope.getBlockScope();
		if(null != bScope) {
			return bScope.getLocal(resId);
		}
		else throw new Exception("CG: getNum, can't access to method scope, for resId:" + resId);
	}
	
	public String getAsm() {
		return scope.getSource();
	}
	
	public boolean isExpressionScope() {
		return (scope instanceof CGExpressionScope);
	}
	
	protected boolean isEven(int reg) {
		return 0 == (reg&0x01);
	}
	
	public Collection<CGDepScope> getDependencies() {
		return  dependies.values();
	}
	
	public void setDpStackAlloc() {
		dpStackAlloc = true;
	}
	
	public CGScope getScope() {
		return scope;
	}
}