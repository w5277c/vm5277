/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
24.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.semantic;

import java.util.Stack;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ru.vm5277.j8b.compiler.SemanticError;
import ru.vm5277.j8b.compiler.SourceBuffer;
import ru.vm5277.j8b.compiler.enums.VarType;

public class SymbolTable {
    private final	Stack<Map<String, SymbolEntry>>	scopes	= new Stack<>();
	private	final	Map<String, ClassInfo>			classes	= new HashMap<>();


    public SymbolTable() {
        enterScope(); // Глобальная область видимости
    }

    public void enterScope() {
        scopes.push(new HashMap<>());
    }

    public void exitScope() {
        scopes.pop();
    }

    
    // Добавляет символ в текущую область видимости
    public void addSymbol(String name, VarType type, boolean isMutable, SourceBuffer sb) throws SemanticError {
        Map<String, SymbolEntry> currentScope = scopes.peek();
        if (currentScope.containsKey(name)) {
            throw new SemanticError(String.format("Symbol '%s' already defined at line %d", name, currentScope.get(name).sb), sb);
        }
        currentScope.put(name, new SymbolEntry(type, isMutable, sb));
    }

	// Добавляем информацию о классе
	public void addClass(String className, ClassInfo classInfo, SourceBuffer sb) {
		if (classes.containsKey(className)) {
            throw new SemanticError("Duplicate class: " + className, sb);
        }
        classes.put(className, classInfo);
	}
	
	// SymbolTable.java
	public void addClassField(String className, String fieldName, VarType type, SourceBuffer sb) throws SemanticError {
		getClassInfo(className, sb).addField(fieldName, type, sb);
	}

	public void addClassMethod(String className, String methodName, VarType returnType, List<VarType> params, SourceBuffer sb) throws SemanticError {
		getClassInfo(className, sb).addMethod(methodName, new MethodInfo(returnType, params), sb);
	}
	
	// Получаем ClassInfo
	public ClassInfo getClassInfo(String className, SourceBuffer sb) throws SemanticError {
		ClassInfo info = classes.get(className);
		if (info == null) {
			throw new SemanticError("Class not found: " + className, sb);
		}
		return info;
	}

	// Поиск метода в классе
	public MethodInfo lookupMethod(String className, String methodName, SourceBuffer sb) throws SemanticError {
		return getClassInfo(className, sb).getMethod(methodName, sb);
	}

	// Поиск поля в классе
	public VarType lookupField(String className, String fieldName, SourceBuffer sb) throws SemanticError {
		return getClassInfo(className, sb).getFieldType(fieldName, sb);
	}	

	// Поиск символа в таблице (с учётом вложенных областей видимости)
    public SymbolEntry lookup(String name, SourceBuffer sb) throws SemanticError {
        for (int i=scopes.size()-1; i>=0; i--) {
            if (scopes.get(i).containsKey(name)) {
                return scopes.get(i).get(name);
            }
        }
        throw new SemanticError("Undefined symbol: " + name, sb);
    }

	// Проверяет, можно ли изменить значение переменной
    public void checkMutability(String name, SourceBuffer sb) throws SemanticError {
        SymbolEntry entry = lookup(name, sb);
        if (!entry.isMutable) {
            throw new SemanticError(String.format("Cannot modify immutable variable '%s' declared at line %d", name, entry.sb), sb);
        }
    }
}