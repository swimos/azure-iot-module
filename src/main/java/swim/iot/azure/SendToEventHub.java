package swim.iot.azure;

import com.azure.messaging.eventhubs.EventData;
import com.azure.messaging.eventhubs.EventDataBatch;
import com.azure.messaging.eventhubs.EventHubClientBuilder;
import com.azure.messaging.eventhubs.EventHubProducerClient;
import swim.iot.util.EnvConfig;

import java.util.Arrays;
import java.util.List;

public class SendToEventHub {

  /**
   * Code sample for publishing events.
   * @throws IllegalArgumentException if the event data is bigger than max batch size.
   */
  public static void publishEvents(String content) {
    // create a producer client
    EventHubProducerClient producer = new EventHubClientBuilder()
        .connectionString(EnvConfig.EVENT_HUB_CONNSTRING, EnvConfig.EVENT_HUB_NAME)
        .buildProducerClient();

    // sample events in an array
    List<EventData> allEvents = Arrays.asList(new EventData(content));

    // create a batch
    EventDataBatch eventDataBatch = producer.createBatch();

    for (EventData eventData : allEvents) {
      // try to add the event from the array to the batch
      if (!eventDataBatch.tryAdd(eventData)) {
        // if the batch is full, send it and then create a new batch
        producer.send(eventDataBatch);
        eventDataBatch = producer.createBatch();

        // Try to add that event that couldn't fit before.
        if (!eventDataBatch.tryAdd(eventData)) {
          throw new IllegalArgumentException("Event is too large for an empty batch. Max size: "
              + eventDataBatch.getMaxSizeInBytes());
        }
      }
    }
    // send the last batch of remaining events
    if (eventDataBatch.getCount() > 0) {
      producer.send(eventDataBatch);
    }
    producer.close();
  }
}
