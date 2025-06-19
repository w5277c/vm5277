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

import ru.vm5277.common.compiler.CodeGenerator;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.compiler.Delimiter;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.ParseException;
import ru.vm5277.common.exceptions.SemanticException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.semantic.Scope;

public class ThrowNode extends CommandNode {
	private	ExpressionNode	exceptionExpr;

	public ThrowNode(TokenBuffer tb, MessageContainer mc) {
		super(tb, mc);

		consumeToken(tb); // Потребляем "throw"

		// Парсим выражение исключения
		try {this.exceptionExpr = new ExpressionNode(tb, mc).parse();} catch (ParseException e) {markFirstError(e);}

		// Потребляем точку с запятой
		try {consumeToken(tb, Delimiter.SEMICOLON);} catch (ParseException e) {markFirstError(e);}
	}

	public ExpressionNode getExceptionExpr() {
		return exceptionExpr;
	}

	@Override
	public String getNodeType() {
		return "throw command";
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
	public boolean postAnalyze(Scope scope) {
		if (null != exceptionExpr) {
			if (exceptionExpr.postAnalyze(scope)) {
				try {
					VarType exprType = exceptionExpr.getType(scope);
					// Проверяем, что выражение имеет тип byte (код ошибки)
					if (VarType.BYTE != exprType) markError("Throw expression must be of type byte, got: " + exprType);
				}
				catch (SemanticException e) {markError(e);}
			}
		}
		return true;
	}
	
	@Override
	public void codeGen(CodeGenerator cg) throws Exception {
		exceptionExpr.codeGen(cg);
		cg.eThrow();
	}
}