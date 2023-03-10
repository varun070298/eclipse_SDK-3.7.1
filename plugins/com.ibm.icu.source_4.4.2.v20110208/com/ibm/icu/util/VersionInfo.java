/*
 *******************************************************************************
 * Copyright (C) 1996-2011, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.util;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Class to store version numbers of the form major.minor.milli.micro.
 * @author synwee
 * @stable ICU 2.6
 */
public final class VersionInfo implements Comparable<VersionInfo>
{
    // public data members -------------------------------------------------

    /**
     * Unicode 1.0 version
     * @stable ICU 2.6
     */
    public static final VersionInfo UNICODE_1_0;
    /**
     * Unicode 1.0.1 version
     * @stable ICU 2.6
     */
    public static final VersionInfo UNICODE_1_0_1;
    /**
     * Unicode 1.1.0 version
     * @stable ICU 2.6
     */
    public static final VersionInfo UNICODE_1_1_0;
    /**
     * Unicode 1.1.5 version
     * @stable ICU 2.6
     */
    public static final VersionInfo UNICODE_1_1_5;
    /**
     * Unicode 2.0 version
     * @stable ICU 2.6
     */
    public static final VersionInfo UNICODE_2_0;
    /**
     * Unicode 2.1.2 version
     * @stable ICU 2.6
     */
    public static final VersionInfo UNICODE_2_1_2;
    /**
     * Unicode 2.1.5 version
     * @stable ICU 2.6
     */
    public static final VersionInfo UNICODE_2_1_5;
    /**
     * Unicode 2.1.8 version
     * @stable ICU 2.6
     */
    public static final VersionInfo UNICODE_2_1_8;
    /**
     * Unicode 2.1.9 version
     * @stable ICU 2.6
     */
    public static final VersionInfo UNICODE_2_1_9;
    /**
     * Unicode 3.0 version
     * @stable ICU 2.6
     */
    public static final VersionInfo UNICODE_3_0;
    /**
     * Unicode 3.0.1 version
     * @stable ICU 2.6
     */
    public static final VersionInfo UNICODE_3_0_1;
    /**
     * Unicode 3.1.0 version
     * @stable ICU 2.6
     */
    public static final VersionInfo UNICODE_3_1_0;
    /**
     * Unicode 3.1.1 version
     * @stable ICU 2.6
     */
    public static final VersionInfo UNICODE_3_1_1;
    /**
     * Unicode 3.2 version
     * @stable ICU 2.6
     */
    public static final VersionInfo UNICODE_3_2;

    /**
     * Unicode 4.0 version
     * @stable ICU 2.6
     */
    public static final VersionInfo UNICODE_4_0;

    /**
     * Unicode 4.0.1 version
     * @stable ICU 3.4
     */
    public static final VersionInfo UNICODE_4_0_1;

    /**
     * Unicode 4.1 version
     * @stable ICU 3.4
     */
    public static final VersionInfo UNICODE_4_1;

    /**
     * Unicode 5.0 version
     * @stable ICU 3.4
     */
    public static final VersionInfo UNICODE_5_0;

    /**
     * Unicode 5.1 version
     * @stable ICU 4.2
     */
    public static final VersionInfo UNICODE_5_1;

    /**
     * Unicode 5.2 version
     * @stable ICU 4.4
     */
    public static final VersionInfo UNICODE_5_2;

    /**
     * ICU4J current release version
     * @stable ICU 2.8
     */
    public static final VersionInfo ICU_VERSION;

    /**
     * Data version string for ICU's internal data.
     * Used for appending to data path (e.g. icudt43b)
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public static final String ICU_DATA_VERSION_PATH = "44b";
    
    /**
     * Data version in ICU4J.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public static final VersionInfo ICU_DATA_VERSION;

    /**
     * ICU4J collator runtime version
     * @stable ICU 2.8
     */
    public static final VersionInfo UCOL_RUNTIME_VERSION;

    /**
     * ICU4J collator builder version
     * @stable ICU 2.8
     */
    public static final VersionInfo UCOL_BUILDER_VERSION;

    /**
     * ICU4J collator tailorings version
     * @stable ICU 2.8
     */
    public static final VersionInfo UCOL_TAILORINGS_VERSION;


    // public methods ------------------------------------------------------

