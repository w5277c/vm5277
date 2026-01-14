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

;Протокол бутлоадера
;+----------------------------------------+
;|0x00:0x01-MAGIC=0x77                    |
;+----------------------------------------+
;|0x01:0x01-REQUEST/RESPONSE ID           |
;+----------------------------------------+
;|0x02:0x02-DATA SIZE                     |
;+----------------------------------------+
;|0x04:0x??-DATA                          |
;+----------------------------------------+
;|0x??:0x01-XOR SUM                       |
;+----------------------------------------+

;Запросы  (от хоста)
	.EQU	REQ_BLDR_INFO							= 0x00	;Запрос информации об устройстве
	.EQU	REQ_BLDR_REBOOT							= 0x01	;Рестарт через WATCHDOG
	.EQU	REQ_BLDR_PAGE_ERASE						= 0x10	;Очистка FLASH страницы
	.EQU	REQ_BLDR_PAGE_VERIFY					= 0x11	;Проверка FLASH страницы
	.EQU	REQ_BLDR_PAGE_WRITE						= 0x12	;Запись FLASH страницы
	.EQU	REQ_BLDR_PAGE_EVERIFY					= 0x20	;Проверка FLASH страницы (зашифрованные данные - на будущее)
	.EQU	REQ_BLDR_PAGE_EWRITE					= 0x21	;Запись FLASH страницы (зашифрованные данные - на будущее)
	.EQU	REQ_BLDR_FUSE_READ						= 0x30	;Чтение значений фьюзов
	.EQU	REQ_BLDR_FUSE_WRITE						= 0x31	;Запись значений фьюзов
;Ответы (от бутлоадера)
	.EQU	RESP_BLDR_OK							= 0x80	;Успешное выполнение
	.EQU	RESP_BLDR_IDENTICAL						= 0x81	;Страницы идентичны (для REQ_BLDR_PAGE_VERIFY и REQ_BLDR_PAGE_WRITE)
	.EQU	RESP_BLDR_NOT_EQUAL						= 0x82	;Страницы не уникальны (для REQ_BLDR_PAGE_VERIFY)
	.EQU	RESP_BLDR_WRONG_REQUEST					= 0x88	;Ошибка в данных запроса
	.EQU	RESP_BLDR_WRONG_PAGE					= 0x89	;Не корректный номер страницы
	.EQU	RESP_BLDR_WRONG_PAGESIZE				= 0x8a	;Не корректный размер страницы
	.EQU	RESP_BLDR_DENIED						= 0x8b	;Отклонено

;Тело запроса REQ_BLDR_INFO (DATA_SIZE:0x14)
;+----------------------------------------------+
;|0x05:0x01-BLDR VERSION (7,6-type,5-0-version) |			;00-стандартный бутлоадер(atmega), 01-сокращенный(tiny)
;+----------------------------------------------+
;|0x06:0x01-PLATFORM TYPE ID                    |
;+----------------------------------------------+
;|0x07:0x04-MCU SIGNATURE                       |
;+----------------------------------------------+
;|0x0b:0x08-MCU UID                             |
;+----------------------------------------------+
;|0x13:0x01-FIRMWARE VERSION (0xff-empty)       |
;+----------------------------------------------+

;Тело запроса REQ_BLDR_PAGE_VERIFY и REQ_BLDR_PAGE_WRITE(DATA_SIZE:0x02+PAGE_SIZE)
;+----------------------------------------+
;|0x05:0x02-FLASH ADDR (in bytes)         |
;+----------------------------------------+
;|0x07:0x??-PAGE DATA                     |
;+----------------------------------------+

;Тело запроса REQ_BLDR_PAGE_ERASE (DATA_SIZE:0x02)
;+----------------------------------------+
;|0x05:0x02-FLASH ADDR (in bytes)         |
;+----------------------------------------+


;Тело запроса REQ_BLDR_FUSE_WRITE и ответа на запрос REQ_BLDR_FUSE_READ (DATA_SIZE:0x06)
;+----------------------------------------+
;|0x05:0x06-FUSE DATA                     | - LlHhEe (каждому байту добавлено инвертированное значение)
;+----------------------------------------+

