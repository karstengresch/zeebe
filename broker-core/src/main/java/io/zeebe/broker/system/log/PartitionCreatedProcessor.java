/*
 * Zeebe Broker Core
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.zeebe.broker.system.log;

import org.agrona.DirectBuffer;

import io.zeebe.broker.logstreams.processor.TypedEvent;
import io.zeebe.broker.logstreams.processor.TypedEventProcessor;
import io.zeebe.broker.logstreams.processor.TypedResponseWriter;
import io.zeebe.broker.logstreams.processor.TypedStreamReader;
import io.zeebe.broker.logstreams.processor.TypedStreamWriter;

public class PartitionCreatedProcessor implements TypedEventProcessor<PartitionEvent>
{
    protected final TopicsIndex topics;
    protected final TypedStreamReader reader;

    protected TypedEvent<TopicEvent> createRequest;

    public PartitionCreatedProcessor(TopicsIndex topics, TypedStreamReader reader)
    {
        this.topics = topics;
        this.reader = reader;
    }

    @Override
    public void processEvent(TypedEvent<PartitionEvent> event)
    {
        final PartitionEvent value = event.getValue();

        final DirectBuffer topicName = value.getTopicName();
        topics.moveTo(topicName);

        final boolean topicCreationComplete = topics.getRemainingPartitions() == 1; // == 1 because this is the last partition

        if (topicCreationComplete)
        {
            createRequest = reader.readValue(topics.getRequestPosition(), TopicEvent.class);
            createRequest.getValue().setState(TopicState.CREATED);
        }
    }

    @Override
    public boolean executeSideEffects(TypedEvent<PartitionEvent> event, TypedResponseWriter responseWriter)
    {
        if (createRequest != null)
        {
            return responseWriter.write(createRequest);
        }
        else
        {
            return true;
        }
    }

    @Override
    public long writeEvent(TypedEvent<PartitionEvent> event, TypedStreamWriter writer)
    {
        if (createRequest != null)
        {
            return writer.writeFollowupEvent(createRequest.getKey(), createRequest.getValue());
        }
        else
        {
            return 0;
        }
    }

    @Override
    public void updateState(TypedEvent<PartitionEvent> event)
    {
        final DirectBuffer topicName = event.getValue().getTopicName();

        topics.moveTo(topicName);
        final int remainingPartitions = topics.getRemainingPartitions();

        if (remainingPartitions > 0)
        {
            topics.putRemainingPartitions(topicName, remainingPartitions - 1);
        }
    }
}
