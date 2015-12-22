package io.github.aparnachaudhary.metrics;

import com.codahale.metrics.Clock;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metered;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.MongoException;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.github.aparnachaudhary.metrics.model.BaseEntity;
import io.github.aparnachaudhary.metrics.model.CounterEntity;
import io.github.aparnachaudhary.metrics.model.GaugeEntity;
import io.github.aparnachaudhary.metrics.model.HistogramEntity;
import io.github.aparnachaudhary.metrics.model.MeteredEntity;
import io.github.aparnachaudhary.metrics.model.TimerEntity;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * A reporter to publish metric values to a MongoDB server.
 *
 * @author aparna
 */
public class MongoDBReporter extends ScheduledReporter {

    /**
     * Returns a new {@link Builder} for {@link MongoDBReporter}.
     *
     * @param registry the registry to report
     * @return a {@link Builder} instance for a {@link MongoDBReporter}
     */
    public static Builder forRegistry(MetricRegistry registry) {
        return new Builder(registry);
    }

    /**
     * A builder for {@link MongoDBReporter} instances. Defaults to not using a prefix, using the default clock, converting rates to
     * events/second, converting durations to milliseconds, and not filtering metrics. The default MongoDB database used to store metrics is
     * 'metricstore'.
     */
    public static class Builder {
        private final MetricRegistry registry;
        private Clock clock;
        private String prefix;
        private TimeUnit rateUnit;
        private TimeUnit durationUnit;
        private MetricFilter filter;
        private ServerAddress[] serverAddresses = new ServerAddress[] { new ServerAddress("localhost", 27017) };
        private MongoCredential[] mongoCredentials;
        private MongoClientOptions mongoClientOptions;
        private String databaseName = "metricstore";
        private Map<String, Object> additionalFields;

        private Builder(MetricRegistry registry) {
            this.registry = registry;
            this.clock = Clock.defaultClock();
            this.prefix = null;
            this.rateUnit = TimeUnit.SECONDS;
            this.durationUnit = TimeUnit.MILLISECONDS;
            this.filter = MetricFilter.ALL;
            this.mongoClientOptions = new MongoClientOptions.Builder().build();
        }

        /**
         * Use the given {@link Clock} instance for the time. Usually the default clock is sufficient.
         *
         * @param clock clock
         * @return {@code this}
         */
        public Builder withClock(Clock clock) {
            this.clock = clock;
            return this;
        }