;Остальные запросы тел не имеют

	.DEF	COK										= r3;
	.DEF	CMAGIC									= r4;

	.EQU	BLDR_VERSION							= 0b01000000 | 0x00000000 ;7,6 тип, 5-0 версия
	.EQU	BLDR_START_WADDR						= BOOT_512W_ADDR
	.EQU	_BLDR_BUFFER_SIZE						= 0x08 + FLASH_PAGESIZE	;Максимально допустимый размер запроса
	.EQU	_BLDR_PROTOCOL_HEADER_SIZE				= 0x05
	

.ORG BLDR_START_WADDR
BLDR_START:
	CLI
	RJMP _BLDR_START__DATASKIP
BLDR_START__DATA:
	.db	BLDR_VERSION,PLATFORM_ID							;Версия загрузчика и ид типа платформы
	.db	low(DEVICE_SIGNATURE>>0x18),low(DEVICE_SIGNATURE>>0x10)	;Сигнатура чипа
	.db	low(DEVICE_SIGNATURE>>0x08),low(DEVICE_SIGNATURE)
	.dw V_UID												;Уникальный идентификатор чипа (2B-vendor, 6B-device)
	.dw D_UID>>0x20
	.dd	D_UID&0xffffffff
.IFDEF KEY1	
	.dd KEY1
	.dd KEY2
	.dd KEY3
	.dd KEY4
	.dd KEY5
	.dd KEY6
	.dd KEY7
	.dd KEY8
.ENDIF
_BLDR_START__DATASKIP:
.IFDEF SPH
	LDI ACCUM_L,high(SRAM_START+SRAM_SIZE-0x01)				;Инициализация стека
	STS SPH,ACCUM_L
.ENDIF
	LDI ACCUM_L,low(SRAM_START+SRAM_SIZE-0x01)
	STS SPL,ACCUM_L

	;WATCHDOG STOP
	WDR
	LDS ACCUM_L,MCUSR
	ANDI ACCUM_L,~(1<<WDRF)
	STS MCUSR,ACCUM_L
	LDS ACCUM_L,WDTCR
	ORI ACCUM_L,(1<<WDCE)|(1<<WDE)
	STS WDTCR,ACCUM_L
	LDI ACCUM_L,(0<<WDE)
	STS WDTCR,ACCUM_L

	EOR C0x00,C0x00
	LDI ACCUM_L,RESP_BLDR_OK
	MOV COK,ACCUM_L
	LDI ACCUM_L,0x77
	MOV CMAGIC,ACCUM_L

	EOR FLAGS,FLAGS

	LDI ZH,0x00												;Проверка наличия прошивки программы, если 0xffff то прошивка не обнаружена
	LDI ZL,0x00
	LPM TEMP_L,Z+
	CPI TEMP_L,0xff
	BRNE _BLDR_START__PROGRAMM_DETECTED
	LPM TEMP_L,Z
	CPI TEMP_L,0xff
	BREQ _BLDR_START__LOOP									;Если прошивки нет, то переходим сразу в режим бутлоадера
_BLDR_START__PROGRAMM_DETECTED:

	CBI BLDR_UART_DDR,BLDR_UART_PINNUM						;Гарантируем режим входа на пине
	CBI BLDR_UART_PORT,BLDR_UART_PINNUM						;Внутренняя подтяжка
	RCALL _BLDR_UART_WAIT_NR


;TODO заменить на WDR с выдержкой паузы в  2-3 сек.			;Выполняем проверку низкого уровня на пине (должен быть подтянут к VCC через внешний резистор)
	LDI TEMP_EL,8											;Где-то 1с для 4МГц и 200мс для 20Мгц (необходимо проверить)
	LDI TEMP_H,183											;200мс нужно чтобы не пропустить код нажатой кнопки к склавиатуры хоста
	LDI TEMP_L,187
_BLDR_START__LOWLEVELDETECTOR_LOOP:
	SBIS BLDR_UART_PIN,BLDR_UART_PINNUM
	RJMP _BLDR_START__LOOP									;Переход на бутлоадер, если обнаружен низкий уровень
	SUBI TEMP_L,0x01
	SBCI TEMP_H,0x00
	SBCI TEMP_EL,0x00
	BRCC _BLDR_START__LOWLEVELDETECTOR_LOOP
	JMP 0x0000												;Переход на программу, если низкий уровень не был обнаружен

