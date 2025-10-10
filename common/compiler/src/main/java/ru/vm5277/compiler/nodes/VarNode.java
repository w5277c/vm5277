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

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import ru.vm5277.common.LabelNames;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.compiler.Delimiter;
import ru.vm5277.compiler.Keyword;
import ru.vm5277.common.Operator;
import ru.vm5277.common.cg.CGCells;
import ru.vm5277.common.cg.scopes.CGBranchScope;
import ru.vm5277.common.cg.scopes.CGLabelScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.cg.scopes.CGVarScope;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.common.messages.WarningMessage;
import ru.vm5277.compiler.nodes.expressions.ArrayExpression;
import ru.vm5277.compiler.nodes.expressions.BinaryExpression;
import ru.vm5277.compiler.nodes.expressions.EnumExpression;
import ru.vm5277.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.compiler.nodes.expressions.FieldAccessExpression;
import ru.vm5277.compiler.nodes.expressions.LiteralExpression;
import ru.vm5277.compiler.nodes.expressions.NewArrayExpression;
import ru.vm5277.compiler.nodes.expressions.NewExpression;
import ru.vm5277.compiler.nodes.expressions.UnresolvedReferenceExpression;
import ru.vm5277.compiler.nodes.expressions.VarFieldExpression;
import ru.vm5277.compiler.semantic.BlockScope;
import ru.vm5277.compiler.semantic.InterfaceScope;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.compiler.semantic.VarSymbol;
import ru.vm5277.compiler.semantic.InitNodeHolder;

public class VarNode extends AstNode implements InitNodeHolder {
	private	final	Set<Keyword>	modifiers;
	private			VarType			type;
	private	final	String			name;
	private			ExpressionNode	init;
	
