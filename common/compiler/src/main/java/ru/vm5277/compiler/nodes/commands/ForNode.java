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
import java.util.HashSet;
import java.util.List;
import static ru.vm5277.common.SemanticAnalyzePhase.DECLARE;
import static ru.vm5277.common.SemanticAnalyzePhase.POST;
import static ru.vm5277.common.SemanticAnalyzePhase.PRE;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.scopes.CGBlockScope;
import ru.vm5277.common.cg.CGBranch;
import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.common.cg.scopes.CGLabelScope;
import ru.vm5277.common.cg.scopes.CGLoopBlockScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.compiler.CodegenResult;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.nodes.BlockNode;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.nodes.VarNode;
import ru.vm5277.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.common.lexer.Delimiter;
import ru.vm5277.common.lexer.J8BKeyword;
import ru.vm5277.common.lexer.TokenType;
import ru.vm5277.common.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import static ru.vm5277.compiler.Main.debugAST;
import ru.vm5277.compiler.nodes.expressions.LiteralExpression;
import ru.vm5277.compiler.semantic.BlockScope;
import ru.vm5277.compiler.semantic.Scope;

public class ForNode extends CommandNode {
    private	AstNode			init;
    private	ExpressionNode	condition;
    private	ExpressionNode	iteration;
    private	BlockNode		blockNode;
	private	BlockNode		elseBlockNode;
	private	BlockScope		forScope;
	private	BlockScope		bodyScope;
	private	BlockScope		elseScope;
	private	CGBlockScope	blockScope;
	private	CGBranch		branch			= new CGBranch();
	private	boolean			alwaysTrue;
	private	boolean			alwaysFalse;
	
    public ForNode(TokenBuffer tb, MessageContainer mc) {
        super(tb, mc);
        
        consumeToken(tb); // Потребляем "for"
        try {consumeToken(tb, Delimiter.LEFT_PAREN);} catch(CompileException e) {markFirstError(e);}
        
        // Инициализация
        if(!tb.match(Delimiter.SEMICOLON)) {
			try {
				VarType type = checkPrimtiveType();
				if(null == type) type = checkClassType();
				List<ExpressionNode> arrDimensions = null;
				if(null != type) arrDimensions = parseArrayDimensions(); //TODO
				if(null != arrDimensions) {
					for(int i=0; i<arrDimensions.size(); i++) type = VarType.arrayOf(type);
				}
				if(null != type) {
					String name = null;
					try {name = consumeToken(tb, TokenType.IDENTIFIER).getStringValue();} catch(CompileException e) {markFirstError(e);}
					this.init = new VarNode(tb, mc, new HashSet<>(), type, name);
				}
				else {
					this.init = new ExpressionNode(tb, mc).parse();
					try {consumeToken(tb, Delimiter.SEMICOLON);} catch(CompileException e) {markFirstError(e);}
				}
			}
			catch(CompileException e) {
				markFirstError(e);
			}
		}
		else {
			this.init = null;
			try {
				consumeToken(tb, Delimiter.SEMICOLON);
			}
			catch(CompileException e) {
				markFirstError(e);
			}
		}
        
        // Условие
        try {
			this.condition = (tb.match(Delimiter.SEMICOLON) ? null : new ExpressionNode(tb, mc).parse());
		}
		catch(CompileException e) {
			markFirstError(e);
		}
        try {
			consumeToken(tb, Delimiter.SEMICOLON);
		}
		catch(CompileException e) {
			markFirstError(e);
		}
        
        // Итерация
        try {
			this.iteration = (tb.match(Delimiter.RIGHT_PAREN) ? null : new ExpressionNode(tb, mc).parse());
		}
		catch(CompileException e) {
			markFirstError(e);
		}
        
		try {
			consumeToken(tb, Delimiter.RIGHT_PAREN);
		}
		catch(CompileException e) {
			markFirstError(e);
		}
		
        // Основной блок
		try {
			blockNode = (tb.match(Delimiter.LEFT_BRACE) ? new BlockNode(tb, mc) : new BlockNode(tb, mc, parseStatement()));
		}
		catch(CompileException e) {
			markFirstError(e);
		}
       
        // Блок else (если есть)
        if(tb.match(J8BKeyword.ELSE)) {
			consumeToken(tb);
			try {
				elseBlockNode = (tb.match(Delimiter.LEFT_BRACE) ? new BlockNode(tb, mc) : new BlockNode(tb, mc, parseStatement()));
			}
			catch(CompileException e) {
				markFirstError(e);
			}
        }
    }
    