_BLDR_START__LOOP:
	RCALL BLDR_UART_RECV_NR
	CPI ZH,0xff
	BREQ _BLDR_START__LOOP
	CPI TEMP_H,0x00
	BRNE _BLDR_START__LOOP
	CPI ZH,0x00
	BRNE PC+0x03
	CPI ZL,_BLDR_PROTOCOL_HEADER_SIZE
	BRCS _BLDR_START__LOOP
	LDI YH,high(SRAM_START)
	LDI YL,low(SRAM_START)
	LD ACCUM_L,Y
	CP ACCUM_L,CMAGIC
	BRNE _BLDR_START__LOOP
	
	SBIW ZL,0x01											;Вычитаем байт XORSUM
	
	LDI YH,high(SRAM_START)
	LDI YL,low(SRAM_START)
	LDD ACCUM_L,Y+0x01
	
	CPI ZH,0x00
	BREQ PC+0x02
	RJMP _BLDR_START__LOOP_NOT_BLDR_PAGE_ERASE
	
	CPI ACCUM_L,REQ_BLDR_INFO
	BRNE _BLDR_START__LOOP_NOT_BLDR_INFO
;---INFO----------------------------------------------------
	CPI ZL,_BLDR_PROTOCOL_HEADER_SIZE-0x01
	BREQ PC+0x04
	LDI ACCUM_L,RESP_BLDR_WRONG_REQUEST
	RCALL _BLDR_ANSWER
	RJMP _BLDR_START__LOOP
	
	ST Y+,CMAGIC
	ST Y+,COK
	ST Y+,C0x00
	LDI ACCUM_L,0x0f
	ST Y+,ACCUM_L
	LDI TEMP_L,0x0e
	LDI ZH,high(BLDR_START__DATA*2)
	LDI ZL,low(BLDR_START__DATA*2)
_BLDR_START__LOOP_BLDR_INFO_LOOP:
	LPM ACCUM_L,Z+
	ST Y+,ACCUM_L
	DEC TEMP_L
	BRNE _BLDR_START__LOOP_BLDR_INFO_LOOP
	LDI ZH,high(BLDR_START_WADDR*2-0x02)
	LDI ZL,low(BLDR_START_WADDR*2-0x02)
	LPM ACCUM_L,Z
	ST Y,ACCUM_L
	LDI XH,high(_BLDR_PROTOCOL_HEADER_SIZE-0x01+0x0f)
	LDI XL,low(_BLDR_PROTOCOL_HEADER_SIZE-0x01+0x0f)
	RCALL BLDR_UART_SEND_NR
	RJMP _BLDR_START__LOOP
_BLDR_START__LOOP_NOT_BLDR_INFO:
	CPI ACCUM_L,REQ_BLDR_REBOOT
	BRNE _BLDR_START__LOOP_NOT_BLDR_REBOOT
;---REBOOT--------------------------------------------------
	CPI ZL,_BLDR_PROTOCOL_HEADER_SIZE-0x01
	BREQ PC+0x04
	LDI ACCUM_L,RESP_BLDR_WRONG_REQUEST
	RCALL _BLDR_ANSWER
	RJMP _BLDR_START__LOOP
	LDI ACCUM_L,RESP_BLDR_OK
	RCALL _BLDR_ANSWER
	RJMP _BLDR_REBOOT
_BLDR_START__LOOP_NOT_BLDR_REBOOT:
	CPI ACCUM_L,REQ_BLDR_FUSE_READ
	BRNE _BLDR_START__LOOP_NOT_BLDR_FUSE_READ
;---FUSE-READ-----------------------------------------------
	ST Y+,CMAGIC
	ST Y+,COK
	ST Y+,C0x00
	LDI ACCUM_L,0x06
	ST Y+,ACCUM_L
	LDI ZL,FUSE_LOW_ADDR*2
	RCALL _BLDR_READ_FUSE_TO_SRAM__NR
	LDI ZL,FUSE_HIGH_ADDR*2
	RCALL _BLDR_READ_FUSE_TO_SRAM__NR
	LDI ZL,FUSE_EXT_ADDR*2
	RCALL _BLDR_READ_FUSE_TO_SRAM__NR
	RCALL BLDR_UART_SEND_NR
	RJMP _BLDR_START__LOOP
_BLDR_START__LOOP_NOT_BLDR_FUSE_READ:
	CPI ACCUM_L,REQ_BLDR_PAGE_ERASE
	BRNE _BLDR_START__LOOP_NOT_BLDR_PAGE_ERASE
