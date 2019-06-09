import java.util.HashMap;
import org.apache.commons.lang3.StringUtils;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.bayeux.client.ClientSessionChannel.MessageListener;
import org.cometd.client.BayeuxClient;

import com.sforce.soap.tooling.ApexTestQueueItem;
import com.sforce.soap.tooling.ApexTestResult;
import com.sforce.soap.tooling.QueryResult;
import com.sforce.soap.tooling.SObject;
import com.sforce.soap.tooling.SoapConnection;
import com.sforce.soap.tooling.TestLevel;
import com.sforce.ws.ConnectionException;

public class RunTestListener {
   private static final String CHANNEL = "/systemTopic/TestResult";
   private SoapConnection conn;

   public RunTestListener(BayeuxClient client, SoapConnection conn) {
      this.conn = conn;  
      System.out.println("Subscribing for channel: " + CHANNEL);  
      client.getChannel(CHANNEL).subscribe(new MessageListener() {   
         @Override
         public void onMessage(ClientSessionChannel channel, Message message) {    
            HashMap data = (HashMap) message.getData();    
            HashMap sobject = (HashMap) data.get("sobject");    
            String id = (String) sobject.get("Id");    
            System.out.println("\nAysncApexJob " + id);    
            getTestQueueItems(id);
         }
     }); 
   }

   public void runTests(String[] apexTestClassIds, String[] apexTestSuiteIds,
      Integer maxFailedTests, String[] apexTestClassNames, String[] apexTestSuiteNames) {

      // All parameters are required

      if (apexTestClassIds == null && apexTestSuiteIds == null
         && apexTestClassNames == null && apexTestSuiteNames == null) {
         System.out.println("No tests to run");
         return;
      }
      String classIds = StringUtils.join(apexTestClassIds,", ");
      String suiteIds = StringUtils.join(apexTestSuiteIds,", ");
      String classNames = StringUtils.join(apexTestClassNames,", ");
      String suiteNames = StringUtils.join(apexTestSuiteNames,", ");

      String tests = null;
      Boolean skipCodeCover = false;

      try {
         System.out.println("Running async test run");
         conn.runTestsAsynchronous(classIds, suiteIds, maxFailedTests,
            TestLevel.RunSpecifiedTests, classNames, suiteNames, tests, skipCodeCover);
      } catch (ConnectionException e) {
         e.printStackTrace();
      }
   }
   public void createAndRunTestsNode(String apexTestClassName,
      String apexTestClassId, String[] apexTestMethods) {

      //Currently, the array size of TestNode objects must be 1

      //Provide a non-null class name or a non-null class ID
      if (apexTestClassName != null && apexTestClassId != null) {
         System.out.println("Specify a class name OR a class ID");
         return;
      } else if (apexTestClassName == null && apexTestClassId == null) {
         System.out.println("No tests to run");
         return;
      }

      TestsNode thisTestsNode = new TestsNode();
      thisTestsNode.setClassName(apexTestClassName);
      thisTestsNode.setClassId(apexTestClassId);
      thisTestsNode.setTestMethods(apexTestMethods);
      TestsNode[] tests = new TestsNode[] { thisTestsNode };

      try {
         System.out.println("Running async test run");
         conn.runTestsAsynchronous(null, null, -1, null, null, null, tests);
      } catch (ConnectionException e) {
         e.printStackTrace();
      }
   }

   private void getTestQueueItems(String asyncApexJobId) {
      try {   
         QueryResult res = conn     
            .query("SELECT Id, Status, ApexClassId FROM ApexTestQueueItem
               WHERE ParentJobId = '" + asyncApexJobId + "'");
         if (res.getSize() > 0) {
            for (SObject o : res.getRecords()) {     
               ApexTestQueueItem atqi = (ApexTestQueueItem) o;     
               System.out.println("\tApexTestQueueItem - "+atqi.getStatus());
               if (atqi.getStatus().equals("Completed")) {      
                  getApexTestResults(atqi.getId());     
               }    
            }   
         } else {    
            System.out.println("No queued items for " + asyncApexJobId);  
         }  
      } catch (ConnectionException e) {   
         e.printStackTrace();  
      } 
   }

   private void getApexTestResults(String apexTestQueueItemId) {
      try {   
         QueryResult res = conn     
            .query("SELECT StackTrace,Message, AsyncApexJobId,MethodName, Outcome,ApexClassId
               FROM ApexTestResult WHERE QueueItemId = '" + apexTestQueueItemId + "'");
         if (res.getSize() > 0) {
            for (SObject o : res.getRecords()) {     
               ApexTestResult atr = (ApexTestResult) o;     
               System.out.println("\tTest result for "       
                  + atr.getApexClassId() + "." + atr.getMethodName());     
               String msg = atr.getOutcome().equals("Fail") ? " - "       
                  + atr.getMessage() + " " + atr.getStackTrace() : "";     
               System.out.println("\t\tTest " + atr.getOutcome() + msg);    
            }   
         } else {    
            System.out.println("No Test Results for " + apexTestQueueItemId);   
         }  
      } catch (ConnectionException e) {
          e.printStackTrace();  
      } 
   }
}
