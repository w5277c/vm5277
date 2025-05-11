/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
07.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes;

import ru.vm5277.j8b.compiler.exceptions.ParseException;
import ru.vm5277.j8b.compiler.messages.MessageContainer;
import ru.vm5277.j8b.compiler.messages.WarningMessage;
import ru.vm5277.j8b.compiler.semantic.ClassScope;
import ru.vm5277.j8b.compiler.semantic.Scope;

public class InterfaceBodyNode extends ClassBlockNode {
	public InterfaceBodyNode(TokenBuffer tb, MessageContainer mc, String className) throws ParseException {
		super(tb, mc, className);
	}
	
	@Override
	public String getNodeType() {
		return "interface body";
	}
	
	@Override
	public boolean preAnalyze() {
		// Проверка всех объявлений в блоке
		for (AstNode declaration : declarations) {
			if(declaration instanceof InterfaceNode) {
				declaration.preAnalyze();
			}
			else if(declaration instanceof FieldNode) {
				FieldNode fieldNode = (FieldNode)declaration;
				if(!fieldNode.getModifiers().isEmpty()) {
					addMessage(new WarningMessage("Modifiers not allowed for interface fields (already public static final)", sp));
				}
				declaration.preAnalyze();
			}
			else if(declaration instanceof MethodNode) {
				MethodNode methoddNode = (MethodNode)declaration;
				if(!methoddNode.getModifiers().isEmpty()) {
					addMessage(new WarningMessage("Modifiers not allowed for interface methods (already public abstract)", sp));
				}
				declaration.preAnalyze();
			}
			else {
				markError("Interface cannot contain " + declaration.getNodeType() + " declarations. Only methods, constants and nested types are allowed");
			}
		}
		
		return true;
	}

	@Override
	public boolean declare(Scope scope) {
		for (AstNode declaration : declarations) {
			declaration.declare(scope);
		}
		return true;
	}

	
	@Override
	public boolean postAnalyze(Scope scope) {
		ClassScope classScope = (ClassScope)scope;
		
		for (AstNode declaration : declarations) {
			if(declaration instanceof InterfaceNode) {
				declaration.postAnalyze(scope);
			}
			else if(declaration instanceof FieldNode) {
				FieldNode fieldNode = (FieldNode)declaration;
				if(null == fieldNode.getInitializer()) {
					markError("Final field '" + fieldNode.getName() + "' must be initialized");
				}
				else {
					fieldNode.getInitializer().postAnalyze(scope);
				}
			}
			else if(declaration instanceof MethodNode) {
				MethodNode methoddNode = (MethodNode)declaration;
				if(methoddNode.isConstructor()) {
					markError("Interfaces cannot have constructors");
				}
				if(null != methoddNode.getBody()) {
					markError("Code blocks are not allowed in interfaces");
				}
			}
		}

		return true;
	}
}