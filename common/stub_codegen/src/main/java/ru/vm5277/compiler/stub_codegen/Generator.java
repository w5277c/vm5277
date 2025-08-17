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

package ru.vm5277.compiler.stub_codegen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import ru.vm5277.common.NativeBinding;
import ru.vm5277.common.Operator;
import ru.vm5277.common.SystemParam;
import ru.vm5277.common.cg.CGCells;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.Operand;
import ru.vm5277.common.cg.RegPair;
import ru.vm5277.common.cg.items.CGIContainer;
import ru.vm5277.common.cg.items.CGItem;
import ru.vm5277.common.cg.scopes.CGBlockScope;
import ru.vm5277.common.cg.scopes.CGCellsScope;
import ru.vm5277.common.cg.scopes.CGMethodScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.compiler.Case;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;

public class Generator extends CodeGenerator {
	private	final	static	String				VERSION		= "0.1";

	public Generator(String platform, Map<String, NativeBinding> nbMap, Map<SystemParam, Object> params) {
		super(platform, nbMap, params);
	}
	
	@Override public void accCast(CGScope scope, VarType type) throws CompileException {}
	@Override public void cellsToAcc(CGScope scope, CGCellsScope cScope) throws CompileException {}
	@Override public void accToCells(CGScope scope, CGCellsScope cScope) throws CompileException {}
	@Override public void setHeapReg(CGScope scope, int offset, CGCells cells) throws CompileException {}
	@Override public void constToAcc(CGScope scope, int size, long value) {}
	@Override public void constToCells(CGScope scope, int offset, long value, CGCells cells) throws CompileException {}
	@Override public void cellsAction(CGScope scope, int offset, CGCells cells, Operator op) throws CompileException {}
	@Override public void constAction(CGScope scope, Operator op, long k) throws CompileException {}
	@Override public void pushAccBE(CGScope scope, int size) {}
	@Override public void popAccBE(CGScope scope, int size) {}
	@Override public int pushHeapReg(CGScope scope) {return 1;}
	@Override public void popHeapReg(CGScope scope) {}
	@Override public int pushStackReg(CGScope scope) {return 1;}
	@Override public void popStackReg(CGScope scope) {}
	@Override public void stackToStackReg(CGScope scope) {}
	@Override public void updateRefCount(CGScope scope, int offset, CGCells cells, boolean isInc) throws CompileException {}
	@Override public void invokeMethod(CGScope scope, String className, String methodName, VarType type, VarType[] types, CGMethodScope mScope) throws CompileException {}
	@Override public void invokeNative(CGScope scope, String className, String methodName, String params, VarType type, Operand[] operands) throws CompileException {}
	@Override public boolean emitInstanceof(long op, int typeId) {return false;}
	@Override public void emitUnary(CGScope scope, Operator op, int offset, CGCells cells) throws CompileException {}
	@Override public CGIContainer eNew(CGScope scope, int size, List<VarType> classTypes, boolean launcPoint, boolean canThrow) throws CompileException {return null;}
	@Override public void eFree(Operand op) {}
	@Override public void eIf(CGBlockScope conditionBlock, CGBlockScope thenBlock, CGBlockScope elseBlock) {}
	@Override public void eTry(CGBlockScope blockScope, List<Case> cases, CGBlockScope defaultBlockScope) {}
	@Override public void eWhile(CGScope scope, CGScope condScope, CGBlockScope bodyScope) throws CompileException {}
	@Override public void eReturn(CGScope scope, int size) {}
	@Override public void eThrow() {}
	@Override public int getRefSize() {return 1;}
	@Override public int getCallSize() {return 1;}
	@Override public ArrayList<RegPair> buildRegsPool() {return null;}
	@Override public void pushConst(CGScope scope, int size, long value) {}
	@Override public void pushCells(CGScope scope, int offset, int size, CGCells cells) throws CompileException {}
	@Override public CGItem stackAlloc(CGScope scope, int size, boolean modifyIreg) {return null;}
	@Override public CGItem stackFree(CGScope scope, int size, boolean modifyIreg) {return null;}
	@Override public String getVersion() {return VERSION;}
}