;---PAGE-ERASE----------------------------------------------
	CPI ZL,_BLDR_PROTOCOL_HEADER_SIZE+0x02-0x01
	BREQ PC+0x04
	LDI ACCUM_L,RESP_BLDR_WRONG_REQUEST
	RCALL _BLDR_ANSWER
	RJMP _BLDR_START__LOOP
	LDD ZH,Y+0x04
	LDD ZL,Y+0x05
	RCALL _BLDR_CHECK_ADDR
	BREQ PC+0x02
	RJMP _BLDR_START__LOOP
	RCALL _BLDR_FLASHPAGE_ERASE_NR
	LDI ACCUM_L,RESP_BLDR_OK
	RCALL _BLDR_ANSWER
	RJMP _BLDR_START__LOOP
_BLDR_START__LOOP_NOT_BLDR_PAGE_ERASE:
	CPI ZH,high(_BLDR_PROTOCOL_HEADER_SIZE-0x01+0x02+FLASH_PAGESIZE)
	BRNE PC+0x03
	CPI ZL,low(_BLDR_PROTOCOL_HEADER_SIZE-0x01+0x02+FLASH_PAGESIZE)
	BREQ PC+0x04
	LDI ACCUM_L,RESP_BLDR_WRONG_PAGESIZE
	RCALL _BLDR_ANSWER
	RJMP _BLDR_START__LOOP
	
	CPI ACCUM_L,REQ_BLDR_PAGE_VERIFY
	BRNE _BLDR_START__LOOP_NOT_BLDR_PAGE_VERIFY
;---PAGE-VERIFY---------------------------------------------
	LDD ZH,Y+0x04
	LDD ZL,Y+0x05
	RCALL _BLDR_CHECK_ADDR
	BREQ PC+0x02
	RJMP _BLDR_START__LOOP
	RCALL _BLDR_VERIFY_FLASHPAGE
	LDI ACCUM_L,RESP_BLDR_OK
	BREQ PC+0x02
	LDI ACCUM_L,RESP_BLDR_NOT_EQUAL
	RCALL _BLDR_ANSWER
	RJMP _BLDR_START__LOOP

_BLDR_START__LOOP_NOT_BLDR_PAGE_VERIFY:
	CPI ACCUM_L,REQ_BLDR_PAGE_WRITE
	BRNE _BLDR_START__LOOP_NOT_BLDR_PAGE_WRITE
;---PAGE-WRITE----------------------------------------------
	LDD ZH,Y+0x04
	LDD ZL,Y+0x05
	RCALL _BLDR_CHECK_ADDR
	BREQ PC+0x02
	RJMP _BLDR_START__LOOP
	MOVW TEMP_L,ZL
	RCALL _BLDR_VERIFY_FLASHPAGE
	BRNE PC+0x04
	LDI ACCUM_L,RESP_BLDR_IDENTICAL
	RCALL _BLDR_ANSWER
	RJMP _BLDR_START__LOOP
	MOVW ZL,TEMP_L
	LDI YH,high(SRAM_START+_BLDR_PROTOCOL_HEADER_SIZE-0x01+0x02)
	LDI YL,low(SRAM_START+_BLDR_PROTOCOL_HEADER_SIZE-0x01+0x02)
	RCALL _BLDR_FLASHPAGE_WRITE_NR
	LDI ACCUM_L,RESP_BLDR_OK
	RCALL _BLDR_ANSWER
	RJMP _BLDR_START__LOOP
_BLDR_START__LOOP_NOT_BLDR_PAGE_WRITE:
	CPI ACCUM_L,REQ_BLDR_FUSE_WRITE
	BRNE _BLDR_START__LOOP_NOT_BLDR_FUSE_WRITE
