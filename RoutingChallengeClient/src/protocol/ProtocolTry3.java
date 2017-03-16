package protocol;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import client.DataTable;
import client.IRoutingProtocol;
import client.LinkLayer;
import client.Packet;

public class ProtocolTry3 implements IRoutingProtocol {
	private LinkLayer linkLayer;

	// You can use this data structure to store your forwarding table with extra
	// information.
	private HashMap<Integer, SmartRoute> fTable = new HashMap<>();
	private ArrayList<Integer> neighbours = new ArrayList<Integer>();
	private int myAddress;
	private int tickcounter = 0;

	@Override
	public void init(LinkLayer linkLayer) {
		this.linkLayer = linkLayer;
		myAddress = linkLayer.getOwnAddress();
		System.out.println("[NEW]Adding route: dest = " + myAddress + ", totalcost = " + 0 + ", link =" + myAddress);
		fTable.put(myAddress, new SmartRoute(myAddress, 0, tickcounter));
		DataTable dt = new DataTable(2);
		dt.addRow(new Integer[] { myAddress, 0 });
		Packet p = new Packet(myAddress, 0, dt);
		linkLayer.transmit(p);
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
		
		for (int i = 0; i < neighbours.size(); i++) {
			if (linkLayer.getLinkCost(neighbours.get(i)) == -1) {
				System.out.println("Removing " + neighbours.get(i) + "from neighbours.");
				neighbours.remove(i);
			}
		}
		ArrayList<Integer> deletelist = new ArrayList<Integer>();
		for (int key : fTable.keySet()) {
			if  (!neighbours.contains(fTable.get(key).link) && key != myAddress) {
				deletelist.add(key);
				fTable.get(key).cost = 99999999;
			}
		}
		
		// Check entries

		for (int key : fTable.keySet()) {
			if (tickcounter - (fTable.get(key).tick) >= 1 && key != myAddress) {
				System.out.println("Entry Expired. ");
				fTable.get(key).cost = 99999999;
				deletelist.add(key);
			}
		}

		// Send packets

		for (int neighbour : neighbours) {
			DataTable vector = new DataTable(2);
			for (int dest : fTable.keySet()) {
				if (dest != neighbour) {
					vector.addRow(new Integer[] { dest, fTable.get(dest).cost });
				}
			}
			Packet p = new Packet(myAddress, neighbour, vector);
			linkLayer.transmit(p);
		}
		DataTable dt = new DataTable(2);
		dt.addRow(new Integer[] { myAddress, 0 });
		Packet p = new Packet(myAddress, 0, dt);
		linkLayer.transmit(p);
		

		// deleting expired entries

		for (int key : deletelist) {
			if (key != myAddress) {
				System.out.println("Removing: " + key);
				fTable.remove(key);
			}
		}

		tickcounter++;
	}

	private void received(DataTable dt, int neighbour) {
		// received Vector dt from link neightbour
		int linkcost = linkLayer.getLinkCost(neighbour);
		for (int i = 0; i < dt.getNRows(); i++) {
			int dest = dt.get(i, 0);
			int totalcost = linkcost + dt.get(i, 1);
			if (!fTable.containsKey(dest) || totalcost <= fTable.get(dest).cost || fTable.get(dest).link == neighbour) {
				// new Route
				System.out.println(
						"Adding route: dest = " + dest + ", totalcost = " + totalcost + ", link =" + neighbour);
				SmartRoute r = new SmartRoute(neighbour, totalcost, tickcounter);
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
