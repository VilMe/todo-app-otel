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

    // GET /todos -> returns JSON: { "todos": [...], "count": N, "last_modified": "ISO-8601" }
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        try {
            // fetch todos with last_modified
            PreparedStatement listStmt = conn.prepareStatement("SELECT id, description, completed, last_modified FROM todos ORDER BY id ASC");
            ResultSet rs = listStmt.executeQuery();
            JSONArray arr = new JSONArray();
            while(rs.next()){
                JSONObject obj = new JSONObject();
                obj.put("id", rs.getInt("id"));
                obj.put("description", rs.getString("description"));
                obj.put("completed", rs.getBoolean("completed"));
                Timestamp ts = rs.getTimestamp("last_modified");
                if (ts != null) {
                    obj.put("last_modified", ts.toInstant().toString());
                } else {
                    obj.put("last_modified", JSONObject.NULL);
                }
                arr.put(obj);
            }
            rs.close();
            listStmt.close();

            // fetch count
            int count = 0;
            PreparedStatement countStmt = conn.prepareStatement("SELECT COUNT(*) FROM todos");
            ResultSet crs = countStmt.executeQuery();
            if (crs.next()) {
                count = crs.getInt(1);
            }
            crs.close();
            countStmt.close();

            // fetch overall last_modified (most recent)
            String overallLastModified = null;
            PreparedStatement maxStmt = conn.prepareStatement("SELECT MAX(last_modified) FROM todos");
            ResultSet mrs = maxStmt.executeQuery();
            if (mrs.next()) {
                Timestamp mts = mrs.getTimestamp(1);
                if (mts != null) overallLastModified = mts.toInstant().toString();
            }
            mrs.close();
            maxStmt.close();

            JSONObject response = new JSONObject();
            response.put("todos", arr);
            response.put("count", count);
            if (overallLastModified != null) response.put("last_modified", overallLastModified);
            else response.put("last_modified", JSONObject.NULL);

            out.print(response.toString());
        } catch(Exception e){
            resp.setStatus(500);
            JSONObject err = new JSONObject();
            err.put("error", "Server error");
            err.put("detail", e.getMessage());
            out.print(err.toString());
        }
    }

    // POST /todos (create)
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo(); // null or "/..."
        // Only support POST /todos to create (no other POST endpoints)
        if (pathInfo != null && !pathInfo.equals("/")) {
            resp.setStatus(404);
            resp.getWriter().print("{\"error\":\"Not found\"}");
            return;
        }

        String body = readRequestBody(req);
        try {
            JSONObject obj = new JSONObject(body);
            String desc = obj.getString("description");
            PreparedStatement ps = conn.prepareStatement("INSERT INTO todos (description) VALUES (?)");
            ps.setString(1, desc);
            ps.executeUpdate();
            ps.close();
            resp.setStatus(201);
        } catch(Exception e){
            resp.setStatus(400);
            resp.getWriter().print("{\"error\":\"Bad Request\",\"detail\":\"" + e.getMessage() + "\"}");
        }
    }

    // PUT /todos/{id}
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo(); // e.g. "/3"
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
            String description = obj.has("description") ? obj.getString("description") : null;
            Boolean completed = obj.has("completed") ? obj.getBoolean("completed") : null;

            StringBuilder sql = new StringBuilder("UPDATE todos SET ");
            boolean first = true;
            if (description != null) {
                sql.append("description=?");
                first = false;
            }
            if (completed != null) {
                if (!first) sql.append(", ");
                sql.append("completed=?");
            }
            sql.append(" WHERE id=?");

            PreparedStatement ps = conn.prepareStatement(sql.toString());
            int idx = 1;
            if (description != null) ps.setString(idx++, description);
            if (completed != null) ps.setBoolean(idx++, completed);
            ps.setInt(idx, id);
            int res = ps.executeUpdate();
            ps.close();
            if (res == 0) {
                resp.setStatus(404);
                resp.getWriter().print("{\"error\":\"Not found\"}");
            } else {
                // last_modified is updated automatically by DB (see schema)
                resp.setStatus(200);
            }
        } catch(Exception e){
            resp.setStatus(400);
            resp.getWriter().print("{\"error\":\"Bad Request\",\"detail\":\"" + e.getMessage() + "\"}");
        }
    }

    // DELETE /todos/{id}
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
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
            ps.close();
            if (res == 0) {
                resp.setStatus(404);
                resp.getWriter().print("{\"error\":\"Not found\"}");
            } else {
                resp.setStatus(204);
            }
        } catch(Exception e) {
            resp.setStatus(400);
            resp.getWriter().print("{\"error\":\"Bad Request\",\"detail\":\"" + e.getMessage() + "\"}");
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