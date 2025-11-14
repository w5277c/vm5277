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

.IFNDEF OS_DIV32P2
;-----------------------------------------------------------
;Деление 32бит числа на степень двойки(на базе метки)
;IN: ACCUM_L/H/EL/EH-32b число,
;OUT: ACCUM_L/H/EL/EH-32b результат
;-----------------------------------------------------------
OS_DIV32P2_X128:
	LSR ACCUM_L
	ROR ACCUM_H
	ROR ACCUM_EL
	ROR ACCUM_EH
OS_DIV32P2_X64:
	LSR ACCUM_L
	ROR ACCUM_H
	ROR ACCUM_EL
	ROR ACCUM_EH
OS_DIV32P2_X32:
	LSR ACCUM_L
	ROR ACCUM_H
	ROR ACCUM_EL
	ROR ACCUM_EH
OS_DIV32P2_X16:
	LSR ACCUM_L
	ROR ACCUM_H
	ROR ACCUM_EL
	ROR ACCUM_EH
OS_DIV32P2_X8:
	LSR ACCUM_L
	ROR ACCUM_H
	ROR ACCUM_EL
	ROR ACCUM_EH
OS_DIV32P2_X4:
	LSR ACCUM_L
	ROR ACCUM_H
	ROR ACCUM_EL
	ROR ACCUM_EH
OS_DIV32P2_X2:
	LSR ACCUM_L
	ROR ACCUM_H
	ROR ACCUM_EL
	ROR ACCUM_EH
	RET
.ENDIF