    /**
     * Returns an instance of VersionInfo with the argument version.
     * @param version version String in the format of "major.minor.milli.micro"
     *                or "major.minor.milli" or "major.minor" or "major",
     *                where major, minor, milli, micro are non-negative numbers
     *                <= 255. If the trailing version numbers are
     *                not specified they are taken as 0s. E.g. Version "3.1" is
     *                equivalent to "3.1.0.0".
     * @return an instance of VersionInfo with the argument version.
     * @exception IllegalArgumentException when the argument version
     *                is not in the right format
     * @stable ICU 2.6
     */
    public static VersionInfo getInstance(String version)
    {
        int length  = version.length();
        int array[] = {0, 0, 0, 0};
        int count   = 0;
        int index   = 0;

        while (count < 4 && index < length) {
            char c = version.charAt(index);
            if (c == '.') {
                count ++;
            }
            else {
                c -= '0';
                if (c < 0 || c > 9) {
                    throw new IllegalArgumentException(INVALID_VERSION_NUMBER_);
                }
                array[count] *= 10;
                array[count] += c;
            }
            index ++;
        }
        if (index != length) {
            throw new IllegalArgumentException(
                                               "Invalid version number: String '" + version + "' exceeds version format");
        }
        for (int i = 0; i < 4; i ++) {
            if (array[i] < 0 || array[i] > 255) {
                throw new IllegalArgumentException(INVALID_VERSION_NUMBER_);
            }
        }

        return getInstance(array[0], array[1], array[2], array[3]);
    }

    /**
     * Returns an instance of VersionInfo with the argument version.
     * @param major major version, non-negative number <= 255.
     * @param minor minor version, non-negative number <= 255.
     * @param milli milli version, non-negative number <= 255.
     * @param micro micro version, non-negative number <= 255.
     * @exception IllegalArgumentException when either arguments are negative or > 255
     * @stable ICU 2.6
     */
    public static VersionInfo getInstance(int major, int minor, int milli,
                                          int micro)
    {
        // checks if it is in the hashmap
        // else
        if (major < 0 || major > 255 || minor < 0 || minor > 255 ||
            milli < 0 || milli > 255 || micro < 0 || micro > 255) {
            throw new IllegalArgumentException(INVALID_VERSION_NUMBER_);
        }
        int     version = getInt(major, minor, milli, micro);
        Integer key     = new Integer(version);
        VersionInfo  result  = MAP_.get(key);
        if (result == null) {
            result = new VersionInfo(version);
            VersionInfo tmpvi = MAP_.putIfAbsent(key, result);
            if (tmpvi != null) {
                result = tmpvi;
            }
        }
        return result;
    }

    /**
     * Returns an instance of VersionInfo with the argument version.
     * Equivalent to getInstance(major, minor, milli, 0).
     * @param major major version, non-negative number <= 255.
     * @param minor minor version, non-negative number <= 255.
     * @param milli milli version, non-negative number <= 255.
     * @exception IllegalArgumentException when either arguments are
     *                                     negative or > 255
     * @stable ICU 2.6
     */
    public static VersionInfo getInstance(int major, int minor, int milli)
    {
        return getInstance(major, minor, milli, 0);
    }

    /**
     * Returns an instance of VersionInfo with the argument version.
     * Equivalent to getInstance(major, minor, 0, 0).
     * @param major major version, non-negative number <= 255.
     * @param minor minor version, non-negative number <= 255.
     * @exception IllegalArgumentException when either arguments are
     *                                     negative or > 255
     * @stable ICU 2.6
     */
    public static VersionInfo getInstance(int major, int minor)
    {
        return getInstance(major, minor, 0, 0);
    }

    /**
     * Returns an instance of VersionInfo with the argument version.
     * Equivalent to getInstance(major, 0, 0, 0).
     * @param major major version, non-negative number <= 255.
     * @exception IllegalArgumentException when either arguments are
     *                                     negative or > 255
     * @stable ICU 2.6
     */
    public static VersionInfo getInstance(int major)
    {
        return getInstance(major, 0, 0, 0);
    }

    private static VersionInfo javaVersion;

    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public static VersionInfo javaVersion() {
        if (javaVersion == null) {
            String s = System.getProperty("java.version");
            // clean string
            // preserve only digits, separated by single '.'
            // ignore over 4 digit sequences
            // does not test < 255, very odd...

            char[] chars = s.toCharArray();
            int r = 0, w = 0, count = 0;
            boolean numeric = false; // ignore leading non-numerics
            while (r < chars.length) {
                char c = chars[r++];
                if (c < '0' || c > '9') {
                    if (numeric) {
                        if (count == 3) {
                            // only four digit strings allowed
                            break;
                        }
                        numeric = false;
                        chars[w++] = '.';
                        ++count;
                    }
                } else {
                    numeric = true;
                    chars[w++] = c;
                }
            }
            while (w > 0 && chars[w-1] == '.') {
                --w;
            }

            String vs = new String(chars, 0, w);

            javaVersion = VersionInfo.getInstance(vs);
        }
        return javaVersion;
    }

    /**
     * Returns the String representative of VersionInfo in the format of
     * "major.minor.milli.micro"
     * @return String representative of VersionInfo
     * @stable ICU 2.6
     */
    public String toString()
    {
        StringBuilder result = new StringBuilder(7);
        result.append(getMajor());
        result.append('.');
        result.append(getMinor());
        result.append('.');
        result.append(getMilli());
        result.append('.');
        result.append(getMicro());
        return result.toString();
    }

