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

import ru.vm5277.common.LabelNames;
import ru.vm5277.common.RTOSFeature;
import ru.vm5277.common.RTOSLibs;
import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.items.CGIAsm;
import ru.vm5277.common.cg.items.CGIAsmCall;
import ru.vm5277.common.cg.items.CGIAsmCondJump;
import ru.vm5277.common.cg.items.CGIAsmJump;
import ru.vm5277.common.cg.items.CGIAsmLd;
import ru.vm5277.common.cg.items.CGIContainer;
import ru.vm5277.common.cg.scopes.CGLabelScope;
import ru.vm5277.common.cg.scopes.CGScope;
import static ru.vm5277.common.cg.scopes.CGScope.genId;
import ru.vm5277.common.exceptions.CompileException;
import static ru.vm5277.compiler.avr_codegen.Generator.CALL_INSTR;

public class ExcsChecker {
	
	//UNCHECKED
	public static void stackOverflow(CodeGenerator cg, CGScope scope, CGExcs excs, int size, byte[] popRegIds) throws CompileException {
		cg.setFeature(RTOSFeature.OS_FT_ETRACE);
		int id = CGExcs.STACK_OVERFLOW;
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

	public static void mathOverflow(CodeGenerator cg, CGScope scope, CGExcs excs) throws CompileException {
		int id = CGExcs.MATH_OVERFLOW;
		if(excs.getRuntimeChecks().containsKey(id)) {
			cg.setFeature(RTOSFeature.OS_FT_ETRACE);
			excs.getProduced().add(id);
			
			CGLabelScope skipLbScope = new CGLabelScope(null, genId(), LabelNames.THROW_SKIP, false);
			CGLabelScope catchLbScope = excs.getRuntimeChecks().get(id);
			scope.append(new CGIAsmCondJump("brcc", skipLbScope));
			scope.append(new CGIAsmLd("ldi", "r16", Integer.toString(id)));
			RTOSLibs.ETRACE_ADD.setRequired();
			scope.append(new CGIAsmCall(CALL_INSTR, "j8bproc_etrace_addfirst", true));		
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

	public static void divByZero(CodeGenerator cg, CGScope scope, CGExcs excs, byte[] regIds, boolean popRequired) throws CompileException {
		int id = CGExcs.DIV_BY_ZERO;
		if(excs.getRuntimeChecks().containsKey(id)) {
			cg.setFeature(RTOSFeature.OS_FT_ETRACE);
			excs.getProduced().add(id);
			
			CGLabelScope skipLbScope = new CGLabelScope(null, genId(), LabelNames.THROW_SKIP, false);
			CGLabelScope catchLbScope = excs.getRuntimeChecks().get(id);
			for(byte regId : regIds) {
				scope.append(new CGIAsm("tst", "r" + regId));
				scope.append(new CGIAsmCondJump("brne", skipLbScope));
			}
			if(popRequired) {
				for(byte regId : regIds) {
					scope.append(new CGIAsm("pop", "r" + regId));
				}
			}
			scope.append(new CGIAsmLd("ldi", "r16", Integer.toString(id)));
			RTOSLibs.ETRACE_ADD.setRequired();
			scope.append(new CGIAsmCall(CALL_INSTR, "j8bproc_etrace_addfirst", true));		
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
	public static void outOfMemory(CodeGenerator cg, CGScope scope, CGExcs excs) throws CompileException {
		cg.setFeature(RTOSFeature.OS_FT_ETRACE);
		int id = CGExcs.OUT_OF_MEMORY;
		excs.getProduced().add(id);
		
		CGLabelScope skipLbScope = new CGLabelScope(null, genId(), LabelNames.THROW_SKIP, true);
		CGLabelScope catchLbScope = excs.getRuntimeChecks().get(id);
		scope.append(new CGIAsmCondJump("brcc", skipLbScope));
		scope.append(new CGIAsmLd("ldi", "r16", Integer.toString(id)));
		RTOSLibs.ETRACE_ADD.setRequired();
		scope.append(new CGIAsmCall(CALL_INSTR, "j8bproc_etrace_addfirst", true));		
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

	public static void arrMathOverflow(CodeGenerator cg, CGScope scope, CGExcs excs, byte[] popRegIds) throws CompileException {
		int id = CGExcs.ARRAY_INIT;
		if(excs.getRuntimeChecks().containsKey(id)) {
			cg.setFeature(RTOSFeature.OS_FT_ETRACE);
			excs.getProduced().add(id);
			
			CGLabelScope skipLbScope = new CGLabelScope(null, genId(), LabelNames.THROW_SKIP, false);
			CGLabelScope catchLbScope = excs.getRuntimeChecks().get(id);
			scope.append(new CGIAsmCondJump("brcc", skipLbScope));
			scope.append(new CGIAsmLd("ldi", "r16", Integer.toString(id)));
			scope.append(new CGIAsmLd("ldi", "r17", Integer.toString(CGExcs.MATH_OVERFLOW)));
			RTOSLibs.ETRACE_ADD.setRequired();
			scope.append(new CGIAsmCall(CALL_INSTR, "j8bproc_etrace_addfirst", true));		

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
	
	public static void arrOutOfMemory(CodeGenerator cg, CGScope scope, CGExcs excs, byte[] popRegIds) throws CompileException {
		int id = CGExcs.ARRAY_INIT;
		if(excs.getRuntimeChecks().containsKey(id)) {
			cg.setFeature(RTOSFeature.OS_FT_ETRACE);
			excs.getProduced().add(id);
			
			CGLabelScope skipLbScope = new CGLabelScope(null, genId(), LabelNames.THROW_SKIP, false);
			CGLabelScope catchLbScope = excs.getRuntimeChecks().get(id);
			scope.append(new CGIAsmCondJump("brcc", skipLbScope));
			scope.append(new CGIAsmLd("ldi", "r16", Integer.toString(id)));
			scope.append(new CGIAsmLd("ldi", "r17", Integer.toString(CGExcs.OUT_OF_MEMORY)));
			RTOSLibs.ETRACE_ADD.setRequired();
			scope.append(new CGIAsmCall(CALL_INSTR, "j8bproc_etrace_addfirst", true));		

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

	public static void invalidIndex(CodeGenerator cg, CGScope scope, CGExcs excs) throws CompileException {
		int id = CGExcs.INVALID_INDEX;
		if(excs.getRuntimeChecks().containsKey(id)) {
			cg.setFeature(RTOSFeature.OS_FT_ETRACE);
			excs.getProduced().add(id);
			
			CGLabelScope skipLbScope = new CGLabelScope(null, genId(), LabelNames.THROW_SKIP, false);
			CGLabelScope catchLbScope = excs.getRuntimeChecks().get(id);
			scope.append(new CGIAsmCondJump("brcc", skipLbScope));
			scope.append(new CGIAsmLd("ldi", "r16", Integer.toString(id)));
			RTOSLibs.ETRACE_ADD.setRequired();
			scope.append(new CGIAsmCall(CALL_INSTR, "j8bproc_etrace_addfirst", true));		

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
}
