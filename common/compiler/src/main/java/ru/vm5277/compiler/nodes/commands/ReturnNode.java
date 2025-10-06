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
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.scopes.CGBlockScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.compiler.CodegenResult;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.compiler.Delimiter;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.semantic.MethodScope;
import ru.vm5277.compiler.semantic.Scope;

public class ReturnNode extends CommandNode {
	private	ExpressionNode	expr;
	private	VarType			returnType;
	
	public ReturnNode(TokenBuffer tb, MessageContainer mc) {
		super(tb, mc);
		
		consumeToken(tb); // Потребляем "return"
		
		try {this.expr = tb.match(Delimiter.SEMICOLON) ? null : new ExpressionNode(tb, mc).parse();} catch(CompileException e) {markFirstError(e);}
        
        // Обязательно потребляем точку с запятой
        try {consumeToken(tb, Delimiter.SEMICOLON);}catch(CompileException e) {markFirstError(e);}
    }

	public ReturnNode(MessageContainer mc, ExpressionNode expr) {
		super(null, mc);
		
		this.expr = expr;
	}
	
    public ExpressionNode getExpression() {
        return expr;
    }

    public boolean returnsValue() {
        return null != expr;
    }
	
	@Override
	public String getNodeType() {
		return "return command";
	}

	@Override
	public boolean preAnalyze() {
		if (expr != null) {
			if(!expr.preAnalyze()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean declare(Scope scope) {
		if(null != expr) {
			if(!expr.declare(scope)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		cgScope = cg.enterCommand();
	
		// Находим ближайший MethodScope
		MethodScope methodScope = findEnclosingMethodScope(scope);
		if (null == methodScope) {
			markError("'return' outside of method");
		}
		else {
			// Получаем тип возвращаемого значения метода
			returnType = methodScope.getSymbol().getType();

			// Проверяем соответствие возвращаемого значения
			if (null == expr) { // return без значения
				if (!returnType.isVoid()) markError("Void method cannot return a value");
			}
			else { // return с выражением
				if(returnType.isVoid()) {
					markError("Non-void method must return a value");
				}
				else {
					if(!expr.postAnalyze(scope, cg)) {
						cg.leaveCommand();
						return false;
					}
					// Проверяем тип выражения
					try {
						VarType exprType = expr.getType(scope);
						if(null == exprType) {
							markError(String.format("TODO Can't resolve expression: " + expr));
						}
						else {
							if (!isCompatibleWith(scope, returnType, exprType)) {
								markError(String.format("Return type mismatch: expected " + returnType + ", got " + exprType));
							}
						}
					}
					catch (CompileException e) {markError(e);}
				}
			}
		}
		
		cg.leaveCommand();
		return true;
	}
	
	@Override
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean toAccum) throws Exception {
		if(cgDone) return null;
		cgDone = true;

		CGScope cgs = (null == parent ? cgScope : parent);
		
		if(null != expr) {
			if (CodegenResult.RESULT_IN_ACCUM != expr.codeGen(cg, cgs, true)) {
				throw new CompileException("Accum not used for operand:" + expr);
			}
		}
		// Можно было бы вызвать cg.eReturn(cgScope, returnType.getSize());, но в блоке мог быть выделен STACK_FRAME и возможно что-то еще, поэтому переходим
		// на завершение блока. Оптимизатор в будущем почистит лишние JMP(например если после JMP сразу следует метка.
		CGBlockScope bScope = (CGBlockScope)cgScope.getParent();
		cg.jump(cgScope, bScope.getELabel());
		
		if(null != expr) {
			return CodegenResult.RESULT_IN_ACCUM;
		}
		return null;
	}

	@Override
	public List<AstNode> getChildren() {
		return null;
	}
}
