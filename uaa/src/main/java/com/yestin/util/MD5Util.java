package com.yestin.util;

import org.apache.commons.codec.digest.DigestUtils;

public class MD5Util {
    public static String encode(String pwd, String salt) {
        return DigestUtils.md5Hex("" + salt.charAt(0) + salt.charAt(2) + pwd + salt.charAt(4) + salt.charAt(5));
    }
}