    // Геттеры
    public AstNode getInitialization() {
        return init;
    }
    
    public ExpressionNode getCondition() {
        return condition;
    }
    
    public ExpressionNode getIteration() {
        return iteration;
    }
    
	public boolean isAlwaysFalse() {
		return alwaysFalse;
	}
	
    public BlockNode getBody() {
        return blockNode;
    }
    
    public BlockNode getElseBlock() {
        return elseBlockNode;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("for (");
        sb.append(init != null ? init : ";");
        sb.append(condition != null ? condition : ";");
        sb.append(iteration != null ? iteration : "");
        sb.append(") ");
        sb.append(getBody());
        
        if(getElseBlock()!=null) {
            sb.append(" else ").append(getElseBlock());
        }
        
        return sb.toString();
    }
	
	public String getFullInfo() {
		return toString();
	}

	@Override
	public boolean preAnalyze() {
		boolean result = true;
		debugAST(this, PRE, true, getFullInfo());

		// Проверка блока инициализации
		if(null!=init) {
			result&=init.preAnalyze();
		}

		// Проверка условия цикла
		if(null!=condition) {
			result&=condition.preAnalyze();
		}

		// Проверка блока итерации
		if(null!=iteration) {
			result&=iteration.preAnalyze();
		}

		// Проверка основного тела цикла
		if(null!=blockNode) {
			result&=blockNode.preAnalyze();
		}

		// Проверка else-блока (если есть)
		if(null!=elseBlockNode) {
			result&=elseBlockNode.preAnalyze();
		}
		
		debugAST(this, PRE, false, result, getFullInfo());
		return result;
	}

	@Override
	public boolean declare(Scope scope) {
		boolean result = true;
		debugAST(this, DECLARE, true, getFullInfo());
		
		// Создаем новую область видимости для всего цикла
		forScope = new BlockScope(scope);

		// Объявление блока инициализации
		if(null!=init) {
			result&=init.declare(forScope);
		}

		// Объявление условия
		if(null!=condition) {
			result&=condition.declare(forScope);
		}

		// Объявление блока итерации
		if(null!=iteration) {
			result&=iteration.declare(forScope);
		}

		// Объявление тела цикла
		if(null!=blockNode) {
			bodyScope = new BlockScope(forScope);
			result&=blockNode.declare(bodyScope);
		}
		
		// Объявление else-блока (если есть)
		if(null!=elseBlockNode) {
			elseScope = new BlockScope(forScope);
			result&=elseBlockNode.declare(elseScope);
		}
		
		debugAST(this, DECLARE, false, result, getFullInfo() + (declarationPendingNodes.containsKey(this) ? " [DP]" : ""));
		return result;
	}

	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		boolean result = true;
		debugAST(this, POST, true, getFullInfo());
		cgScope = cg.enterLoopBlock();
		
