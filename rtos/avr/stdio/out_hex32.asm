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

.IFNDEF OS_OUT_HEX32
.include "stdio/out_hex8.asm"
;-----------------------------------------------------------
OS_OUT_HEX32:
;-----------------------------------------------------------
;Вывод в шестнадцатеричной форме числа(32 бит)
;IN: ACCUM_L/H/EL/EH-32b число
;-----------------------------------------------------------
	PUSH ACCUM_L
	MOV ACCUM_L,ACCUM_EH
	MCALL OS_OUT_HEX8
	MOV ACCUM_L,ACCUM_EL
	MCALL OS_OUT_HEX8
	MOV ACCUM_L,ACCUM_H
	MCALL OS_OUT_HEX8
	POP ACCUM_L
	MCALL OS_OUT_HEX8
	RET
.ENDIF
