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
import java.io.File;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.jbpm.bpmn2.activity.ScriptTaskModel;
import org.jbpm.bpmn2.activity.ScriptTaskProcess;
import org.jbpm.bpmn2.adhoc.AdHocSubProcessAutoCompleteExpressionModel;
import org.jbpm.bpmn2.adhoc.AdHocSubProcessAutoCompleteExpressionProcess;
import org.jbpm.bpmn2.adhoc.AdHocSubProcessAutoCompleteModel;
import org.jbpm.bpmn2.adhoc.AdHocSubProcessAutoCompleteProcess;
import org.jbpm.bpmn2.adhoc.AdHocSubProcessEmptyCompleteExpressionProcess;
import org.jbpm.bpmn2.adhoc.AdHocTerminateEndEventModel;
import org.jbpm.bpmn2.adhoc.AdHocTerminateEndEventProcess;
import org.jbpm.bpmn2.data.DataObjectModel;
import org.jbpm.bpmn2.data.DataObjectProcess;
import org.jbpm.bpmn2.data.Evaluation2Model;
import org.jbpm.bpmn2.data.Evaluation2Process;
import org.jbpm.bpmn2.data.Evaluation3Model;
import org.jbpm.bpmn2.data.Evaluation3Process;
import org.jbpm.bpmn2.data.EvaluationModel;
import org.jbpm.bpmn2.data.EvaluationProcess;
import org.jbpm.bpmn2.event.ErrorEndEventModel;
import org.jbpm.bpmn2.event.ErrorEndEventProcess;
import org.jbpm.bpmn2.event.SignalEndEventModel;
import org.jbpm.bpmn2.event.SignalEndEventProcess;
import org.jbpm.bpmn2.flow.CompositeWithDIGraphicalModel;
import org.jbpm.bpmn2.flow.CompositeWithDIGraphicalProcess;
import org.jbpm.bpmn2.flow.ExclusiveSplitModel;
import org.jbpm.bpmn2.flow.ExclusiveSplitProcess;
import org.jbpm.bpmn2.flow.InclusiveSplitModel;
import org.jbpm.bpmn2.flow.InclusiveSplitProcess;
import org.jbpm.bpmn2.flow.MinimalModel;
import org.jbpm.bpmn2.flow.MinimalProcess;
import org.jbpm.bpmn2.flow.MinimalWithDIGraphicalModel;
import org.jbpm.bpmn2.flow.MinimalWithDIGraphicalProcess;
import org.jbpm.bpmn2.flow.MinimalWithGraphicalModel;
import org.jbpm.bpmn2.flow.MinimalWithGraphicalProcess;
import org.jbpm.bpmn2.flow.SubProcessModel;
import org.jbpm.bpmn2.flow.SubProcessProcess;
import org.jbpm.bpmn2.flow.UserTaskModel;
import org.jbpm.bpmn2.flow.UserTaskProcess;
import org.jbpm.bpmn2.intermediate.EventBasedSplit4Model;
import org.jbpm.bpmn2.intermediate.EventBasedSplit4Process;
import org.jbpm.bpmn2.intermediate.EventBasedSplitModel;
import org.jbpm.bpmn2.intermediate.EventBasedSplitProcess;
import org.jbpm.bpmn2.intermediate.IntermediateThrowEventMessageModel;
import org.jbpm.bpmn2.intermediate.IntermediateThrowEventMessageProcess;
import org.jbpm.bpmn2.objects.Person;
import org.jbpm.bpmn2.objects.TestUserTaskWorkItemHandler;
import org.jbpm.bpmn2.objects.TestWorkItemHandler;
import org.jbpm.bpmn2.subprocess.CallActivityModel;
import org.jbpm.bpmn2.subprocess.CallActivityProcess;
import org.jbpm.bpmn2.subprocess.CallActivitySubProcessProcess;
import org.jbpm.bpmn2.task.SendTaskModel;
import org.jbpm.bpmn2.task.SendTaskProcess;
import org.jbpm.process.workitem.builtin.DoNothingWorkItemHandler;
import org.jbpm.process.workitem.builtin.ReceiveTaskHandler;
import org.jbpm.process.workitem.builtin.SendTaskHandler;
import org.jbpm.process.workitem.builtin.SystemOutWorkItemHandler;
import org.jbpm.test.util.NodeLeftCountDownProcessEventListener;
import org.jbpm.test.util.ProcessCompletedCountDownProcessEventListener;
import org.jbpm.test.utils.ProcessTestHelper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.kie.api.event.process.ProcessStartedEvent;
import org.kie.api.io.Resource;
import org.kie.internal.io.ResourceFactory;
import org.kie.kogito.Application;
import org.kie.kogito.auth.SecurityPolicy;
import org.kie.kogito.event.impl.MessageProducer;
import org.kie.kogito.internal.process.event.DefaultKogitoProcessEventListener;
import org.kie.kogito.internal.process.runtime.KogitoProcessInstance;
import org.kie.kogito.internal.process.runtime.KogitoWorkflowProcessInstance;
import org.kie.kogito.internal.process.workitem.KogitoWorkItem;
import org.kie.kogito.process.ProcessInstance;
import org.kie.kogito.process.impl.Sig;
import org.kie.kogito.process.workitems.impl.KogitoWorkItemImpl;
import org.w3c.dom.Document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class StandaloneBPMNProcessTest extends JbpmBpmn2TestCase {

    @Test
    public void testMinimalProcess() {
        Application app = ProcessTestHelper.newApplication();
        org.kie.kogito.process.Process<MinimalModel> minimalProcess = MinimalProcess.newProcess(app);
        MinimalModel model = minimalProcess.createModel();
        org.kie.kogito.process.ProcessInstance<MinimalModel> instance = minimalProcess.createInstance(model);
        instance.start();
        assertThat(instance.status()).isEqualTo(org.kie.kogito.process.ProcessInstance.STATE_COMPLETED);
    }

    @Test
    public void testMinimalProcessWithGraphical() {
        Application app = ProcessTestHelper.newApplication();
        org.kie.kogito.process.Process<MinimalWithGraphicalModel> minimalWithGraphicalProcess = MinimalWithGraphicalProcess.newProcess(app);
        MinimalWithGraphicalModel model = minimalWithGraphicalProcess.createModel();
        org.kie.kogito.process.ProcessInstance<MinimalWithGraphicalModel> instance = minimalWithGraphicalProcess.createInstance(model);
        instance.start();
        assertThat(instance.status()).isEqualTo(org.kie.kogito.process.ProcessInstance.STATE_COMPLETED);
    }

    @Test
    public void testMinimalProcessWithDIGraphical() {
        Application app = ProcessTestHelper.newApplication();
        org.kie.kogito.process.Process<MinimalWithDIGraphicalModel> process = MinimalWithDIGraphicalProcess.newProcess(app);
        MinimalWithDIGraphicalModel model = process.createModel();
        org.kie.kogito.process.ProcessInstance<MinimalWithDIGraphicalModel> instance = process.createInstance(model);
        instance.start();
        assertThat(instance.status()).isEqualTo(org.kie.kogito.process.ProcessInstance.STATE_COMPLETED);
    }

    @Test
    public void testCompositeProcessWithDIGraphical() throws Exception {
        Application app = ProcessTestHelper.newApplication();
        org.kie.kogito.process.Process<CompositeWithDIGraphicalModel> compositeWithDIGraphicalProcess = CompositeWithDIGraphicalProcess.newProcess(app);
        CompositeWithDIGraphicalModel model = compositeWithDIGraphicalProcess.createModel();
        org.kie.kogito.process.ProcessInstance<CompositeWithDIGraphicalModel> instance = compositeWithDIGraphicalProcess.createInstance(model);
        instance.start();
        assertThat(instance.status()).isEqualTo(org.kie.kogito.process.ProcessInstance.STATE_COMPLETED);
    }

    @Test
    public void testScriptTask() {
        Application app = ProcessTestHelper.newApplication();
        org.kie.kogito.process.Process<ScriptTaskModel> scriptTaskProcess = ScriptTaskProcess.newProcess(app);
        ScriptTaskModel model = scriptTaskProcess.createModel();
        org.kie.kogito.process.ProcessInstance<ScriptTaskModel> instance = scriptTaskProcess.createInstance(model);
        instance.start();
        assertThat(instance.status()).isEqualTo(org.kie.kogito.process.ProcessInstance.STATE_COMPLETED);
    }

    @Test
    public void testDataObject() {
        Application app = ProcessTestHelper.newApplication();
        org.kie.kogito.process.Process<DataObjectModel> dataObjectProcess = DataObjectProcess.newProcess(app);
        DataObjectModel model = dataObjectProcess.createModel();
        model.setEmployee("UserId-12345");
        org.kie.kogito.process.ProcessInstance<DataObjectModel> instance = dataObjectProcess.createInstance(model);
        instance.start();
        assertThat(instance.status()).isEqualTo(org.kie.kogito.process.ProcessInstance.STATE_COMPLETED);
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
    public void testUserTask() {
        Application app = ProcessTestHelper.newApplication();
        TestUserTaskWorkItemHandler workItemHandler = new TestUserTaskWorkItemHandler();
        ProcessTestHelper.registerHandler(app, "Human Task", workItemHandler);
        org.kie.kogito.process.Process<UserTaskModel> processDefinition = UserTaskProcess.newProcess(app);
        UserTaskModel model = processDefinition.createModel();
        model.setS("initialValue");
        org.kie.kogito.process.ProcessInstance<UserTaskModel> instance = processDefinition.createInstance(model);
        instance.start();
        assertThat(instance).extracting(ProcessInstance::status).isEqualTo(ProcessInstance.STATE_ACTIVE);
        KogitoWorkItem workItem = workItemHandler.getWorkItem();
        assertThat(workItem).isNotNull();
        assertThat(workItem.getParameter("ActorId")).isEqualTo("john");
        instance.completeWorkItem(workItem.getStringId(), Collections.emptyMap(), SecurityPolicy.of("john", null));
        assertThat(instance).extracting(ProcessInstance::status).isEqualTo(ProcessInstance.STATE_COMPLETED);
    }

    @Test
    public void testLane() throws Exception {
        kruntime = createKogitoProcessRuntime("org/jbpm/bpmn2/flow/BPMN2-Lane.bpmn2");

        TestWorkItemHandler workItemHandler = new TestWorkItemHandler();
        kruntime.getKogitoWorkItemManager().registerWorkItemHandler("Human Task", workItemHandler);
        KogitoProcessInstance processInstance = kruntime.startProcess("Lane");
        assertThat(processInstance.getState()).isEqualTo(KogitoProcessInstance.STATE_ACTIVE);
        kruntime.getKogitoWorkItemManager().registerWorkItemHandler("Human Task", workItemHandler);
        KogitoWorkItem workItem = workItemHandler.getWorkItem();
        assertThat(workItem).isNotNull();
        assertThat(workItem.getParameter("ActorId")).isEqualTo("john");
        Map<String, Object> results = new HashMap<>();
        ((KogitoWorkItemImpl) workItem).setParameter("ACTUA_OWNER", "mary");
        kruntime.getKogitoWorkItemManager().completeWorkItem(workItem.getStringId(), results);
        kruntime.getKogitoWorkItemManager().registerWorkItemHandler("Human Task", workItemHandler);
        workItem = workItemHandler.getWorkItem();
        assertThat(workItem).isNotNull();
        assertThat(workItem.getParameter("SwimlaneActorId")).isEqualTo("mary");
        kruntime.getKogitoWorkItemManager().completeWorkItem(workItem.getStringId(), null);
        assertProcessInstanceCompleted(processInstance.getStringId(), kruntime);
    }

    @Test
    public void testExclusiveSplit() {
        Application app = ProcessTestHelper.newApplication();
        ProcessTestHelper.registerHandler(app, "Email", new SystemOutWorkItemHandler());

        org.kie.kogito.process.Process<ExclusiveSplitModel> processDefinition = ExclusiveSplitProcess.newProcess(app);
        ExclusiveSplitModel model = processDefinition.createModel();
        model.setX("First");
        model.setY("Second");

        org.kie.kogito.process.ProcessInstance<ExclusiveSplitModel> instance = processDefinition.createInstance(model);
        instance.start();
        assertThat(instance).extracting(ProcessInstance::status).isEqualTo(ProcessInstance.STATE_COMPLETED);
    }

    @Test
    public void testInclusiveSplit() {
        Application app = ProcessTestHelper.newApplication();

        org.kie.kogito.process.Process<InclusiveSplitModel> processDefinition = InclusiveSplitProcess.newProcess(app);
        InclusiveSplitModel model = processDefinition.createModel();
        model.setX(15);

        org.kie.kogito.process.ProcessInstance<InclusiveSplitModel> instance = processDefinition.createInstance(model);
        instance.start();
        assertThat(instance).extracting(ProcessInstance::status).isEqualTo(ProcessInstance.STATE_COMPLETED);
    }

    @Test
    @Disabled
    public void testExclusiveSplitXPath() throws Exception {
        kruntime = createKogitoProcessRuntime("BPMN2-ExclusiveSplitXPath.bpmn2");

        kruntime.getKogitoWorkItemManager().registerWorkItemHandler("Email", new SystemOutWorkItemHandler());
        Document document = DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(new ByteArrayInputStream(
                        "<myDocument><chapter1>BlaBla</chapter1><chapter2>MoreBlaBla</chapter2></myDocument>".getBytes()));
        Map<String, Object> params = new HashMap<>();
        params.put("x", document);
        params.put("y", "SomeString");
        KogitoProcessInstance processInstance = kruntime.startProcess("com.sample.test", params);
        assertThat(processInstance.getState()).isEqualTo(KogitoProcessInstance.STATE_COMPLETED);
    }

    @Test
    public void testEventBasedSplit() {
        Application app = ProcessTestHelper.newApplication();
        ProcessTestHelper.registerHandler(app, "Email1", new SystemOutWorkItemHandler());
        ProcessTestHelper.registerHandler(app, "Email2", new SystemOutWorkItemHandler());

        org.kie.kogito.process.Process<EventBasedSplitModel> processDefinition = EventBasedSplitProcess.newProcess(app);
        EventBasedSplitModel model = processDefinition.createModel();
        org.kie.kogito.process.ProcessInstance<EventBasedSplitModel> instance = processDefinition.createInstance(model);
        instance.start();
        assertThat(instance).extracting(ProcessInstance::status).isEqualTo(ProcessInstance.STATE_ACTIVE);
        instance.send(Sig.of("Yes", "YesValue"));
        assertThat(instance).extracting(ProcessInstance::status).isEqualTo(ProcessInstance.STATE_COMPLETED);
        instance = processDefinition.createInstance(model);
        instance.start();
        assertThat(instance).extracting(ProcessInstance::status).isEqualTo(ProcessInstance.STATE_ACTIVE);
        instance.send(Sig.of("No", "NoValue"));
        assertThat(instance).extracting(ProcessInstance::status).isEqualTo(ProcessInstance.STATE_COMPLETED);
    }

    @Test
    public void testEventBasedSplitBefore() {
        Application app = ProcessTestHelper.newApplication();
        ProcessTestHelper.registerHandler(app, "Email1", new DoNothingWorkItemHandler());
        ProcessTestHelper.registerHandler(app, "Email2", new DoNothingWorkItemHandler());

        org.kie.kogito.process.Process<EventBasedSplitModel> processDefinition = EventBasedSplitProcess.newProcess(app);
        EventBasedSplitModel model = processDefinition.createModel();
        org.kie.kogito.process.ProcessInstance<EventBasedSplitModel> instance = processDefinition.createInstance(model);
        instance.start();
        assertThat(instance).extracting(ProcessInstance::status).isEqualTo(ProcessInstance.STATE_ACTIVE);
        instance.send(Sig.of("Yes", "YesValue"));
        assertThat(instance).extracting(ProcessInstance::status).isEqualTo(ProcessInstance.STATE_ACTIVE);

        instance = processDefinition.createInstance(model);
        instance.start();
        assertThat(instance).extracting(ProcessInstance::status).isEqualTo(ProcessInstance.STATE_ACTIVE);
        instance.send(Sig.of("No", "NoValue"));
        assertThat(instance).extracting(ProcessInstance::status).isEqualTo(ProcessInstance.STATE_ACTIVE);
    }

    @Test
    public void testEventBasedSplitAfter() {
        // signaling the other alternative after one has been selected should
        // have no effect
        Application app = ProcessTestHelper.newApplication();
        ProcessTestHelper.registerHandler(app, "Email1", new SystemOutWorkItemHandler());
        ProcessTestHelper.registerHandler(app, "Email2", new DoNothingWorkItemHandler());

        org.kie.kogito.process.Process<EventBasedSplitModel> processDefinition = EventBasedSplitProcess.newProcess(app);
        EventBasedSplitModel model = processDefinition.createModel();
        org.kie.kogito.process.ProcessInstance<EventBasedSplitModel> instance = processDefinition.createInstance(model);
        // Yes
        instance.start();
        assertThat(instance).extracting(ProcessInstance::status).isEqualTo(ProcessInstance.STATE_ACTIVE);
        instance.send(Sig.of("Yes", "YesValue"));
        assertThat(instance).extracting(ProcessInstance::status).isEqualTo(ProcessInstance.STATE_ACTIVE);
        // No
        instance.send(Sig.of("No", "NoValue"));
        assertThat(instance).extracting(ProcessInstance::status).isEqualTo(ProcessInstance.STATE_ACTIVE);
    }

    @Test
    @Timeout(10)
    public void testEventBasedSplit2() throws Exception {
        ProcessCompletedCountDownProcessEventListener countDownListener = new ProcessCompletedCountDownProcessEventListener(2);
        kruntime = createKogitoProcessRuntime("org/jbpm/bpmn2/intermediate/BPMN2-EventBasedSplit2.bpmn2");

        kruntime.getProcessEventManager().addEventListener(countDownListener);
        kruntime.getKogitoWorkItemManager().registerWorkItemHandler("Email1", new SystemOutWorkItemHandler());
        kruntime.getKogitoWorkItemManager().registerWorkItemHandler("Email2", new SystemOutWorkItemHandler());
        // Yes
        KogitoProcessInstance processInstance = kruntime.startProcess("EventBasedSplit2");
        assertThat(processInstance.getState()).isEqualTo(KogitoProcessInstance.STATE_ACTIVE);
        kruntime.signalEvent("Yes", "YesValue", processInstance.getStringId());
        assertProcessInstanceCompleted(processInstance.getStringId(), kruntime);

        // Timer
        processInstance = kruntime.startProcess("EventBasedSplit2");
        assertThat(processInstance.getState()).isEqualTo(KogitoProcessInstance.STATE_ACTIVE);

        countDownListener.waitTillCompleted();
        assertProcessInstanceCompleted(processInstance.getStringId(), kruntime);
    }

    @Test
    @Disabled("process does not complete")
    public void testEventBasedSplit3() throws Exception {
        kruntime = createKogitoProcessRuntime("BPMN2-EventBasedSplit3.bpmn2");

        kruntime.getKogitoWorkItemManager().registerWorkItemHandler("Email1", new SystemOutWorkItemHandler());
        kruntime.getKogitoWorkItemManager().registerWorkItemHandler("Email2", new SystemOutWorkItemHandler());
        Person jack = new Person();
        jack.setName("Jack");
        // Yes
        KogitoProcessInstance processInstance = kruntime.startProcess("com.sample.test");
        assertThat(processInstance.getState()).isEqualTo(KogitoProcessInstance.STATE_ACTIVE);
        kruntime.getKogitoWorkItemManager().registerWorkItemHandler("Email1", new SystemOutWorkItemHandler());
        kruntime.getKogitoWorkItemManager().registerWorkItemHandler("Email2", new SystemOutWorkItemHandler());
        kruntime.signalEvent("Yes", "YesValue", processInstance.getStringId());
        assertProcessInstanceCompleted(processInstance.getStringId(), kruntime);
        // Condition
        processInstance = kruntime.startProcess("com.sample.test");
        assertThat(processInstance.getState()).isEqualTo(KogitoProcessInstance.STATE_ACTIVE);
        kruntime.getKogitoWorkItemManager().registerWorkItemHandler("Email1", new SystemOutWorkItemHandler());
        kruntime.getKogitoWorkItemManager().registerWorkItemHandler("Email2", new SystemOutWorkItemHandler());
        kruntime.getKieSession().insert(jack);
        assertProcessInstanceCompleted(processInstance.getStringId(), kruntime);
    }

    @Test
    public void testEventBasedSplit4() {
        Application app = ProcessTestHelper.newApplication();
        ProcessTestHelper.registerHandler(app, "Email1", new SystemOutWorkItemHandler());
        ProcessTestHelper.registerHandler(app, "Email2", new SystemOutWorkItemHandler());

        org.kie.kogito.process.Process<EventBasedSplit4Model> processDefinition = EventBasedSplit4Process.newProcess(app);
        EventBasedSplit4Model model = processDefinition.createModel();
        org.kie.kogito.process.ProcessInstance<EventBasedSplit4Model> instance = processDefinition.createInstance(model);
        instance.start();
        assertThat(instance).extracting(ProcessInstance::status).isEqualTo(ProcessInstance.STATE_ACTIVE);
        instance.send(Sig.of("Message-YesMessage", "YesValue"));
        assertThat(instance).extracting(ProcessInstance::status).isEqualTo(ProcessInstance.STATE_COMPLETED);

        instance = processDefinition.createInstance(model);
        instance.start();
        instance.send(Sig.of("Message-NoMessage", "NoValue"));
        assertThat(instance).extracting(ProcessInstance::status).isEqualTo(ProcessInstance.STATE_COMPLETED);
    }

    @Test
    public void testEventBasedSplit5() throws Exception {
        kruntime = createKogitoProcessRuntime("org/jbpm/bpmn2/intermediate/BPMN2-EventBasedSplit5.bpmn2");

        kruntime.getKogitoWorkItemManager().registerWorkItemHandler("Email1", new SystemOutWorkItemHandler());
        kruntime.getKogitoWorkItemManager().registerWorkItemHandler("Email2", new SystemOutWorkItemHandler());
        ReceiveTaskHandler receiveTaskHandler = new ReceiveTaskHandler();
        kruntime.getKogitoWorkItemManager().registerWorkItemHandler("Receive Task", receiveTaskHandler);
        // Yes
        KogitoProcessInstance processInstance = kruntime.startProcess("EventBasedSplit5");
        assertThat(processInstance.getState()).isEqualTo(KogitoProcessInstance.STATE_ACTIVE);
        kruntime.getKogitoWorkItemManager().registerWorkItemHandler("Email1", new SystemOutWorkItemHandler());
        kruntime.getKogitoWorkItemManager().registerWorkItemHandler("Email2", new SystemOutWorkItemHandler());
        kruntime.getKogitoWorkItemManager().registerWorkItemHandler("Receive Task", receiveTaskHandler);

        receiveTaskHandler.getWorkItemId().stream().findFirst().ifPresent(id -> kruntime.getKogitoWorkItemManager().completeWorkItem(id, Map.of("Message", "YesValue")));

        assertProcessInstanceCompleted(processInstance.getStringId(), kruntime);

        receiveTaskHandler.getWorkItemId().forEach(id -> kruntime.getKogitoWorkItemManager().completeWorkItem(id, Map.of("Message", "NoValue")));

        kruntime.getKogitoWorkItemManager().registerWorkItemHandler("Email1", new SystemOutWorkItemHandler());
        kruntime.getKogitoWorkItemManager().registerWorkItemHandler("Email2", new SystemOutWorkItemHandler());
        kruntime.getKogitoWorkItemManager().registerWorkItemHandler("Receive Task", receiveTaskHandler);
        // No
        processInstance = kruntime.startProcess("EventBasedSplit5");
        receiveTaskHandler.getWorkItemId().stream().findFirst().ifPresent(id -> kruntime.getKogitoWorkItemManager().completeWorkItem(id, Map.of("Message", "NoValue")));

        assertProcessInstanceCompleted(processInstance.getStringId(), kruntime);

        receiveTaskHandler.getWorkItemId().stream().findFirst().ifPresent(id -> kruntime.getKogitoWorkItemManager().completeWorkItem(id, Map.of("Message", "YesValue")));
    }

    @Test
    public void testCallActivity() {
        Application app = ProcessTestHelper.newApplication();
        org.kie.kogito.process.Process<CallActivityModel> processDefinition = CallActivityProcess.newProcess(app);
        CallActivitySubProcessProcess.newProcess(app);
        CallActivityModel model = processDefinition.createModel();
        model.setX("oldValue");
        ProcessInstance<CallActivityModel> instance = processDefinition.createInstance(model);

        CallActivitySubProcessProcess.newProcess(app);

        instance.start();
        assertThat(instance).extracting(ProcessInstance::status).isEqualTo(ProcessInstance.STATE_COMPLETED);
        assertThat(instance.variables().getY()).isEqualTo("new value");
    }

    @Test
    public void testSubProcess() {
        Application app = ProcessTestHelper.newApplication();
        org.kie.kogito.process.Process<SubProcessModel> processDefinition = SubProcessProcess.newProcess(app);
        SubProcessModel model = processDefinition.createModel();
        org.kie.kogito.process.ProcessInstance<SubProcessModel> instance = processDefinition.createInstance(model);
        instance.start();
        assertThat(instance).extracting(ProcessInstance::status).isEqualTo(ProcessInstance.STATE_COMPLETED);
    }

    @Test
    public void testMultiInstanceLoopCharacteristicsProcess() throws Exception {
        kruntime = createKogitoProcessRuntime("org/jbpm/bpmn2/flow/BPMN2-MultiInstanceLoopCharacteristicsProcess.bpmn2");
        Map<String, Object> params = new HashMap<>();
        List<String> myList = new ArrayList<>();
        myList.add("First Item");
        myList.add("Second Item");
        params.put("list", myList);
        KogitoProcessInstance processInstance = kruntime.startProcess("MultiInstanceLoopCharacteristicsProcess", params);
        assertThat(processInstance.getState()).isEqualTo(KogitoProcessInstance.STATE_COMPLETED);
    }

    @Test
    public void testErrorBoundaryEvent() throws Exception {
        kruntime = createKogitoProcessRuntime("org/jbpm/bpmn2/error/BPMN2-ErrorBoundaryEventInterrupting.bpmn2");

        kruntime.getKogitoWorkItemManager().registerWorkItemHandler("MyTask", new DoNothingWorkItemHandler());
        KogitoProcessInstance processInstance = kruntime.startProcess("ErrorBoundaryEventInterrupting");
        assertProcessInstanceCompleted(processInstance.getStringId(), kruntime);
    }

    @Test
    @Timeout(10)
    public void testTimerBoundaryEvent() throws Exception {
        NodeLeftCountDownProcessEventListener countDownListener = new NodeLeftCountDownProcessEventListener("TimerEvent", 1);
        ProcessCompletedCountDownProcessEventListener processEventListener = new ProcessCompletedCountDownProcessEventListener();
        kruntime = createKogitoProcessRuntime("org/jbpm/bpmn2/timer/BPMN2-TimerBoundaryEventDuration.bpmn2");

        kruntime.getProcessEventManager().addEventListener(countDownListener);
        kruntime.getProcessEventManager().addEventListener(processEventListener);
        kruntime.getKogitoWorkItemManager().registerWorkItemHandler("MyTask", new DoNothingWorkItemHandler());
        KogitoProcessInstance processInstance = kruntime.startProcess("TimerBoundaryEventDuration");
        assertThat(processInstance.getState()).isEqualTo(KogitoProcessInstance.STATE_ACTIVE);
        countDownListener.waitTillCompleted();
        processEventListener.waitTillCompleted();
        assertProcessInstanceCompleted(processInstance.getStringId(), kruntime);
    }

    @Test
    //    @Timeout(10)
    public void testTimerBoundaryEventInterrupting() throws Exception {
        NodeLeftCountDownProcessEventListener countDownListener = new NodeLeftCountDownProcessEventListener("TimerEvent", 1);
        ProcessCompletedCountDownProcessEventListener processEventListener = new ProcessCompletedCountDownProcessEventListener();
        kruntime = createKogitoProcessRuntime("org/jbpm/bpmn2/timer/BPMN2-TimerBoundaryEventInterrupting.bpmn2");

        kruntime.getProcessEventManager().addEventListener(countDownListener);
        kruntime.getProcessEventManager().addEventListener(processEventListener);
        kruntime.getKogitoWorkItemManager().registerWorkItemHandler("MyTask", new DoNothingWorkItemHandler());
        KogitoProcessInstance processInstance = kruntime.startProcess("TimerBoundaryEventInterrupting");
        assertThat(processInstance.getState()).isEqualTo(KogitoProcessInstance.STATE_ACTIVE);
        countDownListener.waitTillCompleted();
        processEventListener.waitTillCompleted();
        assertProcessInstanceCompleted(processInstance.getStringId(), kruntime);
    }

    @Test
    @Disabled("Process does not complete.")
    public void testAdHocSubProcess() throws Exception {
        kruntime = createKogitoProcessRuntime("BPMN2-AdHocSubProcess.bpmn2");

        TestUserTaskWorkItemHandler workItemHandler = new TestUserTaskWorkItemHandler();
        kruntime.getKogitoWorkItemManager().registerWorkItemHandler("Human Task", workItemHandler);
        KogitoProcessInstance processInstance = kruntime.startProcess("AdHocSubProcess");
        assertThat(processInstance.getState()).isEqualTo(KogitoProcessInstance.STATE_ACTIVE);
        KogitoWorkItem workItem = workItemHandler.getWorkItem();
        assertThat(workItem).isNull();
        kruntime.getKogitoWorkItemManager().registerWorkItemHandler("Human Task", workItemHandler);
        kruntime.getKieSession().fireAllRules();

        kruntime.signalEvent("Hello2", null, processInstance.getStringId());
        workItem = workItemHandler.getWorkItem();
        assertThat(workItem).isNotNull();
        kruntime.getKogitoWorkItemManager().registerWorkItemHandler("Human Task", workItemHandler);
        kruntime.getKogitoWorkItemManager().completeWorkItem(workItem.getStringId(), null);
        assertProcessInstanceCompleted(processInstance.getStringId(), kruntime);
    }

    @Test
    public void testAdHocSubProcessAutoComplete() throws Exception {
        // this autocomplete when we detect
        // getActivityInstanceAttribute("numberOfActiveInstances") == 0
        Application app = ProcessTestHelper.newApplication();
        ProcessTestHelper.registerHandler(app, "Human Task", new TestUserTaskWorkItemHandler());
        org.kie.kogito.process.Process<AdHocSubProcessAutoCompleteModel> definition = AdHocSubProcessAutoCompleteProcess.newProcess(app);

        org.kie.kogito.process.ProcessInstance<AdHocSubProcessAutoCompleteModel> instance = definition.createInstance(definition.createModel());
        instance.start();
        assertThat(instance.status()).isEqualTo(KogitoProcessInstance.STATE_ACTIVE);

        // adhoc we need to trigger the human task
        instance.triggerNode("_2-1");
        ProcessTestHelper.completeWorkItem(instance, Collections.emptyMap(), "john");

        assertThat(instance.status()).isEqualTo(KogitoProcessInstance.STATE_COMPLETED);
    }

    @Test
    public void testAdHocSubProcessAutoCompleteExpression() throws Exception {
        // this autocomplete when we detect
        // getActivityInstanceAttribute("numberOfActiveInstances") == 0
        Application app = ProcessTestHelper.newApplication();
        ProcessTestHelper.registerHandler(app, "Human Task", new TestUserTaskWorkItemHandler());
        org.kie.kogito.process.Process<AdHocSubProcessAutoCompleteExpressionModel> definition = AdHocSubProcessAutoCompleteExpressionProcess.newProcess(app);
        AdHocSubProcessAutoCompleteExpressionModel model = definition.createModel();
        model.setCounter(3);
        org.kie.kogito.process.ProcessInstance<AdHocSubProcessAutoCompleteExpressionModel> instance = definition.createInstance(model);
        instance.start();
        assertThat(instance.status()).isEqualTo(KogitoProcessInstance.STATE_ACTIVE);

        // adhoc we need to trigger the human task
        instance.triggerNode("_2-1");
        ProcessTestHelper.completeWorkItem(instance, Collections.singletonMap("testHT", 0), "john");

        assertThat(instance.status()).isEqualTo(KogitoProcessInstance.STATE_COMPLETED);
    }

    @Test
    public void testAdHocTerminateEndEvent() throws Exception {
        // this autocomplete when we detect
        // terminate end event within adhoc process the adhoc should finish
        Application app = ProcessTestHelper.newApplication();
        ProcessTestHelper.registerHandler(app, "Human Task", new TestUserTaskWorkItemHandler());
        org.kie.kogito.process.Process<AdHocTerminateEndEventModel> definition = AdHocTerminateEndEventProcess.newProcess(app);
        AdHocTerminateEndEventModel model = definition.createModel();
        model.setComplete(false);
        org.kie.kogito.process.ProcessInstance<AdHocTerminateEndEventModel> instance = definition.createInstance(model);
        instance.start();
        assertThat(instance.status()).isEqualTo(KogitoProcessInstance.STATE_ACTIVE);

        // adhoc we need to trigger the human task
        instance.triggerNode("_560E157E-3173-4CFD-9CC6-26676D8B0A02");
        ProcessTestHelper.completeWorkItem(instance, Collections.emptyMap(), "john");

        assertThat(instance.status()).isEqualTo(KogitoProcessInstance.STATE_COMPLETED);
    }

    @Test
    public void testAdHocSubProcessEmptyCompleteExpression() throws Exception {
        try {
            Application app = ProcessTestHelper.newApplication();
            AdHocSubProcessEmptyCompleteExpressionProcess.newProcess(app);
            fail("Process should be invalid, there should be build errors");
        } catch (RuntimeException e) {
            // there should be build errors
        }
    }

    @Test
    public void testIntermediateCatchEventSignal() throws Exception {
        kruntime = createKogitoProcessRuntime("org/jbpm/bpmn2/intermediate/BPMN2-IntermediateCatchEventSignal.bpmn2");

        kruntime.getKogitoWorkItemManager().registerWorkItemHandler("Human Task", new SystemOutWorkItemHandler());
        KogitoProcessInstance processInstance = kruntime.startProcess("IntermediateCatchEventSignal");
        assertThat(processInstance.getState()).isEqualTo(KogitoProcessInstance.STATE_ACTIVE);
        // now signal process instance
        kruntime.signalEvent("MyMessage", "SomeValue", processInstance.getStringId());
        assertProcessInstanceCompleted(processInstance.getStringId(), kruntime);
    }

    @Test
    public void testIntermediateCatchEventMessage() throws Exception {
        kruntime = createKogitoProcessRuntime("org/jbpm/bpmn2/intermediate/BPMN2-IntermediateCatchEventMessage.bpmn2");

        kruntime.getKogitoWorkItemManager().registerWorkItemHandler("Human Task", new SystemOutWorkItemHandler());

        KogitoProcessInstance processInstance = kruntime.startProcess("IntermediateCatchEventMessage");
        assertThat(processInstance.getState()).isEqualTo(KogitoProcessInstance.STATE_ACTIVE);
        // now signal process instance
        kruntime.signalEvent("Message-HelloMessage", "SomeValue", processInstance.getStringId());
        assertProcessInstanceCompleted(processInstance.getStringId(), kruntime);
    }

    @Test
    @Timeout(10)
    public void testIntermediateCatchEventTimer() throws Exception {
        NodeLeftCountDownProcessEventListener countDownListener = new NodeLeftCountDownProcessEventListener("timer", 1);
        ProcessCompletedCountDownProcessEventListener processEventListener = new ProcessCompletedCountDownProcessEventListener();
        kruntime = createKogitoProcessRuntime("org/jbpm/bpmn2/intermediate/BPMN2-IntermediateCatchEventTimerDuration.bpmn2");

        kruntime.getProcessEventManager().addEventListener(countDownListener);
        kruntime.getProcessEventManager().addEventListener(processEventListener);
        kruntime.getKogitoWorkItemManager().registerWorkItemHandler("Human Task", new DoNothingWorkItemHandler());
        KogitoProcessInstance processInstance = kruntime.startProcess("IntermediateCatchEventTimerDuration");
        assertThat(processInstance.getState()).isEqualTo(KogitoProcessInstance.STATE_ACTIVE);
        // now wait for 1 second for timer to trigger
        countDownListener.waitTillCompleted();
        processEventListener.waitTillCompleted();
        assertProcessInstanceCompleted(processInstance.getStringId(), kruntime);
    }

    @Test
    @Disabled("process does not complete")
    public void testIntermediateCatchEventCondition() throws Exception {
        kruntime = createKogitoProcessRuntime("BPMN2-IntermediateCatchEventCondition.bpmn2");

        KogitoProcessInstance processInstance = kruntime.startProcess("IntermediateCatchEvent");
        assertThat(processInstance.getState()).isEqualTo(KogitoProcessInstance.STATE_ACTIVE);
        // now activate condition
        Person person = new Person();
        person.setName("Jack");
        kruntime.getKieSession().insert(person);
        assertProcessInstanceCompleted(processInstance.getStringId(), kruntime);
    }

    @Test
    public void testErrorEndEventProcess() {
        Application app = ProcessTestHelper.newApplication();

        org.kie.kogito.process.Process<ErrorEndEventModel> processDefinition = ErrorEndEventProcess.newProcess(app);
        ErrorEndEventModel model = processDefinition.createModel();

        org.kie.kogito.process.ProcessInstance<ErrorEndEventModel> instance = processDefinition.createInstance(model);
        instance.start();
        assertThat(instance).extracting(ProcessInstance::status).isEqualTo(ProcessInstance.STATE_ABORTED);
    }

    @Test
    public void testSendTask() {
        Application app = ProcessTestHelper.newApplication();
        SendTaskHandler sendTaskHandler = new SendTaskHandler();
        ProcessTestHelper.registerHandler(app, "Send Task", sendTaskHandler);
        org.kie.kogito.process.Process<SendTaskModel> processDefinition = SendTaskProcess.newProcess(app);
        SendTaskModel model = processDefinition.createModel();
        model.setS("john");
        org.kie.kogito.process.ProcessInstance<SendTaskModel> instance = processDefinition.createInstance(model);
        instance.start();
        assertThat(instance).extracting(ProcessInstance::status).isEqualTo(ProcessInstance.STATE_COMPLETED);
    }

    @Test
    public void testReceiveTask() throws Exception {
        kruntime = createKogitoProcessRuntime("org/jbpm/bpmn2/task/BPMN2-ReceiveTask.bpmn2");

        ReceiveTaskHandler receiveTaskHandler = new ReceiveTaskHandler();
        kruntime.getKogitoWorkItemManager().registerWorkItemHandler("Receive Task", receiveTaskHandler);
        KogitoWorkflowProcessInstance processInstance = (KogitoWorkflowProcessInstance) kruntime.startProcess("ReceiveTask");
        assertThat(processInstance.getState()).isEqualTo(KogitoProcessInstance.STATE_ACTIVE);

        receiveTaskHandler.getWorkItemId().stream().findFirst().ifPresent(id -> kruntime.getKogitoWorkItemManager().completeWorkItem(id, Map.of("Message", "Hello john!")));

        assertProcessInstanceCompleted(((KogitoProcessInstance) processInstance).getStringId(), kruntime);
    }

    @Test
    @Disabled("bpmn does not compile")
    public void testConditionalStart() throws Exception {
        kruntime = createKogitoProcessRuntime("BPMN2-ConditionalStart.bpmn2");

        Person person = new Person();
        person.setName("jack");
        kruntime.getKieSession().insert(person);
        kruntime.getKieSession().fireAllRules();
        person = new Person();
        person.setName("john");
        kruntime.getKieSession().insert(person);
        kruntime.getKieSession().fireAllRules();
    }

    @Test
    @Timeout(1000)
    public void testTimerStart() throws Exception {
        NodeLeftCountDownProcessEventListener countDownListener = new NodeLeftCountDownProcessEventListener("StartProcess", 5);
        kruntime = createKogitoProcessRuntime("org/jbpm/bpmn2/start/BPMN2-TimerStart.bpmn2");

        kruntime.getProcessEventManager().addEventListener(countDownListener);
        final List<String> list = new ArrayList<>();
        kruntime.getProcessEventManager().addEventListener(new DefaultKogitoProcessEventListener() {

            public void beforeProcessStarted(ProcessStartedEvent event) {
                list.add(((KogitoProcessInstance) event.getProcessInstance()).getStringId());
            }

        });

        assertThat(list).isEmpty();
        countDownListener.waitTillCompleted();
        assertThat(list).hasSize(5);
    }

    @Test
    public void testSignalStart() throws Exception {
        kruntime = createKogitoProcessRuntime("org/jbpm/bpmn2/start/BPMN2-SignalStart.bpmn2");
        final List<String> list = new ArrayList<>();
        kruntime.getProcessEventManager().addEventListener(new DefaultKogitoProcessEventListener() {
            public void afterProcessStarted(ProcessStartedEvent event) {
                list.add(((KogitoProcessInstance) event.getProcessInstance()).getStringId());
            }
        });
        kruntime.signalEvent("MySignal", "NewValue");
        assertThat(list).hasSize(1);
    }

    @Test
    public void testSignalEnd() {
        Application app = ProcessTestHelper.newApplication();

        org.kie.kogito.process.Process<SignalEndEventModel> processDefinition = SignalEndEventProcess.newProcess(app);
        SignalEndEventModel model = processDefinition.createModel();
        model.setX("MyValue");

        org.kie.kogito.process.ProcessInstance<SignalEndEventModel> instance = processDefinition.createInstance(model);
        instance.start();
        assertThat(instance.status()).isEqualTo(org.jbpm.process.instance.ProcessInstance.STATE_COMPLETED);
    }

    @Test
    public void testMessageStart() throws Exception {
        kruntime = createKogitoProcessRuntime("org/jbpm/bpmn2/start/BPMN2-MessageStart.bpmn2");
        final List<String> list = new ArrayList<>();
        kruntime.getProcessEventManager().addEventListener(new DefaultKogitoProcessEventListener() {
            public void afterProcessStarted(ProcessStartedEvent event) {
                list.add(((KogitoProcessInstance) event.getProcessInstance()).getStringId());
            }
        });
        kruntime.signalEvent("Message-HelloMessage", "NewValue");
        assertThat(list).hasSize(1);
    }

    @Test
    public void testMessageIntermediateThrow() {
        Application app = ProcessTestHelper.newApplication();
        ProcessTestHelper.registerHandler(app, "Send Task", new SendTaskHandler());
        IntermediateThrowEventMessageProcess definition = (IntermediateThrowEventMessageProcess) IntermediateThrowEventMessageProcess.newProcess(app);
        StringBuilder builder = new StringBuilder();
        definition.setProducer__2(new MessageProducer<String>() {
            @Override
            public void produce(KogitoProcessInstance pi, String eventData) {
                builder.append(eventData);
            }
        });
        IntermediateThrowEventMessageModel model = definition.createModel();
        model.setX("MyValue");

        org.kie.kogito.process.ProcessInstance<IntermediateThrowEventMessageModel> instance = definition.createInstance(model);
        instance.start();
        assertThat(builder.toString()).isEqualTo("MyValue");
        assertThat(instance.status()).isEqualTo(org.kie.kogito.process.ProcessInstance.STATE_COMPLETED);
    }

    @Test
    public void testSignalIntermediateThrow() throws Exception {
        kruntime = createKogitoProcessRuntime("org/jbpm/bpmn2/intermediate/BPMN2-IntermediateThrowEventSignal.bpmn2");
        Map<String, Object> params = new HashMap<>();
        params.put("x", "MyValue");
        KogitoProcessInstance processInstance = kruntime.startProcess("IntermediateThrowEventSignal", params);
        assertThat(processInstance.getState()).isEqualTo(KogitoProcessInstance.STATE_COMPLETED);
    }

    @Test
    public void testNoneIntermediateThrow() throws Exception {
        kruntime = createKogitoProcessRuntime("org/jbpm/bpmn2/intermediate/BPMN2-IntermediateThrowEventNone.bpmn2");
        KogitoProcessInstance processInstance = kruntime.startProcess("IntermediateThrowEventNone");
        assertThat(processInstance.getState()).isEqualTo(KogitoProcessInstance.STATE_COMPLETED);
    }

    @Test
    public void testXXEProcessVulnerability() throws Exception {
        Resource processResource = ResourceFactory.newClassPathResource("xxe-protection/BPMN2-XXE-Process.bpmn2");

        File dtdFile = new File("src/test/resources/xxe-protection/external.dtd");
        assertThat(dtdFile).exists();

        String dtdContent = new String(Files.readAllBytes(dtdFile.toPath()));
        dtdContent = dtdContent.replaceAll("@@PATH@@", dtdFile.getParentFile().getAbsolutePath());

        Files.write(dtdFile.toPath(), dtdContent.getBytes("UTF-8"));

        byte[] data = Files.readAllBytes(Paths.get(this.getClass().getResource("/xxe-protection/BPMN2-XXE-Process.bpmn2").getPath()));
        String processAsString = new String(data, "UTF-8");
        // replace place holders with actual paths
        File testFiles = new File("src/test/resources/xxe-protection");

        assertThat(testFiles).exists();

        String path = testFiles.getAbsolutePath();
        processAsString = processAsString.replaceAll("@@PATH@@", path);

        Resource resource = ResourceFactory.newReaderResource(new StringReader(processAsString));
        resource.setSourcePath(processResource.getSourcePath());
        resource.setTargetPath(processResource.getTargetPath());

        kruntime = createKogitoProcessRuntime(resource);
        KogitoProcessInstance processInstance = kruntime.startProcess("async-examples.bp1");

        String var1 = getProcessVarValue(processInstance, "testScript1");
        String var2 = getProcessVarValue(processInstance, "testScript2");

        assertThat(var1).isNull();
        assertThat(var2).isNull();
        assertThat(processInstance.getState()).isEqualTo(KogitoProcessInstance.STATE_COMPLETED);
    }

    @Test
    public void testVariableRefInIntermediateThrowEvent() throws Exception {
        kruntime = createKogitoProcessRuntime("BPMN2-WorkingMessageModel.bpmn2");

        kruntime.getKogitoWorkItemManager().registerWorkItemHandler("Send Task", new DoNothingWorkItemHandler());
        kruntime.getKogitoWorkItemManager().registerWorkItemHandler("Service Task", new DoNothingWorkItemHandler());

        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("messageContent", "some text");
        KogitoProcessInstance processInstance = kruntime.startProcess("workingMessageModel", parameters);

        assertThat(processInstance).isNotNull();
    }

}
