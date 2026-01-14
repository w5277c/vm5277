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
package ru.vm5277.avr_asm.nodes.operands;

import ru.vm5277.avr_asm.scope.Scope;
import ru.vm5277.avr_asm.semantic.Expression;
import ru.vm5277.common.lexer.SourcePosition;
import ru.vm5277.common.exceptions.CompileException;

public class IOReg {
	private	int	id;

	public IOReg(Scope scope, SourcePosition sp, Expression expr) throws CompileException {
		id = Expression.getLong(expr, sp).intValue();
		if(0>id || 0x1f<id) {
			throw new CompileException("TODO ожидаем IO регистр(0-31), получили " + id, sp);
		}
	}
	
	public int getId() {
		return id;
	}
}
