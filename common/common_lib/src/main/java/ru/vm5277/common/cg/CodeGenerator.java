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
import ru.vm5277.common.NativeBinding;
import ru.vm5277.common.Operator;
import ru.vm5277.common.RTOSFeature;
import ru.vm5277.common.SystemParam;
import ru.vm5277.common.cg.scopes.CGCellsScope;
import ru.vm5277.common.cg.scopes.CGFieldScope;
import ru.vm5277.common.cg.scopes.CGLabelScope;
import ru.vm5277.common.compiler.Case;
import static ru.vm5277.common.cg.OperandType.LITERAL;
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
	protected					boolean						dpStackAlloc		= false; // зависимость от процедур выделения и освобождения памяти в стеке
	
	public CodeGenerator(String platform, Map<String, NativeBinding> nbMap, Map<SystemParam, Object> params) {
		this.platform = platform;
		this.nbMap = nbMap;
		this.params = params;
	}
	
	public CGClassScope enterClass(VarType type, VarType[] intrerfaceTypes, String name, boolean isRoot) {
		scope = new CGClassScope(scope, genId(), type, intrerfaceTypes, name, null != scope && isRoot);
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
		scope = new CGMethodScope(this, (CGClassScope)scope, genId(), VarType.NULL, types, "constr", buildRegsPool());
		return (CGMethodScope)scope;
	}
	public void leaveConstructor() {
		scope = scope.free();
	}

	public CGMethodScope enterMethod(VarType type, VarType[] types, String name) {
		scope = new CGMethodScope(this, (CGClassScope)scope, genId(), type, types, name, buildRegsPool());
		return (CGMethodScope)scope;
	}
	public void leaveMethod() {
		scope = scope.free();
	}

	public CGBlockScope enterBlock() {
		scope = new CGBlockScope(scope, genId());
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

	public void defineStr(CGCellsScope cScope, String text) {
		DataSymbol ds = new DataSymbol("JavlD" + cScope.getResId(), -1, text);
		flashData.put(cScope.getResId(), ds);
		cScope.setDataSymbol(ds);
	}
	public void defineData(CGCellsScope cScope, int size, long value) {
		DataSymbol ds = new DataSymbol("JavlD" + cScope.getResId(), size, value);
		flashData.put(cScope.getResId(), ds);
		cScope.setDataSymbol(ds);
	}

	public CGLabelScope makeLabel(CGScope scope, String name, boolean isUsed) {
		CGLabelScope lScope = new CGLabelScope(scope, null, name, isUsed);
		labels.put(lScope.getResId(), lScope);
		return lScope;
	}

	public abstract void accCast(CGScope scope, VarType type) throws CompileException;
	public abstract void accToRet(CGScope cope);
	public abstract void cellsToAcc(CGScope scope, CGCellsScope cScope) throws CompileException;
	public abstract void accToCells(CGScope scope, CGCellsScope cScope) throws CompileException;
	public abstract void retToCells(CGScope scope, CGCellsScope cScope) throws CompileException;
	public abstract void cellsToRet(CGScope scope, long stackOffset, CGCell[] cells) throws CompileException;
	public abstract void constToAcc(CGScope scope, int size, long value);
	public abstract void constToCells(CGScope scope, long stackOffset, long value, CGCell[] cells) throws CompileException;
	public abstract void cellsAction(CGScope scope, long stackOffset, CGCell[] cells, Operator op) throws CompileException;
	public abstract void constAction(CGScope scope, Operator op, long k) throws CompileException;
	public abstract void loadRegsConst(CGScope scope, byte[] regs, long value);
	public abstract void constLoadRegs(String label, byte[] registers);
	public abstract	void pushAcc(CGScope scope, int size);
	public abstract	void popAcc(CGScope scope, int size);
	public abstract	void popRet(CGScope scope, int size);
	//public abstract void pushRef(CGScope scope, String label);
	public abstract void refCountInc(CGScope scope, long stackOffset, CGCell[] cells) throws CompileException;
	public abstract void refCountDec(CGScope scope, long stackOffset, CGCell[] cells) throws CompileException;

	
	public abstract void invokeMethod(CGScope scope, String className, String methodName, VarType type, VarType[] types, CGMethodScope mScope)
																																	throws CompileException;
	public abstract void invokeNative(CGScope scope, String className, String methodName, String params, VarType type, Operand[] operands)
																																	throws CompileException;
	public abstract boolean emitInstanceof(long op, int typeId);
	public abstract void emitUnary(Operator op, Integer resId) throws CompileException;
	
	//TODO набор методов для реализации команд if, switch, for, loop и .т.д
	public abstract void eNew(CGScope scope, int size, List<VarType> classTypes, boolean canThrow) throws CompileException;
	public abstract void eFree(Operand op);
	public abstract void eIf(CGBlockScope conditionBlock, CGBlockScope thenBlock, CGBlockScope elseBlock);
	public abstract void eTry(CGBlockScope blockScope, List<Case> cases, CGBlockScope defaultBlockScope);
	public abstract void eWhile(CGScope scope, CGScope condScope, CGBlockScope bodyScope) throws CompileException;
	public abstract void eReturn(CGScope scope, int size);
	public abstract void eThrow();
	
	public abstract int getRefSize();
	public abstract ArrayList<RegPair> buildRegsPool();
	public abstract CGItem pushRegAsm(int reg);
	public abstract CGItem popRegAsm(int reg);
	public abstract CGCell[] getRetCells(int size);
	public abstract void pushConst(CGScope scope, int size, long value);
	public abstract void pushCells(CGScope scope, int size, CGCell[] cells);
	public abstract void popCells(CGScope scope, int size, CGCell[] cells);
//	public abstract void setValueByIndex(Operand op, int size, List<Byte> tempRegs) throws CompileException;
	public abstract CGItem stackAllocAsm(int size);
	public abstract CGItem stackFreeAsm();
	public abstract void stackFree(CGScope scope, int size);
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
	
	public void build() throws CompileException {
		append(new CGIText("; vm5277." + platform + " v" + getVersion() + " at " + new Date().toString()));
		
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
		if(dpStackAlloc) {
			append(new CGIText(".include \"sys/stack_alloc.asm\""));
			append(new CGIText(".include \"sys/stack_free.asm\""));	
		}
		if(RTOSFeatures.contains(RTOSFeature.OS_FT_DRAM)) {
			append(new CGIText(".include \"dmem/dram.asm\""));	
		}
		for(String include : includes) {
			append(new CGIText(".include \"" + include + "\""));
		}
		append(new CGIText(""));
		append(new CGIText("MAIN:"));
		
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
	
	public void setDpStackAlloc() {
		dpStackAlloc = true;
	}

	public CGScope getScope() {
		return scope;
	}
	public CGScope setScope(CGScope scope) {
		CGScope old = this.scope;
		this.scope = scope;
		return old;
	}
	
	public static String cellsToStr(CGCell[] cells) {
		StringBuilder sb = new StringBuilder("[");
		for(int i=0; i<cells.length; i++) {
			sb.append(cells[i].toString());
			if(i != cells.length-1) {
				sb.append(",");
			}
		}
		sb.append("]");
		return sb.toString();
	}
	
	public void addLabel(CGLabelScope lbScope) {
		labels.put(lbScope.getResId(), lbScope);
	}
}