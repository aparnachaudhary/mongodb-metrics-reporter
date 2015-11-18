package io.github.aparnachaudhary.metrics.model;

/**
 * Created by aparna on 11/17/15.
 */
public class CounterEntity extends BaseEntity {

    private Object count;

    public Object getCount() {
        return count;
    }

    public void setCount(final Object count) {
        this.count = count;
        put("count", count);
    }
}
