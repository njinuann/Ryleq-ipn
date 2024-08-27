//package com.starise.ipn.Util;
//
//import java.beans.Introspector;
//
//public class IpnUtil {
//    public String formatMsisdn(String msisdn, boolean local)
//    {
//        if (!isBlank(msisdn) && msisdn.length() >= 9)
//        {
//            msisdn = msisdn.split("[/\\\\,;]")[0].replaceAll("[^\\d]", "");
//            msisdn = msisdn.replaceAll("[\\D]", "");
//            if (msisdn.length() == 9)
//            {
//                return (local ? "0" : APController.countryCode) + msisdn;
//            }
//            else if (msisdn.startsWith(String.valueOf(APController.countryCode)))
//            {
//                return local ? "0" + msisdn.substring(3) : msisdn;
//            }
//            else if (msisdn.startsWith("0"))
//            {
//                return local ? msisdn : APController.countryCode + msisdn.substring(1);
//            }
//            else if (msisdn.startsWith("[\\D]")) //remove + for mobile captured after upgrade
//            {
//                return local ? msisdn : msisdn.substring(1);
//            }
//        }
//        return msisdn;
//    }
//    public String cleanText(String text)
//    {
//        String line;
//        StringBuilder buffer = new StringBuilder();
//        if (!isBlank(text))
//        {
//            try ( BufferedReader bis = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(text.getBytes()))))
//            {
//                while ((line = bis.readLine()) != null)
//                {
//                    buffer.append(line.trim());
//                }
//            }
//            catch (Exception ex)
//            {
//                getLog().logEvent(text, ex);
//            }
//        }
//        return buffer.toString().replaceAll(">\\s+<", "><");
//    }
//
//    public BigDecimal cleanAmount(String amountText, BigDecimal defaultValue)
//    {
//        return convertToType(checkBlank(amountText, "").replaceAll("[^\\d.]", ""), BigDecimal.class, defaultValue);
//    }
//
//    public <T> T cloneObject(Object source, Class<T> clazz)
//    {
//        T result = null;
//        if (source != null)
//        {
//            try
//            {
//                result = clazz.newInstance();
//                for (PropertyDescriptor propertyDesc : Introspector.getBeanInfo(source.getClass()).getPropertyDescriptors())
//                {
//                    if (propertyDesc.getReadMethod() != null)
//                    {
//                        Object value = propertyDesc.getReadMethod().invoke(source);
//                        if (propertyDesc.getWriteMethod() != null)
//                        {
//                            propertyDesc.getWriteMethod().invoke(result, value);
//                        }
//                    }
//                }
//            }
//            catch (Exception ex)
//            {
//                getLog().logEvent(ex);
//            }
//        }
//        return result;
//    }
//    public String cleanCsvList(String csvList)
//    {
//        ArrayList<String> list = new ArrayList<>();
//        if (!isBlank(csvList))
//        {
//            for (String listItem : csvList.replaceAll(";", ",").split(","))
//            {
//                if (!isBlank(listItem) && !list.contains(listItem))
//                {
//                    list.add(listItem.trim());
//                }
//            }
//        }
//        return createCsvList(list);
//    }
//}
