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
import java.util.Set;
import ru.vm5277.common.cg.CGBranch;
import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.scopes.CGBlockScope;
import ru.vm5277.common.cg.scopes.CGLoopBlockScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.compiler.CodegenResult;
import ru.vm5277.compiler.nodes.BlockNode;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.compiler.Delimiter;
import ru.vm5277.compiler.Keyword;
import ru.vm5277.compiler.TokenType;
import ru.vm5277.common.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.nodes.expressions.LiteralExpression;
import ru.vm5277.compiler.semantic.BlockScope;
import ru.vm5277.compiler.semantic.Scope;

public class DoWhileNode extends CommandNode {
	private	ExpressionNode	condition;
	private	BlockNode		blockNode;
	private	BlockScope		blockScope;
	private	CGBranch		branch		= new CGBranch();
	private	boolean			alwaysTrue;
	private	boolean			alwaysFalse;

	public DoWhileNode(TokenBuffer tb, MessageContainer mc) {
		super(tb, mc);

		consumeToken(tb);
		// Тело цикла
		try {
			blockNode = tb.match(Delimiter.LEFT_BRACE) ? new BlockNode(tb, mc) : new BlockNode(tb, mc, parseStatement());
		}
		catch(CompileException e) {markFirstError(e);}

		try {
			consumeToken(tb, TokenType.COMMAND, Keyword.WHILE);
			consumeToken(tb, Delimiter.LEFT_PAREN);
			this.condition = new ExpressionNode(tb, mc).parse();
			consumeToken(tb, Delimiter.RIGHT_PAREN);
		}
		catch(CompileException e) {
			markFirstError(e);
		}

		try {consumeToken(tb, Delimiter.SEMICOLON);}catch(CompileException e) {markFirstError(e);}
	}

	public ExpressionNode getCondition() {
		return condition;
	}

	public boolean isAlwaysFalse() {
		return alwaysFalse;
	}
	
	@Override
	public boolean preAnalyze() {
		boolean result = true;		
		
		result&=blockNode.preAnalyze();
		

		if(result && null!=condition) {
			result&=condition.preAnalyze();
		}

		return result;
	}

	@Override
	public boolean declare(Scope scope) {
		boolean result = true;
		
		// Создаем новую область видимости для тела цикла
		blockScope = new BlockScope(scope);

		// Объявляем элементы тела цикла
		result&=blockNode.declare(blockScope);

		// Объявляем переменные условия (в той же области, что и тело)
		if(result && null!=condition) {
			result&=condition.declare(blockScope);
		}

		return result;
	}

	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		boolean result = true;
		cgScope = cg.enterLoopBlock();

		// Анализ тела цикла
		result&=blockNode.postAnalyze(blockScope, cg);

		if(result) {
			result&=condition.postAnalyze(blockScope, cg);
		}
		
		if(result) {
			try {
				ExpressionNode optimizedExpr = condition.optimizeWithScope(blockScope, cg);
				if(null != optimizedExpr) {
					condition = optimizedExpr;
					result&=condition.postAnalyze(blockScope, cg);
				}
			}
			catch (CompileException e) {
				markFirstError(e);
				result = false;
			}
		}
		
		if(result) {
			VarType condType = condition.getType();
			if(null==condType || VarType.BOOL!=condType) {
				markError("Loop condition must be boolean, got: " + condType);
				result = false;
			}
		}

		if(result) {
			// Проверяем бесконечный цикл с возвратом
			if (condition instanceof LiteralExpression && Boolean.TRUE.equals(((LiteralExpression)condition).getValue()) &&
				isControlFlowInterrupted(blockNode)) {
				
				markWarning("Code after infinite while loop is unreachable");
			}
		}
		
		cg.leaveLoopBlock();
		return result;
	}
	
	@Override
	public void codeOptimization(Scope scope, CodeGenerator cg) {
		CGScope oldScope = cg.setScope(cgScope);
		
		condition.codeOptimization(scope, cg);
		if(null!=blockNode) {
			blockNode.codeOptimization(scope, cg);
		}
		
		if(condition instanceof LiteralExpression) {
			LiteralExpression le = (LiteralExpression)condition;
			if(VarType.BOOL==le.getType()) {
				if((boolean)le.getValue()) {
					alwaysTrue = true;
				}
				else {
					alwaysFalse = true;
				}
			}
		}

		cg.setScope(oldScope);
	}

	@Override
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean toAccum, CGExcs excs) throws CompileException {
		if(cgDone) return null;
		cgDone = true;

		CodegenResult result = null;
		CGScope cgs = null == parent ? cgScope : parent;
		cgs.setBranch(branch);
		
		if(!alwaysFalse) {
			cgs.append(((CGLoopBlockScope)cgScope).getStartLbScope());
			blockNode.codeGen(cg, cgs, false, excs);
		}

		if(!alwaysFalse && !alwaysTrue) {
			condition.codeGen(cg, cgs, false, excs);
		}

		if(!alwaysFalse) {
			cg.jump(cgs, ((CGLoopBlockScope)cgScope).getStartLbScope());
		}
		if(!alwaysFalse) {
			cgs.append(branch.getEnd());
		}

		cgs.append(((CGLoopBlockScope)cgScope).getEndLbScope());
		
		((CGBlockScope)cgScope).build(cg, false, excs);
		((CGBlockScope)cgScope).restoreRegsPool();
		return result;
	}

	@Override
	public List<AstNode> getChildren() {
		return Arrays.asList(blockNode);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("do ");
		sb.append(blockNode);
		sb.append(" while (");
		sb.append(condition);
		sb.append(");");
		return sb.toString();
	}
}