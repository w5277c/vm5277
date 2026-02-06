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

;TODO научить компилятор подставлять константы методу (таймаут в 0 для J8BPROC_STDIN_READW) и оставить два метода в native_bindings.cfg

.IFNDEF J8BPROC_STDIN_READ

	.INCLUDE "./j8b/stdin_readw.asm"

;-----------------------------------------------------------
J8BPROC_STDIN_READ:
;-----------------------------------------------------------
;Возвращаем символ из STDIN или ждем бесконечно
;OUT: ACCUM_L-символ
;-----------------------------------------------------------
	PUSH ACCUM_H
	LDI ACCUM_H,0x00
	LDI ACCUM_L,0x00
	MCALL J8BPROC_STDIN_READW
	POP ACCUM_H
	RET
.ENDIF
