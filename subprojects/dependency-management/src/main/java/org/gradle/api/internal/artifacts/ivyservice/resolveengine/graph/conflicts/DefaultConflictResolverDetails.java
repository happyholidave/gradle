/*
 * Copyright 2017 the original author or authors.
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
package org.gradle.api.internal.artifacts.ivyservice.resolveengine.graph.conflicts;

import org.gradle.api.artifacts.ModuleIdentifier;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.ComponentResolutionState;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.ConflictResolverDetails;

import javax.annotation.Nullable;
import java.util.Collection;

public class DefaultConflictResolverDetails<T extends ComponentResolutionState> implements ConflictResolverDetails<T> {
    private final Collection<ModuleIdentifier> participants;
    private final Collection<? extends T> candidates;
    private T selected;
    private Throwable failure;

    public DefaultConflictResolverDetails(Collection<ModuleIdentifier> participants, Collection<? extends T> candidates) {
        this.participants = participants;
        this.candidates = candidates;
    }

    @Override
    public Collection<? extends T> getCandidates() {
        return candidates;
    }

    @Override
    public void select(T candidate) {
        selected = candidate;
    }

    @Override
    public void fail(Throwable error) {
        failure = error;
    }

    @Override
    public T getSelected() {
        return selected;
    }


    @Override
    public Collection<ModuleIdentifier> getParticipants() {
        return participants;
    }

    @Nullable
    @Override
    public Throwable getFailure() {
        return failure;
    }

    @Override
    public boolean hasFailure() {
        return failure != null;
    }

    @Override
    public boolean hasSelected() {
        return selected != null;
    }
}
