/*
 *******************************************************************************
 * Copyright (C) 1996-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.util;

import java.util.Date;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * <b>Note:</b> The Holiday framework is a technology preview.
 * Despite its age, is still draft API, and clients should treat it as such.
 * 
 * An abstract class representing a holiday.
 * @draft ICU 2.8 (retainAll)
 * @provisional This API might change or be removed in a future release.
 */
public abstract class Holiday implements DateRule
{
    /**
     * @draft ICU 2.8
     * @provisional This API might change or be removed in a future release.
     */
    public static Holiday[] getHolidays()
    {
        return getHolidays(ULocale.getDefault());
    }

    /**
     * @draft ICU 2.8
     * @provisional This API might change or be removed in a future release.
     */
    public static Holiday[] getHolidays(Locale locale)
    {
        return getHolidays(ULocale.forLocale(locale));
    }

    /**
     * @draft ICU 3.2
     * @provisional This API might change or be removed in a future release.
     */
    public static Holiday[] getHolidays(ULocale locale)
    {
        Holiday[] result = noHolidays;

        try {
            ResourceBundle bundle = UResourceBundle.getBundleInstance("com.ibm.icu.impl.data.HolidayBundle", locale);

            result = (Holiday[]) bundle.getObject("holidays");
        }
        catch (MissingResourceException e) {
        }
        return result;
    }

    /**
     * Return the first occurrence of this holiday on or after the given date
     *
     * @param start Only holidays on or after this date are returned.
     *
     * @return      The date on which this holiday occurs, or null if it
     *              does not occur on or after the start date.
     *
     * @see #firstBetween
     * @draft ICU 2.8
     * @provisional This API might change or be removed in a future release.
     */
    public Date firstAfter(Date start) {
        return rule.firstAfter(start);
    }

    /**
     * Return the first occurrence of this holiday that is on or after
     * the given start date and before the given end date.
     *
     * @param start Only occurrences on or after this date are returned.
     * @param end   Only occurrences before this date are returned.
     *
     * @return      The date on which this event occurs, or null if it
     *              does not occur between the start and end dates.
     *
     * @see #firstAfter
     * @draft ICU 2.8
     * @provisional This API might change or be removed in a future release.
     */
    public Date firstBetween(Date start, Date end) {
        return rule.firstBetween(start, end);
    }

    /**
     * Checks whether this holiday falls on the given date.  This does
     * <em>not</em> take time of day into account; instead it checks
     * whether the holiday and the given date are on the same day.
     *
     * @param date  The date to check.
     * @return      true if this holiday occurs on the given date.
     * @draft ICU 2.8
     * @provisional This API might change or be removed in a future release.
     */
    public boolean isOn(Date date) {
        //System.out.println(name + ".isOn(" + date.toString() + "):");
        return rule.isOn(date);
    }

    /**
     * Check whether this holiday occurs at least once between the two
     * dates given.
     * @draft ICU 2.8
     * @provisional This API might change or be removed in a future release.
     */
    public boolean isBetween(Date start, Date end) {
        return rule.isBetween(start, end);
    }

    /**
     * Construct a new Holiday object.  This is for use by subclasses only.
     * This constructs a new holiday with the given name and date rules.
     *
     * @param name  The name of this holiday.  The getDisplayName method
     *              uses this string as a key to look up the holiday's name a
     *              resource bundle object named HolidayBundle.
     *
     * @param rule  The date rules used for determining when this holiday
     *              falls.  Holiday's implementation of the DateRule interface
     *              simply delegates to this DateRule object.
     * @draft ICU 2.8
     * @provisional This API might change or be removed in a future release.
     */
    protected Holiday(String name, DateRule rule)
    {
        this.name = name;
        this.rule = rule;
    }

    /**
     * Return the name of this holiday in the language of the default locale.
     * @draft ICU 2.8
     * @provisional This API might change or be removed in a future release.
     */
    public String getDisplayName() {
        return getDisplayName(ULocale.getDefault());
    }

    /**
     * Return the name of this holiday in the language of the specified locale.
     * The <code>name</code> parameter passed to this object's constructor is used
     * as a key to look up the holiday's localized name in a ResourceBundle object
     * named HolidayBundle.
     *
     * @param locale   A locale specifying the language in which the name is desired.
     *
     * @see ResourceBundle
     * @draft ICU 2.8
     * @provisional This API might change or be removed in a future release.
     */
    public String getDisplayName(Locale locale)
    {
        return getDisplayName(ULocale.forLocale(locale));
    }

    /**
     * Return the name of this holiday in the language of the specified locale
     * The <code>name</code> parameter passed to this object's constructor is used
     * as a key to look up the holiday's localized name in a ResourceBundle object
     * named HolidayBundle.
     *
     * @param locale   A locale specifying the language in which the name is desired.
     *
     * @see ResourceBundle
     * @draft ICU 3.2
     * @provisional This API might change or be removed in a future release.
     */
    public String getDisplayName(ULocale locale)
    {
        String dispName = name;

        try {
            ResourceBundle bundle = UResourceBundle.getBundleInstance("com.ibm.icu.impl.data.HolidayBundle", locale);
            dispName = bundle.getString(name);
        }
        catch (MissingResourceException e) {
        }
        return dispName;
    }

    /**
     * @draft ICU 2.8
     * @provisional This API might change or be removed in a future release.
     */
    public DateRule getRule() {
        return rule;
    }

    /**
     * @draft ICU 2.8
     * @provisional This API might change or be removed in a future release.
     */
    public void setRule(DateRule rule) {
        this.rule = rule;
    }

    private String      name;
    private DateRule    rule;

    private static Holiday[] noHolidays = {};
}
