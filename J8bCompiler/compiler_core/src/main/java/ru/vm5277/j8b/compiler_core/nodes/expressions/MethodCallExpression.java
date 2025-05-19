/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler_core.nodes.expressions;

import java.util.ArrayList;
import java.util.List;
import ru.vm5277.j8b.compiler.common.enums.VarType;
import ru.vm5277.j8b.compiler.common.exceptions.SemanticException;
import ru.vm5277.j8b.compiler_core.messages.MessageContainer;
import ru.vm5277.j8b.compiler_core.nodes.TokenBuffer;
import ru.vm5277.j8b.compiler_core.semantic.ClassScope;
import ru.vm5277.j8b.compiler_core.semantic.InterfaceSymbol;
import ru.vm5277.j8b.compiler_core.semantic.MethodSymbol;
import ru.vm5277.j8b.compiler_core.semantic.Scope;

public class MethodCallExpression extends ExpressionNode {
	private	final	ExpressionNode			parent;
	private	final	String					methodName;
	private	final	List<ExpressionNode>	arguments;
	private			MethodSymbol			method;
	
    public MethodCallExpression(TokenBuffer tb, MessageContainer mc, ExpressionNode parent, String methodName, List<ExpressionNode> arguments) {
        super(tb, mc);
        
		this.parent = parent;
		this.methodName = methodName;
        this.arguments = arguments;
    }

	public String getMethodName() {
		return methodName;
	}
	
	public ExpressionNode getParent() {
		return parent;
	}

	public List<ExpressionNode> getArguments() {
		return arguments;
	}
	
	@Override
	public VarType getType(Scope scope) throws SemanticException {
		// Получаем типы аргументов
		List<VarType> argTypes = new ArrayList<>();
		for (ExpressionNode arg : arguments) {
			argTypes.add(arg.getType(scope));
		}

		// Если есть parent (вызов через объект или класс)
		if (null != parent) {
			VarType parentType = parent.getType(scope);

			// Если parent - класс (статический вызов)
			if (parentType.isClassType()) {
				ClassScope classScope = scope.resolveClass(parentType.getName());
				if (null == classScope) throw new SemanticException("Class '" + parentType.getName() + "' not found");

				method = classScope.resolveMethod(methodName, argTypes);
				if (method != null) return method.getType();
			}

			// Если parent - объект (вызов метода экземпляра)
			else {
				// Получаем класс объекта
				ClassScope classScope = scope.resolveClass(parentType.getName());
				if (null == classScope) throw new SemanticException("Class '" + parentType.getName() + "' not found");

				method = classScope.resolveMethod(methodName, argTypes);
				if (null != method && !method.isStatic()) return method.getType();
			}

			throw new SemanticException("Method '" + methodName + "' not found in " + parentType);
		}

		// Вызов метода текущего класса (без parent)
		if (scope instanceof ClassScope) {
			method = ((ClassScope)scope).resolveMethod(methodName, argTypes);
			if (null != method) return method.getType();
		}

		// TODO Проверка статических импортов
/*		if (scope instanceof ClassScope) {
			MethodSymbol method = ((ClassScope)scope).resolveStaticImport(methodName, argTypes);
			if (null != method) return method.getType();
		}
*/
		// Проверка интерфейсов (если scope - класс)
		if (scope instanceof ClassScope) {
			ClassScope classScope = (ClassScope)scope;
			for (InterfaceSymbol interfaceSym : classScope.getInterfaces().values()) {
				List<MethodSymbol> methods = interfaceSym.getMethods(methodName);
				if (null != methods) {
					for (MethodSymbol interfaceMethod : methods) {
						if (isArgumentsMatch(scope, interfaceMethod, argTypes)) {
							method = interfaceMethod;
							return interfaceMethod.getType();
						}
					}
				}
			}
		}

		throw new SemanticException("Method '" + methodName + "' not found");
	}
	
	public MethodSymbol getMethod() {
		return method;
	}
	
	private boolean isArgumentsMatch(Scope scope, MethodSymbol method, List<VarType> argTypes) {
		List<VarType> paramTypes = method.getParameterTypes();
		if (paramTypes.size() != argTypes.size()) return false;

		for (int i = 0; i < paramTypes.size(); i++) {
			if (!isCompatibleWith(scope, argTypes.get(i), paramTypes.get(i))) {
				return false;
			}
		}
		return true;
	}
	@Override
	public boolean preAnalyze() {
		if (methodName == null || methodName.isEmpty()) {
			markError("Method name cannot be empty");
			return false;
		}

		if (arguments == null) {
			markError("Arguments list cannot be null");
			return false;
		}

		for (ExpressionNode arg : arguments) {
			if (arg == null) {
				markError("Argument cannot be null");
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
			// Проверка родительского объекта (если метод не статический)
			if (parent != null && !parent.postAnalyze(scope)) {
				return false;
			}

			// Проверка аргументов
			for (ExpressionNode arg : arguments) {
				if (!arg.postAnalyze(scope)) return false;
			}

			// Получаем типы аргументов
			List<VarType> argTypes = new ArrayList<>();
			for (ExpressionNode arg : arguments) {
				argTypes.add(arg.getType(scope));
			}

			// Поиск метода в ClassScope
			if (scope instanceof ClassScope) {
				ClassScope classScope = (ClassScope)scope;
				MethodSymbol methodSymbol = classScope.resolveMethod(methodName, argTypes);
				if (methodSymbol == null) {
					markError("Method '" + methodName + "' not found");
					return false;
				}
			}
		}
		catch (SemanticException e) {
            markError(e.getMessage());
            return false;
        }
		return true;
	}
}