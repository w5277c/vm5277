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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import ru.vm5277.compiler.nodes.expressions.ExpressionNode;
import java.util.Set;
import ru.vm5277.common.LabelNames;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.lexer.Delimiter;
import ru.vm5277.common.lexer.J8BKeyword;
import ru.vm5277.common.lexer.Operator;
import static ru.vm5277.common.enums.SemanticAnalyzePhase.DECLARE;
import static ru.vm5277.common.enums.SemanticAnalyzePhase.POST;
import static ru.vm5277.common.enums.SemanticAnalyzePhase.PRE;
import ru.vm5277.common.StrUtils;
import ru.vm5277.common.cg.CGBranch;
import ru.vm5277.common.cg.CGCells;
import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.common.cg.scopes.CGCellsScope;
import ru.vm5277.common.cg.scopes.CGClassScope;
import ru.vm5277.common.cg.scopes.CGFieldScope;
import ru.vm5277.common.cg.scopes.CGLabelScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.VarType;
import ru.vm5277.common.enums.StrictLevel;
import ru.vm5277.common.exceptions.CompileException;
import static ru.vm5277.compiler.Main.debugAST;
import ru.vm5277.compiler.nodes.expressions.ArrayExpression;
import ru.vm5277.compiler.nodes.expressions.EnumExpression;
import ru.vm5277.compiler.nodes.expressions.LiteralExpression;
import ru.vm5277.compiler.nodes.expressions.NewArrayExpression;
import ru.vm5277.compiler.nodes.expressions.NewExpression;
import ru.vm5277.compiler.nodes.expressions.VarFieldExpression;
import ru.vm5277.compiler.nodes.expressions.bin.BinaryExpression;
import ru.vm5277.compiler.semantic.CIScope;
import ru.vm5277.compiler.semantic.ClassScope;
import ru.vm5277.compiler.semantic.FieldSymbol;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.compiler.semantic.InitNodeHolder;
import ru.vm5277.common.lexer.Keyword;
import ru.vm5277.common.lexer.SourceBuffer;
import ru.vm5277.common.lexer.tokens.TAnnotation;
import ru.vm5277.common.lexer.tokens.TNote;
import ru.vm5277.compiler.Instance;
import ru.vm5277.compiler.nodes.expressions.ArrayInitExpression;
import ru.vm5277.compiler.semantic.InterfaceScope;

public class FieldNode extends AstNode implements InitNodeHolder {
	private	final	ObjectTypeNode						objectTypeNode;
	private	final	Map<String, AnnotationParamNode>	annotations;
	private	final	Set<Keyword>						modifiers;
	private			VarType								type;
	private			String								name;
	private			ExpressionNode						init;
	private			byte[]								rawData; // Альтернатива init, используется для аннотаций @FlashData и @Melody
	
	public FieldNode(	Instance inst, TokenBuffer tb, ObjectTypeNode objectTypeNode, Map<String, AnnotationParamNode> annotations, Set<Keyword> modifiers,
						VarType type, String name) {
		super(inst, tb);
		
		this.objectTypeNode = objectTypeNode;
		this.annotations = annotations;
		this.modifiers = modifiers;
		this.type = type;
		this.name = name;

		if (!tb.match(Operator.ASSIGN)) {
            init = null;
        }
		else {
			consumeToken(tb);
			try {
				init = new ExpressionNode(inst, tb).parse();
			}
			catch(CompileException e) {markFirstError(e);}
		}
        try {consumeToken(tb, Delimiter.SEMICOLON);}catch(CompileException e) {markFirstError(e);}
	}

	public VarType getType() {
		return type;
	}
	
	public String getName() {
		return name;
	}
	
	public ExpressionNode getInitializer() {
		return init;
	}
	
