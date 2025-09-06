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
import ru.vm5277.common.cg.items.CGIAsm;
import ru.vm5277.common.cg.items.CGIContainer;
import ru.vm5277.common.cg.scopes.CGBlockScope;
import ru.vm5277.common.cg.scopes.CGBranchScope;
import ru.vm5277.common.cg.scopes.CGCellsScope;
import ru.vm5277.common.cg.scopes.CGLabelScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.compiler.Case;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;

public class Generator extends CodeGenerator {
	private	final	static	String				VERSION		= "0.1";

	public Generator(String platform, Map<String, NativeBinding> nbMap, Map<SystemParam, Object> params) {
		super(platform, nbMap, params);
	}
	
	@Override public CGIContainer accCast(CGScope scope, int size) throws CompileException {return null;}
	@Override public void cellsToAcc(CGScope scope, CGCellsScope cScope) throws CompileException {}
	@Override public void accToCells(CGScope scope, CGCellsScope cScope) throws CompileException {}
	@Override public void setHeapReg(CGScope scope, int offset, CGCells cells) throws CompileException {}
	@Override public void constToAcc(CGScope scope, int size, long value) {}
	@Override public void constToCells(CGScope scope, int offset, long value, CGCells cells) throws CompileException {}
	@Override public void cellsAction(CGScope scope, int offset, CGCells cells, Operator op) throws CompileException {}
	@Override public void constAction(CGScope scope, Operator op, long k) throws CompileException {}
	@Override public void pushAccBE(CGScope scope, int size) {}
	@Override public void popAccBE(CGScope scope, int size) {}
	@Override public CGIContainer pushHeapReg(CGScope scope, boolean half) {return null;}
	@Override public CGIContainer popHeapReg(CGScope scope, boolean half) {return null;}
	@Override public CGIContainer pushStackReg(CGScope scope) {return null;}
	@Override public CGIContainer popStackReg(CGScope scope) {return null;}
	@Override public CGIContainer stackToStackReg(CGScope scope) {return null;}
	@Override public void updateRefCount(CGScope scope, int offset, CGCells cells, boolean isInc) throws CompileException {}
	@Override public void invokeClassMethod(CGScope scope, String className, String methodName, VarType type, VarType[] types, CGLabelScope lbScope, boolean isInternal) throws CompileException {}
	@Override public void invokeInterfaceMethod(CGScope scope, String className, String methodName, VarType type, VarType[] types, VarType ifaceType, int methodSN) throws CompileException {}
	@Override public void invokeNative(CGScope scope, String className, String methodName, String params, VarType type, Operand[] operands) throws CompileException {}
	@Override public void eInstanceof(CGScope scope, VarType type) throws CompileException {}
	@Override public void emitUnary(CGScope scope, Operator op, int offset, CGCells cells) throws CompileException {}
	@Override public CGIContainer eNewInstance(int size, CGLabelScope iidLabel, VarType type, boolean launchPoint, boolean canThrow) throws CompileException {return null;}
	@Override public void eFree(Operand op) {}
	@Override public void eIf(CGBranchScope branchScope, CGScope condScope, CGBlockScope thenScope, CGBlockScope elseScope) throws CompileException {}
	@Override public void eTry(CGBlockScope blockScope, List<Case> cases, CGBlockScope defaultBlockScope) {}
	@Override public void eWhile(CGScope scope, CGScope condScope, CGBlockScope bodyScope) throws CompileException {}
	@Override public CGIContainer eReturn(CGScope scope, int argsSize, int varsSize, int retSize) throws CompileException {return null;}
	@Override public void eThrow() {}
	@Override public int getRefSize() {return 1;}
	@Override public int getCallSize() {return 1;}
	@Override public ArrayList<RegPair> buildRegsPool() {return null;}
	@Override public void pushConst(CGScope scope, int size, long value) {}
	@Override public void pushCells(CGScope scope, int offset, int size, CGCells cells) throws CompileException {}
	@Override public CGIContainer stackAlloc(CGScope scope, int size) {return null;}
	@Override public CGIContainer blockFree(CGScope scope, int size) {return null;}
	@Override public String getVersion() {return VERSION;}
	@Override public CGIContainer jump(CGScope scope, CGLabelScope lScope) throws CompileException {return null;}
	@Override public void pushLabel(CGScope scope, String label) {}
	@Override public void constCond(CGScope scope, int offset, CGCells cells, Operator op, long k, boolean isNot, boolean isOr, CGBranchScope condScope) throws CompileException {}
	@Override public CGIAsm pushRegAsm(byte reg) {return null;}
	@Override public CGIAsm popRegAsm(byte reg) {return null;}
	@Override public CGIContainer moveIReg(CGScope scope, char iReg, int offset) {return null;}
}
