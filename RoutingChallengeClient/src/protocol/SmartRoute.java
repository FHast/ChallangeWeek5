package protocol;

import java.time.LocalTime;

/**
 * Simple object which describes a route entry in the forwarding table.
 * Can be extended to include additional data.
 */
public class SmartRoute {
    public int link;
    public int cost;
    public int tick;
    
    public SmartRoute(int l, int c, int t) {
    	link = l;
    	cost = c;
    	tick = t;
    }
}
