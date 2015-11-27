package io.github.aparnachaudhary.metrics;

import com.codahale.metrics.*;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author aparna
 */
@RunWith(MockitoJUnitRunner.class)
public class MetricGaugeTest extends AbstractMetricTest {


    @Test
    public void reportStringGaugeValues() throws Exception {

        reporter.report(map("server.status", gauge("OK")), this.<Counter>map(), this.<Histogram>map(), this.<Meter>map(), this.<Timer>map());

        MongoCollection mongoCollection = getMongoCollection("gauge");
        assertEquals(1, mongoCollection.count());
        final Document value = (Document) mongoCollection.find().first();
        assertEquals("Does not save correct value", "OK", value.get("value"));
        assertEquals("Does not save correct name", "junit.server.status", value.get("name"));
    }

    @Test
    public void byteGaugeValuesNotSupported() throws Exception {
        reporter.report(map("gauge", gauge((byte) 1)), this.<Counter>map(), this.<Histogram>map(), this.<Meter>map(), this.<Timer>map());
        MongoCollection mongoCollection = getMongoCollection("gauge");
        assertEquals(1, mongoCollection.count());
        final Document value = (Document) mongoCollection.find().first();
        assertNotNull("Does not set value", value);
        assertNotEquals("Unexpected type", Byte.class, value.get("value").getClass());
        assertEquals("Does not save correct name", "junit.gauge", value.get("name"));
    }

    @Test
    public void shortGaugeValuesNotSupported() throws Exception {
        reporter.report(map("gauge", gauge((short) 1)), this.<Counter>map(), this.<Histogram>map(), this.<Meter>map(), this.<Timer>map());
        MongoCollection mongoCollection = getMongoCollection("gauge");
        assertEquals(1, mongoCollection.count());
        final Document value = (Document) mongoCollection.find().first();
        assertNotEquals("Unexpected type", Short.class, value.get("value").getClass());
        assertEquals("Does not save correct name", "junit.gauge", value.get("name"));
    }

    // TODO: Add tests for Integer, Float, Double, Long


    private <T> Gauge gauge(final T value) {
        final Gauge gauge = mock(Gauge.class);
        when(gauge.getValue()).thenReturn(value);
        return gauge;
    }
}
