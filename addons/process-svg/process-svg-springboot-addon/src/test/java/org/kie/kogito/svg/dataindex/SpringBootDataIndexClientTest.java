/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
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

package org.kie.kogito.svg.dataindex;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kie.kogito.svg.ProcessSVGException;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class SpringBootDataIndexClientTest {

    private String PROCESS_INSTANCE_ID = "pId";
    private final static String jsonString = "{\n" +
            "  \"data\": {\n" +
            "    \"ProcessInstances\": [\n" +
            "      {\n" +
            "        \"id\": \"piId\",\n" +
            "        \"processId\": \"processId\",\n" +
            "        \"nodes\": [\n" +
            "          {\n" +
            "            \"definitionId\": \"_9861B686-DF6B-4B1C-B370-F9898EEB47FD\",\n" +
            "            \"exit\": \"2020-10-11T06:49:47.26Z\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"definitionId\": \"_8B62D3CA-5D03-4B2B-832B-126469288BB4\",\n" +
            "            \"exit\": null\n" +
            "          }\n" +
            "        ]\n" +
            "      } " +
            "    ]\n" +
            "  }\n" +
            "}";

    SpringBootDataIndexClient client = new SpringBootDataIndexClient("data-indexURL", null);

    @Test
    public void testGetNodeInstancesFromResponse() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode response = objectMapper.readTree(jsonString);
        List<NodeInstance> nodes = client.getNodeInstancesFromResponse(response);
        assertThat(nodes).hasSize(2).containsExactly(
                new NodeInstance(true, "_9861B686-DF6B-4B1C-B370-F9898EEB47FD"),
                new NodeInstance(false, "_8B62D3CA-5D03-4B2B-832B-126469288BB4"));
    }

    @Test
    public void testGetEmptyNodeInstancesFromResponse() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        String emptyResults = "{ \"data\": { \"ProcessInstances\": [] }}";
        JsonNode response = objectMapper.readTree(emptyResults);
        List<NodeInstance> nodes = client.getNodeInstancesFromResponse(response);
        assertThat(nodes).isEmpty();
    }

    @Test
    public void testGetNodeInstancesFromProcessInstanceOkResponse() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        client = new SpringBootDataIndexClient("data-indexURL", restTemplate);
        lenient().when(restTemplate.postForEntity(eq("data-indexURL/graphql"), any(HttpEntity.class), eq(String.class))).thenReturn(ResponseEntity.ok(jsonString));

        List<NodeInstance> nodes = client.getNodeInstancesFromProcessInstance(PROCESS_INSTANCE_ID, "authHeader");
        assertThat(nodes).hasSize(2).containsExactly(
                new NodeInstance(true, "_9861B686-DF6B-4B1C-B370-F9898EEB47FD"),
                new NodeInstance(false, "_8B62D3CA-5D03-4B2B-832B-126469288BB4"));
    }

    @Test
    public void testGetNodeInstancesFromProcessInstance() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        client = new SpringBootDataIndexClient("data-indexURL", restTemplate);

        lenient().when(restTemplate.postForEntity(eq("data-indexURL/graphql"), any(HttpEntity.class), eq(String.class))).thenThrow(HttpClientErrorException.NotFound.class);
        assertThatThrownBy(() -> client.getNodeInstancesFromProcessInstance(PROCESS_INSTANCE_ID, "authHeader")).isInstanceOf(ProcessSVGException.class);
    }
}
