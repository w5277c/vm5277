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

.IFNDEF J8BPROC_STDIN_READW

	.INCLUDE "./core/wait.asm"
;-----------------------------------------------------------
J8BPROC_STDIN_READW:
;-----------------------------------------------------------
;Возвращаем символ из STDIN или ждем до истечения таймаута
;IN: ACCUM_L/H - таймаут в мс
;OUT: ACCUM_L-символ, флаг Z=true - TimeoutException
;-----------------------------------------------------------
	PUSH_T16
	PUSH FLAGS
	
	LDI FLAGS,0x00
	OR FLAGS,ACCUM_L
	OR FLAGS,ACCUM_H

	MOVW TEMP_L,ACCUM_L
	LSR TEMP_H
	ROR TEMP_L
	LSR TEMP_H
	ROR TEMP_L
J8BPROC_STDIN_READW__LOOP:
	MCALL OS_STDIN_GET
	BRNE J8BPROC_STDIN_READW__END
	MCALL OS_WAIT
	CLC
	CPSE FLAGS,C0x00
	SBIW TEMP_L,0x01
	BRCC J8BPROC_STDIN_READW__LOOP

	SEZ
J8BPROC_STDIN_READW__END:
	POP FLAGS
	POP_T16
	RET
.ENDIF
