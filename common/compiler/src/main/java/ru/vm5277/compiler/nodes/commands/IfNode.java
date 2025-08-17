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

import java.util.Arrays;
import java.util.List;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.scopes.CGBlockScope;
import ru.vm5277.compiler.nodes.BlockNode;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.compiler.Delimiter;
import ru.vm5277.compiler.Keyword;
import ru.vm5277.compiler.TokenType;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.nodes.expressions.InstanceOfExpression;
import ru.vm5277.compiler.nodes.expressions.LiteralExpression;
import ru.vm5277.compiler.semantic.BlockScope;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.compiler.semantic.Symbol;

public class IfNode extends CommandNode {
    private	ExpressionNode	condition;
	private	BlockNode		blockNode;
	private	BlockNode		elseBlockNode;
	
	private	BlockScope		thenScope;
	private	BlockScope		elseScope;
	private	String			varName;
	private	boolean			alwaysTrue;
	private	boolean			alwaysFalse;
	
	public IfNode(TokenBuffer tb, MessageContainer mc) {
		super(tb, mc);
		
        consumeToken(tb); // Потребляем "if"
		// Условие
		try {consumeToken(tb, Delimiter.LEFT_PAREN);} catch(CompileException e){markFirstError(e);}
		try {this.condition = new ExpressionNode(tb, mc).parse();} catch(CompileException e) {markFirstError(e);}
		try {consumeToken(tb, Delimiter.RIGHT_PAREN);} catch(CompileException e){markFirstError(e);}

		// Then блок
		tb.getLoopStack().add(this);
		try {
			blockNode = tb.match(Delimiter.LEFT_BRACE) ? new BlockNode(tb, mc) : new BlockNode(tb, mc, parseStatement());
		}
		catch(CompileException e) {markFirstError(e);}
		tb.getLoopStack().remove(this);

		// Else блок
        if (tb.match(Keyword.ELSE)) {
			consumeToken(tb);
        
			if (tb.match(TokenType.COMMAND, Keyword.IF)) {
				// Обработка else if
				tb.getLoopStack().add(this);
				elseBlockNode = new BlockNode(tb, mc, new IfNode(tb, mc));
				tb.getLoopStack().remove(this);
			}
			else {
				tb.getLoopStack().add(this);
				try {
					elseBlockNode = tb.match(Delimiter.LEFT_BRACE) ? new BlockNode(tb, mc) : new BlockNode(tb, mc, parseStatement());
				}
				catch(CompileException e) {markFirstError(e);}
				tb.getLoopStack().remove(this);
			}
		}
	}

    // Геттеры
    public ExpressionNode getCondition() {
        return condition;
    }

    public BlockNode getThenBlock() {
        return blockNode;
    }

    public BlockNode getElseBlock() {
        return elseBlockNode;
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
					VarType type;
					if(instanceOf.getTypeExpr() instanceof LiteralExpression && ((LiteralExpression)instanceOf.getTypeExpr()).getValue() instanceof VarType) {
						type = (VarType)((LiteralExpression)instanceOf.getTypeExpr()).getValue();
					}
					else {
						type = instanceOf.getTypeExpr().getType(scope);
					}
					if (null != type && null != thenScope) {
						thenScope.addVariable(new Symbol(varName, type, false, false));
						
					}
				}
				catch (CompileException e) {markError(e);
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
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		// Проверка типа условия
		if (null != condition) {
			if (condition.postAnalyze(scope, cg)) {
				try {
					VarType condType = condition.getType(scope);
					if (VarType.BOOL != condType) markError("If condition must be boolean, got: " + condType);
				}
				catch (CompileException e) {markError(e);}
			}
		}

		// Анализ then-блока
		if (null != getThenBlock()) getThenBlock().postAnalyze(thenScope, cg);

		// Анализ else-блока
		if (null != getElseBlock()) getElseBlock().postAnalyze(elseScope, cg);
		
		// Проверяем недостижимый код после if с возвратом во всех ветках
        if (null != getElseBlock() && isControlFlowInterrupted(getThenBlock()) && isControlFlowInterrupted(getElseBlock())) {
            markWarning("Code after if statement may be unreachable");
        }
		
		try {
			ExpressionNode optimizedExpr = condition.optimizeWithScope(scope);
			if(null != optimizedExpr) {
				condition = optimizedExpr;
			}
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
		catch (CompileException e) {
			markFirstError(e);
		}
		
		return true;
	}
	
	@Override
	public Object codeGen(CodeGenerator cg) throws Exception {
		if(cgDone) return null;
		cgDone = true;

		if(alwaysTrue) {
			cg.enterBlock(cg.getScope());
			getThenBlock().codeGen(cg);
			cg.leaveBlock();
			return null;
		}
		if(alwaysFalse()) {
			if (getElseBlock() != null) {
				cg.enterBlock(cg.getScope());
				getElseBlock().codeGen(cg);
				cg.leaveBlock();
			}
			return null;
		}
		
		CGBlockScope condBlockScope = cg.enterBlock(cg.getScope());
		Object obj = condition.codeGen(cg);
		cg.leaveBlock();
		
		/* TODO Рудимент. Сейчас codeGen возвращает true, если результат операции содержится в аккумуляторе. Поломал логику?
		//Если результат стал известен без кодогенерации
		if(obj instanceof Boolean) {
			if(((Boolean)obj)) {
				cg.enterBlock(cg.getScope());
				getThenBlock().codeGen(cg);
				cg.leaveBlock();
			}
			else if(getElseBlock() != null) {
				cg.enterBlock(cg.getScope());
				getElseBlock().codeGen(cg);
				cg.leaveBlock();
			}
			return null;
		}
		*/
		CGBlockScope thenBlockScope = cg.enterBlock(cg.getScope());
		getThenBlock().codeGen(cg);
		cg.leaveBlock();

		CGBlockScope elseBlockScope = null;
		if(null != getElseBlock()) {
			elseBlockScope = cg.enterBlock(cg.getScope());
			getElseBlock().codeGen(cg);
			cg.leaveBlock();
		}
		
		cg.eIf(condBlockScope, thenBlockScope, elseBlockScope);
		
		return null;
	}

	@Override
	public List<AstNode> getChildren() {
		if(null == elseBlockNode) {
			return Arrays.asList(blockNode);
		}
		return Arrays.asList(blockNode, elseBlockNode);
	}
}
