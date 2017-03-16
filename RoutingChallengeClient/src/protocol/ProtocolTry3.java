package protocol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import client.DataTable;
import client.IRoutingProtocol;
import client.LinkLayer;
import client.Packet;

public class ProtocolTry3 implements IRoutingProtocol {
	private LinkLayer linkLayer;

	private HashMap<Integer, SmartRoute> fTable = new HashMap<>();
	private ArrayList<Integer> neighbors = new ArrayList<Integer>();
	private int myAddress;
	private int tickcounter = 0;

	@Override
	public void init(LinkLayer linkLayer) {
		
		// Put own link in forwarding table.
		this.linkLayer = linkLayer;
		myAddress = linkLayer.getOwnAddress();		
		fTable.put(myAddress, new SmartRoute(myAddress, 0, tickcounter));
		
		// Put own link in data table.
		DataTable dt = new DataTable(2);
		dt.addRow(new Integer[] { myAddress, 0 });
		
		// Transmit data table to everyone.
		Packet p = new Packet(myAddress, 0, dt);
		linkLayer.transmit(p);
	}

	@Override
	public void tick(Packet[] packets) {

		// Check incoming packets / update fTable
		for (Packet p : packets) {
			int neighbor = p.getSourceAddress();
			if (!neighbors.contains(neighbor)) {
				// New neighbor / neighbor is not listed yet.
				neighbors.add(neighbor);
			}
			// Analyze received data.
			received(p.getDataTable(), neighbor);
		}

		// Check connections to neighbors.
		for (int i = 0; i < neighbors.size(); i++) {
			if (linkLayer.getLinkCost(neighbors.get(i)) == -1) {
				// Connection to neighbor is broken, he gets removed.
				neighbors.remove(i);
			}
		}
		
		// Refresh old data.
		ArrayList<Integer> deletelist = new ArrayList<Integer>();
		for (int key : fTable.keySet()) {
			if (!neighbors.contains(fTable.get(key).link) && key != myAddress) {
				// There are still forwarding tables using broken connections.
				deletelist.add(key);
				fTable.get(key).cost = 99999999;
			}
		}

		// Check entries.
		for (int key : fTable.keySet()) {
			if (tickcounter - (fTable.get(key).tick) >= 1 && key != myAddress) {
				// Entry is expired, needs to be reset. 
				fTable.get(key).cost = 99999999;
				deletelist.add(key);
			}
		}

		// Send packets.
		for (int neighbor : neighbors) {
			// Initialize vector.
			DataTable vector = new DataTable(2);
			for (int dest : fTable.keySet()) {
				if (dest != neighbor) {
					// Fill vector.
					if (fTable.get(dest).link == neighbor) {
						// Poison reverse.
						vector.addRow(new Integer[] { dest, 10000000 });
					} else {
						// Normal transmission, no poison reverse.
						vector.addRow(new Integer[] { dest, fTable.get(dest).cost });
					}
				}
			}
			// Transmit only to neighbor, split horizon.
			Packet p = new Packet(myAddress, neighbor, vector);
			linkLayer.transmit(p);
		}
		// Transmit connection to itself.
		DataTable dt = new DataTable(2);
		dt.addRow(new Integer[] { myAddress, 0 });
		Packet p = new Packet(myAddress, 0, dt);
		linkLayer.transmit(p);

		// deleting expired entries.
		for (int key : deletelist) {
			if (key != myAddress) {
				fTable.remove(key);
			}
		}

		// Increase tickcounter to detect expired entries.
		tickcounter++;
	}

	private void received(DataTable dt, int neighbour) {
		// received Vector dt from link neighbour.
		int linkcost = linkLayer.getLinkCost(neighbour);
		for (int i = 0; i < dt.getNRows(); i++) {
			int dest = dt.get(i, 0);
			int totalcost = linkcost + dt.get(i, 1);
			// Check if route is better than saved one.
			if (!fTable.containsKey(dest) || totalcost <= fTable.get(dest).cost || fTable.get(dest).link == neighbour) {
				// new Route.
				SmartRoute r = new SmartRoute(neighbour, totalcost, tickcounter);
				// update forwarding table.
				fTable.put(dest, r);
			}
		}
	}

	public HashMap<Integer, Integer> getForwardingTable() {
		// This code transforms your forwarding table which may contain extra
		// information
		// to a simple one with only a next hop (value) for each destination
		// (key).
		// The result of this method is send to the server to validate and score
		// your protocol.

		// <Destination, NextHop>
		HashMap<Integer, Integer> ft = new HashMap<>();

		for (Map.Entry<Integer, SmartRoute> entry : fTable.entrySet()) {
			ft.put(entry.getKey(), entry.getValue().link);
		}

		return ft;
	}
}
