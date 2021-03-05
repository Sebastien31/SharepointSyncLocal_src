package systhemes.rest;

import com.google.common.base.Joiner;
import org.apache.commons.io.FileUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import systhemes.com.DbConnectionAuto;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Service {

    private String hostname;
    private String username;
    private String password;
    private String sync_dir;
    private String local_path;
    private Token token;
    private DbConnectionAuto dbca;
    private String securityToken;
    private List<String> cookies;
    private String formDigestValue;


    public Service(String hostname,String username, String password, String sync_dir, String local_path){
        this.hostname = hostname;
        this.username = username;
        this.password = password;
        this.sync_dir = sync_dir;
        this.local_path = local_path;
        this.dbca = new DbConnectionAuto();
        dbca.connect();
        token = new Token();
        token.setConnection(dbca.getConnection());
        initializeSecurityToken();
    }

    public String receiveSecurityToken() throws TransformerException, URISyntaxException {
        if(token.isLastExpired() || formDigestValue == null || cookies.size()==0) {
            RequestEntity<String> requestEntity = new RequestEntity<>(buildSecurityTokenRequestEnvelope(), HttpMethod.POST, new URI("https://login.microsoftonline.com/extSTS.srf"));
            ResponseEntity<String> responseEntity = new RestTemplate().exchange(requestEntity, String.class);
            String response = responseEntity.getBody();
//            System.out.println("receiveSecurityToken response: " + response);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder;
            try {
                builder = factory.newDocumentBuilder();
                Document doc = builder.parse(new InputSource(new StringReader(response)));
                NodeList nList = doc.getElementsByTagName("wst:Lifetime");
                for (int temp = 0; temp < nList.getLength(); temp++) {
                    Node nNode = nList.item(temp);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element eElement = (Element) nNode;
                        token.setCreated(eElement.getElementsByTagName("wsu:Created").item(0).getTextContent());
                        token.setExpires(eElement.getElementsByTagName("wsu:Expires").item(0).getTextContent());
                        token.setLifetime(24.0);
                    }
                }
                nList = doc.getElementsByTagName("wst:RequestedSecurityToken");
                for (int temp = 0; temp < nList.getLength(); temp++) {
                    Node nNode = nList.item(temp);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element eElement = (Element) nNode;
                        securityToken = eElement.getElementsByTagName("wsse:BinarySecurityToken").item(0).getTextContent();
                        token.setToken(securityToken);
                    }
                }
                token.saveToken();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else{
            securityToken = token.getCurrentSecurityToken();
        }
        if (StringUtils.isEmpty(securityToken)) {
            throw new TransformerException("Unable to authenticate: empty token");
        }
        return securityToken;
    }

    public List<String> getSignInCookies(String securityToken) throws Exception {
//        RequestEntity<String> requestEntity = new RequestEntity<>(securityToken, HttpMethod.POST, new URI("https://lolaca.sharepoint.com/_forms/default.aspx?wa=wsignin1.0"));
        RequestEntity<String> requestEntity = new RequestEntity<>(securityToken, HttpMethod.POST, new URI("https://" + hostname + "/_forms/default.aspx?wa=wsignin1.0"));
        ResponseEntity<String> responseEntity = new RestTemplate().exchange(requestEntity, String.class);
        HttpHeaders headers = responseEntity.getHeaders();
        List<String> cookies = headers.get("Set-Cookie");
        if (CollectionUtils.isEmpty(cookies)) {
            throw new Exception("Unable to sign in: no cookies returned in response");
        }
        return cookies;
    }

    public String getFormDigestValue(List<String> cookies) throws HttpClientErrorException, IOException, URISyntaxException, TransformerException, JSONException {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Cookie", Joiner.on(';').join(cookies));
        headers.add("Accept", "application/json;odata=verbose");
        headers.add("X-ClientService-ClientTag", "SDK-JAVA");
//        RequestEntity<String> requestEntity = new RequestEntity<>(headers, HttpMethod.POST, new URI("https://lolaca.sharepoint.com/_api/contextinfo"));
        RequestEntity<String> requestEntity = new RequestEntity<>(headers, HttpMethod.POST, new URI("https://" + hostname + "/_api/contextinfo"));
        ResponseEntity<String> responseEntity = new RestTemplate().exchange(requestEntity, String.class);
        JSONObject json = new JSONObject(responseEntity.getBody());
//        System.out.println("getFormDigestValue: " + json.toString());
        return json.getJSONObject("d").getJSONObject("GetContextWebInformation").getString("FormDigestValue");
    }

    public String buildSecurityTokenRequestEnvelope(){
        return "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:a=\"http://www.w3.org/2005/08/addressing\">" +
                "  <s:Header>" +
                "    <a:Action s:mustUnderstand=\"1\">http://schemas.xmlsoap.org/ws/2005/02/trust/RST/Issue</a:Action> " +
                "    <a:ReplyTo>" +
                "      <a:Address>http://www.w3.org/2005/08/addressing/anonymous</a:Address>" +
                "    </a:ReplyTo> " +
                "    <a:To s:mustUnderstand=\"1\">https://login.microsoftonline.com/extSTS.srf</a:To>" +
                "    <o:Security s:mustUnderstand=\"1\" xmlns:o=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">" +
                "      <o:UsernameToken>" +
                "        <o:Username>"+ username + "</o:Username>" +
                "        <o:Password>" + password + "</o:Password>" +
                "      </o:UsernameToken>" +
                "    </o:Security>" +
                "  </s:Header>" +
                "  <s:Body>" +
                "    <t:RequestSecurityToken xmlns:t=\"http://schemas.xmlsoap.org/ws/2005/02/trust\">" +
                "      <wsp:AppliesTo xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2004/09/policy\">" +
                "        <a:EndpointReference>" +
                "          <a:Address>https://" + hostname + "</a:Address>" +
                "        </a:EndpointReference>" +
                "      </wsp:AppliesTo> " +
                "      <t:KeyType>http://schemas.xmlsoap.org/ws/2005/05/identity/NoProofKey</t:KeyType>" +
                "      <t:RequestType>http://schemas.xmlsoap.org/ws/2005/02/trust/Issue</t:RequestType>" +
                "      <t:TokenType>urn:oasis:names:tc:SAML:1.0:assertion</t:TokenType>" +
                "    </t:RequestSecurityToken>" +
                "  </s:Body>" +
                "</s:Envelope>";
    }

    public HttpEntity callRequest(String uri) throws HttpClientErrorException, TransformerException, URISyntaxException {
        HttpEntity entity = null;
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(AuthScope.ANY),
                new NTCredentials(username, password, hostname, ""));
        CloseableHttpClient httpclient = HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider)
                .setConnectionTimeToLive(60, TimeUnit.SECONDS)
                .build();
        RequestConfig defaultRequestConfig = RequestConfig.custom()
                .setConnectTimeout(60000)
                .setSocketTimeout(60000)
