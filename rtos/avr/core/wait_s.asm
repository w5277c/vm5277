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
.include "./core/wait_ms.asm"
.IFNDEF OS_WAIT_S
;--------------------------------------------------------
OS_WAIT_S:
;--------------------------------------------------------
;Ждем
;IN ACCUM_H, ACCUM_L - время в 1s
;--------------------------------------------------------
	PUSH ACCUM_H
	PUSH ACCUM_L
	PUSH_T16

	MOV TEMP_H,ACCUM_H
	MOV TEMP_L,ACCUM_L
	LDI ACCUM_H,high(1000);
	LDI ACCUM_L,low(1000);

_OS_WAIT_S__LOOP:
	SUBI TEMP_L,0x01
	SBCI TEMP_H,0x00
	BRCS _OS_WAIT_S__END
	MCALL OS_WAIT_MS
	RJMP _OS_WAIT_S__LOOP

_OS_WAIT_S__END:
	POP_T16
	POP ACCUM_L
	POP ACCUM_H
	RET
.ENDIF
