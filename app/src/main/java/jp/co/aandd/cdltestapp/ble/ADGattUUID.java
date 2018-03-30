package jp.co.aandd.cdltestapp.ble;

/**
 * Created by sbhattacharya on 3/29/18.
 */

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class ADGattUUID {
    public static final UUID ClientCharacteristicConfiguration = uuidFromShortString("2902");
    /*
     * A&D Custom
     */
    public static final UUID AndCustomWeightScaleService = UUID.fromString("23434100-1FE4-1EFF-80CB-00FF78297D8B");
    public static final UUID AndCustomWeightScaleMeasurement = UUID.fromString("23434101-1FE4-1EFF-80CB-00FF78297D8B");

    public static final UUID AndCustomService = UUID.fromString("233BF000-5A34-1B6D-975C-000D5690ABE4");
    public static final UUID AndCustomCharacteristic = UUID.fromString("233BF001-5A34-1B6D-975C-000D5690ABE4");

	/*
     * Services
     */

    public static final UUID BloodPressureService = uuidFromShortString("1810");
    public static final UUID CurrentTimeService = uuidFromShortString("1805");
    public static final UUID DeviceInformationService = uuidFromShortString("180a");
    public static final UUID WeightScaleService = uuidFromShortString("181d");

    /*
     * Characteristics
     */

    public static final UUID BloodPressureFeature = uuidFromShortString("2a49");
    public static final UUID BloodPressureMeasurement = uuidFromShortString("2a35");
    public static final UUID CurrentTime = uuidFromShortString("2a2b");
    public static final UUID DateTime = uuidFromShortString("2a08");
    public static final UUID FirmwareRevisionString = uuidFromShortString("2a26");
    public static final UUID SerialNumberString = uuidFromShortString("2a25"); //Sim added for Serial number
    public static final UUID TemperatureMeasurement = uuidFromShortString("2a1c");
    public static final UUID TemperatureType = uuidFromShortString("2a1d");
    public static final UUID WeightScaleMeasurement = uuidFromShortString("2a9d");



    public static List<UUID> ServicesUUIDs = new ArrayList<UUID>();
    public static List<UUID> MeasuCharacUUIDs = new ArrayList<UUID>();

    static {
        ServicesUUIDs.add(AndCustomWeightScaleService);
        ServicesUUIDs.add(BloodPressureService);
        ServicesUUIDs.add(WeightScaleService);

        MeasuCharacUUIDs.add(AndCustomWeightScaleMeasurement);
        MeasuCharacUUIDs.add(BloodPressureMeasurement);
        MeasuCharacUUIDs.add(WeightScaleMeasurement);

    }


    public static UUID uuidFromShortString(String uuid) {
        return UUID.fromString(String.format("0000%s-0000-1000-8000-00805f9b34fb", uuid));
    }
}
