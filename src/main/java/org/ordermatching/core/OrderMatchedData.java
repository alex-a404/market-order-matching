package org.ordermatching.core;

/**
 * @param matched
 * @param incomingOrderID
 * @param openOrderID ID of order matched against
 * @param quantity
 * @param price
 */
public record OrderMatchedData(boolean matched, long incomingOrderID, long openOrderID, long quantity, long price) {}
