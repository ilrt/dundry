package uk.ac.bristol.dundry.dao;

import com.turn.ttorrent.tracker.TrackedTorrent;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Tracks torrents and purges clients.
 * TTorrent implements this, of course, but uses its own http server.
 * 
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
@Component
public class BTTracking {
    
    final static Logger log = LoggerFactory.getLogger(BTTracking.class);
    
    final Thread purger;
    
    private final ConcurrentMap<String, TrackedTorrent> torrents;
    
    public BTTracking() {
        torrents = new ConcurrentHashMap<>();
        purger = new Thread(new Purger(torrents), "Torrent client purger");
        purger.start(); // may not be a good idea? Could quartz do this work instead?
    }

    public ConcurrentMap<String, TrackedTorrent> getTorrents() {
        return torrents;
    }
    
    static class Purger implements Runnable {
        private final ConcurrentMap<String, TrackedTorrent> torrents;
        
        Purger(ConcurrentMap<String, TrackedTorrent> torrents) {
            this.torrents = torrents;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(30 * 1000);
            } catch (InterruptedException ex) {
                log.error("Purger sleep disturbed", ex);
            }
            
            log.info("checking unfresh peers");
            for (TrackedTorrent torrent: torrents.values()) {
                torrent.collectUnfreshPeers();
            }
        }
        
    }
}
