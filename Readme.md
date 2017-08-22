BetterBank - Welcome!
==========
##### by Chun Yin Tan

# Description
This is a RESTful API project providing two endpoints: 1) POST: /transactions and 2) GET /statistics (actual usage see below).

Basically, the goal of the project is to make the "GET /statistics" endpoint to return the statistics of the transations of the last 60 seconds, which is inserted by "POST: /transactions"

The project has the following requirements:
1. No database of any kind is allowed. Therefore to store the results locally, a Singleton of the REST resource class is used. (Same resource instance of for all HTTP requests, instead of per resource instance per request as default)
2. O(1) memory and runtime for both endpoints
3. Allowing a delayed transaction to be recorded, as long as it is within last 60 seconds

Technologies used: Java EE/Maven/Jersey/JUnit deployed in Apache Tomcat with Eclipse IDE
# Build and Run
The project utilises Maven. Therefore, please run
```sh
$ mvn clean install
```
The project should then resolve dependencies, build and pass all the test cases. Then you can deploy the `.war` file or else for your testing. Alternatively, you can import this project to any IDEs and run the predefined tests.

The resulting APIs can be accessed via 
1. HTTP POST: http://localhost:8080/BetterBank/rest/api/transactions with JSON body 
```json
{   "amount": 10.0, 
    "timestamp":1510044509228 
}
```
This returns empty body with **201 (successful)**/**204 (expired transaction)**.

2. HTTP GET: http://localhost:8080/BetterBank/rest/api/statistics 
```json
{ "sum": 100.0, "avg": 100.0, "max": 200.0, "min": 50.0, "count": 10}
```
 returns **200 (successful)** along with a JSON response
 
Otherwise it returns **404 (no data)**.

## Main Issues (For reference only)

**a) [O(1)](#a-o1)** 

**b) [Concurrency](#b-concurrency)**

**c) [Time Discrepancy](#c-time-discrepancy)**

### a) O(1)
The intuition of this idea is to aggregate the transaction data for each given millisecond, which is the smallest unit of time, making sure there is no los of time precison by rounding. 

To address *O(1)* requirement, I have decided to use HashMap in the form of `HashMap<Long, TransactionsPerMillisecond>`, which stores mapping from key **timestamp** to value of a custom class **TransactionsPerMillisecond**. TransactionsPerMillisecond, containing member variables of *count*, *sum*, *avg*, *max*, *min*, is to used to store the aggregated information of all transactions happening at that exact timestamp.


**Runtime Analysis for _POST /transactions_**
During insertion of a transaction, the program **1)** checks if the hashmap already contains data for the exact same timestamp, **2a)** if yes then aggregrate the current amount to the existing data, **2b)** if no then create a new object of TransactionsPerMillisecond initialised current amount data.  

These operations, i.e. `hashMap.containsKey(key)`/ `hashMap.get(key)`/ `hashMap.push(key, value)`, are bounded by *O(1)*, based on the properties of HashMap.

**Runtime Analysis for _GET /statistics_**
In order to get the statistics for the past 60 seconds, we only need to find the transactions happened for the past 60 seconds = 60000 milliseconds. Let current timestamp be *t*, for each millisecond, we search for an entry in the HashMap for key = *t-0*, *t-1*, *t-2*, ..., *t-59999*. 

In total, in terms of runtime, we run the iteration for 60000 times, which is already both upper- and lower-bounded. Therefore it is independent of the number of transactions, i.e. *O(1)*.

**Memory Analysis**
In terms of memory, the only storage of this program is the HashMap. To prevent it from growing infinitely with the number of transactions, there is a scheduled task to clean up the expired data every two minutes. Therefore, at any given point in time, at most there are 2 minutes of transaction data stored in the hashmap, which is at most 2*60000 entries, upper-bounded.

The clean-up is scheduled as soon as the first data is put into the hashmap; and if later the clean-up did remove all entries in the hashmap, i.e. all data are expired, it will stop the schedule until a new data is put to the hashmap again. The choice of 2 minutes acts as a buffer to allow other unfinished requests have a chance to get the old data.  

### b) Concurrency 
Since no databases of any kind is allowed, including in-memory databases, the only possible way to store data would be to make the API endpoint class a singleton, in order to keep its member variable alive for as long as the server is online. And as mentioned, this program uses HashMap to store the data. Therefore the concurrency issue lies on the usage of HashMap. Based on the propoerties, I have decided to use **ConcurrentHashMap**, in which, according to [Java documentation], "all operations are thread-safe". 

Another potential race condition may occur during insertion, while we check for the existence of a entry with certain timestamp. For example, if two threads are trying to insert a transaction with the same timestamp, which does not exist yet in the HashMap, there is a chance that they both found that the HashMap does not contain the entry and both threads try to make a new entry. In this case, the entry of the earlier thread would be covered by the entry of the latter thread, because `hashMap.push(key, value)` would replace the older entry with the same key. This leads to inaccuracy and loss of data. In view of that, ReentrantLock is implemented in order to make sure that portion of code is thread-safe too.

### c) Time Discrepancy
Any newly arrived data can always be put to the HashMap with *O(1)* runtime, by `hashMap.out(key, value)`.
Another potential race condition may occur during insertion, while we check for the existence of a entry with certain timestamp. For example, if two threads are trying to insert a transaction with the same timestamp, which does not exist yet in the HashMap, there is a chance that they both found that the HashMap does not contain the entry and both threads try to make a new entry. In this case, the entry of the earlier thread would be covered by the entry of the latter thread, because `hashMap.push(key, value)` would replace the older entry with the same key. This leads to inaccuracy and loss of data. In view of that, ReentrantLock is implemented in order to make sure that portion of code is thread-safe too.

## Remarks
1. Double is used for the “amount”. But because of the potential precision problem of double, BigDecimal could be preferred.
2. Three JUnit test classes have been implemented. However OneMinuteStatisticsRestTest is commmented out, since it generates 1 transaction per second for 60 seconds to test the result.

[Java documentation]: https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ConcurrentHashMap.html
