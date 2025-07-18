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

import ru.vm5277.compiler.nodes.BlockNode;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.compiler.Delimiter;
import ru.vm5277.compiler.Keyword;
import java.util.ArrayList;
import java.util.List;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.semantic.BlockScope;
import ru.vm5277.compiler.semantic.Scope;

public class SwitchNode extends CommandNode {
	private	ExpressionNode	expression;
	private	final	List<AstCase>			cases			= new ArrayList<>();
	private			BlockNode				defaultBlock	= null;
	private			BlockScope				switchScope;
	private			BlockScope				defaultScope;
	
	public SwitchNode(TokenBuffer tb, MessageContainer mc) {
		super(tb, mc);

		consumeToken(tb); // Потребляем "switch"
		// Парсим выражение switch
		try {consumeToken(tb, Delimiter.LEFT_PAREN);} catch(CompileException e) {markFirstError(e);}
		try {this.expression = new ExpressionNode(tb, mc).parse();} catch(CompileException e) {markFirstError(e);}
		try {consumeToken(tb, Delimiter.RIGHT_PAREN);} catch(CompileException e) {markFirstError(e);}
		
		try {consumeToken(tb, Delimiter.LEFT_BRACE);} catch(CompileException e) {markFirstError(e);}
		// Парсим case-блоки
		while (!tb.match(Delimiter.RIGHT_BRACE)) {
			if (tb.match(Keyword.CASE)) {
				AstCase c = parseCase(tb, mc);
				if(null != c) cases.add(c);
			}
			else if (tb.match(Keyword.DEFAULT)) {
				consumeToken(tb); // Потребляем "default"
				try {consumeToken(tb, Delimiter.COLON);} catch(CompileException e) {markFirstError(e);}
				tb.getLoopStack().add(this);
				try {defaultBlock = tb.match(Delimiter.LEFT_BRACE) ? new BlockNode(tb, mc) : new BlockNode(tb, mc, parseStatement());}
				catch(CompileException e) {markFirstError(e);}
				tb.getLoopStack().remove(this);
			}
			else {
				markFirstError(parserError("Expected 'case' or 'default' in switch statement"));
			}
		}
		try {consumeToken(tb, Delimiter.RIGHT_BRACE);}catch(CompileException e) {markFirstError(e);}
	}


	public ExpressionNode getExpression() {
		return expression;
	}

	public List<AstCase> getCases() {
		return cases;
	}

	public BlockNode getDefaultBlock() {
		return defaultBlock;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("switch (");
		sb.append(expression).append(") {\n");

		for (AstCase astCase : cases) {
			sb.append("case ").append(astCase.getFrom());
			if (null != astCase.getTo()) {
				sb.append("..").append(astCase.getTo());
			}
			sb.append(": ").append(astCase.getBlock()).append("\n");
		}

		if (null != defaultBlock) {
			sb.append("default: ").append(defaultBlock).append("\n");
		}

		sb.append("}");
		return sb.toString();
	}
	
	@Override
	public String getNodeType() {
		return "switch command";
	}

	@Override
	public boolean preAnalyze() {
		// Проверка выражения switch
		if (null != expression) expression.preAnalyze();
		else markError("Switch expression cannot be null");

		// Проверка всех case-блоков
		for (AstCase c : cases) {
			if (null != c.getBlock()) c.getBlock().preAnalyze();
		}

		// Проверка default-блока
		if (null != defaultBlock) defaultBlock.preAnalyze();
		
		return true;
	}

	@Override
	public boolean declare(Scope scope) {
		// Объявление выражения switch
		if (expression != null) expression.declare(scope);

		// Создаем новую область видимости для switch
		switchScope = new BlockScope(scope);

		// Объявление всех case-блоков
		for (AstCase c : cases) {
			if (null != c.getBlock()) {
				BlockScope caseScope = new BlockScope(switchScope);
				c.getBlock().declare(caseScope);
				c.setScope(caseScope);
			}
		}

		// Объявление default-блока
		if (null != defaultBlock) {
			defaultScope = new BlockScope(switchScope);
			defaultBlock.declare(defaultScope);
		}
		
		return true;
	}

	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		boolean allCasesReturn = true;		
		
		// Проверка типа выражения switch
		if (expression != null) {
			if (expression.postAnalyze(scope, cg)) {
				try {
					VarType exprType = expression.getType(scope);
					if (!exprType.isInteger() && VarType.BYTE != exprType && VarType.SHORT != exprType) {
						markError("Switch expression must be integer type, got: " + exprType);
					}
				}
				catch (CompileException e) {markError(e);}
			}
		}

		// Проверка case-значений на уникальность
		List<Long> caseValues = new ArrayList<>();
		for (AstCase astCase : cases) {
			// Проверка диапазона
			if (null != astCase.getTo() && astCase.getFrom() > astCase.getTo()) {
				markError("Invalid case range: " + astCase.getFrom() + ".." + astCase.getTo());
			}

			// Проверка на дубликаты
			if (null == astCase.getTo()) {
				if (caseValues.contains(astCase.getFrom())) markError("Duplicate case value: " + astCase.getFrom());
				else caseValues.add(astCase.getFrom());
			}
			else {
				for (long i = astCase.getFrom(); i <= astCase.getTo(); i++) {
					if (caseValues.contains(i)) markError("Duplicate case value in range: " + i);
					else caseValues.add(i);
				}
			}

			// Анализ блока case
			if (null != astCase.getBlock()) {
				astCase.getBlock().postAnalyze(astCase.getScope(), cg);
				if (!isControlFlowInterrupted(astCase.getBlock())) {
					allCasesReturn = false;
				}
			}
		}

		// Анализ default-блока (если есть)
		if (null != defaultBlock) defaultBlock.postAnalyze(defaultScope, cg);

		if (allCasesReturn && !cases.isEmpty()) {
			markWarning("Code after switch statement may be unreachable");
		}
		
		return true;
	}

	@Override
	public List<AstNode> getChildren() {
		List<AstNode> result = new ArrayList<>(cases);
		if(null != defaultBlock) result.add(defaultBlock);
		return result;
	}
}