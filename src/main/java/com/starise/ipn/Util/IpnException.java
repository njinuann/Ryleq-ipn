//package com.starise.ipn.Util;
//
//import com.starise.ipn.model.ErrorData;
//
//public class IpnException  extends RuntimeException {
//
//    private static final long serialVersionUID = 7718828512143293558L;
//
//    private final ErrorData code;
//
//    public IpnException(ErrorData code) {
//        super(code.getMessage());
//        this.code = code;
//    }
//
//    public ErrorData getErrorMessage() {
//        return this.code;
//    }
//}