Design notes
============

#1) Usage examples
---- Build: mvn clean install

---- Execute microservice: 
java -jar target/lock-limiter-0.0.1-SNAPSHOT.jar
java -jar target/lock-limiter.jar
 
---- List all items and all processing queues (1 processing queue per item and dead letter queue):

curl --header "'Accept: application/json'" --noproxy localhost --silent --request GET http://localhost:8080/items

---- Create datacenter:

curl --header "Content-Type: application/json" --noproxy localhost --silent --request POST --data '[{"dataCenterId":"1", "itemType":"DATACENTER", "attachToServerId":"0", "action":"CREATE"}]' http://localhost:8080/requests

---- Receive fulfillment for the call above:

curl --header "Content-Type: application/json" --noproxy localhost --silent --request POST --data '{"dataCenterId":"1", "itemType":"DATACENTER", "attachToServerId":"0", "action":"CREATE", "created":""}' http://localhost:8080/fulfillment

---- Create 2 servers:

curl --header "Content-Type: application/json" --noproxy localhost --silent --request POST --data '[{"dataCenterId":"1", "itemType":"SERVER", "attachToServerId":"0", "action":"CREATE"},{"dataCenterId":"1", "itemType":"SERVER", "attachToServerId":"0", "action":"CREATE"}]' http://localhost:8080/requests

---- Create storage:

curl --header "Content-Type: application/json" --noproxy localhost --silent --request POST --data '[{"dataCenterId":"1", "itemType":"STORAGE", "attachToServerId":"0", "action":"CREATE"}]' http://localhost:8080/requests

#2) High level design (diagram lock-limiter.png) 
see diagram lock-limiter.png in the top level project directory

---- Entry point (boundary): LinkLimiterController as exemplified above providing 3 rest endpoints :

/requests: forwarding each Request in the array, one by one to RequestService.processRequest

/fulfillment: forwarding the fulfilled Request to RequestService.processRequestFulfilled

/items: method to get all Items and all processing queues (one for each received unique item / itemId) plus the dead letter queue

---- Service layer:
ItemService: API for Item CRUD (delegate to JPA Repository) and lock and attach Item(s)

RequestService: core component to handle the Request processing, creation of Item's processing queues, dispatch of the Item Request to the corresponding Item's Queue and the processing of the Queue of Requests.

---- Model:
Request and Item classes , latter marked as @Data and JPA @Entity. 
Note that, although it was suggested not to use database, the solution employs the in memory h2, as locking an item (and transitively all its dependencies tree) can be easily done in a JPA transaction. 
Otherwise some other type of transactional datastructures should have been employed to ensure referential integrity in the locking process (distributed maps etc, such as Redis, Apache Ignite, Hazelcase, Infinispan).  

The service provides Request ingestion processing backpressure .

#3) Configuration
The file src/ main/ resources/ application.properties provides the following

fulfillment.timeout.seconds=3 
	not used yet

fulfillment.url=http://fulfillemt:8080/request

item.requests.queue.size.max=10 
	maximum size of the BlockingProirityQueue' s to prevent against out of memory errors

lock.timeout.seconds=1 
	timeout to wait for completion of an operation on the Item's BlockingQueue (bounded by the max size above) thus providing inbound processing backpressure. 

#4) Java developer notes 
Lombok library annotations are used to reduce the amount of lines of code of Item and Request an to have ItemService delegate all CRUD calls to ItemRepository.

Parallelism / multi-threading is done thru the Tomcat application server execution thread pools. 
All REST HttpRequests are therefore processed in parallel without delay and all Requests on Items are dispatched to a corresponding BlockingQueue.

The processing halts unless the first element in the queue in not locked, in that case is locked and forwarded to the Fulfillment service.

The processing of the queue resumes only after  receiving a corresponding fulfillment Rest invocation, i.e. a Request for the Item was fulfilled therefore allowing the next Request in the Item's queue to be forwarded to Fulfillment service.

#5) Future extensions or improvements
Apply micro-services reliability patterns: circuit breaker and response timeout on the FulfillmentServiceClient.

Bulkheads: decouple the HTTP Request ingestion thread pool form the core processing pool by making @Async /@Asynchronous (all public methods in ) the RequestService.

Design a mechanism for automatic unlocking of Items whose lock has timed out in order to allow retry of processing in case Fulfillment service is unreliable.

Rewrite the micro-service with JEE 7 and Microservice profile constructs @Stateless @Singleton @CircuitBreaker @Timeout @Fallback etc.

If necessary, verify the opportunity of the solution performing correctly in clustered deployment and if so, possibly migrate the internal queues to distributed queues (Redis, Infinispan)

#6) Testing strategy

Provide bash script wrappers for the operations at point 1) and develop based on them a few use case scenarios.

Develop assertions and data invariants to be checked for the Requests/Items. 

Execute the scripts above in a high volume / high throughput condition injecting random errors.

Ensure data is consistent and the system can auto recover (no queues/memory overflows, processing resumes correctly and after 

#7) Final note
I have paired with 1&1 Domain Product team back in 2013 for the Domain Lifecycle when I implemented the Job Aquirer / Processor / Executor executor module that dealt with life cycle events processing of Domains , processing done by Jobs / Tasks.

Although as far as I can recall it was somehow more sophisticated (parallel no busy waits processing same as here, tasks correlated to a domain were locked and grouped in a batch job and processed in one DB trip), automatic unlocking of failed tasks, exponential backoff retries etc) the development effort was about 2 devs for 2 or 3 scrum sprints.