import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.math.BigDecimal;
import java.lang.reflect.Field;

public class Backup {
	private final Products products;

	public Backup(Products products) {
		if (products == null) throw new IllegalArgumentException("products cannot be null");
		this.products = products;
	}

	// Export all formats into given directory (creates files <prefix>_csv,json,xml)
	public void exportAll(Path dir, String prefix) throws IOException {
		Files.createDirectories(dir);
		exportCSV(dir.resolve(prefix + ".csv"));
		exportJSON(dir.resolve(prefix + ".json"));
		exportXML(dir.resolve(prefix + ".xml"));
	}

	// CSV format: header then lines: id,name,stock,threshold,paymentDue,shipmentDates(pipe-separated ISO),shippers(pipe-separated)
	public void exportCSV(Path file) throws IOException {
		List<Product> all = products.listAllProducts();
		try (BufferedWriter w = Files.newBufferedWriter(file)) {
			w.write("id,name,stock,threshold,paymentDue,shipmentDates,shippers");
			w.newLine();
			for (Product p : all) {
				String dates = String.join("|", p.getShipmentDates().stream().map(LocalDate::toString).toArray(String[]::new));
				String shippers = String.join("|", p.getShippers());
				String line = String.format("%s,%s,%d,%d,%s,%s,%s",
					escapeCSV(p.getId()), escapeCSV(p.getName()), p.getStock(), p.getThreshold(),
					p.getPaymentDue().toPlainString(), escapeCSV(dates), escapeCSV(shippers));
				w.write(line);
				w.newLine();
			}
		}
	}

	public void importCSV(Path file) throws IOException {
		try (BufferedReader r = Files.newBufferedReader(file)) {
			String header = r.readLine(); // skip header
			String line;
			while ((line = r.readLine()) != null) {
				// naive CSV parse compatible with exportCSV (no commas inside fields except handled minimally)
				String[] parts = splitCSVLine(line, 7);
				if (parts.length < 7) continue;
				String id = unescapeCSV(parts[0]);
				String name = unescapeCSV(parts[1]);
				int stock = Integer.parseInt(parts[2]);
				int threshold = Integer.parseInt(parts[3]);
				BigDecimal paymentDue = new BigDecimal(parts[4]);
				String dates = unescapeCSV(parts[5]);
				String shippers = unescapeCSV(parts[6]);

				Product p = products.registerProduct(id, stock, threshold, name);
				setProductInternals(p, paymentDue, dates, shippers);
			}
		} catch (ReflectiveOperationException e) {
			throw new IOException("Failed to set product internals", e);
		}
	}

	// JSON export/import (simple, no external libs). Exports an array of objects created by exportJSON.
	public void exportJSON(Path file) throws IOException {
		List<Product> all = products.listAllProducts();
		try (BufferedWriter w = Files.newBufferedWriter(file)) {
			w.write("[");
			boolean first = true;
			for (Product p : all) {
				if (!first) w.write(",");
				first = false;
				String datesJson = toJsonArray(p.getShipmentDates().stream().map(LocalDate::toString).toArray(String[]::new));
				String shippersJson = toJsonArray(p.getShippers().toArray(new String[0]));
				w.write("{");
				w.write("\"id\":\"" + jsonEscape(p.getId()) + "\",");
				w.write("\"name\":\"" + jsonEscape(p.getName()) + "\",");
				w.write("\"stock\":" + p.getStock() + ",");
				w.write("\"threshold\":" + p.getThreshold() + ",");
				w.write("\"paymentDue\":\"" + p.getPaymentDue().toPlainString() + "\",");
				w.write("\"shipmentDates\":" + datesJson + ",");
				w.write("\"shippers\":" + shippersJson);
				w.write("}");
			}
			w.write("]");
		}
	}