;---FUSE-WRITE----------------------------------------------
	LDD TEMP_L,Y+0x02
	CPI TEMP_L,0x00
	BRNE _BLDR_START__LOOP_BLDR_FUSE_WRITE_INVALID_DATA
	LDD TEMP_L,Y+0x03
	CPI TEMP_L,0x06
	BRNE _BLDR_START__LOOP_BLDR_FUSE_WRITE_INVALID_DATA
	LDD ACCUM_L,Y+0x04
	LDD ACCUM_H,Y+0x06
	LDD ACCUM_EL,Y+0x08
	LDD TEMP_L,Y+0x05
	COM TEMP_L
	CP ACCUM_L,TEMP_L
	BRNE _BLDR_START__LOOP_BLDR_FUSE_WRITE_INVALID_DATA
	LDD TEMP_L,Y+0x07
	COM TEMP_L
	CP ACCUM_H,TEMP_L
	BRNE _BLDR_START__LOOP_BLDR_FUSE_WRITE_INVALID_DATA
	LDD TEMP_L,Y+0x09
	COM TEMP_L
	CP ACCUM_EL,TEMP_L
	BREQ PC+0x04
_BLDR_START__LOOP_BLDR_FUSE_WRITE_INVALID_DATA:
	LDI ACCUM_L,RESP_BLDR_WRONG_REQUEST
	RCALL _BLDR_ANSWER
	RJMP _BLDR_START__LOOP
	MOV r1,C0x00
	MOV r0,ACCUM_L											;Значение Low Fuse
	LDI ACCUM_L,(1<<BLBSET)|(1<<SPMEN)
	LDI ZH,0x00
	LDI ZL,FUSE_LOW_ADDR*2
	RCALL _BLDR_FLASHPAGE_DO_SMP_NR
	MOV r0,ACCUM_H											;Значение High Fuse
	LDI ZL,FUSE_HIGH_ADDR*2
	RCALL _BLDR_FLASHPAGE_DO_SMP_NR
	MOV r0,ACCUM_EL											;Значение Ext Fuse
	LDI ZL,FUSE_EXT_ADDR*2
	RCALL _BLDR_FLASHPAGE_DO_SMP_NR
	LDI ACCUM_L,RESP_BLDR_OK
	RCALL _BLDR_ANSWER
	RJMP _BLDR_REBOOT
_BLDR_START__LOOP_NOT_BLDR_FUSE_WRITE:

;---INCORRECT-REQUEST---------------------------------------
	LDI ACCUM_L,RESP_BLDR_WRONG_REQUEST
	RCALL _BLDR_ANSWER
	RJMP _BLDR_START__LOOP

;-----------------------------------------------------------
_BLDR_VERIFY_FLASHPAGE:
;-----------------------------------------------------------
;Проверка идентичности FLASH страницы
;IN;Z-адрес FLASH страницы
;OUT: Flag Z-(0=идентичны)
;-----------------------------------------------------------
	LDI XH,high(FLASH_PAGESIZE)
	LDI XL,low(FLASH_PAGESIZE)
	LDI YH,high(SRAM_START+_BLDR_PROTOCOL_HEADER_SIZE-0x01+0x02)
	LDI YL,low(SRAM_START+_BLDR_PROTOCOL_HEADER_SIZE-0x01+0x02)
_BLDR_VERIFY_FLASHPAGE__LOOP:
	LPM ACCUM_L,Z+
	LD ACCUM_H,Y+
	CP ACCUM_L,ACCUM_H
	BREQ PC+0x02
	RET
	SBIW XL,0x01
	BRNE _BLDR_VERIFY_FLASHPAGE__LOOP
	SEZ
	RET

;-----------------------------------------------------------
_BLDR_CHECK_ADDR:
;-----------------------------------------------------------
;Проверка корректности адреса FLASH страницы
;IN;Z-адрес FLASH страницы
;OUT: Flag Z-(1=корректый)
;-----------------------------------------------------------
	CPI ZH,high(FLASH_SIZE-FLASH_PAGESIZE-1024+1)
	BRCS _BLDR_CHECK_ADDR__CORRECT
	BRNE PC+0x3
	CPI ZL,low(FLASH_SIZE-FLASH_PAGESIZE-1024+1)
	BRCS _BLDR_CHECK_ADDR__CORRECT
	LDI ACCUM_L,RESP_BLDR_WRONG_PAGE
	RCALL _BLDR_ANSWER
	CLZ
	RET
_BLDR_CHECK_ADDR__CORRECT:
	SEZ
	RET

;-----------------------------------------------------------
_BLDR_ANSWER:
;-----------------------------------------------------------
;Заполняем стандартный пакет ответа и отправляем по UART
;OUT: ACCUM_L-код ответа
;-----------------------------------------------------------
	LDI YH,high(SRAM_START)
	LDI YL,low(SRAM_START)
	RCALL _BLDR_MAKE_HEADER
	LDI XH,high(_BLDR_PROTOCOL_HEADER_SIZE-0x01)
	LDI XL,low(_BLDR_PROTOCOL_HEADER_SIZE-0x01)
	RJMP BLDR_UART_SEND_NR

