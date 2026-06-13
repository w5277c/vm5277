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
import ru.vm5277.common.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import static ru.vm5277.compiler.Main.debugAST;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.semantic.ClassSymbol;
import ru.vm5277.compiler.semantic.Scope;
import static ru.vm5277.common.enums.SemanticAnalyzePhase.DECLARE;
import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.common.enums.CodegenResult;
import ru.vm5277.compiler.Instance;
import ru.vm5277.compiler.semantic.CIScope;

public class ThisExpression extends TypeReferenceExpression {
	public ThisExpression(Instance inst, TokenBuffer tb, CIScope cis) {
		super(inst, tb, null, "this", cis);
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
	public boolean postAnalyze(Scope scope, CodeGenerator cg, CGScope parent) {
		if(null!=cgScope) cgScope.disable();
		cgScope = new CGScope(parent, CGScope.genId(), "this");
		return true;
	}
	
	@Override
	public Object codeGen(CodeGenerator cg, boolean toAccum, CGExcs excs) throws CompileException {
		if(toAccum) {
			cg.thisToAcc(cgScope);
			return CodegenResult.RESULT_IN_ACCUM;
		}
		
		return null;
	}
	
	@Override
	public String toString() {
		return "this";
	}
	
	@Override
	public String getFullInfo() {
		return getClass().getSimpleName();
	}
}
