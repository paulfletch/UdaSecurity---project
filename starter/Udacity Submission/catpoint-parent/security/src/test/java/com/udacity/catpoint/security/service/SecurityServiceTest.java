package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.service.ImageService;

import com.udacity.catpoint.security.application.StatusListener;
import com.udacity.catpoint.security.data.*;

import com.udacity.catpoint.security.data.SecurityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.Set;
import java.util.stream.Stream;


/**
 * Unit test for SecurityService.
 */

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {

    @Mock
    private ImageService imageService;
    @Mock
    private SecurityRepository securityRepository;
    @Mock
    private Sensor sensor1;
    @Mock
    private Sensor sensor2;
    @Mock
    private BufferedImage bufferedImage;
    @Mock
    private Set<Sensor> sensorSet;
    @Mock
    private StatusListener statusListener1;
    @Mock
    private StatusListener statusListener2;


    private SecurityService securityService;


    @BeforeEach
    void setUp() {
        securityService = new SecurityService(securityRepository, imageService);
    }


    /**
     * Test 0
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue() {
        assertTrue(true);
    }


    /**
     * Test 1
     * If alarm is armed and a sensor becomes activated,
     * put the system into pending alarm status.
     */
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    public void alarmIsArmed_sensorIsActivated_setAlarmStatusPending(ArmingStatus armingStatus) {
        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(sensor1, true);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    /**
     * Test 2
     * If alarm is armed and a sensor becomes activated and the system is already pending alarm,
     * set the alarm status to alarm.
     */
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    public void alarmIsPending_sensorIsActivated_setAlarmStatusAlarm(ArmingStatus armingStatus) {
        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor1, true);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }


    /**
     * Test 3
     * If pending alarm and all sensors are inactive,
     * return to no alarm state.
     */
    @Test
    public void alarmIsPending_allSensorsAreInactive_setNoAlarmState() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(sensor1.getActive()).thenReturn(true);
        securityService.changeSensorActivationStatus(sensor1, false);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    /**
     * Test 4
     * If alarm is active,
     * change in sensor state should not affect the alarm state.
     */
    @Test   // Sensor Activation
    public void alarmIsActive_changeInSensorFromActivatedToDeactivated_alarmRemainsActive() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        when(sensor1.getActive()).thenReturn(true);
        securityService.changeSensorActivationStatus(sensor1, false);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    @Test // Sensor Deactivation
    public void alarmIsActive_changeInSensorFromDeactivatedToActivated_alarmRemainsActive() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        when((securityRepository.getArmingStatus())).thenReturn(ArmingStatus.ARMED_HOME);
        when(sensor1.getActive()).thenReturn(false);
        securityService.changeSensorActivationStatus(sensor1, true);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }


    /**
     * Test 5
     * If a sensor is activated while already active and the system is in pending state,
     * change it to alarm state.
     */
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    public void sensorIsActive_sensorIsActivated_systemIsPendingState_setAlarm(ArmingStatus armingstatus) {
        when((securityRepository.getArmingStatus())).thenReturn(armingstatus);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(sensor1.getActive()).thenReturn(true);
        securityService.changeSensorActivationStatus(sensor1, true);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    /**
     * Test 6
     * If a sensor is deactivated while already inactive,
     * make no changes to the alarm state.
     */
    @Test
    public void sensorIsInactive_sensorIsDeactivated_noChangeToAlarm() {
        when(sensor1.getActive()).thenReturn(false);
        securityService.changeSensorActivationStatus(sensor1, false);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }



    /**
     * Test 7
     * If the image service identifies an image containing a cat while the system is armed-home,
     * put the system into alarm status.
     */
    @Test
    public void systemIsArmedHome_imageServiceIdentifiesCat_setAlarm() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        securityService.processImage(bufferedImage);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    // Inferred from requirement
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"DISARMED", "ARMED_AWAY"})
    public void systemIsNotArmedHome_imageServiceIdentifiesCat_doesNotSetAlarm(ArmingStatus armingStatus) {
        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        securityService.processImage(bufferedImage);
        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.ALARM);
    }



    /**
     * Test 8
     * If the image service identifies an image that does not contain a cat,
     * change the status to no alarm as long as the sensors are not active.
     */
    @Test
    public void imageServiceIdentifiesNoCat_sensorsNotActive_setNoAlarm() {
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(false);
        when(securityRepository.getSensors()).thenReturn(sensorSet);
        when(sensorSet.stream()).thenReturn(Stream.of(sensor1, sensor2));
        when(sensor1.getActive()).thenReturn(false);
        when(sensor2.getActive()).thenReturn(false);
        securityService.processImage(bufferedImage);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    public void imageServiceIdentifiesNoCat_sensorIsActive_doNotAdjustAlarm() {
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(false);
        when(securityRepository.getSensors()).thenReturn(sensorSet);
        when(sensorSet.stream()).thenReturn(Stream.of(sensor1, sensor2));
        when(sensor1.getActive()).thenReturn(false);
        when(sensor2.getActive()).thenReturn(true);
        securityService.processImage(bufferedImage);
        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    /**
     * Test 9
     * If the system is disarmed,
     * set the status to no alarm.
     */
    @Test
    public void systemIsDisarmed_setNoAlarm() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }


    /**
     * Test 10
     * If the system is armed,
     * reset all sensors to inactive.
     */
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    public void systemIsArmed_allSensorsResetToInactive(ArmingStatus armingStatus) {
        when(securityService.getSensors() ).thenReturn(Set.of(sensor1, sensor2)  );
        securityService.setArmingStatus(armingStatus);
        verify(sensor1, times(1)).setActive(false);
        verify(sensor2, times(1)).setActive(false);
        securityService.addStatusListener(statusListener1);
    }




    /**
     * Test 11
     * If the system is armed-home while the camera shows a cat,
     * set the alarm status to alarm.
     */
    @Test
    public void systemIsArmed_cameraShowsCat_setAlarmAlarm() {
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        securityService.processImage(bufferedImage);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    public void systemIsArmed_cameraShowsNoCat_doNotSetAlarmAlarm() {
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(false);
        securityService.processImage(bufferedImage);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.ALARM);
    }


    // Further tests


    // When a status listener is added, it receives notifications of sensor changes
    // When a status listener is removed, it does not receive notifications of sensor changes
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void statusListenerAdded_sensorIsChange_sensorStatusChanged_listenerIsNotifiedOfSensorChange(boolean sensorActivationStatus) {
        securityService.addStatusListener(statusListener1);
        securityService.addSensor(sensor1);
        securityService.changeSensorActivationStatus(sensor1,sensorActivationStatus);
        verify(statusListener1, times(1)).sensorStatusChanged();
    }
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void statusListenerAdded_statusListenerRemoved_removedListenerIsNotNotifiedOfSensorChange(boolean sensorActivationStatus) {
        securityService.addStatusListener(statusListener1);
        securityService.addStatusListener(statusListener2);
        securityService.removeStatusListener(statusListener2);
        securityService.addSensor(sensor1);
        securityService.changeSensorActivationStatus(sensor1,sensorActivationStatus);
        verify(statusListener1, times(1)).sensorStatusChanged();
        verify(statusListener2, never()).sensorStatusChanged();
    }

    // When a status listener is added, it receives notifications of cat detections
    // When a status listener is removed, it does not receive notifications of cat detections
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void statusListenerAdded_catDetected_listenerIsNotifiedOfCatDetection(boolean catDetectedStatus) {
        when(imageService.imageContainsCat(any(),anyFloat())).thenReturn(catDetectedStatus);
        securityService.addStatusListener(statusListener1);
        securityService.addSensor(sensor1);
        securityService.processImage(bufferedImage);
        verify(statusListener1, times(1)).catDetected(catDetectedStatus);
    }
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void statusListenerAdded_catDetected_removedListenerIsNotNotifiedOfCatDetection(boolean catDetectedStatus) {
        when(imageService.imageContainsCat(any(),anyFloat())).thenReturn(catDetectedStatus);
        securityService.addStatusListener(statusListener1);
        securityService.addStatusListener(statusListener2);
        securityService.removeStatusListener(statusListener2);
        securityService.addSensor(sensor1);
        securityService.processImage(bufferedImage);
        verify(statusListener1, times(1)).catDetected(catDetectedStatus);
        verify(statusListener2, never()).catDetected(catDetectedStatus);
    }

    // When a status listener is added, it receives notifications of Alarm Status
    // When a status listener is removed, it does not receive notifications of Alarm Status
    @ParameterizedTest
    @EnumSource(value = AlarmStatus.class, names = {"ALARM", "NO_ALARM","PENDING_ALARM"})
    public void statusListenerAdded_alarmStatusChange_listenerIsNotifiedOfAlarmStatus(AlarmStatus alarmStatus) {
        securityService.addStatusListener(statusListener1);
        securityService.addSensor(sensor1);
        securityService.setAlarmStatus(alarmStatus);
        verify(statusListener1, times(1)).notify(alarmStatus);
    }
    @ParameterizedTest
    @EnumSource(value = AlarmStatus.class, names = {"ALARM", "NO_ALARM","PENDING_ALARM"})
    public void statusListenerAdded_alarmStatusChange_removedListenerIsNotNotifiedOfAlarmStatus(AlarmStatus alarmStatus) {
        securityService.addStatusListener(statusListener1);
        securityService.addStatusListener(statusListener2);
        securityService.removeStatusListener(statusListener2);
        securityService.addSensor(sensor1);
        securityService.setAlarmStatus(alarmStatus);
        verify(statusListener1, times(1)).notify(alarmStatus);
        verify(statusListener2, never()).notify(alarmStatus);
    }


    // Tests for when sensor is added  or removed.  The sensor maybe active or inactive at the time of adding or removing

    @Test
    public void inactiveSensorIsAdded_systemIsArmedAndPendingAlarm_noChangeAlarmState() {
        when(sensor1.getActive()).thenReturn(false);
        securityService.addSensor(sensor1);
        verify(securityRepository, times(1)).addSensor(sensor1);
        verify(securityRepository, never()).setAlarmStatus(any());
    }

    @Test
    public void activeSensorIsAdded_systemIsArmedAndPendingAlarm_setAlarmState() {
        when(sensor1.getActive()).thenReturn(true);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.addSensor(sensor1);
        verify(securityRepository, times(1)).addSensor(sensor1);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    public void inactiveSensorIsRemoved_systemIsArmedAndPendingAlarm_noChangeToAlarm() {
        when(sensor1.getActive()).thenReturn(false);
        securityService.removeSensor(sensor1);
        verify(securityRepository, times(1)).removeSensor(sensor1);
        verify(securityRepository, never()).setAlarmStatus(any());
    }

    @Test
    public void activeSensorIsRemoved_systemIsArmedAndPendingAlarm_changeToNoAlarm() {
        when(sensor1.getActive()).thenReturn(true);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.removeSensor(sensor1);
        verify(securityRepository, times(1)).removeSensor(sensor1);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }




}





