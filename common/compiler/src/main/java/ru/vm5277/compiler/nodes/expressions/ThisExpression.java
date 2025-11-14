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
package ru.vm5277.compiler.nodes.expressions;

import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import static ru.vm5277.compiler.Main.debugAST;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.semantic.ClassSymbol;
import ru.vm5277.compiler.semantic.Scope;
import static ru.vm5277.common.SemanticAnalyzePhase.DECLARE;
import ru.vm5277.common.compiler.CodegenResult;
import ru.vm5277.compiler.semantic.CIScope;

public class ThisExpression extends TypeReferenceExpression {
	public ThisExpression(TokenBuffer tb, MessageContainer mc, CIScope cis) {
		super(tb, mc, null, "this", cis);
	}
	
	@Override
	public boolean preAnalyze() {
		boolean result = true;
		return result;
	}
	
	@Override
	public boolean declare(Scope scope) {
		boolean result = true;
		debugAST(this, DECLARE, true, getFullInfo());
		cis = scope.getThis();
		
		type = VarType.fromClassName(cis.getName());
		symbol = new ClassSymbol(cis.getName(), type, true, false, cis);
		
		debugAST(this, DECLARE, false, result, getFullInfo() + (declarationPendingNodes.containsKey(this) ? " [DP]" : ""));
		return result;
	}
	
	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		return true;
	}
	
	@Override
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean toAccum) throws CompileException {
		CGScope cgs = null == parent ? cgScope : parent;
		
		if(toAccum) {
			cg.thisToAcc(cgs);
			return CodegenResult.RESULT_IN_ACCUM;
		}
		
		return null;
	}
	
	@Override
	public String toString() {
		return "this";
	}
	
	public String getFullInfo() {
		return getClass().getSimpleName();
	}
}
