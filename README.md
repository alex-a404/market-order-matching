## Order-matching engine
This is a Java implementation of an order matching engine. An order matching
engine handles bids/asks (buy/sell requests), matching 'best' buyers to 'best'
sellers on price levels, within the context of a financial market.
It exposes API (written with Spring Boot) endpoints that allow you to
send BUY/SELL requests for symbols, query their status and cancel them.

### Design
***Core service:***  
Price-time priority of matching (highest bid <-> lowest ask) is ensured by a
`TreeMap<Long,Deque<Order>>` where the key is the price level (sorted- price priority) and the
queue is FIFO (time priority). Every price level has at least one order.

***Handler domain:***  
`MatchingEngine` maintains order books for all symbols. Single-threaded thread.  
`EngineRunner` is the class that interacts with the single thread of the engine.

***API layer:***  
Spring Boot application. `EngineRunner` and `MatchingEngine` instantiated in EngineConfig.


### API Endpoints

### Concurrency handling

The benefit of this design is that there is no race condition on
the `Orderbook` and within the core logic. Therefore it doesn't have
to be thread-safe since there is only one thread that ever interacts with it,
the `EngineRunner`.

Other threads send `Command` records to `EngineRunner`, whose `.run()` method takes
them from the `BlockingQueue` and assigns the result of the actual operation to the
`CompletableFuture` of the command.