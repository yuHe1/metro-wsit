/*
 * Copyright (c) 2005 Sun Microsystems, Inc.
 * All rights reserved.
 */
package wstrust.scenario5.server;


@javax.jws.WebService (endpointInterface="wstrust.scenario5.server.IPingService")
public class PingImpl implements IPingService {
    
    public PingResponseBody ping(Ping ping){
        
        PingResponseBody resp = new PingResponseBody();
        
        String scenario = ping.getScenario();
        System.out.println("scenario = " + scenario);
        resp.setScenario(scenario);
        
        String origin = ping.getOrigin();
        System.out.println("origin = " + origin);
        resp.setOrigin(origin);
        
        String text = ping.getText();
        System.out.println("text = " + text);
        resp.setText(text);
        
        return resp;
    }  
}
