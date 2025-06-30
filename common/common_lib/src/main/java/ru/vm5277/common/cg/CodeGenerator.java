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
import ru.vm5277.common.cg.scopes.CGLocalScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.cg.scopes.CGBlockScope;
import ru.vm5277.common.cg.scopes.CGMethodScope;
import ru.vm5277.common.cg.items.CGItem;
import ru.vm5277.common.cg.items.CGIText;
import java.util.Arrays;
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
import ru.vm5277.common.cg.items.CGIContainer;
import ru.vm5277.common.cg.scopes.CGLabelScope;
import ru.vm5277.common.compiler.Case;
import ru.vm5277.common.compiler.Operand;
import static ru.vm5277.common.compiler.OperandType.LITERAL;
import ru.vm5277.common.compiler.VarType;

public abstract class CodeGenerator extends CGScope {
	protected	final	String						platform;
	protected	final	Set<RTOSFeature>			RTOSFeatures		= new HashSet<>();
	protected	final	Set<String>					includes			= new HashSet<>();
	protected	final	Map<String, NativeBinding>	nbMap;
	protected	final	Map<Integer, CGLabelScope>	labels				= new HashMap<>();
	protected	final	Map<SystemParam, Object>	params;
	protected	final	Map<Integer, DataSymbol>	flashData			= new HashMap<>();
	protected			CGScope						scope				= null;
	protected	final	CGAccum						accum				= new CGAccum();
	protected			boolean						dpStackAlloc		= false; // зависимость от процедур выделения и освобождения памяти в стеке

	public CodeGenerator(String platform, Map<String, NativeBinding> nbMap, Map<SystemParam, Object> params) {
		this.platform = platform;
		this.nbMap = nbMap;
		this.params = params;
	}
	
	public int enterClass(int typeId, int[] intrerfaceIds, String name) {
		int _resId = genId();
		System.out.println("CG:enterClass, typeId:" + typeId + ", interfaces id:" + Arrays.toString(intrerfaceIds));
		scope = new CGClassScope(scope, _resId, typeId, intrerfaceIds, name);
		return _resId;
	}
	public void leaveClass() {
		if(null != scope.getParent()) scope = scope.free(); // оставялем верхний класс в зоне видимости
		
		//TODO
		System.out.println("CG:LeaveClass");
	}

	public int enterInterface(int typeId, int[] intrerfaceIds, String name) {
		int _resId = genId();
		System.out.println("CG:enterInterface, typeId:" + typeId + ", interfaces id:" + Arrays.toString(intrerfaceIds));
		scope = new CGInterfaceScope(scope, _resId, typeId, intrerfaceIds, name);
		return _resId;
	}
	public void leaveInterface() {
		scope = scope.free();
		//TODO
		System.out.println("CG:LeaveInterface");
	}

	public int enterConstructor(int[] typeIds) {
		int _resId = genId();
		System.out.println("CG:enterConstructor, id:" + _resId + ", parameters:" + Arrays.toString(typeIds));
		scope = new CGMethodScope(scope, null, _resId, VarType.NULL.getId(), typeIds, "contr", buildRegsPool());
		return _resId;
	}
	public void leaveConstructor() {
		scope = scope.free();
		//TODO
		System.out.println("CG:LeaveConstructor");
	}

	public int enterMethod(int typeId, int[] typeIds, String name) {
		int _resId = genId();
		System.out.println("CG:enterMethod, id:" + _resId + ", typeId:" + typeId + ", parameters:" + Arrays.toString(typeIds));
		CGLabelScope lbScope = makeLabel(scope.getPath());
		scope = new CGMethodScope(scope, lbScope, _resId, typeId, typeIds, name, buildRegsPool());
		return _resId;
	}
	public void leaveMethod() {
		CGMethodScope mScope = (CGMethodScope)scope;
		
		if(!mScope.getUsedRegs().isEmpty()) {
			mScope.begin();
			for(int i=0; i<mScope.getUsedRegs().size(); i++) {
				mScope.insert(pushRegAsm(mScope.getUsedRegs().get(i)));
			}
		}
		
		int size = mScope.getStackBlockSize();
		if(0x00 != size) {
			dpStackAlloc = true;
			mScope.insert(stackAllocAsm(size));
		}
		
		//TODO инициализируем все переменные
		

		if(!mScope.getUsedRegs().isEmpty()) {
			for(int i=mScope.getUsedRegs().size()-1; i>=0; i--) {
				mScope.append(popRegAsm(mScope.getUsedRegs().get(i)));
			}
		}
		
		if(0x00 != size) mScope.append(stackFreeAsm());
		scope = scope.free();
		
		System.out.println("CG:LeaveMethod");
	}

