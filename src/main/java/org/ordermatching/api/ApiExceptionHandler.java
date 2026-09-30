package org.ordermatching.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.concurrent.TimeoutException;


@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    // rejected by the engine: unknown symbol, invalid values, unsupported order type
    @ExceptionHandler({IllegalArgumentException.class, UnsupportedOperationException.class})
    ProblemDetail rejected(RuntimeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    ProblemDetail noAccount(MissingRequestHeaderException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Missing " + e.getHeaderName() + " header");
    }

    @ExceptionHandler(EngineBusyException.class)
    ProblemDetail busy(EngineBusyException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    }

    @ExceptionHandler(TimeoutException.class)
    ProblemDetail timeout(TimeoutException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "Matching engine did not respond in time");
    }
}
