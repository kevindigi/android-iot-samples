package com.digicorp.helper;

/**
 * @author kevin.adesara on 07/07/17.
 */

public class BeaconIdentifier {
    public static boolean isBeacon(byte[] scanData) {
        int startByte = 2;
        boolean patternFound = false;
        while (startByte <= 5) {
            if (((int) scanData[startByte + 2] & 0xff) == 0x02 &&
                    ((int) scanData[startByte + 3] & 0xff) == 0x15) {
                // yes!  This is an iBeacon
                patternFound = true;
                break;
            }
            startByte++;
        }

        return patternFound;
    }
}
