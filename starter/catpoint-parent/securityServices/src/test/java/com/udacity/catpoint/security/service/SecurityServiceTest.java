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
     * Armed + sensor activated -> pending alarm.
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
     * Pending + sensor activated -> alarm.
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
     * Pending + all inactive -> no alarm.
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
     * Alarm active -> sensor changes ignored.
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
     * Sensor already active + pending -> alarm.
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
     * Sensor already inactive -> no change.
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
     * Cat detected + armed home -> alarm.
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
     * No cat detected -> no alarm.
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
     * No cat + active sensor -> keep alarm.
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
     * Disarmed -> no alarm.
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
     * Armed -> sensors inactive.
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
     * Armed home + cat detected -> alarm.
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
     * Alarm active + sensor deactivated
     * -> alarm remains active.
     */
    @Test
    void whenAlarmAndSensorDeactivated_thenAlarmStaysActive() {

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

        verify(securityRepository,
                never())
                .setAlarmStatus(
                        AlarmStatus.PENDING_ALARM);
    }

    @Test
    void whenSystemArmed_thenAllSensorsResetInactive() {

        Sensor sensor1 =
                new Sensor(
                        "Door",
                        SensorType.DOOR);

        Sensor sensor2 =
                new Sensor(
                        "Window",
                        SensorType.WINDOW);

        sensor1.setActive(true);
        sensor2.setActive(true);

        Set<Sensor> sensors =
                Set.of(sensor1, sensor2);

        when(securityRepository.getSensors())
                .thenReturn(sensors);

        securityService.setArmingStatus(
                ArmingStatus.ARMED_HOME);

        assertFalse(sensor1.getActive());
        assertFalse(sensor2.getActive());

        verify(securityRepository)
                .updateSensor(sensor1);

        verify(securityRepository)
                .updateSensor(sensor2);
    }
}