/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
25.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler_core.nodes.expressions;

import ru.vm5277.j8b.compiler_core.enums.Delimiter;
import ru.vm5277.j8b.compiler.common.enums.VarType;
import ru.vm5277.j8b.compiler.common.exceptions.ParseException;
import ru.vm5277.j8b.compiler.common.exceptions.SemanticException;
import ru.vm5277.j8b.compiler_core.messages.MessageContainer;
import ru.vm5277.j8b.compiler_core.nodes.TokenBuffer;
import ru.vm5277.j8b.compiler_core.semantic.Scope;

public class ArrayExpression extends ExpressionNode {
    private	final	ExpressionNode	array;
    private			ExpressionNode	index;

	public ArrayExpression(TokenBuffer tb, MessageContainer mc, ExpressionNode expr) {
		super(tb, mc);

		this.array = expr;
		
		try {consumeToken(tb, Delimiter.LEFT_BRACKET);} catch(ParseException e){markFirstError(e);} // Потребляем '['
		try {index = new ExpressionNode(tb, mc).parse();} catch(ParseException e) {markFirstError(e);} // Парсим выражение-индекс
		try {consumeToken(tb, Delimiter.RIGHT_BRACKET);} catch(ParseException e){markFirstError(e);} // Потребляем ']'
	}

   @Override
	public VarType getType(Scope scope) throws SemanticException {
		// Получаем тип массива
		VarType arrayType = array.getType(scope);

		// Проверяем что это действительно массив
		if (!arrayType.isArray()) throw new SemanticException("Array access on non-array type: " + arrayType);

		// Возвращаем тип элементов массива
		return arrayType.getElementType();
	}

	@Override
	public boolean preAnalyze() {
		if (null == array) {
			markError("Array expression is missing");
			return false;
		}

		if (null == index) {
			markError("Index expression is missing");
			return false;
		}

		return array.preAnalyze() && index.preAnalyze();
	}

	@Override
	public boolean postAnalyze(Scope scope) {
		try {
			// Проверяем подвыражения
			if (!array.postAnalyze(scope) || !index.postAnalyze(scope)) {
				return false;
			}

			// Проверяем тип массива
			VarType arrayType = array.getType(scope);
			if (!arrayType.isArray()) {
				markError("Cannot index non-array type: " + arrayType);
				return false;
			}

			// Проверяем тип индекса
			VarType indexType = index.getType(scope);
			if (!indexType.isInteger()) {
				markError("Array index must be integer, got: " + indexType);
				return false;
			}

			// Проверка границ для статических массивов
			if (arrayType.getArraySize() != null && index instanceof LiteralExpression) {
				int idx = ((Number)((LiteralExpression)index).getValue()).intValue();
				if (idx < 0 || idx >= arrayType.getArraySize()) {
					markError("Array index out of bounds [0.." + (arrayType.getArraySize()-1) + "]");
					return false;
				}
			}

			return true;
		}
		catch (SemanticException e) {
			markError(e.getMessage());
			return false;
		}
	}
}