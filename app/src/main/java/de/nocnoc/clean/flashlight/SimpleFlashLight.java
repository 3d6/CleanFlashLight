package de.nocnoc.clean.flashlight;

/*
 This file is part of CleanFlashLight.

 CleanFlashLight is free software: you can redistribute it and/or modify
 it under the terms of the GNU Lesser General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 CleanFlashLight is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public License
 along with Foobar.  If not, see <http://www.gnu.org/licenses/>.

 Diese Datei ist Teil von CleanFlashLight.

 CleanFlashLight ist Freie Software: Sie koennen sie unter den Bedingungen
 der GNU Lesser General Public License, wie von der Free Software Foundation,
 Version 3 der Lizenz oder (nach Ihrer Wahl) jeder spaeteren
 veroeffentlichten Version, weiterverbreiten und/oder modifizieren.

 CleanFlashLight wird in der Hoffnung, dass es nuetzlich sein wird, aber
 OHNE JEDE GEWAEHRLEISTUNG, bereitgestellt; sogar ohne die implizite
 Gewaehrleistung der MARKTFAEHIGKEIT oder EIGNUNG FUER EINEN BESTIMMTEN ZWECK.
 Siehe die GNU Lesser General Public License fuer weitere Details.

 Sie sollten eine Kopie der GNU Lesser General Public License zusammen mit diesem
 Programm erhalten haben. Wenn nicht, siehe <http://www.gnu.org/licenses/>.
 */

/**
 * Simple Flash Light interface provides
 * easy access and functionality to camera flashlight.
 * <p/>
 *
 * @author nocnoc
 */
public interface SimpleFlashLight {

    /**
     * Returns the device initialization state
     *
     * @return true if the device is initialized can be used.
     */
    boolean isInitialized();

    /**
     * Opens the camera device.
     *
     * @return true if the device was opened successfully
     */
    boolean openCamera();

    /**
     * Closes the camera device.
     *
     * @return true if the device was opened successfully
     */
    boolean closeCamera();


    /**
     * Returns the opened state of the device
     *
     * @return true if the device is open and can be switched on and off
     */
    boolean isDeviceOpened();

    /**
     * switches the state of the flash. the camera device is needed to be opened.
     */
    void switchFlash();

    /**
     * Turns the flash on. the camera device is needed to be opened.
     */
    void turnOnFlash();

    /**
     * Turns the flash off. the camera device is needed to be opened.
     */
    void turnOffFlash();

    /**
     * Returns the flash-on state. this indicator may not be reliable.
     *
     * @return true if the flash is enabled.
     */
    boolean isFlashOn();

}
