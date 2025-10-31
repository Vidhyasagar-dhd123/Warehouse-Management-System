import java.time.LocalDate;
import java.util.*;
import java.math.BigDecimal;

public class Products {
	// Product model moved to Product.java

	// Simple inventory manager
	private final Map<String, Product> inventory = new HashMap<>();

	// Register a product (will replace existing with same id)
	public synchronized Product registerProduct(String id, int initialStock, int threshold, String name) {
		Product p = new Product(id, initialStock, threshold, name);
		inventory.put(id, p);
		return p;
	}

	public synchronized Optional<Product> findProduct(String id) {
		return Optional.ofNullable(inventory.get(id));
	}

	// Convenience methods: operate by product id
	public synchronized boolean receiveShipment(String id, int qty, LocalDate date, String shipper, BigDecimal cost) {
		Product p = inventory.get(id);
		if (p == null) return false;
		p.addShipment(qty, date, shipper, cost);
		return true;
	}

	public synchronized boolean deliver(String id, int qty) {
		Product p = inventory.get(id);
		if (p == null) return false;
		return p.addDelivery(qty);
	}

	// New utility methods

	// Return a snapshot list of all registered products
	public synchronized List<Product> listAllProducts() {
		return new ArrayList<>(inventory.values());
	}

	// Remove a product by id, return true if removed
	public synchronized boolean removeProduct(String id) {
		return inventory.remove(id) != null;
	}

	// Return products currently below their threshold
	public synchronized List<Product> getLowStockProducts() {
		List<Product> low = new ArrayList<>();
		for (Product p : inventory.values()) {
			if (p.isBelowThreshold()) low.add(p);
		}
		return low;
	}

	// Pay an amount toward a product's paymentDue. Returns remaining due if product found.
	public synchronized Optional<BigDecimal> payForProduct(String id, BigDecimal amount) {
		Product p = inventory.get(id);
		if (p == null) return Optional.empty();
		return Optional.ofNullable(p.pay(amount));
	}

	// Get number of registered products
	public synchronized int size() {
		return inventory.size();
	}

}