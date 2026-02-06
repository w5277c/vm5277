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

package ru.vm5277.compiler.avr_codegen;

import ru.vm5277.common.ExcsThrowPoint;
import ru.vm5277.common.LabelNames;
import ru.vm5277.common.RTOSFeature;
import ru.vm5277.common.RTOSLibs;
import ru.vm5277.common.VarType;
import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.common.cg.CodeExcsChecker;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.items.CGIAsm;
import ru.vm5277.common.cg.items.CGIAsmCall;
import ru.vm5277.common.cg.items.CGIAsmCondJump;
import ru.vm5277.common.cg.items.CGIAsmJump;
import ru.vm5277.common.cg.items.CGIAsmLd;
import ru.vm5277.common.cg.items.CGIContainer;
import ru.vm5277.common.cg.scopes.CGLabelScope;
import ru.vm5277.common.cg.scopes.CGMethodScope;
import ru.vm5277.common.cg.scopes.CGScope;
import static ru.vm5277.common.cg.scopes.CGScope.genId;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.lexer.Operator;
import static ru.vm5277.compiler.avr_codegen.Generator.CALL_INSTR;

public class ExcsChecker extends CodeExcsChecker {

	@Override
	public Integer getHandled(CGExcs excs, String exceptionName) {
		int id = VarType.getExceptionId(exceptionName);
		if(-1!=id) {
			return excs.getThrowable(id);
		}
		return null;
	}
	
	//UNCHECKED
	@Override
	public void stackOverflow(CodeGenerator cg, CGScope scope, CGExcs excs, int size, byte[] popRegIds) throws CompileException {
		int id = VarType.getExceptionId("StackOverflowException");
		if(-1==id) return;

		cg.setFeature(RTOSFeature.OS_FT_ETRACE);
		excs.getProduced().add(id);
		
		CGLabelScope skipLbScope = new CGLabelScope(null, genId(), LabelNames.THROW_SKIP, false);
		CGLabelScope catchLbScope = excs.getRuntimeChecks().get(id);
//TODO проверять Y - size с константой RTOS нижней границы стека
/*
		//scope.append(new CGIAsmCondJump("brcc", skipLbScope));
		scope.append(new CGIAsmLd("ldi", "r16", Integer.toString(id)));*/
		RTOSLibs.ETRACE_ADD.setRequired();
/*		scope.append(new CGIAsmCall(CALL_INSTR, "j8bproc_etrace_addfirst", true));		
		if(null!=popRegIds) {
			for(byte popRegId : popRegIds) {
				scope.append(new CGIAsm("pop r" + popRegId));
			}
		}
		if(null==catchLbScope) {
			scope.append(new CGIAsm("set"));
			scope.append(new CGIAsmJump("jmp", excs.getMeathodEndLabel()));
		}
		else {
			scope.append(new CGIAsm("clt")); // Функции типа eNewInstance могут блокировать работу диспетчера устанавливая флаг T
			scope.append(new CGIAsmJump("jmp", catchLbScope));
		}
		scope.append(skipLbScope);*/
	}

	@Override
	public void mathOverflow(CodeGenerator cg, CGScope scope, CGExcs excs, Operator op, int size) throws CompileException {
		int id = VarType.getExceptionId("MathOverflowException");
		if(-1==id) return;
		Integer throwableId = excs.getThrowable(id);
		if(null!=throwableId) {
			cg.setFeature(RTOSFeature.OS_FT_ETRACE);
			excs.getProduced().add(id);
			
			CGLabelScope skipLbScope = new CGLabelScope(null, genId(), LabelNames.THROW_SKIP, true);
			CGLabelScope catchLbScope = excs.getRuntimeChecks().get(throwableId);
			if(Operator.MULT==op) {
				if(0x01==size) {
					scope.append(new CGIAsmLd("cpi", "r17", "0x00"));
					scope.append(new CGIAsmCondJump("breq", skipLbScope));
				}
				else if(0x02==size) {
					scope.append(new CGIAsmLd("cpi", "r18", "0x00"));
					scope.append(new CGIAsmCondJump("breq", skipLbScope));
				}
				else {
					scope.append(new CGIAsmCondJump("brcc", skipLbScope));
				}
			}
			else {
				scope.append(new CGIAsmCondJump("brcc", skipLbScope));
			}
			
			scope.append(new CGIAsmLd("ldi", "r16", Integer.toString(id)));
			scope.append(new CGIAsmLd("ldi", "r17", "0x00"));
			
			doThrow(cg, scope, excs);
			if(null==catchLbScope) {
				scope.append(new CGIAsm("set"));
				excs.getMeathodEndLabel().setUsed();
				scope.append(new CGIAsmJump(Generator.JUMP_INSTR, excs.getMeathodEndLabel(), false));
			}
			else {
				catchLbScope.setUsed();
				scope.append(new CGIAsmJump(Generator.JUMP_INSTR, catchLbScope, false));
			}
			scope.append(skipLbScope);
		}
	}