	public void importJSON(Path file) throws IOException {
		String content = new String(Files.readAllBytes(file));
		// Expect the file to be in the exact shape produced by exportJSON; split by "}," boundaries.
		String trimmed = content.trim();
		if (trimmed.length() < 2) return;
		trimmed = trimmed.substring(1, trimmed.length() - 1).trim(); // remove [ ]
		if (trimmed.isEmpty()) return;
		String[] objs = trimmed.split("\\},\\s*\\{");
		for (int i = 0; i < objs.length; i++) {
			String obj = objs[i];
			if (!obj.startsWith("{")) obj = "{" + obj;
			if (!obj.endsWith("}")) obj = obj + "}";
			Map<String, String> map = parseSimpleJsonObject(obj);
			String id = map.get("id");
			String name = map.getOrDefault("name", "");
			int stock = Integer.parseInt(map.getOrDefault("stock", "0"));
			int threshold = Integer.parseInt(map.getOrDefault("threshold", "0"));
			BigDecimal paymentDue = new BigDecimal(map.getOrDefault("paymentDue", "0"));
			String[] dates = parseJsonArray(map.getOrDefault("shipmentDates", "[]"));
			String[] shippers = parseJsonArray(map.getOrDefault("shippers", "[]"));
			try {
				Product p = products.registerProduct(id, stock, threshold, name);
				setProductInternals(p, paymentDue, String.join("|", dates), String.join("|", shippers));
			} catch (ReflectiveOperationException e) {
				throw new IOException(e);
			}
		}
	}

	// XML export/import (very simple)
	public void exportXML(Path file) throws IOException {
		List<Product> all = products.listAllProducts();
		try (BufferedWriter w = Files.newBufferedWriter(file)) {
			w.write("<products>");
			for (Product p : all) {
				w.write("<product>");
				w.write("<id>" + xmlEscape(p.getId()) + "</id>");
				w.write("<name>" + xmlEscape(p.getName()) + "</name>");
				w.write("<stock>" + p.getStock() + "</stock>");
				w.write("<threshold>" + p.getThreshold() + "</threshold>");
				w.write("<paymentDue>" + p.getPaymentDue().toPlainString() + "</paymentDue>");
				w.write("<shipmentDates>");
				for (LocalDate d : p.getShipmentDates()) w.write("<d>" + d.toString() + "</d>");
				w.write("</shipmentDates>");
				w.write("<shippers>");
				for (String s : p.getShippers()) w.write("<s>" + xmlEscape(s) + "</s>");
				w.write("</shippers>");
				w.write("</product>");
			}
			w.write("</products>");
		}
	}

	public void importXML(Path file) throws IOException {
		String content = new String(Files.readAllBytes(file));
		// Very simple parsing for the structure produced above
		List<String> productsXml = extractBetween(content, "<product>", "</product>");
		for (String prodXml : productsXml) {
			String id = extractFirst(prodXml, "<id>", "</id>");
            String name = extractFirst(prodXml, "<name>", "</name>");
			int stock = Integer.parseInt(extractFirst(prodXml, "<stock>", "</stock>"));
			int threshold = Integer.parseInt(extractFirst(prodXml, "<threshold>", "</threshold>"));
			BigDecimal paymentDue = new BigDecimal(extractFirst(prodXml, "<paymentDue>", "</paymentDue>"));
			List<String> dates = extractBetween(prodXml, "<d>", "</d>");
			List<String> shippers = extractBetween(prodXml, "<s>", "</s>");
			try {
				Product p = products.registerProduct(id, stock, threshold, name);
				setProductInternals(p, paymentDue, String.join("|", dates), String.join("|", shippers));
			} catch (ReflectiveOperationException e) {
				throw new IOException(e);
			}
		}
	}

	// --- Helpers ---

	private static String escapeCSV(String s) {
		if (s == null) return "";
		return s.replace("\"", "\"\"");
	}

	private static String unescapeCSV(String s) {
		if (s == null) return "";
		return s.replace("\"\"", "\"");
	}

