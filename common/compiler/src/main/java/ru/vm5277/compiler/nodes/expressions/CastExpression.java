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
package ru.vm5277.compiler.nodes.expressions;

import ru.vm5277.common.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.common.cg.CodeGenerator;
import java.util.List;
import java.util.Arrays;
import ru.vm5277.common.cg.scopes.CGScope;
import static ru.vm5277.compiler.Main.debugAST;
import ru.vm5277.compiler.nodes.AstNode;
import static ru.vm5277.common.enums.SemanticAnalyzePhase.DECLARE;
import static ru.vm5277.common.enums.SemanticAnalyzePhase.PRE;
import static ru.vm5277.common.enums.SemanticAnalyzePhase.POST;
import ru.vm5277.common.lexer.SourcePosition;
import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.compiler.Instance;
import ru.vm5277.compiler.nodes.expressions.bin.BinaryExpression;

public class CastExpression extends ExpressionNode {
	private			VarType			sourceType;
	private			ExpressionNode	operand;

	public CastExpression(Instance inst, TokenBuffer tb, SourcePosition sp, VarType targetType, ExpressionNode operand) {
		super(inst, tb, sp);
		
		this.type = targetType;
		this.operand = operand;
	}

	@Override
	public boolean preAnalyze() {
		boolean result = true;
		debugAST(this, PRE, true, getFullInfo());
		
		result&=operand.preAnalyze();
		
		debugAST(this, PRE, false, result, getFullInfo());
		return result;
	}

	@Override
	public boolean declare(Scope scope) {
		boolean result = true;
		debugAST(this, DECLARE, true, getFullInfo());

		result&=operand.declare(scope);
		
		debugAST(this, DECLARE, false, result, getFullInfo() + (declarationPendingNodes.containsKey(this) ? " [DP]" : ""));
		return result;
	}

	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg, CGScope parent) {
		boolean result = true;
		debugAST(this, POST, true, getFullInfo() + " type:" + type);
		cgScope = cg.enterExpression(parent, cgScope, toString());
		
		result &=operand.postAnalyze(scope, cg, cgScope);
		if(result) {
			// Резолвинг QualifiedPathExpression
			ExpressionNode resolved = resolveQualifiedPathExpr(operand);
			if(null!=resolved) {
				operand = resolved;
			}

			sourceType = operand.getType();

			try {
				if(!canCast(scope, sourceType, type)) {
					markError("Invalid cast from " + sourceType + " to " + type);
					result = false;
				}
			}
			catch(CompileException ex) {
				markError(ex);
				result = false;
			}
		}

		debugAST(this, POST, false, result, getFullInfo());
		return result;
	}

	@Override
	public void codeOptimization(Scope scope, CodeGenerator cg) {
		operand.codeOptimization(scope, cg);
		
		try {
			ExpressionNode optimizedExpr = operand.optimizeWithScope(scope, cg);
			if(null != optimizedExpr) {
				operand = optimizedExpr;
			}
		}
		catch(CompileException ex) {
			markError(ex);
		}
	}

	@Override
	public Object codeGen(CodeGenerator cg, boolean toAccum, CGExcs excs) throws CompileException {
		// Для констант мы можем задать размер аккумулятора изначально, это даст оптимальный код
		// TODO точно? Проверить!
		// Учет выражения (fixed)byte - ранее я убрал из этого условия VarFieldExpression, что-то оно ломало, нужно проверять
		//!!! Мы задаем порядок в postAnalyze а состояние аккумулятора - здесь, порядок должен быть единым 
		
		//Вопрос оптимизации - мы знаем в какой тип будет выполнено преобразование. Поэтому в аккумулятор можно загрузить только полезную часть
		
//		if(operand instanceof LiteralExpression || operand instanceof VarFieldExpression) {
			cg.accumLock(type, !(operand instanceof BinaryExpression) && !(operand instanceof ArrayExpression));
			Object result = operand.codeGen(cg, toAccum, excs);
//			if(sourceType!=type) {
//				cgScope.append(cg.accCast(sourceType, type));
//			}
			cg.accumUnlock();
			return result;
/*		}
		else {
			// В остальных случаев обработка выражений не может быть построена с учетом определенного размера аккумулятора, поэтому используем пост приведение
			//cg.getAccum().set(-1==operand.getType().getSize() ? cg.getRefSize() : operand.getType().getSize(), operand.getType().isFixedPoint());
			cg.accResize(sourceType);
			Object result = operand.codeGen(cg, null, toAccum, excs);
			if(sourceType!=type) {
				cgScope.append(cg.accCast(sourceType, type));
			}
			return result;
		}*/
	}

	public ExpressionNode getOperand() {
		return operand;
	}
	public void setOperand(ExpressionNode expr)  {
		operand = expr;
	}
	
	@Override
	public List<AstNode> getChildren() {
		return Arrays.asList(operand);
	}
	
	@Override
	public String toString() {
		return "(" + type + ")" + operand;
	}
	
	public String getFullInfo() {
		return getClass().getSimpleName() + " " + toString();
	}
}