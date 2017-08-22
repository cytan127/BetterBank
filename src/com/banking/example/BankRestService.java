package com.banking.example;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.json.JSONException;
import org.json.JSONObject;


@Singleton
@Path("/api")
@Produces("application/json")
public class BankRestService {
	private int visitedTimes = 0;
	private boolean isCleanUpScheduled = false;
	
	private final ReentrantLock lock = new ReentrantLock(true);

	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private final Runnable cleanUpRunnable = new Runnable() { public void run() { clearOldTransactions(); } };
	private ScheduledFuture<?> scheduledFuture;

	private final Map<Long, TransactionsPerMillisecond> hashMap = new ConcurrentHashMap<Long, TransactionsPerMillisecond>();
	
	/**
	 * API endpoint for inserting transactions
	 * @param data - String containing JSON representation of the transaction
	 * @return 201, if succeed
	 * 		   204, if timestamp is expired
	 * 		   400, if invalid data
	 * @throws JSONException
	 */
	@POST
	@Path("/transactions")
	@Produces("application/json")
	public Response postTransaction(final String data) throws JSONException {
		long currentTimeStamp = System.currentTimeMillis();
		
		JSONObject dataObject = new JSONObject(data);
		try {
			Double amount  = dataObject.getDouble("amount");
			Long timestamp = dataObject.getLong("timestamp");
			//System.out.println("POST Received: amount:"+amount+" timestamp:"+timestamp);
			long differenceInSeconds = (currentTimeStamp - timestamp)/1000;
			if(differenceInSeconds > 60) {
				return Response.status(204).entity(new JSONObject().toString()).build();
			}
			else {
				  lock.lock();
				  try {
					  if(hashMap.containsKey(timestamp)) {
							TransactionsPerMillisecond transactions = hashMap.get(timestamp);
							transactions.count++;
							transactions.sum += amount;
							transactions.avg = transactions.sum/transactions.count;
							transactions.max = transactions.max>amount? transactions.max:amount;
							transactions.min = transactions.min<amount? transactions.min:amount;
						}
						else {
							hashMap.put(timestamp, new TransactionsPerMillisecond(amount));
						}
				  } finally {
					  lock.unlock();
				  }
				  
				  if(!isCleanUpScheduled) {
					  //System.out.println("CleanUp scheduled");
					  scheduledFuture = scheduler.scheduleAtFixedRate(cleanUpRunnable, 120, 120, TimeUnit.SECONDS);
					  isCleanUpScheduled = true;
				  }
			}
		}
		catch(JSONException e){
			JSONObject result = new JSONObject();
			result.put("error", 400);
			result.put("description", e.getMessage());
			 return Response.status(400).entity(result.toString()).build();
		}
		
		return Response.status(201).build();
	 }
	/**
	 * API endpoint for getting all statistics of transactions from past 60 seconds
	 * @return 200, with a JSON body containing {sum, avg, max, min, count} if succeed
	 * 		   404, if there is no data for the past 60 seconds
	 * 
	 */
	@GET
	@Path("/statistics") 
	@Produces("application/json")
	public Response getStatistics() {
		long currentTimeStamp = System.currentTimeMillis();
		long count = 0;
		double sum = 0.0;
		double avg = 0.0;
		double max = -1;
		double min = Double.MAX_VALUE;
		
		//search transaction data for the last 60000 milliseconds 
		for(long i=currentTimeStamp;i>currentTimeStamp-60000;i--) {
			if(hashMap.containsKey(i)) {
				TransactionsPerMillisecond transactions = hashMap.get(i);
				count += transactions.count;
				sum += transactions.sum;
				max = transactions.max>max? transactions.max:max;
				min = transactions.min<min? transactions.min:min;
			}
		}
		 
		// if there is no data for the past 60 seconds, we return 404
		if(count==0) {
			JSONObject result = new JSONObject();
			result.put("error", 404);
			result.put("description", "There is no transaction data for the past 60 seconds.");
			return Response.status(404).entity(result.toString()).build();
		}
		//otherwise, we calculate the average and then parse the statistics accordingly
		avg = sum/count;
		
		//Make sure that the format looks good, as precise as possible but without trailing zeros and without scientific notations
		DecimalFormat df = new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
		df.setMaximumFractionDigits(340); 
		
		//Could have used JSONObject, but since it does not respect input order
		String jsonString = "{ "+
						"\"sum\": "+ df.format(sum) + ", " +
						"\"avg\": "+ df.format(avg) + ", " +
						"\"max\": "+ df.format(max) + ", " +
						"\"min\": "+ df.format(min)+ ", " +
						"\"count\": "+ count +
						" }";
		return Response.status(200).entity(jsonString).build();
		
	}
	/**
	 * Debugging purpose only
	 * This method outputs each TransactionsPerMillisecond with the last 60 seconds
	 * 
	 */
	public void outputAllTransactionsWithinLast60Seconds() {
		long currentTimeStamp = System.currentTimeMillis();
		long startTime = System.nanoTime();
		long count = 0;
		double sum = 0.0;
		double avg = 0.0;
		double max = -1;
		double min = Double.MAX_VALUE;
		for(long i=currentTimeStamp;i>currentTimeStamp-60000;i--) {
			
			if(hashMap.containsKey(i)) {
				TransactionsPerMillisecond transactions = hashMap.get(i);
				count += transactions.count;
				sum += transactions.sum;
				max = transactions.max>max? transactions.max:max;
				min = transactions.min<min? transactions.min:min;
				System.out.println("T:"+i+" count:"+transactions.count+" avg: "+transactions.avg+
						" sum: "+transactions.sum+" max: "+transactions.max+" min: "+transactions.min);
			}
		}
		long endTime = System.nanoTime();
		avg = sum/count;
		System.out.println("time taken in nano seconds to finish the method: " + (endTime-startTime)); 
		System.out.println("------------------------------------------------");
		System.out.println("In total,  count:"+count+" avg: "+avg+ " sum: "+sum+" max: "+max+" min: "+min);
		System.out.println("------------------------------------------------");
	}

