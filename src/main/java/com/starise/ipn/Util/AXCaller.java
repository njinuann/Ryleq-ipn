package com.starise.ipn.Util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.*;

@Component
public final class AXCaller implements Serializable
{
    private String callerTag = "caller";
    private final String indent = "\t";
    private final HashMap<String, Object> calls = new HashMap<>();
    private ArrayList<Exception> exceptionsList = new ArrayList();

    public void logException(Exception ex) {
        getExceptionsList().add(ex);
    }

    public String toString()
    {
        StringBuilder buffer = new StringBuilder("<" + getCallerTag() + ">");
        for (String key : sortArray(getCalls().keySet().toArray(new String[getCalls().size()]), true))
        {
            Object payload = getCalls().get(key);
            if (payload instanceof Throwable)
            {

                    buffer.append("\r\n").append(getIndent()).append("<exception>");
                    buffer.append("\r\n").append(getIndent()).append(getIndent()).append("<class>").append(((Throwable) payload).getClass().getSimpleName()).append("</class>");
                    String emsg = (((Throwable) payload).getMessage() == null) ? "" : ((Throwable) payload).getMessage();

                    if (emsg.contains("\r\n"))
                    {
                        buffer.append("\r\n").append(getIndent()).append(getIndent()).append("<message>");
                        buffer.append("\r\n").append(getIndent()).append(getIndent()).append(getIndent()).append(((Throwable) payload).getMessage().replaceAll("\r\n", "\r\n" + getIndent() + getIndent() + getIndent()));
                        buffer.append("\r\n").append(getIndent()).append(getIndent()).append("</message>");
                    }
                    else
                    {
                        buffer.append("\r\n").append(getIndent()).append(getIndent()).append("<message>").append(((Throwable) payload).getMessage()).append("</message>");
                    }
                    buffer.append("\r\n").append(getIndent()).append(getIndent()).append("<stacktrace>");
                    for (StackTraceElement s : ((Throwable) payload).getStackTrace())
                    {
                        buffer.append("\r\n").append(getIndent()).append(getIndent()).append(getIndent()).append("at ").append(s.toString());
                    }
                    buffer.append("\r\n").append(getIndent()).append(getIndent()).append("</stacktrace>");
                    buffer.append("\r\n").append(getIndent()).append("</exception>");

            }
            else
            {
                buffer.append("\r\n").append(getIndent()).append("<").append(key.replaceAll("\\d", "")).append(">").append(cleanText(getCalls().get(key).toString())).append("</").append(key.replaceAll("\\d", "")).append(">");
            }
        }
        for (Object exception : getExceptionsList().toArray()) {
            if (exception != null) {

                buffer.append("\r\n").append(getIndent()).append("<exception>");
                buffer.append("\r\n").append(getIndent()).append(getIndent()).append("<class>").append(((Exception) exception).getClass().getSimpleName()).append("</class>");
                String emsg = (((Exception) exception).getMessage() == null) ? "" : ((Exception) exception).getMessage();

                if (emsg.contains("\r\n")) {
                    buffer.append("\r\n").append(getIndent()).append(getIndent()).append("<message>");
                    buffer.append("\r\n").append(getIndent()).append(getIndent()).append(getIndent()).append(((Exception) exception).getMessage().replaceAll("\r\n", "\r\n" + getIndent() + getIndent() + getIndent()));
                    buffer.append("\r\n").append(getIndent()).append(getIndent()).append("</message>");
                } else {
                    buffer.append("\r\n").append(getIndent()).append(getIndent()).append("<message>").append(((Exception) exception).getMessage()).append("</message>");
                }
                buffer.append("\r\n").append(getIndent()).append(getIndent()).append("<stacktrace>");
                for (StackTraceElement s : ((Throwable) exception).getStackTrace()) {
                    buffer.append("\r\n").append(getIndent()).append(getIndent()).append(getIndent()).append("at ").append(s.toString());
                }
                buffer.append("\r\n").append(getIndent()).append(getIndent()).append("</stacktrace>");
                buffer.append("\r\n").append(getIndent()).append("</exception>");

            }
        }


        buffer.append("\r\n").append("</").append(getCallerTag()).append(">");
        return buffer.toString();
    }

