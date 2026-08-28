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

.IFNDEF J8BPROC_ARRVIEW_ARRADDR
;-----------------------------------------------------------
J8BPROC_ARRVIEW_ARRADDR:
;-----------------------------------------------------------
;Получаем адрес заголовка массива
;IN: X-адрес заголовка VIEW или массива
;OUT: X-адрес заголовка массива
;-----------------------------------------------------------
	PUSH ACCUM_L
	LD ACCUM_L,X											;Проверяем заголовк и выходим, если это массив
	SBRS ACCUM_L,0x07										;включенный 7-й бит в заголовке означает что это VIEW, иначе массив
	RJMP J8BPROC_ARRVIEW_ARRADDR__END
	ADIW XL,0x01
	LD ACCUM_L,X+											;LE
	LD XH,X
	MOV XL,ACCUM_L
J8BPROC_ARRVIEW_ARRADDR__END:
	POP ACCUM_L
	RET
.ENDIF
