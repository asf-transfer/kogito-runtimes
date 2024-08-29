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
package org.jbpm.bpmn2;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jbpm.bpmn2.activity.XPathProcessModel;
import org.jbpm.bpmn2.activity.XPathProcessProcess;
import org.jbpm.bpmn2.core.Association;
import org.jbpm.bpmn2.core.DataStore;
import org.jbpm.bpmn2.core.Definitions;
import org.jbpm.bpmn2.data.AssociationModel;
import org.jbpm.bpmn2.data.AssociationProcess;
import org.jbpm.bpmn2.data.CorrelationKeyModel;
import org.jbpm.bpmn2.data.CorrelationKeyProcess;
import org.jbpm.bpmn2.data.DataInputAssociationsLazyCreatingModel;
import org.jbpm.bpmn2.data.DataInputAssociationsLazyCreatingProcess;
import org.jbpm.bpmn2.data.DataInputAssociationsModel;
import org.jbpm.bpmn2.data.DataInputAssociationsProcess;
import org.jbpm.bpmn2.data.DataInputAssociationsStringModel;
import org.jbpm.bpmn2.data.DataInputAssociationsStringNoQuotesModel;
import org.jbpm.bpmn2.data.DataInputAssociationsStringNoQuotesProcess;
import org.jbpm.bpmn2.data.DataInputAssociationsStringObjectModel;
import org.jbpm.bpmn2.data.DataInputAssociationsStringObjectProcess;
import org.jbpm.bpmn2.data.DataInputAssociationsStringProcess;
import org.jbpm.bpmn2.data.DataInputAssociationsXmlLiteralModel;
import org.jbpm.bpmn2.data.DataInputAssociationsXmlLiteralProcess;
import org.jbpm.bpmn2.data.DataObjectModel;
import org.jbpm.bpmn2.data.DataObjectProcess;
import org.jbpm.bpmn2.data.DataOutputAssociationsModel;
import org.jbpm.bpmn2.data.DataOutputAssociationsProcess;
import org.jbpm.bpmn2.data.DataOutputAssociationsXmlNodeModel;
import org.jbpm.bpmn2.data.DataOutputAssociationsXmlNodeProcess;
import org.jbpm.bpmn2.data.DataStoreModel;
import org.jbpm.bpmn2.data.DataStoreProcess;
import org.jbpm.bpmn2.data.Evaluation2Model;
import org.jbpm.bpmn2.data.Evaluation2Process;
import org.jbpm.bpmn2.data.Evaluation3Model;
import org.jbpm.bpmn2.data.Evaluation3Process;
import org.jbpm.bpmn2.data.EvaluationModel;
import org.jbpm.bpmn2.data.EvaluationProcess;
import org.jbpm.bpmn2.data.ImportModel;
import org.jbpm.bpmn2.data.ImportProcess;
import org.jbpm.bpmn2.flow.DataOutputAssociationsHumanTaskModel;
import org.jbpm.bpmn2.flow.DataOutputAssociationsHumanTaskProcess;
import org.jbpm.process.core.datatype.impl.type.ObjectDataType;
import org.jbpm.process.instance.impl.demo.SystemOutWorkItemHandler;
import org.jbpm.test.utils.ProcessTestHelper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.kie.kogito.Application;
import org.kie.kogito.internal.process.runtime.KogitoProcessInstance;
import org.kie.kogito.internal.process.runtime.KogitoWorkItem;
import org.kie.kogito.internal.process.runtime.KogitoWorkItemHandler;
import org.kie.kogito.internal.process.runtime.KogitoWorkItemManager;
import org.kie.kogito.process.ProcessInstance;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import static org.assertj.core.api.Assertions.assertThat;

public class DataTest extends JbpmBpmn2TestCase {

    @Test
    public void testImport() {
        Application app = ProcessTestHelper.newApplication();
        org.kie.kogito.process.Process<ImportModel> process = ImportProcess.newProcess(app);
        ImportModel model = process.createModel();
        ProcessInstance<ImportModel> processInstance = process.createInstance(model);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(org.jbpm.process.instance.ProcessInstance.STATE_COMPLETED);
    }

    @Test
    public void testDataObject() {
        Application app = ProcessTestHelper.newApplication();
        org.kie.kogito.process.Process<DataObjectModel> process = DataObjectProcess.newProcess(app);
        DataObjectModel model = process.createModel();
        model.setEmployee("UserId-12345");
        ProcessInstance<DataObjectModel> processInstance = process.createInstance(model);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(org.jbpm.process.instance.ProcessInstance.STATE_COMPLETED);

    }

