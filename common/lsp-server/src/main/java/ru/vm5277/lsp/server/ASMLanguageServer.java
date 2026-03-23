/*
 * Copyright 2026 konstantin@5277.ru
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

package ru.vm5277.lsp.server;

import java.util.HashSet;
import java.util.Set;
import ru.vm5277.common.lexer.ExternalTokenProvider;
import ru.vm5277.common.lexer.SourceBuffer;
import ru.vm5277.common.lexer.Lexer;
import ru.vm5277.common.lexer.LexerType;
import ru.vm5277.common.lexer.SourcePosition;
import ru.vm5277.common.lexer.TokenType;
import ru.vm5277.common.lexer.tokens.Token;

public class ASMLanguageServer implements LanguageServer {
	private	final	static	String		instrStr	=
		"ADD,ADC,ADIW,SUB,SUBI,SBC,SBCI,SBIW,AND,ANDI,OR,ORI,EOR,COM,NEG,SBR,CBR,INC,DEC,TST,CLR,SER,MUL,MULS,MULSU,FMUL,FMULSU," +
		"RJMP,IJMP,JMP,RCALL,ICALL,CALL,RET,RETI,CPSE,CP,CPC,CPI,SBRC,SBRS,SBIC,SBIS,BRBS,BRBC,BREQ,BRNE,BRCS,BRCC,BRSH,BRLO,BRMI,BRPL,BRGE,BRLT,BRHS,BRHC,BRTS,BRTC,BRVS,BRVC,BRIE,BRID," +
		"SBI,CBI,LSL,LSR,ROL,ROR,ASR,SWAP,BSET,BCLR,BST,BLD,SEC,CLC,SEN,CLN,SEZ,CLZ,SEI,CLI,SES,CLS,SEV,CLV,SET,CLT,SEH,CLH," +
		"MOV,MOVW,LDI,LD,LDD,LDS,ST,STD,STS,LPM,SPM,IN,OUT,PUSH,POP," +
		"NOP,SLEEP,WDR,BREAK";
	private	final	static	Set<String>	instrSet	= new HashSet<>();
	private					Lexer		lexer		= new Lexer(LexerType.ASM, tokenProvider);

	static {
		String parts[] = instrStr.split("\\,");
		for(String part : parts) {
			instrSet.add(part.toLowerCase().trim());
		}
	}
	
	private	final	static	ExternalTokenProvider	tokenProvider	= new ExternalTokenProvider() {
		@Override
		public Token getExternalToken(SourceBuffer sb, SourcePosition sp, String str) {
			if(instrSet.contains(str.toLowerCase())) {
				return new Token(sb, sp, TokenType.MNEMONIC, str.toLowerCase());
			}
			return null;
		}
	};

	@Override
	public LSPToken readNextToken(SourceBuffer sb) {
		Token token = lexer.parseToken(sb);
		if(null==token || TokenType.EOF==token.getType()) {
			return null;
		}
		return new LSPToken(token);
	}
}
