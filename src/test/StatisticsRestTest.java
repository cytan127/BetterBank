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

public class StatisticsRestTest extends JerseyTest{
	private int numOfTransactionsCalled = 0;
    @Override
    public Application configure() {
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);
        return new ResourceConfig(BankRestService.class);
    }
    /**
     *  This method keeps inserting transactions for @param times of iterations
     * @param times - number of times of the predefined JSON to be inserted, with the same current timestamp and amount of 1.0
     */
    public void addTransactionForTimes(final int times) {
    		final long millis = System.currentTimeMillis();
        for(int i=0;i<times;i++) {
        		String json = "{\"amount\": 1.0,\n"+"\"timestamp\": "+millis+"}";
             target("api/transactions").request(MediaType.APPLICATION_JSON_TYPE).post(Entity.json(json));
             numOfTransactionsCalled++;
        }
    }
    /**
     * This method inserts a single transaction with  @param amount with current timestamp
     * @param amount in double, to be inserted
     */
    public void addTransactionWithAmount(final double amount) {
    		final long millis = System.currentTimeMillis();
    		final String json = "{\"amount\": "+amount+",\n"+"\"timestamp\": "+millis+"}";
        target("api/transactions").request(MediaType.APPLICATION_JSON_TYPE).post(Entity.json(json));
        numOfTransactionsCalled++;
    }
    /**
     * The test method, this inserts one transaction with smallest amount, one transaction with largest amount, 
     * and a repeated (@param times) of transactions of each amount of 1
     * so supposedly:
     * count = (1+1+times)
     *  sum  = smallest+largest+times*1
     *  min  = smallest
     *  max  = largest
     *  avg = (smallest+largest+times*1)/(1+1+times)
     * 
     */
    @Test
    public void testStatisticsAPI() {
    		System.out.println("JUnit Test in StatisticsRestTest");
    		//creating test data
    		final double smallest = 0.00000001;
    		final double largest = 99999.0;
    		final int times = 1000;
    		addTransactionForTimes(times);
    		addTransactionWithAmount(smallest);
    		addTransactionWithAmount(largest);
    		
    		//precision as a margin of error for comparing two double value
    		final double precision = 0.0000001;
    		
    		final Response response = target("api/statistics").request().get();
        assertEquals(200, response.getStatus());
        final String jsonMessage = response.readEntity(String.class);
       // System.out.println("Test Response :"+ jsonMessage);
        
        JSONObject jsonObject =  new JSONObject(jsonMessage);
       // System.out.println("average should be " + ((smallest+largest+times)/numOfTransactionsCalled));
        assertEquals(numOfTransactionsCalled, jsonObject.getInt("count"));
        assertEquals((smallest+largest+times)/numOfTransactionsCalled, jsonObject.getDouble("avg"), precision);
        assertEquals(smallest,jsonObject.getDouble("min"),precision);
        assertEquals(largest,jsonObject.getDouble("max"),precision);
        assertEquals(smallest+largest+times,jsonObject.getDouble("sum"),precision);
    	
    }
}