    @Test
    public void testDataStore() throws Exception {
        Application app = ProcessTestHelper.newApplication();

        org.kie.kogito.process.Process<DataStoreModel> processDefinition = DataStoreProcess.newProcess(app);
        DataStoreModel model = processDefinition.createModel();
        org.kie.kogito.process.ProcessInstance<DataStoreModel> instance = processDefinition.createInstance(model);
        instance.start();

        /*
         * metaData is not present, I think it is protected and hence cannot import.
         * How to get process metadata from the process object(only then we can get the definition metadata)
         */

        Definitions def = (Definitions) instance.process().metaData().get("Definitions");
        assertThat(def.getDataStores()).isNotNull().hasSize(1);
        DataStore dataStore = def.getDataStores().get(0);
        assertThat(dataStore.getId()).isEqualTo("employee");
        assertThat(dataStore.getName()).isEqualTo("employeeStore");
        assertThat(((ObjectDataType) dataStore.getType()).getClassName()).isEqualTo(String.class.getCanonicalName());
    }

    @Test
    public void testAssociation() throws Exception {
        Application app = ProcessTestHelper.newApplication();
        org.kie.kogito.process.Process<AssociationModel> processDefinition = AssociationProcess.newProcess(app);
        AssociationModel model = processDefinition.createModel();
        org.kie.kogito.process.ProcessInstance<AssociationModel> instance = processDefinition.createInstance(model);
        instance.start();

        /*
         * metaData is not present, I think it is protected and hence cannot import.
         * How to get process metadata from the process object(only then we can get the definition metadata)
         */

        Definitions def = (Definitions) instance.process().metaData().get("Definitions");
        List<Association> associations = def.getAssociations();
        assertThat(associations).isNotNull().hasSize(1);
        Association assoc = associations.get(0);
        assertThat(assoc.getId()).isEqualTo("_1234");
        assertThat(assoc.getSourceRef()).isEqualTo("_1");
        assertThat(assoc.getTargetRef()).isEqualTo("_2");
    }

    @Test
    public void testEvaluationProcess() {
        Application app = ProcessTestHelper.newApplication();
        ProcessTestHelper.registerHandler(app, "Human Task", new SystemOutWorkItemHandler());
        ProcessTestHelper.registerHandler(app, "RegisterRequest", new SystemOutWorkItemHandler());

        org.kie.kogito.process.Process<EvaluationModel> processDefinition = EvaluationProcess.newProcess(app);
        EvaluationModel model = processDefinition.createModel();
        model.setEmployee("UserId-12345");

        org.kie.kogito.process.ProcessInstance<EvaluationModel> instance = processDefinition.createInstance(model);
        instance.start();
        assertThat(instance).extracting(ProcessInstance::status).isEqualTo(ProcessInstance.STATE_COMPLETED);
    }

    @Test
    public void testEvaluationProcess2() {
        Application app = ProcessTestHelper.newApplication();
        ProcessTestHelper.registerHandler(app, "Human Task", new SystemOutWorkItemHandler());

        org.kie.kogito.process.Process<Evaluation2Model> processDefinition = Evaluation2Process.newProcess(app);
        Evaluation2Model model = processDefinition.createModel();
        model.setEmployee("UserId-12345");

        org.kie.kogito.process.ProcessInstance<Evaluation2Model> instance = processDefinition.createInstance(model);
        instance.start();
        assertThat(instance).extracting(ProcessInstance::status).isEqualTo(ProcessInstance.STATE_COMPLETED);
    }

    @Test
    public void testEvaluationProcess3() {
        Application app = ProcessTestHelper.newApplication();
        ProcessTestHelper.registerHandler(app, "Human Task", new SystemOutWorkItemHandler());
        ProcessTestHelper.registerHandler(app, "RegisterRequest", new SystemOutWorkItemHandler());

        org.kie.kogito.process.Process<Evaluation3Model> processDefinition = Evaluation3Process.newProcess(app);
        Evaluation3Model model = processDefinition.createModel();
        model.setEmployee("john2");

        org.kie.kogito.process.ProcessInstance<Evaluation3Model> instance = processDefinition.createInstance(model);
        instance.start();
        assertThat(instance).extracting(ProcessInstance::status).isEqualTo(ProcessInstance.STATE_COMPLETED);
    }

