/*
 * Copyright 2018 the original author or authors.
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
package org.gradle.api.internal.artifacts.dsl.dependencies;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import org.gradle.api.artifacts.ModuleIdentifier;
import org.gradle.api.artifacts.dsl.CapabilityHandler;
import org.gradle.internal.component.external.model.CapabilityDescriptor;
import org.gradle.internal.component.external.model.DefaultImmutableCapability;
import org.gradle.internal.typeconversion.NotationParser;

import java.util.Set;

class DefaultCapability implements CapabilityHandler, CapabilityInternal {
    private final NotationParser<Object, ModuleIdentifier> notationParser;
    private final String id;
    private String reason;

    final Set<ModuleIdentifier> providedBy = Sets.newTreeSet(MODULE_IDENTIFIER_COMPARATOR);
    ModuleIdentifier prefer;

    DefaultCapability(NotationParser<Object, ModuleIdentifier> notationParser, String id) {
        this.id = id;
        this.notationParser = notationParser;
    }

    @Override
    public CapabilityHandler providedBy(String moduleIdentifier) {
        providedBy.add(notationParser.parseNotation(moduleIdentifier));
        return this;
    }

    @Override
    public CapabilityHandler prefer(String moduleIdentifier) {
        prefer = notationParser.parseNotation(moduleIdentifier);
        providedBy.add(prefer); // implicit assumption made explicit
        return this;
    }

    @Override
    public CapabilityHandler because(String reason) {
        this.reason = reason;
        return this;
    }

    @Override
    public String getCapabilityId() {
        return id;
    }

    @Override
    public Set<ModuleIdentifier> getProvidedBy() {
        return providedBy;
    }

    @Override
    public ModuleIdentifier getPrefer() {
        return prefer;
    }

    @Override
    public String getReason() {
        return reason;
    }

    @Override
    public CapabilityDescriptor toCapabilityDescriptor() {
        ImmutableList.Builder<String> providers = new ImmutableList.Builder<String>();
        for (ModuleIdentifier identifier : providedBy) {
            providers.add(identifier.toString());
        }
        return new DefaultImmutableCapability(id, providers.build(), prefer == null ? null : prefer.toString(), reason);
    }
}
