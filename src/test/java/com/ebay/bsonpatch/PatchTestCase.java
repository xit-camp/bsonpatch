/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.ebay.bsonpatch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bson.BsonDocument;
import org.bson.BsonValue;

public class PatchTestCase {

    private final boolean operation;
    private final BsonDocument node;
    private final String sourceFile;

    private PatchTestCase(boolean isOperation, BsonDocument node, String sourceFile) {
        this.operation = isOperation;
        this.node = node;
        this.sourceFile = sourceFile;
    }

    public boolean isOperation() {
        return operation;
    }

    public BsonDocument getNode() {
        return node;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public static Collection<PatchTestCase> load(String fileName) throws IOException {
        String path = "/testdata/" + fileName + ".json";
        BsonDocument tree = (BsonDocument) TestUtils.loadResourceAsBsonValue(path);

        List<PatchTestCase> result = new ArrayList<PatchTestCase>();
        for (BsonValue node : tree.getArray("errors")) {
            if (isEnabled(node)) {
                result.add(new PatchTestCase(false, node.asDocument(), path));
            }
        }
        for (BsonValue node : tree.getArray("ops")) {
            if (isEnabled(node)) {
                result.add(new PatchTestCase(true, node.asDocument(), path));
            }
        }
        return result;
    }

    private static boolean isEnabled(BsonValue node) {
        BsonValue disabled = node.asDocument().get("disabled");
        return (disabled == null || !disabled.asBoolean().getValue());
    }
}
