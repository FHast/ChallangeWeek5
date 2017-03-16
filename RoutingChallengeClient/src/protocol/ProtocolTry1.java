package protocol;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import client.DataTable;
import client.IRoutingProtocol;
import client.LinkLayer;
import client.Packet;

public class ProtocolTry1 implements IRoutingProtocol {
	private LinkLayer linkLayer;

	// You can use this data structure to store your forwarding table with extra
	// information.
	private HashMap<Integer, SmartRoute> fTable = new HashMap<>();
	private ArrayList<Integer> neighbours = new ArrayList<Integer>();
	private int myAddress;

	@Override
	public void init(LinkLayer linkLayer) {
		this.linkLayer = linkLayer;
		myAddress = linkLayer.getOwnAddress();
	}

	@Override
	public void tick(Packet[] packets) {

		// Check incoming packets / update fTable

		for (Packet p : packets) {
			int neighbour = p.getSourceAddress();
			if (!neighbours.contains(neighbour)) {
				neighbours.add(neighbour);
			}
			received(p.getDataTable(), neighbour);
		}

		// Send packets

		for (int i = 0; i < neighbours.size() - 1; i++) {
			DataTable vector = new DataTable(fTable.size() - 1);
			for (int dest : fTable.keySet()) {
				if (dest != neighbours.get(i)) {
					vector.addRow(new Integer[] { dest, fTable.get(dest).cost });
				}
			}
			Packet p = new Packet(myAddress, neighbours.get(1), vector);
			linkLayer.transmit(p);
		}
	}

	private void received(DataTable dt, int neighbour) {
		// received Vector dt from link neightbour
		System.out.println("Received vector " + dt.toString() + " from link " + neighbour);
		int cost = linkLayer.getLinkCost(neighbour);
		for (int i = 0; i < dt.getNRows(); i++) {
			if (!fTable.containsKey(dt.get(i, 0))) {
				// new Route
				SmartRoute r = new SmartRoute(neighbour, cost + dt.get(i, 1), LocalTime.now());
			} else {
				// existing Route, is new better?
				if (cost + dt.get(i, 1) < fTable.get(dt.get(i, 0)).cost || fTable.get(dt.get(i, 0)).link == neighbour) {
					// better Route, change current route!
					SmartRoute r = new SmartRoute(neighbour, cost + dt.get(i, 1), LocalTime.now());
				}
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
