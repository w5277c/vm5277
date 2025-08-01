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

.IFNDEF OS_MUL10
;--------------------------------------------------------;
OS_MUL10:
;--------------------------------------------------------
;Умножение 8бит числа на 8бит число(быстрее чем OS_MUL8X8)
;IN: ACCUM_L-число
;OUT: ACCUM_L
;--------------------------------------------------------
	PUSH TEMP_L

	LSL ACCUM_L
	MOV TEMP_L,ACCUM_L
	LSL ACCUM_L
	LSL ACCUM_L
	ADD ACCUM_L,TEMP_L

	POP TEMP_L
	RET
.ENDIF
