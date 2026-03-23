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
import ru.vm5277.common.LabelNames;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.CGBranch;
import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.common.cg.scopes.CGLabelScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.compiler.CodegenResult;
import ru.vm5277.compiler.nodes.BlockNode;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.common.lexer.Delimiter;
import ru.vm5277.common.lexer.J8BKeyword;
import ru.vm5277.common.lexer.TokenType;
import ru.vm5277.common.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.nodes.expressions.InstanceOfExpression;
import ru.vm5277.compiler.nodes.expressions.LiteralExpression;
import ru.vm5277.compiler.semantic.BlockScope;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.compiler.semantic.Symbol;

public class IfNode extends CommandNode {
    private	ExpressionNode	conditionExpr;
	private	BlockNode		thenBlockNode;
	private	BlockNode		elseBlockNode;
	
	private	BlockScope		thenScope;
	private	BlockScope		elseScope;
	private	String			varName;
	private	boolean			alwaysTrue;
	private	boolean			alwaysFalse;
	private	CGBranch		branch		= new CGBranch();
	
	
	public IfNode(TokenBuffer tb, MessageContainer mc) {
		super(tb, mc);
		
        consumeToken(tb); // Потребляем "if"
		// Условие
		try {consumeToken(tb, Delimiter.LEFT_PAREN);} catch(CompileException e){markFirstError(e);}
		try {this.conditionExpr = new ExpressionNode(tb, mc).parse();} catch(CompileException e) {markFirstError(e);}
		try {consumeToken(tb, Delimiter.RIGHT_PAREN);} catch(CompileException e){markFirstError(e);}

		// Then блок
		try {
			thenBlockNode = tb.match(Delimiter.LEFT_BRACE) ? new BlockNode(tb, mc, "then") : new BlockNode(tb, mc, parseStatement(), "then");
		}
		catch(CompileException e) {markFirstError(e);}

		// Else блок
        if (tb.match(J8BKeyword.ELSE)) {
			consumeToken(tb);
        
			if (tb.match(TokenType.COMMAND, J8BKeyword.IF)) {
				// Обработка else if
				elseBlockNode = new BlockNode(tb, mc, new IfNode(tb, mc), "elseif");

			}
			else {
				try {
					elseBlockNode = tb.match(Delimiter.LEFT_BRACE) ? new BlockNode(tb, mc, "else") : new BlockNode(tb, mc, parseStatement(), "else");
				}
				catch(CompileException e) {markFirstError(e);}
			}
		}
	}

	// Геттеры
	public ExpressionNode getCondition() {
		return conditionExpr;
	}