    public String convertToString(Object object, String tag)
    {
        tag = decapitalize(tag);
        String text = "", items = "", prevVal = "";
        Class<?> beanClass = !isBlank(object) ? object.getClass() : String.class;
        try
        {
            if (isBlank(object))
            {
                return !isBlank(tag) ? (tag + "=<" + String.valueOf(object) + ">") : String.valueOf(object);
            }
            for (MethodDescriptor methodDescriptor : Introspector.getBeanInfo(beanClass).getMethodDescriptors())
            {
                if ("toString".equalsIgnoreCase(methodDescriptor.getName()) && beanClass == methodDescriptor.getMethod().getDeclaringClass() && !(methodDescriptor.getMethod().getAnnotation(AXIgnore.class) instanceof AXIgnore))
                {
                    return !isBlank(tag) ? (tag + "=<" + String.valueOf(object) + ">") : String.valueOf(object);
                }
            }
            if (object instanceof byte[])
            {
                return !isBlank(tag) ? (tag + "=<" + new String((byte[]) object) + ">") : new String((byte[]) object);
            }

            tag = isBlank(tag) ? beanClass.getSimpleName() : tag;

            if (object instanceof Collection)
            {
                for (Object item : ((Collection) object).toArray())
                {
                    items += (isBlank(items) ? "" : ", ") + (prevVal.contains("\r\n") ? "\r\n" : "") + (prevVal = convertToString(item, null)).trim();
                }
                return items.contains("\r\n") ? ("\r\n" + tag + "=<[" + "\r\n\t" + items + "\r\n" + "]>") : (tag + "=<[" + (!isBlank(items) ? " " + items + " " : "") + "]>");
            }
            else if (object instanceof Map)
            {
                for (Object key : ((Map) object).keySet())
                {
                    items += (isBlank(items) ? "" : ", ") + (prevVal.contains("\r\n") ? "\r\n" : "") + (prevVal = convertToString(((Map) object).get(key), String.valueOf(key))).trim();
                }
                return items.contains("\r\n") ? ("\r\n" + tag + "=<[" + "\r\n\t" + items + "\r\n" + "]>") : (tag + "=<[" + (!isBlank(items) ? " " + items + " " : "") + "]>");
            }
            else if (beanClass.isArray())
            {
                switch (beanClass.getSimpleName())
                {
                    case "int[]":
                        return (tag + "=<" + Arrays.toString((int[]) object) + ">");
                    case "long[]":
                        return (tag + "=<" + Arrays.toString((long[]) object) + ">");
                    case "boolean[]":
                        return (tag + "=<" + Arrays.toString((boolean[]) object) + ">");
                    case "byte[]":
                        return (tag + "=<" + Arrays.toString((byte[]) object) + ">");
                    case "char[]":
                        return (tag + "=<" + Arrays.toString((char[]) object) + ">");
                    case "double[]":
                        return (tag + "=<" + Arrays.toString((double[]) object) + ">");
                    case "float[]":
                        return (tag + "=<" + Arrays.toString((float[]) object) + ">");
                    case "short[]":
                        return (tag + "=<" + Arrays.toString((short[]) object) + ">");
                }
                if (object instanceof Object[])
                {
                    for (Object item : (Object[]) object)
                    {
                        items += (isBlank(items) ? "" : ", ") + (prevVal.contains("\r\n") ? "\r\n\t" : "") + (prevVal = convertToString(item, null)).trim();
                    }
                    return items.contains("\r\n") ? ("\r\n" + tag + "=<[" + "\r\n\t" + items + "\r\n" + "]>") : (tag + "=<[" + (!isBlank(items) ? " " + items + " " : "") + "]>");
                }
                else
                {
                    return (tag + "=<[" + String.valueOf(object) + "]>");
                }
            }
            else
            {
                Method readMethod;
                for (PropertyDescriptor propertyDesc : Introspector.getBeanInfo(beanClass).getPropertyDescriptors())
                {
                    if ((readMethod = propertyDesc.getReadMethod()) != null)
                    {
                        Object value = readMethod.invoke(object);
                        if (!(value instanceof Class))
                        {
                            text += (isBlank(text) ? "" : ", ") + convertToString(value, propertyDesc.getName());
                        }
                    }
                }
            }
        }
        catch (Exception ex)
        {
            addError(ex);
        }
        return (!isBlank(tag) ? (tag + "=<[ " + (isBlank(text) ? String.valueOf(object) : text) + " ]>") : (isBlank(text) ? String.valueOf(object) : text));
    }