		// Для метка на next блок используется для ForNode 
		((CGLoopBlockScope)cgScope).getNextLbScope().setUsed();
		// Анализ блока инициализации
		if(null!=init) {
			result&=init.postAnalyze(forScope, cg);
		}
		// Проверка типа условия
		if(condition!=null) {
			result&=condition.postAnalyze(forScope, cg);
			if(result) {
				try {
					ExpressionNode optimizedExpr = condition.optimizeWithScope(forScope, cg);
					if(null != optimizedExpr) {
						condition = optimizedExpr;
						result&=condition.postAnalyze(forScope, cg);
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
			
			if(result && condition instanceof LiteralExpression) {
				if(((LiteralExpression)condition).getBooleanValue()) {
					alwaysTrue = true;
				}
				else {
					alwaysFalse = true;
				}
			}
		}

		// Анализ блока итерации
		if(null!=iteration) {
			result&=iteration.postAnalyze(forScope, cg);
		}

		// Анализ тела цикла
		if(null!=getBody()) {
			getBody().postAnalyze(bodyScope, cg);
		}
		
		// Анализ else-блока
		if(null!=getElseBlock()) {
			getElseBlock().postAnalyze(elseScope, cg);
		}
		
		// Проверяем бесконечный цикл с возвратом
		if(condition instanceof LiteralExpression && Boolean.TRUE.equals(((LiteralExpression)condition).getValue()) && isControlFlowInterrupted(getBody())) {
			markWarning("Code after infinite while loop is unreachable");
		}

		cg.leaveLoopBlock();
		debugAST(this, POST, false, result, getFullInfo());
		return result;
	}

	@Override
	public void codeOptimization(Scope scope, CodeGenerator cg) {
		CGScope oldScope = cg.setScope(cgScope);
		
		//TODO добавить остальные елементы
		
		if(null!=init) {
			init.codeOptimization(scope, cg);
		}
		
		if(null!=condition) {
			condition.codeOptimization(scope, cg);
			try {
				ExpressionNode optimizedExpr = condition.optimizeWithScope(scope, cg);
				if(null != optimizedExpr) {
					condition = optimizedExpr;
				}
			}
			catch(CompileException ex) {
				markError(ex);
			}
		}
		
		if(null!=iteration) {
			iteration.codeOptimization(scope, cg);
			try {
				ExpressionNode optimizedExpr = iteration.optimizeWithScope(scope, cg);
				if(null != optimizedExpr) {
					iteration = optimizedExpr;
				}
			}
			catch(CompileException ex) {
				markError(ex);
			}
		}

		if(null!=blockNode) {
			blockNode.codeOptimization(scope, cg);
		}
		
		if(null!=elseBlockNode) {
			elseBlockNode.codeOptimization(scope, cg);
		}

		if(null!=condition && condition instanceof LiteralExpression) {
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
	public List<AstNode> getChildren() {
		if(null==elseBlockNode) {
			return Arrays.asList(blockNode);
		}
		return Arrays.asList(blockNode, elseBlockNode);
	}
	
	@Override
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean toAccum, CGExcs excs) throws CompileException {
		if(cgDone) return null;
		cgDone = true;
		
		CodegenResult result = null;
		
		CGScope cgs = null == parent ? cgScope : parent;
		cgs.setBranch(branch);
		
		if(null!=init) {
			//TODO VarScope не учитывает CGScope родиеля(этот) при регистрации переменной
			init.codeGen(cg, cgs, false, excs);
		}
		
		if(null==condition) {
			cgs.append(((CGLoopBlockScope)cgScope).getStartLbScope());
		}
		else {
			if(!alwaysFalse) {
				cgs.append(((CGLoopBlockScope)cgScope).getStartLbScope());
				if(!alwaysTrue) {
					condition.codeGen(cg, cgs, false, excs);
				}
			}
		}
		
		if(null!=blockNode && !alwaysFalse) {
			blockNode.codeGen(cg, cgs, false, excs);
		}
		
		if(null!=iteration && !alwaysFalse) {
			CGLabelScope nextLbScope = ((CGLoopBlockScope)cgScope).getNextLbScope();
			cgs.append(nextLbScope);
			nextLbScope.setUsed();
			iteration.codeGen(cg, cgs, false, excs);
		}

		if(!alwaysFalse) {
			cg.jump(cgs, ((CGLoopBlockScope)cgScope).getStartLbScope());
		}
		if(null!=condition && !alwaysFalse) {
			cgs.append(branch.getEnd());
		}
		
		if(null!=elseBlockNode) {
			elseBlockNode.codeGen(cg, cgs, false, excs);
		}
		
		cgs.append(((CGLoopBlockScope)cgScope).getEndLbScope());
		
		((CGBlockScope)cgScope).build(cg, false, excs);
		((CGBlockScope)cgScope).restoreRegsPool();

		return result;
	}
}