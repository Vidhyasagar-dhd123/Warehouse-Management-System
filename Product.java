import java.time.LocalDate;
import java.util.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class Product {
	// Product model and related operations
	private final String id;
	private int stock;
    private String name;
	private int threshold;
	private BigDecimal paymentDue = BigDecimal.ZERO;
	private final List<LocalDate> shipmentDates = new ArrayList<>();
	private final List<String> shippers = new ArrayList<>();

	public Product(String id, int initialStock, int threshold, String name) {
		if (id == null) throw new IllegalArgumentException("id cannot be null");
		this.id = id;
		this.stock = Math.max(0, initialStock);
		this.threshold = Math.max(0, threshold);
        this.name = name != null ? name : "Unnamed Product";
	}

	// Increase stock and record shipment metadata and cost
	public synchronized void addShipment(int quantity, LocalDate shipmentDate, String shipper, BigDecimal cost) {
		if (quantity <= 0) throw new IllegalArgumentException("quantity must be positive");
		this.stock += quantity;
		if (shipmentDate != null) shipmentDates.add(shipmentDate);
		shippers.add(shipper != null ? shipper : "Unknown");
		if (cost != null) paymentDue = paymentDue.add(cost).setScale(2, RoundingMode.HALF_UP);
	}

	// Decrease stock when delivering to customers/other warehouses
	// Returns true if delivery succeeded, false if insufficient stock (no change)
	public synchronized boolean addDelivery(int quantity) {
		if (quantity <= 0) throw new IllegalArgumentException("quantity must be positive");
		if (quantity > stock) return false;
		stock -= quantity;
		return true;
	}

	public synchronized boolean isBelowThreshold() {
		return stock < threshold;
	}

	public synchronized BigDecimal getPaymentDue() {
		return paymentDue.setScale(2, RoundingMode.HALF_UP);
	}

	// Pay amount against paymentDue. Returns remaining due.
	public synchronized BigDecimal pay(BigDecimal amount) {
		if (amount == null) throw new IllegalArgumentException("amount cannot be null");
		if (amount.signum() < 0) throw new IllegalArgumentException("amount cannot be negative");
		paymentDue = paymentDue.subtract(amount);
		if (paymentDue.signum() < 0) paymentDue = BigDecimal.ZERO;
		return paymentDue.setScale(2, RoundingMode.HALF_UP);
	}

	// Simple accessors
	public String getId() { return id; }
	public synchronized int getStock() { return stock; }
	public synchronized int getThreshold() { return threshold; }
	public synchronized void setThreshold(int threshold) { this.threshold = Math.max(0, threshold); }

	public synchronized List<LocalDate> getShipmentDates() { return new ArrayList<>(shipmentDates); }
	public synchronized List<String> getShippers() { return new ArrayList<>(shippers); }

	// new name accessors
	public synchronized String getName() { return name; }
	public synchronized void setName(String name) { this.name = name != null ? name : "Unnamed Product"; }

	@Override
	public synchronized String toString() {
		return "Product{id='" + id + '\'' +
			", name='" + name + '\'' +
			", stock=" + stock +
			", threshold=" + threshold +
			", paymentDue=" + paymentDue.setScale(2, RoundingMode.HALF_UP) +
			", shipments=" + shipmentDates.size() +
			", shippers=" + shippers +
			'}';
	}
}