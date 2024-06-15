import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.rise.trading.options.CSVParser;

class Seating {
	public Seating(String name2, String tableNo2, String seatCount2) {
		this.name = name2;
		this.tableNo = tableNo2;
		this.seatCount = seatCount2;
	}

	String name;
	String tableNo;
	String seatCount;
	String category;
	String position;
	String line;
	String phoneNumber;

}

public class TTADonorMerger {

	// System.out.println("sno,countrycode,number,name,title,URN,donortype,WelcomeKit,RegistrationKit");

	public static void main(String[] args) throws Exception {
		// mergeDonors();
		// missingDonors();
		// createMergedDonorsWithURN();
		// mergeAllGuestsExceptDonors();
		// createFileForSeatingAssignments();
		mergeAllGuests();
	}

	public static String rootDirectory = "/Users/sivad/Google Drive/TTA_Registrations/";

	private static void createFileForSeatingAssignments() throws Exception {
		String seating = "Donors List_Total_05-24-2024_Seating_Banquet.xlsx - Sheet1.csv";
		String donors = "master_contacts.csv";
		List<Donor> donorsList = getDonors(donors, 3, 2);
		List<Seating> seatingList = makeSeatingList(seating);
		addPhoneNumbersToSeatingList(seatingList, donorsList);
		// findSeatingListForEachDonor(donorsList, seatingList);
		// displayDonorsWithSeats(donorsList);
		// System.out.println("-----");
		// displaySeatsThatAreNotAssociatedWithDonors(seatingList);
		displaySeatsWithPhones(seatingList);
	}

	private static void displaySeatsWithPhones(List<Seating> seatingList) {
		for (Seating s : seatingList) {
			System.out.println(s.line + "," + s.phoneNumber);
		}

	}

	private static void addPhoneNumbersToSeatingList(List<Seating> seatingList, List<Donor> donorsList) {
		for (Seating s : seatingList) {
			for (Donor d : donorsList) {
				String sn = s.name;
				String dn = d.name;
				if (sn.equals(dn)) {
					s.phoneNumber = d.phone;
					continue;
				}
			}
		}

	}

	private static void displaySeatsThatAreNotAssociatedWithDonors(List<Seating> seatingList) {
		for (Seating s : seatingList) {
			System.out.println(s.name);
		}
	}

	private static void displayDonorsWithSeats(List<Donor> donorsList) {
		for (Donor d : donorsList) {
			List<Seating> seats = d.seating;
			if (seats.size() == 0) {
				System.out.println(d.name + " -> no seats");
			} else {
				System.out.print(d.name + " -> ");
				String x = "";
				for (Seating s : seats) {
					x = x + s.tableNo + " : " + s.seatCount + " : ";
				}
				System.out.println(x);
			}
		}

	}

	private static void findSeatingListForEachDonor(List<Donor> donorsList, List<Seating> seatingList) {
		for (Donor d : donorsList) {
			List<Seating> removableSeats = new ArrayList<Seating>();
			for (Seating s : seatingList) {
				String donorName = d.name;
				String seatDonorName = s.name;
				if (donorName.equalsIgnoreCase(seatDonorName)) {
					d.seating.add(s);
					removableSeats.add(s);
				}
			}
			seatingList.removeAll(removableSeats);
		}

	}

	private static List<Seating> makeSeatingList(String seatingFile) throws Exception {
		BufferedReader cbr = new BufferedReader(new FileReader(rootDirectory + seatingFile));
		ArrayList<Seating> seating = new ArrayList<Seating>();
		while (true) {
			String line = cbr.readLine();
			if (line == null)
				break;
			try {
				List<String> a = new CSVParser().parseCSV(line);
				// String a[] = line.split(",");
				String name = stripBracketsAndReplaceComma(SheetsQuickStart.capitalize(a.get(0)));
				String tableNo = a.get(1);
				String seatCount = a.get(2);
				String category = a.get(3);
				String position = a.get(4);
				Seating s = new Seating(name, tableNo, seatCount);
				s.category = category;
				s.position = position;
				s.line = line;

				seating.add(s);
			} catch (Exception e) {
				System.out.println("Exception in parsing an entry in " + seatingFile + " - " + line);
			}
		}
		cbr.close();
		return seating;

	}

