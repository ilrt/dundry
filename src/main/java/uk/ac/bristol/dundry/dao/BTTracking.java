package uk.ac.bristol.dundry.dao;

import com.turn.ttorrent.client.Client;
import com.turn.ttorrent.client.SharedTorrent;
import com.turn.ttorrent.common.Torrent;
import com.turn.ttorrent.tracker.TrackedTorrent;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Tracks trackedTorrents and purges clients. TTorrent implements this, of course, but
 * uses its own http server.
 *
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
public class BTTracking {

    final static Logger log = LoggerFactory.getLogger(BTTracking.class);
    final Thread purger;
    private final ConcurrentMap<String, TrackedTorrent> trackedTorrents;
    private final ConcurrentMap<String, Client> clients;
    private final InetAddress address;

    public BTTracking(String publishBase) throws IOException, NoSuchAlgorithmException {
        log.info("Starting up bittorrent");
        address = InetAddress.getLocalHost();
        log.info("Address is {}", address.getHostAddress());
        trackedTorrents = new ConcurrentHashMap<>();
        clients = new ConcurrentHashMap<>();
        initTorrents(Paths.get(publishBase), trackedTorrents, clients);
        purger = new Thread(new Purger(trackedTorrents), "Torrent client purger");
        purger.start(); // may not be a good idea? Could quartz do this work instead?
    }
    
    public ConcurrentMap<String, TrackedTorrent> getTorrents() {
        return trackedTorrents;
    }
    
    /**
     * Look for, load, and serve trackedTorrents in base
     * @param base
     * @param map
     * @return 
     */
    private void initTorrents(Path base, 
            ConcurrentMap<String, TrackedTorrent> trackedTorrents,
            ConcurrentMap<String, Client> clients)
                throws IOException, NoSuchAlgorithmException {
        log.info("Looking for existing torrents in {}", base);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(base)) {
            for (Path pubDir: stream) {
                // Find a torrent for this dir if possible
                Path torrentFile = findTorrent(pubDir, pubDir, base);
                
                if (torrentFile != null) {
                    log.info("Found torrent {}", torrentFile);
                    
                    // Load as a seeder
                    Torrent torrent = Torrent.load(torrentFile.toFile(), base.toFile(), true);
                    
                    SharedTorrent shared = new SharedTorrent(torrent, base.toFile(), true);
                    
                    if (!shared.isSeeder()) {
                        log.warn("Torrent {} is not seeding. Skipping.", torrentFile);
                        continue;
                    }
                    
                    TrackedTorrent tracked = new TrackedTorrent(torrent);
                    
                    trackedTorrents.put(torrent.getHexInfoHash(), tracked);
                    
                    Client client = new Client(address, shared);
                    clients.put(torrent.getHexInfoHash(), client);
                    client.share();
                    
                    log.info("Sharing {}", torrentFile);
                } 
            }
        }
    }

    private Path findTorrent(Path forDir, Path... locations) {
        Path toFind = Paths.get(forDir.getFileName().toString() + ".torrent");
        
        for (Path location: locations) {
            Path candidate = location.resolve(toFind);
            if (Files.exists(candidate)) return candidate;
        }
        
        return null;
    }

    static class Purger implements Runnable {

        private final ConcurrentMap<String, TrackedTorrent> torrents;

        Purger(ConcurrentMap<String, TrackedTorrent> torrents) {
            this.torrents = torrents;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(30 * 1000);
                } catch (InterruptedException ex) {
                    log.error("Purger sleep disturbed", ex);
                }

                log.info("checking unfresh peers");
                for (TrackedTorrent torrent : torrents.values()) {
                    torrent.collectUnfreshPeers();
                }
            }
        }
    }
}
