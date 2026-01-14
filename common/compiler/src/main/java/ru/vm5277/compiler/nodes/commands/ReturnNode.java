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

import java.util.List;
import static ru.vm5277.common.SemanticAnalyzePhase.DECLARE;
import static ru.vm5277.common.SemanticAnalyzePhase.POST;
import static ru.vm5277.common.SemanticAnalyzePhase.PRE;
import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.scopes.CGBlockScope;
import ru.vm5277.common.cg.scopes.CGMethodScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.compiler.CodegenResult;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.common.lexer.Delimiter;
import ru.vm5277.common.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import static ru.vm5277.compiler.Main.debugAST;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.semantic.MethodScope;
import ru.vm5277.compiler.semantic.Scope;

public class ReturnNode extends CommandNode {
	private	ExpressionNode	expr;
	private	VarType			type;
	
	public ReturnNode(TokenBuffer tb, MessageContainer mc) {
		super(tb, mc);
		
		consumeToken(tb); // Потребляем "return"
		
		try {
			this.expr = (tb.match(Delimiter.SEMICOLON) ? null : new ExpressionNode(tb, mc).parse());
		}
		catch(CompileException e) {markFirstError(e);}
        
        // Обязательно потребляем точку с запятой
        try {
			consumeToken(tb, Delimiter.SEMICOLON);
		}
		catch(CompileException e) {
			markFirstError(e);
		}
    }
	
	public CommandNode getThis() {
		return this;
	}

	public ReturnNode(MessageContainer mc, ExpressionNode expr) {
		super(null, mc);
		
		this.expr = expr;
	}
	
    public ExpressionNode getExpression() {
        return expr;
    }

    public boolean returnsValue() {
        return null!=expr;
    }
	
	@Override
	public boolean preAnalyze() {
		boolean result = true;
		debugAST(this, PRE, true, getFullInfo());

		if(expr!=null) {
			result&=expr.preAnalyze();
		}
		
		debugAST(this, PRE, false, result, getFullInfo());
		return result;
	}

	@Override
	public boolean declare(Scope scope) {
		boolean result = true;
		debugAST(this, DECLARE, true, getFullInfo());

		if(null!=expr) {
			result&=expr.declare(scope);
		}
		
		debugAST(this, DECLARE, false, result, getFullInfo() + (declarationPendingNodes.containsKey(this) ? " [DP]" : ""));
		return result;
	}

	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		boolean result = true;
		debugAST(this, POST, true, getFullInfo() + " type:" + type);
		cgScope = cg.enterCommand();
	
		// Находим ближайший MethodScope
		MethodScope methodScope = findEnclosingMethodScope(scope);
		if(null==methodScope) {
			markError("'return' outside of method");
			result = false;
		}

		if(result) {
			// Получаем тип возвращаемого значения метода
			type = methodScope.getSymbol().getType();

			// Проверяем соответствие возвращаемого значения
			if(null==expr) { // return без значения
				if(!type.isVoid()) {
					markError("Void method cannot return a value");
					result = false;
				}
			}
			else { // return с выражением
				if(type.isVoid()) {
					markError("Non-void method must return a value");
					result = false;
				}
				else {
					result&=expr.postAnalyze(scope, cg);

					if(result) {
						try {
							ExpressionNode optimizedExpr = expr.optimizeWithScope(scope, cg);
							if(null != optimizedExpr) {
								expr = optimizedExpr;
							}
						}
						catch(CompileException ex) {
							markError(ex);
							result = false;
						}

						if(result) {
							// Проверяем тип выражения
							VarType exprType = expr.getType();
							if(null==exprType) {
								markError("Cannot determine type of expression: " + expr.toString());
								result = false;
							}
							else {
								if(!isCompatibleWith(scope, exprType, type)) {
									markError("Return type mismatch: expected " + type + ", got " + exprType);
									result = false;
								}
							}
						}
					}
				}
			}
		}
		
		cg.leaveCommand();
		debugAST(this, POST, false, result, getFullInfo());
		return result;
	}
	
	@Override
	public void codeOptimization(Scope scope, CodeGenerator cg) {
		CGScope oldScope = cg.setScope(cgScope);

		expr.codeOptimization(scope, cg);
		
		try {
			ExpressionNode optimizedExpr = expr.optimizeWithScope(scope, cg);
			if(null != optimizedExpr) {
				expr = optimizedExpr;
			}
		}
		catch(CompileException ex) {
			markError(ex);
		}

		cg.setScope(oldScope);
	}

	@Override
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean toAccum, CGExcs excs) throws CompileException {
		if(cgDone) return null;
		cgDone = true;

		CGScope cgs = (null == parent ? cgScope : parent);
		
		if(null!=expr) {
			if(CodegenResult.RESULT_IN_ACCUM!=expr.codeGen(cg, cgs, true, excs)) {
				throw new CompileException("Accum not used for operand:" + expr);
			}
		}
		
		CGScope scope = cgScope.getParent();
		while(null!=scope) {
			CGScope nextScope = scope.getParent();
			if(null!=nextScope && nextScope instanceof CGMethodScope) {
				break;
			}
			scope = nextScope;
		}
				
		if(null==scope || !(scope instanceof CGBlockScope)) {
			markError("COPMILER BUG: block scope without method scope");
		}
		else {
			cg.jump(cgs, ((CGBlockScope)scope).getELabel());

			if(null!=expr) {
				return CodegenResult.RESULT_IN_ACCUM;
			}
		}
		return null;
	}

	@Override
	public List<AstNode> getChildren() {
		return null;
	}
	
	@Override
	public String toString() {
		return expr.toString();
	}

	public String getFullInfo() {
		return getClass().getSimpleName() + " " + toString();
	}
}
