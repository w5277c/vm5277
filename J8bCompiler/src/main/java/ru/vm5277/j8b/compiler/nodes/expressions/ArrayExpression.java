/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
25.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes.expressions;

import ru.vm5277.j8b.compiler.SemanticError;
import ru.vm5277.j8b.compiler.enums.Delimiter;
import ru.vm5277.j8b.compiler.enums.VarType;
import ru.vm5277.j8b.compiler.nodes.TokenBuffer;
import ru.vm5277.j8b.compiler.semantic.SymbolTable;

public class ArrayExpression extends ExpressionNode {
    private	final	ExpressionNode	array;
    private	final	ExpressionNode	index;

	public ArrayExpression(TokenBuffer tb, ExpressionNode expr) {
		super(tb);

		this.array = expr;
		
		tb.consume(Delimiter.LEFT_BRACKET); // Пропускаем '['
		index = new ExpressionParser(tb).parse(); // Парсим выражение-индекс
		tb.consume(Delimiter.RIGHT_BRACKET); // Пропускаем ']'
	}

	@Override
	public VarType semanticAnalyze(SymbolTable symbolTable) throws SemanticError {
		// 1. Проверяем, что array — действительно массив
		VarType arrayType = array.semanticAnalyze(symbolTable);

		// Проверка на null
		if (arrayType == VarType.NULL) {
			throw new SemanticError("Null pointer array access", sp);
		}

		if (!arrayType.isArray()) {
			throw new SemanticError(String.format("Array access on non-array type '%s'", arrayType.getName()), sp);
		}

		// 2. Проверка глубины вложенности (максимум 3 уровня)
		int depth = 1;
		VarType currentType = arrayType;
		while (currentType.isArray()) {
			depth++;
			currentType = currentType.getElementType();
			if (depth > 3) {
				throw new SemanticError("Array nesting depth exceeds maximum allowed (3 levels)", sp);
			}
		}

		// 3. Проверяем индекс (должен быть целочисленным)
		VarType indexType = index.semanticAnalyze(symbolTable);
		if (!indexType.isInteger()) {
			throw new SemanticError("Array index must be integer type, got '" + indexType.getName() + "'", sp);
		}

		// 4. Проверка границ для статических массивов
		if (arrayType.getArraySize() != null) {
			if (index instanceof LiteralExpression) {
				int idxValue = (int)((LiteralExpression)index).getValue();
				if (idxValue < 0 || idxValue >= arrayType.getArraySize()) {
					throw new SemanticError(String.format("Array index out of bounds [0..%d]", arrayType.getArraySize()-1), sp);
				}
			}
		}

		// TODO добавить проверку изменяемости для операций записи

		// Возвращаем тип элемента массива
		return arrayType.getElementType();
	}
	
	@Override
	public <T> T accept(ExpressionVisitor<T> visitor) {
		return visitor.visit(this);
	}
}