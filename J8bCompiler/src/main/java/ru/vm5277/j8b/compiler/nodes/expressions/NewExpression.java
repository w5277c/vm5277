/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
01.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes.expressions;

import java.util.ArrayList;
import java.util.List;
import ru.vm5277.j8b.compiler.enums.VarType;
import ru.vm5277.j8b.compiler.exceptions.SemanticException;
import ru.vm5277.j8b.compiler.nodes.TokenBuffer;
import ru.vm5277.j8b.compiler.semantic.ClassScope;
import ru.vm5277.j8b.compiler.semantic.MethodSymbol;
import ru.vm5277.j8b.compiler.semantic.Scope;

public class NewExpression extends ExpressionNode {
	private	String					className;
	private	List<ExpressionNode>	args;
	
	public NewExpression(TokenBuffer tb, String className, List<ExpressionNode> args) {
        super(tb);
        
		this.className = className;
		this.args = args;
    }
	
	
	@Override
	public VarType getType(Scope scope) throws SemanticException {
		return VarType.fromClassName(className);
	}
	
	
	@Override
	public boolean preAnalyze() {
		if (null == className|| className.isEmpty()) {
			markError("Class name cannot be empty");
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
	public boolean postAnalyze(Scope scope) {
		try {
			// Проверяем аргументы конструктора
			for (ExpressionNode arg : args) {
				if (!arg.postAnalyze(scope)) return false;
			}

			// Проверяем существование класса
			ClassScope classScope = scope.resolveClass(className);
			if (null == classScope) {
				markError("Class '" + className + "' not found");
				return false;
			}

			// Проверяем конструкторы (если есть)
			List<MethodSymbol> constructors = classScope.getConstructors();
			if (!constructors.isEmpty()) {
				List<VarType> argTypes = new ArrayList<>();
				for (ExpressionNode arg : args) {
					argTypes.add(arg.getType(scope));
				}

				if (findMatchingConstructor(constructors, argTypes) == null) {
					markError("No valid constructor for class '" + className + "' with arguments: " + argTypes);
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
	
	private MethodSymbol findMatchingConstructor(List<MethodSymbol> constructors, List<VarType> argTypes) {
		for (MethodSymbol constructor : constructors) {
			if (isArgumentsMatch(constructor, argTypes)) {
				return constructor;
			}
		}
		return null;
	}

	private boolean isArgumentsMatch(MethodSymbol constructor, List<VarType> argTypes) {
		List<VarType> paramTypes = constructor.getParameterTypes();
		if (paramTypes.size() != argTypes.size()) {
			return false;
		}

		for (int i = 0; i < paramTypes.size(); i++) {
			if (!argTypes.get(i).isCompatibleWith(paramTypes.get(i))) {
				return false;
			}
		}
		return true;
	}
}