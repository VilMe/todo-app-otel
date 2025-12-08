package com.example;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;
import org.json.*;

public class TodoServlet extends HttpServlet {
    private Connection conn;

    public void init() throws ServletException {
        // ... (same as before)
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // ... (same as before)
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // ... (same as before)
    }

    // Handles PUT /todos/{id}
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo(); // e.g., "/3"
        if (pathInfo == null || pathInfo.length() <= 1) {
            resp.setStatus(400);
            return;
        }
        int id = Integer.parseInt(pathInfo.substring(1));
        BufferedReader reader = req.getReader();
        StringBuilder sb = new StringBuilder();
        String line;
        while((line = reader.readLine()) != null){
            sb.append(line);
        }
        try {
            JSONObject obj = new JSONObject(sb.toString());
            String description = obj.optString("description", null);
            Boolean completed = obj.has("completed") ? obj.getBoolean("completed") : null;

            String sql = "UPDATE todos SET ";
            boolean setDesc = description != null, setComp = completed != null;
            if (!setDesc && !setComp) {
                resp.setStatus(400); return;
            }
            if (setDesc) sql += "description=? ";
            if (setDesc && setComp) sql += ", ";
            if (setComp) sql += "completed=? ";
            sql += "WHERE id=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            int idx = 1;
            if (setDesc) ps.setString(idx++, description);
            if (setComp) ps.setBoolean(idx++, completed);
            ps.setInt(idx, id);
            int res = ps.executeUpdate();
            if (res == 0) resp.setStatus(404); else resp.setStatus(200);
        } catch(Exception e){
            resp.setStatus(400);
        }
    }

    // Handles DELETE /todos/{id}
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo(); // e.g., "/3"
        if (pathInfo == null || pathInfo.length() <= 1) {
            resp.setStatus(400);
            return;
        }
        int id = Integer.parseInt(pathInfo.substring(1));
        try {
            PreparedStatement ps = conn.prepareStatement("DELETE FROM todos WHERE id=?");
            ps.setInt(1, id);
            int res = ps.executeUpdate();
            if (res == 0) resp.setStatus(404);
            else resp.setStatus(204);
        } catch(Exception e) {
            resp.setStatus(400);
        }
    }
}