	/**
	 * Debugging purposes only.
	 * This method checks if this class is singleton.
	 */
	@GET
	@Path("/single") 
	public Response testSingleton() {
		String output = "Count ="+visitedTimes++;
		return Response.status(200).entity(output).build();
	}
	
	/**
	 * This method is to clean up data in-between the 2nd minute ago to the 3th minute ago (compared to the current timestamp)
	 * 
	 */
	private void clearOldTransactions() {
		//System.out.println("Cleaning started...");
		long currentTimeStamp = System.currentTimeMillis();
		long deleteFromTimeStamp = currentTimeStamp - 120*1000;
		long deleteToTimeStamp = deleteFromTimeStamp - 60*1000;
		for(;deleteToTimeStamp<deleteFromTimeStamp;deleteToTimeStamp++) {
			//System.out.println("Checking for "+deleteToTimeStamp);
			hashMap.remove(deleteToTimeStamp);
		}
		//System.out.println("Cleaning finished!");
		if(hashMap.isEmpty()) {
			//System.out.printf("The hashmap is now empty.");
			if(scheduledFuture!=null) {
				scheduledFuture.cancel(false);
				scheduledFuture = null;
				isCleanUpScheduled = false;
				System.out.printf("The clean-up schedule is cancelled.");
			}
			//System.out.println();
		}
	}
	

	/**
	 * A helper class to store aggregated transactions per millisecond
	 *
	 */
	private class TransactionsPerMillisecond {
		long count;
		double sum, avg, max, min;
		public TransactionsPerMillisecond(double sum) {
			this.count = 1;
			this.sum = sum;
			this.avg = sum;
			this.max = sum;
			this.min = sum;
		}
	}
}