	private static String[] splitCSVLine(String line, int expected) {
		// simple split by comma, not handling quoted commas. Works with exportCSV above.
		return line.split(",", expected);
	}

	private static String toJsonArray(String[] items) {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		boolean first = true;
		for (String it : items) {
			if (!first) sb.append(",");
			first = false;
			sb.append("\"").append(jsonEscape(it)).append("\"");
		}
		sb.append("]");
		return sb.toString();
	}

	private static String jsonEscape(String s) {
		if (s == null) return "";
		return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
	}

	private static String xmlEscape(String s) {
		if (s == null) return "";
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private static Map<String, String> parseSimpleJsonObject(String obj) {
		Map<String, String> map = new HashMap<>();
		// remove { } and split on top-level commas (naive, compatible with exportJSON)
		String core = obj.trim();
		if (core.startsWith("{")) core = core.substring(1);
		if (core.endsWith("}")) core = core.substring(0, core.length() - 1);
		String[] parts = core.split("\",?\\s*\\,\\s*\""); // crude split
		for (String part : parts) {
			part = part.trim();
			if (part.startsWith("\"")) part = part.substring(1);
			String[] kv = part.split(":", 2);
			if (kv.length != 2) continue;
			String key = kv[0].trim().replaceAll("^\"|\"$", "");
			String val = kv[1].trim();
			// trim surrounding quotes if present
			if (val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2) val = val.substring(1, val.length() - 1);
			map.put(key, val.replace("\\\"", "\"").replace("\\\\", "\\").replace("\\n", "\n").replace("\\r", "\r"));
		}
		return map;
	}

	private static String[] parseJsonArray(String arr) {
		String t = arr.trim();
		if (t.startsWith("[")) t = t.substring(1);
		if (t.endsWith("]")) t = t.substring(0, t.length() - 1);
		if (t.trim().isEmpty()) return new String[0];
		// split on "," but naive as exportJSON does not include commas inside items
		String[] parts = t.split("\",\\s*\"");
		for (int i = 0; i < parts.length; i++) {
			String s = parts[i];
			s = s.replaceAll("^\"|\"$", "");
			parts[i] = s.replace("\\\"", "\"").replace("\\\\", "\\");
		}
		return parts;
	}

	private static List<String> extractBetween(String src, String open, String close) {
		List<String> res = new ArrayList<>();
		int idx = 0;
		while (true) {
			int a = src.indexOf(open, idx);
			if (a < 0) break;
			a += open.length();
			int b = src.indexOf(close, a);
			if (b < 0) break;
			res.add(src.substring(a, b));
			idx = b + close.length();
		}
		return res;
	}

	private static String extractFirst(String src, String open, String close) {
		int a = src.indexOf(open);
		if (a < 0) return "";
		a += open.length();
		int b = src.indexOf(close, a);
		if (b < 0) return "";
		return src.substring(a, b);
	}

	// Use reflection to set private fields on Product (paymentDue, shipmentDates, shippers)
	private static void setProductInternals(Product p, BigDecimal paymentDue, String pipeDates, String pipeShippers)
		throws ReflectiveOperationException {
		Field fd = Product.class.getDeclaredField("paymentDue");
		fd.setAccessible(true);
		fd.set(p, paymentDue);

		Field fDates = Product.class.getDeclaredField("shipmentDates");
		fDates.setAccessible(true);
		List<LocalDate> dates = new ArrayList<>();
		if (pipeDates != null && !pipeDates.isEmpty()) {
			for (String s : pipeDates.split("\\|")) if (!s.isEmpty()) dates.add(LocalDate.parse(s));
		}
		fDates.set(p, dates);

		Field fSh = Product.class.getDeclaredField("shippers");
		fSh.setAccessible(true);
		List<String> sh = new ArrayList<>();
		if (pipeShippers != null && !pipeShippers.isEmpty()) {
			for (String s : pipeShippers.split("\\|")) if (!s.isEmpty()) sh.add(s);
		}
		fSh.set(p, sh);
	}
}
