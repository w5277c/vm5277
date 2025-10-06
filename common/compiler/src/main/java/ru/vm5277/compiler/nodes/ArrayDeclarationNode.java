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
import java.util.Set;
import ru.vm5277.compiler.Delimiter;
import ru.vm5277.compiler.Keyword;
import ru.vm5277.common.Operator;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.common.messages.WarningMessage;
import ru.vm5277.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.compiler.nodes.expressions.LiteralExpression;
import ru.vm5277.compiler.semantic.BlockScope;
import ru.vm5277.compiler.semantic.ClassScope;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.compiler.semantic.Symbol;

public class ArrayDeclarationNode extends AstNode {
    private	final	Set<Keyword>	modifiers;
	private	final	VarType			elementType;
    private	final	String			name;
	private			ExpressionNode	size;
	private			ExpressionNode	initializer;
	
	//TODO рудимент?
	public ArrayDeclarationNode(TokenBuffer tb, MessageContainer mc, Set<Keyword> modifiers, VarType type, String name) {
		super(tb, mc);
		
		this.modifiers = modifiers;
		this.elementType = type;
		this.name = name;
		
		consumeToken(tb); // Потребляем '['
        
        // Размер массива
        try{size = new ExpressionNode(tb, mc).parse();}  catch(CompileException e) {markFirstError(e);}
        
        try {consumeToken(tb, Delimiter.RIGHT_BRACKET);} catch(CompileException e) {markFirstError(e);} // Потребляем ']'

        // Инициализация (опционально)
        if (tb.match(Operator.ASSIGN)) {
            consumeToken(tb);
            try {initializer = new ExpressionNode(tb, mc).parse();} catch(CompileException e) {markFirstError(e);}
        }
		else {
			initializer = null;
		}
		
		try {consumeToken(tb, Delimiter.SEMICOLON);}catch(CompileException e) {markFirstError(e);}
	}

	@Override
	public String getNodeType() {
		return "array declaration";
	}
	
	@Override
	public boolean preAnalyze() {
		if(Character.isUpperCase(name.charAt(0))) addMessage(new WarningMessage("Array name should start with lowercase letter:" + name, sp));
		
		// Проверка типа элементов
		if (null == elementType || VarType.VOID == elementType || VarType.UNKNOWN == elementType) markError("Invalid array element type: " + elementType);

		if(!size.preAnalyze()) {
			return false;
		}
		if(!initializer.preAnalyze()) {
			return false;
		}
		
		return true;
	}

	@Override
	public boolean declare(Scope scope) {
		try {
			// Создаем базовый тип массива без указания размера, конкретный размер будет проверяться в postAnalyze
			VarType arrayType = VarType.arrayOf(elementType);
			
			// Проверяем конфликты имён и регистрируем переменную
			symbol = new Symbol(name, arrayType, modifiers.contains(Keyword.FINAL), modifiers.contains(Keyword.STATIC));
			if (scope instanceof ClassScope) {
				((ClassScope)scope).addField(symbol);
			}
			else if (scope instanceof BlockScope) {
				((BlockScope)scope).addVariable(symbol);
			}
			else markError("Arrays can only be declared in class or block scope");
		}
		catch (CompileException e) {markError(e);}
		
		return true;
	}
	
	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		// Проверка размера массива (если указан)
		Integer declaredSize = null;
		if (size != null) {
			if (size.postAnalyze(scope, cg)) {
				// Проверяем, что размер - целочисленная константа
				if (!(size instanceof LiteralExpression) || VarType.INT != ((LiteralExpression)size).getType(scope)) {
					markError("Array size must be a constant expression");
				}
				else {
					Number sizeValue = (Number)((LiteralExpression)size).getValue();
					if (sizeValue.intValue() <= 0) {
						markError("Array size must be positive, got: " + sizeValue);
					}
					else {
						declaredSize = sizeValue.intValue();						
						//Обновляем тип массива с учетом размера
						if(null != symbol) symbol.setType(VarType.arrayOf(elementType, sizeValue.intValue()));
					}
				}
			}
		}

		// Проверка инициализатора (если есть)
		if (initializer != null) {
			if (initializer.postAnalyze(scope, cg)) {
				// Проверка совместимости типов
				try {
					VarType initType = initializer.getType(scope);
					if (!initType.isArray()) markError("Array initializer must be an array");
					else if (!isCompatibleWith(scope, initType.getElementType(), elementType)) {
						markError(String.format("Type mismatch: cannot initialize %s[] with %s[]", elementType, initType.getElementType()));
					}
					// Дополнительная проверка на сужающее преобразование
					else if (elementType.isNumeric() && initType.isNumeric() && elementType.getSize() < initType.getSize()) {
						markError("Narrowing conversion from " + initType + " to " + elementType + " requires explicit cast");
					}
					// Проверка размера, если массив с фиксированным размером
					else if (declaredSize != null) {
						// Для литеральных массивов
						if (initializer instanceof LiteralExpression) {
							Object value = ((LiteralExpression)initializer).getValue();
							if (value instanceof Object[]) {
								int actualSize = ((Object[])value).length;
								if (actualSize != declaredSize) {
									markError("Array size mismatch: declared " + declaredSize + ", initializer has " + actualSize);
								}
							}
						}
						// Для других выражений (например, вызовов методов, возвращающих массивы)
						// Можно добавить дополнительную проверку во время выполнения
						else {
							markWarning("Array size will be checked at runtime");
						}
					}
				}
				catch (CompileException e) {markError(e);}
			}
		}

		// 3. Проверка final-массивов
		if (modifiers.contains(Keyword.FINAL) && null == initializer) markError("Final array must be initialized");

		return true;
	}

	@Override
	public List<AstNode> getChildren() {
		return null;
	}
}