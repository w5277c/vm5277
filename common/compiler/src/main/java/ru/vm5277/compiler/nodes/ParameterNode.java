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

import java.util.List;
import static ru.vm5277.common.SemanticAnalyzePhase.DECLARE;
import static ru.vm5277.common.SemanticAnalyzePhase.POST;
import static ru.vm5277.common.SemanticAnalyzePhase.PRE;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.compiler.Keyword;
import ru.vm5277.compiler.TokenType;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import static ru.vm5277.compiler.Main.debugAST;
import ru.vm5277.compiler.semantic.MethodScope;
import ru.vm5277.compiler.semantic.Scope;

public class ParameterNode extends AstNode {
	private			VarType	type;
    private	final	String	name;
	private			boolean	isFinal;
	
	public ParameterNode(TokenBuffer tb, MessageContainer mc) throws CompileException {
		super(tb, mc);
		
		if(tb.match(TokenType.MODIFIER, Keyword.FINAL)) {
            consumeToken(tb); // Потребляем 'final'
			isFinal = true;
		}
		
		type = checkPrimtiveType();
		if(null==type) type = checkClassType();
		if(null!=type) type = checkArrayType(type);
		
		name = (String)consumeToken(tb, TokenType.ID).getValue();
	}

	public ParameterNode(MessageContainer mc, boolean isFinal, VarType type, String name) throws CompileException {
		super(null, mc);
		
		this.isFinal = isFinal;
		this.type = type;
		this.name = name;
	}
	
	public boolean isFinal() {
		return isFinal;
	}
	
	public VarType getType() {
		return type;
	}
	
	public String getName() {
		return name;
	}

	@Override
	public boolean preAnalyze() {
		boolean result = true;
		debugAST(this, PRE, true, getFullInfo());

		// Проверка имени параметра (должно начинаться с маленькой буквы)
		if(Character.isUpperCase(name.charAt(0))) {
			markWarning("Parameter name '" + name + "' should start with lowercase letter");
		}

		// Проверка корректности типа
		if(null==type || VarType.UNKNOWN==type) {
			markError("Invalid parameter type: " + type);
			result = false;
		}

		debugAST(this, PRE, false, result, getFullInfo());
		return result;
	}

	@Override
	public boolean declare(Scope scope) {
		boolean result = true;
		debugAST(this, DECLARE, true, getFullInfo());

		if (!(scope instanceof MethodScope)) {
			markError("COMPILER BUG: Parameters can only be declared in method scope");
			result = false;
		}

		debugAST(this, DECLARE, false, result, getFullInfo());
		return result;
	}

	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		boolean result = true;
		debugAST(this, POST, true, getFullInfo());

		// Дополнительные проверки для массивов
		if(type.isArray()) {
			// Проверяем тип элементов массива
			VarType elementType = type.getElementType();
			if(VarType.UNKNOWN==elementType || VarType.NULL==elementType) {
				markError("Array element type cannot be UNKNOWN or NULL");
			}

			// Проверяем вложенность массивов
			if(type.getArrayDepth()>3) {
				markError("Array nesting depth exceeds maximum allowed (3)");
			}

			// Проверяем размер массива не должен быть указан
			if(null!=type.getArraySize() && 0!=type.getArraySize()) {
				markError("Array size cannot be specified in parameter declaration");
			}
		}

		debugAST(this, POST, false, result, getFullInfo());
		return result;
	}

	@Override
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean toAccum) throws CompileException {
		return null;
	}
	
	@Override
	public List<AstNode> getChildren() {
		return null;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + ": " + type + ", " + name;
	}
	
	public String getFullInfo() {
		return getClass().getSimpleName() + " " + toString();
	}
}
