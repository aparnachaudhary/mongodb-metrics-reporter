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
public class MetricMeteredTest extends AbstractMetricTest {


    @Test
    public void reportsMeters() throws Exception {
        final Meter meter = mock(Meter.class);
        when(meter.getCount()).thenReturn(1L);
        when(meter.getOneMinuteRate()).thenReturn(2.0);
        when(meter.getFiveMinuteRate()).thenReturn(3.0);
        when(meter.getFifteenMinuteRate()).thenReturn(4.0);
        when(meter.getMeanRate()).thenReturn(5.0);

        reporter.report(this.<Gauge>map(), this.<Counter>map(), this.<Histogram>map(), this.<Meter>map("meter", meter), this.<Timer>map());

        MongoCollection mongoCollection = getMongoCollection("metered");
        assertEquals(1, mongoCollection.count());
        final Document value = (Document) mongoCollection.find().first();
        assertEquals("Does not save correct name", "junit.meter", value.get("name"));

        assertEquals("Does not have correct count", 1L, value.get("count"));
        assertEquals("Does not have correct one minute rate", 2.0, value.get("m1Rate"));
        assertEquals("Does not have correct five minute rate", 3.0, value.get("m5Rate"));
        assertEquals("Does not have correct fifteen minute rate", 4.0, value.get("m15Rate"));
        assertEquals("Does not have correct mean rate", 5.0, value.get("meanRate"));

    }

}
