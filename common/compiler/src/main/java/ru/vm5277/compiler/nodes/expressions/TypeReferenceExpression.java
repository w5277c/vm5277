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

import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.semantic.InterfaceSymbol;
import ru.vm5277.compiler.semantic.Scope;

public class TypeReferenceExpression extends ExpressionNode {
	private final	String	className;
	private			VarType	varType;
	
	public TypeReferenceExpression(TokenBuffer tb, MessageContainer mc, String className) {
		super(tb, mc);
		
		this.className = className;
	}

	public String getClassName() {
		return className;
	}

	@Override
	public VarType getType(Scope scope) throws CompileException {
		// Возвращаем тип-класс, если он существует
		VarType type = VarType.fromClassName(className);
		if (null != type && !type.isClassType()) throw new CompileException("Type '" + className + "' not found");

		if(null == type) {
			// Затем проверяем, является ли это интерфейсом
			InterfaceSymbol interfaceSymbol = scope.getThis().resolveInterface(className);
			if (null != interfaceSymbol) {
				type = VarType.addClassName(className);
			}
		}
		
		return type;
	}

	@Override
	public boolean preAnalyze() {
		if (null == className || className.isEmpty()) {
			markError("Classname cannot be empty");
			return false;
		}
		return true;
	}

	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		try {
			varType = getType(scope); // Проверяем существование типа
			return true;
		}
		catch (CompileException e) {
			markError(e.getMessage());
			return false;
		}
	}

	@Override
	public String toString() {
		return "TypeReference: " + className;
	}
	
	public String getName() {
		return className;
	}
	
	@Override
	public Object codeGen(CodeGenerator cg) throws Exception {
		//cg.setAcc(new Operand(VarType.CLASS, OperandType.TYPE, varType.getId()));
		return null;
	}
}