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

package ru.vm5277.common;

import java.util.Base64;

public class DatatypeConverter {

   public static byte[] parseHexBinary(String l_hex_string) {
      if(null == l_hex_string || l_hex_string.isEmpty()) {
         return new byte[0x00];
      }

      byte[] result = new byte[l_hex_string.length() / 0x02];
      for(int pos = 0; pos < result.length; pos++) {
         String substr = l_hex_string.substring(pos * 0x02, pos * 0x02 + 0x02).toLowerCase();
         result[pos] = (byte)Integer.parseInt(substr, 16);
      }
      return result;
   }

   public static String printHexBinary(byte[] l_bytes) {
      if(null == l_bytes || 0 == l_bytes.length) {
         return "";
      }

      StringBuilder result = new StringBuilder();
      for(int pos = 0; pos < l_bytes.length; pos++) {
         String num = Integer.toHexString(l_bytes[pos] & 0xff).toLowerCase();
         if(num.length() < 0x02) {
            result.append("0");
         }
         result.append(num);
      }
      return result.toString();
   }

   public static byte[] parseBase64Binary(String l_base64_string) {
      return Base64.getDecoder().decode(l_base64_string);
   }
   public static String printBase64Binary(byte l_bytes[]) {
      return Base64.getEncoder().encodeToString(l_bytes);
   }
}
