/* Licensed under the Apache License, Version 2.0 (the "License");
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
package org.camunda.tngp.client.event.impl.builder;

import org.agrona.DirectBuffer;
import org.camunda.tngp.client.event.Event;
import org.camunda.tngp.client.event.impl.dto.UnknownEvent;
import org.camunda.tngp.protocol.log.MessageHeaderDecoder;

public class UnknownEventBuilder implements EventBuilder
{
    protected final MessageHeaderDecoder messageHeaderDecoder = new MessageHeaderDecoder();

    @Override
    public Event build(long position, DirectBuffer buffer)
    {
        messageHeaderDecoder.wrap(buffer, 0);

        final UnknownEvent event = new UnknownEvent();

        event.setPosition(position);
        event.setRawBuffer(buffer);

        event.setTemplateId(messageHeaderDecoder.templateId());

        return event;
    }

}