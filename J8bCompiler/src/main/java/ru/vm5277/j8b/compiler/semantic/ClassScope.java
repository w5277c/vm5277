/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
06.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.semantic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ru.vm5277.j8b.compiler.enums.VarType;
import ru.vm5277.j8b.compiler.exceptions.SemanticException;

public class ClassScope implements Scope { // Плохая идея, область видимости класса не является символом... сделать отдельный метод для поиска классов
	private			ClassScope						parent;
	private			String							name;
	
	private final	Map<String, String>				imports			= new HashMap<>();
	private final	Map<String, String>				staticImports	= new HashMap<>();
	private	final	Map<String, Symbol>				fields			= new HashMap<>();
	private	final	Map<String, ClassScope>			classes			= new HashMap<>();
	private	final	Map<String, InterfaceSymbol>	interfaces		= new HashMap<>();
	private	final	Map<String, List<MethodSymbol>>	methods			= new HashMap<>();
	private	final	List<MethodSymbol>				constructors	= new ArrayList<>();

	public ClassScope() {
	}
	
	public ClassScope(String name, Scope parentScope) throws SemanticException {
		if (null != parentScope && !(parentScope instanceof ClassScope)) throw new SemanticException("Сlass " + name + " can only be declared within a class.");
		if (name == null || name.isEmpty()) throw new SemanticException("Class name cannot be empty");

		this.parent = (ClassScope)parentScope;
		this.name = name;
	}
	
	public void addImport(String importPath, String alias) throws SemanticException {
		String importName = (alias != null) ? alias : getLastComponent(importPath);
		if (imports.containsKey(importName)) throw new SemanticException("Duplicate import for name: " + importName);
		imports.put(importName, importPath);
	}

	public void addStaticImport(String importPath, String alias) throws SemanticException {
		String importName = (alias != null) ? alias : getLastComponent(importPath);
		if (staticImports.containsKey(importName)) throw new SemanticException("Duplicate static import for name: " + importName);
		staticImports.put(importName, importPath);
	}
	
	public void addClass(ClassScope classScope) throws SemanticException {
        String scopeName = classScope.getName();
		
		if (imports.containsKey(name)) throw new SemanticException("Class '" + scopeName + "' conflicts with import");
        if (staticImports.containsKey(name)) throw new SemanticException("Class '" + scopeName + "' conflicts with static import");
		if (classes.containsKey(classScope.getName())) throw new SemanticException("Duplicate class: " + scopeName);
		if (interfaces.containsKey(classScope.getName())) throw new SemanticException("Class: " + scopeName + " already defined as interface");
		classes.put(scopeName, classScope);
	}
	
	public void addInterface(InterfaceSymbol symbol) throws SemanticException {
        String symbolName = symbol.getName();
		
		if (imports.containsKey(name)) throw new SemanticException("Interface '" + symbolName + "' conflicts with import");
        if (staticImports.containsKey(name)) throw new SemanticException("Interface '" + symbolName + "' conflicts with static import");
		if (classes.containsKey(symbol.getName())) throw new SemanticException("Interface " + symbolName + " conflicts with class of the same name");
		if (interfaces.containsKey(symbol.getName())) throw new SemanticException("Class " + symbolName + " conflicts with interface of the same name");
		interfaces.put(symbolName, symbol);
	}
	
	public void addConstructor(MethodSymbol newSymbol) throws SemanticException {
		if(!name.equals(newSymbol.getName())) throw new SemanticException("Constructor name must match class name '" + name + "'");
		
		String signature = newSymbol.getSignature();
		for(MethodSymbol symbol : constructors) {
			if(signature.equals(symbol.getSignature())) throw new SemanticException("Duplicate constructor '" + signature + "'");
		}
		constructors.add(newSymbol);
	}
	
	public void addMethod(MethodSymbol newSymbol) throws SemanticException {
		String symbolName = newSymbol.getName();
		
        if (imports.containsKey(name)) throw new SemanticException("Method '" + symbolName + "' conflicts with import");
        if (staticImports.containsKey(name)) throw new SemanticException("Method '" + symbolName + "' conflicts with static import");
		if (fields.containsKey(symbolName)) throw new SemanticException("Method name '" + symbolName + "' conflicts with field name");
		if (classes.containsKey(symbolName)) throw new SemanticException("Method name '" + symbolName + "' conflicts with class name");
		if (interfaces.containsKey(symbolName)) throw new SemanticException("Method name '" + symbolName + "' conflicts with interface name");

		
		// Получаем список методов с таким же именем
		List<MethodSymbol> methodsWithSameName = methods.get(symbolName);
		if (null == methodsWithSameName) {
			methodsWithSameName = new ArrayList<>();
			methods.put(symbolName, methodsWithSameName);
		}
		
		// Проверяем на перегрузку/дублирование
		String newSignature = newSymbol.getSignature();
		for (MethodSymbol existingMethod : methodsWithSameName) {
			// Проверяем полное совпадение сигнатур
			if (newSignature.equals(existingMethod.getSignature())) throw new SemanticException("Duplicate method '" + newSignature + "' in class '" +
																								name + "'");
			// Дополнительная проверка на конфликт при наследовании (если нужно)
			if (newSymbol.getType().equals(existingMethod.getType()) &&
				newSymbol.getParameterTypes().equals(existingMethod.getParameterTypes())) throw new SemanticException(	"Method '" + newSignature +
																														"' conflicts with inherited method");
		}

		methodsWithSameName.add(newSymbol);
	}

