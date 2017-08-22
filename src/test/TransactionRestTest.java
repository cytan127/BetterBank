package test;

import static org.junit.Assert.*;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.junit.Test;

import com.banking.example.BankRestService;


public class TransactionRestTest extends JerseyTest{

    @Override
    public Application configure() {
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);
        return new ResourceConfig(BankRestService.class);
    }
    /**
     * This method tests 3 cases:
     * 	1) correctly receive, parse and put the data to the hashmap, with a current timestamp
     *  2) correctly receive, parse and put the data to the hashmap, given a timestamp which already exists
     *  3) correctly receive, parse but decide not to put the hashmap. with an expired timestamp
     **/
    @Test 
    public void testTransactionAPI() {
		System.out.println("JUnit Test in TransactionRestTest");
    		//test 1
     	long millis = System.currentTimeMillis();
     	final String jsonUpdated = "{\"amount\": 10,\n"+"\"timestamp\": "+(millis)+"}";
        final Response response1 = target("api/transactions").request(MediaType.APPLICATION_JSON_TYPE).post(Entity.json(jsonUpdated));
        assertEquals(response1.getStatus(), 201);
        
        //test 2
        final Response response2 = target("api/transactions").request(MediaType.APPLICATION_JSON_TYPE).post(Entity.json(jsonUpdated));
        assertEquals(response2.getStatus(), 201);
        
        //test 3
     	millis = System.currentTimeMillis()-61000; // 61 seconds ago
     	final String jsonOutdated = "{\"amount\": 10.1,\n"+"\"timestamp\": "+millis+"}";
     	final Response response3 = target("api/transactions").request(MediaType.APPLICATION_JSON_TYPE).post(Entity.json(jsonOutdated));
        assertEquals(response3.getStatus(), 204);
    }
}


