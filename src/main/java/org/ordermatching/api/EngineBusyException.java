package org.ordermatching.api;

// the engine's queue is full or the engine is stopped
class EngineBusyException extends RuntimeException {
    EngineBusyException() { super("Matching engine is not accepting commands"); }
}