    public String indentAllLines(String text, String indent)
    {
        String line = "", buffer = "";
        try (BufferedReader bis = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(text.getBytes()))))
        {
            while (line != null)
            {
                buffer += indent + line + "\r\n";
                line = bis.readLine();
            }
            return indent + buffer.trim();
        }
        catch (Exception ex)
        {
            return indent + buffer.trim();
        }
    }

    public String cleanText(String text)
    {
        String line;
        StringBuilder buffer = new StringBuilder();
        if (!isBlank(text))
        {
            try (BufferedReader bis = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(text.getBytes()))))
            {
                while ((line = bis.readLine()) != null)
                {
                    buffer.append(line.trim());
                }
            }
            catch (Exception ex)
            {
                ex = null;
            }
        }
        return buffer.toString().replaceAll(">\\s+<", "><");
    }

    public String capitalize(String text, boolean convertAllXters)
    {
        if (text != null ? text.length() > 0 : false)
        {
            StringBuilder builder = new StringBuilder();
            for (String word : text.replace("_", " ").split("\\s"))
            {
                builder.append(word.length() > 2 ? word.substring(0, 1).toUpperCase() + (convertAllXters ? word.substring(1).toLowerCase() : word.substring(1)) : word.toLowerCase()).append(" ");
            }
            return builder.toString().trim();
        }
        return text;
    }

    public String decapitalize(String text)
    {
        if (text != null ? text.length() > 0 : false)
        {
            StringBuilder builder = new StringBuilder();
            for (String word : text.split("\\s"))
            {
                builder.append(word.substring(0, 1).toLowerCase()).append(word.substring(1)).append(" ");
            }
            return builder.toString().trim();
        }
        return text;
    }

    public <T> T[] sortArray(T[] array, boolean ascending)
    {
        Arrays.sort(array, ascending ? ((Comparator<T>) (T o1, T o2) -> ((Comparable) o1).compareTo(o2)) : ((Comparator<T>) (T o1, T o2) -> ((Comparable) o2).compareTo(o1)));
        return array;
    }

    public boolean isBlank(Object object)
    {
        return object == null || "".equals(String.valueOf(object).trim()) || "null".equals(String.valueOf(object).trim()) || String.valueOf(object).trim().toLowerCase().contains("---select");
    }

    public String convertToString(Object object)
    {
        return convertToString(object, null).trim();
    }

    public void setReference(String reference)
    {
        setCall("reference", blankNull(reference));
    }

    public void setAlertCode(String alertCode)
    {
        setCall("alertcode", blankNull(alertCode));
    }

    public void setAccount(String account)
    {
        setCall("account", blankNull(account));
    }

    public void setNarration(String narration)
    {
        setCall("narration", blankNull(narration));
    }

    public void setOurTerminal(boolean ourTerminal)
    {
        setCall("ourterminal", yesNo(ourTerminal));
    }

    public void setOurCustomer(boolean ourCustomer)
    {
        setCall("ourcustomer", yesNo(ourCustomer));
    }

    public void setIsReversal(boolean isReversal)
    {
        setCall("isreversal", yesNo(isReversal));
    }

    public void setRequest(Object request)
    {
        setCall("request", request);
    }

    public void setResponse(Object response)
    {
        setCall("response", response);
    }

    public void setResult(String result)
    {
        setCall("result", blankNull(result));
    }

    public void setDuration(String duration)
    {
        setCall("duration", blankNull(duration));
    }

    public String yesNo(boolean isYes)
    {
        return isYes ? "Yes" : "No";
    }

    public String blankNull(String value)
    {
        return checkBlank(value, "");
    }

    public <T> T checkBlank(T value, T nillValue)
    {
        return !isBlank(value) ? value : nillValue;
    }

    public String capitalize(String text)
    {
        return capitalize(text, true);
    }

    /**
     * @return the indent
     */
    public String getIndent()
    {
        return indent;
    }

    /**
     * @return the calls
     */
    public HashMap<String, Object> getCalls()
    {
        return calls;
    }

    public void addError(Throwable ex)
    {
        setCall("error", ex);
    }

    public void setCall(String callRef, Object callObject)
    {
        setCall(callRef, callObject, false);
    }

    public void setCall(String callRef, Object callObject, boolean replace)
    {
        getCalls().put(String.format("%02d", (replace ? getCalls().size() - 1 : getCalls().size())) + callRef.toLowerCase(), convertToString(callObject));
    }

    public void dump(PrintStream p, String indent)
    {
        p.print(indentAllLines(toString(), indent));
    }

    /**
     * @return the callerTag
     */
    public String getCallerTag()
    {
        return "exec";
    }

    /**
     * @param callerTag the callerTag to set
     */
    public void setCallerTag(String callerTag)
    {
        this.callerTag = "exec";
    }

    public ArrayList<Exception> getExceptionsList()
    {
        return exceptionsList;
    }

    public void setExceptionsList(ArrayList<Exception> exceptionsList)
    {
        this.exceptionsList = exceptionsList;
    }
}
