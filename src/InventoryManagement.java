import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

public class InventoryManagement {

    private static final String URL =
            "jdbc:mysql://localhost:3306/retail_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Dev@%1001";

    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        try (Connection connection =
                     DriverManager.getConnection(URL, DB_USER, DB_PASSWORD)) {

            System.out.println("Connected to retail_db successfully.");

            int choice;
            do {
                showMenu();
                choice = readInt("Enter your choice: ");

                try {
                    switch (choice) {
                        case 1:
                            insertThreeProducts(connection);
                            break;
                        case 2:
                            retrieveLowStockByCategory(connection);
                            break;
                        case 3:
                            addStock(connection);
                            break;
                        case 4:
                            updatePrice(connection);
                            break;
                        case 5:
                            showInventoryValueAndDelete(connection);
                            break;
                        case 0:
                            System.out.println("Exiting program.");
                            break;
                        default:
                            System.out.println("Invalid choice. Try again.");
                    }
                } catch (SQLException e) {
                    System.out.println("Database operation failed: " + e.getMessage());
                }
            } while (choice != 0);

        } catch (SQLException e) {
            System.out.println("Could not connect to MySQL: " + e.getMessage());
            System.out.println("Check that MySQL is running and your URL, username, and password are correct.");
        }
    }

    private static void showMenu() {
        System.out.println("\n===== Inventory Management =====");
        System.out.println("1. Insert three sample products");
        System.out.println("2. Find category products with quantity below 20");
        System.out.println("3. Add stock to a product");
        System.out.println("4. Search product and update its price");
        System.out.println("5. Show inventory value and delete zero-stock product");
        System.out.println("0. Exit");
    }

    private static void insertThreeProducts(Connection connection)
            throws SQLException {
        String sql = "INSERT INTO Product "
                + "(product_id, product_name, category, quantity, price) "
                + "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            addProduct(statement, 101, "Keyboard", "Electronics", 15, 1200.0);
            addProduct(statement, 102, "Notebook", "Stationery", 30, 50.0);
            addProduct(statement, 103, "Mouse", "Electronics", 10, 700.0);

            int[] results = statement.executeBatch();

            for (int result : results) {
                if (result > 0 || result == PreparedStatement.SUCCESS_NO_INFO) {
                    System.out.println("Product inserted successfully.");
                } else {
                    System.out.println("Product was not inserted.");
                }
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                System.out.println("A product ID already exists. Duplicate IDs are not inserted.");
            } else {
                throw e;
            }
        }
    }

    private static void addProduct(PreparedStatement statement, int id,
            String name, String category, int quantity, double price)
            throws SQLException {
        statement.setInt(1, id);
        statement.setString(2, name);
        statement.setString(3, category);
        statement.setInt(4, quantity);
        statement.setDouble(5, price);
        statement.addBatch();
    }

    private static void retrieveLowStockByCategory(Connection connection)
            throws SQLException {
        System.out.print("Enter category: ");
        String category = scanner.nextLine();

        String sql = "SELECT * FROM Product "
                + "WHERE category = ? AND quantity < 20 "
                + "ORDER BY price DESC";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, category);

            try (ResultSet rs = statement.executeQuery()) {
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    printProduct(rs);
                }
                if (!found) {
                    System.out.println("No matching products found.");
                }
            }
        }
    }

    private static void addStock(Connection connection) throws SQLException {
        int id = readInt("Enter product ID: ");
        int amount = readInt("Enter quantity to add: ");

        if (amount <= 0) {
            System.out.println("Quantity to add must be greater than zero.");
            return;
        }

        String sql = "UPDATE Product SET quantity = quantity + ? "
                + "WHERE product_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, amount);
            statement.setInt(2, id);

            int rows = statement.executeUpdate();
            if (rows > 0) {
                System.out.println("Stock updated successfully.");
            } else {
                System.out.println("Product does not exist.");
            }
        }
    }

    private static void updatePrice(Connection connection) throws SQLException {
        int id = readInt("Enter product ID: ");
        double newPrice = readDouble("Enter new price: ");

        if (newPrice <= 0) {
            System.out.println("Price must be greater than zero.");
            return;
        }

        String searchSql = "SELECT * FROM Product WHERE product_id = ?";

        try (PreparedStatement search =
                     connection.prepareStatement(searchSql)) {
            search.setInt(1, id);

            try (ResultSet rs = search.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("Product does not exist.");
                    return;
                }
            }
        }

        String updateSql = "UPDATE Product SET price = ? WHERE product_id = ?";

        try (PreparedStatement update =
                     connection.prepareStatement(updateSql)) {
            update.setDouble(1, newPrice);
            update.setInt(2, id);
            update.executeUpdate();
        }

        String updatedSql = "SELECT * FROM Product WHERE product_id = ?";
        try (PreparedStatement statement =
                     connection.prepareStatement(updatedSql)) {
            statement.setInt(1, id);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    System.out.println("Updated product details:");
                    printProduct(rs);
                }
            }
        }
    }

    private static void showInventoryValueAndDelete(Connection connection)
            throws SQLException {
        String sumSql = "SELECT SUM(quantity * price) AS inventory_value "
                + "FROM Product";

        try (PreparedStatement statement =
                     connection.prepareStatement(sumSql);
             ResultSet rs = statement.executeQuery()) {
            if (rs.next()) {
                System.out.printf("Total inventory value: %.2f%n",
                        rs.getDouble("inventory_value"));
            }
        }

        int id = readInt("Enter product ID to delete: ");
        String deleteSql = "DELETE FROM Product "
                + "WHERE product_id = ? AND quantity = 0";

        try (PreparedStatement statement =
                     connection.prepareStatement(deleteSql)) {
            statement.setInt(1, id);

            int rows = statement.executeUpdate();
            if (rows > 0) {
                System.out.println("Zero-stock product deleted successfully.");
            } else {
                System.out.println(
                        "Product was not deleted. It may not exist or its quantity is not zero.");
            }
        }
    }

    private static void printProduct(ResultSet rs) throws SQLException {
        System.out.printf(
                "ID: %d | Name: %s | Category: %s | Quantity: %d | Price: %.2f%n",
                rs.getInt("product_id"),
                rs.getString("product_name"),
                rs.getString("category"),
                rs.getInt("quantity"),
                rs.getDouble("price"));
    }

    private static int readInt(String prompt) {
        System.out.print(prompt);
        return Integer.parseInt(scanner.nextLine());
    }

    private static double readDouble(String prompt) {
        System.out.print(prompt);
        return Double.parseDouble(scanner.nextLine());
    }
}