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

.IFNDEF OS_MUL8P2
;-----------------------------------------------------------
;Умножение 8бит числа на степень двойки(на базе метки)
;IN: ACCUM_L-8b число,
;OUT: ACCUM_L-8b результат
;-----------------------------------------------------------
OS_MUL8P2_X128:
	LSL ACCUM_L
OS_MUL8P2_X64:
	LSL ACCUM_L
OS_MUL8P2_X32:
	LSL ACCUM_L
OS_MUL8P2_X16:
	SWAP ACCUM_L
	ANDI ACCUM_L,0xf0
	RET
OS_MUL8P2_X8:
	LSL ACCUM_L
OS_MUL8P2_X4:
	LSL ACCUM_L
OS_MUL8P2_X2:
	LSL ACCUM_L
	RET
.ENDIF