    @Test
    public void testXpathExpression() throws Exception {
        Application app = ProcessTestHelper.newApplication();
        org.kie.kogito.process.Process<XPathProcessModel> process = XPathProcessProcess.newProcess(app);
        XPathProcessModel model = process.createModel();

        Document document = DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(new ByteArrayInputStream(
                        "<instanceMetadata><user approved=\"false\" /></instanceMetadata>"
                                .getBytes()));

        model.setInstanceMetadata(document);
        ProcessInstance<XPathProcessModel> processInstance = process.createInstance(model);
        processInstance.start();
        assertThat(processInstance).extracting(ProcessInstance::status).isEqualTo(ProcessInstance.STATE_COMPLETED);

    }

    @Test
    public void testDataInputAssociations() throws Exception {
        Application app = ProcessTestHelper.newApplication();
        ProcessTestHelper.registerHandler(app, "Human Task", new KogitoWorkItemHandler() {
            @Override
            public void abortWorkItem(KogitoWorkItem workItem, KogitoWorkItemManager mgr) {
            }

            @Override
            public void executeWorkItem(KogitoWorkItem workItem, KogitoWorkItemManager mgr) {
                assertThat(workItem.getParameter("coId")).isEqualTo("hello world");
            }
        });

        Document document = DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(new ByteArrayInputStream("<user hello='hello world' />".getBytes()));

        org.kie.kogito.process.Process<DataInputAssociationsModel> processDefinition = DataInputAssociationsProcess.newProcess(app);
        DataInputAssociationsModel model = processDefinition.createModel();
        model.setInstanceMetadata(document.getFirstChild());

        org.kie.kogito.process.ProcessInstance<DataInputAssociationsModel> instance = processDefinition.createInstance(model);
        instance.start();
    }

    @Test
    public void testDataInputAssociationsWithStringObject() {
        Application app = ProcessTestHelper.newApplication();
        ProcessTestHelper.registerHandler(app, "Human Task", new KogitoWorkItemHandler() {
            @Override
            public void abortWorkItem(KogitoWorkItem workItem, KogitoWorkItemManager mgr) {
            }

            @Override
            public void executeWorkItem(KogitoWorkItem workItem, KogitoWorkItemManager mgr) {
                assertThat(workItem.getParameter("coId")).isEqualTo("hello");
            }
        });

        org.kie.kogito.process.Process<DataInputAssociationsStringObjectModel> processDefinition = DataInputAssociationsStringObjectProcess.newProcess(app);
        DataInputAssociationsStringObjectModel model = processDefinition.createModel();
        model.setInstanceMetadata("hello");

        org.kie.kogito.process.ProcessInstance<DataInputAssociationsStringObjectModel> instance = processDefinition.createInstance(model);
        instance.start();
    }

    /**
     * TODO testDataInputAssociationsWithLazyLoading
     */
    @Test
    public void testDataInputAssociationsWithLazyLoading() throws Exception {
        Application app = ProcessTestHelper.newApplication();
        ProcessTestHelper.registerHandler(app, "Human Task", new KogitoWorkItemHandler() {
            @Override
            public void abortWorkItem(KogitoWorkItem workItem, KogitoWorkItemManager mgr) {
            }

            @Override
            public void executeWorkItem(KogitoWorkItem workItem, KogitoWorkItemManager mgr) {
                Element coIdParamObj = (Element) workItem.getParameter("coId");
                assertThat(coIdParamObj.getNodeName()).isEqualTo("mydoc");
                assertThat(coIdParamObj.getFirstChild().getNodeName()).isEqualTo("mynode");
                assertThat(coIdParamObj.getFirstChild().getFirstChild().getNodeName()).isEqualTo("user");
                assertThat(coIdParamObj.getFirstChild().getFirstChild().getAttributes().getNamedItem("hello").getNodeValue()).isEqualTo("hello world");
            }
        });

        Document document = DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(new ByteArrayInputStream("<user hello='hello world' />".getBytes()));
        Map<String, Object> params = new HashMap<>();
        params.put("instanceMetadata", document.getFirstChild());

        org.kie.kogito.process.Process<DataInputAssociationsLazyCreatingModel> processDefinition = DataInputAssociationsLazyCreatingProcess.newProcess(app);
        DataInputAssociationsLazyCreatingModel model = processDefinition.createModel();
        model.setInstanceMetadata(document.getFirstChild());

        org.kie.kogito.process.ProcessInstance<DataInputAssociationsLazyCreatingModel> instance = processDefinition.createInstance(model);
        instance.start();
    }

