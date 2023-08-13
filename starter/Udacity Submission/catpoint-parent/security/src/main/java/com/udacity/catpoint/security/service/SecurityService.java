package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.service.ImageService;
import com.udacity.catpoint.security.application.StatusListener;
import com.udacity.catpoint.security.data.AlarmStatus;
import com.udacity.catpoint.security.data.ArmingStatus;
import com.udacity.catpoint.security.data.SecurityRepository;
import com.udacity.catpoint.security.data.Sensor;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

/**
 * Service that receives information about changes to the security system. Responsible for
 * forwarding updates to the repository and making any decisions about changing the system state.
 * <p>
 * This is the class that should contain most of the business logic for our system, and it is the
 * class you will be writing unit tests for.
 */
public class SecurityService {

    final private ImageService imageService;
    final private SecurityRepository securityRepository;
    final private Set<StatusListener> statusListeners = new HashSet<>();

    private boolean cameraShowsCat = false;

    public SecurityService(SecurityRepository securityRepository, ImageService imageService) {
        this.securityRepository = securityRepository;
        this.imageService = imageService;
    }

    /**
     * Sets the current arming status for the system. Changing the arming status
     * may update both the alarm status.
     *
     * @param armingStatus
     */
    public void setArmingStatus(ArmingStatus armingStatus) {

        //  Requirement 9:  If the system is disarmed, set the status to no alarm.
        //  Requirement 10: If the system is armed, reset all sensors to inactive.
        //  Requirement 11: If the system is armed-home while the camera shows a cat, set the alarm status to alarm.

        switch (armingStatus) {
            case DISARMED:
                setAlarmStatus(AlarmStatus.NO_ALARM);
                break;
            case ARMED_HOME:
                if (cameraShowsCat) setAlarmStatus(AlarmStatus.ALARM);
                //no break :  intentional falling through switch case
            case ARMED_AWAY:
                Set<Sensor> sensors = new HashSet<>(getSensors());
                sensors.stream().forEach(s -> changeSensorActivationStatus(s, false));
                break;
        }
        securityRepository.setArmingStatus(armingStatus);
    }

    private boolean anySensorActive() {

        return getSensors().stream().anyMatch(Sensor::getActive);
    }

    /**
     * Internal method that handles alarm status changes based on whether
     * the camera currently shows a cat.
     *
     * @param cat True if a cat is detected, otherwise false.
     */
    private void catDetected(Boolean cat) {
        // Requirement 7 : If the image service identifies an image containing a cat while the system is armed-home, put the system into alarm status.
        // Requirement 8 :  If the image service identifies an image that does not contain a cat, change the status to no alarm as long as the sensors are not active.
        if (cat && getArmingStatus() == ArmingStatus.ARMED_HOME) {
            setAlarmStatus(AlarmStatus.ALARM);
        } else if (!cat && !anySensorActive()) {
            setAlarmStatus(AlarmStatus.NO_ALARM);
        }
        cameraShowsCat = cat;
        statusListeners.stream().forEach(sl -> sl.catDetected(cat));
    }

    /**
     * Register the StatusListener for alarm system updates from within the SecurityService.
     *
     * @param statusListener
     */
    public void addStatusListener(StatusListener statusListener) {
        statusListeners.add(statusListener);
    }

    public void removeStatusListener(StatusListener statusListener) {
        statusListeners.remove(statusListener);
    }

    /**
     * Change the alarm status of the system and notify all listeners.
     *
     * @param status
     */
    public void setAlarmStatus(AlarmStatus status) {
        securityRepository.setAlarmStatus(status);
        statusListeners.stream().forEach(sl -> sl.notify(status));
    }

    /**
     * Internal method for updating the alarm status when a sensor has been activated.
     */
    private void handleSensorActivated() {
        // Requirement 1: If alarm is armed and a sensor becomes activated, put the system into pending alarm status.
        // Requirement 2: If alarm is armed and a sensor becomes activated and the system is already pending alarm, set the alarm status to alarm.
        // Requirement 4 : If alarm is active, change in sensor state should not affect the alarm state.
        if (getArmingStatus() == ArmingStatus.ARMED_HOME
                || getArmingStatus() == ArmingStatus.ARMED_AWAY) {
            switch (getAlarmStatus()) {
                case PENDING_ALARM -> setAlarmStatus(AlarmStatus.ALARM);
                case NO_ALARM -> setAlarmStatus(AlarmStatus.PENDING_ALARM);
            }
        }

    }

    /**
     * Internal method for updating the alarm status when a sensor has been deactivated
     */
    private void handleSensorDeactivated() {
        // Requirement 3 : If pending alarm and all sensors are inactive, return to no alarm state.
        // Requirement 4 : If alarm is active, change in sensor state should not affect the alarm state.
        if (getAlarmStatus() == AlarmStatus.PENDING_ALARM && !anySensorActive()) {
            setAlarmStatus(AlarmStatus.NO_ALARM);
        }
    }

    /**
     * Change the activation status for the specified sensor and update alarm status if necessary.
     *
     * @param sensor
     * @param active
     */
    public void changeSensorActivationStatus(Sensor sensor, Boolean active) {
        // Requirement 5 : If a sensor is activated while already active and the system is in pending state, change it to alarm state.
        // Requirement 6 : If a sensor is deactivated while already inactive, make no changes to the alarm state.

        boolean priorActivation = sensor.getActive();

        // Update Sensor with change
        sensor.setActive(active);
        securityRepository.updateSensor(sensor);
        statusListeners.stream().forEach(StatusListener::sensorStatusChanged);

        // Handle Status change
        if (active) {
            handleSensorActivated();
        } else if (priorActivation) {
            handleSensorDeactivated();
        }

    }

    /**
     * Send an image to the SecurityService for processing. The securityService will use the provided
     * ImageService to analyze the image for cats and update the alarm status accordingly.
     *
     * @param currentCameraImage
     */
    public void processImage(BufferedImage currentCameraImage) {
        catDetected(imageService.imageContainsCat(currentCameraImage, 50.0f));
    }

    public AlarmStatus getAlarmStatus() {
        return securityRepository.getAlarmStatus();
    }

    public Set<Sensor> getSensors() {
        return securityRepository.getSensors();
    }

    public void addSensor(Sensor sensor) {
        securityRepository.addSensor(sensor);
        if (sensor.getActive()) handleSensorActivated();
    }

    public void removeSensor(Sensor sensor) {
        securityRepository.removeSensor(sensor);
        if (sensor.getActive()) handleSensorDeactivated();
    }

    public ArmingStatus getArmingStatus() {
        return securityRepository.getArmingStatus();
    }

}
