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
import ru.vm5277.common.cg.scopes.CGCellsScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.cg.scopes.CGVarScope;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.semantic.AliasSymbol;
import ru.vm5277.compiler.semantic.ClassScope;
import ru.vm5277.compiler.semantic.FieldSymbol;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.compiler.semantic.Symbol;

public class FieldAccessExpression extends ExpressionNode {
	private			ExpressionNode	target;
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

		if (!(target instanceof TypeReferenceExpression || target instanceof VarFieldExpression || target instanceof ThisExpression)) {
			markError("Invalid field access target: " + target.toString());
			return false;
		}
		return true;
	}
	
	@Override
	public boolean declare(Scope scope) {
		try {
			if(target instanceof UnresolvedReferenceExpression) {
				UnresolvedReferenceExpression ure = (UnresolvedReferenceExpression)target;
				if("this".equals(ure.getId())) {
					target = new ThisExpression(tb, mc);
				}
				else if(null != VarType.fromClassName(ure.getId())) {
					target = new TypeReferenceExpression(tb, mc, ure.getId());
				}
				else {
					target =  new VarFieldExpression(tb, mc, ure.getId());
				}
			}

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
			else if(target instanceof ThisExpression) {
				symbol = scope.getThis().resolve(fieldName);
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
		try {
			cgScope = cg.enterExpression();
		
			if(null == symbol) {
				cg.leaveExpression();
				return false;
			}

			// Проверка видимости поля
			if (((FieldSymbol)symbol).isPrivate() && !(target instanceof ThisExpression)) {
				markError("Private field '" + fieldName + "' is not accessible");
				cg.leaveExpression();
				return false;
			}

			if(!target.postAnalyze(scope, cg)) {
				cg.leaveExpression();
				return false;
			}
		}
		catch (CompileException e) {
			markError(e.getMessage());

			cg.leaveExpression();
			return false;
		}
		cg.leaveExpression();
		return true;
	}
	
	public Object getValue() {
		return null == symbol ? null : symbol;
	}
	
	@Override
	public Object codeGen(CodeGenerator cg) throws Exception {
		CGScope oldCGScope = cg.setScope(cgScope);
		if(null == depCodeGen(cg)) {
			if(symbol.getCGScope() instanceof CGCellsScope) {
				cg.cellsToAcc(cgScope, (CGCellsScope)symbol.getCGScope());
			}
			else {
				throw new CompileException("Unsupported scope: " + symbol.getCGScope());
			}
		}
		cg.setScope(oldCGScope);
		return true;
	}
	
	@Override
	public List<AstNode> getChildren() {
		return Arrays.asList(target);
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + " " + target + "." + fieldName;
	}
}