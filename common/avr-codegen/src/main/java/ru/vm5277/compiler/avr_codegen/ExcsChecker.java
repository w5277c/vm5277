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
import ru.vm5277.common.enums.RtosFeature;
import ru.vm5277.common.enums.RTOSLibs;
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
import ru.vm5277.common.cg.items.CGIRAsm;
import ru.vm5277.common.cg.scopes.CGLabelScope;
import ru.vm5277.common.cg.scopes.CGMethodScope;
import ru.vm5277.common.cg.scopes.CGScope;
import static ru.vm5277.common.cg.scopes.CGScope.genId;
import ru.vm5277.common.enums.J8BException;
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
	
	//TODO Реализовать в рамках общей задачи проверки переполнения стека!
	//UNCHECKED
	@Override
	public void stackOverflow(CodeGenerator cg, CGIContainer scope, CGExcs excs, int size, byte[] popRegIds) throws CompileException {
/*		int id = VarType.getExceptionId("StackOverflowException");
		if(-1==id) return;

		cg.setFeature(RTOSFeature.OS_FT_ETRACE);
		excs.getProduced().add(id);
		
		CGLabelScope skipLbScope = new CGLabelScope(null, genId(), LabelNames.THROW_SKIP, false);
		CGLabelScope catchLbScope = excs.getRuntimeChecks().get(id);*/
//TODO проверять Y - size с константой RTOS нижней границы стека
/*
		//scope.append(new CGIAsmCondJump("brcc", skipLbScope));
		scope.append(new CGIAsmLd("ldi", "r16", Integer.toString(id)));*/
/*		RTOSLibs.ETRACE_ADD.setRequired();*/
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
	public CGIContainer mathOverflow(CodeGenerator cg, CGScope scope, CGExcs excs, byte[] regIds, boolean popRequired, boolean byZero) throws CompileException {
		int id = VarType.getExceptionId(J8BException.MathOverflowException.name());
		if(-1==id) return null;
		Integer throwableId = excs.getThrowable(id);
		if(null!=throwableId) {
			CGIContainer cont = new CGIContainer("mathOverflow checker");
			cg.setFeature(RtosFeature.OS_FT_ETRACE);
			excs.getProduced().add(id);
			
			CGLabelScope skipLbScope = new CGLabelScope(null, genId(), LabelNames.THROW_SKIP, null==regIds);
			CGLabelScope catchLbScope = excs.getRuntimeChecks().get(throwableId);
			
			if(null==regIds) {
				cont.append(new CGIAsmCondJump(byZero ? "breq" : "brcc", skipLbScope));
			}
			else {
				CGLabelScope throwLabel = new CGLabelScope(null, genId(), LabelNames.COMPARE_END, true);
				for(int i=0; i<regIds.length; i++) {
					byte regId = regIds[i];
					cont.append(new CGIRAsm("cpi", "r" + regId, ",0x00"));
					if(i<regIds.length-1) {
						cont.append(new CGIAsmCondJump("brne", throwLabel));
					}
					else {
						cont.append(new CGIAsmCondJump("breq", skipLbScope));
					}
				}

				cont.append(throwLabel);
				if(popRequired) {
					for(byte regId : regIds) {
						cont.append(new CGIAsm("pop", "r" + regId));
					}
				}
			}
			
			cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_ACCUM_L, Integer.toString(id)));
			cont.append(new CGIAsmLd("ldi", "r"+Generator.REG_ACCUM_H, "0x00"));
			
			cont.append(doThrow(cg, scope, excs));
			if(null==catchLbScope) {
				cont.append(new CGIAsm("set"));
				excs.getMethodEndLabel().setUsed();
				//TODO костыль. Иначе CodeOptimazer.removeUnusedLabels удаляет эту метку, хотя не должен
				excs.getMethodEndLabel().setPersist();
				cont.append(new CGIAsmJump(Generator.JUMP_INSTR, excs.getMethodEndLabel(), false));
			}
			else {
				catchLbScope.setUsed();
				cont.append(new CGIAsmJump(Generator.JUMP_INSTR, catchLbScope, false));
			}
			cont.append(skipLbScope);
			return cont;
		}
		return null;
	}

	@Override
	public CGIContainer divByZero(CodeGenerator cg, CGScope scope, CGExcs excs, byte[] regIds, boolean popRequired) throws CompileException {
		int id = VarType.getExceptionId(J8BException.DivByZeroException.name());
		if(-1==id) return null;
		Integer throwableId = excs.getThrowable(id);
		if(null!=throwableId) {
			CGIContainer cont = new CGIContainer("divByZero checker");
			cg.setFeature(RtosFeature.OS_FT_ETRACE);
			excs.getProduced().add(id);
			
			CGLabelScope skipLbScope = new CGLabelScope(null, genId(), LabelNames.THROW_SKIP, false);
			CGLabelScope catchLbScope = excs.getRuntimeChecks().get(throwableId);
			for(byte regId : regIds) {
				cont.append(new CGIAsm("tst", "r" + regId));
				skipLbScope.setUsed();
				cont.append(new CGIAsmCondJump("brne", skipLbScope));
			}
			if(popRequired) {
				for(byte regId : regIds) {
					cont.append(new CGIAsm("pop", "r" + regId));
				}
			}
			cont.append(new CGIAsmLd("ldi", "r16", Integer.toString(id)));
			cont.append(new CGIAsmLd("ldi", "r17", "0x00"));
			cont.append(doThrow(cg, scope, excs));
			if(null==catchLbScope) {
				cont.append(new CGIAsm("set"));
				excs.getMethodEndLabel().setUsed();
				//TODO костыль. Иначе CodeOptimazer.removeUnusedLabels удаляет эту метку, хотя не должен
				excs.getMethodEndLabel().setPersist();
				cont.append(new CGIAsmJump(Generator.JUMP_INSTR, excs.getMethodEndLabel(), false));
			}
			else {
				catchLbScope.setUsed();
				cont.append(new CGIAsmJump(Generator.JUMP_INSTR, catchLbScope, false));
			}
			cont.append(skipLbScope);
			return cont;
		}
		return null;
	}

	//UNCHECKED
	@Override
	public void outOfMemory(CodeGenerator cg, CGIContainer cont, CGExcs excs) throws CompileException {

		int id = VarType.getExceptionId(J8BException.OutOfMemoryException.name());
		if(-1==id) return;
		
		cg.setFeature(RtosFeature.OS_FT_ETRACE);
		excs.getProduced().add(id);
		
		CGLabelScope skipLbScope = new CGLabelScope(null, genId(), LabelNames.THROW_SKIP, true);
		CGLabelScope catchLbScope = excs.getRuntimeChecks().get(id);
		cont.append(new CGIAsmCondJump("brcc", skipLbScope));
		cont.append(new CGIAsmLd("ldi", "r16", Integer.toString(id)));
		cont.append(new CGIAsmLd("ldi", "r17", "0x00"));
		doThrow(cg, cont, excs, "...");
		if(null==catchLbScope) {
			cont.append(new CGIAsm("set"));
			if(null==excs.getMethodEndLabel()) {
				throw new CompileException("COMPILER ERROR: Method end label is null (OutOfMemory checker)");
			}
			excs.getMethodEndLabel().setUsed();
			//TODO костыль. Иначе CodeOptimazer.removeUnusedLabels удаляет эту метку, хотя не должен
			excs.getMethodEndLabel().setPersist();
			cont.append(new CGIAsmJump(Generator.JUMP_INSTR, excs.getMethodEndLabel(), false));
		}
		else {
			cont.append(new CGIAsm("clt")); // Функции типа eNewInstance могут блокировать работу диспетчера устанавливая флаг T
			catchLbScope.setUsed();
			cont.append(new CGIAsmJump(Generator.JUMP_INSTR, catchLbScope, false));
		}
		cont.append(skipLbScope);
	}

	@Override
	public void arrMathOverflow(CodeGenerator cg, CGScope scope, CGExcs excs, byte[] popRegIds) throws CompileException {
		int id = VarType.getExceptionId("ArrayInitException");
		if(-1==id) return;
		Integer throwableId = excs.getThrowable(id);
		if(null!=throwableId) {
			cg.setFeature(RtosFeature.OS_FT_ETRACE);
			excs.getProduced().add(id);
			
			CGLabelScope skipLbScope = new CGLabelScope(null, genId(), LabelNames.THROW_SKIP, true);
			CGLabelScope catchLbScope = excs.getRuntimeChecks().get(throwableId);
			scope.append(new CGIAsmCondJump("brcc", skipLbScope));
			scope.append(new CGIAsmLd("ldi", "r16", Integer.toString(id)));
			scope.append(new CGIAsmLd("ldi", "r17", Integer.toString(VarType.getExceptionId("MathOverflowException"))));
			scope.append(doThrow(cg, scope, excs));
			if(null!=popRegIds) {
				for(byte popRegId : popRegIds) {
					scope.append(new CGIAsm("pop", "r" + popRegId));
				}
			}
			if(null==catchLbScope) {
				scope.append(new CGIAsm("set"));
				excs.getMethodEndLabel().setUsed();
				//TODO костыль. Иначе CodeOptimazer.removeUnusedLabels удаляет эту метку, хотя не должен
				excs.getMethodEndLabel().setPersist();
				scope.append(new CGIAsmJump(Generator.JUMP_INSTR, excs.getMethodEndLabel(), false));
			}
			else {
				catchLbScope.setUsed();
				scope.append(new CGIAsmJump(Generator.JUMP_INSTR, catchLbScope, false));
			}
			scope.append(skipLbScope);
		}
	}
	
	//TODO должен быть UNCHECKED, использовать стандартный OutOfMemory и ввести код=array
	@Override
	public void arrOutOfMemory(CodeGenerator cg, CGScope scope, CGExcs excs, byte[] popRegIds) throws CompileException {
		int id = VarType.getExceptionId(J8BException.OutOfMemoryException.name());
		if(-1==id) return;
		cg.setFeature(RtosFeature.OS_FT_ETRACE);
		excs.getProduced().add(id);

		CGLabelScope skipLbScope = new CGLabelScope(null, genId(), LabelNames.THROW_SKIP, true);
		CGLabelScope catchLbScope = excs.getRuntimeChecks().get(id);
		scope.append(new CGIAsmCondJump("brcc", skipLbScope));
		scope.append(new CGIAsmLd("ldi", "r16", Integer.toString(id)));
		scope.append(new CGIAsmLd("ldi", "r17", "0"));//Integer.toString(VarType.getExceptionId("OutOfMemoryException"))));
		scope.append(doThrow(cg, scope, excs));
		if(null!=popRegIds) {
			for(byte popRegId : popRegIds) {
				scope.append(new CGIAsm("pop", "r" + popRegId));
			}
		}
		if(null==catchLbScope) {
			scope.append(new CGIAsm("set"));
			if(null==excs.getMethodEndLabel()) {
				throw new CompileException("COMPILER ERROR: Method end label is null (arrOutOfMemory checker)");
			}
			excs.getMethodEndLabel().setUsed();
			//TODO костыль. Иначе CodeOptimazer.removeUnusedLabels удаляет эту метку, хотя не должен
			excs.getMethodEndLabel().setPersist();
			scope.append(new CGIAsmJump(Generator.JUMP_INSTR, excs.getMethodEndLabel(), false));
		}
		else {
			scope.append(new CGIAsm("clt")); // Функции типа eNewInstance могут блокировать работу диспетчера устанавливая флаг T
			catchLbScope.setUsed();
			scope.append(new CGIAsmJump(Generator.JUMP_INSTR, catchLbScope, false));
		}
		scope.append(skipLbScope);
	}

	@Override
	public void invalidIndex(CodeGenerator cg, CGScope scope, CGExcs excs) throws CompileException {
		int id = VarType.getExceptionId("InvalidIndexException");
		if(-1==id) return;
		Integer throwableId = excs.getThrowable(id);
		if(null!=throwableId) {
			cg.setFeature(RtosFeature.OS_FT_ETRACE);
			excs.getProduced().add(id);
			
			CGLabelScope skipLbScope = new CGLabelScope(null, genId(), LabelNames.THROW_SKIP, false);
			CGLabelScope catchLbScope = excs.getRuntimeChecks().get(throwableId);
			scope.append(new CGIAsmCondJump("brcc", skipLbScope));
			scope.append(new CGIAsmLd("ldi", "r16", Integer.toString(id)));
			scope.append(new CGIAsmLd("ldi", "r17", "0x00"));
			scope.append(doThrow(cg, scope, excs));
			if(null==catchLbScope) {
				scope.append(new CGIAsm("set"));
				excs.getMethodEndLabel().setUsed();
				//TODO костыль. Иначе CodeOptimazer.removeUnusedLabels удаляет эту метку, хотя не должен
				excs.getMethodEndLabel().setPersist();
				scope.append(new CGIAsmJump(Generator.JUMP_INSTR, excs.getMethodEndLabel(), false));
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
		cg.setFeature(RtosFeature.OS_FT_ETRACE);
		excs.getProduced().add(throwableId);
		scope.append(new CGIAsmLd("ldi", "r16", Integer.toString(throwableId)));
		scope.append(new CGIAsmLd("ldi", "r17", "0x00"));
		scope.append(doThrow(cg, scope, excs));
		CGLabelScope catchLbScope = excs.getRuntimeChecks().get(throwableId);
		if(null==catchLbScope) {
			scope.append(new CGIAsm("set"));
			excs.getMethodEndLabel().setUsed();
			//TODO костыль. Иначе CodeOptimazer.removeUnusedLabels удаляет эту метку, хотя не должен
			excs.getMethodEndLabel().setPersist();
			scope.append(new CGIAsmJump(Generator.JUMP_INSTR, excs.getMethodEndLabel(), false));
		}
		else {
			catchLbScope.setUsed();
			scope.append(new CGIAsmJump(Generator.JUMP_INSTR, catchLbScope, false));
		}
	}

	public CGIContainer doThrow(CodeGenerator cg, CGScope scope, CGExcs excs) throws CompileException {
		CGIContainer cont = new CGIContainer();
		CGMethodScope mScope = (CGMethodScope)scope.getScope(CGMethodScope.class);
		ExcsThrowPoint point = cg.getTargetInfoBuilder().addExcsThrowPoint(cg, excs.getSourcePosition(), null==mScope ? "" : mScope.getSignature());
		RTOSLibs.ETRACE_ADD.setRequired();
		CGIContainer cont7b = new CGIContainer();
		// Нельзя применять CGAsmLd, иначе оптимизатор вырежет 'лишний' ldi
		cont7b.append(new CGIAsm("ldi", "r18," + (point.getId()&0x7f)));
		CGIContainer cont15b = new CGIContainer();
		cont15b.append(new CGIAsm("ldi", "r18,low(" + (point.getId()&0x7fff) + ")"));
		cont15b.append(new CGIAsm("ldi", "r19,high(" + (point.getId()&0x7fff) + ")"));
		cont.append(point.makeContainer(cont7b, cont15b));
		cont.append(new CGIAsmCall(CALL_INSTR, "j8bproc_etrace_addfirst", true));
		return cont;
	}
	//TODO
	public void doThrow(CodeGenerator cg, CGIContainer cont, CGExcs excs, String signature) throws CompileException {
		ExcsThrowPoint point = cg.getTargetInfoBuilder().addExcsThrowPoint(cg, excs.getSourcePosition(), signature);
		RTOSLibs.ETRACE_ADD.setRequired();
		CGIContainer cont7b = new CGIContainer();
		// Нельзя применять CGAsmLd, иначе оптимизатор вырежет 'лишний' ldi
		cont7b.append(new CGIAsm("ldi", "r18," + (point.getId()&0x7f)));
		CGIContainer cont15b = new CGIContainer();
		cont15b.append(new CGIAsm("ldi", "r18,low(" + (point.getId()&0x7fff) + ")"));
		cont15b.append(new CGIAsm("ldi", "r19,high(" + (point.getId()&0x7fff) + ")"));
		cont.append(point.makeContainer(cont7b, cont15b));
		cont.append(new CGIAsmCall(CALL_INSTR, "j8bproc_etrace_addfirst", true));
	}
}
