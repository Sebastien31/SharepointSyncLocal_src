package systhemes.com;

import java.sql.Connection;
import java.sql.DriverManager;

public class DbConnectionAuto extends Object{
    private String strDriver = "com.informix.jdbc.IfxDriver";
    private String strUrl ;
    private String strUsername ;
    private String strPassword ;
    private String strBdname ;
    private String gestDB ;
    private String gestDBPath ;
    private String gestDir ;
    private String host;
    private String port;
    private String server;
    private Connection cnn;
    private boolean booCnnOpen;
    private String webDir;

    public DbConnectionAuto() {
        booCnnOpen = false;
        strUrl = null;
        strUsername ="loladmz";
        strPassword ="sTupruzuqeCR5dr";
        strBdname ="loladb";
        gestDB = null;
        gestDBPath = null;
        gestDir=null;
        cnn=null;
        host="192.168.163.5";
        port="1538";
        server="serveur_on";
        webDir="/dashboard";
    }

    public boolean connect(){
        boolean returnVal=false;
        try{
            if (!(booCnnOpen)){
                Class.forName(strDriver);
                strUrl = "jdbc:informix-sqli://"+ host+":"+ port +"/"+strBdname+":INFORMIXSERVER="+server+";DBDATE=y4md-";
                cnn = DriverManager.getConnection(strUrl,strUsername,strPassword);
                booCnnOpen = true;
                returnVal = true;
            }
        }
        catch(Exception e){
            System.out.println("DbConnectionAuto:"+e.getMessage());
            returnVal = false;
        }
        finally{
            return returnVal;
        }
    }
    public void disconnect(){
        try{
            if(cnn != null){
                booCnnOpen = true;
                cnn.close();
            }
        }
        catch(Exception e){
            System.out.println("DbConnectionAuto:"+e.getMessage());
        }
    }
    public boolean isConnected(){
        return booCnnOpen;
    }
    public void setUsername(String strUsername){
        this.strUsername = strUsername.trim();
    }

    public String getUsername() {
        return this.strUsername;
    }

    public String getPassword() {
        return this.strPassword;
    }

    public void setPassword(String strPassword) {
        this.strPassword = strPassword.trim();
    }

    public String getBdname() {
        return this.strBdname;
    }

    public void setHost(String val) {
        this.host = val.trim();
    }
    public String getHost() {
        return this.host;
    }
    public void setPort(String val) {
        this.port = val.trim();
    }
    public String getPort() {
        return this.port;
    }

    public void setServer(String val) {
        this.server = val.trim();
    }
    public String getServer() {
        return this.server;
    }

    public void setBdname(String strBdname) {
        this.strBdname = strBdname.trim();
    }
    public void setVariable(String strDb){
        strBdname = strDb.trim();
        gestDB = strDb.substring(strDb.lastIndexOf("/")+1);
        strDb = strDb.substring(0,strDb.lastIndexOf("/"));
        gestDBPath = strDb;
        gestDir = strDb.substring(0,strDb.lastIndexOf("/"));
    }
    public String getGestDB(){
        return gestDB;
    }
    public String getGestDBPath(){
        return gestDBPath;
    }
    public String getGestDir(){
        return gestDir;
    }
    public Connection getConnection(){
        return cnn;
    }

    public String getWebDir() {
        return webDir;
    }

    public void setWebDir(String val) {
        webDir = val;
    }
}
