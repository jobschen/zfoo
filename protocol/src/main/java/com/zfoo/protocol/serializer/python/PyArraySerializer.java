/*
 * Copyright (C) 2020 The zfoo Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.zfoo.protocol.serializer.python;

import com.zfoo.protocol.generate.GenerateProtocolFile;
import com.zfoo.protocol.registration.field.ArrayField;
import com.zfoo.protocol.registration.field.IFieldRegistration;
import com.zfoo.protocol.serializer.CodeLanguage;
import com.zfoo.protocol.serializer.CutDownArraySerializer;

import com.zfoo.protocol.util.StringUtils;

import java.lang.reflect.Field;

import static com.zfoo.protocol.util.FileUtils.LS;

/**
 * @author godotg
 * @version 3.0
 */
public class PyArraySerializer implements IPySerializer {
    @Override
    public String fieldDefaultValue(Field field, IFieldRegistration fieldRegistration) {
        return "[]";
    }

    @Override
    public void writeObject(StringBuilder builder, String objectStr, int deep, Field field, IFieldRegistration fieldRegistration) {
        GeneratePyUtils.addTab(builder, deep);
        if (CutDownArraySerializer.getInstance().writeObject(builder, objectStr, field, fieldRegistration, CodeLanguage.GdScript)) {
            return;
        }

        ArrayField arrayField = (ArrayField) fieldRegistration;

        builder.append(StringUtils.format("if ({} == null):", objectStr)).append(LS);
        GeneratePyUtils.addTab(builder, deep + 1);
        builder.append("buffer.writeInt(0)").append(LS);
        GeneratePyUtils.addTab(builder, deep);

        builder.append("else:").append(LS);
        GeneratePyUtils.addTab(builder, deep + 1);
        builder.append(StringUtils.format("buffer.writeInt({}.size())", objectStr)).append(LS);

        String element = "element" + GenerateProtocolFile.index.getAndIncrement();
        GeneratePyUtils.addTab(builder, deep + 1);
        builder.append(StringUtils.format("for {} in {}:", element, objectStr)).append(LS);
        GeneratePyUtils.pySerializer(arrayField.getArrayElementRegistration().serializer())
                .writeObject(builder, element, deep + 2, field, arrayField.getArrayElementRegistration());
    }

    @Override
    public String readObject(StringBuilder builder, int deep, Field field, IFieldRegistration fieldRegistration) {
        GeneratePyUtils.addTab(builder, deep);
        var cutDown = CutDownArraySerializer.getInstance().readObject(builder, field, fieldRegistration, CodeLanguage.GdScript);
        if (cutDown != null) {
            return cutDown;
        }

        ArrayField arrayField = (ArrayField) fieldRegistration;
        String result = "result" + GenerateProtocolFile.index.getAndIncrement();

        builder.append(StringUtils.format("{} = []", result)).append(LS);

        String i = "index" + GenerateProtocolFile.index.getAndIncrement();
        String size = "size" + GenerateProtocolFile.index.getAndIncrement();

        GeneratePyUtils.addTab(builder, deep);
        builder.append(StringUtils.format("{} = buffer.readInt()", size)).append(LS);

        GeneratePyUtils.addTab(builder, deep);
        builder.append(StringUtils.format("if ({} > 0):", size)).append(LS);
        GeneratePyUtils.addTab(builder, deep + 1);
        builder.append(StringUtils.format("for {} in range({}):", i, size)).append(LS);
        String readObject = GeneratePyUtils.pySerializer(arrayField.getArrayElementRegistration().serializer())
                .readObject(builder, deep + 2, field, arrayField.getArrayElementRegistration());
        GeneratePyUtils.addTab(builder, deep + 2);
        builder.append(StringUtils.format("{}.append({})", result, readObject)).append(LS);
        return result;
    }
}
