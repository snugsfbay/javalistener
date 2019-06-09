SoapConnection toolingConn; //Already set and logged in
BayeuxClient client; //Already set and logged in

//Listen on the handshake event 
boolean handshaken = client.waitFor(10 * 1000, BayeuxClient.State.CONNECTED); 
if (!handshaken) {
   System.out.println("Failed to handshake: " + client); 
   System.exit(1); 
} 
final RunTestListener = null; 
client.getChannel(Channel.META_SUBSCRIBE).addListener( 
   new ClientSessionChannel.MessageListener() { 
      public void onMessage(ClientSessionChannel channel, Message message) { 
         boolean success = message.isSuccessful(); 
         if (success) { 
            //Replace with your own classes and suites
            String apexTestClassId1 = "01pD00000007M0CIAU"; 
            String apexTestClassId2 = "01pD00000007NqtIAE";
            String apexTestSuiteId1 = "05FD00000004CDBMA2";
            String apexTestClassName1 = "Test_MyClass";
            String apexTestSuiteName1 = "TestSuite_MySuite";
            listener.runTests(new String[]{apexTestClassId1, apexTestClassId2},
               new String[]{apexTestSuiteId1}, 1, new String[]{apexTestClassName1},
               new String[]{apexTestSuiteName1});
         }
      }
   };
);
//This will subscribe to the TestRun system topic 
listener = new RunTestListener(client, toolingConn);
