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
package ru.vm5277.common.compiler;

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
import ru.vm5277.common.cg_scope.CGBlockScope;
import ru.vm5277.common.cg_scope.CGClassScope;
import ru.vm5277.common.cg_scope.CGInterfaceScope;
import ru.vm5277.common.cg_scope.CGLocalScope;
import ru.vm5277.common.cg_scope.CGMethodScope;
import ru.vm5277.common.cg_scope.CGScope;

public abstract class CodeGenerator {
//TODO модель пока не использует регистры МК для оптимизации(использует выделение памяти в стеке)

	private	int	idCntr	= 0;
	
	protected	final	String						genName;
	protected	final	Set<RTOSFeature>			RTOSFeatures		= new HashSet<>();
	protected	final	Set<String>					includes			= new HashSet<>();
	protected	final	Map<String, NativeBinding>	nbMap;
	protected	final	Map<SystemParam, Object>	params;
	protected	final	Map<Integer, DataSymbol>	flashData			= new HashMap<>();
	protected			CGScope						scope				= null;
	protected			Operand						acc;		//TODO похоже будет не нужен
	protected			boolean						dp_stack_alloc		= false; // зависимость от процедур выделения и освобождения памяти в стеке

	public CodeGenerator(String genName, Map<String, NativeBinding> nbMap, Map<SystemParam, Object> params) {
		this.genName = genName;
		this.nbMap = nbMap;
		this.params = params;
	}
	
	public int genId() {
		return idCntr++;
	}

	public int enterClass(int typeId, int[] intrerfaceIds, String name) {
		int id = genId();
		System.out.println("CG:enterClass, typeId:" + typeId + ", interfaces id:" + Arrays.toString(intrerfaceIds));
		scope = new CGClassScope(scope, id, typeId, intrerfaceIds, name);
		return id;
	}
	public void leaveClass() {
		scope.asmPrepend(";======== class: " + scope.getName() + " ========\n");

		if(null != scope.getParent()) scope = scope.free(); // оставялем верхний класс в зоне видимости
		
		//TODO
		System.out.println("CG:LeaveClass");
		scope.asmAppend(";==== end class ====\n");
	}

	public int enterInterface(int typeId, int[] intrerfaceIds, String name) {
		int id = genId();
		System.out.println("CG:enterInterface, typeId:" + typeId + ", interfaces id:" + Arrays.toString(intrerfaceIds));
		scope = new CGInterfaceScope(scope, id, typeId, intrerfaceIds, name);
		return id;
	}
	public void leaveInterface() {
		scope = scope.free();
		//TODO
		System.out.println("CG:LeaveInterface");
	}

	public int enterConstructor(int[] typeIds) {
		int id = genId();
		System.out.println("CG:enterConstructor, id:" + id + ", parameters:" + Arrays.toString(typeIds));
		scope = new CGMethodScope(scope, id, VarType.NULL.getId(), typeIds, "contr");
		return id;
	}
	public void leaveConstructor() {
		scope = scope.free();
		//TODO
		System.out.println("CG:LeaveConstructor");
		scope.asmAppend(";==== end constructor ====\n");
	}

	public int enterMethod(int typeId, int[] typeIds, String name) {
		int id = genId();
		System.out.println("CG:enterMethod, id:" + id + ", typeId:" + typeId + ", parameters:" + Arrays.toString(typeIds));
		scope = new CGMethodScope(scope, id, typeId, typeIds, name);
		//asmSource.append("markStack\n");
		return id;
	}
	public void leaveMethod() {
		CGMethodScope mScope = (CGMethodScope)scope;
		
		mScope.asmPrepend(";======== method: " + mScope.getName() + " ========\n");
		
		if(!mScope.getUsedRegs().isEmpty()) {
			for(int i=0; i<mScope.getUsedRegs().size(); i++) {
				mScope.asmInsert(pushRegAsm(mScope.getUsedRegs().get(i)) + "\n");
			}
			mScope.asmInsert("\n");
		}
		
		int size = mScope.getStackSize();
		if(0x00 != size) {
			dp_stack_alloc = true;
			mScope.asmInsert(stackAllocAsm(size));
		}
		
		//TODO инициализируем все переменные
		

		if(!mScope.getUsedRegs().isEmpty()) {
			mScope.asmAppend("\n");
			for(int i=mScope.getUsedRegs().size()-1; i>=0; i--) {
				mScope.asmAppend(popRegAsm(mScope.getUsedRegs().get(i)) + "\n");
			}
		}
		
		if(0x00 != size) mScope.asmAppend(stackFreeAsm());
		scope = scope.free();
		
		System.out.println("CG:LeaveMethod");
		scope.asmAppend(";==== end method ====\n");
	}

	public int enterBlock() {
		int id = genId();
		System.out.println("CG:enterBlock, id:" + id);
		scope = new CGBlockScope(scope, id);		
		return id;
	}
	public void leaveBlock() {
		scope = scope.free();
		//TODO
		System.out.println("CG:LeaveBlock");
	}

	
	public int enterLocal(int typeId, int size, boolean isConstant, String name) { //TODO сделать индексацию вместо имен
		int id = genId();
		
		if(-1 == size) size = getRefSize(); // ссылка, записываем реальный размер для текущего чипа

		System.out.println("CG:enterLocal, id:" + id + ", size:" + size + ", typeId:" + typeId + ", name:" + name);
		
		
		CGScope parentScope = scope;
		// проверка на константу, нет необходимости выделять память
		scope = new CGLocalScope(scope, id, typeId, size, isConstant, name);
		if(!isConstant) {
			if(parentScope instanceof CGMethodScope) {
				((CGMethodScope)parentScope).addLocal((CGLocalScope)scope);
			}
			else if(parentScope instanceof CGBlockScope) {
				((CGBlockScope)parentScope).addLocal((CGLocalScope)scope);
			}
			else {
				System.err.println("Try to put local to " + scope);
			}
		}

		return id;
	}
	public void leaveLocal() {
		((CGLocalScope)scope).setValue(acc);
		scope = scope.free();
		//TODO
		System.out.println("CG:LeaveLocal");
	}

