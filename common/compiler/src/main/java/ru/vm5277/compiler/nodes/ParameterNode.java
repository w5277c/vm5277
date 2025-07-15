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
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.compiler.Keyword;
import ru.vm5277.compiler.TokenType;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.ParseException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.semantic.MethodScope;
import ru.vm5277.compiler.semantic.Scope;

public class ParameterNode extends AstNode {
	private			VarType	type;
    private	final	String	name;
	private	final	boolean	isFinal;
	
	public ParameterNode(TokenBuffer tb, MessageContainer mc) throws ParseException {
		super(tb, mc);
		
		this.isFinal = tb.match(TokenType.MODIFIER, Keyword.FINAL);
		if (this.isFinal) {
            consumeToken(tb); // Потребляем 'final'
		}
		
		this.type = checkPrimtiveType();
		if (null == this.type) this.type = checkClassType();
		this.name = (String)consumeToken(tb, TokenType.ID).getValue();
	}

	public ParameterNode(MessageContainer mc, boolean isFinal, VarType type, String name) throws ParseException {
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
	public String getNodeType() {
		return "parameter";
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + ": " + type + ", " + name;
	}

	@Override
	public boolean preAnalyze() {
		// Проверка имени параметра (должно начинаться с маленькой буквы)
		if (Character.isUpperCase(name.charAt(0))) {
			markWarning("Parameter name '" + name + "' should start with lowercase letter");
		}

		// Проверка корректности типа
		if (null == type || VarType.UNKNOWN == type) {
			markError("Invalid parameter type: " + type);
			return false;
		}

		return true;
	}

	@Override
	public boolean declare(Scope scope) {
		if (!(scope instanceof MethodScope)) {
			markError("Parameters can only be declared in method scope");
			return false;
		}

		// Уже сделано уровнем выше
/*		try {
			// Создаем символ параметра и добавляем его в область видимости метода
			Symbol paramSymbol = new Symbol(name, type, isFinal, false);
			((MethodScope)scope).getSymbol().getParameters().add(paramSymbol);
		}
		catch (Exception e) {
			markError("Failed to declare parameter: " + e.getMessage());
		}*/
		
		return true;
	}

	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		// Проверка типа параметра
		if (VarType.UNKNOWN == type || VarType.NULL == type) {
			markError("Invalid parameter type: " + type);
		}

		// Дополнительные проверки для массивов
		if (type.isArray()) {
			// Проверяем тип элементов массива
			VarType elementType = type.getElementType();
			if (VarType.UNKNOWN == elementType || VarType.NULL == elementType) {
				markError("Array element type cannot be UNKNOWN or NULL");
			}

			// Проверяем вложенность массивов
			if (type.getArrayDepth() > 3) {
				markError("Array nesting depth exceeds maximum allowed (3)");
			}

			// Проверяем размер массива не должен быть указан
			if (null != type.getArraySize() && 0 != type.getArraySize()) {
				markError("Array size cannot be specified in parameter declaration");
			}
		}

		return true;
	}

	@Override
	public List<AstNode> getChildren() {
		return null;
	}
}