;-----------------------------------------------------------
_BLDR_MAKE_HEADER:
;-----------------------------------------------------------
;Заполняем заголовок для ответа
;OUT: ACCUM_L-код ответа
;-----------------------------------------------------------
	ST Y+,CMAGIC
	ST Y+,ACCUM_L
	ST Y+,C0x00
	ST Y+,C0x00
	RET


;===========================================================
;========UART=FUNCTIONS=====================================
;===========================================================

;-----------------------------------------------------------
BLDR_UART_RECV_NR:											;NR-NO_RESTORE - не восстанавливает регистры X,Y,Z, ACCUM_H/L, TEMP_H
;-----------------------------------------------------------
;Слушаем UART-ожидаем данные, принимаем и обрабатываем
;OUT: Z-длина данных, TEMP_H-XORSUM, ZH=0xff-ошибка приема
;-----------------------------------------------------------
	LDI YH,high(SRAM_START)									;Устанавливаем адрес буфера
	LDI YL,low(SRAM_START)
	LDI ZH,0x00												;Длина принятых данных
	LDI ZL,0x00
	LDI TEMP_H,0x00											;Счетчик XOR суммы

BLDR_UART_RECV_NR__WAIT_BYTE:								;Ждем байт
	LDI XH,0												;Пауза в 2 бита UART
	LDI XL,24												;~10мкс на 16МГц
	RCALL BLDR_UART_RECV_BYTE_NR
	BRNE BLDR_UART_RECV_NR__GOT_BYTE
	CPI ZH,0x00												;Проверка на нулевую длину
	BRNE PC+0x03
	CPI ZL,0x00
	BREQ BLDR_UART_RECV_NR__WAIT_BYTE						;При длине=0 бесконечно ждем старт
	RET
	
BLDR_UART_RECV_NR__GOT_BYTE:
	CPI ZH,0xff												;Признак переполнения буфера
	BREQ BLDR_UART_RECV_NR__WAIT_BYTE						;Повторяем прием в холостую пока не завершится передача

	CPI ZH,high(_BLDR_BUFFER_SIZE)							;Проверка на достижение максимальной длины - игнорируем последующие данные
	BRCS _BLDR_UART_RECV_NR__NEXT							;Переполнения нет - переходим к блоку записи байта и переходе к следующей итерации
	BRNE PC+0x03											;Есть переполнение - переходим к следующей итерации включив признак переполнения и игнорируем запись байта
	CPI ZL,low(_BLDR_BUFFER_SIZE)
	BRCS _BLDR_UART_RECV_NR__NEXT
	LDI ZH,0xff
	RJMP BLDR_UART_RECV_NR__WAIT_BYTE

_BLDR_UART_RECV_NR__NEXT:
	EOR TEMP_H,ACCUM_L										;Учитываем XORSUM
	ST Y+,ACCUM_L											;Записываем байт
	ADIW ZL,0x01											;Инкрементируем счетчик длины данных
	RJMP BLDR_UART_RECV_NR__WAIT_BYTE						;Переходим к следующей итерации

;-----------------------------------------------------------;
BLDR_UART_RECV_BYTE_NR:										;NR-NO_RESTORE - не восстанавливает регистры X, ACCUM_H
;-----------------------------------------------------------;
;Прием байта по программному UART
;IN: X-таймаут
;OUT: ACCUM_L-байт, Flag Z=1-ошибка приема
;-----------------------------------------------------------

BLDR_UART_RECV_BYTE_NR__START_LOOP:							;Ждем START
	SBIS BLDR_UART_PIN,BLDR_UART_PINNUM						;Если низкий уровень на пине, значит обнаружен START
	RJMP _BLDR_UART_RECV_BYTE_NR__GOT_START
	SBIW XL,0x01
	BRCC BLDR_UART_RECV_BYTE_NR__START_LOOP
	SEZ
	RET
	
_BLDR_UART_RECV_BYTE_NR__GOT_START:							;Получен START
	LDI XL,0x06												;Пауза ~1/3 бита ;TODO скорректировать на реальном железе
	DEC XL
	BRNE PC-0x01

	LDI XH,0x08												;Цикл в 8 бит (8n0)
