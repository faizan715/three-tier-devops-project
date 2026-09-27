import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;

import java.net.InetSocketAddress;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

public class App {

    private static final String DB_URL =
            "jdbc:mysql://database:3306/mydb";

    private static final String DB_USER = "root";

    private static final String DB_PASSWORD = "root";

    public static void main(String[] args) throws Exception {

        Class.forName("com.mysql.cj.jdbc.Driver");

        String createTable =
                "CREATE TABLE IF NOT EXISTS employees (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "name VARCHAR(100)," +
                "email VARCHAR(100)," +
                "designation VARCHAR(100))";

        try (Connection connection =
                     DriverManager.getConnection(
                             DB_URL,
                             DB_USER,
                             DB_PASSWORD
                     )) {

            connection.createStatement().execute(createTable);
        }

        HttpServer server = HttpServer.create(
                new InetSocketAddress(8080),
                0
        );

        server.createContext("/", new HttpHandler() {

            @Override
            public void handle(HttpExchange exchange) {

                try {

                    if (exchange.getRequestMethod()
                            .equalsIgnoreCase("POST")) {

                        BufferedReader reader =
                                new BufferedReader(
                                        new InputStreamReader(
                                                exchange.getRequestBody()
                                        )
                                );

                        StringBuilder body =
                                new StringBuilder();

                        String line;

                        while ((line = reader.readLine()) != null) {
                            body.append(line);
                        }

                        String requestBody =
                                body.toString();

                        String name =
                                requestBody
                                        .split("\"name\":\"")[1]
                                        .split("\"")[0];

                        String email =
                                requestBody
                                        .split("\"email\":\"")[1]
                                        .split("\"")[0];

                        String designation =
                                requestBody
                                        .split("\"designation\":\"")[1]
                                        .split("\"")[0];

                        String sql =
                                "INSERT INTO employees" +
                                "(name, email, designation) " +
                                "VALUES (?, ?, ?)";

                        try (Connection connection =
                                     DriverManager.getConnection(
                                             DB_URL,
                                             DB_USER,
                                             DB_PASSWORD
                                     );
                             PreparedStatement statement =
                                     connection.prepareStatement(sql)) {

                            statement.setString(1, name);
                            statement.setString(2, email);
                            statement.setString(3, designation);

                            statement.executeUpdate();
                        }

                        String response =
                                "Employee Saved Successfully";

                        exchange.sendResponseHeaders(
                                200,
                                response.length()
                        );

                        try (OutputStream os =
                                     exchange.getResponseBody()) {

                            os.write(response.getBytes());
                        }

                    } else {

                        String response =
                                "Method Not Allowed";

                        exchange.sendResponseHeaders(
                                405,
                                response.length()
                        );

                        try (OutputStream os =
                                     exchange.getResponseBody()) {

                            os.write(response.getBytes());
                        }
                    }

                } catch (Exception e) {

                    e.printStackTrace();

                    try {

                        String response =
                                e.getMessage() != null
                                        ? e.getMessage()
                                        : "Internal Server Error";

                        exchange.sendResponseHeaders(
                                500,
                                response.length()
                        );

                        try (OutputStream os =
                                     exchange.getResponseBody()) {

                            os.write(response.getBytes());
                        }

                    } catch (Exception ex) {

                        ex.printStackTrace();
                    }
                }
            }
        });

        server.start();

        System.out.println(
                "Backend running on port 8080"
        );
    }
}
