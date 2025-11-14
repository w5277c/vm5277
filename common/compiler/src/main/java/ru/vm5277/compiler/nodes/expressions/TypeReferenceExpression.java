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
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.semantic.ClassScope;
import ru.vm5277.compiler.semantic.Scope;
import static ru.vm5277.compiler.Main.debugAST;
import static ru.vm5277.common.SemanticAnalyzePhase.DECLARE;
import static ru.vm5277.common.SemanticAnalyzePhase.PRE;
import static ru.vm5277.common.SemanticAnalyzePhase.POST;
import ru.vm5277.common.SourcePosition;
import ru.vm5277.compiler.semantic.CIScope;
import ru.vm5277.compiler.semantic.EnumScope;

//Выражение описывающее доступ к типу которое может состоять из других типпов (по аналогии с FS: путь к файлу, без имени саммого файла, только директории)
public class TypeReferenceExpression extends ExpressionNode {
	private		final	TypeReferenceExpression	parentExpr;
	private				String					lastId;
	protected			CIScope					cis;
	
	public TypeReferenceExpression(TokenBuffer tb, MessageContainer mc, TypeReferenceExpression parentExpr, String lastId) {
		super(tb, mc);
		
		this.parentExpr = parentExpr;
		this.lastId = lastId;
	}

	// Для Enum
	public TypeReferenceExpression(TokenBuffer tb, MessageContainer mc, TypeReferenceExpression parentExpr, String lastId, VarType type) {
		super(tb, mc);
		
		this.parentExpr = parentExpr;
		this.lastId = lastId;
		this.type = type;
	}

	public TypeReferenceExpression(TokenBuffer tb, MessageContainer mc, TypeReferenceExpression parentExpr, String lastId, CIScope cis) {
		super(tb, mc);
		
		this.parentExpr = parentExpr;
		this.lastId = lastId;
		this.cis = cis;
		this.type = VarType.fromClassName(cis.getName());
	}

	public String getClassPath() {
		return lastId;
	}

	@Override
	public boolean preAnalyze() {
		boolean result = true;
		debugAST(this, PRE, true, getFullInfo());
		
		if(null==lastId || lastId.isEmpty()) {
			markError("TODO Empty last part");
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

		if(null!=parentExpr) {
			result&=parentExpr.declare(scope);
			if(null!=parentExpr.getScope()) {
				cis = parentExpr.getScope().resolveCI(lastId, true);
			}
		}
		else {
			cis = scope.getThis().resolveCI(lastId, false);
		}

		if(null==cis) {
			declarationPendingNodes.put(this, scope);
		}
		else {
			declarationPendingNodes.remove(this);

			if(!(cis instanceof ClassScope) && !(cis instanceof EnumScope)) {
				markError("Unexpected scope:" + cis + ", for:" + lastId);
				result = false;
			}
			else {
				type = VarType.fromClassName((((CIScope)cis).getName()));
			}
		}

		debugAST(this, DECLARE, false, result, getFullInfo() + (declarationPendingNodes.containsKey(this) ? " [DP]" : ""));
		return result;
	}
	
	public CIScope getScope() {
		return cis;
	}
	
	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		boolean result = true;
		debugAST(this, POST, true, getFullInfo() + " type:" + type);
		
		// Находим тип-класс, если он существует
		if(null!=type && !type.isClassType()) {
			markError("Type '" + lastId + "' not found");
			result = false;
		}

/*		if(null==type) {
			// Затем проверяем, является ли это интерфейсом
			InterfaceScope interfaceSymbol = scope.getThis().resolveInterface(lastId);
			if (null != interfaceSymbol) {
				type = VarType.addClassName(lastId);
			}
		}
*/		
		if(null!=parentExpr) {
			result&=parentExpr.postAnalyze(scope, cg);
		}
		
		debugAST(this, POST, false, result, getFullInfo());
		return result;
	}

	public String getLastId() {
		return lastId;
	}
	
	@Override
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean toAccum) throws CompileException {
		//cg.setAcc(new Operand(VarType.CLASS, OperandType.TYPE, varType.getId()));
		return null;
	}
	
	@Override
	public String toString() {
		return (null==parentExpr ? "" : parentExpr + ".") + lastId;
	}
	
	public String getFullInfo() {
		return getClass().getSimpleName() + " " + (null==parentExpr ? "" : parentExpr + ".") + lastId;
	}
}