/*
 * Copyright 2005 JBoss Inc
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

package org.drools.compiler.rule.builder;

import java.util.List;
import java.util.Map;

import org.drools.core.base.TypeResolver;
import org.drools.compiler.lang.descr.FunctionDescr;
import org.drools.core.rule.LineMappings;
import org.drools.core.rule.Package;
import org.kie.builder.KnowledgeBuilderResult;

public interface FunctionBuilder extends EngineElementBuilder {
    public String build(final Package pkg,
                        final FunctionDescr functionDescr,
                        final TypeResolver typeResolver,
                        final Map<String, LineMappings> lineMappings,
                        final List<KnowledgeBuilderResult> errors);
}
