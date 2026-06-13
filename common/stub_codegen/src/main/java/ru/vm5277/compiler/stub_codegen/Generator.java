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
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import ru.vm5277.common.Device;
import ru.vm5277.common.ExcsThrowPoint;
import ru.vm5277.common.NativeBinding;
import ru.vm5277.common.lexer.Operator;
import ru.vm5277.common.Pair;
import ru.vm5277.common.StrUtils;
import ru.vm5277.common.cg.CGArrCells;
import ru.vm5277.common.cg.CGCells;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.DataSymbol;
import ru.vm5277.common.cg.RegPair;
import ru.vm5277.common.cg.items.CGIContainer;
import ru.vm5277.common.cg.scopes.CGBlockScope;
import ru.vm5277.common.cg.CGBranch;
import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.common.cg.scopes.CGCellsScope;
import ru.vm5277.common.cg.scopes.CGLabelScope;
import ru.vm5277.common.cg.scopes.CGMethodScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.compiler.Case;
import ru.vm5277.common.VarType;
import ru.vm5277.common.cg.items.CGIAsm;
import ru.vm5277.common.enums.InstanceType;
import ru.vm5277.common.exceptions.CompileException;

public class Generator extends CodeGenerator {
	private	final	static	String				VERSION		= StrUtils.readVersion(Generator.class);

	public Generator(Device device) {
		super(device);
	}
	