	private static void mergeAllGuests() throws Exception {
		String guests = "all_guests_except_donors.csv";
		String donors = "Gold_donors_no_1k_with_urn_v1.csv";

		List<Donor> guestsDonors = getDonors(guests, 3, 2);
		List<Donor> donorsDonors = getDonors(donors, 3, 2);

		List<Donor> mergedDonors = new ArrayList<Donor>();

		mergeList(mergedDonors, guestsDonors);
		mergeList(mergedDonors, donorsDonors);
		displayDonors(mergedDonors);

	}

	private static void mergeAllGuestsExceptDonors() throws Exception {
		// static String rootDirectory = "/Users/sivad/Google Drive/TTA_Registrations/";
		String registrations = "registrations.csv";
		String kalyanam = "kalyanam.csv";
		String freeTickets = "WATA_General_Tickets.csv";
		String volunteers = "volunteers_May_22_v1.csv";
		String portland = "Portland All Users List.csv";
		String donors1K = "donors_1K.csv";
		List<Donor> registeredDonors = getDonors(registrations, 0, 1);
		List<Donor> kalyanamDonors = getDonors(kalyanam, 3, 2);
		List<Donor> freeDonors = getDonors(freeTickets, 2, 3);
		List<Donor> volunteersDonors = getDonors(volunteers, 3, 2);
		List<Donor> portlandDonors = getDonors(portland, 3, 2);
		List<Donor> donors1KDonors = getDonors(donors1K, 0, 3);

		List<Donor> mergedDonors = new ArrayList<Donor>();

		mergeList(mergedDonors, registeredDonors);
		mergeList(mergedDonors, kalyanamDonors);
		mergeList(mergedDonors, freeDonors);
		mergeList(mergedDonors, volunteersDonors);
		mergeList(mergedDonors, portlandDonors);
		mergeList(mergedDonors, donors1KDonors);
		displayDonors(mergedDonors);

	}

	private static void displayDonors(List<Donor> mergedDonors) {
		System.out.println("sno,countrycode,number,name,title");
		Collections.sort(mergedDonors, new DonorNameComparator());
		int counter = 0;
		for (Donor d : mergedDonors) {
			System.out.println(counter + "," + d.countryCode + "," + d.phone + "," + d.name + ",Garu");
			counter++;
		}
	}

	public static void mergeList(List<Donor> master, List<Donor> child) {
		for (Donor c : child) {
			addDonor(master, c);
		}

	}

	public static List<Donor> getDonors(String file, int nameLocation, int phoneLocation) throws Exception {
		BufferedReader cbr = new BufferedReader(new FileReader(rootDirectory + file));
		ArrayList<Donor> donors = new ArrayList<Donor>();
		while (true) {
			String line = cbr.readLine();
			if (line == null)
				break;
			try {
				String a[] = line.split(",");
				String name = stripBracketsAndReplaceComma(SheetsQuickStart.capitalize(a[nameLocation]));
				String phone = VolunteerParser.cleanPhone(a[phoneLocation]);
				if (phone.length() != 10) {
					continue;
				}
				Donor d = new Donor(name, phone);
				// System.out.println(name + " -> "+phone);
				donors.add(d);
			} catch (Exception e) {
				// System.out.println("Exception in parsing an entry in " + file + " - " +
				// nameLocation + " - "+ phoneLocation+" - " + line);
			}
		}
		cbr.close();
		return donors;

	}

	private static void createMergedDonorsWithURN() throws Exception {
		String aa = "https://wa.me/+17868820882?text=TTA-Infra:WelcomeKitReceived-";
		String bb = "https://wa.me/+17868820882?text=TTA-Infra:RegistrationKitReceived-";
		String clean = "/Users/sivad/Downloads/merged_donors.csv";
		String all_donors = "/Users/sivad/Downloads/all_donors_with_unique_ids.csv";
		BufferedReader cbr = new BufferedReader(new FileReader(clean));
		BufferedReader abr = new BufferedReader(new FileReader(all_donors));
		List<Donor> donors = new ArrayList<Donor>();
		System.out.println("sno,countrycode,number,name,title,URN,donortype,WelcomeKit,RegistrationKit");
		while (true) {
			String line = cbr.readLine();
			if (line == null)
				break;

			String a[] = line.split(",");
			Donor d = new Donor(a[0], a[1]);
			d.type = a[2];
			addDonor(donors, d);
		}
		abr.readLine();
		TreeMap<String, String> phoneURN = new TreeMap<String, String>();
		while (true) {
			String line = abr.readLine();
			if (line == null)
				break;

			String a[] = line.split(",");
			String number = a[2];
			String urn = a[5];
			phoneURN.put(number, urn);
		}
		Collections.sort(donors, new DonorNameComparator());
		int counter = 1;
		for (Donor d : donors) {
			String urn = phoneURN.get(d.phone);
			if (urn == null) {
				urn = createURN(d.phone);
			}
			d.URN = urn;

			// "sno,countrycode,phone,name,UniqueId";
			// System.out.println("sno,countrycode,number,name,title,URN,donortype");
			String aax = aa + d.URN;
			String bbx = bb + d.URN;
			System.out.println(
					counter + ",1," + d.phone + "," + d.name + ",Garu," + d.URN + "," + d.type + "," + aax + "," + bbx);
			counter++;
		}

	}

