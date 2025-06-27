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

import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.compiler.nodes.BlockNode;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.compiler.Delimiter;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.ParseException;
import ru.vm5277.common.exceptions.SemanticException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.expressions.LiteralExpression;
import ru.vm5277.compiler.semantic.BlockScope;
import ru.vm5277.compiler.semantic.Scope;

public class WhileNode extends CommandNode {
	private	ExpressionNode	condition;
	private	BlockNode		body;
	private	BlockScope		blockScope;

	public WhileNode(TokenBuffer tb, MessageContainer mc) {
		super(tb, mc);

		consumeToken(tb); // Потребляем "while"
		try {consumeToken(tb, Delimiter.LEFT_PAREN);} catch(ParseException e) {markFirstError(e);}
		try {this.condition = new ExpressionNode(tb, mc).parse();} catch(ParseException e) {markFirstError(e);}
		try {consumeToken(tb, Delimiter.RIGHT_PAREN);} catch(ParseException e) {markFirstError(e);}

		tb.getLoopStack().add(this);
		try {
			body = tb.match(Delimiter.LEFT_BRACE) ? new BlockNode(tb, mc) : new BlockNode(tb, mc, parseStatement());
			blocks.add(body);
		}
		catch(ParseException e) {markFirstError(e);}
		tb.getLoopStack().remove(this);
	}

	public ExpressionNode getCondition() {
		return condition;
	}

	public BlockNode getBody() {
		return body;
	}

	@Override
	public String getNodeType() {
		return "while loop";
	}

	@Override
	public boolean preAnalyze() {
		// Проверка условия цикла
		if (null != condition) condition.preAnalyze();
		else markError("While condition cannot be null");

		if (null != body) body.preAnalyze();
		
		return true;
	}

	@Override
	public boolean declare(Scope scope) {
		// Объявление переменных условия (если нужно)
		if (null != condition) condition.declare(scope);

		// Создаем новую область видимости для тела цикла
		blockScope = new BlockScope(scope);
		// Объявляем элементы тела цикла
		if (null != body) body.declare(blockScope);

		return true;
	}

	@Override
	public boolean postAnalyze(Scope scope) {
		// Проверка типа условия
		if (null != condition) {
			if(condition.postAnalyze(scope)) {
				try {
					VarType condType = condition.getType(scope);
					if (VarType.BOOL != condType) markError("While condition must be boolean, got: " + condType);
				}
				catch (SemanticException e) {markError(e);}
			}
		}

		// Анализ тела цикла
		if (null != body) body.postAnalyze(blockScope);
		
		// Проверяем бесконечный цикл с возвратом
		if (condition instanceof LiteralExpression && Boolean.TRUE.equals(((LiteralExpression)condition).getValue()) &&	isControlFlowInterrupted(body)) {
			markWarning("Code after infinite while loop is unreachable");
		}

		return true;
	}
	
	@Override
	public void codeGen(CodeGenerator cg) throws Exception {
		int condBlockId = cg.enterBlock();
		condition.codeGen(cg);
		cg.leaveBlock();

		int bodyBlockId = cg.enterBlock();
		body.codeGen(cg);
		cg.leaveBlock();

		cg.eWhile(condBlockId, bodyBlockId);
	}
}