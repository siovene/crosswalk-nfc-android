package org.crosswalkproject.nfc;

import java.lang.reflect.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import android.util.Log;

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
            response = (InternalProtocolMessage) method.invoke(
                target, instance, requestObject);
        } catch (NoSuchMethodException e) {
            Log.e("XWALK_NFC", "Invalid action: missing method.");
            response = InternalProtocolMessage.build(
                requestObject.id, "nfc_invalid_action", "missing_method", false);
        } catch (IllegalAccessException e) {
            Log.e("XWALK_NFC", "Invalid action: illegal access.");
            response = InternalProtocolMessage.build(
                requestObject.id, "nfc_invalid_action", "illegal_access", false);
        } catch (InvocationTargetException e) {
            Log.e("XWALK_NFC", "Invalid action: invocation target.");
            Throwable t = e.getCause();
            Log.e("XWALK_NFC", "Cause: " + t.getMessage());
            response = InternalProtocolMessage.build(
                requestObject.id, "nfc_invalid_action", "invocation_target: " + t.getMessage(), false);
        }


        return response;
    }
}