	public void addField(Symbol symbol) throws SemanticException {
		String symbolName = symbol.getName();
		
        if (imports.containsKey(symbolName)) throw new SemanticException("Field '" + symbolName + "' conflicts with import");
        if (staticImports.containsKey(symbolName)) throw new SemanticException("Field '" + symbolName + "' conflicts with static import");
		if (classes.containsKey(symbolName)) throw new SemanticException("Field name " + symbolName + " conflicts with class name");
		if (interfaces.containsKey(symbolName)) throw new SemanticException("Field name " + symbolName + " conflicts with interface name");
		if (fields.containsKey(symbolName)) throw new SemanticException("Duplicate field: " + symbolName);
		fields.put(symbolName, symbol);
	}
	
	protected ClassScope getClass(String className) {
		return classes.get(className);
	}
	
	public InterfaceSymbol getInterface(String interfaceName) {
		return interfaces.get(interfaceName);
	}
	public Map<String, InterfaceSymbol> getInterfaces() {
		return interfaces;
	}
	
	public boolean checkStaticImportExists(String path) {
		return staticImports.containsKey(path);
	}
	
	public List<MethodSymbol> getMethods(String methodName) {
		return methods.get(methodName);
	}
	
	public Map<String, List<MethodSymbol>> getMethods() {
		return methods;
	}
	
	public Map<String, Symbol> getFields() {
		return fields;
	}

	public List<MethodSymbol> getConstructors() {
		return constructors;
	}


	@Override
	public Symbol resolve(String name) {
		// Поиск в полях текущего класса
		if (fields.containsKey(name)) {
			return fields.get(name);
		}

		// Поиск методов (без параметров - для простых случаев)
		if (methods.containsKey(name) && !methods.get(name).isEmpty()) {
			// Возвращаем первый метод с таким именем (для точного поиска нужно использовать resolveMethod)
			return methods.get(name).get(0);
		}

		// Поиск в интерфейсах
		if (interfaces.containsKey(name)) {
			return interfaces.get(name);
		}

		// Поиск в родительской области видимости (если есть)
		if (parent != null) {
			return parent.resolve(name);
		}

		// Символ не найден
		return null;
	}
	
	@Override
	public ClassScope resolveClass(String className) {
		return Scope.resolveClass(this, className);
	}
	
	public MethodSymbol resolveMethod(String methodName, List<VarType> argTypes) {
		// Ищем методы в текущем классе
		List<MethodSymbol> candidates = methods.get(methodName);
		if (null == candidates) return null;
		for (MethodSymbol method : candidates) {
			if (isApplicable(method, argTypes)) {
				return method; // Пока возвращаем первое совпадение по количеству
			}
		}
		return null;
	}
/*TODO	
	public MethodSymbol resolveStaticImport(String methodName, List<VarType> argTypes) {
		for (MethodSymbol method : staticImports.values()) {
			if (method.getName().equals(methodName) && isArgumentsMatch(method, argTypes)) return method;
		}
		return null;
	}
*/
	
	@Override
	public InterfaceSymbol resolveInterface(String interfaceName) {
		// Поиск в текущем классе
		if (interfaces.containsKey(interfaceName)) return interfaces.get(interfaceName);

		// Поиск в импортах (если интерфейс объявлен в другом пакете)
		String importedName = imports.get(interfaceName);
		if (null != importedName && interfaces.containsKey(importedName)) return interfaces.get(importedName);

		// Поиск во вложенных классах
		for (ClassScope innerClass : classes.values()) {
			InterfaceSymbol innerInterface = innerClass.resolveInterface(interfaceName);
			if (innerInterface != null) return innerInterface;
		}

		// Поиск в родительской области видимости (если есть)
		if (parent != null) {
			return parent.resolveInterface(interfaceName);
		}

		return null;
	}

	
	private boolean isApplicable(MethodSymbol method, List<VarType> argTypes) {
		List<VarType> paramTypes = method.getParameterTypes();
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
	
	private String getLastComponent(String path) {
		int pos = path.lastIndexOf('.');
		return pos >= 0 ? path.substring(pos+1) : path;
	}
	
	public String getName() {
		return name;
	}

	@Override
	public Scope getParent() {
		return parent;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + name + "]";
	}
}