	public int enterFiled(int typeId, int size, boolean isConstant, String name) {
		int id = genId();

		if(-1 == size) size = getRefSize(); // ссылка, записываем реальный размер для текущего чипа
		
		System.out.println("CG:enterField, id:" + id + ", size:" + size + ", typeId:" + typeId + ", name:" + name);
		CGScope parentScope = scope;
		scope = new CGLocalScope(scope, id, typeId, size, isConstant, name);
		if(!isConstant) {
			if(parentScope instanceof CGClassScope) {
				((CGClassScope)parentScope).addField((CGLocalScope)scope);
			}
			else {
				System.err.println("Try to put field to " + scope);
			}
		}

		return id;
	}
	public void leaveField() {
		scope = scope.free();
		//TODO
		System.out.println("CG:LeaveField");
	}

	public int defineData(Operand constant) { // формируем блок данных константы во FLASH
		int resId = genId();
		flashData.put(resId, new DataSymbol(resId, scope.getParent().getPath()+"_resid_"+resId, constant));
		return resId;
	}
	
	public void setAcc(Operand source) {
		System.out.println("CG:setAcc, op:" + source);
		acc = source;
	}
	public Operand getAcc() {
		System.out.println("CG:getAcc, acc:" + acc);
		return acc;
	}
	public void loadAcc(int id) { //Загружаем значение переменной в acc
		System.out.println("CG:loadAcc, srcId:" + id);
	}
	public void storeAcc(int id) { //Записываем acc в переменную
		System.out.println("CG:storeAcc, dstId:" + id);
		
	}
	
	public abstract void invokeMethod(int id, int typeId, Operand[] args);
	public abstract void invokeNative(String methodQName, int typeId, Operand[] parameters) throws Exception; 
	public abstract Operand emitInstanceof(Operand op, int typeId);	//todo может быть поросто boolean?
	public abstract void emitUnary(Operator op); //PLUS, MINUS, BIT_NOT, NOT, PRE_INC, PRE_DEC, POST_INC, POST_DEC
	
	//TODO набор методов для реализации команд if, switch, for, loop и .т.д
	public abstract void eNew(int typeId, Operand[] parameters, boolean canThrow);
	public abstract void eFree(Operand op);
	
	public abstract void eIf(int conditionBlockId, int thenBlockId, Integer elseBlockId);
	public abstract void eTry(int blockId, List<Case> cases, Integer defaultBlockId);
	public abstract void eWhile(int conditionBlockId, int bodyBlockId);
	public abstract void eReturn();
	public abstract void eThrow();
	
	public abstract int getRefSize();
	public abstract String pushRegAsm(int reg);
	public abstract String popRegAsm(int reg);
	public abstract String stackAllocAsm(int size);
	public abstract String stackFreeAsm();
	public abstract String getVersion();

	public void postBuild() {
		StringBuilder asmHeaders = new StringBuilder();
		asmHeaders.append("; vm5277.").append(genName).append(" v").append(getVersion()).append(" at ").append(new Date().toString()).append("\n");
		asmHeaders.append("\n");
		if(params.keySet().contains(SystemParam.CORE_FREQ)) {
			asmHeaders.append(".equ core_freq = ").append(params.get(SystemParam.CORE_FREQ)).append("\n");
		}
		if(params.keySet().contains(SystemParam.STDOUT_PORT)) {
			asmHeaders.append(".equ stdout_port = ").append(params.get(SystemParam.STDOUT_PORT)).append("\n");

		}
		asmHeaders.append("\n");
		
		for(RTOSFeature feature : RTOSFeatures) {
			asmHeaders.append(".set ").append(feature).append(" = 1\n");
		}
		asmHeaders.append("\n");
		
		asmHeaders.append(".include \"devices/").append(params.get(SystemParam.MCU)).append(".def").append("\"\n");
		asmHeaders.append(".include \"core/core.asm\"\n");		

		if(dp_stack_alloc) {
			asmHeaders.append(".include \"sys/stack_alloc.asm\"\n");
			asmHeaders.append(".include \"sys/stack_free.asm\"\n");	
		}

		for(String include : includes) {
			asmHeaders.append(".include \"").append(include).append("\"\n");
		}
		asmHeaders.append("\n");

		for(int resId : flashData.keySet()) {
			DataSymbol ds = flashData.get(resId);
			asmHeaders.append(ds.getLabel()).append(":\n");
			String value = ds.getOperand().getValue().toString();
			asmHeaders.append(".db \"").append(value).append("\",0x00");
			if(0x00==(value.length()&0x01)) asmHeaders.append(",0x00"); 
			asmHeaders.append("\n");
		}
		
		asmHeaders.append("\n");
		asmHeaders.append("main:\n");
		scope.asmPrepend(asmHeaders.toString());
	}
	
	public String getAsm() {
		return scope.getAsm();
	}
}