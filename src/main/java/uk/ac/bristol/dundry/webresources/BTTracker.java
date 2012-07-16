/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.bristol.dundry.webresources;

import com.google.common.base.Joiner;
import com.turn.ttorrent.bcodec.BDecoder;
import com.turn.ttorrent.bcodec.BEValue;
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
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
   
    final static Logger log = LoggerFactory.getLogger(BTTracker.class);
    
    @Autowired
    BTTracking torrentTracker;
    
    @Autowired
    Repository repository;
        
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
        @QueryParam("event") @DefaultValue("NONE") String eventName,
        @QueryParam("numwant") @DefaultValue("50") int numwant,
        @QueryParam("compact") @DefaultValue("0") int compact,
        @QueryParam("nopeerid") @DefaultValue("0") int noPeerId,
        @Context UriInfo uriInfo
            ) throws UnsupportedEncodingException, IOException, MessageValidationException {
        
        log.debug("info {} peer {} ip {} port {} up {} down {} left {} event {} numwant {} compact {} nopeerid {}",
                new Object[]{infoHash, peerId, ip, port, uploaded, downloaded, left, eventName, numwant, compact, noPeerId});
        
        RequestEvent event = RequestEvent.valueOf(eventName.toUpperCase());
        
        ConcurrentMap<String, TrackedTorrent> trackedTorrents = torrentTracker.getTorrents();
        
        
        // infoHash is encoded in an annoying way that jersey messes up
        String rawQuery = uriInfo.getRequestUri().getRawQuery();
        int start = rawQuery.indexOf("info_hash=") + 10; // plus length of param bit
        String encodedHash = rawQuery.substring(start,rawQuery.indexOf('&', start));
        byte[] hashBytes = URLDecoder.decode(encodedHash, "iso-8859-1").getBytes("iso-8859-1");
        infoHash = new BigInteger(hashBytes).toString(16).toUpperCase();
        
        log.warn("encodedHash: {} infoHash: {}", encodedHash, infoHash);
        
        String peerIdHash = new BigInteger(peerId.getBytes("iso-8859-1")).toString(16);
        
        if (!trackedTorrents.containsKey(infoHash)) {
            String torrents = Joiner.on(" , ").join(trackedTorrents.keySet());
            log.warn("Torrent {} is unknown. I know [{}].", infoHash, torrents);
            return error(Response.Status.BAD_REQUEST, FailureReason.UNKNOWN_TORRENT);
        }
        
        TrackedTorrent tt = trackedTorrents.get(infoHash);
        
        Map<String, TrackedPeer> peers = tt.getPeers();
        if (!peers.containsKey(peerIdHash) && event != RequestEvent.STARTED) {
            String allPeers = Joiner.on(" , ").join(peers.keySet());
            log.warn("Peer {} is unknown. I know [{}].", peerId, allPeers);
            
            if (event == RequestEvent.NONE) event = RequestEvent.STARTED;
        }
        
        log.warn("Event: {}", event);
                
        TrackedPeer peer = tt.update(
                            event,
                            ByteBuffer.wrap(peerId.getBytes("iso-8859-1")), 
                            peerIdHash, ip, port, uploaded, downloaded, left);
        
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

