/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.artemis.api.core;

import org.apache.activemq.artemis.core.client.ActiveMQClientMessageBundle;
import org.apache.activemq.artemis.utils.Base64;
import org.apache.activemq.artemis.utils.StringEscapeUtils;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class JsonUtil {
   public static JsonArray toJSONArray(final Object[] array) throws Exception {
      JsonArrayBuilder jsonArray = Json.createArrayBuilder();

      for (Object parameter : array) {
         if (parameter instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) parameter;

            JsonObjectBuilder jsonObject = Json.createObjectBuilder();

            for (Map.Entry<String, Object> entry : map.entrySet()) {
               String key = entry.getKey();

               Object val = entry.getValue();

               if (val != null) {
                  if (val.getClass().isArray()) {
                     JsonArray objectArray = toJSONArray((Object[]) val);
                     jsonObject.add(key, objectArray);
                  }
                  else {
                     addToObject(key, val, jsonObject);
                  }
               }
            }
            jsonArray.add(jsonObject);
         }
         else {
            if (parameter != null) {
               Class<?> clz = parameter.getClass();

               if (clz.isArray()) {
                  Object[] innerArray = (Object[]) parameter;

                  if (innerArray instanceof CompositeData[]) {
                     JsonArrayBuilder innerJsonArray = Json.createArrayBuilder();
                     for (Object data : innerArray) {
                        String s = Base64.encodeObject((CompositeDataSupport) data);
                        innerJsonArray.add(s);
                     }
                     JsonObjectBuilder jsonObject = Json.createObjectBuilder();
                     jsonObject.add(CompositeData.class.getName(), innerJsonArray);
                     jsonArray.add(jsonObject);
                  }
                  else {
                     jsonArray.add(toJSONArray(innerArray));
                  }
               }
               else {
                  addToArray(parameter, jsonArray);
               }
            }
            else {
               jsonArray.addNull();
            }
         }
      }
      return jsonArray.build();
   }

   public static Object[] fromJsonArray(final JsonArray jsonArray) throws Exception {
      Object[] array = new Object[jsonArray.size()];

      for (int i = 0; i < jsonArray.size(); i++) {
         Object val = jsonArray.get(i);

         if (val instanceof JsonArray) {
            Object[] inner = fromJsonArray((JsonArray) val);

            array[i] = inner;
         }
         else if (val instanceof JsonObject) {
            JsonObject jsonObject = (JsonObject) val;

            Map<String, Object> map = new HashMap<>();

            Set<String> keys = jsonObject.keySet();

            for (String key : keys) {
               Object innerVal = jsonObject.get(key);

               if (innerVal instanceof JsonArray) {
                  innerVal = fromJsonArray(((JsonArray) innerVal));
               }
               else if (innerVal instanceof JsonObject) {
                  Map<String, Object> innerMap = new HashMap<>();
                  JsonObject o = (JsonObject) innerVal;
                  Set<String> innerKeys = o.keySet();
                  for (String k : innerKeys) {
                     innerMap.put(k, o.get(k));
                  }
                  innerVal = innerMap;
               }
               else if (innerVal instanceof JsonNumber) {
                  JsonNumber jsonNumber = (JsonNumber)innerVal;
                  innerVal = jsonNumber.longValue();
               }
               if (CompositeData.class.getName().equals(key)) {
                  Object[] data = (Object[]) innerVal;
                  CompositeData[] cds = new CompositeData[data.length];
                  for (int i1 = 0; i1 < data.length; i1++) {
                     ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(Base64.decode((data[i1].toString()))));
                     cds[i1] = (CompositeDataSupport) ois.readObject();
                  }
                  innerVal = cds;
               }

               map.put(key, innerVal);
            }

            array[i] = map;
         }
         else {
            if (val == JsonObject.NULL) {
               array[i] = null;
            }
            else {
               array[i] = val;
            }
         }
      }

      return array;
   }

   public static JsonValue nullSafe(String input) {
      return new NullableJsonString(input);
   }

   public static void addToObject(final String key, final Object param, final JsonObjectBuilder jsonObjectBuilder) {
      if (param instanceof Integer) {
         jsonObjectBuilder.add(key, (Integer) param);
      }
      else if (param instanceof Long) {
         jsonObjectBuilder.add(key, (Long) param);
      }
      else if (param instanceof Double) {
         jsonObjectBuilder.add(key, (Double) param);
      }
      else if (param instanceof String) {
         jsonObjectBuilder.add(key, (String) param);
      }
      else if (param instanceof Boolean) {
         jsonObjectBuilder.add(key, (Boolean) param);
      }
      else if (param instanceof Map) {
         JsonObject mapObject = toJsonObject((Map<String,Object>) param);
         jsonObjectBuilder.add(key, mapObject);
      }
      else if (param instanceof Short) {
         jsonObjectBuilder.add(key, (Short) param);
      }
      else if (param instanceof Byte) {
         //??
      }
      else if (param instanceof SimpleString) {
         jsonObjectBuilder.add(key, param.toString());
      }
      else {
         throw ActiveMQClientMessageBundle.BUNDLE.invalidManagementParam(param.getClass().getName());
      }
   }

   public static void addToArray(final Object param, final JsonArrayBuilder jsonArrayBuilder) {
      if (param instanceof Integer) {
         jsonArrayBuilder.add((Integer) param);
      }
      else if (param instanceof Long) {
         jsonArrayBuilder.add((Long) param);
      }
      else if (param instanceof Double) {
         jsonArrayBuilder.add((Double) param);
      }
      else if (param instanceof String) {
         jsonArrayBuilder.add((String) param);
      }
      else if (param instanceof Boolean) {
         jsonArrayBuilder.add((Boolean) param);
      }
      else if (param instanceof Map) {
         JsonObject mapObject = toJsonObject((Map<String,Object>) param);
         jsonArrayBuilder.add(mapObject);
      }
      else if (param instanceof Short) {
         jsonArrayBuilder.add((Short) param);
      }
      else if (param instanceof Byte) {
         //??
      }
      else {
         throw ActiveMQClientMessageBundle.BUNDLE.invalidManagementParam(param.getClass().getName());
      }
   }

   public static JsonArray toJsonArray(List<String> strings) {
      JsonArrayBuilder array = Json.createArrayBuilder();
      if (strings != null) {
         for (String connector : strings) {
            array.add(connector);
         }
      }
      return array.build();
   }

   public static JsonObject toJsonObject(Map<String, Object> map) {
      JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
      if (map != null) {
         for (Map.Entry<String, Object> entry : map.entrySet()) {
            addToObject(entry.getKey(), entry.getValue(), jsonObjectBuilder);
         }
      }
      return jsonObjectBuilder.build();
   }

   public static JsonArray readJsonArray(String jsonString) {
      return Json.createReader(new StringReader(jsonString)).readArray();
   }

   public static JsonObject readJsonObject(String jsonString) {
      return Json.createReader(new StringReader(jsonString)).readObject();
   }

   private JsonUtil() {
   }

   private static class NullableJsonString implements JsonValue, JsonString {
      private final String value;
      private String escape;

      NullableJsonString(String value) {
         if (value == null || value.length() == 0) {
            this.value = null;
         }
         else {
            this.value = value;
         }
      }

      @Override
      public ValueType getValueType() {
         return value == null ? ValueType.NULL : ValueType.STRING;
      }

      @Override
      public String getString() {
         return this.value;
      }

      @Override
      public CharSequence getChars() {
         return getString();
      }

      public String toString() {
         if (this.value == null) {
            return null;
         }
         String s = this.escape;
         if (s == null) {
            s = '\"' + StringEscapeUtils.escapeString(this.value) + '\"';
            this.escape = s;
         }
         return s;
      }
   }
}