//                .setConnectionRequestTimeout(60000)
                .build();
        try {
            if (cookies.size() > 0){
                HttpHost target = new HttpHost(hostname, 443, "https");
                HttpGet request = new HttpGet(uri);
                request.setConfig(defaultRequestConfig);
                request.addHeader("Accept", "application/json;odata=verbose");
                request.addHeader("Content-type", "application/json;odata=verbose");
                request.addHeader("X-HTTP-Method", "POST");
                request.addHeader("Cookie", Joiner.on(';').join(cookies));
                request.addHeader("X-RequestDigest", formDigestValue);
                CloseableHttpResponse httpResponse = httpclient.execute(target, request);
                entity = httpResponse.getEntity();
                int rc = httpResponse.getStatusLine().getStatusCode();
                String reason = httpResponse.getStatusLine().getReasonPhrase();

                //TODO logger le nom du fichier et la date de la mauvaise synchro de celui ci avec le code et passer au call suivant dans le cas d'une exception repete
                if (rc == HttpStatus.SC_NOT_FOUND || rc == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
                    System.out.println("----------------------------------------------");
                    System.out.println("receiveSecurityToken() " + rc + " " + reason + ": " + uri);
                    System.out.println("----------------------------------------------");
                    System.exit(0);
                } else if (rc != HttpStatus.SC_OK) {
                    System.out.println("----------------------------------------------");
                    System.out.println("receiveSecurityToken() " + rc + " " + reason + ": " + uri);
                    System.out.println("----------------------------------------------");
                    token.setTokensExpired();
                    initializeSecurityToken();
                    callRequest(uri);
                }
            }else{
                token.setTokensExpired();
                initializeSecurityToken();
                callRequest(uri);
            }
        }  catch (Exception e) {
            e.printStackTrace();
            try {
                System.out.println("----------------------------------------");
                System.out.println("callRequest(" + uri + ") Exception");
                System.out.println("----------------------------------------");
//                token.setCurrentTokenExpired(token.getId());
                httpclient.close();
                token.setTokensExpired();
                initializeSecurityToken();
                callRequest(uri);
            }catch (Exception e2){
                e2.printStackTrace();
            }
        }
        return entity;
    }

    public ArrayList <Elem> parse(JSONObject response) throws JSONException{
        ArrayList<Elem> list = new ArrayList<>();
        JSONArray arr = (JSONArray) response.getJSONObject("d").getJSONObject("Files").get("results");
        int len = arr.length();
        for (int i = 0; i < len; i++) {
            JSONObject obj = ((JSONObject) arr.get(i));
            Elem elem = new Elem();
            elem.setName(obj.get("Name").toString());
            elem.setServerRelativeUrl(obj.get("ServerRelativeUrl").toString());
            elem.setTimeLastModified(obj.get("TimeLastModified").toString());

            Metadata meta = elem.getMeta();
            meta.setId(((JSONObject)obj.get("__metadata")).get("id").toString());
            meta.setUri(((JSONObject)obj.get("__metadata")).get("uri").toString().replaceAll("%27","%27%27"));
            meta.setType(((JSONObject)obj.get("__metadata")).get("type").toString());
            elem.setMeta(elem.getMeta());
            list.add(elem);
        }
        arr = (JSONArray) response.getJSONObject("d").getJSONObject("Folders").get("results");
        len = arr.length();

        for (int i = 0; i < len; i++) {
            JSONObject obj = ((JSONObject) arr.get(i));
            Elem elem = new Elem();
            elem.setName(obj.get("Name").toString());
            elem.setServerRelativeUrl(obj.get("ServerRelativeUrl").toString());
            elem.setTimeLastModified(obj.get("TimeLastModified").toString());

            Metadata meta = elem.getMeta();
            meta.setId(((JSONObject)obj.get("__metadata")).get("id").toString());
            meta.setUri(((JSONObject)obj.get("__metadata")).get("uri").toString().replaceAll("%27","%27%27"));
            meta.setType(((JSONObject)obj.get("__metadata")).get("type").toString());
            elem.setMeta(elem.getMeta());
            list.add(elem);
        }
        return list;
    }

    public void navigate(Service service,String uri){
        ArrayList <Elem> elem1;
        ArrayList <String> listFileSharepoint = new ArrayList<>();
        FileSystem fs = FileSystems.getDefault();
        try {
            HttpEntity entity = service.callRequest(uri);
            String local_dir = "";
            JSONObject json = new JSONObject(EntityUtils.toString(entity));

            if(json.getJSONObject("d").length()>0){
                elem1 = service.parse(json);
                for (Elem e : elem1) {
                    String path = local_path + fs.getSeparator() + e.getServerRelativeUrl().substring(e.getServerRelativeUrl().indexOf(sync_dir.replaceAll("%20", " ")));
                    local_dir = path.substring(0, path.indexOf(e.getName()));
//                    System.out.println("local_dir: " + local_dir);

                    if (e.getMeta().getType().equals("SP.Folder")) {
                        listFileSharepoint.add(e.getName());
//                        System.out.println("navigate(service," + e.getMeta().getUri() + "?$expand=Folders,Files&select=Name");
                        navigate(service, e.getMeta().getUri() + "?$expand=Folders,Files&select=Name");
                    } else {
                        //SYNCHRONISATION DES FICHIERS
//                    System.out.println("Folder/File: " + e.getName());
                        listFileSharepoint.add(e.getName());
                        File localFile = new File(path);

                        if (localFile.exists()) {
                            BasicFileAttributes attr = Files.readAttributes(localFile.toPath(), BasicFileAttributes.class);
                            String sharepointLastModifiedTime = e.getTimeLastModified().replaceAll("T", " ").replaceAll("Z", "");
                            SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            String localtime = gmttoLocalDate(s.parse(attr.lastModifiedTime().toString().replaceAll("T", " ").replaceAll("Z", "")));
                            if (!sharepointLastModifiedTime.equals(localtime)) {
                                writeFile(e, localFile, uri);
                            }
                        } else {
                            writeFile(e, localFile, uri);
                        }
                    }
                }
            }
            if(local_dir!=null&&!local_dir.equals(""))
                deleteLocalFile(local_dir,listFileSharepoint, uri);
        } catch (Exception e) {
            e.printStackTrace();
            try {
                System.out.println("-----------------------------");
                System.out.println("navigate(service," + uri + ") Exception");
                System.out.println("-----------------------------");
                navigate(service,uri);
            }catch (Exception e2){
                e2.printStackTrace();
            }
        }
    }
    public void writeFile(Elem elem, File localFile, String uri){
        try {
            //Special caracters prohibited by Sharepoint
            if(!elem.getServerRelativeUrl().contains("#") &&
                !elem.getServerRelativeUrl().contains("~") &&
                !elem.getServerRelativeUrl().contains("%") &&
                !elem.getServerRelativeUrl().contains("&") &&
                !elem.getServerRelativeUrl().contains("*") &&
                !elem.getServerRelativeUrl().contains("{") &&
                !elem.getServerRelativeUrl().contains("}") &&
                !elem.getServerRelativeUrl().contains("\\") &&
                !elem.getServerRelativeUrl().contains(":") &&
                !elem.getServerRelativeUrl().contains("?") &&
                !elem.getServerRelativeUrl().contains("<") &&
                !elem.getServerRelativeUrl().contains("+")) {
                System.out.println("writeFile: " + localFile.getPath());
                String relativeUri = elem.getServerRelativeUrl()
                        .replaceAll(" ", "%20")
                        .replaceAll("'", "%27")
                        .replaceAll("`", "%60")
                        .replaceAll("\\[", "%5B")
                        .replaceAll("]", "%5D")
//                    .replaceAll("#","%23")
                        .replaceAll("%27", "%27%27");

                System.out.println("relativeUri: " + relativeUri);

                HttpEntity entity = callRequest("/sites/lola-library/_api/web/GetFileByServerRelativeUrl('" + relativeUri + "')/$value");
                FileUtils.writeByteArrayToFile(localFile, EntityUtils.toByteArray(entity));
                SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date d = s.parse(elem.getTimeLastModified().replaceAll("T", " ").replaceAll("Z", ""));
                FileTime ft = FileTime.fromMillis(d.getTime());
                Files.setLastModifiedTime(localFile.toPath(), ft);
            }else{
                System.out.println("not writing special caracters: " + localFile.getPath());
            }
        }catch(Exception e){
            e.printStackTrace();
            try {
                System.out.println("----------------------------------------");
                System.out.println("writeFile(" + elem + "," + localFile + "," +  uri + ") Exception");
                System.out.println("----------------------------------------");
//                token.setCurrentTokenExpired(token.getId());
                token.setTokensExpired();
                initializeSecurityToken();
                callRequest(uri);
            }catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }

    public void deleteLocalFile(String path,ArrayList<String>listFileSharepoint, String uri){
        try {
            File[] listOfFiles = new File(path).listFiles();
            if(listOfFiles != null) {
                for (int i = 0; i < listOfFiles.length; i++) {
//                System.out.println("localFile " + listOfFiles[i].getName() + " is in Sharepoint: " + listFileSharepoint.contains(listOfFiles[i].getName()));
                    if (listOfFiles[i].isFile()) {
                        listFileSharepoint.contains(listOfFiles[i].getName());
                        if (!listFileSharepoint.contains(listOfFiles[i].getName())) {
                            System.out.println("deleteLocalFile: " + listOfFiles[i].getName());
                            listOfFiles[i].delete();
                        }
                    } else if (listOfFiles[i].isDirectory()) {
                        if (!listFileSharepoint.contains(listOfFiles[i].getName())) {
                            System.out.println("deleteLocalFile-dir: " + listOfFiles[i].getName());
                            FileUtils.deleteDirectory(listOfFiles[i]);
                        }
                    }
                }
            }
        }catch(Exception e){
            e.printStackTrace();
            try {
                System.out.println("----------------------------------------");
                System.out.println("deleteLocalFile(" + path + ", " + uri + ") Exception");
                System.out.println("----------------------------------------");
                token.setTokensExpired();
                initializeSecurityToken();
                callRequest(uri);
            }catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }

    public void initializeSecurityToken(){
        try{
            securityToken = receiveSecurityToken();
            cookies = getSignInCookies(securityToken);
            formDigestValue = getFormDigestValue(cookies);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public String gmttoLocalDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timeZone = Calendar.getInstance().getTimeZone().getID();
        Date d = new Date(date.getTime() + TimeZone.getTimeZone(timeZone).getOffset(date.getTime()));
        return sdf.format(d);
    }

    protected void finalize() throws Throwable {
        try{
            dbca.disconnect();
            //release resources, perform cleanup;
        }catch(Throwable t){
            throw t;
        }finally{
            super.finalize();
        }

    }
}