	public int enterBlock() {
		int _resId = genId();
		System.out.println("CG:enterBlock, id:" + _resId);
		scope = new CGBlockScope(scope, _resId, "");		
		return _resId;
	}
	public void leaveBlock() {
		CGBlockScope bScope = (CGBlockScope)scope;

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

		if(0x00 != size) bScope.append(stackFreeAsm());
		scope = scope.free();

		
		System.out.println("CG:LeaveBlock");
	}

	
	public int enterLocal(int typeId, int size, boolean isConstant, String name) throws Exception { //TODO сделать индексацию вместо имен
		int _resId = genId();
		
		if(-1 == size) size = getRefSize(); // ссылка, записываем реальный размер для текущего чипа

		System.out.println("CG:enterLocal, id:" + _resId + ", size:" + size + ", typeId:" + typeId + ", name:" + name);
		
		
		CGScope parentScope = scope;
		// проверка на константу, нет необходимости выделять память
		if(parentScope instanceof CGMethodScope) {
			CGMethodScope mScope = (CGMethodScope)parentScope;
			CGCell[] cells = (isConstant ? null : mScope.memAllocate(size));
			scope = new CGLocalScope(scope, _resId, typeId, size, isConstant, name, cells);
			mScope.addLocal((CGLocalScope)scope);
		}
		else if(parentScope instanceof CGBlockScope) {
			CGBlockScope bScope = (CGBlockScope)parentScope;
			CGCell[] cells = (isConstant ? null : bScope.memAllocate(size));
			scope = new CGLocalScope(scope, _resId, typeId, size, isConstant, name, cells);
			bScope.addLocal((CGLocalScope)scope);
		}
		else {
			System.err.println("Try to put local to " + scope);
		}

		return _resId;
	}
	public void leaveLocal() throws Exception {
		CGLocalScope lScope = (CGLocalScope)scope;
		if(lScope.isConstant()) {
			lScope.setDataSymbol(flashData.get(lScope.getResId()));
		}
		else {
//			lScope.setValue(accum.getValue());
//			loadRegs(lScope.getSize());
//			storeAcc(lScope.getResId());
		}
		
		scope = scope.free();
		//TODO
		System.out.println("CG:LeaveLocal");
	}
	public abstract void localStore(int resId, long value) throws Exception;
	
	public int enterFiled(int typeId, int size, boolean isConstant, String name) {
		int _resId = genId();

		if(-1 == size) size = getRefSize(); // ссылка, записываем реальный размер для текущего чипа
		
		System.out.println("CG:enterField, id:" + _resId + ", size:" + size + ", typeId:" + typeId + ", name:" + name);
		CGScope parentScope = scope;
		scope = new CGLocalScope(scope, _resId, typeId, size, isConstant, name, null);
		
		if(!isConstant) {
			if(parentScope instanceof CGClassScope) {
				((CGClassScope)parentScope).addField((CGLocalScope)scope);
			}
			else {
				System.err.println("Try to put field to " + scope);
			}
		}

		return _resId;
	}
	public void leaveField() {
		scope = scope.free();
		//TODO
		System.out.println("CG:LeaveField");
	}
	
	
	public int enterExpression() throws Exception {
		int _resId = genId();
		if(isExpressionScope()) {
			((CGExpressionScope)scope).enter();
		}
		else {
			System.out.println("CG:enterExpression, id:" + _resId);

			accum.set(0x01, 0x00, _resId); // Сброс аккумулятора

			scope = new CGExpressionScope(scope, _resId);
		}
		return _resId;
	}
	
	public void leaveExpression() {
		if(0==((CGExpressionScope)scope).leave()) {
			scope = scope.free();
			//TODO
			System.out.println("CG:LeaveExpression");
		}
	}

