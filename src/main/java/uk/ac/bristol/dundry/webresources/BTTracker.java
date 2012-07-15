/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.bristol.dundry.webresources;

import com.turn.ttorrent.common.protocol.TrackerMessage;
import com.turn.ttorrent.common.protocol.TrackerMessage.AnnounceRequestMessage.RequestEvent;
import com.turn.ttorrent.common.protocol.TrackerMessage.ErrorMessage.FailureReason;
import com.turn.ttorrent.common.protocol.TrackerMessage.MessageValidationException;
import com.turn.ttorrent.common.protocol.http.HTTPAnnounceResponseMessage;
import com.turn.ttorrent.common.protocol.http.HTTPTrackerErrorMessage;
import com.turn.ttorrent.tracker.TrackedPeer;
import com.turn.ttorrent.tracker.TrackedTorrent;
import com.turn.ttorrent.tracker.TrackerFactory;
import com.turn.ttorrent.tracker.TrackerService;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
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
        
    /** See http://wiki.theory.org/BitTorrent_Tracker_Protocol#Basic_Tracker_Announce_Request **/
    @GET
    @Path("announce")
    public Response track(
        @QueryParam("info_hash") String infoHash, 
        @QueryParam("peer_id") String peerId,
        @QueryParam("ip") @DefaultValue("") String ip, 
        @QueryParam("port") int port,
        @QueryParam("uploaded") long uploaded, 
        @QueryParam("downloaded") long downloaded,
        @QueryParam("left") long left, 
        @QueryParam("event") @DefaultValue("none") RequestEvent event,
        @QueryParam("numwant") @DefaultValue("50") int numwant,
        @QueryParam("compact") @DefaultValue("0") int compact,
        @QueryParam("nopeerid") @DefaultValue("0") int noPeerId
            ) throws UnsupportedEncodingException, IOException, MessageValidationException {
        
        ConcurrentMap<String, TrackedTorrent> trackedTorrents = torrentTracker.getTorrents();
                
        if (!trackedTorrents.containsKey(infoHash)) {
            return error(Response.Status.BAD_REQUEST, FailureReason.UNKNOWN_TORRENT);
        }
        
        TrackedTorrent tt = trackedTorrents.get(infoHash);
        
        TrackedPeer peer = tt.update(
                            TrackerMessage.AnnounceRequestMessage.RequestEvent.NONE,
                            ByteBuffer.wrap(peerId.getBytes("US-ASCII")), 
                            peerId, ip, port, uploaded, downloaded, left);
        
        HTTPAnnounceResponseMessage response = HTTPAnnounceResponseMessage.craft(
				tt.getAnnounceInterval(),
				TrackedTorrent.MIN_ANNOUNCE_INTERVAL_SECONDS,
				"Dundry (TTorrent)",
				tt.seeders(),
				tt.leechers(),
				tt.getSomePeers(peer));
        
        return Response.ok(response.getData(), "text/plain")
                .header("Date", System.currentTimeMillis())
                .header("Server", "Dundry (TTorrent)")
                .build();
    }

    private Response error(Status status, FailureReason reason)
            throws IOException, MessageValidationException {
        HTTPTrackerErrorMessage message = HTTPTrackerErrorMessage.craft(reason);
        return Response.status(status).entity(message.getData()).build();
    }
}

