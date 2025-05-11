/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes.expressions;

import java.util.ArrayList;
import java.util.List;
import ru.vm5277.j8b.compiler.enums.VarType;
import ru.vm5277.j8b.compiler.exceptions.SemanticException;
import ru.vm5277.j8b.compiler.messages.MessageContainer;
import ru.vm5277.j8b.compiler.nodes.TokenBuffer;
import ru.vm5277.j8b.compiler.semantic.ClassScope;
import ru.vm5277.j8b.compiler.semantic.InterfaceSymbol;
import ru.vm5277.j8b.compiler.semantic.MethodSymbol;
import ru.vm5277.j8b.compiler.semantic.Scope;

public class MethodCallExpression extends ExpressionNode {
	private	final	ExpressionNode			parent;
	private	final	String					methodName;
	private	final	List<ExpressionNode>	arguments;
    
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
		if (!(scope instanceof ClassScope)) throw new SemanticException("Method call outside class scope");
    	
		ClassScope classScope = (ClassScope)scope;		
		// Получаем типы аргументов
        List<VarType> argTypes = new ArrayList<>();
        for (ExpressionNode arg : arguments) {
            argTypes.add(arg.getType(scope));
        }
		
		// Ищем метод в scope или классе
		MethodSymbol method = classScope.resolveMethod(methodName, argTypes);
		if (null != method) return method.getType();

		// Поиск в реализованных интерфейсах
		for (InterfaceSymbol interfaceSym : classScope.getInterfaces().values()) {
			List<MethodSymbol> methods = interfaceSym.getMethods(methodName);
			if (null != methods) {
				for (MethodSymbol interfaceMethod : methods) {
					if (isArgumentsMatch(interfaceMethod, argTypes)) {
						return interfaceMethod.getType();
					}
				}
			}
		}

		throw new SemanticException("Method '" + methodName + "' not found in class or interfaces");
	}
	
	private boolean isArgumentsMatch(MethodSymbol method, List<VarType> argTypes) {
		List<VarType> paramTypes = method.getParameterTypes();
		if (paramTypes.size() != argTypes.size()) return false;

		for (int i = 0; i < paramTypes.size(); i++) {
			if (!argTypes.get(i).isCompatibleWith(paramTypes.get(i))) {
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
				MethodSymbol method = classScope.resolveMethod(methodName, argTypes);
				if (method == null) {
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