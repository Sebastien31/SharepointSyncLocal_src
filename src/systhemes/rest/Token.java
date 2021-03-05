package systhemes.rest;

import java.sql.*;

/**
 * Created by prog on 2017-08-29.
 */
public class Token {
    private long id;
    private String token;
    private String created;
    private String expires;
    private Double lifetime;
    private String lastSync;
    private Connection cnn;

    public Token(String token, String created, String expires, Double lifetime, Connection cnn) {
        this.token = token;
        this.created = created;
        this.expires = expires;
        this.lifetime = lifetime;
        this.cnn = cnn;
    }

    public Token(boolean current) {
        PreparedStatement ps = null;
        String sql = "select first 1 * from wstoken1 where wst1_expires > CURRENT order by wst1_created DESC";
        try {
            ps = cnn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                id = rs.getLong("wst1_seq");
                token = rs.getString("wst1_token");
                created = rs.getString("wst1_created");
                expires = rs.getString("wst1_expires");
                lifetime = rs.getDouble("wst1_lifetime");
                lastSync = rs.getString("wst1_lastsync");
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (ps != null) ps.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public Token() { }

    public String getToken() {
        return token;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public String getExpires() {
        return expires;
    }

    public void setExpires(String expires) {
        this.expires = expires;
    }

    public Double getLifetime() {
        return lifetime;
    }

    public void setLifetime(Double lifetime) {
        this.lifetime = lifetime;
    }

    public String getLastSync() {
        return lastSync;
    }

    public void setLastSync(String lastSync) {
        this.lastSync = lastSync;
    }

    public Connection getConnection() { return cnn; }

    public void setConnection(Connection cnn){
        try {
            this.cnn = cnn;
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public boolean isLastExpired(){
        PreparedStatement ps = null;
        ResultSet rs = null;
        boolean isExpired = true;
        String sql = "select first 1 * from wstoken1 where wst1_expires > CURRENT order by wst1_created DESC";
        try {
            ps = cnn.prepareStatement(sql);
            rs = ps.executeQuery();
            if(rs.next()){
                isExpired = false;
            }
            rs.close();
            ps.close();
        }catch(SQLException e){
            e.printStackTrace();
        }finally {
            try {
                if(ps != null) ps.close();
            }catch (SQLException e){
                e.printStackTrace();
            }
        }
        return isExpired;
    }

    public void saveToken(){
        PreparedStatement ps = null;
        String sql = "insert into wstoken1 (wst1_token, wst1_created, wst1_expires, wst1_lifetime) values(?,?,?,?)";
        try {
            ps = cnn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1,token);
            ps.setString(2,created.replaceAll("T"," ").replaceAll("Z",""));
            ps.setString(3,expires.replaceAll("T"," ").replaceAll("Z",""));
            ps.setDouble(4,24.0);
            ps.execute();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                id = rs.getLong(1);
            }
            rs.close();

            ps.close();
        }catch(SQLException e){
            e.printStackTrace();
        }finally {
            try {
                if(ps != null) ps.close();
            }catch (SQLException e){
                e.printStackTrace();
            }
        }
    }

    public void setCurrentTokenExpired(long id){
        PreparedStatement ps = null;
        String sql = "update wstoken1 set wst1_expires = ( CURRENT - 1 units second) where wst1_seq = ?";
        try {
            ps = cnn.prepareStatement(sql);
            ps.setLong(1,id);
            ps.executeUpdate();
            ps.close();
        }catch(SQLException e){
            e.printStackTrace();
        }finally {
            try {
                if(ps != null) ps.close();
            }catch (SQLException e){
                e.printStackTrace();
            }
        }
    }

    public void setTokensExpired(){
        PreparedStatement ps = null;
        String sql = "update wstoken1 set wst1_expires = ( CURRENT - 1 units second) where wst1_expires >= CURRENT and wst1_created between (TODAY - 2 units day) and today + 1 units day";
        try {
            ps = cnn.prepareStatement(sql);
            ps.executeUpdate();
            ps.close();
        }catch(SQLException e){
            e.printStackTrace();
        }finally {
            try {
                if(ps != null) ps.close();
            }catch (SQLException e){
                e.printStackTrace();
            }
        }
    }

    public String getCurrentSecurityToken(){
        PreparedStatement ps = null;
        ResultSet rs = null;
        String wst1_token = "";
        String sql = "select first 1 wst1_token from wstoken1 where wst1_expires > CURRENT order by wst1_created DESC";
        try {
            ps = cnn.prepareStatement(sql);
            rs = ps.executeQuery();
            while(rs.next()) {
                wst1_token = rs.getString("wst1_token");
            }
            rs.close();
            ps.close();
        }catch(SQLException e){
            e.printStackTrace();
        }finally {
            try {
                if(ps != null) ps.close();
            }catch (SQLException e){
                e.printStackTrace();
            }
        }
        return wst1_token;
    }


}
