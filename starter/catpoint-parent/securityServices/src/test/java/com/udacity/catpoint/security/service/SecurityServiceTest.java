package com.udacity.catpoint.security.service;

import com.udacity.catpoint.application.StatusListener;

import com.udacity.catpoint.data.AlarmStatus;
import com.udacity.catpoint.data.ArmingStatus;
import com.udacity.catpoint.data.SecurityRepository;
import com.udacity.catpoint.data.Sensor;
import com.udacity.catpoint.data.SensorType;

import com.udacity.catpoint.image.service.ImageService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SecurityServiceTest {

    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private ImageService imageService;

    private SecurityService securityService;

    @BeforeEach
    void setUp() {

        MockitoAnnotations.openMocks(this);

        securityService =
                new SecurityService(
                        securityRepository,
                        imageService);
    }

    /**
     * If alarm is armed and a sensor becomes activated,
     * put system into pending alarm state.
     */
    @Test
    void whenSystemArmedAndSensorActivated_thenPendingAlarm() {

        Sensor sensor =
                new Sensor(
                        "Front Door",
                        SensorType.DOOR);

        when(securityRepository.getArmingStatus())
                .thenReturn(
                        ArmingStatus.ARMED_HOME);

        when(securityRepository.getAlarmStatus())
                .thenReturn(
                        AlarmStatus.NO_ALARM);

        securityService.changeSensorActivationStatus(
                sensor,
                true);

        verify(securityRepository)
                .setAlarmStatus(
                        AlarmStatus.PENDING_ALARM);
    }

    /**
     * If alarm is armed and sensor activates while pending,
     * set alarm state.
     */
    @Test
    void whenPendingAlarmAndSensorActivated_thenAlarmState() {

        Sensor sensor =
                new Sensor(
                        "Window",
                        SensorType.WINDOW);

        when(securityRepository.getArmingStatus())
                .thenReturn(
                        ArmingStatus.ARMED_HOME);

        when(securityRepository.getAlarmStatus())
                .thenReturn(
                        AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(
                sensor,
                true);

        verify(securityRepository)
                .setAlarmStatus(
                        AlarmStatus.ALARM);
    }

    /**
     * If pending alarm and all sensors inactive,
     * return to no alarm.
     */
    @Test
    void whenPendingAlarmAndAllSensorsInactive_thenNoAlarm() {

        Sensor sensor =
                new Sensor(
                        "Back Window",
                        SensorType.WINDOW);

        sensor.setActive(true);

        when(securityRepository.getAlarmStatus())
                .thenReturn(
                        AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(
                sensor,
                false);

        verify(securityRepository)
                .setAlarmStatus(
                        AlarmStatus.NO_ALARM);
    }

    /**
     * If alarm already active,
     * sensor changes should not affect alarm.
     */
    @Test
    void whenAlarmActive_thenSensorChangesDoNotAffectAlarm() {

        Sensor sensor =
                new Sensor(
                        "Door",
                        SensorType.DOOR);

        when(securityRepository.getAlarmStatus())
                .thenReturn(
                        AlarmStatus.ALARM);

        securityService.changeSensorActivationStatus(
                sensor,
                true);

        verify(securityRepository,
                never())
                .setAlarmStatus(
                        AlarmStatus.PENDING_ALARM);
    }

    /**
     * If sensor activated while already active
     * and system pending,
     * set alarm state.
     */
    @Test
    void whenSensorAlreadyActiveAndPending_thenAlarm() {

        Sensor sensor =
                new Sensor(
                        "Garage",
                        SensorType.DOOR);

        sensor.setActive(true);

        when(securityRepository.getAlarmStatus())
                .thenReturn(
                        AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(
                sensor,
                true);

        verify(securityRepository)
                .setAlarmStatus(
                        AlarmStatus.ALARM);
    }

    /**
     * If sensor already inactive,
     * no alarm changes.
     */
    @Test
    void whenSensorAlreadyInactive_thenNoAlarmStateChange() {

        Sensor sensor =
                new Sensor(
                        "Window",
                        SensorType.WINDOW);

        sensor.setActive(false);

        securityService.changeSensorActivationStatus(
                sensor,
                false);

        verify(securityRepository,
                never())
                .setAlarmStatus(any());
    }

    /**
     * If cat detected while armed home,
     * set alarm.
     */
    @Test
    void whenCatDetectedAndArmedHome_thenAlarm() {

        when(securityRepository.getArmingStatus())
                .thenReturn(
                        ArmingStatus.ARMED_HOME);

        when(imageService.imageContainsCat(
                any(),
                anyFloat()))
                .thenReturn(true);

        securityService.processImage(null);

        verify(securityRepository)
                .setAlarmStatus(
                        AlarmStatus.ALARM);
    }

    /**
     * If no cat detected,
     * set no alarm.
     */
    @Test
    void whenNoCatDetected_thenNoAlarm() {

        when(imageService.imageContainsCat(
                any(),
                anyFloat()))
                .thenReturn(false);

        securityService.processImage(null);

        verify(securityRepository)
                .setAlarmStatus(
                        AlarmStatus.NO_ALARM);
    }

    /**
     * If no cat but sensors active,
     * keep alarm state.
     */
    @Test
    void whenNoCatButSensorActive_thenKeepAlarm() {

        Sensor sensor =
                new Sensor(
                        "Door",
                        SensorType.DOOR);

        sensor.setActive(true);

        when(imageService.imageContainsCat(
                any(),
                anyFloat()))
                .thenReturn(false);

        when(securityRepository.getSensors())
                .thenReturn(Set.of(sensor));

        securityService.processImage(null);

        verify(securityRepository,
                never())
                .setAlarmStatus(
                        AlarmStatus.NO_ALARM);
    }

    /**
     * If system disarmed,
     * set no alarm.
     */
    @Test
    void whenSystemDisarmed_thenSetNoAlarm() {

        securityService.setArmingStatus(
                ArmingStatus.DISARMED);

        verify(securityRepository)
                .setAlarmStatus(
                        AlarmStatus.NO_ALARM);
    }

    /**
     * Parameterized test:
     * when system armed,
     * all sensors become inactive.
     */
    @ParameterizedTest
    @EnumSource(
            value = ArmingStatus.class,
            names = {
                    "ARMED_HOME",
                    "ARMED_AWAY"
            }
    )
    void whenSystemArmed_thenSensorsBecomeInactive(
            ArmingStatus status) {

        Sensor sensor =
                new Sensor(
                        "Door",
                        SensorType.DOOR);

        sensor.setActive(true);

        when(securityRepository.getSensors())
                .thenReturn(Set.of(sensor));

        securityService.setArmingStatus(status);

        assertFalse(sensor.getActive());
    }

    /**
     * If armed home while camera detects cat,
     * set alarm state.
     */
    @Test
    void whenArmedHomeAndCatDetected_thenAlarm() {

        when(securityRepository.getArmingStatus())
                .thenReturn(
                        ArmingStatus.ARMED_HOME);

        when(imageService.imageContainsCat(
                any(),
                anyFloat()))
                .thenReturn(true);

        securityService.processImage(null);

        verify(securityRepository)
                .setAlarmStatus(
                        AlarmStatus.ALARM);
    }

    /**
     * Test addSensor().
     */
    @Test
    void whenAddSensor_thenRepositoryCalled() {

        Sensor sensor =
                new Sensor(
                        "Main Door",
                        SensorType.DOOR);

        securityService.addSensor(sensor);

        verify(securityRepository)
                .addSensor(sensor);
    }

    /**
     * Test removeSensor().
     */
    @Test
    void whenRemoveSensor_thenRepositoryCalled() {

        Sensor sensor =
                new Sensor(
                        "Window",
                        SensorType.WINDOW);

        securityService.removeSensor(sensor);

        verify(securityRepository)
                .removeSensor(sensor);
    }

    /**
     * Test getSensors().
     */
    @Test
    void whenGetSensors_thenRepositoryCalled() {

        securityService.getSensors();

        verify(securityRepository)
                .getSensors();
    }

    /**
     * Test getAlarmStatus().
     */
    @Test
    void whenGetAlarmStatus_thenRepositoryCalled() {

        securityService.getAlarmStatus();

        verify(securityRepository)
                .getAlarmStatus();
    }

    /**
     * Test getArmingStatus().
     */
    @Test
    void whenGetArmingStatus_thenRepositoryCalled() {

        securityService.getArmingStatus();

        verify(securityRepository)
                .getArmingStatus();
    }

    /**
     * Test addStatusListener().
     */
    @Test
    void whenAddStatusListener_thenAdded() {

        StatusListener listener = null;

        securityService.addStatusListener(listener);
    }

    /**
     * Test removeStatusListener().
     */
    @Test
    void whenRemoveStatusListener_thenRemoved() {

        StatusListener listener = null;

        securityService.removeStatusListener(listener);
    }

    /**
     * If alarm active and sensor deactivated,
     * move to pending alarm.
     */
    @Test
    void whenAlarmAndSensorDeactivated_thenPendingAlarm() {

        Sensor sensor =
                new Sensor(
                        "Door",
                        SensorType.DOOR);

        sensor.setActive(true);

        when(securityRepository.getAlarmStatus())
                .thenReturn(
                        AlarmStatus.ALARM);

        securityService.changeSensorActivationStatus(
                sensor,
                false);

        verify(securityRepository)
                .setAlarmStatus(
                        AlarmStatus.PENDING_ALARM);
    }
}