	public BlockNode getThenBlock() {
		return thenBlockNode;
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
	public boolean preAnalyze() {
		if (conditionExpr != null) conditionExpr.preAnalyze();
		else markError("If condition cannot be null");

		// Проверка что есть хотя бы один блок
		if (null == thenBlockNode && null == elseBlockNode) markError("If statement must have at least one block (then or else)");
		
		// Проверка then-блока
		if (null != thenBlockNode) thenBlockNode.preAnalyze();

		// Проверка else-блока (если есть)
		if (null != elseBlockNode) elseBlockNode.preAnalyze();
		
		return true;
	}

	@Override
	public boolean declare(Scope scope) {
		// Объявление переменных условия
		if (null != conditionExpr) conditionExpr.declare(scope);
		
		// Объявление then-блока в новой области видимости
		if (null != thenBlockNode) {
			thenScope = new BlockScope(scope);
		}
		
		// Для pattern matching: объявляем новую переменную в then-блоке
		if (null != varName) {
			if(conditionExpr instanceof InstanceOfExpression) {
				InstanceOfExpression instanceOf = (InstanceOfExpression) conditionExpr;
				try {
					VarType type;
					if(instanceOf.getTypeExpr() instanceof LiteralExpression && ((LiteralExpression)instanceOf.getTypeExpr()).getValue() instanceof VarType) {
						type = (VarType)((LiteralExpression)instanceOf.getTypeExpr()).getValue();
					}
					else {
						type = instanceOf.getTypeExpr().getType();//TODO должно быть как минимум в postAnalyze
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
			thenBlockNode.declare(thenScope);
		}

		// Объявление else-блока в новой области видимости
		if (null != elseBlockNode) {
			elseScope = new BlockScope(scope);
			elseBlockNode.declare(elseScope);
		}
		
		return true;
	}

	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg, CGScope parent) {
		boolean result = true;
		cgScope = cg.enterCommand(parent, "if");

		if(null!=conditionExpr) {
			result&=conditionExpr.postAnalyze(scope, cg, cgScope);
			if(result) {
				// Резолвинг QualifiedPathExpression
				ExpressionNode resolved = resolveQualifiedPathExpr(conditionExpr);
				if(null!=resolved) {
					conditionExpr = resolved;
				}
			}
		}

		if(result) {
			VarType condType = conditionExpr.getType();
			if(VarType.BOOL!=condType) {
				markError("If condition must be boolean, got: " + condType);
				result = false;
			}
		}

		if(result) {
			// Анализ then-блока
			if(null!=thenBlockNode) {
				result&=thenBlockNode.postAnalyze(thenScope, cg, cgScope);
			}
		}

		if(result) {
			// Анализ else-блока
			if(null!=elseBlockNode) {
				result&=elseBlockNode.postAnalyze(elseScope, cg, cgScope);
			}
		}
		
		if(result) {
			// Проверяем недостижимый код после if с возвратом во всех ветках
			if(null!=elseBlockNode && isControlFlowInterrupted(thenBlockNode) && isControlFlowInterrupted(elseBlockNode)) {
				markWarning("Code after if statement may be unreachable");
	        }
		}
		
		return result;
	}
	
	@Override
	public void codeOptimization(Scope scope, CodeGenerator cg) {
		conditionExpr.codeOptimization(scope, cg);
		try {
			ExpressionNode optimizedExpr = conditionExpr.optimizeWithScope(scope, cg);
			if(null != optimizedExpr) {
				conditionExpr = optimizedExpr;
			}
		}
		catch(CompileException ex) {
			markError(ex);
		}

		if(null!=thenBlockNode) {
			thenBlockNode.codeOptimization(scope, cg);
		}
		if(null!=elseBlockNode) {
			elseBlockNode.codeOptimization(scope, cg);
		}
		
		if(conditionExpr instanceof LiteralExpression) {
			LiteralExpression le = (LiteralExpression)conditionExpr;
			if(VarType.BOOL==le.getType()) {
				if((boolean)le.getValue()) {
					alwaysTrue = true;
				}
				else {
					alwaysFalse = true;
				}
			}
		}
	}
	
	@Override
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean toAccum, CGExcs excs) throws CompileException {
		if(cgDone) return null;
		cgDone = true;

		//CGScope cgs = null == parent ? cgScope : parent;
		cgScope.setBranch(branch);
		
		if(alwaysTrue) {
			thenBlockNode.codeGen(cg, null, false, excs);
			return null;
		}
		if(alwaysFalse) {
			if(elseBlockNode != null) {
				elseBlockNode.codeGen(cg, null, false, excs);
			}
			return null;
		}
		
		Object obj = conditionExpr.codeGen(cg, null, true, excs);
		
		//Если результат стал известен без runtime
		if(obj == CodegenResult.TRUE) {
			thenBlockNode.codeGen(cg, null, false, excs);
			return null;
		}
		else if(obj == CodegenResult.FALSE) {
			if(elseBlockNode != null) {
				elseBlockNode.codeGen(cg, null, false, excs);
			}
			return null;
		}
		else if(obj==CodegenResult.RESULT_IN_ACCUM) {
			cg.boolAccCond(conditionExpr.getCGScope(), branch, (VarType.BOOL==conditionExpr.getType()));
		}
		else if(obj==CodegenResult.RESULT_IN_FLAG) {
			cg.boolFlagCond(conditionExpr.getCGScope(), branch);
		}
		
		CGLabelScope endScope = new CGLabelScope(null, CGScope.genId(), LabelNames.COMPARE_END, true);
		
		thenBlockNode.codeGen(cg, null, false, excs);
		if(null!=elseBlockNode) {
			cg.jump(thenBlockNode.getCGScope(), endScope);
			thenBlockNode.getCGScope().append(branch.getEnd());
			elseBlockNode.codeGen(cg, null, false, excs);
			elseBlockNode.getCGScope().append(endScope);
		}
		else {
			thenBlockNode.getCGScope().append(branch.getEnd());
		}
		
//		cg.eIf(cgs, branch, thenBlockNode.getCGScope(), null == elseBlockNode ? null : elseBlockNode.getCGScope());
		
		return null;
	}

	@Override
	public List<AstNode> getChildren() {
		if(null == elseBlockNode) {
			return Arrays.asList(thenBlockNode);
		}
		return Arrays.asList(thenBlockNode, elseBlockNode);
	}
}
