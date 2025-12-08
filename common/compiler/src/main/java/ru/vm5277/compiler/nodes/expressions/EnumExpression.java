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
import ru.vm5277.common.compiler.CodegenResult;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import static ru.vm5277.compiler.Main.debugAST;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.semantic.Scope;
import static ru.vm5277.common.SemanticAnalyzePhase.DECLARE;
import static ru.vm5277.common.SemanticAnalyzePhase.PRE;
import static ru.vm5277.common.SemanticAnalyzePhase.POST;
import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.common.VarType;
import ru.vm5277.compiler.semantic.CIScope;
import ru.vm5277.compiler.semantic.EnumScope;

public class EnumExpression extends ExpressionNode {
	private	TypeReferenceExpression	targetExpr;
	private	String					value;
	private	int						index		= -1;
	private	EnumScope				targetScope;

	public EnumExpression(TokenBuffer tb, MessageContainer mc, TypeReferenceExpression targetExpr, String value) throws CompileException {
		super(tb, mc);
		
		this.targetExpr = targetExpr;
		this.value = value;
	}
	
	public ExpressionNode getTargetExpr() {
		return targetExpr;
	}
	
	public CIScope getTargetScope() {
		return targetScope;
	}
	
	public int getIndex() {
		return index;
	}
	
	@Override
	public boolean preAnalyze() {
		boolean result = true;
		debugAST(this, PRE, true, getFullInfo());
		if(null==targetExpr) {
			markError("Null-target in enum expression");
			result = false;
		}
		debugAST(this, PRE, false, result, getFullInfo());
		return result;
	}
	
	@Override
	public boolean declare(Scope scope) {
		boolean result = true;
		debugAST(this, DECLARE, true, getFullInfo());
		
		result&=targetExpr.declare(scope);
		
		debugAST(this, DECLARE, false, result, getFullInfo() + (declarationPendingNodes.containsKey(this) ? " [DP]" : ""));
		return result;
	}
	
	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		boolean result = true;
		debugAST(this, POST, true, getFullInfo() + " type:" + type);
		
		targetScope = (EnumScope)targetExpr.getScope();
		index = targetScope.getValueIndex(value);
		type = targetExpr.getType();
		
		if(0>index) {
			markError("Invalid enum value '" + value + "' for enum " + targetExpr.toString());
			result = false;
		}

		debugAST(this, POST, false, result, getFullInfo());
		return result;
	}

	@Override
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean toAccum, CGExcs excs) throws CompileException {
		if(toAccum) {
			cg.constToAcc(parent, 0x01, index, false);
			return CodegenResult.RESULT_IN_ACCUM;
		}
		
		return null;
	}
	
	@Override
	public String toString() {
		return targetExpr.toString() + "." + value;
	}
	
	public String getFullInfo() {
		return getClass().getSimpleName() + " " + toString();
	}
}
