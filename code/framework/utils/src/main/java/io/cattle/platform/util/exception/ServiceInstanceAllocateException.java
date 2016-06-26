package io.cattle.platform.util.exception;

import org.slf4j.Logger;

public class ServiceInstanceAllocateException extends IllegalStateException implements LoggableException {

    private static final long serialVersionUID = -5376205462062705074L;

    @Override
    public void log(Logger log) {
        log.info(this.getMessage());
    }

    public ServiceInstanceAllocateException(String s) {
        super(s);
    }

}
