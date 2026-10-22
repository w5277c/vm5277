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
import ru.vm5277.common.cg.CGCells;
import ru.vm5277.common.cg.CGBranch;
import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.common.cg.scopes.CGCellsScope;
import ru.vm5277.common.cg.scopes.CGLabelScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.cg.scopes.CGVarScope;
import ru.vm5277.common.VarType;
import ru.vm5277.common.cg.scopes.CGBlockScope;
import ru.vm5277.common.cg.scopes.CGCondBlockScope;
import ru.vm5277.common.cg.scopes.CGLoopBlockScope;
import ru.vm5277.common.enums.CodegenResult;
import ru.vm5277.common.enums.StrictLevel;
import ru.vm5277.common.exceptions.CompileException;
import static ru.vm5277.compiler.Main.debugAST;
import ru.vm5277.compiler.nodes.expressions.ArrayExpression;
import ru.vm5277.compiler.nodes.expressions.EnumExpression;
import ru.vm5277.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.compiler.nodes.expressions.LiteralExpression;
import ru.vm5277.compiler.nodes.expressions.NewArrayExpression;
import ru.vm5277.compiler.nodes.expressions.NewExpression;
import ru.vm5277.compiler.nodes.expressions.VarFieldExpression;
import ru.vm5277.compiler.nodes.expressions.bin.BinaryExpression;
import ru.vm5277.compiler.semantic.BlockScope;
import ru.vm5277.compiler.semantic.CIScope;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.compiler.semantic.VarSymbol;
import ru.vm5277.compiler.semantic.InitNodeHolder;
import ru.vm5277.common.lexer.Keyword;
import ru.vm5277.common.lexer.SourceBuffer;
import ru.vm5277.common.lexer.tokens.TAnnotation;
import ru.vm5277.common.lexer.tokens.TNote;
import ru.vm5277.compiler.Instance;
import ru.vm5277.compiler.nodes.expressions.ArrayInitExpression;
import ru.vm5277.compiler.nodes.expressions.PropertyExpression;

public class VarNode extends AstNode implements InitNodeHolder {
	private	final	Map<String, AnnotationParamNode>	annotations;
	private	final	Set<Keyword>						modifiers;
	private			VarType								type;
	private	final	String								name;
	private			ExpressionNode						init;
	
