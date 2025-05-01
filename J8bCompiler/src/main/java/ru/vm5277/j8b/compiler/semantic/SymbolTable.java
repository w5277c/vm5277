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
import ru.vm5277.j8b.compiler.SourcePosition;
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
    public void addSymbol(String name, VarType type, boolean isMutable, SourcePosition sp) throws SemanticError {
        Map<String, SymbolEntry> currentScope = scopes.peek();
        if (currentScope.containsKey(name)) {
            throw new SemanticError(String.format("Symbol '%s' already defined at line %d", name, currentScope.get(name).sp), sp);
        }
        currentScope.put(name, new SymbolEntry(type, isMutable, sp));
    }

	// Добавляем информацию о классе
	public void addClass(String className, ClassInfo classInfo, SourcePosition sp) {
		if (classes.containsKey(className)) {
            throw new SemanticError("Duplicate class: " + className, sp);
        }
        classes.put(className, classInfo);
	}
	
	// SymbolTable.java
	public void addClassField(String className, String fieldName, VarType type, SourcePosition sp) throws SemanticError {
		getClassInfo(className, sp).addField(fieldName, type, sp);
	}

	public void addClassMethod(String className, String methodName, VarType returnType, List<VarType> params, SourceBuffer sb) throws SemanticError {
		getClassInfo(className, sb).addMethod(methodName, new MethodInfo(returnType, params), sb);
	}
	
	// Получаем ClassInfo
	public ClassInfo getClassInfo(String className, SourcePosition sp) throws SemanticError {
		ClassInfo info = classes.get(className);
		if (info == null) {
			throw new SemanticError("Class not found: " + className, sp);
		}
		return info;
	}

	// Поиск метода в классе
	public MethodInfo lookupMethod(String className, String methodName, SourcePosition sp) throws SemanticError {
		return getClassInfo(className, sp).getMethod(methodName, sp);
	}

	// Поиск поля в классе
	public VarType lookupField(String className, String fieldName, SourcePosition sp) throws SemanticError {
		return getClassInfo(className, sp).getFieldType(fieldName, sp);
	}	

	// Поиск символа в таблице (с учётом вложенных областей видимости)
    public SymbolEntry lookup(String name, SourcePosition sp) throws SemanticError {
        for (int i=scopes.size()-1; i>=0; i--) {
            if (scopes.get(i).containsKey(name)) {
                return scopes.get(i).get(name);
            }
        }
        throw new SemanticError("Undefined symbol: " + name, sp);
    }

	// Проверяет, можно ли изменить значение переменной
    public void checkMutability(String name, SourcePosition sp) throws SemanticError {
        SymbolEntry entry = lookup(name, sp);
        if (!entry.isMutable) {
            throw new SemanticError(String.format("Cannot modify immutable variable '%s' declared at line %d", name, entry.sp), sp);
        }
    }
}