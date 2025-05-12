/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes;

import ru.vm5277.j8b.compiler.enums.Keyword;
import ru.vm5277.j8b.compiler.enums.TokenType;
import ru.vm5277.j8b.compiler.enums.VarType;
import ru.vm5277.j8b.compiler.exceptions.ParseException;
import ru.vm5277.j8b.compiler.messages.MessageContainer;
import ru.vm5277.j8b.compiler.semantic.MethodScope;
import ru.vm5277.j8b.compiler.semantic.Scope;
import ru.vm5277.j8b.compiler.semantic.Symbol;

public class ParameterNode extends AstNode {
	private	final	VarType	type;
    private	final	String	name;
	private	final	boolean	isFinal;
	
	public ParameterNode(TokenBuffer tb, MessageContainer mc) throws ParseException {
		super(tb, mc);
		
		this.isFinal = tb.match(Keyword.FINAL);
		if (this.isFinal) {
            consumeToken(tb); // Потребляем 'final'
		}
		
		this.type = checkPrimtiveType();
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

		try {
			// Создаем символ параметра и добавляем его в область видимости метода
			Symbol paramSymbol = new Symbol(name, type, isFinal, false);
			((MethodScope)scope).getSymbol().getParameters().add(paramSymbol);
		}
		catch (Exception e) {
			markError("Failed to declare parameter: " + e.getMessage());
		}
		
		return true;
	}

	@Override
	public boolean postAnalyze(Scope scope) {
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
				markError("Array size can't be initialized");
			}
		}

		return true;
	}
}
