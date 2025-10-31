import java.time.LocalDate;
import java.util.*;
import java.math.BigDecimal;

public class Warehouse {
	// Underlying inventory manager
	private final Products products = new Products();

	// Register or replace a product
	public Product addProduct(String id, int initialStock, int threshold, String name) {
		return products.registerProduct(id, initialStock, threshold, name);
	}

	// Remove a product by id
	public boolean removeProduct(String id) {
		return products.removeProduct(id);
	}

	// Receive a shipment for a product
	public boolean receiveShipment(String id, int qty, LocalDate date, String shipper, BigDecimal cost) {
		return products.receiveShipment(id, qty, date, shipper, cost);
	}

	// Deliver product (decrease stock)
	public boolean deliverProduct(String id, int qty) {
		return products.deliver(id, qty);
	}

	// Pay outstanding amount for a product; returns remaining due if product exists
	public Optional<BigDecimal> paySupplier(String id, BigDecimal amount) {
		return products.payForProduct(id, amount);
	}

	// Get a snapshot list of all products
	public List<Product> listProducts() {
		return products.listAllProducts();
	}

	// Get products currently below threshold
	public List<Product> lowStockProducts() {
		return products.getLowStockProducts();
	}

	// Find a product by id
	public Optional<Product> findProduct(String id) {
		return products.findProduct(id);
	}

	// Number of registered products
	public int inventorySize() {
		return products.size();
	}

	// Small textual report for low-stock items
	public String lowStockReport() {
		List<Product> low = lowStockProducts();
		if (low.isEmpty()) return "All products at or above threshold.";
		StringBuilder sb = new StringBuilder();
		sb.append("Low stock products:\n");
		for (Product p : low) {
			sb.append(String.format("- %s: stock=%d, threshold=%d%n", p.getId(), p.getStock(), p.getThreshold()));
		}
		return sb.toString();
	}

	public Products getProducts() {
		return products;
	}
}