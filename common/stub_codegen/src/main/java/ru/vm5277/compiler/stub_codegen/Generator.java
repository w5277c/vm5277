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
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import ru.vm5277.common.ExcsThrowPoint;
import ru.vm5277.common.NativeBinding;
import ru.vm5277.common.Operator;
import ru.vm5277.common.Pair;
import ru.vm5277.common.StrUtils;
import ru.vm5277.common.RTOSParam;
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
import ru.vm5277.common.exceptions.CompileException;

public class Generator extends CodeGenerator {
	private	final	static	String				VERSION		= StrUtils.readVersion(Generator.class);

	public Generator(String platform, int optLevel, Map<String, NativeBinding> nbMap, Map<RTOSParam, Object> params) {
		super(platform, optLevel, nbMap, params);
	}
	
	@Override public CGIContainer accCast(VarType accType, VarType opType) throws CompileException {return null;}
	@Override public void cellsToAcc(CGScope scope, CGCellsScope cScope) throws CompileException {}
	@Override public void accToCells(CGScope scope, CGCellsScope cScope) throws CompileException {}
	@Override public void setHeapReg(CGScope scope, CGCells cells) throws CompileException {}
	@Override public void constToAcc(CGScope scope, int size, long value, boolean isFixed) {}
	@Override public CGIContainer constToCells(CGScope scope, long value, CGCells cells, boolean isFixed) throws CompileException {return null;}
	@Override public void cellsAction(CGScope scope, CGCells cells, Operator op, boolean isFixed, CGExcs excs) throws CompileException {}
	@Override public void constAction(CGScope scope, Operator op, long k, boolean isFixed, CGExcs excs) throws CompileException {}
	@Override public CGIContainer pushAccBE(CGScope scope, Integer size) {return null;}
	@Override public void popAccBE(CGScope scope, Integer size) {}
	@Override public CGIContainer pushHeapReg(CGScope scope) {return null;}
	@Override public CGIContainer popHeapReg(CGScope scope) {return null;}
	@Override public CGIContainer pushStackReg(CGScope scope) {return null;}
	@Override public CGIContainer popStackReg(CGScope scope) {return null;}
	@Override public void updateClassRefCount(CGScope scope, CGCells cells, boolean isInc) throws CompileException {}
	@Override public void invokeInterfaceMethod(CGScope scope, String className, String methodName, VarType type, VarType[] types, VarType ifaceType, int methodSN) throws CompileException {}
	@Override public void eInstanceof(CGScope scope, VarType type) throws CompileException {}
	@Override public void eUnary(CGScope scope, Operator opt, CGCells cells, boolean toAccum, CGExcs excs) throws CompileException {}
	@Override public void eNewInstance(CGScope scope, int size, CGLabelScope iidLabel, VarType type, boolean launchPoint, CGExcs excs) throws CompileException {}
	@Override public CGIContainer eNewArray(VarType type, int depth, int[] cDims, CGExcs excs) throws CompileException {return null;}
	@Override public CGIContainer eNewArrView(int depth, CGExcs excs) throws CompileException {return null;}
	@Override public void eTry(CGBlockScope blockScope, List<Case> cases, CGBlockScope defaultBlockScope) {}
	@Override public CGIContainer eReturn(CGScope scope, int argsSize, int varsSize, VarType retType) throws CompileException {return null;}
	@Override public void eThrow(CGScope cgs, int exceptioId, boolean isMethodLeave, CGLabelScope lbScope, boolean withCode, ExcsThrowPoint point) throws CompileException {}
	@Override public void throwCheck(CGScope scope, List<Pair<CGLabelScope, Set<Integer>>> exceptionHandlers, CGLabelScope methodEndLbScope, ExcsThrowPoint point) throws CompileException {}
	@Override public void exTypeIdToAcc(CGScope scope) throws CompileException {}
	@Override public void exCodeToAcc(CGScope scope) throws CompileException {}
	@Override public int getRefSize() {return 1;}
	@Override public int getCallSize() {return 1;}
	@Override public ArrayList<RegPair> buildRegsPool() {return null;}
	@Override public CGIContainer pushConst(CGScope scope, int size, long value, boolean isFixed) {return null;}
	@Override public CGIContainer pushCells(CGScope scope, int size, CGCells cells) throws CompileException {return null;}
	@Override public CGIContainer stackPrepare(boolean firstBlock, int argsSize, int varsSize, CGExcs excs) {return null;}
	@Override public CGIContainer blockFree(int size) {return null;}
	@Override public String getVersion() {return VERSION;}
	@Override public CGIContainer jump(CGScope scope, CGLabelScope lScope) throws CompileException {return null;}
	@Override public void pushLabel(CGScope scope, String label) {}
	@Override public void cellsCond(CGScope scope, CGCells cells, Operator op, boolean isNot, boolean isOr, CGBranch branchScope) throws CompileException {}
	@Override public void constCond(CGScope scope, CGCells cells, Operator op, long k, boolean isNot, boolean isOr, CGBranch branchScope) throws CompileException {}
	@Override public void boolCond(CGScope scope, CGBranch branchScope, boolean byBit0) throws CompileException {}
	@Override public CGIContainer call(CGScope scope, CGLabelScope lScope) throws CompileException {return null;}
	@Override public int getCallStackSize() {return 0;}
	@Override public boolean normalizeIRegConst(CGMethodScope mScope, char iReg, AtomicInteger lastOffset) {return false;}
	@Override public void computeArrCellAddr(CGScope scope, CGCells arrAddrCells, CGArrCells arrCells, CGExcs excs) throws CompileException {}
	@Override public void arrToAcc(CGScope scope, CGArrCells arrCells) throws CompileException {}
	@Override public void accToArr(CGScope scope, CGArrCells arrScope) throws CompileException {}
	@Override public void arrToCells(CGScope scope, CGArrCells arrCells, CGCells cells, CGExcs excs) throws CompileException {}
	@Override public void flashDataToArr(CGScope scope, DataSymbol symbol, int offset) {}
	@Override public void updateArrRefCount(CGScope cgScope, CGCells cells, boolean isInc, boolean isView) throws CompileException {}
	@Override public void cellsToArrReg(CGScope cgScope, CGCells cells) throws CompileException {}
	@Override public CGIContainer pushArrReg(CGScope scope) {return null;}
	@Override public CGIContainer popArrReg(CGScope scope) {return null;}
	@Override public void arrSizetoAcc(CGScope cgScope, boolean isView) {}
	@Override public void arrRegToCells(CGScope scope, CGCells cells) throws CompileException {}
	@Override public void arrRegToAcc(CGScope scope) throws CompileException {}
	@Override public void accToArrReg(CGScope scope) throws CompileException {}
	@Override public void accResize(VarType opType) throws CompileException {}
	@Override public void cellsToAcc(CGScope scope, CGCells cells) throws CompileException {}
	@Override public void thisToAcc(CGScope scope) throws CompileException {}
	@Override public void nativeMethodInit(CGScope scope, String signature) throws CompileException {}
	@Override public void nativeMethodSetArg(CGScope scope, String signature, int index) throws CompileException {}
	@Override public void nativeMethodInvoke(CGScope scope, String signature) throws CompileException {}
	@Override public CGIContainer finMethod(VarType type, int argsSize, int varsSize, int lastStackOffset) throws CompileException {return null;}
	@Override public void terminate(CGScope scope, boolean systemStop, boolean excsCheck) throws CompileException {}
	@Override public void exCodeToAccH(CGScope scope, long numValue) throws CompileException {}
}
