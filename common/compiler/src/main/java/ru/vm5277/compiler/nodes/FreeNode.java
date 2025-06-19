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
package ru.vm5277.compiler.nodes;

import ru.vm5277.compiler.Delimiter;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.ParseException;
import ru.vm5277.common.exceptions.SemanticException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.compiler.semantic.Scope;

public class FreeNode extends AstNode {
	private ExpressionNode target;

	public FreeNode(TokenBuffer tb, MessageContainer mc) {
		super(tb, mc);

		consumeToken(tb); // Наличие гарантировано вызывающим
		try {this.target = new ExpressionNode(tb, mc).parse();} catch(ParseException e) {markFirstError(e);}
		try {consumeToken(tb, Delimiter.SEMICOLON);}catch(ParseException e) {markFirstError(e);}
	}

	@Override
	public boolean preAnalyze() {
		if (target == null) markError("Invalid free expression");

		return true;
	}
	@Override
	public boolean declare(Scope scope) {
        return true;
	}
	@Override
	public boolean postAnalyze(Scope scope) {
		if (null != target && target.postAnalyze(scope)) {
			try {
				VarType type = target.getType(scope);
				if (!type.isReferenceType() || type == VarType.CSTR) {
					markError("Free can only be applied to pointers, got: " + type);
				}
			}
			catch (SemanticException e) {markError(e);}
		}
		return true;
	}
	
	@Override
	public String getNodeType() {
		return "free";
	}
	
	@Override
	public String toString() {
		return "free: " + target;
	}
}