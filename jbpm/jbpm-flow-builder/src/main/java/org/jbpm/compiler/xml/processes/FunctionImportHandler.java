/*
 * Copyright 2010 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jbpm.compiler.xml.processes;

import java.util.ArrayList;
import java.util.HashSet;

import org.jbpm.compiler.xml.Handler;
import org.jbpm.compiler.xml.Parser;
import org.jbpm.compiler.xml.core.BaseAbstractHandler;
import org.jbpm.process.core.Process;
import org.jbpm.workflow.core.impl.WorkflowProcessImpl;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class FunctionImportHandler extends BaseAbstractHandler
        implements
        Handler {
    public FunctionImportHandler() {
        if ((this.validParents == null) && (this.validPeers == null)) {
            this.validParents = new HashSet();
            this.validParents.add(Process.class);

            this.validPeers = new HashSet();
            this.validPeers.add(null);

            this.allowNesting = false;
        }
    }

    public Object start(final String uri,
            final String localName,
            final Attributes attrs,
            final Parser parser) throws SAXException {
        parser.startElementBuilder(localName,
                attrs);

        WorkflowProcessImpl process = (WorkflowProcessImpl) parser.getParent();

        final String name = attrs.getValue("name");
        emptyAttributeCheck(localName, "name", name, parser);

        java.util.List<String> list = process.getFunctionImports();
        if (list == null) {
            list = new ArrayList<>();
            process.setFunctionImports(list);
        }
        list.add(name);

        return null;
    }

    public Object end(final String uri,
            final String localName,
            final Parser parser) throws SAXException {
        return null;
    }

    public Class generateNodeFor() {
        return null;
    }

}
