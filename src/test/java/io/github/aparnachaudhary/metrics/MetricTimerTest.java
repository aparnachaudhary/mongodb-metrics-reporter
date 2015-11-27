package io.github.aparnachaudhary.metrics;

import com.codahale.metrics.*;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author aparna
 */
@RunWith(MockitoJUnitRunner.class)
public class MetricTimerTest extends AbstractMetricTest {

    @Test
    public void reportsTimers() throws Exception {
        final Timer timer = mock(Timer.class);
        when(timer.getCount()).thenReturn(1L);
        when(timer.getMeanRate()).thenReturn(2.0);
        when(timer.getOneMinuteRate()).thenReturn(3.0);
        when(timer.getFiveMinuteRate()).thenReturn(4.0);
        when(timer.getFifteenMinuteRate()).thenReturn(5.0);

        final Snapshot snapshot = mock(Snapshot.class);
        when(snapshot.getMax()).thenReturn(TimeUnit.MILLISECONDS.toNanos(100));
        when(snapshot.getMean()).thenReturn((double) TimeUnit.MILLISECONDS.toNanos(200));
        when(snapshot.getMin()).thenReturn(TimeUnit.MILLISECONDS.toNanos(300));
        when(snapshot.getStdDev()).thenReturn((double) TimeUnit.MILLISECONDS.toNanos(400));
        when(snapshot.getMedian()).thenReturn((double) TimeUnit.MILLISECONDS.toNanos(500));
        when(snapshot.get75thPercentile()).thenReturn((double) TimeUnit.MILLISECONDS.toNanos(600));
        when(snapshot.get95thPercentile()).thenReturn((double) TimeUnit.MILLISECONDS.toNanos(700));
        when(snapshot.get98thPercentile()).thenReturn((double) TimeUnit.MILLISECONDS.toNanos(800));
        when(snapshot.get99thPercentile()).thenReturn((double) TimeUnit.MILLISECONDS.toNanos(900));
        when(snapshot.get999thPercentile()).thenReturn((double) TimeUnit.MILLISECONDS.toNanos(1000));

        when(timer.getSnapshot()).thenReturn(snapshot);

        reporter.report(this.<Gauge>map(), this.<Counter>map(), this.<Histogram>map(), this.<Meter>map(), map("timer", timer));

        MongoCollection mongoCollection = getMongoCollection("timer");
        assertEquals(1, mongoCollection.count());
        final Document value = (Document) mongoCollection.find().first();


        assertEquals("Does not save correct name", "junit.timer", value.get("name"));

        assertEquals("Does not have correct count", 1L, value.get("count"));
        assertEquals("Does not have correct one minute rate", 3.0, value.get("m1Rate"));
        assertEquals("Does not have correct five minute rate", 4.0, value.get("m5Rate"));
        assertEquals("Does not have correct fifteen minute rate", 5.0, value.get("m15Rate"));
        assertEquals("Does not have correct mean rate", 2.0, value.get("meanRate"));

        final Document reportedSnaphot = (Document) value.get("snapshot");

        assertEquals("Does not have correct max", 100000000L, reportedSnaphot.get("max"));
        assertEquals("Does not have correct mean", 2.0E8, reportedSnaphot.get("mean"));
        assertEquals("Does not have correct min", 300000000L, reportedSnaphot.get("min"));
        assertEquals("Does not have correct stdDev", 4.0E8, reportedSnaphot.get("stdDev"));
        assertEquals("Does not have correct median", 5.0E8, reportedSnaphot.get("median"));
        assertEquals("Does not have correct p75", 6.0E8, reportedSnaphot.get("p75"));
        assertEquals("Does not have correct p95", 7.0E8, reportedSnaphot.get("p95"));
        assertEquals("Does not have correct p98", 8.0E8, reportedSnaphot.get("p98"));
        assertEquals("Does not have correct p99", 9.0E8, reportedSnaphot.get("p99"));
        assertEquals("Does not have correct p999", 1.0E9, reportedSnaphot.get("p999"));

    }

}