	@Override
	public void divByZero(CodeGenerator cg, CGScope scope, CGExcs excs, byte[] regIds, boolean popRequired) throws CompileException {
		int id = VarType.getExceptionId("DivByZeroException");
		if(-1==id) return;
		Integer throwableId = excs.getThrowable(id);
		if(null!=throwableId) {
			cg.setFeature(RTOSFeature.OS_FT_ETRACE);
			excs.getProduced().add(id);
			
			CGLabelScope skipLbScope = new CGLabelScope(null, genId(), LabelNames.THROW_SKIP, false);
			CGLabelScope catchLbScope = excs.getRuntimeChecks().get(throwableId);
			for(byte regId : regIds) {
				scope.append(new CGIAsm("tst", "r" + regId));
				skipLbScope.setUsed();
				scope.append(new CGIAsmCondJump("brne", skipLbScope));
			}
			if(popRequired) {
				for(byte regId : regIds) {
					scope.append(new CGIAsm("pop", "r" + regId));
				}
			}
			scope.append(new CGIAsmLd("ldi", "r16", Integer.toString(id)));
			scope.append(new CGIAsmLd("ldi", "r17", "0x00"));
			doThrow(cg, scope, excs);
			if(null==catchLbScope) {
				scope.append(new CGIAsm("set"));
				excs.getMeathodEndLabel().setUsed();
				scope.append(new CGIAsmJump(Generator.JUMP_INSTR, excs.getMeathodEndLabel(), false));
			}
			else {
				catchLbScope.setUsed();
				scope.append(new CGIAsmJump(Generator.JUMP_INSTR, catchLbScope, false));
			}
			scope.append(skipLbScope);
		}
	}

	//UNCHECKED
	@Override
	public void outOfMemory(CodeGenerator cg, CGScope scope, CGExcs excs) throws CompileException {
		int id = VarType.getExceptionId("OutOfMemoryException");
		if(-1==id) return;
		
		cg.setFeature(RTOSFeature.OS_FT_ETRACE);
		excs.getProduced().add(id);
		
		CGLabelScope skipLbScope = new CGLabelScope(null, genId(), LabelNames.THROW_SKIP, true);
		CGLabelScope catchLbScope = excs.getRuntimeChecks().get(id);
		scope.append(new CGIAsmCondJump("brcc", skipLbScope));
		scope.append(new CGIAsmLd("ldi", "r16", Integer.toString(id)));
		scope.append(new CGIAsmLd("ldi", "r17", "0x00"));
		doThrow(cg, scope, excs);
		if(null==catchLbScope) {
			scope.append(new CGIAsm("set"));
			if(null==excs.getMeathodEndLabel()) {
				throw new CompileException("COMPILER ERROR: Method end label is null (OutOgMemory checker)");
			}
			excs.getMeathodEndLabel().setUsed();
			scope.append(new CGIAsmJump(Generator.JUMP_INSTR, excs.getMeathodEndLabel(), false));
		}
		else {
			scope.append(new CGIAsm("clt")); // Функции типа eNewInstance могут блокировать работу диспетчера устанавливая флаг T
			catchLbScope.setUsed();
			scope.append(new CGIAsmJump(Generator.JUMP_INSTR, catchLbScope, false));
		}
		scope.append(skipLbScope);
	}

	@Override
	public void arrMathOverflow(CodeGenerator cg, CGScope scope, CGExcs excs, byte[] popRegIds) throws CompileException {
		int id = VarType.getExceptionId("ArrayInitException");
		if(-1==id) return;
		Integer throwableId = excs.getThrowable(id);
		if(null!=throwableId) {
			cg.setFeature(RTOSFeature.OS_FT_ETRACE);
			excs.getProduced().add(id);
			
			CGLabelScope skipLbScope = new CGLabelScope(null, genId(), LabelNames.THROW_SKIP, true);
			CGLabelScope catchLbScope = excs.getRuntimeChecks().get(throwableId);
			scope.append(new CGIAsmCondJump("brcc", skipLbScope));
			scope.append(new CGIAsmLd("ldi", "r16", Integer.toString(id)));
			scope.append(new CGIAsmLd("ldi", "r17", Integer.toString(VarType.getExceptionId("MathOverflowException"))));
			doThrow(cg, scope, excs);
			if(null!=popRegIds) {
				for(byte popRegId : popRegIds) {
					scope.append(new CGIAsm("pop", "r" + popRegId));
				}
			}
			if(null==catchLbScope) {
				scope.append(new CGIAsm("set"));
				excs.getMeathodEndLabel().setUsed();
				scope.append(new CGIAsmJump(Generator.JUMP_INSTR, excs.getMeathodEndLabel(), false));
			}
			else {
				catchLbScope.setUsed();
				scope.append(new CGIAsmJump(Generator.JUMP_INSTR, catchLbScope, false));
			}
			scope.append(skipLbScope);
		}
	}
	
