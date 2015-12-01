package io.github.aparnachaudhary.metrics.model;

import com.codahale.metrics.Snapshot;

/**
 * @author aparna
 */
public class HistogramEntity extends BaseEntity {

    private Long count;
    private Long max;
    private Double mean;
    private Long min;
    private Double stdDev;
    private Double median;
    private Double p75;
    private Double p95;
    private Double p98;
    private Double p99;
    private Double p999;

    public HistogramEntity(final Snapshot snapshot) {
        max = snapshot.getMax();
        mean = snapshot.getMean();
        min = snapshot.getMin();
        stdDev = snapshot.getStdDev();
        median = snapshot.getMedian();
        p75 = snapshot.get75thPercentile();
        p95 = snapshot.get95thPercentile();
        p98 = snapshot.get98thPercentile();
        p99 = snapshot.get99thPercentile();
        p999 = snapshot.get999thPercentile();
        put("max", max);
        put("mean", mean);
        put("min", min);
        put("stdDev", stdDev);
        put("median", median);
        put("p75", p75);
        put("p95", p95);
        put("p98", p98);
        put("p99", p99);
        put("p999", p999);

    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
        put("count", count);
    }

    public Long getMax() {
        return max;
    }

    public void setMax(Long max) {
        this.max = max;
        put("max", max);
    }

    public Double getMean() {
        return mean;
    }

    public void setMean(Double mean) {
        this.mean = mean;
        put("mean", mean);
    }

    public Long getMin() {
        return min;
    }

    public void setMin(Long min) {
        this.min = min;
        put("min", min);
    }

    public Double getStdDev() {
        return stdDev;
    }

    public void setStdDev(Double stdDev) {
        this.stdDev = stdDev;
        put("stdDev", stdDev);
    }

    public Double getMedian() {
        return median;
    }

    public void setMedian(Double median) {
        this.median = median;
        put("median", median);
    }

    public Double getP75() {
        return p75;
    }

    public void setP75(Double p75) {
        this.p75 = p75;
        put("p75", p75);
    }

    public Double getP95() {
        return p95;
    }

    public void setP95(Double p95) {
        this.p95 = p95;
        put("p95", p95);
    }

    public Double getP98() {
        return p98;
    }

    public void setP98(Double p98) {
        this.p98 = p98;
        put("p98", p98);
    }

    public Double getP99() {
        return p99;
    }

    public void setP99(Double p99) {
        this.p99 = p99;
        put("p99", p99);
    }

    public Double getP999() {
        return p999;
    }

    public void setP999(Double p999) {
        this.p999 = p999;
        put("p999", p999);
    }
}
