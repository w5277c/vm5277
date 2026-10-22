/*
 * Copyright 2025 konstantin@5277.ru
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.vm5277.compiler.nodes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ru.vm5277.common.ImplementInfo;
import static ru.vm5277.common.enums.SemanticAnalyzePhase.PRE;
import static ru.vm5277.common.enums.SemanticAnalyzePhase.DECLARE;
import static ru.vm5277.common.enums.SemanticAnalyzePhase.POST;
import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.scopes.CGClassScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.lexer.Delimiter;
import ru.vm5277.common.lexer.J8BKeyword;
import ru.vm5277.common.lexer.TokenType;
import ru.vm5277.common.VarType;
import ru.vm5277.common.enums.InstanceType;
import ru.vm5277.common.enums.RtosFeature;
import ru.vm5277.common.enums.StrictLevel;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.WarningMessage;
import ru.vm5277.compiler.semantic.CIScope;
import ru.vm5277.compiler.semantic.ClassScope;
import ru.vm5277.compiler.semantic.ImportableScope;
import ru.vm5277.compiler.semantic.InterfaceScope;
import ru.vm5277.compiler.semantic.MethodSymbol;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.common.lexer.Keyword;
import ru.vm5277.common.lexer.tokens.TAnnotation;
import ru.vm5277.common.messages.ErrorMessage;
import ru.vm5277.compiler.Instance;
import static ru.vm5277.compiler.Main.debugAST;

public class ClassNode extends ObjectTypeNode {
	private	ClassBlockNode	blockNode;
	private	boolean			fieldsGenerated;
	
	public ClassNode(	Instance inst, TokenBuffer tb, Map<String, AnnotationParamNode> annotations, Set<Keyword> modifiers, boolean isInner,
						List<ObjectTypeNode> importedClasses)																		throws CompileException {
		super(inst, tb, annotations, modifiers, null, importedClasses);
		
		if(null!=name) {
			VarType.addClassName(this.name, false);
		}
		
		this.isInner = isInner;
		if(!isInner) {
			inst.setPrimaryObject(this);
		}
		
		// Проверка на запрещенное наследование классов
		if(tb.match(TokenType.OOP, J8BKeyword.EXTENDS)) {
			markError("Class inheritance is not supported. Use composition instead of extends.");
			tb.skip(Delimiter.RIGHT_BRACE);
			return;
		}

		// Парсинг интерфейсов (если есть)
		if(tb.match(TokenType.OOP, J8BKeyword.IMPLEMENTS)) {
			consumeToken(tb);
			while(true) {
				try {
					String interfaceName = (String)consumeToken(tb, TokenType.IDENTIFIER).getValue();
					resolveImportFromCurrentDir(inst, tb.getSP().getSourceFile(), interfaceName);
					//TODO QualifiedPath
					impl.add(interfaceName);
				}
				catch(CompileException e) {
					markFirstError(e); // встретили что-то кроме ID интерфейса
				}
				if(!tb.match(Delimiter.COMMA)) break;
				consumeToken(tb);
			}
		}

		// Парсинг тела класса
		blockNode = new ClassBlockNode(inst, tb, this);
	}
	
	@Override
	public ClassBlockNode getBody() {
		return blockNode;
	}
	public void setBody(ClassBlockNode body) {
		blockNode = body;
	}

	@Override
	public boolean preAnalyze() {
		boolean result = true;
		debugAST(this, PRE, true, getFullInfo());
		
		try {validateName(name);} catch(CompileException e) {
			addMessage(e);
			result = false;
		}

		if(result && Character.isLowerCase(name.charAt(0))) {
			addMessage(new WarningMessage("Class name should start with uppercase letter:" + name, sp));
		}
		
		if(result) {
			try{
				validateModifiers(modifiers, J8BKeyword.PUBLIC, J8BKeyword.PRIVATE, J8BKeyword.STATIC);
			}
			catch(CompileException e) {
				addMessage(e);
				result = false;
			}
		}

		if(result && null!=imported && !isInner) {
			for(ObjectTypeNode imported : imported) {
				result &= imported.preAnalyze();
				if(!result) break;
			}
		}

		// Анализ тела класса
		if(result) {
			result &= blockNode.preAnalyze();
		}

		debugAST(this, PRE, false, result, getFullInfo());
		return result;
	}
	
	@Override
	public boolean declare(Scope scope) {
		boolean result = true;
		debugAST(this, DECLARE, true, getFullInfo());
		
		try {
			ciScope = new ClassScope(scope, name, isInner);
			if(null!=scope) {
				((ImportableScope)scope).addCI(ciScope, true);
			}

			if(null!=imported && !isInner) {
				for(ObjectTypeNode imported : imported) {
					result&=imported.declare(ciScope);

					if(result) {
						try {
							ciScope.addCI(((ObjectTypeNode)imported).getScope(), false);
						}
						catch(CompileException ex) {
							markError(ex);
							result = false;
						}
					}
				}
			}

			List<VarType> implTypes = new ArrayList<>();
			for(String ifaceName : impl) {
				if(null==ciScope.resolveCI(null, ifaceName, false, new HashSet<>())) {
					markError("Interface not found: " + ifaceName);
					result = false;
				}
				else {
					implTypes.add(VarType.fromClassName(ifaceName));
				}
			}
			
			if(result) {
				ciScope.setImplTypes(implTypes);
			}

			if(result) {
				result&=blockNode.declare(ciScope);
			}
		}
		catch(CompileException e) {
			markError(e);
			return false;
		}

		debugAST(this, DECLARE, false, result, getFullInfo());
		return result;
	}
	
	
	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg, CGScope parent) {
		boolean result = true;
		debugAST(this, POST, true, getFullInfo());
		if(null!=imported && !isInner) {
			for(ObjectTypeNode imported : imported) {
				imported.postAnalyze(ciScope, cg, parent); // Заменил scope на classScope, надо проверить
			}
		}

		List<ImplementInfo> implInfos = new ArrayList<>();
		if(!impl.isEmpty()) {
			List<InterfaceScope> iScopes = new ArrayList<>();
			for(String ifaceName : impl) {
				fillInterfaces(ciScope, iScopes, ifaceName);
			}	
			for(InterfaceScope iScope : iScopes) {
				VarType iType = VarType.fromClassName(iScope.getName());
				List<String> signatures = new ArrayList<>();
				for(MethodSymbol mSymbol : iScope.getMethods()) {
					signatures.add(mSymbol.getSignature());
				}
				implInfos.add(new ImplementInfo(iType, signatures));
			}
			
			Collections.sort(implInfos, new Comparator<ImplementInfo>() {
				@Override
				public int compare(ImplementInfo ii1, ImplementInfo ii2) {
					return Integer.compare(ii1.getType().getId(), ii2.getType().getId());
				}
			});
		}


		VarType driverType = VarType.fromClassName(VarType.CLASSNAME_RTOS_DRIVER);
		if(null!=driverType && ciScope.isImplements(driverType)) {
			isRtosDriver = true;
		}

		if(null!=annotations) {
			for(String annotation : annotations.keySet()) {
				if(	isRtosDriver && (
					annotation.equals(TAnnotation.EXPAND_HEAP) || annotation.equals(TAnnotation.REGISTER_SOFT_TIMER) ||
					annotation.equals(TAnnotation.RTOS_FEATURE))) {
					
					continue;
				}
				
				if(StrictLevel.LIGHT==inst.getStrictLevel()) {
					addMessage(new WarningMessage("Unsupported class annotation: @" + annotation, annotations.get(annotation).getSP()));
				}
				else if(StrictLevel.STRONG==inst.getStrictLevel()) {
					addMessage(new ErrorMessage("Unsupported class annotation: @" + annotation, annotations.get(annotation).getSP()));
				}
			}
		}

		// Иногда функционал фичи вынесен в нативный код, компилятор не видит использования и не включает нужную фичу - аннотацией можно укзать включить фичу
		AnnotationParamNode apn = annotations.get(TAnnotation.RTOS_FEATURE);
		if(null!=apn) {
			RtosFeature feature = null;
			try {
				feature = RtosFeature.valueOf(apn.getTextValue(0));
			}
			catch(Exception ex) {
			}
			if(null!=feature) {
				cg.setFeature(feature);
			}
			else {
				addMessage(new ErrorMessage("@" + apn.getAnnotationName() + ", unknown feature: '" + apn.getTextValue(0) + "'", apn.getSP()));
			}
		}
		
		InstanceType instType = (null == classPath ? InstanceType.NO_PARENT : InstanceType.CLASS);
		VarType timerType = VarType.fromClassName(VarType.CLASSNAME_TIMER);
		if(null!=timerType && ciScope.isImplements(timerType)) {
			instType = InstanceType.TIMER;
		}
		else {
			VarType threadType = VarType.fromClassName(VarType.CLASSNAME_THREAD);
			if(null!=threadType && ciScope.isImplements(threadType)) {
				instType = InstanceType.THREAD;
			}
			else {
				VarType callbackType = VarType.fromClassName(VarType.CLASSNAME_NATIVE_CALLBACK);
				if(null!=callbackType && ciScope.isImplements(callbackType)) {
					instType = InstanceType.NATIVE_CALLBACK;
				}
			}
		}

		cgScope = cg.enterClass(parent, sp, VarType.fromClassName(name), name, implInfos, instType);

		result &= blockNode.postAnalyze(ciScope, cg, cgScope);

		debugAST(this, POST, false, result, getFullInfo());
		return result;
	}
	
	@Override
	public void codeOptimization(Scope scope, CodeGenerator cg) {
		if(!isInner) {
			for(ObjectTypeNode node : imported) {
				node.codeOptimization(scope, cg);
			}
		}
		
		blockNode.codeOptimization(ciScope, cg);
	}

	private void fillInterfaces(CIScope scope, List<InterfaceScope> iScopes, String ifaceName) {
		CIScope cis = scope.resolveCI(null, ifaceName, false, new HashSet<>());
		if(null==cis || !(cis instanceof InterfaceScope)) {
			markError("Interface not found: " + ifaceName);
		}
		else if(!iScopes.contains((InterfaceScope)cis)) {
			checkInterfaceImplementation(ciScope, (InterfaceScope)cis);
			iScopes.add((InterfaceScope)cis);
			for(VarType type : cis.getImpl()) {
				fillInterfaces(cis, iScopes, type.getClassName());
			}
		}
	}
	
	private boolean checkInterfaceImplementation(CIScope classScope, InterfaceScope interfaceSymbol) {
		boolean allMethodsImplemented = true;
		
		// Для каждого метода в интерфейсе
		for(MethodSymbol interfaceMethod : interfaceSymbol.getMethods()) {
			boolean found = false;
			// Получаем методы класса с таким же именем
			List<MethodSymbol> classMethods = classScope.getMethods(interfaceMethod.getName());
			
			// Проверяем каждый метод класса
			if(null!=classMethods) {
				for(MethodSymbol classMethod : classMethods) {
					if(interfaceMethod.getSignature().equals(classMethod.getSignature())) {
						classMethod.setInterfaceImpl(true);
						found = true;
						if(null!=(MethodNode)classMethod.getNode()) {
							Map<String, AnnotationParamNode> ann = ((MethodNode)classMethod.getNode()).getAnnotations();
							if(null==ann || !ann.containsKey(TAnnotation.OVERRIDE)) {
								if(StrictLevel.LIGHT==inst.getStrictLevel()) {
									markWarning("'@Override' annotation is missing for method '" + classMethod.getSignature() + "' implementing '" +
												interfaceSymbol.getName() + "'", classMethod.getNode().getSP());
								}
								else if(StrictLevel.STRONG==inst.getStrictLevel()) {
									markError("'@Override' annotation is missing for method '" + classMethod.getSignature() + "' implementing '" +
												interfaceSymbol.getName() + "'", classMethod.getNode().getSP());
								}

							}
						}
						break;
					}
				}
			}

			if(!found && !interfaceMethod.isNative() && !interfaceMethod.isStatic()) {
				markError(	"Class '" + classScope.getName() + "' must implement method: " + interfaceMethod.getSignature() + 
							" from interface '" + interfaceSymbol.getName() + "'");
				allMethodsImplemented = false;
			}
			if(found && interfaceMethod.isNative()) {
				markError(	"Class '" + classScope.getName() + "' cannot override native method: " + interfaceMethod.getSignature() + 
							" from interface '" + interfaceSymbol.getName() + "'");
				allMethodsImplemented = false;
			}
		}
		return allMethodsImplemented;
	}
	
	public void fieldsCodeGen(CodeGenerator cg, CGExcs excs) throws CompileException {
		if(!fieldsGenerated && isRtosDriver) {
			fieldsGenerated = true;
			
			for(AstNode node : blockNode.getChildren()) {
				if(node instanceof FieldNode) {
					node.depCodeGen(cg, excs);
				}
			}

		}
	}

	public void firstCodeGen(CodeGenerator cg, MethodNode methodNode, CGExcs excs) throws CompileException {
		cgDone = true;
		CGScope.launchPointActivate();
		
		fieldsCodeGen(cg, excs);
		methodNode.firstCodeGen(cg, excs);
		
		AnnotationParamNode apn = annotations.get(TAnnotation.EXPAND_HEAP);
		((CGClassScope)cgScope).build(cg, true, (null==apn ? 0 : apn.getNumValue(0)), excs);
	}

	@Override
	public Object codeGen(CodeGenerator cg, boolean toAccum, CGExcs excs) throws CompileException {
		// Необходима множетсвенная генерация (размер heap мог измениться) if(cgDone || disabled) return null;
		if(disabled) return null;
		cgDone = true;
		
		fieldsCodeGen(cg, excs);
		
		AnnotationParamNode apn = annotations.get(TAnnotation.EXPAND_HEAP);
		((CGClassScope)cgScope).build(cg, false, (null==apn ? 0 : apn.getNumValue(0)), excs);
/*		if(null != importedClasses) {
			for (ClassNode imported : importedClasses) {
				if(isUsed()) imported.codeGen(cg);
			}
		}
*/

		return null;
	}
	
	@Override
	public boolean isStatic() {
		return modifiers.contains(J8BKeyword.STATIC);
	}
	@Override
	public boolean isFinal() {
		return modifiers.contains(J8BKeyword.FINAL);
	}
	@Override
	public boolean isPublic() {
		return modifiers.contains(J8BKeyword.PUBLIC);
	}
	public boolean isPrivate() {
		return modifiers.contains(J8BKeyword.PRIVATE);
	}

	public boolean isInner() {
		return isInner;
	}
	
	@Override
	public List<AstNode> getChildren() {
		return Arrays.asList(blockNode);
	}
	
	@Override
	public String toString() {
		return modifiers + ", " + name + ", " + impl;
	}

	public String getFullInfo() {
		return getClass().getSimpleName() + ": " + toString();
	}
}