_BLDR_UART_RECV_BYTE_NR__BITLOOP:
	RCALL _BLDR_UART_WAIT_NR								;Пауза в 1 бит, также сбрасывает флаг C
	SBIC BLDR_UART_PIN,BLDR_UART_PINNUM						;Передаем бит с UART в флаг C
	SEC
	ROR ACCUM_L												;Записываем флаг в аккумулятор используя битовый сдвиг
	DEC XH													;Декрементируем счетчик бит
	BRNE _BLDR_UART_RECV_BYTE_NR__BITLOOP						;Повторяем для 8 итераций

	RCALL _BLDR_UART_WAIT_NR								;Пауза в 1 бит
	CLZ
	SBIS BLDR_UART_PIN,BLDR_UART_PINNUM						;Проверяем STOP
	SEZ														;STOP не получен
	RET

;-----------------------------------------------------------;
BLDR_UART_SEND_NR:											;NR-NO_RESTORE - не восстанавливает регистры ACCUM_L\H, TEMP_L\H
;-----------------------------------------------------------;
;Передача RAM данных по UART
;IN: X-размер данных без XORSUM (не может быть 0)
;-----------------------------------------------------------
	SBI BLDR_UART_PORT,BLDR_UART_PINNUM						;Режим выхода для UART пина
	SBI BLDR_UART_DDR,BLDR_UART_PINNUM
	RCALL _BLDR_UART_WAIT_NR

	LDI YH,high(SRAM_START)
	LDI YL,low(SRAM_START)
	LDI TEMP_H,0x00
BLDR_UART_SEND_NR__LOOP:
	LD ACCUM_L,Y+											;Считываем байт в аккумулятор и инкрементируем индексный регистр
	EOR TEMP_H,ACCUM_L
	RCALL BLDR_UART_SEND_BYTE_NR							;Передаем байт по UART
	SBIW XL,0x01
	BRNE BLDR_UART_SEND_NR__LOOP

	MOV ACCUM_L,TEMP_H
	RCALL BLDR_UART_SEND_BYTE_NR							;Передаем байт XORSUM по UART

	CBI BLDR_UART_DDR,BLDR_UART_PINNUM
	CBI BLDR_UART_PORT,BLDR_UART_PINNUM						;Режим входа для пина UART без внутренней подтяжки
	RCALL _BLDR_UART_WAIT_NR
	RET

;-----------------------------------------------------------;
BLDR_UART_SEND_BYTE_NR:										;NR-NO_RESTORE - не восстанавливает регистры ACCUM_L\H, TEMP_L
;-----------------------------------------------------------;
;Передача байта по программному UART
;IN: ACCUM_L-байт
;-----------------------------------------------------------
	CBI BLDR_UART_PORT,BLDR_UART_PINNUM						;Выставляем START
	RCALL _BLDR_UART_WAIT_NR

	LDI TEMP_L,0x08											;Цикл передачи байта побитно
BLDR_UART_SEND_BYTE_NR__BITLOOP:
	SBRC ACCUM_L,0x00										;В зависимости от текущего бита задаем на пине высокий или низкий уровень
	SBI BLDR_UART_PORT,BLDR_UART_PINNUM
	SBRS ACCUM_L,0x00
	CBI BLDR_UART_PORT,BLDR_UART_PINNUM
	LSR ACCUM_L												;Сдвигаем биты в аккумуляторе
	
	NOP														;Строго выдерживаем паузу
	RCALL _BLDR_UART_WAIT_NR
	DEC TEMP_L												;Декрементируем счетчик бит
	BRNE BLDR_UART_SEND_BYTE_NR__BITLOOP					;Выполняем все 8 итераций

	SBI BLDR_UART_PORT,BLDR_UART_PINNUM						;Выставляем STOP
	RCALL _BLDR_UART_WAIT_NR
	RET

;-----------------------------------------------------------
_BLDR_UART_WAIT_NR:											;NR-NO_RESTORE - не восстанавливает регистр ACCUM_H
;-----------------------------------------------------------
;Пауза между битами
;-----------------------------------------------------------
	LDI ACCUM_H,0x13										;TODO скорректировать на реальном железе
	SUBI ACCUM_H,0x01
	BRNE PC-0x01
	RET


