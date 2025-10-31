import java.util.*;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        Warehouse warehouse = new Warehouse();
        Backup backup = new Backup(warehouse.getProducts());
        Scanner in = new Scanner(System.in);
        System.out.println("Warehouse CLI. Type 'help' for commands.");

        while (true) {
            System.out.print("> ");
            String line = in.hasNextLine() ? in.nextLine().trim() : "";
            if (line.isEmpty()) continue;
            String[] parts = splitArgs(line);
            String cmd = parts[0].toLowerCase(Locale.ROOT);

            try {
                switch (cmd) {
                    case "exit":
                    case "quit":
                        System.out.println("Bye.");
                        in.close();
                        return;
                    case "help":
                        printHelp();
                        break;
                    case "add":
                        // add <id> <initialStock> <threshold> [name]
                        if (parts.length < 4) { System.out.println("Usage: add <id> <initialStock> <threshold> [name]"); break; }
                        String id = parts[1];
                        int stock = Integer.parseInt(parts[2]);
                        int thr = Integer.parseInt(parts[3]);
                        String name = parts.length >= 5 ? parts[4] : null;
                        Product added = warehouse.addProduct(id, stock, thr, name);
                        System.out.println("Added: " + added);
                        break;
                    case "remove":
                        // remove <id>
                        if (parts.length < 2) { System.out.println("Usage: remove <id>"); break; }
                        System.out.println(warehouse.removeProduct(parts[1]) ? "Removed." : "Not found.");
                        break;
                    case "receive":
                        // receive <id> <qty> <date|today> <shipper> <cost>
                        if (parts.length < 6) { System.out.println("Usage: receive <id> <qty> <date|today> <shipper> <cost>"); break; }
                        id = parts[1];
                        int qty = Integer.parseInt(parts[2]);
                        LocalDate date = parts[3].equalsIgnoreCase("today") ? LocalDate.now() : LocalDate.parse(parts[3]);
                        String shipper = parts[4];
                        BigDecimal cost = new BigDecimal(parts[5]);
                        System.out.println(warehouse.receiveShipment(id, qty, date, shipper, cost) ? "Shipment recorded." : "Product not found.");
                        break;
                    case "deliver":
                        // deliver <id> <qty>
                        if (parts.length < 3) { System.out.println("Usage: deliver <id> <qty>"); break; }
                        id = parts[1];
                        qty = Integer.parseInt(parts[2]);
                        System.out.println(warehouse.deliverProduct(id, qty) ? "Delivered." : "Insufficient stock or product not found.");
                        break;
                    case "pay":
                        // pay <id> <amount>
                        if (parts.length < 3) { System.out.println("Usage: pay <id> <amount>"); break; }
                        id = parts[1];
                        BigDecimal amt = new BigDecimal(parts[2]);
                        warehouse.paySupplier(id, amt).ifPresentOrElse(
                            rem -> System.out.println("Remaining due: " + rem.toPlainString()),
                            () -> System.out.println("Product not found.")
                        );
                        break;
                    case "list":
                        List<Product> all = warehouse.listProducts();
                        if (all.isEmpty()) System.out.println("(no products)");
                        else for (Product p : all) System.out.println(p);
                        break;
                    case "low":
                        List<Product> low = warehouse.lowStockProducts();
                        if (low.isEmpty()) System.out.println("All products at or above threshold.");
                        else for (Product p : low) System.out.println(p);
                        break;
                    case "find":
                        if (parts.length < 2) { System.out.println("Usage: find <id>"); break; }
                        warehouse.findProduct(parts[1]).ifPresentOrElse(
                            p -> System.out.println(p),
                            () -> System.out.println("Not found.")
                        );
                        break;
                    case "size":
                        System.out.println("Inventory size: " + warehouse.inventorySize());
                        break;
                    case "exportall":
                        // exportall <dir> <prefix>
                        if (parts.length < 3) { System.out.println("Usage: exportall <dir> <prefix>"); break; }
                        Path dir = Paths.get(parts[1]);
                        String prefix = parts[2];
                        try { backup.exportAll(dir, prefix); System.out.println("Exported to " + dir.toAbsolutePath()); }
                        catch (IOException e) { System.out.println("Export failed: " + e.getMessage()); }
                        break;
                    case "exportcsv":
                        if (parts.length < 2) { System.out.println("Usage: exportcsv <file>"); break; }
                        try { backup.exportCSV(Paths.get(parts[1])); System.out.println("Exported CSV."); }
                        catch (IOException e) { System.out.println("Export failed: " + e.getMessage()); }
                        break;
                    case "exportjson":
                        if (parts.length < 2) { System.out.println("Usage: exportjson <file>"); break; }
                        try { backup.exportJSON(Paths.get(parts[1])); System.out.println("Exported JSON."); }
                        catch (IOException e) { System.out.println("Export failed: " + e.getMessage()); }
                        break;
                    case "exportxml":
                        if (parts.length < 2) { System.out.println("Usage: exportxml <file>"); break; }
                        try { backup.exportXML(Paths.get(parts[1])); System.out.println("Exported XML."); }
                        catch (IOException e) { System.out.println("Export failed: " + e.getMessage()); }
                        break;
                    case "importcsv":
                        if (parts.length < 2) { System.out.println("Usage: importcsv <file>"); break; }
                        try { backup.importCSV(Paths.get(parts[1])); System.out.println("Imported CSV."); }
                        catch (IOException e) { System.out.println("Import failed: " + e.getMessage()); }
                        break;
                    case "importjson":
                        if (parts.length < 2) { System.out.println("Usage: importjson <file>"); break; }
                        try { backup.importJSON(Paths.get(parts[1])); System.out.println("Imported JSON."); }
                        catch (IOException e) { System.out.println("Import failed: " + e.getMessage()); }
                        break;
                    case "importxml":
                        if (parts.length < 2) { System.out.println("Usage: importxml <file>"); break; }
                        try { backup.importXML(Paths.get(parts[1])); System.out.println("Imported XML."); }
                        catch (IOException e) { System.out.println("Import failed: " + e.getMessage()); }
                        break;
                    default:
                        System.out.println("Unknown command. Type 'help' for list.");
                        break;
                }
            } catch (Exception ex) {
                System.out.println("Error: " + ex.getMessage());
            }
        }
    }

    private static void printHelp() {
        System.out.println("Commands:");
        System.out.println("  help");
        System.out.println("  add <id> <initialStock> <threshold> [name]   (optional name, use quotes for spaces)");
        System.out.println("    Example: add P123 10 3 \"Red Widget\"");
        System.out.println("  remove <id>");
        System.out.println("  receive <id> <qty> <date|today> <shipper> <cost>");
        System.out.println("  deliver <id> <qty>");
        System.out.println("  pay <id> <amount>");
        System.out.println("  list");
        System.out.println("  low");
        System.out.println("  find <id>");
        System.out.println("  size");
        System.out.println("  exportall <dir> <prefix>");
        System.out.println("  exportcsv <file>");
        System.out.println("  exportjson <file>");
        System.out.println("  exportxml <file>");
        System.out.println("  importcsv <file>");
        System.out.println("  importjson <file>");
        System.out.println("  importxml <file>");
        System.out.println("  exit");
    }

    // naive argument splitter: preserves tokens, treats quoted tokens as one
    private static String[] splitArgs(String line) {
        List<String> out = new ArrayList<>();
        boolean inQuote = false;
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') { inQuote = !inQuote; continue; }
            if (!inQuote && Character.isWhitespace(c)) {
                if (cur.length() > 0) { out.add(cur.toString()); cur.setLength(0); }
            } else {
                cur.append(c);
            }
        }
        if (cur.length() > 0) out.add(cur.toString());
        return out.toArray(new String[0]);
    }
}