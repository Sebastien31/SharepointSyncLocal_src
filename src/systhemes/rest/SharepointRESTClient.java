package systhemes.rest;

import org.apache.http.util.EntityUtils;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class SharepointRESTClient {

    public static void main(String []args) throws IOException {
//        String password = "1SysThe!me";
//        String username = "prog@lola.ca";
//        String hostname = "lolaca.sharepoint.com";

        String password = "G6LYwvty9e5ES0SNHn3t";
        String username = "systhemes@teops.onmicrosoft.com";
//        String username = "systhemes@lola.ca";
        String hostname = "teops.sharepoint.com";

        String local_path = args[0];
        ArrayList<String> lstDir = new ArrayList<>();
        lstDir.add("Port%20info");
        lstDir.add("final%20DA");
        lstDir.add("Cargo%20Documents");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println("Start synchro Sharepoint: " + sdf.format(new Date()));
        for(String sync_dir: lstDir) {
            String uri = "/sites/lola-library/_api/Web/GetFolderByServerRelativePath(decodedurl='/sites/lola-library/Dashboard/" + sync_dir + "')?$expand=Folders,Files&select=Name";
            System.out.println("top folder: " + sync_dir);
            Service service = new Service(hostname, username, password, sync_dir, local_path);
            try {
//                System.out.println(EntityUtils.toString(service.callRequest(uri)));
                service.navigate(service, uri);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        System.out.println("End synchro Sharepoint: " + sdf.format(new Date()));
    }
}