    @Test
    public void testDataInputAssociationsWithString() {
        Application app = ProcessTestHelper.newApplication();
        ProcessTestHelper.registerHandler(app, "Human Task", new KogitoWorkItemHandler() {
            @Override
            public void abortWorkItem(KogitoWorkItem workItem, KogitoWorkItemManager mgr) {
            }

            @Override
            public void executeWorkItem(KogitoWorkItem workItem, KogitoWorkItemManager mgr) {
                assertThat(workItem.getParameter("coId")).isEqualTo("hello");
            }
        });

        org.kie.kogito.process.Process<DataInputAssociationsStringModel> processDefinition = DataInputAssociationsStringProcess.newProcess(app);
        DataInputAssociationsStringModel model = processDefinition.createModel();

        org.kie.kogito.process.ProcessInstance<DataInputAssociationsStringModel> instance = processDefinition.createInstance(model);
        instance.start();
    }

    @Test
    public void testDataInputAssociationsWithStringWithoutQuotes() {
        Application app = ProcessTestHelper.newApplication();
        ProcessTestHelper.registerHandler(app, "Human Task", new KogitoWorkItemHandler() {
            @Override
            public void abortWorkItem(KogitoWorkItem workItem, KogitoWorkItemManager mgr) {
            }

            @Override
            public void executeWorkItem(KogitoWorkItem workItem, KogitoWorkItemManager mgr) {
                assertThat(workItem.getParameter("coId")).isEqualTo("hello");
            }
        });

        org.kie.kogito.process.Process<DataInputAssociationsStringNoQuotesModel> processDefinition = DataInputAssociationsStringNoQuotesProcess.newProcess(app);
        DataInputAssociationsStringNoQuotesModel model = processDefinition.createModel();

        org.kie.kogito.process.ProcessInstance<DataInputAssociationsStringNoQuotesModel> instance = processDefinition.createInstance(model);
        instance.start();
    }

    @Test
    public void testDataInputAssociationsWithXMLLiteral() {
        Application app = ProcessTestHelper.newApplication();
        ProcessTestHelper.registerHandler(app, "Human Task", new KogitoWorkItemHandler() {
            @Override
            public void abortWorkItem(KogitoWorkItem workItem, KogitoWorkItemManager mgr) {
            }

            @Override
            public void executeWorkItem(KogitoWorkItem workItem, KogitoWorkItemManager mgr) {
                assertThat(((org.w3c.dom.Node) workItem.getParameter("coId")).getNodeName()).isEqualTo("id");
                assertThat(((org.w3c.dom.Node) workItem.getParameter("coId")).getFirstChild().getTextContent()).isEqualTo("some text");
            }
        });

        org.kie.kogito.process.Process<DataInputAssociationsXmlLiteralModel> processDefinition = DataInputAssociationsXmlLiteralProcess.newProcess(app);
        DataInputAssociationsXmlLiteralModel model = processDefinition.createModel();

        org.kie.kogito.process.ProcessInstance<DataInputAssociationsXmlLiteralModel> instance = processDefinition.createInstance(model);
        instance.start();
    }

    /**
     * TODO testDataInputAssociationsWithTwoAssigns
     */
    @Test
    @Disabled
    public void testDataInputAssociationsWithTwoAssigns() throws Exception {
        kruntime = createKogitoProcessRuntime("BPMN2-DataInputAssociations-two-assigns.bpmn2");
        kruntime.getKogitoWorkItemManager().registerWorkItemHandler("Human Task",
                new KogitoWorkItemHandler() {

                    public void abortWorkItem(KogitoWorkItem manager,
                            KogitoWorkItemManager mgr) {

                    }

                    public void executeWorkItem(KogitoWorkItem workItem,
                            KogitoWorkItemManager mgr) {
                        assertThat(((Element) workItem.getParameter("Comment")).getNodeName()).isEqualTo("foo");
                        // assertEquals("mynode", ((Element)
                        // workItem.getParameter("Comment")).getFirstChild().getNodeName());
                        // assertEquals("user", ((Element)
                        // workItem.getParameter("Comment")).getFirstChild().getFirstChild().getNodeName());
                        // assertEquals("hello world", ((Element)
                        // workItem.getParameter("coId")).getFirstChild().getFirstChild().getAttributes().getNamedItem("hello").getNodeValue());
                    }

                });
        Document document = DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(new ByteArrayInputStream("<user hello='hello world' />"
                        .getBytes()));
        Map<String, Object> params = new HashMap<>();
        params.put("instanceMetadata", document.getFirstChild());
        KogitoProcessInstance processInstance = kruntime.startProcess("process",
                params);

    }

