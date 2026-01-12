# Прошивка микроконтроллера через USBasp и avrdude

Этот способ использует стандартный не дорогой и широко распространенный программатор **USBasp** и популярную консольную утилиту **avrdude** для записи скомпилированного HEX-файла в микроконтроллер.  
Подходит для большинства AVR-чипов, включая ATmega328(P), и не требует установки дополнительного ПО на МК.

>*Примечание: Здесь рассмотрена только одна, но самая популярная утилита.  
На просторах интернета и на специализированных форумах существует множество различных решений — как программаторов, так и утилит.  
К сожалению, многие из них обладают рядом минусов: работа только под Windows, отсутствие возможности работы в debug-WIRE режиме, ограниченное количество поддерживаемых программаторов и МК.*

## Подготовка

### 1. Установите avrdude
Если утилита `avrdude` не установлена в системе, выполните:

```bash
sudo apt update
sudo apt install avrdude
```
*Также можно посетить официальный сайт утилиты [avrdude](https://www.nongnu.org/avrdude/)*

### 2. Подключите программатор

Подключите USBasp к компьютеру и к целевому микроконтроллеру по схеме ISP:
| USBasp (10-pin) | ATmega328P |
|-|-|
| MOSI | PB3 (MOSI) |
| MISO | PB4 (MISO) |
| SCK | PB5 (SCK) |
| RESET | PC6 (RESET) |
| VCC (+5V) | VCC |
| GND | GND |

>Важно: Убедитесь, что МК запитан (либо от USBasp, либо от внешнего источника).

## Прошивка HEX-файла
После успешной компиляции проекта J8B вы получите файл прошивки в директории `target/` — например, `Main_cseg.hex`.
### 1. Перейдите в каталог проекта:
```bash
cd /путь/к/вашему/проекту
```
### 2. Выполните команду прошивки:
Для ATmega328P:
```bash
avrdude -p atmega328p -c usbasp -P usb -U flash:w:target/Main_cseg.hex:i
```
## Пример успешного выполнения
```bash
$ avrdude -p atmega328p -c usbasp -P usb -U flash:w:target/Main_cseg.hex:i

avrdude: AVR device initialized and ready to accept instructions

Reading | ################################################## | 100% 0.00s

avrdude: Device signature = 0x1e950f
avrdude: NOTE: "flash" memory has been specified, an erase cycle will be performed
         To disable this feature, specify the -D option.
avrdude: erasing chip
avrdude: reading input file "target/Main_cseg.hex"
avrdude: input file target/Main_cseg.hex auto detected as Intel Hex
avrdude: writing flash (298 bytes):

Writing | ################################################## | 100% 0.04s

avrdude: 298 bytes of flash written
avrdude: verifying flash memory against target/Main_cseg.hex:
avrdude: load data flash data from input file target/Main_cseg.hex:
avrdude: input file target/Main_cseg.hex auto detected as Intel Hex
avrdude: input file target/Main_cseg.hex contains 298 bytes
avrdude: reading on-chip flash data:

Reading | ################################################## | 100% 0.03s

avrdude: verifying ...
avrdude: 298 bytes of flash verified

avrdude done.  Thank you.
```

После успешной прошивки микроконтроллер готов к работе.

>*Более подробную информацию смотрите на официальном сайте [avrdude](https://www.nongnu.org/avrdude/)*


[← Назад к прошивке микроконтроллера](flashing.md)  
[← Назад к содержанию](index.md)