	@Override public void cellsToAcc(CGScope scope, CGCellsScope cScope) throws CompileException {}
	@Override public void accToCells(CGScope scope, VarType varType, CGCellsScope cScope) throws CompileException {}
	@Override public void setHeapReg(CGScope scope, CGCells cells) throws CompileException {}
	@Override public void constToAcc(CGScope scope, int size, long value, boolean isFixed) {}
	@Override public CGIContainer constToCells(CGScope scope, long value, CGCells cells, boolean isFixed) throws CompileException {return null;}
	@Override public void cellsAction(CGScope scope, CGCells leftcells, Operator op, CGCells rightCells, boolean leftisFixed, boolean rightIsFixed, CGExcs excs) throws CompileException {}
	@Override public void constAction(CGScope scope, CGCells cells, Operator op, long k, boolean isFixed, CGExcs excs) throws CompileException {}
	@Override public CGIContainer pushAccBE(CGScope scope, int size) throws CompileException {return null;} 
	@Override public CGIContainer pushAccLE(CGScope scope, int size) throws CompileException {return null;}
	@Override public void popAccBE(CGScope scope, int size) throws CompileException {}
	@Override public CGIContainer pushHeapReg(CGScope scope) {return null;}
	@Override public CGIContainer popHeapReg(CGScope scope) {return null;}
	@Override public CGIContainer pushStackReg(CGScope scope) {return null;}
	@Override public CGIContainer popStackReg(CGScope scope) {return null;}
	@Override public void updateClassRefCount(CGScope scope, CGCells cells, boolean isInc) throws CompileException {}
	@Override public void invokeInterfaceMethod(CGScope scope, String className, String methodName, VarType type, VarType[] types, VarType ifaceType, int methodSN) throws CompileException {}
	@Override public void eInstanceof(CGScope scope, VarType type) throws CompileException {}
	@Override public void eUnary(CGScope scope, Operator opt, CGCells cells, boolean toAccum, CGExcs excs) throws CompileException {}
	@Override public CGIContainer eNewInstance(CGScope scope, int fieldsSize, CGLabelScope iidLabel, VarType type, InstanceType instType, CGIContainer postfixCont, CGExcs excs) throws CompileException {return null;}
	@Override public void eNewArray(CGScope scope, VarType type, int depth, int[] cDims, CGExcs excs) throws CompileException {};
	@Override public void eNewArrView(CGScope scope, int depth, CGExcs excs) throws CompileException {};
	@Override public void eTry(CGBlockScope blockScope, List<Case> cases, CGBlockScope defaultBlockScope) {}
	@Override public void eCatch(CGScope scope) throws CompileException {};
	@Override public CGIContainer eReturn(CGScope scope, VarType retType, CGLabelScope lScope) throws CompileException {return null;}
	@Override public void eThrow(CGScope cgs, int exceptioId, boolean isMethodLeave, CGLabelScope lbScope, boolean withCode, ExcsThrowPoint point) throws CompileException {}
	@Override public void throwCheck(CGScope scope, List<Pair<CGLabelScope, Set<Integer>>> exceptionHandlers, CGLabelScope methodEndLbScope, ExcsThrowPoint point) throws CompileException {}
	@Override public void exTypeIdToAcc(CGScope scope) throws CompileException {}
	@Override public void exCodeToAcc(CGScope scope) throws CompileException {}
	@Override public int getRefSize() {return 1;}
	@Override public int getCallSize() {return 1;}
	@Override public HashMap<Byte, RegPair> buildRegsPool() {return null;}
	@Override public CGIContainer pushConst(CGScope scope, int size, long value, boolean isFixed) {return null;}
	@Override public CGIContainer pushCells(CGScope scope, int size, CGCells cells) throws CompileException {return null;}
	@Override public void stackPrepare(CGIContainer scope, boolean firstBlock, int argsSize, int varsSize, CGExcs excs) throws CompileException {};
	@Override public CGIContainer blockFree(int size) {return null;}
	@Override public String getVersion() {return VERSION;}
	@Override public CGIContainer jump(CGScope scope, CGLabelScope lScope) throws CompileException {return null;}
	@Override public void pushLabel(CGScope scope, String label) {}
	@Override public void boolAccCond(CGScope scope, CGBranch branchScope, boolean byBit0) throws CompileException {}
	@Override public void boolFlagCond(CGScope scope, boolean inverted, CGBranch branchScope) throws CompileException {}
	@Override public CGIContainer call(CGScope scope, CGLabelScope lScope) throws CompileException {return null;}
	@Override public int getCallStackSize() {return 0;}
	@Override public boolean normalizeIRegConst(CGMethodScope mScope, char iReg, AtomicInteger lastOffset) {return false;}
	@Override public void computeArrCellAddr(CGScope scope, CGCells arrAddrCells, CGArrCells arrCells, CGExcs excs) throws CompileException {}
	@Override public void arrToAcc(CGScope scope, CGArrCells arrCells, boolean isFixed) throws CompileException {}
	@Override public void accToArr(CGScope scope, CGArrCells arrScope) throws CompileException {}
	@Override public void flashDataToArr(CGScope scope, DataSymbol symbol, int offset) {}
	@Override public void updateArrRefCount(CGScope cgScope, CGCells cells, boolean isInc, boolean isView) throws CompileException {}
	@Override public void cellsToArrReg(CGScope cgScope, CGCells cells) throws CompileException {}
	@Override public CGIContainer pushArrReg(CGScope scope) {return null;}
	@Override public CGIContainer popArrReg(CGScope scope) {return null;}
	@Override public void arrSizetoAcc(CGScope cgScope, boolean isView) {}
	@Override public void accToArrReg(CGScope scope) throws CompileException {}
	@Override public void cellsToAcc(CGScope scope, CGCells cells, boolean isFixed) throws CompileException {}
	@Override public void thisToAcc(CGScope scope) throws CompileException {}
	@Override public NativeBinding nativeMethodInit(CGScope scope, String signature, boolean isConstants) throws CompileException {return null;}
	@Override public void nativeMethodSetArg(CGScope scope, String signature, List<Byte> storedRegs, int index, boolean isFixed) throws CompileException {}
	@Override public void nativeMethodInvoke(CGScope scope, String signature, List<Byte> storedRegs, VarType type) throws CompileException {}
	@Override public void functApply(CGScope scope, String funct, long[] constants) throws CompileException {}
	@Override public CGIContainer finMethod(VarType type, int argsSize, int varsSize, int lastStackOffset) throws CompileException {return null;}
	@Override public void terminate(CGScope scope, boolean systemStop, boolean excsCheck) throws CompileException {}
	@Override public void exCodeToAccH(CGScope scope, long numValue) throws CompileException {}
	@Override public void cellsToCells(CGScope scope, CGCells leftCells, VarType leftType, CGCells rightCells, VarType rigthType) throws CompileException {}
	@Override public void computeArrViewCellsAddr(CGScope scope, CGCells arrAddrCells, CGArrCells arrCells, CGExcs excs) throws CompileException {}
	@Override public CGIAsm pushReg(byte reg) throws CompileException {return null;}
	@Override public CGIAsm popReg(byte reg) throws CompileException {return null;}
	@Override public void methodInvoke(CGScope scope, CGLabelScope lbScope, String signature, VarType type) throws CompileException {}
	@Override public void invokeConstr(CGScope scope, CGLabelScope lScope) throws CompileException {}
	@Override public void nativeMethodArrArgPrepare(CGScope cgScope, String signature, List<Byte> storedRegs, int index, boolean checkedByCompiler, CGLabelScope lbScope) throws CompileException {}
	@Override public int getThreadHeaderSize() {return 0;}
	@Override public int getTimerHeaderSize() {return 0;}
	@Override public void cellsOpCells(CGScope scope, CGCells leftCells, VarType leftType, Operator op, CGCells rightCells, VarType rightType) throws CompileException {}
	@Override public void cellsCpCells(CGScope scope, CGCells leftCells, VarType leftType, CGCells rightCells, VarType rigthType, Operator op, boolean isNot, boolean isOr, CGBranch branchScope) throws CompileException {}
}
