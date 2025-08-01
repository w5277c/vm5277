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

import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.common.cg.CodeGenerator;
import java.util.List;
import java.util.Arrays;
import ru.vm5277.compiler.nodes.AstNode;

public class CastExpression extends ExpressionNode {
	private	final	VarType			targetType;
	private			VarType			sourceType;
	private			ExpressionNode	operand;

	public CastExpression(TokenBuffer tb, MessageContainer mc, VarType targetType, ExpressionNode operand) {
		super(tb, mc);
		this.targetType = targetType;
		this.operand = operand;
	}

	@Override
	public VarType getType(Scope scope) throws CompileException {
		return targetType;
	}
	
	public VarType getType() {
		return targetType;
	}

	@Override
	public boolean preAnalyze() {
		return operand.preAnalyze();
	}

	@Override
	public boolean declare(Scope scope) {
		return operand.declare(scope);
	}

	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		try {
			cgScope = cg.enterExpression();
			if (!operand.postAnalyze(scope, cg)) {
				cg.leaveExpression();
				return false;
			}

			try {
				sourceType = operand.getType(scope);
				if(!canCast(sourceType, targetType)) {
					markError("Invalid cast from " + sourceType + " to " + targetType);
					cg.leaveExpression();
					return false;
				}
			}
			catch (CompileException e) {
				markError(e);
				cg.leaveExpression();
				return false;
			}
			cg.leaveExpression();
			return true;
		}
		catch (CompileException e) {
			markError(e.getMessage());
			cg.leaveExpression();
			return false;
		}
	}

	@Override
	public Object codeGen(CodeGenerator cg) throws Exception {
		Object result = operand.codeGen(cg);
		if (sourceType != targetType) {
			if (sourceType.isNumeric() && targetType.isNumeric()) {
				cg.accCast(cgScope, targetType);
			}
		}
		return result;
	}

	public VarType getTargetType() {
		return targetType;
	}
	
	public ExpressionNode getOperand() {
		return operand;
	}
	
	@Override
	public List<AstNode> getChildren() {
		return Arrays.asList(operand);
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + " (" + targetType + ")" + operand;
	}
}