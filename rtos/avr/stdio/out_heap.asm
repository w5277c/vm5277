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

;Выводим блок памяти на STDIO порт
.IFNDEF OS_OUT_HEAP
	.include "stdio/out_rambytes.asm"
	.include "stdio/out_char.asm"
;-----------------------------------------------------------
OS_OUT_HEAP:
;-----------------------------------------------------------
;Выводим кучу объекта
;IN: Z-адрес начала кучи
;-----------------------------------------------------------
	PUSH_T16
	PUSH_Z
	PUSH ACCUM_L

	MOV ACCUM_L,ZH
	MCALL OS_OUT_HEX8
	MOV ACCUM_L,ZL
	MCALL OS_OUT_HEX8
	LDI ACCUM_L,':'
	MCALL OS_OUT_CHAR
	LDD TEMP_L,Z+0x00
	LDD TEMP_H,Z+0x01
	MCALL OS_OUT_RAMBYTES_NR

	LDI ACCUM_L,'\n'
	MCALL OS_OUT_CHAR

	POP ACCUM_L
	POP_Z
	POP_T16
	RET
.endif
