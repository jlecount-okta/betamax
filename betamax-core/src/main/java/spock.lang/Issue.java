package spock.lang;

import java.lang.annotation.*;
/**
 * Indicates that a feature method or specification relates to one or more
 * issues in an external issue tracking system.
 *
 * @author Peter Niederwieser
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Issue {
    /**
     * The IDs of the issues that the annotated element relates to.
     *
     * @return the IDs of the issues that the annotated element relates to
     */
    String[] value();
}