    /**
     * Returns the major version number
     * @return the major version number
     * @stable ICU 2.6
     */
    public int getMajor()
    {
        return (m_version_ >> 24) & LAST_BYTE_MASK_ ;
    }

    /**
     * Returns the minor version number
     * @return the minor version number
     * @stable ICU 2.6
     */
    public int getMinor()
    {
        return (m_version_ >> 16) & LAST_BYTE_MASK_ ;
    }

    /**
     * Returns the milli version number
     * @return the milli version number
     * @stable ICU 2.6
     */
    public int getMilli()
    {
        return (m_version_ >> 8) & LAST_BYTE_MASK_ ;
    }

    /**
     * Returns the micro version number
     * @return the micro version number
     * @stable ICU 2.6
     */
    public int getMicro()
    {
        return m_version_ & LAST_BYTE_MASK_ ;
    }

    /**
     * Checks if this version information is equals to the argument version
     * @param other object to be compared
     * @return true if other is equals to this object's version information,
     *         false otherwise
     * @stable ICU 2.6
     */
    public boolean equals(Object other)
    {
        return other == this;
    }

    /**
     * Compares other with this VersionInfo.
     * @param other VersionInfo to be compared
     * @return 0 if the argument is a VersionInfo object that has version
     *           information equals to this object.
     *           Less than 0 if the argument is a VersionInfo object that has
     *           version information greater than this object.
     *           Greater than 0 if the argument is a VersionInfo object that
     *           has version information less than this object.
     * @stable ICU 2.6
     */
    public int compareTo(VersionInfo other)
    {
        return m_version_ - other.m_version_;
    }

    // private data members ----------------------------------------------

    /**
     * Version number stored as a byte for each of the major, minor, milli and
     * micro numbers in the 32 bit int.
     * Most significant for the major and the least significant contains the
     * micro numbers.
     */
    private int m_version_;
    /**
     * Map of singletons
     */
    private static final ConcurrentHashMap<Integer, VersionInfo> MAP_ = new ConcurrentHashMap<Integer, VersionInfo>();
    /**
     * Last byte mask
     */
    private static final int LAST_BYTE_MASK_ = 0xFF;
    /**
     * Error statement string
     */
    private static final String INVALID_VERSION_NUMBER_ =
        "Invalid version number: Version number may be negative or greater than 255";

    // static declaration ------------------------------------------------

    /**
     * Initialize versions only after MAP_ has been created
     */
    static {
        UNICODE_1_0   = getInstance(1, 0, 0, 0);
        UNICODE_1_0_1 = getInstance(1, 0, 1, 0);
        UNICODE_1_1_0 = getInstance(1, 1, 0, 0);
        UNICODE_1_1_5 = getInstance(1, 1, 5, 0);
        UNICODE_2_0   = getInstance(2, 0, 0, 0);
        UNICODE_2_1_2 = getInstance(2, 1, 2, 0);
        UNICODE_2_1_5 = getInstance(2, 1, 5, 0);
        UNICODE_2_1_8 = getInstance(2, 1, 8, 0);
        UNICODE_2_1_9 = getInstance(2, 1, 9, 0);
        UNICODE_3_0   = getInstance(3, 0, 0, 0);
        UNICODE_3_0_1 = getInstance(3, 0, 1, 0);
        UNICODE_3_1_0 = getInstance(3, 1, 0, 0);
        UNICODE_3_1_1 = getInstance(3, 1, 1, 0);
        UNICODE_3_2   = getInstance(3, 2, 0, 0);
        UNICODE_4_0   = getInstance(4, 0, 0, 0);
        UNICODE_4_0_1 = getInstance(4, 0, 1, 0);
        UNICODE_4_1   = getInstance(4, 1, 0, 0);
        UNICODE_5_0   = getInstance(5, 0, 0, 0);
        UNICODE_5_1   = getInstance(5, 1, 0, 0);
        UNICODE_5_2   = getInstance(5, 2, 0, 0);
        ICU_VERSION   = getInstance(4, 4, 2, 1);
        ICU_DATA_VERSION = getInstance(4, 4, 0, 1);
        UCOL_RUNTIME_VERSION = getInstance(6);
        UCOL_BUILDER_VERSION = getInstance(7);
        UCOL_TAILORINGS_VERSION = getInstance(1);
    }

    // private constructor -----------------------------------------------

    /**
     * Constructor with int
     * @param compactversion a 32 bit int with each byte representing a number
     */
    private VersionInfo(int compactversion)
    {
        m_version_ = compactversion;
    }

    /**
     * Gets the int from the version numbers
     * @param major non-negative version number
     * @param minor non-negativeversion number
     * @param milli non-negativeversion number
     * @param micro non-negativeversion number
     */
    private static int getInt(int major, int minor, int milli, int micro)
    {
        return (major << 24) | (minor << 16) | (milli << 8) | micro;
    }

}