	public VarNode(Instance inst, TokenBuffer tb, Map<String, AnnotationParamNode> annotations, Set<Keyword> modifiers, VarType type, String name) {
		super(inst, tb);
		
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
	
	public boolean isStatic() {
		return modifiers.contains(J8BKeyword.STATIC);
	}

	public boolean isFinal() {
		return modifiers.contains(J8BKeyword.FINAL);
	}

	public Set<Keyword> getModifiers() {
		return modifiers;
	}
	
	@Override
	public boolean preAnalyze() {
		boolean result = true;
		debugAST(this, PRE, true, getFullInfo());
		
		try{
			validateModifiers(modifiers, J8BKeyword.STATIC, J8BKeyword.FINAL);
		}
		catch(CompileException e) {
			addMessage(e);
			result = false;
		}
		
		if(Character.isUpperCase(name.charAt(0))) {
			markWarning("Variable name should start with lowercase letter:" + name);
		}

		if(null!=annotations) {
			for(String annotation : annotations.keySet()) {
				if(!annotation.equals(TAnnotation.MELODY)) {
					if(StrictLevel.LIGHT==inst.getStrictLevel()) {
						markWarning("Unsupported var annotation: @" + annotation);
					}
					else if(StrictLevel.STRONG==inst.getStrictLevel()) {
						markError("Unsupported var annotation: @" + annotation);
						result = false;
					}
				}
				else {
					if(null!=init) {
						markError("@Melody cannot be used on a variable with an initializer");
						result = false;
					}
					else if(!type.isArray() || VarType.BYTE!=type.getElementType() || 0x01!=type.getArrayDepth()) {
						markError("@Melody can only be applied to a one-dimensional byte array (byte[])");
						result = false;
					}
					else {
						try {
							TNote tnote = new TNote(new SourceBuffer(annotations.get(TAnnotation.MELODY).getTextValue(0), 0x04));
							byte[] data = (byte[])tnote.getValue();
							VarType arrType = VarType.arrayOf(VarType.BYTE, 1);
							init = new NewArrayExpression(inst, null, sp, arrType, new ArrayList<>(), new ArrayInitExpression(inst, data, sp, arrType));
						}
						catch(CompileException ex) {
							markError(ex);
							result = false;
						}
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
			if(scope instanceof BlockScope) {
				BlockScope blockScope = (BlockScope)scope;
				symbol = new VarSymbol(name, type, isFinal() || VarType.CSTR == type, isStatic(), scope, this);

				try {blockScope.addVariable(symbol);}
				catch(CompileException e) {markError(e);}

	//			if(init instanceof NewArrayExpression) {
	//				((VarSymbol)symbol).setArrayDimensions(((NewArrayExpression)init).getConsDimensions());
	//			}
			}
			else {
				markError("Unexpected scope:" + scope.getClass().getSimpleName() + " in var:" + name);
			}
		}

		debugAST(this, DECLARE, false, result, getFullInfo() + (declarationPendingNodes.containsKey(this) ? " [DP]" : ""));
		return result;
	}

	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg, CGScope parent) {
		boolean result = true;
		debugAST(this, POST, true, getFullInfo() + " type:" + type);

		try {
			cgScope = cg.enterLocal(parent, sp, type, (type.isReferenceType() ? cg.getRefSize() : type.getSize()), VarType.CSTR == type, name);
			symbol.setCGScope(cgScope);
			
			((CGBlockScope)parent).addVar((CGVarScope)cgScope); // Перенесено из CGVarScope.build

			// Проверка инициализации final-полей
			if(isFinal() && init==null) {
				markError("Final variable  '" + name + "' must be initialized");
				result = false;
			}

			// Анализ инициализатора, если есть
			if(null!=init) {
				result&=init.postAnalyze(scope, cg, cgScope);
				if(result) {
					// Резолвинг QualifiedPathExpression
					ExpressionNode resolved = resolveQualifiedPathExpr(init);
					if(null!=resolved) {
						init = resolved;
					}
				}
				
				if(result) {
					((CGCellsScope)cgScope).setArrayView(init instanceof ArrayExpression);
					// Проверка совместимости типов
					VarType initType = init.getType();
					if(null==initType) {
						markError("Type is null");
						result = false;
					}
					if(result && initType.isClassType() && type.isClassType()) {
						if(initType!=type) {
							CIScope cis = scope.getThis().resolveCI(null, initType.getClassName(), false, new HashSet<>());
							if(!cis.isImplements(type)) {
								markError("Type mismatch: cannot assign " + initType + " to " + type);
								result = false;
							}
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
				if(null!=optimizedExpr) {
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
				if(null!=optimizedExpr) {
					init = optimizedExpr;
				}
			}
			catch(CompileException ex) {
				markError(ex);
			}

			if(!isCompatibleWith(scope, type, init.getType())) {
				// Дополнительная проверка автоматического привдения целочисленной константы к fixed.
				if(VarType.FIXED == type && init instanceof LiteralExpression && init.getType().isInteger()) {
					long num = ((LiteralExpression)init).getNumValue();
					if(num<VarType.FIXED_MIN || num>VarType.FIXED_MAX) {
						markError("Type mismatch: cannot assign " + init.getType() + " to " + type);
					}
				}
				else {
					markError("Type mismatch: cannot assign " + init.getType() + " to " + type);
				}
			}
		}
	}
	
	@Override
	public Object codeGen(CodeGenerator cg, boolean toAccum, CGExcs excs) throws CompileException {
		// Позволяет обнаружить недописанную логику - все обращения к кодогенерации переменной должны содержать скоп вызывающего блока
		throw new UnsupportedOperationException();
	}
	
	public Object codeGen(CodeGenerator cg, boolean toAccum, BlockScope callerScope, CGExcs excs) throws CompileException {
		if(cgDone || disabled) return null;
		cgDone = true;

		if(null==callerScope) { // Позволяет обнаружить недописанную логику - все обращения к кодогенерации переменной должны содержать скоп вызывающего блока
			throw new CompileException("TODO: " + getFullInfo() + " callerScope is null");
		}
		
		CGVarScope vScope = (CGVarScope)cgScope;
		
		VarSymbol vSymbol = (VarSymbol)symbol;
		// Постройка списка участвующих переменных должна осуществляться на уровне первого обращения к переменной, а не на уровне инициализации
		// Иначе при вложенных блоках мы увидим переменные только первого блока
		Set<VarSymbol> varsInRange = ((BlockScope)callerScope).getVarsInRange(vSymbol.getNode().getSN(), vSymbol.getLastAccessedSN());
		Set<CGCells> usedRegsCells = new HashSet<>();
		for(VarSymbol vs : varsInRange) {
			CGCellsScope cgCellsScope = (CGCellsScope)vs.getCGScope();
			if(null!=cgCellsScope && null!=cgCellsScope.getCells() && CGCells.Type.REG==cgCellsScope.getCells().getType()) {
				usedRegsCells.add(cgCellsScope.getCells());
			}
		}
		
		VarType castType = cg._getAcc().pullCast();
		
		Boolean accUsed = null;
		vScope.build(usedRegsCells);
		if(VarType.CSTR == type) {
			if(init instanceof LiteralExpression) {
				LiteralExpression le = (LiteralExpression)init;
				if(VarType.CSTR == le.getType()) {
					vScope.setCells(new CGCells(cg.defineData(vScope.getResId(), -1, (String)le.getValue()).getLabel()));
				}
			}
			else throw new CompileException("unexpected expression:" + init + " for constant");
		}
		else {
			// Инициализация(заполнение нулями необходима только регистрам, остальные проинициализированы вместе с HEAP/STACK)
			if(null==init) {
				if(CGCells.Type.REG==vScope.getCells().getType()) {
					cg.cellsToCells(cgScope, vScope.getCells(), type, new CGCells(0), null, castType);
					//cgScope.append(cg.constToCells(cgScope, 0, vScope.getCells(), false));
				}
			}
			else if(init instanceof LiteralExpression) { // Не нужно вычислять, можно сразу сохранять не используя аккумулятор
				LiteralExpression le = (LiteralExpression)init;
				cg.cellsToCells(cgScope, vScope.getCells(), type,
								new CGCells(le.getType().isFixedPoint() ? le.getFixedValue() : le.getNumValue()), null, castType);
				//cgScope.append(cg.constToCells(cgScope, isFixed ? le.getFixedValue() : le.getNumValue(), vScope.getCells(), isFixed));
			}
			else if(init instanceof NewExpression) {
				init.codeGen(cg, true, excs);
				cg.cellsToCells(cgScope, vScope.getCells(), type, new CGCells(CGCells.Type.ACC), null, castType);
			}
			else if(init instanceof NewArrayExpression) {
				init.codeGen(cg, true, excs);
				//При инициализации счетчику ссылок сразу назначаем 1 (не допускаем создание висящих объектов)
				//cg.updateRefCount(vScope, vScope.getCells(), true);
				
				//Перенес в declare //((VarSymbol)symbol).setArrayDimensions(((NewArrayExpression)initializer).getConsDimensions());
				cg.cellsToCells(cgScope, vScope.getCells(), type, new CGCells(CGCells.Type.ACC), null, castType);
			}
//			else if(init instanceof ArrayExpression) {
//				init.codeGen(cg, null, true);
//				//cg.arrToCells(cgs, (CGArrCells)((CGCellsScope)init.getSymbol().getCGScope()).getCells(), vScope.getCells());
//				//cg.arrToCells(cgs, (CGArrCells)((CGCellsScope)init.getSymbol().getCGScope()).getCells(), vScope.getCells());
//			}
			else if(init instanceof BinaryExpression) {
//!!!				CGScope oldScope = cg.setScope(cgScope);
				CGBranch branch = new CGBranch();
				cgScope.setBranch(branch);
				
				init.codeGen(cg, true, excs);
				//TODO проверить на объекты и на массивы(передача ref)
				VarType initType = init.getType();
				if(VarType.CLASS == initType) {
					cg.pushHeapReg(cgScope);
					cg.updateClassRefCount(cgScope, vScope.getCells(), true);
					cg.popHeapReg(cgScope);
				}
				else if(initType.isArray()) {
					cg.updateArrRefCount(cgScope, vScope.getCells(), true);
				}
				if(branch.isUsed()) {
					//cg.constToAcc(cgScope, 1, 1, false);
					cg.cellsToCells(cgScope, new CGCells(CGCells.Type.ACC), VarType.BOOLEAN, new CGCells(1), VarType.BOOLEAN, castType);
					CGLabelScope lbScope = new CGLabelScope(null, null, LabelNames.LOCGIC_END, true);
					cg.jump(cgScope, lbScope);
					cgScope.append(((CGBranch)branch).getEnd());
					//cg.constToAcc(cgScope, 1, 0, false);
					cg.cellsToCells(cgScope, new CGCells(CGCells.Type.ACC), VarType.BOOLEAN, new CGCells(0), VarType.BOOLEAN, castType);
					cgScope.append(lbScope);
					cg.cellsToCells(cgScope, vScope.getCells(), type, new CGCells(CGCells.Type.ACC), null, castType);
					//cg.accToCells(cgScope, type, vScope);
				}
				cg.cellsToCells(cgScope, vScope.getCells(), type, new CGCells(CGCells.Type.ACC), null, castType);
				//cg.accToCells(cgScope, type, vScope);
				accUsed = true;
//!!!				cg.setScope(oldScope);
			}
			else if(init instanceof ArrayExpression) {
				try {
					if( null!=init.getSymbol() && null!=init.getSymbol().getCGScope() &&
						init.getSymbol().getCGScope() instanceof CGCellsScope && null!=((CGCellsScope)init.getSymbol().getCGScope()).getCells()) {
					
						if(type.isArray()) {
							((ArrayExpression)init).codeGen(cg, true, true, excs);
							// Если назначение - массив, то результат будет в аккумуляторе (byte[] b2 = b1[3];)
							cg.cellsToCells(cgScope, vScope.getCells(), type, new CGCells(CGCells.Type.ACC), null, castType);
							accUsed = true;
						}
						else {
							// Иначе назначение это примитив или ссылка на объект (byte octet = data[index];)
							((ArrayExpression)init).codeGen(cg, false, false, excs);
							cg.cellsToCells(cgScope, vScope.getCells(), type,
											((CGCellsScope)init.getSymbol().getCGScope()).getCells(), init.getType(), castType);
						}
					}
				}
				catch(Exception ex) {
					System.out.println("TODO:" + ex.getMessage());
					ex.printStackTrace();
				}

				// Здесь мы инкрементируем ссылку если в переменную типа CLASS передается элменет массива (а не подмассив)
				if(!type.isArray() && VarType.CLASS==type) {
					cg.pushHeapReg(cgScope);
					cg.updateClassRefCount(cgScope, vScope.getCells(), true);
					cg.popHeapReg(cgScope);
				}
				if(type.isArray()) {
					// Выполняем инкремент ссылки массива если создается view (byte[] b2 = b1[3];)
					cg.updateArrRefCount(cgScope, vScope.getCells(), true);
				}
			}
			else if(init instanceof EnumExpression) {
				cg.cellsToCells(cgScope, vScope.getCells(), type, new CGCells(((EnumExpression)init).getIndex()), null, castType);
				//cgScope.append(cg.constToCells(cgScope, ((EnumExpression)init).getIndex(), vScope.getCells(), false));
			}
			else if(init instanceof VarFieldExpression && null!=init.getSymbol() && null!=init.getSymbol().getCGScope() &&
					init.getSymbol().getCGScope() instanceof CGCellsScope && null!=((CGCellsScope)init.getSymbol().getCGScope()).getCells()) {

				init.codeGen(cg, false, excs);
				cg.cellsToCells(cgScope, vScope.getCells(), type, ((CGCellsScope)init.getSymbol().getCGScope()).getCells(), init.getType(), castType);

				if(VarType.CLASS == init.getType()) {
					cg.pushHeapReg(cgScope);
					cg.updateClassRefCount(cgScope, vScope.getCells(), true);
					cg.popHeapReg(cgScope);
				}
				else if(init.getType().isArray()) {
					cg.updateArrRefCount(cgScope, vScope.getCells(), true);
				}
			}
			else { // Вызом метода, cast и прочее попадающее под общую логику (использующее аккумулятор)
				Object _result = init.codeGen(cg, true, excs);
				if(CodegenResult.RESULT_IN_FLAG==_result) {
					cg.cellsToCells(cgScope, vScope.getCells(), type, new CGCells(CGCells.Type.FLAG), VarType.BOOLEAN, castType);
				}
				else if(CodegenResult.RESULT_IN_ACCUM==_result) {
					cg.cellsToCells(cgScope, vScope.getCells(), type, new CGCells(CGCells.Type.ACC), init.getType(), castType);
				}
				else {
					throw new CompileException("Accum not used for expr:" + init);
				}
				accUsed = true;

				if(VarType.CLASS == init.getType()) {
					cg.pushHeapReg(cgScope);
					cg.updateClassRefCount(cgScope, vScope.getCells(), true);
					cg.popHeapReg(cgScope);
				}
				else if(init.getType().isArray()) {
					cg.updateArrRefCount(cgScope, vScope.getCells(), true);
				}
			}
		}

		releaseVars(cg);
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
	
	@Override
	public String toString() {
		return (StrUtils.toString(modifiers) + " " + type + " " + name).trim();
	}

	public String getFullInfo() {
		return getClass().getSimpleName() + " " + toString();
	}
}
