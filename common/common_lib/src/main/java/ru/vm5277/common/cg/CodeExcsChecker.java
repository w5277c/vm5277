/*
 * Copyright 2026 konstantin@5277.ru
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

import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.lexer.Operator;

public abstract class CodeExcsChecker {
	public abstract Integer getHandled(CGExcs excs, String exceptionName);
	public abstract void stackOverflow(CodeGenerator cg, CGScope scope, CGExcs excs, int size, byte[] popRegIds) throws CompileException;
	public abstract void mathOverflow(CodeGenerator cg, CGScope scope, CGExcs excs, Operator op, int size) throws CompileException;
	public abstract void divByZero(CodeGenerator cg, CGScope scope, CGExcs excs, byte[] regIds, boolean popRequired) throws CompileException;
	public abstract void outOfMemory(CodeGenerator cg, CGScope scope, CGExcs excs) throws CompileException;
	public abstract void arrMathOverflow(CodeGenerator cg, CGScope scope, CGExcs excs, byte[] popRegIds) throws CompileException;
	public abstract void arrOutOfMemory(CodeGenerator cg, CGScope scope, CGExcs excs, byte[] popRegIds) throws CompileException;
	public abstract void invalidIndex(CodeGenerator cg, CGScope scope, CGExcs excs) throws CompileException;
	public abstract void makeException(CodeGenerator cg, CGScope scope, CGExcs excs, int throwableId) throws CompileException;
}
