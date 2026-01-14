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

package ru.vm5277.common.lexer.tokens;

import ru.vm5277.common.lexer.SourceBuffer;
import ru.vm5277.common.lexer.TokenType;
import ru.vm5277.common.lexer.Operator;
import ru.vm5277.common.lexer.SourcePosition;

public class TOpearator extends Token {
	private TOpearator(SourceBuffer sb, SourcePosition sp, Operator op) {
		super(sb, sp);
		
		this.type = TokenType.OPERATOR;
		this.value = op;
		this.length = sb.getPos()-sp.getPos();
	}

	public static TOpearator parse(SourceBuffer sb) {
		SourcePosition sp = sb.snapSP();
		
		// Проверяем трехсимвольные операторы (если есть)
        if(sb.available(3)) {
            Operator op = Operator.fromSymbol(sb.peekString(3));
            if(null!=op) {
				sb.next(3);
				TOpearator result = new TOpearator(sb, sp, op);
				return result;
			}
        }
        
        // Проверяем двухсимвольные операторы
        if(sb.available(2)) {
            Operator op = Operator.fromSymbol(sb.peekString(2));
            if(null!=op) {
				sb.next(2);
				TOpearator result = new TOpearator(sb, sp, op);
				return result;
			}
        }
        
		// Проверяем односимвольные операторы
        if(sb.available()) {
            Operator op = Operator.fromSymbol(sb.peekString(1));
            if(null!=op) {
				sb.next();
				TOpearator result = new TOpearator(sb, sp, op);
				return result;
			}
        }
		return null;
	}
}
