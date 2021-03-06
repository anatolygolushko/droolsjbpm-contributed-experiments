/*
 * Copyright 2017 Red Hat Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jbpm.persistence.mapdb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jbpm.persistence.api.PersistentCorrelationKey;
import org.kie.internal.process.CorrelationProperty;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.serializer.GroupSerializerObjectArray;

public class PersistentCorrelationKeySerializer extends GroupSerializerObjectArray<PersistentCorrelationKey> {

    @Override
    public void serialize(DataOutput2 out, PersistentCorrelationKey value) throws IOException {
        out.writeBoolean(value.getName() != null);
        if (value.getName() != null) {
            out.writeUTF(value.getName());
        }
        out.writeLong(value.getProcessInstanceId());
        List<CorrelationProperty<?>> props = value.getProperties();
        out.writeBoolean(props != null);
        if (props != null) {
            out.writeInt(props.size());
            for (CorrelationProperty<?> prop : props) {
                out.writeBoolean(prop.getName() != null);
                if (prop.getName() != null) {
                    out.writeUTF(prop.getName());
                }
                out.writeBoolean(prop.getType() != null);
                if (prop.getType() != null) {
                    out.writeUTF(prop.getType());
                }
                out.writeBoolean(prop.getValue() != null);
                if (prop.getValue() != null) {
                    out.writeUTF(String.valueOf(prop.getValue()));
                }
            }
        }
    }

    @Override
    public PersistentCorrelationKey deserialize(DataInput2 input, int available) throws IOException {
        MapDBCorrelationKey ck = new MapDBCorrelationKey();
        if (input.readBoolean()) {
            ck.setName(input.readUTF());
        }
        ck.setProcessInstanceId(input.readLong());
        if (input.readBoolean()) {
            List<CorrelationProperty<?>> props = new ArrayList<>();
            for (int index = 0; index < input.readInt(); index++) {
                MapDBCorrelationProperty prop = new MapDBCorrelationProperty();
                if (input.readBoolean()) {
                    prop.setName(input.readUTF());
                }
                if (input.readBoolean()) {
                    prop.setType(input.readUTF());
                }
                if (input.readBoolean()) {
                    prop.setValue(input.readUTF());
                }
                props.add(prop);
            }
            ck.setProperties(props);
        }
        return ck;
    }

    @Override
    public int compare(PersistentCorrelationKey o1, PersistentCorrelationKey o2) {
        return o1.getName().compareTo(o2.getName());
    }

    
}
