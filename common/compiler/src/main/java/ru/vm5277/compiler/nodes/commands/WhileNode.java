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
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.scopes.CGBlockScope;
import ru.vm5277.common.cg.CGBranch;
import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.common.cg.scopes.CGLoopBlockScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.compiler.CodegenResult;
import ru.vm5277.compiler.nodes.BlockNode;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.compiler.Delimiter;
import ru.vm5277.common.VarType;
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
	private	CGBranch		branch		= new CGBranch();
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
	
	public boolean isAlwaysFalse() {
		return alwaysFalse;
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
			result&=blockNode.preAnalyze();
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
			VarType condType = condition.getType();
			if(null==condType || VarType.BOOL!=condType) {
				markError("Loop condition must be boolean, got: " + condType);
				result = false;
			}
		}

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
			if(!alwaysTrue) {
				condition.codeGen(cg, cgs, false, excs);
			}
		}

		if(null!=blockNode && !alwaysFalse) {
			blockNode.codeGen(cg, cgs, false, excs);
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
}