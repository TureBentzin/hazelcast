/*
 * Copyright (c) 2008-2017, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.jet.stream.impl.pipeline;

import com.hazelcast.jet.DAG;
import com.hazelcast.jet.Vertex;
import com.hazelcast.jet.stream.impl.processor.LimitP;

import static com.hazelcast.jet.Edge.between;
import static com.hazelcast.jet.stream.impl.StreamUtil.uniqueVertexName;

public class LimitPipeline<T> extends AbstractIntermediatePipeline<T, T> {
    private final long limit;

    public LimitPipeline(StreamContext context, Pipeline<T> upstream, long limit) {
        super(context, upstream.isOrdered(), upstream);
        this.limit = limit;
    }

    @Override
    public Vertex buildDAG(DAG dag) {
        Vertex previous = upstream.buildDAG(dag);
        // required final for lambda variable capture
        final long lim = limit;
        Vertex first = dag.newVertex(uniqueVertexName("limit-local"), () -> new LimitP(lim)).localParallelism(1);
        dag.edge(between(previous, first));

        if (upstream.isOrdered()) {
            return first;
        }

        Vertex second = dag.newVertex(uniqueVertexName("limit-distributed"), () -> new LimitP(lim)).localParallelism(1);
        dag.edge(between(first, second)
                .distributed()
                .allToOne()
        );

        return second;
    }
}
