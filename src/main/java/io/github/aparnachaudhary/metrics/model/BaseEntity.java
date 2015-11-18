package io.github.aparnachaudhary.metrics.model;

import java.util.Date;
import java.util.HashMap;

/**
 * @author aparna
 */
public abstract class BaseEntity extends HashMap {

    public void setName(String name) {
        put("name", name);
    }

    public void setTimestamp(Date date) {
        put("timestamp", date);
    }
}
