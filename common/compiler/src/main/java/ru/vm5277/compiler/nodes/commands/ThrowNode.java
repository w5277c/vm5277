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
package ru.vm5277.compiler.nodes.commands;

import java.util.List;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.compiler.Delimiter;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.semantic.Scope;

public class ThrowNode extends CommandNode {
	private	ExpressionNode	exceptionExpr;

	public ThrowNode(TokenBuffer tb, MessageContainer mc) {
		super(tb, mc);

		consumeToken(tb); // Потребляем "throw"

		// Парсим выражение исключения
		try {this.exceptionExpr = new ExpressionNode(tb, mc).parse();} catch (CompileException e) {markFirstError(e);}

		// Потребляем точку с запятой
		try {consumeToken(tb, Delimiter.SEMICOLON);} catch (CompileException e) {markFirstError(e);}
	}

	public ExpressionNode getExceptionExpr() {
		return exceptionExpr;
	}

	@Override
	public boolean preAnalyze() {
		if (null != exceptionExpr) {exceptionExpr.preAnalyze();}
		else markError("Exception expression cannot be null");
		
		return true;
	}

	@Override
	public boolean declare(Scope scope) {
		if (null != exceptionExpr) exceptionExpr.declare(scope);

		return true;
	}

	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		if (null != exceptionExpr) {
			if (exceptionExpr.postAnalyze(scope, cg)) {
				VarType exprType = exceptionExpr.getType();
				// Проверяем, что выражение имеет тип byte (код ошибки)
				if (VarType.BYTE != exprType) {
					markError("Throw expression must be of type byte, got: " + exprType);
				}
			}
		}
		return true;
	}
	
	@Override
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean toAccum) throws CompileException {
		if(cgDone) return null;
		cgDone = true;

		exceptionExpr.codeGen(cg, null, false);
		cg.eThrow();
		
		return null;
	}

	@Override
	public List<AstNode> getChildren() {
		return null;
	}
}