    @Test
    public void testDataOutputAssociationsforHumanTask() {
        Application app = ProcessTestHelper.newApplication();
        ProcessTestHelper.registerHandler(app, "Human Task", new KogitoWorkItemHandler() {
            @Override
            public void abortWorkItem(KogitoWorkItem workItem, KogitoWorkItemManager mgr) {
            }

            @Override
            public void executeWorkItem(KogitoWorkItem workItem, KogitoWorkItemManager mgr) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder;
                try {
                    builder = factory.newDocumentBuilder();
                } catch (ParserConfigurationException e) {
                    throw new RuntimeException(e);
                }
                final Map<String, Object> results = new HashMap<>();
                // process metadata
                org.w3c.dom.Document processMetadaDoc = builder.newDocument();
                org.w3c.dom.Element processMetadata = processMetadaDoc.createElement("previoustasksowner");
                processMetadaDoc.appendChild(processMetadata);
                processMetadata.setAttribute("primaryname", "my_result");
                results.put("output", processMetadata);
                mgr.completeWorkItem(workItem.getStringId(), results);
            }
        });

        org.kie.kogito.process.Process<DataOutputAssociationsHumanTaskModel> processDefinition = DataOutputAssociationsHumanTaskProcess.newProcess(app);
        DataOutputAssociationsHumanTaskModel model = processDefinition.createModel();

        org.kie.kogito.process.ProcessInstance<DataOutputAssociationsHumanTaskModel> instance = processDefinition.createInstance(model);
        instance.start();
    }

    @Test
    public void testDataOutputAssociations() {
        Application app = ProcessTestHelper.newApplication();
        ProcessTestHelper.registerHandler(app, "Human Task", new KogitoWorkItemHandler() {
            @Override
            public void abortWorkItem(KogitoWorkItem workItem, KogitoWorkItemManager mgr) {
            }

            @Override
            public void executeWorkItem(KogitoWorkItem workItem, KogitoWorkItemManager mgr) {
                try {
                    Document document = DocumentBuilderFactory
                            .newInstance()
                            .newDocumentBuilder()
                            .parse(new ByteArrayInputStream("<user hello='hello world' />".getBytes()));
                    Map<String, Object> params = new HashMap<>();
                    params.put("output", document.getFirstChild());
                    mgr.completeWorkItem(workItem.getStringId(), params);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
        });

        org.kie.kogito.process.Process<DataOutputAssociationsModel> processDefinition = DataOutputAssociationsProcess.newProcess(app);
        DataOutputAssociationsModel model = processDefinition.createModel();

        org.kie.kogito.process.ProcessInstance<DataOutputAssociationsModel> instance = processDefinition.createInstance(model);
        instance.start();
    }

    @Test
    public void testDataOutputAssociationsXmlNode() {
        Application app = ProcessTestHelper.newApplication();
        ProcessTestHelper.registerHandler(app, "Human Task", new KogitoWorkItemHandler() {
            @Override
            public void abortWorkItem(KogitoWorkItem workItem, KogitoWorkItemManager mgr) {
            }

            @Override
            public void executeWorkItem(KogitoWorkItem workItem, KogitoWorkItemManager mgr) {
                try {
                    Document document = DocumentBuilderFactory
                            .newInstance()
                            .newDocumentBuilder()
                            .parse(new ByteArrayInputStream("<user hello='hello world' />".getBytes()));
                    Map<String, Object> params = new HashMap<>();
                    params.put("output", document.getFirstChild());
                    mgr.completeWorkItem(workItem.getStringId(), params);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
        });

        org.kie.kogito.process.Process<DataOutputAssociationsXmlNodeModel> processDefinition = DataOutputAssociationsXmlNodeProcess.newProcess(app);
        DataOutputAssociationsXmlNodeModel model = processDefinition.createModel();

        org.kie.kogito.process.ProcessInstance<DataOutputAssociationsXmlNodeModel> instance = processDefinition.createInstance(model);
        instance.start();
    }

    public void testDefaultProcessVariableValue() {
        Application app = ProcessTestHelper.newApplication();
        org.kie.kogito.process.Process<CorrelationKeyModel> processDefinition = CorrelationKeyProcess.newProcess(app);
        CorrelationKeyModel model = processDefinition.createModel();
        org.kie.kogito.process.ProcessInstance<CorrelationKeyModel> instance = processDefinition.createInstance(model);
        instance.start();

        /*
         * CorrelationKeyModel class does not have methods named getProcVar and getIntVar .
         * This is due to acutal variables/methods being different in the CorrelationKeyModel class.
         */
        assertThat(model.getProcVar()).isEqualTo("defaultProc");
        assertThat(model.getIntVar()).isEqualTo(1);

    }

}
