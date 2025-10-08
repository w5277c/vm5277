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
import ru.vm5277.common.cg.scopes.CGBlockScope;
import ru.vm5277.common.cg.scopes.CGBranchScope;
import ru.vm5277.common.cg.scopes.CGLabelScope;
import ru.vm5277.common.cg.scopes.CGLoopBlockScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.compiler.CodegenResult;
import ru.vm5277.compiler.nodes.BlockNode;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.compiler.Delimiter;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.nodes.expressions.LiteralExpression;
import ru.vm5277.compiler.semantic.BlockScope;
import ru.vm5277.compiler.semantic.Scope;

public class WhileNode extends CommandNode {
	private	ExpressionNode	condition;
	private	BlockNode		blockNode;
	private	BlockScope		blockScope;
	private	CGBranchScope	brScope;
	private	boolean			alwaysTrue;
	private	boolean			alwaysFalse;
		
	public WhileNode(TokenBuffer tb, MessageContainer mc) {
		super(tb, mc);

		consumeToken(tb); // Потребляем "while"
		try {consumeToken(tb, Delimiter.LEFT_PAREN);} catch(CompileException e) {markFirstError(e);}
		try {this.condition = new ExpressionNode(tb, mc).parse();} catch(CompileException e) {markFirstError(e);}
		try {consumeToken(tb, Delimiter.RIGHT_PAREN);} catch(CompileException e) {markFirstError(e);}

		try {
			blockNode = tb.match(Delimiter.LEFT_BRACE) ? new BlockNode(tb, mc) : new BlockNode(tb, mc, parseStatement());
		}
		catch(CompileException e) {markFirstError(e);}
	}

	public ExpressionNode getCondition() {
		return condition;
	}

	public BlockNode getBody() {
		return blockNode;
	}

	@Override
	public String getNodeType() {
		return "while loop";
	}

	@Override
	public boolean preAnalyze() {
		boolean result = true;
		// Проверка условия цикла
		if(null!=condition) {
			result&=condition.preAnalyze();
		}
		else {
			markError("While condition cannot be null");
			result = false;
		}

		if(null!=blockNode) {
			tb.getLoopStack().add(this);
			result&=blockNode.preAnalyze();
			tb.getLoopStack().remove(this);
		}
		
		return result;
	}

	@Override
	public boolean declare(Scope scope) {
		boolean result = true;
		
		// Объявление переменных условия (если нужно)
		result&=condition.declare(scope);

		// Создаем новую область видимости для тела цикла
		blockScope = new BlockScope(scope);
		// Объявляем элементы тела цикла
		if(null!=blockNode) {
			result&=blockNode.declare(blockScope);
		}

		return result;
	}

	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		boolean result = true;
		cgScope = cg.enterLoopBlock();

		// Проверка типа условия
		brScope = cg.enterBranch();
		result&=condition.postAnalyze(blockScope, cg);

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
			try {
				VarType condType = condition.getType(blockScope);
				if(null==condType || VarType.BOOL!=condType) {
					markError("Loop condition must be boolean, got: " + condType);
					result = false;
				}
			}
			catch (CompileException e) {
				markError(e);
				result = false;
			}
		}

		if(result && condition instanceof LiteralExpression) {
			if(((LiteralExpression)condition).getBooleanValue()) {
				alwaysTrue = true;
			}
			else {
				alwaysFalse = true;
			}
		}
		cg.leaveBranch();
		
		// Анализ тела цикла
		if(null!=blockNode) {
			result&=blockNode.postAnalyze(blockScope, cg);
		}
		
		// Проверяем бесконечный цикл с возвратом
		//TODO нужно доделать isControlFlowInterrupted и добавить в другие циклы
//		if (condition instanceof LiteralExpression && Boolean.TRUE.equals(((LiteralExpression)condition).getValue()) &&	isControlFlowInterrupted(blockNode)) {
//			markWarning("Code after infinite loop is unreachable");
//		}

		cg.leaveLoopBlock();
		return result;
	}
	
	@Override
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean toAccum) throws Exception {
		if(cgDone) return null;
		cgDone = true;

		CodegenResult result = null;
		CGScope cgs = null == parent ? cgScope : parent;

		CGLabelScope loopLbScope = new CGLabelScope(null, null, LabelNames.LOOP, true);
		if(!alwaysFalse) {
			brScope.append(loopLbScope);
			if(!alwaysTrue) {
				condition.codeGen(cg, brScope, false);
			}
		}

		if(null!=blockNode && !alwaysFalse) {
			blockNode.codeGen(cg, cgs, false);
		}

		if(!alwaysFalse) {
			cg.jump(cgs, loopLbScope);
		}
		if(!alwaysFalse) {
			cgs.append(brScope.getEnd());
		}

		cgs.append(((CGLoopBlockScope)cgScope).getEndLbScope());
		
		((CGBlockScope)cgScope).build(cg, false);
		((CGBlockScope)cgScope).restoreRegsPool();
		return result;
	}
	
	@Override
	public List<AstNode> getChildren() {
		return Arrays.asList(blockNode);
	}
}