	private static String createURN(String phone) {
		return TTA.createURN(phone);
	}

	private static void missingDonors() throws FileNotFoundException, IOException {
		String all = "/Users/sivad/Downloads/all_donors_with_or_without_phones.csv";
		String clean = "/Users/sivad/Downloads/merged_donors.csv";
		BufferedReader allBR = new BufferedReader(new FileReader(all));
		BufferedReader cleanBR = new BufferedReader(new FileReader(clean));
		List<Donor> donors = new ArrayList<Donor>();
		while (true) {
			String line = cleanBR.readLine();
			if (line == null)
				break;

			String a[] = line.split(",");
			Donor d = new Donor(a[0], a[1]);
			addDonor(donors, d);
		}
		TreeSet<String> names = new TreeSet<String>();
		while (true) {
			String line = allBR.readLine();
			if (line == null)
				break;

			String a[] = line.split(",");
			String name = a[0];
			String phone = a[1];
			findIfDoesntExistInCleanFile(name, phone, donors, names);
		}
		for (String s : names) {
			System.out.println(s);
		}
	}

	private static void findIfDoesntExistInCleanFile(String name, String phone, List<Donor> donors, Set<String> names) {
		for (Donor d : donors) {
			if (d.name.equals(name)) {
				return;
			}
			if (d.phone.equals(phone)) {
				return;
			}
		}
		names.add(name);

	}

	public static String stripBracketsAndReplaceComma(String x) {
		x = x.replace(",", "&");

		int y = x.indexOf("(");
		if (y == -1) {
			return x;
		}
		return x.substring(0, y).trim();

	}

	public static void mergeDonors() throws Exception {
		String sahodar = "/Users/sivad/Downloads/sahodar_donors.csv";
		// String all_donors = "/Users/sivad/Google
		// Drive/TTA_Registrations/all_donors_with_unique_ids.csv";
		String all_donors = "/Users/sivad/Downloads/all_contacts_from_hospitality.csv";
		BufferedReader sbr = new BufferedReader(new FileReader(sahodar));
		BufferedReader abr = new BufferedReader(new FileReader(all_donors));
		List<Donor> donors = new ArrayList<Donor>();
		while (true) {
			String line = sbr.readLine();
			if (line == null)
				break;

			String a[] = line.split(",");
			Donor d = new Donor(a[0], a[1]);
			d.type = a[2];
			addDonor(donors, d);
		}
		abr.readLine();
		while (true) {
			String line = sbr.readLine();
			if (line == null)
				break;

			String a[] = line.split(",");
			Donor d = new Donor(a[3], a[2]);
			// d.type =
			addDonor(donors, d);
		}
		Collections.sort(donors, new DonorNameComparator());
		for (Donor d : donors) {
			System.out.println(d.name + "," + d.phone + "," + d.type);
		}

	}

	private static void addDonor(List<Donor> donors, Donor donor) {
		if (donor.phone.length() != 10) {
			return;
		}
		for (Donor x : donors) {
			if (x.phone.equals(donor.phone)) {
				return;
			}

		}
		donors.add(donor);
	}

}

class DonorNameComparator implements Comparator<Donor> {
	@Override
	public int compare(Donor d1, Donor d2) {
		return d1.name.compareTo(d2.name);
	}
}

class Donor {
	String name;
	String phone;
	String URN;
	String type;
	String countryCode = "1";
	List<Seating> seating = new ArrayList<Seating>();

	Donor(String name, String phone) {
		this.name = name;
		this.phone = phone;
	}
}
