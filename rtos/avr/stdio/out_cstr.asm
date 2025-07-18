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
.IFNDEF OS_OUT_CSTR
;--------------------------------------------------------
OS_OUT_CSTR:
;--------------------------------------------------------
;Логирование строки, конец определяется по 0x00
;IN: Z-адрес FLASH на строку
;--------------------------------------------------------
	PUSH ACCUM_L
	PUSH_Z

_OS_OUT_CSTR__LOOP:
	;Считываем байт
	LPM ACCUM_L,Z+
	CPI ACCUM_L,0x00
	BREQ _OS_OUT_CSTR__END
	MCALL OS_OUT_CHAR
	RJMP _OS_OUT_CSTR__LOOP

_OS_OUT_CSTR__END:
	POP_Z
	POP ACCUM_L
	RET
.ENDIF
