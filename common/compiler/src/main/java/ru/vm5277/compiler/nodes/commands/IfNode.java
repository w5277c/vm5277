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
import ru.vm5277.compiler.Keyword;
import ru.vm5277.compiler.TokenType;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.ParseException;
import ru.vm5277.common.exceptions.SemanticException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.expressions.InstanceOfExpression;
import ru.vm5277.compiler.nodes.expressions.LiteralExpression;
import ru.vm5277.compiler.semantic.BlockScope;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.compiler.semantic.Symbol;

public class IfNode extends CommandNode {
    private	ExpressionNode	condition;
	private	BlockScope		thenScope;
	private	BlockScope		elseScope;
	private	String			varName;
	private	boolean			alwaysTrue;
	private	boolean			alwaysFalse;
	
	public IfNode(TokenBuffer tb, MessageContainer mc) {
		super(tb, mc);
		
        consumeToken(tb); // Потребляем "if"
		// Условие
		try {consumeToken(tb, Delimiter.LEFT_PAREN);} catch(ParseException e){markFirstError(e);}
		try {this.condition = new ExpressionNode(tb, mc).parse();} catch(ParseException e) {markFirstError(e);}
		// Парсинг условия (обычное или pattern matching)
		if (tb.match(Keyword.AS)) {
			tb.consume(); // Потребляем "as"
			// Проверяем, что после типа идет идентификатор
			if (tb.match(TokenType.ID)) {
				this.varName = consumeToken(tb).getStringValue();
			}
			else {
				markError("Expected variable name after type in pattern matching");
			}
		}
		try {consumeToken(tb, Delimiter.RIGHT_PAREN);} catch(ParseException e){markFirstError(e);}

		// Then блок
		tb.getLoopStack().add(this);
		try {blocks.add(tb.match(Delimiter.LEFT_BRACE) ? new BlockNode(tb, mc) : new BlockNode(tb, mc, parseStatement()));}
		catch(ParseException e) {markFirstError(e);}
		tb.getLoopStack().remove(this);

		// Else блок
        if (tb.match(Keyword.ELSE)) {
			consumeToken(tb);
        
			if (tb.match(TokenType.COMMAND, Keyword.IF)) {
				// Обработка else if
				tb.getLoopStack().add(this);
				blocks.add(new BlockNode(tb, mc, new IfNode(tb, mc)));
				tb.getLoopStack().remove(this);
			}
			else {
				tb.getLoopStack().add(this);
				try {blocks.add(tb.match(Delimiter.LEFT_BRACE) ? new BlockNode(tb, mc) : new BlockNode(tb, mc, parseStatement()));}
				catch(ParseException e) {markFirstError(e);}
				tb.getLoopStack().remove(this);
			}
		}
	}

    // Геттеры
    public ExpressionNode getCondition() {
        return condition;
    }

    public BlockNode getThenBlock() {
        return blocks.get(0);
    }

    public BlockNode getElseBlock() {
        return (0x02 == blocks.size() ? blocks.get(1) : null);
    }
	
	public String getVarName() {
		return varName;
	}

	public boolean alwaysTrue() {
		return alwaysTrue;
	}
	public boolean alwaysFalse() {
		return alwaysFalse;
	}

	@Override
	public String getNodeType() {
		return "if condition";
	}

	@Override
	public boolean preAnalyze() {
		if (condition != null) condition.preAnalyze();
		else markError("If condition cannot be null");

		// Проверка что есть хотя бы один блок
		if (null == getThenBlock() && null == getElseBlock()) markError("If statement must have at least one block (then or else)");
		
		// Проверка then-блока
		if (null != getThenBlock()) getThenBlock().preAnalyze();

		// Проверка else-блока (если есть)
		if (null != getElseBlock()) getElseBlock().preAnalyze();
		
		return true;
	}

	@Override
	public boolean declare(Scope scope) {
		// Объявление переменных условия
		if (null != condition) condition.declare(scope);
		
		// Объявление then-блока в новой области видимости
		if (null != getThenBlock()) {
			thenScope = new BlockScope(scope);
		}
		
		// Для pattern matching: объявляем новую переменную в then-блоке
		if (null != varName) {
			if(condition instanceof InstanceOfExpression) {
				InstanceOfExpression instanceOf = (InstanceOfExpression) condition;
				try {
					VarType type = instanceOf.getTypeExpr().getType(scope);
					if (null != type && null != thenScope) {
						thenScope.addLocal(new Symbol(varName, type, false, false));
					}
				}
				catch (SemanticException e) {markError(e);
				}
			}
			else {
				markError("Pattern matching requires 'is' check before 'as'");
			}
		}		

		if(null != thenScope) {
			getThenBlock().declare(thenScope);
		}

		// Объявление else-блока в новой области видимости
		if (null != getElseBlock()) {
			elseScope = new BlockScope(scope);
			getElseBlock().declare(elseScope);
		}
		
		return true;
	}

	@Override
	public boolean postAnalyze(Scope scope) {
		// Проверка типа условия
		if (null != condition) {
			if (condition.postAnalyze(scope)) {
				try {
					VarType condType = condition.getType(scope);
					if (VarType.BOOL != condType) markError("If condition must be boolean, got: " + condType);
				}
				catch (SemanticException e) {markError(e);}
			}
		}

		// Анализ then-блока
		if (null != getThenBlock()) getThenBlock().postAnalyze(thenScope);

		// Анализ else-блока
		if (null != getElseBlock()) getElseBlock().postAnalyze(elseScope);
		
		// Проверяем недостижимый код после if с возвратом во всех ветках
        if (null != getElseBlock() && isControlFlowInterrupted(getThenBlock()) && isControlFlowInterrupted(getElseBlock())) {
            markWarning("Code after if statement may be unreachable");
        }
		
		try {
			condition = condition.optimizeWithScope(scope);
			if(condition instanceof LiteralExpression) {
				LiteralExpression le = (LiteralExpression)condition;
				if(VarType.BOOL == le.getType(scope)) {
					if((boolean)le.getValue()) {
						alwaysTrue = true;
					}
					else {
						alwaysFalse = true;
					}
				}
			}
		}
		catch (ParseException e) {
			markFirstError(e);
		}
		
		return true;
	}
	
	@Override
	public void codeGen(CodeGenerator cg) throws Exception {
		if(alwaysTrue) {
			cg.enterBlock();
			getThenBlock().codeGen(cg);
			cg.leaveBlock();
			return;
		}
		if(alwaysFalse()) {
			if (getElseBlock() != null) {
				cg.enterBlock();
				getElseBlock().codeGen(cg);
				cg.leaveBlock();
			}
			return;
		}
		
		int condBlockId = cg.enterBlock();
		condition.codeGen(cg);
		cg.leaveBlock();
		
		int thenBlockId = cg.enterBlock();
		getThenBlock().codeGen(cg);
		cg.leaveBlock();

		Integer elseBlockId = null;
		if(null != getElseBlock()) {
			elseBlockId = cg.enterBlock();
			getElseBlock().codeGen(cg);
			cg.leaveBlock();
		}
		
		cg.eIf(condBlockId, thenBlockId, elseBlockId);
	}
}
