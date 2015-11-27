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
public class MetricHistogramTest extends AbstractMetricTest {


    @Test
    public void reportsHistograms() throws Exception {
        final Histogram histogram = mock(Histogram.class);
        when(histogram.getCount()).thenReturn(1L);

        final Snapshot snapshot = mock(Snapshot.class);
        when(snapshot.getMax()).thenReturn(2L);
        when(snapshot.getMean()).thenReturn(3.0);
        when(snapshot.getMin()).thenReturn(4L);
        when(snapshot.getStdDev()).thenReturn(5.0);
        when(snapshot.getMedian()).thenReturn(6.0);
        when(snapshot.get75thPercentile()).thenReturn(7.0);
        when(snapshot.get95thPercentile()).thenReturn(8.0);
        when(snapshot.get98thPercentile()).thenReturn(9.0);
        when(snapshot.get99thPercentile()).thenReturn(10.0);
        when(snapshot.get999thPercentile()).thenReturn(11.0);

        when(histogram.getSnapshot()).thenReturn(snapshot);

        reporter.report(this.<Gauge>map(), this.<Counter>map(), this.<Histogram>map("histogram", histogram), this.<Meter>map(), this.<Timer>map());

        MongoCollection mongoCollection = getMongoCollection("histogram");
        assertEquals(1, mongoCollection.count());
        final Document value = (Document) mongoCollection.find().first();

        assertEquals("Does not save correct name", "junit.histogram", value.get("name"));

        assertEquals("Does not have correct count", 1L, value.get("count"));
        assertEquals("Does not have correct max", 2L, value.get("max"));
        assertEquals("Does not have correct mean", 3.0, value.get("mean"));
        assertEquals("Does not have correct min", 4L, value.get("min"));
        assertEquals("Does not have correct stdDev", 5.0, value.get("stdDev"));
        assertEquals("Does not have correct median", 6.0, value.get("median"));
        assertEquals("Does not have correct p75", 7.0, value.get("p75"));
        assertEquals("Does not have correct p95", 8.0, value.get("p95"));
        assertEquals("Does not have correct p98", 9.0, value.get("p98"));
        assertEquals("Does not have correct p99", 10.0, value.get("p99"));
        assertEquals("Does not have correct p999", 11.0, value.get("p999"));

    }

}
