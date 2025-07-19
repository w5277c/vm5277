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

import java.util.Arrays;
import java.util.List;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.semantic.ClassScope;
import ru.vm5277.compiler.semantic.FieldSymbol;
import ru.vm5277.compiler.semantic.Scope;

public class FieldAccessExpression extends ExpressionNode {
	private	final	ExpressionNode	target;
	private	final	String			fieldName;
	
	private			String			className;
    
	public FieldAccessExpression(TokenBuffer tb, MessageContainer mc, ExpressionNode target, String fieldName) {
		super(tb, mc);
		this.target = target;
		this.fieldName = fieldName;
	}

	public ExpressionNode getTarget() {
		return target;
	}

	public String getClassName() {
		return className;
	}
	public String getFieldName() {
		return fieldName;
	}

	@Override
	public String getNodeType() {
		return "member access";
	}

	@Override
	public VarType getType(Scope scope) throws CompileException {
		if(null == symbol) {
			declare(scope);
		}
		return null == symbol ? null : symbol.getType();
	}
	
	
	@Override
	public boolean preAnalyze() {
		if (target == null) {
			markError("Field access target cannot be null");
			return false;
		}

		if(!target.preAnalyze()) {
			return false;
		}

		if (target instanceof TypeReferenceExpression || target instanceof VarFieldExpression) {
		}
		else {
			markError("Invalid field access target: " + target.toString());
			return false;
		}
		return true;
	}
	
	@Override
	public boolean declare(Scope scope) {
		try {
			if (target instanceof TypeReferenceExpression) {
				TypeReferenceExpression tre = (TypeReferenceExpression) target;
				className = tre.getClassName();
				ClassScope classScope = scope.getThis().resolveClass(tre.getType(scope).getClassName());
				if(null != classScope) {
					symbol = classScope.resolve(fieldName);
				}
				else {
					markError("Can't resolve class scope:" + tre.toString());
					return false;
				}
			}
			else {
				// Нестатическое поле (this.field или obj.field)
				VarType targetType = target.getType(scope);
				if (targetType == null || targetType.isPrimitive()) {
					markError("Cannot access field of primitive type: " + targetType);
					return false;
				}
				ClassScope classScope = scope.getThis().resolveClass(target.getType(scope).getClassName());
				if(null != classScope) {
					symbol = classScope.resolve(fieldName);
				}
				else {
					markError("Can't resolve class scope:" + target.toString());
					return false;
				}
			}

			if (symbol == null) {
				throw new CompileException("Field not found: " + fieldName);
			}
		}
		catch (CompileException e) {
            markError(e.getMessage());
			return false;
        }

		return true;
	}
	
	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		if(null == symbol) return false;
		
		// Проверка видимости поля
		if (((FieldSymbol)symbol).isPrivate()) {
			markError("Private field '" + fieldName + "' is not accessible");
			return false;
		}

		if(!target.postAnalyze(scope, cg)) {
			return false;
		}
		
		return true;
	}
	
	public Object getValue() {
		return null == symbol ? null : symbol;
	}
	
	@Override
	public Object codeGen(CodeGenerator cg) throws Exception {
		return null;
	}
	
	@Override
	public List<AstNode> getChildren() {
		return Arrays.asList(target);
	}
}