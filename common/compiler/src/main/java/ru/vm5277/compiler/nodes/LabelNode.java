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

import java.util.List;
import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.scopes.CGBlockScope;
import ru.vm5277.common.cg.scopes.CGLabelScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.compiler.CodegenResult;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.semantic.BlockScope;
import ru.vm5277.compiler.semantic.LabelSymbol;
import ru.vm5277.compiler.semantic.Scope;

public class LabelNode extends AstNode {
	private	final	String			name;
	private			CGLabelScope	labelScope;

	public LabelNode(TokenBuffer tb, MessageContainer mc) {
		super(tb, mc);

		this.name = consumeToken(tb).getStringValue(); // Гарантирован вызывающим
	}

	public String getName() {
		return name;
	}

	@Override
	public boolean preAnalyze() {
		return true;
	}
	@Override
	public boolean declare(Scope scope) {
		if (!(scope instanceof BlockScope)) markError("Labels can only be declared in block scope");
        
		try {
			symbol = new LabelSymbol(name, scope);
			((BlockScope)scope).addLabel((LabelSymbol)symbol);
		}
		catch (CompileException e) {markError(e);}

		return true;
	}
	
	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		cgScope = cg.enterCommand();
		
		labelScope = ((CGBlockScope)cgScope.getScope(CGBlockScope.class)).addLabel(name);
		
		if(!((LabelSymbol)symbol).isUsed()) {
			markWarning("Unused label '" + name + "'");
		}

		((LabelSymbol)symbol).setCGScopes(cgScope, labelScope);
		
		// TODO Контроль достижимости кода после return/break/continue
		cg.leaveCommand();
		return true;
	}

	@Override
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean toAccum, CGExcs excs) throws CompileException {
		CodegenResult result = null;
		
		CGScope cgs = null == parent ? cgScope : parent;
		cgs.append(labelScope);
		return result;
	}

	public CGLabelScope getLabelScope() {
		return labelScope;
	}
	
	@Override
	public String toString() {
		return "label: " + name;
	}

	@Override
	public List<AstNode> getChildren() {
		return null;
	}
}