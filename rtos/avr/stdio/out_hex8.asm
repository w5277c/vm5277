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

.include "stdio/out_char.asm"

.IFNDEF OS_OUT_HEX8
;-----------------------------------------------------------
OS_OUT_HEX8:
;-----------------------------------------------------------
;Вывод в шестнадцатеричной форме числа(8 бит)
;IN: ACCUM_L-8b число
;-----------------------------------------------------------
	PUSH ACCUM_L
	
	ANDI ACCUM_L,0x0f
	CPI ACCUM_L,0x0a
	BRCS PC+0x02
	SUBI ACCUM_L,low(-0x27)									;+0x27
	SUBI ACCUM_L,low(-0x30)									;+0x30
	MCALL OS_OUT_CHAR

	POP ACCUM_L
	PUSH ACCUM_L

	SWAP ACCUM_L
	ANDI ACCUM_L,0x0f
	CPI ACCUM_L,0x0a
	BRCS PC+0x02
	SUBI ACCUM_L,low(-0x27)									;+0x27
	SUBI ACCUM_L,low(-0x30)									;+0x30
	MCALL OS_OUT_CHAR

	POP ACCUM_L
	RET
.ENDIF
