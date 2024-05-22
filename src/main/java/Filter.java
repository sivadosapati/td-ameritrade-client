//https://leetcode.com/problems/capitalize-the-title/description/

//https://leetcode.com/problems/lru-cache/description/



import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/*
 * Class to manage filters that are displayable in UI, Filters are like label and count that you see on the website when you are searching for options in Search Result
 * 
 * 
 * Your job as a code reviewer is to review FilterManager class. It has 2 functionalities
 * 1 - Add Filter (This will add Filter to the the list of Filters), each filter will have a label, and the count should be increased when adding a filter
 * 2 - Get displayable filters - This will return the top 5 filters from the list in the descending order of occcurence
 * 
 * For ref: Look at this website : https://www.abelgm.com/VehicleSearchResults
 */
class FilterManager {
	private List<Filter> filters = null;

	// Method to add different filters
	public void addFilter(String label) {
		Filter f = getFilter(label);
		if (f == null) {
			filters.add(new Filter(label));
		} else {
			f.count++;
		}
	}

	// Sort in the descending order or occurrence and return only top 5 filters
	public List<Filter> getDisplayableFilters() {
		TreeSet<Filter> sortableResults = new TreeSet<Filter>(filters);
		List<Filter> results = new ArrayList<Filter>();
		int counter = 0;
		for (Filter f : filters) {
			if (counter == 5)
				break;
			results.add(f);
		}
		return results;
	}

	private Filter getFilter(String label) {
		for (Filter f : filters) {
			if (f.label.equals(label)) {
				return f;
			}

		}
		return null;
	}

}

class Filter implements Comparable {
	String label;
	int count;

	Filter(String label) {
		this.label = label;
	}

	@Override
	public int compareTo(Object o) {
		Filter x = (Filter) o;
		return x.count - this.count;
	}
}
