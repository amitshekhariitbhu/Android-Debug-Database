package com.amitshekhar.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by amitshekhar on 15/11/16.
 */

public class Response {

    public List rows = new ArrayList();
    public List<String> columns = new ArrayList<>();
    public boolean isSuccessful;
    public String error;

    public Response() {

    }

}
