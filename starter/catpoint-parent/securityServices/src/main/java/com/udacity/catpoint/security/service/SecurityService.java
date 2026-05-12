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

    private ImageService imageService;

    private SecurityRepository securityRepository;

    private Set<StatusListener> statusListeners =
            new HashSet<>();

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

        // If system disarmed -> NO_ALARM
        if (armingStatus ==
                ArmingStatus.DISARMED) {

            setAlarmStatus(
                    AlarmStatus.NO_ALARM);
        }

        // If system armed -> reset sensors inactive
        if (armingStatus ==
                ArmingStatus.ARMED_HOME
                ||
                armingStatus ==
                        ArmingStatus.ARMED_AWAY) {

            securityRepository
                    .getSensors()
                    .forEach(sensor ->
                            sensor.setActive(false));
        }

        securityRepository
                .setArmingStatus(
                        armingStatus);
    }

    /**
     * Handles cat detection.
     */
    private void catDetected(Boolean cat) {

        // Cat detected while armed home
        if (cat &&
                getArmingStatus()
                        == ArmingStatus.ARMED_HOME) {

            setAlarmStatus(
                    AlarmStatus.ALARM);
        }

        // No cat and no active sensors
        else if (
                securityRepository
                        .getSensors()
                        .stream()
                        .noneMatch(
                                Sensor::getActive)
        ) {

            setAlarmStatus(
                    AlarmStatus.NO_ALARM);
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
            default->{
                break;
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

            case PENDING_ALARM ->
                    setAlarmStatus(
                            AlarmStatus.NO_ALARM);

            case ALARM ->
                    setAlarmStatus(
                            AlarmStatus.PENDING_ALARM);
            default->{
                break;
            }
        }
    }

    /**
     * Changes sensor activation state.
     */
    public void changeSensorActivationStatus(
            Sensor sensor,
            Boolean active) {

        // Already active + pending alarm -> ALARM
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
        else if (
                !sensor.getActive()
                        &&
                        active) {

            handleSensorActivated();
        }

        // Deactivated sensor
        else if (
                sensor.getActive()
                        &&
                        !active) {

            handleSensorDeactivated();
        }

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