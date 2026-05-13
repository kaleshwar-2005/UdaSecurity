package com.udacity.catpoint.security.service;

import com.udacity.catpoint.application.StatusListener;
import com.udacity.catpoint.data.AlarmStatus;
import com.udacity.catpoint.data.ArmingStatus;
import com.udacity.catpoint.data.SecurityRepository;
import com.udacity.catpoint.data.Sensor;
import com.udacity.catpoint.image.service.ImageService;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

/**
 * Service that handles security system logic.
 */
public class SecurityService {

    private final ImageService imageService;

    private final SecurityRepository securityRepository;

    private final Set<StatusListener> statusListeners =
            new HashSet<>();

    // Track cat detection state
    private boolean catDetected = false;

    public SecurityService(
            SecurityRepository securityRepository,
            ImageService imageService) {

        this.securityRepository =
                securityRepository;

        this.imageService =
                imageService;
    }

    /**
     * Sets arming status.
     */
    public void setArmingStatus(
            ArmingStatus armingStatus) {

        // DISARMED -> NO_ALARM
        if (armingStatus ==
                ArmingStatus.DISARMED) {

            setAlarmStatus(
                    AlarmStatus.NO_ALARM);
        }

        // IMPORTANT:
        // Set arming status FIRST
        securityRepository
                .setArmingStatus(armingStatus);

        // Reset all sensors when armed
        if (armingStatus ==
                ArmingStatus.ARMED_HOME
                ||
                armingStatus ==
                        ArmingStatus.ARMED_AWAY) {

            // Create copy to avoid ConcurrentModificationException
            Set<Sensor> sensorsCopy =
                    new HashSet<>(
                            securityRepository.getSensors());

            for (Sensor sensor : sensorsCopy) {

                sensor.setActive(false);

                securityRepository.updateSensor(sensor);
            }
        }

        // Cat already detected + armed-home -> ALARM
        if (armingStatus ==
                ArmingStatus.ARMED_HOME
                &&
                catDetected) {

            setAlarmStatus(
                    AlarmStatus.ALARM);
        }
    }

    /**
     * Handles cat detection.
     */
    private void catDetected(Boolean cat) {

        this.catDetected = cat;

        // Cat detected while armed-home
        if (cat &&
                getArmingStatus()
                        == ArmingStatus.ARMED_HOME) {

            setAlarmStatus(
                    AlarmStatus.ALARM);
        }

        // No cat detected
        else if (!cat) {

            // Only reset if NO active sensors
            boolean activeSensorsExist =
                    securityRepository
                            .getSensors()
                            .stream()
                            .anyMatch(Sensor::getActive);

            if (!activeSensorsExist) {

                setAlarmStatus(
                        AlarmStatus.NO_ALARM);
            }
        }

        statusListeners.forEach(
                sl -> sl.catDetected(cat));
    }

    /**
     * Adds status listener.
     */
    public void addStatusListener(
            StatusListener statusListener) {

        statusListeners.add(statusListener);
    }

    /**
     * Removes status listener.
     */
    public void removeStatusListener(
            StatusListener statusListener) {

        statusListeners.remove(statusListener);
    }

    /**
     * Updates alarm status.
     */
    public void setAlarmStatus(
            AlarmStatus status) {

        securityRepository
                .setAlarmStatus(status);

        statusListeners.forEach(
                sl -> sl.notify(status));
    }

    /**
     * Handles newly activated sensor.
     */
    private void handleSensorActivated() {

        // Ignore if disarmed
        if (securityRepository
                .getArmingStatus()
                == ArmingStatus.DISARMED) {

            return;
        }

        switch (
                securityRepository
                        .getAlarmStatus()) {

            case NO_ALARM ->
                    setAlarmStatus(
                            AlarmStatus.PENDING_ALARM);

            case PENDING_ALARM ->
                    setAlarmStatus(
                            AlarmStatus.ALARM);

            case ALARM -> {
                // Keep ALARM state
            }

            default -> {
            }
        }
    }

    /**
     * Handles sensor deactivation.
     */
    private void handleSensorDeactivated() {

        switch (
                securityRepository
                        .getAlarmStatus()) {

            case PENDING_ALARM -> {

                boolean activeSensorsExist =
                        securityRepository
                                .getSensors()
                                .stream()
                                .anyMatch(Sensor::getActive);

                if (!activeSensorsExist) {

                    setAlarmStatus(
                            AlarmStatus.NO_ALARM);
                }
            }

            case ALARM -> {
                // ALARM must remain ALARM
            }

            default -> {
            }
        }
    }

    /**
     * Changes sensor activation state.
     */
    public void changeSensorActivationStatus(
            Sensor sensor,
            Boolean active) {

        // If alarm already active
        // sensor changes should not affect alarm
        if (getAlarmStatus()
                == AlarmStatus.ALARM) {

            sensor.setActive(active);

            securityRepository.updateSensor(sensor);

            return;
        }

        // Already active + pending -> ALARM
        if (sensor.getActive()
                &&
                active
                &&
                getAlarmStatus()
                        == AlarmStatus.PENDING_ALARM) {

            setAlarmStatus(
                    AlarmStatus.ALARM);
        }

        // Newly activated sensor
        else if (!sensor.getActive()
                &&
                active) {

            handleSensorActivated();
        }

        // Deactivated sensor
        else if (sensor.getActive()
                &&
                !active) {

            sensor.setActive(false);

            securityRepository.updateSensor(sensor);

            handleSensorDeactivated();

            return;
        }

        // Already inactive -> no change
        sensor.setActive(active);

        securityRepository
                .updateSensor(sensor);
    }

    /**
     * Processes image from camera.
     */
    public void processImage(
            BufferedImage currentCameraImage) {

        catDetected(
                imageService.imageContainsCat(
                        currentCameraImage,
                        50.0f));
    }

    public AlarmStatus getAlarmStatus() {

        return securityRepository
                .getAlarmStatus();
    }

    public Set<Sensor> getSensors() {

        return securityRepository
                .getSensors();
    }

    public void addSensor(Sensor sensor) {

        securityRepository
                .addSensor(sensor);
    }

    public void removeSensor(Sensor sensor) {

        securityRepository
                .removeSensor(sensor);
    }

    public ArmingStatus getArmingStatus() {

        return securityRepository
                .getArmingStatus();
    }
}