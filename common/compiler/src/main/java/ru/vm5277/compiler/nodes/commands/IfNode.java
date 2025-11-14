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
import ru.vm5277.common.cg.scopes.CGLabelScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.compiler.CodegenResult;
import ru.vm5277.compiler.nodes.BlockNode;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.compiler.Delimiter;
import ru.vm5277.compiler.Keyword;
import ru.vm5277.compiler.TokenType;
import ru.vm5277.common.compiler.VarType;
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
		try {this.condition = new ExpressionNode(tb, mc).parse();} catch(CompileException e) {markFirstError(e);}
		try {consumeToken(tb, Delimiter.RIGHT_PAREN);} catch(CompileException e){markFirstError(e);}

		// Then блок
		tb.getLoopStack().add(this);
		try {
			thenBlockNode = tb.match(Delimiter.LEFT_BRACE) ? new BlockNode(tb, mc) : new BlockNode(tb, mc, parseStatement());
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
		if (condition != null) condition.preAnalyze();
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
		if (null != condition) condition.declare(scope);
		
		// Объявление then-блока в новой области видимости
		if (null != thenBlockNode) {
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
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		boolean result = true;
		cgScope = cg.enterCommand();
		
		//TODO привести в соответствие как в ForNode
		// Проверка типа условия
		if(null!=condition) {
			result&=condition.postAnalyze(scope, cg);
		}

		if(result) {
			try {
				ExpressionNode optimizedExpr = condition.optimizeWithScope(scope, cg);
				if(null!=optimizedExpr) {
					condition = optimizedExpr;
					result&=condition.postAnalyze(scope, cg);
				}
			}
			catch (CompileException e) {
				markFirstError(e);
				result = false;
			}
		}		

		if(result) {
			VarType condType = condition.getType();
			if(VarType.BOOL!=condType) {
				markError("If condition must be boolean, got: " + condType);
				result = false;
			}
		}

		if(result) {
			if(condition instanceof LiteralExpression) {
				LiteralExpression le = (LiteralExpression)condition;
				if(VarType.BOOL == le.getType()) {
					if((boolean)le.getValue()) {
						alwaysTrue = true;
					}
					else {
						alwaysFalse = true;
					}
				}
			}
		}

		if(result) {
			// Анализ then-блока
			if(null!=thenBlockNode) {
				result&=thenBlockNode.postAnalyze(thenScope, cg);
			}
		}

		if(result) {
			// Анализ else-блока
			if(null!=elseBlockNode) {
				result&=elseBlockNode.postAnalyze(elseScope, cg);
			}
		}
		
		if(result) {
			// Проверяем недостижимый код после if с возвратом во всех ветках
			if(null!=elseBlockNode && isControlFlowInterrupted(thenBlockNode) && isControlFlowInterrupted(elseBlockNode)) {
				markWarning("Code after if statement may be unreachable");
	        }
		}
		
		cg.leaveCommand();
		return result;
	}
	
	@Override
	public void codeOptimization(Scope scope, CodeGenerator cg) {
		CGScope oldScope = cg.setScope(cgScope);
		
		condition.codeOptimization(scope, cg);
		if(null!=thenBlockNode) {
			thenBlockNode.codeOptimization(scope, cg);
		}
		if(null!=elseBlockNode) {
			elseBlockNode.codeOptimization(scope, cg);
		}
		
		cg.setScope(oldScope);
	}
	
	@Override
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean toAccum) throws CompileException {
		if(cgDone) return null;
		cgDone = true;

		CGScope cgs = null == parent ? cgScope : parent;
		cgs.setBranch(branch);
		
		if(alwaysTrue) {
			thenBlockNode.codeGen(cg, cgs, false);
			return null;
		}
		if(alwaysFalse) {
			if(elseBlockNode != null) {
				elseBlockNode.codeGen(cg, cgs, false);
			}
			return null;
		}
		
		Object obj = condition.codeGen(cg, cgs, true);
		
		//Если результат стал известен без runtime
		if(obj == CodegenResult.TRUE) {
			thenBlockNode.codeGen(cg, cgs, false);
			return null;
		}
		else if(obj == CodegenResult.FALSE) {
			if(elseBlockNode != null) {
				elseBlockNode.codeGen(cg, cgs, false);
			}
			return null;
		}
		//TODO оптимизировать на прямую проверку(без участия аккумулятора)
		else if(obj==CodegenResult.RESULT_IN_ACCUM) {
			cg.boolCond(cgs, branch, (VarType.BOOL==condition.getType()));
		}
		
		CGLabelScope endScope = new CGLabelScope(null, CGScope.genId(), LabelNames.COMPARE_END, true);
		
		thenBlockNode.codeGen(cg, cgs, false);
		if(null!=elseBlockNode) {
			cg.jump(cgs, endScope);
			cgs.append(branch.getEnd());
			elseBlockNode.codeGen(cg, cgs, false);
			cgs.append(endScope);
		}
		else {
			cgs.append(branch.getEnd());
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
