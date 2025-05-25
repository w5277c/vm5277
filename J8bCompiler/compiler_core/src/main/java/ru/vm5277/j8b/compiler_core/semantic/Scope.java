/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
06.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler_core.semantic;

public interface Scope {
	public Symbol resolve(String name);
	
	public Scope getParent();
	
	public ClassScope resolveClass(String className);
	public InterfaceSymbol resolveInterface(String className);

	public static ClassScope getThis(Scope scope) {
		while(true) {
			if(scope instanceof ClassScope) {
				return (ClassScope)scope;
			}
			if(null == scope.getParent()) return null;
			scope = scope.getParent();
		}
	}
	
	public static ClassScope resolveClass(Scope scope, String className) {
		while(true) {
			if(scope instanceof ClassScope) {
				ClassScope result = ((ClassScope)scope).getClass(className);
				if(null != result) return result;
			}
			if(null == scope.getParent()) return null;
			scope = scope.getParent();
		}
	}
}