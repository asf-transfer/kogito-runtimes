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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.annotation.PostConstruct;

import org.kie.kogito.svg.ProcessSVGException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;

@Component
public class SpringBootDataIndexClient implements DataIndexClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringBootDataIndexClient.class);
    private String dataIndexHttpURL;

    private RestTemplate restTemplate;

    @Autowired
    public SpringBootDataIndexClient(
            @Value("${kogito.dataindex.http.url:http://localhost:8180}") String dataIndexHttpURL,
            @Autowired(required = false) RestTemplate restTemplate) {
        this.dataIndexHttpURL = dataIndexHttpURL;
        this.restTemplate = restTemplate;
    }

    @PostConstruct
    public void initialize() {
        if (restTemplate == null) {
            restTemplate = new RestTemplate();
            LOGGER.debug("No RestTemplate found, creating a default one");
        }
    }

    @Override
    public List<NodeInstance> getNodeInstancesFromProcessInstance(String processInstanceId, String authHeader) {
        String query = getNodeInstancesQuery(processInstanceId);
        CompletableFuture<List<NodeInstance>> cf = new CompletableFuture<>();
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String requestJson = objectMapper.writeValueAsString(singletonMap("query", query));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("Authorization", authHeader);
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity(requestJson, headers);
            ResponseEntity<String> result = restTemplate.postForEntity(dataIndexHttpURL + "/graphql",
                    request, String.class);
            if (result.getStatusCode().value() == 200) {
                cf.complete(getNodeInstancesFromResponse(objectMapper.readTree(result.getBody())));
            }
            return cf.get();
        } catch (Exception e) {
            throw new ProcessSVGException("Exception while trying to get data from Data Index service", e);
        }
    }

    protected List<NodeInstance> getNodeInstancesFromResponse(JsonNode response) {
        JsonNode pInstancesArray = response.path("data").path("ProcessInstances");
        if (pInstancesArray != null && !pInstancesArray.isEmpty()) {
            List<NodeInstance> nodes = new ArrayList<>();
            pInstancesArray.get(0).path("nodes").forEach(node -> nodes.add(new NodeInstance(!node.path("exit").isNull(), node.path("definitionId").asText())));
            return nodes;
        } else {
            return emptyList();
        }
    }
}