        /**
         * Configure a prefix for each metric name. Optional, but useful to identify originator of metric.
         *
         * @param prefix prefix for metric name
         * @return {@code this}
         */
        public Builder prefixedWith(String prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * Convert all the rates to a certain TimeUnit, defaults to TimeUnit.SECONDS.
         *
         * @param rateUnit unit of rate
         * @return {@code this}
         */
        public Builder convertRatesTo(TimeUnit rateUnit) {
            this.rateUnit = rateUnit;
            return this;
        }

        /**
         * Convert all the durations to a certain TimeUnit, defaults to TimeUnit.MILLISECONDS
         *
         * @param durationUnit unit of duration
         * @return {@code this}
         */
        public Builder convertDurationsTo(TimeUnit durationUnit) {
            this.durationUnit = durationUnit;
            return this;
        }

        /**
         * Allows to configure a special MetricFilter, which defines what metrics are reported
         *
         * @param filter metrics filter
         * @return {@code this}
         */
        public Builder filter(MetricFilter filter) {
            this.filter = filter;
            return this;
        }

        /**
         * The database to write the metrics data. Defaults to 'metricstore'.
         *
         * @param databaseName mongodb database name
         * @return {@code this}
         */
        public Builder withDatabaseName(String databaseName) {
            this.databaseName = databaseName;
            return this;
        }

        /**
         * The connection details of MongoDB server.
         *
         * @param serverAddresses server addresses; defaults to localhost:27017.
         * @return {@code this}
         */
        public Builder serverAddresses(ServerAddress[] serverAddresses) {
            this.serverAddresses = serverAddresses;
            return this;
        }

        /**
         * MongoDB connection options
         *
         * @param mongoClientOptions MongoDB connection options.
         * @return {@code this}
         */
        public Builder mongoClientOptions(MongoClientOptions mongoClientOptions) {
            this.mongoClientOptions = mongoClientOptions;
            return this;
        }

        /**
         * Credentials to connect to MongoDB database.
         *
         * @param mongoCredentials credentials
         * @return {@code this}
         */
        public Builder mongoCredentials(MongoCredential[] mongoCredentials) {
            this.mongoCredentials = mongoCredentials;
            return this;
        }

        /**
         * Additional fields to be included for each metric
         *
         * @param additionalFields custom fields for reporting
         * @return {@code this}
         */
        public Builder additionalFields(Map<String, Object> additionalFields) {
            this.additionalFields = additionalFields;
            return this;
        }

        /**
         * Builds a {@link MongoDBReporter} with the given properties.
         *
         * @return a {@link MongoDBReporter}
         */
        public MongoDBReporter build() {
            return new MongoDBReporter(registry,
                    databaseName,
                    serverAddresses,
                    mongoCredentials,
                    mongoClientOptions,
                    clock,
                    prefix,
                    rateUnit,
                    durationUnit,
                    filter,
                    additionalFields);
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoDBReporter.class);

    private final ServerAddress[] serverAddresses;
    private final MongoCredential[] mongoCredentials;
    private final MongoClientOptions mongoClientOptions;
    private final String databaseName;
    private final Clock clock;
    private final String prefix;
    private final MongoDatabase mongoDatabase;
    private Map<String, Object> additionalFields;

    public MongoDBReporter(MetricRegistry registry, String databaseName, ServerAddress[] serverAddresses,
            MongoCredential[] mongoCredentials, MongoClientOptions mongoClientOptions,
            Clock clock, String prefix, TimeUnit rateUnit, TimeUnit durationUnit,
            MetricFilter filter, Map<String, Object> additionalFields) {
        super(registry, "mongodb-reporter", filter, rateUnit, durationUnit);
        this.databaseName = databaseName;
        this.serverAddresses = serverAddresses;
        this.mongoCredentials = mongoCredentials;
        this.mongoClientOptions = mongoClientOptions;
        this.clock = clock;
        this.prefix = prefix;
        this.additionalFields = additionalFields;
        this.mongoDatabase = getDB();
    }

    @Override
    public void report(SortedMap<String, Gauge> gauges,
            SortedMap<String, Counter> counters,
            SortedMap<String, Histogram> histograms,
            SortedMap<String, Meter> meters,
            SortedMap<String, Timer> timers) {

        // nothing to do if we don't have any metrics to report
        if (gauges.isEmpty() && counters.isEmpty() && histograms.isEmpty() && meters.isEmpty() && timers.isEmpty()) {
            LOGGER.info("All metrics empty, nothing to report");
            return;
        }

        final Date reportingTime = Date.from(ZonedDateTime.now(ZoneOffset.UTC).toInstant());

        for (Map.Entry<String, Gauge> entry : gauges.entrySet()) {
            if (entry.getValue().getValue() != null) {
                reportGauge(entry.getKey(), entry.getValue(), reportingTime);
            }
        }

        for (Map.Entry<String, Counter> entry : counters.entrySet()) {
            reportCounter(entry.getKey(), entry.getValue(), reportingTime);
        }

        for (Map.Entry<String, Histogram> entry : histograms.entrySet()) {
            reportHistogram(entry.getKey(), entry.getValue(), reportingTime);
        }

        for (Map.Entry<String, Meter> entry : meters.entrySet()) {
            reportMetered(entry.getKey(), entry.getValue(), reportingTime);
        }

        for (Map.Entry<String, Timer> entry : timers.entrySet()) {
            reportTimer(entry.getKey(), entry.getValue(), reportingTime);
        }

    }

    private void reportGauge(final String name, final Gauge gauge, final Date timestamp) {
        final MongoCollection coll = mongoDatabase.getCollection("gauge");
        final Object value = gauge.getValue();

        final GaugeEntity entity = new GaugeEntity();
        entity.setName(name(prefix, name));
        entity.setTimestamp(timestamp);
        entity.setValue(value);

        try {
            storeInMongo(coll, entity);
        } catch (MongoException e) {
            LOGGER.warn("Unable to report gauge {}", name, e);
        }
    }

    private void reportCounter(final String name, final Counter counter, final Date timestamp) {
        final MongoCollection coll = mongoDatabase.getCollection("counter");

        final CounterEntity entity = new CounterEntity();
        entity.setName(name(prefix, name));
        entity.setCount(counter.getCount());
        entity.setTimestamp(timestamp);

        try {
            storeInMongo(coll, entity);
        } catch (MongoException e) {
            LOGGER.warn("Unable to report counter {}", name, e);
        }
    }

    private void reportHistogram(final String name, final Histogram histogram, final Date timestamp) {
        final MongoCollection coll = mongoDatabase.getCollection("histogram");

        final Snapshot snapshot = histogram.getSnapshot();
        final HistogramEntity entity = new HistogramEntity(snapshot);
        entity.setName(name(prefix, name));
        entity.setCount(histogram.getCount());
        entity.setTimestamp(timestamp);

        try {
            storeInMongo(coll, entity);
        } catch (MongoException e) {
            LOGGER.warn("Unable to report histogram {}", name, e);
        }
    }

    private void reportMetered(final String name, final Metered meter, final Date timestamp) {
        final MongoCollection coll = mongoDatabase.getCollection("metered");
        final MeteredEntity entity = new MeteredEntity(meter);
        entity.setName(name(prefix, name));
        entity.setTimestamp(timestamp);
        try {
            storeInMongo(coll, entity);
        } catch (MongoException e) {
            LOGGER.warn("Unable to report meter {}", name, e);
        }
    }

    private void reportTimer(final String name, final Timer timer, final Date timestamp) {
        final MongoCollection coll = mongoDatabase.getCollection("timer");
        final TimerEntity entity = new TimerEntity(timer);
        entity.setName(name(prefix, name));
        entity.setTimestamp(timestamp);

        try {
            storeInMongo(coll, entity);
        } catch (MongoException e) {
            LOGGER.warn("Unable to report timer {}", name, e);
        }
    }

    private void storeInMongo(MongoCollection coll, BaseEntity entity) {
        final Document document = new Document(entity);

        if (additionalFields != null) {
            for (final Map.Entry<String, Object> field : additionalFields.entrySet()) {
                document.putIfAbsent(field.getKey(), field.getValue());
            }
        }
        coll.insertOne(document);
    }

    private MongoDatabase getDB() {
        MongoClient mongoClient = null;
        if (mongoCredentials == null) {
            mongoClient = new MongoClient(Arrays.asList(serverAddresses), mongoClientOptions);
        } else {
            mongoClient = new MongoClient(Arrays.asList(serverAddresses), Arrays.asList(mongoCredentials), mongoClientOptions);
        }
        return mongoClient.getDatabase(databaseName);
    }
}
