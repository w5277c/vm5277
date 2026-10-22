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
package ru.vm5277.compiler.nodes.expressions;

import java.util.HashSet;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.semantic.Scope;
import static ru.vm5277.compiler.Main.debugAST;
import static ru.vm5277.common.enums.SemanticAnalyzePhase.DECLARE;
import static ru.vm5277.common.enums.SemanticAnalyzePhase.PRE;
import static ru.vm5277.common.enums.SemanticAnalyzePhase.POST;
import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.common.cg.scopes.CGMethodCallExpressionScope;
import ru.vm5277.common.cg.scopes.CGMethodScope;
import ru.vm5277.compiler.Instance;
import ru.vm5277.compiler.semantic.MethodSymbol;

//Выражение описывающее доступ к типу которое может состоять из других типпов (по аналогии с FS: путь к файлу, без имени саммого файла, только директории)
public class MethodReferenceExpression extends ExpressionNode {
	private		final	TypeReferenceExpression	parentExpr;
	private				String					name;
	protected			MethodSymbol			mSymbol;
	private				Scope					scope;
	
	public MethodReferenceExpression(Instance inst, TokenBuffer tb, TypeReferenceExpression parentExpr, String name) {
		super(inst, tb);
		
		this.parentExpr = parentExpr;
		this.name = name;
		this.type = VarType.fromClassName(VarType.CLASSNAME_NATIVE_CALLBACK);
	}

	@Override
	public String getQualifiedPath() {
		return (null!=parentExpr ? parentExpr.getQualifiedPath() + "::": "") + name;
	}

	@Override
	public boolean preAnalyze() {
		boolean result = true;
		debugAST(this, PRE, true, getFullInfo());
		
		if(null==name || name.isEmpty()) {
			markError("TODO Empty name");
			result = false;
		}
		else {
			if(null!=parentExpr) {
				result&=parentExpr.preAnalyze();
			}
		}
		
		debugAST(this, PRE, false, result, getFullInfo());
		return result;
	}

	@Override
	public boolean declare(Scope scope) {
		boolean result = true;
		debugAST(this, DECLARE, true, getFullInfo());

		try {
			if(null!=parentExpr) {
				result&=parentExpr.declare(scope);
				if(null!=parentExpr.getScope()) {
					mSymbol = parentExpr.getScope().resolveMethod(name, new VarType[]{VarType.BYTE}, true, new HashSet<>());
				}
			}
			else {
				mSymbol = scope.getThis().resolveMethod(name, new VarType[]{VarType.BYTE}, false, new HashSet<>());
			}
		}
		catch(CompileException ex) {
			markError(ex);
			result = false;
		}

		if(null==mSymbol) {
			declarationPendingNodes.put(this, scope);
		}
		else {
			declarationPendingNodes.remove(this);
		}

		debugAST(this, DECLARE, false, result, getFullInfo() + (declarationPendingNodes.containsKey(this) ? " [DP]" : ""));
		return result;
	}
	
	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg, CGScope parent) {
		boolean result = true;
		debugAST(this, POST, true, getFullInfo() + " type:" + type);
		this.scope = scope;
		cgScope = cg.enterExpression(parent, sp, cgScope, toString());
		
		if(!(parent instanceof CGMethodCallExpressionScope)) {
			markError("Method reference '::' is only allowed as an argument in a method call");
			result = false;
		}

		if(null!=parentExpr) {
			result&=parentExpr.postAnalyze(scope, cg, parent);
		}
		
		debugAST(this, POST, false, result, getFullInfo());
		return result;
	}

	public String getLastId() {
		return name;
	}
	
	@Override
	public Object codeGen(CodeGenerator cg, boolean toAccum, CGExcs excs) throws CompileException {
		depCodeGen(cg, mSymbol, scope, excs);
		
		cg.pushMethodReference(cgScope, ((CGMethodScope)mSymbol.getCGScope()).getStartLabel());

		releaseVars(cg);
		return null;
	}
	
	@Override
	public String toString() {
		return (null==parentExpr ? "" : parentExpr + "::") + name;
	}
	
	@Override
	public String getFullInfo() {
		return getClass().getSimpleName() + " " + (null==parentExpr ? "" : parentExpr + ".") + name;
	}
}