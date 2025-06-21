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
.IFNDEF STK_FREE
;--------------------------------------------------------
STK_FREE:
;--------------------------------------------------------
;Освобождаем в стеке блок памяти
;OUT: Y-восстановленный Y, сохраненный перед
;вызовом STK_ALLOC!
;--------------------------------------------------------
	PUSH TEMP_L
	LDS TEMP_L,SREG
	CLI

	POP ATOM_EL
	POP ATOM_L
	POP ATOM_H

.IFDEF SPH
	POP YH
.ENDIF
	POP YL

	OUT SPL,_SPL
.IFDEF SPH
	OUT SPH,_SPH
.ENDIF

	PUSH ATOM_H
	PUSH ATOM_L
	PUSH ATOM_EL

	STS SREG,TEMP_L
	POP TEMP_L
	RET
.ENDIF
