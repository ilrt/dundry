package com.turn.ttorrent.tracker;

import java.util.concurrent.ConcurrentMap;

/**
 * Work around constructor visibility issue
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
public class TrackerFactory {
    
    public static TrackerService getTrackerService(String version,
			ConcurrentMap<String, TrackedTorrent> torrents) {
        return new TrackerService(version, torrents);
    }
    
}