	public VarNode(TokenBuffer tb, MessageContainer mc, Set<Keyword> modifiers, VarType type, String name) {
		super(tb, mc);
		
		this.modifiers = modifiers;
		this.type = type;
		this.name = name;

		if (!tb.match(Operator.ASSIGN)) {
            init = null;
        }
		else {
			consumeToken(tb);
			try {
				init = new ExpressionNode(tb, mc).parse();
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
		return modifiers.contains(Keyword.STATIC);
	}

	public boolean isFinal() {
		return modifiers.contains(Keyword.FINAL);
	}

	public Set<Keyword> getModifiers() {
		return modifiers;
	}
	
	@Override
	public String getNodeType() {
		return "var";
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + ": " + modifiers + ", " + type + ", " + name;
	}

	@Override
	public boolean preAnalyze() {
		boolean result = true;
		
		try{
			validateModifiers(modifiers, Keyword.STATIC, Keyword.FINAL);
		}
		catch(CompileException e) {
			addMessage(e);
		}
		
		if(Character.isUpperCase(name.charAt(0))) addMessage(new WarningMessage("Variable name should start with lowercase letter:" + name, sp));

		if(null != init) {
			result &= init.preAnalyze();
			
			if(init instanceof VarFieldExpression) {
				if(name.equals(((VarFieldExpression)init).getValue())) {
					markError("Self-assignment for '" + ((VarFieldExpression)init).getValue() + "' has no effect");
					result = false;
				}
			}
		}
		return result;
	}

	
	@Override
	public boolean declare(Scope scope) {
		if(null != init) {
			if(init instanceof NewArrayExpression) {
				try {
					NewArrayExpression nae = (NewArrayExpression)init;
					if(null != nae.getType(scope)) {
						if(type.getArrayDepth() != nae.getType(scope).getArrayDepth()) {
							markError("Array dimension mismatch, expected:" + type.getArrayDepth() + ", found:" + nae.getType(scope).getArrayDepth());
							return false;
						}
						type = nae.getType(scope);
						// Проверяем вложенность массивов
						if (type.getArrayDepth() > 3) {
							markError("Array nesting depth exceeds maximum allowed (3)");
							return false;
						}
					}
				}
				catch(Exception ex){}
			}
			if(!init.declare(scope)) {	
				return false;
			}
		}
		
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

		return true;
	}

	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		try {

			cgScope = cg.enterLocal(type, (-1 == type.getSize() ? cg.getRefSize() : type.getSize()), VarType.CSTR == type, name);
			symbol.setCGScope(cgScope);
			
			// Проверка инициализации final-полей
			if (isFinal() && init == null) markError("Final variable  '" + name + "' must be initialized");

			// Анализ инициализатора, если есть
			if (init != null) {
				if(!init.postAnalyze(scope, cg)) {
					cg.leaveLocal();
					return false;
				}
				if(init instanceof UnresolvedReferenceExpression) {
					init = ((UnresolvedReferenceExpression)init).getResolvedExpr();
				}

				// Проверка совместимости типов
				VarType initType = init.getType(scope);
				if(initType.isClassType() && type.isClassType()) {
					if(initType != type) {
						InterfaceScope iScope = scope.getThis().resolveScope(initType.getClassName());
						if(!iScope.isImplements(type)) {
							markError("Type mismatch: cannot assign " + initType + " to " + type);
							cg.leaveLocal();
							return false;
						}
					}
				}
				else if (!isCompatibleWith(scope, type, initType)) {
					// Дополнительная проверка автоматического привдения целочисленной константы к fixed.
					if(VarType.FIXED == type && init instanceof LiteralExpression && initType.isInteger()) {
						long num = ((LiteralExpression)init).getNumValue();
						if(num<VarType.FIXED_MIN || num>VarType.FIXED_MAX) {
							markError("Type mismatch: cannot assign " + initType + " to " + type);
							cg.leaveLocal();
							return false;
						}
					}
					else {
						markError("Type mismatch: cannot assign " + initType + " to " + type);
						cg.leaveLocal();
						return false;
					}
				}

				// Дополнительная проверка на сужающее преобразование
				if (type.isNumeric() && initType.isNumeric() && type.getSize() < initType.getSize()) { //TODO верятно нужно и в других местах
					markError("Narrowing conversion from " + initType + " to " + type + " requires explicit cast"); 
					cg.leaveLocal();
					return false;
				}

				ExpressionNode optimizedExpr = init.optimizeWithScope(scope, cg);
				if(null != optimizedExpr) {
					init = optimizedExpr; //TODO не передаю созданный Symbol?
				}

				if(!symbol.isReassigned()) {
					if(	init instanceof LiteralExpression ||
						(init instanceof VarFieldExpression && null!=((VarFieldExpression)init).getSymbol() &&
						((VarFieldExpression)init).getSymbol().isFinal())) {

						modifiers.add(Keyword.FINAL);
						symbol.setFinal(true);
						if(type.isArray() || VarType.CSTR==type) {
							symbol.setReassigned();
						}
					}
				}
			}
		}
		catch (CompileException e) {
			markError(e);
			cg.leaveLocal();
			return false;
		}
		
		cg.leaveLocal();
		return true;
	}
	
	@Override
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean toAccum) throws Exception {
		if(cgDone || disabled) return null;
		cgDone = true;

		CGVarScope vScope = (CGVarScope)cgScope;
		
		Boolean accUsed = null;
		vScope.build();
		if(VarType.CSTR == type) {
			if(init instanceof LiteralExpression) {
				LiteralExpression le = (LiteralExpression)init;
				if(VarType.CSTR == le.getType(null)) {
					vScope.setDataSymbol(cg.defineData(vScope.getResId(), -1, (String)le.getValue()));
					//TODO рудимент?
					//symbol.getConstantOperand().setValue(cgScope.getResId());
				}
			}
			else throw new Exception("unexpected expression:" + init + " for constant");
		}
		else {
			// Инициализация(заполнение нулями необходима только регистрам, остальные проинициализированы вместе с HEAP/STACK)
			if(null == init) {
				if(CGCells.Type.REG==vScope.getCells().getType()) cgScope.append(cg.constToCells(cgScope, 0, vScope.getCells(), false));
			}
			else if(init instanceof LiteralExpression) { // Не нужно вычислять, можно сразу сохранять не используя аккумулятор
				LiteralExpression le = (LiteralExpression)init;
				boolean isFixed = le.isFixed() || VarType.FIXED == vScope.getType();
				cgScope.append(cg.constToCells(cgScope, isFixed ? le.getFixedValue() : le.getNumValue(), vScope.getCells(), isFixed));
			}
			else if(init instanceof FieldAccessExpression) {
				cgScope.append(cg.constToCells(cgScope, 0, vScope.getCells(), false));
				accUsed = true;
			}
			else if(init instanceof NewExpression) {
				init.codeGen(cg, null, true);
				cg.accCast(null, VarType.SHORT);
				cg.accToCells(cgScope, vScope);
			}
			else if(init instanceof NewArrayExpression) {
				init.codeGen(cg, null, true);
				//TODO оптимизация, можно сразу назначать счетчику 1
				//cg.updateRefCount(vScope, vScope.getCells(), true);
				
				//Перенес в declare //((VarSymbol)symbol).setArrayDimensions(((NewArrayExpression)initializer).getConsDimensions());
				cg.accCast(null, VarType.SHORT);
				cg.accToCells(cgScope, vScope);
			}
//			else if(init instanceof ArrayExpression) {
//				init.codeGen(cg, null, true);
//				//cg.arrToCells(cgScope, (CGArrCells)((CGCellsScope)init.getSymbol().getCGScope()).getCells(), vScope.getCells());
//				//cg.arrToCells(cgScope, (CGArrCells)((CGCellsScope)init.getSymbol().getCGScope()).getCells(), vScope.getCells());
//			}
			else if(init instanceof BinaryExpression) {
				CGScope oldScope = cg.setScope(cgScope);
				CGScope brScope = cg.enterBranch();
				init.codeGen(cg, brScope, true);
				//TODO проверить на объекты и на массивы(передача ref)
				VarType initType = init.getType(null);
				if(VarType.CLASS == initType) {
					cg.updateClassRefCount(brScope, vScope.getCells(), true);
				}
				else if(initType.isArray()) {
					cg.updateArrRefCount(brScope, vScope.getCells(), true, init instanceof ArrayExpression);
				}
				if(((CGBranchScope)brScope).isUsed()) {
					cg.constToAcc(brScope, 1, 1, false);
					CGLabelScope lbScope = new CGLabelScope(null, null, LabelNames.LOCGIC_END, true);
					cg.jump(brScope, lbScope);
					brScope.append(((CGBranchScope)brScope).getEnd());
					cg.constToAcc(brScope, 1, 0, false);
					brScope.append(lbScope);
					cg.accToCells(brScope, vScope);
				}
				accUsed = true;
				cg.leaveBranch();
				cg.setScope(oldScope);
			}
			else if(init instanceof ArrayExpression) {
				init.codeGen(cg, cgScope, false);
				cg.accToCells(cgScope, vScope);
				VarType initType = init.getType(null);
				if(VarType.CLASS == initType) {
					cg.updateClassRefCount(cgScope, vScope.getCells(), true);
				}
				else if(initType.isArray()) {
					cg.updateArrRefCount(cgScope, vScope.getCells(), true, init instanceof ArrayExpression);
				}
				accUsed = true;
			}
			else if(init instanceof EnumExpression) {
				cgScope.append(cg.constToCells(cgScope, ((EnumExpression)init).getIndex(), vScope.getCells(), false));
			}
			else {
				init.codeGen(cg, cgScope, true);
				cg.accToCells(cgScope, vScope);
				VarType initType = init.getType(null);
				if(VarType.CLASS == initType) {
					cg.updateClassRefCount(cgScope, vScope.getCells(), true);
				}
				else if(initType.isArray()) {
					cg.updateArrRefCount(cgScope, vScope.getCells(), true, init instanceof ArrayExpression);
				}

				accUsed = true;
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
}