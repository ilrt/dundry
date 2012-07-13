/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.bristol.dundry.webresources;

import com.turn.ttorrent.tracker.TrackerFactory;
import com.turn.ttorrent.tracker.TrackerService;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.ac.bristol.dundry.dao.BTTracking;
import uk.ac.bristol.dundry.dao.Repository;
import uk.ac.bristol.dundry.webresources.adapter.SFContainerAdapter;

/**
 *
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
@Component
@Path("tracker")
public class BTTracker {
   
    @Autowired
    BTTracking torrentTracker;
    
    @Autowired
    Repository repository;
    
    /*@GET
    @Path("announce")
    public void track(HttpServletRequest request, HttpServletResponse response) {
        TrackerService ts = TrackerFactory.getTrackerService("Dundry (TTorrent)", torrentTracker.getTorrents());
        SFContainerAdapter.handle(ts, request, response);
    }*/
    
    enum Event { started, completed, stopped, none }
    
    /** See http://wiki.theory.org/BitTorrent_Tracker_Protocol#Basic_Tracker_Announce_Request **/
    @GET
    @Path("announce")
    public void track(
        @QueryParam("info_hash") String info_hash, 
        @QueryParam("peer_id") String peer_id,
        @QueryParam("ip") @DefaultValue("") String ip, 
        @QueryParam("port") int port,
        @QueryParam("uploaded") long uploaded, 
        @QueryParam("downloaded") long downloaded,
        @QueryParam("left") long left, 
        @QueryParam("event") @DefaultValue("none") Event event,
        @QueryParam("numwant") @DefaultValue("50") int numwant
            ) {
        
    }
}

