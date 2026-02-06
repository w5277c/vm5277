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

.include "math/mul8.asm"
.IFNDEF OS_DIVA8BY10
;--------------------------------------------------------
OS_DIVA8BY10:
;--------------------------------------------------------
;Деление 8b числа на 10
;IN: ACCUM_L-8b число
;OUT: ACCUM_L-8b результат
;--------------------------------------------------------
	PUSH ACCUM_H

	LDI ACCUM_H,205													;204.8
	MCALL OS_MUL8
	MOV ACCUM_L,ACCUM_H
	LSR ACCUM_L
	LSR ACCUM_L
	LSR ACCUM_L

	POP ACCUM_H
	RET
.ENDIF
