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

.IFNDEF J8BPROC_INC_REFCOUNT
;-----------------------------------------------------------
J8BPROC_INC_REFCOUNT:
;-----------------------------------------------------------
;Инкремент количества ссылок объекта
;IN: Z-адрес HEAP
;-----------------------------------------------------------
	PUSH ACCUM_EL
	LDD ACCUM_EL,Z+0x02
	CPI ACCUM_EL,0xff
    BREQ _J8BPROC_INC_REFCOUNT__SKIP1
	INC ACCUM_EL
	STD Z+0x02,ACCUM_EL
_J8BPROC_INC_REFCOUNT__SKIP1:
	POP ACCUM_EL
	RET
.ENDIF

