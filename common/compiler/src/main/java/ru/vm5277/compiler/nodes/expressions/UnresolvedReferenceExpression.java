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

import java.util.List;
import ru.vm5277.common.Property;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.semantic.Scope;

public class UnresolvedReferenceExpression extends ExpressionNode {
	private	String					id;
	private	List<ExpressionNode>	args;
	private	String					methodName;
	private	ExpressionNode			resolvedExpr;

	public UnresolvedReferenceExpression(TokenBuffer tb, MessageContainer mc, String id) {
		super(tb, mc);
		
		this.id = id;
	}
	
	public void set(String methodName, List<ExpressionNode> args) {
		this.methodName = methodName;
		this.args = args;
	}

	
	@Override
	public VarType getType(Scope scope) throws CompileException {
		return null;
	}
	
	@Override
	public boolean preAnalyze() {
		return true;
	}
	
	@Override
	public boolean declare(Scope scope) {
		return true;
	}
	
	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		boolean result = true;
		try {
			if("this".equals(id)) {
				resolvedExpr = new ThisExpression(tb, mc);
			}
			else if(null != VarType.fromClassName(id)) {
				resolvedExpr = new TypeReferenceExpression(tb, mc, id);
			}
			else if(null != VarType.fromEnumName(id)) {
				resolvedExpr = new EnumExpression(tb, mc, id);
			}
			else {
				resolvedExpr = new VarFieldExpression(tb, mc, id);
			}
			
			if(result && !resolvedExpr.preAnalyze()) result = false;
			if(result && !resolvedExpr.declare(scope)) result = false;
			if(result && !resolvedExpr.postAnalyze(scope, cg)) result = false;
			
			if(result && resolvedExpr instanceof VarFieldExpression && resolvedExpr.getType(scope).isEnum()) {
				Property prop = null;
				try {prop = Property.valueOf(methodName.toUpperCase());} catch(Exception ex){}
				resolvedExpr = new PropertyExpression(tb, mc, new VarFieldExpression(tb, mc, id), prop, args);

				if(result && !resolvedExpr.preAnalyze()) result = false;
				if(result && !resolvedExpr.declare(scope)) result = false;
				if(result && !resolvedExpr.postAnalyze(scope, cg)) result = false;
			}
		}
		catch(CompileException ex) {
			markError(ex);
			result = false;
		}
		
		return result;
	}
	
	public ExpressionNode getResolvedExpr() {
		return resolvedExpr;
	}
	
	public String getId() {
		return id;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}