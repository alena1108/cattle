package io.cattle.platform.eventing.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import java.util.Date;

public interface Event {

    String TRANSITIONING_YES = "yes";
    String TRANSITIONING_NO = "no";
    String TRANSITIONING_ERROR = "error";

    String REPLY_PREFIX = "reply.";
    String REPLY_SUFFIX = ".reply";

    String getId();

    String getName();

    @JsonInclude(Include.NON_EMPTY)
    String getReplyTo();

    String getResourceId();

    String getResourceType();

    @JsonInclude(Include.NON_EMPTY)
    String[] getPreviousIds();

    @JsonInclude(Include.NON_EMPTY)
    String[] getPreviousNames();

    @JsonInclude(Include.NON_EMPTY)
    String getTransitioning();

    @JsonInclude(Include.NON_EMPTY)
    Integer getTransitioningProgress();

    @JsonInclude(Include.NON_EMPTY)
    String getTransitioningMessage();

    Object getData();

    @JsonInclude(Include.NON_EMPTY)
    Date getTime();

    @JsonInclude(Include.NON_EMPTY)
    Long getTimeoutMillis();
}
