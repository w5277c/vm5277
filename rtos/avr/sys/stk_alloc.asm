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
.IFNDEF STK_ALLOC
;--------------------------------------------------------
STK_ALLOC:
;--------------------------------------------------------
;Выделяем в стеке блок памяти
;IN: Y-размер в байтах
;OUT: Y-адрес на выделенный блок памяти
;--------------------------------------------------------
	PUSH TEMP_L
	LDS TEMP_L,SREG
	CLI

.IFDEF SPH
	IN ATOM_L,SPL
	IN ATOM_H,SPH
	ADIW ATOM_L,0x03
	MOVW _SPL,ATOM_L
	SUB ATOM_L,YL
	SBC ATOM_H,YH
.ELSE
	IN ATOM_L,SPL
	SUBI ATOM_L,low(0x100-0x02)
	MOV _SPL,ATOM_L
	SUB ATOM_L,YL
.ENDIF

	POP ATOM_EL
	POP YL
	POP YH

	OUT SPL,ATOM_L
.IFDEF SPH
	OUT SPH,ATOM_H
.ENDIF

	PUSH YH
	PUSH YL
	PUSH ATOM_EL
.IFDEF SPH
	MOVW YL,ATOM_L
.ELSE
	CLR YH
	MOVW YL,ATOM_L
.ENDIF

	STS SREG,TEMP_L
	POP TEMP_L
	RET
.ENDIF
