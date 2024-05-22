
public class InventoryManagementSystem {

	private int availableQuantity;

	public InventoryManagementSystem(int initialQuantity) {
		this.availableQuantity = initialQuantity;
	}

	// Intentional bug: Incorrectly decrementing availableQuantity instead of
	// incrementing
	public boolean sellProduct(int quantityRequested) {
		if (quantityRequested <= 0) {
			System.out.println("Invalid quantity requested.");
			return false;
		}

		if (quantityRequested > availableQuantity) {
			System.out.println("Insufficient quantity in stock.");
			return false;
		}

		availableQuantity -= quantityRequested; // Bug: Should be availableQuantity += quantityRequested;
		System.out.println("Product sold successfully.");
		return true;
	}

	// Intentional bug: No validation for negative quantityAdded
	public void addStock(int quantityAdded) {
		availableQuantity += quantityAdded;
		System.out.println("Stock added successfully.");
	}

	// Intentional bug: Incorrectly returning negative quantity
	public int checkStock() {
		return -availableQuantity; // Bug: Should be return availableQuantity;
	}
	
}

class IMS {
    
    private int availableQuantity;

    public IMS(int initialQuantity) {
        this.availableQuantity = initialQuantity;
    }

    
    public boolean sellProduct(int quantityRequested) {
        if (quantityRequested <= 0) {
            System.out.println("Invalid quantity requested.");
            return false;
        }
        
        if (quantityRequested > availableQuantity) {
            System.out.println("Insufficient quantity in stock.");
            return false;
        }
        
        availableQuantity -= quantityRequested; // Bug: Should be availableQuantity += quantityRequested;
        System.out.println("Product sold successfully.");
        return true;
    }

    // Intentional bug: No validation for negative quantityAdded
    public void addStock(int quantityAdded) {
        availableQuantity += quantityAdded;
        System.out.println("Stock added successfully.");
    }

    // Intentional bug: Incorrectly returning negative quantity
    public int checkStock() {
        return -availableQuantity; // Bug: Should be return availableQuantity;
    }
}




