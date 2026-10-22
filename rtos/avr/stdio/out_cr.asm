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

.IF OS_FT_STDOUT == 0x01
.include "stdio/out_char.asm"

.IFNDEF OS_OUT_CR
;--------------------------------------------------------
OS_OUT_CR:
;--------------------------------------------------------
;Перевод строки
;--------------------------------------------------------
	PUSH ACCUM_L

	LDI ACCUM_L,0x0a
	MCALL OS_OUT_CHAR

	POP ACCUM_L
	RET
.ENDIF
.ENDIF
