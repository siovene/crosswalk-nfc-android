package org.crosswalkproject.nfc;

import java.lang.reflect.Method;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ActionRunner {
    private Object target = null;
    private Gson gson = new GsonBuilder().create();

    public ActionRunner(Object target)
    {
        this.target = target;
    }

    public InternalProtocolMessage run(int instance, String request)
    {
        Method method;
        InternalProtocolMessage requestObject, response = null;
       
        requestObject = gson.fromJson(request, InternalProtocolMessage.class);

        try {
            method = target.getClass().getMethod(
                    requestObject.content, int.class,
                    InternalProtocolMessage.class);
            response = (InternalProtocolMessage) method.invoke(target, instance, requestObject);
        } catch (Exception e) {
            response = InternalProtocolMessage.build(
                    requestObject.id, "nfc_invalid_action",
                    e.getMessage(), false);
        }

        return response;
    }
}