	@Override
	public void arrOutOfMemory(CodeGenerator cg, CGScope scope, CGExcs excs, byte[] popRegIds) throws CompileException {
		int id = VarType.getExceptionId("ArrayInitException");
		if(-1==id) return;
		Integer throwableId = excs.getThrowable(id);
		if(null!=throwableId) {
			cg.setFeature(RTOSFeature.OS_FT_ETRACE);
			excs.getProduced().add(id);
			
			CGLabelScope skipLbScope = new CGLabelScope(null, genId(), LabelNames.THROW_SKIP, true);
			CGLabelScope catchLbScope = excs.getRuntimeChecks().get(throwableId);
			scope.append(new CGIAsmCondJump("brcc", skipLbScope));
			scope.append(new CGIAsmLd("ldi", "r16", Integer.toString(id)));
			scope.append(new CGIAsmLd("ldi", "r17", Integer.toString(VarType.getExceptionId("OutOfMemoryException"))));
			doThrow(cg, scope, excs);
			if(null!=popRegIds) {
				for(byte popRegId : popRegIds) {
					scope.append(new CGIAsm("pop", "r" + popRegId));
				}
			}
			if(null==catchLbScope) {
				scope.append(new CGIAsm("set"));
				excs.getMeathodEndLabel().setUsed();
				scope.append(new CGIAsmJump(Generator.JUMP_INSTR, excs.getMeathodEndLabel(), false));
			}
			else {
				scope.append(new CGIAsm("clt")); // Функции типа eNewInstance могут блокировать работу диспетчера устанавливая флаг T
				catchLbScope.setUsed();
				scope.append(new CGIAsmJump(Generator.JUMP_INSTR, catchLbScope, false));
			}
			scope.append(skipLbScope);
		}
	}

	@Override
	public void invalidIndex(CodeGenerator cg, CGScope scope, CGExcs excs) throws CompileException {
		int id = VarType.getExceptionId("InvalidIndexException");
		if(-1==id) return;
		Integer throwableId = excs.getThrowable(id);
		if(null!=throwableId) {
			cg.setFeature(RTOSFeature.OS_FT_ETRACE);
			excs.getProduced().add(id);
			
			CGLabelScope skipLbScope = new CGLabelScope(null, genId(), LabelNames.THROW_SKIP, false);
			CGLabelScope catchLbScope = excs.getRuntimeChecks().get(throwableId);
			scope.append(new CGIAsmCondJump("brcc", skipLbScope));
			scope.append(new CGIAsmLd("ldi", "r16", Integer.toString(id)));
			scope.append(new CGIAsmLd("ldi", "r17", "0x00"));
			doThrow(cg, scope, excs);
			if(null==catchLbScope) {
				scope.append(new CGIAsm("set"));
				excs.getMeathodEndLabel().setUsed();
				scope.append(new CGIAsmJump(Generator.JUMP_INSTR, excs.getMeathodEndLabel(), false));
			}
			else {
				scope.append(new CGIAsm("clt")); // Функции типа eNewInstance могут блокировать работу диспетчера устанавливая флаг T
				catchLbScope.setUsed();
				scope.append(new CGIAsmJump(Generator.JUMP_INSTR, catchLbScope, false));
			}
			scope.append(skipLbScope);
		}
	}

	
	@Override
	public void makeException(CodeGenerator cg, CGScope scope, CGExcs excs, int throwableId) throws CompileException {
		cg.setFeature(RTOSFeature.OS_FT_ETRACE);
		excs.getProduced().add(throwableId);
		scope.append(new CGIAsmLd("ldi", "r16", Integer.toString(throwableId)));
		scope.append(new CGIAsmLd("ldi", "r17", "0x00"));
		doThrow(cg, scope, excs);
		CGLabelScope catchLbScope = excs.getRuntimeChecks().get(throwableId);
		if(null==catchLbScope) {
			scope.append(new CGIAsm("set"));
			excs.getMeathodEndLabel().setUsed();
			scope.append(new CGIAsmJump(Generator.JUMP_INSTR, excs.getMeathodEndLabel(), false));
		}
		else {
			catchLbScope.setUsed();
			scope.append(new CGIAsmJump(Generator.JUMP_INSTR, catchLbScope, false));
		}
	}

	public void doThrow(CodeGenerator cg, CGScope scope, CGExcs excs) throws CompileException {
		CGMethodScope mScope = (CGMethodScope)scope.getScope(CGMethodScope.class);
		ExcsThrowPoint point = cg.getTargetInfoBuilder().addExcsThrowPoint(cg, excs.getSourcePosition(), null==mScope ? "" : mScope.getSignature());
		RTOSLibs.ETRACE_ADD.setRequired();
		CGIContainer cont7b = new CGIContainer();
		// Нельзя применять CGAsmLd, иначе оптимизатор вырежет 'лишний' ldi
		cont7b.append(new CGIAsm("ldi", "r18," + (point.getId()&0x7f)));
		CGIContainer cont15b = new CGIContainer();
		cont15b.append(new CGIAsm("ldi", "r18,low(" + (point.getId()&0x7fff) + ")"));
		cont15b.append(new CGIAsm("ldi", "r19,high(" + (point.getId()&0x7fff) + ")"));
		scope.append(point.makeContainer(cont7b, cont15b));
		scope.append(new CGIAsmCall(CALL_INSTR, "j8bproc_etrace_addfirst", true));
	}
}
