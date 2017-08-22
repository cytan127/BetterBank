package test;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.json.JSONObject;
import org.junit.Test;

import com.banking.example.BankRestService;

public class OneMinuteStatisticsRestTest extends JerseyTest{
    @Override
    public Application configure() {
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);
        return new ResourceConfig(BankRestService.class);
    }
    /**
     * This method insert one transaction per second for a total of 60 seconds, 
     * the APIs should be able to correctly calculate all statistics of a total of 60 transactions
     * @throws InterruptedException
     */
    //commented since it will take one minute
    //@Test
	public void testTransactionsForLast60Seconds() throws InterruptedException {
		System.out.println("JUnit Test in OneMinuteStatisticsRestTest");
		int amount = 0;
		//add one transaction per second
		//this will take 60 seconds, inserting one transaction with current timestamp for each second
		while(amount<60) {
		    long millis = System.currentTimeMillis();
		    //code to run
		    String json = "{\"amount\": " +amount+",\n"+"\"timestamp\": "+(millis)+"}";
		    target("api/transactions").request(MediaType.APPLICATION_JSON_TYPE).post(Entity.json(json));
		    Thread.sleep(1000 - millis % 1000);
		    amount++;
		}
		//precision as a margin of error for comparing two double value
		final double precision = 0.0000001;
		
	    Response response = target("api/statistics").request().get();
	    assertEquals(200, response.getStatus());
	    String jsonMessage = response.readEntity(String.class);
	    System.out.println("Test Response :"+ jsonMessage);
	    
	    final JSONObject jsonObject =  new JSONObject(jsonMessage);
	    //System.out.println("average should be " + ((59.0*60.0/2.0)/60.0));
	    assertEquals(60, jsonObject.getInt("count"));
	    assertEquals(((59.0*60.0/2.0)/60.0), jsonObject.getDouble("avg"), precision);
	    assertEquals(0,jsonObject.getDouble("min"),precision);
	    assertEquals(59,jsonObject.getDouble("max"),precision);
	    assertEquals((59*60.0/2),jsonObject.getDouble("sum"),precision);
		
		
	}
}