	public void setAcc(int size, long value) throws Exception {
		System.out.println("CG:setAcc, op:" + value);
		accum.set(size, value);
//		VarType vt = value.getVarType();
		//Long value = ((Number)value.getValue()).longValue();
//		if(VarType.BYTE == vt || VarType.SHORT == vt || VarType.INT == vt || VarType.CLASS==vt) {
//			if(source.getValue() instanceof Number) {
//				value = ((Number)source.getValue()).longValue();
//			}
//			else if(source.getValue() instanceof Character) {
//				value = (long)(Character)source.getValue();
//			}
//		}

/*ЭТО КОД ДЛЯ loadAcc!!!		if(null != value) {
			scope.asmAppend("ldi r16," + (value&0xff)); value >>= 0x08;
			if(vt.getSize()>1) scope.asmAppend("ldi r17," + (value&0xff)); value >>= 0x08;
			if(vt.getSize()>2) scope.asmAppend("ldi r18," + (value&0xff)); value >>= 0x08;
			if(vt.getSize()>3) scope.asmAppend("ldi r19," + (value&0xff)); value >>= 0x08;
		}
		else {
			System.out.println("Unexpected value for set accum:" + source.toString());
		}*/
	}

	public long getAcc() {
		System.out.println("CG:getAcc, acc:" + accum);
		return accum.getValue();
	}
	public abstract void loadAcc(int resId) throws Exception;
	public abstract void storeAcc(int resId) throws Exception;
		
	public abstract void emitUnary(Operator op, Integer resId) throws Exception ;
	
	public void defineStr(int resId, String text) {
		flashData.put(resId, new DataSymbol(resId, "javl_" + scope.getParent().getPath()+"_r"+resId, -1, text));
	}
	public void defineData(int resId, int size, long value) {
		flashData.put(resId, new DataSymbol(resId, "javl_" + scope.getParent().getPath()+"_r"+resId, size, value));
	}

	protected CGLabelScope makeLabel(String name) {
		int _resId = genId();
		String fullname = "javl_" + scope.getPath() + "_l" + _resId + (null != name ? name : "");
		CGLabelScope lbScope = new CGLabelScope(scope, _resId, fullname);
		labels.put(_resId, lbScope);
		return lbScope;

	}

	public abstract void invokeMethod(String methodQName, int id, int typeId, Operand[] operands);
	public abstract void invokeNative(String methodQName, int typeId, Operand[] operands) throws Exception; 
	public abstract boolean emitInstanceof(long op, int typeId);
	
	//TODO набор методов для реализации команд if, switch, for, loop и .т.д
	public abstract void eNew(int typeId, long[] parameters, boolean canThrow);
	public abstract void eFree(Operand op);
	
	public abstract void eIf(int conditionBlockId, int thenBlockId, Integer elseBlockId);
	public abstract void eTry(int blockId, List<Case> cases, Integer defaultBlockId);
	public abstract void eWhile(Integer condResId, int bodyResId) throws Exception;
	public abstract void eReturn();
	public abstract void eThrow();
	
	public abstract int getRefSize();
	public abstract void loadRegs(int size);
	
	public abstract List<Byte> buildRegsPool();
	public abstract CGItem pushRegAsm(int reg);
	public abstract CGItem popRegAsm(int reg);
//	public abstract void setValueByIndex(Operand op, int size, List<Byte> tempRegs) throws Exception;
	public abstract CGItem stackAllocAsm(int size);
	public abstract CGItem stackFreeAsm();
	public abstract String getVersion();

	public abstract void localAction(Operator op, int resId) throws Exception;
	public abstract void constAction(Operator op, long k) throws Exception;
	public abstract void localLoadRegs(CGLocalScope lScope, byte[] registers) throws Exception;
	public abstract void localStoreRegs(int stackOffset, int size);
	public abstract void loadRegsConst(byte[] regs, long value);
	public abstract void constLoadRegs(String label, byte[] registers);
	public abstract void addAccum(int value);
	public abstract void subAccum(int value);
	
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
			else throw new Exception("CG: postBuild, unsupported value:" + ds.getValue());
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

	protected CGLocalScope getLocal(int resId) throws Exception {
		if(scope instanceof CGLocalScope && scope.getResId() == resId) return (CGLocalScope)scope;

		CGBlockScope bScope = scope.getBlockScope();
		if(null != bScope) {
			return bScope.getLocal(resId);
		}
		else throw new Exception("CG: getNum, can't access to method scope, for resId:" + resId);
	}
	
	public String getAsm() {
		return scope.build();
	}
	
	public boolean isExpressionScope() {
		return (scope instanceof CGExpressionScope);
	}
	
	protected boolean isEven(int reg) {
		return 0 == (reg&0x01);
	}
}