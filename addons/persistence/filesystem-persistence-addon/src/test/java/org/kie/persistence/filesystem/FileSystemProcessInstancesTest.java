/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.persistence.filesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE;
import static org.kie.api.runtime.process.ProcessInstance.STATE_COMPLETED;

import java.nio.file.Paths;
import java.util.Collections;

import org.drools.core.io.impl.ClassPathResource;
import org.junit.jupiter.api.Test;
import org.kie.kogito.auth.SecurityPolicy;
import org.kie.kogito.persistence.KogitoProcessInstancesFactory;
import org.kie.kogito.process.ProcessInstance;
import org.kie.kogito.process.WorkItem;
import org.kie.kogito.process.bpmn2.BpmnProcess;
import org.kie.kogito.process.bpmn2.BpmnVariables;
import org.kie.kogito.services.identity.StaticIdentityProvider;

public class FileSystemProcessInstancesTest {

	
	private SecurityPolicy securityPolicy = SecurityPolicy.of(new StaticIdentityProvider("john"));
	
    @Test
    public void testBasicFlow() {

        BpmnProcess process = (BpmnProcess) BpmnProcess.from(new ClassPathResource("BPMN2-UserTask.bpmn2")).get(0);
        process.setProcessInstancesFactory(new FileSystemProcessInstancesFactory());
        process.configure();
                                     
        ProcessInstance<BpmnVariables> processInstance = process.createInstance(BpmnVariables.create(Collections.singletonMap("test", "test")));

        processInstance.start();
        assertEquals(STATE_ACTIVE, processInstance.status());
        assertEquals("User Task", processInstance.description());
        
        process.instances().values();
        
        String testVar = (String) processInstance.variables().get("test");
        assertEquals("test", testVar);
        
        assertEquals("User Task", processInstance.description());

        WorkItem workItem = processInstance.workItems(securityPolicy).get(0);
        assertNotNull(workItem);
        assertEquals("john", workItem.getParameters().get("ActorId"));
        processInstance.completeWorkItem(workItem.getId(), null, securityPolicy);
        assertEquals(STATE_COMPLETED, processInstance.status());
    }
    
    private class FileSystemProcessInstancesFactory extends KogitoProcessInstancesFactory {
        
    	FileSystemProcessInstancesFactory() {
            super(Paths.get("target"));
        }

        @Override
        public String path() {
            return "target";
        }
    }
}
