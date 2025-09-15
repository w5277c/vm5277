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
package ru.vm5277.compiler.nodes.expressions;

import ru.vm5277.common.Operator;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.scopes.CGBranchScope;
import ru.vm5277.common.cg.scopes.CGCellsScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.cg.scopes.CGVarScope;
import ru.vm5277.common.compiler.CodegenResult;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.semantic.AliasSymbol;
import ru.vm5277.compiler.semantic.ClassSymbol;
import ru.vm5277.compiler.semantic.FieldSymbol;
import ru.vm5277.compiler.semantic.InterfaceScope;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.compiler.semantic.Symbol;
import ru.vm5277.compiler.semantic.VarSymbol;

public class VarFieldExpression extends ExpressionNode {
    private final	String	value;
	private			Scope	scope;
	
    public VarFieldExpression(TokenBuffer tb, MessageContainer mc, String value) {
        super(tb, mc);
        
		this.value = value;
    }
    
	public String getValue() {
		return value;
	}
	
	@Override
	public VarType getType(Scope scope) throws CompileException {
		if (symbol == null) {
			symbol = scope.resolveSymbol(value);
			if(null != symbol) {
				return symbol.getType();
			}
			else {
				InterfaceScope iScope = scope.getThis().resolveScope(value);
				if(null != iScope) {
					//symbol = new Symbol(value, VarType.fromClassName(value), false, false);
					symbol = new ClassSymbol(value, VarType.fromClassName(value), false, false, iScope);
				}
			}
        }
        if(null == symbol) throw new CompileException("Can't resolve: " + value);
		return symbol.getType();
	}
	
	@Override
	public boolean preAnalyze() {
		if (null == value || value.isEmpty()) {
			markError("Variable name cannot be empty");
			return false;
		}
		return true;
	}

	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		boolean result = true;
		try {
			this.scope = scope;
			
			cgScope = cg.enterExpression();
			if (symbol == null) {
				symbol = scope.resolveSymbol(value);
				if(null == symbol) {
					InterfaceScope iScope = scope.getThis().resolveScope(value);
					if(null != iScope) {
						// TODO что здесь делает ClassSymbol? 
						symbol = new ClassSymbol(value, VarType.fromClassName(value), false, false, iScope);
					}
				}
			}
		}
		catch (CompileException e) {markError(e); result = false;}
		if (null == symbol) {
			markError("Variable '" + value + "' is not declared");
			result = false;
		}
		
		if(symbol instanceof VarSymbol) ((VarSymbol)symbol).markUsed();
		
		cg.leaveExpression();
		return result;
	}
	
	@Override
	public Symbol getSymbol() {
		// Переменная/поле могут быть переданы в вызываемый метод(в котором данное выражение), но на этапе семантики мы получаем Symbol построенный на
		// параметрах метода(которые по понятным причинам не содержат значение)
		// Значение появляется позже(на этапе кодогенерации в MethodCallExpression) в виде VarSymbol/FieldSymbol(вероятно также AliasSymbol, если не рудимент)
		// Здесь мы проверяем на Symbol, и обновляем symbol на актуальное значение.

		if(symbol instanceof VarSymbol || symbol instanceof FieldSymbol || symbol instanceof AliasSymbol) {
			return symbol;
		}
		if(null != scope) { //TODO вероятно всегда null
			symbol = scope.resolveSymbol(value);
		}
		return symbol;
	}

	
	@Override
	public Object codeGen(CodeGenerator cg, boolean accumStore) throws Exception {
		CGScope oldCGScope = cg.setScope(cgScope);
		
		// Актуализируем symbol
		getSymbol();
		
		// Выполняет запись значения в аккумулятор. Но зачастую это не требуется, достаточно вызвать depCodeGen
		if(null == depCodeGen(cg)) {
			if(symbol instanceof AliasSymbol) {
				Symbol  vSymbol = scope.resolveSymbol(value);
				while(vSymbol instanceof AliasSymbol) {
					symbol = ((AliasSymbol)vSymbol).getSymbol();
					vSymbol = scope.resolveSymbol(symbol.getName());
					if(null != vSymbol && null != vSymbol.getCGScope()) {
						depCodeGen(cg);
						if(accumStore) {
							CGVarScope vScope = (CGVarScope)vSymbol.getCGScope();
							cg.cellsToAcc(cgScope, vScope);
							//Назначаем алиас ссылающийся на реальную переменную
						}
						symbol = vSymbol;
						break;
					}
				}
			}
			else {
				if(symbol.getCGScope() instanceof CGCellsScope) {
					if(accumStore) cg.cellsToAcc(cgScope, (CGCellsScope)symbol.getCGScope());
				}
				else {
					throw new CompileException("Unsupported scope: " + symbol.getCGScope());
				}
			}
		}
		cg.setScope(oldCGScope);
		return (accumStore ? CodegenResult.RESULT_IN_ACCUM : null);
	}
	
	public void codeGen(CodeGenerator cg, boolean isInvert, boolean opOr, CGBranchScope brScope) throws Exception {
		depCodeGen(cg);
		
		if(null != brScope) {
			CGCellsScope cScope = (CGCellsScope)symbol.getCGScope();
			cg.constCond(cgScope, cScope.getCells(), Operator.NEQ, 0, isInvert, opOr, brScope);
		}
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + ": " + value;
	}
}