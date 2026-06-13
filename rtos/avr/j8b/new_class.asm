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

.IFNDEF J8BPROC_NEW_CLASS
.include "dmem/dram.asm"
;-----------------------------------------------------------
J8BPROC_NEW_CLASS:
;-----------------------------------------------------------
;Вынесен блок кода для оптимизации - выделяет и
;инициализирует HEAP для класса
;IN: ACCUM_L/H-размер блока
;MOD: ACCUM_L/H
;OUT: Z-адрес на выделенный блок, флаг С-OUT OF RAM
;-----------------------------------------------------------
	SET
	MCALL OS_DRAM_ALLOC
	BRCS _J8BPROC_NEW_CLASS__OVF
	STD Z+0x00,ACCUM_L										;Длина
	STD Z+0x01,ACCUM_H
	;REF CNTR будет заполнен верхним уровнем
	;META будет заполнена верхним уровнем
	CLC
_J8BPROC_NEW_CLASS__OVF:
	CLT
	RET
.ENDIF