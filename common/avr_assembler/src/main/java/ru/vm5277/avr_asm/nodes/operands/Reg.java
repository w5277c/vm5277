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
import ru.vm5277.avr_asm.semantic.IRegExpression;
import ru.vm5277.avr_asm.semantic.IdExpression;
import ru.vm5277.avr_asm.semantic.LiteralExpression;
import ru.vm5277.common.SourcePosition;
import ru.vm5277.common.exceptions.CompileException;

public class Reg {
	protected	int	id;
	protected	boolean	inc	= false;
	protected	boolean	dec	= false;
	
	protected Reg(int id) {
		this.id = id;
	}
	
	public Reg(Scope scope, SourcePosition sp, Expression expr) throws CompileException {
		if(expr instanceof LiteralExpression) {
			String str = (String)((LiteralExpression)expr).getValue();
			switch (str) {
				case "x": id=26; break;
				case "x+": id=26; inc=true; break;
				case "-x": id=26; dec=true; break;
				case "y": id=28; break;
				case "y+": id=28; inc=true; break;
				case "-y": id=28; dec=true; break;
				case "z": id=30; break;
				case "z+": id=30; inc=true; break;
				case "-z": id=30; dec=true; break;
				default:
					throw new CompileException("TODO ожидаем регистр, получили " + expr, sp);
			}
		}
		else if(expr instanceof IdExpression) {
			String str = (String)((IdExpression)expr).getId();
			Byte result = scope.resolveReg(str);
			if(null == result) {
				throw new CompileException("Unable to resolve register '" + str + "'", sp); 
			}
			id = result;
		}
		else if(expr instanceof IRegExpression) {
			id = ((IRegExpression)expr).getId();
		}
		else {
			throw new CompileException("TODO ожидаем регистр, получили " + expr, sp);
		}
		if(0x00>id || 0x1f<id) {
			throw new CompileException("TODO ожидаем регистр, получили " + id, sp);
		}
	}
	public Reg(byte id) {
		this.id = id;
	}
	
	public int getId() {
		return id;
	}
	
	@Override
	public String toString() {
		return Integer.toString(id);
	}
}
