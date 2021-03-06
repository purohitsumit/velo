package org.globusonline.transfer;

import java.security.GeneralSecurityException;
import java.util.*;
import java.io.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.json.*;

public class Example {
    private JSONTransferAPIClient client;
    private static DateFormat isoDateFormat =
                            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    public Example(JSONTransferAPIClient client) {
        this.client = client;
    }

    public static void main(String args[]) {
        if (args.length < 1) {
            System.err.println(
                "Usage: java org.globusonline.transfer.Example "
                + "username [cafile certfile keyfile [baseurl]]]");
            System.exit(1);
        }
        String username = args[0];

        String cafile = null;
        if (args.length > 1 && args[1].length() > 0)
            cafile = args[1];

        String certfile = null;
        if (args.length > 2 && args[2].length() > 0)
            certfile = args[2];

        String keyfile = null;
        if (args.length > 3 && args[3].length() > 0)
            keyfile = args[3];

        String baseUrl = null;
        if (args.length > 4 && args[4].length() > 0)
            baseUrl = args[4];

        try {
            JSONTransferAPIClient c = new JSONTransferAPIClient(username,
                                         cafile, certfile, keyfile, baseUrl);
            System.out.println("base url: " + c.getBaseUrl());
            Example e = new Example(c);
            e.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run()
    throws IOException, JSONException, GeneralSecurityException, APIError {
        JSONTransferAPIClient.Result r;
        System.out.println("=== Before Transfer ===");

        displayTasksummary();
        displayTaskList(60 * 60 * 24 * 7); // tasks at most a week old
        //displayEndpointList();

        if (!autoActivate("go#ep1") || !autoActivate("go#ep2")) {
            System.err.println("Unable to auto activate go tutorial endpoints, "
                               + " exiting");
            return;
        }

        displayLs("go#ep1", "~");
        displayLs("go#ep2", "~");

        r = client.getResult("/transfer/submission_id");
        String submissionId = r.document.getString("value");
        JSONObject transfer = new JSONObject();
        transfer.put("DATA_TYPE", "transfer");
        transfer.put("submission_id", submissionId);
        JSONObject item = new JSONObject();
        item.put("DATA_TYPE", "transfer_item");
        item.put("source_endpoint", "go#ep1");
        item.put("source_path", "~/.bashrc");
        item.put("destination_endpoint", "go#ep2");
        item.put("destination_path", "~/api-example-bashrc-copy");
        transfer.append("DATA", item);

        r = client.postResult("/transfer", transfer, null);
        String taskId = r.document.getString("task_id");
        if (!waitForTask(taskId, 120)) {
            System.out.println(
                "Transfer not complete after 2 minutes, exiting");
            return;
        }

        System.out.println("=== After Transfer ===");

        displayTasksummary();
        displayLs("go#ep2", "~");

        System.out.println("=== Endpoint Management ===");
        String code;
        String copyName = "example_copygoep1";

        code = copyEndpoint("go#ep1", copyName);
        System.out.println("copy go#ep1 to " + copyName + ": " + code);

        code = setEndpointDescription(copyName, "copy of go#ep1");
        System.out.println("endpoint update: " + code);

        r = client.getResult(BaseTransferAPIClient.endpointPath(copyName));
        System.out.println("description after update: "
                           + r.document.getString("description"));

        code = deleteEndpoint(copyName);
        System.out.println("endpoint delete: " + code);

        try {
            r = client.getResult(BaseTransferAPIClient.endpointPath(copyName));
        } catch(APIError e) {
            System.out.println("get on deleted endpoint raised APIError: " + e);
        }
    }

    public void displayTasksummary()
    throws IOException, JSONException, GeneralSecurityException, APIError {
        JSONTransferAPIClient.Result r = client.getResult("/tasksummary");
        System.out.println("Task Summary for " + client.getUsername()
                           + ": ");
        Iterator keysIter = r.document.sortedKeys();
        while (keysIter.hasNext()) {
            String key = (String)keysIter.next();
            if (!key.equals("DATA_TYPE"))
                System.out.println("  " + key + ": "
                                   + r.document.getString(key));
        }
    }

    public void displayTaskList(long maxAge)
    throws IOException, JSONException, GeneralSecurityException, APIError {
//      Map<String, String> params = new HashMap<String, String>();
        Map params = new HashMap();
        if (maxAge > 0) {
            long minTime = System.currentTimeMillis() - 1000 * maxAge;
            params.put("filter", "request_time:"
                       + isoDateFormat.format(new Date(minTime)) + ",");
        }
        JSONTransferAPIClient.Result r = client.getResult("/task_list",
                                                          params);

        int length = r.document.getInt("length");
        if (length == 0) {
            System.out.println("No tasks were submitted in the last "
                               + maxAge + " seconds");
            return;
        }
        JSONArray tasksArray = r.document.getJSONArray("DATA");
        for (int i=0; i < tasksArray.length(); i++) {
            JSONObject taskObject = tasksArray.getJSONObject(i);
            System.out.println("Task " + taskObject.getString("task_id")
                               + ":");
            displayTask(taskObject);
        }
    }

    private static void displayTask(JSONObject taskObject)
    throws JSONException {
        Iterator keysIter = taskObject.sortedKeys();
        while (keysIter.hasNext()) {
            String key = (String)keysIter.next();
            if (!key.equals("DATA_TYPE") && !key.equals("LINKS")
                && !key.endsWith("_link")) {
                System.out.println("  " + key + ": "
                                   + taskObject.getString(key));
            }
        }
    }

    public boolean autoActivate(String endpointName)
    throws IOException, JSONException, GeneralSecurityException, APIError {
        String resource = BaseTransferAPIClient.endpointPath(endpointName)
                          + "/autoactivate";
        JSONTransferAPIClient.Result r = client.postResult(resource, null,
                                                           null);
        String code = r.document.getString("code");
        if (code.startsWith("AutoActivationFailed")) {
            return false;
        }
        return true;
    }

    public void displayLs(String endpointName, String path)
    throws IOException, JSONException, GeneralSecurityException, APIError {
//        Map<String, String> params = new HashMap<String, String>();
        Map params = new HashMap();
        if (path != null) {
            params.put("path", path);
        }
        String resource = BaseTransferAPIClient.endpointPath(endpointName)
                          + "/ls";
        JSONTransferAPIClient.Result r = client.getResult(resource, params);
        System.out.println("Contents of " + path + " on "
                           + endpointName + ":");

        JSONArray fileArray = r.document.getJSONArray("DATA");
        for (int i=0; i < fileArray.length(); i++) {
            JSONObject fileObject = fileArray.getJSONObject(i);
            System.out.println("  " + fileObject.getString("name"));
            Iterator keysIter = fileObject.sortedKeys();
            while (keysIter.hasNext()) {
                String key = (String)keysIter.next();
                if (!key.equals("DATA_TYPE") && !key.equals("LINKS")
                    && !key.endsWith("_link") && !key.equals("name")) {
                    System.out.println("    " + key + ": "
                                       + fileObject.getString(key));
                }
            }
        }

    }

    public boolean waitForTask(String taskId, int timeout)
    throws IOException, JSONException, GeneralSecurityException, APIError {
        String status = "ACTIVE";
        JSONTransferAPIClient.Result r;

        String resource = "/task/" +  taskId;
//        Map<String, String> params = new HashMap<String, String>();
        Map params = new HashMap();
        params.put("fields", "status");

        while (timeout > 0 && status.equals("ACTIVE")) {
            r = client.getResult(resource, params);
            status = r.document.getString("status");
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                return false;
            }
            timeout -= 10;
        }

        if (status.equals("ACTIVE"))
            return false;
        return true;
    }

    public String copyEndpoint(String endpointName, String copyName)
    throws IOException, JSONException, GeneralSecurityException, APIError {
        String resource = BaseTransferAPIClient.endpointPath(endpointName);
        JSONTransferAPIClient.Result r = client.getResult(resource);
        JSONObject endpoint = r.document;
        endpoint.put("name", copyName);
        endpoint.remove("username");
        endpoint.remove("canonical_name");

        r = client.postResult("/endpoint", endpoint);
        return r.document.getString("code");
    }

    public String setEndpointDescription(String endpointName,
                                          String description)
    throws IOException, JSONException, GeneralSecurityException, APIError {
        String resource = BaseTransferAPIClient.endpointPath(endpointName);
        JSONObject o = new JSONObject();
        o.put("DATA_TYPE", "endpoint");
        o.put("description", description);

        JSONTransferAPIClient.Result r = client.putResult(resource, o);
        return r.document.getString("code");
    }

    public String deleteEndpoint(String endpointName)
    throws IOException, JSONException, GeneralSecurityException, APIError {
        String resource = BaseTransferAPIClient.endpointPath(endpointName);
        JSONTransferAPIClient.Result r = client.deleteResult(resource);
        return r.document.getString("code");
    }
}