	public Set<Keyword> getModifiers() {
		return modifiers;
	}
	public boolean isStatic() {
		return modifiers.contains(J8BKeyword.STATIC);
	}
	public boolean isFinal() {
		return modifiers.contains(J8BKeyword.FINAL);
	}
	public boolean isPublic() {
		return modifiers.contains(J8BKeyword.PUBLIC);
	}
	public boolean isNative() {
		return modifiers.contains(J8BKeyword.NATIVE);
	}
	public boolean isPrivate() {
		return modifiers.contains(J8BKeyword.PRIVATE);
	}
	
	@Override
	public boolean preAnalyze() {
		boolean result = true;
		debugAST(this, PRE, true, getFullInfo());
		
		try{
			validateModifiers(modifiers, J8BKeyword.STATIC, J8BKeyword.FINAL, J8BKeyword.PRIVATE, J8BKeyword.PUBLIC);
		}
		catch(CompileException e) {
			addMessage(e);
			result = false;
		}
		
		if(isFinal() && isStatic()) {
			if(!name.equals(name.toUpperCase())) {
				markWarning("Static final field name should in UPPER_CASE: " + name);
			}
		}
		else if(Character.isUpperCase(name.charAt(0))) {
			markWarning("Field name should start with lowercase letter: " + name);
		}

		if(null!=annotations) {
			for(String annotation : annotations.keySet()) {
				if(annotation.equals(TAnnotation.MELODY)) {
					if(null!=init) {
						markError("@Melody cannot be used on a field with an initializer");
						result = false;
					}
					else if(type.isArray() && VarType.BYTE==type.getElementType() && 0x01==type.getArrayDepth()) {
						try {
							TNote tnote = new TNote(new SourceBuffer(annotations.get(TAnnotation.MELODY).getTextValue(0), 0x04));
							rawData = (byte[])tnote.getValue();
							VarType arrType = VarType.arrayOf(VarType.BYTE, 1);
							init = new NewArrayExpression(inst, null, sp, arrType, new ArrayList<>(), new ArrayInitExpression(inst, rawData, sp, arrType));
						}
						catch(CompileException ex) {
							markError(ex);
							result = false;
						}
					}
					else if(type==VarType.fromClassName(VarType.CLASSNAME_FLASH_DATA)) {
						try {
							TNote tnote = new TNote(new SourceBuffer(annotations.get(TAnnotation.MELODY).getTextValue(0), 0x04));
							rawData = (byte[])tnote.getValue();
						}
						catch(CompileException ex) {
							markError(ex);
							result = false;
						}
					}
					else {
						markError("@Melody can't be applied to field type '" + type.toString() + "'");
						result = false;
					}
				}
				else if(annotation.equals(TAnnotation.FLASH_DATA)) {
					if(null!=init) {
						markError("@FlashData cannot be used on a field with an initializer");
						result = false;
					}
					else if(!type.isClassType() || !type.getClassName().equals(VarType.CLASSNAME_FLASH_DATA)) {
						markError("@FlashData can only be applied to a FlashData field type");
						result = false;
					}
					else {
						AnnotationParamNode apn = annotations.get(TAnnotation.FLASH_DATA);
						rawData = apn.getBytes(0);
					}
				}
				else if(objectTypeNode instanceof ClassNode && annotation.equals(TAnnotation.NATIVE)) {
					if(!isStatic()) {
						markError("@Native cannot be used on non-static field");
						result = false;
					}
				}
				else {
					if(StrictLevel.LIGHT==inst.getStrictLevel()) {
						markWarning("Unsupported field annotation: @" + annotation);
					}
					else if(StrictLevel.STRONG==inst.getStrictLevel()) {
						markError("Unsupported field annotation: @" + annotation);
						result = false;
					}
				}
			}
		}

		if(null!=init) {
			result&=init.preAnalyze();
			
			if(init instanceof VarFieldExpression) {
				if(name.equals(((VarFieldExpression)init).getName())) {
					markError("Self-assignment for '" + ((VarFieldExpression)init).getName() + "' has no effect");
					result = false;
				}
			}
		}
		
		debugAST(this, PRE, false, result, getFullInfo());
		return result;
	}

	
	@Override
	public boolean declare(Scope scope) {
		boolean result = true;
		debugAST(this, DECLARE, true, getFullInfo());

		if(null!=init) {
			if(init instanceof NewArrayExpression) {
				try {
					NewArrayExpression nae = (NewArrayExpression)init;
					if(null != nae.getType()) {
						if(type.getArrayDepth() != nae.getType().getArrayDepth()) {
							markError("Array dimension mismatch, expected:" + type.getArrayDepth() + ", found:" + nae.getType().getArrayDepth());
							result = false;
						}
						else {
							type = nae.getType();
							// Проверяем вложенность массивов
							if(type.getArrayDepth()>3) {
								markError("Array nesting depth exceeds maximum allowed (3)");
								result = false;
							}
						}
					}
				}
				catch(Exception ex){}
			}
			
			result&=init.declare(scope);
		}
		
		if(result) {
			if(scope instanceof ClassScope) {
				ClassScope classScope = (ClassScope)scope;
				boolean isFinal = modifiers.contains(J8BKeyword.FINAL);
				symbol = new FieldSymbol(name, type, isFinal || VarType.CSTR == type, isStatic(), isPrivate(), classScope, this);

				try{classScope.addField(symbol);}
				catch(CompileException e) {markError(e);}
			}
			else if(scope instanceof InterfaceScope) {
				InterfaceScope ifaceScope = (InterfaceScope)scope;
				symbol = new FieldSymbol(name, type, true || VarType.CSTR == type, true, false, ifaceScope, this);

				try{ifaceScope.addField(symbol);}
				catch(CompileException e) {markError(e);}
			}
			else {
				markError("Unexpected scope:" + scope.getClass().getSimpleName() + " in filed:" + name);
			}
		}

		// Если указана аннотация @Native то поле не может быть оптимизировано как final
		if(null!=annotations.get(TAnnotation.NATIVE)) {
			symbol.setReassigned();
		}
		
		debugAST(this, DECLARE, false, result, getFullInfo() + (declarationPendingNodes.containsKey(this) ? " [DP]" : ""));
		return result;
	}

	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg, CGScope parent) {
		boolean result = true;
		debugAST(this, POST, true, getFullInfo() + " type:" + type);

		try {
			cgScope = cg.enterField(parent, sp, type, isStatic(), !type.isArray() && isFinal(), name);
			symbol.setCGScope(cgScope);
			
			// Проверка инициализации final-полей
			if(isFinal() && init==null && (!type.isClassType() || !type.getClassName().equals(VarType.CLASSNAME_FLASH_DATA))) {
				markError("Final field '" + name + "' must be initialized");
				result = false;
			}

			// Анализ инициализатора, если есть
			if(null!=init) {
				if(!init.postAnalyze(scope, cg, cgScope)) {
					result = false;
				}
				else {
					// Резолвинг QualifiedPathExpression
					ExpressionNode resolved = resolveQualifiedPathExpr(init);
					if(null!=resolved) {
						init = resolved;
					}
					
					((CGCellsScope)cgScope).setArrayView(init instanceof ArrayExpression);
					// Проверка совместимости типов
					VarType initType = init.getType();
					if(initType.isClassType() && type.isClassType()) {
						if(initType!=type) {
							CIScope cis = scope.getThis().resolveCI(null, initType.getClassName(), false, new HashSet<>());
							if(!cis.isImplements(type)) {
								markError("Type mismatch: cannot assign " + initType + " to " + type);
								result = false;
							}
						}
					}
					else if(!isCompatibleWith(scope, type, initType)) {
						// Дополнительная проверка автоматического привдения целочисленной константы к fixed.
						if(VarType.FIXED == type && init instanceof LiteralExpression && initType.isInteger()) {
							long num = ((LiteralExpression)init).getNumValue();
							if(num<VarType.FIXED_MIN || num>VarType.FIXED_MAX) {
								markError("Type mismatch: cannot assign " + initType + " to " + type);
								result = false;
							}
						}
						else {
							markError("Type mismatch: cannot assign " + initType + " to " + type);
							result = false;
						}
					}

					if(result) {
						// Дополнительная проверка на сужающее преобразование
						if(type.isNumeric() && initType.isNumeric() && type.getSize() < initType.getSize()) {
							markError("Narrowing conversion from " + initType + " to " + type + " requires explicit cast");
							result = false;
						}
					}
				}
			}
			
			if(result) {
				if(type.isClassType() && type.getClassName().equals(VarType.CLASSNAME_FLASH_DATA)) {
					if(null==rawData && null==annotations.get(TAnnotation.FLASH_DATA)) {
						result = false;
						markError("FlashData field without @FlashData annotation");
					}
				}
				else if(null!=annotations.get(TAnnotation.FLASH_DATA)) {
					result = false;
					markError("Invalid field type for @FlashData annotation, FlashData type is required");
				}
			}
			
			if(result) {
				AnnotationParamNode apn = annotations.get(TAnnotation.NATIVE);
				if(null!=apn) {
					if(!((ClassNode)objectTypeNode).isRTOSDriver()) {
						if(StrictLevel.LIGHT==inst.getStrictLevel()) {
							markWarning("Unsupported field annotation: @" + apn.getAnnotationName());
						}
						else if(StrictLevel.STRONG==inst.getStrictLevel()) {
							markError("Unsupported field annotation: @" + apn.getAnnotationName());
							result = false;
						}
					}
					if(result && symbol.isFinal()) {
						markError("@Native cannot be used on final field");
						result = false;
					}
				}
			}
		}
		catch (CompileException e) {
			markError(e);
		}
		
		debugAST(this, POST, false, result, getFullInfo());
		return result;
	}
	
	@Override
	public void codeOptimization(Scope scope, CodeGenerator cg) {
		if(null!=init) {
			init.codeOptimization(scope, cg);
		
			try {
				ExpressionNode optimizedExpr = init.optimizeWithScope(scope, cg);
				if(null != optimizedExpr) {
					init = optimizedExpr;
				}
			}
			catch(CompileException ex) {
				markError(ex);
			}

			if(!symbol.isReassigned()) {
				// Возможно упростить?
				if(	init instanceof LiteralExpression || init.getType().isEnum() ||
					(init instanceof VarFieldExpression && null!=((VarFieldExpression)init).getSymbol() &&
					((VarFieldExpression)init).getSymbol().isFinal())) {

					modifiers.add(J8BKeyword.FINAL);
					symbol.setFinal(true);
					if(type.isArray() || VarType.CSTR==type) {
						symbol.setReassigned();
					}
				}
			}
			try {
				ExpressionNode optimizedExpr = init.optimizeWithScope(scope, cg);
				if(null != optimizedExpr) {
					init = optimizedExpr;
				}
			}
			catch(CompileException ex) {
				markError(ex);
			}
		}
	}
	
	@Override
	public Object codeGen(CodeGenerator cg, boolean toAccum, CGExcs excs) throws CompileException {
		if(cgDone || disabled) return null;
		cgDone = true;

		Boolean accUsed = null;
		CGFieldScope fScope = (CGFieldScope)cgScope;		

		if(type.isClassType() && type.getClassName().equals(VarType.CLASSNAME_FLASH_DATA)) {
			fScope.setCells(new CGCells(cg.defineData(fScope.getResId(), rawData.length, rawData).getLabel()));
		}

		// Игнорируем отстройку кода для пимитивов объявленных как final
		if(!isFinal() || !type.isPrimitive()) {
		
			CGScope cScope = fScope.getParent();

			if(cg.getDevice().isLowRAM() && StrictLevel.NONE!=inst.getStrictLevel() && !isStatic()) {
				if(StrictLevel.STRONG==inst.getStrictLevel()) {
					markError("HEAP usage on low memory device " + cg.getDevice().getUniqueName());
				}
				else {
					markWarning("HEAP usage on low memory device " + cg.getDevice().getUniqueName());
				}
			}

			objectTypeNode.codeGen(cg, false, excs);
			((CGFieldScope)cgScope).build();

			if(isStatic() && objectTypeNode instanceof ClassNode) {
				AnnotationParamNode apn = annotations.get(TAnnotation.NATIVE);
				if(null!=apn) {
					if(!cg.addStaticField(apn.getTextValue(0), fScope.getCells().getId(0))) {
						markError("@Native '" + apn.getTextValue(0) + "' already defined");
					}
				}
			}
			
			VarType castType = cg._getAcc().pullCast();
			
			if(null!=init && (!type.isClassType() || !type.getClassName().equals(VarType.CLASSNAME_FLASH_DATA))) {
				if(isStatic()) {
					CGLabelScope cgLabelScope = new CGLabelScope(null, -1, LabelNames.ENDPOINT, true);
					excs.setMethodEndLabel(cgLabelScope);
				}
				else {
					excs.setMethodEndLabel(((CGClassScope)cgScope.getParent()).getInstInitEndLabel());
				}
			}
			
			if(VarType.CSTR==type) {
				if(init instanceof LiteralExpression) {
					LiteralExpression le = (LiteralExpression)init;
					if(VarType.CSTR == le.getType()) {
						fScope.setCells(new CGCells(cg.defineData(fScope.getResId(), -1, (String)le.getValue()).getLabel()));
					}
				}
				else throw new CompileException("unexpected expression:" + init + " for constant");
			}
			else if(type.isClassType() && type.getClassName().equals(VarType.CLASSNAME_FLASH_DATA)) {
				// Выполнено выше
			}
			else {
				// Инициализация(заполнение нулями необходима только регистрам, остальные проинициализированы вместе с HEAP/STACK)
				if(null==init) {
					// Ничего не делаем, инициализация(заполнение нулями необходима только регистрам, остальные проинициализированы вместе с HEAP/STACK)
				}
				else if(init instanceof LiteralExpression) { // Не нужно вычислять, можно сразу сохранять не используя аккумулятор
					LiteralExpression le = (LiteralExpression)init;
					cg.cellsToCells(cgScope, fScope.getCells(), type,
									new CGCells(le.getType().isFixedPoint() ? le.getFixedValue() : le.getNumValue()), null, castType);
					//cgScope.append(cg.constToCells(null, isFixed ? le.getFixedValue() : le.getNumValue(), ((CGFieldScope)cgScope).getCells(), isFixed));
				}
				else if(init instanceof NewExpression) {
					init.codeGen(cg, true, excs);
					cg.cellsToCells(cgScope, fScope.getCells(), type, new CGCells(CGCells.Type.ACC), null, castType);
					accUsed = true;
				}
				else if(init instanceof NewArrayExpression) {
					init.codeGen(cg, true, excs);
					//При инициализации счетчику ссылок сразу назначаем 1 (не допускаем создание висящих объектов)
					cg.cellsToCells(cgScope, fScope.getCells(), type, new CGCells(CGCells.Type.ACC), null, castType);
				}
				else if(init instanceof BinaryExpression) {
	//!!!				CGScope oldScope = cg.setScope(cgScope);
					CGBranch branch = new CGBranch();
					cgScope.setBranch(branch);

					init.codeGen(cg, true, excs);
					//TODO проверить на объекты и на массивы(передача ref)
					VarType initType = init.getType();
					if(VarType.CLASS == initType) {
						cg.pushHeapReg(cgScope);
						cg.updateClassRefCount(cgScope, fScope.getCells(), true);
						cg.popHeapReg(cgScope);
					}
					else if(initType.isArray()) {
						cg.updateArrRefCount(cgScope, fScope.getCells(), true);
					}
					if(branch.isUsed()) {
						//cg.constToAcc(cgScope, 1, 1, false);
						cg.cellsToCells(cgScope, new CGCells(CGCells.Type.ACC), VarType.BOOLEAN, new CGCells(1), VarType.BOOLEAN, castType);
						CGLabelScope lbScope = new CGLabelScope(null, null, LabelNames.LOCGIC_END, true);
						cg.jump(cgScope, lbScope);
						cgScope.append(((CGBranch)branch).getEnd());
						cg.cellsToCells(cgScope, new CGCells(CGCells.Type.ACC), VarType.BOOLEAN, new CGCells(0), VarType.BOOLEAN, castType);
						cgScope.append(lbScope);
						cg.cellsToCells(cgScope, fScope.getCells(), type, new CGCells(CGCells.Type.ACC), null, castType);
					}
					cg.cellsToCells(cgScope, fScope.getCells(), type, new CGCells(CGCells.Type.ACC), null, castType);
					accUsed = true;
				}
				else if(init instanceof EnumExpression) {
					if(0!=((EnumExpression)init).getIndex()) { // Поле уже проинициализировано значением 0
						cg.cellsToCells(cgScope, fScope.getCells(), type, new CGCells(((EnumExpression)init).getIndex()), null, castType);
						//cgScope.append(cg.constToCells(cgScope, ((EnumExpression)init).getIndex(), fScope.getCells(), false));
					}
				}
				else {
					boolean optimized = false;
					try {
						if(	init instanceof VarFieldExpression && null!=init.getSymbol() && null!=init.getSymbol().getCGScope() &&
							init.getSymbol().getCGScope() instanceof CGCellsScope && null!=((CGCellsScope)init.getSymbol().getCGScope()).getCells()) {

							init.codeGen(cg, false, excs);
							cg.cellsToCells(cgScope, fScope.getCells(), type,
											((CGCellsScope)init.getSymbol().getCGScope()).getCells(), init.getType(), castType);
							optimized = true;
						}
					}
					catch(Exception ex) {
						System.out.println("TODO:" + ex.getMessage());
						ex.printStackTrace();
					}

					if(!optimized) {
						System.out.println("TODO: artifact in FieldNode");
						init.codeGen(cg, true, excs);
						cg.cellsToCells(cgScope, fScope.getCells(), type, new CGCells(CGCells.Type.ACC), null, castType);
						accUsed = true;
					}

					//TODO проверить на объекты и на массивы(передача ref)
					VarType initType = init.getType();
					if(VarType.CLASS == initType) {
						cg.pushHeapReg(cgScope);
						cg.updateClassRefCount(cgScope, fScope.getCells(), true);
						cg.popHeapReg(cgScope);
					}
					else if(initType.isArray()) {
						cg.updateArrRefCount(cgScope, fScope.getCells(), true);
					}
				}

				if(null!=init) {
					cgScope.getParent().remove(cgScope);
					if(isStatic()) {
						((CGClassScope)cScope).getStatFieldsInitCont().append(cgScope);
					}
					else {
						((CGClassScope)cScope).getFieldsInitCont().append(cgScope);
					}
				}
			}
		}

		return accUsed;
	}

	@Override
	public List<AstNode> getChildren() {
		if(null != init) return Arrays.asList(init);
		return null;
	}
	
	@Override
	public ExpressionNode getInitNode() {
		return init;
	}
	void setInitializer(ExpressionNode init) {
		this.init = init;
	}
	
	public ObjectTypeNode getObjectTypeNode() {
		return objectTypeNode;
	}

	@Override
	public String toString() {
		return (StrUtils.toString(modifiers) + " " + type + " " + name).trim();
	}

	public String getFullInfo() {
		return getClass().getSimpleName() + " " + toString();
	}
}
