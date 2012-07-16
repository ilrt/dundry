package uk.ac.bristol.dundry.webresources;

import com.coremedia.iso.Hex;
import com.google.common.base.Joiner;
import com.turn.ttorrent.common.protocol.TrackerMessage.AnnounceRequestMessage.RequestEvent;
import com.turn.ttorrent.common.protocol.TrackerMessage.ErrorMessage.FailureReason;
import com.turn.ttorrent.common.protocol.TrackerMessage.MessageValidationException;
import com.turn.ttorrent.common.protocol.http.HTTPAnnounceResponseMessage;
import com.turn.ttorrent.common.protocol.http.HTTPTrackerErrorMessage;
import com.turn.ttorrent.tracker.TrackedPeer;
import com.turn.ttorrent.tracker.TrackedTorrent;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.ac.bristol.dundry.dao.BTTracking;
import uk.ac.bristol.dundry.dao.Repository;

/**
 *
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
@Component
@Path("tracker")
public class BTTracker {
   
    final static Logger log = LoggerFactory.getLogger(BTTracker.class);
    
    final static Charset ISO88591 = Charset.forName("iso-8859-1");
    
    @Autowired
    BTTracking torrentTracker;
    
    @Autowired
    Repository repository;
        
    /** See http://wiki.theory.org/BitTorrent_Tracker_Protocol#Basic_Tracker_Announce_Request **/
    @GET
    @Path("announce")
    public Response track(
        @QueryParam("info_hash") String infoHashBroken, 
        @QueryParam("peer_id") String peerIdBroken,
        @QueryParam("ip") @DefaultValue("") String ip, 
        @QueryParam("port") int port,
        @QueryParam("uploaded") long uploaded, 
        @QueryParam("downloaded") long downloaded,
        @QueryParam("left") long left, 
        @QueryParam("event") @DefaultValue("NONE") String eventName,
        @QueryParam("numwant") @DefaultValue("50") int numwant,
        @QueryParam("compact") @DefaultValue("0") int compact,
        @QueryParam("nopeerid") @DefaultValue("0") int noPeerId,
        @Context HttpServletRequest req
            ) throws UnsupportedEncodingException, IOException, MessageValidationException {
        
        log.debug("info {} peer {} ip {} port {} up {} down {} left {} event {} numwant {} compact {} nopeerid {}",
                new Object[]{infoHashBroken, peerIdBroken, ip, port, uploaded, downloaded, left, eventName, numwant, compact, noPeerId});
         
        // Fix up request params
        
        RequestEvent event = RequestEvent.valueOf(eventName.toUpperCase());
        
        // Jersey and BT disagree about string decoding
        String rawQuery = req.getQueryString();
        String infoHash = Hex.encodeHex(getParam("info_hash", rawQuery));
        byte[] peerId = getParam("peer_id", rawQuery);
        String peerIdHash = Hex.encodeHex(peerId);
        
        if (ip.isEmpty()) {
            // TODO: I seem to have an ipv6 issue with loopback
            ip = req.getRemoteHost();
            log.debug("IP for {} is empty, using {}", peerIdHash, ip);
        }
        
        ConcurrentMap<String, TrackedTorrent> trackedTorrents = torrentTracker.getTorrents();
        
        if (!trackedTorrents.containsKey(infoHash)) {
            String torrents = Joiner.on(" , ").join(trackedTorrents.keySet());
            log.warn("Torrent {} is unknown. I know [{}].", infoHash, torrents);
            return error(Response.Status.BAD_REQUEST, FailureReason.UNKNOWN_TORRENT);
        }
        
        TrackedTorrent tt = trackedTorrents.get(infoHash);
        
        Map<String, TrackedPeer> peers = tt.getPeers();
        if (!peers.containsKey(peerIdHash) && event != RequestEvent.STARTED) {
            String allPeers = Joiner.on(" , ").join(peers.keySet());
            log.warn("Peer {} is unknown. I know [{}].", peerIdHash, allPeers);
            
            // I think we seem sometimes miss started events?
            if (event == RequestEvent.NONE) event = RequestEvent.STARTED;
        }
                        
        TrackedPeer peer = tt.update(
                            event,
                            ByteBuffer.wrap(peerId), 
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
    
    /**
     * Bittorrent treats some query param fields as raw bytes, which conflicts
     * mightily with jersey
     * @param paramName 
     * @param rawQueryString
     * @return 
     */
    private byte[] getParam(String paramName, String rawQueryString) throws UnsupportedEncodingException {
        
        // Get the value of interest
        int start = rawQueryString.indexOf(paramName) + 1 + paramName.length();
        int end = rawQueryString.indexOf('&', start);
        String encodedValue = (end == -1) ?
                rawQueryString.substring(start) : 
                rawQueryString.substring(start, end);
        
        // Decode using iso 8859 1, which will pull values out simply
        return URLDecoder.decode(encodedValue, "iso-8859-1").getBytes(ISO88591);
    }
}

