/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates.
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
package org.kie.kogito.serverless.workflow.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;

import org.kie.kogito.serverless.workflow.parser.ParserContext;
import org.kie.kogito.serverless.workflow.utils.WorkflowResource;

import io.serverlessworkflow.api.Workflow;

public class URIContentLoaderFactory {

    public static byte[] readAllBytes(URIContentLoader loader) {
        try (InputStream is = loader.getInputStream()) {
            return is.readAllBytes();
        } catch (IOException io) {
            throw new UncheckedIOException(io);
        }
    }

    public static byte[] readAllBytes(Builder builder) {
        return readAllBytes(builder.build());
    }

    public static String readString(URIContentLoader loader) {
        return new String(readAllBytes(loader));
    }

    public static Path uriToPath(URI uri) {
        switch (URIContentLoaderType.from(uri)) {
            case CLASSPATH:
                return Path.of(ClassPathContentLoader.getPath(uri));
            case FILE:
                return FileContentLoader.getPath(uri);
            case HTTP:
            default:
                return Path.of(uri.getPath());
        }
    }

    public static String readString(Builder builder) {
        return readString(builder.build());
    }

    public static URIContentLoader buildLoader(URI uri, WorkflowResource workflowURI, String authRef) {
        return new Builder(uri).withWorkflow(workflowURI.getWorkflow()).withBaseURI(workflowURI.getUri()).withAuthRef(authRef).build();
    }

    public static URIContentLoader buildLoader(URI uri, Workflow workflow, Optional<ParserContext> context, String authRef) {
        Builder builder = new Builder(uri).withWorkflow(workflow).withAuthRef(authRef);
        context.map(c -> c.getContext().getClassLoader()).ifPresent(builder::withClassloader);
        // TODO add base URI
        return builder.build();
    }

    public static byte[] readBytes(String uriStr, Workflow workflow, ParserContext parserContext) {
        return readBytes(uriStr, workflow, Optional.ofNullable(parserContext));
    }

    public static byte[] readBytes(String uriStr, Workflow workflow, Optional<ParserContext> parserContext) {
        return readAllBytes(buildLoader(URI.create(uriStr), workflow, parserContext, null));
    }

    public static Builder builder(URI uri) {
        return new Builder(uri);
    }

    public static Builder builder(String uri) {
        return new Builder(URI.create(uri));
    }

    public static class Builder {
        private URI uri;
        private ClassLoader cl;
        private Workflow workflow;
        private String authRef;
        private URI baseURI;

        private Builder(URI uri) {
            this.uri = uri;
        }

        public Builder withClassloader(ClassLoader cl) {
            this.cl = cl;
            return this;
        }

        public Builder withWorkflow(Workflow workflow) {
            this.workflow = workflow;
            return this;
        }

        public Builder withAuthRef(String authRef) {
            this.authRef = authRef;
            return this;
        }

        public Builder withBaseURI(URI baseURI) {
            this.baseURI = baseURI;
            return this;
        }

        public URIContentLoader build() {
            if (uri.getScheme() == null && baseURI != null) {
                uri = compoundURI(baseURI, uri);
            }
            switch (URIContentLoaderType.from(uri)) {
                default:
                case FILE:
                    return new FileContentLoader(uri);
                case HTTP:
                    return new HttpContentLoader(uri, Optional.ofNullable(workflow), authRef);
                case CLASSPATH:
                    return new ClassPathContentLoader(uri, Optional.ofNullable(cl));
            }
        }
    }

    private static URI compoundURI(URI baseURI, URI uri) {
        char separator;
        switch (URIContentLoaderType.from(uri)) {
            case HTTP:
            case CLASSPATH:
                separator = '/';
                break;
            case FILE:
            default:
                separator = File.separatorChar;
                break;
        }
        String result = baseURI.toString();
        int lastIndexOf = result.lastIndexOf(separator);
        if (lastIndexOf != -1) {
            result = result.substring(0, lastIndexOf);
        }
        return URI.create(result.concat(separator + uri.toString()));
    }

    private URIContentLoaderFactory() {
    }
}
