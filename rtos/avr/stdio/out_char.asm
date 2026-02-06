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

;Выводит символ на STDIO порт
;Ничего не делает, если выключена фича STDOUT, просто выход
;Если фича STDIN выключена - просто вызываем процедуру STDIO или аналогичную в бутлоадере
;Так как порт STDIO инициализирован изначально и не меняет свое направление
;Иначе вначале вызываем процедуру перевода порта STDIO в режим выхода
;А в конце в режим входа. Попутно сохраняя и востанавливая регистры TEMP_L,ACCUM_L
;Вызываемые процедуры их не восстанавливают.

	.include "stdio/stdout.asm"

.IFNDEF OS_OUT_CHAR
;-----------------------------------------------------------
OS_OUT_CHAR:
;-----------------------------------------------------------
;Вывод символа
;IN: ACCUM_L-байт
;-----------------------------------------------------------
.IF OS_FT_STDOUT == 0x01
.IFDEF STDIO_PORT_REGID
.IF OS_FT_STDIN == 0x01
	MCALL OS_STDIO_SEND_MODE_NR
.ENDIF
.ENDIF
.IF OS_FT_DEV_MODE
	MCALL PROC__BLDR_UART_SEND_BYTE_NR
.ELSE
	MCALL OS_STDOUT_SEND_BYTE
.ENDIF
.IFDEF STDIO_PORT_REGID
.IF OS_FT_STDIN == 0x01
	MCALL OS_STDIO_RECV_MODE_NR
.ENDIF
.ENDIF
.ENDIF
	RET
.ENDIF

