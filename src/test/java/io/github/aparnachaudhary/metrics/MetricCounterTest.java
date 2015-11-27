package io.github.aparnachaudhary.metrics;

import com.codahale.metrics.*;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author aparna
 */
@RunWith(MockitoJUnitRunner.class)
public class MetricCounterTest extends AbstractMetricTest {

    @Test
    public void reportsCounters() throws Exception {
        final Counter counter = mock(Counter.class);
        when(counter.getCount()).thenReturn(100L);

        reporter.report(this.<Gauge>map(), this.<Counter>map("counter", counter), this.<Histogram>map(), this.<Meter>map(), this.<Timer>map());

        MongoCollection mongoCollection = getMongoCollection("counter");
        assertEquals(1, mongoCollection.count());

        final Document value = (Document) mongoCollection.find().first();
        assertEquals("Does not save correct name", "junit.counter", value.get("name"));
        assertEquals("Does not save correct count", 100L, value.get("count"));
    }

}
