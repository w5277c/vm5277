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

import ru.vm5277.common.Operator;

public class Term {
    private	Operator		operator;
    private	ExpressionNode	node;
    private	boolean			isPositive;
    
    public Term(Operator op, ExpressionNode node, boolean isPositive) {
        this.operator = op;
        this.node = node;
        this.isPositive = isPositive;
    }

	public boolean isNumber() {
		return node instanceof LiteralExpression && ((LiteralExpression)node).getValue() instanceof Number;
	}

	public Operator getOperator() {
		return operator;
	}
	
	public ExpressionNode getNode() {
		return node;
	}
	
	public boolean isPositive() {
		return isPositive;
	}
}