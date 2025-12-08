package com.example;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;
import org.json.*;

public class TodoServlet extends HttpServlet {
    private Connection conn;

    @Override
    public void init() throws ServletException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            // Use environment variables if available, fallback to default
            String dbHost = System.getenv().getOrDefault("DB_HOST", "localhost");
            String dbPort = System.getenv().getOrDefault("DB_PORT", "3306");
            String dbName = System.getenv().getOrDefault("DB_NAME", "todo_db");
            String dbUser = System.getenv().getOrDefault("DB_USER", "todouser");
            String dbPass = System.getenv().getOrDefault("DB_PASSWORD", "todopass");
            String dbUrl = "jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName + "?useSSL=false&allowPublicKeyRetrieval=true";
            conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
        } catch(Exception e){
            throw new ServletException(e);
        }
    }

    // GET /todos
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM todos");
            JSONArray arr = new JSONArray();
            while(rs.next()){
                JSONObject obj = new JSONObject();
                obj.put("id", rs.getInt("id"));
                obj.put("description", rs.getString("description"));
                obj.put("completed", rs.getBoolean("completed"));
                arr.put(obj);
            }
            out.print(arr.toString());
        } catch(Exception e){
            resp.setStatus(500);
            out.print("{ \"error\": \"Server error\" }");
        }
    }

    // POST /todos
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String body = readRequestBody(req);
        try {
            JSONObject obj = new JSONObject(body);
            String desc = obj.getString("description");
            PreparedStatement ps = conn.prepareStatement("INSERT INTO todos (description) VALUES (?)");
            ps.setString(1, desc);
            ps.executeUpdate();
            resp.setStatus(201);
        } catch(Exception e){
            resp.setStatus(400);
            resp.getWriter().print("{\"error\":\"Bad Request\"}");
        }
    }

    // PUT /todos/{id}
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo(); // e.g., "/3"
        if (pathInfo == null || pathInfo.length() <= 1) {
            resp.setStatus(400);
            resp.getWriter().print("{\"error\":\"Missing id\"}");
            return;
        }
        int id;
        try {
            id = Integer.parseInt(pathInfo.substring(1));
        } catch(NumberFormatException e) {
            resp.setStatus(400);
            resp.getWriter().print("{\"error\":\"Invalid id\"}");
            return;
        }
        String body = readRequestBody(req);
        try {
            JSONObject obj = new JSONObject(body);
            String description = obj.optString("description", null);
            Boolean completed = obj.has("completed") ? obj.getBoolean("completed") : null;

            String sql = "UPDATE todos SET ";
            boolean setDesc = description != null, setComp = completed != null;
            if (!setDesc && !setComp) {
                resp.setStatus(400);
                resp.getWriter().print("{\"error\":\"Nothing to update\"}");
                return;
            }
            if (setDesc) sql += "description=?";
            if (setDesc && setComp) sql += ", ";
            if (setComp) sql += "completed=?";
            sql += " WHERE id=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            int idx = 1;
            if (setDesc) ps.setString(idx++, description);
            if (setComp) ps.setBoolean(idx++, completed);
            ps.setInt(idx, id);
            int res = ps.executeUpdate();
            if (res == 0) {
                resp.setStatus(404);
                resp.getWriter().print("{\"error\":\"Not found\"}");
            } else {
                resp.setStatus(200);
            }
        } catch(Exception e){
            resp.setStatus(400);
            resp.getWriter().print("{\"error\":\"Bad Request\"}");
        }
    }

    // DELETE /todos/{id}
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo(); // e.g., "/3"
        if (pathInfo == null || pathInfo.length() <= 1) {
            resp.setStatus(400);
            resp.getWriter().print("{\"error\":\"Missing id\"}");
            return;
        }
        int id;
        try {
            id = Integer.parseInt(pathInfo.substring(1));
        } catch(NumberFormatException e) {
            resp.setStatus(400);
            resp.getWriter().print("{\"error\":\"Invalid id\"}");
            return;
        }
        try {
            PreparedStatement ps = conn.prepareStatement("DELETE FROM todos WHERE id=?");
            ps.setInt(1, id);
            int res = ps.executeUpdate();
            if (res == 0) {
                resp.setStatus(404);
                resp.getWriter().print("{\"error\":\"Not found\"}");
            } else {
                resp.setStatus(204);
            }
        } catch(Exception e) {
            resp.setStatus(400);
            resp.getWriter().print("{\"error\":\"Bad Request\"}");
        }
    }

    @Override
    public void destroy() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch(Exception ignored) {}
    }

    // Utility to read full POST/PUT body
    private String readRequestBody(HttpServletRequest req) throws IOException {
        BufferedReader reader = req.getReader();
        StringBuilder sb = new StringBuilder();
        String line;
        while((line = reader.readLine()) != null){
            sb.append(line);
        }
        return sb.toString();
    }
}