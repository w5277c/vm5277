/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
25.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.semantic;

import java.util.HashMap;
import java.util.Map;
import ru.vm5277.j8b.compiler.SemanticError;
import ru.vm5277.j8b.compiler.enums.VarType;

public class ClassInfo {
    private	final	Map<String, MethodInfo>	methods	= new HashMap<>();
    private	final	Map<String, VarType>	fields	= new HashMap<>();

	// Добавляем поле
    public void addField(String name, VarType type) throws SemanticError {
        if (fields.containsKey(name)) {
            throw new SemanticError("Duplicate field: " + name);
        }
        fields.put(name, type);
    }

    // Добавляем метод
    public void addMethod(String name, MethodInfo method) throws SemanticError {
        if (methods.containsKey(name)) {
            throw new SemanticError("Duplicate method: " + name);
        }
        methods.put(name, method);
    }

	// Получаем тип поля
    public VarType getFieldType(String name) throws SemanticError {
        VarType type = fields.get(name);
        if (type == null) {
            throw new SemanticError("Field not found: " + name);
        }
        return type;
    }

    // Получаем метод
    public MethodInfo getMethod(String name) throws SemanticError {
        MethodInfo method = methods.get(name);
        if (method == null) {
            throw new SemanticError("Method not found: " + name);
        }
        return method;
    }
	
	public Map<String, MethodInfo> getMethods() {
		return methods;
	}
}