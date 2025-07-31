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
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.semantic.MethodSymbol;
import ru.vm5277.compiler.semantic.Scope;

public class NewExpression extends ExpressionNode {
	private	TypeReferenceExpression	expr;
	private	List<ExpressionNode>	args;
	private	VarType					type;
	
	public NewExpression(TokenBuffer tb, MessageContainer mc, TypeReferenceExpression expr, List<ExpressionNode> args) {
        super(tb, mc);
        
		this.expr = expr;
		this.args = args;
    }
	
	@Override
	public VarType getType(Scope scope) {
		if(null == type) {
			try {type =  expr.getType(scope);} catch(CompileException e) {markError(e);}
		}
		return type;
	}
	
	public String getName() {
		return expr.getClassName();
	}
	
	public List<ExpressionNode> getArgs() {
		return args;
	}
	
	@Override
	public boolean preAnalyze() {
		if (null == expr) {
			markError("Class name cannot be empty");
			return false;
		}

		if(!expr.preAnalyze()) {
			return false;
		}
		
		for (ExpressionNode arg : args) {
			if (arg == null) {
				markError("Constructor argument cannot be null");
				return false;
			}
			if (!arg.preAnalyze()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean declare(Scope scope) {
		if(null == type) {
			try {type =  expr.getType(scope);} catch(CompileException e) {markError(e);}
		}
		return true;
	}
	
	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
//		try {
			

			// Проверяем аргументы конструктора
			for (ExpressionNode arg : args) {
				if (!arg.postAnalyze(scope, cg)) return false;
			}

/*			// Проверяем существование класса
			ClassScope classScope = scope.getThis().resolveClass(expr);
			if (null == classScope) {
				markError("Class '" + expr + "' not found");
				return false;
			}

			// Проверяем конструкторы (если есть)
			List<MethodSymbol> constructors = classScope.getConstructors();
			if (!constructors.isEmpty()) {
				List<VarType> argTypes = new ArrayList<>();
				for (ExpressionNode arg : args) {
					argTypes.add(arg.getType(scope));
				}

				if (findMatchingConstructor(scope, constructors, argTypes) == null) {
					markError("No valid constructor for class '" + expr + "' with arguments: " + argTypes);
					return false;
				}
			}

			return true;
		}
		catch (CompileException e) {
			markError(e.getMessage());
			return false;
		}*/
return true;
	}
	
	private MethodSymbol findMatchingConstructor(Scope scope, List<MethodSymbol> constructors, List<VarType> argTypes) throws CompileException {
		for (MethodSymbol constructor : constructors) {
			if (isArgumentsMatch(scope, constructor, argTypes)) {
				return constructor;
			}
		}
		return null;
	}
//TODO дубликат в MethodCallExpression
	private boolean isArgumentsMatch(Scope scope, MethodSymbol constructor, List<VarType> argTypes) throws CompileException {
		List<VarType> paramTypes = constructor.getParameterTypes();
		if (paramTypes.size() != argTypes.size()) {
			return false;
		}

		for (int i = 0; i < paramTypes.size(); i++) {
			if (!isCompatibleWith(scope, argTypes.get(i), paramTypes.get(i))) {
				return false;
			}
		}
		return true;
	}
	
	//TODO isUsed setUsed getChildren
	
	@Override
	public Object codeGen(CodeGenerator cg) throws Exception {
		long[] operands = null;
		if(!args.isEmpty()) {
			
			operands = new long[args.size()];
			for(int i=0; i<args.size(); i++) {
				args.get(i).codeGen(cg);
				operands[i] = -1;//cg.getAcc();
			}
		}

		cg.eNew(getType(null).getId(), operands, false);//TODO canThrow
		
		return null;
	}
}