;-----------------------------------------------------------
_BLDR_REBOOT:
;-----------------------------------------------------------
;Перезагружка МК
;-----------------------------------------------------------
	CBI BLDR_UART_PORT,BLDR_UART_PINNUM						;Из измененного критична только конфигурация пина, возвращаем назад
	CBI BLDR_UART_DDR,BLDR_UART_PINNUM
	WDR														;Полный сброс через WATCHDOG
	LDS ACCUM_L,WDTCR
	ORI ACCUM_L,(1<<WDCE)|(1<<WDE)
	STS WDTCR,ACCUM_L
	LDI ACCUM_L,(0<<WDCE)|(1<<WDE)|(0<<WDIE)|(0<<WDP3)|(0<<WDP2)|(0<<WDP1)|(1<<WDP0)
	STS WDTCR,ACCUM_L
	RJMP PC

;===========================================================
;========FLASH=PROCS========================================
;===========================================================

;-----------------------------------------------------------
_BLDR_FLASHPAGE_WRITE_NR:									;NR-NO_RESTORE - не восстанавливает регистр Z,ACCUM_L,ACCUM_EH,TEMP_L,r0,r1
;-----------------------------------------------------------
;Блок процедуры записи страницы FLASH
;IN: Y-адрес данных в RAM, Z-адрес страницы во FLASH
;-----------------------------------------------------------
	RCALL _BLDR_FLASHPAGE_ERASE_NR

	MOVW XL,ZL
	LDI TEMP_L,FLASH_PAGESIZE/2
	LDI ACCUM_L,(1<<SPMEN)
_BLDR_FLASHPAGE_WRITE_NR__WLOOP:
	LD r0,Y+
	LD r1,Y+
	RCALL _BLDR_FLASHPAGE_DO_SMP_NR
	ADIW ZL,0x02
	DEC TEMP_L
	BRNE _BLDR_FLASHPAGE_WRITE_NR__WLOOP
	MOVW ZL,XL
	
	LDI ACCUM_L,(1<<PGWRT)|(1<<SPMEN)
	RCALL _BLDR_FLASHPAGE_DO_SMP_NR
	RET

;-----------------------------------------------------------
_BLDR_FLASHPAGE_ERASE_NR:									;NR-NO_RESTORE - не восстанавливает регистр ACCUM_L
;-----------------------------------------------------------
;Блок процедуры стирания страницы FLASH
;IN: Z-адрес страницы во FLASH
;-----------------------------------------------------------
	LDI ACCUM_L,(1<<PGERS)|(1<<SPMEN)
	RCALL _BLDR_FLASHPAGE_DO_SMP_NR
	RET

;-----------------------------------------------------------
_BLDR_READ_FUSE_TO_SRAM__NR:								;NR-NO_RESTORE - не восстанавливает регистры ACCUM_L,YH,YL
;-----------------------------------------------------------
;Чтение байта Fuse битов
;IN:ACCUM_L-значение SPMCSR, Z-src адрес,Y-dst адрес 
;-----------------------------------------------------------
	LDI ACCUM_L,(1<<BLBSET)|(1<<SPMEN)
	LDI ZH,0x00
	STS SPMCSR,ACCUM_L
	LPM ACCUM_L, Z
	ST Y+, ACCUM_L
	COM ACCUM_L
	ST Y+, ACCUM_L
	RET

;-----------------------------------------------------------
_BLDR_FLASHPAGE_DO_SMP_NR:									;NR-NO_RESTORE - не восстанавливает регистр ACCUM_EH
;-----------------------------------------------------------
;Выполняю команду SPM
;IN: Z-адрес FLASH,r1/r0-слово, ACCUM_L-значение SPMCSR
;-----------------------------------------------------------
_BLDR_FLASHPAGE_DO_SMP__WAIT_SPM:
	LDS ACCUM_EH,SPMCSR
	SBRC ACCUM_EH,SPMEN
	RJMP _BLDR_FLASHPAGE_DO_SMP__WAIT_SPM

_BLDR_FLASHPAGE_DO_SMP__WAIT_EE:
	LDS ACCUM_EH,EECR
	SBRC ACCUM_EH,EEPE
	RJMP _BLDR_FLASHPAGE_DO_SMP__WAIT_EE

	STS SPMCSR,ACCUM_L
	SPM
	RET
