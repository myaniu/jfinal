package com.jfinal;

public class Version {
	public final static int MajorVersion    = 3;
    public final static int MinorVersion    = 1;
    public final static int RevisionVersion = 0;
    public final static String extVersion   = "-java8-v1";

    public static final String VERSION    = Version.MajorVersion + "." + Version.MinorVersion + "." + Version.RevisionVersion + extVersion;
    public static final String RELEASE_AT = "2017-06-11";
    public static final String RELEASE_NOTE="向jfinal3.1迁移";
    private Version() {
    }
}
