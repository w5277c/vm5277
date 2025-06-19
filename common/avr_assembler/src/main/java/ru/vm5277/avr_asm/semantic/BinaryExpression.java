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
package ru.vm5277.avr_asm.semantic;

import ru.vm5277.avr_asm.TokenBuffer;
import ru.vm5277.avr_asm.scope.Scope;
import ru.vm5277.common.Operator;
import ru.vm5277.common.messages.MessageContainer;

public class BinaryExpression extends Expression {
	private	Expression	leftExpr;
	private	Operator	operator;
	private	Expression	rightExpr;
	
	public BinaryExpression(TokenBuffer tb, Scope scope, MessageContainer mc, Expression left, Operator operator, Expression right) {
		this.leftExpr = left;
		this.operator = operator;
		this.rightExpr = right;
	}
	
	public Expression getLeftExpr() {
		return leftExpr;
	}

	public Operator getOp() {
		return operator;
	}
	
	public Expression getRightExpr() {
		return rightExpr;
	}
	
	@Override
	public String toString() {
		return leftExpr.toString() + operator.getSymbol() + rightExpr.toString();
	}
}
