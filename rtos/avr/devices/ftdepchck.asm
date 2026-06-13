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

;Подключение родительских фич

.IF OS_FT_WELCOME == 0x01									;WELCOME требует STDOUT
	.SET	OS_FT_STDOUT 							= 0x01
.ENDIF
.IF OS_FT_SOFT_RESET== 0x01									;Использует STDIN
	.SET	OS_FT_STDIN 							= 0x01
.ENDIF
.IF OS_FT_STDIN == 0x01										;Режим STDIN использует внешние прерывания
	.SET	OS_FT_PCINT								= 0x01
.ENDIF
.IF	OS_FT_TIMERTASK == 0x01									;Поддержка задачи-таймера требует многозадачность
	.SET	OS_FT_MULTITHREADING					= 0x01
.ENDIF
.IF OS_FT_MULTITHREADING == 0x01							;Многопоточность использует таймер
	.SET	OS_FT_TIMER 							= 0x01
.ENDIF
.IF OS_FT_TIMER == 0x01 || OS_FT_PCINT == 0x01 || OS_FT_USTIMER == 0x01
	.SET	OS_FT_IR_TABLE							= 0x01		;Таймер и внешние прерывания зависят о таблицы прерываний
.ENDIF

