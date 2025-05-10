/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
06.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.semantic;

public interface Scope {
	public Symbol resolve(String name);
	
	public Scope getParent();
	
	public ClassScope resolveClass(String className);
	
	public static ClassScope resolveClass(Scope scope, String className) {
        // Ищем класс в текущей и родительских областях видимости
        while (scope != null) {
            if (scope instanceof ClassScope) {
                ClassScope classScope = (ClassScope) scope;
                if (className.equals(classScope.getName())) {
                    return classScope;
                }
            }
            scope = scope.getParent();
        }
        return null;
    }
}